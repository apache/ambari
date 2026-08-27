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

import { renderHook, waitFor } from "@testing-library/react";
import type { ContextType, PropsWithChildren } from "react";
import { describe, expect, it, vi } from "vitest";
import Host from "../models/host";
import { AppContext } from "../store/context";

const mocks = vi.hoisted(() => ({
  getHostsList: vi.fn(),
}));

vi.mock("../api/hostsApi", () => ({
  HostsApi: {
    getHostsList: mocks.getHostsList,
  },
}));
vi.mock("./usePolling", () => ({
  default: vi.fn(),
}));

import { useHostConfigUpdater } from "./useHostConfigUpdater";

function wrapperWithMessage(
  message: Record<string, unknown>,
  overrides: Record<string, unknown> = {},
) {
  const contextValue = {
    cluster: {},
    clusterName: "",
    parsedSocketMessages: [message],
    serviceComponentInfo: {},
    supports: { displayOlderVersions: false },
    ...overrides,
  } as unknown as ContextType<typeof AppContext>;

  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <AppContext.Provider value={contextValue}>
        {children}
      </AppContext.Provider>
    );
  };
}

describe("useHostConfigUpdater realtime initialization", () => {
  it("ignores host events until the initial REST models are available", () => {
    const setAllHostModels = vi.fn();

    renderHook(
      () => useHostConfigUpdater({}, [], setAllHostModels),
      {
        wrapper: wrapperWithMessage({
          destination: "/events/hosts",
          host_name: "host1",
          host_state: "HEARTBEAT_LOST",
        }),
      },
    );

    expect(setAllHostModels).not.toHaveBeenCalled();
  });

  it("applies host events after the initial REST models are available", async () => {
    const host = new Host({} as Host);
    host.hostName = "host1";
    host.state = "HEALTHY";
    const setAllHostModels = vi.fn();

    renderHook(
      () => useHostConfigUpdater({}, [host], setAllHostModels),
      {
        wrapper: wrapperWithMessage({
          destination: "/events/hosts",
          host_name: "host1",
          host_state: "HEARTBEAT_LOST",
        }),
      },
    );

    await waitFor(() => expect(setAllHostModels).toHaveBeenCalled());
    expect(setAllHostModels.mock.calls[0][0][0].state).toBe("HEARTBEAT_LOST");
  });

  it("does not report a loaded host as an empty REST result", async () => {
    mocks.getHostsList.mockResolvedValueOnce({
      items: [{
        Hosts: { host_name: "host1" },
        host_components: [],
        stack_versions: [],
      }],
    });
    const setAllHostModels = vi.fn();
    const queryParams = {
      RequestInfo: { query: "Hosts/host_name.in(host1)" },
    };

    const { result } = renderHook(
      () => useHostConfigUpdater(
        queryParams,
        [],
        setAllHostModels,
      ),
      {
        wrapper: wrapperWithMessage({}, {
          clusterName: "cluster1",
          parsedSocketMessages: [],
          serviceComponentInfo: { items: [] },
        }),
      },
    );

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.isEmptyResult).toBe(false);
    expect(setAllHostModels).toHaveBeenCalled();
  });
});
