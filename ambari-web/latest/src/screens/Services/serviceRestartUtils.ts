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

import type { BackgroundRequest } from "../../Utils/backgroundOperations";

export type ServiceRestartScope = "ALL" | "MASTERS" | "SLAVES";
export type ServiceRestartMode = "ROLLING" | "EXPRESS";

export type RestartableServiceComponent = {
  serviceName: string;
  componentName: string;
  displayName: string;
  hostName: string;
  category: "MASTER" | "SLAVE";
  maintenanceState: string;
  haState: string;
};

export type ServiceRestartGroups = {
  masters: RestartableServiceComponent[];
  slaves: RestartableServiceComponent[];
};

type BuildScheduleOptions = {
  clusterName: string;
  serviceName: string;
  components: RestartableServiceComponent[];
  batchSize: number;
  intervalTimeSeconds: number;
  tolerateSize: number;
};

type BuildExpressOptions = {
  clusterName: string;
  serviceName: string;
  scope: ServiceRestartScope;
  components: RestartableServiceComponent[];
};

type HostComponentLike = {
  HostRoles?: {
    host_name?: string;
    component_name?: string;
    display_name?: string;
    maintenance_state?: string;
    ha_state?: string;
  };
  hostName?: string;
  host?: { hostName?: string };
  componentName?: string;
  component_name?: string;
  displayName?: string;
  passiveState?: string;
  haStatus?: string;
  nnHAState?: string;
};

type ComponentGroupLike = {
  componentName?: string;
  component_name?: string;
  displayName?: string;
  hostComponents?: HostComponentLike[];
};

type ServiceRestartModelLike = {
  masterComponents?: ComponentGroupLike[];
  slaveComponents?: ComponentGroupLike[];
  clientComponents?: ComponentGroupLike[];
  standbyNameNodes?: HostComponentLike[];
  activeNameNodes?: HostComponentLike[];
  isNameNodeHaEnabled?: boolean;
};

const ACTIVE_REQUEST_STATUSES = new Set([
  "IN_PROGRESS",
  "QUEUED",
  "PENDING",
  "HOLDING",
  "HOLDING_FAILED",
  "HOLDING_TIMEDOUT",
  "PAUSED",
]);

function asArray<T>(value: T[] | undefined | null): T[] {
  return Array.isArray(value) ? value : [];
}

function hostName(component: HostComponentLike): string {
  return component?.HostRoles?.host_name
    || component?.hostName
    || component?.host?.hostName
    || "";
}

function componentName(
  component: HostComponentLike,
  group: ComponentGroupLike,
): string {
  return component?.HostRoles?.component_name
    || component?.componentName
    || component?.component_name
    || group?.componentName
    || group?.component_name
    || "";
}

function normalizeGroups(
  groups: ComponentGroupLike[] | undefined,
  serviceName: string,
  category: "MASTER" | "SLAVE",
): RestartableServiceComponent[] {
  const result: RestartableServiceComponent[] = [];
  const seen = new Set<string>();

  asArray(groups).forEach((group) => {
    asArray(group?.hostComponents).forEach((component) => {
      const normalizedHostName = hostName(component);
      const normalizedComponentName = componentName(component, group);
      const key = `${normalizedComponentName}\u0000${normalizedHostName}`;
      if (!normalizedHostName || !normalizedComponentName || seen.has(key)) {
        return;
      }
      seen.add(key);
      result.push({
        serviceName,
        componentName: normalizedComponentName,
        displayName: component?.HostRoles?.display_name
          || component?.displayName
          || group?.displayName
          || normalizedComponentName,
        hostName: normalizedHostName,
        category,
        maintenanceState: component?.HostRoles?.maintenance_state
          || component?.passiveState
          || "OFF",
        haState: component?.HostRoles?.ha_state
          || component?.haStatus
          || component?.nnHAState
          || "",
      });
    });
  });

  return result;
}

function modelHostNames(value: HostComponentLike[] | undefined): Set<string> {
  return new Set(asArray(value).map(hostName).filter(Boolean));
}

function orderHdfsMasters(
  masters: RestartableServiceComponent[],
  serviceModel: ServiceRestartModelLike,
): RestartableServiceComponent[] {
  const secondaryNameNodes = masters.filter(
    (component) => component.componentName === "SECONDARY_NAMENODE",
  );
  const nameNodes = masters.filter(
    (component) => component.componentName === "NAMENODE",
  );
  const journalNodes = masters.filter(
    (component) => component.componentName === "JOURNALNODE",
  );
  const zkfcs = masters.filter((component) => component.componentName === "ZKFC");
  const standbyHosts = modelHostNames(serviceModel?.standbyNameNodes);
  const activeHosts = modelHostNames(serviceModel?.activeNameNodes);
  const isHaEnabled = Boolean(
    serviceModel?.isNameNodeHaEnabled
      || standbyHosts.size
      || activeHosts.size
      || (nameNodes.length > 1 && secondaryNameNodes.length === 0),
  );
  const ordered: RestartableServiceComponent[] = [];
  const included = new Set<string>();
  const append = (component: RestartableServiceComponent) => {
    const key = `${component.componentName}\u0000${component.hostName}`;
    if (!included.has(key)) {
      included.add(key);
      ordered.push(component);
    }
  };

  if (isHaEnabled) {
    journalNodes.forEach(append);
    const standby = nameNodes.filter((component) =>
      component.haState.toUpperCase() === "STANDBY"
        || standbyHosts.has(component.hostName),
    );
    const active = nameNodes.filter((component) =>
      component.haState.toUpperCase() === "ACTIVE"
        || activeHosts.has(component.hostName),
    );
    [...standby, ...active].forEach((nameNode) => {
      append(nameNode);
      zkfcs
        .filter((zkfc) => zkfc.hostName === nameNode.hostName)
        .forEach(append);
    });
  } else {
    secondaryNameNodes.forEach(append);
    nameNodes.forEach(append);
  }

  // Keep stack extensions restartable even when Classic's HDFS special-order
  // list does not know their component names.
  masters.forEach(append);
  return ordered;
}

export function getServiceRestartGroups(
  serviceName: string,
  serviceModel: ServiceRestartModelLike | undefined,
): ServiceRestartGroups {
  const masters = normalizeGroups(
    serviceModel?.masterComponents,
    serviceName,
    "MASTER",
  );
  const slaves = normalizeGroups(
    serviceModel?.slaveComponents,
    serviceName,
    "SLAVE",
  );
  const hdfsZkfcs = serviceName === "HDFS"
    ? slaves
      .filter((component) => component.componentName === "ZKFC")
      .map((component) => ({ ...component, category: "MASTER" as const }))
    : [];
  return {
    masters: serviceName === "HDFS"
      ? orderHdfsMasters([...masters, ...hdfsZkfcs], serviceModel || {})
      : masters,
    slaves,
  };
}

export function selectServiceRestartComponents(
  groups: ServiceRestartGroups,
  scope: ServiceRestartScope,
): RestartableServiceComponent[] {
  if (scope === "MASTERS") {
    return groups.masters;
  }
  if (scope === "SLAVES") {
    return groups.slaves;
  }
  const seen = new Set<string>();
  return [...groups.masters, ...groups.slaves].filter((component) => {
    const key = `${component.componentName}\u0000${component.hostName}`;
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}

function rollingRequest(
  clusterName: string,
  serviceName: string,
  componentNameValue: string,
  hosts: string[],
  orderId: number,
  batchNumber: number,
  batchCount: number,
) {
  return {
    order_id: orderId,
    type: "POST",
    uri: `/clusters/${clusterName}/requests`,
    RequestBodyInfo: {
      RequestInfo: {
        context: `_PARSE_.ROLLING-RESTART.${componentNameValue}.${batchNumber}.${batchCount}`,
        command: "RESTART",
      },
      "Requests/resource_filters": [
        {
          service_name: serviceName,
          component_name: componentNameValue,
          hosts: hosts.join(","),
        },
      ],
    },
  };
}

export function buildServiceRestartSchedule({
  clusterName,
  serviceName,
  components,
  batchSize,
  intervalTimeSeconds,
  tolerateSize,
}: BuildScheduleOptions) {
  const masters = components.filter((component) => component.category === "MASTER");
  const slaves = components.filter((component) => component.category === "SLAVE");
  const batches: ReturnType<typeof rollingRequest>[] = [];
  let orderId = 1;

  const masterTotals = new Map<string, number>();
  const masterIndexes = new Map<string, number>();
  masters.forEach((component) => {
    masterTotals.set(
      component.componentName,
      (masterTotals.get(component.componentName) || 0) + 1,
    );
  });
  masters.forEach((component) => {
    const batchNumber = (masterIndexes.get(component.componentName) || 0) + 1;
    masterIndexes.set(component.componentName, batchNumber);
    batches.push(rollingRequest(
      clusterName,
      serviceName,
      component.componentName,
      [component.hostName],
      orderId++,
      batchNumber,
      masterTotals.get(component.componentName) || 1,
    ));
  });

  const slaveGroups = new Map<string, RestartableServiceComponent[]>();
  slaves.forEach((component) => {
    const group = slaveGroups.get(component.componentName) || [];
    group.push(component);
    slaveGroups.set(component.componentName, group);
  });
  slaveGroups.forEach((group, name) => {
    const batchCount = Math.ceil(group.length / batchSize);
    for (let index = 0; index < group.length; index += batchSize) {
      const batchNumber = Math.floor(index / batchSize) + 1;
      batches.push(rollingRequest(
        clusterName,
        serviceName,
        name,
        group.slice(index, index + batchSize).map((component) => component.hostName),
        orderId++,
        batchNumber,
        batchCount,
      ));
    }
  });

  return [
    {
      RequestSchedule: {
        batch: [
          { requests: batches },
          {
            batch_settings: {
              batch_separation_in_seconds: intervalTimeSeconds,
              task_failure_tolerance: tolerateSize,
            },
          },
        ],
      },
    },
  ];
}

export function buildExpressServiceRestartRequest({
  clusterName,
  serviceName,
  scope,
  components,
}: BuildExpressOptions) {
  const componentHosts = new Map<string, string[]>();
  components
    .filter((component) => component.maintenanceState.toUpperCase() === "OFF")
    .forEach((component) => {
      const hosts = componentHosts.get(component.componentName) || [];
      hosts.push(component.hostName);
      componentHosts.set(component.componentName, hosts);
    });

  return {
    RequestInfo: {
      command: "RESTART",
      context: `_PARSE_.RESTART.${serviceName}.${scope}`,
      operation_level: {
        level: "SERVICE",
        cluster_name: clusterName,
        service_name: serviceName,
      },
    },
    "Requests/resource_filters": [...componentHosts].map(([name, hosts]) => ({
      service_name: serviceName,
      component_name: name,
      hosts: hosts.join(","),
    })),
  };
}

export function hasActiveServiceComponentRestart(
  requests: BackgroundRequest[],
  componentNames: string[],
  serviceName?: string,
): boolean {
  const serviceComponents = new Set(componentNames);
  return requests.some((request) => {
    const status = request?.Requests?.request_status?.toUpperCase() || "";
    const match = request?.Requests?.request_context?.match(
      /^_PARSE_\.ROLLING-RESTART\.([^.]+)\./,
    );
    const resourceFilters = request?.Requests?.resource_filters;
    const targetServices = Array.isArray(resourceFilters)
      ? resourceFilters
        .map((filter) => String(filter?.service_name || "").toUpperCase())
        .filter(Boolean)
      : [];
    const targetsService = !serviceName
      || targetServices.length === 0
      || targetServices.includes(serviceName.toUpperCase());
    return ACTIVE_REQUEST_STATUSES.has(status)
      && targetsService
      && Boolean(match && serviceComponents.has(match[1]));
  });
}
