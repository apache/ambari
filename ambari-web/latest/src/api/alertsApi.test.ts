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

import { AlertsApi } from "./alertsApi";

describe("alerts API", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.request.mockResolvedValue({ data: { items: [] } });
  });

  it("loads alert instances for an encoded cluster and an exact host", async () => {
    await AlertsApi.getHostAlertInstances("cluster/name", "host & one", 1234);

    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/cluster%2Fname/alerts",
      method: "GET",
      params: {
        fields: "*",
        "Alert/host_name": "host & one",
        _: 1234,
      },
    });
  });

  it("loads one definition directly by ID", async () => {
    await AlertsApi.getAlertDefinitionById("cluster/name", "definition id", 1234);

    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/cluster%2Fname/alert_definitions/definition%20id",
      method: "GET",
      params: { fields: "*", _: 1234 },
    });
  });

  it("loads instances for one definition", async () => {
    await AlertsApi.getAlertInstancesByDefinition("cluster/name", 42, 1234);

    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/cluster%2Fname/alerts",
      method: "GET",
      params: {
        fields: "*",
        "Alert/definition_id": "42",
        _: 1234,
      },
    });
  });

  it("loads the exact definition history window", async () => {
    await AlertsApi.getAlertHistory("cluster/name", "definition name", 1000);

    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/cluster%2Fname/alert_history?(AlertHistory/definition_name=definition%20name)&(AlertHistory/timestamp>=1000)",
      method: "GET",
    });
  });

  it("creates a definition on the cluster collection", async () => {
    const definition = { "AlertDefinition/name": "custom" };
    await AlertsApi.createAlertDefinition("cluster/name", definition);

    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/cluster%2Fname/alert_definitions/",
      method: "POST",
      data: definition,
    });
  });

  it("preserves complete group replacement payloads", async () => {
    const payload = { AlertGroup: { name: "group", definitions: [1], targets: [2] } };
    await AlertsApi.createAlertGroup("cluster", payload);
    await AlertsApi.updateAlertGroup("cluster", 3, payload);

    expect(mocks.request).toHaveBeenNthCalledWith(1, {
      url: "clusters/cluster/alert_groups",
      method: "POST",
      data: payload,
    });
    expect(mocks.request).toHaveBeenNthCalledWith(2, {
      url: "clusters/cluster/alert_groups/3",
      method: "PUT",
      data: payload,
    });
  });
});
