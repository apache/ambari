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

import { ambariApi } from "../../../../api/config/axiosConfig";
import {
  buildRangerAdminConfigBodies,
  buildRangerAdminConfigQuery,
} from "./rangerAdminHaUtils";
import { wizardConfigs } from "./wizardConstants";

type UnknownRecord = Record<string, unknown>;

export interface RangerAdminConfigApi {
  loadConfigTags: (clusterName: string) => Promise<unknown>;
  reassignLoadConfigs: (clusterName: string, query: string) => Promise<unknown>;
  updateServiceMultiConfigurations: (
    clusterName: string,
    data: { configs: UnknownRecord[] },
  ) => Promise<unknown>;
}

export interface RangerAdminEnablementApi {
  loadRangerAdminComponent: (clusterName: string) => Promise<unknown>;
}

export const RANGER_ADMIN_ENABLEMENT_MESSAGES = {
  hostCount:
    "Ranger Admin high availability requires at least two cluster hosts.",
  rangerAdminMissing:
    "An installed Ranger Admin is required before Ranger Admin high availability can be enabled.",
  rangerAdminNotInstalled:
    "Ranger Admin must be installed before high availability can be enabled.",
  alreadyEnabled: "Ranger Admin high availability is already enabled.",
} as const;

export type RangerAdminEnablementStatus = "enabled" | "disabled" | "hidden";

export interface RangerAdminEnablementResult {
  status: RangerAdminEnablementStatus;
  errors: string[];
}

interface RangerAdminHostComponent {
  hostName: string;
  state: string;
}

const isRecord = (value: unknown): value is UnknownRecord =>
  Boolean(value) && typeof value === "object" && !Array.isArray(value);

export const rangerAdminConfigApi: RangerAdminConfigApi = {
  async loadConfigTags(clusterName) {
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}?fields=Clusters/desired_configs`,
      method: "GET",
    });
    return response.data;
  },

  async reassignLoadConfigs(clusterName, query) {
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}/configurations?${query}`,
      method: "GET",
    });
    return response.data;
  },

  async updateServiceMultiConfigurations(clusterName, data) {
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}`,
      method: "PUT",
      data: data.configs,
    });
    return isRecord(response.data)
      ? { ...response.data, status: response.status }
      : { data: response.data, status: response.status };
  },
};

export const rangerAdminEnablementApi: RangerAdminEnablementApi = {
  async loadRangerAdminComponent(clusterName) {
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}/services/RANGER/components/RANGER_ADMIN?fields=host_components/HostRoles/host_name,host_components/HostRoles/state`,
      method: "GET",
    });
    return response.data;
  },
};

function parseRangerAdminHostComponents(
  response: unknown,
): RangerAdminHostComponent[] {
  if (!isRecord(response) || !Array.isArray(response.host_components)) {
    throw new Error("Ambari returned malformed Ranger Admin component data.");
  }
  return response.host_components.map((hostComponent) => {
    if (
      !isRecord(hostComponent) ||
      !isRecord(hostComponent.HostRoles) ||
      typeof hostComponent.HostRoles.host_name !== "string" ||
      !hostComponent.HostRoles.host_name ||
      typeof hostComponent.HostRoles.state !== "string" ||
      !hostComponent.HostRoles.state
    ) {
      throw new Error("Ambari returned malformed Ranger Admin host data.");
    }
    return {
      hostName: hostComponent.HostRoles.host_name,
      state: hostComponent.HostRoles.state,
    };
  });
}

export function countRangerAdminHostComponents(response: unknown) {
  return parseRangerAdminHostComponents(response).length;
}

export function evaluateRangerAdminEnablement(
  response: unknown,
  hostCount: number,
): RangerAdminEnablementResult {
  const rangerAdmins = parseRangerAdminHostComponents(response);
  if (rangerAdmins.length > 1) {
    return {
      status: "hidden",
      errors: [RANGER_ADMIN_ENABLEMENT_MESSAGES.alreadyEnabled],
    };
  }

  const errors: string[] = [];
  if (hostCount <= 1) {
    errors.push(RANGER_ADMIN_ENABLEMENT_MESSAGES.hostCount);
  }
  if (!rangerAdmins.length) {
    errors.push(RANGER_ADMIN_ENABLEMENT_MESSAGES.rangerAdminMissing);
  } else if (["INIT", "INSTALL_FAILED"].includes(rangerAdmins[0].state)) {
    errors.push(RANGER_ADMIN_ENABLEMENT_MESSAGES.rangerAdminNotInstalled);
  }
  return {
    status: errors.length ? "disabled" : "enabled",
    errors,
  };
}

function desiredConfigsFromResponse(response: unknown) {
  if (!isRecord(response) || !isRecord(response.Clusters)) {
    throw new Error("Ambari returned malformed desired configuration data.");
  }
  const desiredConfigs = response.Clusters.desired_configs;
  if (!isRecord(desiredConfigs)) {
    throw new Error("Ambari returned malformed desired configuration data.");
  }

  const candidateSites = new Set(wizardConfigs.map(({ siteName }) => siteName));
  Object.entries(desiredConfigs).forEach(([siteName, config]) => {
    if (!candidateSites.has(siteName)) return;
    if (!isRecord(config) || typeof config.tag !== "string" || !config.tag) {
      throw new Error(
        `The current ${siteName} configuration tag is missing or malformed.`,
      );
    }
  });

  const adminProperties = desiredConfigs["admin-properties"];
  if (
    !isRecord(adminProperties) ||
    typeof adminProperties.tag !== "string" ||
    !adminProperties.tag
  ) {
    throw new Error(
      "The current admin-properties configuration tag is missing or malformed.",
    );
  }
  return desiredConfigs as Record<string, { tag?: string }>;
}

function validateLoadedConfigs(response: unknown, expectedSiteNames: string[]) {
  if (!isRecord(response) || !Array.isArray(response.items)) {
    throw new Error("Ambari returned a malformed Ranger configuration list.");
  }
  const items = response.items;
  if (
    items.some(
      (item) =>
        !isRecord(item) ||
        typeof item.type !== "string" ||
        !isRecord(item.properties),
    )
  ) {
    throw new Error("Ambari returned a malformed Ranger configuration item.");
  }

  expectedSiteNames.forEach((siteName) => {
    if (!items.some((item) => isRecord(item) && item.type === siteName)) {
      throw new Error(
        `Ambari did not return the current ${siteName} configuration.`,
      );
    }
  });
  return items as UnknownRecord[];
}

export async function reconfigureRangerAdminServices(
  clusterName: string,
  loadBalancerUrl: string,
  api: RangerAdminConfigApi = rangerAdminConfigApi,
) {
  const tags = await api.loadConfigTags(clusterName);
  const desiredConfigs = desiredConfigsFromResponse(tags);
  const query = buildRangerAdminConfigQuery(desiredConfigs);
  const expectedSiteNames = wizardConfigs
    .filter(({ siteName }) => desiredConfigs[siteName]?.tag)
    .map(({ siteName }) => siteName);
  const loadedConfigs = await api.reassignLoadConfigs(clusterName, query);
  const configItems = validateLoadedConfigs(loadedConfigs, expectedSiteNames);
  const note = "This configuration is created by Enable Ranger Admin HA wizard";
  const configs = buildRangerAdminConfigBodies(
    configItems,
    loadBalancerUrl,
    note,
  );
  if (!configs.length || configs.length !== expectedSiteNames.length) {
    throw new Error("No complete Ranger Admin HA configurations were loaded.");
  }
  return api.updateServiceMultiConfigurations(clusterName, { configs });
}
