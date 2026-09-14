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

import { cloneDeep, isEqual } from "lodash";
import type { ManagedDependencyType } from "../../api/serviceDependenciesApi";
import type { ConfigPropertiesType } from "../CommonConfigs/types";
import {
  dependencyTypes,
  type ManagedDependencyChoice,
  type ManagedDependencySelections,
} from "./managedDependencySelection";

export type ManagedDependencyClientConfig = Record<string, Record<string, string>>;

export type ReviewedManagedDependency = {
  choice: ManagedDependencyChoice;
  dependencyType: ManagedDependencyType;
};

export function reviewedManagedDependencies(
  selections: ManagedDependencySelections,
): ReviewedManagedDependency[] {
  return dependencyTypes.flatMap((dependencyType) => {
    const choice = selections[dependencyType];
    return choice?.mode === "managed" ? [{ choice, dependencyType }] : [];
  });
}

export function buildManagedDependencyClientConfig(
  selections: ManagedDependencySelections,
): ManagedDependencyClientConfig {
  const result: ManagedDependencyClientConfig = {};
  const setRequiredValue = (type: string, property: string, value?: string) => {
    if (!value) return;
    result[type] ||= {};
    const existing = result[type][property];
    if (existing != null && existing !== value) {
      throw new Error(`Managed dependency previews disagree on ${type}/${property}.`);
    }
    result[type][property] = value;
  };

  reviewedManagedDependencies(selections).forEach(({ choice }) => {
    const preview = choice.preview;
    if (!preview?.compatible) return;
    // Provider client profiles such as core-site and hdfs-site are installed by
    // the managed dependency task. Publishing them as cluster-wide desired
    // configs could overwrite a local HDFS used by other consumer services.
    Object.entries(preview.client_config?.["hbase-site"] || {}).forEach(
      ([property, value]) => setRequiredValue("hbase-site", property, value),
    );
    setRequiredValue("hbase-site", "hbase.rootdir", preview.namespace?.root_uri);
    setRequiredValue("hbase-site", "hbase.wal.dir", preview.namespace?.wal_uri);
    setRequiredValue("hbase-site", "zookeeper.znode.parent", preview.namespace?.znode);
    setRequiredValue("hbase-env", "hbase_user", preview.consumer.planned_hbase_user);
  });

  return result;
}

const normalizedConfigType = (value?: string) =>
  value?.endsWith(".xml") ? value.slice(0, -4) : value;

type ManagedDependencyBaseField = {
  present: boolean;
  value?: unknown;
};

type ManagedDependencyBase = Record<string, ManagedDependencyBaseField>;

const managedFields = ["value", "recommendedValue", "isEditable"] as const;

const restoreManagedDependencyProperty = (property: Record<string, any>) => {
  const base = property._managedDependencyBase as ManagedDependencyBase | undefined;
  if (!base) return;
  managedFields.forEach((field) => {
    if (base[field]?.present) {
      property[field] = cloneDeep(base[field].value);
    } else {
      delete property[field];
    }
  });
  delete property._managedDependencyBase;
  delete property.isManagedDependency;
};

const captureManagedDependencyBase = (property: Record<string, any>) =>
  Object.fromEntries(managedFields.map((field) => [field, {
    present: Object.prototype.hasOwnProperty.call(property, field),
    value: cloneDeep(property[field]),
  }])) as ManagedDependencyBase;

export function mergeManagedDependencyConfigProperties(
  configProperties: ConfigPropertiesType,
  requiredConfig: ManagedDependencyClientConfig,
): ConfigPropertiesType {
  const result = cloneDeep(configProperties);

  Object.values(result).forEach((sections) => {
    Object.entries(sections).forEach(([sectionName, section]) => {
      Object.values(section.properties || {}).forEach((property) => {
        const type = normalizedConfigType(
          property.type || property.fileName || sectionName,
        );
        const requiredValue = type && property.propertyName
          ? requiredConfig[type]?.[property.propertyName]
          : undefined;
        if (requiredValue === undefined) {
          restoreManagedDependencyProperty(property);
          return;
        }
        property._managedDependencyBase ||= captureManagedDependencyBase(property);
        property.value = requiredValue;
        property.recommendedValue = requiredValue;
        property.isEditable = false;
        (property as any).isManagedDependency = true;
      });
    });
  });

  return isEqual(result, configProperties) ? configProperties : result;
}
