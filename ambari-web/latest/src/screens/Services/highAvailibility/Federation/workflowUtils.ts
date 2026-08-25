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

export interface ConfigItem {
  type: string;
  tag?: string;
  version?: number;
  properties: Record<string, string>;
  properties_attributes?: Record<string, unknown>;
  [key: string]: unknown;
}

export interface ConfigSnapshot {
  items: ConfigItem[];
  [key: string]: unknown;
}

export interface NamespaceTopology {
  name: string;
  hosts: string[];
}

export interface ComponentAssignment {
  component?: string;
  component_name?: string;
  hostName?: string;
  selectedHost?: string;
  isInstalled?: boolean;
  isAvailable?: boolean;
}

export interface ReviewedProperty {
  name: string;
  displayName: string;
  value: string;
  recommendedValue: string;
  filename: string;
  category: "HDFS" | "RANGER" | "ACCUMULO" | "HAWQ";
  description?: string;
  isEditable: boolean;
  isRequired?: boolean;
}

export interface NameNodeFederationConfigInput {
  clusterName: string;
  newNameserviceId: string;
  namespaces: NamespaceTopology[];
  assignments: ComponentAssignment[];
  journalNodeHosts: string[];
  installedServices: string[];
  snapshot: ConfigSnapshot;
}

export interface GeneratedConfiguration {
  snapshot: ConfigSnapshot;
  reviewedProperties: ReviewedProperty[];
}

export interface HawqCapabilityInput {
  serviceInstalled: boolean;
  hostCount?: number;
  configTypes: string[];
  stackComponents: Array<{
    name: string;
    customCommands?: string[];
  }>;
  installedComponents: Array<{
    name: string;
    hostName?: string;
    state?: string;
  }>;
}

export interface HawqCapabilities {
  supported: boolean;
  canAdd: boolean;
  canRemove: boolean;
  canActivate: boolean;
  reason?: string;
  masterHost?: string;
  standbyHost?: string;
}

export const NAME_SERVICE_ID_PATTERN =
  /^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9])$/;

const property = (
  name: string,
  value: string,
  filename: string,
  category: ReviewedProperty["category"],
  options: Partial<ReviewedProperty> = {},
): ReviewedProperty => ({
  name,
  displayName: name,
  value,
  recommendedValue: value,
  filename,
  category,
  isEditable: false,
  ...options,
});

const componentName = (assignment: ComponentAssignment) =>
  assignment.component || assignment.component_name || "";

const assignmentHost = (assignment: ComponentAssignment) =>
  assignment.hostName || assignment.selectedHost || "";

const unique = (values: string[]) => [...new Set(values.filter(Boolean))];

const cloneSnapshot = (snapshot: ConfigSnapshot): ConfigSnapshot => ({
  ...snapshot,
  items: (snapshot?.items || []).map((item) => ({
    ...item,
    properties: { ...(item.properties || {}) },
    ...(item.properties_attributes
      ? { properties_attributes: { ...item.properties_attributes } }
      : {}),
  })),
});

function requireSite(snapshot: ConfigSnapshot, type: string): ConfigItem {
  const site = snapshot.items.find((item) => item.type === type);
  if (!site) {
    throw new Error(`The current ${type} configuration is missing.`);
  }
  return site;
}

function ensureSite(snapshot: ConfigSnapshot, type: string): ConfigItem {
  let site = snapshot.items.find((item) => item.type === type);
  if (!site) {
    site = { type, properties: {} };
    snapshot.items.push(site);
  }
  return site;
}

function portFromAddress(value: string | undefined, fallback: string): string {
  const match = value?.match(/:(\d+)$/);
  return match?.[1] || fallback;
}

function nameservicesFrom(
  namespaces: NamespaceTopology[],
  hdfsProperties: Record<string, string>,
): string[] {
  const topologyNames = unique(namespaces.map((namespace) => namespace.name));
  const configuredNames = unique(
    (hdfsProperties["dfs.nameservices"] || "")
      .split(",")
      .map((name) => name.trim())
      .filter(Boolean),
  );
  if (topologyNames.length && configuredNames.length) {
    const differs =
      topologyNames.some((name) => !configuredNames.includes(name)) ||
      configuredNames.some((name) => !topologyNames.includes(name));
    if (differs) {
      throw new Error(
        "The loaded HDFS namespace topology does not match dfs.nameservices. Retry after the cluster model refreshes.",
      );
    }
  }
  return topologyNames.length ? topologyNames : configuredNames;
}

export function validateNameserviceId(
  value: string,
  existingNameservices: string[],
  topologyReady = true,
): string {
  if (!topologyReady) return "The existing HDFS namespaces are still loading.";
  if (!value) return "A nameservice ID is required.";
  if (!NAME_SERVICE_ID_PATTERN.test(value)) {
    return "Must contain 1 to 63 letters, numbers, or hyphens and cannot begin or end with a hyphen.";
  }
  if (existingNameservices.includes(value)) {
    return "This nameservice ID already exists.";
  }
  return "";
}

export function validateJournalNodeDirectory(value: string): string {
  if (!value) return "A JournalNode directory is required.";
  if (/\s$/.test(value)) return "Cannot contain trailing whitespace.";
  if (value.split(",").some((path) => /^\s/.test(path))) {
    return "Directories cannot begin with whitespace.";
  }
  const paths = value.split(",");
  const validPath = (path: string) =>
    /^\/(?!homes?(?:\/|$))[^\s,]*$/.test(path) ||
    /^[a-zA-Z]:[\\/][^,]*$/.test(path) ||
    /^file:\/\/\/[a-zA-Z]:[\\/][^,]*$/.test(path) ||
    /^file:\/\/\/[a-zA-Z]:$/.test(path);
  if (!paths.every(validPath)) {
    return "Use an absolute Unix path, Windows drive path, or file:/// Windows URL; /home and /homes are not allowed.";
  }
  return "";
}

export function validateComponentAssignments(
  assignments: ComponentAssignment[],
  component: string,
  expectedAdditional: number | "at-least-one",
  unavailableHosts: string[] = [],
): string {
  const matches = assignments.filter(
    (assignment) => componentName(assignment) === component,
  );
  const additional = matches.filter((assignment) => !assignment.isInstalled);
  const hosts = matches.map(assignmentHost);
  if (
    (expectedAdditional === "at-least-one" && additional.length < 1) ||
    (typeof expectedAdditional === "number" &&
      additional.length !== expectedAdditional)
  ) {
    return expectedAdditional === "at-least-one"
      ? `Select at least one additional ${component} host.`
      : `Select exactly ${expectedAdditional} additional ${component} hosts.`;
  }
  if (hosts.some((host) => !host)) return "Every component requires a host.";
  if (new Set(hosts).size !== hosts.length) {
    return `Two ${component} instances cannot share a host.`;
  }
  const missing = matches.find(
    (assignment) => assignment.isAvailable === false,
  );
  if (missing) {
    return `${assignmentHost(missing)} is no longer available in this cluster.`;
  }
  const unavailable = hosts.find((host) => unavailableHosts.includes(host));
  if (unavailable) return `${unavailable} is in maintenance mode.`;
  return "";
}

export function buildNameNodeFederationConfiguration(
  input: NameNodeFederationConfigInput,
): GeneratedConfiguration {
  const snapshot = cloneSnapshot(input.snapshot);
  const hdfsSite = requireSite(snapshot, "hdfs-site");
  const hdfs = hdfsSite.properties;
  const nameservices = nameservicesFrom(input.namespaces, hdfs);
  const idError = validateNameserviceId(
    input.newNameserviceId,
    nameservices,
  );
  if (idError) throw new Error(idError);

  const nameNodes = input.assignments.filter(
    (assignment) => componentName(assignment) === "NAMENODE",
  );
  const additionalNameNodes = nameNodes.filter(
    (assignment) => !assignment.isInstalled,
  );
  const assignmentError = validateComponentAssignments(
    input.assignments,
    "NAMENODE",
    2,
  );
  if (assignmentError) throw new Error(assignmentError);
  const journalNodeHosts = unique(input.journalNodeHosts).sort();
  if (!journalNodeHosts.length) {
    throw new Error("At least one JournalNode host is required.");
  }

  const firstNamespace = nameservices.find(
    (name) => hdfs[`dfs.namenode.rpc-address.${name}.nn1`],
  ) || nameservices[0];
  if (!firstNamespace) {
    throw new Error("The existing HDFS nameservice topology is missing.");
  }
  const firstNamespaceModel = input.namespaces.find(
    (namespace) => namespace.name === firstNamespace,
  );
  const originalNameNode1 =
    hdfs[`dfs.namenode.rpc-address.${firstNamespace}.nn1`]?.split(":")[0] ||
    firstNamespaceModel?.hosts[0];
  const originalNameNode2 =
    hdfs[`dfs.namenode.rpc-address.${firstNamespace}.nn2`]?.split(":")[0] ||
    firstNamespaceModel?.hosts[1];
  const newNameNode1 = assignmentHost(additionalNameNodes[0]);
  const newNameNode2 = assignmentHost(additionalNameNodes[1]);
  const newNameNode1Index = `nn${nameNodes.length - 1}`;
  const newNameNode2Index = `nn${nameNodes.length}`;
  const rpcPort = portFromAddress(hdfs["dfs.namenode.rpc-address"], "8020");
  const httpPort = portFromAddress(hdfs["dfs.namenode.http-address"], "50070");
  const httpsPort = portFromAddress(
    hdfs["dfs.namenode.https-address"],
    "50470",
  );
  const allNameservices = [...nameservices, input.newNameserviceId];
  const journalQuorum = journalNodeHosts
    .map((host) => `${host}:8485`)
    .join(";");
  const reviewed: ReviewedProperty[] = [];
  const setHdfs = (
    name: string,
    value: string,
    options: Partial<ReviewedProperty> = {},
  ) => {
    hdfs[name] = value;
    reviewed.push(property(name, value, "hdfs-site", "HDFS", options));
  };

  delete hdfs["dfs.namenode.shared.edits.dir"];
  delete hdfs["dfs.journalnode.edits.dir"];
  setHdfs("dfs.nameservices", allNameservices.join(","));
  setHdfs("dfs.internal.nameservices", allNameservices.join(","));
  setHdfs(
    `dfs.ha.namenodes.${input.newNameserviceId}`,
    `${newNameNode1Index},${newNameNode2Index}`,
  );
  [
    [newNameNode1Index, newNameNode1],
    [newNameNode2Index, newNameNode2],
  ].forEach(([index, host]) => {
    setHdfs(
      `dfs.namenode.rpc-address.${input.newNameserviceId}.${index}`,
      `${host}:${rpcPort}`,
    );
    setHdfs(
      `dfs.namenode.http-address.${input.newNameserviceId}.${index}`,
      `${host}:${httpPort}`,
    );
    setHdfs(
      `dfs.namenode.https-address.${input.newNameserviceId}.${index}`,
      `${host}:${httpsPort}`,
    );
  });
  setHdfs(
    `dfs.client.failover.proxy.provider.${input.newNameserviceId}`,
    "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider",
  );
  setHdfs(
    `dfs.namenode.shared.edits.dir.${input.newNameserviceId}`,
    `qjournal://${journalQuorum}/${input.newNameserviceId}`,
  );
  setHdfs(`dfs.journalnode.edits.dir.${input.newNameserviceId}`, "", {
    isEditable: true,
    isRequired: true,
    description: "The directory where JournalNode stores this nameservice's local state.",
  });

  const firstExpansion = nameservices.length === 1;
  if (firstExpansion) {
    setHdfs(
      `dfs.journalnode.edits.dir.${firstNamespace}`,
      input.snapshot.items.find((item) => item.type === "hdfs-site")
        ?.properties["dfs.journalnode.edits.dir"] || "/hadoop/hdfs/journal",
    );
    setHdfs(
      `dfs.namenode.shared.edits.dir.${firstNamespace}`,
      `qjournal://${journalQuorum}/${firstNamespace}`,
    );
  }

  const hasServiceRpc = Boolean(
    hdfs[`dfs.namenode.servicerpc-address.${firstNamespace}.nn1`] ||
      hdfs[`dfs.namenode.servicerpc-address.${firstNamespace}.nn2`],
  );
  if (hasServiceRpc) {
    if (firstExpansion && originalNameNode1 && originalNameNode2) {
      setHdfs(
        `dfs.namenode.servicerpc-address.${firstNamespace}.nn1`,
        `${originalNameNode1}:8021`,
      );
      setHdfs(
        `dfs.namenode.servicerpc-address.${firstNamespace}.nn2`,
        `${originalNameNode2}:8021`,
      );
    }
    setHdfs(
      `dfs.namenode.servicerpc-address.${input.newNameserviceId}.${newNameNode1Index}`,
      `${newNameNode1}:8021`,
    );
    setHdfs(
      `dfs.namenode.servicerpc-address.${input.newNameserviceId}.${newNameNode2Index}`,
      `${newNameNode2}:8021`,
    );
  }

  if (input.installedServices.includes("RANGER")) {
    const rangerTagsync = requireSite(snapshot, "ranger-tagsync-site");
    const rangerSecurity = requireSite(snapshot, "ranger-hdfs-security");
    const coreSite = requireSite(snapshot, "core-site");
    const configuredRepo =
      rangerSecurity.properties["ranger.plugin.hdfs.service.name"];
    const repoPrefix =
      configuredRepo === "{{repo_name}}"
        ? `${input.clusterName}_hadoop_`
        : `${configuredRepo || ""}_`;
    allNameservices.forEach((nameservice) => {
      const name =
        `ranger.tagsync.atlas.hdfs.instance.${input.clusterName}.` +
        `nameservice.${nameservice}.ranger.service`;
      const value = `${repoPrefix}${nameservice}`;
      rangerTagsync.properties[name] = value;
      reviewed.push(property(name, value, "ranger-tagsync-site", "RANGER"));
    });
    const defaultNameservice = (coreSite.properties["fs.defaultFS"] || "")
      .replace(/^hdfs:\/\//, "")
      .split(/[/:]/)[0];
    if (defaultNameservice) {
      const name =
        `ranger.tagsync.atlas.hdfs.instance.${input.clusterName}.ranger.service`;
      const value = `${repoPrefix}${defaultNameservice}`;
      rangerTagsync.properties[name] = value;
      reviewed.push(property(name, value, "ranger-tagsync-site", "RANGER"));
    }
  }

  if (input.installedServices.includes("ACCUMULO")) {
    const accumuloSite = requireSite(snapshot, "accumulo-site");
    const volumes = allNameservices
      .map((nameservice) => `hdfs://${nameservice}/apps/accumulo/data`)
      .join(",");
    const replacements = allNameservices
      .map((nameservice) => {
        const host =
          nameservice === input.newNameserviceId
            ? newNameNode1
            : input.namespaces.find((namespace) => namespace.name === nameservice)
                ?.hosts[0];
        if (!host) {
          throw new Error(`The NameNode host for ${nameservice} is missing.`);
        }
        return (
          `hdfs://${host}:8020/apps/accumulo/data ` +
          `hdfs://${nameservice}/apps/accumulo/data`
        );
      })
      .join(",");
    accumuloSite.properties["instance.volumes"] = volumes;
    accumuloSite.properties["instance.volumes.replacements"] = replacements;
    reviewed.push(
      property("instance.volumes", volumes, "accumulo-site", "ACCUMULO"),
      property(
        "instance.volumes.replacements",
        replacements,
        "accumulo-site",
        "ACCUMULO",
      ),
    );
  }

  return { snapshot, reviewedProperties: reviewed };
}

export function applyReviewedProperty(
  generated: GeneratedConfiguration,
  propertyName: string,
  value: string,
): GeneratedConfiguration {
  const reviewedProperty = generated.reviewedProperties.find(
    (item) => item.name === propertyName,
  );
  if (!reviewedProperty?.isEditable) {
    throw new Error(`${propertyName} is not editable.`);
  }
  const error = validateJournalNodeDirectory(value);
  if (error) throw new Error(error);
  const snapshot = cloneSnapshot(generated.snapshot);
  requireSite(snapshot, reviewedProperty.filename).properties[propertyName] =
    value;
  return {
    snapshot,
    reviewedProperties: generated.reviewedProperties.map((item) =>
      item.name === propertyName
        ? { ...item, value, recommendedValue: value }
        : item,
    ),
  };
}

export function buildRouterFederationConfiguration(
  snapshotValue: ConfigSnapshot,
  namespaces: NamespaceTopology[],
): GeneratedConfiguration {
  if (namespaces.length < 2) {
    throw new Error("Router-based Federation requires multiple nameservices.");
  }
  const snapshot = cloneSnapshot(snapshotValue);
  const coreSite = requireSite(snapshot, "core-site");
  const hdfsSite = requireSite(snapshot, "hdfs-site");
  nameservicesFrom(namespaces, hdfsSite.properties);
  const routerSite = ensureSite(snapshot, "hdfs-rbf-site");
  let nameNodeCounter = 1;
  const monitored = namespaces.flatMap((namespace) =>
    namespace.hosts.slice(0, 2).map(() => `${namespace.name}.nn${nameNodeCounter++}`),
  );
  if (monitored.length !== namespaces.length * 2) {
    throw new Error("Every nameservice must contain two NameNode hosts.");
  }
  const values: Record<string, string> = {
    "dfs.federation.router.monitor.namenode": monitored.join(","),
    "dfs.federation.router.default.nameserviceId": namespaces[0].name,
    "zk-dt-secret-manager.zkAuthType": "none",
    "zk-dt-secret-manager.zkConnectionString":
      coreSite.properties["ha.zookeeper.quorum"] || "",
  };
  if (!values["zk-dt-secret-manager.zkConnectionString"]) {
    throw new Error("The HDFS ZooKeeper quorum is missing.");
  }
  Object.assign(routerSite.properties, values);
  return {
    snapshot,
    reviewedProperties: Object.entries(values).map(([name, value]) =>
      property(name, value, "hdfs-rbf-site", "HDFS"),
    ),
  };
}

export function buildDesiredConfigQuery(
  desiredConfigs: Record<string, { tag?: string }>,
  requiredTypes: string[],
  optionalTypes: string[] = [],
): string {
  const query = requiredTypes.map((type) => {
    const tag = desiredConfigs[type]?.tag;
    if (!tag) throw new Error(`The current ${type} configuration tag is missing.`);
    return `(type=${type}&tag=${tag})`;
  });
  optionalTypes.forEach((type) => {
    const tag = desiredConfigs[type]?.tag;
    if (tag) query.push(`(type=${type}&tag=${tag})`);
  });
  return query.join("|");
}

export function buildFederationRestartPayload(clusterName: string) {
  const exclusions = [
    "NAMENODE",
    "JOURNALNODE",
    "ZKFC",
    "RANGER_ADMIN",
    "RANGER_USERSYNC",
  ].map((name) => `HostRoles/component_name!=${name}`);
  return {
    RequestInfo: {
      command: "RESTART",
      context: "Restart all required services",
      operation_level: "host_component",
    },
    "Requests/resource_filters": [
      {
        hosts_predicate: [
          `HostRoles/cluster_name=${clusterName}`,
          ...exclusions,
        ].join("&"),
      },
    ],
  };
}

export const federationTaskKeys = (
  installedServices: string[],
): string[] => [
  "stopRequiredServices",
  "reconfigureServices",
  "installNameNode",
  "installZKFC",
  "startJournalNodes",
  ...(installedServices.includes("RANGER") &&
  installedServices.includes("AMBARI_INFRA_SOLR")
    ? ["startInfraSolr"]
    : []),
  ...(installedServices.includes("RANGER")
    ? ["startRangerAdmin", "startRangerUsersync"]
    : []),
  "startNameNodes",
  "startZKFCs",
  "formatNameNode",
  "formatZKFC",
  "startZKFC",
  "startNameNode",
  "bootstrapNameNode",
  "startZKFC2",
  "startNameNode2",
  "restartAllServices",
];

export function buildHostValidationPayload(
  hosts: string[],
  services: string[],
  assignments: ComponentAssignment[],
) {
  const hostGroups = unique(hosts).map((host, index) => ({
    name: `host-group-${index + 1}`,
    host,
  }));
  return {
    hosts: unique(hosts),
    services: unique(services),
    validate: "host_groups",
    recommendations: {
      blueprint: {
        host_groups: hostGroups.map(({ name, host }) => ({
          name,
          components: assignments
            .filter((assignment) => assignmentHost(assignment) === host)
            .map((assignment) => ({ name: componentName(assignment) })),
        })),
      },
      blueprint_cluster_binding: {
        host_groups: hostGroups.map(({ name, host }) => ({
          name,
          hosts: [{ fqdn: host }],
        })),
      },
    },
  };
}

export function evaluateHawqCapabilities(
  input: HawqCapabilityInput,
): HawqCapabilities {
  const stackNames = input.stackComponents.map((component) => component.name);
  if (
    !input.serviceInstalled ||
    !input.configTypes.includes("hawq-site") ||
    !stackNames.includes("HAWQMASTER") ||
    !stackNames.includes("HAWQSTANDBY")
  ) {
    return {
      supported: false,
      canAdd: false,
      canRemove: false,
      canActivate: false,
      reason:
        "The installed stack does not expose the HAWQ master, standby, and hawq-site contracts.",
    };
  }
  const master = input.installedComponents.find(
    (component) => component.name === "HAWQMASTER",
  );
  const standby = input.installedComponents.find(
    (component) => component.name === "HAWQSTANDBY",
  );
  const hasMaster = Boolean(master?.hostName);
  const hasStandby = Boolean(standby?.hostName);
  const masterCommands =
    input.stackComponents.find((component) => component.name === "HAWQMASTER")
      ?.customCommands || [];
  const standbyCommands =
    input.stackComponents.find((component) => component.name === "HAWQSTANDBY")
      ?.customCommands || [];
  return {
    supported: hasMaster,
    canAdd: Boolean(
      hasMaster &&
        !standby &&
        (input.hostCount === undefined || input.hostCount > 1),
    ),
    canRemove: Boolean(
      hasMaster &&
        hasStandby &&
        master?.state === "STARTED" &&
        masterCommands.includes("REMOVE_HAWQ_STANDBY"),
    ),
    canActivate: Boolean(
      hasMaster &&
        hasStandby &&
        standbyCommands.includes("ACTIVATE_HAWQ_STANDBY"),
    ),
    masterHost: master?.hostName,
    standbyHost: standby?.hostName,
    ...(!hasMaster
      ? { reason: "The cluster does not have an installed HAWQ Master." }
      : input.hostCount !== undefined && input.hostCount <= 1 && !hasStandby
        ? { reason: "Adding a HAWQ Standby requires more than one cluster host." }
      : {}),
  };
}

export function mutateHawqConfiguration(
  snapshotValue: ConfigSnapshot,
  mode: "add" | "remove" | "activate",
  hosts: { masterHost: string; standbyHost: string },
): ConfigSnapshot {
  const snapshot = cloneSnapshot(snapshotValue);
  const hawqSite = requireSite(snapshot, "hawq-site");
  if (mode === "add") {
    if (!hosts.standbyHost) throw new Error("The new HAWQ Standby host is missing.");
    hawqSite.properties.hawq_standby_address_host = hosts.standbyHost;
  } else {
    delete hawqSite.properties.hawq_standby_address_host;
    if (mode === "activate") {
      if (!hosts.standbyHost) throw new Error("The HAWQ Standby host is missing.");
      hawqSite.properties.hawq_master_address_host = hosts.standbyHost;
    }
  }
  return snapshot;
}

export const hawqTaskKeys = (
  mode: "add" | "remove" | "activate",
): string[] => {
  if (mode === "add") {
    return [
      "stopRequiredServices",
      "installHawqStandbyMaster",
      "reconfigureHAWQ",
      "startRequiredServices",
    ];
  }
  if (mode === "remove") {
    return [
      "removeStandby",
      "stopRequiredServices",
      "reconfigureHAWQ",
      "deleteHawqStandbyComponent",
      "startRequiredServices",
    ];
  }
  return [
    "activateStandby",
    "stopRequiredServices",
    "reconfigureHAWQ",
    "installHawqMaster",
    "deleteOldHawqMaster",
    "deleteHawqStandby",
    "startRequiredServices",
  ];
};

export function isMissingComponentError(error: unknown): boolean {
  const value = error as {
    message?: string;
    status?: number;
    response?: { data?: { message?: string }; status?: number };
  };
  if (value?.status === 404 || value?.response?.status === 404) return true;
  const message = value?.response?.data?.message || value?.message || "";
  return /NoSuchResourceException|does not exist|not found/i.test(message);
}
