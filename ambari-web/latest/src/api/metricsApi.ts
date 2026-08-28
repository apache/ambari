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

import { ambariApi, supressErrorAmbariApi } from "./config/axiosConfig";
import {
  Dashboard,
  DashboardInput,
  ChartShare,
  ChartShareInput,
  Datasource,
  DatasourceInput,
  PrometheusResponse,
} from "../screens/Monitoring/types";

interface MetricsEnvelope<T> {
  data: T;
  error: string;
}

const data = <T>(response: { data?: MetricsEnvelope<T> }): T => {
  if (!response.data || !("data" in response.data)) {
    throw new Error("Metrics API returned an invalid response");
  }
  return response.data.data;
};

const listData = <T>(response: { data?: MetricsEnvelope<unknown> }): T[] => {
  const value = data<unknown>(response);
  return Array.isArray(value) ? value as T[] : [];
};

export const MetricsApi = {
  listDatasources: async (clusterName: string) => listData<Datasource>(await ambariApi.post(
    "/metrics/datasource/list",
    { cluster_name: clusterName },
  )),

  listDatasourcePlugins: async (clusterName: string) => listData<{ type: string; name: string }>(
    await ambariApi.post("/metrics/datasource/plugin/list", { cluster_name: clusterName }),
  ),

  getDatasource: async (clusterName: string, id: number) => data<Datasource>(await ambariApi.post(
    "/metrics/datasource/desc",
    { id, cluster_name: clusterName },
  )),

  testDatasource: async (clusterName: string, id: number) => data<{ status: string }>(
    await ambariApi.post(`/metrics/datasource/${id}/test`, undefined, {
      params: { cluster_name: clusterName },
    }),
  ),

  saveDatasource: async (input: DatasourceInput) => data<Datasource>(await ambariApi.post(
    "/metrics/datasource/upsert",
    input,
  )),

  updateDatasourceStatus: async (
    clusterName: string,
    id: number,
    status: "enabled" | "disabled",
  ) => data<Datasource>(await ambariApi.post("/metrics/datasource/status/update", {
    id,
    status,
    cluster_name: clusterName,
  })),

  deleteDatasource: async (clusterName: string, id: number) => data<boolean>(await ambariApi.delete(
    `/metrics/datasource/${id}`,
    { params: { cluster_name: clusterName } },
  )),

  query: async (
    datasourceId: number,
    query: string,
    time?: number,
  ) => (await supressErrorAmbariApi.get<PrometheusResponse>(
    `/metrics/${datasourceId}/api/v1/query`,
    { params: { query, time } },
  )).data,

  queryRange: async (
    datasourceId: number,
    query: string,
    start: number,
    end: number,
    step: number,
  ) => (await supressErrorAmbariApi.get<PrometheusResponse>(
    `/metrics/${datasourceId}/api/v1/query_range`,
    { params: { query, start, end, step } },
  )).data,

  labels: async (datasourceId: number) => (await supressErrorAmbariApi.get<{
    status: string;
    data: string[];
  }>(`/metrics/${datasourceId}/api/v1/labels`)).data,

  labelValues: async (datasourceId: number, label: string) => (await supressErrorAmbariApi.get<{
    status: string;
    data: string[];
  }>(`/metrics/${datasourceId}/api/v1/label/${encodeURIComponent(label)}/values`)).data,

  targets: async (datasourceId: number) => (await supressErrorAmbariApi.get<{
    status: string;
    data: { activeTargets?: unknown[]; droppedTargets?: unknown[] };
  }>(`/metrics/${datasourceId}/api/v1/targets`)).data,

  proxyDatasourceGet: async <T>(datasourceId: number, path: string, params?: Record<string, unknown>) => (
    await supressErrorAmbariApi.get<T>(`/metrics/proxy/${datasourceId}/${path.replace(/^\/+/, "")}`, { params })
  ).data,

  proxyDatasourcePost: async <T>(datasourceId: number, path: string, body: unknown) => (
    await supressErrorAmbariApi.post<T>(`/metrics/proxy/${datasourceId}/${path.replace(/^\/+/, "")}`, body)
  ).data,

  listDashboards: async (clusterName: string, query = "") => listData<Dashboard>(await ambariApi.get(
    "/metrics/boards",
    { params: { cluster_name: clusterName, query } },
  )),

  getDashboard: async (clusterName: string, id: string | number, pure = false) => data<Dashboard>(
    await ambariApi.get(`/metrics/board/${id}${pure ? "/pure" : ""}`, {
      params: { cluster_name: clusterName },
    }),
  ),

  createDashboard: async (clusterName: string, input: DashboardInput) => data<Dashboard>(
    await ambariApi.post("/metrics/boards", input, { params: { cluster_name: clusterName } }),
  ),

  updateDashboard: async (clusterName: string, id: number, input: Partial<DashboardInput>) => data<Dashboard>(
    await ambariApi.put(`/metrics/board/${id}`, input, { params: { cluster_name: clusterName } }),
  ),

  updateDashboardConfigs: async (clusterName: string, id: number, configs: string) => data<Dashboard>(
    await ambariApi.put(`/metrics/board/${id}/configs`, { configs }, {
      params: { cluster_name: clusterName },
    }),
  ),

  cloneDashboard: async (clusterName: string, id: number) => data<Dashboard>(await ambariApi.post(
    `/metrics/board/${id}/clone`,
    undefined,
    { params: { cluster_name: clusterName } },
  )),

  deleteDashboard: async (clusterName: string, id: number) => data<Record<string, never>>(
    await ambariApi.delete(`/metrics/board/${id}`, { params: { cluster_name: clusterName } }),
  ),

  getChartShares: async (clusterName: string, ids: string) => listData<ChartShare>(await ambariApi.get(
    "/metrics/share-charts",
    { params: { cluster_name: clusterName, ids } },
  )),

  createChartShares: async (clusterName: string, shares: ChartShareInput[]) => data<number[]>(
    await ambariApi.post("/metrics/share-charts", shares, { params: { cluster_name: clusterName } }),
  ),
};

export default MetricsApi;
