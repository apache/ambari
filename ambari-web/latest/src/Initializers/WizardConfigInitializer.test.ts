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

import { describe, expect, it } from "vitest";
import WizardConfigInitializer from "./WizardConfigInitializer";

function initializeHiveDatabase(
  dependencies: Record<string, boolean>,
  value = "New MySQL Database",
) {
  const property = {
    propertyName: "hive_database",
    value,
    options: [
      { displayName: "New MySQL Database" },
      { displayName: "Existing MySQL Database" },
    ],
  };
  return WizardConfigInitializer(property, {}, dependencies)
    .initialValue(property, {}, dependencies);
}

describe("Hive database wizard initialization", () => {
  it("hides managed MySQL and changes its default on an unsupported OS", () => {
    const result = initializeHiveDatabase({
      alwaysEnableManagedMySQLForHive: false,
      isManagedMySQLForHiveEnabled: false,
      isServiceConfigRoute: false,
    });
    expect(result.value).toBe("Existing MySQL Database");
    expect(result.options[0].hidden).toBe(true);
  });

  it("keeps managed MySQL when the support override is enabled", () => {
    const result = initializeHiveDatabase({
      alwaysEnableManagedMySQLForHive: true,
      isManagedMySQLForHiveEnabled: false,
      isServiceConfigRoute: false,
    });
    expect(result.value).toBe("New MySQL Database");
    expect(result.options[0].hidden).toBe(false);
  });

  it("keeps managed MySQL for a supported OS or the service config page", () => {
    expect(initializeHiveDatabase({
      alwaysEnableManagedMySQLForHive: false,
      isManagedMySQLForHiveEnabled: true,
      isServiceConfigRoute: false,
    }).options[0].hidden).toBe(false);
    expect(initializeHiveDatabase({
      alwaysEnableManagedMySQLForHive: false,
      isManagedMySQLForHiveEnabled: false,
      isServiceConfigRoute: true,
    }).options[0].hidden).toBe(false);
  });
});
