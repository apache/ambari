/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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

import { act, fireEvent, render, renderHook, screen, waitFor, cleanup } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { hasPermission, clusterEndpoint } from "../api/clusterManagement";
import useAdminResource from "../hooks/useAdminResource";
import { PermissionEditor } from "../screens/ClusterManagement/ClusterPermissions";

const transport = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn(), delete: vi.fn() }));
vi.mock("../api/configs/axiosConfig", () => ({ adminApi: transport }));
vi.mock("../context/ManagementContext", () => ({ useManagement: () => ({ can: () => true }) }));
afterEach(cleanup);
beforeEach(() => { vi.resetAllMocks(); });

describe("cluster management boundaries", () => {
  it("does not apply cluster A permissions to cluster B or global management", () => {
    const grants = [{ authorization_id: "HOST.ADD_DELETE_HOSTS", resource_type: "CLUSTER", cluster_name: "alpha" }];
    expect(hasPermission(grants, "HOST.ADD_DELETE_HOSTS", "alpha")).toBe(true);
    expect(hasPermission(grants, "HOST.ADD_DELETE_HOSTS", "beta")).toBe(false);
    expect(hasPermission(grants, "HOST.ADD_DELETE_HOSTS")).toBe(false);
    expect(clusterEndpoint("east / prod")).toBe("/clusters/east%20%2F%20prod");
  });

  it("discards a late response from the previously selected cluster", async () => {
    let finishAlpha!: (value: unknown) => void;
    transport.get.mockImplementation((path: string) => path === "/clusters/alpha"
      ? new Promise((resolve) => { finishAlpha = resolve; })
      : Promise.resolve({ data: { Clusters: { cluster_id: 2 } } }));
    const hook = renderHook(({ path }) => useAdminResource<{ Clusters: { cluster_id: number } }>(path), { initialProps: { path: "/clusters/alpha" } });
    hook.rerender({ path: "/clusters/beta" });
    await waitFor(() => expect(hook.result.current.data?.Clusters.cluster_id).toBe(2));
    await act(async () => finishAlpha({ data: { Clusters: { cluster_id: 1 } } }));
    expect(hook.result.current.data?.Clusters.cluster_id).toBe(2);
  });

  it("does not treat a malformed collection as an empty successful result", async () => {
    transport.get.mockResolvedValue({ data: {} });
    const hook = renderHook(() => useAdminResource("/hosts", true));
    await waitFor(() => expect(hook.result.current.error).toBeTruthy());
    expect(hook.result.current.data).toBeUndefined();
  });

  it("reconciles a lost grant response using the exact server grant", async () => {
    let saved = false;
    transport.get.mockImplementation((path: string) => Promise.resolve({ data: { items:
      path.startsWith("/permissions") ? [{ PermissionInfo: { resource_name: "CLUSTER", permission_name: "CLUSTER.USER", permission_label: "Cluster User" } }]
      : path.startsWith("/users") ? [{ Users: { user_name: "operator" } }]
      : saved ? [{ PrivilegeInfo: { privilege_id: 7, principal_type: "USER", principal_name: "operator", permission_name: "CLUSTER.USER" } }] : []
    } }));
    transport.post.mockImplementation(async () => { saved = true; throw new Error("Response lost"); });
    render(<PermissionEditor clusterName="alpha" />);
    await screen.findByRole("option", { name: "operator" });
    fireEvent.change(screen.getByLabelText("User or group"), { target: { value: "operator" } });
    fireEvent.change(screen.getByLabelText("Cluster role"), { target: { value: "CLUSTER.USER" } });
    fireEvent.click(screen.getByRole("button", { name: "Grant role" }));
    expect(await screen.findByText("Grant saved and confirmed by the server.")).toBeTruthy();
    expect(transport.post).toHaveBeenCalledTimes(1);
    expect(transport.post.mock.calls[0][0]).toBe("/clusters/alpha/privileges");
  });
});
