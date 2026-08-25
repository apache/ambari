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

import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { ConfigPropertiesType } from "./types";
import { normalizeThemeResponse } from "./themeEngine";
import {
  collectSensitiveConfigValues,
  connectionSourceHosts,
  databaseConnectionParameters,
  findRequiredConfigProperty,
  sanitizeConnectionDiagnostics,
  resolveRequiredPropertyValues,
} from "./testConnectionUtils";

const repoRoot = resolve(
  dirname(fileURLToPath(import.meta.url)),
  "../../../../..",
);

const loadTheme = (relativePath: string) =>
  JSON.parse(readFileSync(resolve(repoRoot, relativePath), "utf8"));

const responseFor = (serviceName: string, theme: unknown) => ({
  items: [
    {
      StackServices: { service_name: serviceName },
      themes: [
        {
          ThemeInfo: {
            service_name: serviceName,
            theme_data: { Theme: theme },
          },
        },
      ],
    },
  ],
});

const configProperty = (propertyName: string, value: unknown) => ({
  propertyName,
  propertyDisplayname: propertyName,
  propertyValue: value,
  propertyAttributes: {},
  previousValue: value,
  value,
  final: "false",
  isEditable: true,
});

const serviceConfigs = (
  serviceName: string,
  entries: Record<string, Record<string, unknown>>,
): ConfigPropertiesType => ({
  [serviceName]: Object.fromEntries(
    Object.entries(entries).map(([configType, properties]) => [
      configType,
      {
        errors: 0,
        properties: Object.fromEntries(
          Object.entries(properties).map(([name, value]) => [
            name,
            configProperty(name, value),
          ]),
        ),
      },
    ]),
  ),
});

describe("Theme Test Connection contract", () => {
  it("builds the HIVE request from the real Theme semantic mapping", () => {
    const hive = loadTheme(
      "ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services/HIVE/themes/theme.json",
    );
    const theme = normalizeThemeResponse(
      responseFor("HIVE", hive),
      "default",
      ["HIVE"],
    ).byService.HIVE;
    const required =
      theme.widgetsByConfigPath["hive-env/test_db_connection"]
        .requiredProperties;
    const configs = serviceConfigs("HIVE", {
      "hive-site": {
        "javax.jdo.option.ConnectionDriverName": "org.postgresql.Driver",
        "javax.jdo.option.ConnectionURL": "jdbc:postgresql://db/hive",
        hive_server_hosts: ["hive01"],
        "javax.jdo.option.ConnectionUserName": "hive_user",
        "javax.jdo.option.ConnectionPassword": "hive_password",
      },
      "hive-env": { hive_database: "New PostgreSQL Database" },
    });

    const resolved = resolveRequiredPropertyValues(
      configs,
      "HIVE",
      required,
    );
    expect(resolved.valid).toBe(true);
    expect(connectionSourceHosts(resolved.values)).toBe("hive01");
    expect(
      databaseConnectionParameters(
        resolved.values,
        {
          "java.home": "/java",
          jdk_location: "/jdk",
          "jdk.name": "jdk.tar.gz",
        },
        "ambari01",
      ),
    ).toMatchObject({
      user_name: "hive_user",
      user_passwd: "hive_password",
      db_connection_url: "jdbc:postgresql://db/hive",
      db_name: "postgres",
      ambari_server_host: "ambari01",
      java_home: "/java",
      jdk_location: "/jdk",
      jdk_name: "jdk.tar.gz",
    });
  });

  it("serializes multiple source hosts using the custom-action contract", () => {
    expect(
      connectionSourceHosts({
        "db.connection.source.host": ["hive01", " hive02 ", ""],
      }),
    ).toBe("hive01,hive02");
    expect(
      connectionSourceHosts({
        "db.connection.source.host": "ranger01, ranger02",
      }),
    ).toBe("ranger01,ranger02");
  });

  it("keeps the real RANGER normal and root widgets on distinct credentials and URLs", () => {
    const ranger = loadTheme(
      "ambari-server/src/main/resources/stacks/BIGTOP/3.3.0/services/RANGER/themes/database.json",
    );
    const theme = normalizeThemeResponse(
      responseFor("RANGER", ranger),
      "database",
      ["RANGER"],
    ).byService.RANGER;
    const configs = serviceConfigs("RANGER", {
      "ranger-admin-site": {
        "ranger.jpa.jdbc.driver": "driver",
        "ranger.jpa.jdbc.url": "jdbc:postgresql://db/ranger",
      },
      "ranger-site": { ranger_admin_hosts: ["ranger01"] },
      "admin-properties": {
        DB_FLAVOR: "POSTGRES",
        db_host: "db",
        db_user: "ranger_user",
        db_password: "ranger_password",
        db_root_user: "postgres",
        db_root_password: "root_password",
      },
      "ranger-env": {
        ranger_privelege_user_jdbc_url: "jdbc:postgresql://db/postgres",
      },
    });

    const normal = resolveRequiredPropertyValues(
      configs,
      "RANGER",
      theme.widgetsByConfigPath["ranger-env/test_db_connection"]
        .requiredProperties,
    );
    const root = resolveRequiredPropertyValues(
      configs,
      "RANGER",
      theme.widgetsByConfigPath["ranger-env/test_root_db_connection"]
        .requiredProperties,
    );

    expect(databaseConnectionParameters(normal.values, {}, "ambari01")).toMatchObject(
      {
        user_name: "ranger_user",
        user_passwd: "ranger_password",
        db_connection_url: "jdbc:postgresql://db/ranger",
      },
    );
    expect(databaseConnectionParameters(root.values, {}, "ambari01")).toMatchObject(
      {
        user_name: "postgres",
        user_passwd: "root_password",
        db_connection_url: "jdbc:postgresql://db/postgres",
      },
    );
  });

  it("requires the exact config type even when another type has the same property name", () => {
    const configs = serviceConfigs("SVC", {
      "type-a": { shared: "a" },
      "type-b": { shared: "b" },
    });
    expect(findRequiredConfigProperty(configs, "SVC", "type-b/shared")?.value).toBe(
      "b",
    );
    expect(
      resolveRequiredPropertyValues(configs, "SVC", {
        "db.connection.user": "missing/shared",
      }).valid,
    ).toBe(false);
  });

  it("redacts configured and key-labelled secrets from backend diagnostics", () => {
    const configs = serviceConfigs("HIVE", {
      "hive-site": {
        "javax.jdo.option.ConnectionPassword": "local-secret",
      },
    });
    configs.HIVE["hive-site"].properties[
      "javax.jdo.option.ConnectionPassword"
    ].propertyAttributes.type = "password";
    const sensitiveValues = collectSensitiveConfigValues(
      configs,
      "HIVE",
      {
        "db.connection.password":
          "hive-site/javax.jdo.option.ConnectionPassword",
      },
    );

    expect(
      sanitizeConnectionDiagnostics(
        {
          error_log: "password=server-secret",
          stderr: "Login failed for local-secret",
          output_log: "credential: unknown-secret",
          stdout: 'user_passwd="local-secret"',
        },
        sensitiveValues,
      ),
    ).toEqual({
      error_log: "password=[REDACTED]",
      stderr: "Login failed for [REDACTED]",
      output_log: "credential: [REDACTED]",
      stdout: "user_passwd=[REDACTED]",
    });
  });
});
