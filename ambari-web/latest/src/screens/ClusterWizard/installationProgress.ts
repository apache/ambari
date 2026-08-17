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

export type InstallWizardName = "clusterCreation" | "addHost" | "addService";
export type InstallationPhase = "INSTALL" | "KEYTABS" | "START" | "COMPLETE";

export const terminalRequestStatuses = new Set([
  "ABORTED",
  "COMPLETED",
  "FAILED",
  "TIMEDOUT",
]);
export const failedRequestStatuses = new Set(["ABORTED", "FAILED", "TIMEDOUT"]);
export const terminalTaskStatuses = new Set([
  "ABORTED",
  "COMPLETED",
  "FAILED",
  "TIMEDOUT",
]);
export const failedTaskStatuses = new Set(["ABORTED", "FAILED", "TIMEDOUT"]);

type RequestId = string | number;

type AmbariTask = {
  Tasks?: {
    id?: RequestId;
    request_id?: RequestId;
    status?: string;
  };
  [key: string]: unknown;
};

type AmbariRequestResponse = {
  Requests?: { id?: RequestId; request_status?: string };
  data?: { Requests?: { id?: RequestId } };
  id?: RequestId;
  tasks?: AmbariTask[];
};

export function requestIdFrom(response: AmbariRequestResponse): RequestId | undefined {
  return response?.Requests?.id ?? response?.data?.Requests?.id ?? response?.id;
}

export function mergeInstallTasks(
  existing: AmbariTask[],
  incoming: AmbariTask[],
): AmbariTask[] {
  const tasks = new Map(existing.map((task) => [
    `${task.Tasks?.request_id || ""}:${task.Tasks?.id}`,
    task,
  ]));
  incoming.forEach((task) => {
    tasks.set(`${task.Tasks?.request_id || ""}:${task.Tasks?.id}`, task);
  });
  return [...tasks.values()];
}

export function requestFinished(response: AmbariRequestResponse): boolean {
  const tasks = response?.tasks || [];
  return terminalRequestStatuses.has(response?.Requests?.request_status || "")
    || (tasks.length > 0 && tasks.every((task) =>
      terminalTaskStatuses.has(task.Tasks?.status || ""),
    ));
}

export function requestFailed(response: AmbariRequestResponse): boolean {
  return failedRequestStatuses.has(response?.Requests?.request_status || "")
    || (response?.tasks || []).some((task) =>
      failedTaskStatuses.has(task.Tasks?.status || ""),
    );
}

export function canRetryInstallation(status: string): boolean {
  return status === "INSTALL FAILED";
}

export function canEnterSummary(wizard: InstallWizardName, status: string): boolean {
  if (["STARTED", "START FAILED", "START_SKIPPED"].includes(status)) {
    return true;
  }
  return wizard !== "clusterCreation" && status === "INSTALL FAILED";
}

export function wizardCheckpoint(
  wizard: InstallWizardName,
  stage: "PREP" | "INSTALLING" | "INSTALLED" | "STARTING",
): string {
  if (stage === "STARTING") {
    return "SERVICE_STARTING_3";
  }
  const prefix = wizard === "clusterCreation"
    ? "CLUSTER"
    : wizard === "addHost"
      ? "ADD_HOSTS"
      : "ADD_SERVICES";
  const suffix = stage === "PREP"
    ? "DEPLOY_PREP_2"
    : stage === "INSTALLING"
      ? "INSTALLING_3"
      : "INSTALLED_4";
  return `${prefix}_${suffix}`;
}
