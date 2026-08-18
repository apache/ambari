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

import { AlertStatus } from "../screens/Alerts/alertStatus";
import type { MergedAlert } from "../screens/Alerts/types";

interface AlertHistoryItem {
  AlertHistory?: { host_name?: string };
}

interface SortableAlertInstance {
  service_name?: string;
  host_name?: string;
  state?: string;
  last_updated_time?: number;
  [key: string]: unknown;
}

interface AlertInstanceFilters {
  service: string;
  hostName: string;
  state: string;
}

interface AlertInstanceSort {
  id: string;
  desc: boolean;
}

interface DefinitionConfiguration {
  description?: string;
  interval?: number | string;
  source?: {
    parameters?: Array<{
      name?: string;
      type?: string;
      value?: unknown;
      visibility?: string;
    }>;
    reporting?: Record<string, unknown>;
    [key: string]: unknown;
  };
}

interface AlertDefinitionDetailsResource extends DefinitionConfiguration {
  id: number;
  cluster_name?: string;
  enabled?: boolean;
  name?: string;
  label?: string;
  service_name?: string;
  component_name?: string;
  source_type?: string;
  repeat_tolerance?: string | number;
  repeat_tolerance_enabled?: boolean;
  help_url?: string;
}

interface AlertDefinitionGroupResource {
  AlertGroup: {
    id: number;
    name: string;
    default?: boolean;
    cluster_name?: string;
    definitions?: Array<number | { id?: number }>;
  };
}

interface GroupedSummaryResource {
  definition_id: string | number;
  summary?: Record<string, {
    count?: number;
    maintenance_count?: number;
    original_timestamp?: string | null;
    latest_text?: string;
  }>;
}

export function countAlertHistoryByHost(items: AlertHistoryItem[] = []): Record<string, number> {
  return items.reduce<Record<string, number>>((counts, item) => {
    const hostName = item.AlertHistory?.host_name;
    if (hostName) counts[hostName] = (counts[hostName] || 0) + 1;
    return counts;
  }, {});
}

export function filterAndSortAlertInstances<T extends SortableAlertInstance>(
  instances: T[],
  filters: AlertInstanceFilters,
  sorting: AlertInstanceSort | undefined,
  historyCounts: Record<string, number>,
): T[] {
  const filtered = instances.filter((instance) => {
    if (filters.service && filters.service !== "All" && instance.service_name !== filters.service) return false;
    if (filters.hostName && !String(instance.host_name || "").toLowerCase().includes(filters.hostName.toLowerCase())) return false;
    if (filters.state && filters.state !== "All" && String(instance.state || "").toLowerCase() !== filters.state.toLowerCase()) return false;
    return true;
  });

  if (!sorting) return filtered;

  return filtered.sort((left, right) => {
    let comparison: number;
    if (sorting.id === "history_count") {
      comparison = (historyCounts[left.host_name || ""] || 0) - (historyCounts[right.host_name || ""] || 0);
    } else if (sorting.id === "last_updated_time") {
      comparison = Number(left.last_updated_time || 0) - Number(right.last_updated_time || 0);
    } else {
      comparison = String(left[sorting.id] ?? "").localeCompare(String(right[sorting.id] ?? ""));
    }
    return sorting.desc ? -comparison : comparison;
  });
}

export function buildAlertDefinitionDetails(
  definition: AlertDefinitionDetailsResource,
  groups: AlertDefinitionGroupResource[] = [],
  summaries: GroupedSummaryResource[] = [],
): MergedAlert {
  const memberships = groups.filter((group) => (group.AlertGroup.definitions || []).some((reference) =>
    Number(typeof reference === "number" ? reference : reference.id) === definition.id,
  ));
  const summary = summaries.find((item) => Number(item.definition_id) === definition.id)?.summary || {};
  const priority: Record<string, number> = { critical: 4, warning: 3, ok: 2, unknown: 1 };
  const statuses = Object.entries(summary)
    .filter(([, value]) => (value.count || 0) > 0 || (value.maintenance_count || 0) > 0)
    .map(([status, value]) => ({
      status: status.toLowerCase() as AlertStatus,
      count: value.count || 0,
      maintenance_count: value.maintenance_count || 0,
      last_status_changed: value.original_timestamp || null,
      latest_text: value.latest_text || "",
    }))
    .sort((left, right) => (priority[right.status] || 0) - (priority[left.status] || 0));
  const primaryGroup = memberships[0]?.AlertGroup;
  const lastStatusChanged = statuses[0]?.last_status_changed || "Unknown";

  return {
    cluster_name: definition.cluster_name || primaryGroup?.cluster_name || "",
    alert_group_id: primaryGroup?.id || 0,
    alert_group_name: primaryGroup?.name || "",
    enabled: Boolean(definition.enabled),
    name: definition.name || "",
    label: definition.label || definition.name || "",
    description: definition.description || "",
    serviceDisplayName: definition.service_name || "",
    component_name: definition.component_name || "",
    alert_definition_id: definition.id,
    source_type: String(definition.source?.type || definition.source_type || ""),
    repeat_tolerance: definition.repeat_tolerance ?? 1,
    repeat_tolerance_enabled: Boolean(definition.repeat_tolerance_enabled),
    help_url: definition.help_url,
    statuses,
    last_status_changed: lastStatusChanged,
    lastTriggeredFormatted: lastStatusChanged,
    lastTriggeredAgoFormatted: "Unknown",
    lastTriggeredRaw: lastStatusChanged === "Unknown" ? null : lastStatusChanged,
    state: definition.enabled ? "Enabled" : "Disabled",
    latest_text: statuses[0]?.latest_text || "",
    groups: memberships.map((group) =>
      `${group.AlertGroup.name}${group.AlertGroup.default ? " Default" : ""}`,
    ).join(", "),
  };
}

export function validateRepeatTolerance(value: string | number): string | null {
  const normalized = String(value).trim();
  if (normalized === "DEBUG") return null;
  if (!/^\d+$/.test(normalized) || Number(normalized) < 1 || Number(normalized) > 99) {
    return "Check Count must be an integer between 1 and 99, or DEBUG.";
  }
  return null;
}

function reportingValue(source: DefinitionConfiguration["source"], state: string): number | null {
  const entry = source?.reporting?.[state];
  if (!entry || typeof entry !== "object" || !("value" in entry)) return null;
  const value = Number((entry as { value?: unknown }).value);
  return Number.isFinite(value) ? value : null;
}

function hasReportingValue(source: DefinitionConfiguration["source"], state: string): boolean {
  const entry = source?.reporting?.[state];
  return Boolean(entry && typeof entry === "object" && "value" in entry);
}

export function validateAlertDefinitionConfiguration(configuration: DefinitionConfiguration): string[] {
  const errors: string[] = [];
  const interval = Number(configuration.interval);
  if (!Number.isInteger(interval) || interval < 1) errors.push("Check Interval must be a positive integer.");

  for (const parameter of configuration.source?.parameters || []) {
    if (parameter.visibility === "HIDDEN" || parameter.visibility === "READ_ONLY") continue;
    const value = String(parameter.value ?? "").trim();
    if (!value) errors.push(`${parameter.name || "Parameter"} is required.`);
    if ((parameter.type === "NUMERIC" || parameter.type === "PERCENT") && (!Number.isFinite(Number(value)) || Number(value) <= 0)) {
      errors.push(`${parameter.name || "Parameter"} must be a positive number.`);
    }
  }

  const warning = reportingValue(configuration.source, "warning");
  const critical = reportingValue(configuration.source, "critical");
  if (hasReportingValue(configuration.source, "warning") && warning === null) errors.push("Warning threshold must be a number.");
  else if (warning !== null && warning <= 0) errors.push("Warning threshold must be positive.");
  if (hasReportingValue(configuration.source, "critical") && critical === null) errors.push("Critical threshold must be a number.");
  else if (critical !== null && critical <= 0) errors.push("Critical threshold must be positive.");
  if (warning !== null && critical !== null && warning > critical) {
    errors.push("Warning threshold cannot be greater than Critical threshold.");
  }
  return errors;
}

export function buildAlertDefinitionUpdate(
  current: DefinitionConfiguration,
  original: DefinitionConfiguration,
): Record<string, unknown> {
  const payload: Record<string, unknown> = {};
  if (current.description !== original.description) payload["AlertDefinition/description"] = current.description || "";
  if (String(current.interval) !== String(original.interval)) payload["AlertDefinition/interval"] = String(current.interval);
  if (JSON.stringify(current.source || {}) !== JSON.stringify(original.source || {})) {
    payload["AlertDefinition/source"] = JSON.parse(JSON.stringify(current.source || {}));
  }
  return payload;
}

export function openAlertResponseInNewWindow(
  value: string,
  openWindow: typeof window.open = window.open.bind(window),
): boolean {
  const target = openWindow("about:blank", "_blank");
  if (!target) return false;
  target.opener = null;
  target.document.title = "Ambari Alert Response";
  const pre = target.document.createElement("pre");
  pre.textContent = value;
  target.document.body.replaceChildren(pre);
  return true;
}
