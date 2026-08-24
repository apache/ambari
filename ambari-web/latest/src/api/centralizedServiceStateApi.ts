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

interface ServiceStateData {
  serviceName: string;
  state: string;
  alertsCount: number;
  hasCriticalAlerts: boolean;
}

class CentralizedServiceStateApi {
  private cache: Map<string, ServiceStateData> = new Map();
  private lastFetchTime: number = 0;
  private readonly CACHE_DURATION = 5000; // 5 seconds cache
  private subscribers: ((data: Map<string, ServiceStateData>) => void)[] = [];
  private pendingRequest: Promise<Map<string, ServiceStateData>> | null = null;

  /**
   * Calculate alert counts per service from alert summary and definitions (EmberJS pattern)
   * Matches alert_definition_summary_mapper.js logic
   *
   * @param alertSummary - Alert summary with grouped alerts (by definition_id)
   * @param alertDefinitions - Alert definitions with service_name mapping
   */
  private calculateServiceAlertCounts(
    alertSummary?: { alerts_summary_grouped: any[] },
    alertDefinitions?: any[]
  ): Map<string, { alertsCount: number, hasCriticalAlerts: boolean }> {
    const serviceAlerts = new Map<string, { alertsCount: number, hasCriticalAlerts: boolean }>();

    if (!alertSummary?.alerts_summary_grouped || !alertDefinitions) {
      return serviceAlerts;
    }

    // Create map of definition_id -> service_name
    const definitionIdToService = new Map<number, string>();
    alertDefinitions.forEach((def: any) => {
      if (def.id && def.service_name) {
        definitionIdToService.set(def.id, def.service_name);
      }
    });

    // Group alerts by service_name and count CRITICAL + WARNING
    alertSummary.alerts_summary_grouped.forEach((alert: any) => {
      const definitionId = alert.definition_id;
      if (!definitionId) return;

      const serviceName = definitionIdToService.get(definitionId);
      if (!serviceName) return;

      const criticalCount = alert.summary?.CRITICAL?.count || 0;
      const warningCount = alert.summary?.WARNING?.count || 0;
      const totalCount = criticalCount + warningCount;
      const hasCritical = criticalCount > 0;

      if (!serviceAlerts.has(serviceName)) {
        serviceAlerts.set(serviceName, { alertsCount: 0, hasCriticalAlerts: false });
      }

      const current = serviceAlerts.get(serviceName)!;
      current.alertsCount += totalCount;
      current.hasCriticalAlerts = current.hasCriticalAlerts || hasCritical;
    });

    return serviceAlerts;
  }

  /**
   * Fetches service states and calculates alert counts (EmberJS pattern)
   *
   * Alert counts come from /alerts?format=groupedSummary via AlertsContext (useAlerts hook),
   * not from a separate /alerts API call here - alertSummary/alertDefinitions carry that data.
   *
   * REQUEST DEDUPLICATION: If a request is already in progress, return the pending promise
   */
  async fetchAllServiceStatesAndAlerts(
    clusterName: string,
    alertSummary?: { alerts_summary_grouped: any[] },
    alertDefinitions?: any[]
  ): Promise<Map<string, ServiceStateData>> {
    const now = Date.now();

    // Return cached data if still fresh
    if (now - this.lastFetchTime < this.CACHE_DURATION && this.cache.size > 0) {
      return this.cache;
    }

    // REQUEST DEDUPLICATION: If a request is already pending, return that promise
    if (this.pendingRequest) {
      return this.pendingRequest;
    }

    const executeRequest = async (): Promise<Map<string, ServiceStateData>> => {
      try {
        const response = await ambariApi.request({
          url: `/clusters/${clusterName}/services?fields=ServiceInfo/state,ServiceInfo/maintenance_state&minimal_response=true`,
          method: "GET",
        });

        const newCache = new Map<string, ServiceStateData>();

        const serviceAlertCounts = this.calculateServiceAlertCounts(alertSummary, alertDefinitions);

        response.data.items?.forEach((service: any) => {
          const serviceName = service.ServiceInfo.service_name;
          const state = service.ServiceInfo.state;

          const alertData = serviceAlertCounts.get(serviceName) || { alertsCount: 0, hasCriticalAlerts: false };

          newCache.set(serviceName, {
            serviceName,
            state,
            alertsCount: alertData.alertsCount,
            hasCriticalAlerts: alertData.hasCriticalAlerts,
          });
        });

        this.cache = newCache;
        this.lastFetchTime = now;

        // Notify subscribers
        this.notifySubscribers();

        return this.cache;
      } catch (error) {
        console.error('Error fetching service states:', error);
        // Return existing cache on error
        return this.cache;
      } finally {
        // Clear pending request when done
        this.pendingRequest = null;
      }
    };

    // Set and execute pending request
    this.pendingRequest = executeRequest();
    return this.pendingRequest;
  }

  /**
   * Get service state data for a specific service
   */
  getServiceStateData(serviceName: string): ServiceStateData | null {
    return this.cache.get(serviceName) || null;
  }

  /**
   * Subscribe to service state updates
   */
  subscribe(callback: (data: Map<string, ServiceStateData>) => void): () => void {
    this.subscribers.push(callback);
    
    // Return unsubscribe function
    return () => {
      const index = this.subscribers.indexOf(callback);
      if (index > -1) {
        this.subscribers.splice(index, 1);
      }
    };
  }

  /**
   * Notify all subscribers of data updates
   */
  private notifySubscribers(): void {
    this.subscribers.forEach(callback => callback(this.cache));
  }

  /**
   * Clear cache (useful for testing or forced refresh)
   */
  clearCache(): void {
    this.cache.clear();
    this.lastFetchTime = 0;
  }
}

// Export singleton instance
export const centralizedServiceStateApi = new CentralizedServiceStateApi();
