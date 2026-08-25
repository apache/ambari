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

import { fireEvent, render, screen } from "@testing-library/react";
import type { ComponentProps, ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ClusterApi from "../../api/clusterApi";
import { AppContext } from "../../store/context";
import Experimental from ".";

const policy = vi.hoisted(() => ({ canPersist: false }));

vi.mock("../../api/clusterApi", () => ({
  default: { postPersistData: vi.fn() },
}));
vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({ user: { user_name: "operator" } }),
}));
vi.mock("../../hooks/useAuthorizationPolicy", () => ({
  default: () => ({ isAuthorized: () => policy.canPersist }),
}));
vi.mock("../../Utils/db", () => ({
  db: { cleanUp: vi.fn() },
}));
vi.mock("../../components/Modal", () => ({
  default: ({ isOpen, modalBody, successCallback }: {
    isOpen: boolean;
    modalBody: ReactNode;
    successCallback: () => void;
  }) => isOpen ? (
    <div>
      {modalBody}
      <button onClick={successCallback}>Yes</button>
    </div>
  ) : null,
}));

function renderExperimental() {
  const value = {
    setSupports: vi.fn(),
    supports: { enableAddDeleteServices: true },
  } as unknown as ComponentProps<typeof AppContext.Provider>["value"];
  return render(
    <AppContext.Provider value={value}>
      <MemoryRouter>
        <Experimental />
      </MemoryRouter>
    </AppContext.Provider>,
  );
}

describe("Experimental authorization", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    policy.canPersist = false;
  });

  it("blocks both support-flag Save and Reset without authorized mutation access", () => {
    renderExperimental();

    const save = screen.getByRole("button", { name: "Save" });
    expect(save.hasAttribute("disabled")).toBe(true);
    expect(screen.queryByRole("button", { name: "Reset UI States" })).toBeNull();
    fireEvent.click(save);
    expect(ClusterApi.postPersistData).not.toHaveBeenCalled();
  });
});
