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

import { configValidator } from "./validators";

export type ExistingCustomProperty = {
  isVisible?: boolean;
  value?: unknown;
};

export type ParsedCustomProperty = {
  key: string;
  value: string;
};

export function validateCustomPropertyKey(
  key: string,
  existingProperties: Record<string, ExistingCustomProperty> = {}
): string {
  const normalizedKey = key.trim();
  if (!normalizedKey) {
    return "Property key is required";
  }
  if (!configValidator.isValidConfigKey(normalizedKey)) {
    return "Invalid key. Only alphanumerics, hyphens, underscores, asterisks and periods are allowed.";
  }

  const existingProperty = existingProperties[normalizedKey];
  if (
    existingProperty &&
    existingProperty.isVisible !== false &&
    existingProperty.value !== null
  ) {
    return `Property "${normalizedKey}" already exists`;
  }
  return "";
}

export function parseCustomPropertyInput(
  input: string,
  existingProperties: Record<string, ExistingCustomProperty> = {}
): { properties: ParsedCustomProperty[]; errors: string[] } {
  const properties: ParsedCustomProperty[] = [];
  const errors: string[] = [];
  const seenKeys = new Set<string>();

  input.split(/\r?\n/).forEach((rawLine, index) => {
    const line = rawLine.trim();
    if (!line) {
      return;
    }

    const lineNumber = index + 1;
    const equalIndex = line.indexOf("=");
    if (equalIndex < 0) {
      errors.push(`Line ${lineNumber}: Invalid format. Expected key=value`);
      return;
    }

    const key = line.substring(0, equalIndex).trim();
    const value = line.substring(equalIndex + 1).trim();
    if (!key) {
      errors.push(`Line ${lineNumber}: Key cannot be empty`);
      return;
    }
    if (!configValidator.isValidConfigKey(key)) {
      errors.push(
        `Line ${lineNumber}: Invalid key "${key}". Only alphanumerics, hyphens, underscores, asterisks and periods are allowed.`
      );
      return;
    }
    if (seenKeys.has(key)) {
      errors.push(`Line ${lineNumber}: Duplicate key "${key}"`);
      return;
    }

    const existingProperty = existingProperties[key];
    if (
      existingProperty &&
      existingProperty.isVisible !== false &&
      existingProperty.value !== null
    ) {
      errors.push(`Line ${lineNumber}: Property "${key}" already exists`);
      return;
    }

    seenKeys.add(key);
    properties.push({ key, value });
  });

  return { properties, errors };
}
