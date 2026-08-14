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
  getConfigValues: vi.fn(),
  getNnCheckPointTime: vi.fn(),
  hide: vi.fn(),
  parseNnCheckPointTime: vi.fn(),
  show: vi.fn(),
}));

vi.mock("../../api/hostsApi", () => ({
  HostsApi: { getNnCheckPointTime: mocks.getNnCheckPointTime },
}));
vi.mock("../../api/configsApi", () => ({
  default: { getConfigValues: mocks.getConfigValues },
}));
vi.mock("../../store/ModalManager", () => ({
  default: { hide: mocks.hide, show: mocks.show },
}));
vi.mock("./utils", async (importOriginal) => ({
  ...await importOriginal<typeof import("./utils")>(),
  parseNnCheckPointTime: mocks.parseNnCheckPointTime,
}));

import RecommendationModal from "../../components/RecommendationModal";
import { addComponent, checkNnLastCheckpointTime } from "./actions";

describe("Hosts actions", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getNnCheckPointTime.mockImplementation(
      (_clusterName: string, hostName: string) => Promise.resolve({ hostName }),
    );
    mocks.parseNnCheckPointTime.mockImplementation(
      (response: { hostName: string }) => Promise.resolve(response.hostName),
    );
    mocks.getConfigValues.mockResolvedValue({
      items: [{
        configurations: [{
          type: "hadoop-env",
          properties: { hdfs_user: "hdfs-service" },
        }],
      }],
    });
  });

  it("checks every NameNode and continues the operation only once", async () => {
    const operation = vi.fn();

    await checkNnLastCheckpointTime(operation, ["nn1", "nn2"], "c1");

    expect(mocks.getNnCheckPointTime.mock.calls).toEqual([
      ["c1", "nn1"],
      ["c1", "nn2"],
    ]);
    expect(mocks.show).toHaveBeenCalledTimes(1);
    const modal = mocks.show.mock.calls[0][0];
    expect(modal.modalBody).toContain("nn1, nn2");
    expect(modal.modalBody).toContain("sudo su hdfs-service");
    expect(operation).not.toHaveBeenCalled();

    await modal.successCallback();
    expect(mocks.hide).toHaveBeenCalledTimes(1);
    expect(operation).toHaveBeenCalledTimes(1);
  });

  it("uses the optional component's service instead of stale caller context", async () => {
    const component = {
      clusterName: "c1",
      componentName: "HBASE_MASTER",
      displayName: "HBase Master",
      hostName: "host1",
      serviceName: "HBASE",
    } as any;

    await addComponent(component, {
      fromServiceSummary: true,
      serviceName: "HDFS",
      validDropDownHosts: ["host1"],
    });

    expect(mocks.show).toHaveBeenCalledTimes(1);
    const modal = mocks.show.mock.calls[0][0];
    expect(modal.type).toBe(RecommendationModal);
    expect(modal.props.serviceName).toBe("HBASE");
    expect(modal.props.componentName).toBe("HBASE_MASTER");
  });
});
