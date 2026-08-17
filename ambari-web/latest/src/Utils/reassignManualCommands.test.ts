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
import {
  getDatabaseManualCommands,
  getOozieJdbcDriver,
  hasReassignManualCommands,
  oozieUsesEmbeddedDatabase,
} from "./reassignManualCommands";

describe("Reassign manual commands", () => {
  it("builds the classic embedded Oozie database copy sequence", () => {
    const steps = getDatabaseManualCommands({
      componentName: "OOZIE_SERVER",
      groupName: "cluster-group",
      sourceHost: "source1",
      targetHost: "target1",
    });

    expect(steps).toHaveLength(4);
    expect(steps?.[0]).toMatchObject({
      description: expect.stringContaining("source1"),
      command: "/hadoop/oozie/data/oozie-db",
    });
    expect(steps?.[1].description).toContain("target1");
    expect(steps?.[3].command).toBe(
      "chown -R oozie:cluster-group /hadoop/oozie/data"
    );
  });

  it("builds the classic MySQL export and import sequence", () => {
    const steps = getDatabaseManualCommands({
      componentName: "MYSQL_SERVER",
      sourceHost: "source1",
      targetHost: "target1",
    });

    expect(steps?.map((step) => step.command).filter(Boolean)).toEqual([
      "mysqldump db_name > backup-file.sql",
      "CREATE DATABASE db_name;",
      "mysql db_name < backup-file.sql",
    ]);
  });

  it("recognizes only Derby as the Oozie embedded database", () => {
    expect(oozieUsesEmbeddedDatabase("org.apache.derby.jdbc.EmbeddedDriver")).toBe(true);
    expect(oozieUsesEmbeddedDatabase("com.mysql.jdbc.Driver")).toBe(false);
    expect(oozieUsesEmbeddedDatabase(undefined)).toBe(false);
  });

  it("derives Oozie manual-step eligibility from the current config", () => {
    const derbyResponse = {
      items: [
        {
          configurations: [
            {
              type: "oozie-site",
              properties: {
                "oozie.service.JPAService.jdbc.driver":
                  "org.apache.derby.jdbc.EmbeddedDriver",
              },
            },
          ],
        },
      ],
    };
    const driver = getOozieJdbcDriver(derbyResponse);

    expect(driver).toBe("org.apache.derby.jdbc.EmbeddedDriver");
    expect(hasReassignManualCommands("OOZIE_SERVER", driver)).toBe(true);
    expect(
      hasReassignManualCommands("OOZIE_SERVER", "com.mysql.jdbc.Driver")
    ).toBe(false);
    expect(hasReassignManualCommands("MYSQL_SERVER")).toBe(true);
    expect(hasReassignManualCommands("RESOURCEMANAGER")).toBe(false);
  });

  it("treats a missing or malformed Oozie config as non-embedded", () => {
    expect(getOozieJdbcDriver(undefined)).toBeUndefined();
    expect(getOozieJdbcDriver({ items: [{ configurations: [] }] }))
      .toBeUndefined();
    expect(hasReassignManualCommands("OOZIE_SERVER", undefined)).toBe(false);
  });
});
