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

import { ambariApi } from "./config/axiosConfig";

export const AlertsApi = {
  getAlerts: async (
      clusterName: string,
      fields: string,
      time:number
  ) => {
    const url = `clusters/${clusterName}/alert_groups?fields=${fields}&_=${time}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  getAlertSummary: async function (
      clusterName: string,
      time: number
  ) {
    const url = `clusters/${clusterName}/alerts?format=groupedSummary&_=${time}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data
  },
  getAlertsListDetailed: async function (
      clusterName: string,
  ) {
    const fields=`Alert/component_name,Alert/definition_id,Alert/definition_name,Alert/host_name,Alert/id,Alert/instance,Alert/label,Alert/latest_timestamp,Alert/maintenance_state,Alert/original_timestamp,Alert/scope,Alert/service_name,Alert/state,Alert/text,Alert/repeat_tolerance,Alert/repeat_tolerance_remaining&Alert/state.in(CRITICAL,WARNING)&Alert/maintenance_state.in(OFF)&from=0&page_size=100`
    let url = `clusters/${clusterName}/alerts?fields=${fields}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data
  },

  getAlertsList: async function (
      clusterName: string,
      time: number,
      hostName?: string,
      componentName?:string
  ) {
    let url = `clusters/${clusterName}/alerts?fields=*&_=${time}`;
    if (hostName) {
      url += `&Alert/host_name=${hostName}`;
    }
    if (componentName) {
      url += `&Alert/component_name=${componentName}`;
    }
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data
  },
  getAlertDetails: async function (
    clusterName: string,
      alert_id: string,
    time: number
) {
  const url = `clusters/${clusterName}/alerts?fields=*&Alert/definition_id=${alert_id}&_=${time}`;
  const response = await ambariApi.request({
    url: url,
    method: "GET",
  });
  return response.data
  },
  getAlertDefinition: async function (
      clusterName: string,
      fields: string,
      time: number){
    const url = `clusters/${clusterName}/alert_definitions?fields=${fields}&_=${time}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data
  },
  
    getAlertsNotifications: async function (
        clusterName: string,
        fields: string,
        time: number=Date.now())
    {
        const url = `clusters/${clusterName}/alerts?fields=${fields}&_=${time}`;
        const response = await ambariApi.request({
            url: url,
            method: "GET",
        });
        return response.data
    },
  getGroupFormattedAlertsNotifications: async function (
    clusterName: string,
    time: number=Date.now())
{
    const url = `clusters/${clusterName}/alerts?format=groupedSummary&_=${time}`;
    const response = await ambariApi.request({
        url: url,
        method: "GET",
    });
    return response.data
},

  // ADDED: Get alerts with maintenance mode filtering (following Ember pattern)
  getGroupFormattedAlertsNotificationsWithMaintenanceFilter: async function (
    clusterName: string,
    time: number=Date.now())
{
    // Following Ember pattern: &Alert/maintenance_state.in(OFF) - only get alerts NOT in maintenance mode
    const url = `clusters/${clusterName}/alerts?format=groupedSummary&Alert/maintenance_state.in(OFF)&_=${time}`;
    const response = await ambariApi.request({
        url: url,
        method: "GET",
    });
    return response.data
},

  getAlertGroups: async function (
    clusterName: string,
    time: number = Date.now()
  ) {
  const url = `clusters/${clusterName}/alert_groups?fields=*&_=${time}`;
  const response = await ambariApi.request({
    url: url,
    method: "GET",
  });
  return response.data;
},

createAlertGroup: async function (
    clusterName: string,
    groupData: {
      AlertGroup: {
        name: string;
        definitions?: (number | { id: number | string })[];
      }
    }
) {
  try {
    const url = `clusters/${clusterName}/alert_groups`;
    
    // Handle the definitions array properly
    const definitionIds: number[] = [];
    
    if (groupData.AlertGroup.definitions && groupData.AlertGroup.definitions.length > 0) {
      // Map to a completely new array with just numeric IDs
      groupData.AlertGroup.definitions.forEach((def: number | { id: number | string }) => {
        if (typeof def === 'number') {
          definitionIds.push(def);
        } else if (def && typeof def === 'object' && 'id' in def) {
          // Push just the ID, not the entire object
          definitionIds.push(Number(def.id));
        }
      });
    }
    
    // Create the minimal request payload with exactly the format required by the API
    const minimalPayload = JSON.stringify({
      AlertGroup: {
        name: groupData.AlertGroup.name,
        ...(definitionIds.length > 0 && { definitions: definitionIds })
      }
    });
    
    console.log('Creating alert group with payload:', minimalPayload);
    
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: JSON.parse(minimalPayload)
    });
    return response.data;
  } catch (error) {
    console.error('Error in createAlertGroup:', error);
    throw error;
  }
},

deleteAlertGroup: async function (
    clusterName: string,
    groupId: number
) {
  const url = `clusters/${clusterName}/alert_groups/${groupId}`;
  const response = await ambariApi.request({
    url: url,
    method: "DELETE"
  });
  return response.data;
},

renameAlertGroup: async function (
    clusterName: string,
    groupId: number,
    newName: string
) {
  try {
    // First get the current group data to get the definition IDs
    const currentGroupUrl = `clusters/${clusterName}/alert_groups/${groupId}?fields=*`;
    const currentGroup = await ambariApi.request({
      url: currentGroupUrl,
      method: "GET"
    });

    // Create a completely new array with just the IDs
    const definitionIds: number[] = [];
    if (currentGroup.data.AlertGroup.definitions) {
      // Map to a completely new array with just numeric IDs
      currentGroup.data.AlertGroup.definitions.forEach((def: number | { id: number | string }) => {
        if (typeof def === 'number') {
          definitionIds.push(def);
        } else if (def && typeof def.id === 'number') {
          // Push just the ID, not the entire object
          definitionIds.push(Number(def.id));
        }
      });
    }

    // Create the minimal request payload with exactly the format required by the API
    // Using a hardcoded structure to ensure no prototype inheritance or extra properties
    const minimalPayload = JSON.stringify({
      AlertGroup: {
        name: newName,
        definitions: definitionIds,
        targets: []
      }
    });

    console.log('Sending minimal request body:', minimalPayload);

    const url = `clusters/${clusterName}/alert_groups/${groupId}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: JSON.parse(minimalPayload)
    });
    return response.data;
  } catch (error) {
    console.error('Error in renameAlertGroup:', error);
    throw error;
  }
},

duplicateAlertGroup: async function (
    clusterName: string,
    sourceGroupId: number,
    newGroupName: string
) {
  // First get the source group details
  const sourceGroupUrl = `clusters/${clusterName}/alert_groups/${sourceGroupId}?fields=*`;
  const sourceGroupResponse = await ambariApi.request({
    url: sourceGroupUrl,
    method: "GET"
  });

  const sourceGroup = sourceGroupResponse.data.AlertGroup;

  // Create a completely new array with just the IDs
  const definitionIds: number[] = [];
  if (sourceGroup.definitions) {
    // Map to a completely new array with just numeric IDs
    sourceGroup.definitions.forEach((def: number | { id: number | string }) => {
      if (typeof def === 'number') {
        definitionIds.push(def);
      } else if (def && typeof def === 'object' && 'id' in def) {
        // Push just the ID, not the entire object
        definitionIds.push(Number(def.id));
      }
    });
  }

  // Create new group with same definitions (but only IDs)
  const createUrl = `clusters/${clusterName}/alert_groups`;
  const data = {
    AlertGroup: {
      name: newGroupName,
      definitions: definitionIds
    }
  };

  console.log('Sending duplicate group request body:', JSON.stringify(data));

  const response = await ambariApi.request({
    url: createUrl,
    method: "POST",
    data: data
  });

  return response.data;
},

// Alert Definition Management within Groups
addAlertDefinitionToGroup: async function (
    clusterName: string,
    groupId: number,
    definitionId: number
) {
  const url = `clusters/${clusterName}/alert_groups/${groupId}/alert_definitions/${definitionId}`;
  const response = await ambariApi.request({
    url: url,
    method: "POST"
  });
  return response.data;
},

removeAlertDefinitionFromGroup: async function (
    clusterName: string,
    groupId: number,
    definitionId: number
) {
  const url = `clusters/${clusterName}/alert_groups/${groupId}/alert_definitions/${definitionId}`;
  const response = await ambariApi.request({
    url: url,
    method: "DELETE"
  });
  return response.data;
},

// Notification Management
getNotifications: async function (
    _clusterName: string,
    time: number = Date.now()
  ) {
    const url = `alert_targets?fields=*&_=${time}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

createNotification: async function (
    _clusterName: string,
    notificationData: any
) {
  // Direct endpoint without clusters prefix
  const url = `alert_targets`;
  const response = await ambariApi.request({
    url: url,
    method: "POST",
    data: notificationData
  });
  return response;
},

updateNotification: async function (
    _clusterName: string,
    targetId: number,
    notificationData: any
) {
  // Direct endpoint without clusters prefix
  const url = `alert_targets/${targetId}`;
  const response = await ambariApi.request({
    url: url,
    method: "PUT",
    data: notificationData
  });
  return response;
},

deleteNotification: async function (
    _clusterName: string,
    targetId: number
) {
  // Direct endpoint without clusters prefix
  const url = `alert_targets/${targetId}`;
  const response = await ambariApi.request({
    url: url,
    method: "DELETE"
  });
  return response;
},

updateAlertDefinitionState: async function (
    clusterName: string,
    definitionId: number | string,
    enabled: boolean
) {
    const url = `clusters/${clusterName}/alert_definitions/${definitionId}`;
    const payload = {
      "AlertDefinition/enabled": enabled
    };
    
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: payload
    });
    return response.data;
  },

  // Update Alert Definition
  updateAlertDefinition: async function (
    clusterName: string,
    definitionId: number | string,
    data: { [key: string]: any }
  ) {
    const url = `clusters/${clusterName}/alert_definitions/${definitionId}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: data
    });
    return response.data;
  },

  // Alert Group Notification Management
addNotificationToGroup: async function (
    clusterName: string,
    groupId: number,
    targetId: number
) {
  const url = `clusters/${clusterName}/alert_groups/${groupId}/alert_targets/${targetId}`;
  const response = await ambariApi.request({
    url: url,
    method: "POST"
  });
  return response.data;
},

removeNotificationFromGroup: async function (
    clusterName: string,
    groupId: number,
    targetId: number
) {
  const url = `clusters/${clusterName}/alert_groups/${groupId}/alert_targets/${targetId}`;
  const response = await ambariApi.request({
    url: url,
    method: "DELETE"
  });
  return response.data;
},

// Update entire alert group
updateAlertGroup: async function (
    clusterName: string,
    groupId: number,
    groupData: {
      AlertGroup: {
        name: string;
        definitions?: (number | { id: number })[];
      }
    }
) {
  try {
    // Check if this is a temporary ID (from Date.now())
    // If it's a large number (> 1000000), it's likely a temporary ID and we should skip the update
    if (groupId > 1000000) {
      console.warn('Skipping update for temporary group ID:', groupId);
      return { success: false, message: 'Cannot update a temporary group. Save the group first.' };
    }

    // Create a completely new array with just the IDs
    const definitionIds: number[] = [];
    if (groupData.AlertGroup.definitions) {
      // Map to a completely new array with just numeric IDs
      groupData.AlertGroup.definitions.forEach((def: number | { id: number | string }) => {
        if (typeof def === 'number') {
          definitionIds.push(def);
        } else if (def && typeof def === 'object' && 'id' in def) {
          // Push just the ID, not the entire object
          definitionIds.push(Number(def.id));
        }
      });
    }

    // Create the minimal request payload with exactly the format required by the API
    const minimalPayload = JSON.stringify({
      AlertGroup: {
        name: groupData.AlertGroup.name,
        definitions: definitionIds
      }
    });

    console.log('Updating alert group with payload:', minimalPayload);

    const url = `clusters/${clusterName}/alert_groups/${groupId}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: JSON.parse(minimalPayload)
    });
    return response.data;
  } catch (error) {
    console.error('Error in updateAlertGroup:', error);
    throw error;
  }
  },

  // Get cluster configuration (including alert settings)
  getClusterConfig: async function (
    clusterName: string,
    configType: string = 'cluster-env'
  ) {
    const url = `clusters/${clusterName}/configurations?type=${configType}&fields=*`;
    const response = await ambariApi.request({
      url: url,
      method: "GET"
    });
    return response.data;
  },

  // Save Alert Settings (repeat tolerance)
  saveAlertSettings: async function (
    clusterName: string,
    alertRepeatTolerance: number | string
  ) {
    // First get the current cluster-env properties
    const currentConfigResponse = await this.getClusterConfig(clusterName, 'cluster-env');
    let currentProperties = {};
    
    if (currentConfigResponse && currentConfigResponse.items && currentConfigResponse.items.length > 0) {
      // Get the most recent cluster-env configuration
      const latestConfig = currentConfigResponse.items[0];
      currentProperties = latestConfig.properties || {};
    }

    // Update the alerts_repeat_tolerance property
    const updatedProperties = {
      ...currentProperties,
      alerts_repeat_tolerance: alertRepeatTolerance.toString()
    };

    const url = `clusters/${clusterName}`;
    const data = {
      Clusters: {
        desired_config: {
          type: "cluster-env",
          properties: updatedProperties
        }
      }
    };
    
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: data
    });
    return response.data;
  }
};
