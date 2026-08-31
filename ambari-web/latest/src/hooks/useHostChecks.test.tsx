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

import { act, renderHook, waitFor } from "@testing-library/react";
import type { ContextType, PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../store/context";

const mocks = vi.hoisted(() => ({
  getHostsData: vi.fn(),
  getRequestStatus: vi.fn(),
  makeRequest: vi.fn(),
  pausePolling: vi.fn(),
  poll: undefined as undefined | (() => Promise<void>),
  resumePolling: vi.fn(),
}));

vi.mock("../api/hostsApi", () => ({
  HostsApi: {
    getHostsData: mocks.getHostsData,
    getRequestStatus: mocks.getRequestStatus,
    makeRequest: mocks.makeRequest,
  },
}));
vi.mock("./usePolling", () => ({
  default: (poll: () => Promise<void>) => {
    mocks.poll = poll;
    return {
      pausePolling: mocks.pausePolling,
      resumePolling: mocks.resumePolling,
      stopPolling: vi.fn(),
    };
  },
}));

import { useHostChecks } from "./useHostChecks";

const contextValue = {
  ambariProperties: {
    "ambari.java.home": "/opt/ambari-java",
    "ambari.java.version": "17",
    jdk_location: "/resources",
  },
  clusterName: "cluster1",
} as ContextType<typeof AppContext>;

function wrapper({ children }: PropsWithChildren) {
  return <AppContext.Provider value={contextValue}>{children}</AppContext.Provider>;
}

describe("useHostChecks custom JDK validation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.poll = undefined;
    mocks.makeRequest
      .mockResolvedValueOnce({ Requests: { id: 11 } })
      .mockResolvedValueOnce({ Requests: { id: 12 } });
    mocks.getHostsData.mockResolvedValue({
      items: [{
        Hosts: {
          cpu_count: 4,
          disk_info: [],
          host_name: "host1",
          os_family: "redhat7",
          total_mem: 8192,
        },
      }],
    });
  });

  it("runs java_home_check as a separate request and records host warnings", async () => {
    const { result } = renderHook(() => useHostChecks(false, true), { wrapper });

    await act(async () => {
      result.current.startHostCheck([{ name: "host1" }]);
      await Promise.resolve();
    });
    expect(mocks.makeRequest).toHaveBeenCalledTimes(1);

    mocks.getRequestStatus.mockResolvedValueOnce({
      Requests: {
        inputs: "last_agent_env_check,installed_packages,existing_repos,transparentHugePage",
        request_status: "COMPLETED",
      },
      tasks: [{
        Tasks: {
          host_name: "host1",
          status: "COMPLETED",
          structured_out: {
            installed_packages: [],
            last_agent_env_check: {},
            transparentHugePage: { message: "never" },
          },
        },
      }],
    });
    await act(async () => {
      await mocks.poll?.();
    });

    expect(mocks.makeRequest).toHaveBeenLastCalledWith({
      RequestInfo: {
        action: "check_host",
        context: "Check hosts",
        parameters: {
          check_execute_list: "java_home_check",
          java_home: "/opt/ambari-java",
          java_version: "17",
          jdk_location: "/resources",
          threshold: "60",
        },
      },
      "Requests/resource_filters": [{ hosts: "host1" }],
    });

    mocks.getRequestStatus.mockResolvedValueOnce({
      Requests: { inputs: "java_home_check", request_status: "COMPLETED" },
      tasks: [{
        Tasks: {
          host_name: "host1",
          status: "COMPLETED",
          structured_out: { java_home_check: { exit_code: 1 } },
        },
      }],
    });
    await act(async () => {
      await mocks.poll?.();
    });

    await waitFor(() => expect(
      result.current.hostCheckResult.jdkCategoryWarnings[0].hostsNames,
    ).toEqual(["host1"]));
  });
});
