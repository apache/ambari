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
import WizardApi from "../../../api/wizardApi";
import Spinner from "../../../components/Spinner";
import CredentialsTab, { processDataForCredentialsTab } from "./CredentialsTab";
import AccountsTab from "./AccountsTab";
import {
  Alert,
  Button,
  Modal as BootstrapModal,
  Nav,
  Row,
  Tab,
} from "react-bootstrap";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faAlignJustify,
  faFolderOpen,
  faLock,
  faUser,
  faWrench,
} from "@fortawesome/free-solid-svg-icons";
import { cloneDeep, flatten, get, isEmpty, isObject, map, set } from "lodash";
import { ConfigPropertiesType } from "../../CommonConfigs/types";
import RestAllTabs from "./RestAllTabs";
import WizardFooter from "../../../components/StepWizard/WizardFooter";
import { hdfs_properties } from "../../../data/configs/services/hdfs_properties";
import { alert_notifications } from "../../../data/configs/alert_notifications";
import { yarn_properties } from "../../../data/configs/services/yarn_properties";
import { hbase_properties } from "../../../data/configs/services/hbase_properties";
import { hive_properties } from "../../../data/configs/services/hive_properties";
import { ranger_properties } from "../../../data/configs/services/ranger_properties";
import { mapreduce2_properties } from "../../../data/configs/services/mapreduce2_properties";
import { tez_properties } from "../../../data/configs/services/tez_properties";
import { zookeeper_properties } from "../../../data/configs/services/zookeeper_properties";
import { ambari_metrics_properties } from "../../../data/configs/services/ambari_metrics_properties";
import { ActionTypes } from "../clusterStore/types";
import { ContextWrapper } from "..";
import {
  addTabNames,
  buildConfigsJSON,
  formatPropertyValue,
  getConfigByName,
  getConfigCategories,
  getConfigPropertyByName,
  removeRangerConfigs,
  shouldSupportFinal,
  updateVisibilityByForeignKeys,
  updateVisibilityForDependsOn,
  validateAllProperties,
} from "../../CommonConfigs/ConfigUtils";
import wizardSteps from "../wizardSteps";
import { BootStatus } from "../Step3";
import ConfigsApi from "../../../api/configsApi";
import WizardConfigInitializer from "../../../Initializers/WizardConfigInitializer";
import useServiceComponents from "../hooks/useServiceComponents";
import {
  blueprintUtils,
  isShownOnInstallerSlaveClientPage,
  minToInstall,
} from "../utils";
import { getStepData as getStepDataFromUtil } from "../../../Utils/Utility";
import useEnhancedConfigs from "../../../hooks/useEnhancedConfigs";
import { useDebounce } from "../../../hooks/useDebounce";
import { AppContext } from "../../../store/context";
import DependentConfigurationsModal from "../../../components/DependentConfigurationsModal";
import { ServiceContext } from "../../../store/ServiceContext";
import { serviceNameModelMapping } from "../../../constants";
import { fetchComponentHostNamesByComponent } from "../../CommonConfigs/ConfigUtils";
import {
  nextAddServiceStep,
  previousAddServiceStep,
} from "../../Services/AddServiceWizard/addServiceNavigation";
import { shouldWarnBeforeSkippingPreInstallChecks } from "../preInstallChecks";
import {
  classifyDefaultThemeResponse,
  describeThemeRequestError,
  ThemeLoadNotice,
} from "../../CommonConfigs/themeLoadUtils";
import { getCategoryClientErrors } from "./categoryValidation";

type PropTypes = {
  wizardName?: string;
};

const createConfigurationStepPayload = (
  step: string,
  configProperties: ConfigPropertiesType,
  themes: unknown,
  configs: unknown,
  stackLevelConfigs: unknown,
  preInstallChecksWereRun: boolean,
  selectedTab: string,
  selectedServicesByTab: Record<string, string>,
) => ({
  step,
  data: {
    configProperties,
    themes,
    configs,
    stackLevelConfigs,
    preInstallChecksWereRun,
    navigation: {
      selectedTab,
      selectedServicesByTab,
    },
  },
});

export const findNextEnabledConfigurationTab = (
  currentTab: string,
  tabMapping: Record<string, { nextTab?: string }>,
  disabledTabs: string[],
): string | undefined => {
  let nextTab = tabMapping[currentTab]?.nextTab;
  const visited = new Set<string>();
  while (nextTab && disabledTabs.includes(nextTab) && !visited.has(nextTab)) {
    visited.add(nextTab);
    nextTab = tabMapping[nextTab]?.nextTab;
  }
  return nextTab;
};

export const findPreviousEnabledConfigurationTab = (
  currentTab: string,
  tabMapping: Record<string, { nextTab?: string }>,
  disabledTabs: string[],
): string | undefined => {
  let previousTab = Object.keys(tabMapping).find(
    (tabName) => tabMapping[tabName]?.nextTab === currentTab,
  );
  const visited = new Set<string>();
  while (
    previousTab &&
    disabledTabs.includes(previousTab) &&
    !visited.has(previousTab)
  ) {
    visited.add(previousTab);
    const disabledTab = previousTab;
    previousTab = Object.keys(tabMapping).find(
      (tabName) => tabMapping[tabName]?.nextTab === disabledTab,
    );
  }
  return previousTab;
};

export const findInitialConfigurationTab = (disabledTabs: string[]) => {
  if (!disabledTabs.includes("credentials")) return "credentials";
  if (!disabledTabs.includes("databases")) return "databases";
  if (!disabledTabs.includes("directories")) return "directories";
  return "allConfigurations";
};

const preserveEditedConfigValues = (
  nextConfigs: ConfigPropertiesType,
  currentConfigs: ConfigPropertiesType,
): ConfigPropertiesType => {
  const currentByPath = new Map<string, any>();
  Object.entries(currentConfigs).forEach(([serviceName, sections]) => {
    Object.entries(sections).forEach(([sectionName, section]) => {
      Object.values(section.properties || {}).forEach((property) => {
        const configType = property.type || sectionName;
        currentByPath.set(
          `${serviceName}/${configType}/${property.propertyName}`,
          property,
        );
      });
    });
  });

  const result = cloneDeep(nextConfigs);
  Object.entries(result).forEach(([serviceName, sections]) => {
    Object.entries(sections).forEach(([sectionName, section]) => {
      Object.values(section.properties || {}).forEach((property) => {
        const configType = property.type || sectionName;
        const current = currentByPath.get(
          `${serviceName}/${configType}/${property.propertyName}`,
        );
        if (!current) return;
        [
          "value",
          "confirmPassword",
          "final",
          "overrideValues",
          "errorMessage",
          "hasError",
          "warnMessage",
        ].forEach((field) => {
          if (field in current) property[field] = cloneDeep(current[field]);
        });
      });
    });
  });
  return result;
};

export default function Step7({ wizardName = "clusterCreation" }: PropTypes) {
  const { Context } = useContext(ContextWrapper);
  const { ambariProperties, clusterName, supports } = useContext(AppContext);
  const { allServiceModels } = useContext(ServiceContext);
  const {
    state,
    dispatch,
    installedHosts,
    installedServices,
    flushStateToDb,
    stepWizardUtilities: { currentStep, handleNextImperitive, jumpToStep },
  } = useContext(Context) as any;
  const getStepData = (stepName: string, dataKey: string) => {
    const stepData = get(state, `${wizardName}Steps.${stepName}.data`, {});
    return get(stepData, dataKey, "");
  };
  const addServiceFlow = get(
    state,
    "addServiceSteps.SERVICES.data.addServiceFlow",
    {},
  );
  const storedConfigProperties =
    getStepData("CONFIGURATION", "configProperties") || {};
  const storedNavigation =
    getStepData("CONFIGURATION", "navigation") || {};
  const [themes, setThemes] = useState<any>(
    getStepData("CONFIGURATION", "themes") || {}
  );
  const [themesSettled, setThemesSettled] = useState(
    !isEmpty(getStepData("CONFIGURATION", "themes"))
  );
  const [configs, setConfigs] = useState<any>(
    getStepData("CONFIGURATION", "configs") || {}
  );
  const [stackLevelConfigs, setStackLevelConfigs] = useState<any>(
    getStepData("CONFIGURATION", "stackLevelConfigs") || {}
  );
  const [loading, setLoading] = useState<boolean>(true);
  const [selectedTab, setSelectedTab] = useState<string>(() =>
    !isEmpty(storedConfigProperties)
      ? "allConfigurations"
      : get(storedNavigation, "selectedTab", "credentials"),
  );
  const [selectedServicesByTab, setSelectedServicesByTab] = useState<
    Record<string, string>
  >(() => get(storedNavigation, "selectedServicesByTab", {}));
  const [disabledTabs, setDisabledTabs] = useState<string[]>([]);
  const [configProperties, setConfigProperties] = useState(
    storedConfigProperties
  );
  const configPropertiesRef = useRef(configProperties);
  const themeRequestId = useRef(0);
  //@ts-ignore
  const [validationErrors, setValidationErrors] = useState<any>([]);
  const [isNextEnabled, setIsNextEnabled] = useState(true);
  const [showDependentConfigsModal, setShowDependentConfigsModal] =
    useState(false);
  const [dependentConfigsToShow, setDependentConfigsToShow] = useState<any[]>(
    []
  );

  const [configPropertiesLoaded, setConfigPropertiesLoaded] = useState(false);
  const [propertyValues, setPropertyValues] = useState<any>({});
  const [preInstallChecksWereRun, setPreInstallChecksWereRun] = useState(
    Boolean(getStepData("CONFIGURATION", "preInstallChecksWereRun")),
  );
  const [showPreInstallChecks, setShowPreInstallChecks] = useState(false);
  const [showSkippedChecksWarning, setShowSkippedChecksWarning] = useState(false);
  const [themeLoadNotice, setThemeLoadNotice] =
    useState<ThemeLoadNotice | null>(null);
  const [themeRetrying, setThemeRetrying] = useState(false);

  useEffect(() => {
    configPropertiesRef.current = configProperties;
  }, [configProperties]);

  useEffect(() => () => {
    themeRequestId.current += 1;
  }, []);

  const hostsData = get(
    state,
    `${wizardName}Steps.${wizardSteps[3].name}.data.hosts`,
    []
  );

  const hostsList =
    wizardName === "addService"
      ? installedHosts
      : hostsData
          .filter((host: any) => {
            return host.bootStatus === BootStatus.REGISTERED;
          })
          .map((host: any) => host.name);

  const mastersData: any = getStepData("MASTERS", "mastersData");
  const slavesData: any = getStepData(
    "SLAVES_AND_CLIENTS",
    "serviceComponents"
  );

  const versionStepData = get(state, `${wizardName}Steps.VERSION.data`, {});
  const stackVersion = get(
    versionStepData,
    "selectedVersion.stack_version",
    ""
  );
  const stackName = get(versionStepData, "selectedStack.stack_name", "");

  const { 
    processRecommendations, 
    loadAddServiceRecommendations, 
    recommendedChanges,
    setRecommendedChanges,
  } = useEnhancedConfigs(
    setConfigProperties,
    undefined, // serviceName
    installedServices, // installedServices
    undefined, // recommendationsDataToSend
    wizardName, // controllerName - THIS WAS MISSING!
    stackName, // STACK
    stackVersion, // VERSION
    hostsList // HOSTS
  );

  const servicesData: any = get(
    state,
    `${wizardName}Steps.SERVICES.data.services`,
    {}
  );

  useEffect(() => {
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: createConfigurationStepPayload(
        currentStep.name,
        configProperties,
        themes,
        configs,
        stackLevelConfigs,
        preInstallChecksWereRun,
        selectedTab,
        selectedServicesByTab,
      ),
    });
  }, [
    JSON.stringify(configProperties),
    configs,
    themes,
    stackLevelConfigs,
    preInstallChecksWereRun,
    selectedTab,
    selectedServicesByTab,
  ]);

  const initialServiceComponents = get(
    state,
    `${wizardName}Steps.SLAVES_AND_CLIENTS.data.serviceComponents`,
    {}
  );

  const validationsRef = useRef({});

  const {
    serviceComponents,
    hosts,
    getClientComponents,
    allServiceComponentsList,
    ComponentCategory,
  } = useServiceComponents(wizardName || "", initialServiceComponents);
  const services = Object.keys(servicesData).filter((service) => {
    return servicesData[service].selected;
  });

  const conditionServices = [
    ...new Set([...(installedServices || []), ...services]),
  ];

  // Kerberos contributes condition/config context but does not get an ordinary
  // Add Service configuration tab.
  const filteredServices =
    wizardName === "addService"
      ? conditionServices.filter((service) => service !== "KERBEROS")
      : services;


  const propertiesFileMap: { [key: string]: any } = {
    HDFS: hdfs_properties,
    YARN: yarn_properties,
    HIVE: hive_properties,
    HBASE: hbase_properties,
    RANGER: ranger_properties,
    MAPREDUCE2: mapreduce2_properties,
    TEZ: tez_properties,
    ZOOKEEPER: zookeeper_properties,
    AMBARI_METRICS: ambari_metrics_properties,
  };

  // useEffect(() => {
  //   setValidationErrors(validationsRef.current);
    
  //   // Disable Next button if there are critical errors (same as Ember.js behavior)
  //   const hasCriticalErrors =
  //     (validationsRef.current as any)?.criticalErrors?.length > 0;
    
  //   // For cluster installation, be more lenient with validation errors
  //   // Only disable if there are actual critical errors that would prevent installation
  //   if (wizardName === "clusterCreation") {
  //     // During cluster installation, only disable for actual blocking errors
  //     setIsNextEnabled(!hasCriticalErrors);
  //   } else {
  //     // For other wizards (like add service), use stricter validation
  //     setIsNextEnabled(!hasCriticalErrors);
  //   }
  // }, [validationsRef.current, wizardName]);

  const validateConfigProperties = useDebounce(validatedConfigProperties, 500);

  async function validatedConfigProperties() {
    const recommendations: any = getValidationRequestBody();
    recommendations.blueprint.configurations = buildConfigsJSON(
      configProperties,
      wizardName === "addService"
    );
    const response = await ConfigsApi.validateConfigProperties(
      stackName,
      stackVersion,
      {
        hosts: hostsList,
        recommendations,
        services: conditionServices,
        validate: "configurations",
      }
    );
    const {
      resources: [validationResult],
    } = response;
    const { items } = validationResult;
    const criticalErrors = items.filter(
      (item: any) => item.level === "NOT_APPLICABLE"
    );
    const warnings = items.filter((item: any) => item.level === "WARN");
    const validationErrorsCopy: any = cloneDeep(validationsRef.current);
    const processedCriticalItems = new Map(); // Track processed items with their details to avoid duplicates
    const detailedCriticalErrors = criticalErrors
      .map((error: any) => {
        const property = getConfigPropertyByName(
          error["config-name"],
          configProperties
        );
        
        // Apply Ember.js filtering logic: only show validation errors for properties that exist and are visible
        if (property && property.isVisible !== false && !property.isHidden) {
          // Create unique key for deduplication using property ID (same as Ember.js)
          const propertyId = `${property.propertyName}_${
            property.fileName || property.type + ".xml"
          }`;
          
          // Skip if we've already processed this property (deduplication like Ember.js)
          if (processedCriticalItems.has(propertyId)) {
            return null;
          }
          
          const processedItem = {
            ...error,
            serviceName: property?.serviceName,
            propertyName: error["config-name"],
            value: property?.value,
            type: "Critical", // Map NOT_APPLICABLE to Critical (same as Ember.js)
          };
          
          processedCriticalItems.set(propertyId, processedItem);
          return processedItem;
        }
        return null; // Filter out properties that don't exist or are not visible
      })
      .filter(Boolean); // Remove null entries
    
    set(validationErrorsCopy, "criticalErrors", detailedCriticalErrors);
    
    const processedWarningItems = new Map(); // Track processed items with their details to avoid duplicates
    const detailedWarnings = warnings
      .map((warning: any) => {
        const property = getConfigPropertyByName(
          warning["config-name"],
          configProperties
        );
        
        // Apply Ember.js filtering logic: only show validation errors for properties that exist and are visible
        if (property && property.isVisible !== false && !property.isHidden) {
          // Create unique key for deduplication using property ID (same as Ember.js)
          const propertyId = `${property.propertyName}_${
            property.fileName || property.type + ".xml"
          }`;
          
          // Skip if we've already processed this property (deduplication like Ember.js)
          if (processedWarningItems.has(propertyId)) {
            return null;
          }
          
          const processedItem = {
            ...warning,
            serviceName: property?.serviceName,
            propertyName: warning["config-name"],
            value: property?.value,
            type: "Warning", // Map WARN to Warning (same as Ember.js)
          };
          
          processedWarningItems.set(propertyId, processedItem);
          return processedItem;
        }
        return null; // Filter out properties that don't exist or are not visible
      })
      .filter(Boolean); // Remove null entries
    
    set(validationErrorsCopy, "warnings", detailedWarnings);
    validationsRef.current = validationErrorsCopy;
    setValidationErrors(validationErrorsCopy);
  }
  function validateClientSideValidations() {
    //get all the keys which have hasError nn empty string
    const errorProperties: any = [];
    const validationErrorsCopy: any = cloneDeep(validationsRef.current);
    Object.keys(configProperties).forEach((serviceName) => {
      Object.keys(configProperties[serviceName]).forEach((configType) => {
        Object.keys(
          configProperties[serviceName][configType].properties
        ).forEach((propertyName) => {
          const property =
            configProperties[serviceName][configType].properties[propertyName];
          if (property.hasError || property.errorMessage) {
            errorProperties.push(property);
          }
        });
      });
    });
    set(validationErrorsCopy, "clientSideErrors", errorProperties);
    validationsRef.current = validationErrorsCopy;
    setValidationErrors(validationErrorsCopy);
  }

  useEffect(() => {
    if (selectedTab === "credentials") return;
    if (selectedTab === "databases") {
      setIsNextEnabled(
        getCategoryClientErrors({
          configProperties,
          selectedTab,
          serviceNames: conditionServices,
          themes,
        }).length === 0,
      );
      return;
    }
    if (selectedTab === "allConfigurations") {
      setIsNextEnabled(
        getCategoryClientErrors({
          configProperties,
          selectedTab,
          serviceNames: conditionServices,
          themes,
        }).length === 0 &&
          (validationErrors.criticalErrors?.length || 0) === 0,
      );
      return;
    }
    setIsNextEnabled(true);
  }, [configProperties, selectedTab, themes, validationErrors]);
  useEffect(() => {
    if (configPropertiesLoaded) {
      if (wizardName === "clusterCreation") {
        // Load recommendations for cluster creation - this was missing the proper call
        loadConfigRecommendations();
      } else if (wizardName === "addService") {
        const newlyAddingServices = services.filter(service => 
          !installedServices?.includes(service) && service !== "MISC"
        );
        
        loadAddServiceRecommendations(
          configProperties,
          newlyAddingServices, // Pass only newly adding services
          getValidationRequestBody()
        );
      }
    }
  }, [configPropertiesLoaded]);

  // Monitor recommended changes and prepare data for add service wizard
  // This implements the Ember.js filtering logic from changedProperties and filterRequiredChanges
  useEffect(() => {
    if (
      wizardName === "addService" &&
      recommendedChanges &&
      Object.keys(recommendedChanges).length > 0
    ) {
      const recommendations = Object.values(recommendedChanges);
      
      // Apply Ember.js changedProperties filtering logic
      // Filter recommendations based on config group (Default group for wizard)
      const changedProperties = recommendations.filter((dp: any) => {
        // In wizard, we use default config group, so filter for "Default" configGroup
        return dp.configGroup && dp.configGroup.includes("Default");
      });
      
      // Apply Ember.js filterRequiredChanges logic
      // Split into editable (recommendedChanges) and non-editable (requiredChanges)
      const recommendedChanges_filtered = changedProperties.filter(
        (recommendation: any) => {
        return recommendation.isEditable !== false;
        }
      );
      
      const requiredChanges = changedProperties.filter(
        (recommendation: any) => {
        return recommendation.isEditable === false;
        }
      );
      
      // Combine both types of changes (matching Ember.js showChangedDependentConfigs)
      const allChanges = [...recommendedChanges_filtered, ...requiredChanges];
      
      if (allChanges.length > 0) {
        // Further filter to only show changes for installed services (dependent configs)
        const dependentChanges = allChanges.filter((change: any) => {
          return installedServices.includes(change.serviceName);
        });

        if (dependentChanges.length > 0) {
          const formattedChanges = dependentChanges.map((change: any) => ({
            propertyName: change.propertyName,
            serviceName: change.serviceName,
            serviceDisplayName: change.serviceName,
            configGroup: change.configGroup || "Default",
            propertyFileName: change.fileName, // Use fileName instead of propertyFileName
            initialValue: change.initialValue,
            originalValue: change.initialValue, // Add originalValue for backward compatibility
            recommendedValue: change.recommendedValue,
            saveRecommended: change.saveRecommended !== false,
            isEditable: change.isEditable !== false,
          }));
          
          setDependentConfigsToShow(formattedChanges);
        } else {
          setDependentConfigsToShow([]);
        }
      } else {
        setDependentConfigsToShow([]);
      }
    } else {
      setDependentConfigsToShow([]);
    }
  }, [recommendedChanges, wizardName, installedServices]);

  const getSlaveBlueprint = () => {
    const clientComponents = getClientComponents();
    let currentHostComponentMapping: any = [];
    for (let host of Object.keys(hosts)) {
      const correspondingHostMapping = serviceComponents.find((sc: any) => {
        return sc.hostname === host;
      });
      if (correspondingHostMapping) {
        const selectedComponents = correspondingHostMapping.checkboxes
          .filter((cb: any) => {
            return cb.checked && cb.label !== "CLIENT";
          })
          ?.map((comp: any) => {
            return { name: comp.label };
          });
        const isClientChecked = !!correspondingHostMapping.checkboxes.find(
          (selectedComponent: any) =>
            selectedComponent.label === "CLIENT" && selectedComponent.checked
        );
        if (isClientChecked) {
          currentHostComponentMapping.push({
            hostname: host,
            components: [
              ...selectedComponents,
              ...clientComponents.map((component: any) => {
                return {
                  name: component,
                };
              }),
            ],
          } as never);
        } else {
          currentHostComponentMapping.push({
            hostname: host,
            components: [...selectedComponents],
          } as never);
        }
      }
    }
    const slaveBlueprint = blueprintUtils.getBlueprint(
      Object.keys(hosts),
      currentHostComponentMapping
    );
    return slaveBlueprint;
  };

  const getSelectedMastersGroupedMapping = () => {
    const mastersDataLocal = getStepDataFromUtil(
      state,
      "MASTERS",
      "mastersData",
      `${wizardName}Steps`
    );

    const hostComponentMapping: any = [];
    if (mastersDataLocal) {
      mastersDataLocal.forEach((selectedMaster: any) => {
        hostComponentMapping.push({
          hostname: selectedMaster.host_name,
          components: selectedMaster.masterServices.map(
            (selectedComponent: any) => {
              return {
                name: selectedComponent.component,
              };
            }
          ),
        });
      });
    }
    return hostComponentMapping;
  };

  const getInvisibleSlaveAndClients = () => {
    return allServiceComponentsList.filter((serviceComponent: any) => {
      if (
        (serviceComponent["component_category"] === ComponentCategory.SLAVE &&
          !isShownOnInstallerSlaveClientPage(serviceComponent)) ||
        (serviceComponent["component_category"] === ComponentCategory.CLIENT &&
          minToInstall(serviceComponent.cardinality) === Infinity)
      ) {
        return serviceComponent;
      }
    });
  };

  const getPropertiesValues = async () => {
    setLoading(true);
    try {
      const response = await ConfigsApi.getConfigValues(
        clusterName,
        installedServices.join(",")
      );
      return response;
    } catch {
      console.error("Error fetching wizard property values.");
    } finally {
      setLoading(false);
    }
  };

  const getClusterEnvValues = async () => {
    if (wizardName !== "addService") {
      return null;
    }
    
    try {
      const response = await ConfigsApi.loadConfigTags(clusterName);
      const clusterEnvTag =
        response?.Clusters?.desired_configs?.["cluster-env"]?.tag;
      
      if (clusterEnvTag) {
        const clusterEnvConfigs = await ConfigsApi.getConfigsByTags(
          clusterName,
          `(type=cluster-env&tag=${clusterEnvTag})`
        );
        return clusterEnvConfigs;
      }
    } catch {
      console.error("Error fetching cluster environment values.");
    }
    return null;
  };
  


  const getValidationRequestBody = () => {
    const slaveBlueprint = getSlaveBlueprint();
    const hostnames = Object.keys(hosts);
    //@ts-ignore
    const invisibleSlavesAndClients = map(
      getInvisibleSlaveAndClients(),
      "component_name"
    );
    const masterBlueprint = blueprintUtils.getBlueprint(
      hostnames,
      getSelectedMastersGroupedMapping()
    );
    const mergedBlueprints = blueprintUtils.mergeBlueprints(
      cloneDeep(slaveBlueprint),
      cloneDeep(masterBlueprint)
    );
    return mergedBlueprints;
  };

  const tabMapping: { [key: string]: any } = {
    credentials: {
      title: "CREDENTIALS",
      icon: <FontAwesomeIcon icon={faLock} />,
      component: (
        <CredentialsTab
          themes={themes}
          configs={configs}
          configProperties={configProperties}
          setConfigProperties={setConfigProperties}
          setIsNextEnabled={setIsNextEnabled}
        />
      ),
      nextTab: "databases",
    },
    databases: {
      title: "DATABASES",
      icon: <FontAwesomeIcon icon={faAlignJustify} />,
      nextTab: "directories",
      component: (
        <RestAllTabs
          themes={themes}
          configs={configs}
          configProperties={configProperties}
          setConfigProperties={setConfigProperties}
          services={services}
          tabName="database"
          recommendationsDataToSend={getValidationRequestBody()}
          stack={stackName}
          stackVersion={stackVersion}
          hosts={hostsList}
          wizardName={wizardName}
          selectedService={selectedServicesByTab.databases}
          onServiceChange={(serviceName) =>
            setSelectedServicesByTab((current) => ({
              ...current,
              databases: serviceName,
            }))
          }
          conditionServices={conditionServices}
        />
      ),
    },
    directories: {
      title: "DIRECTORIES",
      nextTab: "accounts",
      icon: <FontAwesomeIcon icon={faFolderOpen} />,
      component: (
        <RestAllTabs
          themes={themes}
          configs={configs}
          configProperties={configProperties}
          setConfigProperties={setConfigProperties}
          services={services}
          tabName="directories"
          recommendationsDataToSend={getValidationRequestBody()}
          stack={stackName}
          stackVersion={stackVersion}
          hosts={hostsList}
          wizardName={wizardName}
          selectedService={selectedServicesByTab.directories}
          onServiceChange={(serviceName) =>
            setSelectedServicesByTab((current) => ({
              ...current,
              directories: serviceName,
            }))
          }
          conditionServices={conditionServices}
        />
      ),
    },
    accounts: {
      title: "ACCOUNTS",
      nextTab: "allConfigurations",
      icon: <FontAwesomeIcon icon={faUser} />,
      component: (
        <AccountsTab
          configProperties={configProperties}
          setConfigProperties={setConfigProperties}
          services={services}
        />
      ),
    },
    allConfigurations: {
      title: "ALL CONFIGURATIONS",
      icon: <FontAwesomeIcon icon={faWrench} />,
      component: (
        <RestAllTabs
          themes={themes}
          configs={configs}
          configProperties={configProperties}
          setConfigProperties={setConfigProperties}
          validationErrors={validationErrors}
          services={services}
          tabName="default"
          recommendationsDataToSend={getValidationRequestBody()}
          stack={stackName}
          stackVersion={stackVersion}
          hosts={hostsList}
          wizardName={wizardName}
          selectedService={selectedServicesByTab.allConfigurations}
          onServiceChange={(serviceName) =>
            setSelectedServicesByTab((current) => ({
              ...current,
              allConfigurations: serviceName,
            }))
          }
          conditionServices={conditionServices}
        />
      ),
    },
  };

  useEffect(() => {
    const loadAllData = async () => {
      setLoading(true);
      try {
        const requiredDataPromises = [];
        
        if (isEmpty(configs)) {
          requiredDataPromises.push(getConfigurations());
        }
        if (isEmpty(stackLevelConfigs)) {
          requiredDataPromises.push(getStackLevelConfigurations());
        }

        const themePromise = isEmpty(themes)
          ? getThemes()
          : Promise.resolve();
        await Promise.all([...requiredDataPromises, themePromise]);
      } catch {
        console.error("Error loading required wizard configuration data.");
      } finally {
        setLoading(false);
      }
    };
    
    loadAllData();
  }, []);

  useEffect(() => {
    if (!isEmpty(configs)) {
      const tempDisabledTabs: string[] = [];
      const credentialsData = processDataForCredentialsTab(configs, themes);
      if (!credentialsData.length) {
        tempDisabledTabs.push("credentials");
      }

      const databasesData: any[] = [];
      get(themes, "items", []).forEach((item: any) => {
        get(item, "themes", []).forEach((theme: any) => {
          if (get(theme, "ThemeInfo.theme_data.Theme.name") === "database") {
            databasesData.push(get(theme, "ThemeInfo.theme_data.Theme"));
          }
        });
      });
      if (!databasesData.length) {
        tempDisabledTabs.push("databases");
      }

      const hasDirectoriesData = get(themes, "items", []).some((item: any) =>
        get(item, "themes", []).some(
          (theme: any) =>
            get(theme, "ThemeInfo.theme_data.Theme.name") === "directories"
        )
      );
      if (!hasDirectoriesData) {
        tempDisabledTabs.push("directories");
      }

      setDisabledTabs(tempDisabledTabs);
      setSelectedTab((currentTab) => {
        if (!tempDisabledTabs.includes(currentTab)) return currentTab;
        return findInitialConfigurationTab(tempDisabledTabs);
      });
    }
  }, [configs, themes]);

  useEffect(() => {
    const fetchInitialData = async () => {
      if (wizardName === "addService") {
        try {
          const response = await getPropertiesValues();
          setPropertyValues(response);
        } catch {
          console.error("Error loading wizard property values.");
        }
      }
    };
    fetchInitialData();
  }, [wizardName]);

  useEffect(() => {
    if (!themesSettled) {
      return;
    }
    if (wizardName === "addService") {
      // For addService, wait for configs and propertyValues
      if (!isEmpty(configs) && !isEmpty(propertyValues) && isEmpty(configProperties)) {
        getConfigProperties();
      }
    } else {
      // For cluster creation, wait for configs and stackLevelConfigs
      if (!isEmpty(configs) && !isEmpty(stackLevelConfigs) && isEmpty(configProperties)) {
        getConfigProperties();
      }
    }
  }, [configs, stackLevelConfigs, propertyValues, themesSettled, wizardName]);

  useEffect(() => {
    if (!isEmpty(configProperties)) {
      setConfigPropertiesLoaded(true);
    }

    if (
      configPropertiesLoaded &&
      (wizardName === "clusterCreation" || wizardName === "addService")
    ) {
      validateClientSideValidations();
      validateConfigProperties();
    }
  }, [configProperties, configPropertiesLoaded]);

  const loadConfigRecommendations = async () => {
    try {
      let recommendationsInPayload: any = getValidationRequestBody();
      recommendationsInPayload.blueprint.configurations = buildConfigsJSON(
        configProperties,
        wizardName === "addService"
      );

      const dataToSend = {
        autoComplete: false,
        clusterId: null,
        configsResponse: false,
        recommend: "configurations",
        hosts: hostsList,
        recommendations: recommendationsInPayload,
        services: services,
        user_context: { operation: "ClusterCreate" },
      };

      const response = await ConfigsApi.getRecommendations(
        stackName,
        stackVersion,
        dataToSend
      );

      processRecommendations(response, configProperties);
    } catch {
      console.error("Error loading configuration recommendations.");
      // Don't fail the entire process if recommendations fail
    }
  };

  const getConfigDependencies = (configProperties: ConfigPropertiesType) => {
    let dependencies: any = {};
    let hiveMetastore = getConfigByName(
      "hive.metastore.uris",
      "General",
      "HIVE",
      configProperties
    );
    let clientPort = getConfigByName(
      "clientPort",
      "ZOOKEEPER_SERVER",
      "ZOOKEEPER",
      configProperties
    );
    let atlasTls = getConfigByName(
      "atlas.enableTLS",
      "application-properties",
      "ATLAS",
      configProperties
    );
    let atlasHttpPort = getConfigByName(
      "atlas.server.http.port",
      "application-properties",
      "ATLAS",
      configProperties
    );
    let atlasHttpsPort = getConfigByName(
      "atlas.server.https.port",
      "application-properties",
      "ATLAS",
      configProperties
    );

    if (hiveMetastore)
      dependencies["hive.metastore.uris"] = hiveMetastore.recommendedValue;
    if (clientPort) dependencies.clientPort = clientPort.recommendedValue;
    if (atlasTls) dependencies["atlas.enableTLS"] = atlasTls.recommendedValue;
    if (atlasHttpPort)
      dependencies["atlas.server.http.port"] = atlasHttpPort.recommendedValue;
    if (atlasHttpsPort)
      dependencies["atlas.server.https.port"] = atlasHttpsPort.recommendedValue;
    return dependencies;
  };
  const initializeValues = (configProperties: ConfigPropertiesType) => {
    // Create a deep clone to avoid modifying the original
    const result = cloneDeep(configProperties);

    let localDB: any = {
      hosts: hostsList,
      masterComponentHosts: mastersData,
      slaveComponentHosts: slavesData,
      selectedStack: {},
    };

    // For add service mode, we need to include existing installed components
    if (wizardName === "addService" && allServiceModels) {
      // Build master component hosts from existing service models
      const existingMasterComponents: any[] = [];
      
      Object.keys(allServiceModels).forEach(serviceKey => {
        const serviceModel = allServiceModels[serviceKey];
        if (serviceModel.masterComponents) {
          serviceModel.masterComponents.forEach((component: any) => {
            if (component.hostComponents) {
              component.hostComponents.forEach((hostComponent: any) => {
                existingMasterComponents.push({
                  component: component.componentName,
                  hostName: hostComponent.HostRoles.host_name,
                  isInstalled: true,
                });
              });
            }
          });
        }
      });
      
      // Combine existing components with new ones from mastersData
      const newMasterComponents = flatten(map(mastersData, "masterServices"));
      localDB.masterComponentHosts = [...existingMasterComponents, ...newMasterComponents];
    } else {
      localDB.masterComponentHosts = flatten(map(mastersData, "masterServices"));
    }
    
    localDB.slaveComponentHosts = flatten(map(slavesData, "checkboxes"));

    if (stackVersion) {
      localDB.selectedStack = stackVersion;
    }
    const dependencies = {
      ...getConfigDependencies(result),
      alwaysEnableManagedMySQLForHive:
        supports.alwaysEnableManagedMySQLForHive,
      isManagedMySQLForHiveEnabled: !["redhat5", "suse11"].includes(
        ambariProperties?.["server.os_family"],
      ),
      isServiceConfigRoute: false,
    };

    // Check if we have stored service config properties (like in addService mode)
    const hasStoredServiceConfigProperties = wizardName === "addService";

    Object.keys(result).forEach((serviceName: string) => {
      // Check if this service is newly being added (not in installedServices)
      const isNewService = !installedServices?.includes(serviceName);
      
      Object.keys(result[serviceName]).forEach((configType: string) => {
        Object.keys(result[serviceName][configType].properties).forEach(
          (propertyName: string) => {
            let serviceConfigProperty =
              result[serviceName][configType].properties[propertyName];

            const shouldProcessInitializers = 
              (!hasStoredServiceConfigProperties && !serviceConfigProperty.hasInitialValue) ||
              (hasStoredServiceConfigProperties && isNewService && !serviceConfigProperty.hasInitialValue);


            if (shouldProcessInitializers) {
              result[serviceName][configType].properties[propertyName] =
                WizardConfigInitializer(
                  serviceConfigProperty as any,
                  localDB,
                  dependencies
                ).initialValue(
                  serviceConfigProperty as any,
                  localDB,
                  dependencies
                );
              
            } else {
              serviceConfigProperty.hasInitialValue = true;
              serviceConfigProperty.initialValue = serviceConfigProperty.value;
              
            }
          }
        );
      });
    });

    return result;
  };


  /**
   * Initialize the base configuration structure from configs
   */
  const initializeBaseConfigStructure = () => {
    let configPropertiesCopy: ConfigPropertiesType = {};
    let updatedConfigProperties: ConfigPropertiesType = {};

    // Initialize MISC section for user groups
    updatedConfigProperties["MISC"] = {};
    updatedConfigProperties["MISC"]["Users and Groups"] = {
      errors: 0,
      properties: {},
      displayName: "Service Accounts",
    };
    
    // Only add Notifications section in cluster creation wizard, not in add service wizard
    // This matches Ember.js behavior where Notifications category is removed in addServiceController
    if (wizardName !== "addService") {
      updatedConfigProperties["MISC"]["Notifications"] = {
        errors: 0,
        properties: {},
      };
    }

    return { configPropertiesCopy, updatedConfigProperties };
  };

  /**
   * Process configurations from stack and create the base property structure
   */
  const processStackConfigurations = (
    configPropertiesCopy: ConfigPropertiesType,
    updatedConfigProperties: ConfigPropertiesType
  ) => {
    // Create deep clones to avoid modifying the originals
    const resultConfigPropertiesCopy = cloneDeep(configPropertiesCopy);
    const resultUpdatedConfigProperties = cloneDeep(updatedConfigProperties);

    configs?.items?.forEach((service: any) => {
      service.configurations?.forEach((config: any) => {
        const fileName = config.StackConfigurations.type as string;
        const configType = fileName.slice(0, -4);
        const propertyName = config.StackConfigurations.property_name as string;
        const serviceName = config.StackConfigurations.service_name;
        const propertyType = config.StackConfigurations.property_type;

        if (!resultConfigPropertiesCopy[serviceName]) {
          resultConfigPropertiesCopy[serviceName] = {};
        }
        if (!resultConfigPropertiesCopy[serviceName][configType]) {
          resultConfigPropertiesCopy[serviceName][configType] = {
            errors: 0,
            properties: {},
          };
        }

        const tempProperty = {
            propertyName: propertyName,
            value: config.StackConfigurations.property_value,
            propertyAttributes: {
            ...config.StackConfigurations.property_value_attributes,
            ...(propertyName === "content" && { type: "content" }),
            },
        };

        const formattedValue = formatPropertyValue(
          tempProperty as any,
          config.StackConfigurations.property_value
        );

        resultConfigPropertiesCopy[serviceName][configType].properties[
          propertyName
        ] = {
          propertyName: propertyName,
          ...(config.StackConfigurations.property_display_name && {
            propertyDisplayname:
              config.StackConfigurations.property_display_name,
          }),
          propertyDescription: config.StackConfigurations.property_description,
          propertyValue: config.StackConfigurations.property_value,
          propertyAttributes:
            config.StackConfigurations.property_value_attributes,
          previousValue: formattedValue,
          value: formattedValue,
          supportsFinal: shouldSupportFinal(serviceName, fileName, configs),
          final: "false",
          savedFinal: "false",
          fileName: fileName,
          propertyType: propertyType ? propertyType : [],
          type: configType,
          serviceName: serviceName,
          isEditable: true,
          isVisible: true,
          recommendedValue: formattedValue,
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
            config.StackConfigurations.property_value_attributes?.unit || null,
        };

        // Handle password type properties
        if (
          resultConfigPropertiesCopy[serviceName][configType].properties[
            propertyName
          ].propertyAttributes.type == "password"
        ) {
          
          resultConfigPropertiesCopy[serviceName][configType].properties[
            propertyName
          ] = {
            ...resultConfigPropertiesCopy[serviceName][configType].properties[
              propertyName
            ],
            confirmPassword: formattedValue,
          };
        }

        if (propertyName === "content") {
            resultConfigPropertiesCopy[serviceName][configType].properties[
          propertyName
        ].propertyAttributes.type = "content";
          }

        // Handle user and group properties
        if (
          propertyType &&
          (propertyType.includes("USER") || propertyType.includes("GROUP"))
        ) {
          resultUpdatedConfigProperties["MISC"]["Users and Groups"].properties[
            propertyName
          ] =
            resultConfigPropertiesCopy[serviceName][configType].properties[
              propertyName
            ];
          delete resultConfigPropertiesCopy[serviceName][configType].properties[
            propertyName
          ];
        }
      });
    });

    return {
      configPropertiesCopy: resultConfigPropertiesCopy,
      updatedConfigProperties: resultUpdatedConfigProperties,
    };
  };

  /**
   * Create custom config types for each service
   */
  const createCustomConfigTypes = (
    configPropertiesCopy: ConfigPropertiesType
  ) => {
    // Create a deep clone to avoid modifying the original
    const result = cloneDeep(configPropertiesCopy);

    Object.keys(result).map((serviceName: string) => {
      Object.keys(result[serviceName]).map((configType: string) => {
        // if (!!!configType.endsWith("env")) {
          result[serviceName]["Custom " + configType] = {
            errors: 0,
            properties: {},
            displayName: "Custom " + configType,
          };
        // }
      });
    });

    return result;
  };

  /**
   * Add service config categories and host information
   */
  const addServiceConfigCategories = (
    configPropertiesCopy: ConfigPropertiesType,
    updatedConfigProperties: ConfigPropertiesType
  ) => {
    // Create a deep clone to avoid modifying the original
    const result = cloneDeep(updatedConfigProperties);

    Object.keys(configPropertiesCopy).forEach((serviceName) => {
      if (!result[serviceName]) {
        result[serviceName] = {};
      }

      const serviceConfigCategories = getConfigCategories(serviceName);
      serviceConfigCategories.forEach((category) => {
        if (!result[serviceName][category.name]) {
          result[serviceName][category.name] = {
            errors: 0,
            properties: {},
            displayName: category.displayName,
          };
        }

        // Get master hosts for this category
        const masterHosts = mastersData
          .filter(
            (host: any) =>
              Array.isArray(host.masterServices) &&
              host.masterServices.some(
                (service: any) => service.component === category.name
              )
          )
          .map((host: any) => host.host_name);

        // Get slave hosts for this category
        const slaveHosts = slavesData
          .filter(
            (host: any) =>
              Array.isArray(host.checkboxes) &&
              host.checkboxes.some(
                (service: any) =>
                  service.label === category.name && service.checked
              )
          )
          .map((host: any) => host.hostname);

        // Add host information if available
        if (!isEmpty(masterHosts) || !isEmpty(slaveHosts)) {
          result[serviceName][category.name].properties[
            category.name.toLowerCase() + "_hosts"
          ] = {
            propertyName: category.name.toLowerCase() + "_hosts",
            propertyDisplayname: category.displayName + " hosts",
            propertyValue: "",
            propertyAttributes: {
              type: "hosts",
              overridable: false,
            },
            previousValue: "",
            value: !isEmpty(masterHosts) ? masterHosts : slaveHosts,
            fileName: serviceName.toLowerCase() + "-site.xml",
            final: "false",
            savedFinal: "false",
            propertyType: [],
            type: serviceName.toLowerCase() + "-site",
            isEditable: false,
          };
        }
      });
    });

    return result;
  };

  /**
   * Organize properties by categories based on the properties file map
   */
  const organizePropertiesByCategories = (
    configPropertiesCopy: ConfigPropertiesType,
    updatedConfigProperties: ConfigPropertiesType
  ) => {
    // Create a deep clone to avoid modifying the original
    const result = cloneDeep(updatedConfigProperties);

    Object.keys(propertiesFileMap).map((service: string) => {
      if (configPropertiesCopy[service]) {
        propertiesFileMap[service].forEach((property: any) => {
          const {
            serviceName,
            filename,
            name,
            category,
            options,
            displayType,
          } = property;
          if (!category) {
            return;
          }
          const configType = filename.slice(0, -4);

          if (
            configPropertiesCopy[serviceName][configType]?.properties[name]
              ?.tabName
          ) {
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

              // Deep clone the property to avoid reference issues
              result[serviceName][category].properties[name] = cloneDeep(
                configPropertiesCopy[serviceName][configType].properties[name]
              );

              if (displayType) {
                result[serviceName][category].properties[
                  name
                ].propertyAttributes.type = displayType;
              }
              if (options) {
                result[serviceName][category].properties[
                  name
                ].propertyAttributes.options = options;
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
   * Add alert notification properties
   * Note: In add service wizard, notification properties should not be added (matching Ember.js behavior)
   */
  const addAlertNotificationProperties = (
    updatedConfigProperties: ConfigPropertiesType
  ) => {
    // Skip adding notification properties in add service wizard
    // This matches Ember.js behavior where Notifications category is removed from MISC in addServiceController
    if (wizardName === "addService") {
      return updatedConfigProperties;
    }

    // Create a deep clone to avoid modifying the original
    const result = cloneDeep(updatedConfigProperties);

    alert_notifications.forEach((property) => {
      const {
        serviceName,
        name,
        category,
        displayName,
        displayType,
        filename,
        isVisible,
      } = property;

      if (!result[serviceName]) {
        result[serviceName] = {};
      }
      if (!result[serviceName][category]) {
        result[serviceName][category] = {
          errors: 0,
          properties: {},
        };
      }

      if (isVisible) {
        result[serviceName][category].properties[name] = {
          propertyName: name,
          propertyDisplayname: displayName || "",
          propertyValue: "",
          propertyAttributes: {
            type: displayType || "string",
            overridable: false,
          },
          previousValue: "",
          value: displayType === "checkbox" ? "false" : "",
          final: "false",
          savedFinal: "false",
          fileName: filename,
          type: filename,
          isEditable: true,
        };
      }
    });

    return result;
  };

  /**
   * Process stack level configurations
   */
  const processStackLevelConfigurations = async (
    updatedConfigProperties: ConfigPropertiesType
  ) => {
    // Create a deep clone to avoid modifying the original
    const result = cloneDeep(updatedConfigProperties);

    if (stackLevelConfigs) {
      const serviceName = "CLUSTER-ENV";
      result[serviceName] = {};
      const additionalUserGroupProperties: any = {};

      let clusterEnvValues: any = null;
      if (wizardName === "addService") {
        clusterEnvValues = await getClusterEnvValues();
      }

      stackLevelConfigs?.configurations?.forEach((config: any) => {
        const fileName = config.StackLevelConfigurations.type as string;
        const configType = fileName.slice(0, -4);
        const propertyName = config.StackLevelConfigurations
          .property_name as string;
        const propertyType = config.StackLevelConfigurations.property_type;

        if (!result[serviceName][configType]) {
          result[serviceName][configType] = {
            errors: 0,
            properties: {},
          };
        }

        let configValue = config.StackLevelConfigurations.property_value;
        let previousValue = config.StackLevelConfigurations.property_value;
        let hasExistingClusterValue = false;
        
        if (
          wizardName === "addService" &&
          clusterEnvValues?.items?.length > 0
        ) {
          const clusterEnvItem = clusterEnvValues.items.find(
            (item: any) => item.type === "cluster-env"
          );
          if (clusterEnvItem?.properties?.[propertyName] !== undefined) {
            configValue = clusterEnvItem.properties[propertyName];
            previousValue = clusterEnvItem.properties[propertyName];
            hasExistingClusterValue = true;
          }
        }

        result[serviceName][configType].properties[propertyName] = {
          propertyName: propertyName,
          ...(config.StackLevelConfigurations.property_display_name && {
            propertyDisplayname:
              config.StackLevelConfigurations.property_display_name,
          }),
          propertyDescription:
            config.StackLevelConfigurations.property_description,
          propertyValue: config.StackLevelConfigurations.property_value,
          propertyAttributes:
            config.StackLevelConfigurations.property_value_attributes,
          previousValue: previousValue,
          value: configValue,
          final: "false",
          savedFinal: "false",
          propertyType: propertyType ? propertyType : [],
          fileName: fileName,
          type: configType,
          serviceName: serviceName,
          isEditable: true,
          ...(hasExistingClusterValue && {
            foundInPropertyValues: true,
            hasInitialValue: true,
            initialValue: configValue,
          }),
        };

        // Handle user and group properties
        if (
          (propertyType && propertyType.includes("USER")) ||
          propertyType.includes("GROUP")
        ) {
          result["MISC"]["Users and Groups"].properties[propertyName] =
            result[serviceName][configType].properties[propertyName];

          delete result[serviceName][configType].properties[propertyName];
        }

        // Handle additional user and group properties
        if (
          (propertyType && propertyType.includes("ADDITIONAL_USER_PROPERTY")) ||
          propertyType.includes("ADDITIONAL_GROUP_PROPERTY")
        ) {
          additionalUserGroupProperties[propertyName] =
            result[serviceName][configType].properties[propertyName];
          delete result[serviceName][configType].properties[propertyName];
        }
      });

      // Merge additional user and group properties
      if (!isEmpty(additionalUserGroupProperties)) {
        result["MISC"]["Users and Groups"].properties = {
          ...result["MISC"]["Users and Groups"].properties,
          ...additionalUserGroupProperties,
        };
      }
    }

    return result;
  };

  /**
   * Initialize the configuration structure with base properties from configs
   * Copied from ServiceConfigs
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

        const tempProperty = {
          propertyName: propertyName,
          value: config.StackConfigurations.property_value,
          propertyAttributes: {
            ...config.StackConfigurations.property_value_attributes,
            ...(propertyName === "content" && { type: "content" }),
          },
        };

        const formattedValue = formatPropertyValue(
          tempProperty as any,
          config.StackConfigurations.property_value
        );

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
            previousValue: formattedValue,
            value: formattedValue,
            supportsFinal : shouldSupportFinal(serviceName, fileName, configs),
            final: "false",
            savedFinal : "false",
            fileName: fileName,
            propertyType: propertyType ? propertyType : [],
            type: configType,
            serviceName: serviceName,
            isEditable: true,
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
          configPropertiesCopy[serviceName]["Custom " + configType] = {
            errors: 0,
            properties: {},
            displayName: "Custom " + configType,
          };
        });
      }
    });

    return configPropertiesCopy;
  };

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

              const currentProperty =
                result[serviceName][type].properties[propertyName];
              currentProperty.foundInPropertyValues = true;
              currentProperty.hasInitialValue = true;
              currentProperty.initialValue = currentProperty.value;

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
                    isEditable: true,
                    foundInPropertyValues: true,
                    hasInitialValue: true,
                    initialValue: properties[propertyName],
                  };
              }
            }
          }
        });

        Object.keys(propertyAttributes).forEach((attr:string) => {
          if(attr !== 'final') return;
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
   * Copied from ServiceConfigs
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
                    serviceName: serviceName,
                    isEditable: true,
                    foundInPropertyValues: true,
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
   * Remove properties that don't have corresponding values in propertyValues
   * Modified to handle newly added services properly
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
      // Check if this service is newly being added (not in installedServices)
      const isNewService = !installedServices.includes(serviceName);
      
      Object.keys(result[serviceName]).forEach((configType) => {
        Object.keys(result[serviceName][configType].properties).forEach(
          (propertyName) => {
            const property =
              result[serviceName][configType].properties[propertyName];

            if (property.foundInPropertyValues === false && !isNewService) {
              result[serviceName][configType].properties[propertyName].value = null;
              result[serviceName][configType].properties[propertyName].previousValue = null;
              result[serviceName][configType].properties[propertyName].isVisible = false;
              result[serviceName][configType].properties[propertyName].isHidden = true;
            }
          }
        );
      });
    });

    return result;
  };

  /**
   * Add component host information to the configuration
   * Using the exact same approach as ServiceConfigs
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

        // Add host information using the same approach as ServiceConfigs
        if (wizardName === "addService" && allServiceModels) {
          const mappedServiceName = serviceNameModelMapping[serviceName.toUpperCase()];
          const serviceModel = allServiceModels[mappedServiceName];
          
          if (serviceModel) {
            const isMasterComponent = serviceModel.masterComponents?.some(
              (component: any) => component.componentName === category.name
            );

            const isSlaveComponent = serviceModel.slaveComponents?.some(
              (component: any) => component.componentName === category.name
            );

            if (isMasterComponent || isSlaveComponent) {
              const hostValues = isMasterComponent
                ? fetchComponentHostNamesByComponent(
                    serviceModel.masterComponents,
                    category.name
                  )
                : fetchComponentHostNamesByComponent(
                    serviceModel.slaveComponents,
                    category.name
                  );

              if (hostValues && hostValues.length > 0) {
                const hostPropertyName = category.name.toLowerCase() + "_hosts";
                updatedConfigProperties[serviceName][category.name].properties[
                  hostPropertyName
                ] = {
                  propertyName: hostPropertyName,
                  propertyDisplayname: category.displayName + " hosts",
                  propertyValue: hostValues,
                  propertyAttributes: {
                    type: "hosts",
                    overridable: false,
                  },
                  previousValue: hostValues,
                  value: hostValues,
                  fileName: serviceName.toLowerCase() + "-site.xml",
                  final: "false",
                  savedFinal: "false",
                  propertyType: [],
                  type: serviceName.toLowerCase() + "-site",
                  serviceName: serviceName,
                  isEditable: false,
                  isVisible: true,
                  foundInPropertyValues: true,
                };
              }
            }
          }
        }
      });
    });

    return updatedConfigProperties;
  };

  /**
   * Organize properties by categories
   * Copied from ServiceConfigs
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
   * Main function to get and process configuration properties
   * Following ServiceConfigs approach exactly
   */
  const getConfigProperties = async (
    themeData = themes,
    preserveCurrentValues = false,
    activeThemeRequestId?: number,
  ) => {
    if (wizardName === "addService") {
      // For addService, use the exact same approach as ServiceConfigs but with host information
      let configPropertiesCopy = initializeConfigStructure();
      
      configPropertiesCopy = addTabNames(configPropertiesCopy, themeData);
      configPropertiesCopy = processDefaultConfigurations(configPropertiesCopy);
      configPropertiesCopy = processOverrideConfigurations(configPropertiesCopy);
      configPropertiesCopy = removePropertiesWithoutValues(configPropertiesCopy);
      
      // Add host information like classic implementation
      let updatedConfigProperties = addComponentHostInformation(configPropertiesCopy);
      updatedConfigProperties = organizeByCategories(configPropertiesCopy, updatedConfigProperties);
      updatedConfigProperties = addRemainingProperties(configPropertiesCopy, updatedConfigProperties);
      
      if (!updatedConfigProperties["MISC"]) {
        updatedConfigProperties["MISC"] = {};
      }
      if (!updatedConfigProperties["MISC"]["Users and Groups"]) {
        updatedConfigProperties["MISC"]["Users and Groups"] = {
          errors: 0,
          properties: {},
          displayName: "Service Accounts",
        };
      }
      
      // Process stack level configurations to get MISC properties
      updatedConfigProperties = await processStackLevelConfigurations(updatedConfigProperties);
      
      updatedConfigProperties = onLoadOverrides(updatedConfigProperties);
      // Process initializers for newly added services
      updatedConfigProperties = initializeValues(updatedConfigProperties);
      updatedConfigProperties = updateVisibilityByForeignKeys(updatedConfigProperties);
      updatedConfigProperties = updateVisibilityForDependsOn(
        updatedConfigProperties,
        themeData,
        "default",
        conditionServices,
      );
      updatedConfigProperties = validateAllProperties(updatedConfigProperties);
      if (preserveCurrentValues) {
        updatedConfigProperties = preserveEditedConfigValues(
          updatedConfigProperties,
          configPropertiesRef.current,
        );
      }
      if (
        activeThemeRequestId !== undefined &&
        activeThemeRequestId !== themeRequestId.current
      ) {
        return;
      }
      configPropertiesRef.current = updatedConfigProperties;
      setConfigProperties(updatedConfigProperties);
    } else {
      // For cluster creation, use the existing wizard approach
      let { configPropertiesCopy, updatedConfigProperties } = initializeBaseConfigStructure();
      ({ configPropertiesCopy, updatedConfigProperties } = processStackConfigurations(configPropertiesCopy, updatedConfigProperties));
      configPropertiesCopy = createCustomConfigTypes(configPropertiesCopy);
      configPropertiesCopy = addTabNames(configPropertiesCopy, themeData);
      updatedConfigProperties = addServiceConfigCategories(configPropertiesCopy, updatedConfigProperties);
      updatedConfigProperties = organizePropertiesByCategories(configPropertiesCopy, updatedConfigProperties);
      updatedConfigProperties = addRemainingProperties(configPropertiesCopy, updatedConfigProperties);
      updatedConfigProperties = addAlertNotificationProperties(updatedConfigProperties);
      updatedConfigProperties = await processStackLevelConfigurations(updatedConfigProperties);
      updatedConfigProperties = onLoadOverrides(updatedConfigProperties);
      updatedConfigProperties = initializeValues(updatedConfigProperties);
      updatedConfigProperties = updateVisibilityByForeignKeys(updatedConfigProperties);
      updatedConfigProperties = updateVisibilityForDependsOn(
        updatedConfigProperties,
        themeData,
        "default",
        conditionServices,
      );
      updatedConfigProperties = validateAllProperties(updatedConfigProperties);
      if (preserveCurrentValues) {
        updatedConfigProperties = preserveEditedConfigValues(
          updatedConfigProperties,
          configPropertiesRef.current,
        );
      }
      if (
        activeThemeRequestId !== undefined &&
        activeThemeRequestId !== themeRequestId.current
      ) {
        return;
      }
      configPropertiesRef.current = updatedConfigProperties;
      setConfigProperties(updatedConfigProperties);
    }
  };

  const onLoadOverrides = (updatedConfigProperties: ConfigPropertiesType) => {
    // Create a deep clone to avoid modifying the original
    let configs = cloneDeep(updatedConfigProperties);
    const isRangerPresent = services.includes("RANGER");

    if (!isRangerPresent) {
      configs = removeRangerConfigs(configs);
    }

    return configs;
  };

  const getThemes = async () => {
    const requestId = ++themeRequestId.current;
    setThemeLoadNotice(null);
    setThemeRetrying(true);
    try {
      const response = await WizardApi.getStackThemes(
        stackName,
        stackVersion,
        conditionServices.join(","),
        "themes/*"
      );
      if (requestId !== themeRequestId.current) {
        return;
      }
      const notice = classifyDefaultThemeResponse(response, conditionServices);
      setThemes(response);
      setThemeLoadNotice(notice);
      if (!isEmpty(configPropertiesRef.current)) {
        await getConfigProperties(response, true, requestId);
      }
    } catch (error: any) {
      if (requestId !== themeRequestId.current) {
        return;
      }
      setThemeLoadNotice({
        kind: "request",
        message: describeThemeRequestError(
          error,
          "Service configuration layouts could not be loaded.",
        ),
      });
    } finally {
      if (requestId === themeRequestId.current) {
        setThemesSettled(true);
        setThemeRetrying(false);
      }
    }
  };

  const getConfigurations = async () => {
    const response = await WizardApi.getStackConfigurations(
      stackName,
      stackVersion,
      conditionServices.join(","),
      "configurations/*,configurations/dependencies/*,StackServices/config_types/*"
    );
    setConfigs(response);
  };

  const getStackLevelConfigurations = async () => {
    const response = await WizardApi.getStackLevelConfigurations(
      stackName,
      stackVersion,
      "configurations/*,Versions/config_types/*"
    );
    setStackLevelConfigs(response);
  };

  const checkIfDisabled = (tabName: string) => {
    return disabledTabs.includes(tabName);
  };

  const handleDependentConfigsModalCallback = (updatedProperties?: any[]) => {
    if (updatedProperties) {
      // Apply the user's selections back to the recommended changes
      const updatedRecommendations = { ...recommendedChanges };

      // Create a copy of configProperties to update actual values
      const updatedConfigProperties = cloneDeep(configProperties);
      
      updatedProperties.forEach((property: any) => {
        const key = property.propertyName + property.propertyFileName;
        if (updatedRecommendations[key]) {
          // Update the saveRecommended flag based on user selection
          updatedRecommendations[key].saveRecommended =
            property.saveRecommended;

          // the config property that needs to be updated depending on user choice
          const configProperty = getConfigPropertyByName(
            property.propertyName,
            updatedConfigProperties
          );

          if (configProperty) {
            // value is checked
            if (property.saveRecommended) {
              configProperty.value = property.recommendedValue;
            } else {
              configProperty.value =
                property.initialValue || property.originalValue;
            }

            // Clear any error messages
            configProperty.errorMessage = "";
            configProperty.warnMessage = "";
          }
        }
      });
      
      setRecommendedChanges(updatedRecommendations);

      // apply 
      setConfigProperties(updatedConfigProperties);
    }
    setShowDependentConfigsModal(false);
  };

  const handleShowDetails = () => {
    setShowDependentConfigsModal(true);
  };

  // Calculate dependent config changes summary
  const getDependentConfigsSummary = () => {
    if (!dependentConfigsToShow.length) return null;
    
    // Filter only selected/checked recommendations (matching Ember.js filterProperty('saveRecommended'))
    const selectedChanges = dependentConfigsToShow.filter(
      (change) => change.saveRecommended
    );
    
    const changesCount = selectedChanges.length;
    const servicesCount = [
      ...new Set(selectedChanges.map((change) => change.serviceName)),
    ].length;
    
    // Show different message based on whether any recommendations are selected
    const message = changesCount > 0
      ? `There ${
          changesCount === 1 ? "is" : "are"
        } ${changesCount} configuration change${
          changesCount === 1 ? "" : "s"
        } in ${servicesCount} service${servicesCount === 1 ? "" : "s"}`
      : `There are ${dependentConfigsToShow.length} available recommendations (none selected)`;
    
    return {
      changesCount,
      servicesCount,
      message,
    };
  };

  const continueAfterConfiguration = async () => {
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: createConfigurationStepPayload(
        currentStep.name,
        configProperties,
        themes,
        configs,
        stackLevelConfigs,
        preInstallChecksWereRun,
        selectedTab,
        selectedServicesByTab,
      ),
    });
    if (wizardName === "addService") {
      const nextStep = nextAddServiceStep(4, addServiceFlow);
      await Promise.resolve(flushStateToDb("jump", nextStep));
      jumpToStep(nextStep);
    } else {
      await Promise.resolve(flushStateToDb("next"));
      handleNextImperitive();
    }
  };

  const runPreInstallChecks = () => {
    setPreInstallChecksWereRun(true);
    setShowSkippedChecksWarning(false);
    setShowPreInstallChecks(true);
  };

  const handleNext = async () => {
    if (tabMapping[selectedTab].nextTab && wizardName === "clusterCreation") {
      const nextTab = findNextEnabledConfigurationTab(
        selectedTab,
        tabMapping,
        disabledTabs,
      );
      if (nextTab) {
        setSelectedTab(nextTab);
        return;
      }
    }
    if (shouldWarnBeforeSkippingPreInstallChecks(
      wizardName,
      Boolean(supports.preInstallChecks),
      preInstallChecksWereRun,
    )) {
      setShowSkippedChecksWarning(true);
      return;
    }
    await continueAfterConfiguration();
  };

  const addServiceTabMapping = () => {
    // Filter out Notifications section from MISC in add service mode (matches Ember.js behavior)
    const filteredConfigProperties = cloneDeep(configProperties);
    if (filteredConfigProperties["MISC"]?.["Notifications"]) {
      delete filteredConfigProperties["MISC"]["Notifications"];
    }

    return (
      <RestAllTabs
        themes={themes}
        configs={configs}
        configProperties={filteredConfigProperties}
        setConfigProperties={setConfigProperties}
        validationErrors={validationErrors}
        services={filteredServices}
        tabName="default"
        recommendationsDataToSend={getValidationRequestBody()}
        stack={stackName}
        stackVersion={stackVersion}
        hosts={hostsList}
        wizardName={wizardName}
        selectedService={
          selectedServicesByTab.allConfigurations ||
          filteredServices.find(
            (serviceName) => !installedServices.includes(serviceName),
          )
        }
        onServiceChange={(serviceName) =>
          setSelectedServicesByTab((current) => ({
            ...current,
            allConfigurations: serviceName,
          }))
        }
        conditionServices={conditionServices}
      />
    );
  };
  if (loading || isEmpty(configProperties)) {
    return <Spinner />;
  }

  return (
    <>
      <BootstrapModal
        show={showSkippedChecksWarning}
        onHide={() => setShowSkippedChecksWarning(false)}
      >
        <BootstrapModal.Header closeButton>
          <BootstrapModal.Title>Skipping Pre Install Checks</BootstrapModal.Title>
        </BootstrapModal.Header>
        <BootstrapModal.Body>
          Skipping Pre Install Checks is not recommended.
        </BootstrapModal.Body>
        <BootstrapModal.Footer>
          <Button variant="secondary" onClick={runPreInstallChecks}>
            Run Pre Install Checks
          </Button>
          <Button
            variant="primary"
            onClick={() => {
              setShowSkippedChecksWarning(false);
              void continueAfterConfiguration();
            }}
          >
            Ignore and Proceed
          </Button>
        </BootstrapModal.Footer>
      </BootstrapModal>
      <BootstrapModal
        show={showPreInstallChecks}
        onHide={() => setShowPreInstallChecks(false)}
      >
        <BootstrapModal.Header closeButton>
          <BootstrapModal.Title>Pre Install Checks</BootstrapModal.Title>
        </BootstrapModal.Header>
        <BootstrapModal.Body />
        <BootstrapModal.Footer>
          <Button variant="primary" onClick={() => setShowPreInstallChecks(false)}>
            Close
          </Button>
        </BootstrapModal.Footer>
      </BootstrapModal>
      <div>
        {themeLoadNotice && (
          <Alert
            variant={themeLoadNotice.kind === "empty" ? "info" : "warning"}
            className="mb-3"
          >
            <div className="d-flex justify-content-between align-items-center gap-3">
              <div>
                <div>
                  {themeLoadNotice.kind === "empty"
                    ? "No Theme layouts are defined for the selected services."
                    : "Some service configuration layouts could not be loaded."}
                  {" "}Advanced configurations remain available.
                </div>
                <small>{themeLoadNotice.message}</small>
              </div>
              {themeLoadNotice.kind !== "empty" && (
                <Button
                  size="sm"
                  variant="outline-warning"
                  disabled={themeRetrying}
                  onClick={() => void getThemes()}
                >
                  {themeRetrying ? "Retrying..." : "Retry"}
                </Button>
              )}
            </div>
          </Alert>
        )}
        {/* Dependent Configurations Warning Banner for Add Service */}
        {wizardName === "addService" && getDependentConfigsSummary() && (
          <div className="alert alert-warning mb-3" role="alert">
            <div className="d-flex justify-content-between align-items-center">
              <span>{getDependentConfigsSummary()?.message}</span>
              <button
                type="button"
                className="btn btn-link p-0 text-primary"
                onClick={handleShowDetails}
                style={{ textDecoration: "underline" }}
              >
                Show Details
              </button>
            </div>
          </div>
        )}

        {wizardName === "addService" ? (
          addServiceTabMapping()
        ) : (
          <Tab.Container activeKey={selectedTab} transition={false}>
            <Row className="ps-3 mb-3">
              <Nav variant="underline">
                {Object.keys(tabMapping).map((tabKey) => (
                  <Nav.Item
                    onClick={() => {
                      if (!checkIfDisabled(tabKey)) {
                        setSelectedTab(tabKey);
                      }
                    }}
                    key={tabKey}
                  >
                    <Nav.Link
                      as="div"
                      className={
                        checkIfDisabled(tabKey)
                          ? "me-4 text-dark disabled-btn ambari-tabs nav-link nav-link-underlined ms-3"
                          : "me-4 text-dark ambari-tabs nav-link nav-link-underlined ms-3"
                      }
                      eventKey={tabKey}
                      disabled={checkIfDisabled(tabKey)}
                    >
                      {get(tabMapping, tabKey + ".icon", "")}{" "}
                      {get(tabMapping, tabKey + ".title", "")}
                    </Nav.Link>
                  </Nav.Item>
                ))}
              </Nav>
            </Row>
            <Row>
              <Tab.Content>
                {Object.keys(tabMapping).map((tabKey) => (
                  <Tab.Pane key={tabKey} eventKey={tabKey}>
                    {selectedTab === tabKey
                      ? get(tabMapping, tabKey + ".component", "")
                      : null}
                  </Tab.Pane>
                ))}
              </Tab.Content>
            </Row>
          </Tab.Container>
        )}
      </div>
      <WizardFooter
        isNextEnabled={isNextEnabled}
        lifted
        onNext={() => void handleNext()}
        onCancel={() => flushStateToDb("cancel")}
        onBack={async () => {
          if (wizardName === "addService") {
            const previousStep = previousAddServiceStep(4, addServiceFlow);
            await Promise.resolve(flushStateToDb("jump", previousStep));
            jumpToStep(previousStep);
          } else {
            const previousTab = findPreviousEnabledConfigurationTab(
              selectedTab,
              tabMapping,
              disabledTabs,
            );
            if (previousTab) {
              setSelectedTab(previousTab);
              return;
            }
            await Promise.resolve(flushStateToDb("back"));
            jumpToStep(6);
          }
        }}
        step={currentStep}
        sideItems={
          wizardName === "clusterCreation" && supports.preInstallChecks ? (
            <Button size="sm" variant="outline-secondary" onClick={runPreInstallChecks}>
              Pre Install Checks
            </Button>
          ) : null
        }
      />
      
      {/* Dependent Configurations Modal */}
      {showDependentConfigsModal && (
        <DependentConfigurationsModal
          isOpen={showDependentConfigsModal}
          onClose={() => setShowDependentConfigsModal(false)}
          dependentConfigs={dependentConfigsToShow}
          onSave={handleDependentConfigsModalCallback}
        />
      )}
    </>
  );
}
