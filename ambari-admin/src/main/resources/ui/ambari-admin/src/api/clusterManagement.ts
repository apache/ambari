/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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

import { adminApi } from "./configs/axiosConfig";

export type ClusterSummary = {
  Clusters: { cluster_id: number; cluster_name: string; provisioning_state: string; version?: string;
    security_type?: string; health_report?: Record<string, number> };
  hosts?: { Hosts: { host_name: string } }[];
  services?: { ServiceInfo: { service_name: string; state?: string } }[];
};
export type HostSummary = { Hosts: { host_name: string; cluster_name?: string; ip?: string; host_status?: string } };
export type Grant = { PrivilegeInfo: { privilege_id: number; principal_name: string;
  principal_type: string; permission_name: string; permission_label?: string } };
export type Role = { PermissionInfo: { permission_name: string; permission_label: string; resource_name: string } };
export type Authorization = { authorization_id: string; resource_type: string; cluster_name?: string };
export const clusterEndpoint = (name: string) => `/clusters/${encodeURIComponent(name)}`;
export const clusterListPath = "/clusters?fields=Clusters/cluster_id,Clusters/cluster_name,Clusters/provisioning_state,Clusters/version,Clusters/security_type,Clusters/health_report,hosts/Hosts/host_name,services/ServiceInfo/service_name";
export const hostsPath = "/hosts?fields=Hosts/host_name,Hosts/cluster_name,Hosts/ip,Hosts/host_status";

export async function readResource<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await adminApi.get(path, { signal });
  if (!response.data || typeof response.data !== "object") throw new Error("Invalid server response");
  return response.data;
}
export function items<T>(data: { items?: T[] } | undefined): T[] {
  if (!data) return [];
  if (!Array.isArray(data.items)) throw new Error("The server did not return a resource collection");
  return data.items;
}
export function errorMessage(error: unknown): string {
  const response = (error as { response?: { status?: number; data?: { message?: string; code?: string } } })?.response;
  return response?.data?.message || (response?.status ? `Request failed (HTTP ${response.status}). Refresh to reconcile the current server state.` : "The response was lost or the server is unavailable. Refresh before retrying the operation.");
}
export function hasPermission(grants: Authorization[], permission: string, clusterName?: string) {
  return grants.some((grant) => grant.authorization_id === permission
    && (grant.resource_type === "AMBARI" || (Boolean(clusterName) && grant.resource_type === "CLUSTER" && grant.cluster_name === clusterName)));
}
