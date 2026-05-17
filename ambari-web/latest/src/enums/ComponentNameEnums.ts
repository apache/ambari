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

/**
 * Gets the mapping of slave component names to their plural display names for a given service.
 * This function replaces the static ComponentNameEnums with a dynamic approach.
 * 
 * @param serviceName - The name of the service (e.g., "HDFS", "HBASE")
 * @returns A mapping object of slave component names to their plural display names
 */
export const getSlaveComponentMapping = (() => {
  // Cache to store previous results for better performance
  const cache = new Map<string, { [key: string]: string }>();
  
  return (
    serviceName: string,
    allServiceModels: { [key: string]: any } = {}
  ): { [key: string]: string } => {
    // Check if we have a cached result for this service
    if (cache.has(serviceName)) {
      return cache.get(serviceName)!;
    }
    
    // Convert service name to lowercase to match the keys in allServiceModels
    const serviceKey = serviceName.toLowerCase();
    
    // Get the service model
    const serviceModel = allServiceModels[serviceKey];
    
    // If service model doesn't exist, return the static fallback mapping
    if (!serviceModel) {
      console.warn(`Service model for ${serviceName} not found, using static fallback`);
      return ComponentNameEnums;
    }
    
    // Create the mapping object
    const componentMapping: { [key: string]: string } = {};
    
    // Process only slave components
    if (serviceModel.slaveComponents) {
      serviceModel.slaveComponents.forEach((component: any) => {
        if (component.componentName && component.displayName) {
          // Pluralize the display name
          let pluralDisplayName = component.displayName;
          if (!pluralDisplayName.endsWith('s')) {
            pluralDisplayName += 's';
          }
          componentMapping[component.componentName] = pluralDisplayName;
        }
      });
    }
    
    // Merge with static mappings for backward compatibility
    const mergedMapping = { ...ComponentNameEnums, ...componentMapping };
    
    // Cache the result
    cache.set(serviceName, mergedMapping);
    
    return mergedMapping;
  };
})();

// Static component name mappings
export const ComponentNameEnums = {
  'DATANODE': "DataNodes",
  'RANGER_TAGSYNC': "Ranger TagSyncs",
  'JOURNALNODE': "JournalNodes",
  'PINOT_BROKER': "Pinot Brokers",
  'PINOT_MINION': "Pinot Minions",
  'PINOT_SERVER': "Pinot Servers",
};
