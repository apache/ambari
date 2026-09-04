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

import type { DashboardPanel, JsonObject, PrometheusResult } from "../../types";

export interface DashboardPanelResult extends PrometheusResult {
  displayName: string;
  seriesKey: string;
  targetRefId: string;
  targetName: string;
}

export interface DashboardThreshold {
  value: number | null;
  color: string;
}

export interface DashboardValueMapping {
  color?: string;
  text?: string;
}

const asRecord = (value: unknown): JsonObject => (
  value && typeof value === "object" && !Array.isArray(value)
    ? value as JsonObject
    : {}
);

const namedColors: Record<string, string> = {
  green: "#278541",
  red: "#b33a3a",
  orange: "#bd6418",
  yellow: "#c58a00",
  blue: "#1769aa",
  purple: "#8a4f9d",
  gray: "#6c757d",
};

const resolveColor = (value: unknown, fallback: string) => {
  if (typeof value !== "string") return fallback;
  return namedColors[value] || value || fallback;
};

export function latestPanelValue(result: DashboardPanelResult) {
  const point = result.values?.at(-1) || result.value;
  if (!point) return null;
  const numeric = Number(point[1]);
  return Number.isFinite(numeric) ? numeric : null;
}

export function calculatePanelValue(result: DashboardPanelResult, calculation = "lastNotNull") {
  const points = result.values || (result.value ? [result.value] : []);
  const values = points.map(([, value]) => Number(value)).filter(Number.isFinite);
  if (!values.length) return null;
  switch (calculation) {
    case "last":
    case "lastNotNull": return values.at(-1) ?? null;
    case "first":
    case "firstNotNull": return values[0] ?? null;
    case "min": return Math.min(...values);
    case "max": return Math.max(...values);
    case "avg": return values.reduce((sum, value) => sum + value, 0) / values.length;
    case "sum": return values.reduce((sum, value) => sum + value, 0);
    case "count": return values.length;
    default: return values.at(-1) ?? null;
  }
}

export function panelCustomOptions(panel: DashboardPanel): JsonObject {
  return asRecord(panel.custom);
}

export function panelThresholds(panel: DashboardPanel): DashboardThreshold[] {
  const options = asRecord(panel.options);
  const raw = asRecord(options.thresholds);
  const steps = Array.isArray(raw.steps) ? raw.steps : [];
  return steps
    .map((step) => {
      const item = asRecord(step);
      const rawValue = item.value;
      const value = rawValue === null || rawValue === undefined ? null : Number(rawValue);
      return {
        value: value === null || Number.isFinite(value) ? value : null,
        color: resolveColor(item.color, "#6c757d"),
      };
    })
    .filter((step) => step.value === null || Number.isFinite(step.value))
    .sort((left, right) => (left.value ?? Number.NEGATIVE_INFINITY) - (right.value ?? Number.NEGATIVE_INFINITY));
}

const valueMapping = (value: number, rawMappings: unknown): DashboardValueMapping | undefined => {
  const mappings = Array.isArray(rawMappings) ? rawMappings.map(asRecord) : [];
  const mapping = mappings.find((item) => {
    if (item.type !== "range") return false;
    const match = asRecord(item.match);
    const from = typeof match.from === "number" ? match.from : Number.NEGATIVE_INFINITY;
    const to = typeof match.to === "number" ? match.to : Number.POSITIVE_INFINITY;
    return value >= from && value <= to;
  });
  if (!mapping) return undefined;
  const result = asRecord(mapping.result);
  const color = resolveColor(result.color, "");
  const text = typeof result.text === "string" && result.text.length > 0 ? result.text : undefined;
  return color || text ? { color: color || undefined, text } : undefined;
};

export function panelValueText(panel: DashboardPanel, value: number | null) {
  if (value === null) return undefined;
  return valueMapping(value, asRecord(panel.options).valueMappings)?.text;
}

export function panelValueColor(panel: DashboardPanel, value: number | null) {
  if (value !== null) {
    const mappedColor = valueMapping(value, asRecord(panel.options).valueMappings)?.color;
    if (mappedColor) return mappedColor;
  }
  const thresholds = panelThresholds(panel);
  if (value === null || thresholds.length === 0) return undefined;
  let color: string | undefined;
  thresholds.forEach((threshold) => {
    if (threshold.value === null || value >= threshold.value) color = threshold.color;
  });
  return color;
}

export function panelStandardOptions(panel: DashboardPanel): JsonObject {
  const options = asRecord(panel.options);
  return asRecord(options.standardOptions);
}

const matchingOverride = (panel: DashboardPanel, refId: string): JsonObject | undefined => (
  (panel.overrides || []).map(asRecord).find((override) => {
    const matcher = asRecord(override.matcher);
    return matcher.id === "byFrameRefID" && matcher.value === refId;
  })
);

export function panelFieldStandardOptions(panel: DashboardPanel, refId: string): JsonObject {
  const override = matchingOverride(panel, refId);
  const properties = asRecord(override?.properties);
  return {
    ...panelStandardOptions(panel),
    ...asRecord(properties.standardOptions),
  };
}

export function panelFieldColor(panel: DashboardPanel, refId: string, value: number | null) {
  if (value === null) return undefined;
  const override = matchingOverride(panel, refId);
  const properties = asRecord(override?.properties);
  return valueMapping(value, properties.valueMappings)?.color || panelValueColor(panel, value);
}

export function panelFieldValueText(panel: DashboardPanel, refId: string, value: number | null) {
  if (value === null) return undefined;
  const override = matchingOverride(panel, refId);
  const properties = asRecord(override?.properties);
  return valueMapping(value, properties.valueMappings)?.text || panelValueText(panel, value);
}

export function panelNumericBounds(panel: DashboardPanel): { min?: number; max?: number } {
  const standard = panelStandardOptions(panel);
  const min = typeof standard.min === "number" && Number.isFinite(standard.min) ? standard.min : undefined;
  const max = typeof standard.max === "number" && Number.isFinite(standard.max) ? standard.max : undefined;
  return { min, max };
}
