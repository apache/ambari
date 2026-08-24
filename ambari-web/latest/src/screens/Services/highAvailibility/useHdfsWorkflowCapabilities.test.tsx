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

const mocks = vi.hoisted(() => ({ getStackService: vi.fn() }));

vi.mock("../../../api/federationApi", () => ({
  default: { getStackService: mocks.getStackService },
}));

import useHdfsWorkflowCapabilities, {
  evaluateHdfsWorkflowCapabilities,
} from "./useHdfsWorkflowCapabilities";

const supportedStack = {
  StackServices: {
    service_type: "HDFS",
    config_types: {
      "core-site": {},
      "hdfs-site": {},
      "hdfs-rbf-site": {},
    },
  },
  components: ["NAMENODE", "JOURNALNODE", "ZKFC", "ROUTER"].map(
    (name) => ({ StackServiceComponents: { component_name: name } }),
  ),
};

function wrapper(versionNum: string) {
  return function ContextWrapper({ children }: PropsWithChildren) {
    return (
      <AppContext.Provider
        value={
          { cluster: { stack: "BIGTOP", versionNum } } as unknown as ComponentProps<
            typeof AppContext.Provider
          >["value"]
        }
      >
        {children}
      </AppContext.Provider>
    );
  };
}

describe("HDFS workflow stack capabilities", () => {
  beforeEach(() => {
    mocks.getStackService.mockReset();
  });

  it("requires authoritative service type, config types, and stack components", () => {
    expect(evaluateHdfsWorkflowCapabilities(supportedStack)).toEqual({
      nameNodeFederation: true,
      routerFederation: true,
    });
    expect(
      evaluateHdfsWorkflowCapabilities({
        ...supportedStack,
        StackServices: {
          ...supportedStack.StackServices,
          service_type: "OTHER_FS",
        },
      }),
    ).toEqual({ nameNodeFederation: false, routerFederation: false });
    expect(
      evaluateHdfsWorkflowCapabilities({
        ...supportedStack,
        components: supportedStack.components.slice(0, 3),
      }),
    ).toEqual({ nameNodeFederation: true, routerFederation: false });
  });

  it("deduplicates concurrent stack metadata reads by stack version", async () => {
    let resolveRequest: (value: unknown) => void = () => undefined;
    mocks.getStackService.mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = resolve;
      }),
    );
    const first = renderHook(() => useHdfsWorkflowCapabilities(), {
      wrapper: wrapper("dedupe"),
    });
    const second = renderHook(() => useHdfsWorkflowCapabilities(), {
      wrapper: wrapper("dedupe"),
    });

    await waitFor(() => expect(mocks.getStackService).toHaveBeenCalledOnce());
    act(() => resolveRequest(supportedStack));
    await waitFor(() => {
      expect(first.result.current.capabilities.routerFederation).toBe(true);
      expect(second.result.current.capabilities.routerFederation).toBe(true);
    });
  });

  it("fails closed and retries a failed metadata read", async () => {
    mocks.getStackService
      .mockRejectedValueOnce(new Error("stack metadata failed"))
      .mockResolvedValueOnce(supportedStack);
    const first = renderHook(() => useHdfsWorkflowCapabilities(), {
      wrapper: wrapper("retry"),
    });
    const second = renderHook(() => useHdfsWorkflowCapabilities(), {
      wrapper: wrapper("retry"),
    });

    await waitFor(() =>
      expect(first.result.current.error).toBe("stack metadata failed"),
    );
    expect(second.result.current.capabilities.routerFederation).toBe(false);

    act(() => first.result.current.retry());
    await waitFor(() => {
      expect(first.result.current.capabilities.routerFederation).toBe(true);
      expect(second.result.current.capabilities.routerFederation).toBe(true);
    });
    expect(mocks.getStackService).toHaveBeenCalledTimes(2);
  });
});
