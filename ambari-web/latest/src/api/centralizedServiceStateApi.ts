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
  maintenance_state: string;
  alertsCount: number;
  hasCriticalAlerts: boolean;
}

interface ClusterServiceStateEntry {
  cache: Map<string, ServiceStateData>;
  lastFetchTime: number;
  subscribers: Array<(data: Map<string, ServiceStateData>) => void>;
  pendingRequest: Promise<Map<string, ServiceStateData>> | null;
}

export class CentralizedServiceStateApi {
  private readonly CACHE_DURATION = 5000; // 5 seconds cache
  private entries = new Map<string, ClusterServiceStateEntry>();

  private entry(runtimeKey: string): ClusterServiceStateEntry {
    let entry = this.entries.get(runtimeKey);
    if (!entry) {
      entry = {
        cache: new Map(),
        lastFetchTime: 0,
        subscribers: [],
        pendingRequest: null,
      };
      this.entries.set(runtimeKey, entry);
    }
    return entry;
  }

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
    runtimeKey: string,
    alertSummary?: { alerts_summary_grouped: any[] },
    alertDefinitions?: any[]
  ): Promise<Map<string, ServiceStateData>> {
    const now = Date.now();
    const entry = this.entry(runtimeKey);

    // Return cached data if still fresh
    if (now - entry.lastFetchTime < this.CACHE_DURATION && entry.cache.size > 0) {
      return entry.cache;
    }

    // REQUEST DEDUPLICATION: If a request is already pending, return that promise
    if (entry.pendingRequest) {
      return entry.pendingRequest;
    }

    const executeRequest = async (): Promise<Map<string, ServiceStateData>> => {
      try {
        const response = await ambariApi.request({
          url: `/clusters/${encodeURIComponent(clusterName)}/services?fields=ServiceInfo/state,ServiceInfo/maintenance_state&minimal_response=true`,
          method: "GET",
        });
        if (this.entries.get(runtimeKey) !== entry) return new Map();

        const newCache = new Map<string, ServiceStateData>();

        const serviceAlertCounts = this.calculateServiceAlertCounts(alertSummary, alertDefinitions);

        response.data.items?.forEach((service: any) => {
          const serviceName = service.ServiceInfo.service_name;
          const state = service.ServiceInfo.state;
          const maintenance_state = service.ServiceInfo.maintenance_state;

          const alertData = serviceAlertCounts.get(serviceName) || { alertsCount: 0, hasCriticalAlerts: false };

          newCache.set(serviceName, {
            serviceName,
            state,
            maintenance_state,
            alertsCount: alertData.alertsCount,
            hasCriticalAlerts: alertData.hasCriticalAlerts,
          });
        });

        entry.cache = newCache;
        entry.lastFetchTime = now;

        // Notify subscribers
        this.notifySubscribers(entry);

        return entry.cache;
      } catch (error) {
        console.error('Error fetching service states:', error);
        // Return existing cache on error
        return entry.cache;
      } finally {
        // Clear pending request when done
        if (this.entries.get(runtimeKey) === entry) entry.pendingRequest = null;
      }
    };

    // Set and execute pending request
    entry.pendingRequest = executeRequest();
    return entry.pendingRequest;
  }

  /**
   * Get service state data for a specific service
   */
  getServiceStateData(runtimeKey: string, serviceName: string): ServiceStateData | null {
    return this.entry(runtimeKey).cache.get(serviceName) || null;
  }

  /**
   * Subscribe to service state updates
   */
  subscribe(runtimeKey: string, callback: (data: Map<string, ServiceStateData>) => void): () => void {
    const entry = this.entry(runtimeKey);
    entry.subscribers.push(callback);
    
    // Return unsubscribe function
    return () => {
      const index = entry.subscribers.indexOf(callback);
      if (index > -1) {
        entry.subscribers.splice(index, 1);
      }
    };
  }

  /**
   * Notify all subscribers of data updates
   */
  private notifySubscribers(entry: ClusterServiceStateEntry): void {
    entry.subscribers.forEach(callback => callback(entry.cache));
  }

  /**
   * Set service state data directly from derived components data.
   * Allows ServiceContext to populate the cache without a separate /services API call.
   */
  setDerivedServiceStates(runtimeKey: string, data: Map<string, ServiceStateData>): void {
    const entry = this.entry(runtimeKey);
    entry.cache = data;
    entry.lastFetchTime = Date.now();
    this.notifySubscribers(entry);
  }

  /**
   * Clear cache (useful for testing or forced refresh)
   */
  clearCache(runtimeKey?: string): void {
    if (runtimeKey) this.entries.delete(runtimeKey);
    else this.entries.clear();
  }
}

// Export singleton instance
export const centralizedServiceStateApi = new CentralizedServiceStateApi();
