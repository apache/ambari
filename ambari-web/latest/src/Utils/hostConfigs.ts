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

import { get } from "lodash";

export type HostConfigGroupState = {
  assignedGroupByService: Record<string, string>;
  groupsByService: Record<string, any[]>;
};

export type ConfigGroupMembershipUpdate = {
  group: any;
  groupId: string;
  payload: any;
};

function groupHosts(group: any): string[] {
  return get(group, "ConfigGroup.hosts", [])
    .map((host: any) => host?.host_name)
    .filter(Boolean);
}

export function buildHostConfigGroupState(
  groups: any[],
  serviceNames: string[],
  hostName: string,
): HostConfigGroupState {
  const groupsByService: Record<string, any[]> = {};
  const assignedGroupByService: Record<string, string> = {};

  serviceNames.forEach((serviceName) => {
    groupsByService[serviceName] = [];
    assignedGroupByService[serviceName] = "Default";
  });
  groups.forEach((group) => {
    const serviceName = get(
      group,
      "ConfigGroup.service_name",
      get(group, "ConfigGroup.tag", ""),
    );
    if (!groupsByService[serviceName]) {
      return;
    }
    groupsByService[serviceName].push(group);
    if (groupHosts(group).includes(hostName)) {
      assignedGroupByService[serviceName] = get(
        group,
        "ConfigGroup.group_name",
        "Default",
      );
    }
  });

  return { assignedGroupByService, groupsByService };
}

function payloadForHosts(group: any, serviceName: string, hosts: string[]) {
  const configGroup = get(group, "ConfigGroup", {});
  return [{
    ConfigGroup: {
      ...configGroup,
      group_name: configGroup.group_name || "",
      description: configGroup.description || "",
      tag: configGroup.tag || serviceName,
      service_name: configGroup.service_name || serviceName,
      hosts: hosts.map((host_name) => ({ host_name })),
      desired_configs: configGroup.desired_configs || [],
    },
  }];
}

export function buildConfigGroupMembershipUpdates(
  groups: any[],
  serviceName: string,
  currentGroupName: string,
  targetGroupName: string,
  hostName: string,
): ConfigGroupMembershipUpdate[] {
  if (currentGroupName === targetGroupName) {
    return [];
  }

  return groups.flatMap((group) => {
    const groupName = get(group, "ConfigGroup.group_name", "");
    if (groupName !== currentGroupName && groupName !== targetGroupName) {
      return [];
    }
    const hosts = new Set(groupHosts(group));
    if (groupName === currentGroupName) {
      hosts.delete(hostName);
    }
    if (groupName === targetGroupName) {
      hosts.add(hostName);
    }
    const updatedHosts = [...hosts];
    const updatedGroup = {
      ...group,
      ConfigGroup: {
        ...group.ConfigGroup,
        hosts: updatedHosts.map((host_name) => ({ host_name })),
      },
    };
    return [{
      group: updatedGroup,
      groupId: String(get(group, "ConfigGroup.id")),
      payload: payloadForHosts(updatedGroup, serviceName, updatedHosts),
    }];
  });
}
