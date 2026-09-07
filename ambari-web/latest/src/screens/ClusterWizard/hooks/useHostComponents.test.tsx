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
import { ComponentProps, PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../store/context";

const mocks = vi.hoisted(() => ({
  getServices: vi.fn(),
  getHostComponents: vi.fn(),
}));

vi.mock("../../../api/chooseServicesApi", () => ({
  ChooseServicesApi: { getServices: mocks.getServices },
}));
vi.mock("../../../api/hostsApi", () => ({
  HostsApi: {
    getMasterSlaveClusterComponentsByComponentName: mocks.getHostComponents,
  },
}));

import useHostComponents from "./useHostComponents";

function wrapper({ children }: PropsWithChildren) {
  return (
    <AppContext.Provider
      value={
        {
          clusterName: "c1",
          cluster: { stack: "BIGTOP", versionNum: "3.2.0" },
        } as unknown as ComponentProps<typeof AppContext.Provider>["value"]
      }
    >
      {children}
    </AppContext.Provider>
  );
}

describe("useHostComponents recovery", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getServices.mockResolvedValue({ items: [] });
    mocks.getHostComponents.mockResolvedValue({ items: [] });
  });

  it("settles and retries a stack-service read failure", async () => {
    mocks.getServices.mockRejectedValueOnce(new Error("stack services failed"));
    const { result } = renderHook(() => useHostComponents(["HDFS"]), { wrapper });

    await waitFor(() => expect(result.current.error).toBe("stack services failed"));
    expect(result.current.isLoading).toBe(false);
    expect(mocks.getHostComponents).not.toHaveBeenCalled();

    act(() => result.current.retry());
    await waitFor(() => expect(mocks.getServices).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.error).toBe("");
  });

  it("settles and retries a host-component read failure", async () => {
    mocks.getHostComponents.mockRejectedValueOnce(
      new Error("host components failed"),
    );
    const { result } = renderHook(() => useHostComponents(["HDFS"]), { wrapper });

    await waitFor(() => expect(result.current.error).toBe("host components failed"));
    expect(result.current.isLoading).toBe(false);

    act(() => result.current.retry());
    await waitFor(() => expect(mocks.getHostComponents).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.error).toBe("");
  });

  it("does not request JMX fields for enablement validation", async () => {
    const { result } = renderHook(() => useHostComponents(["HDFS", "HBASE"]), {
      wrapper,
    });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    const fields = mocks.getHostComponents.mock.calls[0][2] as string;

    expect(fields).not.toContain("host_components/metrics");
  });
});
