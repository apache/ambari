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

import { ChartShare, Dashboard, DashboardPanel, Datasource, JsonObject } from "./types";

export type DashboardVariables = Record<string, string | number | string[]>;

export const RESERVED_DASHBOARD_VARIABLES = new Set(["cluster", "__rate_interval"]);

export const escapePrometheusLabelValue = (value: string) => value
  .replaceAll("\\", "\\\\")
  .replaceAll("\n", "\\n")
  .replaceAll("\r", "\\r")
  .replaceAll('"', '\\"');

export const withDashboardBuiltIns = (
  variables: DashboardVariables,
  clusterName: string,
  step: number,
): DashboardVariables => {
  const normalizedStep = Number.isFinite(step) && step > 0 ? Math.ceil(step) : 1;
  return {
    ...variables,
    cluster: escapePrometheusLabelValue(clusterName),
    __rate_interval: `${Math.max(4 * normalizedStep, 120)}s`,
  };
};

export const replaceDashboardVariables = (value: string, variables: DashboardVariables) => {
  let result = value;
  Object.entries(variables).sort(([left], [right]) => right.length - left.length).forEach(([name, selected]) => {
    const replacement = Array.isArray(selected) ? selected.join("|") : String(selected);
    const escapedName = name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    result = result
      .replaceAll(`\${${name}}`, () => replacement)
      .replace(new RegExp(`\\$${escapedName}(?![A-Za-z0-9_])`, "g"), () => replacement);
  });
  return result;
};

const datasourceId = (value: number | string | undefined, variables: DashboardVariables) => {
  if (typeof value === "number") return value;
  if (typeof value !== "string") return 0;
  const match = value.match(/^\$\{([^}]+)}$/);
  const resolved = match ? variables[match[1]] : value;
  return typeof resolved === "number" ? resolved : Number(resolved) || 0;
};

export const resolvePanelDatasourceId = (
  panel: DashboardPanel,
  variables: DashboardVariables,
  datasources: Datasource[],
) => {
  const requestedId = datasourceId(panel.datasourceValue, variables);
  const category = panel.datasourceCate || "prometheus";
  const compatible = datasources.filter((datasource) => datasource.status === "enabled"
    && (datasource.category === category || datasource.plugin_type === category));
  return compatible.some((datasource) => datasource.id === requestedId)
    ? requestedId
    : (compatible.find((datasource) => datasource.is_default) || compatible[0])?.id || 0;
};

export const dashboardAppearsAt = (dashboard: Dashboard, location: string) => (dashboard.display_locations || "")
  .split(/[\s,]+/)
  .some((value) => value.toUpperCase() === location.toUpperCase());

export const panelFromShare = (share: ChartShare): DashboardPanel | null => {
  try {
    const parsed = JSON.parse(share.configs) as JsonObject;
    const dataProps = parsed.dataProps;
    if (!dataProps || Array.isArray(dataProps) || typeof dataProps !== "object") return null;
    return {
      ...(dataProps as JsonObject),
      datasourceValue: share.datasource_id || (dataProps as JsonObject).datasourceValue,
    } as DashboardPanel;
  } catch {
    return null;
  }
};
