/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

type Repository = {
  applicableServices?: any[];
  baseUrl?: string;
  components?: any;
  distribution?: any;
  id?: string;
  mirrorsList?: any;
  name?: string;
  tags?: string[];
  unique?: boolean;
};

type OperatingSystem = {
  isAdded?: boolean;
  os?: string;
  repos?: Repository[];
};

type VersionDefinitionItem = {
  VersionDefinition?: {
    id?: string | number;
    repository_version?: string;
    stack_name?: string;
    stack_version?: string;
  };
  operating_systems?: Array<{
    OperatingSystems?: {
      ambari_managed_repositories?: boolean;
      os_type?: string;
    };
    repositories?: Array<{
      Repositories?: {
        applicable_services?: any[];
        base_url?: string;
        components?: any;
        distribution?: any;
        mirrors_list?: any;
        repo_id?: string;
        repo_name?: string;
        tags?: string[];
        unique?: boolean;
      };
    }>;
  }>;
};

export type RepositoryVersionResolution =
  | { kind: "create" }
  | {
      kind: "reuse";
      repositoryVersionId: string;
      stackName: string;
      stackVersion: string;
    }
  | { kind: "conflict"; versionLabel: string };

const normalizedText = (value: unknown) => String(value ?? "").trim();

const stableValue = (value: any): any => {
  if (Array.isArray(value)) {
    return value.map(stableValue).sort((left, right) =>
      JSON.stringify(left).localeCompare(JSON.stringify(right)),
    );
  }
  if (!value || typeof value !== "object") return value ?? null;
  return Object.fromEntries(
    Object.keys(value).sort().map((key) => [key, stableValue(value[key])]),
  );
};

const normalizedRepositories = (
  operatingSystems: OperatingSystem[],
  ambariManagedRepositories: boolean,
) =>
  operatingSystems
    .filter((operatingSystem) => operatingSystem.isAdded !== false)
    .map((operatingSystem) => ({
      osType: normalizedText(operatingSystem.os),
      ambariManagedRepositories,
      repositories: (operatingSystem.repos || [])
        .map((repository) => ({
          applicableServices: stableValue(repository.applicableServices || []),
          baseUrl: normalizedText(repository.baseUrl),
          components: stableValue(repository.components),
          distribution: stableValue(repository.distribution),
          id: normalizedText(repository.id),
          mirrorsList: stableValue(repository.mirrorsList),
          name: normalizedText(repository.name),
          tags: stableValue(repository.tags || []),
          unique: repository.unique === true,
        }))
        .sort((left, right) => left.id.localeCompare(right.id)),
    }))
    .sort((left, right) => left.osType.localeCompare(right.osType));

const normalizedExistingRepositories = (item: VersionDefinitionItem) =>
  (item.operating_systems || [])
    .map((operatingSystem) => ({
      osType: normalizedText(operatingSystem.OperatingSystems?.os_type),
      ambariManagedRepositories:
        operatingSystem.OperatingSystems?.ambari_managed_repositories !== false,
      repositories: (operatingSystem.repositories || [])
        .map((repository) => ({
          applicableServices: stableValue(repository.Repositories?.applicable_services || []),
          baseUrl: normalizedText(repository.Repositories?.base_url),
          components: stableValue(repository.Repositories?.components),
          distribution: stableValue(repository.Repositories?.distribution),
          id: normalizedText(repository.Repositories?.repo_id),
          mirrorsList: stableValue(repository.Repositories?.mirrors_list),
          name: normalizedText(repository.Repositories?.repo_name),
          tags: stableValue(repository.Repositories?.tags || []),
          unique: repository.Repositories?.unique === true,
        }))
        .sort((left, right) => left.id.localeCompare(right.id)),
    }))
    .sort((left, right) => left.osType.localeCompare(right.osType));

export function resolveRepositoryVersion({
  ambariManagedRepositories,
  items,
  operatingSystems,
  repositoryVersion,
  stackName,
  stackVersion,
}: {
  ambariManagedRepositories: boolean;
  items: VersionDefinitionItem[];
  operatingSystems: OperatingSystem[];
  repositoryVersion: string;
  stackName: string;
  stackVersion: string;
}): RepositoryVersionResolution {
  const expectedVersion = normalizedText(repositoryVersion);
  const expectedStack = normalizedText(stackName);
  const expectedStackVersion = normalizedText(stackVersion);
  if (!expectedVersion || !expectedStack || !expectedStackVersion) {
    return { kind: "create" };
  }

  // Available stack templates have non-numeric IDs and still need to be materialized.
  const materializedMatches = items.filter((item) => {
    const definition = item.VersionDefinition || {};
    return /^\d+$/.test(normalizedText(definition.id))
      && normalizedText(definition.repository_version) === expectedVersion
      && normalizedText(definition.stack_name) === expectedStack
      && normalizedText(definition.stack_version) === expectedStackVersion;
  });
  if (!materializedMatches.length) {
    return { kind: "create" };
  }

  const desiredRepositories = JSON.stringify(
    normalizedRepositories(operatingSystems || [], ambariManagedRepositories),
  );
  const compatible = materializedMatches.find((item) =>
    JSON.stringify(normalizedExistingRepositories(item)) === desiredRepositories,
  );
  if (compatible) {
    return {
      kind: "reuse",
      repositoryVersionId: normalizedText(compatible.VersionDefinition?.id),
      stackName: expectedStack,
      stackVersion: expectedStackVersion,
    };
  }

  return {
    kind: "conflict",
    versionLabel: `${expectedStack}-${expectedVersion}`,
  };
}

export function repositorySelectionRequiresDeferredWrite(wizardName: string) {
  return wizardName === "clusterCreation";
}

export type InitialOperatingSystem = {
  "OperatingSystems/ambari_managed_repositories": boolean;
  "OperatingSystems/os_type": string;
  repositories: Array<Record<string, unknown>>;
};

export function buildInitialOperatingSystems(
  operatingSystems: OperatingSystem[],
  ambariManagedRepositories: boolean,
): InitialOperatingSystem[] {
  return (operatingSystems || [])
    .filter((operatingSystem) => operatingSystem.isAdded !== false)
    .map((operatingSystem) => ({
      "OperatingSystems/ambari_managed_repositories": ambariManagedRepositories,
      "OperatingSystems/os_type": normalizedText(operatingSystem.os),
      repositories: (operatingSystem.repos || []).map((repository) => {
        const payload: Record<string, unknown> = {
          "Repositories/base_url": normalizedText(repository.baseUrl),
          "Repositories/repo_id": normalizedText(repository.id),
          "Repositories/repo_name": normalizedText(repository.name),
        };
        if (repository.applicableServices != null) {
          payload["Repositories/applicable_services"] = repository.applicableServices;
        }
        if (repository.components != null) {
          payload["Repositories/components"] = repository.components;
        }
        if (repository.distribution != null) {
          payload["Repositories/distribution"] = repository.distribution;
        }
        if (repository.mirrorsList != null) {
          payload["Repositories/mirrors_list"] = repository.mirrorsList;
        }
        if (repository.tags != null) {
          payload["Repositories/tags"] = repository.tags;
        }
        if (repository.unique != null) {
          payload["Repositories/unique"] = repository.unique;
        }
        return payload;
      }),
    }));
}

export function utf8ToBase64(value: string): string {
  const bytes = new TextEncoder().encode(value);
  let binary = "";
  for (let offset = 0; offset < bytes.length; offset += 0x8000) {
    binary += String.fromCharCode(...bytes.subarray(offset, offset + 0x8000));
  }
  return btoa(binary);
}

export function buildAtomicVersionDefinitionPayload(
  sourcePayload: unknown,
  operatingSystems: InitialOperatingSystem[],
): Record<string, unknown> {
  const definition = typeof sourcePayload === "string"
    ? { VersionDefinition: { version_base64: utf8ToBase64(sourcePayload) } }
    : { ...(sourcePayload as Record<string, unknown> || {}) };
  return { ...definition, operating_systems: operatingSystems };
}
