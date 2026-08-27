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

import { useState, useContext } from "react";
import { isEmpty, isArray, get } from "lodash";
import {
  ConfigPropertiesType,
  PropertyType,
} from "../screens/CommonConfigs/types";
import ConfigsApi from "../api/configsApi";
import { getConfigTagFromFileName } from "../screens/CommonConfigs/ConfigUtils";
import { AppContext } from "../store/context";
import { trimProperty } from "../screens/CommonConfigs/ConfigUtils";
import { messages } from "../screens/messages";
import ConfirmationModal from "../components/ConfirmationModal";
import modalManager from "../store/ModalManager";

interface AttributesType {
  final: { [key: string]: string };
  password: { [key: string]: string };
  user: { [key: string]: string };
  group: { [key: string]: string };
  text: { [key: string]: string };
  additional_user_property: { [key: string]: string };
  not_managed_hdfs_path: { [key: string]: string };
  value_from_property_file: { [key: string]: string };
}

export const useConfigSaver = (
  isSubmitDisabled: boolean,
  setIsSubmitDisabled:(value:boolean)=>void,
  selectedConfigGroup: string,
  configProperties: ConfigPropertiesType,
  serviceName: string,
  configGroupsData: any,
  serviceConfigVersionNote: string
) => {
  const [saveInProgress, setSaveInProgress] = useState(false);

  const heapsizeException = [
    "hadoop_heapsize",
    "yarn_heapsize",
    "nodemanager_heapsize",
    "resourcemanager_heapsize",
    "apptimelineserver_heapsize",
    "jobhistory_heapsize",
    "nfsgateway_heapsize",
    "accumulo_master_heapsize",
    "accumulo_tserver_heapsize",
    "accumulo_monitor_heapsize",
    "accumulo_gc_heapsize",
    "accumulo_other_heapsize",
    "hbase_master_heapsize",
    "hbase_regionserver_heapsize",
    "metrics_collector_heapsize",
    "hive_heapsize",
  ];

  const heapsizeRegExp =
    /_heapsize|_newsize|_maxnewsize|_permsize|_maxpermsize$/;
  const coreSiteServiceNames = ["HDFS", "GLUSTERFS", "RANGER_KMS"];

  const { services, clusterName } = useContext(AppContext);

  const serviceNames = services.map((service: any) => service.ServiceInfo?.service_name) || [];

  const saveStepConfigs = async () => {
    if (!isSubmitDisabled) {
      return saveConfigs();
    }
    return false;
  };

  const saveConfigs = async (): Promise<boolean> => {
    startSave();
    try {
      if (selectedConfigGroup === "Default") {
        await saveConfigsForDefaultGroup();
      } else {
        await saveConfigsForNonDefaultGroup();
      }
      return true;
    } catch {
      return false;
    } finally {
      completeSave();
    }
  };

  const saveConfigsForNonDefaultGroup = async () => {
    if (selectedConfigGroup && selectedConfigGroup !== "Default") {
      const overriddenConfigs = getConfigsForGroup(
        configProperties,
        selectedConfigGroup
      );

      if (
        !isEmpty(overriddenConfigs) && 
        isOverriddenConfigsModified(overriddenConfigs)
      ) {
        await saveGroup(overriddenConfigs);
      } else {
        putConfigGroupChangesSuccess();
      }
    }
  };

  const isOverriddenConfigsModified = (overriddenConfigs: PropertyType[]) => {
    const hasChangedConfigs = overriddenConfigs.some((config: any) => {
      const override = (config.overrideValues || []).find(
        (ov: any) => ov.groupName === selectedConfigGroup
      );
      
      return override && (
        override.value === null ||
        override.value !== override.previousValue ||
        override.final !== override.savedFinal
      );
    });

    return hasChangedConfigs;
  };

  const saveConfigsForDefaultGroup = async () => {
    let data: any = [];
    Object.keys(configProperties).map((serviceName: string) => {
      var serviceConfigs = getServiceConfigToSave(
        serviceName,
        configProperties
      );
      if (serviceConfigs) {
        data.push(serviceConfigs);
      }
    });

    if (data.length) {
      await putChangedConfigurations(data);
    } else {
      onDoPutClusterConfigurations(true);
    }
  };


  const startSave = () => {
    setSaveInProgress(true);
    setIsSubmitDisabled(true);
  };

  const completeSave = () => {
    setSaveInProgress(false);
    setIsSubmitDisabled(false);
  };

  //TODO: Implement the following functions

  // const showWarningPopupsBeforeSave = () => {
  //   if (isDirChanged()) {
  //     // showConfirmationPopup()
  //     // showChangedDependentConfigs()
  //     // restartServicePopup()
  //     //completeSave()
  //   } else {
  //     // showChangedDependentConfigs()
  //     // restartServicePopup()
  //     //completeSave()
  //   }
  // };

  // const restartServicePopup = () => {
  //   // serverSideValidation() ? saveConfigs() : completSave()
  // };

  // const isDirChanged = () => {
  //   let dirChanged = false;
  //   if (serviceName === "HDFS") {

  //     const hdfsConfigs = configProperties[serviceName];

  //     const dirConfigsToCheck = [
  //       "dfs.namenode.name.dir",
  //       "dfs.namenode.checkpoint.dir",
  //       "dfs.datanode.data.dir",
  //     ];


  //     dirConfigsToCheck.forEach((configName) => {
  //       Object.keys(hdfsConfigs).forEach((configType) => {
  //         const property = hdfsConfigs[configType].properties[configName];
  //         if (property && property.value !== property.propertyValue) {
  //           dirChanged = true;
  //         }
  //       });
  //     });
  //   }
  //   return dirChanged;
  // };

  const getModifiedConfigs = (
    configProperties: ConfigPropertiesType,
    serviceName: string
  ) => {
    let modifiedConfigs: any[] = [];

    const serviceConfigs = configProperties[serviceName];
    if (!serviceConfigs) {
      return modifiedConfigs;
    }

    Object.keys(serviceConfigs).forEach((configType) => {
      const properties = serviceConfigs[configType].properties;

      Object.keys(properties).forEach((propertyName) => {
        const property = properties[propertyName];

        if (
          property.isRequiredByAgent !== false &&
          (property.value !== property.previousValue ||
            property.final !== property.savedFinal ||
            (property.foundInPropertyValues === false && property.value !== null))
        ) {
          modifiedConfigs.push(property);
        }
      });
    });

    const uniqueFilenames = [
      ...new Set(modifiedConfigs.map((config) => config.fileName)),
    ];

    const configsByFile = uniqueFilenames.flatMap((filename) =>
      Object.keys(serviceConfigs).flatMap((configType) => {
          const properties = serviceConfigs[configType].properties;
          return Object.values(properties).filter(
            (property) => property.fileName === filename
          );
      })
    );

    return configsByFile;
  };

  const getConfigsForGroup = (
    configProperties: ConfigPropertiesType,
    configGroupName: string
  ) => {
    let overriddenConfigs: any[] = [];

    Object.keys(configProperties).forEach((serviceName) => {
      Object.keys(configProperties[serviceName]).forEach((configType) => {
        const properties = configProperties[serviceName][configType].properties;

        Object.keys(properties).forEach((propertyName) => {
          const property = properties[propertyName];
          if (property.overrideValues?.length) {
            const groupOverrides = property.overrideValues.filter(
              (override) => override.groupName === configGroupName
            );

            if (groupOverrides.length > 0) {
              overriddenConfigs.push(property);
            }
          }
        });
      });
    });

    return overriddenConfigs;
  };

  const getServiceConfigToSave = (
    serviceName: string,
    configProperties: any
  ) => {
    // if (serviceName === 'YARN') {
    //   configProperties = textareaIntoFileConfigs(configProperties, 'capacity-scheduler.xml');
    // }

    const modifiedConfigs = getModifiedConfigs(configProperties, serviceName);

    const serviceFileNames = getUniqueServiceFileNames(
      configProperties[serviceName]
    );
    // const filteredConfigs = modifiedConfigs.filter(config =>
    //   !config.group
    // );

    const filteredConfigs = modifiedConfigs;

    if (!Array.isArray(filteredConfigs) || filteredConfigs.length === 0) {
      return null;
    }

    const fileNamesToSave = [
      ...new Set(
        filteredConfigs
          .map((config) => config.fileName)
          .filter((fileName): fileName is string => fileName !== undefined)
      ),
    ].filter((filename) => serviceFileNames.includes(filename));

    const configsToSave = generateDesiredConfigsJSON(
      filteredConfigs,
      fileNamesToSave,
      serviceConfigVersionNote,
      false
    );

    if (configsToSave.length > 0) {
      return {
        Clusters: {
          desired_config: configsToSave,
        },
      };
    } else {
      return null;
    }
  };

  const generateDesiredConfigsJSON = function (
    configsToSave: any,
    fileNamesToSave: string[],
    serviceConfigNote: string,
    ignoreVersionNote: boolean
  ) {
    let desired_config: any = [];
    if (
      isArray(configsToSave) &&
      isArray(fileNamesToSave) &&
      fileNamesToSave.length &&
      configsToSave.length
    ) {
      serviceConfigNote = serviceConfigNote || "";
    }

    fileNamesToSave.forEach((fileName: string) => {
      if (allowSaveSite(fileName)) {
        const properties = configsToSave.filter(
          (config: any) => config.fileName === fileName
        );
        const type = getConfigTagFromFileName(fileName);
        desired_config.push(
          createDesiredConfig(
            type,
            properties,
            serviceConfigNote,
            ignoreVersionNote
          )
        );
      }
    });

    return desired_config;
  };

  const allowSaveSite = function (fileName: string) {
    switch (getConfigTagFromFileName(fileName)) {
      case "mapred-queue-acls":
        return false;
      case "core-site":
        return allowSaveCoreSite();
      default:
        return true;
    }
  };

  const allowSaveCoreSite = function () {
    return serviceNames.some((service: string) =>
      coreSiteServiceNames.includes(service)
    );
  };

  const createDesiredConfig = function (
    type: string,
    properties: any,
    serviceConfigNote: string,
    ignoreVersionNote: boolean
  ) {
    let desired_config: any = {
      type: type,
      properties: {},
    };

    if (!ignoreVersionNote) {
      desired_config.service_config_version_note = serviceConfigNote || "";
    }

    var attributes: AttributesType = {
      final: {},
      password: {},
      user: {},
      group: {},
      text: {},
      additional_user_property: {},
      not_managed_hdfs_path: {},
      value_from_property_file: {},
    };

    if (isArray(properties)) {
      properties.forEach((property: any) => {
        if (get(property, "isRequiredByAgent", "") !== false) {
          const name = get(property, "propertyName");
          if(get(property,"propertyAttributes.type")==="hosts" || get(property,"value", null) === null){
            return;
          }
          desired_config.properties[name] = formatValuesBeforeSave(property);
          if (get(property, "final") === "true") {
            attributes.final[name] = "true";
          }
          if (!isEmpty(property.propertyType)) {
            property.propertyType.forEach((propType: string) => {
              const key = propType.toLowerCase() as keyof AttributesType;
              if (key in attributes) {
                attributes[key][name] = "true";
              }
            });
          }
        }
      });
    }

    if (
      Object.values(attributes).some(
        (attributeValues) => Object.keys(attributeValues).length > 0
      )
    ) {
      desired_config.properties_attributes = attributes;
    }

    return desired_config;
  };

  const formatValuesBeforeSave = function (property: any) {
    const name = get(property, "propertyName");
    
    // Find the correct override value for the selected config group
    let value;
    if (selectedConfigGroup === "Default") {
      value = get(property, "value");
    } else {
      const override = (property.overrideValues || []).find(
        (ov: any) => ov.groupName === selectedConfigGroup
      );
      value = override ? override.value : get(property, "value");
    }
    
    // var kdcTypesMap = App.router.get('mainAdminKerberosController.kdcTypesValues');

    if (addM(name, value)) {
      return (value += "m");
    }
    if (typeof value === "boolean") {
      return value.toString();
    }
    switch (name) {
      // case 'kdc_type':
      //   return Em.keys(kdcTypesMap).filter(function(key) {
      //       return kdcTypesMap[key] === value;
      //   })[0];
      case "storm.zookeeper.servers":
      case "nimbus.seeds":
        if (isArray(value)) {
          return JSON.stringify(value).replace(/"/g, "'");
        } else {
          return value;
        }
        break;
      default:
        return trimProperty(property, value);
    }
  };

  const addM = (name: string, value: string) => {
    return (
      typeof value === "string" &&
      heapsizeRegExp.test(name) &&
      !heapsizeException.includes(name) &&
      !value.endsWith("m")
    );
  };

  const saveGroup = async (overriddenConfigs: PropertyType[]) => {
    const fileNamesToSave = [...new Set(
      overriddenConfigs
        .map(config => config.fileName)
        .filter((fileName): fileName is string => fileName !== undefined)
    )];

    // Find the config group from configGroupsData array
    const configGroup = configGroupsData.find(
      (group: any) => group.ConfigGroup.group_name === selectedConfigGroup
    );
    
    const groupId = get(configGroup, 'ConfigGroup.id');
    
    if (!groupId) {
      const error = new Error("Config group ID not found");
      doPUTClusterConfigurationSiteErrorCallback();
      throw error;
    }

    // Prepare the configs specifically for this config group
    const configsForGroup = overriddenConfigs.map(config => {
      // Create a modified copy of the config for the group
      return {
        ...config,
        // For non-default groups, we need to use the override value for this specific group
        value: (config.overrideValues || []).find(
          (ov: any) => ov.groupName === selectedConfigGroup
        )?.value ?? config.value,
        final: (config.overrideValues || []).find(
          (ov: any) => ov.groupName === selectedConfigGroup
        )?.final ?? "false"
      };
    });

    const groupData: any = {
      ConfigGroup: {
        cluster_name: clusterName,
        group_name: selectedConfigGroup,
        tag: get(configGroup, 'ConfigGroup.tag'),
        service_name: serviceName,
        description: get(configGroup, 'ConfigGroup.description', ""),
        hosts: get(configGroup, 'ConfigGroup.hosts', []), // Include hosts from the config group
        service_config_version_note: serviceConfigVersionNote || "",
        desired_configs: generateDesiredConfigsJSON(
          configsForGroup,
          fileNamesToSave,
          serviceConfigVersionNote,
          true
        )
      }
    };

    groupData.ConfigGroup.id = groupId;
    await updateConfigGroup(groupData);
  };

  // const createConfigGroup = () => {
  //   // To be implemented when a new configGroup group has to be created during the installation wizard
  // };

  const updateConfigGroup = async (groupData: any) => {
    try {
      const response = await ConfigsApi.updateConfigGroupProperties(
        clusterName,
        groupData.ConfigGroup.id,
        groupData
      );

      if (response) {
        putConfigGroupChangesSuccess();
      }
    } catch (error) {
      doPUTClusterConfigurationSiteErrorCallback();
      throw error;
    }
  };

  const putChangedConfigurations = async (data: any) => {
    try {
      const response = await ConfigsApi.saveConfigs(clusterName, data);
      doPUTClusterConfigurationSiteSuccessCallback();
      return response;
    } catch (error) {
      doPUTClusterConfigurationSiteErrorCallback();
      throw error;
    }
  };

  const putConfigGroupChangesSuccess = () => {
    onDoPutClusterConfigurations(true);
  };

  const doPUTClusterConfigurationSiteSuccessCallback = () => {
    let doConfigActions = true;
    onDoPutClusterConfigurations(true, doConfigActions);
  };

  const doPUTClusterConfigurationSiteErrorCallback = () => {
    onDoPutClusterConfigurations(false);
  };

  const onDoPutClusterConfigurations = (
    isSuccess: boolean,
    doConfigActions?: boolean
  ) => {
    let status = "unknown";
    let result = {
      flag: isSuccess,
      message: "",
      value: "",
    };

    let popupOptions = getSaveConfigsPopupOptions(result);

    if (!result.flag) {
      result.message = get(messages, "services.service.config.failSaveConfig");
    }

    showSaveConfigsPopup(
      popupOptions.header,
      result.flag,
      popupOptions.message,
      popupOptions.messageClass,
      popupOptions.value,
      status,
      popupOptions.urlParams,
      doConfigActions
    );
    // clearAllRecommendations();
  };

  const getSaveConfigsPopupOptions = (result: any) => {
    if (result.flag) {
      const options = {
        header: get(
          messages,
          "services.service.config.saved",
          "Configuration Saved"
        ),
        message: get(
          messages,
          "services.service.config.saved.message",
          "The configuration was successfully saved."
        ),
        messageClass: "alert alert-success",
        urlParams:
          ",ServiceComponentInfo/installed_count,ServiceComponentInfo/total_count",
        value: "",
      };

      if (serviceName === "HDFS") {
        options.urlParams += "&ServiceComponentInfo/service_name.in(HDFS)";
      }

      return options;
    }

    return {
      header: get(messages, "common.failure", "Failed"),
      message: result.message,
      messageClass: "alert alert-error",
      urlParams: "",
      value: result.value,
    };
  };
  
  const showSaveConfigsPopup = (
    header: string,
    isSuccess: boolean,
    message: string,
    messageClass: string,
    _value: string | undefined,
    _status: string,
    _urlParams: string,
    _doConfigActions: boolean | undefined
  ) => {
    modalManager.show(
      <ConfirmationModal
        isOpen={true}
        onClose={() => modalManager.hide()}
        modalTitle={header}
        modalBody={<div className={messageClass}>{message}</div>}
        successCallback={() => modalManager.hide()}
        buttonVariant={isSuccess ? "success" : "danger"}
        cancellable={false}
        okButtonText="OK"
      />
    );
  };

  // const showSavePopup = () => {
  //   // To be implemented
  // };

  const getUniqueServiceFileNames = (serviceConfigProperties: any) => {
    const uniqueFileNames = new Set<string>();
    Object.keys(serviceConfigProperties).forEach((configType) => {
      const properties = serviceConfigProperties[configType].properties;
      Object.values(properties).forEach((property: any) => {
        if (property.fileName) {
          uniqueFileNames.add(property.fileName);
        }
      });
    });

    return Array.from(uniqueFileNames);
  };

  return {
    saveInProgress,
    saveStepConfigs,
  };
};
