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

export type JsonObject = Record<string, unknown>;

export const DASHBOARD_SCHEMA_VERSION = "3.0.0" as const;

export const DASHBOARD_PANEL_TYPES = [
  "row",
  "timeseries",
  "stat",
  "gauge",
  "barGauge",
  "table",
  "tableNG",
  "pie",
  "barchart",
  "heatmap",
  "hexbin",
  "text",
  "iframe",
] as const;

export type DashboardPanelType = typeof DASHBOARD_PANEL_TYPES[number];

export type DatasourceStatus = "enabled" | "disabled";

export interface Datasource {
  id: number;
  name: string;
  description: string;
  category: string;
  plugin_id: number;
  plugin_type: string;
  plugin_type_name: string;
  cluster_name: string;
  settings: JsonObject;
  http: JsonObject;
  auth_configured: boolean;
  status: DatasourceStatus;
  is_default: boolean;
  created_at: number;
  created_by: string;
  updated_at: number;
  updated_by: string;
}

export interface DatasourceInput {
  id?: number;
  name: string;
  description: string;
  category: string;
  plugin_id: number;
  plugin_type: string;
  plugin_type_name: string;
  cluster_name: string;
  settings?: JsonObject;
  http?: JsonObject;
  auth?: JsonObject;
  status: DatasourceStatus;
  is_default: boolean;
}

export interface Dashboard {
  id: number;
  group_id: number;
  name: string;
  ident: string;
  tags: string;
  public: number;
  built_in: number;
  hide: number;
  create_at: number;
  create_by: string;
  update_at: number;
  update_by: string;
  public_cate: number;
  display_locations: string;
  configs?: string;
}

export interface DashboardInput {
  group_id?: number;
  name: string;
  ident?: string;
  tags?: string;
  public?: number;
  built_in?: number;
  hide?: number;
  public_cate?: number;
  display_locations?: string;
  configs?: string;
}

export interface DashboardTarget {
  refId: string;
  expr: string;
  legend?: string;
  instant?: boolean;
  hide?: boolean;
  maxDataPoints?: number;
  time?: JsonObject;
  variables?: JsonObject;
  __mode__?: string;
}

export interface DashboardLayout {
  h: number;
  w: number;
  x: number;
  y: number;
  i: string;
  isResizable: boolean;
}

export interface DashboardPanel {
  id: string;
  name: string;
  titleKey?: string;
  description?: string;
  type: DashboardPanelType;
  datasourceCate?: string;
  datasourceValue?: number | string;
  targets: DashboardTarget[];
  layout: DashboardLayout;
  version?: string;
  collapsed?: boolean;
  custom?: JsonObject;
  options?: JsonObject;
  overrides?: unknown[];
  links?: unknown[];
  maxPerRow?: number;
  transformations?: unknown[];
  panels?: DashboardPanel[];
}

export interface DashboardVariable {
  name: string;
  label?: string;
  type: "textbox" | "datasource" | "query";
  definition?: string;
  value?: string;
  multi?: boolean;
  includeAll?: boolean;
}

export interface DashboardPayload {
  version: typeof DASHBOARD_SCHEMA_VERSION;
  var: DashboardVariable[];
  panels: DashboardPanel[];
  graphTooltip?: string;
  graphZoom?: string;
}

export interface PrometheusResult {
  metric: Record<string, string>;
  value?: [number, string];
  values?: Array<[number, string]>;
}

export interface PrometheusResponse {
  status: "success" | "error";
  data?: {
    resultType: string;
    result: PrometheusResult[];
  };
  errorType?: string;
  error?: string;
}

export interface PrometheusTarget {
  discoveredLabels?: Record<string, string>;
  labels?: Record<string, string>;
  scrapePool?: string;
  scrapeUrl?: string;
  globalUrl?: string;
  lastError?: string;
  lastScrape?: string;
  lastScrapeDuration?: number;
  health?: string;
  scrapeInterval?: string;
  scrapeTimeout?: string;
}

export interface ChartShare {
  id: number;
  cluster: string;
  datasource_id: number;
  configs: string;
  create_at: number;
  create_by: string;
}

export interface ChartShareInput {
  datasource_id?: number;
  configs: string;
}
