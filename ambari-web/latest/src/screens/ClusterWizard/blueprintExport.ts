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

import { flatten } from "lodash";
import { formatValuesBeforeSave, minToInstall } from "./utils";

type BlueprintExportInput = {
  clusterName: string;
  configProperties: UnknownRecord;
  hosts: unknown[];
  masterAssignments: unknown[];
  selectedServiceNames: string[];
  serviceComponents: unknown[];
  slaveAssignments: unknown[];
  stackName: string;
  stackVersion: string;
};

type UnknownRecord = Record<string, unknown>;

function asRecord(value: unknown): UnknownRecord {
  return value !== null && typeof value === "object"
    ? value as UnknownRecord
    : {};
}

function stringValue(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function hostName(host: unknown): string {
  return typeof host === "string"
    ? host
    : stringValue(asRecord(host).name)
      || stringValue(asRecord(host).hostName)
      || stringValue(asRecord(host).host_name);
}

function configType(property: UnknownRecord, sectionName: string) {
  const fileName = stringValue(property.type)
    || stringValue(property.fileName)
    || stringValue(property.filename)
    || sectionName;
  return fileName.endsWith(".xml") ? fileName.slice(0, -4) : fileName;
}

export function buildBlueprintConfigurations(configProperties: UnknownRecord) {
  const configurations = new Map<string, {
    properties: Record<string, unknown>;
    properties_attributes?: { isFinal: Record<string, string> };
  }>();

  Object.values(configProperties || {}).forEach((service) => {
    Object.entries(asRecord(service)).forEach(([sectionName, section]) => {
      Object.values(asRecord(asRecord(section).properties)).forEach((propertyValue) => {
        const property = asRecord(propertyValue);
        const name = stringValue(property.propertyName) || stringValue(property.name);
        const type = configType(property, sectionName);
        if (
          !name ||
          !type ||
          type === "hosts" ||
          property.value == null ||
          property.isRequiredByAgent === false
        ) return;
        const current = configurations.get(type) || { properties: {} };
        try {
          current.properties[name] = formatValuesBeforeSave(property);
        } catch {
          current.properties[name] = property.value;
        }
        if (String(property.final) === "true") {
          current.properties_attributes ||= { isFinal: {} };
          current.properties_attributes.isFinal[name] = "true";
        }
        configurations.set(type, current);
      });
    });
  });

  return [...configurations].map(([type, details]) => ({ [type]: details }));
}

export function buildBlueprintExport({
  clusterName,
  configProperties,
  hosts,
  masterAssignments,
  selectedServiceNames,
  serviceComponents,
  slaveAssignments,
  stackName,
  stackVersion,
}: BlueprintExportInput) {
  const hostNames = [...new Set(hosts.map(hostName).filter(Boolean))];
  const componentsByHost = new Map(
    hostNames.map((name) => [name, new Set<string>()]),
  );
  const componentMetadata = flatten(
    serviceComponents.map((service) => {
      const components = asRecord(service).components;
      return Array.isArray(components) ? components : [];
    }),
  ).map((component) => {
    const componentRecord = asRecord(component);
    return asRecord(componentRecord.StackServiceComponents || componentRecord);
  });
  const selectedComponents = componentMetadata.filter((component) =>
    selectedServiceNames.includes(stringValue(component.service_name)),
  );
  const clientComponents = selectedComponents
    .filter((component) => component.is_client === true)
    .map((component) => stringValue(component.component_name))
    .filter(Boolean);

  masterAssignments.forEach((hostAssignmentValue) => {
    const hostAssignment = asRecord(hostAssignmentValue);
    const fallbackHost = stringValue(hostAssignment.host_name)
      || stringValue(hostAssignment.hostName);
    const masterServices = Array.isArray(hostAssignment.masterServices)
      ? hostAssignment.masterServices
      : [];
    masterServices.forEach((componentValue) => {
      const component = asRecord(componentValue);
      const name = stringValue(component.hostName)
        || stringValue(component.host_name)
        || fallbackHost;
      const componentName = stringValue(component.component);
      if (componentsByHost.has(name) && componentName) {
        componentsByHost.get(name)?.add(componentName);
      }
    });
  });

  slaveAssignments.forEach((hostAssignmentValue) => {
    const hostAssignment = asRecord(hostAssignmentValue);
    const name = stringValue(hostAssignment.hostname)
      || stringValue(hostAssignment.hostName);
    if (!componentsByHost.has(name)) return;
    const checkboxes = Array.isArray(hostAssignment.checkboxes)
      ? hostAssignment.checkboxes
      : [];
    checkboxes
      .map(asRecord)
      .filter((component) => component.checked === true)
      .forEach((component) => {
        const label = stringValue(component.label);
        if (label === "CLIENT") {
          clientComponents.forEach((client) => componentsByHost.get(name)?.add(client));
        } else if (label) {
          componentsByHost.get(name)?.add(label);
        }
      });
  });

  selectedComponents
    .filter((component) => minToInstall(stringValue(component.cardinality)) === Infinity)
    .forEach((component) => {
      const componentName = stringValue(component.component_name);
      if (!componentName) return;
      hostNames.forEach((name) => componentsByHost.get(name)?.add(componentName));
    });

  const groupedHosts = new Map<string, string[]>();
  componentsByHost.forEach((components, name) => {
    const key = [...components].sort().join("\u0000");
    groupedHosts.set(key, [...(groupedHosts.get(key) || []), name]);
  });

  const hostGroups = [...groupedHosts].map(([componentKey, grouped], index) => ({
    name: `host_group_${index}`,
    cardinality: String(grouped.length),
    components: componentKey
      ? componentKey.split("\u0000").map((name) => ({ name }))
      : [],
  }));
  const clusterTemplateHostGroups = [...groupedHosts].map(([, grouped], index) => ({
    name: `host_group_${index}`,
    hosts: grouped.map((fqdn) => ({ fqdn })),
  }));

  return {
    blueprint: {
      configurations: buildBlueprintConfigurations(configProperties),
      host_groups: hostGroups,
      Blueprints: {
        blueprint_name: clusterName,
        stack_name: stackName,
        stack_version: stackVersion,
      },
    },
    clusterTemplate: {
      blueprint: clusterName,
      config_recommendation_strategy: "NEVER_APPLY",
      provision_action: "INSTALL_AND_START",
      configurations: [],
      host_groups: clusterTemplateHostGroups,
      Clusters: { cluster_name: clusterName },
    },
  };
}
