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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import type { ContextType, ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AppContext } from "../../../../store/context";

const mocks = vi.hoisted(() => ({
  getHosts: vi.fn(),
  getClusterComponents: vi.fn(),
  postPersistData: vi.fn(),
  hasAuthorization: vi.fn(),
}));

vi.mock("../../../../hooks/useAuth", () => ({
  default: () => ({ hasAuthorization: mocks.hasAuthorization }),
}));
vi.mock("../../../../api/clusterApi", () => ({
  default: {
    postPersistData: mocks.postPersistData,
  },
}));
vi.mock("./rmHaApi", () => ({
  default: {
    getHosts: mocks.getHosts,
    getClusterComponents: mocks.getClusterComponents,
  },
}));
vi.mock("../../../../components/Modal", () => ({
  default: ({
    modalBody,
    onClose,
  }: {
    modalBody: ReactNode;
    onClose: () => void;
  }) => (
    <div>
      {modalBody}
      <button type="button" onClick={onClose}>
        Close outer wizard
      </button>
    </div>
  ),
}));
vi.mock("../../../../components/ConfirmationModal", () => ({
  default: () => null,
}));

import ValidateEnablement from "./ValidateEnablement";

describe("ResourceManager HA top-level close", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.hasAuthorization.mockReturnValue(true);
    mocks.getHosts.mockReturnValue(new Promise(() => undefined));
    mocks.getClusterComponents.mockReturnValue(new Promise(() => undefined));
  });

  afterEach(() => cleanup());

  it("keeps the wizard open and exposes checkpoint cleanup failure", async () => {
    mocks.postPersistData.mockRejectedValue(
      new Error("RM persisted state is unavailable"),
    );
    const contextValue = {
      clusterName: "c1",
      clusterState: {},
      services: [{ ServiceInfo: { service_name: "YARN" } }],
    } as unknown as ContextType<typeof AppContext>;

    render(
      <MemoryRouter>
        <AppContext.Provider value={contextValue}>
          <ValidateEnablement />
        </AppContext.Provider>
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Close outer wizard" }));

    expect((await screen.findByRole("alert")).textContent).toContain(
      "RM persisted state is unavailable",
    );
    expect(screen.getByRole("button", { name: "Close outer wizard" })).toBeTruthy();
    expect(mocks.postPersistData).toHaveBeenCalledOnce();
  });
});
