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

export type RmHaHost = {
  hostName: string;
  cpuCount?: number;
  totalMemory?: number;
  maintenanceState?: string;
  diskInfo?: unknown[];
};

export type RmHaTopologyEntry = {
  component: string;
  hostName: string;
  serviceName?: string;
  state?: string;
  maintenanceState?: string;
  isInstalled: boolean;
};

export type RmHaMasterAssignment = {
  component: "RESOURCEMANAGER";
  component_name: "RESOURCEMANAGER";
  hostName: string;
  selectedHost: string;
  serviceId: "YARN";
  isInstalled: boolean;
};

export type RmHaAssignment = {
  currentRM: string;
  additionalRM: string;
  hosts: RmHaHost[];
  masterComponentHosts: RmHaMasterAssignment[];
  topologyHosts: RmHaTopologyEntry[];
};

export type RmHaConfigProperty = {
  name: string;
  displayName?: string;
  category: string;
  filename: string;
  value: string | number | boolean;
  changedValue: string | number | boolean;
  recommendedValue?: string | number | boolean;
  displayType?: string;
  isEditable: boolean;
  isOverridable: boolean;
  serviceName?: string;
};

export type RmHaReviewConfig = {
  serviceName: string;
  displayName: string;
  configCategories: Array<{ name: string; displayName: string }>;
  configs: RmHaConfigProperty[];
};

export type RmHaOperation = {
  id: string;
  label: string;
  skippable: false;
  callback: () => Promise<unknown>;
  requestId?: string | number;
  requestInfo?: Record<string, unknown>;
  status?: string;
  progress?: number;
  error?: string;
};

export type PersistedRmHaOperation = Omit<RmHaOperation, "callback">;
