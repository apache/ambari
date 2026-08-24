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

const mocks = vi.hoisted(() => ({
  request: vi.fn(),
  suppressedRequest: vi.fn(),
}));

vi.mock("../../../../api/config/axiosConfig", () => ({
  ambariApi: { request: mocks.request },
  supressErrorAmbariApi: { request: mocks.suppressedRequest },
}));

import rmHaApi from "./rmHaApi";

describe("ResourceManager HA API", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("posts the exact Stack Advisor URL and payload", async () => {
    const payload = {
      recommend: "host_groups",
      hosts: ["h1", "h2"],
      services: ["YARN"],
      recommendations: { blueprint: { host_groups: [] } },
    };
    mocks.request.mockResolvedValue({ data: { resources: [] } });

    await rmHaApi.getHostRecommendations("HDP", "3.1", payload);

    expect(mocks.request).toHaveBeenCalledWith({
      url: "/stacks/HDP/versions/3.1/recommendations",
      method: "POST",
      data: payload,
    });
  });

  it("uses exact desired-config GET and PUT contracts", async () => {
    mocks.request
      .mockResolvedValueOnce({ data: { items: [] } })
      .mockResolvedValueOnce({ data: {}, status: 200 });

    await rmHaApi.getConfigs("c 1", "(type=yarn-site&tag=version1)");
    const payload = {
      Clusters: {
        desired_config: [
          { type: "yarn-site", properties: { enabled: "true" } },
        ],
      },
    };
    await rmHaApi.saveDesiredConfig("c 1", payload);

    expect(mocks.request.mock.calls[0][0]).toEqual({
      url: "/clusters/c%201/configurations?(type=yarn-site&tag=version1)",
      method: "GET",
    });
    expect(mocks.request.mock.calls[1][0]).toEqual({
      url: "/clusters/c%201",
      method: "PUT",
      data: payload,
      headers: { "Content-Type": "text/plain" },
    });
  });

  it("stops the selected non-HDFS services and starts all with smoke tests", async () => {
    mocks.request
      .mockResolvedValueOnce({
        data: { Requests: { id: 11 } },
        status: 202,
      })
      .mockResolvedValueOnce({
        data: { Requests: { id: 12 } },
        status: 202,
      });

    await rmHaApi.stopRequiredServices("c1", ["YARN", "HBASE"]);
    await rmHaApi.startAllServices("c1", true);

    expect(mocks.request.mock.calls[0][0]).toEqual({
      url: "/clusters/c1/services?ServiceInfo/service_name.in(YARN,HBASE)",
      method: "PUT",
      data: {
        RequestInfo: {
          context: "Stop required services",
          operation_level: { level: "CLUSTER", cluster_name: "c1" },
        },
        Body: { ServiceInfo: { state: "INSTALLED" } },
      },
      headers: { "Content-Type": "text/plain" },
    });
    expect(mocks.request.mock.calls[1][0]).toEqual({
      url: "/clusters/c1/services?params/run_smoke_test=true",
      method: "PUT",
      data: {
        RequestInfo: {
          context: "Start all services",
          operation_level: { level: "CLUSTER", cluster_name: "c1" },
        },
        Body: { ServiceInfo: { state: "STARTED" } },
      },
      headers: { "Content-Type": "text/plain" },
    });
  });

  it("rejects a successful polled operation without a request ID", async () => {
    mocks.request.mockResolvedValue({ data: {}, status: 202 });

    await expect(
      rmHaApi.startAllServices("c1", false),
    ).rejects.toThrow("request ID");
  });

  it("installs an already registered ResourceManager without recreating it", async () => {
    mocks.request
      .mockResolvedValueOnce({
        data: { items: [{ HostRoles: { host_name: "rm2" } }] },
      })
      .mockResolvedValueOnce({
        data: { Requests: { id: 21 } },
        status: 202,
      });

    await expect(
      rmHaApi.installAdditionalResourceManager("c1", "rm2"),
    ).resolves.toMatchObject({ Requests: { id: 21 }, status: 202 });

    expect(mocks.suppressedRequest).not.toHaveBeenCalled();
    expect(mocks.request).toHaveBeenCalledTimes(2);
    expect(mocks.request.mock.calls[1][0]).toEqual({
      url: "/clusters/c1/host_components?HostRoles/component_name=RESOURCEMANAGER&HostRoles/host_name.in(rm2)&HostRoles/maintenance_state=OFF",
      method: "PUT",
      data: {
        RequestInfo: {
          context: "Install ResourceManager",
          operation_level: { level: "CLUSTER", cluster_name: "c1" },
          query:
            "HostRoles/component_name=RESOURCEMANAGER&HostRoles/host_name.in(rm2)&HostRoles/maintenance_state=OFF",
        },
        Body: { HostRoles: { state: "INSTALLED" } },
      },
      headers: { "Content-Type": "text/plain" },
    });
  });

  it("creates the service component, registers the host, then installs it", async () => {
    mocks.request
      .mockResolvedValueOnce({ data: { items: [] } })
      .mockResolvedValueOnce({ data: {}, status: 201 })
      .mockResolvedValueOnce({ data: {}, status: 201 })
      .mockResolvedValueOnce({
        data: { Requests: { id: 22 } },
        status: 202,
      });
    mocks.suppressedRequest.mockRejectedValue({ response: { status: 404 } });

    await rmHaApi.installAdditionalResourceManager("c1", "rm2");

    expect(mocks.suppressedRequest).toHaveBeenCalledWith({
      url: "/clusters/c1/services/YARN/components/RESOURCEMANAGER",
      method: "GET",
    });
    expect(mocks.request.mock.calls[1][0]).toEqual({
      url: "/clusters/c1/services?ServiceInfo/service_name=YARN",
      method: "POST",
      data: {
        components: [
          {
            ServiceComponentInfo: { component_name: "RESOURCEMANAGER" },
          },
        ],
      },
    });
    expect(mocks.request.mock.calls[2][0]).toEqual({
      url: "/clusters/c1/hosts",
      method: "POST",
      data: {
        RequestInfo: { query: "Hosts/host_name=rm2" },
        Body: {
          host_components: [
            { HostRoles: { component_name: "RESOURCEMANAGER" } },
          ],
        },
      },
    });
    expect(mocks.request.mock.calls[3][0].method).toBe("PUT");
  });

  it("skips service-component creation when it already exists", async () => {
    mocks.request
      .mockResolvedValueOnce({ data: { items: [] } })
      .mockResolvedValueOnce({ data: {}, status: 201 })
      .mockResolvedValueOnce({
        data: { Requests: { id: 23 } },
        status: 202,
      });
    mocks.suppressedRequest.mockResolvedValue({ data: {} });

    await rmHaApi.installAdditionalResourceManager("c1", "rm2");

    expect(mocks.request).toHaveBeenCalledTimes(3);
    expect(mocks.request.mock.calls[1][0].url).toBe("/clusters/c1/hosts");
    expect(mocks.request.mock.calls[2][0].method).toBe("PUT");
  });

  it("propagates duplicate-check, component-create, and registration failures", async () => {
    mocks.request.mockRejectedValueOnce(new Error("duplicate check failed"));
    await expect(
      rmHaApi.installAdditionalResourceManager("c1", "rm2"),
    ).rejects.toThrow("duplicate check failed");
    expect(mocks.suppressedRequest).not.toHaveBeenCalled();

    vi.clearAllMocks();
    mocks.request
      .mockResolvedValueOnce({ data: { items: [] } })
      .mockRejectedValueOnce(new Error("component create failed"));
    mocks.suppressedRequest.mockRejectedValue({ response: { status: 404 } });
    await expect(
      rmHaApi.installAdditionalResourceManager("c1", "rm2"),
    ).rejects.toThrow("component create failed");

    vi.clearAllMocks();
    mocks.request
      .mockResolvedValueOnce({ data: { items: [] } })
      .mockRejectedValueOnce(new Error("registration failed"));
    mocks.suppressedRequest.mockResolvedValue({ data: {} });
    await expect(
      rmHaApi.installAdditionalResourceManager("c1", "rm2"),
    ).rejects.toThrow("registration failed");
  });

  it("rejects malformed duplicate-check responses", async () => {
    mocks.request.mockResolvedValue({ data: {} });
    await expect(
      rmHaApi.installAdditionalResourceManager("c1", "rm2"),
    ).rejects.toThrow("invalid ResourceManager host response");
  });
});
