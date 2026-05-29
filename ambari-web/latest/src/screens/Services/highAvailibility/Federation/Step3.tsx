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
import { cloneDeep, find, get, isEmpty, map } from "lodash";
import { EnableNamenodeFederationContext } from "./store/context";
import { getStepData } from "../../../../Utils/Utility";
import { enableNamenodeFederationSteps } from "./wizardSteps";
import Spinner from "../../../../components/Spinner";
import {
  Accordion,
  Alert,
  Badge,
  Card,
  CardBody,
  Col,
  Form,
  FormControl,
  Row,
} from "react-bootstrap";
import WizardFooter from "../../../../components/StepWizard/WizardFooter";
import { ActionTypes } from "./store/types";
import useConfigsTags from "../../../../hooks/useConfigsTags";
import ConfigsApi from "../../../../api/configsApi";
import { federationProperties } from "./federation_properties";
import { ServiceContext } from "../../../../store/ServiceContext";
import { t } from "i18next";

function Step3() {
  const {
    state,
    dispatch,
    stepWizardUtilities: { currentStep, handleNextImperitive },
    flushStateToDb,
  } = useContext(EnableNamenodeFederationContext);
  const { clusterName, services } = useContext(AppContext);
  const serverConfigDataRef = useRef<any>([]);
  // const [clusterHostComponentsMapping, setClusterHostComponentsMapping] =
  //   useState<any>([]);
  // const [stepConfigs, setStepConfigs] = useState<any>(null);
  const stepConfigs = useRef<any>(null);
  // const [selectedService, setSelectedService] = useState({});
  const [errors, setErrors] = useState<any>({});
  const { configsData } = useConfigsTags();
  const [overridenProperties, setOverridenProperties] = useState({});
  const configTagsLoaded = useRef(false);
  const [isNextEnabled, setIsNextEnabled] = useState(true);
  const configsToRemove: any = {
    "hdfs-site": ["dfs.namenode.shared.edits.dir", "dfs.journalnode.edits.dir"],
  };

  const { serviceModels } = useContext(ServiceContext);

  const selectedServices = map(services, "ServiceInfo.service_name");

  function prepareDependencies() {
    let ret: any = {};
    const configsFromServer = serverConfigDataRef.current.items;

    const journalNodes = serviceModels["hdfs"]?.slaveComponents
      .filter(
        (slaveComp: any) =>
          slaveComp.componentName === "JOURNALNODE" &&
          slaveComp.hostComponents &&
          slaveComp.hostComponents.length > 0
      )
      .flatMap((hc: any) => hc.hostComponents);

    // Get nameNodes from step data
    const nameNodes = getStepData(
      state,
      "SELECT_HOSTS",
      "masterComponentHosts",
      "enableNamenodeFederationSteps"
    ).filter((host: any) => host.component === "NAMENODE");


    // Get HDFS site configs
    const hdfsSiteConfigs =
      find(configsFromServer, ["type", "hdfs-site"])?.properties || {};

    // Get HDFS service
    //@ts-ignore
    const hdfsService = find(services, ["ServiceInfo.service_name", "HDFS"]);

    // Get new nameservice ID from step 1
    ret.newNameservice = getStepData(
      state,
      enableNamenodeFederationSteps.GET_STARTED,
      "nameserviceIds.newNameServiceId",
      "enableNamenodeFederationSteps"
    );

    // Get existing nameservice ID from step 1
    ret.existingNameservice = getStepData(
      state,
      enableNamenodeFederationSteps.GET_STARTED,
      "nameserviceIds.existingNameServiceId",
      "enableNamenodeFederationSteps"
    );

    ret.nameservice1 = ret.existingNameservice;

    // Get existing nameservices from HDFS service (matching ui version logic)
    const existingNameServices = hdfsService?.masterComponentGroups?.map((group: any) => group.name) || [ret.existingNameservice];
    
    ret.nameServicesList = existingNameServices.join(",");


    // Get namenode hostnames
    ret.namenode1 =
      hdfsSiteConfigs[
        `dfs.namenode.rpc-address.${ret.nameservice1}.nn1`
      ]?.split(":")[0];
    ret.namenode2 =
      hdfsSiteConfigs[
        `dfs.namenode.rpc-address.${ret.nameservice1}.nn2`
      ]?.split(":")[0];

    // Set up new namenode indices and hostnames
    ret.newNameNode1Index = `nn${nameNodes.length - 1}`;
    ret.newNameNode2Index = `nn${nameNodes.length}`;
    ret.newNameNode1 = nameNodes.filter(
      (node: any) => node.isInstalled === false
    )[0]?.hostName;
    ret.newNameNode2 = nameNodes.filter(
      (node: any) => node.isInstalled === false
    )[1]?.hostName;


    if (ret.newNameNode1 === undefined) {
      ret.newNameNode1 = "false";
    }

    if (ret.newNameNode2 === undefined) {
      ret.newNameNode2 = "false";
    }

    ret.journalnodes = journalNodes
      .map((jn: any) => `${jn.HostRoles?.host_name || jn.host_name}:8485`)
      .join(";");

    ret.clustername = clusterName;

    const dfsHttpA = hdfsSiteConfigs["dfs.namenode.http-address"];
    ret.nnHttpPort = dfsHttpA ? dfsHttpA.split(":")[1] : 50070;

    const dfsHttpsA = hdfsSiteConfigs["dfs.namenode.https-address"];
    ret.nnHttpsPort = dfsHttpsA ? dfsHttpsA.split(":")[1] : 50470;

    const dfsRpcA = hdfsSiteConfigs["dfs.namenode.rpc-address"];
    ret.nnRpcPort = dfsRpcA ? dfsRpcA.split(":")[1] : 8020;

    ret.journalnode_edits_dir = hdfsSiteConfigs["dfs.journalnode.edits.dir"];
    return ret;
  }

  function createRangerServiceProperty(
    nameservice: string,
    reponamePrefix: string,
    propertyName: string
  ) {
    return {
      name: propertyName,
      displayName: propertyName,
      isReconfigurable: false,
      recommendedValue: reponamePrefix + nameservice,
      value: reponamePrefix + nameservice,
      category: "RANGER",
      filename: "ranger-tagsync-site",
      serviceName: "MISC",
    };
  }

  function tweakServiceConfigs(configs: any) {
    const dependencies = prepareDependencies();

    // Handle special cases for Federation
    const nameServices = dependencies.nameServicesList.split(",");
    // Only add the new nameservice if it's not already in the list
    if (!nameServices.includes(dependencies.newNameservice)) {
      nameServices.push(dependencies.newNameservice);
    }

    const result: any[] = [];
    const configsToRemoveList: string[] = [];
    const hdfsSiteConfigs = find(serverConfigDataRef.current.items, [
      "type",
      "hdfs-site",
    ])?.properties;

    if (
      hdfsSiteConfigs &&
      !hdfsSiteConfigs[
        "dfs.namenode.servicerpc-address." + dependencies.nameservice1 + ".nn1"
      ] &&
      !hdfsSiteConfigs[
        "dfs.namenode.servicerpc-address." + dependencies.nameservice1 + ".nn2"
      ]
    ) {
      configsToRemoveList.push(
        "dfs.namenode.servicerpc-address.{{nameservice1}}.nn1",
        "dfs.namenode.servicerpc-address.{{nameservice1}}.nn2",
        "dfs.namenode.servicerpc-address.{{newNameservice}}.{{newNameNode1Index}}",
        "dfs.namenode.servicerpc-address.{{newNameservice}}.{{newNameNode2Index}}"
      );
    }

    // Handle Ranger service properties
    if (selectedServices.includes("RANGER")) {
      const hdfsRangerConfigs = find(
        serverConfigDataRef.current.items,
        (item: any) => item.type === "ranger-hdfs-security"
      )?.properties;
      const reponamePrefix =
        hdfsRangerConfigs &&
        hdfsRangerConfigs["ranger.plugin.hdfs.service.name"] === "{{repo_name}}"
          ? dependencies.clustername + "_hadoop_"
          : hdfsRangerConfigs
          ? hdfsRangerConfigs["ranger.plugin.hdfs.service.name"] + "_"
          : "";

      const coreSiteConfigs = find(
        serverConfigDataRef.current.items,
        (item: any) => item.type === "core-site"
      )?.properties;
      const defaultFSNS =
        coreSiteConfigs && coreSiteConfigs["fs.defaultFS"]
          ? coreSiteConfigs["fs.defaultFS"].split("hdfs://")[1]
          : "";

      nameServices.forEach((nameService: string) => {
        configs.push(
          createRangerServiceProperty(
            nameService,
            reponamePrefix,
            "ranger.tagsync.atlas.hdfs.instance." +
              dependencies.clustername +
              ".nameservice." +
              nameService +
              ".ranger.service"
          )
        );

        if (defaultFSNS) {
          configs.push(
            createRangerServiceProperty(
              defaultFSNS,
              reponamePrefix,
              "ranger.tagsync.atlas.hdfs.instance." +
                dependencies.clustername +
                ".ranger.service"
            )
          );
        }
      });
    }

    // Handle Accumulo service properties
    if (selectedServices.includes("ACCUMULO")) {
      const hdfsNameSpacesModel =
        find(services, ["ServiceInfo.service_name", "HDFS"])
          ?.masterComponentGroups || [];
      const newNameSpace = dependencies.newNameservice;

      const volumesValue = nameServices
        .map((ns: string) => {
          return "hdfs://" + ns + "/apps/accumulo/data";
        })
        .join(",");

      const replacementsValue = nameServices
        .map((ns: string) => {
          let hostName;
          if (ns === newNameSpace) {
            const hostNames = getStepData(
              state,
              enableNamenodeFederationSteps.SELECT_HOSTS,
              "masterComponentHosts",
              "enableNamenodeFederationSteps"
            )
              .filter(
                (hc: any) => hc.component === "NAMENODE" && !hc.isInstalled
              )
              .map((hc: any) => hc.hostName);
            hostName = hostNames[0];
          } else {
            const nameSpaceObject = find(
              hdfsNameSpacesModel,
              (item: any) => item.name === ns
            );
            hostName =
              nameSpaceObject && nameSpaceObject.hosts
                ? nameSpaceObject.hosts[0]
                : "";
          }
          return (
            "hdfs://" +
            hostName +
            ":8020/apps/accumulo/data hdfs://" +
            ns +
            "/apps/accumulo/data"
          );
        })
        .join(",");

      configs.push(
        {
          name: "instance.volumes",
          displayName: "instance.volumes",
          isReconfigurable: false,
          value: volumesValue,
          recommendedValue: volumesValue,
          category: "ACCUMULO",
          filename: "accumulo-site",
          serviceName: "MISC",
        },
        {
          name: "instance.volumes.replacements",
          displayName: "instance.volumes.replacements",
          isReconfigurable: false,
          value: replacementsValue,
          recommendedValue: replacementsValue,
          category: "ACCUMULO",
          filename: "accumulo-site",
          serviceName: "MISC",
        }
      );
    }

    // Process all configs
    configs.forEach((config: any) => {
      if (!configsToRemoveList.includes(config.name)) {
        config.isOverridable = false;

        // Replace dependencies in config properties
        config.name = config.name.replace(
          /\{\{(\w+)\}\}/g,
          (match: string, key: string) => {
            return dependencies[key] || match;
          }
        );

        config.displayName = config.displayName.replace(
          /\{\{(\w+)\}\}/g,
          (match: string, key: string) => {
            return dependencies[key] || match;
          }
        );

        config.value = config.value.replace(
          /\{\{(\w+)\}\}/g,
          (match: string, key: string) => {
            if (dependencies[key] !== undefined && dependencies[key] !== null) {
              return dependencies[key];
            }
            // Special handling for journalnode_edits_dir - use default if not found
            if (key === 'journalnode_edits_dir') {
              return '/hadoop/hdfs/journal';
            }
            return match;
          }
        );

        config.recommendedValue = config.recommendedValue.replace(
          /\{\{(\w+)\}\}/g,
          (match: string, key: string) => {
            if (dependencies[key] !== undefined && dependencies[key] !== null) {
              return dependencies[key];
            }
            // Special handling for journalnode_edits_dir - use default if not found
            if (key === 'journalnode_edits_dir') {
              return '/hadoop/hdfs/journal';
            }
            return match;
          }
        );

        result.push(config);
      }
    });

    return result;
  }

  function removeConfigs(configsToRemove: any, configs: any) {
    Object.keys(configsToRemove).forEach(function (site) {
      const siteConfigs = find(configs.items, ["type", site]);
      if (siteConfigs) {
        configsToRemove[site].forEach(function (property: string) {
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
    // setStepConfigs(serviceConfig as any);
    stepConfigs.current = serviceConfig;
    //@ts-ignore
    setSelectedService(serviceConfig?.configCategories?.[0]?.name);
  }

  const loadStep = async () => {
    await loadConfigsTags();
    validateForErrors();
  };

  const loadConfigsTags = async () => {
    try {
      const configsTagApiResponsedata = await ConfigsApi.loadConfigTags(
        clusterName
      );
      if (JSON.stringify(configsTagApiResponsedata) !== "{}") {
        await onLoadConfigsTags(configsTagApiResponsedata);
      }
    } catch (error) {
      console.error("Error loading config tags", error);
    }
  };

  async function onLoadConfigsTags(data: any) {
    // Get installed services names
    const servicesModel = selectedServices;
    let urlParams = `(type=hdfs-site&tag=${data.Clusters.desired_configs["hdfs-site"].tag})`;

    if (servicesModel.includes("RANGER")) {
      urlParams +=
        `|(type=core-site&tag=${data.Clusters.desired_configs["core-site"].tag})` +
        `|(type=ranger-tagsync-site&tag=${data.Clusters.desired_configs["ranger-tagsync-site"].tag})` +
        `|(type=ranger-hdfs-security&tag=${data.Clusters.desired_configs["ranger-hdfs-security"].tag})`;
    }

    // need to check if any issues here
    // Add Accumulo-related parameters if Accumulo is installed
    if (servicesModel.includes("ACCUMULO")) {
      urlParams += `|(type=accumulo-site&tag=${data.Clusters.desired_configs["accumulo-site"].tag})`;
    }

    try {
      const configsByTagApiResponseData = await ConfigsApi.getConfigsByTags(
        clusterName,
        urlParams
      );
      onLoadConfigs(configsByTagApiResponseData);
    } catch (error) {
      console.error("Error loading configurations", error);
    }
  }
  const onLoadConfigs = (configsData: any) => {
    serverConfigDataRef.current = configsData;
    removeConfigs(configsToRemove, configsData);
    let federationConfigProperties =
      federationProperties().federationConfig.configs;

    //TODO: Check if serviceModels field is needed

    // Filter out configs that should only be included on first run if NameNode Federation is already enabled
    //@ts-ignore
    const hasNameNodeFederation = services.some(
      (service: any) =>
        service.ServiceInfo?.service_name === "HDFS" &&
        service.masterComponentGroups &&
        service.masterComponentGroups.length > 1
    );

    // Filter out firstRun configs if NameNode Federation is already enabled (matching ui version logic)
    if (hasNameNodeFederation) {
      federationConfigProperties = federationConfigProperties.filter(
        (config: any) => !config.firstRun
      );
    }

    // Tweak service configs with dependencies
    const tweakedConfigs = tweakServiceConfigs(federationConfigProperties);
    federationConfigProperties = tweakedConfigs;

    // Prepare overridden properties
    const overridenPropertiesCopy = cloneDeep(configsData);
    for (const siteConfig of federationConfigProperties) {
      const site = get(siteConfig, "filename", "");
      const correspondingSite = find(
        overridenPropertiesCopy.items,
        (item: any) => item.type === site
      );
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

    // Create a modified federation config that uses the overridden properties
    const modifiedFederationConfig = {
      ...federationProperties().federationConfig,
      configs: federationConfigProperties,
    };

    renderServiceConfigs(modifiedFederationConfig);
  };

  useEffect(() => {
    if (
      // clusterHostComponentsMapping.length &&
      configsData &&
      !isEmpty(configsData)
    ) {
      configTagsLoaded.current = true;
      loadStep();
    }
  }, [configsData]);

  function getMastersInfo() {
    const step2Data = getStepData(
      state,
      "SELECT_HOSTS",
      "masterComponentHosts",
      "enableNamenodeFederationSteps"
    );

    const currentNameNodes = step2Data.filter(
      (host: any) => host.component === "NAMENODE" && host.isInstalled
    );

    const additionalNameNodes = step2Data.filter(
      (host: any) => host.component === "NAMENODE" && !host.isInstalled
    );

    return { currentNameNodes, additionalNameNodes };
  }

  function handleValueChange(propertyName: string, value: string) {
    const stepConfigsCopy: any = cloneDeep(stepConfigs.current);
    const property = find(stepConfigsCopy.configs, ["name", propertyName]);
    if (property) {
      property.changedValue = value;
      const copiedProperties = cloneDeep(overridenProperties);
      const items = get(copiedProperties, "items", []);
      for (const item of items) {
        if ((item as any).type === property.filename) {
          (item as any).properties[propertyName] = value;
          break;
        }
      }
      setOverridenProperties(copiedProperties);
      // setStepConfigs(stepConfigsCopy);
      stepConfigs.current = stepConfigsCopy;
      validateForErrors();
    }
  }

  function validateForErrors() {
    const errorsCopy = {}; // Start with a fresh object to clear all previous errors
    let hasErrors = false;

    stepConfigs.current?.configs.forEach((config: any) => {
      if (config.isEditable) {
        if (config.isRequired && isEmpty(config.changedValue)) {
          //@ts-ignore
          errorsCopy[config.name] = {
            message: t("errorMessage.config.required"),
            category: config.category,
          };
          hasErrors = true;
        } else if (
          config.name.includes("dfs.journalnode.edits.dir") &&
          !isEmpty(config.changedValue)
        ) {
          const value = config.changedValue;
          // Check if it starts with a slash or drive letter (e.g., C:)
          const startsWithSlashOrDrive = /^(\/|[a-zA-Z]:)/.test(value);
          // Check if it contains white spaces
          const containsWhiteSpace = /\s/.test(value);

          if (!startsWithSlashOrDrive || containsWhiteSpace) {
            //@ts-ignore
            errorsCopy[config.name] = {
              message:
                "Must be a slash or drive at the start, and must not contain white spaces",
              category: config.category,
            };
            hasErrors = true;
          }
        }
      }
    });

    setErrors(errorsCopy);
    setIsNextEnabled(!hasErrors);
  }

  const { currentNameNodes, additionalNameNodes } = getMastersInfo();

  if (!stepConfigs.current) {
    return (
      <div className="d-flex justify-content-center align-items-center p-5">
        <Spinner />
      </div>
    );
  }

  return (
    <>
      <h2 className="step-title">Review</h2>
      <h3 className="step-description light-text">
        Confirm your host selections.
      </h3>

      <Card className="mt-3">
        <CardBody>
          {currentNameNodes.map((node: any, index: number) => (
            <Row key={`current-${index}`} className="mb-2">
              <Col md={3} className="bolder">
                Current NameNode:
              </Col>
              <Col md={9}>{node.hostName}</Col>
            </Row>
          ))}

          {additionalNameNodes.map((node: any, index: number) => (
            <Row key={`additional-${index}`} className="mb-2">
              <Col md={3} className="bolder">
                Additional NameNode:
              </Col>
              <Col md={9}>
                {node.hostName ? node.hostName : "false"}{" "}
                <Badge bg="success">TO BE INSTALLED</Badge>
              </Col>
            </Row>
          ))}
        </CardBody>
      </Card>

      <Alert variant="info" className="mt-3">
        <div className="bolder mb-2">Review Configuration Changes.</div>
        <div>
          The following lists the configuration changes that will be made by the
          Wizard to enable NameNode Federation. This information is for{" "}
          <strong>review only</strong> and is not editable except for the{" "}
          <strong>dfs.journalnode.edits.dir</strong> properties.
        </div>
      </Alert>

      <Accordion defaultActiveKey="0" className="mt-3">
        {stepConfigs.current?.configCategories?.map((category: any, categoryIndex: number) => {
          const categoryConfigs = stepConfigs.current?.configs.filter((config: any) => config.category === category.name);
          if (!categoryConfigs?.length) return null;
          
          // Count errors in this category
          const categoryErrorCount = categoryConfigs.filter((config: any) => errors[config.name]?.message).length;
          
          return (
            <Accordion.Item eventKey={categoryIndex.toString()} key={categoryIndex}>
              <Accordion.Header>
                <div className="d-flex align-items-center">
                  <span className="me-2">{category.displayName}</span>
                  {categoryErrorCount > 0 && (
                    <Badge bg="danger" pill>
                      {categoryErrorCount}
                    </Badge>
                  )}
                </div>
              </Accordion.Header>
              <Accordion.Body>
                <Form>
                  {categoryConfigs.map((config: any, index: number) => (
                    <Form.Group as={Row} key={index} className="mb-3">
                      <Form.Label column sm={4} className="text-break pe-3" style={{ wordWrap: 'break-word', overflowWrap: 'break-word' }}>
                        {config.displayName}
                      </Form.Label>
                      <Col sm={8}>
                        <FormControl
                          type="text"
                          value={config.changedValue || ""}
                          onChange={(e) =>
                            handleValueChange(config.name, e.target.value)
                          }
                          disabled={!config.isEditable}
                          isInvalid={errors[config.name]?.message}
                        />
                        {errors[config.name]?.message && (
                          <Form.Control.Feedback type="invalid">
                            {errors[config.name].message}
                          </Form.Control.Feedback>
                        )}
                      </Col>
                    </Form.Group>
                  ))}
                </Form>
              </Accordion.Body>
            </Accordion.Item>
          );
        })}
      </Accordion>

      <WizardFooter
        step={currentStep}
        isNextEnabled={isNextEnabled}
        onBack={() => {
          flushStateToDb("back");
        }}
        onNext={() => {
          dispatch({
            type: ActionTypes.STORE_INFORMATION,
            payload: {
              step: currentStep.name,
              data: {
                overridenProperties,
              },
            },
          });
          flushStateToDb("next");
          handleNextImperitive();
        }}
      />
    </>
  );
}

export default Step3;
