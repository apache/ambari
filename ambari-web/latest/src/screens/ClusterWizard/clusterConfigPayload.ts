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

import { isEqual } from "lodash";
import { ConfigPropertiesType } from "../CommonConfigs/types";
import { formatValuesBeforeSave } from "./utils";

type ClusterConfigPayloadInput = {
  configProperties: ConfigPropertiesType;
  includeInstalledChanges: boolean;
  installedServices: string[];
  requiredConfigurations?: Record<string, Record<string, string>>;
};

type CanonicalProperty = ConfigPropertiesType[string][string]["properties"][string];

const configType = (property: CanonicalProperty, sectionName: string) => {
  const type = property.type || property.fileName || sectionName;
  return type.endsWith(".xml") ? type.slice(0, -4) : type;
};

const changedFromLoadedValue = (property: CanonicalProperty) => {
  if (property.didUserOverrideValue === true) return true;
  if (Object.prototype.hasOwnProperty.call(property, "initialValue")) {
    return !isEqual(property.value, property.initialValue);
  }
  return !isEqual(property.value, property.previousValue);
};

const formattedValue = (property: CanonicalProperty) => {
  try {
    return formatValuesBeforeSave(property);
  } catch {
    return property.value;
  }
};

export const buildClusterConfigurationPayload = ({
  configProperties,
  includeInstalledChanges,
  installedServices,
  requiredConfigurations = {},
}: ClusterConfigPayloadInput) => {
  const payload: Array<{
    Clusters: {
      desired_config: Array<{
        type: string;
        properties: Record<string, unknown>;
        service_config_version_note: string;
      }>;
    };
  }> = [];

  Object.entries(configProperties).forEach(([serviceName, sections]) => {
    if (serviceName === "MISC") return;
    const isInstalledService = installedServices.includes(serviceName);
    if (isInstalledService && !includeInstalledChanges) return;
    const propertiesByType = new Map<string, Record<string, unknown>>();

    const addProperty = (
      property: CanonicalProperty,
      sectionName: string,
      installedChangeOnly: boolean,
    ) => {
      const type = configType(property, sectionName);
      if (
        !property.propertyName ||
        !type ||
        type === "hosts" ||
        property.value === null ||
        property.isRequiredByAgent === false ||
        (installedChangeOnly && !changedFromLoadedValue(property))
      ) {
        return;
      }
      const typeProperties = propertiesByType.get(type) || {};
      typeProperties[property.propertyName] = formattedValue(property);
      propertiesByType.set(type, typeProperties);
    };

    Object.entries(sections).forEach(([sectionName, section]) => {
      Object.values(section.properties || {}).forEach((property) =>
        addProperty(property, sectionName, isInstalledService),
      );
    });

    const accountProperties =
      configProperties.MISC?.["Users and Groups"]?.properties || {};
    Object.values(accountProperties).forEach((property) => {
      if (property.serviceName === serviceName) {
        addProperty(property, "Users and Groups", isInstalledService);
      }
    });

    const desiredConfigs = [...propertiesByType].map(([type, properties]) => ({
      type,
      properties,
      service_config_version_note: `Initial version of ${serviceName} configurations`,
    }));
    if (desiredConfigs.length) {
      payload.push({ Clusters: { desired_config: desiredConfigs } });
    }
  });

  Object.entries(requiredConfigurations).forEach(([type, requiredProperties]) => {
    let target: {
      type: string;
      properties: Record<string, unknown>;
      service_config_version_note: string;
    } | undefined;
    payload.forEach(({ Clusters }) => {
      Clusters.desired_config.forEach((desiredConfig) => {
        if (desiredConfig.type !== type) return;
        target ||= desiredConfig;
        Object.keys(requiredProperties).forEach((propertyName) => {
          delete desiredConfig.properties[propertyName];
        });
      });
    });
    if (!target) {
      target = {
        type,
        properties: {},
        service_config_version_note: "Managed HBase dependency configuration",
      };
      payload.push({ Clusters: { desired_config: [target] } });
    }
    Object.assign(target.properties, requiredProperties);
  });

  return payload
    .map(({ Clusters }) => ({
      Clusters: {
        desired_config: Clusters.desired_config.filter(
          ({ properties }) => Object.keys(properties).length > 0,
        ),
      },
    }))
    .filter(({ Clusters }) => Clusters.desired_config.length > 0);
};
