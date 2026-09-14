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
    localStorage.setItem("i18nextLng", "zh");
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
    expect(localStorage.getItem("i18nextLng")).toBe("zh");
  });

  it.each([
    [403, "login.error.invalidCredentials"],
    [500, "login.error.server"],
    [0, "login.error.unavailable"],
  ])("returns a translatable fallback for login status %s", async (status, key) => {
    mocks.authenticate.mockRejectedValue({ response: { status } });
    await renderProvider();

    await act(async () => {
      expect(await currentContext.login("operator", "test-password")).toBe(false);
    });
    expect(currentContext.loginError).toBe(key);
    expect(currentContext.isAuthenticated).toBe(false);
  });

  it("preserves a server-provided login diagnostic", async () => {
    mocks.authenticate.mockRejectedValue({
      response: { status: 403, data: { message: "Server-specific diagnostic" } },
    });
    await renderProvider();

    await act(async () => {
      await currentContext.login("operator", "test-password");
    });
    expect(currentContext.loginError).toBe("Server-specific diagnostic");
  });

  it("identifies only the exact CLUSTER.USER privilege as a cluster user", async () => {
    mocks.authenticate.mockResolvedValue({});
    mocks.loadAuthorizationsCallback.mockResolvedValue({ data: { items: [] } });
    mocks.handleSuccessfulLogin.mockResolvedValue({
      data: {
        Users: user,
        privileges: [{ PrivilegeInfo: { permission_name: "SERVICE.ADMINISTRATOR" } }],
      },
    });
    await renderProvider();

    await act(async () => {
      expect(await currentContext.login("operator/name", "secret")).toBe(true);
    });
    expect(currentContext.isClusterUser()).toBe(false);

    mocks.handleSuccessfulLogin.mockResolvedValue({
      data: {
        Users: user,
        privileges: [{ PrivilegeInfo: { permission_name: "CLUSTER.USER" } }],
      },
    });
    await act(async () => {
      expect(await currentContext.login("operator/name", "secret")).toBe(true);
    });
    expect(currentContext.isClusterUser()).toBe(true);
  });

  it("evaluates task visibility by cluster with administrator precedence", async () => {
    mocks.authenticate.mockResolvedValue({});
    mocks.handleSuccessfulLogin.mockResolvedValue({
      data: {
        Users: user,
        privileges: [
          { PrivilegeInfo: { permission_name: "CLUSTER.USER", type: "CLUSTER", cluster_name: "alpha" } },
          { PrivilegeInfo: { permission_name: "CLUSTER.ADMINISTRATOR", type: "CLUSTER", cluster_name: "beta" } },
          { PrivilegeInfo: { permission_name: "CLUSTER.USER", type: "CLUSTER", cluster_name: "gamma" } },
          { PrivilegeInfo: { permission_name: "SERVICE.ADMINISTRATOR", type: "CLUSTER", cluster_name: "gamma" } },
        ],
      },
    });
    mocks.loadAuthorizationsCallback.mockResolvedValue({
      data: {
        items: ["alpha", "beta", "gamma"].map((clusterName) => ({
          AuthorizationInfo: {
            authorization_id: "CLUSTER.VIEW_STATUS_INFO",
            resource_type: "CLUSTER",
            cluster_name: clusterName,
          },
        })),
      },
    });
    await renderProvider();
    await act(async () => {
      expect(await currentContext.login("operator/name", "secret")).toBe(true);
    });

    expect(currentContext.canViewClusterTasks("alpha")).toBe(false);
    expect(currentContext.canViewClusterTasks("beta")).toBe(true);
    expect(currentContext.canViewClusterTasks("gamma")).toBe(true);

    mocks.handleSuccessfulLogin.mockResolvedValue({
      data: {
        Users: user,
        privileges: [{ PrivilegeInfo: {
          permission_name: "AMBARI.ADMINISTRATOR",
          type: "AMBARI",
        } }],
      },
    });
    mocks.loadAuthorizationsCallback.mockResolvedValue({ data: { items: [] } });
    await act(async () => {
      expect(await currentContext.login("operator/name", "secret")).toBe(true);
    });
    expect(currentContext.canViewClusterTasks("alpha")).toBe(true);
  });

  it("keeps authorization scope when clusters grant the same role IDs", async () => {
    mocks.authenticate.mockResolvedValue({});
    mocks.handleSuccessfulLogin.mockResolvedValue({
      data: {
        Users: user,
        privileges: [{
          PrivilegeInfo: {
            permission_name: "CLUSTER.ADMINISTRATOR",
            permission_label: "Cluster Administrator",
            type: "CLUSTER",
            cluster_name: "alpha",
          },
        }, {
          PrivilegeInfo: {
            permission_name: "AMBARI.VIEW_STATUS",
            permission_label: "View Ambari Status",
            type: "AMBARI",
          },
        }],
      },
    });
    mocks.loadAuthorizationsCallback.mockResolvedValue({
      data: {
        items: [
          { AuthorizationInfo: {
            authorization_id: "CLUSTER.ADMINISTRATOR",
            authorization_name: "Cluster administrator",
            resource_type: "CLUSTER",
            cluster_name: "alpha",
          } },
          { AuthorizationInfo: {
            authorization_id: "AMBARI.MANAGE_SETTINGS",
            authorization_name: "Manage settings",
            resource_type: "AMBARI",
          } },
          { AuthorizationInfo: {
            authorization_id: "CLUSTER.VIEW_METRICS",
            authorization_name: "Unscoped record",
            resource_type: "CLUSTER",
          } },
        ],
      },
    });
    await renderProvider();
    await act(async () => {
      expect(await currentContext.login("operator/name", "secret")).toBe(true);
    });

    expect(currentContext.hasClusterAuthorization("alpha", "CLUSTER.ADMINISTRATOR")).toBe(true);
    expect(currentContext.hasClusterAuthorization("beta", "CLUSTER.ADMINISTRATOR")).toBe(false);
    expect(currentContext.hasClusterAuthorization("alpha", "AMBARI.MANAGE_SETTINGS")).toBe(true);
    expect(currentContext.hasClusterAuthorization("alpha", "CLUSTER.VIEW_METRICS")).toBe(false);
    expect(currentContext.canAccessCluster("alpha")).toBe(true);
    expect(currentContext.canAccessCluster("beta")).toBe(false);
    expect(currentContext.isAdmin()).toBe(false);
    expect(currentContext.isOperator("alpha")).toBe(true);
    expect(currentContext.isOperator("beta")).toBe(false);
    expect(currentContext.hasPrivilege("CLUSTER.ADMINISTRATOR", "alpha")).toBe(true);
    expect(currentContext.hasPrivilege("Cluster Administrator", "alpha")).toBe(false);
    expect(currentContext.hasPrivilege("CLUSTER.ADMINISTRATOR", "beta")).toBe(false);
    expect(currentContext.hasPrivilege("CLUSTER.ADMINISTRATOR")).toBe(false);
    expect(currentContext.hasGlobalPrivilege("AMBARI.VIEW_STATUS")).toBe(true);

    const persistedSession = JSON.parse(db.getItem("ambari") || "{}");
    expect(persistedSession.app.auth[0]).toMatchObject({
      resource_type: "CLUSTER",
      cluster_name: "alpha",
    });
  });
});
