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
import type { ContextType, PropsWithChildren, ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import adminApi from "../api/adminApi";
import { AppContext } from "../store/context";
import useKDCSessionState from "./useKDCSessionState";

const modalMocks = vi.hoisted(() => ({
  show: vi.fn(),
  hide: vi.fn(),
}));

vi.mock("../store/ModalManager", () => ({
  default: modalMocks,
}));

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
} as ContextType<typeof AppContext>;

function wrapper({ children }: PropsWithChildren) {
  return <AppContext.Provider value={contextValue}>{children}</AppContext.Provider>;
}

function disabledSecurityWrapper({ children }: PropsWithChildren) {
  return (
    <AppContext.Provider
      value={{ ...contextValue, isKerberosEnabled: false }}
    >
      {children}
    </AppContext.Provider>
  );
}

describe("useKDCSessionState", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(adminApi.getSecurityStatus).mockResolvedValue({
      Clusters: { security_type: "KERBEROS" },
    });
  });

  it("reports invalid-KDC credential cancellation to the protected operation", async () => {
    vi.mocked(adminApi.getSecurityType).mockResolvedValue({
      items: [
        {
          configurations: [
            { type: "kerberos-env", properties: { kdc_type: "mit-kdc" } },
          ],
        },
      ],
    });
    vi.mocked(adminApi.getKerberosSessionState).mockResolvedValue({
      Services: { attributes: { kdc_validation_result: "INVALID" } },
    });
    const callback = vi.fn();
    const errorCallback = vi.fn();
    const { result } = renderHook(() => useKDCSessionState(() => {}), { wrapper });

    await result.current.getKDCSessionState(callback, errorCallback);
    const popup = modalMocks.show.mock.calls.at(-1)?.[0] as ReactElement<{
      onCancel: () => void;
    }>;
    popup.props.onCancel();

    expect(callback).not.toHaveBeenCalled();
    expect(errorCallback).toHaveBeenCalledWith(
      expect.objectContaining({ message: "KDC credential entry was cancelled." }),
    );
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
    });
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

  it("can validate the wizard KDC session before cluster security is enabled", async () => {
    vi.mocked(adminApi.getSecurityStatus).mockResolvedValue({
      Clusters: { security_type: "NONE" },
    } as any);
    vi.mocked(adminApi.getSecurityType).mockResolvedValue({
      items: [{
        configurations: [{
          type: "kerberos-env",
          properties: { kdc_type: "mit-kdc" },
        }],
      }],
    } as any);
    vi.mocked(adminApi.getKerberosSessionState).mockResolvedValue({
      Services: { attributes: { kdc_validation_result: "OK" } },
    } as any);
    const callback = vi.fn();
    const { result } = renderHook(() => useKDCSessionState(() => {}), {
      wrapper: disabledSecurityWrapper,
    });

    await result.current.getKDCSessionState(
      callback,
      vi.fn(),
      { forceCheck: true },
    );

    expect(adminApi.getSecurityType).toHaveBeenCalledWith("c1");
    expect(adminApi.getKerberosSessionState).toHaveBeenCalledWith("c1");
    expect(callback).toHaveBeenCalledTimes(1);
  });
});
