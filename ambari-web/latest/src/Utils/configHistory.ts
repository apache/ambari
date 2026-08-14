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

export type ConfigHistoryFilter = {
  field: { label: string; value: string };
  value: { label: string; value: string };
};

export type ConfigHistoryQuery = {
  currentPage: number;
  pageSize: number;
  sortColumn: string;
  sortOrder: "asc" | "desc";
  filters: ConfigHistoryFilter[];
};

export type ConfigHistoryItem = {
  serviceConfigVersion: number | string;
  user?: string;
  groupId?: number;
  groupName?: string;
  isCurrent?: boolean;
  createTime?: number;
  serviceName: string;
  hosts?: string[];
  serviceConfigVersionNote?: string;
  isClusterCompatible?: boolean;
  stackId?: string;
};

export type ConfigHistoryNavigationState = {
  serviceName: string;
  serviceConfigVersion: string;
  configGroupId?: number;
  configGroup: string;
};

function escapePredicateValue(value: string): string {
  return value.replace(/[\\^$.*+?()[\]{}|]/g, "\\$&");
}

export function relativeTimeStart(value: string, now = Date.now()): number | null {
  const match = /^(\d+)([hd])$/.exec(value);
  if (!match) {
    return null;
  }
  const amount = Number(match[1]);
  const unit = match[2] === "h" ? 60 * 60 * 1000 : 24 * 60 * 60 * 1000;
  return now - amount * unit;
}

export function configHistorySort(column: string, order: "asc" | "desc"): string {
  if (column === "service_name" || column === "group_name") {
    return `${column}.${order},service_config_version.desc`;
  }
  return `${column}.${order}`;
}

export function buildConfigHistoryParameters(query: ConfigHistoryQuery, now = Date.now()): string {
  const parameters = [
    `page_size=${query.pageSize}`,
    `from=${Math.max(0, (query.currentPage - 1) * query.pageSize)}`,
    `sortBy=${configHistorySort(query.sortColumn, query.sortOrder)}`,
  ];
  const latestByField = new Map<string, ConfigHistoryFilter>();
  query.filters.forEach((filter) => latestByField.set(filter.field.value, filter));
  latestByField.forEach((filter, field) => {
    const value = filter.value.value;
    if (field === "createtime") {
      const start = relativeTimeStart(value, now);
      if (start !== null) {
        parameters.push(`createtime>${start}`);
      }
    } else if (field === "user" || field === "service_config_version_note") {
      parameters.push(`${field}.matches(.*${encodeURIComponent(escapePredicateValue(value))}.*)`);
    } else {
      parameters.push(`${field}=${encodeURIComponent(value)}`);
    }
  });
  return parameters.join("&");
}

export function configHistoryNavigationState(item: {
  serviceName: string;
  serviceConfigVersion: number | string;
  groupId?: number;
  groupName?: string;
}): ConfigHistoryNavigationState {
  return {
    serviceName: item.serviceName,
    serviceConfigVersion: String(item.serviceConfigVersion),
    configGroupId: item.groupId,
    configGroup: item.groupName || "Default",
  };
}

export function resolveConfigHistorySelection(
  currentDefaultVersion: string,
  navigationState?: ConfigHistoryNavigationState | null,
) {
  const selectedVersion = navigationState?.serviceConfigVersion || currentDefaultVersion;
  const configGroup = navigationState?.configGroup || "Default";
  const versionsToLoad = selectedVersion === currentDefaultVersion && configGroup === "Default"
    ? null
    : configGroup === "Default"
      ? selectedVersion
      : `${currentDefaultVersion},${selectedVersion}`;

  return { configGroup, selectedVersion, versionsToLoad };
}

export function transformConfigHistoryItems(data: Record<string, unknown>[] = []): ConfigHistoryItem[] {
  return data.map((item) => ({
    serviceConfigVersion: item.service_config_version as number | string,
    user: item.user as string | undefined,
    groupId: item.group_id as number | undefined,
    groupName: item.group_name as string | undefined,
    isCurrent: item.is_current as boolean | undefined,
    createTime: item.createtime as number | undefined,
    serviceName: item.service_name as string,
    hosts: Array.isArray(item.hosts) ? item.hosts as string[] : [],
    serviceConfigVersionNote: item.service_config_version_note as string | undefined,
    isClusterCompatible: item.is_cluster_compatible as boolean | undefined,
    stackId: item.stack_id as string | undefined,
  }));
}

export function canOpenConfigHistoryItem(
  item: Pick<ConfigHistoryItem, "serviceName" | "groupName">,
  installedServices: string[],
): boolean {
  return item.groupName !== "Deleted" && installedServices.includes(item.serviceName);
}

export function formatConfigHistoryDate(timestamp: number, timezone: string): string {
  try {
    return new Intl.DateTimeFormat("en-US", {
      weekday: "short",
      year: "numeric",
      month: "short",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
      timeZone: timezone,
    }).format(new Date(timestamp));
  } catch {
    return new Intl.DateTimeFormat("en-US", {
      weekday: "short",
      year: "numeric",
      month: "short",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(new Date(timestamp));
  }
}
