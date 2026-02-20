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

import { get } from "lodash";
import ConfigGroupApi from "../api/configGroupApi";

/**
 * Sync locally stored config groups to the server after cluster installation
 * This function should be called after cluster is successfully installed
 * 
 * @param clusterName - The name of the newly created cluster
 * @param wizardState - The wizard state containing config group data
 */
export const syncConfigGroupsToServer = async (clusterName: string, wizardState: any) => {
  try {
    console.log("Syncing config groups to server after cluster installation...");
    
    // Get stored config groups from wizard state
    const storedConfigGroups = get(wizardState, "clusterCreationSteps.step7.data.configGroupData.items", []);
    
    if (storedConfigGroups.length === 0) {
      console.log("No config groups to sync");
      return;
    }
    
    console.log(`Syncing ${storedConfigGroups.length} config groups to server...`);
    
    // Create API calls to add each config group to the server (including default groups)
    // Default groups need to be created explicitly since they were created locally during installation
    const syncPromises = storedConfigGroups.map(async (configGroup: any) => {
      try {
        const configGroupData = {
          ConfigGroup: {
            description: get(configGroup, "ConfigGroup.description", ""),
            desired_configs: get(configGroup, "ConfigGroup.desired_configs", []),
            group_name: get(configGroup, "ConfigGroup.group_name", ""),
            hosts: get(configGroup, "ConfigGroup.hosts", []).map((host: any) => ({
              host_name: typeof host === 'string' ? host : get(host, "host_name", "")
            })),
            service_name: get(configGroup, "ConfigGroup.service_name", "") || get(configGroup, "ConfigGroup.tag", ""),
            tag: get(configGroup, "ConfigGroup.tag", "")
          }
        };
        
        console.log(`Creating config group: ${configGroupData.ConfigGroup.group_name} for service: ${configGroupData.ConfigGroup.service_name || configGroupData.ConfigGroup.tag}`);
        
        await ConfigGroupApi.addConfigGroup(clusterName, [configGroupData]);
        
        console.log(`Successfully created config group: ${configGroupData.ConfigGroup.group_name}`);
      } catch (error) {
        console.error(`Failed to create config group: ${get(configGroup, "ConfigGroup.group_name", "")}`, error);
        // Don't throw here - we want to continue syncing other groups even if one fails
      }
    });
    
    // Wait for all config groups to be synced
    await Promise.all(syncPromises);
    
    console.log("Config groups sync completed");
    
  } catch (error) {
    console.error("Error syncing config groups to server:", error);
    // Don't throw the error - we don't want to break the installation completion
    // Config groups can be created manually later if needed
  }
};

/**
 * Get config groups from local storage during cluster installation
 * This handles the case when page is reloaded during installation
 * 
 * @param serviceName - The service name to filter config groups
 * @param wizardState - The wizard state containing config group data
 * @param hostsList - List of host names for default group (from wizard context)
 */
export const getLocalConfigGroups = (serviceName: string, wizardState: any, hostsList: string[] = []) => {
  try {
    // Get stored config groups from wizard state
    const storedConfigGroups = wizardState ? 
      get(wizardState, "clusterCreationSteps.step7.data.configGroupData.items", []) : [];
    
    // Filter config groups for the current service
    const serviceConfigGroups = storedConfigGroups.filter((group: any) => 
      get(group, "ConfigGroup.service_name", "") === serviceName ||
      get(group, "ConfigGroup.tag", "") === serviceName
    );
    
    // If we have stored config groups for this service, return them
    if (serviceConfigGroups.length > 0) {
      return serviceConfigGroups;
    }
    
    // Otherwise, return default config group structure
    return [{
      ConfigGroup: {
        id: `${serviceName}_Default`,
        group_name: "Default",
        service_name: serviceName,
        tag: serviceName,
        description: `Default cluster level ${serviceName} configuration`,
        hosts: hostsList || [],
        desired_configs: [],
        is_default: true
      }
    }];
    
  } catch (error) {
    console.error("Error getting local config groups:", error);
    
    // Fallback to default config group
    return [{
      ConfigGroup: {
        id: `${serviceName}_Default`,
        group_name: "Default",
        service_name: serviceName,
        tag: serviceName,
        description: `Default cluster level ${serviceName} configuration`,
        hosts: hostsList || [],
        desired_configs: [],
        is_default: true
      }
    }];
  }
};
