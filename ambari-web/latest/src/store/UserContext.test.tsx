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

import { act, render, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { UserContextType } from "../types/auth";

const mocks = vi.hoisted(() => ({
  authenticate: vi.fn(),
  handleSuccessfulLogin: vi.fn(),
  loadAuthorizationsCallback: vi.fn(),
  loadLoginMessage: vi.fn(),
  logout: vi.fn(),
  probeSession: vi.fn(),
}));
vi.mock("../api/loginApi", () => ({ default: mocks }));

import { UserProvider, useUserContext } from "./UserContext";
import { db } from "../Utils/db";

let currentContext: UserContextType;

function ContextReader() {
  currentContext = useUserContext();
  return null;
}

const user = {
  user_name: "operator/name",
  user_id: 1,
  user_type: "LOCAL",
  admin: false,
  operator: true,
  cluster_user: false,
  active: true,
  ldap_user: false,
  principal_type: "USER",
};

describe("user session lifecycle", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    sessionStorage.clear();
    mocks.probeSession.mockRejectedValue({ response: { status: 401 } });
    mocks.loadLoginMessage.mockResolvedValue(null);
  });

  async function renderProvider() {
    render(<UserProvider><ContextReader /></UserProvider>);
    await waitFor(() => expect(currentContext.isLoading).toBe(false));
  }

  it("does not establish a session when authorization loading fails", async () => {
    db.set("Installer", "currentStep", 4);
    mocks.authenticate.mockResolvedValue({});
    mocks.handleSuccessfulLogin.mockResolvedValue({
      data: { Users: user, privileges: [] },
    });
    mocks.loadAuthorizationsCallback.mockRejectedValue(new Error("unavailable"));
    await renderProvider();

    let result = true;
    await act(async () => {
      result = await currentContext.login("operator/name", "secret");
    });

    expect(result).toBe(false);
    expect(currentContext.isAuthenticated).toBe(false);
    expect(db.get("Installer", "currentStep")).toBe(4);
  });

  it("recovers a server-authenticated session from the User header", async () => {
    mocks.probeSession.mockResolvedValue({ headers: { user: "operator/name" } });
    mocks.handleSuccessfulLogin.mockResolvedValue({
      data: { Users: user, privileges: [] },
    });
    mocks.loadAuthorizationsCallback.mockResolvedValue({ data: { items: [] } });
    await renderProvider();

    expect(currentContext.isAuthenticated).toBe(true);
    expect(currentContext.user?.user_name).toBe("operator/name");
    expect(mocks.handleSuccessfulLogin).toHaveBeenCalledWith({
      usr: "",
      loginName: "operator/name",
    });
  });

  it("completes client cleanup when server logout fails", async () => {
    mocks.authenticate.mockResolvedValue({});
    mocks.handleSuccessfulLogin.mockResolvedValue({
      data: { Users: user, privileges: [] },
    });
    mocks.loadAuthorizationsCallback.mockResolvedValue({ data: { items: [] } });
    mocks.logout.mockRejectedValue(new Error("server unavailable"));
    await renderProvider();

    await act(async () => {
      expect(await currentContext.login("operator/name", "secret")).toBe(true);
    });
    db.set("Installer", "currentStep", 4);
    await act(async () => {
      await currentContext.logout();
    });

    expect(currentContext.isAuthenticated).toBe(false);
    expect(currentContext.user).toBeNull();
    expect(db.get("Installer", "currentStep")).toBeUndefined();
  });
});
