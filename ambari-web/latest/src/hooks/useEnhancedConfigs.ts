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
import {
  ConfigPropertiesType,
  PropertyType,
} from "../screens/CommonConfigs/types";
import { AppContext } from "../store/context";
import ConfigsApi from "../api/configsApi";
import ClusterApi from "../api/clusterApi";
import {
  cloneDeep,
  flatten,
  forEach,
  get,
  has,
  map,
  set,
  uniq,
} from "lodash";
import {
  buildConfigsJSON,
  formatPropertyValue,
  getConfigTagFromFileName,
  updateVisibilityByForeignKeys,
  validateAllProperties,
} from "../screens/CommonConfigs/ConfigUtils";
import { useDebounce } from "./useDebounce";
import { groupPropertyValues } from "../Utils/dataUtils";
import useHostComponents from "../screens/ClusterWizard/hooks/useHostComponents";
import { getClusterEnvProperties } from "../Utils/clusterConfigUtils";
import WizardApi from "../api/wizardApi";

// List of config types that always need to be processed
const ALWAYS_PROCESS_CONFIG_TYPES = ["capacity-scheduler"];

/**
 * Dynamically builds a mapping from config types to service names based on stack services
 * @param services - Array of services from the context
 * @param serviceComponentInfo - Stack service component information
 * @param stack - Stack name for API calls
 * @param versionNum - Stack version for API calls
 * @returns Promise<Object> mapping config types to service names
 */
async function buildConfigTypeToServiceMap(
  services: any[], 
  serviceComponentInfo: any, 
  stack: string, 
  versionNum: string
): Promise<{ [key: string]: string }> {
  const configTypeToServiceMap: { [key: string]: string } = {};
  
  services?.forEach((service) => {
    const serviceName = get(service, "StackServices.service_name") || get(service, "ServiceInfo.service_name");
    const configTypes = get(service, "StackServices.config_types", {});
    
    if (serviceName && configTypes) {
      Object.keys(configTypes).forEach((configType) => {
        configTypeToServiceMap[configType] = serviceName;
      });
    }
  });
  
  if (Object.keys(configTypeToServiceMap).length === 0 && serviceComponentInfo?.items) {
    serviceComponentInfo.items.forEach((service: any) => {
      const serviceName = get(service, "StackServices.service_name");
      const configTypes = get(service, "StackServices.config_types", {});
      
      if (serviceName && configTypes) {
        Object.keys(configTypes).forEach((configType) => {
          configTypeToServiceMap[configType] = serviceName;
        });
      }
    });
  }
  
  // If still no mappings, fetch from stack API dynamically
  if (Object.keys(configTypeToServiceMap).length === 0 && stack && versionNum) {
    try {
      const stackServicesData = await ConfigsApi.loadConfigsFromStack(stack, versionNum, []);
      
      if (stackServicesData?.items) {
        stackServicesData.items.forEach((service: any) => {
          const serviceName = get(service, "StackServices.service_name");
          const configTypes = get(service, "StackServices.config_types", {});
          
          if (serviceName && configTypes) {
            Object.keys(configTypes).forEach((configType) => {
              configTypeToServiceMap[configType] = serviceName;
            });
          }
        });
      }
    } catch (error) {
      console.error("Failed to fetch config type mappings from stack API:", error);
    }
  }
  
  return configTypeToServiceMap;
}

function useEnhancedConfigs(
  setConfigProperties: (configs: ConfigPropertiesType) => void,
  serviceName?: string,
  installedServices?: string[],
  recommendationsDataToSend?: Object,
  controllerName?: string,
  STACK?: string,
  VERSION?: string,
  HOSTS?: string[]
) {
  const {
    cluster: { stack, versionNum, cluster_id: clusterId },
    services,
    allHostNames,
    serviceComponentInfo,
  } = useContext(AppContext);
  const [recommendationsInProgress, setRecommendationsInProgress] =
    useState<boolean>(false);
  const [currentlyChangedConfig, setCurrentlyChangedConfig] =
    useState<any>(null);
  const [processingConfig, setProcessingConfig] = useState<boolean>(false);
  const serviceNames =
    services.map((service: any) => service.ServiceInfo?.service_name) || [];
  const { hostComponents } = useHostComponents(serviceNames);
  const recommededConfigsRef = useRef<{ [propertyName: string]: any }>({});
  const [recommendedChanges, setRecommendedChanges] = useState<any>();
  const [configTypeToServiceMap, setConfigTypeToServiceMap] = useState<{ [key: string]: string }>({});

  useEffect(() => {
    const loadConfigTypeMapping = async () => {
      if (stack && versionNum) {
        const mapping = await buildConfigTypeToServiceMap(services, serviceComponentInfo, stack, versionNum);
        setConfigTypeToServiceMap(mapping);
      }
    };
    
    loadConfigTypeMapping();
  }, [services, serviceComponentInfo, stack, versionNum]);

  const isWizard = () => {
    return (
      controllerName === "clusterCreation" || controllerName === "addService"
    );
  };

  useEffect(() => {
    setRecommendedChanges(recommededConfigsRef.current);
  }, [JSON.stringify(recommededConfigsRef.current)]);

  async function loadRecommendationsForConfigOnLoad(
    configProperties: ConfigPropertiesType
  ) {
    setRecommendationsInProgress(true);
    try {
      const payload = {
        autoComplete: "true",
        clusterId: clusterId,
        configsResponse: "true",
        recommend: "configurations",
        recommendations: {},
        serviceName: serviceName,
        user_context: {
          operation: "RecommendAttribute",
        },
      };
      const recommendationsResponse = await ConfigsApi.getRecommendations(
        stack,
        versionNum,
        payload
      );
      processRecommendations(recommendationsResponse, configProperties, true);
    } catch (error) {
      console.error("Error loading recommendations:", error);
    } finally {
      setRecommendationsInProgress(false);
    }
  }

  function findPropertyByPropertyName(
    config: ConfigPropertiesType,
    propertyName: string,
    configType: string
  ) {
    for (const groupKey in config) {
      const group = config[groupKey];
      for (const subGroupKey in group) {
        const subGroup = group[subGroupKey];
        const props = subGroup.properties;
        for (const propKey in props) {
          const prop = props[propKey];
          if (prop.propertyName === propertyName && prop.type === configType) {
            return prop;
          }
        }
      }
    }
    return null;
  }

  function allowUpdateProperty(
    propertyName: string,
    _fileName: string,
    _configGroup: string = "Default",
    savedValue: any,
    property: PropertyType | null
  ): boolean {
    const isAddServiceMode = controllerName === "addService";
    
    if (propertyName.includes('proxyuser')) {
      return true;
    }
    
    if (controllerName === "clusterCreation") {
      return true;
    }
    
    if (!isWizard()) {
      return true;
    }
    
    // Classic logic for addServiceController
    if (isAddServiceMode) {
      const stackProperty = property; // TODO: Should get from stack definition
      
      if (!stackProperty || !stackProperty.serviceName || !installedServices?.includes(stackProperty.serviceName)) {
        return true;
      }
      
      const propertyDependsOn = stackProperty.propertyDependsOn || [];
      if (propertyDependsOn.length > 0) {
        
        const hasNewServiceDependency = propertyDependsOn.some((dep: any) => {
          // Use dynamic config type to service mapping
          const dependentServiceName = configTypeToServiceMap[dep.type];
          if (!dependentServiceName) {
            return false;
          }
          
          const isNewService = !installedServices?.includes(dependentServiceName);
          return isNewService;
        });
        
        return hasNewServiceDependency;
      } else {
        const allowBasedOnValue = savedValue != null && stackProperty.recommendedValue === savedValue;
        return allowBasedOnValue;
      }
    }
    
    return true;
  }

  function updateConfigByRecommendation(
    config: PropertyType,
    recommendedValue: string
  ): PropertyType {
    if (!config) {
      throw new Error("Config object is required");
    }
    config.recommendedValue = recommendedValue;
    const name = config.propertyName;
    let allowConfigUpdate = true;
    
    if (currentlyChangedConfig) {
      const currentConfigId = `${currentlyChangedConfig.name}_${currentlyChangedConfig.fileName}`;
      const thisConfigId = `${name}_${config.fileName || ""}`;
      if (currentConfigId === thisConfigId) {
        allowConfigUpdate = false;
      }
    }
    if (allowConfigUpdate) {
      config.errorMessage = "";
      config.warnMessage = "";
    }
    
    if (recommendedValue !== null) {
      config.isVisible = true;
      config.isHidden = false;
    }
    return config;
  }

  const deletedProperties = new Set<string>();

  function updatePropertyAttributesByName(
    configs: ConfigPropertiesType,
    propertyName: string,
    propertyOperation: any,
    configType: string,
    shouldProcessDelete: boolean = true
  ) {
    const matchingConfigProperty = findPropertyByPropertyName(
      configs,
      propertyName,
      configType
    );

    // Handle delete operations even if property doesn't exist in current configs
    if (shouldProcessDelete !== false && propertyOperation?.delete === "true") {
      deletedProperties.add(`${propertyName}_${configType}`);
      
      if (matchingConfigProperty) {
        matchingConfigProperty.isVisible = false;
        matchingConfigProperty.isHidden = true;
        matchingConfigProperty.value = null;
        
        const originalValue = matchingConfigProperty.foundInPropertyValues ? matchingConfigProperty.value : matchingConfigProperty.previousValue;
        
        recommededConfigsRef.current[propertyName + (matchingConfigProperty.fileName || "")] = {
          propertyName: propertyName,
          recommendedValue: null, // null indicates property removal
          initialValue: originalValue,
          originalValue: originalValue,
          type: configType,
          fileName: matchingConfigProperty.fileName,
          serviceName: matchingConfigProperty.serviceName,
          serviceDisplayName: matchingConfigProperty.serviceName,
          isChanged: true,
          configGroup: "Default",
          saveRecommended: true,
          isEditable: true,
          isDeleted: true, // Mark as deleted
          propertyDependsOn: matchingConfigProperty.propertyDependsOn || [],
          propertyDependedBy: matchingConfigProperty.propertyDependedBy || [],
        };
      } else {
        // Property doesn't exist in current configs but needs to be deleted
        // Don't add to recommendations since there's nothing to delete
        console.log(`[DELETE] Skipping deletion recommendation for non-existent property: ${propertyName} (${configType})`);
      }
      return;
    }

    if (matchingConfigProperty) {
      matchingConfigProperty["propertyAttributes"] = {
        ...matchingConfigProperty["propertyAttributes"],
        ...propertyOperation,
      };
      if (has(propertyOperation, "visible")) {
        matchingConfigProperty.isVisible = propertyOperation.visible;
      }
      if (has(propertyOperation, "read_only")) {
        matchingConfigProperty.isEditable = !propertyOperation.read_only;
      }
      if (has(propertyOperation, "hidden")) {
        matchingConfigProperty.isVisible = !propertyOperation.hidden;
      }
    }
  }

  function isAlwaysProcessedConfigType(configType: string): boolean {
    return ALWAYS_PROCESS_CONFIG_TYPES.some(
      (type) => configType === type || configType.includes(type)
    );
  }

  function normalizeHostBasedUrl(url: string): string {
    if (!url || typeof url !== 'string') {
      return url;
    }

    // Pattern to match URLs with multiple hosts separated by semicolons
    const kmsPattern = /^(kms:\/\/[^@]+@)([^:\/]+(?:;[^:\/]+)*)(:.+)$/;
    
    // Handle KMS URLs with semicolon-separated hosts
    const kmsMatch = url.match(kmsPattern);
    if (kmsMatch) {
      const [, protocol, hostsStr, suffix] = kmsMatch;
      const hosts = hostsStr.split(';').sort();
      return protocol + hosts.join(';') + suffix;
    }

    // Handle Thrift URLs with comma-separated entries
    if (url.includes('thrift://') && url.includes(',')) {
      const entries = url.split(',');
      const normalizedEntries = entries.map(entry => {
        const match = entry.match(/^(thrift:\/\/)([^:]+)(:\d+)$/);
        if (match) {
          return entry;
        }
        return entry;
      });
      
      normalizedEntries.sort();
      return normalizedEntries.join(',');
    }

    // Handle other multi-host patterns
    const multiHostPattern = /^([^:]+:\/\/)([^\/]+)(\/.*)?$/;
    const multiHostMatch = url.match(multiHostPattern);
    if (multiHostMatch) {
      const [, protocol, hostsStr, path = ''] = multiHostMatch;
      
      if (hostsStr.includes(';') || hostsStr.includes(',')) {
        const separator = hostsStr.includes(';') ? ';' : ',';
        const hosts = hostsStr.split(separator).sort();
        return protocol + hosts.join(separator) + path;
      }
    }

    return url;
  }

  function areValuesEquivalent(value1: any, value2: any): boolean {
    const isNone = (val: any) => val === null || val === undefined;
    
    if ((isNone(value1) && isNone(value2)) || value1 == value2) {
      return true;
    }

    if (typeof value1 === 'string' && typeof value2 === 'string') {
      return normalizeHostBasedUrl(value1) === normalizeHostBasedUrl(value2);
    }

    return false;
  }

  function hasPropertyDependedBy(config: PropertyType): boolean {
    if (!config.propertyDependedBy) {
      return false;
    }
    if (Array.isArray(config.propertyDependedBy)) {
      return config.propertyDependedBy.length > 0;
    }
    if (typeof config.propertyDependedBy === "object" && config.propertyDependedBy !== null) {
      return Object.keys(config.propertyDependedBy).length > 0;
    }
    if (typeof config.propertyDependedBy === "string") {
      return config.propertyDependedBy.trim() !== "";
    }
    return Boolean(config.propertyDependedBy);
  }

  function processRecommendations(
    recommendationsResponse: any,
    configProperties: ConfigPropertiesType,
    recommendAttribute: boolean = false
  ) {
    
    let configsCopy = cloneDeep(configProperties);
    
    let resource;
    if (Array.isArray(recommendationsResponse)) {
      resource = recommendationsResponse[0];
    } else if (recommendationsResponse.resources) {
      resource = recommendationsResponse.resources[0];
    } else {
      return;
    }
    
    const configurations = resource.recommendations?.blueprint?.configurations;
    if (!configurations) {
      return;
    }
    

    const flattenedPropertyAttributes: any = [];
    forEach(configurations, (propertiesOfType, configType) => {
      if (has(propertiesOfType, "property_attributes")) {
        flattenedPropertyAttributes.push({
          propertyAttributes: propertiesOfType.property_attributes,
          configType,
        });
      }
    });


    for (const { propertyAttributes, configType } of flattenedPropertyAttributes) {
      
      forEach(propertyAttributes, (propertyOperation, propertyName) => {        
      updatePropertyAttributesByName(
        configsCopy,
        propertyName,
        propertyOperation,
        configType,
        !recommendAttribute
      );
      });
    }

    if (!recommendAttribute) {
      updateConfigsByRecommendations(configurations, configsCopy);
      addByRecommendations(configurations, configsCopy);
      cleanUpRecommendations();
    }
    configsCopy = updateVisibilityByForeignKeys(configsCopy);
    configsCopy = validateAllProperties(configsCopy);
    setConfigProperties(configsCopy);
  }

  function updateConfigsByRecommendations(
    recommendationObject: any,
    configProperties: ConfigPropertiesType
  ) {
    const processedProperties = new Set<string>();
    Object.keys(configProperties).forEach(serviceName => {
      Object.keys(configProperties[serviceName]).forEach(configCategory => {
        Object.keys(configProperties[serviceName][configCategory].properties).forEach(propertyKey => {
          const property = configProperties[serviceName][configCategory].properties[propertyKey];
          const configType = property.type;
          const propertyName = property.propertyName;
          const fileName = property.fileName;
          
          const propertyId = `${propertyName}_${configType}`;
          if (processedProperties.has(propertyId)) {
            return;
          }
          
          if (deletedProperties.has(propertyId)) {
            return;
          }
          
          processedProperties.add(propertyId);
          
          if (configType && recommendationObject[configType]) {
            const recommendations = recommendationObject[configType];
            if (recommendations && recommendations.properties) {
              const recommendedValue = recommendations.properties[propertyName];
              if (recommendedValue !== undefined) {
                const formattedRecommendedValue = formatPropertyValue(property, recommendedValue);
                
                property.recommendedValue = formattedRecommendedValue;
                
                
                const isAllowed = allowUpdateProperty(propertyName, fileName || "", "Default", property.value, property);
                
                if (isAllowed) {
                  updateConfigByRecommendation(property, formattedRecommendedValue);
                  const existingRecommendation = recommededConfigsRef.current[propertyName + (fileName || "")];
                  const originalValue = existingRecommendation ? existingRecommendation.initialValue : property.previousValue;
                  if (!areValuesEquivalent(originalValue, formattedRecommendedValue)) {
                    recommededConfigsRef.current[propertyName + (fileName || "")] = {
                      propertyName: propertyName,
                      recommendedValue: formattedRecommendedValue,
                      initialValue: originalValue,
                      originalValue: originalValue,
                      type: configType,
                      fileName: fileName || "",
                      serviceName: serviceName||property.serviceName, 
                      serviceDisplayName: serviceName||property.serviceName,
                      isChanged: true,
                      configGroup: "Default",
                      saveRecommended: controllerName === "clusterCreation" || !isWizard(), // Auto-save for cluster creation and service configs, show as recommendation for add service
                      isEditable: property.isEditable !== false,
                      propertyDependsOn: property.propertyDependsOn || [],
                      propertyDependedBy: property.propertyDependedBy || [],
                    };
                  }
                  
                  // Apply recommendations to field values for cluster creation and service dashboard configs
                  // Do this AFTER creating the recommendation entry to preserve original value
                  if (controllerName === "clusterCreation" || !isWizard()) {
                    property.value = formattedRecommendedValue;
                  }
                }
                delete recommendations.properties[propertyName];
              }
            }
          }
        });
      });
    });
  }

  function addByRecommendations(
    recommendationObject: any,
    configProperties: ConfigPropertiesType
  ) {
    for (const configType in recommendationObject) {
      const recommendations = recommendationObject[configType];
      if (recommendations && recommendations.properties) {
        const properties = recommendations.properties;
        
        let targetServiceName: string | undefined = undefined;
        Object.keys(configProperties).forEach(serviceName => {
          if (configProperties[serviceName][configType]) {
            targetServiceName = serviceName;
          }
        });
        
        // If no target service found in existing configs, try to map config type to service
        if (!targetServiceName) {
          targetServiceName = configTypeToServiceMap[configType] || 'MISC';
        }
        
        for (const propertyName in properties) {
          const recommendedValue = properties[propertyName];
          
          const isProxyUserProperty = propertyName.includes('proxyuser');
          const isTezHistoryLoggingProperty = propertyName.includes('tez.history.logging');
          const isAddServiceMode = controllerName === "addService";
          
          let shouldIncludeProperty = true;
          if (isAddServiceMode && !isProxyUserProperty && !isTezHistoryLoggingProperty && targetServiceName && installedServices?.includes(targetServiceName)) {
            shouldIncludeProperty = false;
          }
          if (!isWizard()) {
            shouldIncludeProperty = true;
          }
          
          if (shouldIncludeProperty && allowUpdateProperty(propertyName, configType + ".xml", "Default", null, null)) {
            recommededConfigsRef.current[propertyName + configType + ".xml"] = {
              propertyName: propertyName,
              recommendedValue: recommendedValue,
              initialValue: null,
              originalValue: null,
              type: configType,
              fileName: configType + ".xml",
              serviceName: targetServiceName,
              serviceDisplayName: targetServiceName,
              isChanged: true,
              configGroup: "Default",
              saveRecommended: true,
              isEditable: true,
              propertyDependsOn: [],
              propertyDependedBy: [],
            };
          }
        }
      }
    }
  }

  function processNewServiceStackConfigs(
    newServiceConfigs: any,
    selectedServices: string[]
  ) {
    const newServices = selectedServices;
    
    
    // Process stack configurations for new services directly into recommendations
    for (const configType in newServiceConfigs) {
      const configData = newServiceConfigs[configType];
      if (configData && configData.properties) {
        const properties = configData.properties;
        
        // Use dynamic config type to service mapping
        const targetServiceName = configTypeToServiceMap[configType] || 'MISC';
        
        // Only process if this config type belongs to a newly adding service
        if (newServices.includes(targetServiceName)) {
          for (const propertyName in properties) {
            const recommendedValue = properties[propertyName];
            
            if (recommendedValue != null && recommendedValue !== "") {
              recommededConfigsRef.current[propertyName + configType + ".xml"] = {
                propertyName: propertyName,
                recommendedValue: recommendedValue,
                initialValue: null,
                originalValue: null,
                type: configType,
                fileName: configType + ".xml",
                serviceName: targetServiceName,
                serviceDisplayName: targetServiceName,
                isChanged: true,
                configGroup: "Default",
                saveRecommended: true,
                isEditable: true,
                propertyDependsOn: [],
                propertyDependedBy: [],
              };
            }
          }
        } 
      }
    }
  }

  function cleanUpRecommendations() {
    
    const filteredRecommendations: { [key: string]: any } = {};
    Object.keys(recommededConfigsRef.current).forEach(key => {
      const recommendation = recommededConfigsRef.current[key];
      const initialValue = recommendation.initialValue;
      const recommendedValue = recommendation.recommendedValue;
      
      if (recommendation.isPlaceholder) {
        return;
      }
      
      // Filter out recommendations with undefined or empty recommendedValue (including deleted properties with null values)
      if (recommendedValue === undefined || recommendedValue === null || recommendedValue === "") {
        return; // Skip this recommendation
      }
      
      // Keep deleted properties only if they have a valid original value (something is actually being deleted)
      if (recommendation.isDeleted) {
        if (initialValue != null && initialValue !== "" && initialValue !== undefined) {
          filteredRecommendations[key] = recommendation;
        }
        return;
      }
      
      if (!isWizard()) {
        if (recommendedValue != null && recommendedValue !== "" && recommendedValue !== undefined) {
          filteredRecommendations[key] = recommendation;
        }
        return;
      }
      
      if (recommendedValue != null && recommendedValue !== "" && recommendedValue !== undefined) {
        // Additional check: only keep if there's a meaningful difference from initial value
        if (initialValue !== recommendedValue && !areValuesEquivalent(initialValue, recommendedValue)) {
          filteredRecommendations[key] = recommendation;
        } 
        return;
      }
      
      const shouldKeep = !((initialValue == null && recommendedValue == null) || areValuesEquivalent(initialValue, recommendedValue));
      
      if (shouldKeep) {
        filteredRecommendations[key] = recommendation;
      }
    });
    
    recommededConfigsRef.current = filteredRecommendations;
  }
  //@ts-ignore
  function clearRecommendationsByServiceName(serviceNames: string[]) {
    const filteredRecommendations: { [key: string]: any } = {};
    Object.keys(recommededConfigsRef.current).forEach(key => {
      const recommendation = recommededConfigsRef.current[key];
      
      if (!serviceNames.includes(recommendation.serviceName)) {
        filteredRecommendations[key] = recommendation;
      }
    });
    
    recommededConfigsRef.current = filteredRecommendations;
  }
  
  const onValueUpdate = useDebounce(
    async (config: PropertyType, configProperties: ConfigPropertiesType) => {
      if (
        config.isVisible === false ||
        config.errorMessage ||
        config.hasError
      ) {
        return;
      }

      const recommendationKey = config.propertyName + (config.fileName || "");
      if (!recommededConfigsRef.current[recommendationKey]) {
        const originalValue = config.previousValue || config.value;
        
        recommededConfigsRef.current[recommendationKey] = {
          propertyName: config.propertyName,
          recommendedValue: null, // Will be updated when actual recommendations come
          initialValue: originalValue,
          originalValue: originalValue,
          type: config.type,
          fileName: config.fileName || "",
          serviceName: config.serviceName,
          serviceDisplayName: config.serviceName,
          isChanged: false,
          configGroup: "Default",
          saveRecommended: false,
          isEditable: config.isEditable !== false,
          propertyDependsOn: config.propertyDependsOn || [],
          propertyDependedBy: config.propertyDependedBy || [],
          isPlaceholder: true, // Mark as placeholder
        };
      }

      // Clear all existing recommendations before generating new ones
      // This prevents stale recommendations from persisting
      const filteredRecommendations: { [key: string]: any } = {};
      Object.keys(recommededConfigsRef.current).forEach(key => {
        const recommendation = recommededConfigsRef.current[key];
        // Keep only placeholder entries to preserve original values
        if (recommendation.isPlaceholder) {
          filteredRecommendations[key] = recommendation;
        }
      });
      recommededConfigsRef.current = filteredRecommendations;

      set(config, "didUserOverrideValue", true);
      const configType = config.type || getConfigTagFromFileName(config.fileName || "");
      const old = config.oldValue;
      set(config, "oldValue", config.value);
      
      const changedConfig = {
        type: configType,
        name: config.propertyName,
        old_value: old ? config.previousValue : old,
      };
      
      if (isAlwaysProcessedConfigType(configType)) {
        setCurrentlyChangedConfig({
          name: config.propertyName,
          fileName: config.fileName || `${configType}.xml`,
        });
        await loadConfigRecommendations([changedConfig], configProperties);
        return;
      } else {
        setCurrentlyChangedConfig(null);
      }
      
      if (hasPropertyDependedBy(config)) {
        loadConfigRecommendations([changedConfig], configProperties);
        return;
      }
    },
    800
  );

  function getComponentsBlueprint() {
    const allHostComponents = flatten(map(hostComponents, "host_components"));
    const components = flatten(map(allHostComponents, "HostRoles"));
    forEach(components, (comp: any) => {
      comp.hostName = comp.host_name;
      comp.componentName = comp.component_name;
    });

    const uniqueHosts = uniq([...map(components, "hostName"), ...allHostNames]);
    const mappedComponents = groupPropertyValues(components, "hostName");
    
    const clientComponents: string[] = [];
    services.forEach((service: any) => {
      const serviceComponents = service.components || [];
      serviceComponents.forEach((component: any) => {
        const componentName = component.StackServiceComponents?.component_name;
        if (componentName && componentName.includes('_CLIENT')) {
          clientComponents.push(componentName);
        }
      });
    });
    
    const commonClientComponents = ["KERBEROS_CLIENT", "TRINO_CLI"];
    commonClientComponents.forEach(comp => {
      if (!clientComponents.includes(comp)) {
        clientComponents.push(comp);
      }
    });
    
    const res = {
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] },
    };
    uniqueHosts.forEach(function (host, i) {
      var group_name = "host-group-" + (i + 1);

      // Get existing components for this host
      const existingComponents = mappedComponents[host]
        ? mappedComponents[host].map(function (c: any) {
            return { name: get(c, "componentName") };
          })
        : [];
      
      // Add CLIENT components dynamically from service models
      const allComponents = [...existingComponents];
      clientComponents.forEach(clientComponent => {
        // Only add if not already present
        if (!allComponents.some(comp => comp.name === clientComponent)) {
          allComponents.push({ name: clientComponent });
        }
      });

      res.blueprint.host_groups.push({
        name: group_name,
        components: allComponents,
      } as never);

      res.blueprint_cluster_binding.host_groups.push({
        name: group_name,
        hosts: [{ fqdn: host }],
      } as never);
    });
    return res;
  }

  async function loadConfigRecommendations(
    changedConfigs: any[],
    configProperties: ConfigPropertiesType
  ) {
    if (!changedConfigs || changedConfigs.length === 0 || !configProperties) {
      return;
    }
    setProcessingConfig(true);
    setRecommendationsInProgress(true);
    try {
      let recommendationsInPayload: any = !isWizard()
        ? getComponentsBlueprint()
        : recommendationsDataToSend || {};
      recommendationsInPayload.blueprint.configurations =
        buildConfigsJSON(configProperties);

      let hostNames: string[] = !isWizard() ? allHostNames : HOSTS || [];

      const payload = {
        recommend: "configuration-dependencies",
        hosts: hostNames,
        services: installedServices || serviceNames,
        changed_configurations: changedConfigs?.map((config) => ({
          type: config.type,
          name: config.name,
          old_value: config.old_value,
        })),
        user_context: {
          operation: "EditConfig",
        },
        recommendations: recommendationsInPayload,
        ...(isWizard() ? {} : { serviceName: serviceName }),
        clusterId: clusterId || null,
        autoComplete: "false",
        configsResponse: "false",
      };
      const recommendationsResponse = await ConfigsApi.getRecommendations(
        isWizard() ? STACK : stack,
        isWizard() ? VERSION : versionNum,
        payload
      );
      processRecommendations(recommendationsResponse, configProperties);
    } catch (error) {
      console.error("Error getting recommendations for config change:", error);
    } finally {
      setRecommendationsInProgress(false);
      setProcessingConfig(false);
    }
  }

  const loadAddServiceRecommendations = async (
    configProperties: ConfigPropertiesType,
    selectedServices: string[],
    recommendationsInPayload: any
  ) => {
    setProcessingConfig(true);
    setRecommendationsInProgress(true);

    let recommendationsDataToSend = recommendationsInPayload || {};
    
    const existingConfigs = buildConfigsJSON(configProperties, false);
    
    // Add default configurations for newly selected services
    const extractNewServiceConfigs = async () => {
      const newServiceConfigs: any = {};
      
      // Find services that are being newly added (not in installedServices)
      const newServices = selectedServices.filter(service => 
        !installedServices?.includes(service)
      );
      
      if (newServices.length > 0) {
        try {
          // Fetch default configurations for new services from stack
          const stackConfigs = await WizardApi.getStackConfigurations(
            stack,
            versionNum,
            newServices.join(','),
            'configurations/StackConfigurations/property_name,configurations/StackConfigurations/property_value,configurations/StackConfigurations/type'
          );
          
          if (stackConfigs?.items) {
            stackConfigs.items.forEach((serviceItem: any) => {
              if (serviceItem.configurations) {
                serviceItem.configurations.forEach((config: any) => {
                  const configType = config.StackConfigurations.type;
                  const propertyName = config.StackConfigurations.property_name;
                  const propertyValue = config.StackConfigurations.property_value;
                  
                  if (!newServiceConfigs[configType]) {
                    newServiceConfigs[configType] = { properties: {} };
                  }
                  
                  newServiceConfigs[configType].properties[propertyName] = propertyValue;
                });
              }
            });
          }
        } catch (error) {
          console.error("Error fetching default configurations for new services:", error);
        }
      }
      
      return newServiceConfigs;
    };
        
    // Add Kerberos related configs - extract from existing configProperties
    const extractKerberosConfigs = () => {
      const kerberosConfigs: any = {};
      
      const hasKerberosService = selectedServices.includes('KERBEROS') || installedServices?.includes('KERBEROS');
      
      if (hasKerberosService) {
        for (const serviceName in configProperties) {
          if (configProperties[serviceName]['kerberos-env']) {
            kerberosConfigs['kerberos-env'] = configProperties[serviceName]['kerberos-env'];
          }
          if (configProperties[serviceName]['krb5-conf']) {
            kerberosConfigs['krb5-conf'] = configProperties[serviceName]['krb5-conf'];
          }
        }
      }
      
      return kerberosConfigs;
    };

    // Add cluster-env configuration - fetch from cluster API
    const extractClusterEnvConfigs = async () => {
      try {
        const clusterName = await ClusterApi.getClusterName();
        if (clusterName) {
          const clusterEnvProperties = await getClusterEnvProperties(clusterName);
          return {
            'cluster-env': {
              properties: clusterEnvProperties
            }
          };
        }
      } catch (error) {
        console.error("Error fetching cluster-env configs:", error);
      }
      return {};
    };
    
    const newServiceConfigs = await extractNewServiceConfigs();
    const kerberosConfigs = extractKerberosConfigs();
    const clusterEnvConfigs = await extractClusterEnvConfigs();
    
    const additionalConfigs = {
      ...newServiceConfigs,
      ...kerberosConfigs,
      ...clusterEnvConfigs,
    };
    
    recommendationsDataToSend.blueprint.configurations = {
      ...existingConfigs,
      ...additionalConfigs
    };
    
    if (!recommendationsDataToSend.blueprint_cluster_binding) {
      recommendationsDataToSend.blueprint_cluster_binding = {
        host_groups: recommendationsDataToSend.blueprint.host_groups.map((hostGroup: any, index: number) => ({
          name: hostGroup.name,
          hosts: [{ fqdn: allHostNames[index] || allHostNames[0] }]
        }))
      };
    }
    
    try {
      const allServices = [...(installedServices || []), ...selectedServices, "MISC"].filter((service, index, arr) => 
        arr.indexOf(service) === index // Remove duplicates but keep MISC
      );
      
      const newServices = selectedServices; // selectedServices are already filtered to be new services
      const primaryNewService = newServices.length > 0 ? newServices[0] : serviceName;
      
      const operationDetails = newServices.length > 0 ? newServices.join(',') : (primaryNewService || "AddService");
      
      const payload = {
        recommend: "configurations",
        hosts: allHostNames,
        services: allServices,
        user_context: {
          operation: "AddService",
          operation_details: operationDetails
        },
        recommendations: recommendationsDataToSend,
        clusterId: clusterId,
        autoComplete: "false",
        configsResponse: "false",
      };
      
      const recommendationsResponse = await ConfigsApi.getRecommendations(
        stack,
        versionNum,
        payload
      );
      
      // Process stack configurations for new services before processing recommendations
      processNewServiceStackConfigs(newServiceConfigs, selectedServices);
      
      processRecommendations(recommendationsResponse, configProperties);
    } catch (error) {
      console.error("Error loading add service recommendations:", error);
    } finally {
      setRecommendationsInProgress(false);
      setProcessingConfig(false);
    }
  };

  return {
    loadRecommendationsForConfigOnLoad,
    onValueUpdate,
    recommendationsInProgress,
    processingConfig,
    processRecommendations,
    loadAddServiceRecommendations,
    recommendedChanges,
    setRecommendedChanges,
  };
}
export default useEnhancedConfigs;
