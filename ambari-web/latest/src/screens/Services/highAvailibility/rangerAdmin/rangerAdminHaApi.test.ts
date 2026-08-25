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
vi.mock("../../../../api/config/axiosConfig", () => ({
  ambariApi: { request: mocks.request },
}));

import {
  countRangerAdminHostComponents,
  evaluateRangerAdminEnablement,
  RANGER_ADMIN_ENABLEMENT_MESSAGES,
  rangerAdminConfigApi,
  rangerAdminEnablementApi,
  reconfigureRangerAdminServices,
} from "./rangerAdminHaApi";

describe("Ranger Admin HA configuration API", () => {
  const api = {
    loadConfigTags: vi.fn(),
    reassignLoadConfigs: vi.fn(),
    updateServiceMultiConfigurations: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    api.updateServiceMultiConfigurations.mockResolvedValue({ status: 202 });
  });

  it("loads and validates complete configs before one multi-configuration PUT", async () => {
    api.loadConfigTags.mockResolvedValue({
      Clusters: {
        desired_configs: {
          "admin-properties": { tag: "version1" },
          "ranger-hdfs-security": { tag: "version2" },
        },
      },
    });
    api.reassignLoadConfigs.mockResolvedValue({
      items: [
        { type: "admin-properties", properties: { existing: "one" } },
        { type: "ranger-hdfs-security", properties: { existing: "two" } },
      ],
    });

    const result = await reconfigureRangerAdminServices(
      "c1",
      "https://lb.example.com",
      api,
    );

    expect(api.reassignLoadConfigs).toHaveBeenCalledWith(
      "c1",
      "(type=admin-properties&tag=version1)|(type=ranger-hdfs-security&tag=version2)",
    );
    expect(api.updateServiceMultiConfigurations).toHaveBeenCalledOnce();
    const request = api.updateServiceMultiConfigurations.mock.calls[0][1];
    expect(request.configs).toHaveLength(2);
    expect(
      request.configs[1].Clusters.desired_config[0].properties[
        "ranger.plugin.hdfs.policy.rest.url"
      ],
    ).toBe("https://lb.example.com");
    expect(api.loadConfigTags.mock.invocationCallOrder[0]).toBeLessThan(
      api.reassignLoadConfigs.mock.invocationCallOrder[0],
    );
    expect(api.reassignLoadConfigs.mock.invocationCallOrder[0]).toBeLessThan(
      api.updateServiceMultiConfigurations.mock.invocationCallOrder[0],
    );
    expect(result).toEqual({ status: 202 });
  });

  it.each([
    ["missing desired configs", {}],
    ["non-object desired configs", { Clusters: { desired_configs: [] } }],
    [
      "missing admin-properties",
      {
        Clusters: {
          desired_configs: {
            "ranger-hdfs-security": { tag: "version2" },
          },
        },
      },
    ],
    [
      "malformed admin-properties",
      { Clusters: { desired_configs: { "admin-properties": { tag: "" } } } },
    ],
  ])("rejects %s without attempting a save", async (_description, response) => {
    api.loadConfigTags.mockResolvedValue(response);

    await expect(
      reconfigureRangerAdminServices("c1", "https://lb.example.com", api),
    ).rejects.toThrow();
    expect(api.reassignLoadConfigs).not.toHaveBeenCalled();
    expect(api.updateServiceMultiConfigurations).not.toHaveBeenCalled();
  });

  it.each([
    ["missing items", {}],
    ["non-array items", { items: {} }],
    [
      "malformed item properties",
      {
        items: [
          { type: "admin-properties", properties: {} },
          { type: "ranger-hdfs-security", properties: null },
        ],
      },
    ],
    [
      "missing queried site",
      { items: [{ type: "admin-properties", properties: {} }] },
    ],
  ])("rejects a config response with %s", async (_description, response) => {
    api.loadConfigTags.mockResolvedValue({
      Clusters: {
        desired_configs: {
          "admin-properties": { tag: "version1" },
          "ranger-hdfs-security": { tag: "version2" },
        },
      },
    });
    api.reassignLoadConfigs.mockResolvedValue(response);

    await expect(
      reconfigureRangerAdminServices("c1", "https://lb.example.com", api),
    ).rejects.toThrow();
    expect(api.updateServiceMultiConfigurations).not.toHaveBeenCalled();
  });

  it("propagates config load and save failures to the progress task", async () => {
    api.loadConfigTags.mockRejectedValue(new Error("tags failed"));
    await expect(
      reconfigureRangerAdminServices("c1", "https://lb.example.com", api),
    ).rejects.toThrow("tags failed");

    api.loadConfigTags.mockResolvedValue({
      Clusters: {
        desired_configs: { "admin-properties": { tag: "version1" } },
      },
    });
    api.reassignLoadConfigs.mockResolvedValue({
      items: [{ type: "admin-properties", properties: {} }],
    });
    api.updateServiceMultiConfigurations.mockRejectedValue(
      new Error("save failed"),
    );
    await expect(
      reconfigureRangerAdminServices("c1", "https://lb.example.com", api),
    ).rejects.toThrow("save failed");
  });

  it("pins the Ranger Admin HA REST URLs, array PUT body, attributes, and status", async () => {
    mocks.request
      .mockResolvedValueOnce({
        data: {
          Clusters: {
            desired_configs: {
              "admin-properties": { tag: "version1" },
              "ranger-hdfs-security": { tag: "version2" },
            },
          },
        },
      })
      .mockResolvedValueOnce({
        data: {
          items: [
            {
              type: "admin-properties",
              properties: { existing: "one" },
              properties_attributes: {
                final: { protected_property: "true" },
              },
            },
            {
              type: "ranger-hdfs-security",
              properties: { existing: "two" },
            },
          ],
        },
      })
      .mockResolvedValueOnce({ data: { Requests: { id: 42 } }, status: 202 });

    const result = await reconfigureRangerAdminServices(
      "c1",
      "https://lb.example.com",
      rangerAdminConfigApi,
    );

    expect(mocks.request).toHaveBeenNthCalledWith(1, {
      url: "/clusters/c1?fields=Clusters/desired_configs",
      method: "GET",
    });
    expect(mocks.request).toHaveBeenNthCalledWith(2, {
      url: "/clusters/c1/configurations?(type=admin-properties&tag=version1)|(type=ranger-hdfs-security&tag=version2)",
      method: "GET",
    });
    const putBody = mocks.request.mock.calls[2][0].data;
    expect(mocks.request).toHaveBeenNthCalledWith(3, {
      url: "/clusters/c1",
      method: "PUT",
      data: expect.any(Array),
    });
    expect(putBody).toHaveLength(2);
    expect(putBody[0].Clusters.desired_config[0].properties_attributes).toEqual(
      { final: { protected_property: "true" } },
    );
    expect(result).toEqual({ Requests: { id: 42 }, status: 202 });
  });

  it("loads the exact Ranger Admin host component set for enablement checks", async () => {
    const component = {
      host_components: [
        { HostRoles: { host_name: "ra1.example.com", state: "STARTED" } },
        { HostRoles: { host_name: "ra2.example.com", state: "STARTED" } },
      ],
    };
    mocks.request.mockResolvedValue({ data: component });

    const response =
      await rangerAdminEnablementApi.loadRangerAdminComponent("c1");

    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/c1/services/RANGER/components/RANGER_ADMIN?fields=host_components/HostRoles/host_name,host_components/HostRoles/state",
      method: "GET",
    });
    expect(countRangerAdminHostComponents(response)).toBe(2);
  });

  it.each([
    {
      name: "a one-host cluster",
      states: ["STARTED"],
      hostCount: 1,
      status: "disabled",
      errors: [RANGER_ADMIN_ENABLEMENT_MESSAGES.hostCount],
    },
    {
      name: "a missing Ranger Admin",
      states: [],
      hostCount: 2,
      status: "disabled",
      errors: [RANGER_ADMIN_ENABLEMENT_MESSAGES.rangerAdminMissing],
    },
    {
      name: "an INIT Ranger Admin",
      states: ["INIT"],
      hostCount: 2,
      status: "disabled",
      errors: [RANGER_ADMIN_ENABLEMENT_MESSAGES.rangerAdminNotInstalled],
    },
    {
      name: "an INSTALL_FAILED Ranger Admin",
      states: ["INSTALL_FAILED"],
      hostCount: 2,
      status: "disabled",
      errors: [RANGER_ADMIN_ENABLEMENT_MESSAGES.rangerAdminNotInstalled],
    },
    {
      name: "an installed single Ranger Admin",
      states: ["STOPPED"],
      hostCount: 2,
      status: "enabled",
      errors: [],
    },
    {
      name: "multiple Ranger Admins",
      states: ["STARTED", "STARTED"],
      hostCount: 3,
      status: "hidden",
      errors: [RANGER_ADMIN_ENABLEMENT_MESSAGES.alreadyEnabled],
    },
  ])(
    "evaluates $name with Classic entry semantics",
    ({ states, hostCount, status, errors }) => {
      const response = {
        host_components: states.map((state, index) => ({
          HostRoles: { host_name: `ra-${index + 1}`, state },
        })),
      };

      expect(evaluateRangerAdminEnablement(response, hostCount)).toEqual({
        status,
        errors,
      });
    },
  );

  it.each([
    {},
    { host_components: null },
    { host_components: [{}] },
    { host_components: [{ HostRoles: { host_name: "" } }] },
    {
      host_components: [
        { HostRoles: { host_name: "ra1.example.com", state: "" } },
      ],
    },
  ])("rejects malformed Ranger Admin component data", (response) => {
    expect(() => countRangerAdminHostComponents(response)).toThrow(
      /malformed Ranger Admin/,
    );
  });
});
