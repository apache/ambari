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
import type { BackgroundRequest } from "./backgroundOperations";
import {
  hasActiveServiceRequest,
  isServiceStartStopBlocked,
} from "./serviceActionPolicy";

function request(
  context: string,
  status = "IN_PROGRESS",
): BackgroundRequest {
  return {
    Requests: {
      id: 1,
      request_context: context,
      request_status: status,
    },
  };
}

describe("service action policy", () => {
  it("blocks a service for its active request or an all-services request", () => {
    expect(hasActiveServiceRequest([
      request("_PARSE_.START.HDFS"),
    ], "HDFS")).toBe(true);
    expect(hasActiveServiceRequest([
      request("_PARSE_.STOP.ALL_SERVICES", "QUEUED"),
    ], "YARN")).toBe(true);
  });

  it("ignores terminal, unrelated, and unstructured request contexts", () => {
    expect(hasActiveServiceRequest([
      request("_PARSE_.START.HDFS", "COMPLETED"),
      request("_PARSE_.STOP.YARN"),
      request("Restart HDFS"),
    ], "HDFS")).toBe(false);
  });

  it("blocks transitions, an in-flight submission, and an accepted request", () => {
    expect(isServiceStartStopBlocked("STARTING", false, false, null)).toBe(true);
    expect(isServiceStartStopBlocked("STOPPING", false, false, null)).toBe(true);
    expect(isServiceStartStopBlocked("STARTED", true, false, null)).toBe(true);
    expect(isServiceStartStopBlocked("STARTED", false, true, null)).toBe(true);
    expect(isServiceStartStopBlocked("STARTED", false, false, 42)).toBe(true);
    expect(isServiceStartStopBlocked("STARTED", false, false, null)).toBe(false);
  });
});
