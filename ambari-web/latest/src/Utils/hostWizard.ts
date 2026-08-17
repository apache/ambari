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

export type PreparedHostInput = {
  alreadyInstalled: string[];
  hadPattern: boolean;
  hosts: string[];
};

export function prepareHostInput(
  input: string,
  installedHosts: string[] = [],
): PreparedHostInput {
  let hadPattern = false;
  const expanded = input
    .split(/\s+/)
    .map((host) => host.trim().toLowerCase())
    .filter(Boolean)
    .flatMap((host) => {
      const match = /^(.*)\[(\d+)-(\d+)\](.*)$/.exec(host);
      if (!match) {
        return [host];
      }
      const [, prefix, startText, endText, suffix] = match;
      const start = Number(startText);
      const end = Number(endText);
      if (end < start) {
        return [host];
      }
      hadPattern = true;
      const width = Math.max(startText.length, endText.length);
      return Array.from({ length: end - start + 1 }, (_, index) =>
        `${prefix}${String(start + index).padStart(width, "0")}${suffix}`,
      );
    });
  const uniqueHosts = Array.from(new Set(expanded));
  const installed = new Set(installedHosts.map((host) => host.toLowerCase()));
  return {
    alreadyInstalled: uniqueHosts.filter((host) => installed.has(host)),
    hadPattern,
    hosts: uniqueHosts.filter((host) => !installed.has(host)),
  };
}

export type BootstrapSettings = {
  agentUserAccount?: string;
  customizeAgentUserAccount: boolean;
  hosts: string[];
  sshKey?: string;
  sshPortNumber?: number;
  sshUserAccount?: string;
};

export function buildBootstrapPayload(settings: BootstrapSettings) {
  return {
    verbose: true,
    sshKey: settings.sshKey || "",
    hosts: settings.hosts,
    user: settings.sshUserAccount || "",
    sshPort: String(settings.sshPortNumber || ""),
    userRunAs: settings.customizeAgentUserAccount
      ? settings.agentUserAccount || ""
      : "root",
  };
}

export function addHostRegistrationTimeoutSecs(
  usesAutomaticBootstrap: boolean,
): number {
  return usesAutomaticBootstrap ? 120 : 15;
}

export function selectedAddHostServices(
  assignments: AddHostAssignment[] = [],
  componentMetadata: AddHostComponentMetadata[] = [],
): string[] {
  const selectedComponents = new Set(
    assignments.flatMap((assignment) => (assignment.checkboxes || [])
      .filter((component) => component.checked && component.label !== "CLIENT")
      .map((component) => component.label || "")),
  );
  const clientsSelected = assignments.some((assignment) =>
    (assignment.checkboxes || []).some(
      (component) => component.checked && component.label === "CLIENT",
    ),
  );
  return Array.from(new Set(componentMetadata
    .filter((component) => selectedComponents.has(component.component_name || "")
      || (clientsSelected && isClientComponent(component)))
    .map((component) => component.service_name || "")
    .filter(Boolean)));
}

export type AddHostAssignment = {
  hostname?: string;
  checkboxes?: Array<{ checked?: boolean; label?: string }>;
};

export type AddHostComponentMetadata = {
  component_category?: string;
  component_name?: string;
  is_client?: boolean;
  service_name?: string;
};

export type AddHostConfigGroupUpdate = {
  groupId: string;
  payload: Array<{ ConfigGroup: Record<string, any> }>;
  serviceName: string;
};

function isClientComponent(component: AddHostComponentMetadata) {
  return component.is_client === true || component.component_category === "CLIENT";
}

export function buildAddHostComponentAssignments(
  assignments: AddHostAssignment[] = [],
  componentMetadata: AddHostComponentMetadata[] = [],
): Record<string, string[]> {
  const clientComponents = componentMetadata
    .filter(isClientComponent)
    .map((component) => component.component_name || "")
    .filter(Boolean);
  const hostsByComponent: Record<string, Set<string>> = {};

  assignments.forEach((assignment) => {
    if (!assignment.hostname) return;
    (assignment.checkboxes || [])
      .filter((component) => component.checked)
      .flatMap((component) => component.label === "CLIENT"
        ? clientComponents
        : [component.label || ""])
      .filter(Boolean)
      .forEach((componentName) => {
        if (!hostsByComponent[componentName]) {
          hostsByComponent[componentName] = new Set();
        }
        hostsByComponent[componentName].add(assignment.hostname!);
      });
  });

  return Object.fromEntries(Object.entries(hostsByComponent).map(
    ([componentName, hosts]) => [componentName, [...hosts]],
  ));
}

export function buildAddHostServiceHosts(
  assignments: AddHostAssignment[] = [],
  componentMetadata: AddHostComponentMetadata[] = [],
): Record<string, string[]> {
  const serviceByComponent = Object.fromEntries(componentMetadata.map((component) => [
    component.component_name || "",
    component.service_name || "",
  ]));
  const hostsByComponent = buildAddHostComponentAssignments(assignments, componentMetadata);
  const hostsByService: Record<string, Set<string>> = {};

  Object.entries(hostsByComponent).forEach(([componentName, hosts]) => {
    const serviceName = serviceByComponent[componentName];
    if (!serviceName) return;
    if (!hostsByService[serviceName]) {
      hostsByService[serviceName] = new Set();
    }
    hosts.forEach((hostName) => hostsByService[serviceName].add(hostName));
  });

  return Object.fromEntries(Object.entries(hostsByService).map(
    ([serviceName, hosts]) => [serviceName, [...hosts]],
  ));
}

export function buildAddHostConfigGroupUpdates(
  configurations: Array<Record<string, any>> = [],
  assignments: AddHostAssignment[] = [],
  componentMetadata: AddHostComponentMetadata[] = [],
): AddHostConfigGroupUpdate[] {
  const hostsByService = buildAddHostServiceHosts(assignments, componentMetadata);

  return configurations.flatMap((serviceConfiguration) => {
    const serviceName = serviceConfiguration.serviceName || "";
    const selectedGroup = (serviceConfiguration.configGroups || []).find(
      (group: Record<string, any>) => group.isSelected,
    );
    if (!selectedGroup || selectedGroup.group_name === "Default" || selectedGroup.id == null) {
      return [];
    }
    const existingHosts = (selectedGroup.hosts || [])
      .map((host: Record<string, any>) => host.host_name)
      .filter(Boolean);
    const hosts = Array.from(new Set([
      ...existingHosts,
      ...(hostsByService[serviceName] || []),
    ]));
    const {
      isSelected: _isSelected,
      ...groupData
    } = selectedGroup;
    return [{
      groupId: String(selectedGroup.id),
      serviceName,
      payload: [{
        ConfigGroup: {
          ...groupData,
          hosts: hosts.map((host_name) => ({ host_name })),
        },
      }],
    }];
  });
}

export function buildAddHostConfigGroups(
  serviceNames: string[],
  response: Record<string, any>,
  clusterName: string,
  restored: Array<Record<string, any>> = [],
) {
  return serviceNames.map((serviceName) => {
    const restoredService = restored.find((item) => item.serviceName === serviceName);
    const restoredSelection = restoredService?.configGroups?.find(
      (group: Record<string, any>) => group.isSelected,
    )?.group_name;
    const groups = (response.items || [])
      .filter((item: Record<string, any>) => item.ConfigGroup?.tag === serviceName)
      .map((item: Record<string, any>) => ({
        ...item.ConfigGroup,
        isSelected: item.ConfigGroup.group_name === restoredSelection,
      }));
    groups.unshift({
      cluster_name: clusterName,
      description: "",
      desired_configs: [],
      group_name: "Default",
      hosts: [],
      tag: serviceName,
      isSelected: !restoredSelection || restoredSelection === "Default",
    });
    if (restoredSelection && !groups.some((group: Record<string, any>) => group.isSelected)) {
      groups[0].isSelected = true;
    }
    return { serviceName, configGroups: groups };
  });
}
