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
import ConfigsApi from "../../../api/configsApi";
import { AppContext } from "../../../store/context";
import { useParams } from "react-router-dom";
import { cloneDeep, find, has, map, toString } from "lodash";
import {
  additionalConfigsMap,
  reassignSteps,
  relatedServicesMap,
  secureConfigsMap,
} from "./constants";
import { getStepData } from "../../../Utils/Utility";
import { ReassignContext } from "./store/context";
import { ActionTypes } from "./store/types";
import { ServiceContext } from "../../../store/ServiceContext";
import MoveNameNodeConfigInitializer from "../../../Initializers/MoveNameNodeConfigInitializer";
import MoveRmConfigInitializer from "../../../Initializers/MoveRmConfigInitializer";
import MoveHmConfigInitializer from "../../../Initializers/MoveHmConfigInitializer";
import MoveHsConfigInitializer from "../../../Initializers/MoveHsConfigInitializer";
import MoveWsConfigInitializer from "../../../Initializers/MoveWsConfigInitializer";
import MoveOSConfigInitializer from "../../../Initializers/MoveOSConfigInitializer";
import { groupPropertyValues } from "../../../Utils/dataUtils";
import {
  Accordion,
  Alert,
  Col,
  FormControl,
  Row,
  InputGroup,
  Card,
  Stack,
} from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faLock,
  faMinus,
  faPlus,
  faUndo,
} from "@fortawesome/free-solid-svg-icons";
import classNames from "classnames";
import WizardFooter from "../../../components/StepWizard/WizardFooter";
import { messages } from "../../messages";

function Step3() {
  // const [configTags, setConfigTags] = useState<any>([]);
  const { clusterName, services, isKerberosEnabled } = useContext(AppContext);
  const {
    state,
    dispatch,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  } = useContext(ReassignContext);
  const { allServiceModels } = useContext(ServiceContext);
  const isHAEnabled = allServiceModels?.["hdfs"]?.isNameNodeHaEnabled || false;
  const { componentName } = useParams();
  const propertiesToChangeRef = useRef<any>({});
  const configsRef = useRef<any>({});
  const [attributesState, setAttributes] = useState<any>({});
  const attributesRef = useRef<any>({});
  const [, setConfigs] = useState<any>({});
  const [displayedConfigsState, setDisplayedConfigs] = useState<any>([]);

  useEffect(() => {
    setConfigs(configsRef.current);
  }, [configsRef.current]);

  // Helper functions
  function getNnInitializerSettings(configs: any) {
    const ret: any = {};
    if (isHAEnabled) {
      const configsObject = configs["hdfs-site"];
      const nameSpaces = configsObject["dfs.nameservices"].split(",");
      const nameSpacesCount = nameSpaces.length;
      const propertyNames = Object.keys(configsObject);

      // Get both source and target hostnames for debugging
      const sourceHostName = find(
        assignMastersData,
        (master: any) =>
          master.component_name === componentName &&
          master.isMoving &&
          master.movedHost
      )?.hostName;

      //@ts-ignore
      const targetHostName = find(
        assignMastersData,
        (master: any) =>
          master.component_name === componentName &&
          master.isMoving &&
          master.movedHost
      )?.movedHost;

      for (let i = 0; i < nameSpacesCount; i++) {
        const nameSpace = nameSpaces[i];
        const propertyNameStart = `dfs.namenode.http-address.${nameSpace}.`;
        const httpAddressPropertiesNames = propertyNames.filter(
          (propertyName) => propertyName.startsWith(propertyNameStart)
        );

        // Find properties that contain the source hostname
        const matchingPropertyName = httpAddressPropertiesNames.find(
          (propertyName) => {
            const value = configsObject[propertyName];
            return value.includes(sourceHostName);
          }
        );

        if (matchingPropertyName) {
          // Use a more flexible regex pattern to extract the suffix
          // This will match any characters after the namespace part
          const nameNodeSuffixMatch = matchingPropertyName.match(
            new RegExp(`${propertyNameStart}(.+)$`)
          );

          ret.namespaceId = nameSpace;
          ret.suffix = nameNodeSuffixMatch && nameNodeSuffixMatch[1];

          break;
        }
      }

      // If we didn't find a match, try a different approach
      if (!ret.namespaceId || !ret.suffix) {
        // Look for any namenode property with the source hostname
        for (let i = 0; i < nameSpacesCount; i++) {
          const nameSpace = nameSpaces[i];

          // Check all namenode-related properties
          const nnPropertyPrefixes = [
            `dfs.namenode.http-address.${nameSpace}.`,
            `dfs.namenode.https-address.${nameSpace}.`,
            `dfs.namenode.rpc-address.${nameSpace}.`,
          ];

          for (const prefix of nnPropertyPrefixes) {
            const relatedProps = propertyNames.filter((name) =>
              name.startsWith(prefix)
            );

            for (const propName of relatedProps) {
              const value = configsObject[propName];
              if (value.includes(sourceHostName)) {
                const suffixMatch = propName.match(
                  new RegExp(`${prefix}(.+)$`)
                );
                if (suffixMatch) {
                  ret.namespaceId = nameSpace;
                  ret.suffix = suffixMatch[1];

                  break;
                }
              }
            }

            if (ret.namespaceId && ret.suffix) break;
          }

          if (ret.namespaceId && ret.suffix) break;
        }
      }
    }
    return ret;
  }

  function getRmInitializerSettings(configs: any) {
    const sourceHostName = find(
      assignMastersData,
      (master: any) =>
        master.component_name === componentName &&
        master.isMoving &&
        master.movedHost
    )?.hostName;

    return {
      suffix:
        configs["yarn-site"]["yarn.resourcemanager.hostname.rm1"] ===
        sourceHostName
          ? "rm1"
          : "rm2",
    };
  }

  function getRmAdditionalDependencies(configs: any) {
    const ret: any = {};
    const rm1 = configs["yarn-site"]["yarn.resourcemanager.hostname.rm1"];
    if (rm1) {
      ret.rm1 = rm1;
    }
    const rm2 = configs["yarn-site"]["yarn.resourcemanager.hostname.rm2"];
    if (rm2) {
      ret.rm2 = rm2;
    }
    return ret;
  }

  function getHiveInitializerSettings(configs: any) {
    return {
      hiveUser: configs["hive-env"]["hive_user"],
    };
  }

  function getWsInitializerSettings(configs: any) {
    return {
      webhcatUser: configs["hive-env"]["webhcat_user"],
    };
  }

  function getOsInitializerSettings(configs: any) {
    const ret: any = {};
    const cfg = configs["oozie-env"]["oozie_user"];
    if (cfg) {
      ret.oozieUser = cfg;
    }
    return ret;
  }

  function prepareTopologyDB() {
    return {
      masterComponentHosts: assignMastersData || [],
      installedServices: serviceNames,
      isHaEnabled: isHAEnabled,
    };
  }

  function prepareDependencies() {
    const sourceHostName = find(
      assignMastersData,
      (master: any) =>
        master.component_name === componentName &&
        master.isMoving &&
        master.movedHost
    )?.hostName;
    const targetHostName = find(
      assignMastersData,
      (master: any) =>
        master.component_name === componentName &&
        master.isMoving &&
        master.movedHost
    )?.movedHost;

    return {
      sourceHostName,
      targetHostName,
    };
  }

  function setDynamicConfigs(
    configs: any,
    initializer: any,
    additionalDependencies: any = {}
  ) {
    const topologyDB = prepareTopologyDB();
    const dependencies = {
      ...prepareDependencies(),
      ...additionalDependencies,
    };

    // const initializerObjects = initializer["initializers"];
    // const uniqueInitializerObjects = initializer["uniqueInitializers"];

    const initializerObjects = initializer.getInstance().initializers;
    const uniqueInitializerObjects =
      initializer.getInstance().uniqueInitializers;

    // Debug logging to see what's in the initializers

    Object.keys(configs).forEach((site) => {
      Object.keys(configs[site]).forEach((config) => {
        const cfg = {
          name: config,
          filename: site,
          value: configs[site][config],
        };

        const result = initializer.initialValue(cfg, topologyDB, dependencies);
        configs[site][config] = result.value;
        if (
          has(initializerObjects, config) ||
          has(uniqueInitializerObjects, config)
        ) {
          // Track properties that changed
          if (!propertiesToChangeRef.current.hasOwnProperty(site)) {
            propertiesToChangeRef.current[site] = [];
          }
          propertiesToChangeRef.current[site].push({
            name: config,
          });
        }
      });
    });

    return configs;
  }
  const serviceToConfigSiteMap: any = {
    NAMENODE: ["hdfs-site", "core-site"],
    SECONDARY_NAMENODE: ["hdfs-site", "core-site"],
    JOBTRACKER: ["mapred-site"],
    RESOURCEMANAGER: ["yarn-site"],
    WEBHCAT_SERVER: ["hive-env", "webhcat-site", "core-site"],
    APP_TIMELINE_SERVER: ["yarn-site", "yarn-env"],
    OOZIE_SERVER: ["oozie-site", "core-site", "oozie-env"],
    HIVE_SERVER: ["hive-site", "webhcat-site", "hive-env", "core-site"],
    HIVE_METASTORE: [
      "hive-site",
      "webhcat-site",
      "hive-env",
      "core-site",
      "hive-interactive-site",
    ],
    MYSQL_SERVER: ["hive-site"],
    HISTORYSERVER: ["mapred-site"],
    TIMELINE_READER: ["yarn-site"],
  };
  const assignMastersData = getStepData(
    state,
    reassignSteps.ASSIGN_MASTER,
    "masterComponentHosts",
    "reassignSteps"
  );

  const serviceNames = map(services, "ServiceInfo.service_name");
  const componentSpecificTypesMap: any = {
    NAMENODE: [
      {
        serviceName: "HBASE",
        configTypes: ["hbase-site"],
      },
      {
        serviceName: "ACCUMULO",
        configTypes: ["accumulo-site"],
      },
      {
        serviceName: "HAWQ",
        configTypes: ["hawq-site", "hdfs-client"],
      },
    ],
    RESOURCEMANAGER: [
      {
        serviceName: "HAWQ",
        configTypes: ["hawq-site", "yarn-client"],
      },
    ],
  };
  function getConfigUrlParams(data: any) {
    let urlParams: any = [];
    serviceToConfigSiteMap[componentName!]?.forEach((site: string) => {
      if (data.Clusters.desired_configs[site]) {
        urlParams.push(
          "(type=" +
            site +
            "&tag=" +
            data.Clusters.desired_configs[site].tag +
            ")"
        );
      }
    });
    var specificTypes = componentSpecificTypesMap[componentName!];
    if (specificTypes) {
      specificTypes.forEach(function (service: any) {
        if (serviceNames.includes(service.serviceName)) {
          service.configTypes.forEach(function (site: string) {
            urlParams.push(
              "(type=" +
                site +
                "&tag=" +
                data.Clusters.desired_configs[site].tag +
                ")"
            );
          });
        }
      });
    }
    return urlParams;
  }
  function setAdditionalConfigs(
    configs: any,
    componentName: string,
    replaceValue: string
  ) {
    propertiesToChangeRef.current = {};
    const component = find(additionalConfigsMap, [
      "componentName",
      componentName,
    ]);
    if (!component) {
      return false;
    }
    let additionalConfigs: any = component.configs_Hadoop2
      ? component.configs_Hadoop2
      : component.configs;
    for (let site in additionalConfigs) {
      if (additionalConfigs.hasOwnProperty(site)) {
        for (let property in additionalConfigs[site]) {
          if (additionalConfigs[site].hasOwnProperty(property)) {
            // Skip generic NameNode properties when HA is enabled - matches classic UI behavior
            if (
              isHAEnabled &&
              componentName === "NAMENODE" &&
              [
                "fs.defaultFS",
                "dfs.namenode.rpc-address",
                "dfs.namenode.http-address",
                "dfs.namenode.https-address",
              ].includes(property)
            ) {
              continue;
            }

            configs[site][property] = additionalConfigs[site][property].replace(
              "<replace-value>",
              replaceValue
            );
            if (!propertiesToChangeRef.current.hasOwnProperty(site)) {
              propertiesToChangeRef.current[site] = [];
            }
            propertiesToChangeRef.current[site].push({
              name: property,
            });
          }
        }
      }
    }
    return true;
  }
  function setSecureConfigs(
    secureConfigs: any,
    configs: any,
    componentName: string
  ) {
    var securityEnabled = isKerberosEnabled;
    var component = find(secureConfigsMap, ["componentName", componentName]);
    if (!component || !securityEnabled) return false;

    component.configs.forEach(function (config) {
      secureConfigs.push({
        keytab: configs[config.site][config.keytab],
        principal: configs[config.site][config.principal],
      });
      if (!propertiesToChangeRef.current.hasOwnProperty(config.site)) {
        propertiesToChangeRef.current[config.site] = [];
      }
      propertiesToChangeRef.current[config.site].push(
        {
          name: config.keytab,
          isSecure: true,
        },
        {
          name: config.principal,
          isSecure: true,
        }
      );
    });

    return true;
  }
  function renderServiceConfigs() {
    const configCategories: any[] = [];
    const displayedConfigs: any[] = [];

    // Create service config structure similar to original
    //@ts-ignore
    const serviceConfig = {
      serviceName: "MISC",
      configCategories: configCategories,
      showConfig: true,
      configs: displayedConfigs,
    };

    // Check if we have any properties to change
    if (Object.keys(propertiesToChangeRef.current).length === 0) {
      console.warn(
        "No properties to change found. This may indicate an issue with the configuration initialization."
      );

      // For NameNode in HA mode, we need to ensure we have some properties to display
      // if (componentName === "NAMENODE" && isHAEnabled && configsRef.current) {
      //   // Add some essential NameNode properties manually if they're not already there
      //   const hdfsProps = ["hdfs-site"];
      //   const targetHostName = find(
      //     assignMastersData,
      //     (master: any) => master.component_name === componentName && master.isMoving && master.movedHost
      //   )?.movedHost;

      //   if (targetHostName && configsRef.current["hdfs-site"]) {
      //     // Add some essential properties that should be displayed
      //     // propertiesToChangeRef.current["hdfs-site"] = [
      //     //   { name: "dfs.namenode.rpc-address" },
      //     //   { name: "dfs.namenode.http-address" },
      //     //   { name: "dfs.namenode.https-address" }
      //     // ];

      //     console.log("Added essential NameNode properties manually:", propertiesToChangeRef.current);
      //   }
      // }
    }

    // Process properties to change and create config objects
    Object.keys(propertiesToChangeRef.current).forEach((type) => {
      // Map config type to service name (simplified mapping)
      const serviceByConfigTypeMap: any = {
        "hdfs-site": { serviceName: "HDFS", displayName: "HDFS" },
        "core-site": { serviceName: "HDFS", displayName: "HDFS" },
        "yarn-site": { serviceName: "YARN", displayName: "YARN" },
        "hive-site": { serviceName: "HIVE", displayName: "Hive" },
        "hbase-site": { serviceName: "HBASE", displayName: "HBase" },
        "accumulo-site": { serviceName: "ACCUMULO", displayName: "Accumulo" },
        "hawq-site": { serviceName: "HAWQ", displayName: "HAWQ" },
        "oozie-site": { serviceName: "OOZIE", displayName: "Oozie" },
        "mapred-site": { serviceName: "MAPREDUCE2", displayName: "MapReduce2" },
      };

      const service = serviceByConfigTypeMap[type];
      if (service) {
        const serviceName = service.serviceName;

        // Add category if not exists
        if (!configCategories.find((cat) => cat.name === serviceName)) {
          configCategories.push({
            name: serviceName,
            displayName: service.displayName,
          });
        }

        // Process each property
        propertiesToChangeRef.current[type].forEach((property: any) => {
          const propertyName = property.name;
          const configValue = (configsRef.current as any)[type]
            ? (configsRef.current as any)[type][propertyName]
            : "";

          const displayedProperty = {
            name: propertyName,
            fileName: type,
            value: configValue,
            category: serviceName,
            changedValue: configValue,
            error: !configValue ? "This field is required" : undefined,
            displayName: getDisplayName(
              propertyName,
              propertyName,
              type,
              serviceName
            ),
            isEditable: !property.isSecure,
          };

          displayedConfigs.push(displayedProperty);
        });
      }
    });

    setDisplayedConfigs(displayedConfigs);
  }

  function getDisplayName(
    stackDisplayName: string,
    propertyName: string,
    type: string,
    serviceName: string
  ): string {
    let displayName = stackDisplayName || propertyName;
    const keys = Object.keys(propertiesToChangeRef.current);

    for (let i = 0; i < keys.length; i++) {
      const fileName = keys[i];
      const serviceByConfigTypeMap: any = {
        "hdfs-site": { serviceName: "HDFS" },
        "core-site": { serviceName: "HDFS" },
        "yarn-site": { serviceName: "YARN" },
        "hive-site": { serviceName: "HIVE" },
        "hbase-site": { serviceName: "HBASE" },
        "accumulo-site": { serviceName: "ACCUMULO" },
        "hawq-site": { serviceName: "HAWQ" },
        "oozie-site": { serviceName: "OOZIE" },
        "mapred-site": { serviceName: "MAPREDUCE2" },
      };

      const service = serviceByConfigTypeMap[fileName];
      if (fileName !== type && service && service.serviceName === serviceName) {
        const configs = propertiesToChangeRef.current[fileName];
        if (configs.find((config: any) => config.name === propertyName)) {
          displayName = `${type}/${propertyName}`;
          break;
        }
      }
    }
    return displayName;
  }

  function isPropertyFinal(fileName: string, propertyName: string): boolean {
    const attributes = cloneDeep(attributesState);
    if (
      attributes &&
      attributes[fileName] &&
      attributes[fileName]["changedFinal"]
    ) {
      return attributes[fileName]["changedFinal"][propertyName] === "true";
    }
    return false;
  }
  async function onLoadConfigs(data: any) {
    let hawqSiteIndex = -1;
    for (let i = 0; i < data.items.length; i++) {
      if (data.items[i].type == "hawq-site") {
        hawqSiteIndex = i;
        break;
      }
    }
    // if certain services are deployed, include related site files to additionalConfigsMap and relatedServicesMap.
    if (hawqSiteIndex >= 0) {
      // if HAWQ is deployed
      var hawqSiteProperties = {
        hawq_rm_yarn_address: "<replace-value>:8050",
        hawq_rm_yarn_scheduler_address: "<replace-value>:8030",
      };

      var rmComponent: any = find(additionalConfigsMap, [
        "componentName",
        "RESOURCEMANAGER",
      ]);
      rmComponent.configs["hawq-site"] = hawqSiteProperties;

      if (
        data.items[hawqSiteIndex].properties[
          "hawq_global_rm_type"
        ].toLowerCase() === "yarn"
      ) {
        relatedServicesMap["RESOURCEMANAGER"].append("HAWQ");
      }
    }

    const targetHostName = find(assignMastersData, (master: any) => {
      return (
        master.component_name === componentName &&
        master.isMoving &&
        master.movedHost
      );
    })?.movedHost;
    let configs: any = {};
    let attributes: any = {};
    let secureConfigs: any = [];
    data.items.forEach(function (item: any) {
      configs[item.type] = item.properties;
      if (item.properties_attributes) {
        attributes[item.type] = item.properties_attributes;
      }
    });
    setAdditionalConfigs(configs, componentName!, targetHostName);
    setSecureConfigs(secureConfigs, configs, componentName!);
    switch (componentName) {
      case "NAMENODE":
        const nnSettings = getNnInitializerSettings(configs);

        if (isHAEnabled && (!nnSettings.namespaceId || !nnSettings.suffix)) {
          console.error(
            "WARNING: Missing namespaceId or suffix for NameNode HA configuration!"
          );
          console.error(
            "This will cause config properties to not be properly updated."
          );
          // Add default values if they're missing to prevent errors
          if (!nnSettings.namespaceId) {
            const nameSpaces =
              configs["hdfs-site"]["dfs.nameservices"].split(",");
            if (nameSpaces.length > 0) {
              nnSettings.namespaceId = nameSpaces[0];
            }
          }
          if (!nnSettings.suffix) {
            nnSettings.suffix = "nn1";
          }
        }

        MoveNameNodeConfigInitializer.setup(nnSettings);
        configs = setDynamicConfigs(configs, MoveNameNodeConfigInitializer);
        MoveNameNodeConfigInitializer.cleanup();
        break;
      case "RESOURCEMANAGER":
        const rmSettings = getRmInitializerSettings(configs);
        const additionalDependencies = getRmAdditionalDependencies(configs);
        MoveRmConfigInitializer.setup(rmSettings);
        configs = setDynamicConfigs(
          configs,
          MoveRmConfigInitializer,
          additionalDependencies
        );
        MoveRmConfigInitializer.cleanup();
        break;
      case "HIVE_METASTORE":
        const hmSettings = getHiveInitializerSettings(configs);
        MoveHmConfigInitializer.setup(hmSettings);
        configs = setDynamicConfigs(configs, MoveHmConfigInitializer);
        MoveHmConfigInitializer.cleanup();
        break;
      case "HIVE_SERVER":
        const hsSettings = getHiveInitializerSettings(configs);
        MoveHsConfigInitializer.setup(hsSettings);
        configs = setDynamicConfigs(configs, MoveHsConfigInitializer);
        MoveHsConfigInitializer.cleanup();
        break;
      case "WEBHCAT_SERVER":
        const wsSettings = getWsInitializerSettings(configs);
        MoveWsConfigInitializer.setup(wsSettings);
        configs = setDynamicConfigs(configs, MoveWsConfigInitializer);
        MoveWsConfigInitializer.cleanup();
        break;
      case "OOZIE_SERVER":
        const osSettings = getOsInitializerSettings(configs);
        MoveOSConfigInitializer.setup(osSettings);
        configs = setDynamicConfigs(configs, MoveOSConfigInitializer);
        MoveOSConfigInitializer.cleanup();
        break;
    }
    configsRef.current = configs;
    Object.keys(attributes).forEach((key) => {
      attributes[key] = {
        ...attributes[key],
        changedFinal: attributes[key].final,
      };
    });
    attributesRef.current = attributes;
    setAttributes(attributes);
    renderServiceConfigs();
  }
  async function loadConfigTags() {
    try {
      const data = await ConfigsApi.loadConfigTags(clusterName);
      //onLoadConfigTags
      const urlParams = getConfigUrlParams(data);
      const reassignedConfigs = await ConfigsApi.reassignLoadConfigs(
        clusterName,
        urlParams.join("|")
      );
      onLoadConfigs(reassignedConfigs);
    } catch (err) {}
  }
  useEffect(() => {
    async function makeApiCalls() {
      await loadConfigTags();
    }
    makeApiCalls();
  }, []);

  function changeConfigValueFor(configName: string, value: string) {
    const displayedConfigsCopy = cloneDeep(displayedConfigsState);
    const config = displayedConfigsCopy.find(
      (config: any) => config.displayName === configName
    );
    if (config) {
      config.changedValue = value;
      if (!value) {
        config.error = "This field is required";
      } else {
        config.error = "";
      }
      setDisplayedConfigs([...displayedConfigsCopy]);
    }
  }

  function changeFinalStatusFor(
    fileName: string,
    finalStatus: string,
    propertyName: string
  ) {
    const attributesCopy = cloneDeep(attributesState);
    if (attributesCopy && attributesCopy[fileName]) {
      attributesCopy[fileName]["changedFinal"][propertyName] = finalStatus;
    }
    setAttributes(attributesCopy);
  }

  // Save configuration data to state for Step4
  function saveConfigDataToState() {
    // Update configs with user changes
    const updatedConfigs = cloneDeep(configsRef.current);
    const updatedAttributes = cloneDeep(attributesRef.current);
    let secureConfigs: any[] = [];

    // Apply user changes from displayed configs
    displayedConfigsState.forEach((config: any) => {
      if (updatedConfigs[config.fileName]) {
        updatedConfigs[config.fileName][config.name] = config.changedValue;
      }
    });

    // Collect secure configs if Kerberos is enabled
    if (isKerberosEnabled) {
      setSecureConfigs(secureConfigs, updatedConfigs, componentName!);
    }

    // Save to ReassignContext state using STORE_INFORMATION action
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: reassignSteps.REVIEW,
        data: {
          configs: updatedConfigs,
          configsAttributes: updatedAttributes,
          secureConfigs: secureConfigs,
          propertiesToChange: propertiesToChangeRef.current,
        },
      },
    });
  }

  return (
    <>
      <div className="step-title">Review</div>
      <div className="step-description mt-1">Confirm your host selections.</div>
      
      {(componentName === "HIVE_SERVER" || componentName === "HIVE_METASTORE") && (
        <Alert variant="warning" className="mt-3">
          <Alert.Heading>{messages['services.reassign.step3.hiveserver.database.setup.header']}</Alert.Heading>
          <p className="mb-2">
            {messages['services.reassign.step3.hiveserver.database.setup.message']}
          </p>
          <div className="bg-transparent p-1 rounded font-monospace text-dark">
            <strong>{messages['services.reassign.step3.hiveserver.database.setup.command']}</strong>
          </div>
          <p className="mt-2 mb-0">
            {messages['services.reassign.step3.hiveserver.database.setup.footer']}
          </p>
        </Alert>
      )}
      
      <Card className="mt-2">
        <Card.Body>
          <div className="bg-light-subtle border p-3">
            <Row>
              <Col md={4}>
                <b className="fw-bolder">Component Name:</b>
              </Col>
              <Col>{componentName}</Col>
            </Row>
            <Row className="mt-3">
              <Col md={4}>
                <b className="fw-bolder">Source Host:</b>
              </Col>
              <Col>
                <Stack direction="horizontal">
                  {
                    find(
                      assignMastersData,
                      (master: any) =>
                        master.component_name === componentName &&
                        master.isMoving &&
                        master.movedHost
                    )?.hostName
                  }
                  <Stack direction="horizontal" className="ms-2">
                    <FontAwesomeIcon icon={faMinus} className="text-danger" />
                    <div className="text-danger">TO BE DELETED</div>
                  </Stack>
                </Stack>
              </Col>
            </Row>
            <Row className="mt-3">
              <Col md={4}>
                <b className="fw-bolder">Target Host:</b>
              </Col>
              <Col>
                <Stack direction="horizontal">
                  {
                    find(
                      assignMastersData,
                      (master: any) =>
                        master.component_name === componentName &&
                        master.isMoving &&
                        master.movedHost
                    )?.movedHost
                  }

                  <Stack direction="horizontal" className="ms-2">
                    <FontAwesomeIcon icon={faPlus} className="text-success" />
                    <div className="text-success">TO BE INSTALLED</div>
                  </Stack>
                </Stack>
              </Col>
            </Row>
          </div>

          {/* Review Configuration Changes message - matches Ember template */}
          {displayedConfigsState.length > 0 && (
            <Alert variant="info" className="mt-4 p-3">
              <div className="fw-bold">Review Configuration Changes.</div>
              <div className="mt-2">
                The Wizard will make the following configuration changes.
              </div>
            </Alert>
          )}

          {Object.keys(groupPropertyValues(displayedConfigsState, "category"))
            ?.length > 0 ? (
            <Accordion
              alwaysOpen
              defaultActiveKey={Object.keys(
                groupPropertyValues(displayedConfigsState, "category")
              ).map((_, index) => {
                console.log("Setting default active key:", index.toString());
                return toString(index);
              })}
              className="mt-4"
            >
              {Object.keys(
                groupPropertyValues(displayedConfigsState, "category")
              ).map((category: string, index: number) => {
                return (
                  <Accordion.Item key={category} eventKey={index.toString()}>
                    <Accordion.Header>{category}</Accordion.Header>
                    <Accordion.Body>
                      {groupPropertyValues(displayedConfigsState, "category")[
                        category
                      ].map((config: any) => {
                        const isFinal = isPropertyFinal(
                          config.fileName,
                          config.name
                        );
                        return (
                          <Row
                            className="mt-4 align-items-center"
                            key={config.name}
                          >
                            <Col md={6}>{config.displayName}</Col>
                            <Col md={6}>
                              <div className="d-flex align-items-center">
                                <InputGroup size="sm">
                                  <FormControl
                                    onChange={(e: any) => {
                                      changeConfigValueFor(
                                        config.displayName,
                                        e.target.value
                                      );
                                    }}
                                    value={config.changedValue}
                                    disabled={!config.isEditable}
                                  />
                                  <InputGroup.Text
                                    className="cursor-pointer"
                                    onClick={() => {
                                      changeFinalStatusFor(
                                        config.fileName,
                                        isFinal ? "false" : "true",
                                        config.name
                                      );
                                    }}
                                  >
                                    <span
                                      style={{
                                        color: isFinal ? "#007bff" : "#6c757d",
                                      }}
                                      title={
                                        isFinal
                                          ? "This property is final (read-only)"
                                          : "This property is editable"
                                      }
                                    >
                                      <FontAwesomeIcon icon={faLock} />
                                    </span>
                                  </InputGroup.Text>
                                </InputGroup>
                                <FontAwesomeIcon
                                  onClick={() => {
                                    if (config.value !== config.changedValue) {
                                      changeConfigValueFor(
                                        config.displayName,
                                        config.value
                                      );
                                    }
                                  }}
                                  className={classNames("ms-2", {
                                    "opacity-25":
                                      config.value === config.changedValue,
                                    "cursor-pointer":
                                      config.value !== config.changedValue,
                                  })}
                                  icon={faUndo}
                                />
                              </div>
                              <div className="text-danger mt-1">
                                {config.error && config.error}
                              </div>
                            </Col>
                          </Row>
                        );
                      })}
                    </Accordion.Body>
                  </Accordion.Item>
                );
              })}
            </Accordion>
          ) : null}
        </Card.Body>
      </Card>
      <WizardFooter
        step={currentStep}
        isNextEnabled={
          displayedConfigsState.filter((config: any) => !config.error)
            .length === displayedConfigsState.length
        }
        onCancel={() => flushStateToDb("cancel")}
        onBack={() => {
          flushStateToDb("back");
          jumpToStep(2);
        }}
        onNext={() => {
          // Save configuration data to state before proceeding
          saveConfigDataToState();
          flushStateToDb("next");
          handleNextImperitive();
        }}
      />
    </>
  );
}

export default Step3;
