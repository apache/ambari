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

import { act, renderHook } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import adminApi from "../api/adminApi";
import { AppContext } from "../store/context";
import useKDCSessionState from "./useKDCSessionState";

vi.mock("../api/adminApi", () => ({
  default: {
    getKerberosSessionState: vi.fn(),
    getSecurityStatus: vi.fn(),
    getSecurityType: vi.fn(),
  },
}));

const contextValue = {
  clusterName: "c1",
  isKerberosEnabled: true,
} as any;

function wrapper({ children }: PropsWithChildren) {
  return <AppContext.Provider value={contextValue}>{children}</AppContext.Provider>;
}

describe("useKDCSessionState", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(adminApi.getSecurityStatus).mockResolvedValue({
      Clusters: { security_type: "KERBEROS" },
    } as any);
  });

  it("reports a security-type request failure instead of leaving the caller waiting", async () => {
    const requestError = new Error("security type unavailable");
    vi.mocked(adminApi.getSecurityType).mockRejectedValue(requestError);
    const callback = vi.fn();
    const errorCallback = vi.fn();
    const { result } = renderHook(() => useKDCSessionState(() => {}), { wrapper });

    await result.current.getKDCSessionState(callback, errorCallback);

    expect(callback).not.toHaveBeenCalled();
    expect(errorCallback).toHaveBeenCalledWith(requestError);
  });

  it("waits for the protected operation when manual Kerberos needs no session check", async () => {
    vi.mocked(adminApi.getSecurityType).mockResolvedValue({
      items: [{
        configurations: [{
          type: "kerberos-env",
          properties: { kdc_type: "none" },
        }],
      }],
    } as any);
    let operationCompleted = false;
    const { result } = renderHook(() => useKDCSessionState(() => {}), { wrapper });

    await act(async () => {
      await result.current.getKDCSessionState(async () => {
        await Promise.resolve();
        operationCompleted = true;
      });
    });

    expect(operationCompleted).toBe(true);
    expect(adminApi.getKerberosSessionState).not.toHaveBeenCalled();
  });
});
