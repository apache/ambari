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
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
const mocks = vi.hoisted(() => ({ services: vi.fn(), releases: vi.fn(), bindings: vi.fn(), capabilities: vi.fn(),
  operations: vi.fn(), upload: vi.fn(), plan: vi.fn(), planServices: vi.fn(), submit: vi.fn(),
  operation: vi.fn(), members: vi.fn(), getPlan: vi.fn(), deployment: vi.fn(), username: "admin" }));
vi.mock("../../api/mpackApi", async importOriginal => ({
  ...await importOriginal<typeof import("../../api/mpackApi")>(), default: mocks,
}));
vi.mock("../../hooks/useAuth", () => ({ useAuth: () => ({ user: { user_name: mocks.username } }) }));
import Mpacks from "./Mpacks";
const id = "11111111-1111-4111-8111-111111111111";
const planId = "22222222-2222-4222-8222-222222222222";
const archive = "a".repeat(64), digest = "b".repeat(64);
const entry = { id: "c".repeat(64), release_id: "example/1.0", archive_digest: archive, service_name: "QUEUE",
  display_name: "Queue", description: "Message service", service_version: "1.0", stack_name: "BASE", stack_version: "1.0",
  required_services: [], enabled: false, client_only: false };
const mutation = { schema_version: 1, action: "ENABLE", archive_digests: [], release_ids: ["example/1.0"],
  bindings: [], activate: true, maintenance: false };
const plan = { schema_version: 1, id: planId, digest, mutation, candidate_snapshot: digest, affected_clusters: [],
  maintenance_required: true, restart_required: false, expires_at: Date.now() + 60000,
  deployment: { stack_name: "BASE", stack_version: "1.0", cluster_id: null, service_names: ["QUEUE"], service_ids: [entry.id] } };
const operation = { schema_version: 1, id, plan_id: planId, plan_digest: digest, generation: 1, phase: "ACCEPTED",
  effective_snapshot: null, updated_at: 1, owner_id: 1, hooks: {}, hook_history: [], error_code: null, error_details: {} };
const mount = (path = "/mpacks") => render(<MemoryRouter initialEntries={[path]}><Mpacks /></MemoryRouter>);
beforeEach(() => {
  vi.clearAllMocks();
  mocks.username = "admin";
  localStorage.clear();
  mocks.services.mockResolvedValue({ schema_version: 1, items: [entry], unavailable: [], destinations: [] });
  mocks.releases.mockResolvedValue([]); mocks.bindings.mockResolvedValue([]); mocks.operations.mockResolvedValue([]);
  mocks.capabilities.mockResolvedValue({ target_stack_versions: [{ stack_name: "BASE", stack_version: "1.0" }] });
  mocks.planServices.mockResolvedValue(plan); mocks.getPlan.mockResolvedValue(plan);
  mocks.submit.mockResolvedValue(operation); mocks.operation.mockResolvedValue(operation); mocks.members.mockResolvedValue([]);
});
async function preview() {
  fireEvent.click(await screen.findByLabelText("Queue 1.0"));
  fireEvent.click(screen.getByRole("button", { name: "Continue With Selected Services" }));
  return screen.findByRole("dialog");
}
describe("mpackstore import and service selection", () => {
  it("groups historical versions and sends only the explicitly chosen provider", async () => {
    const newer = { ...entry, id: "d".repeat(64), release_id: "example/1.1" };
    mocks.services.mockResolvedValue({ schema_version: 1, items: [entry, newer], unavailable: [], destinations: [] });
    mount();
    expect(await screen.findAllByLabelText("Queue 1.0")).toHaveLength(1);
    const version = screen.getByLabelText("Package version") as HTMLSelectElement;
    expect(version.value).toBe(newer.id);
    fireEvent.click(screen.getByLabelText("Queue 1.0"));
    fireEvent.change(version, { target: { value: entry.id } });
    fireEvent.click(screen.getByRole("button", { name: "Continue With Selected Services" }));
    await waitFor(() => expect(mocks.planServices).toHaveBeenCalledWith([entry.id], null, false));
  });

  it("shows a recoverable empty search result and filters incompatible destinations", async () => {
    mocks.services.mockResolvedValue({ schema_version: 1, items: [entry], unavailable: [],
      destinations: [{ cluster_id: 3, cluster_name: "incompatible", stack_name: "OTHER", stack_version: "1.0" }] });
    mount();
    await screen.findByLabelText("Queue 1.0");
    fireEvent.change(screen.getByLabelText("Deploy To"), { target: { value: "3" } });
    expect((screen.getByLabelText("Queue 1.0") as HTMLInputElement).disabled).toBe(true);
    fireEvent.change(screen.getByPlaceholderText("Search services or packages"), { target: { value: "not-found" } });
    expect(screen.getByText("No matching services")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Clear filters" }));
    expect(screen.getByLabelText("Queue 1.0")).toBeTruthy();
  });
  it("restores a verified deployment handoff without waiting for catalog refresh", async () => {
    mocks.services.mockReturnValue(new Promise(() => {}));
    mocks.operation.mockResolvedValue({ ...operation, phase: "SUCCEEDED", effective_snapshot: digest });
    mocks.deployment.mockResolvedValue({ schema_version: 1, operation_id: id, plan_id: planId,
      effective_snapshot: digest, deployment: plan.deployment, cluster_name: null });
    mount("/mpacks?operation=" + id);
    await screen.findByRole("link", { name: "Create cluster" });
    expect(mocks.deployment).toHaveBeenCalledWith(id);
  });

  it("rejects a handoff from a foreign plan even when the operation id matches", async () => {
    mocks.operation.mockResolvedValue({ ...operation, phase: "SUCCEEDED", effective_snapshot: digest });
    mocks.deployment.mockResolvedValue({ schema_version: 1, operation_id: id, plan_id: id,
      effective_snapshot: digest, deployment: plan.deployment, cluster_name: null });
    mount("/mpacks?operation=" + id);
    await screen.findByText("Management pack deployment identity changed");
    expect(screen.queryByRole("link", { name: "Create cluster" })).toBeNull();
  });

  it("does not resume another account's checkpoint or an unowned legacy checkpoint", async () => {
    const checkpoint = JSON.stringify({ schema_version: 1, plan_id: planId, plan_digest: digest, key: id });
    localStorage.setItem("ambari.mpack.submission.user.other", checkpoint);
    localStorage.setItem("ambari.mpack.submission.undefined", checkpoint);
    mount();
    await screen.findByLabelText("Queue 1.0");
    expect(screen.queryByRole("button", { name: "Reconcile operation" })).toBeNull();
    expect(mocks.submit).not.toHaveBeenCalled();
    expect(localStorage.getItem("ambari.mpack.submission.user.other")).toBe(checkpoint);
  });

  it("shows the exact release even when the service has a description", async () => {
    mount();
    await screen.findByText("Message service");
    expect(screen.getByText("example/1.0")).not.toBeNull();
  });
  it("enables an unambiguous unused definition without an extra plan dialog", async () => {
    mocks.planServices.mockResolvedValue({ ...plan, maintenance_required: false });
    mount(); fireEvent.click(await screen.findByLabelText("Queue 1.0"));
    fireEvent.click(screen.getByRole("button", { name: "Continue With Selected Services" }));
    await waitFor(() => expect(mocks.submit).toHaveBeenCalledOnce());
    expect(screen.queryByRole("dialog")).toBeNull();
  });
  it("imports every bundle member without activation or target selection", async () => {
    const member = { archive_digest: archive, name: "foundation", version: "1.0", extensions: [], stacks: [] };
    mocks.upload.mockResolvedValue({ schema_version: 1, bundle: true, archive_digest: digest,
      members: [member, { ...member, archive_digest: digest, name: "services" }] });
    mocks.plan.mockImplementation(async value => ({ ...plan, mutation: value, deployment: null }));
    mount();
    fireEvent.click(screen.getByRole("button", { name: "Import bundle" }));
    fireEvent.change(screen.getByLabelText("Package or bundle archive"), {
      target: { files: [new File(["archive"], "store.tar.gz")] },
    });
    await waitFor(() => expect((screen.getByRole("button", { name: "Import Packages" }) as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByRole("button", { name: "Import Packages" }));
    await waitFor(() => expect(mocks.submit).toHaveBeenCalledOnce());
    expect(mocks.plan).toHaveBeenCalledWith({ schema_version: 1, action: "IMPORT", archive_digests: [archive, digest],
      release_ids: [], bindings: [], activate: false, maintenance: false });
  });

  it("clears a definitively rejected submission and allows a fresh preview", async () => {
    mocks.submit.mockRejectedValue({ response: { status: 409, data: { schema_version: 1,
      error: { code: "STALE_PLAN", message: "Preview expired", details: {} } } } });
    mount(); const dialog = await preview();
    fireEvent.click(within(dialog).getByRole("button", { name: "Confirm change" }));
    await screen.findByText("Preview expired");
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
    expect(localStorage.getItem("ambari.mpack.submission.user.admin")).toBeNull();
    expect((screen.getByRole("button", { name: "Continue With Selected Services" }) as HTMLButtonElement).disabled).toBe(false);
  });

  it("keeps uncertain acceptance recoverable outside the modal and reuses its identity", async () => {
    mocks.submit.mockRejectedValueOnce(new Error("Connection lost")).mockResolvedValueOnce(operation);
    mount(); const dialog = await preview();
    fireEvent.click(within(dialog).getByRole("button", { name: "Confirm change" }));
    await screen.findByText("Connection lost");
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
    expect(localStorage.getItem("ambari.mpack.submission.user.admin")).not.toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Reconcile operation" }));
    await waitFor(() => expect(mocks.submit).toHaveBeenCalledTimes(2));
    expect(mocks.submit.mock.calls[1]).toEqual(mocks.submit.mock.calls[0]);
  });

  it("keeps incompatible services and destinations out of one deployment selection", async () => {
    mocks.services.mockResolvedValue({ schema_version: 1, items: [entry, { ...entry, id: digest,
      service_name: "WAREHOUSE", display_name: "Warehouse", stack_name: "PLATFORM" }], unavailable: [], destinations: [
      { cluster_id: 1, cluster_name: "apps", stack_name: "BASE", stack_version: "1.0" },
      { cluster_id: 2, cluster_name: "platform", stack_name: "PLATFORM", stack_version: "1.0" },
    ] });
    mount(); fireEvent.click(await screen.findByLabelText("Queue 1.0"));
    expect((screen.getByLabelText("Warehouse 1.0") as HTMLInputElement).disabled).toBe(true);
    expect(screen.queryByRole("option", { name: "platform" })).toBeNull();
    fireEvent.change(screen.getByLabelText("Deploy To"), { target: { value: "1" } });
    fireEvent.click(screen.getByRole("button", { name: "Continue With Selected Services" }));
    await waitFor(() => expect(mocks.planServices).toHaveBeenCalledWith([entry.id], 1, false));
  });
});
