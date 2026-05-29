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

export interface HostInfo {
  hostName: string;
  publicHostName: string;
  osType: string;
  ip: string;
  rack: string;
  diskTotal: number;
  diskFree: number;
  cpuSystem: number;
  cpuUser: number;
  memTotal: number;
  memFree: number;
  hostComponents: string[];
}

export interface RackInfo {
  name: string;
  rackId: string;
  hosts: HostInfo[];
  isLoaded: boolean;
  index: number;
}

export interface HeatmapMetric {
  name: string;
  units?: string;
  maximumValue?: number;
  minimumValue?: number;
  hostNames: string[];
  hostToValueMap: Record<string, string | undefined>;
  hostToSlotMap?: Record<string, number>;
  slotDefinitions?: any[];
}

export interface HostMetricsData {
  hostName: string;
  name: string;
  data: number | string;
  metric_path: string;
  originalData?: number;
}

export interface HeatmapApiResponse {
  items: Array<{
    Hosts: {
      host_name: string;
      public_host_name: string;
      os_type: string;
      ip: string;
      rack_info: string;
    };
    metrics?: {
      disk?: {
        disk_total: number;
        disk_free: number;
      };
      cpu?: {
        cpu_system: number;
        cpu_user: number;
      };
      memory?: {
        mem_total: number;
        mem_free: number;
      };
    };
    host_components: Array<{
      HostRoles: {
        component_name: string;
      };
    }>;
  }>;
}
