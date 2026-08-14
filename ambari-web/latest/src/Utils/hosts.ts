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

import { cloneDeep } from "lodash";
import { PassiveStateOnFilters } from "../screens/Hosts/enums";
import stringUtilsObj from "./StringUtilsObj";

type HostComponentLike = {
  adminState?: string;
  componentName?: string;
  hostName?: string;
  passiveState?: string;
  staleConfigs?: boolean;
  workStatus?: string;
};

type HostLike = {
  alertsSummary?: unknown;
  healthStatus?: string;
  hostComponents: HostComponentLike[];
  hostName: string;
  lastHeartBeatTime?: number;
  passiveState?: string;
  state?: string;
};

const hasOwn = (value: unknown, property: string): boolean =>
  Object.prototype.hasOwnProperty.call(value, property);

function applyHostMaintenanceToComponent(
  component: HostComponentLike,
  desiredState: string,
) {
  const currentState = component.passiveState || "OFF";
  if (currentState === desiredState) {
    return;
  }
  if (desiredState === "OFF") {
    if (currentState === PassiveStateOnFilters.IMPLIED_FROM_SERVICE_AND_HOST) {
      component.passiveState = PassiveStateOnFilters.IMPLIED_FROM_SERVICE;
    } else if (currentState === PassiveStateOnFilters.IMPLIED_FROM_HOST) {
      component.passiveState = "OFF";
    }
    return;
  }
  if (currentState === PassiveStateOnFilters.IMPLIED_FROM_SERVICE) {
    component.passiveState = PassiveStateOnFilters.IMPLIED_FROM_SERVICE_AND_HOST;
  } else if (currentState === "OFF") {
    component.passiveState = PassiveStateOnFilters.IMPLIED_FROM_HOST;
  }
}

export function applyHostComponentEvent<T extends HostLike>(
  hosts: T[],
  event: Record<string, unknown>,
): T[] {
  const result = cloneDeep(hosts);
  const updates = Array.isArray(event.hostComponents) ? event.hostComponents : [];
  updates.forEach((update: Record<string, unknown>) => {
    const hostName = String(update.hostName || "");
    const componentName = String(update.componentName || "");
    const host = result.find((item) => item.hostName === hostName);
    const component = host?.hostComponents.find(
      (item) => item.componentName === componentName,
    );
    if (!component) {
      return;
    }
    if (hasOwn(update, "currentState")) {
      component.workStatus = update.currentState as string;
    }
    if (hasOwn(update, "staleConfigs")) {
      component.staleConfigs = update.staleConfigs as boolean;
    }
    if (hasOwn(update, "maintenanceState")) {
      component.passiveState = update.maintenanceState as string;
    }
  });
  return result;
}

export function applyHostEvent<T extends HostLike>(
  hosts: T[],
  event: Record<string, unknown>,
): T[] {
  const result = cloneDeep(hosts);
  const host = result.find((item) => item.hostName === event.host_name);
  if (!host) {
    return result;
  }
  const fields: Array<[keyof HostLike, string]> = [
    ["alertsSummary", "alerts_summary"],
    ["healthStatus", "host_status"],
    ["state", "host_state"],
    ["lastHeartBeatTime", "last_heartbeat_time"],
    ["passiveState", "maintenance_state"],
  ];
  fields.forEach(([target, source]) => {
    if (hasOwn(event, source)) {
      Object.assign(host, { [target]: event[source] });
    }
  });
  if (hasOwn(event, "maintenance_state")) {
    host.hostComponents.forEach((component) =>
      applyHostMaintenanceToComponent(component, String(event.maintenance_state)),
    );
  }
  return result;
}

const DECOMMISSION_COMPONENTS = [
  "DATANODE",
  "NODEMANAGER",
  "HBASE_REGIONSERVER",
  "TASKTRACKER",
];

function componentFromRequest(context: string, task: Record<string, unknown>): string {
  const taskComponent = String(
    task.componentName || task.component_name || task.role || "",
  ).toUpperCase();
  if (DECOMMISSION_COMPONENTS.includes(taskComponent)) {
    return taskComponent;
  }
  return DECOMMISSION_COMPONENTS.find((component) =>
    context.includes(component.toLowerCase().replace("hbase_", "")),
  ) || "";
}

export function applyCompletedDecommissionRequest<T extends HostLike>(
  hosts: T[],
  event: Record<string, unknown>,
): T[] {
  if (event.requestStatus !== "COMPLETED") {
    return cloneDeep(hosts);
  }
  const context = String(event.requestContext || "").toLowerCase();
  const isRecommission = context.includes("recommission");
  const isDecommission = !isRecommission && context.includes("decommission");
  if (!isDecommission && !isRecommission) {
    return cloneDeep(hosts);
  }
  const tasks = Array.isArray(event.Tasks)
    ? event.Tasks
    : Array.isArray(event.tasks)
      ? event.tasks
      : [];
  const result = cloneDeep(hosts);
  tasks.forEach((task: Record<string, unknown>) => {
    const hostName = String(task.hostName || task.host_name || "");
    const componentName = componentFromRequest(context, task);
    const component = result
      .find((host) => host.hostName === hostName)
      ?.hostComponents.find((item) => item.componentName === componentName);
    if (component) {
      component.adminState = isRecommission ? "INSERVICE" : "DECOMMISSIONED";
    }
  });
  return result;
}

export function escapeAmbariPredicateValue(value: string): string {
  return value.replace(/[\\^$.*+?()[\]{}|]/g, "\\$&");
}

export function buildHostSuggestionPredicate(field: string, value: string): string {
  if (!/^[a-z_]+$/i.test(field)) {
    throw new Error(`Unsupported host suggestion field: ${field}`);
  }
  return value
    ? `Hosts/${field}.matches(.*${escapeAmbariPredicateValue(value)}.*)`
    : "";
}

export async function deleteHostComponentsInOrder<T>(
  components: T[],
  deleteComponent: (component: T) => Promise<unknown>,
): Promise<void> {
  for (const component of components) {
    await deleteComponent(component);
  }
}

export type HostComponentConfigRules = {
  enableHiveInteractive: boolean;
  hiveDatabaseType: string;
  isOozieServerAddable: boolean;
};

type CurrentServiceConfig = {
  properties?: Record<string, string>;
  type?: string;
};

type CurrentServiceConfigResponse = {
  items?: Array<{ configurations?: CurrentServiceConfig[] }>;
};

export function hostComponentConfigRules(
  response: CurrentServiceConfigResponse,
  installedServices: string[],
): HostComponentConfigRules {
  const configurations = (response.items || []).flatMap(
    (item) => item.configurations || [],
  );
  const hiveEnv = configurations.find((config) => config.type === "hive-env");
  const hiveInteractiveEnv = configurations.find(
    (config) => config.type === "hive-interactive-env",
  );
  const oozieEnv = configurations.find((config) => config.type === "oozie-env");
  const oozieDatabase = oozieEnv?.properties?.oozie_database || "";

  return {
    enableHiveInteractive:
      hiveInteractiveEnv?.properties?.enable_hive_interactive === "true",
    hiveDatabaseType: hiveEnv?.properties?.hive_database || "",
    isOozieServerAddable: !installedServices.includes("OOZIE")
      || Boolean(oozieDatabase && oozieDatabase !== "New Derby Database"),
  };
}

export function shouldExcludeAddableHostComponent(
  componentName: string,
  installedServices: string[],
  rules: HostComponentConfigRules,
): boolean {
  return (componentName === "OOZIE_SERVER" && !rules.isOozieServerAddable)
    || (componentName === "HIVE_SERVER_INTERACTIVE" && !rules.enableHiveInteractive)
    || (componentName === "OZONE_DATANODE" && installedServices.includes("HDFS"));
}

type RawHostStackVersion = {
  HostStackVersions?: {
    stack?: string;
    state?: string;
  };
  is_visible?: boolean;
  repository_versions?: Array<{
    RepositoryVersions?: { repository_version?: string };
  }>;
};

type RawHostWithStackVersions = {
  stack_versions?: RawHostStackVersion[];
};

const repositoryVersionOf = (version: RawHostStackVersion): string =>
  String(version.repository_versions?.[0]?.RepositoryVersions?.repository_version || "");

export function hasCrossStackHostVersions(
  hosts: RawHostWithStackVersions[],
): boolean {
  return hosts.some((host) => {
    const versions = host.stack_versions || [];
    const current = versions.find(
      (version) => version.HostStackVersions?.state === "CURRENT",
    );
    const currentStack = current?.HostStackVersions?.stack;
    return Boolean(currentStack) && versions.some(
      (version) => version.HostStackVersions?.stack !== currentStack,
    );
  });
}

export function shouldLoadCompatibleRepositoryVersions(
  hosts: RawHostWithStackVersions[],
  stackName: string,
  stackVersion: string,
): boolean {
  return Boolean(stackName && stackVersion) && hasCrossStackHostVersions(hosts);
}

export function applyHostStackVersionVisibility(
  hosts: RawHostWithStackVersions[],
  compatibleRepositoryVersions: Iterable<string>,
  displayOlderVersions: boolean,
): void {
  const compatible = new Set(compatibleRepositoryVersions);
  hosts.forEach((host) => {
    const versions = host.stack_versions || [];
    const current = versions.find(
      (version) => version.HostStackVersions?.state === "CURRENT",
    );
    const currentStack = current?.HostStackVersions?.stack;
    const currentRepositoryVersion = current ? repositoryVersionOf(current) : "";

    versions.forEach((version) => {
      const repositoryVersion = repositoryVersionOf(version);
      const isDifferentStack = Boolean(current)
        && version.HostStackVersions?.stack !== currentStack;
      if (isDifferentStack && !compatible.has(repositoryVersion)) {
        version.is_visible = false;
        return;
      }
      version.is_visible = isDifferentStack
        || displayOlderVersions
        || !currentRepositoryVersion
        || stringUtilsObj.compareVersions(
          repositoryVersion,
          currentRepositoryVersion,
        ) >= 0;
    });
  });
}
