/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";

vi.mock("../store/context.tsx", async () => {
  const { createContext } = await import("react");
  return { AppContext: createContext({}) };
});
vi.mock("../store/ServiceContext.tsx", async () => {
  const { createContext } = await import("react");
  return { ServiceContext: createContext({}) };
});
vi.mock("../api/serviceApi", () => ({ ServiceApi: { getServiceState: vi.fn() } }));
vi.mock("../api/cachedServiceApi", () => ({
  cachedServiceApi: { getServiceComponentData: vi.fn(() => undefined) },
}));

import { AppContext } from "../store/context";
import { ServiceContext } from "../store/ServiceContext";
import { ServiceApi } from "../api/serviceApi";
import { useTrinoConfigUpdater } from "./useTrinoConfigUpdater";

describe("Trino updater initialization", () => {
  it("keeps hook order stable and waits for a native model instead of mutating an imported service", async () => {
    vi.mocked(ServiceApi.getServiceState).mockResolvedValue({
      data: { alerts_summary: { CRITICAL: 0, WARNING: 0 }, ServiceInfo: { state: "STARTED" } },
    } as any);
    const app: any = { services: [], clusterName: "c1", runtimeKey: "c1", parsedSocketMessages: [] };
    const service: any = { allServiceModels: {}, updateRegistry: vi.fn(),
      quickLinksMapWithAPIResponse: new Map(), polledHostComponentsData: { items: [] },
      masterSlaveClientsData: {} };
    const wrapper = ({ children }: { children: ReactNode }) => (
      <AppContext.Provider value={app}><ServiceContext.Provider value={service}>{children}</ServiceContext.Provider></AppContext.Provider>
    );
    const hook = renderHook(() => useTrinoConfigUpdater(), { wrapper });
    app.services = [{ ServiceInfo: { service_name: "TRINO" } }];
    hook.rerender();
    expect(ServiceApi.getServiceState).not.toHaveBeenCalled();
    service.allServiceModels = { trino: { updateConfig: vi.fn(), trinoCoordinators: [] } };
    hook.rerender();
    await waitFor(() => expect(ServiceApi.getServiceState).toHaveBeenCalled());
    app.services = [];
    hook.rerender();
    hook.unmount();
  });
});
