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

import { render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps, ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ClusterApi from "../api/clusterApi";
import LoginApi from "../api/loginApi";
import { AppContext } from "../store/context";
import UserSettingsModal from "./UserSettingsModal";

const policy = vi.hoisted(() => ({ canPersist: false }));

vi.mock("../api/clusterApi", () => ({
  default: {
    getPersistData: vi.fn(),
    postPersistData: vi.fn(),
  },
}));
vi.mock("../api/loginApi", () => ({
  default: { loadPrivileges: vi.fn() },
}));
vi.mock("../hooks/useAuth", () => ({
  useAuth: () => ({
    isAdmin: () => false,
    user: { user_name: "operator" },
  }),
}));
vi.mock("../hooks/useAuthorizationPolicy", () => ({
  default: () => ({ isAuthorized: () => policy.canPersist }),
}));
vi.mock("../Utils/timezone", () => ({
  detectUserTimezone: () => "UTC",
  parseTimezones: () => [{ label: "UTC", value: "UTC" }],
}));
vi.mock("./Modal", () => ({
  default: ({ isOpen, modalBody, options, successCallback }: {
    isOpen: boolean;
    modalBody: ReactNode;
    options: { okButtonDisabled?: boolean };
    successCallback: () => void;
  }) => isOpen ? (
    <div>
      {modalBody}
      <button disabled={options.okButtonDisabled} onClick={successCallback}>SAVE</button>
    </div>
  ) : null,
}));

function renderSettings() {
  const value = {
    syncUserBgPreferences: vi.fn(),
    syncUserTimezone: vi.fn(),
  } as unknown as ComponentProps<typeof AppContext.Provider>["value"];
  return render(
    <AppContext.Provider value={value}>
      <UserSettingsModal isOpen onClose={vi.fn()} />
    </AppContext.Provider>,
  );
}

describe("UserSettingsModal authorization", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    policy.canPersist = false;
    vi.mocked(ClusterApi.getPersistData).mockResolvedValue({});
    vi.mocked(LoginApi.loadPrivileges).mockResolvedValue({
      data: { items: [] },
    } as never);
  });

  it("does not write defaults or enable Save without authorized mutation access", async () => {
    renderSettings();

    expect(await screen.findByText("You do not have permission to persist user settings."))
      .toBeTruthy();
    expect(screen.getByRole("button", { name: "SAVE" }).hasAttribute("disabled")).toBe(true);
    await waitFor(() => expect(ClusterApi.postPersistData).not.toHaveBeenCalled());
  });
});
