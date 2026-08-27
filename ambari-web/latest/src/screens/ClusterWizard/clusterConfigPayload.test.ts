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
import { ConfigPropertiesType } from "../CommonConfigs/types";
import { buildClusterConfigurationPayload } from "./clusterConfigPayload";

const canonicalProperty = (
  name: string,
  type: string,
  value: unknown,
  overrides: Record<string, unknown> = {},
) => ({
  propertyName: name,
  propertyDisplayname: name,
  propertyValue: value,
  propertyAttributes: { type: "string" },
  previousValue: value,
  value,
  final: "false",
  type,
  serviceName: "HIVE",
  isEditable: true,
  ...overrides,
});

describe("cluster configuration payload", () => {
  it("saves each canonical config once and excludes Theme UI-only state", () => {
    const configProperties = {
      HIVE: {
        General: {
          errors: 0,
          properties: {
            password: canonicalProperty("db.password", "hive-site", "secret", {
              confirmPassword: "secret",
            }),
            action: canonicalProperty("test_db", "hive-site", "clicked", {
              isRequiredByAgent: false,
            }),
            sameName: canonicalProperty("shared", "hive-site", "hive"),
          },
        },
        Other: {
          errors: 0,
          properties: {
            sameName: canonicalProperty("shared", "hive-env", "env"),
          },
        },
      },
      MISC: { "Users and Groups": { errors: 0, properties: {} } },
    } as ConfigPropertiesType;

    const result = buildClusterConfigurationPayload({
      configProperties,
      includeInstalledChanges: false,
      installedServices: [],
    });
    expect(result[0].Clusters.desired_config).toEqual([
      {
        type: "hive-site",
        properties: { "db.password": "secret", shared: "hive" },
        service_config_version_note: "Initial version of HIVE configurations",
      },
      {
        type: "hive-env",
        properties: { shared: "env" },
        service_config_version_note: "Initial version of HIVE configurations",
      },
    ]);
    expect(JSON.stringify(result)).not.toContain("test_db");
    expect(JSON.stringify(result)).not.toContain("confirmPassword");
  });

  it("saves only intentional dependent changes for installed services", () => {
    const configProperties = {
      HDFS: {
        General: {
          errors: 0,
          properties: {
            unchanged: canonicalProperty("unchanged", "core-site", "same", {
              initialValue: "same",
              serviceName: "HDFS",
            }),
            changed: canonicalProperty("changed", "core-site", "new", {
              initialValue: "old",
              serviceName: "HDFS",
            }),
          },
        },
      },
      MISC: { "Users and Groups": { errors: 0, properties: {} } },
    } as ConfigPropertiesType;

    const result = buildClusterConfigurationPayload({
      configProperties,
      includeInstalledChanges: true,
      installedServices: ["HDFS"],
    });
    expect(result[0].Clusters.desired_config[0].properties).toEqual({
      changed: "new",
    });
  });
});
