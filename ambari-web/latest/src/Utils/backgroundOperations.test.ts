/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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
  BackgroundRequest,
  canAbortOperation,
  isUpgradeRequest,
  isOperationTerminal,
  isRequestScheduleRunning,
  replaceRequestSnapshot,
  statusMatchesFilter,
  shouldShowBackgroundOperations,
  sourceRequestScheduleId,
  upsertTaskEvents,
  upsertRequestEvent,
} from "./backgroundOperations";

const request = (id: number, status = "IN_PROGRESS", context = `Request ${id}`) => ({
  Requests: { id, request_status: status, request_context: context },
}) as BackgroundRequest;

describe("background operation policy", () => {
  it("requires start/stop permission and blocks duplicate abort submissions", () => {
    expect(canAbortOperation("IN_PROGRESS", false)).toBe(false);
    expect(canAbortOperation("IN_PROGRESS", true)).toBe(true);
    expect(canAbortOperation("IN_PROGRESS", true, true)).toBe(false);
    expect(canAbortOperation("COMPLETED", true)).toBe(false);
    expect(canAbortOperation("SOME_NEW_RUNNING_STATE", true)).toBe(true);
  });

  it("treats all server task terminal states as finished", () => {
    expect(isOperationTerminal("TIMEDOUT")).toBe(true);
    expect(isOperationTerminal("SKIPPED_FAILED")).toBe(true);
    expect(isOperationTerminal("QUEUED")).toBe(false);
  });

  it("only exposes schedule cancellation while future batches can run", () => {
    expect(isRequestScheduleRunning("SCHEDULED")).toBe(true);
    expect(isRequestScheduleRunning("IN_PROGRESS")).toBe(true);
    expect(isRequestScheduleRunning("DISABLED")).toBe(false);
    expect(isRequestScheduleRunning("COMPLETED")).toBe(false);
  });

  it("suppresses the schedule association for a one-host recommission", () => {
    expect(sourceRequestScheduleId({
      Requests: {
        inputs: JSON.stringify({ included_hosts: "host1" }),
        request_context: "Recommission DataNode",
        request_schedule: { schedule_id: 9 },
      },
    })).toBeNull();
    expect(sourceRequestScheduleId({
      Requests: {
        inputs: JSON.stringify({ included_hosts: "host1,host2" }),
        request_context: "Recommission DataNodes",
        request_schedule: { schedule_id: 9 },
      },
    })).toBe(9);
  });

  it("replaces REST snapshots, removes upgrades, de-duplicates, and sorts", () => {
    expect(replaceRequestSnapshot([
      request(1),
      request(3),
      request(2, "IN_PROGRESS", "Upgrading cluster"),
      request(1, "COMPLETED"),
    ])).toEqual([request(3), request(1, "COMPLETED")]);
  });

  it("recognizes upgrade contexts from REST and socket message shapes", () => {
    expect(isUpgradeRequest(request(1, "IN_PROGRESS", "Downgrading cluster"))).toBe(true);
    expect(isUpgradeRequest({ requestContext: "Upgrading cluster" })).toBe(true);
    expect(isUpgradeRequest({ request_context: "Restart HDFS" })).toBe(false);
  });

  it("groups skipped failures with failed operations", () => {
    expect(statusMatchesFilter("SKIPPED_FAILED", "failed")).toBe(true);
    expect(statusMatchesFilter("COMPLETED", "failed")).toBe(false);
    expect(statusMatchesFilter("QUEUED", "pending")).toBe(true);
    expect(statusMatchesFilter("COMPLETED", "success")).toBe(true);
  });

  it("honors automatic popup preferences while always restricting cluster users", () => {
    expect(shouldShowBackgroundOperations(true, false, false)).toBe(true);
    expect(shouldShowBackgroundOperations(false, false, false)).toBe(false);
    expect(shouldShowBackgroundOperations(false, true, false)).toBe(true);
    expect(shouldShowBackgroundOperations(true, true, true)).toBe(false);
  });

  it("upserts one socket event without deleting unrelated requests", () => {
    const result = upsertRequestEvent([request(3), request(2)], {
      requestId: 2,
      requestStatus: "COMPLETED",
      progressPercent: 100,
    }, 20);

    expect(result).toHaveLength(2);
    expect(result[0].Requests.id).toBe(3);
    expect(result[1].Requests).toMatchObject({
      id: 2,
      request_context: "Request 2",
      request_status: "COMPLETED",
      progress_percent: 100,
    });
  });

  it("updates existing tasks and adds tasks or hosts introduced by socket events", () => {
    expect(upsertTaskEvents([
      { Tasks: { id: 1, host_name: "host1", request_id: 7, status: "QUEUED", role: "DATANODE" } },
    ], [
      { id: 1, hostName: "host1", requestId: 7, status: "IN_PROGRESS" },
      { id: 2, hostName: "host2", requestId: 7, status: "PENDING" },
    ])).toEqual([
      { Tasks: { id: 1, host_name: "host1", request_id: 7, status: "IN_PROGRESS", role: "DATANODE" } },
      { Tasks: { id: 2, host_name: "host2", request_id: 7, status: "PENDING" } },
    ]);
  });
});
