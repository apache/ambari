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

import { centralizedServiceStateApi } from "../api/centralizedServiceStateApi";
import { ServiceComponentMetricsEnums } from "../enums/ServiceComponentMetricsEnums";
import { cloneDeep, isEqual } from "lodash";

/**
 * Utility function to update service alerts and state using centralized API
 * This replaces individual ServiceApi.getServiceState() calls across all service updaters
 */
export const updateServiceAlertsAndStateFromCentralizedApi = (
  serviceName: string,
  serviceModelKey: string,
  allServiceModels: any,
  updateRegistry: Function
): boolean => {
  // Use centralized service state API instead of individual call
  const serviceStateData = centralizedServiceStateApi.getServiceStateData(serviceName);
  
  if (!serviceStateData || !allServiceModels[serviceModelKey]) {
    return false;
  }

  const { alertsCount, hasCriticalAlerts, state } = serviceStateData;

  if (!alertsCount && alertsCount !== 0) {
    return false;
  }

  const currentConfig = cloneDeep(allServiceModels[serviceModelKey]);

  // Update alerts and state based on service type
  currentConfig[ServiceComponentMetricsEnums.AMBARI_METRICS.hasCriticalAlerts] = hasCriticalAlerts;
  
  // Set service-specific metrics
  const serviceMetrics = ServiceComponentMetricsEnums[serviceName as keyof typeof ServiceComponentMetricsEnums];
  if (serviceMetrics) {
    if (serviceMetrics.alertsCount) {
      currentConfig[serviceMetrics.alertsCount] = alertsCount;
    }
    if (serviceMetrics.state) {
      currentConfig[serviceMetrics.state] = state;
    }
  }

  // Update if there are changes
  if (!isEqual(allServiceModels[serviceModelKey], currentConfig)) {
    allServiceModels[serviceModelKey].updateConfig(currentConfig);
    updateRegistry(allServiceModels);
    return true;
  }

  return false;
};
