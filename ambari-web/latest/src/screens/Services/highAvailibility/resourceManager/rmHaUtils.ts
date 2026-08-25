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

import rmHaProperties from "../../../../data/configs/wizards/rm_ha_properties";
import RmHaConfigInitializer from "../../../../Utils/rm_ha_config_initializer";
import {
  PersistedRmHaOperation,
  RmHaAssignment,
  RmHaConfigProperty,
  RmHaHost,
  RmHaMasterAssignment,
  RmHaReviewConfig,
  RmHaTopologyEntry,
} from "./rmHaTypes";

export const RM_HA_OPERATION_IDS = {
  STOP_REQUIRED_SERVICES: "stop-required-services",
  INSTALL_RESOURCE_MANAGER: "install-resource-manager",
  RECONFIGURE_YARN: "reconfigure-yarn",
  RECONFIGURE_HAWQ: "reconfigure-hawq",
  RECONFIGURE_HDFS: "reconfigure-hdfs",
  START_ALL_SERVICES: "start-all-services",
} as const;

export const RM_HA_ENABLEMENT_MESSAGES = {
  enablePermission: "You are not authorized to enable ResourceManager HA.",
  persistPermission:
    "ResourceManager HA requires permission to persist wizard state.",
  yarnMissing: "YARN must be installed before ResourceManager HA can be enabled.",
  resourceManagerMissing:
    "An installed ResourceManager is required before ResourceManager HA can be enabled.",
  resourceManagerStopped:
    "ResourceManager must be running before you enable ResourceManager HA.",
  zooKeeperCount:
    "You must have at least 3 ZooKeeper Servers in your cluster to enable ResourceManager HA.",
  hostCount:
    "You must have at least 3 hosts in your cluster to enable ResourceManager HA.",
  alreadyEnabled: "ResourceManager HA is already enabled for YARN.",
} as const;

type EnablementInput = {
  topology: RmHaTopologyEntry[];
  hostNames: string[];
  yarnInstalled: boolean;
  alreadyEnabled: boolean;
  canEnableHa: boolean;
  canPersist: boolean;
};

type HostRoleResource = {
  HostRoles?: {
    component_name?: string;
    service_name?: string;
    host_name?: string;
    state?: string;
    maintenance_state?: string;
  };
};

type ComponentResource = {
  ServiceComponentInfo?: {
    component_name?: string;
    service_name?: string;
  };
  component_name?: string;
  service_name?: string;
  host_components?: HostRoleResource[];
};

type HostResource = {
  Hosts?: {
    host_name?: string;
    cpu_count?: number;
    total_mem?: number;
    maintenance_state?: string;
    disk_info?: unknown[];
  };
  host_name?: string;
  cpu_count?: number;
  total_mem?: number;
  maintenance_state?: string;
  disk_info?: unknown[];
};

type MasterCandidate = Partial<RmHaMasterAssignment>;

type ConfigItem = {
  type?: string;
  properties?: Record<string, unknown>;
  properties_attributes?: Record<string, unknown>;
};

type RequiredConfigItem = ConfigItem & {
  type: string;
  properties: Record<string, unknown>;
};

type AdvisorHostGroup = {
  name?: string;
  components?: Array<{ name?: string }>;
  hosts?: Array<{ fqdn?: string }>;
};

type AdvisorResponse = {
  resources?: Array<{
    recommendations?: {
      blueprint?: {
        host_groups?: AdvisorHostGroup[];
        configurations?: Record<
          string,
          { properties?: Record<string, unknown> }
        >;
      };
      blueprint_cluster_binding?: { host_groups?: AdvisorHostGroup[] };
    };
  }>;
};

const asArray = <T,>(value: unknown): T[] =>
  Array.isArray(value) ? (value as T[]) : [];

const responseItems = <T,>(value: unknown): T[] => {
  const response = value as { items?: unknown } | null;
  return asArray<T>(response?.items ?? value);
};

export function responseErrorMessage(
  error: unknown,
  fallback: string,
): string {
  const requestError = error as {
    message?: string;
    response?: { data?: { message?: string } };
  };
  return requestError?.response?.data?.message || requestError?.message || fallback;
}

export function flattenClusterTopology(data: unknown): RmHaTopologyEntry[] {
  const items = responseItems<ComponentResource>(data);
  return items.flatMap((item) => {
    const component =
      item?.ServiceComponentInfo?.component_name || item?.component_name || "";
    const serviceName =
      item?.ServiceComponentInfo?.service_name || item?.service_name || "";
    return asArray<HostRoleResource>(item?.host_components).flatMap((hostComponent) => {
      const role = hostComponent?.HostRoles;
      if (!role?.host_name || !(role.component_name || component)) return [];
      return [
        {
          component: role.component_name || component,
          hostName: role.host_name,
          serviceName: role.service_name || serviceName,
          state: role.state,
          maintenanceState: role.maintenance_state,
          isInstalled: true,
        },
      ];
    });
  });
}

export function parseRmHaHosts(data: unknown): RmHaHost[] {
  const items = responseItems<HostResource>(data);
  return items.flatMap((item) => {
    const host = item?.Hosts ?? item;
    if (typeof host?.host_name !== "string" || !host.host_name) return [];
    return [
      {
        hostName: host.host_name,
        cpuCount:
          typeof host.cpu_count === "number" ? host.cpu_count : undefined,
        totalMemory:
          typeof host.total_mem === "number" ? host.total_mem : undefined,
        maintenanceState:
          typeof host.maintenance_state === "string"
            ? host.maintenance_state
            : undefined,
        diskInfo: Array.isArray(host.disk_info) ? host.disk_info : undefined,
      },
    ];
  });
}

export function getRmHaEnablementErrors({
  topology,
  hostNames,
  yarnInstalled,
  alreadyEnabled,
  canEnableHa,
  canPersist,
}: EnablementInput): string[] {
  const errors: string[] = [];
  if (!canEnableHa) errors.push(RM_HA_ENABLEMENT_MESSAGES.enablePermission);
  if (!canPersist) errors.push(RM_HA_ENABLEMENT_MESSAGES.persistPermission);
  if (!yarnInstalled) errors.push(RM_HA_ENABLEMENT_MESSAGES.yarnMissing);
  if (alreadyEnabled) errors.push(RM_HA_ENABLEMENT_MESSAGES.alreadyEnabled);

  const resourceManagers = topology.filter(
    ({ component }) => component === "RESOURCEMANAGER",
  );
  if (!resourceManagers.length) {
    errors.push(RM_HA_ENABLEMENT_MESSAGES.resourceManagerMissing);
  } else if (resourceManagers[0].state !== "STARTED") {
    errors.push(RM_HA_ENABLEMENT_MESSAGES.resourceManagerStopped);
  }
  if (
    topology.filter(({ component }) => component === "ZOOKEEPER_SERVER").length <
    3
  ) {
    errors.push(RM_HA_ENABLEMENT_MESSAGES.zooKeeperCount);
  }
  if (hostNames.length < 3) errors.push(RM_HA_ENABLEMENT_MESSAGES.hostCount);
  return errors;
}

export function stackCoordinates(cluster: {
  version?: string;
  stack?: string;
  versionNum?: string;
}): {
  stack: string;
  version: string;
} {
  const versionParts = String(cluster?.version || "").split("-");
  const stack = cluster?.stack || versionParts[0] || "";
  const version = cluster?.versionNum || versionParts.slice(1).join("-") || "";
  if (!stack || !version) {
    throw new Error("Ambari did not provide the current stack and version.");
  }
  return { stack, version };
}

export function buildTopologyBlueprint(
  hostNames: string[],
  topology: RmHaTopologyEntry[],
) {
  const uniqueHosts = Array.from(new Set(hostNames));
  return {
    blueprint: {
      host_groups: uniqueHosts.map((hostName, index) => ({
        name: `host-group-${index + 1}`,
        components: Array.from(
          new Set(
            topology
              .filter(({ hostName: candidate }) => candidate === hostName)
              .map(({ component }) => component),
          ),
        ).map((name) => ({ name })),
      })),
    },
    blueprint_cluster_binding: {
      host_groups: uniqueHosts.map((hostName, index) => ({
        name: `host-group-${index + 1}`,
        hosts: [{ fqdn: hostName }],
      })),
    },
  };
}

export function buildHostRecommendationPayload({
  hostNames,
  services,
  topology,
}: {
  hostNames: string[];
  services: string[];
  topology: RmHaTopologyEntry[];
}) {
  return {
    recommend: "host_groups",
    hosts: hostNames,
    services,
    recommendations: buildTopologyBlueprint(hostNames, topology),
  };
}

export function recommendedHostsForComponent(
  data: unknown,
  componentName: string,
): string[] {
  const recommendations = (data as AdvisorResponse)?.resources?.[0]
    ?.recommendations;
  const blueprintGroups = recommendations?.blueprint?.host_groups;
  const bindingGroups = recommendations?.blueprint_cluster_binding?.host_groups;
  if (!Array.isArray(blueprintGroups) || !Array.isArray(bindingGroups)) {
    throw new Error("Stack Advisor returned an invalid host recommendation response.");
  }
  const hostsByGroup = new Map<string, string[]>();
  bindingGroups.forEach((group) => {
    if (!group.name) return;
    hostsByGroup.set(
      group.name,
      asArray<{ fqdn?: string }>(group.hosts)
        .map((host) => host.fqdn)
        .filter((host): host is string => Boolean(host)),
    );
  });
  return Array.from(
    new Set(
      blueprintGroups.flatMap((group) =>
        asArray<{ name?: string }>(group.components).some(
          (component) => component.name === componentName,
        )
          ? group.name
            ? hostsByGroup.get(group.name) || []
            : []
          : [],
      ),
    ),
  );
}

export function chooseAdditionalRmHost(
  recommendedHosts: string[],
  hosts: RmHaHost[],
  currentRM: string,
): string {
  const eligible = hosts.filter(
    ({ hostName, maintenanceState }) =>
      hostName !== currentRM &&
      (!maintenanceState || maintenanceState === "OFF"),
  );
  const eligibleNames = new Set(eligible.map(({ hostName }) => hostName));
  const recommended = recommendedHosts.find((host) => eligibleNames.has(host));
  if (recommended) return recommended;
  return [...eligible]
    .sort(
      (left, right) =>
        (right.totalMemory || 0) - (left.totalMemory || 0) ||
        (right.cpuCount || 0) - (left.cpuCount || 0) ||
        left.hostName.localeCompare(right.hostName),
    )[0]?.hostName || "";
}

export function visibleHostOptions<T extends { label: string }>(
  options: T[],
  inputValue: string,
  useTypeaheadLimit: boolean,
): T[] {
  if (!useTypeaheadLimit) return options;
  const query = inputValue.trim().toLocaleLowerCase();
  return options
    .filter(({ label }) => label.toLocaleLowerCase().includes(query))
    .slice(0, 10);
}

export function createRmHaAssignment(
  currentRM: string,
  additionalRM: string,
  hosts: RmHaHost[],
  topology: RmHaTopologyEntry[],
): RmHaAssignment {
  if (!currentRM) throw new Error("The current ResourceManager host is missing.");
  if (!additionalRM || additionalRM === currentRM) {
    throw new Error("Select a different host for the additional ResourceManager.");
  }
  if (!hosts.some(({ hostName }) => hostName === additionalRM)) {
    throw new Error("The selected additional ResourceManager host is unavailable.");
  }
  const currentHost = hosts.find(({ hostName }) => hostName === currentRM);
  const additionalHost = hosts.find(({ hostName }) => hostName === additionalRM);
  if (!currentHost) {
    throw new Error("The current ResourceManager host is unavailable.");
  }
  if (
    currentHost.maintenanceState &&
    currentHost.maintenanceState !== "OFF"
  ) {
    throw new Error("The current ResourceManager host is in maintenance mode.");
  }
  if (
    additionalHost?.maintenanceState &&
    additionalHost.maintenanceState !== "OFF"
  ) {
    throw new Error("The additional ResourceManager host is in maintenance mode.");
  }
  const masterComponentHosts = [
    {
      component: "RESOURCEMANAGER" as const,
      component_name: "RESOURCEMANAGER" as const,
      hostName: currentRM,
      selectedHost: currentRM,
      serviceId: "YARN" as const,
      isInstalled: true,
    },
    {
      component: "RESOURCEMANAGER" as const,
      component_name: "RESOURCEMANAGER" as const,
      hostName: additionalRM,
      selectedHost: additionalRM,
      serviceId: "YARN" as const,
      isInstalled: false,
    },
  ];
  const topologyHosts = topology.filter(
    ({ component, hostName }) =>
      component !== "RESOURCEMANAGER" || hostName === currentRM,
  );
  topologyHosts.push({
    component: "RESOURCEMANAGER",
    hostName: additionalRM,
    serviceName: "YARN",
    isInstalled: false,
  });
  return { currentRM, additionalRM, hosts, masterComponentHosts, topologyHosts };
}

export function getRmHaAssignment(stepData: unknown): RmHaAssignment | null {
  const envelope = stepData as { data?: unknown } | null;
  const data = (envelope?.data || stepData) as
    | (Partial<RmHaAssignment> & { masterComponentHosts?: unknown })
    | null;
  const masters = asArray<MasterCandidate>(data?.masterComponentHosts);
  const currentRM =
    data?.currentRM ||
    masters.find(
      (item) =>
        (item.component || item.component_name) === "RESOURCEMANAGER" &&
        item.isInstalled,
    )?.hostName;
  const additionalMaster = masters.find(
    (item) =>
      (item.component || item.component_name) === "RESOURCEMANAGER" &&
      !item.isInstalled,
  );
  const additionalRM =
    data?.additionalRM ||
    additionalMaster?.hostName ||
    additionalMaster?.selectedHost;
  if (!currentRM || !additionalRM) return null;
  return {
    currentRM,
    additionalRM,
    hosts: asArray<RmHaHost>(data?.hosts),
    masterComponentHosts: masters as RmHaAssignment["masterComponentHosts"],
    topologyHosts: asArray<RmHaTopologyEntry>(data?.topologyHosts),
  };
}

export function requiredDesiredTag(data: unknown, type: string): string {
  const response = data as {
    Clusters?: { desired_configs?: Record<string, { tag?: unknown }> };
  };
  const tag = response?.Clusters?.desired_configs?.[type]?.tag;
  if (typeof tag !== "string" || !tag) {
    throw new Error(`Ambari did not return the active ${type} configuration tag.`);
  }
  return tag;
}

export function requireConfigItems(
  data: unknown,
  types: string[],
): RequiredConfigItem[] {
  const items = responseItems<ConfigItem>(data);
  types.forEach((type) => {
    const item = items.find((candidate) => candidate?.type === type);
    if (!item || !item.properties || typeof item.properties !== "object") {
      throw new Error(`Ambari did not return a valid ${type} configuration.`);
    }
  });
  return items as RequiredConfigItem[];
}

function extractPort(address: unknown, fallback: string): string {
  if (typeof address !== "string") return fallback;
  const separator = address.lastIndexOf(":");
  const port = separator >= 0 ? address.slice(separator + 1) : "";
  return /^\d+$/.test(port) ? port : fallback;
}

function recommendationConfigurationProperties(data: unknown) {
  const configurations =
    (data as AdvisorResponse)?.resources?.[0]?.recommendations?.blueprint
      ?.configurations;
  if (!configurations || typeof configurations !== "object") {
    throw new Error(
      "Stack Advisor returned an invalid configuration recommendation response.",
    );
  }
  return configurations["core-site"]?.properties || {};
}

export function buildConfigRecommendationPayload({
  hostNames,
  services,
  topology,
  configurations,
}: {
  hostNames: string[];
  services: string[];
  topology: RmHaTopologyEntry[];
  configurations: Record<string, { properties: Record<string, unknown> }>;
}) {
  const topologyBlueprint = buildTopologyBlueprint(hostNames, topology);
  const recommendations = {
    ...topologyBlueprint,
    blueprint: {
      ...topologyBlueprint.blueprint,
      configurations,
    },
  };
  return {
    recommend: "configurations",
    hosts: hostNames,
    services,
    recommendations,
  };
}

export function buildRmHaReviewConfig({
  configData,
  recommendationData,
  selectedServices,
  topology,
}: {
  configData: unknown;
  recommendationData: unknown;
  selectedServices: string[];
  topology: RmHaTopologyEntry[];
}): RmHaReviewConfig {
  const items = requireConfigItems(configData, [
    "zoo.cfg",
    "yarn-site",
    "yarn-env",
  ]);
  const zooCfg = items.find((item) => item.type === "zoo.cfg")!;
  const yarnSite = items.find((item) => item.type === "yarn-site")!;
  const yarnEnv = items.find((item) => item.type === "yarn-env")!;
  const yarnUser = yarnEnv.properties.yarn_user;
  if (typeof yarnUser !== "string" || !yarnUser) {
    throw new Error("Ambari did not return yarn-env/yarn_user.");
  }

  const source = rmHaProperties.haConfig;
  const configCategories = source.configCategories
    .filter((category) => selectedServices.includes(category.name))
    .map((category) => ({ ...category }));
  const allowedCategories = new Set(configCategories.map(({ name }) => name));
  const configs = source.configs
    .filter((config) => allowedCategories.has(config.category))
    .map((config) => ({
      ...config,
      isEditable: false,
      isOverridable: false,
      changedValue: config.value,
    })) as RmHaConfigProperty[];

  const initializer = RmHaConfigInitializer();
  initializer.setup({ yarnUser });
  const dependencies = {
    zkClientPort: String(zooCfg.properties.clientPort || "2181"),
    webAddressPort: extractPort(
      yarnSite.properties["yarn.resourcemanager.webapp.address"],
      "8088",
    ),
    httpsWebAddressPort: extractPort(
      yarnSite.properties["yarn.resourcemanager.webapp.https.address"],
      "8090",
    ),
    trackerAddressPort: extractPort(
      yarnSite.properties["yarn.resourcemanager.resource-tracker.address"],
      "8025",
    ),
  };
  try {
    configs.forEach((config) => {
      initializer.initialValue(
        config as unknown as Parameters<typeof initializer.initialValue>[0],
        { masterComponentHosts: topology },
        dependencies,
      );
      config.changedValue = config.value;
    });
  } finally {
    initializer.cleanup();
  }

  const proxyName = `hadoop.proxyuser.${yarnUser}.hosts`;
  const proxyValue = recommendationConfigurationProperties(recommendationData)[proxyName];
  if (typeof proxyValue === "string" && proxyValue) {
    const existing = configs.find(({ name }) => name === proxyName);
    if (existing) {
      existing.value = proxyValue;
      existing.changedValue = proxyValue;
      existing.recommendedValue = proxyValue;
    } else if (allowedCategories.has("HDFS")) {
      configs.push({
        name: proxyName,
        displayName: proxyName,
        category: "HDFS",
        filename: "core-site",
        value: proxyValue,
        changedValue: proxyValue,
        recommendedValue: proxyValue,
        isEditable: false,
        isOverridable: false,
        serviceName: "MISC",
      });
    }
  }
  return {
    serviceName: source.serviceName,
    displayName: source.displayName,
    configCategories,
    configs,
  };
}

export function buildDesiredConfigPayload(
  configData: unknown,
  type: string,
  reviewConfig: RmHaReviewConfig,
  note: string,
) {
  const items = requireConfigItems(configData, [type]);
  const current = items.find((item) => item.type === type)!;
  const properties = { ...current.properties };
  const changedProperties = reviewConfig.configs.filter(
    (config) => config.filename === type,
  );
  changedProperties.forEach(({ name, value }) => {
    properties[name] = value;
  });
  const desiredConfig: Record<string, unknown> = {
    type,
    properties,
    service_config_version_note: note,
  };
  if (current.properties_attributes) {
    desiredConfig.properties_attributes = current.properties_attributes;
  }
  return { Clusters: { desired_config: [desiredConfig] } };
}

const terminalFailure = new Set(["FAILED", "TIMEDOUT", "ABORTED"]);

export function canCompleteRmHa(operations: PersistedRmHaOperation[]): boolean {
  if (!operations.length) return false;
  const finalIndex = operations.findIndex(
    ({ id }) => id === RM_HA_OPERATION_IDS.START_ALL_SERVICES,
  );
  if (finalIndex < 0 || finalIndex !== operations.length - 1) return false;
  if (
    operations
      .slice(0, finalIndex)
      .some(({ status }) => status !== "COMPLETED")
  ) {
    return false;
  }
  const finalStatus = operations[finalIndex].status || "";
  return finalStatus === "COMPLETED" || terminalFailure.has(finalStatus);
}

export function stripOperationCallbacks(
  operations: Array<PersistedRmHaOperation & { callback?: unknown }>,
): PersistedRmHaOperation[] {
  return operations.map(({ callback: _callback, ...operation }) => operation);
}
