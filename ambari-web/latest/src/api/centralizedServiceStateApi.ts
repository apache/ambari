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

  /**
   * Fetches all service states and alerts in a single API call (Ember.js pattern)
   * This replaces individual ServiceApi.getServiceState() calls
   */
  async fetchAllServiceStatesAndAlerts(clusterName: string): Promise<Map<string, ServiceStateData>> {
    const now = Date.now();
    
    // Return cached data if still fresh
    if (now - this.lastFetchTime < this.CACHE_DURATION && this.cache.size > 0) {
      return this.cache;
    }

    try {
      const response = await ambariApi.request({
        url: `/clusters/${clusterName}/services?fields=ServiceInfo/state,ServiceInfo/maintenance_state&minimal_response=true`,
        method: "GET",
      });

      // Get alerts with proper maintenance state filtering (following Ember pattern)
      // This API call will return NO items for services/components in maintenance mode
      const alertsResponse = await ambariApi.request({
        url: `/clusters/${clusterName}/alerts?fields=Alert/service_name,Alert/state&Alert/state.in(CRITICAL,WARNING)&Alert/maintenance_state.in(OFF)&minimal_response=true`,
        method: "GET",
      });

      const newCache = new Map<string, ServiceStateData>();

      // Count alerts per service - API already filters out maintenance mode alerts
      const serviceAlertsCount: { [key: string]: { critical: number; warning: number } } = {};
      
      alertsResponse.data.items?.forEach((alert: any) => {
        const serviceName = alert.Alert?.service_name;
        const alertState = alert.Alert?.state;
        
        if (serviceName && alertState) {
          if (!serviceAlertsCount[serviceName]) {
            serviceAlertsCount[serviceName] = { critical: 0, warning: 0 };
          }
          
          if (alertState === 'CRITICAL') {
            serviceAlertsCount[serviceName].critical++;
          } else if (alertState === 'WARNING') {
            serviceAlertsCount[serviceName].warning++;
          }
        }
      });

      response.data.items?.forEach((service: any) => {
        const serviceName = service.ServiceInfo.service_name;
        const state = service.ServiceInfo.state;
        
        // Use API-filtered alert counts (already excludes maintenance mode alerts)
        const serviceAlerts = serviceAlertsCount[serviceName] || { critical: 0, warning: 0 };
        const criticalAlerts = serviceAlerts.critical;
        const warningAlerts = serviceAlerts.warning;
        
        const alertsCount = criticalAlerts + warningAlerts;
        const hasCriticalAlerts = criticalAlerts > 0;

        newCache.set(serviceName, {
          serviceName,
          state,
          alertsCount,
          hasCriticalAlerts,
        });
      });

      this.cache = newCache;
      this.lastFetchTime = now;

      // Notify subscribers
      this.notifySubscribers();

      return this.cache;
    } catch (error) {
      // Return existing cache on error
      return this.cache;
    }
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
