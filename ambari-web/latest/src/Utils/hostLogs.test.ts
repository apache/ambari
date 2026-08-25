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

import { describe, expect, it, vi } from "vitest";
import {
  buildLogSearchUrl,
  hostLogsToText,
  mapHostLogEntries,
  mapHostLogLevelCounts,
  mapHostLogRows,
  mergeHostLogEntries,
  openTextInNewWindow,
} from "./hostLogs";

describe("host logs mapping", () => {
  it("maps logging metadata and tolerates missing logging resources", () => {
    const rows = mapHostLogRows({
      host_components: [
        {
          HostRoles: {
            component_name: "NAMENODE",
            display_name: "NameNode",
            service_name: "HDFS",
          },
          logging: {
            name: "hdfs_namenode",
            logs: [{ name: "/var/log/hdfs/namenode.log" }, "/tmp/a.out"],
          },
        },
        { HostRoles: { component_name: "CLIENT" } },
      ],
    }, "host1", { HDFS: "HDFS Display" });

    expect(rows).toHaveLength(1);
    expect(rows[0]).toMatchObject({
      componentDisplayName: "NameNode",
      logComponentName: "hdfs_namenode",
      serviceDisplayName: "HDFS Display",
    });
    expect(rows[0].files.map((file) => file.fileName)).toEqual([
      "namenode.log",
      "a.out",
    ]);
  });

  it("deduplicates polled entries and keeps chronological output", () => {
    const first = mapHostLogEntries({
      logList: [
        { id: 2, logtime: "20", level: "WARN", log_message: "second" },
        { id: 1, logtime: "10", level: "INFO", log_message: "first" },
      ],
    });
    const merged = mergeHostLogEntries(first, [
      { id: 2, logtime: 20, level: "ERROR", logMessage: "updated" },
      { id: 3, logtime: 30, level: "INFO", logMessage: "third" },
    ]);

    expect(merged.map((row) => row.id)).toEqual([1, 2, 3]);
    expect(hostLogsToText(merged)).toContain("ERROR updated");
  });

  it("aggregates server log-level counts by service without synthetic data", () => {
    const rows = mapHostLogLevelCounts({
      host_components: [
        {
          HostRoles: { service_name: "HDFS" },
          logging: {
            log_level_counts: [
              { name: "ERROR", value: "3" },
              { name: "WARN", value: "4" },
            ],
          },
        },
        {
          HostRoles: { service_name: "HDFS" },
          logging: { log_level_counts: [{ name: "ERROR", value: "2" }] },
        },
        {
          HostRoles: { service_name: "YARN" },
          logging: { logs: [] },
        },
      ],
    }, { HDFS: "HDFS Display" });

    expect(rows).toHaveLength(2);
    expect(rows[0]).toMatchObject({
      available: true,
      serviceDisplayName: "HDFS Display",
      serviceName: "HDFS",
    });
    expect(rows[0].counts.ERROR).toBe(5);
    expect(rows[0].counts.WARNING).toBe(4);
    expect(rows[1]).toMatchObject({ available: false, serviceName: "YARN" });
  });

  it("opens loaded text through textContent instead of document.write", () => {
    const pre = { textContent: "" };
    const replaceChildren = vi.fn();
    const document = {
      title: "",
      createElement: vi.fn(() => pre),
      body: { replaceChildren },
    };
    const target = { document, opener: {} };
    const openWindow = vi.fn(() => target);

    expect(openTextInNewWindow("<script>unsafe()</script>", openWindow as any)).toBe(true);
    expect(pre.textContent).toBe("<script>unsafe()</script>");
    expect(replaceChildren).toHaveBeenCalledWith(pre);
    expect((target as any).opener).toBeNull();
  });

  it("builds an encoded link from the LOGSEARCH quick-link root", () => {
    const result = buildLogSearchUrl(
      "https://logsearch.example:61888/#/",
      "host one",
      "hdfs_namenode",
      "/var/log/hdfs/name node.log",
    );

    expect(result).toContain("https://logsearch.example:61888/#/logs/serviceLogs");
    expect(result).toContain("hosts=host%20one");
    expect(result).toContain("components=hdfs_namenode");
    expect(decodeURIComponent(result.split(";query=")[1])).toContain(
      '\"value\":\"/var/log/hdfs/name node.log\"',
    );
  });
});
