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
  hasFinishedUpgradeHistory,
  isTerminalUpgradeStatus,
  serviceCheckFailureSummary,
  skippedServiceCheckNames,
  slaveComponentFailureDetails,
  waitForUpgradeStatus,
} from "./upgradeUtils";

describe("upgrade failure summaries", () => {
  it("parses and deduplicates failed service checks from the last group", () => {
    expect(skippedServiceCheckNames({ items: [
      { upgrade_items: [{ tasks: [{ Tasks: { command_detail: "SERVICE_CHECK OLD" } }] }] },
      { upgrade_items: [
        { tasks: [{ Tasks: { command_detail: "SERVICE_CHECK HDFS" } }] },
        { tasks: [{ Tasks: { command_detail: "RESTART HDFS" } }, { Tasks: { command_detail: "SERVICE_CHECK HDFS" } }] },
        { tasks: [{ Tasks: { command_detail: "SERVICE_CHECK YARN" } }] },
      ] },
    ] })).toEqual(["HDFS", "YARN"]);
  });

  it("parses service and host-component failures without uninitialized state", () => {
    expect(serviceCheckFailureSummary({ tasks: [{ Tasks: { structured_out: { failures: {
      service_check: ["HDFS", "YARN", "HDFS"],
      host_component: { host1: [{ service: "HDFS", component: "DATANODE" }] },
    } } } }] })).toEqual({
      serviceNames: ["HDFS", "YARN"],
      hostDetails: {
        hosts: ["host1"],
        host_detail: { host1: [{ service: "HDFS", component: "DATANODE" }] },
      },
    });
  });

  it("returns safe empty objects for missing task output", () => {
    expect(serviceCheckFailureSummary({ tasks: [] })).toEqual({
      serviceNames: [],
      hostDetails: { hosts: [], host_detail: {} },
    });
    expect(slaveComponentFailureDetails({ tasks: [] })).toEqual({ hosts: [], host_detail: {} });
  });
});

describe("upgrade terminal states", () => {
  it("stops for completed and unrecoverable terminal requests", () => {
    expect(isTerminalUpgradeStatus("COMPLETED")).toBe(true);
    expect(isTerminalUpgradeStatus("FAILED")).toBe(true);
    expect(isTerminalUpgradeStatus("TIMEDOUT")).toBe(true);
    expect(isTerminalUpgradeStatus("ABORTED")).toBe(false);
    expect(isTerminalUpgradeStatus("IN_PROGRESS")).toBe(false);
  });

  it("bounds status polling and propagates request failures", async () => {
    const statuses = ["IN_PROGRESS", "ABORTED"];
    await expect(waitForUpgradeStatus(
      async () => statuses.shift(),
      "ABORTED",
      { attempts: 2, intervalMs: 0 },
    )).resolves.toBeUndefined();
    await expect(waitForUpgradeStatus(
      async () => "IN_PROGRESS",
      "ABORTED",
      { attempts: 2, intervalMs: 0 },
    )).rejects.toThrow("timed out");
    await expect(waitForUpgradeStatus(
      async () => { throw new Error("poll failed"); },
      "ABORTED",
      { attempts: 2, intervalMs: 0 },
    )).rejects.toThrow("poll failed");
  });
});

describe("upgrade history availability", () => {
  const upgrade = (requestId: number, requestStatus: string) => ({
    Upgrade: { request_id: requestId, request_status: requestStatus },
  });

  it("hides a sole running or suspended upgrade", () => {
    expect(hasFinishedUpgradeHistory([upgrade(1, "IN_PROGRESS")])).toBe(false);
    expect(hasFinishedUpgradeHistory([upgrade(1, "ABORTED")])).toBe(false);
  });

  it("shows history when the latest upgrade has finished", () => {
    expect(hasFinishedUpgradeHistory([upgrade(1, "COMPLETED")])).toBe(true);
  });

  it("shows older finished history while a newer upgrade is active", () => {
    expect(hasFinishedUpgradeHistory([
      upgrade(3, "HOLDING"),
      upgrade(1, "COMPLETED"),
      upgrade(2, "PENDING"),
    ])).toBe(true);
  });
});
