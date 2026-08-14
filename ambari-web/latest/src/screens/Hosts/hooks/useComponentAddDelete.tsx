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

import { get, set, compact, map, flatten, isArray, has } from "lodash";
import { useState, useRef, useContext, useEffect } from "react";
import { HostsApi } from "../../../api/hostsApi";
import { IHostComponent } from "../../../models/hostComponent";
import { AppContext } from "../../../store/context";
import {
  showErrorModal,
  zooKeeperRelatedServices,
  serviceMap,
} from "../../../Utils/Utility";
import { t } from "i18next";
import {
  getComponentDisplayName,
  getServiceByConfigTypeMap,
  installHostComponentCall,
} from "../utils";
import AddHiveComponentsInitializer from "../../../Initializers/AddHiveComponentsInitializer";
import AddZooKeeperComponentsInitializer from "../../../Initializers/AddZooKeeperComponentsInitializer";
import { ServiceContext } from "../../../store/ServiceContext";
import { IHost } from "../../../models/host";
import modalManager from "../../../store/ModalManager";
import RecommendationModal from "../../../components/RecommendationModal";

function useComponentAddDelete(
  clusterComponents: any,
  stackServices: any,
  getConfigByName: Function,
  setAllHostModels?: (
    data: IHost[] | ((prevModels: IHost[]) => IHost[])
  ) => void
) {
  const { clusterName, services } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const isNameNodeHAEnabled =
    allServiceModels["hdfs"]?.isNameNodeHaEnabled || false;
  const [configs, setConfigs] = useState({});
  // const [isReconfigureRequired, setIsReconfigureRequired] = useState(false);
  const isReconfigureRequired = useRef(false);

  const [isConfigsLoadingInProgress, setIsConfigsLoadingInProgress] =
    useState(false);
  //@ts-ignore
  const [allPropertiesToChangeState, setAllPropertiesToChangeState] = useState(
    []
  );

  const [recommendedPropertiesToChange, setRecommendedPropertiesToChange] =
    useState<any>([]);
  const recommendedPropertiesToChangeRef = useRef<any>(
    recommendedPropertiesToChange
  );
  const componentHere = useRef<IHostComponent | null>(null);
  const allPropertiesToChange: any = useRef([]);
  const requiredPropertiesToChange: any = useRef([]);
  const groupedPropertiesToChange: any = useRef([]);
  const _deletedHostComponentError = useRef<string>("");
  const componentProperties = useRef([]);

  useEffect(() => {
    setAllPropertiesToChangeState(allPropertiesToChange.current);
  }, [allPropertiesToChange.current]);

  const deleteAndReconfigureComponent = async (
    componentsMapItem: any,
    component: IHostComponent
  ) => {
    componentHere.current = component;
    if (componentsMapItem.deletePropertyName) {
      set(
        componentProperties.current,
        componentsMapItem.deletePropertyName,
        true
      );
    }
    await loadComponentRelatedConfigs(
      componentsMapItem.configTagsCallbackName,
      componentsMapItem.configsCallbackName
    );
    modalManager.show(
      <RecommendationModal
        isOpen={true}
        onClose={() => {
          modalManager.hide();
        }}
        componentDisplayName={getComponentDisplayName(component)}
        add={false}
        recommendedPropertiesToChange={recommendedPropertiesToChangeRef.current}
        selectRecommendedProperties={(newProperties: any) => {
          recommendedPropertiesToChangeRef.current = newProperties;
        }}
        //@ts-ignore
        setRecommendedPropertiesToChange={setRecommendedPropertiesToChange}
        callback={async (properties: any) => {
          setRecommendedPropertiesToChange(properties);
          try {
            await _doDeleteHostComponent(component);
            applyConfigsCustomization();
            await putConfigsToServer(
              groupedPropertiesToChange.current,
              get(component, "componentName")
            );
            clearConfigsChanges();
          } catch (error) {
            showErrorModal(get(error, "response.data.message", get(error, "message", "Unable to delete the host component.")));
          }
        }}
      />
    );
  };

  useEffect(() => {
    recommendedPropertiesToChangeRef.current = recommendedPropertiesToChange;
  }, [recommendedPropertiesToChange]);

  const addAndReconfigureComponent = async (
    componentsMapItem: any,
    hostName: string,
    component: any,
    data: any
  ) => {
    componentHere.current = component;
    set(componentProperties.current, "selectedHost", hostName);
    if (componentsMapItem.addPropertyName) {
      set(componentProperties.current, componentsMapItem.addPropertyName, true);
    }
    if (componentsMapItem.hostPropertyName) {
      set(
        componentProperties.current,
        componentsMapItem.hostPropertyName,
        hostName
      );
      set(component, "hostName", hostName);
    }

    await loadComponentRelatedConfigs(
      componentsMapItem.configTagsCallbackName,
      componentsMapItem.configsCallbackName
    );

    // The component metadata is authoritative when this hook is reused across services.
    const serviceName = get(component, "serviceName") || data.serviceName || "";
    const componentName =
      data.componentNameFromService || get(component, "componentName") || "";

    if (recommendedPropertiesToChangeRef.current.length === 0) {
      modalManager.show(
        <RecommendationModal
          isOpen={true}
          onClose={() => {
            modalManager.hide();
          }}
          componentDisplayName={getComponentDisplayName(component)}
          add={true}
          callback={() => {
            installHostComponentCall(
              hostName,
              component,
              data,
              setAllHostModels
            );
          }}
          fromService={
            data.fromServiceSummary ? data.fromServiceSummary : false
          }
          selectRecommendedProperties={(newProperties: any) => {
            recommendedPropertiesToChangeRef.current = newProperties;
          }}
          serviceName={serviceName}
          componentName={componentName}
          validDropDownHosts={
            data.validDropDownHosts ? data.validDropDownHosts : []
          }
          handleHostChange={async (selectedHost) => {
            modalManager.hide();
            component.hostName = selectedHost;
            await addAndReconfigureComponent(
              componentsMapItem,
              selectedHost,
              component,
              data
            );
          }}
          selectedHostForDropDown={hostName}
        />
      );
    } else {
      modalManager.show(
        <RecommendationModal
          isOpen
          onClose={() => {
            modalManager.hide();
          }}
          componentDisplayName={getComponentDisplayName(component)}
          add
          recommendedPropertiesToChange={
            recommendedPropertiesToChangeRef.current
          }
          selectRecommendedProperties={(newProperties: any) => {
            recommendedPropertiesToChangeRef.current = newProperties;
          }}
          callback={(selectedProperties: any) => {
            setRecommendedPropertiesToChange(selectedProperties);
            installAndReconfigureComponent(
              hostName,
              component,
              componentsMapItem,
              data
            );
          }}
          fromService={
            data.fromServiceSummary ? data.fromServiceSummary : false
          }
          serviceName={serviceName}
          componentName={componentName}
          validDropDownHosts={
            data.validDropDownHosts ? data.validDropDownHosts : []
          }
          handleHostChange={async (selectedHost) => {
            modalManager.hide();
            addAndReconfigureComponent(
              componentsMapItem,
              selectedHost,
              component,
              data
            );
          }}
          selectedHostForDropDown={hostName}
        />
      );
    }
  };

  const installAndReconfigureComponent = async (
    hostName: string,
    component: any,
    componentsMapItem: any,
    data: any
  ) => {
    applyConfigsCustomization();
    await installHostComponentCall(hostName, component, data, setAllHostModels);
    await putConfigsToServer(
      groupedPropertiesToChange.current,
      get(component, "componentName")
    );
    set(componentProperties.current, componentsMapItem.addPropertyName, false);
    clearConfigsChanges(true);
  };

  const putConfigsToServer = async (
    groups: any,
    componentName: any
  ): Promise<void> => {
    const requests: Promise<any>[] = [];
    if (isArray(groups)) {
      groups = flatten(groups);
    }
    if (groups.length) {
      groups.forEach((group: any) => {
        const desiredConfigs = [];
        const properties = group.properties;
        for (let site in properties) {
          if (!properties.hasOwnProperty(site) || !properties[site]) continue;
          desiredConfigs.push({
            type: site,
            properties: properties[site],
            properties_attributes: group.properties_attributes[site],
            service_config_version_note: t(
              "hosts.host.configs.save.note"
            ).replace("{0}", componentName),
          });
        }
        if (desiredConfigs.length > 0) {
          const data = {
            desired_config: desiredConfigs,
            componentName: componentName,
          };
          requests.push(
            HostsApi.commonServiceConfigurations(clusterName, data)
          );
        }
      });
    }
    await Promise.all(requests);
  };

  const applyConfigsCustomization = () => {
    recommendedPropertiesToChangeRef.current.forEach((property: any) => {
      const value = property.saveRecommended
        ? property.recommendedValue
        : property.initialValue;
      const filename = property.propertyFileName;
      if (isArray(groupedPropertiesToChange.current)) {
        groupedPropertiesToChange.current = flatten(
          groupedPropertiesToChange.current
        );
      }
      if (groupedPropertiesToChange.current.length) {
        var group = groupedPropertiesToChange.current.find(
          (item: { properties: Record<string, any> }) => {
            return item.properties && has(item.properties, filename);
          }
        );
        if (group) {
          if (!group.properties[filename]) {
            group.properties[filename] = {};
          }
          group.properties[filename][property.propertyName] = value;
        }
      }
    });
  };

  const _doDeleteHostComponent = async (
    component: IHostComponent,
    deleteComponentSuccessCallback?: (componentName: string, hostName: string) => void | Promise<void>,
    callback?: () => void | Promise<void>
  ) => {
    const componentName = get(component, "componentName");
    const hostName = get(component, "hostName");
    try {
      if (!componentName) {
        throw new Error("A component name is required for component deletion.");
      }
      await HostsApi.deleteHostComponent(clusterName, hostName, componentName);
      if (setAllHostModels) {
        setAllHostModels((prevModels: IHost[]) => {
          return prevModels.map((host) => {
            if (host.hostName === hostName) {
              const updatedHost = Object.create(Object.getPrototypeOf(host));
              Object.assign(updatedHost, {
                ...host,
                hostComponents: host.hostComponents.filter(
                  (hostComponent) =>
                    hostComponent.componentName !== componentName
                ),
              });
              return updatedHost as IHost;
            }
            return host;
          });
        });
      }
      if (deleteComponentSuccessCallback) {
        await deleteComponentSuccessCallback(componentName, hostName);
      }

      if (callback) {
        await callback();
      }
      _deletedHostComponentError.current = "";
    } catch (err) {
      _deletedHostComponentError.current = JSON.stringify(err);
      throw err;
    }
  };

  const loadZookeeperConfigs = async (data: any, _opt: any, params: any) => {
    const urlParams = constructZookeeperConfigUrlParams(data).join("|");
    if (urlParams.length > 0) {
      const response = await HostsApi.reAssignLoadConfigs(
        clusterName,
        urlParams
      );
      params.callback(response);
    }
  };

  const saveZkConfigs = (data: any) => {
    let configs: any = {};
    let attributes: any = {};
    saveLoadedConfigs(data);
    data.items.forEach((item: any) => {
      configs[item.type] = item.properties;
      attributes[item.type] = item.properties_attributes || {};
    });
    updateZkConfigs(configs);
    var groups: any = [];
    var serviceNames = map(services, "ServiceInfo.service_name");
    var zookeeperRelatedServices = zooKeeperRelatedServices.slice(0);
    if (isNameNodeHAEnabled) {
      zookeeperRelatedServices.push({
        serviceName: "HDFS",
        typesToLoad: ["core-site"],
        typesToSave: ["core-site"],
      });
    }
    zookeeperRelatedServices.forEach((service: any) => {
      if (serviceNames.includes(service.serviceName)) {
        var group: any = {
          properties: {},
          properties_attributes: {},
        };

        service.typesToSave.forEach((type: string) => {
          if (configs[type]) {
            group.properties[type] = configs[type];
            group.properties_attributes[type] = attributes[type];
          }
        });
        groups.push(group);
      }
    });
    setConfigsChanges(groups);
  };

  const loadRangerConfigs = async (data: any, _opt: any, params: any) => {
    const urlParams = getUrlParamsForConfigsRequest(data, [
      "core-site",
      "hdfs-site",
      "kms-env",
      "kms-site",
    ]);
    const response = await HostsApi.adminGetAllConfigurations(
      clusterName,
      urlParams
    );
    params.callback(response);
  };

  const onLoadRangerConfigs = (data: any) => {
    const hdfsProperties = [
        {
          type: "core-site",
          name: "hadoop.security.key.provider.path",
        },
        {
          type: "hdfs-site",
          name: "dfs.encryption.key.provider.uri",
        },
      ],
      kmsSiteProperties = [
        {
          name: "hadoop.kms.cache.enable",
          notHaValue: "true",
          haValue: "false",
        },
        {
          name: "hadoop.kms.authentication.zk-dt-secret-manager.enable",
          notHaValue: "false",
          haValue: "true",
        },
        {
          name: "hadoop.kms.cache.timeout.ms",
          notHaValue: "600000",
          haValue: "0",
        },
        {
          name: "hadoop.kms.current.key.cache.timeout.ms",
          notHaValue: "30000",
          haValue: "0",
        },
        {
          name: "hadoop.kms.authentication.signer.secret.provider",
          notHaValue: "random",
          haValue: "zookeeper",
        },
        {
          name: "hadoop.kms.authentication.signer.secret.provider.zookeeper.auth.type",
          notHaValue: "kerberos",
          haValue: "none",
        },
        {
          name: "hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string",
          notHaValue: "#HOSTNAME#:#PORT#,...",
          haValue: getZookeeperConnectionString(),
        },
      ],
      rkmsHosts = getRangerKMSServerHosts(),
      rkmsHostsStr = rkmsHosts.join(";"),
      isHA = rkmsHosts.length > 1,
      rkmsPort = data.items.find(
        (item: { type: string; properties: Record<string, any> }) =>
          item.type === "kms-env"
      )?.properties["kms_port"],
      newValue = "kms://http@" + rkmsHostsStr + ":" + rkmsPort + "/kms",
      coreSiteConfigs = data.items.find(
        (item: {
          type: string;
          properties: Record<string, any>;
          properties_attributes?: Record<string, any>;
        }) => item.type === "core-site"
      ),
      hdfsSiteConfigs = data.items.find(
        (item: { type: string }) => item.type === "hdfs-site"
      ),
      kmsSiteConfigs = data.items.find(
        (item: { type: string }) => item.type === "kms-site"
      ),
      groups = [
        {
          properties: {
            "core-site": coreSiteConfigs.properties,
            "hdfs-site": hdfsSiteConfigs.properties,
          },
          properties_attributes: {
            "core-site": coreSiteConfigs.properties_attributes,
            "hdfs-site": hdfsSiteConfigs.properties_attributes,
          },
        },
        {
          properties: {
            "kms-site": kmsSiteConfigs.properties,
          },
          properties_attributes: {
            "kms-site": kmsSiteConfigs.properties_attributes,
          },
        },
      ],
      propertiesToChange = allPropertiesToChange.current;

    saveLoadedConfigs(data);
    hdfsProperties.forEach((property) => {
      const typeConfigs = data.items.find(
          (item: { type: string; properties: Record<string, any> }) =>
            item.type === property.type
        )?.properties,
        currentValue = typeConfigs[property.name],
        pattern = new RegExp("^kms:\\/\\/http@(.+):" + rkmsPort + "\\/kms$"),
        patternMatch = currentValue && currentValue.match(pattern),
        currentHostsList =
          patternMatch && patternMatch[1].split(";").sort().join(";");
      if (currentHostsList !== rkmsHostsStr) {
        typeConfigs[property.name] = newValue;
        if (isReconfigureRequired.current) {
          const propertyFileName = property.type,
            propertyName = property.name,
            service =
              getServiceByConfigTypeMap(stackServices)[propertyFileName],
            configObject = getConfigByName(propertyName, propertyFileName);
          const displayName = configObject && configObject.displayName;

          // Ensure service display name is populated - fallback to serviceMap if needed
          let serviceDisplayName =
            service && service.StackServices?.display_name;
          if (!serviceDisplayName) {
            const serviceNameMapping: Record<string, string> = {
              "hive-site": "Hive",
              "webhcat-site": "Hive",
              "hive-env": "Hive",
              "yarn-site": "YARN",
              "hbase-site": "HBase",
              "accumulo-site": "Accumulo",
              "kafka-broker": "Kafka",
              "application-properties": "Atlas",
              "infra-solr-env": "Ambari Infra Solr",
              "storm-site": "Storm",
              "core-site": "HDFS",
              "hdfs-site": "HDFS",
              "kms-site": "Ranger KMS",
              "kms-env": "Ranger KMS",
              "zoo.cfg": "ZooKeeper",
            };
            serviceDisplayName =
              serviceNameMapping[propertyFileName] || propertyFileName;
          }

          propertiesToChange.push({
            propertyFileName,
            propertyName,
            propertyTitle: configObject && `Service Config: ${displayName}`,
            propertyDescription: configObject && configObject.description,
            serviceDisplayName: serviceDisplayName,
            initialValue: currentValue,
            recommendedValue: newValue,
            saveRecommended: true,
          });
        }
      }
    });

    kmsSiteProperties.forEach((property) => {
      const currentValue = kmsSiteConfigs.properties[property.name];
      const newValue = isHA ? property.haValue : property.notHaValue;
      kmsSiteConfigs.properties[property.name] = newValue;

      propertiesToChange.push({
        propertyFileName: "kms-site",
        propertyName: property.name,
        serviceDisplayName: serviceMap["RANGER_KMS"],
        initialValue: currentValue,
        recommendedValue: newValue,
        saveRecommended: true,
      });
    });

    allPropertiesToChange.current = propertiesToChange;
    setConfigsChanges(groups);
  };

  const setConfigsChanges = (groups: any) => {
    groupedPropertiesToChange.current.push(groups);
    if (allPropertiesToChange.current.length) {
      setConfigsChangesForDisplay();
    } else {
      setIsConfigsLoadingInProgress(false);
    }
  };

  const setConfigsChangesForDisplay = () => {
    allPropertiesToChange.current.forEach((property: any) => {
      const stackProperty = getConfigByName(
        property.propertyName,
        property.propertyFileName
      );
      if (
        stackProperty &&
        (!stackProperty.isEditable || !stackProperty.isReconfigurable)
      ) {
        requiredPropertiesToChange.current.push(property);
      } else {
        set(property, "saveRecommeded", true);
        set(property, "saveRecommended", true);
        recommendedPropertiesToChangeRef.current.push(property);
        setRecommendedPropertiesToChange(
          recommendedPropertiesToChangeRef.current
        );
      }
    });
    setIsConfigsLoadingInProgress(false);
  };

  const loadHiveConfigs = async (data: any, _opt: any, params: any) => {
    const urlParams = getUrlParamsForConfigsRequest(data, [
      "hive-site",
      "webcat-site",
      "hive-env",
      "core-site",
    ]);
    const response = await HostsApi.adminGetAllConfigurations(
      clusterName,
      urlParams
    );
    return params.callback(response);
  };

  const onLoadHiveConfigs = (data: any, _opt: any, _params: any) => {
    let port = "";
    let configs: any = {};
    let attributes: any = {};
    let userSetup: any = {};
    let localDB: any = {
      masterComponentHosts: getHiveHosts(),
    };
    let dependencies: any = {
      hiveMetastorePort: "",
    };
    let initializer = new AddHiveComponentsInitializer();
    saveLoadedConfigs(data);
    data.items.forEach((item: any) => {
      configs[item.type] = item.properties;
      attributes[item.type] = item.properties_attributes || {};
    });

    var propertiesToChange = allPropertiesToChange.current;

    port = configs["hive-site"]["hive.metastore.uris"].match(/:[0-9]{2,4}/);
    port = port ? port[0].slice(1) : "9083";
    dependencies.hiveMetastorePort = port;
    userSetup.hiveUser = configs["hive-env"]["hive_user"];
    // @ts-ignore
    initializer.setup(userSetup.hiveUser);

    ["hive-site", "webhcat-site", "hive-env", "core-site"].forEach(
      (fileName) => {
        if (configs[fileName]) {
          Object.keys(configs[fileName]).forEach((propertyName) => {
            const currentValue = configs[fileName][propertyName];
            const propertyDef = {
              name: propertyName,
              value: currentValue,
              filename: fileName,
            };
            const configProperty = initializer.initialValue(
              propertyDef,
              localDB,
              dependencies
            );
            initializer.updateSiteObj(configs[fileName], configProperty);
            if (
              isReconfigureRequired.current &&
              currentValue !== configs[fileName][propertyName]
            ) {
              const service =
                getServiceByConfigTypeMap(stackServices)[fileName];
              const configObject = getConfigByName(propertyName, fileName);
              const displayName = configObject && configObject.displayName;

              // Ensure service display name is populated - fallback to serviceMap if needed
              let serviceDisplayName =
                service && service.StackServices?.display_name;
              if (!serviceDisplayName) {
                // Fallback logic for service name mapping
                const serviceNameMapping: Record<string, string> = {
                  "hive-site": "Hive",
                  "webhcat-site": "Hive",
                  "hive-env": "Hive",
                  "yarn-site": "YARN",
                  "hbase-site": "HBase",
                  "accumulo-site": "Accumulo",
                  "kafka-broker": "Kafka",
                  "application-properties": "Atlas",
                  "infra-solr-env": "Ambari Infra Solr",
                  "storm-site": "Storm",
                  "core-site": "HDFS",
                  "zoo.cfg": "ZooKeeper",
                };
                serviceDisplayName = serviceNameMapping[fileName] || fileName;
              }

              propertiesToChange.push({
                propertyFileName: fileName,
                propertyName,
                propertyTitle:
                  configObject &&
                  t("installer.controls.serviceConfigPopover.title")
                    .replace("{0}", displayName)
                    .replace(
                      "{1}",
                      displayName === propertyName ? "" : propertyName
                    ),
                propertyDescription: configObject && configObject.description,
                serviceDisplayName: serviceDisplayName,
                initialValue: currentValue,
                recommendedValue: propertyDef.value,
                saveRecommended: true,
              });
            }
          });
        }
      }
    );
    // initializer.cleanup();
    //@ts-ignore
    const uniquePropertiesToChange = propertiesToChange.filter(
      (property: any, index: any, self: any) => {
        const uniqueKey = `${property.propertyFileName}-${property.propertyName}`;
        return (
          index ===
          self.findIndex(
            (p: any) => `${p.propertyFileName}-${p.propertyName}` === uniqueKey
          )
        );
      }
    );

    allPropertiesToChange.current = uniquePropertiesToChange;

    const newGroups = [
      {
        properties: {
          "hive-site": configs["hive-site"],
          "webhcat-site": configs["webhcat-site"],
          "hive-env": configs["hive-env"],
        },
        properties_attributes: {
          "hive-site": attributes["hive-site"],
          "webhcat-site": attributes["webhcat-site"],
          "hive-env": attributes["hive-env"],
        },
      },
      {
        properties: {
          "core-site": configs["core-site"],
        },
        properties_attributes: {
          "core-site": attributes["core-site"],
        },
      },
    ];
    initializer.cleanup();
    setConfigsChanges(newGroups);
  };

  const getZookeeperConnectionString = () => {
    const zkHosts = get(clusterComponents, "items", [])
      .filter((item: any) => {
        return (
          get(item, "ServiceComponentInfo.component_name") ===
          "ZOOKEEPER_SERVER"
        );
      })
      .flatMap((item: any) => get(item, "host_components", []))
      .map((hostComponent: any) => {
        return get(hostComponent, "HostRoles.host_name");
      });
    return zkHosts
      .map((host: any) => {
        return host + ":2181";
      })
      .join(",");
  };

  const functionMapping = {
    loadZookeeperConfigs,
    saveZkConfigs,
    loadRangerConfigs,
    onLoadRangerConfigs,
    loadHiveConfigs,
    onLoadHiveConfigs,
  };

  const loadComponentRelatedConfigs = async (
    configTagsCallbackName: keyof typeof functionMapping,
    configsCallbackName: keyof typeof functionMapping
  ) => {
    clearConfigsChanges();
    setIsConfigsLoadingInProgress(true);
    //setIsReconfigureRequired(true);
    isReconfigureRequired.current = true;
    const configTagsCallback =
      functionMapping[configTagsCallbackName as keyof typeof functionMapping];
    const configsCallback =
      functionMapping[configsCallbackName as keyof typeof functionMapping];
    await loadConfigs(configTagsCallback, configsCallback);
  };

  const loadConfigs = async (
    configTagsCallback: Function,
    configsCallback: Function
  ) => {
    try {
      const params = {
        callback: configsCallback,
      };
      if (
        typeof configTagsCallback !== "function" ||
        typeof configsCallback !== "function"
      ) {
        throw new Error(
          "Invalid function references passed to loadComponentRelatedConfigs"
        );
      }
      const response = await HostsApi.configTags(clusterName);
      return await configTagsCallback(response, {}, params);
    } catch (err) {
      console.error("Error in loading configs: ", err);
    }
  };

  const constructZookeeperConfigUrlParams = (data: any) => {
    const urlParams: any = [];

    let zookeeperRelatedServices = zooKeeperRelatedServices.slice(0);
    // handle HA enabled case.
    if (isNameNodeHAEnabled) {
      zookeeperRelatedServices.push({
        serviceName: "HDFS",
        typesToLoad: ["core-site"],
        typesToSave: ["core-site"],
      });
    }
    zookeeperRelatedServices.forEach((service: any) => {
      if (
        services.some(
          (svc: any) => svc.ServiceInfo?.service_name === service.serviceName
        )
      ) {
        service.typesToLoad.forEach((type: any) => {
          if (data.Clusters.desired_configs[type]) {
            urlParams.push(
              "(type=" +
                type +
                "&tag=" +
                data.Clusters.desired_configs[type].tag +
                ")"
            );
          }
        });
      }
    });
    return urlParams;
  };

  const saveLoadedConfigs = (data: any) => {
    setConfigs({
      items: data.items.map((item: any) => {
        return {
          type: item.type,
          properties_attributes: item.properties_attributes,
          properties: item.properties,
        };
      }),
    });
  };

  const clearConfigsChanges = (shouldKeepLoadingConfigs: boolean = false) => {
    var arrayNames = [
      "allPropertiesToChange",
      "recommendedPropertiesToChange",
      "requiredPropertiesToChange",
      "groupedPropertiesToChange",
    ];
    arrayNames.forEach((name: string) => {
      if (name === "allPropertiesToChange") {
        allPropertiesToChange.current = [];
      } else if (name === "recommendedPropertiesToChange") {
        setRecommendedPropertiesToChange([]);
      } else if (name === "requiredPropertiesToChange") {
        requiredPropertiesToChange.current = [];
      } else if (name === "groupedPropertiesToChange") {
        groupedPropertiesToChange.current = [];
      }
    });
    // setIsReconfigureRequired(false);
    isReconfigureRequired.current = false;
    if (!shouldKeepLoadingConfigs) {
      setConfigs({});
    }
  };

  const getUrlParamsForConfigsRequest = (data: any, configTypes: string[]) => {
    return compact(
      configTypes.map((type) => {
        const tag = get(data, `Clusters.desired_configs.${type}.tag`, null);
        return tag ? `(type=${type}&tag=${tag})` : null;
      })
    ).join("|");
  };

  const getRangerKMSServerHosts = () => {
    var rkmsHosts = get(clusterComponents, "items", [])
      .filter((item: any) => {
        return (
          get(item, "ServiceComponentInfo.component_name") ===
          "RANGER_KMS_SERVER"
        );
      })
      .flatMap((item: any) => get(item, "host_components", []))
      .map((hostComponent: any) => {
        return get(hostComponent, "HostRoles.host_name");
      });

    const rangerKMSServerHost = get(
      componentProperties.current,
      "rangerKMSServerHost",
      null
    );

    if (rangerKMSServerHost) {
      rkmsHosts.push(rangerKMSServerHost);
    }

    if (
      get(componentProperties.current, "fromDeleteHost", false) ||
      get(componentProperties.current, "deleteRangerKMSServer", false)
    ) {
      rkmsHosts = rkmsHosts.filter(
        (host: string) => host !== get(componentHere.current, "hostName")
      );
    }
    return rkmsHosts.sort();
  };

  const getHiveHosts = () => {
    var removePerformed =
      get(componentProperties.current, "fromDeleteHost", false) ||
      get(componentProperties.current, "deleteHiveMetaStore", false) ||
      get(componentProperties.current, "deleteHiveServer", false) ||
      get(componentProperties.current, "deleteWebHCatServer", false);
    var hiveMasterComponents = [
      "WEBHCAT_SERVER",
      "HIVE_METASTORE",
      "HIVE_SERVER",
    ];
    var masterComponentsMap = hiveMasterComponents
      .map((componentName: string) => {
        return bootstrapHostsMapping(componentName);
      })
      .reduce((p: any, c: any) => {
        return p.concat(c);
      });

    if (removePerformed) {
      set(componentProperties.current, "deleteHiveMetaStore", false);
      set(componentProperties.current, "deleteHiveServer", false);
      set(componentProperties.current, "deleteWebHCatServer", false);
      set(componentProperties.current, "fromDeleteHost", false);
      masterComponentsMap = masterComponentsMap.map((masterComponent) => {
        masterComponent.isInstalled =
          masterComponent.hostName !== get(componentHere.current, "hostName");
        return masterComponent;
      });
    }

    if (get(componentProperties.current, "hiveMetastoreHost", false)) {
      masterComponentsMap.push({
        component: "HIVE_METASTORE",
        hostName: get(componentProperties.current, "hiveMetastoreHost", ""),
        isInstalled: !removePerformed,
      });
      set(componentProperties.current, "hiveMetastoreHost", "");
    }

    if (get(componentProperties.current, "hiveServerHost", false)) {
      masterComponentsMap.push({
        component: "HIVE_SERVER",
        hostName: get(componentProperties.current, "hiveServerHost", ""),
        isInstalled: !removePerformed,
      });
      set(componentProperties.current, "hiveServerHost", "");
    }

    if (get(componentProperties.current, "webhcatServerHost", false)) {
      masterComponentsMap.push({
        component: "webhcatServerHost",
        hostName: get(componentProperties.current, "webhcatServerHost", ""),
        isInstalled: !removePerformed,
      });
      set(componentProperties.current, "webhcatServerHost", "");
    }
    return masterComponentsMap;
  };

  const bootstrapHostsMapping = (
    componentName: string,
    hostNames: string[] = []
  ) => {
    if (
      hostNames === null ||
      hostNames === undefined ||
      hostNames.length === 0
    ) {
      hostNames = get(clusterComponents, "items", [])
        .filter((item: any) => {
          return (
            get(item, "ServiceComponentInfo.component_name") === componentName
          );
        })
        .flatMap((item: any) => get(item, "host_components", []))
        .map((hostComponent: any) => {
          return get(hostComponent, "HostRoles.host_name");
        });
    }
    return hostNames.map((hostName: string) => {
      return {
        component: componentName,
        hostName: hostName,
        isInstalled: true,
      };
    });
  };

  const updateZkConfigs = (configs: any) => {
    const portValue = configs["zoo.cfg"]?.clientPort;
    const zkPort = typeof portValue === "undefined" ? "2181" : portValue;
    const infraSolrZnode =
      configs["infra-solr-env"]?.infra_solr_znode || "/ambari-solr";

    const initializer = new AddZooKeeperComponentsInitializer();
    //@ts-ignore
    initializer.setup();
    const hostComponentsTopology: any = {
      masterComponentHosts: [],
    };
    const propertiesToChange = allPropertiesToChange.current;
    const masterComponents = bootstrapHostsMapping("ZOOKEEPER_SERVER");

    if (
      get(componentProperties.current, "fromDeleteHost", false) ||
      get(componentProperties.current, "fromDeleteZkServer", false)
    ) {
      set(componentProperties.current, "fromDeleteHost", false);
      set(componentProperties.current, "fromDeleteZkServer", false);
      const removedHost = masterComponents.find(
        (host) => host.hostName === get(componentHere.current, "hostName")
      );
      if (removedHost) {
        removedHost.isInstalled = false;
      }
    } else if (get(componentProperties.current, "addZooKeeperServer", false)) {
      set(componentProperties.current, "addZooKeeperServer", false);
      const changedSelectedHostName = get(
        componentProperties.current,
        "selectedHost",
        ""
      );
      const componentHost = get(componentHere, "hostName", "");
      const selectedHostName = changedSelectedHostName
        ? changedSelectedHostName
        : componentHost;
      masterComponents.push({
        component: "ZOOKEEPER_SERVER",
        hostName: selectedHostName,
        isInstalled: true,
      });
    }

    const dependencies = {
      zkClientPort: zkPort,
      infraSolrZnode,
    };

    hostComponentsTopology.masterComponentHosts = masterComponents;

    Object.keys(configs).forEach((fileName) => {
      const properties = configs[fileName];
      Object.keys(properties).forEach((propertyName) => {
        const currentValue = properties[propertyName];
        const propertyDef = {
          filename: fileName,
          name: propertyName,
          value: currentValue,
        };
        const configProperty = initializer.initialValue(
          propertyDef,
          hostComponentsTopology,
          dependencies
        );

        initializer.updateSiteObj(configs[fileName], configProperty);

        if (currentValue !== configs[fileName][propertyName]) {
          const service = getServiceByConfigTypeMap(stackServices)[fileName];
          const configObject = getConfigByName(propertyName, fileName);
          const displayName = configObject && configObject.displayName;

          // Ensure service display name is populated - fallback to serviceMap if needed
          let serviceDisplayName =
            service && service.StackServices?.display_name;
          if (!serviceDisplayName) {
            // Fallback logic for service name mapping
            const serviceNameMapping: Record<string, string> = {
              "hive-site": "Hive",
              "webhcat-site": "Hive",
              "hive-env": "Hive",
              "yarn-site": "YARN",
              "hbase-site": "HBase",
              "accumulo-site": "Accumulo",
              "kafka-broker": "Kafka",
              "application-properties": "Atlas",
              "infra-solr-env": "Ambari Infra Solr",
              "storm-site": "Storm",
              "core-site": "HDFS",
              "zoo.cfg": "ZooKeeper",
            };
            serviceDisplayName = serviceNameMapping[fileName] || fileName;
          }

          propertiesToChange.push({
            propertyFileName: fileName,
            propertyName,
            propertyTitle:
              configObject &&
              t("installer.controls.serviceConfigPopover.title")
                .replace("{0}", displayName)
                .replace(
                  "{1}",
                  displayName === propertyName ? "" : propertyName
                ),
            propertyDescription: configObject && configObject.description,
            serviceDisplayName: serviceDisplayName,
            initialValue: currentValue,
            recommendedValue: propertyDef.value,
            saveRecommended: true,
          });
        }
      });
    });

    allPropertiesToChange.current = propertiesToChange;
  };

  return {
    deleteAndReconfigureComponent,
    _doDeleteHostComponent,
    loadComponentRelatedConfigs,
    saveLoadedConfigs,
    configs,
    setConfigs,
    // setIsReconfigureRequired,
    setIsConfigsLoadingInProgress,
    isReconfigureRequired: isReconfigureRequired.current,
    isConfigsLoadingInProgress,
    allPropertiesToChange,
    recommendedPropertiesToChange,
    requiredPropertiesToChange,
    groupedPropertiesToChange,
    _deletedHostComponentError,
    clearConfigsChanges,
    loadConfigs,
    getUrlParamsForConfigsRequest,
    applyConfigsCustomization,
    putConfigsToServer,
    addAndReconfigureComponent,
    setRecommendedPropertiesToChange,
    installAndReconfigureComponent,
  };
}

export default useComponentAddDelete;
