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

import federationApi, {
  buildComponentCommandPayload,
  desiredConfigsFromSnapshot,
} from "./federationApi";

describe("Federation API", () => {
  beforeEach(() => {
    mocks.request.mockReset();
    mocks.request.mockResolvedValue({ data: {}, status: 202 });
  });

  it("loads only available optional configuration types", async () => {
    mocks.request
      .mockResolvedValueOnce({
        data: {
          Clusters: {
            desired_configs: {
              "hdfs-site": { tag: "v1" },
              "core-site": { tag: "v2" },
            },
          },
        },
      })
      .mockResolvedValueOnce({ data: { items: [] } });

    await federationApi.loadCurrentConfigurations(
      "cluster/name",
      ["hdfs-site", "core-site"],
      ["hdfs-rbf-site"],
    );
    expect(mocks.request).toHaveBeenNthCalledWith(2, {
      url:
        "/clusters/cluster%2Fname/configurations?" +
        "(type=hdfs-site&tag=v1)|(type=core-site&tag=v2)",
      method: "GET",
    });
  });

  it("preserves attributes and uses the multi-configuration body", async () => {
    const reviewed = {
      items: [
        {
          type: "hdfs-site",
          properties: { a: "b" },
          properties_attributes: { final: { a: "true" } },
        },
      ],
    };
    expect(desiredConfigsFromSnapshot(reviewed, ["hdfs-site"], "note")).toEqual([
      {
        type: "hdfs-site",
        properties: { a: "b" },
        properties_attributes: { final: { a: "true" } },
        service_config_version_note: "note",
      },
    ]);
    await federationApi.saveConfigurationTypes(
      "c1",
      reviewed,
      ["hdfs-site"],
      "note",
    );
    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/c1",
      method: "PUT",
      data: [
        {
          Clusters: {
            desired_config: {
              type: "hdfs-site",
              properties: { a: "b" },
              properties_attributes: { final: { a: "true" } },
              service_config_version_note: "note",
            },
          },
        },
      ],
    });
  });

  it("posts exact component command and Federation restart payloads", async () => {
    const input = {
      command: "FORMAT",
      context: "Format NameNode",
      serviceName: "HDFS",
      componentName: "NAMENODE",
      hosts: "nn3",
    };
    expect(buildComponentCommandPayload(input)).toEqual({
      RequestInfo: { command: "FORMAT", context: "Format NameNode" },
      "Requests/resource_filters": [
        { service_name: "HDFS", component_name: "NAMENODE", hosts: "nn3" },
      ],
    });
    await federationApi.executeComponentCommand("c1", input);
    await federationApi.restartNonFederationComponents("c1");
    expect(mocks.request).toHaveBeenNthCalledWith(1, {
      url: "/clusters/c1/requests",
      method: "POST",
      data: buildComponentCommandPayload(input),
    });
    const restartPayload = mocks.request.mock.calls[1][0].data;
    expect(restartPayload["Requests/resource_filters"][0].hosts_predicate).not.toContain(
      "stale_configs",
    );
  });

  it("loads HDFS workflow capability metadata from the stack service", async () => {
    await federationApi.getStackService("BIG/TOP", "3.2.0", "HDFS");
    expect(mocks.request).toHaveBeenCalledWith({
      url:
        "/stacks/BIG%2FTOP/versions/3.2.0/services/HDFS" +
        "?fields=StackServices/config_types,StackServices/service_type," +
        "components/StackServiceComponents/*",
      method: "GET",
    });
  });
});
