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

export type ReassignManualCommandStep = {
  command?: string;
  description: string;
  number: number;
};

type DatabaseManualCommandOptions = {
  componentName: string;
  groupName?: string;
  sourceHost?: string;
  targetHost?: string;
};

export function getDatabaseManualCommands({
  componentName,
  groupName = "hadoop",
  sourceHost = "",
  targetHost = "",
}: DatabaseManualCommandOptions): ReassignManualCommandStep[] | null {
  if (componentName === "OOZIE_SERVER") {
    return [
      {
        number: 1,
        description: `On ${sourceHost}, copy the contents of the embedded Oozie database directory.`,
        command: "/hadoop/oozie/data/oozie-db",
      },
      {
        number: 2,
        description: `Copy the directory to the target host ${targetHost}.`,
      },
      {
        number: 3,
        description: "Create the directory on the target host if it does not exist.",
        command: "mkdir -p /hadoop/oozie/data/oozie-db",
      },
      {
        number: 4,
        description: "Update the Oozie data directory permissions.",
        command: `chown -R oozie:${groupName} /hadoop/oozie/data`,
      },
    ];
  }

  if (componentName === "MYSQL_SERVER") {
    return [
      {
        number: 1,
        description: `On ${sourceHost}, export the MySQL metastore database.`,
        command: "mysqldump db_name > backup-file.sql",
      },
      {
        number: 2,
        description: `Copy backup-file.sql to the target host ${targetHost}.`,
      },
      {
        number: 3,
        description: "Create the database on the target MySQL server.",
        command: "CREATE DATABASE db_name;",
      },
      {
        number: 4,
        description: "Import the database backup on the target MySQL server.",
        command: "mysql db_name < backup-file.sql",
      },
    ];
  }

  return null;
}

export function oozieUsesEmbeddedDatabase(driver: unknown): boolean {
  return typeof driver === "string" && /derby/i.test(driver);
}

export function getOozieJdbcDriver(response: unknown): unknown {
  if (!response || typeof response !== "object") {
    return undefined;
  }

  const items = (response as { items?: unknown }).items;
  if (!Array.isArray(items)) {
    return undefined;
  }

  for (const item of items) {
    if (!item || typeof item !== "object") {
      continue;
    }
    const configurations = (item as { configurations?: unknown }).configurations;
    if (!Array.isArray(configurations)) {
      continue;
    }
    const oozieSite = configurations.find(
      (configuration) =>
        configuration &&
        typeof configuration === "object" &&
        (configuration as { type?: unknown }).type === "oozie-site"
    ) as { properties?: Record<string, unknown> } | undefined;
    if (oozieSite) {
      return oozieSite.properties?.[
        "oozie.service.JPAService.jdbc.driver"
      ];
    }
  }

  return undefined;
}

export function hasReassignManualCommands(
  componentName: string | undefined,
  oozieJdbcDriver?: unknown
): boolean {
  if (componentName === "OOZIE_SERVER") {
    return oozieUsesEmbeddedDatabase(oozieJdbcDriver);
  }
  return [
    "NAMENODE",
    "SECONDARY_NAMENODE",
    "MYSQL_SERVER",
    "APP_TIMELINE_SERVER",
  ].includes(componentName || "");
}
