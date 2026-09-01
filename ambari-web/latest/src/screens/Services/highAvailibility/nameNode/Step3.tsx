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
  uniq,
} from "lodash";
import { EnableHighAvailibilityContext } from "./store/context";
import { getStepData } from "../../../../Utils/Utility";
import { enableNamenodeSteps } from "./wizardSteps";
import NnHaConfigInitializer from "../../../../Utils/configs";
import ha_properties from "../../../../data/configs/wizards/ha_properties";
import { HostsApi } from "../../../../api/hostsApi";
import Spinner from "../../../../components/Spinner";
import {
  Accordion,
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Form,
  Row,
  Stack,
} from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMinus, faPlus } from "@fortawesome/free-solid-svg-icons";
import { configValidator } from "../../../../Utils/validators";
import { t } from "i18next";
import classNames from "classnames";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./store/types";
import useHDFSConfigsTags from "../../../../hooks/useConfigsTags";
import { updateReviewedConfigValue } from "../haWorkflowUtils";
//@ts-ignore
interface ConfigResponse {
  Clusters: {
    desired_configs: {
      [key: string]: {
        tag: string;
      };
    };
  };
}

function Step3() {
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: {
      currentStep,
      handleNextImperitive,
      handleBackImperitive,
    },
  } = useContext(EnableHighAvailibilityContext);
  const { clusterName, services } = useContext(AppContext);
  const serverConfigDataRef = useRef<any>([]);
  const configTagsLoaded = useRef(false);
  const [clusterHostComponentsMapping, setClusterHostComponentsMapping] =
    useState<any>([]);
  const [stepConfigs, setStepConfigs] = useState<any>(null);
  const [selectedService, setSelectedService] = useState({});
  const [errors, setErrors] = useState<any>({});
  const {
    configsData,
    configsError,
    isConfigsLoading,
    reloadConfigs,
  } = useHDFSConfigsTags();
  const [configBuildError, setConfigBuildError] = useState("");
  const [overridenProperties, setOverridenProperties] = useState({});
  const configsToRemove: any = {
    "hdfs-site": [
      "dfs.namenode.secondary.http-address",
      "dfs.namenode.rpc-address",
      "dfs.namenode.http-address",
      "dfs.namenode.https-address",
    ],
  };

  async function getClusterComponents() {
    const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name,host_components/HostRoles/state&minimal_response=true`;
    const data = await HostsApi.getClusterComponents(clusterName, fields);
    setClusterHostComponentsMapping(data.items);
    return data.items;
  }

  const selectedServices = map(services, "ServiceInfo.service_name");

  function _prepareDependencies() {
    const ret: any = {};
    const configsFromServer = serverConfigDataRef.current.items;
    ret.namespaceId = getStepData(
      state,
      enableNamenodeSteps.GET_STARTED,
      "nameserviceId",
      "enableHighAvailibilitySteps"
    );
    ret.serverConfigs = configsFromServer;
    const hdfsConfigs = find(configsFromServer, [
      "type",
      "hdfs-site",
    ]).properties;
    const zkConfigs = find(configsFromServer, ["type", "zoo.cfg"]).properties;
    const dfsHttpA = hdfsConfigs["dfs.namenode.http-address"];
    ret.nnHttpPort = dfsHttpA ? dfsHttpA.split(":")[1] : 50070;

    const dfsHttpsA = hdfsConfigs["dfs.namenode.https-address"];
    ret.nnHttpsPort = dfsHttpsA ? dfsHttpsA.split(":")[1] : 50470;

    const dfsRpcA = hdfsConfigs["dfs.namenode.rpc-address"];
    ret.nnRpcPort = dfsRpcA ? dfsRpcA.split(":")[1] : 8020;

    ret.zkClientPort = zkConfigs["clientPort"] ? zkConfigs["clientPort"] : 2181;

    return ret;
  }

  function tweakServiceConfigs(configs: any) {
    console.log("Initial Configs", configs);
    const localDB = cloneDeep(
      getStepData(
        state,
        "SELECT_HOSTS",
        "",
        "enableHighAvailibilitySteps",
      ),
    );
    const allHostComponents = flatten(
      map(clusterHostComponentsMapping, "host_components")
    );
    const allHosts = map(allHostComponents, "HostRoles.host_name");
    const uniqueHosts = uniq(allHosts);
    const groupings: any = {};
    for (let host of uniqueHosts) {
      groupings[host] = {
        name: host,
        bootStatus: "REGISTERED",
        isInstalled: true,
        hostComponents: filter(allHostComponents, [
          "HostRoles.host_name",
          host,
        ]),
      };
    }
    localDB.hosts = groupings;
    localDB.installedServices = selectedServices;
    for (let masterComponent of localDB.masterComponentHosts) {
      if (!has(masterComponent, "serviceId") || !masterComponent.serviceId) {
        //@ts-ignore
        masterComponent.serviceId = "HDFS";
      }
      if (!has(masterComponent, "hostName") || !masterComponent.hostName) {
        masterComponent.hostName = masterComponent.selectedHost;
        masterComponent.component = masterComponent.component_name;
      }
    }
    const dependencies = _prepareDependencies();
    configs.forEach(function (config: any) {
      NnHaConfigInitializer.prototype.initialValue(
        config,
        localDB,
        dependencies
      );
      config.isOverridable = false;
    });
  }
  function removeConfigs(configs: any) {
    Object.keys(configsToRemove).forEach(function (site) {
      const siteConfigs = find(configs.items, ["type", site]);
      if (siteConfigs) {
        configsToRemove[site].forEach(function (property: any) {
          delete siteConfigs.properties[property];
        });
      }
    });
    return configs;
  }

  function loadComponentConfigs(_componentConfig: any, componentConfig: any) {
    _componentConfig.configs.forEach(function (_serviceConfigProperty: any) {
      componentConfig.configs.push({
        ..._serviceConfigProperty,
        isEditable: _serviceConfigProperty.isReconfigurable,
      });
    });
  }

  function renderServiceConfigs(_serviceConfig: any) {
    const serviceConfig = {
      serviceName: _serviceConfig.serviceName,
      displayName: _serviceConfig.displayName,
      configCategories: [],
      showConfig: true,
      configs: [],
    };
    _serviceConfig.configCategories.forEach(function (_configCategory: any) {
      if (selectedServices.includes(_configCategory.name)) {
        serviceConfig.configCategories.push(_configCategory as never);
      }
    });
    loadComponentConfigs(_serviceConfig, serviceConfig);
    serviceConfig.configs.map((con: any) => (con.changedValue = con.value));
    setStepConfigs(serviceConfig as any);
    //@ts-ignore
    setSelectedService(serviceConfig?.configCategories?.[0]?.name);
  }
  async function loadConfigsTags() {
    setConfigBuildError("");
    try {
      const serverConfigs = removeConfigs(cloneDeep(configsData));
      serverConfigDataRef.current = serverConfigs;
      const haConfig = cloneDeep(ha_properties.haConfig);
      tweakServiceConfigs(haConfig.configs);
      const overridenPropertiesCopy = cloneDeep(serverConfigs);
      for (const siteConfig of haConfig.configs) {
        const site = get(siteConfig, "filename", "");
        const correspondingSite = find(overridenPropertiesCopy.items, [
          "type",
          site,
        ]);
        if (correspondingSite) {
          correspondingSite.properties = {
            ...correspondingSite.properties,
            ...{
              //@ts-ignore
              [siteConfig.name]: siteConfig.changedValue || siteConfig.value,
            },
          };
        }
      }
      setOverridenProperties(overridenPropertiesCopy);
      renderServiceConfigs(haConfig);
    } catch (configErr) {
      console.error("Error loading config tags", configErr);
      setConfigBuildError(
        configErr instanceof Error
          ? configErr.message
          : "Ambari could not prepare the NameNode HA configuration review.",
      );
    }
  }
  useEffect(() => {
    if (
      !configTagsLoaded.current &&
      clusterHostComponentsMapping.length &&
      configsData &&
      !isEmpty(configsData)
    ) {
      configTagsLoaded.current = true;
      loadConfigsTags();
    }
  }, [clusterHostComponentsMapping, configsData]);
  useEffect(() => {
    async function makeApiCalls() {
      try {
        await getClusterComponents();
      } catch (error: any) {
        setConfigBuildError(
          error?.response?.data?.message ||
            error?.message ||
            "Ambari could not load the cluster component topology.",
        );
      }
    }
    makeApiCalls();
  }, []);
  function getMastersInfo() {
    const step2Data = cloneDeep(
      getStepData(
        state,
        "SELECT_HOSTS",
        "masterComponentHosts",
        "enableHighAvailibilitySteps",
      ),
    );
    for (let masterComponent of step2Data) {
      if (!has(masterComponent, "serviceId") || !masterComponent.serviceId) {
        //@ts-ignore
        masterComponent.serviceId = "HDFS";
      }
      if (!has(masterComponent, "hostName") || !masterComponent.hostName) {
        masterComponent.hostName = masterComponent.selectedHost;
        masterComponent.component = masterComponent.component_name;
      }
    }
    const currentNamenode = find(filter(step2Data, ["component", "NAMENODE"]), [
      "isInstalled",
      true,
    ]).hostName;
    const addNamenode = find(filter(step2Data, ["component", "NAMENODE"]), [
      "isInstalled",
      false,
    ]).hostName;
    const secondaryNamenode = find(step2Data, [
      "component",
      "SECONDARY_NAMENODE",
    ]).hostName;
    const journalNodes = map(
      filter(step2Data, ["component", "JOURNALNODE"]),
      "hostName"
    );
    return {
      currentNamenode,
      addNamenode,
      secondaryNamenode,
      journalNodes,
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
    const copiedProperties = updateReviewedConfigValue(
      overridenProperties as Parameters<typeof updateReviewedConfigValue>[0],
      property.filename,
      propertyName,
      value,
    );
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
  const reviewError = configsError || configBuildError;
  if (reviewError) {
    return (
      <>
        <div className="step-title">Review</div>
        <Alert variant="danger" className="mt-3">
          {reviewError}
          <Button
            className="ms-3"
            size="sm"
            onClick={() => {
              setConfigBuildError("");
              setStepConfigs(null);
              void reloadConfigs();
              void getClusterComponents().catch((error: any) => {
                setConfigBuildError(
                  error?.response?.data?.message ||
                    error?.message ||
                    "Ambari could not load the cluster component topology.",
                );
              });
            }}
          >
            Retry
          </Button>
        </Alert>
        <WizardFooter
          step={currentStep}
          isNextEnabled={false}
          onNext={() => undefined}
          onBack={async () => {
            await flushStateToDb("back");
            await handleBackImperitive();
          }}
          onCancel={() => void flushStateToDb("cancel")}
        />
      </>
    );
  }
  if (isConfigsLoading || !stepConfigs || isEmpty(overridenProperties)) {
    return <Spinner />;
  }
  return (
    <>
      <div className="step-title">Review</div>
      <div className="step-description my-2">Confirm your host selections.</div>
      <Card>
        <Card.Body>
          <div className="bg-light-subtle border p-3">
            <Row>
              <Col md={4}>
                <b className="fw-bolder">Current NameNode:</b>
              </Col>
              <Col>{getMastersInfo().currentNamenode}</Col>
            </Row>
            <Row className="mt-3">
              <Col md={4}>
                <b className="fw-bolder">Secondary NameNode:</b>
              </Col>
              <Col>
                <Stack direction="horizontal">
                  {getMastersInfo().secondaryNamenode}
                  <Stack direction="horizontal" className="ms-2">
                    <FontAwesomeIcon icon={faMinus} className="text-danger" />
                    <div className="text-danger">TO BE DELETED</div>
                  </Stack>
                </Stack>
              </Col>
            </Row>
            <Row className="mt-3">
              <Col md={4}>
                <b className="fw-bolder">Additional NameNode:</b>
              </Col>
              <Col>
                <Stack direction="horizontal">
                  {getMastersInfo().addNamenode}
                  <Stack direction="horizontal" className="ms-2">
                    <FontAwesomeIcon icon={faPlus} className="text-success" />
                    <div className="text-success">TO BE INSTALLED</div>
                  </Stack>
                </Stack>
              </Col>
            </Row>
            <Row className="mt-3">
              <Col md={4}>
                <b className="fw-bolder">JournalNode:</b>
              </Col>
              <Col>
                <Stack>
                  {getMastersInfo().journalNodes.map(
                    (journalNode: any, jNIndex: number) => (
                      <Stack
                        direction="horizontal"
                        key={journalNode}
                        className={jNIndex > 0 ? `mt-2` : ""}
                      >
                        {journalNode}
                        <Stack direction="horizontal" className="ms-2">
                          <FontAwesomeIcon
                            icon={faPlus}
                            className="text-success"
                          />
                          <div className="text-success">TO BE INSTALLED</div>
                        </Stack>
                      </Stack>
                    )
                  )}
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
                by the Wizard to enable NameNode HA. This information is for{" "}
                <span className="fw-bolder fs-10">review only</span> and is not
                editable except for the{" "}
                <span className="fw-bolder fs-10">
                  dfs.journalnode.edits.dir
                </span>{" "}
                property
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
        onBack={async () => {
          await flushStateToDb("back");
          await handleBackImperitive();
        }}
        onNext={async () => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: {
                stepConfigs,
                overridenProperties,
                removeConfigs,
                configsToRemove,
              },
            },
          });
          await flushStateToDb("next");
          await handleNextImperitive();
        }}
        onCancel={() => {
          void flushStateToDb("cancel");
        }}
      />
    </>
  );
}

export default Step3;
