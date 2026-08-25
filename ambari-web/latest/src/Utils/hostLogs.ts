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

export type HostLogFile = {
  fileName: string;
  filePath: string;
};

export type HostLogRow = {
  componentDisplayName: string;
  componentName: string;
  files: HostLogFile[];
  hostName: string;
  id: string;
  logComponentName: string;
  serviceDisplayName: string;
  serviceName: string;
};

export type HostLogEntry = {
  id: number | string;
  level: string;
  logMessage: string;
  logtime: number;
};

export const HOST_LOG_LEVELS = [
  "FATAL",
  "CRITICAL",
  "ERROR",
  "WARNING",
  "INFO",
  "DEBUG",
] as const;

export type HostLogLevel = typeof HOST_LOG_LEVELS[number];

export type HostServiceLogCounts = {
  available: boolean;
  counts: Record<HostLogLevel, number>;
  serviceDisplayName: string;
  serviceName: string;
};

const nameFromPath = (path: string) => path.split("/").filter(Boolean).pop() || path;

export function mapHostLogRows(
  response: Record<string, unknown>,
  hostName: string,
  serviceDisplayNames: Record<string, string> = {},
): HostLogRow[] {
  const components = Array.isArray(response.host_components)
    ? response.host_components as Record<string, any>[]
    : [];
  return components.flatMap((component) => {
    const logging = component.logging;
    if (!logging || !logging.name) {
      return [];
    }
    const roles = component.HostRoles || {};
    const serviceName = String(roles.service_name || "");
    const componentName = String(roles.component_name || logging.name || "");
    const logPaths = Array.isArray(logging.logs) ? logging.logs : [];
    const files = logPaths
      .map((item: string | { name?: string }) =>
        typeof item === "string" ? item : item?.name,
      )
      .filter((path: string | undefined): path is string => Boolean(path))
      .map((filePath: string) => ({ fileName: nameFromPath(filePath), filePath }));
    return [{
      componentDisplayName: String(roles.display_name || componentName),
      componentName,
      files,
      hostName,
      id: `${hostName}_${logging.name}`,
      logComponentName: String(logging.name),
      serviceDisplayName: serviceDisplayNames[serviceName] || serviceName,
      serviceName,
    }];
  });
}

export function mapHostLogEntries(response: Record<string, unknown>): HostLogEntry[] {
  const rows = Array.isArray(response.logList)
    ? response.logList as Record<string, unknown>[]
    : [];
  return rows.map((item, index) => ({
    id: item.id as number | string ?? `${item.logtime || 0}-${index}`,
    level: String(item.level || ""),
    logMessage: String(item.log_message || ""),
    logtime: Number(item.logtime || 0),
  }));
}

export function mapHostLogLevelCounts(
  response: Record<string, unknown>,
  serviceDisplayNames: Record<string, string> = {},
): HostServiceLogCounts[] {
  const components = Array.isArray(response.host_components)
    ? response.host_components as Record<string, any>[]
    : [];
  const byService = new Map<string, HostServiceLogCounts>();

  components.forEach((component) => {
    const roles = component.HostRoles || {};
    const serviceName = String(roles.service_name || "");
    const logging = component.logging;
    if (!serviceName || !logging) return;
    const row = byService.get(serviceName) || {
      available: false,
      counts: Object.fromEntries(
        HOST_LOG_LEVELS.map((level) => [level, 0]),
      ) as Record<HostLogLevel, number>,
      serviceDisplayName: serviceDisplayNames[serviceName] || serviceName,
      serviceName,
    };
    const levelCounts = Array.isArray(logging.log_level_counts)
      ? logging.log_level_counts
      : [];
    levelCounts.forEach((levelCount: Record<string, unknown>) => {
      const sourceName = String(levelCount?.name || "").toUpperCase();
      const level = (sourceName === "WARN" ? "WARNING" : sourceName) as HostLogLevel;
      const count = Number(levelCount?.value);
      if (HOST_LOG_LEVELS.includes(level) && Number.isFinite(count) && count >= 0) {
        row.counts[level] += count;
        row.available = true;
      }
    });
    byService.set(serviceName, row);
  });

  return Array.from(byService.values()).sort((left, right) =>
    left.serviceDisplayName.localeCompare(right.serviceDisplayName),
  );
}

export function mergeHostLogEntries(
  current: HostLogEntry[],
  incoming: HostLogEntry[],
): HostLogEntry[] {
  const rows = new Map(current.map((item) => [String(item.id), item]));
  incoming.forEach((item) => rows.set(String(item.id), item));
  return Array.from(rows.values()).sort((left, right) => left.logtime - right.logtime);
}

export function hostLogsToText(rows: HostLogEntry[]): string {
  return rows.map((row) => {
    const timestamp = row.logtime
      ? new Date(row.logtime).toISOString().replace("T", " ").replace("Z", "")
      : "";
    return [timestamp, row.level, row.logMessage].filter(Boolean).join(" ");
  }).join("\n");
}

export function openTextInNewWindow(
  value: string,
  openWindow: typeof window.open = window.open.bind(window),
): boolean {
  const target = openWindow("about:blank", "_blank");
  if (!target) {
    return false;
  }
  target.opener = null;
  target.document.title = "Ambari Log";
  const pre = target.document.createElement("pre");
  pre.textContent = value;
  target.document.body.replaceChildren(pre);
  return true;
}

export function buildLogSearchUrl(
  baseUrl: string,
  hostName: string,
  componentName: string,
  filePath: string,
): string {
  if (!baseUrl) {
    return "";
  }
  const root = baseUrl.split("#", 1)[0].replace(/\/$/, "");
  const query = encodeURIComponent(JSON.stringify([{
    id: 0,
    name: "path",
    label: "Path",
    value: filePath,
    isExclude: false,
  }]));
  return `${root}/#/logs/serviceLogs;hosts=${encodeURIComponent(hostName)}`
    + `;components=${encodeURIComponent(componentName)};query=${query}`;
}
