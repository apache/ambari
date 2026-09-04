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

import {
  ChartShare,
  Dashboard,
  DashboardPanel,
  DashboardPayload,
  DashboardPanelType,
  DashboardTarget,
  DashboardVariable,
  DASHBOARD_PANEL_TYPES,
  DASHBOARD_SCHEMA_VERSION,
  Datasource,
  JsonObject,
  PrometheusResult,
} from "./types";

export type DashboardVariables = Record<string, string | number | string[]>;

export const RESERVED_DASHBOARD_VARIABLES = new Set(["cluster", "__rate_interval"]);

const isJsonObject = (value: unknown): value is JsonObject => value !== null
  && typeof value === "object"
  && !Array.isArray(value);

const failSchema = (path: string, message: string): never => {
  throw new Error(`Invalid dashboard schema at ${path}: ${message}`);
};

const assertObjectKeys = (value: JsonObject, allowed: ReadonlySet<string>, path: string) => {
  Object.keys(value).forEach((key) => {
    if (!allowed.has(key)) failSchema(`${path}.${key}`, "field is not supported");
  });
};

const asRequiredString = (value: unknown, path: string) => {
  if (typeof value !== "string" || !value.trim()) failSchema(path, "expected a non-empty string");
  return value;
};

const asOptionalJsonObject = (value: unknown, path: string) => {
  if (value === undefined) return undefined;
  if (!isJsonObject(value)) failSchema(path, "expected an object");
  return value;
};

const PANEL_KEYS = new Set([
  "id", "name", "description", "type", "datasourceCate", "datasourceValue", "targets", "layout",
  "version", "collapsed", "custom", "options", "overrides", "links", "maxPerRow", "transformations", "panels",
]);
const LAYOUT_KEYS = new Set(["h", "w", "x", "y", "i", "isResizable"]);
const TARGET_KEYS = new Set(["refId", "expr", "legend", "instant", "hide", "maxDataPoints", "time", "variables", "__mode__"]);
const VARIABLE_KEYS = new Set(["name", "label", "type", "definition", "value"]);
const SUPPORTED_PANEL_TYPES = new Set<string>(DASHBOARD_PANEL_TYPES);

const normalizeDashboardLayout = (value: unknown, path: string) => {
  if (!isJsonObject(value)) failSchema(path, "layout is required");
  assertObjectKeys(value, LAYOUT_KEYS, path);
  const numeric = ["h", "w", "x", "y"].map((key) => [key, value[key]] as const);
  numeric.forEach(([key, item]) => {
    if (typeof item !== "number" || !Number.isInteger(item) || item < 0) {
      failSchema(`${path}.${key}`, "expected a non-negative integer");
    }
  });
  if (value.w === 0 || value.h === 0) failSchema(path, "width and height must be greater than zero");
  if ((value.w as number) > 24 || (value.x as number) + (value.w as number) > 24) {
    failSchema(path, "panel must fit within the 24-column grid");
  }
  const id = asRequiredString(value.i, `${path}.i`);
  if (typeof value.isResizable !== "boolean") failSchema(`${path}.isResizable`, "expected a boolean");
  return {
    h: value.h as number,
    w: value.w as number,
    x: value.x as number,
    y: value.y as number,
    i: id,
    isResizable: value.isResizable,
  };
};

const normalizeDashboardTarget = (value: unknown, path: string): DashboardTarget => {
  if (!isJsonObject(value)) failSchema(path, "target must be an object");
  assertObjectKeys(value, TARGET_KEYS, path);
  const target = {
    refId: asRequiredString(value.refId, `${path}.refId`),
    expr: asRequiredString(value.expr, `${path}.expr`),
  } as DashboardTarget;
  (["legend", "__mode__"] as const).forEach((key) => {
    if (value[key] !== undefined) target[key] = asRequiredString(value[key], `${path}.${key}`);
  });
  (["instant", "hide"] as const).forEach((key) => {
    if (value[key] !== undefined && typeof value[key] !== "boolean") failSchema(`${path}.${key}`, "expected a boolean");
    if (value[key] !== undefined) target[key] = value[key] as boolean;
  });
  if (value.maxDataPoints !== undefined) {
    if (typeof value.maxDataPoints !== "number" || !Number.isInteger(value.maxDataPoints) || value.maxDataPoints < 1) {
      failSchema(`${path}.maxDataPoints`, "expected a positive integer");
    }
    target.maxDataPoints = value.maxDataPoints;
  }
  target.time = asOptionalJsonObject(value.time, `${path}.time`);
  target.variables = asOptionalJsonObject(value.variables, `${path}.variables`);
  return target;
};

const normalizeDashboardVariable = (value: unknown, path: string): DashboardVariable => {
  if (!isJsonObject(value)) failSchema(path, "variable must be an object");
  assertObjectKeys(value, VARIABLE_KEYS, path);
  const type = value.type;
  if (type !== "textbox" && type !== "datasource") failSchema(`${path}.type`, "expected textbox or datasource");
  const variable: DashboardVariable = {
    name: asRequiredString(value.name, `${path}.name`),
    type,
  };
  (["label", "definition", "value"] as const).forEach((key) => {
    if (value[key] !== undefined) variable[key] = asRequiredString(value[key], `${path}.${key}`);
  });
  return variable;
};

const normalizeDashboardPanel = (value: unknown, path: string): DashboardPanel => {
  if (!isJsonObject(value)) failSchema(path, "panel must be an object");
  assertObjectKeys(value, PANEL_KEYS, path);
  const type = value.type;
  if (typeof type !== "string" || !SUPPORTED_PANEL_TYPES.has(type)) {
    failSchema(`${path}.type`, `unsupported panel type ${String(type)}`);
  }
  const targets = value.targets === undefined ? [] : value.targets;
  if (!Array.isArray(targets)) failSchema(`${path}.targets`, "expected an array");
  const panels = value.panels === undefined ? undefined : value.panels;
  if (panels !== undefined && !Array.isArray(panels)) failSchema(`${path}.panels`, "expected an array");
  if (type !== "row" && type !== "text" && type !== "iframe" && targets.length === 0) {
    failSchema(`${path}.targets`, "at least one target is required");
  }
  if (type === "row" && (value.layout as JsonObject | undefined)?.w !== 24) {
    failSchema(`${path}.layout.w`, "row panels must span all 24 columns");
  }
  const panel: DashboardPanel = {
    id: asRequiredString(value.id, `${path}.id`),
    name: asRequiredString(value.name, `${path}.name`),
    type: type as DashboardPanelType,
    layout: normalizeDashboardLayout(value.layout, `${path}.layout`),
    targets: targets.map((target, index) => normalizeDashboardTarget(target, `${path}.targets[${index}]`)),
  };
  (["description", "datasourceCate", "version"] as const).forEach((key) => {
    if (value[key] !== undefined) panel[key] = asRequiredString(value[key], `${path}.${key}`);
  });
  if (value.datasourceValue !== undefined) {
    if (typeof value.datasourceValue !== "number" && typeof value.datasourceValue !== "string") {
      failSchema(`${path}.datasourceValue`, "expected a number or string");
    }
    panel.datasourceValue = value.datasourceValue;
  }
  if (value.collapsed !== undefined) {
    if (typeof value.collapsed !== "boolean") failSchema(`${path}.collapsed`, "expected a boolean");
    panel.collapsed = value.collapsed;
  }
  panel.custom = asOptionalJsonObject(value.custom, `${path}.custom`);
  panel.options = asOptionalJsonObject(value.options, `${path}.options`);
  (["overrides", "links", "transformations"] as const).forEach((key) => {
    if (value[key] !== undefined) {
      if (!Array.isArray(value[key])) failSchema(`${path}.${key}`, "expected an array");
      panel[key] = value[key] as unknown[];
    }
  });
  if (value.maxPerRow !== undefined) {
    if (typeof value.maxPerRow !== "number" || !Number.isInteger(value.maxPerRow) || value.maxPerRow < 1) {
      failSchema(`${path}.maxPerRow`, "expected a positive integer");
    }
    panel.maxPerRow = value.maxPerRow;
  }
  if (panels !== undefined) panel.panels = panels.map((child, index) => normalizeDashboardPanel(child, `${path}.panels[${index}]`));
  return panel;
};

export const normalizeDashboardPayload = (value: unknown): DashboardPayload => {
  if (!isJsonObject(value)) failSchema("$", "expected an object");
  assertObjectKeys(value, new Set(["version", "var", "panels", "graphTooltip", "graphZoom"]), "$");
  if (value.version !== DASHBOARD_SCHEMA_VERSION) {
    failSchema("$.version", `expected ${DASHBOARD_SCHEMA_VERSION}`);
  }
  if (!Array.isArray(value.var)) failSchema("$.var", "expected an array");
  if (!Array.isArray(value.panels)) failSchema("$.panels", "expected an array");
  if (value.graphTooltip !== undefined && typeof value.graphTooltip !== "string") failSchema("$.graphTooltip", "expected a string");
  if (value.graphZoom !== undefined && typeof value.graphZoom !== "string") failSchema("$.graphZoom", "expected a string");
  const panels = value.panels.map((panel, index) => normalizeDashboardPanel(panel, `$.panels[${index}]`));
  const ids = new Set<string>();
  const validateUniqueIds = (items: DashboardPanel[], path: string) => {
    items.forEach((panel, index) => {
      if (ids.has(panel.id) || ids.has(panel.layout.i)) failSchema(`${path}[${index}]`, "panel and layout IDs must be unique");
      ids.add(panel.id);
      if (panel.layout.i !== panel.id) ids.add(panel.layout.i);
      if (panel.panels) validateUniqueIds(panel.panels, `${path}[${index}].panels`);
    });
  };
  validateUniqueIds(panels, "$.panels");
  const result: DashboardPayload = {
    version: DASHBOARD_SCHEMA_VERSION,
    var: value.var.map((variable, index) => normalizeDashboardVariable(variable, `$.var[${index}]`)),
    panels,
  };
  if (value.graphTooltip !== undefined) result.graphTooltip = value.graphTooltip as string;
  if (value.graphZoom !== undefined) result.graphZoom = value.graphZoom as string;
  return result;
};

export const parseDashboardPayload = (value: string): DashboardPayload => normalizeDashboardPayload(
  JSON.parse(value) as unknown,
);

const isPrometheusSample = (value: unknown): value is [number, string] => Array.isArray(value)
  && typeof value[0] === "number"
  && typeof value[1] === "string";

export const normalizePrometheusResults = (value: unknown): PrometheusResult[] => {
  if (!Array.isArray(value)) return [];
  return value.filter(isJsonObject).flatMap((result) => {
    if (!isJsonObject(result.metric)) return [];
    const metric = Object.fromEntries(
      Object.entries(result.metric).filter((entry): entry is [string, string] => typeof entry[1] === "string"),
    );
    return [{
      ...result,
      metric,
      value: isPrometheusSample(result.value) ? result.value : undefined,
      values: Array.isArray(result.values) ? result.values.filter(isPrometheusSample) : undefined,
    } as PrometheusResult];
  });
};

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
  const available = datasources.filter((datasource) => datasource.status === "enabled"
    && (datasource.category === category || datasource.plugin_type === category));
  return available.some((datasource) => datasource.id === requestedId)
    ? requestedId
    : (available.find((datasource) => datasource.is_default) || available[0])?.id || 0;
};

export const dashboardAppearsAt = (dashboard: Dashboard, location: string) => (dashboard.display_locations || "")
  .split(/[\s,]+/)
  .some((value) => value.toUpperCase() === location.toUpperCase());

export const panelFromShare = (share: ChartShare): DashboardPanel | null => {
  try {
    const parsed = JSON.parse(share.configs) as JsonObject;
    if (!isJsonObject(parsed.panel)) return null;
    return normalizeDashboardPanel({ ...parsed.panel, datasourceValue: share.datasource_id }, "$.panel");
  } catch {
    return null;
  }
};
