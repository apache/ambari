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

/**
 * Centralized Service Component API Manager
 * Mirrors Ember's updateServiceMetric pattern - makes one consolidated API call
 * and provides data to all consumers. Polling is handled by usePolling in ServiceContext.
 */
class CachedServiceApiManager {
  private static instance: CachedServiceApiManager;
  private pendingRequest: Promise<any> | null = null;
  private lastData: any = null;
  private subscribers = new Set<(data: any) => void>();

  static getInstance(): CachedServiceApiManager {
    if (!CachedServiceApiManager.instance) {
      CachedServiceApiManager.instance = new CachedServiceApiManager();
    }
    return CachedServiceApiManager.instance;
  }

  /**
   * Subscribe to component data updates - notified whenever fetchAllServiceComponents
   * returns fresh data, regardless of which caller initiated the request.
   * If data is already available, immediately notify the new subscriber.
   */
  subscribe(callback: (data: any) => void): () => void {
    this.subscribers.add(callback);
    if (this.lastData) {
      callback(this.lastData);
    }
    return () => this.subscribers.delete(callback);
  }

  private notifySubscribers(data: any): void {
    this.subscribers.forEach(cb => cb(data));
  }

  /**
   * Get all component data (last fetched)
   */
  getAllComponentData(): any {
    return this.lastData;
  }

  /**
   * Get component data for a specific service
   */
  getServiceComponentData(serviceName: string): any {
    if (!this.lastData?.items) return null;
    return this.lastData.items.filter(
      (item: any) => item.ServiceComponentInfo?.service_name === serviceName
    );
  }

  /**
   * Centralized API call for all service components.
   * No caching - always makes a real API call (like Ember).
   * REQUEST DEDUPLICATION: If a request is already in progress, return the pending promise.
   */
  async fetchAllServiceComponents(clusterName: string): Promise<any> {
    if (this.pendingRequest) {
      return this.pendingRequest;
    }

    try {
      const fields = `ServiceComponentInfo/service_name,host_components/HostRoles/display_name,host_components/HostRoles/host_name,host_components/HostRoles/public_host_name,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,host_components/HostRoles/stale_configs,host_components/HostRoles/ha_state,host_components/HostRoles/desired_admin_state,host_components/metrics/jvm/memHeapUsedM,host_components/metrics/jvm/HeapMemoryMax,host_components/metrics/jvm/HeapMemoryUsed,host_components/metrics/jvm/memHeapCommittedM,host_components/metrics/mapred/jobtracker/trackers_decommissioned,host_components/metrics/cpu/cpu_wio,host_components/metrics/rpc/client/RpcQueueTime_avg_time,host_components/metrics/dfs/FSNamesystem/*,host_components/metrics/dfs/namenode/Version,host_components/metrics/dfs/namenode/LiveNodes,host_components/metrics/dfs/namenode/DeadNodes,host_components/metrics/dfs/namenode/DecomNodes,host_components/metrics/dfs/namenode/TotalFiles,host_components/metrics/dfs/namenode/UpgradeFinalized,host_components/metrics/dfs/namenode/Safemode,host_components/metrics/runtime/StartTime,host_components/metrics/hbase/master/IsActiveMaster,host_components/metrics/hbase/master/MasterStartTime,host_components/metrics/hbase/master/MasterActiveTime,host_components/metrics/hbase/master/AverageLoad,host_components/metrics/master/AssignmentManager/ritCount,host_components/metrics/dfs/namenode/ClusterId,host_components/processes/HostComponentProcess,host_components/metrics/yarn/Queue,host_components/metrics/yarn/ClusterMetrics/NumActiveNMs,host_components/metrics/yarn/ClusterMetrics/NumLostNMs,host_components/metrics/yarn/ClusterMetrics/NumUnhealthyNMs,host_components/metrics/yarn/ClusterMetrics/NumRebootedNMs,host_components/metrics/yarn/ClusterMetrics/NumDecommissionedNMs,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name&minimal_response=true`;

      this.pendingRequest = ServiceApi.getAllServiceComponentsListAndInitialMetrics(
        clusterName,
        fields
      );

      const response = await this.pendingRequest;

      if (response?.data?.items) {
        this.lastData = response.data;
        // Notify all subscribers (including ServiceContext) so state updates flow
        // regardless of which caller initiated this fetch
        this.notifySubscribers(response.data);
        return response.data;
      }

      return null;
    } catch (error) {
      console.error('Error fetching service components:', error);
      return null;
    } finally {
      this.pendingRequest = null;
    }
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
}

// Export singleton instance
export const cachedServiceApi = CachedServiceApiManager.getInstance();

// Export the class for testing
export { CachedServiceApiManager };
