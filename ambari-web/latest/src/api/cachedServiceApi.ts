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

import { ServiceApi } from "./serviceApi";
import { serviceCache } from "../Utils/cacheUtils";

/**
 * Centralized Service Component API Manager
 * Similar to Ember.js approach - makes one consolidated API call for all components
 * and provides cached data to individual service hooks
 */
class CachedServiceApiManager {
  private static instance: CachedServiceApiManager;
  private isPolling = false;
  private pollingInterval: NodeJS.Timeout | null = null;
  private subscribers = new Set<(data: any) => void>();
  private pendingRequest: Promise<any> | null = null;

  static getInstance(): CachedServiceApiManager {
    if (!CachedServiceApiManager.instance) {
      CachedServiceApiManager.instance = new CachedServiceApiManager();
    }
    return CachedServiceApiManager.instance;
  }

  /**
   * Subscribe to component data updates
   */
  subscribe(callback: (data: any) => void): () => void {
    this.subscribers.add(callback);
    return () => this.subscribers.delete(callback);
  }

  /**
   * Notify all subscribers of data updates
   */
  private notifySubscribers(data: any): void {
    this.subscribers.forEach(callback => callback(data));
  }

  /**
   * Get cached component data for a specific service
   */
  getServiceComponentData(serviceName: string): any {
    return serviceCache.get(`components_${serviceName.toLowerCase()}`);
  }

  /**
   * Get all cached component data
   */
  getAllComponentData(): any {
    return serviceCache.get('all_components_data');
  }

  /**
   * Centralized API call for all service components
   * Similar to Ember.js approach - one call for all services
   * REQUEST DEDUPLICATION: If a request is already in progress, return the pending promise
   */
  async fetchAllServiceComponents(clusterName: string, forceRefresh: boolean = false): Promise<any> {
    const cacheKey = 'all_components_data';

    // Check cache first (unless force refresh)
    if (!forceRefresh) {
      const cachedData = serviceCache.get(cacheKey);
      if (cachedData) {
        return cachedData;
      }
    }

    // REQUEST DEDUPLICATION: If a request is already pending, return that promise
    // This prevents multiple simultaneous API calls
    if (this.pendingRequest) {
      return this.pendingRequest;
    }

    try {

      // OPTIMIZED: Use the same comprehensive fields as the main ServiceContext for consistency
      const fields = `ServiceComponentInfo/service_name,host_components/HostRoles/display_name,host_components/HostRoles/host_name,host_components/HostRoles/public_host_name,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,host_components/HostRoles/stale_configs,host_components/HostRoles/ha_state,host_components/HostRoles/desired_admin_state,host_components/metrics/jvm/memHeapUsedM,host_components/metrics/jvm/HeapMemoryMax,host_components/metrics/jvm/HeapMemoryUsed,host_components/metrics/jvm/memHeapCommittedM,host_components/metrics/mapred/jobtracker/trackers_decommissioned,host_components/metrics/cpu/cpu_wio,host_components/metrics/rpc/client/RpcQueueTime_avg_time,host_components/metrics/dfs/FSNamesystem/*,host_components/metrics/dfs/namenode/Version,host_components/metrics/dfs/namenode/LiveNodes,host_components/metrics/dfs/namenode/DeadNodes,host_components/metrics/dfs/namenode/DecomNodes,host_components/metrics/dfs/namenode/TotalFiles,host_components/metrics/dfs/namenode/UpgradeFinalized,host_components/metrics/dfs/namenode/Safemode,host_components/metrics/runtime/StartTime,host_components/metrics/hbase/master/IsActiveMaster,host_components/metrics/hbase/master/MasterStartTime,host_components/metrics/hbase/master/MasterActiveTime,host_components/metrics/hbase/master/AverageLoad,host_components/metrics/master/AssignmentManager/ritCount,host_components/metrics/dfs/namenode/ClusterId,host_components/processes/HostComponentProcess,host_components/metrics/yarn/Queue,host_components/metrics/yarn/ClusterMetrics/NumActiveNMs,host_components/metrics/yarn/ClusterMetrics/NumLostNMs,host_components/metrics/yarn/ClusterMetrics/NumUnhealthyNMs,host_components/metrics/yarn/ClusterMetrics/NumRebootedNMs,host_components/metrics/yarn/ClusterMetrics/NumDecommissionedNMs,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name&minimal_response=true`;

      // Set pending request to prevent duplicate calls
      this.pendingRequest = ServiceApi.getAllServiceComponentsListAndInitialMetrics(
        clusterName,
        fields
      );

      const response = await this.pendingRequest;

      if (response?.data?.items) {

        // Cache the consolidated data
        serviceCache.set(cacheKey, response.data, 30000); // 30 second TTL

        // Also cache by individual service for quick access
        const serviceGroups = this.groupComponentsByService(response.data.items);
        Object.entries(serviceGroups).forEach(([serviceName, data]) => {
          serviceCache.set(`components_${serviceName.toLowerCase()}`, data, 30000);
        });

        // Notify subscribers immediately
        this.notifySubscribers(response.data);

        return response.data;
      }

      return null;
    } catch (error) {
      console.error('Error fetching service components:', error);
      return null;
    } finally {
      // Clear pending request when done (success or error)
      this.pendingRequest = null;
    }
  }

  /**
   * Group component data by service name
   */
  private groupComponentsByService(items: any[]): Record<string, any[]> {
    const groups: Record<string, any[]> = {};
    
    items.forEach(item => {
      const serviceName = item.ServiceComponentInfo?.service_name;
      if (serviceName) {
        if (!groups[serviceName]) {
          groups[serviceName] = [];
        }
        groups[serviceName].push(item);
      }
    });
    
    return groups;
  }

  /**
   * Start centralized polling - similar to Ember.js approach
   * Uses timeout-based polling to prevent overlapping requests
   */
  startPolling(clusterName: string, intervalMs: number = 5000): void {
    if (this.isPolling) return;

    this.isPolling = true;

    // Timeout-based polling to prevent overlapping requests
    const poll = async () => {
      if (!this.isPolling) return;

      try {
        await this.fetchAllServiceComponents(clusterName);
      } catch (error) {
        console.error('Polling error:', error);
      } finally {
        // Schedule next poll ONLY after current request completes
        if (this.isPolling) {
          this.pollingInterval = setTimeout(poll, intervalMs);
        }
      }
    };

    // Start initial poll
    poll();
  }

  /**
   * Pause polling without clearing the interval
   * Useful for temporarily stopping polling on specific pages
   */
  pausePolling(): void {
    if (this.pollingInterval) {
      clearTimeout(this.pollingInterval);
      this.pollingInterval = null;
    }
  }

  /**
   * Resume polling after it was paused
   */
  resumePolling(clusterName?: string, intervalMs: number = 5000): void {
    if (this.isPolling && !this.pollingInterval && clusterName) {
      const poll = async () => {
        if (!this.isPolling) return;

        try {
          await this.fetchAllServiceComponents(clusterName);
        } catch (error) {
          console.error('Polling error:', error);
        } finally {
          if (this.isPolling) {
            this.pollingInterval = setTimeout(poll, intervalMs);
          }
        }
      };

      poll();
    }
  }

  /**
   * Stop polling completely
   */
  stopPolling(): void {
    if (this.pollingInterval) {
      clearTimeout(this.pollingInterval);
      this.pollingInterval = null;
    }
    this.isPolling = false;
  }

  /**
   * Check if specific service data is available and fresh
   */
  hasServiceData(serviceName: string): boolean {
    return serviceCache.has(`components_${serviceName.toLowerCase()}`);
  }

  /**
   * Get metrics for a specific service component
   */
  getServiceMetrics(serviceName: string, componentName: string): any {
    const serviceData = this.getServiceComponentData(serviceName);
    if (!serviceData) return null;

    return serviceData.find((item: any) => 
      item.ServiceComponentInfo?.component_name === componentName
    );
  }

  /**
   * Clear all cached data
   */
  clearCache(): void {
    serviceCache.clear();
  }
}

// Export singleton instance
export const cachedServiceApi = CachedServiceApiManager.getInstance();

// Export the class for testing
export { CachedServiceApiManager };
