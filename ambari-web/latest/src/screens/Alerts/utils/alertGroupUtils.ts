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

import { AlertGroupItem, AlertDefinition } from '../types';
import { AlertsApi } from '../../../api/alertsApi';
import { ambariApi } from '../../../api/config/axiosConfig';

/**
 * Check if a group has been modified compared to the original
 */
export const isGroupModified = (group: AlertGroupItem, originalGroups: AlertGroupItem[]) => {
  const original = originalGroups.find(g => g.AlertGroup.id === group.AlertGroup.id);
  if (!original) return true;

  return (
    group.AlertGroup.name !== original.AlertGroup.name ||
    JSON.stringify(group.AlertGroup.definitions) !== JSON.stringify(original.AlertGroup.definitions) ||
    JSON.stringify(group.AlertGroup.targets) !== JSON.stringify(original.AlertGroup.targets)
  );
};

/**
 * Fetch alert groups from the API
 */
export const fetchAlertGroups = async (clusterName: string) => {
  if (!clusterName) {
    console.log('Skipping alert groups fetch - no cluster name');
    throw new Error('Cluster name is required');
  }

  console.log('Fetching alert groups for cluster:', clusterName);
  const response = await AlertsApi.getAlertGroups(clusterName);

  if (response && response.items) {
    // Process and set the alert groups
    const groups = response.items;

    // Sort groups by name
    groups.sort((a: AlertGroupItem, b: AlertGroupItem) => {
      // Default group should always be first
      if (a.AlertGroup.default) return -1;
      if (b.AlertGroup.default) return 1;

      // Otherwise sort alphabetically
      return a.AlertGroup.name.localeCompare(b.AlertGroup.name);
    });

    return groups;
  }
  
  return [];
};

/**
 * Fetch alert definitions from the API
 */
export const fetchAlertDefinitions = async (clusterName: string) => {
  if (!clusterName) {
    console.error('Cannot fetch alert definitions: cluster name is missing');
    throw new Error('Cluster name missing. Please ensure you are viewing alerts for a specific cluster.');
  }

  console.log('Fetching alert definitions for cluster:', clusterName);
  const response = await AlertsApi.getAlertDefinition(
    clusterName,
    'AlertDefinition/component_name,AlertDefinition/description,AlertDefinition/enabled,AlertDefinition/id,AlertDefinition/label,AlertDefinition/name,AlertDefinition/service_name',
    Date.now()
  );
  
  if (response && response.items) {
    const definitions = response.items.map((item: any) => item.AlertDefinition);

    // Sort definitions by service name and then by name
    definitions.sort((a: AlertDefinition, b: AlertDefinition) => {
      if (a.service_name !== b.service_name) {
        return a.service_name.localeCompare(b.service_name);
      }
      return (a.label || a.name).localeCompare(b.label || b.name);
    });

    return definitions;
  }
  
  return [];
};

/**
 * Create a new alert group
 */
export const createAlertGroup = async (clusterName: string, name: string, definitions: number[] = []) => {
  const payload = {
    AlertGroup: {
      name,
      definitions
    }
  };
  
  return await AlertsApi.createAlertGroup(clusterName, payload);
};

/**
 * Update an existing alert group
 */
export const updateAlertGroup = async (clusterName: string, groupId: number, name: string, definitions: number[] = [], targets: number[] = []) => {
  // Create a completely new object with ONLY the required fields
  const alertGroupData: any = {
    name
  };

  // Only include definitions if there are any
  if (definitions && definitions.length > 0) {
    alertGroupData.definitions = definitions;
  }

  // Only include targets if there are any
  if (targets && targets.length > 0) {
    alertGroupData.targets = targets;
  }

  const requestBody = {
    AlertGroup: alertGroupData
  };

  console.log(`Updating group ${groupId} with payload:`, JSON.stringify(requestBody));

  // Make the PUT request directly
  const url = `clusters/${clusterName}/alert_groups/${groupId}`;
  return await ambariApi.request({
    url: url,
    method: "PUT",
    data: requestBody
  });
};

/**
 * Delete an alert group
 */
export const deleteAlertGroup = async (clusterName: string, groupId: number) => {
  return await AlertsApi.deleteAlertGroup(clusterName, groupId);
};

/**
 * Extract definition IDs from various formats
 */
export const extractDefinitionIds = (definitions: any[]): number[] => {
  if (!Array.isArray(definitions)) return [];
  
  return definitions.map(def => {
    if (typeof def === 'number') {
      return def;
    } else if (def && typeof def === 'object') {
      if ('id' in def && typeof def.id === 'number') {
        return def.id;
      } else if (def.AlertDefinition && typeof def.AlertDefinition.id === 'number') {
        return def.AlertDefinition.id;
      }
    }
    console.warn('Could not extract ID from definition:', def);
    return null;
  }).filter((id): id is number => id !== null);
};

/**
 * Extract target IDs from various formats
 */
export const extractTargetIds = (targets: any[]): number[] => {
  if (!Array.isArray(targets)) return [];
  
  return targets.map(target => {
    if (typeof target === 'number') {
      return target;
    } else if (target && typeof target.id === 'number') {
      return target.id;
    }
    return null;
  }).filter((id): id is number => id !== null);
};

/**
 * Filter available definitions that are not already in the group
 */
export const filterAvailableDefinitions = (
  allDefinitions: AlertDefinition[], 
  groupDefinitions: any[]
): AlertDefinition[] => {
  if (!Array.isArray(groupDefinitions)) {
    return allDefinitions;
  }
  
  // Extract IDs of definitions already in the group
  const existingIds = new Set(
    groupDefinitions.map(d => {
      if (typeof d === 'number') {
        return d;
      } else if (d && typeof d === 'object') {
        if ('id' in d) {
          return d.id;
        } else if ('AlertDefinition' in d && d.AlertDefinition && 'id' in d.AlertDefinition) {
          return d.AlertDefinition.id;
        }
      }
      return null;
    }).filter(id => id !== null)
  );
  
  console.log('Existing definition IDs:', [...existingIds]);
  
  // Return definitions not already in the group
  return allDefinitions.filter(def => !existingIds.has(def.id));
};
