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
import { ConfigPropertiesType } from "./types";
import {
  masterHostsByComponent,
  resolveRequiredPropertyValues,
} from "./testConnectionUtils";

// required-properties of the ODP 3.3 RANGER database Theme test-db-connection widget.
const rangerRequiredProperties = {
  "jdbc.driver.class": "ranger-admin-site/ranger.jpa.jdbc.driver",
  "jdbc.driver.url": "ranger-admin-site/ranger.jpa.jdbc.url",
  "db.connection.source.host": "ranger-site/ranger_admin_hosts",
  "db.type": "admin-properties/DB_FLAVOR",
  "db.connection.destination.host": "admin-properties/db_host",
  "db.connection.user": "admin-properties/db_user",
  "db.connection.password": "admin-properties/db_password",
};

const property = (name: string, type: string, value: string) => ({
  propertyName: name,
  propertyDisplayname: name,
  propertyValue: value,
  propertyAttributes: {},
  previousValue: value,
  value,
  final: "false",
  isEditable: true,
  fileName: `${type}.xml`,
  type,
  serviceName: "RANGER",
});

const rangerConfigs = (dbPassword = "test-only-value"): ConfigPropertiesType => ({
  RANGER: {
    "ranger-admin-site": {
      errors: 0,
      properties: {
        "ranger.jpa.jdbc.driver": property("ranger.jpa.jdbc.driver", "ranger-admin-site", "com.mysql.jdbc.Driver"),
        "ranger.jpa.jdbc.url": property("ranger.jpa.jdbc.url", "ranger-admin-site", "jdbc:mysql://db.example:3306/ranger"),
      },
    },
    "admin-properties": {
      errors: 0,
      properties: {
        DB_FLAVOR: property("DB_FLAVOR", "admin-properties", "MYSQL"),
        db_host: property("db_host", "admin-properties", "db.example"),
        db_user: property("db_user", "admin-properties", "rangeradmin"),
        db_password: property("db_password", "admin-properties", dbPassword),
      },
    },
  },
});

// MASTERS step mastersData as stored by the Add Service wizard.
const mastersData = [
  {
    host_name: "host-1.example",
    masterServices: [
      { component: "NAMENODE", hostName: "host-1.example", isInstalled: true },
      { component_name: "RANGER_ADMIN", selectedHost: "host-2.example" },
    ],
  },
  {
    host_name: "host-2.example",
    masterServices: [{ component: "RANGER_ADMIN", hostName: "host-2.example" }],
  },
];

describe("Test Connection component host fallback", () => {
  it("maps wizard master assignments to hosts per component without duplicates", () => {
    expect(masterHostsByComponent(mastersData)).toEqual({
      NAMENODE: ["host-1.example"],
      RANGER_ADMIN: ["host-2.example"],
    });
    expect(masterHostsByComponent(undefined)).toEqual({});
  });

  it("resolves ranger-site/ranger_admin_hosts from the assigned Ranger Admin hosts", () => {
    const result = resolveRequiredPropertyValues(
      rangerConfigs(),
      "RANGER",
      rangerRequiredProperties,
      undefined,
      masterHostsByComponent(mastersData),
    );

    expect(result.valid).toBe(true);
    expect(result.values["db.connection.source.host"]).toEqual(["host-2.example"]);
  });

  it("stays invalid when Ranger Admin has no assigned host", () => {
    const result = resolveRequiredPropertyValues(
      rangerConfigs(),
      "RANGER",
      rangerRequiredProperties,
      undefined,
      masterHostsByComponent([]),
    );

    expect(result.valid).toBe(false);
    expect(result.values["db.connection.source.host"]).toBeUndefined();
  });

  it("does not let assigned hosts hide a missing required config value", () => {
    const configs = rangerConfigs();
    delete configs.RANGER["admin-properties"].properties.db_password;

    const result = resolveRequiredPropertyValues(
      configs,
      "RANGER",
      rangerRequiredProperties,
      undefined,
      masterHostsByComponent(mastersData),
    );

    expect(result.valid).toBe(false);
  });

  it("prefers a real config property over assigned hosts", () => {
    const configs = rangerConfigs();
    configs.RANGER["ranger-site"] = {
      errors: 0,
      properties: {
        ranger_admin_hosts: property("ranger_admin_hosts", "ranger-site", "configured.example"),
      },
    };

    const result = resolveRequiredPropertyValues(
      configs,
      "RANGER",
      rangerRequiredProperties,
      undefined,
      masterHostsByComponent(mastersData),
    );

    expect(result.values["db.connection.source.host"]).toBe("configured.example");
  });
});
