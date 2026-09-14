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
const request = vi.hoisted(() => vi.fn());
vi.mock("./config/axiosConfig", () => ({ ambariApi: { request }, supressErrorAmbariApi: { request } }));
import { apiPathSegment } from "./apiPath";
import { RequestApi } from "./requestApi";
import { HostsApi } from "./hostsApi";

beforeEach(() => { request.mockReset(); request.mockResolvedValue({ data: {} }); });
describe("raw cluster identity at API path boundaries", () => {
  it.each(["cluster-a", "集群 A", "a/b", "a%2Fb", "a?b#c", "a(b)"])("encodes %s without interpreting a raw percent escape", async name => {
    await RequestApi.getRequestStatus(name, "19");
    expect(request.mock.calls[0][0].url).toMatch(`/clusters/${apiPathSegment(name)}/requests/19?`);
    expect(apiPathSegment("a%2Fb")).toBe("a%252Fb");
  });
  it("encodes the cluster and host independently", async () => {
    await HostsApi.getHostData("A/B", "host#one", "Hosts/host_name");
    expect(request.mock.calls[0][0].url).toBe("/clusters/A%2FB/hosts/host%23one?fields=Hosts/host_name");
  });
});
