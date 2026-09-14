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
 * Makes one consolidated API call and provides component state to all consumers.
 * Polling is handled by usePolling in ServiceContext.
 */
class CachedServiceApiManager {
  private static instance: CachedServiceApiManager;
  private entries = new Map<string, {
    pendingRequest: Promise<any> | null;
    lastData: any;
    subscribers: Set<(data: any) => void>;
  }>();

  private entry(runtimeKey: string) {
    let entry = this.entries.get(runtimeKey);
    if (!entry) {
      entry = { pendingRequest: null, lastData: null, subscribers: new Set() };
      this.entries.set(runtimeKey, entry);
    }
    return entry;
  }

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
  subscribe(runtimeKey: string, callback: (data: any) => void): () => void {
    const entry = this.entry(runtimeKey);
    entry.subscribers.add(callback);
    if (entry.lastData) {
      callback(entry.lastData);
    }
    return () => entry.subscribers.delete(callback);
  }

  private notifySubscribers(entry: { subscribers: Set<(data: any) => void> }, data: any): void {
    entry.subscribers.forEach(cb => cb(data));
  }

  /**
   * Get all component data (last fetched)
   */
  getAllComponentData(runtimeKey: string): any {
    return this.entry(runtimeKey).lastData;
  }

  /**
   * Get component data for a specific service
   */
  getServiceComponentData(runtimeKey: string, serviceName: string): any {
    const lastData = this.entry(runtimeKey).lastData;
    if (!lastData?.items) return null;
    return lastData.items.filter(
      (item: any) => item.ServiceComponentInfo?.service_name === serviceName
    );
  }

  /**
   * Centralized API call for all service components.
   * No caching - always makes a real API call (like Ember).
   * REQUEST DEDUPLICATION: If a request is already in progress, return the pending promise.
   */
  fetchAllServiceComponents(clusterName: string, runtimeKey: string): Promise<any> {
    const entry = this.entry(runtimeKey);
    if (entry.pendingRequest) {
      return entry.pendingRequest;
    }

    const fields = `ServiceComponentInfo/service_name,host_components/HostRoles/display_name,host_components/HostRoles/host_name,host_components/HostRoles/public_host_name,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,host_components/HostRoles/stale_configs,host_components/HostRoles/ha_state,host_components/HostRoles/desired_admin_state,host_components/metrics/dfs/FSNamesystem/HAState,host_components/metrics/hbase/master/IsActiveMaster,host_components/processes/HostComponentProcess,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name&minimal_response=true`;

    const operation = ServiceApi.getAllServiceComponents(
        clusterName,
        fields
      ).then((response) => {
      if (this.entries.get(runtimeKey) !== entry) return null;
      if (response?.data?.items) {
        entry.lastData = response.data;
        // Notify all subscribers (including ServiceContext) so state updates flow
        // regardless of which caller initiated this fetch
        this.notifySubscribers(entry, response.data);
        return response.data;
      }

      return null;
    }).catch((error) => {
      console.error('Error fetching service components:', error);
      return null;
    }).finally(() => {
      if (this.entries.get(runtimeKey) === entry) entry.pendingRequest = null;
    });
    entry.pendingRequest = operation;
    return operation;
  }

  clear(runtimeKey?: string): void {
    if (runtimeKey) this.entries.delete(runtimeKey);
    else this.entries.clear();
  }

}

// Export singleton instance
export const cachedServiceApi = CachedServiceApiManager.getInstance();

// Export the class for testing
export { CachedServiceApiManager };
