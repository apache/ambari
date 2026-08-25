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

import { wizardConfigs } from "./wizardConstants";

export interface RangerAdminAssignment {
  component?: string;
  component_name?: string;
  hostName?: string;
  selectedHost?: string;
  isInstalled?: boolean;
  [key: string]: unknown;
}

export interface RangerAdminHosts {
  currentHosts: string[];
  additionalHosts: string[];
}

export interface RangerAdminPreviewProperty {
  id: string;
  name: string;
  displayName: string;
  description?: string;
  fileName: string;
  serviceName: string;
  serviceDisplayName: string;
  value: string;
  isEditable: false;
}

export interface RangerAdminPreviewCategory {
  name: string;
  displayName: string;
}

type UnknownRecord = Record<string, unknown>;

const isRecord = (value: unknown): value is UnknownRecord =>
  Boolean(value) && typeof value === "object" && !Array.isArray(value);

const assignmentHost = (assignment: RangerAdminAssignment) =>
  assignment.hostName || assignment.selectedHost || "";

const isRangerAdmin = (assignment: RangerAdminAssignment) =>
  (assignment.component || assignment.component_name) === "RANGER_ADMIN";

export function getRangerAdminHosts(
  assignments: RangerAdminAssignment[] = [],
): RangerAdminHosts {
  const rangerAdmins = assignments.filter(isRangerAdmin);
  return {
    currentHosts: rangerAdmins
      .filter((assignment) => assignment.isInstalled)
      .map(assignmentHost)
      .filter(Boolean),
    additionalHosts: rangerAdmins
      .filter((assignment) => !assignment.isInstalled)
      .map(assignmentHost)
      .filter(Boolean),
  };
}

export function validateRangerAdminAssignments(
  assignments: RangerAdminAssignment[] = [],
): string[] {
  const rangerAdmins = assignments.filter(isRangerAdmin);
  const current = rangerAdmins.filter((assignment) => assignment.isInstalled);
  const additional = rangerAdmins.filter((assignment) => !assignment.isInstalled);
  const errors: string[] = [];

  if (current.length !== 1) {
    errors.push("Exactly one current Ranger Admin is required.");
  }
  if (!additional.length) {
    errors.push("At least one additional Ranger Admin is required.");
  }
  if (rangerAdmins.some((assignment) => !assignmentHost(assignment))) {
    errors.push("Every Ranger Admin must be assigned to a host.");
  }

  const hosts = rangerAdmins.map(assignmentHost).filter(Boolean);
  if (new Set(hosts).size !== hosts.length) {
    errors.push("Ranger Admin instances must be assigned to different hosts.");
  }
  return errors;
}

function getStackProperty(
  stackServices: UnknownRecord[],
  serviceName: string,
  siteName: string,
  propertyName: string,
) {
  const service = stackServices.find(
    (item) =>
      isRecord(item.StackServices) &&
      item.StackServices.service_name === serviceName,
  );
  const configurations = Array.isArray(service?.configurations)
    ? service.configurations
    : [];
  const property = configurations.find((configuration) => {
    const stackProperty = isRecord(configuration)
      ? configuration.StackConfigurations
      : undefined;
    return (
      isRecord(stackProperty) &&
      stackProperty.property_name === propertyName &&
      String(stackProperty.type || "").split(".")[0] === siteName
    );
  });
  return {
    service: isRecord(service?.StackServices) ? service.StackServices : {},
    property:
      isRecord(property) && isRecord(property.StackConfigurations)
        ? property.StackConfigurations
        : {},
  };
}

export function buildRangerAdminPreview(
  stackServices: UnknownRecord[] = [],
  installedServices: string[] = [],
  loadBalancerUrl: string,
) {
  const categories: RangerAdminPreviewCategory[] = [];
  const properties: RangerAdminPreviewProperty[] = [];

  wizardConfigs.forEach((candidate) => {
    if (!installedServices.includes(candidate.serviceName)) return;
    const stackData = getStackProperty(
      stackServices,
      candidate.serviceName,
      candidate.siteName,
      candidate.propertyName,
    );
    const displayName =
      typeof stackData.service.display_name === "string"
        ? stackData.service.display_name
        : candidate.serviceDisplayName;
    if (!categories.some((category) => category.name === candidate.serviceName)) {
      categories.push({ name: candidate.serviceName, displayName });
    }
    properties.push({
      id: `${candidate.propertyName}__${candidate.siteName}`,
      name: candidate.propertyName,
      displayName:
        typeof stackData.property.property_display_name === "string"
          ? stackData.property.property_display_name
          : candidate.propertyName,
      description:
        typeof stackData.property.property_description === "string"
          ? stackData.property.property_description
          : undefined,
      fileName:
        typeof stackData.property.type === "string"
          ? stackData.property.type
          : `${candidate.siteName}.xml`,
      serviceName: candidate.serviceName,
      serviceDisplayName: displayName,
      value: loadBalancerUrl,
      isEditable: false,
    });
  });

  return { categories, properties };
}

export function buildRangerAdminConfigQuery(
  desiredConfigs: Record<string, { tag?: string }> = {},
) {
  return wizardConfigs
    .flatMap((candidate) => {
      const tag = desiredConfigs[candidate.siteName]?.tag;
      return tag ? [`(type=${candidate.siteName}&tag=${tag})`] : [];
    })
    .join("|");
}

export function buildRangerAdminConfigBodies(
  configItems: UnknownRecord[] = [],
  loadBalancerUrl: string,
  note: string,
) {
  const itemsByType = new Map(
    configItems.map((item) => [String(item.type || ""), item]),
  );

  return wizardConfigs.flatMap((candidate) => {
    const current = itemsByType.get(candidate.siteName);
    if (!current) return [];
    const currentProperties = isRecord(current.properties)
      ? current.properties
      : {};
    const desiredConfig: UnknownRecord = {
      type: candidate.siteName,
      properties: {
        ...currentProperties,
        [candidate.propertyName]: loadBalancerUrl,
      },
      service_config_version_note: note,
    };
    if (current.properties_attributes) {
      desiredConfig.properties_attributes = current.properties_attributes;
    }
    return [{ Clusters: { desired_config: [desiredConfig] } }];
  });
}
