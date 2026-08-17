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

import { useCallback, useContext, useEffect, useRef, useState, useTransition } from "react";
import { useBlocker, useLocation, useNavigate, useParams } from "react-router-dom";
import ConfigsApi from "../../api/configsApi";
import { useAuth } from "../../hooks/useAuth";
import { AuthGuard } from "../../components/AuthGuard";
import { ConfigPropertiesType } from "../CommonConfigs/types";
import { ambari_metrics_properties } from "../../data/configs/services/ambari_metrics_properties";
import { hbase_properties } from "../../data/configs/services/hbase_properties";
import { hdfs_properties } from "../../data/configs/services/hdfs_properties";
import { hive_properties } from "../../data/configs/services/hive_properties";
import { mapreduce2_properties } from "../../data/configs/services/mapreduce2_properties";
import { ranger_properties } from "../../data/configs/services/ranger_properties";
import { tez_properties } from "../../data/configs/services/tez_properties";
import { yarn_properties } from "../../data/configs/services/yarn_properties";
import { zookeeper_properties } from "../../data/configs/services/zookeeper_properties";
import { kerberos_properties } from "../../data/configs/services/kerberos_properties";
import { get, isEmpty, isObject, cloneDeep, map } from "lodash";
import { Alert, Button, Card, Form, Spinner } from "react-bootstrap";
import Config from "../CommonConfigs/Config";
import {
  addTabNames,
  fetchComponentHostNamesByComponent,
  formatPropertyValue,
  getConfigCategories,
  getIsSecure,
  removeRangerConfigs,
  shouldSupportFinal,
  setPropertyIsEditable,
  updateVisibilityByForeignKeys,
  updateVisibilityForDependsOn,
  validateAllProperties,
  hideComponentConfigsBasedOnAvailability,
} from "../CommonConfigs/ConfigUtils";
import VersionsList from "../ConfigVersions/VersionsList";
import ChooseConfigGroup from "../CommonConfigs/ChooseConfigGroup";
import AddToConfigGroupModal from "../ConfigGroups/AddToConfigGroupModal";
import ManageConfigGroups from "../ConfigGroups/ManageConfigGroups";
import { useConfigSaver } from "../../hooks/useConfigSaver";
import { AppContext } from "../../store/context";
import { messages } from "../messages";
import Modal from "../../components/Modal";
import ConfigsComparator from "../ConfigVersions/ConfigsComparator";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faExchangeAlt, faXmark } from "@fortawesome/free-solid-svg-icons";
import { ServiceContext } from "../../store/ServiceContext";
import { serviceNameModelMapping } from "../../constants";
import useEnhancedConfigs from "../../hooks/useEnhancedConfigs";
import useHostComponents from "../ClusterWizard/hooks/useHostComponents";
import Table from "../../components/Table";
import useServerValidation from "../../hooks/useServerValidation";
import { translate } from "../../Utils/Utility";
import { kyuubi_properties } from "../../data/configs/services/kyuubi_properties";
import { sqoop_properties } from "../../data/configs/services/sqoop_properties";
import {
  ConfigHistoryNavigationState,
  resolveConfigHistorySelection,
} from "../../Utils/configHistory";

type ServiceConfigsProps = {
  serviceName?: string;
};

export default function ServiceConfigs({
  serviceName: serviceNameProps,
}: ServiceConfigsProps) {
  const { serviceName: serviceNameParams } = useParams();
  const serviceName =
    serviceNameProps?.toUpperCase() || serviceNameParams?.toUpperCase() || "";
  const location = useLocation();
  const navigate = useNavigate();
  const locationSelection = location.state as ConfigHistoryNavigationState | null;
  const pendingHistorySelection = useRef<ConfigHistoryNavigationState | null>(
    locationSelection?.serviceName === serviceName ? locationSelection : null,
  );

  useEffect(() => {
    if (pendingHistorySelection.current) {
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [location.pathname, navigate]);

  // Authorization hooks - implementing Ember.js App.isAuthorized patterns
  const { hasAuthorization } = useAuth();

  // Check SERVICE.MODIFY_CONFIGS authorization like in Ember.js ui/app/controllers/main/service/info/configs.js
  const canModifyConfigs = hasAuthorization('SERVICE.MODIFY_CONFIGS');
  const [loading, setLoading] = useState<boolean>(true);
  const [themes, setThemes] = useState<any>({});
  const [configs, setConfigs] = useState<any>({});
  const [configProperties, setConfigProperties] =
    useState<ConfigPropertiesType>({});
  const [propertyValues, setPropertyValues] = useState<any>({});
  const [defaultVersionNumber, setDefaultVersionNumber] = useState<string>();
  const [selectedVersion, setSelectedVersion] = useState<string>();
  const [configGroup, setConfigGroup] = useState<string>("Default");
  const [showAddToGroupModal, setShowAddToGroupModal] =
    useState<boolean>(false);
  const [showManageConfigGroupModal, setShowManageConfigGroupModal] =
    useState<boolean>(false);
  const [isSubmitDisabled, setIsSubmitDisabled] = useState<boolean>(true);
  const [isPending, startTransition] = useTransition();
  const [configGroupsData, setConfigGroupsData] = useState<any[]>([]);
  const [serviceConfigVersionNote, setServiceConfigVersionNote] =
    useState<string>("");
  const [showSaveConfigsModal, setShowSaveConfigModal] =
    useState<boolean>(false);
  const [showUnsaveChangesModal, setShowUnsaveChangesModal] =
    useState<boolean>(false);
  const [isComparing, setIsComparing] = useState<boolean>(false);
  const [firstVersion, setFirstVersion] = useState<string>("");
  const [versionCompared, setVersionComparedState] = useState<string>("");
  const [configsLoaded, setIsConfigsLoaded] = useState<boolean>(false);
  const [refetchTrigger, setRefetchTrigger] = useState<number>(0);

  const { loadRecommendationsForConfigOnLoad } = useEnhancedConfigs(
    setConfigProperties,
    serviceName
  );
  const { clusterName, services, cluster } = useContext(AppContext);
  const selectedServices = map(services, "ServiceInfo.service_name");
  const { hostComponents } = useHostComponents(selectedServices);
  const [currentVersion, setCurrentVersion] = useState<string>("");
  const { validationErrors, validateConfigProperties } = useServerValidation(
    hostComponents,
    configProperties,
    serviceName,
    (errors: any) => {
      if (errors.length) {
        setShowValidationErrorsModal(true);
      } else {
        saveConfigs();
      }
    }
  );

  const { saveStepConfigs } = useConfigSaver(
    isSubmitDisabled,
    setIsSubmitDisabled,
    configGroup,
    configProperties,
    serviceName,
    configGroupsData,
    serviceConfigVersionNote
  );

  const { allServiceModels } = useContext(ServiceContext);

  const stackName = get(cluster, "stack");
  const stackVersion = get(cluster, "versionNum");
  const [showValidationErrorsModal, setShowValidationErrorsModal] =
    useState<boolean>(false);

  const propertiesFileMap: { [key: string]: any } = {
    HDFS: hdfs_properties,
    YARN: yarn_properties,
    HIVE: hive_properties,
    HBASE: hbase_properties,
    RANGER: ranger_properties,
    MAPREDUCE2: mapreduce2_properties,
    TEZ: tez_properties,
    SQOOP: sqoop_properties,
    ZOOKEEPER: zookeeper_properties,
    AMBARI_METRICS: ambari_metrics_properties,
    KERBEROS: kerberos_properties,
    KYUUBI: kyuubi_properties,
  };

  const servicesToFetch = services.map(service => service.ServiceInfo.service_name).join(',')

  const blocker = useBlocker(
      useCallback(
        ({ currentLocation, nextLocation }: { currentLocation: any; nextLocation: any }) => {
          // Only block if there are unsaved changes and we're actually navigating away
          return (
            !isSubmitDisabled && !isComparing &&
            currentLocation.pathname !== nextLocation.pathname
          );
        },
        [isSubmitDisabled, isComparing]
      )
    );

  // Define onVersionChange function before useEffects
  async function onVersionChange(versionNumber: any) {
    try {
      setSelectedVersion(versionNumber);
      let apiVersionNumber = versionNumber;
      if (configGroup !== "Default") {
        apiVersionNumber = defaultVersionNumber + "," + versionNumber;
      }

      const response = await (apiVersionNumber != defaultVersionNumber
        ? ConfigsApi.getVersionConfigValues(
            clusterName,
            serviceName,
            apiVersionNumber
          )
        : ConfigsApi.getConfigValues(clusterName, serviceName));

      // Merge the response with existing property values for other services
      const updatedPropertyValues = { ...propertyValues };

      // Filter out items for the current service
      updatedPropertyValues.items = propertyValues.items.filter(
        (item: any) => item.service_name !== serviceName
      );

      // Add the new items for the current service
      updatedPropertyValues.items = [
        ...updatedPropertyValues.items,
        ...response.items,
      ];

      setPropertyValues(updatedPropertyValues);
    } catch (error) {
      console.error("Error changing version:", error);
    }
  }

  useEffect(() => {
    if (blocker.state === 'blocked') {
      setShowUnsaveChangesModal(true);
    }
  }, [blocker]);

  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        setLoading(true);
        await Promise.all([
          getConfigurations(),
          getThemes(),
          getPropertiesValues(),
        ]);
        setLoading(false);
      } catch (error) {
        console.error("Error loading initial data:", error);
      }
    };
    fetchInitialData();
  }, [serviceName]);

  useEffect(() => {
    setIsComparing(false);
  }, [serviceName]);

  useEffect(() => {
    if (isComparing && !firstVersion && defaultVersionNumber) {
      setFirstVersion(defaultVersionNumber);
    }
  }, [isComparing, firstVersion, defaultVersionNumber]);

  useEffect(() => {
    if (!isEmpty(configs) && !isEmpty(propertyValues) && configsLoaded) {
      getConfigProperties();
    }
  }, [configs, propertyValues, configsLoaded]);

  useEffect(() => {
    function allMastersLoaded() {
      let masterComponentsLoaded = false;
      let slaveComponentsLoaded = false;
      const mappedServiceName =
        serviceNameModelMapping[serviceName.toUpperCase()];

      if (
        allServiceModels[mappedServiceName]?.masterComponents &&
        allServiceModels[mappedServiceName].masterComponents.length > 0
      ) {
        masterComponentsLoaded = true;
      }

      if (
        allServiceModels[mappedServiceName]?.slaveComponents &&
        allServiceModels[mappedServiceName].slaveComponents.length > 0
      ) {
        slaveComponentsLoaded = true;
      }
      return (
        masterComponentsLoaded ||
        slaveComponentsLoaded ||
        allServiceModels?.[serviceNameModelMapping[serviceName.toUpperCase()]]
          ?.isClientOnlyService
      );
    }
    setIsConfigsLoaded(allMastersLoaded());
  }, [JSON.stringify(allServiceModels)]);

 useEffect(() => {
  if (!isEmpty(configProperties)) {
    startTransition(() => {
      const hasChanges =
        configProperties?.[serviceName] &&
        Object.values(configProperties[serviceName]).some((section) =>
          Object.values(section.properties).some(
            (prop) => {              
              if (prop.value === null && prop.isVisible === false) {
                return prop.foundInPropertyValues === true;
              }
              
              const mainValueChanged = prop.value !== prop.previousValue;
              
              // Check if this is a new custom property that hasn't been saved yet
              const isNewCustomProperty = prop.foundInPropertyValues === false && prop.value !== null;
              
              const overrideValuesChanged = prop.overrideValues && 
                Array.isArray(prop.overrideValues) && 
                prop.overrideValues.some(override => 
                  // Skip override values that are null (removed)
                  override.value !== null && override.value !== override.previousValue
                );
              
              const finalChanged = prop.final !== prop.savedFinal;

              return mainValueChanged || isNewCustomProperty || overrideValuesChanged || finalChanged;
            }
          )
        );
      setIsSubmitDisabled(!hasChanges);
    });
  }
}, [configProperties, serviceName]);


  const setVersionCompared = (versionNumber:any) => {
    if (versionNumber !== firstVersion) {
      setVersionComparedState(versionNumber);
    }
  };

  const getThemes = async () => {
    setLoading(true);
    try {
      const response = await ConfigsApi.getTheme(
        stackName,
        stackVersion,
        servicesToFetch
      );
      setThemes(response);
    } catch (error) {
      console.error("Error fetching themes:", error);
    } finally {
      setLoading(false);
    }
  };

  const getConfigurations = async () => {
    setLoading(true);
    try {
      const response = await ConfigsApi.getServiceConfigurations(
        stackName,
        stackVersion,
        servicesToFetch
      );
      setConfigs(response);
    } catch (error) {
      console.error("Error fetching configurations:", error);
    } finally {
      setLoading(false);
    }
  };

  const getPropertiesValues = async () => {
    setLoading(true);
    try {
      const response = await ConfigsApi.getConfigValues(
        clusterName,
        servicesToFetch
      );
      // Find the default version for the currently selected service
      const currentServiceItems = response.items.filter((item: any) => 
        item.service_name === serviceName && item.group_name === "Default"
      );
      
      if (currentServiceItems.length > 0) {
        const latestDefaultVersion = String(get(currentServiceItems[0], "service_config_version", ""));
        const historySelection = pendingHistorySelection.current;
        const selection = resolveConfigHistorySelection(latestDefaultVersion, historySelection);
        let selectedPropertyValues = response;
        if (selection.versionsToLoad) {
          const selectedResponse = await ConfigsApi.getVersionConfigValues(
            clusterName,
            serviceName,
            selection.versionsToLoad,
          );
          selectedPropertyValues = {
            ...response,
            items: [
              ...response.items.filter((item: any) => item.service_name !== serviceName),
              ...selectedResponse.items,
            ],
          };
        }
        setPropertyValues(selectedPropertyValues);
        setDefaultVersionNumber(latestDefaultVersion);
        setConfigGroup(selection.configGroup);
        // Only set firstVersion if not already set (to preserve comparison state)
        if (!firstVersion) {
          setFirstVersion(selection.selectedVersion);
        }
        setSelectedVersion(selection.selectedVersion);
        pendingHistorySelection.current = null;
      } else {
        setPropertyValues(response);
      }
    } catch (error) {
      console.error("Error fetching property values:", error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Initialize the configuration structure with base properties from configs
   */
  const initializeConfigStructure = () => {
    let configPropertiesCopy: ConfigPropertiesType = {};

    configs?.items?.forEach((service: any) => {
      service.configurations?.forEach((config: any) => {
        const fileName = config.StackConfigurations.type as string;
        const configType = fileName.slice(0, -4);
        const propertyName = config.StackConfigurations.property_name as string;
        const serviceName = config.StackConfigurations.service_name;
        const propertyType = config.StackConfigurations.property_type;

        if (!configPropertiesCopy[serviceName]) {
          configPropertiesCopy[serviceName] = {};
        }
        if (!configPropertiesCopy[serviceName][configType]) {
          configPropertiesCopy[serviceName][configType] = {
            errors: 0,
            properties: {},
          };
        }

        configPropertiesCopy[serviceName][configType].properties[propertyName] =
          {
            propertyName: propertyName,
            ...(config.StackConfigurations.property_display_name && {
              propertyDisplayname:
                config.StackConfigurations.property_display_name,
            }),
            ...(config.StackConfigurations.property_description && {
              propertyDescription:
                config.StackConfigurations.property_description,
            }),
            propertyValue: config.StackConfigurations.property_value,
            propertyAttributes:
              config.StackConfigurations.property_value_attributes,
            previousValue: config.StackConfigurations.property_value,
            value: config.StackConfigurations.property_value,
            supportsFinal : shouldSupportFinal(serviceName, fileName, configs),
            isSecureConfig: getIsSecure(propertyName),
            final: "false",
            savedFinal : "false",
            fileName: fileName,
            propertyType: propertyType ? propertyType : [],
            type: configType,
            serviceName: serviceName,
            isEditable: config.StackConfigurations.property_value_attributes.editable_only_at_install ? false :
              configGroup === "Default" &&
              selectedVersion === defaultVersionNumber &&
              canModifyConfigs,
            isVisible: config.StackConfigurations.property_value_attributes.visible === false ?  false : true,
            propertyDependsOn:
              config.StackConfigurations.property_depends_on || [],
            propertyDependedBy: (config.dependencies || []).map(
              (dependency: any) => {
                const { dependency_name, dependency_type } =
                  dependency.StackConfigurationDependency;
                return { propertyName: dependency_name, type: dependency_type };
              }
            ),
            unit:
              config.StackConfigurations.property_value_attributes?.unit ||
              null,
            isHidden: config.StackConfigurations.property_value_attributes.visible === false ?  true : false,
          };

        if (
          configPropertiesCopy[serviceName][configType].properties[propertyName]
            .propertyAttributes.type == "password"
        ) {
          configPropertiesCopy[serviceName][configType].properties[
            propertyName
          ] = {
            ...configPropertiesCopy[serviceName][configType].properties[
              propertyName
            ],
            confirmPassword: config.StackConfigurations.property_value,
          };
        }

        if (propertyName === "content") {
            configPropertiesCopy[serviceName][configType].properties[
          propertyName
        ].propertyAttributes.type = "content";
          }

        if (
          propertyType &&
          (propertyType.includes("USER") || propertyType.includes("GROUP"))
        ) {
          configPropertiesCopy[serviceName][configType].properties[
            propertyName
          ].isHidden = true;
        }
      });
    });


    Object.keys(configPropertiesCopy).forEach(serviceName => {
      if (isObject(configPropertiesCopy[serviceName])) {
        Object.keys(configPropertiesCopy[serviceName]).forEach(configType => {
          // if (!!!configType.endsWith("env")) {
            configPropertiesCopy[serviceName]["Custom " + configType] = {
              errors: 0,
              properties: {},
              displayName: "Custom " + configType,
            };
          // }
        });
      }
    });


    return configPropertiesCopy;
  };

  /**
   * Process default configuration items
   */
  const processDefaultConfigurations = (
    configPropertiesCopy: ConfigPropertiesType
  ) => {
    if (isEmpty(propertyValues)) {
      return configPropertiesCopy;
    }

    // Create a deep clone to avoid modifying the original
    const result = cloneDeep(configPropertiesCopy);

    const defaultItems = propertyValues?.items?.filter(
      (item: any) => item.group_name === "Default"
    );

    // Mark all properties as not found in propertyValues initially
    Object.keys(result).forEach((serviceName) => {
      Object.keys(result[serviceName]).forEach((configType) => {
        Object.keys(result[serviceName][configType].properties).forEach(
          (propertyName) => {
            result[serviceName][configType].properties[
              propertyName
            ].foundInPropertyValues = false;
          }
        );
      });
    });

    defaultItems?.forEach((item: any) => {
      item?.configurations?.forEach((config: any) => {
        const type = config.type;
        const properties = config.properties;
        const propertyAttributes = config.properties_attributes;
        const serviceName = get(item, "service_name", "");

        Object.keys(properties).forEach((propertyName: string) => {
          if (result[serviceName]?.[type]) {
            if (result[serviceName][type]?.properties[propertyName]) {
              result[serviceName][type].properties[propertyName].value =
                formatPropertyValue(
                  result[serviceName][type]?.properties[propertyName],
                  properties[propertyName]
                );
              result[serviceName][type].properties[propertyName].previousValue =
                formatPropertyValue(
                  result[serviceName][type]?.properties[propertyName],
                  properties[propertyName]
                );

              // Mark this property as found in propertyValues
              result[serviceName][type].properties[
                propertyName
              ].foundInPropertyValues = true;

              if (
                result[serviceName][type].properties[propertyName]
                  .propertyAttributes.type === "password"
              ) {
                result[serviceName][type].properties[
                  propertyName
                ].confirmPassword =
                  result[serviceName][type].properties[propertyName].value;
              }
            } else {
              if (result[serviceName]["Custom " + type]) {
                result[serviceName]["Custom " + type].properties[propertyName] =
                  {
                    propertyName: propertyName,
                    propertyDisplayname: propertyName,
                    propertyValue: properties[propertyName],
                    propertyAttributes: {type:"custom"},
                    previousValue: properties[propertyName],
                    value: properties[propertyName],
                    final: "false",
                    savedFinal : "false",
                    fileName: type + ".xml",
                    propertyType: [],
                    type: type,
                    isEditable:
                      configGroup === "Default" &&
                      selectedVersion === defaultVersionNumber,
                    foundInPropertyValues: true, // Mark as found since it's being added from propertyValues
                  };
              }
            }
          }
        });

        Object.keys(propertyAttributes).forEach((attr:string) => {
          if(attr !== 'final') return; // Currently, only 'final' attribute is processed
          Object.keys(propertyAttributes[attr]).forEach((propertyName) => {
            if (result[serviceName]?.[type]?.properties[propertyName]) {
              result[serviceName][type].properties[propertyName].final = propertyAttributes[attr][propertyName]; 
              result[serviceName][type].properties[propertyName].savedFinal = propertyAttributes[attr][propertyName];
            }
          })
        })

      });
    });

    return result;
  };

  /**
   * Process override configuration items (non-default config groups)
   */
  const processOverrideConfigurations = (
    configPropertiesCopy: ConfigPropertiesType
  ) => {
    if (isEmpty(propertyValues)) {
      return configPropertiesCopy;
    }

    // Create a deep clone to avoid modifying the original
    const result = cloneDeep(configPropertiesCopy);

    const otherItems = propertyValues?.items?.filter(
      (item: any) => item.group_name !== "Default"
    );

    otherItems?.forEach((item: any) => {
      item?.configurations?.forEach((config: any) => {
        const type = config.type;
        const properties = config.properties;
        const serviceName = get(item, "service_name", "");
        const groupName = get(item, "group_name", "");

        Object.keys(properties).forEach((propertyName: string) => {
          if (result[serviceName]?.[type]) {
            if (result[serviceName][type]?.properties[propertyName]) {
              // Mark this property as found in propertyValues
              result[serviceName][type].properties[
                propertyName
              ].foundInPropertyValues = true;

              if (
                !result[serviceName][type].properties[propertyName]
                  .overrideValues
              ) {
                result[serviceName][type].properties[
                  propertyName
                ].overrideValues = [];
              }
              result[serviceName][type].properties[
                propertyName
              ].overrideValues.push({
                value: formatPropertyValue(
                  result[serviceName][type]?.properties[propertyName],
                  properties[propertyName]
                ),
                groupName: groupName,
                previousValue: formatPropertyValue(
                  result[serviceName][type]?.properties[propertyName],
                  properties[propertyName]
                ),
              });
            } else {
              if (
                !result[serviceName]["Custom " + type]?.properties[propertyName]
              ) {
                if (result[serviceName]["Custom " + type]) {
                  result[serviceName]["Custom " + type].properties[
                    propertyName
                  ] = {
                    propertyName: propertyName,
                    propertyDisplayname: propertyName,
                    propertyValue: "Undefined",
                    propertyAttributes: { type: "custom" },
                    previousValue: "Undefined",
                    value: "Undefined",
                    final: "false",
                    savedFinal : "false",
                    fileName: type + ".xml",
                    propertyType: [],
                    type: type,
                    serviceName: serviceName, // Explicitly set the serviceName to ensure correct association
                    isEditable:
                      configGroup === "Default" &&
                      selectedVersion === defaultVersionNumber,
                    foundInPropertyValues: true, // Mark as found since it's being added from propertyValues
                  };
                }
              }

              if (
                !result[serviceName]["Custom " + type].properties[propertyName]
                  .overrideValues
              ) {
                result[serviceName]["Custom " + type].properties[
                  propertyName
                ].overrideValues = [];
              }
              result[serviceName]["Custom " + type].properties[
                propertyName
              ].overrideValues?.push({
                value: properties[propertyName],
                groupName: groupName,
                previousValue: properties[propertyName],
              });
            }
          }
        });
      });
    });

    return result;
  };

  /**
   * Add component host information to the configuration
   */
  const addComponentHostInformation = (
    configPropertiesCopy: ConfigPropertiesType
  ) => {
    // Create a new object to avoid modifying the original
    let updatedConfigProperties: ConfigPropertiesType = {};

    Object.keys(configPropertiesCopy).forEach((serviceName) => {
      if (!updatedConfigProperties[serviceName]) {
        updatedConfigProperties[serviceName] = {};
      }

      const serviceConfigCategories = getConfigCategories(serviceName);
      serviceConfigCategories.forEach((category) => {
        if (!updatedConfigProperties[serviceName][category.name]) {
          updatedConfigProperties[serviceName][category.name] = {
            errors: 0,
            properties: {},
            displayName: category.displayName,
          };
        }

        const isMasterComponent = allServiceModels[
          serviceNameModelMapping[serviceName.toUpperCase()]
        ]?.masterComponents?.some(
          (component: any) => component.componentName === category.name
        );

        const isSlaveComponent = allServiceModels[
          serviceNameModelMapping[serviceName.toUpperCase()]
        ]?.slaveComponents?.some(
          (component: any) => component.componentName === category.name
        );

        if (isMasterComponent || isSlaveComponent) {
          const hostValues = isMasterComponent
            ? fetchComponentHostNamesByComponent(
                allServiceModels[serviceNameModelMapping[serviceName]]
                  .masterComponents,
                category.name
              )
            : fetchComponentHostNamesByComponent(
                allServiceModels[serviceNameModelMapping[serviceName]]
                  .slaveComponents,
                category.name
              );
          updatedConfigProperties[serviceName][category.name].properties[
            category.name.toLowerCase() + "_hosts"
          ] = {
            propertyName: category.name.toLowerCase() + "_hosts",
            propertyDisplayname: category.displayName + "_hosts",
            propertyValue: hostValues,
            propertyAttributes: {
              type: "hosts",
              overridable: false,
            },
            previousValue: hostValues,
            value: hostValues,
            fileName: serviceName.toLowerCase() + "-site.xml",
            final: "false",
            savedFinal : "false",
            propertyType: [],
            type: serviceName.toLowerCase() + "-site",
            serviceName: serviceName, // Explicitly set the serviceName to ensure correct association
            isEditable: false,
          };
        }
      });
    });

    return updatedConfigProperties;
  };

  /**
   * Organize properties by categories based on the properties file map
   */
  const organizeByCategories = (
    configPropertiesCopy: ConfigPropertiesType,
    updatedConfigProperties: ConfigPropertiesType
  ) => {
    // Create a deep clone to avoid modifying the original
    const result = cloneDeep(updatedConfigProperties);

    Object.keys(propertiesFileMap).map((service: string) => {
      if (configPropertiesCopy[service]) {
        propertiesFileMap[service].forEach((property: any) => {
          const { serviceName, filename, name, category, options, displayType } = property;
          if (!category) {
            return;
          }
          const configType = filename.slice(0, -4);

          if(configPropertiesCopy[serviceName][configType]?.properties[name]?.tabName)
          {
            return;
          }

          if (configPropertiesCopy[serviceName][configType]?.properties[name]) {
            if (!category.includes("Advanced")) {
              if (!result[serviceName]) {
                result[serviceName] = {};
              }
              if (!result[serviceName][category]) {
                result[serviceName][category] = {
                  errors: 0,
                  properties: {},
                };
              }

              result[serviceName][category].properties[name] = cloneDeep(
                configPropertiesCopy[serviceName][configType].properties[name]
              );

              if(displayType) {
                result[serviceName][category].properties[name].propertyAttributes.type = displayType;
              }
              if(options) {
                result[serviceName][category].properties[name].propertyAttributes.options = options;
              }

              delete configPropertiesCopy[serviceName][configType].properties[
                name
              ];
            }
          }
        });
      }
    });

    return result;
  };

  /**
   * Add remaining properties to the updated configuration
   */
  const addRemainingProperties = (
    configPropertiesCopy: ConfigPropertiesType,
    updatedConfigProperties: ConfigPropertiesType
  ) => {
    // Create a deep clone to avoid modifying the original
    const result = cloneDeep(updatedConfigProperties);

    Object.keys(configPropertiesCopy).forEach((serviceName) => {
      if (!result[serviceName]) {
        result[serviceName] = {};
      }
      Object.keys(configPropertiesCopy[serviceName]).forEach((configType) => {
        if (!result[serviceName][configType]) {
          result[serviceName][configType] = {
            errors: 0,
            properties: {},
            displayName: !configType.includes("Custom")
              ? "Advanced " + configType
              : configType,
          };
        }
        Object.keys(
          configPropertiesCopy[serviceName][configType].properties
        ).forEach((propertyName) => {
          // Deep clone the property to avoid reference issues
          result[serviceName][configType].properties[propertyName] = cloneDeep(
            configPropertiesCopy[serviceName][configType].properties[
              propertyName
            ]
          );
        });
      });
    });

    return result;
  };

  /**
   * Remove properties that don't have corresponding values in propertyValues
   */
  const removePropertiesWithoutValues = (
    configPropertiesCopy: ConfigPropertiesType
  ) => {
    if (isEmpty(propertyValues)) {
      return configPropertiesCopy;
    }

    // Create a deep clone to avoid modifying the original
    const result = cloneDeep(configPropertiesCopy);

    Object.keys(result).forEach((serviceName) => {
      Object.keys(result[serviceName]).forEach((configType) => {
        const propertiesToDelete: string[] = [];

        Object.keys(result[serviceName][configType].properties).forEach(
          (propertyName) => {
            const property =
              result[serviceName][configType].properties[propertyName];

            // If the property wasn't found in propertyValues, mark it for deletion
            if (property.foundInPropertyValues === false) {
              propertiesToDelete.push(propertyName);
            }
          }
        );

        // Delete the properties that weren't found in propertyValues
        propertiesToDelete.forEach((propertyName) => {
          result[serviceName][configType].properties[propertyName].value = null;
          result[serviceName][configType].properties[propertyName].previousValue = null;
          result[serviceName][configType].properties[propertyName].isVisible = false;
          result[serviceName][configType].properties[propertyName].isHidden = true;


        });
      });
    });

    return result;
  };

  /**
   * Main function to get and process configuration properties
   */
  const getConfigProperties = async () => {
    // Initialize the configuration structure
    let configPropertiesCopy = initializeConfigStructure();

    configPropertiesCopy = addTabNames(configPropertiesCopy, themes);

    // Process default and override configurations
    // Each function returns a new object without modifying the input
    configPropertiesCopy = processDefaultConfigurations(configPropertiesCopy);
    configPropertiesCopy = processOverrideConfigurations(configPropertiesCopy);

    // Remove properties that don't have corresponding values in propertyValues
    configPropertiesCopy = removePropertiesWithoutValues(configPropertiesCopy);

    // Create the updated configuration structure with host information
    let updatedConfigProperties =
      addComponentHostInformation(configPropertiesCopy);

    // Organize properties by categories
    updatedConfigProperties = organizeByCategories(
      configPropertiesCopy,
      updatedConfigProperties
    );

    // Add remaining properties
    updatedConfigProperties = addRemainingProperties(
      configPropertiesCopy,
      updatedConfigProperties
    );

    // Apply service-specific overrides
    updatedConfigProperties = onLoadOverrides(updatedConfigProperties);
    updatedConfigProperties = setVisibilityForKerberosProperties(
      updatedConfigProperties
    );

    updatedConfigProperties = setPropertyIsEditable(
      updatedConfigProperties,
      { isDefault: configGroup === "Default" },
      selectedServices.includes("KERBEROS")

    );

    updatedConfigProperties = updateVisibilityByForeignKeys(updatedConfigProperties);
    updatedConfigProperties = updateVisibilityForDependsOn(updatedConfigProperties, themes,"default", services.map(service => service.ServiceInfo.service_name) );
    
    // Hide component configs based on availability (following Ember.js logic)
    updatedConfigProperties = hideComponentConfigsBasedOnAvailability(updatedConfigProperties, allServiceModels);
    
    updatedConfigProperties = validateAllProperties(updatedConfigProperties);

    loadRecommendationsForConfigOnLoad(updatedConfigProperties);

    // setConfigProperties(updatedConfigProperties);
  };

  const onLoadOverrides = (updatedConfigProperties: ConfigPropertiesType) => {
    const isRangerPresent = services.some(
      (service) => service.ServiceInfo.service_name === "RANGER"
    );

    // Create a deep clone to avoid modifying the original
    let configs = cloneDeep(updatedConfigProperties);

    if (!isRangerPresent) {
      configs = removeRangerConfigs(configs);
    }

    return configs;
  };

  const setVisibilityForKerberosProperties = (
    configProps: ConfigPropertiesType
  ) => {
    // Create a deep clone instead of a shallow copy
    const updatedConfigs = cloneDeep(configProps);

    if (serviceName !== "KERBEROS") {
      return updatedConfigs;
    }

    const kdcType = updatedConfigs?.["KERBEROS"]
      ? Object.values(updatedConfigs["KERBEROS"])
          .flatMap((section) => Object.values(section.properties))
          .find((prop) => prop.propertyName === "kdc_type")
      : "";

    if (!kdcType) {
      return updatedConfigs;
    }

    const updatePropertyVisibility = (
      propertyName: string,
      isVisible: boolean
    ) => {
      Object.values(updatedConfigs["KERBEROS"]).forEach((section) => {
        const property = section.properties[propertyName];
        if (property) {
          property.isVisible = isVisible;
        }
      });
    };

    switch (kdcType?.value?.toLowerCase()) {
      case messages["admin.kerberos.wizard.step1.option.manual"]:
        updatePropertyVisibility("kdc_hosts", false);
        updatePropertyVisibility("admin_server_host", false);
        updatePropertyVisibility("domains", false);
        break;

      case messages["admin.kerberos.wizard.step1.option.ad"]:
        updatePropertyVisibility("container_dn", true);
        updatePropertyVisibility("ldap_url", true);
        break;

      case messages["admin.kerberos.wizard.step1.option.ipa"]:
        updatePropertyVisibility("group", true);
        Object.values(updatedConfigs["KERBEROS"]).forEach((section) => {
          const manageKrb5Conf = section.properties["manage_krb5_conf"];
          const installPackages = section.properties["install_packages"];

          if (manageKrb5Conf) {
            manageKrb5Conf.value = "false";
          }
          if (installPackages) {
            installPackages.value = "false";
          }
        });
        updatePropertyVisibility("admin_server_host", false);
        updatePropertyVisibility("domains", false);
        break;
    }

    updatedConfigs["KERBEROS"]["KDC"].properties["Test.KDC.Connection"] = {
      propertyName: "Test.KDC.Connection",
      propertyDisplayname: " ",
      propertyDescription: "Test KDC Connection",
      propertyValue: "TEST KDC CONNECTION",
      propertyAttributes: {
        type: "button",
        overridable: false,
      },
      previousValue: "TEST KDC CONNECTION",
      value: "TEST KDC CONNECTION",
      final: "false",
      savedFinal : "false",
      isEditable: true,
    };

    return updatedConfigs;
  };




  const validationColumns = [
    {
      header: "Type",
      id: "type",
      accessorKey: "type",
      width: "10%",
    },
    {
      header: "Service",
      id: "service",
      accessorKey: "service",
    },
    {
      header: "Property",
      id: "property",
      accessorKey: "property",
      width: "25%",
    },
    {
      header: "Current Value",
      id: "currentValue",
      accessorKey: "currentValue",
    },
    {
      header: "Description",
      id: "message",
      accessorKey: "message",
      cell: (info: any) => {
        return <strong className="fw-bold">{info.row.original.message}</strong>;
      },
    },
  ];

  function getValidationsModalBody() {
    const criticalErrors = validationErrors.filter((error: any) => error.type === "Critical");
    const warnings = validationErrors.filter((error: any) => error.type === "Warning");
    
    return (
      <>
        {/* Critical Issues Section - matching Ember.js template */}
        {criticalErrors.length > 0 && (
          <>
            <p>
              You must correct the following critical issues before proceeding:
              <span className="badge bg-danger ms-2">{criticalErrors.length}</span>
            </p>
            <div className="config-validation-warnings">
              <Table data={criticalErrors} columns={validationColumns} />
            </div>
          </>
        )}
        
        {/* Highly Recommended Configurations Section - matching Ember.js template */}
        {warnings.length > 0 && (
          <>
            <p>
              Highly Recommended Configurations
              <span className="badge bg-warning ms-2">{warnings.length}</span>
            </p>
            <p className="recommendations-message fs-12">
              Please review the following recommended changes, and click on the
              property name to change its value.
            </p>
            <div className="config-validation-warnings">
              <Table data={warnings} columns={validationColumns} />
            </div>
          </>
        )}
      </>
    );
  }

  async function saveConfigs() {
    const saved = await saveStepConfigs();
    if (!saved) {
      return false;
    }

    setShowSaveConfigModal(false);
    setShowValidationErrorsModal(false);
    setShowUnsaveChangesModal(false);
    setServiceConfigVersionNote("");
    await getPropertiesValues();
    if (blocker.state === "blocked") {
      blocker.proceed();
    }
    return true;
  }

  if (!configsLoaded || loading) {
    return (
      <div className="d-flex justify-content-center align-items-center p-5">
        <Spinner />
      </div>
    );
  }
  return (
    <div>
      <Card>
        {loading ? (
          <div className="d-flex justify-content-center align-items-center p-5">
            <Spinner />
          </div>
        ) : !isComparing ? (
          <>
            <Modal
              isOpen={showValidationErrorsModal}
              onClose={() => setShowValidationErrorsModal(false)}
              modalTitle="Configurations"
              className="bg-operations-modal"
              modalBody={getValidationsModalBody()}
              successCallback={() => {
                saveConfigs();
              }}
              options={{
                okButtonText: "PROCEED ANYWAYS",
                okButtonVariant: "danger",
                modalSize: "modal-width",
                // Disable proceed button if there are critical errors (same as Ember.js behavior)
                okButtonDisabled: validationErrors.some((error: any) => error.type === "Critical"),
              }}
            />
            <div className="p-3 d-flex justify-content-between">
              <VersionsList
                serviceName={serviceName}
                setCurrentVersion={setCurrentVersion}
                onVersionChange={(versionNumber: any) => {
                    onVersionChange(versionNumber);
                }}
                configGroup={configGroup}
                isComparing={isComparing}
                setIsComparing={setIsComparing}
                setVersionCompared={setVersionCompared}
                versionToShow={selectedVersion || defaultVersionNumber || ""}
                onMakeCurrentComplete={() => {
                  getPropertiesValues();
                }}
              />
              <div className="d-flex ">
                <ChooseConfigGroup
                  serviceName={serviceName}
                  selectedConfigGroup={configGroup}
                  onConfigGroupChange={(configGroup: string) => {
                    setConfigGroup(configGroup);
                    setIsComparing(false);
                    setVersionCompared("");
                  }}
                  setShowManageConfigGroupModal={setShowManageConfigGroupModal}
                  configGroupsData={configGroupsData}
                  setConfigGroupsData={setConfigGroupsData}
                  refetchTrigger={refetchTrigger}
                />
              </div>
            </div>
            <Config
              configProperties={configProperties}
              setConfigProperties={setConfigProperties}
              configPropertiesData={configs}
              configSection="default"
              themeData={themes}
              servicesList={[serviceName]}
              configGroup={configGroup}
              setShowAddToGroupModal={setShowAddToGroupModal}
              setConfigGroup={setConfigGroup}
              version={selectedVersion}
              installedServices={services.map(
                (service) => service.ServiceInfo.service_name
              )}
            />
            <div className="p-3 d-flex justify-content-end">
              <Button
                className="mx-1"
                variant="outline-secondary"
                onClick={getPropertiesValues}
                disabled={isPending || isSubmitDisabled}
              >
                CANCEL
              </Button>

              {/* SERVICE.MODIFY_CONFIGS authorization check - like Ember.js ui/app/controllers/main/service/info/configs.js */}
              <AuthGuard
                requireAuthorization="SERVICE.MODIFY_CONFIGS"
                fallback={
                  <Button
                    variant="outline-primary"
                    disabled={true}
                    title="You need SERVICE.MODIFY_CONFIGS authorization to save configurations"
                  >
                    SAVE (No Permission)
                  </Button>
                }
              >
                <Button
                  variant="outline-primary"
                  disabled={isSubmitDisabled || isPending || !canModifyConfigs}
                  onClick={() => setShowSaveConfigModal(true)}
                >
                  SAVE
                </Button>
              </AuthGuard>
            </div>
          </>
        ) : (
          <div>
            <div className="p-2 d-flex justify-content-between bg-very-light">
              <div className="d-flex align-items-center">
                <FontAwesomeIcon
                  className="mx-2"
                  size="lg"
                  icon={faExchangeAlt}
                />
                Comparing changes in
                <VersionsList
                  serviceName={serviceName}
                  onVersionChange={(versionNumber: any) => {
                    setFirstVersion(versionNumber);
                  }}
                  configGroup={configGroup}
                  isComparing={isComparing}
                  setIsComparing={setIsComparing}
                  setVersionCompared={setFirstVersion}
                  versionToShow={defaultVersionNumber || ""}
                  firstVersion={firstVersion}
                  versionCompared={versionCompared}
                />
                <span className="mx-1">with</span>
                <VersionsList
                  serviceName={serviceName}
                  onVersionChange={(versionNumber: any) => {
                    setVersionCompared(versionNumber);
                  }}
                  configGroup={configGroup}
                  isComparing={isComparing}
                  setIsComparing={setIsComparing}
                  setVersionCompared={setVersionCompared}
                  versionToShow={versionCompared || ""}
                  firstVersion={firstVersion}
                  versionCompared={versionCompared}
                />
              </div>
              <ChooseConfigGroup
                serviceName={serviceName}
                selectedConfigGroup={configGroup}
                onConfigGroupChange={(configGroup: string) => {
                  setConfigGroup(configGroup);
                  setIsComparing(false);
                  setVersionCompared("");
                }}
                setShowManageConfigGroupModal={setShowManageConfigGroupModal}
                configGroupsData={configGroupsData}
                setConfigGroupsData={setConfigGroupsData}
              />
              <div>
                <FontAwesomeIcon
                  size="lg"
                  className="mx-2"
                  onClick={() => {
                    setIsComparing(false);
                    setVersionCompared("");
                  }}
                  icon={faXmark}
                />
              </div>
            </div>

            <ConfigsComparator
              version1={firstVersion}
              version2={versionCompared}
              defaultVersion={defaultVersionNumber || ""}
              clusterName={clusterName}
              serviceName={serviceName}
              configs={configs}
              themeData={themes}
              currentVersion={currentVersion}
            />
          </div>
        )}
      </Card>

      <AddToConfigGroupModal
        isOpen={showAddToGroupModal}
        onClose={() => setShowAddToGroupModal(false)}
        serviceName={serviceName}
        configGroupNames={
          propertyValues?.items
            ?.filter((item: any) => item.group_name !== "Default")
            .map((item: any) => item.group_name) || []
        }
        onConfigGroupSelect={(configGroup: string) => {
          setConfigGroup(configGroup);
        }}
        setShowManageConfigGroupModal={setShowManageConfigGroupModal}
      />
      {showManageConfigGroupModal && (
        <ManageConfigGroups
          isOpen={showManageConfigGroupModal}
          onClose={() => setShowManageConfigGroupModal(false)}
          clusterName={clusterName}
          successCallback={() => {
            setShowManageConfigGroupModal(false);
            // Trigger refresh of config groups in ChooseConfigGroup component
            setRefetchTrigger(prev => prev + 1);
          }}
          serviceName={serviceName}
        />
      )}
      <Modal
        isOpen={showSaveConfigsModal}
        onClose={() => setShowSaveConfigModal(false)}
        modalTitle="Save Configuration"
        modalBody={
          <div className="d-flex align-items-center">
            <Form.Label className="mx-2">Note</Form.Label>
            <Form.Control
              as="textarea"
              value={serviceConfigVersionNote}
              onChange={(e) => setServiceConfigVersionNote(e.target.value)}
              placeholder="What did you change?"
            />
          </div>
        }
        successCallback={async () => {
          setShowSaveConfigModal(false);
          await validateConfigProperties();
        }}
        options={{
          okButtonText: "SAVE",
          okButtonDisabled: !serviceConfigVersionNote,
          extraButtons: [
            {
              text: "DISCARD",
              onClick: () => {
                getPropertiesValues();
                setShowSaveConfigModal(false);
              },
              className: "bg-secondary-subtle border-light",
              variant: "outline-secondary",
            },
          ],
        }}
      />
      {showUnsaveChangesModal && 
      <Modal
        isOpen={showUnsaveChangesModal}
        onClose={() => setShowUnsaveChangesModal(false)}
        modalTitle={translate("common.warning")}
        modalBody={
          <div>
            <Alert variant="warning">{translate("services.service.config.exitPopup.body")}</Alert>
            <div className="d-flex align-items-center">
              <Form.Label className="mx-2">Note</Form.Label>
              <Form.Control
                as="textarea"
                value={serviceConfigVersionNote}
                onChange={(e) => setServiceConfigVersionNote(e.target.value)}
                placeholder="What did you change?"
              />
            </div>
          </div>
        }
        successCallback={async () => {
          await validateConfigProperties();
          setShowUnsaveChangesModal(false);
        }}
        options={{
          okButtonText: "SAVE",
          okButtonDisabled: !serviceConfigVersionNote,
          extraButtons: [
            {
              text: translate("common.discard"),
              variant: "warning",
              onClick: () => {
                if (blocker.state === 'blocked') {
                  blocker.proceed();
                }
                setShowUnsaveChangesModal(false)
              },
              order: 1,
            },
          ],
        }}
      />
      }
    </div>
  );
}
