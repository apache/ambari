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
  canEnterSummary,
  canRetryInstallation,
  mergeInstallTasks,
  requestFailed,
  requestFinished,
  requestIdFrom,
  wizardCheckpoint,
} from "./installationProgress";

describe("installation progress", () => {
  it("extracts request IDs from supported Ambari response shapes", () => {
    expect(requestIdFrom({ Requests: { id: 1 } })).toBe(1);
    expect(requestIdFrom({ data: { Requests: { id: 2 } } })).toBe(2);
    expect(requestIdFrom({ id: "3" })).toBe("3");
  });

  it("recognizes terminal and failed request or task states", () => {
    expect(requestFinished({ Requests: { request_status: "COMPLETED" } })).toBe(true);
    expect(requestFinished({ tasks: [{ Tasks: { status: "TIMEDOUT" } }] })).toBe(true);
    expect(requestFailed({ tasks: [{ Tasks: { status: "FAILED" } }] })).toBe(true);
    expect(requestFinished({ tasks: [{ Tasks: { status: "IN_PROGRESS" } }] })).toBe(false);
  });

  it("keeps logs from prior requests while replacing matching task snapshots", () => {
    const merged = mergeInstallTasks(
      [{ Tasks: { request_id: 1, id: 1, status: "IN_PROGRESS" } }],
      [
        { Tasks: { request_id: 1, id: 1, status: "COMPLETED" } },
        { Tasks: { request_id: 2, id: 1, status: "PENDING" } },
      ],
    );
    expect(merged.map((task) => task.Tasks?.status)).toEqual(["COMPLETED", "PENDING"]);
  });

  it("matches the Classic retry and Summary gates", () => {
    expect(canRetryInstallation("INSTALL FAILED")).toBe(true);
    expect(canRetryInstallation("START FAILED")).toBe(false);
    expect(canEnterSummary("clusterCreation", "INSTALL FAILED")).toBe(false);
    expect(canEnterSummary("addService", "INSTALL FAILED")).toBe(true);
    expect(canEnterSummary("clusterCreation", "START FAILED")).toBe(true);
  });

  it("builds recovery checkpoints for every wizard", () => {
    expect(wizardCheckpoint("clusterCreation", "PREP")).toBe("CLUSTER_DEPLOY_PREP_2");
    expect(wizardCheckpoint("addHost", "INSTALLING")).toBe("ADD_HOSTS_INSTALLING_3");
    expect(wizardCheckpoint("addService", "INSTALLED")).toBe("ADD_SERVICES_INSTALLED_4");
    expect(wizardCheckpoint("addService", "STARTING")).toBe("SERVICE_STARTING_3");
  });
});
