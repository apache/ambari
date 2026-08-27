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

import { PropertyType } from "./types";

export type ThemeWidgetEntry = {
  value: string;
  label: string;
  description?: string;
};

export const getThemeWidgetEntries = (
  property: PropertyType,
): ThemeWidgetEntry[] => {
  const attributes = property.propertyAttributes ?? {};
  const labels = Array.isArray(attributes.entry_labels)
    ? attributes.entry_labels
    : [];
  const descriptions = Array.isArray(attributes.entry_descriptions)
    ? attributes.entry_descriptions
    : [];
  return (Array.isArray(attributes.entries) ? attributes.entries : []).map(
    (entry: unknown, index: number) => {
      if (typeof entry === "object" && entry !== null) {
        const item = entry as Record<string, unknown>;
        const value = String(item.value ?? "");
        return {
          value,
          label: String(item.label ?? labels[index] ?? value),
          description: String(item.description ?? descriptions[index] ?? ""),
        };
      }
      const value = String(entry);
      return {
        value,
        label: String(labels[index] ?? value),
        description: String(descriptions[index] ?? ""),
      };
    },
  );
};

export const areThemeEntriesEditable = (property: PropertyType): boolean => {
  const attributes = property.propertyAttributes ?? {};
  const editable =
    attributes.entriesEditable ?? attributes.entries_editable ?? true;
  return editable !== false && editable !== "false";
};

export const parseSelectionCardinality = (
  cardinality: unknown,
): { minimum: number; maximum: number } => {
  if (!cardinality) return { minimum: 1, maximum: 1 };
  const value = String(cardinality).trim().toUpperCase();
  if (value === "ALL") {
    return {
      minimum: Number.POSITIVE_INFINITY,
      maximum: Number.POSITIVE_INFINITY,
    };
  }
  if (value.includes("-")) {
    const [minimum, maximum] = value.split("-").map(Number);
    return {
      minimum: Number.isFinite(minimum) ? minimum : 1,
      maximum: Number.isFinite(maximum) ? maximum : 1,
    };
  }
  if (/^\d+\+$/.test(value)) {
    return {
      minimum: Number(value.slice(0, -1)),
      maximum: Number.POSITIVE_INFINITY,
    };
  }
  const exact = Number(value);
  return Number.isFinite(exact)
    ? { minimum: exact, maximum: exact }
    : { minimum: 1, maximum: 1 };
};

export const validateThemeListValue = (
  property: PropertyType,
  value: unknown,
): string => {
  const entries = getThemeWidgetEntries(property);
  const selected = String(value ?? "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
  const cardinality = parseSelectionCardinality(
    property.propertyAttributes?.selection_cardinality,
  );
  const minimum = Number.isFinite(cardinality.minimum)
    ? cardinality.minimum
    : entries.length;
  const maximum = Number.isFinite(cardinality.maximum)
    ? cardinality.maximum
    : entries.length;
  if (selected.length < minimum) return `Select at least ${minimum} item(s).`;
  if (selected.length > maximum) return `Select at most ${maximum} item(s).`;
  return "";
};

const checkboxValuePairs: Array<[string, string]> = [
  ["true", "false"],
  ["Yes", "No"],
  ["YES", "NO"],
  ["yes", "no"],
];

export const getThemeCheckboxState = (property: PropertyType) => {
  const value = String(property.value);
  const pair =
    checkboxValuePairs.find((candidate) => candidate.includes(value)) ??
    checkboxValuePairs[0];
  const [positive, negative] =
    property.displayType === "boolean-inverted" ? [pair[1], pair[0]] : pair;
  return {
    checked: value === positive,
    checkedValue: positive,
    uncheckedValue: negative,
  };
};

export const isThemeCheckboxValueSupported = (
  property: PropertyType,
): boolean => {
  const value = String(property.value);
  return checkboxValuePairs.some((candidate) => candidate.includes(value));
};

export const getUnsupportedThemeEntryValues = (
  property: PropertyType,
  multiple = false,
): string[] => {
  const supportedValues = new Set(
    getThemeWidgetEntries(property).map((entry) => entry.value),
  );
  const currentValues = multiple
    ? String(property.value ?? "")
        .split(",")
        .map((value) => value.trim())
        .filter(Boolean)
    : [String(property.value ?? "")].filter(Boolean);
  return currentValues.filter((value) => !supportedValues.has(value));
};
