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
vi.mock("./config/axiosConfig", () => ({ ambariApi: { request: mocks.request } }));

import WizardApi from "./wizardApi";

describe("host bootstrap API", () => {
  beforeEach(() => mocks.request.mockResolvedValue({ data: {} }));

  it("launches and polls bootstrap without interpolating the request body", async () => {
    const payload = { hosts: ["host1"], userRunAs: "root" };
    await WizardApi.launchBootstrap(payload);
    await WizardApi.getBootstrapStatus("request/1");

    expect(mocks.request).toHaveBeenNthCalledWith(1, {
      url: "/bootstrap",
      method: "POST",
      data: payload,
    });
    expect(mocks.request).toHaveBeenNthCalledWith(2, {
      url: "/bootstrap/request%2F1",
      method: "GET",
    });
  });
});
