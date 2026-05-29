/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useContext, useEffect, useRef, useState } from "react";
import { AppContext } from "../../../../store/context";
import {
  cloneDeep,
  filter,
  find,
  flatten,
  get,
  has,
  isEmpty,
  map,
} from "lodash";
import { EnableHighAvailibilityContext } from "./store/context";
import { getStepData } from "../../../../Utils/Utility";
import { HostsApi } from "../../../../api/hostsApi";
import Spinner from "../../../../components/Spinner";
import {
  Accordion,
  Alert,
  Badge,
  Card,
  Col,
  Form,
  Row,
  Stack,
} from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faPlus } from "@fortawesome/free-solid-svg-icons";
import { configValidator } from "../../../../Utils/validators";
import { t } from "i18next";
import classNames from "classnames";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./store/types";
import useHDFSConfigsTags from "../../../../hooks/useConfigsTags";
import rm_ha_properties from "../../../../data/configs/wizards/rm_ha_properties";
import ConfigsApi from "../../../../api/configsApi";
import RmHaConfigInitializer from "../../../../Utils/rm_ha_config_initializer";
import { useConfigs } from "../../../../hooks/useConfigs";
import { groupPropertyValues } from "../../../../Utils/dataUtils";
import useHostComponents from "../../../ClusterWizard/hooks/useHostComponents";
import { blueprintUtils } from "../../../ClusterWizard/utils";
import RecommendationsApi from "../../../../api/recommendationsApi";
import useKDCSessionState from "../../../../hooks/useKDCSessionState";


function Step3() {
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  } = useContext(EnableHighAvailibilityContext);
  const { clusterName, services } = useContext(AppContext);
  const serverConfigDataRef = useRef<any>([]);
  const [clusterHostComponentsMapping, setClusterHostComponentsMapping] =
    useState<any>([]);
  const [stepConfigs, setStepConfigs] = useState<any>(null);
  const [selectedService, setSelectedService] = useState({});
  const [errors, setErrors] = useState<any>({});
  const { configsData } = useHDFSConfigsTags();
  const { getKDCSessionState } = useKDCSessionState(() => {});
  const { createDefaultConfig, getConfigTagFromFileName } = useConfigs();
  const [overridenProperties, setOverridenProperties] = useState(configsData);
  const {
    hostComponents: serviceHostComponents,
    isLoading: hostComponentsLoading,
  } = useHostComponents(map(services, "ServiceInfo.service_name"));
  const { cluster, allHostNames } = useContext(AppContext);

  useEffect(() => {
    setOverridenProperties(configsData);
  }, [configsData]);

  async function getClusterComponents() {
    const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name,host_components/HostRoles/state&minimal_response=true`;
    const data = await HostsApi.getClusterComponents(clusterName, fields);
    setClusterHostComponentsMapping(data.items);
    return data.items;
  }

  const selectedServices = map(services, "ServiceInfo.service_name");

  async function renderConfigs() {
    const configs = rm_ha_properties.haConfig;

    const serviceConfig = {
      serviceName: configs.serviceName,
      displayName: configs.displayName,
      configCategories: [],
      showConfig: true,
      configs: [],
    };
    configs.configCategories.forEach(function (_configCategory: any) {
      if (selectedServices.includes(_configCategory.name)) {
        serviceConfig.configCategories.push(_configCategory as never);
      }
    });
    renderConfigProperties(configs, serviceConfig);
    serviceConfig.configs.map((con: any) => (con.changedValue = con.value));
    setStepConfigs(serviceConfig);
    try {
      const data = await ConfigsApi.loadConfigTags(clusterName);
      loadConfigTagsSuccessCallBack(data, { serviceConfig });
    } catch (error) {
      console.log("Error 1");
    }
  }

  async function loadConfigTagsSuccessCallBack(data: any, params: any) {
    var urlParams =
      "(type=zoo.cfg&tag=" +
      data.Clusters.desired_configs["zoo.cfg"].tag +
      ")|" +
      "(type=yarn-site&tag=" +
      data.Clusters.desired_configs["yarn-site"].tag +
      ")|" +
      "(type=yarn-env&tag=" +
      data.Clusters.desired_configs["yarn-env"].tag +
      ")";

    // try {
    const response = await ConfigsApi.getConfigsByTags(clusterName, urlParams);
    loadConfigsSuccessCallback(response, params);
    console.log("data", response);

    // } catch (error) {
    //   console.log("Error 2");
    // }
  }

  async function loadConfigsSuccessCallback(data: any, params: any) {
    const blueprintConfigurations = get(data, "items", []).reduce(
      (prev: any, cur: any) => {
        prev[cur.type] = { properties: cur.properties };
        return prev;
      },
      {}
    );

    params = params.serviceConfig
      ? params.serviceConfig
      : arguments[4].serviceConfig;
    setDynamicConfigValues(params.configs, data);

    // try {
    const recommendations = await loadRecommendations(blueprintConfigurations);
    console.log("Recommendations", recommendations);
    applyRecommendedConfigurations(recommendations, data, params);
    setSelectedService(params);
    // setIsLoaded(true);
    // }
    // catch (error) {
    //   console.error("Failed to load recommendations", error);
    // }
  }

  function getCurrentMasterSlaveBlueprint() {
    console.log("serviceHostComponents", serviceHostComponents);
    const components = formatRecommendComponents(serviceHostComponents);
    return getComponentsBlueprint(components);
  }

  //   function getComponentsBlueprint(components, allHostNames){

  //   }

  async function loadRecommendations(blueprintConfigurations: any) {
    const blueprint = getCurrentMasterSlaveBlueprint();
    const hostGroupName = blueprintUtils.getHostGroupByFqdn(
      blueprint,
      getMastersInfo().addResourceManager
    );
    var dataToSend = {
      recommend: "configurations",
      hosts: allHostNames,
      services: selectedServices,
      recommendations: {},
    };

    if (!!hostGroupName) {
      blueprintUtils.addComponentToHostGroup(
        blueprint,
        "RESOURCEMANAGER",
        hostGroupName
      );
    }
    blueprint.blueprint.configurations = blueprintConfigurations;
    dataToSend.recommendations = blueprint;

    let recommendations = {};
    //  try {

    const stack = get(cluster, "version", "").split("-")[0];
    const version = get(cluster, "version", "").split("-")[1];
    recommendations = await RecommendationsApi.loadRecommendations(
      stack,
      version,
      dataToSend
    );
    //  } catch (error) {
    //   console.log(error);

    //  }
    console.log("hello", recommendations);
    return recommendations;
  }

  function formatRecommendComponents(components: any) {
    const res: any = [];
    if (!components) return res;

    components.forEach((component: any) => {
      const componentName =
        component.component_name ||
        component?.ServiceComponentInfo?.component_name;
      component.hosts = map(
        get(component, "host_components", []),
        "HostRoles.host_name"
      );
      if (component.hosts && component.hosts.length > 0) {
        component.hosts.forEach((hostName: string) => {
          res.push({
            componentName: componentName,
            hostName: hostName,
          });
        });
      }
    });
    console.log("Returning components are", components, res);
    return res;
  }

  function getComponentsBlueprint(components: any) {
    const uniqueHosts = allHostNames;
    const mappedComponents = groupPropertyValues(components, "hostName");
    const res: any = {
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] },
    };
    uniqueHosts.forEach(function (host, i) {
      var group_name = "host-group-" + (i + 1);

      res.blueprint.host_groups.push({
        name: group_name,
        components: mappedComponents[host]
          ? mappedComponents[host].map(function (c: any) {
              return { name: get(c, "componentName") };
            })
          : [],
      } as never);

      res.blueprint_cluster_binding.host_groups.push({
        name: group_name,
        hosts: [{ fqdn: host }],
      } as never);
    });
    return res;
  }

  function applyRecommendedConfigurations(
    recommendations: any,
    configurations: any,
    stepConfigs: any
  ) {
    console.log("yarn user");
    const yarnEnv =
      configurations?.items.find((item: any) => item.type === "yarn-env") || {};
    const yarnUser = yarnEnv?.properties?.yarn_user || false;
    const coreSite =
      recommendations?.resources[0]?.recommendations?.blueprint
        ?.configurations?.["core-site"]?.properties || {};
    const proxyHostName = `hadoop.proxyuser.${yarnUser}.hosts`;
    const recommendedHosts = coreSite[proxyHostName] || false;

    if (yarnUser && recommendedHosts) {
      const stepConfigsCopy = cloneDeep(stepConfigs);
      const existingConfig = stepConfigsCopy.configs.find(
        (config: any) => config.name === proxyHostName
      );
      if (existingConfig) {
        existingConfig.setProperties({
          recommendedValue: recommendedHosts,
          value: recommendedHosts,
          changedValue: recommendedHosts,
        });
      } else {
        const newProp = createDefaultConfig(proxyHostName, "core-site", false, {
          category: "HDFS",
          isUserProperty: false,
          isEditable: false,
          isOverridable: false,
          serviceName: "MISC",
          value: recommendedHosts,
          changedValue: recommendedHosts,
          recommendedValue: recommendedHosts,
        });
        newProp.filename = getConfigTagFromFileName(newProp.filename);
        stepConfigsCopy.configs.push(newProp);
      }
      setStepConfigs(stepConfigsCopy);
    }
  }

  function setDynamicConfigValues(configs: any, data: any) {
    console.log("dynamic configs", configs);

    const masterComponentHosts = getStepData(
      state,
      "SELECT_HOSTS",
      "masterComponentHosts",
      "enableHighAvailibilitySteps"
    );

    const hosts = getStepData(
      state,
      "SELECT_HOSTS",
      "hosts",
      "enableHighAvailibilitySteps"
    );

    const slaveComponentHosts = getStepData(
      state,
      "SELECT_HOSTS",
      "hosts",
      "enableHighAvailibilitySteps"
    );

    const localDB: any = {
      hosts: hosts,
      masterComponentHosts: masterComponentHosts,
      slaveComponentHosts: slaveComponentHosts,
    };

    console.log("localdb", localDB);

    const yarnUser = find(data?.items, ["type", "yarn-env"]).properties
      .yarn_user;

    RmHaConfigInitializer().setup({ yarnUser: yarnUser });
    const dependencies = _prepareDependencies(data);
    configs.forEach((config: any) => {
      RmHaConfigInitializer().initialValue(config, localDB, dependencies);
      config.isOverridable = false;
      config.changedValue = config.value;
    });

    RmHaConfigInitializer().cleanup();
    console.log("Final COnfigs", configs);
  }

  function _prepareDependencies(data: any) {
    var ret: any = {};
    var zooCfg =
      data && data.items
        ? find(data?.items, ["type", "zoo.cfg"]).properties
        : null;
    var yarnSite =
      data && data.items
        ? find(data?.items, ["type", "yarn-site"]).properties
        : null;
    var portValue = zooCfg && get(zooCfg, "properties.clientPort");
    var webAddressPort =
      yarnSite && yarnSite.properties
        ? yarnSite.properties["yarn.resourcemanager.webapp.address"]
        : "";
    var httpsWebAddressPort =
      yarnSite && yarnSite.properties
        ? yarnSite.properties["yarn.resourcemanager.webapp.https.address"]
        : "";
    const trackerAddressPort =
      yarnSite && yarnSite.properties
        ? yarnSite.properties["yarn.resourcemanager.resource-tracker.address"]
        : "";

    ret.webAddressPort =
      webAddressPort && webAddressPort.includes(":")
        ? webAddressPort.split(":")[1]
        : "8088";
    ret.httpsWebAddressPort =
      httpsWebAddressPort && httpsWebAddressPort.includes(":")
        ? httpsWebAddressPort.split(":")[1]
        : "8090";
    ret.trackerAddressPort =
      trackerAddressPort && trackerAddressPort.includes(":")
        ? trackerAddressPort.split(":")[1]
        : "8025";
    ret.zkClientPort = portValue ? portValue : "2181";
    return ret;
  }

  function renderConfigProperties(_componentConfig: any, componentConfig: any) {
    _componentConfig.configs.forEach(function (_serviceConfigProperty: any) {
      componentConfig.configs.push({
        ..._serviceConfigProperty,
        isEditable: _serviceConfigProperty.isReconfigurable,
      });
    });
  }

  async function loadConfigsTags() {
    try {
      serverConfigDataRef.current = configsData;
      renderConfigs();
    } catch (configErr) {
      console.error("Error loading config tags", configErr);
    }
  }
  useEffect(() => {
    if (
      clusterHostComponentsMapping.length &&
      configsData &&
      !isEmpty(configsData)&&!hostComponentsLoading
    ) {
      loadConfigsTags();
    }
  }, [clusterHostComponentsMapping, configsData,hostComponentsLoading]);
  useEffect(() => {
    async function makeApiCalls() {
      await getClusterComponents();
    }
    makeApiCalls();
  }, []);
  function getMastersInfo() {
    const step2Data = getStepData(
      state,
      "SELECT_HOSTS",
      "masterComponentHosts",
      "enableHighAvailibilitySteps"
    );

    console.log("step2data", step2Data);

    for (let masterComponent of step2Data) {
      if (!has(masterComponent, "serviceId") || !masterComponent.serviceId) {
        //@ts-ignore
        masterComponent.serviceId = "YARN";
      }
      if (!has(masterComponent, "hostName") || !masterComponent.hostName) {
        masterComponent.hostName = masterComponent.selectedHost;
        masterComponent.component = masterComponent.component_name;
      }
    }
    const currentResourceManager = find(
      filter(step2Data, ["component", "RESOURCEMANAGER"]),
      ["isInstalled", true]
    )?.hostName;
    const addResourceManager = find(
      filter(step2Data, ["component", "RESOURCEMANAGER"]),
      ["isInstalled", false]
    )?.hostName;

    return {
      currentResourceManager,
      addResourceManager,
    };
  }
  function getErrorValidator(displayType: string) {
    switch (displayType) {
      case "checkbox":
      case "custom":
        return function () {
          return "";
        };
      case "int":
        return function (value: string) {
          return !configValidator.isValidInt(value) &&
            !configValidator.isConfigValueLink(value)
            ? t("errorMessage.config.number.integer")
            : "";
        };
      case "float":
        return function (value: string) {
          return !configValidator.isValidFloat(value) &&
            !configValidator.isConfigValueLink(value)
            ? t("errorMessage.config.number.float")
            : "";
        };
      case "directories":
      case "directory":
        return function (value: string, name: string) {
          if (["dfs.datanode.data.dir"].includes(name)) {
            if (!configValidator.isValidDataNodeDir(value))
              return t("errorMessage.config.directory.heterogeneous");
          } else {
            if (!configValidator.isValidDir(value))
              return t("errorMessage.config.directory.default");
          }
          if (!configValidator.isAllowedDir(value)) {
            return t("errorMessage.config.directory.allowed");
          }
          return configValidator.isNotTrimmedRight(value)
            ? t("errorMessage.config.spaces.trailing")
            : "";
        };
      case "email":
        return function (value: string) {
          return !configValidator.isValidEmail(value)
            ? t("errorMessage.config.mail")
            : "";
        };
      case "supportTextConnection":
      case "host":
        return function (value: string) {
          return configValidator.isNotTrimmed(value)
            ? t("errorMessage.config.spaces.trim")
            : "";
        };
      case "password":
        return function (value: string, name: string, retypedPassword: string) {
          if (name === "ranger_admin_password") {
            if (String(value).length < 9) {
              return t("errorMessage.config.password.length");
            }
          }
          return value !== retypedPassword
            ? t("errorMessage.config.password")
            : "";
        };
      case "user":
      case "database":
      case "db_user":
        return function (value: string) {
          return !configValidator.isValidDbName(value)
            ? t("errorMessage.config.user")
            : "";
        };
      case "ldap_url":
        return function (value: string) {
          return !configValidator.isValidLdapsURL(value)
            ? t("errorMessage.config.ldapUrl")
            : "";
        };
      default:
        return function (value: string, name: string) {
          if (
            [
              "javax.jdo.option.ConnectionURL",
              "oozie.service.JPAService.jdbc.url",
            ].includes(name) &&
            !configValidator.isConfigValueLink(value) &&
            configValidator.isConfigValueLink(value)
          ) {
            return t("errorMessage.config.spaces.trim");
          }
          return configValidator.isNotTrimmedRight(value)
            ? t("errorMessage.config.spaces.trailing")
            : "";
        };
    }
  }
  function handleValueChange(propertyName: string, value: string) {
    const stepConfigsCopy: any = cloneDeep(stepConfigs);
    const property = find(stepConfigsCopy.configs, ["name", propertyName]);
    property.changedValue = value;
    const copiedProperties = cloneDeep(overridenProperties);
    const allProperties = flatten(
      map(get(copiedProperties, "items"), "properties")
    );
    allProperties[propertyName as any] = value;
    setOverridenProperties(copiedProperties);
    setStepConfigs(stepConfigsCopy);
  }

  function validateForErrors() {
    const errorsCopy = cloneDeep(errors);
    stepConfigs?.configs.forEach((config: any) => {
      if (config.isEditable) {
        if (config.isRequired && isEmpty(config.changedValue)) {
          errorsCopy[config.name] = {
            message: t("errorMessage.config.required"),
            category: config.category,
          };
        } else {
          // @ts-ignore
          errorsCopy[config.name] = {
            //@ts-ignore
            message: getErrorValidator(config.displayType)(
              config.changedValue,
              config.name
            ),
            category: config.category,
          };
        }
      }
    });
    console.log("Setting errors as", errorsCopy);
    setErrors(errorsCopy);
  }
  function getErrorCountFor(category: string) {
    let errorsCount: number = 0;
    for (let error in errors) {
      if (errors[error].category === category && errors[error].message !== "") {
        errorsCount++;
      }
    }
    return errorsCount;
  }

  useEffect(() => {
    validateForErrors();
  }, [stepConfigs]);
  if (!stepConfigs || isEmpty(overridenProperties) || hostComponentsLoading) {
    return <Spinner />;
  }

  console.log("overridden props", serviceHostComponents);

  return (
    <>
      <div className="step-title">Review</div>
      <div className="step-description my-2">Confirm your host selections.</div>
      <Card>
        <Card.Body>
          <div className="bg-light-subtle border p-3">
            <Row>
              <Col md={4}>
                <b className="fw-bolder">Current ResourceManager:</b>
              </Col>
              <Col>{getMastersInfo().currentResourceManager}</Col>
            </Row>
            <Row className="mt-3">
              <Col md={4}>
                <b className="fw-bolder">Additional ResourceManager:</b>
              </Col>
              <Col>
                <Stack direction="horizontal">
                  {getMastersInfo().addResourceManager}
                  <Stack direction="horizontal" className="ms-2">
                    <FontAwesomeIcon icon={faPlus} className="text-success" />
                    <div className="text-success">TO BE INSTALLED</div>
                  </Stack>
                </Stack>
              </Col>
            </Row>
          </div>
          <Alert variant="info" className="mt-4">
            <Stack>
              <div className="fw-bolder fs-12">
                Review Configuration Changes.
              </div>
              <div className="fs-10 mt-2">
                The following lists the configuration changes that will be made
                by the Wizard to enable ResourceManager HA. This information is
                for <b>review only</b> and is not editable
              </div>
            </Stack>
          </Alert>
          <Accordion defaultActiveKey={selectedService as any} className="mt-4">
            {stepConfigs.configCategories?.map(
              (category: any, index: number) => {
                return (
                  <Accordion.Item eventKey={category.name} key={index}>
                    <Accordion.Header>
                      {category.displayName}
                      {getErrorCountFor(category.name) > 0 ? (
                        <Badge className="ms-2 bg-danger">
                          {getErrorCountFor(category.name)}
                        </Badge>
                      ) : null}
                    </Accordion.Header>
                    <Accordion.Body>
                      {filter(stepConfigs.configs, [
                        "category",
                        category.name,
                      ]).map((config: any) => {
                        return (
                          <Row
                            key={config.name}
                            className="mt-3 align-items-center"
                          >
                            <Col md={5}>
                              <small>{config.displayName}</small>
                            </Col>
                            <Col>
                              <Form.Control
                                type="text"
                                size="sm"
                                value={config.changedValue}
                                className={classNames({
                                  "border-danger":
                                    errors[config.name] &&
                                    errors[config.name]?.message !== "",
                                })}
                                disabled={!config.isEditable}
                                onChange={(e) => {
                                  handleValueChange(
                                    config.name,
                                    e.target.value
                                  );
                                }}
                              ></Form.Control>
                              {errors[config.name]?.message !== "" ? (
                                <div className="mt-1 text-danger">
                                  <small className="text-danger">
                                    {errors[config.name]?.message}
                                  </small>
                                </div>
                              ) : null}
                            </Col>
                          </Row>
                        );
                      })}
                    </Accordion.Body>
                  </Accordion.Item>
                );
              }
            )}
          </Accordion>
        </Card.Body>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={
          errors &&
          (Object.keys(errors).length === 0 ||
            Object.keys(errors)?.filter((error) => errors[error].message !== "")
              .length === 0)
        }
        onBack={() => {
          flushStateToDb("back");
          jumpToStep(2);
        }}
        onNext={() => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: {
                stepConfigs,
                overridenProperties,
              },
            },
          });
          getKDCSessionState(() => {
            flushStateToDb("next");
            handleNextImperitive();
          });
        }}
        onCancel={() => {
          flushStateToDb("cancel");
        }}
      />
    </>
  );
}

export default Step3;
