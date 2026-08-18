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

export interface CheckpointEvaluation {
  ready: boolean;
  started: boolean;
  error?: string;
}

export interface JournalNodeFormatEvaluation {
  ready: boolean;
  missingHosts: string[];
  invalidHosts: string[];
  error?: string;
}

export interface JournalNodeChangeSet {
  finalHosts: string[];
  addedHosts: string[];
  deletedHosts: string[];
  isDeleteOnly: boolean;
  isNoOp: boolean;
}

export interface HdfsNamespace {
  name: string;
  hosts: string[];
}

export interface JournalNodeDirectoryEvaluation {
  directories: string[];
  missingProperties: string[];
}

type UnknownRecord = Record<string, unknown>;

interface MasterComponentHost extends UnknownRecord {
  component?: string;
  component_name?: string;
  hostName?: string;
  selectedHost?: string;
  isInstalled?: boolean;
}

interface ConfigSite extends UnknownRecord {
  type?: string;
  properties?: Record<string, string>;
}

const asArray = (value: unknown): unknown[] =>
  Array.isArray(value) ? value : [];

export function getHdfsUser(configData: unknown, fallback = "hdfs"): string {
  const configurations = asArray(asRecord(configData)?.items).flatMap(
    (item) => asArray(asRecord(item)?.configurations),
  );
  const hadoopEnv = configurations
    .map(asRecord)
    .find((config) => config?.type === "hadoop-env");
  const hdfsUser = asRecord(hadoopEnv?.properties)?.hdfs_user;
  return typeof hdfsUser === "string" && hdfsUser ? hdfsUser : fallback;
}

export function getHdfsNamespaces(hdfsModel: unknown): HdfsNamespace[] {
  const model = asRecord(hdfsModel);
  const federationNamespaces = asArray(model?.federationNamespaces);
  const namespaceData = federationNamespaces.length
    ? federationNamespaces
    : asArray(model?.namespaces);
  return namespaceData
    .map((namespaceValue) => {
      const namespace = asRecord(namespaceValue);
      const name = namespace?.name || namespace?.nameSpace;
      const hosts = namespace?.hosts || namespace?.hostNames;
      return {
        name: typeof name === "string" ? name : "",
        hosts: asArray(hosts).filter(
          (host): host is string => typeof host === "string" && Boolean(host),
        ),
      };
    })
    .filter(
      (namespace: HdfsNamespace) =>
        namespace.name && namespace.hosts.some(Boolean),
    )
    .map((namespace: HdfsNamespace) => ({
      ...namespace,
      hosts: [...new Set(namespace.hosts.filter(Boolean))],
    }));
}

export function getJournalNodeDirectories(
  hdfsModel: unknown,
  configProperties: Record<string, string> = {},
): JournalNodeDirectoryEvaluation {
  const namespaces = getHdfsNamespaces(hdfsModel);
  const propertyNames =
    namespaces.length > 1
      ? namespaces.map(
          (namespace) => `dfs.journalnode.edits.dir.${namespace.name}`,
        )
      : ["dfs.journalnode.edits.dir"];
  const missingProperties = propertyNames.filter(
    (propertyName) => !configProperties[propertyName]?.trim(),
  );
  const directories = [
    ...new Set(
      propertyNames
        .map((propertyName) => configProperties[propertyName]?.trim())
        .filter(Boolean),
    ),
  ] as string[];

  return { directories, missingProperties };
}

export function isValidNameNodeHaAssignment(
  masterComponentHosts: MasterComponentHost[] = [],
): boolean {
  const hostFor = (item: MasterComponentHost) =>
    item.hostName || item.selectedHost;
  const nameNodes = masterComponentHosts.filter(
    (item) =>
      item.component === "NAMENODE" || item.component_name === "NAMENODE",
  );
  const journalNodes = masterComponentHosts.filter(
    (item) =>
      item.component === "JOURNALNODE" ||
      item.component_name === "JOURNALNODE",
  );
  const installedNameNodes = nameNodes.filter((item) => item.isInstalled);
  const additionalNameNodes = nameNodes.filter((item) => !item.isInstalled);
  const uniqueNameNodeHosts = new Set(nameNodes.map(hostFor).filter(Boolean));
  const uniqueJournalNodeHosts = new Set(
    journalNodes.map(hostFor).filter(Boolean),
  );

  return (
    installedNameNodes.length === 1 &&
    additionalNameNodes.length === 1 &&
    uniqueNameNodeHosts.size === nameNodes.length &&
    journalNodes.length >= 3 &&
    uniqueJournalNodeHosts.size === journalNodes.length
  );
}

type Operation = {
  id: string | number;
  callback: () => Promise<unknown>;
  [key: string]: unknown;
};

function asRecord(value: unknown): UnknownRecord | null {
  if (value && typeof value === "object" && !Array.isArray(value)) {
    return value as UnknownRecord;
  }
  if (typeof value !== "string" || !value.trim()) {
    return null;
  }
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === "object" && !Array.isArray(parsed)
      ? parsed
      : null;
  } catch {
    return null;
  }
}

export function evaluateNameNodeCheckpoint(data: unknown): CheckpointEvaluation {
  const root = asRecord(data);
  const started = asRecord(root?.HostRoles)?.desired_state === "STARTED";
  const namenode = asRecord(
    asRecord(asRecord(root?.metrics)?.dfs)?.namenode,
  );
  const safeMode = namenode?.Safemode;
  const transactionInfo = asRecord(
    namenode?.JournalTransactionInfo,
  );
  if (!transactionInfo) {
    return {
      ready: false,
      started,
      error: "NameNode checkpoint transaction data is missing or malformed.",
    };
  }

  const lastWritten = Number(transactionInfo.LastAppliedOrWrittenTxId);
  const checkpoint = Number(transactionInfo.MostRecentCheckpointTxId);
  if (!Number.isFinite(lastWritten) || !Number.isFinite(checkpoint)) {
    return {
      ready: false,
      started,
      error: "NameNode checkpoint transaction IDs are invalid.",
    };
  }

  return {
    ready: Boolean(safeMode) && lastWritten - checkpoint <= 1,
    started,
  };
}

export function evaluateJournalNodeFormatted(
  data: unknown,
  nameserviceId: string,
): { ready: boolean; error?: string } {
  const root = asRecord(data);
  const journalnode = asRecord(
    asRecord(asRecord(root?.metrics)?.dfs)?.journalnode,
  );
  const journalsStatus = asRecord(journalnode?.journalsStatus);
  if (!journalsStatus) {
    return {
      ready: false,
      error: "JournalNode formatted status is missing or malformed.",
    };
  }
  const nameserviceStatus = journalsStatus[nameserviceId];
  if (!nameserviceStatus) {
    return {
      ready: false,
      error: `JournalNode did not report nameservice ${nameserviceId}.`,
    };
  }
  return {
    ready:
      String(asRecord(nameserviceStatus)?.Formatted).toLowerCase() === "true",
  };
}

export function evaluateJournalNodeFormatSet(
  expectedHosts: string[],
  responses: Record<string, unknown>,
  nameserviceId: string,
): JournalNodeFormatEvaluation {
  const hosts = [...new Set(expectedHosts)].sort();
  const missingHosts = hosts.filter((host) => !(host in responses));
  const invalidHosts: string[] = [];
  let firstError = "";

  hosts.forEach((host) => {
    if (!(host in responses)) {
      return;
    }
    const evaluation = evaluateJournalNodeFormatted(
      responses[host],
      nameserviceId,
    );
    if (!evaluation.ready) {
      invalidHosts.push(host);
      firstError ||= evaluation.error || "JournalNode is not formatted yet.";
    }
  });

  return {
    ready: hosts.length >= 3 && !missingHosts.length && !invalidHosts.length,
    missingHosts,
    invalidHosts,
    error: firstError || undefined,
  };
}

export function evaluateCheckpointSet(
  expectedHosts: string[],
  items: unknown[],
): CheckpointEvaluation {
  const hosts = [...new Set(expectedHosts)].sort();
  const responseHosts = items
    .map((item) => asRecord(asRecord(item)?.HostRoles)?.host_name)
    .filter((host): host is string => typeof host === "string" && Boolean(host));
  const uniqueResponseHosts = [...new Set(responseHosts)].sort();

  if (
    hosts.length !== items.length ||
    hosts.length !== uniqueResponseHosts.length ||
    hosts.some((host, index) => host !== uniqueResponseHosts[index])
  ) {
    return {
      ready: false,
      started: false,
      error: "Ambari returned an incomplete or duplicate NameNode checkpoint set.",
    };
  }

  const evaluations = items.map(evaluateNameNodeCheckpoint);
  return {
    ready: evaluations.every((item) => item.ready),
    started: evaluations.every((item) => item.started),
    error: evaluations.find((item) => item.error)?.error,
  };
}

export function buildJournalNodeSharedEditsConfigs(
  nameserviceIds: string[],
  journalNodeHosts: string[],
  federated: boolean,
) {
  const hosts = [...new Set(journalNodeHosts)].sort();
  const nameservices = [...new Set(nameserviceIds.filter(Boolean))];
  const journalQuorum = hosts.map((host) => `${host}:8485`).join(";");
  const targetNameservices = federated ? nameservices : nameservices.slice(0, 1);

  return targetNameservices.map((nameserviceId) => {
    const name = federated
      ? `dfs.namenode.shared.edits.dir.${nameserviceId}`
      : "dfs.namenode.shared.edits.dir";
    const value = `qjournal://${journalQuorum}/${nameserviceId}`;
    return {
      name,
      displayName: name,
      description:
        "The URI which identifies the group of JournalNodes used for shared edits.",
      isReconfigurable: false,
      isOverridable: false,
      recommendedValue: value,
      value,
      changedValue: value,
      category: "HDFS",
      filename: "hdfs-site",
      serviceName: "MISC",
    };
  });
}

export function getJournalNodeChangeSet(
  masterComponentHosts: MasterComponentHost[] = [],
  originalHosts: string[] = [],
): JournalNodeChangeSet {
  const finalHosts = [
    ...new Set(
      masterComponentHosts
        .filter(
          (item) =>
            item.component === "JOURNALNODE" ||
            item.component_name === "JOURNALNODE",
        )
        .map((item) => item.hostName || item.selectedHost)
        .filter(
          (host): host is string => typeof host === "string" && Boolean(host),
        ),
    ),
  ].sort();
  const originals = [...new Set(originalHosts.filter(Boolean))].sort();
  const addedHosts = finalHosts.filter((host) => !originals.includes(host));
  const deletedHosts = originals.filter((host) => !finalHosts.includes(host));

  return {
    finalHosts,
    addedHosts,
    deletedHosts,
    isDeleteOnly: !addedHosts.length && deletedHosts.length > 0,
    isNoOp: !addedHosts.length && !deletedHosts.length,
  };
}

export function mergeSavedOperations<T extends Operation>(
  operations: T[],
  savedOperations: Array<Partial<T>> | undefined,
): T[] {
  if (!Array.isArray(savedOperations)) {
    return operations;
  }
  return operations.map((operation) => {
    const saved = savedOperations.find(
      (item) =>
        item.id === operation.id ||
        (item.label && item.label === operation.label),
    );
    return saved
      ? ({
          ...operation,
          ...saved,
          id: operation.id,
          callback: operation.callback,
        } as T)
      : operation;
  });
}

export function buildDesiredConfigQuery(
  desiredConfigs: Record<string, { tag?: string }> = {},
  siteNames: string[],
): string {
  return [...new Set(siteNames)].map((siteName) => {
    const tag = desiredConfigs[siteName]?.tag;
    if (!tag) {
      throw new Error(`The current ${siteName} configuration tag is missing.`);
    }
    return `(type=${siteName}&tag=${tag})`;
  }).join("|");
}

export function mergeReviewedConfigs(
  currentConfigs: { items?: ConfigSite[] },
  reviewedConfigs: { items?: ConfigSite[] },
  configsToRemove: Record<string, string[]> = {},
): { items: ConfigSite[] } {
  const reviewedItems = reviewedConfigs?.items || [];
  const items = (currentConfigs?.items || []).map((currentItem) => {
    const reviewedItem = reviewedItems.find(
      (item) => item.type === currentItem.type,
    );
    const properties = {
      ...(currentItem.properties || {}),
      ...(reviewedItem?.properties || {}),
    };
    (configsToRemove[currentItem.type || ""] || []).forEach((propertyName) => {
      delete properties[propertyName];
    });
    return {
      ...currentItem,
      properties,
    };
  });

  return { ...currentConfigs, items };
}

export function updateReviewedConfigValue(
  configData: { items?: ConfigSite[] },
  siteName: string,
  propertyName: string,
  value: string,
) {
  let siteFound = false;
  const items = (configData?.items || []).map((item) => {
    if (item.type !== siteName) return item;
    siteFound = true;
    return {
      ...item,
      properties: {
        ...(item.properties || {}),
        [propertyName]: value,
      },
    };
  });
  if (!siteFound) {
    throw new Error(`The reviewed ${siteName} configuration is missing.`);
  }
  return { ...configData, items };
}

export function getRangerReconfigureSiteGroups(
  selectedServices: string[],
  configItems: ConfigSite[],
): string[][] {
  const hasAuditProperty = (type: string) => {
    const site = configItems.find((item) => item.type === type);
    return Boolean(
      site?.properties &&
        Object.prototype.hasOwnProperty.call(
          site.properties,
          "xasecure.audit.destination.hdfs.dir",
        ),
    );
  };
  const present = (...types: string[]) => types.filter(hasAuditProperty);
  const groups: string[][] = [["ranger-env"]];

  if (selectedServices.includes("YARN")) {
    groups.push(present("ranger-yarn-audit"));
  }
  if (selectedServices.includes("STORM")) {
    groups.push(
      present("ranger-storm-plugin-properties", "ranger-storm-audit"),
    );
  }
  if (selectedServices.includes("KAFKA")) {
    groups.push(present("ranger-kafka-audit"));
  }
  if (selectedServices.includes("KNOX")) {
    groups.push(
      present("ranger-knox-plugin-properties", "ranger-knox-audit"),
    );
  }
  if (selectedServices.includes("ATLAS")) {
    groups.push(present("ranger-atlas-audit"));
  }
  if (selectedServices.includes("HIVE")) {
    groups.push(
      present("ranger-hive-plugin-properties", "ranger-hive-audit"),
    );
  }
  if (selectedServices.includes("RANGER_KMS")) {
    groups.push(present("ranger-kms-audit"));
  }

  return groups.filter((group) => group.length > 0);
}
