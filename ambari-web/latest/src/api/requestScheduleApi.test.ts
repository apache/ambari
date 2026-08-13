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

import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({ request: vi.fn() }));
vi.mock("./config/axiosConfig", () => ({
  ambariApi: { request: mocks.request },
}));

import RequestScheduleApi from "./requestScheduleApi";

describe("request schedule API", () => {
  beforeEach(() => {
    mocks.request.mockReset();
    mocks.request.mockResolvedValue({ data: {} });
  });

  it("loads one schedule and cancels its future batches", async () => {
    await RequestScheduleApi.fetch("cluster/name", 17);
    await RequestScheduleApi.cancel("cluster/name", 17);

    expect(mocks.request).toHaveBeenNthCalledWith(1, {
      url: "/clusters/cluster%2Fname/request_schedules/17",
      method: "GET",
    });
    expect(mocks.request).toHaveBeenNthCalledWith(2, {
      url: "/clusters/cluster%2Fname/request_schedules/17",
      method: "DELETE",
    });
  });

  it("queries scheduled and in-progress schedules for conflict detection", async () => {
    await RequestScheduleApi.fetchPending("c1");
    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/c1/request_schedules?fields=*&(RequestSchedule/status.in(SCHEDULED,IN_PROGRESS))",
      method: "GET",
    });
  });
});
