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

import { serviceNameModelMapping } from "../../constants";

/**
 * Dynamically generates component actions mapping based on slave components in the service model
 * 
 * @param serviceName - The name of the service (e.g., "HDFS", "HBASE")
 * @param allServiceModels - The service models object from ServiceContext
 * @returns A mapping object similar to ComponentActionsMapping but generated dynamically
 */
export const getDynamicComponentActions = (() => {
  // Cache to store previous results for better performance
  const cache = new Map<string, any[]>();
  
  return (
    serviceName: string,
    allServiceModels: { [key: string]: any }
  ): any[] => {
    // Early return if required parameters are missing
    if (!serviceName || !allServiceModels) {
      return [];
    }

    // Check if we have a cached result for this service
    if (cache.has(serviceName)) {
      return cache.get(serviceName)!;
    }
    
    // Convert service name to lowercase to match the keys in allServiceModels
    const serviceKey = serviceName.toLowerCase();
    
    // Get the service model with additional fallback attempts
    let serviceModel = allServiceModels[serviceKey];
    
    // If not found with lowercase, try with original case
    if (!serviceModel) {
      serviceModel = allServiceModels[serviceNameModelMapping?.[serviceName]];
    }

    
    // If service model still doesn't exist, return empty array but don't cache the result
    // This prevents caching of failed lookups during race conditions
    if (!serviceModel) {
      console.warn(`Service model for ${serviceName} not found after all fallback attempts. This might be a race condition - service models may not be loaded yet.`);
      return [];
    }
    
    // Get slave components with null safety
    const slaveComponents = serviceModel.slaveComponents || [];
    
    // Create the component actions array
    const componentActions: any[] = [];
    
    // Process slave components to create restart actions
    slaveComponents.forEach((component: any) => {
      if (component && component.componentName && component.displayName) {
        // Create a display name for the restart action
        const displayName = component.displayName;
        const pluralDisplayName = displayName.endsWith('s') ? displayName : `${displayName}s`;
        const restartActionName = `Restart ${pluralDisplayName}`;
        
        // Add to component actions
        componentActions.push({
          component: component.componentName,
          actionMap: {
            actionRestart: restartActionName,
          },
        });
      }
    });
    
    // Only cache the result if we successfully found the service model
    // This prevents caching empty results during race conditions
    if (serviceModel) {
      cache.set(serviceName, componentActions);
    }
    
    return componentActions;
  };
})();
