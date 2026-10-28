/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { cloneDeep } from "lodash";
import type { ConfigPropertiesType } from "../screens/CommonConfigs/types";

export function serializeConfigContent(format: string, properties: Record<string, string>): string | null {
  const entries = Object.entries(properties);
  if (entries.length === 0) return null;
  if (!entries.every(([key, value]) => /^[A-Za-z_][A-Za-z0-9_.\[\]-]*$/.test(key) && typeof value === "string")) return null;
  const escape = (value: string) => value.replace(/\\/g, "\\\\").replace(/\n/g, "\\n")
    .replace(/\r/g, "\\r").replace(/\t/g, "\\t").replace(/ /g, "\\ ")
    .replace(/[^\x20-\x7e]/g, char => "\\u" + char.charCodeAt(0).toString(16).padStart(4, "0"));
  if (format === "properties") return entries.map(([key, value]) => key + "=" + escape(value)).join("\n") + "\n";
  if (format === "yaml") return entries.map(([key, value]) => JSON.stringify(key) + ": " + JSON.stringify(value)).join("\n") + "\n";
  if (format === "environment") return entries.map(([key, value]) => key + "='" + value.replace(/'/g, "'\"'\"'") + "'").join("\n") + "\n";
  if (format === "ini") {
    const groups: Record<string, Array<[string, string]>> = Object.create(null);
    for (const [key, value] of entries) {
      const dot = key.indexOf(".");
      if (dot < 1 || /[\r\n]/.test(value)) return null;
      (groups[key.slice(0, dot)] ??= []).push([key.slice(dot + 1), value]);
    }
    return Object.entries(groups).map(([name, values]) => "[" + name + "]\n" +
      values.map(([key, value]) => key + " = " + value).join("\n")).join("\n\n") + "\n";
  }
  return null;
}

/** Convert current scalar values only when metadata explicitly declares a file format. */
export function migrateConfigContent(configs: ConfigPropertiesType, versions: any, isCurrent: boolean, selectedService?: string): ConfigPropertiesType {
  if (!isCurrent) return configs;
  const result = cloneDeep(configs);
  for (const [service, sections] of Object.entries(result)) {
    if (selectedService && service !== selectedService) continue;
    const current = (versions?.items || []).filter((item: any) => item.service_name === service && item.group_name === "Default");
    if (current.length !== 1 || !Array.isArray(current[0].configurations)) continue;
    for (const [kind, section] of Object.entries(sections)) {
      const content = section.properties.content;
      const format = content?.propertyAttributes?.["property-file-type"];
      if (!format || content.foundInPropertyValues !== false || Object.keys(section.properties).length !== 1) continue;
      const observed = current[0].configurations.filter((item: any) => item.type === kind);
      if (observed.length !== 1 || !observed[0].properties || "content" in observed[0].properties) continue;
      // Scalar host overrides must be migrated together with their default file.
      // Keep the compatible scalar mode until that explicit group migration occurs.
      if (Object.values(sections).some(other => Object.values(other.properties).some(
        property => property.type === kind && (property.overrideValues?.length || 0) > 0,
      ))) continue;
      const attributes = observed[0].properties_attributes || {};
      if (Object.values(attributes.password || {}).some(value => value === "true" || value === true)) continue;
      const document = serializeConfigContent(format, observed[0].properties);
      if (document === null) continue;
      content.value = "# Migrated from the previous Ambari configuration version.\n" + document;
      content.previousValue = null;
      content.isVisible = true;
      content.isHidden = false;
      content.contentMigrationPending = true;
      for (const other of Object.values(sections)) {
        for (const property of Object.values(other.properties)) {
          if (property !== content && property.type === kind) {
            property.value = null;
            property.isVisible = false;
            property.isHidden = true;
          }
        }
      }
    }
  }
  return result;
}
