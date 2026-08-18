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
  getHostAlertInstances: async function (
    clusterName: string,
    hostName: string,
    time: number = Date.now()
  ) {
    const response = await ambariApi.request({
      url: `/clusters/${encodeURIComponent(clusterName)}/alerts`,
      method: "GET",
      params: {
        fields: "*",
        "Alert/host_name": hostName,
        _: time,
      },
    });
    return response.data;
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

  getAlertDefinitionById: async function (
    clusterName: string,
    definitionId: number | string,
    time: number = Date.now(),
  ) {
    const response = await ambariApi.request({
      url: `/clusters/${encodeURIComponent(clusterName)}/alert_definitions/${encodeURIComponent(String(definitionId))}`,
      method: "GET",
      params: { fields: "*", _: time },
    });
    return response.data;
  },

  getAlertInstancesByDefinition: async function (
    clusterName: string,
    definitionId: number | string,
    time: number = Date.now(),
  ) {
    const response = await ambariApi.request({
      url: `/clusters/${encodeURIComponent(clusterName)}/alerts`,
      method: "GET",
      params: {
        fields: "*",
        "Alert/definition_id": String(definitionId),
        _: time,
      },
    });
    return response.data;
  },

  getAlertHistory: async function (
    clusterName: string,
    definitionName: string,
    since: number,
  ) {
    const url = `/clusters/${encodeURIComponent(clusterName)}/alert_history?` +
      `(AlertHistory/definition_name=${encodeURIComponent(definitionName)})&` +
      `(AlertHistory/timestamp>=${since})`;
    const response = await ambariApi.request({ url, method: "GET" });
    return response.data;
  },

  createAlertDefinition: async function (
    clusterName: string,
    definition: Record<string, unknown>,
  ) {
    const response = await ambariApi.request({
      url: `/clusters/${encodeURIComponent(clusterName)}/alert_definitions/`,
      method: "POST",
      data: definition,
    });
    return response.data;
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

  getGroupFormattedAlertsNotificationsWithMaintenanceFilter: async function (
    clusterName: string,
    time: number=Date.now())
{
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
  groupData: { AlertGroup: { name: string; definitions: number[]; targets: number[] } },
) {
  const response = await ambariApi.request({
    url: `clusters/${clusterName}/alert_groups`,
    method: "POST",
    data: groupData,
  });
  return response.data;
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

// Update entire alert group
updateAlertGroup: async function (
  clusterName: string,
  groupId: number,
  groupData: { AlertGroup: { name: string; definitions: number[]; targets: number[] } },
) {
  const response = await ambariApi.request({
    url: `clusters/${clusterName}/alert_groups/${groupId}`,
    method: "PUT",
    data: groupData,
  });
  return response.data;
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
