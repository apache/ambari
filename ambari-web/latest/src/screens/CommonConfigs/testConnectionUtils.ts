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

import { ConfigPropertiesType, PropertyType } from "./types";

export type RequiredPropertyValues = Record<string, unknown>;

export type ConnectionDiagnostics = {
  error_log: string;
  stderr: string;
  output_log: string;
  stdout: string;
};

const sensitiveNamePattern =
  /password|passwd|passphrase|secret|token|credential|private[_-]?key/i;

const splitConfigPath = (configPath: string) => {
  const separator = configPath.indexOf("/");
  if (separator <= 0 || separator === configPath.length - 1) return null;
  return {
    configType: configPath.slice(0, separator),
    propertyName: configPath.slice(separator + 1),
  };
};

export const findRequiredConfigProperty = (
  configProperties: ConfigPropertiesType,
  serviceName: string,
  configPath: string,
): PropertyType | undefined => {
  const path = splitConfigPath(configPath);
  if (!path) return undefined;
  const direct =
    configProperties[serviceName]?.[path.configType]?.properties?.[
      path.propertyName
    ];
  if (direct) return direct;

  for (const configType of Object.keys(configProperties[serviceName] ?? {})) {
    const property =
      configProperties[serviceName][configType]?.properties?.[
        path.propertyName
      ];
    if (property?.fileName?.replace(/\.xml$/, "") === path.configType) {
      return property;
    }
  }
  return undefined;
};

export const resolveRequiredPropertyValues = (
  configProperties: ConfigPropertiesType,
  serviceName: string,
  requiredProperties: Record<string, string>,
  sourceHostFallback?: unknown,
): { values: RequiredPropertyValues; valid: boolean } => {
  const values: RequiredPropertyValues = {};
  let valid = true;

  Object.entries(requiredProperties).forEach(([semanticName, configPath]) => {
    const property = findRequiredConfigProperty(
      configProperties,
      serviceName,
      configPath,
    );
    const fallback =
      semanticName === "db.connection.source.host"
        ? sourceHostFallback
        : undefined;
    const value = property?.value ?? fallback;
    values[semanticName] = value;
    if (
      value === undefined ||
      value === null ||
      property?.hasError ||
      Boolean(property?.errorMessage)
    ) {
      valid = false;
    }
  });

  return { values, valid };
};

export const normalizeDatabaseType = (value: unknown): string => {
  const type = String(value ?? "")
    .toUpperCase()
    .match(/DERBY|POSTGRES|ORACLE|MYSQL|MSSQL|ANYWHERE|SQLA/)?.[0];
  const aliases: Record<string, string> = {
    DERBY: "derby",
    POSTGRES: "postgres",
    ORACLE: "oracle",
    MYSQL: "mysql",
    MSSQL: "mssql",
    ANYWHERE: "sqlanywhere",
    SQLA: "sqlanywhere",
  };
  return type ? aliases[type] : String(value ?? "").toLowerCase();
};

export const databaseConnectionParameters = (
  values: RequiredPropertyValues,
  ambariProperties: Record<string, unknown>,
  ambariServerHost: string,
) => ({
  user_name: String(values["db.connection.user"] ?? ""),
  user_passwd: String(values["db.connection.password"] ?? ""),
  db_connection_url: String(values["jdbc.driver.url"] ?? ""),
  db_name: normalizeDatabaseType(values["db.type"]),
  ambari_server_host: ambariServerHost,
  java_home: String(ambariProperties["ambari.java.home"] ?? ""),
  jdk_location: String(ambariProperties.jdk_location ?? ""),
  jdk_name: String(ambariProperties["ambari.jdk.name"] ?? ""),
  check_execute_list: "db_connection_check",
  threshold: "60",
});

export const connectionSourceHosts = (
  values: RequiredPropertyValues,
): string => {
  const hosts = values["db.connection.source.host"];
  const normalizedHosts = Array.isArray(hosts)
    ? hosts.map(String)
    : String(hosts ?? "").split(",");
  return normalizedHosts.map((host) => host.trim()).filter(Boolean).join(",");
};

export const collectSensitiveConfigValues = (
  configProperties: ConfigPropertiesType,
  serviceName: string,
  requiredProperties: Record<string, string> = {},
): string[] => {
  const sensitivePaths = new Set(
    Object.entries(requiredProperties)
      .filter(([semanticName]) => sensitiveNamePattern.test(semanticName))
      .map(([, configPath]) => configPath),
  );
  const values = new Set<string>();

  Object.values(configProperties[serviceName] ?? {}).forEach((section) => {
    Object.values(section.properties ?? {}).forEach((property) => {
      const configType =
        property.fileName?.replace(/\.xml$/, "") || property.type || "";
      const configPath = `${configType}/${property.propertyName}`;
      const sensitive =
        sensitivePaths.has(configPath) ||
        sensitiveNamePattern.test(property.propertyName) ||
        property.propertyAttributes?.type === "password";
      if (!sensitive) return;

      [property.value, property.confirmPassword].forEach((value) => {
        if (value !== undefined && value !== null && String(value) !== "") {
          values.add(String(value));
        }
      });
    });
  });

  return [...values].sort((left, right) => right.length - left.length);
};

export const redactConnectionDiagnostic = (
  value: unknown,
  sensitiveValues: string[],
): string => {
  let result = String(value ?? "");
  sensitiveValues.forEach((sensitiveValue) => {
    result = result.split(sensitiveValue).join("[REDACTED]");
  });
  return result.replace(
    /((?:password|passwd|passphrase|secret|token|credential|private[_-]?key)\s*(?::|=|=>)\s*)(?:"[^"]*"|'[^']*'|[^\s,;]+)/gi,
    "$1[REDACTED]",
  );
};

export const sanitizeConnectionDiagnostics = (
  diagnostics: Record<string, unknown> | null | undefined,
  sensitiveValues: string[],
): ConnectionDiagnostics => ({
  error_log: redactConnectionDiagnostic(
    diagnostics?.error_log ?? "Connection Test Failed",
    sensitiveValues,
  ),
  stderr: redactConnectionDiagnostic(diagnostics?.stderr, sensitiveValues),
  output_log: redactConnectionDiagnostic(
    diagnostics?.output_log ?? "Connection test did not complete",
    sensitiveValues,
  ),
  stdout: redactConnectionDiagnostic(diagnostics?.stdout, sensitiveValues),
});
