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

import type {
  AlertDefinitionReference,
  AlertGroupItem,
  AlertTarget,
} from "../screens/Alerts/types";

export interface AlertGroupPayload {
  AlertGroup: {
    name: string;
    definitions: number[];
    targets: number[];
  };
}

export interface AlertGroupSavePlan {
  create: AlertGroupItem[];
  update: AlertGroupItem[];
  delete: AlertGroupItem[];
}

export type AlertGroupOperationKind = "create" | "update" | "delete";

export interface AlertGroupSaveFailure {
  kind: AlertGroupOperationKind;
  groupId: number;
  groupName: string;
  reason: unknown;
}

export interface AlertGroupSaveResult {
  successful: Record<AlertGroupOperationKind, number>;
  failures: AlertGroupSaveFailure[];
}

export interface AlertGroupServerReconciliation {
  groups: AlertGroupItem[];
  failures: AlertGroupSaveFailure[];
}

export interface AlertGroupSaveOperations {
  create: (group: AlertGroupItem, payload: AlertGroupPayload) => Promise<unknown>;
  update: (group: AlertGroupItem, payload: AlertGroupPayload) => Promise<unknown>;
  delete: (group: AlertGroupItem) => Promise<unknown>;
}

export function validateAlertGroupName(name: string): string | null {
  const normalized = name.trim();
  if (!normalized) return "Group name cannot be empty";
  if (!/^[\s0-9a-z_-]+$/i.test(normalized)) {
    return "Group name contains invalid characters";
  }
  return null;
}

function numericIds(values: Array<AlertDefinitionReference | AlertTarget> | undefined): number[] {
  if (!values) {
    return [];
  }

  return values.flatMap((value) => {
    const candidate = typeof value === "number" ? value : value?.id;
    const id = Number(candidate);
    return Number.isFinite(id) && id > 0 ? [id] : [];
  });
}

export function buildAlertGroupPayload(group: AlertGroupItem): AlertGroupPayload {
  return {
    AlertGroup: {
      name: group.AlertGroup.name.trim(),
      definitions: numericIds(group.AlertGroup.definitions),
      targets: numericIds(group.AlertGroup.targets),
    },
  };
}

export function planAlertGroupSave(groups: AlertGroupItem[]): AlertGroupSavePlan {
  return groups.reduce<AlertGroupSavePlan>((plan, group) => {
    if (group.AlertGroup._deleted) {
      if (group.AlertGroup.id > 0) {
        plan.delete.push(group);
      }
    } else if (group.AlertGroup._isNew) {
      plan.create.push(group);
    } else if (group.AlertGroup._isModified && group.AlertGroup.id > 0) {
      plan.update.push(group);
    }
    return plan;
  }, { create: [], update: [], delete: [] });
}

export async function executeAlertGroupSave(
  plan: AlertGroupSavePlan,
  operations: AlertGroupSaveOperations,
): Promise<AlertGroupSaveResult> {
  const successful: AlertGroupSaveResult["successful"] = {
    create: 0,
    update: 0,
    delete: 0,
  };
  const failures: AlertGroupSaveFailure[] = [];

  const settle = async (
    kind: AlertGroupOperationKind,
    groups: AlertGroupItem[],
    operation: (group: AlertGroupItem) => Promise<unknown>,
  ) => {
    const results = await Promise.allSettled(groups.map(operation));
    results.forEach((result, index) => {
      const group = groups[index];
      if (result.status === "fulfilled") {
        successful[kind] += 1;
      } else {
        failures.push({
          kind,
          groupId: group.AlertGroup.id,
          groupName: group.AlertGroup.name,
          reason: result.reason,
        });
      }
    });
  };

  await settle("delete", plan.delete, operations.delete);

  await Promise.all([
    settle("update", plan.update, (group) => operations.update(group, buildAlertGroupPayload(group))),
    settle("create", plan.create, (group) => operations.create(group, buildAlertGroupPayload(group))),
  ]);

  return { successful, failures };
}

export function reconcileAlertGroupSave(
  groups: AlertGroupItem[],
  failures: AlertGroupSaveFailure[],
): AlertGroupItem[] {
  const failed = new Set(failures.map((failure) => `${failure.kind}:${failure.groupId}`));
  return groups.flatMap((group) => {
    const id = group.AlertGroup.id;
    if (group.AlertGroup._isNew) {
      return failed.has(`create:${id}`) ? [group] : [];
    }
    if (group.AlertGroup._deleted) {
      return failed.has(`delete:${id}`) ? [group] : [];
    }
    if (group.AlertGroup._isModified && !failed.has(`update:${id}`)) {
      return [{
        ...group,
        AlertGroup: { ...group.AlertGroup, _isModified: false },
      }];
    }
    return [group];
  });
}

function sameIds(left: number[], right: number[]): boolean {
  if (left.length !== right.length) return false;
  const sortedLeft = [...left].sort((a, b) => a - b);
  const sortedRight = [...right].sort((a, b) => a - b);
  return sortedLeft.every((id, index) => id === sortedRight[index]);
}

function hasDesiredState(serverGroup: AlertGroupItem, localGroup: AlertGroupItem): boolean {
  const server = buildAlertGroupPayload(serverGroup).AlertGroup;
  const local = buildAlertGroupPayload(localGroup).AlertGroup;
  return server.name === local.name &&
    sameIds(server.definitions, local.definitions) &&
    sameIds(server.targets, local.targets);
}

export function reconcileAlertGroupSaveWithServer(
  serverGroups: AlertGroupItem[],
  localGroups: AlertGroupItem[],
  failures: AlertGroupSaveFailure[],
): AlertGroupServerReconciliation {
  const unresolved = new Set(failures);
  const failedUpdates = new Map<number, { failure: AlertGroupSaveFailure; group?: AlertGroupItem }>();
  const failedDeletes = new Map<number, { failure: AlertGroupSaveFailure; group?: AlertGroupItem }>();

  failures.forEach((failure) => {
    const group = localGroups.find((candidate) => candidate.AlertGroup.id === failure.groupId);
    if (failure.kind === "update") failedUpdates.set(failure.groupId, { failure, group });
    if (failure.kind === "delete") failedDeletes.set(failure.groupId, { failure, group });
  });

  const groups = serverGroups.map((serverGroup) => {
    const id = serverGroup.AlertGroup.id;
    const failedUpdate = failedUpdates.get(id);
    if (failedUpdate?.group) {
      if (hasDesiredState(serverGroup, failedUpdate.group)) {
        unresolved.delete(failedUpdate.failure);
        return serverGroup;
      }
      return failedUpdate.group;
    }

    const failedDelete = failedDeletes.get(id);
    if (failedDelete?.group) return failedDelete.group;
    return serverGroup;
  });

  failedDeletes.forEach(({ failure }) => {
    if (!serverGroups.some((group) => group.AlertGroup.id === failure.groupId)) {
      unresolved.delete(failure);
    }
  });

  failures.filter((failure) => failure.kind === "create").forEach((failure) => {
    const localGroup = localGroups.find((group) => group.AlertGroup.id === failure.groupId);
    if (!localGroup) return;
    if (serverGroups.some((serverGroup) => hasDesiredState(serverGroup, localGroup))) {
      unresolved.delete(failure);
    } else {
      groups.push(localGroup);
    }
  });

  return { groups, failures: [...unresolved] };
}
