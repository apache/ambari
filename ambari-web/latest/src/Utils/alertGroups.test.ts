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

import { describe, expect, it, vi } from "vitest";
import {
  buildAlertGroupPayload,
  executeAlertGroupSave,
  planAlertGroupSave,
  reconcileAlertGroupSave,
  reconcileAlertGroupSaveWithServer,
  validateAlertGroupName,
} from "./alertGroups";
import type { AlertGroupItem } from "../screens/Alerts/types";

const group = (
  id: number,
  flags: Partial<AlertGroupItem["AlertGroup"]> = {},
): AlertGroupItem => ({
  AlertGroup: {
    id,
    name: `group-${id}`,
    cluster_name: "c1",
    default: false,
    definitions: [],
    targets: [],
    ...flags,
  },
});

describe("alert group contracts", () => {
  it("applies the Classic Alert Group name rules", () => {
    expect(validateAlertGroupName("  operators_1  ")).toBeNull();
    expect(validateAlertGroupName("   ")).toBe("Group name cannot be empty");
    expect(validateAlertGroupName("operators/group")).toBe("Group name contains invalid characters");
  });

  it("builds a complete replacement payload with numeric definitions and targets", () => {
    expect(buildAlertGroupPayload(group(4, {
      name: "  operators  ",
      definitions: [1, { id: 2 }, { id: -1 }],
      targets: [{ id: 7, name: "mail", notification_type: "EMAIL" }, { id: 8, name: "snmp", notification_type: "AMBARI_SNMP" }],
    }))).toEqual({
      AlertGroup: {
        name: "operators",
        definitions: [1, 2],
        targets: [7, 8],
      },
    });
  });

  it("classifies only server mutations", () => {
    const plan = planAlertGroupSave([
      group(-1, { _isNew: true }),
      group(2, { _isModified: true }),
      group(3, { _deleted: true }),
      group(-4, { _deleted: true }),
      group(5),
    ]);

    expect(plan.create.map((item) => item.AlertGroup.id)).toEqual([-1]);
    expect(plan.update.map((item) => item.AlertGroup.id)).toEqual([2]);
    expect(plan.delete.map((item) => item.AlertGroup.id)).toEqual([3]);
  });

  it("settles every delete before starting concurrent updates and creates", async () => {
    let releaseDeletes!: () => void;
    const deleteBarrier = new Promise<void>((resolve) => { releaseDeletes = resolve; });
    const calls: string[] = [];
    const operations = {
      delete: vi.fn(async (item: AlertGroupItem) => {
        calls.push(`delete-${item.AlertGroup.id}`);
        await deleteBarrier;
      }),
      update: vi.fn(async (item: AlertGroupItem) => { calls.push(`update-${item.AlertGroup.id}`); }),
      create: vi.fn(async (item: AlertGroupItem) => { calls.push(`create-${item.AlertGroup.id}`); }),
    };

    const resultPromise = executeAlertGroupSave({
      delete: [group(1), group(2)],
      update: [group(3)],
      create: [group(-4)],
    }, operations);
    await Promise.resolve();
    expect(calls).toEqual(["delete-1", "delete-2"]);

    releaseDeletes();
    const result = await resultPromise;
    expect(calls.slice(2).sort()).toEqual(["create--4", "update-3"]);
    expect(result).toEqual({
      successful: { create: 1, update: 1, delete: 2 },
      failures: [],
    });
  });

  it("continues the second phase and aggregates rejected operations", async () => {
    const failure = new Error("rejected");
    const result = await executeAlertGroupSave({
      delete: [group(1)],
      update: [group(2)],
      create: [group(-3)],
    }, {
      delete: vi.fn().mockRejectedValue(failure),
      update: vi.fn().mockResolvedValue(undefined),
      create: vi.fn().mockRejectedValue(failure),
    });

    expect(result.successful).toEqual({ create: 0, update: 1, delete: 0 });
    expect(result.failures.map(({ kind, groupId }) => ({ kind, groupId }))).toEqual([
      { kind: "delete", groupId: 1 },
      { kind: "create", groupId: -3 },
    ]);
  });

  it("retains only failed pending operations when a server refresh is unavailable", () => {
    const reconciled = reconcileAlertGroupSave([
      group(-1, { _isNew: true }),
      group(-2, { _isNew: true }),
      group(3, { _isModified: true }),
      group(4, { _deleted: true }),
    ], [
      { kind: "create", groupId: -2, groupName: "group--2", reason: new Error("failed") },
      { kind: "delete", groupId: 4, groupName: "group-4", reason: new Error("failed") },
    ]);

    expect(reconciled.map((item) => item.AlertGroup.id)).toEqual([-2, 3, 4]);
    expect(reconciled.find((item) => item.AlertGroup.id === 3)?.AlertGroup._isModified).toBe(false);
  });

  it("uses refreshed server state to resolve ambiguous request failures", () => {
    const failedCreate = group(-1, {
      name: "created",
      definitions: [{ id: 10 }],
      targets: [{ id: 20, name: "mail", notification_type: "EMAIL" }],
      _isNew: true,
    });
    const failedUpdate = group(2, {
      name: "updated",
      definitions: [{ id: 11 }],
      targets: [],
      _isModified: true,
    });
    const failedDelete = group(3, { _deleted: true });
    const failures = [
      { kind: "create" as const, groupId: -1, groupName: "created", reason: new Error("timeout") },
      { kind: "update" as const, groupId: 2, groupName: "updated", reason: new Error("timeout") },
      { kind: "delete" as const, groupId: 3, groupName: "group-3", reason: new Error("timeout") },
    ];

    const reconciled = reconcileAlertGroupSaveWithServer([
      group(7, {
        name: "created",
        definitions: [{ id: 10 }],
        targets: [{ id: 20, name: "mail", notification_type: "EMAIL" }],
      }),
      group(2, { name: "updated", definitions: [{ id: 11 }], targets: [] }),
    ], [failedCreate, failedUpdate, failedDelete], failures);

    expect(reconciled.failures).toEqual([]);
    expect(reconciled.groups.map((item) => item.AlertGroup.id)).toEqual([7, 2]);
    expect(reconciled.groups.some((item) => item.AlertGroup._isNew || item.AlertGroup._deleted)).toBe(false);
  });

  it("keeps only failures that refreshed server state proves are still pending", () => {
    const failedCreate = group(-1, { name: "created", definitions: [{ id: 10 }], _isNew: true });
    const failedUpdate = group(2, { name: "updated", definitions: [{ id: 11 }], _isModified: true });
    const failedDelete = group(3, { _deleted: true });
    const failures = [
      { kind: "create" as const, groupId: -1, groupName: "created", reason: new Error("failed") },
      { kind: "update" as const, groupId: 2, groupName: "updated", reason: new Error("failed") },
      { kind: "delete" as const, groupId: 3, groupName: "group-3", reason: new Error("failed") },
    ];

    const reconciled = reconcileAlertGroupSaveWithServer([
      group(2, { name: "old", definitions: [] }),
      group(3),
    ], [failedCreate, failedUpdate, failedDelete], failures);

    expect(reconciled.failures).toEqual(failures);
    expect(reconciled.groups.map((item) => item.AlertGroup.id)).toEqual([2, 3, -1]);
    expect(reconciled.groups[0].AlertGroup._isModified).toBe(true);
    expect(reconciled.groups[1].AlertGroup._deleted).toBe(true);
  });
});
