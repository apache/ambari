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

export type BackgroundRequest = {
  Requests: {
    id: number;
    request_context?: string;
    request_status?: string;
    [key: string]: unknown;
  };
  [key: string]: unknown;
};

export type BackgroundTask = {
  Tasks: {
    id: number;
    host_name?: string;
    request_id?: number;
    status?: string;
    [key: string]: unknown;
  };
};

export const TERMINAL_OPERATION_STATUSES = new Set([
  "ABORTED",
  "COMPLETED",
  "FAILED",
  "SKIPPED_FAILED",
  "TIMEDOUT",
]);

const KNOWN_NON_ABORTABLE_STATUSES = new Set([
  ...TERMINAL_OPERATION_STATUSES,
  "HOLDING",
  "HOLDING_FAILED",
  "HOLDING_TIMEDOUT",
  "PAUSED",
]);

export function requestId(request: BackgroundRequest): number {
  return Number(request?.Requests?.id);
}

export function isUpgradeRequest(request: Partial<BackgroundRequest> & {
  request_context?: string;
  requestContext?: string;
}): boolean {
  const context = request?.Requests?.request_context
    || request?.request_context
    || request?.requestContext;
  return context ? /(upgrading|downgrading)/i.test(context) : false;
}

export function isOperationTerminal(status?: string): boolean {
  return TERMINAL_OPERATION_STATUSES.has(status?.toUpperCase() || "");
}

export function canAbortOperation(
  status: string | undefined,
  hasStartStopPermission: boolean,
  isSubmitting = false,
): boolean {
  if (!hasStartStopPermission || isSubmitting) {
    return false;
  }
  const normalized = status?.toUpperCase() || "";
  return normalized === "IN_PROGRESS" || !KNOWN_NON_ABORTABLE_STATUSES.has(normalized);
}

export function isRequestScheduleRunning(status?: string): boolean {
  return ["SCHEDULED", "IN_PROGRESS"].includes(status?.toUpperCase() || "");
}

type ScheduledBackgroundRequest = {
  Requests?: {
    inputs?: string;
    request_context?: string;
    request_schedule?: { schedule_id?: number | string };
  };
};

export function sourceRequestScheduleId(request: ScheduledBackgroundRequest): number | null {
  const scheduleId = Number(request?.Requests?.request_schedule?.schedule_id);
  if (!Number.isFinite(scheduleId)) return null;

  if (/Recommission/.test(request?.Requests?.request_context || "")) {
    try {
      const inputs = JSON.parse(request?.Requests?.inputs || "{}");
      if (inputs.included_hosts?.split(",").length === 1) return null;
    } catch {
      // Invalid inputs must not hide a valid schedule association.
    }
  }
  return scheduleId;
}

export function statusMatchesFilter(status: string | undefined, filter: string): boolean {
  const normalizedStatus = status?.toUpperCase() || "";
  const normalizedFilter = filter.toUpperCase();
  return normalizedStatus === normalizedFilter
    || (normalizedFilter === "FAILED" && normalizedStatus === "SKIPPED_FAILED")
    || (normalizedFilter === "PENDING" && normalizedStatus === "QUEUED")
    || (normalizedFilter === "SUCCESS" && normalizedStatus === "COMPLETED");
}

export function shouldShowBackgroundOperations(
  showAutomaticPopups: boolean,
  isExplicitClick: boolean,
  isClusterUser: boolean,
): boolean {
  return !isClusterUser && (isExplicitClick || showAutomaticPopups);
}

export function replaceRequestSnapshot(requests: BackgroundRequest[]): BackgroundRequest[] {
  const byId = new Map<number, BackgroundRequest>();
  requests.forEach((request) => {
    const id = requestId(request);
    if (Number.isFinite(id) && !isUpgradeRequest(request)) {
      byId.set(id, request);
    }
  });
  return [...byId.values()].sort((left, right) => requestId(right) - requestId(left));
}

function camelToSnake(value: string): string {
  return value.replace(/([A-Z])/g, "_$1").toLowerCase();
}

export function requestFromSocketMessage(
  message: Record<string, unknown>,
  existing?: BackgroundRequest,
): BackgroundRequest {
  const request = { ...(existing?.Requests || {}) } as BackgroundRequest["Requests"];
  Object.entries(message).forEach(([key, value]) => {
    if (key !== "Tasks" && key !== "destination" && value !== undefined) {
      request[camelToSnake(key)] = value;
    }
  });
  request.id = Number(message.requestId ?? request.id);
  return { ...(existing || {}), Requests: request };
}

export function upsertRequestEvent(
  requests: BackgroundRequest[],
  message: Record<string, unknown>,
  limit: number,
): BackgroundRequest[] {
  const incoming = requestFromSocketMessage(message);
  if (!Number.isFinite(requestId(incoming)) || isUpgradeRequest(incoming)) {
    return requests;
  }
  const existing = requests.find((request) => requestId(request) === requestId(incoming));
  const updated = requestFromSocketMessage(message, existing);
  return [updated, ...requests.filter((request) => requestId(request) !== requestId(updated))]
    .sort((left, right) => requestId(right) - requestId(left))
    .slice(0, limit);
}

function taskFromSocketMessage(task: Record<string, unknown>): BackgroundTask {
  return {
    Tasks: {
      id: Number(task.id),
      host_name: task.hostName as string | undefined,
      request_id: Number(task.requestId),
      status: task.status as string | undefined,
    },
  };
}

export function upsertTaskEvents(
  tasks: BackgroundTask[],
  socketTasks: Record<string, unknown>[],
): BackgroundTask[] {
  const byId = new Map(tasks.map((task) => [Number(task.Tasks.id), task]));
  socketTasks.forEach((socketTask) => {
    const incoming = taskFromSocketMessage(socketTask);
    if (!Number.isFinite(incoming.Tasks.id)) return;
    const existing = byId.get(incoming.Tasks.id);
    byId.set(incoming.Tasks.id, {
      ...(existing || {}),
      Tasks: { ...(existing?.Tasks || {}), ...incoming.Tasks },
    });
  });
  return [...byId.values()].sort((left, right) => left.Tasks.id - right.Tasks.id);
}
