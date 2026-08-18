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

export type FailedHostDetails = {
  hosts: string[];
  host_detail: Record<string, Array<{ component?: string; service?: string }>>;
};

export type ServiceCheckFailureSummary = {
  serviceNames: string[];
  hostDetails: FailedHostDetails;
};

function unique(values: string[]): string[] {
  return [...new Set(values.filter(Boolean))];
}

export function skippedServiceCheckNames(response: any): string[] {
  const items = Array.isArray(response?.items) ? response.items : [];
  const lastGroup = items.at(-1);
  const names = (lastGroup?.upgrade_items || []).flatMap((item: any) => (
    (item?.tasks || []).map((task: any) => task?.Tasks?.command_detail)
  )).filter((detail: unknown): detail is string => (
    typeof detail === "string" && detail.startsWith("SERVICE_CHECK ")
  )).map((detail: string) => detail.slice("SERVICE_CHECK ".length));
  return unique(names);
}

export function serviceCheckFailureSummary(response: any): ServiceCheckFailureSummary {
  const failures = response?.tasks?.[0]?.Tasks?.structured_out?.failures;
  const hostDetail = failures?.host_component && typeof failures.host_component === "object"
    ? failures.host_component
    : {};
  return {
    serviceNames: unique(Array.isArray(failures?.service_check) ? failures.service_check : []),
    hostDetails: {
      hosts: Object.keys(hostDetail),
      host_detail: hostDetail,
    },
  };
}

export function slaveComponentFailureDetails(response: any): FailedHostDetails {
  const structured = response?.tasks?.[0]?.Tasks?.structured_out;
  if (!structured || typeof structured !== "object") {
    return { hosts: [], host_detail: {} };
  }
  return {
    hosts: Array.isArray(structured.hosts) ? structured.hosts : [],
    host_detail: structured.host_detail && typeof structured.host_detail === "object"
      ? structured.host_detail
      : {},
  };
}

export function isTerminalUpgradeStatus(status?: string): boolean {
  return ["COMPLETED", "FAILED", "TIMEDOUT"].includes(status || "");
}

export function isRunningUpgradeStatus(status?: string): boolean {
  return status === "IN_PROGRESS"
    || status === "PENDING"
    || Boolean(status?.includes("HOLDING"));
}

export function hasFinishedUpgradeHistory(items: any[]): boolean {
  const upgrades = [...items]
    .filter((item) => item?.Upgrade)
    .sort((left, right) => (
      Number(left.Upgrade.request_id || 0) - Number(right.Upgrade.request_id || 0)
    ));
  const latest = upgrades.pop();
  if (!latest) return false;

  const latestStatus = latest.Upgrade.request_status;
  const latestIsActive = latestStatus === "ABORTED" || isRunningUpgradeStatus(latestStatus);
  if (!latestIsActive) return true;

  return upgrades.some((item) => !isRunningUpgradeStatus(item.Upgrade.request_status));
}

export async function waitForUpgradeStatus(
  fetchStatus: () => Promise<string | undefined>,
  expectedStatus: string,
  options: { attempts?: number; intervalMs?: number; signal?: AbortSignal } = {},
): Promise<void> {
  const attempts = options.attempts ?? 60;
  const intervalMs = options.intervalMs ?? 1000;
  for (let attempt = 0; attempt < attempts; attempt += 1) {
    if (options.signal?.aborted) throw new DOMException("Upgrade status wait was cancelled", "AbortError");
    if (await fetchStatus() === expectedStatus) return;
    if (attempt < attempts - 1) {
      await new Promise<void>((resolve, reject) => {
        const timeout = setTimeout(resolve, intervalMs);
        options.signal?.addEventListener("abort", () => {
          clearTimeout(timeout);
          reject(new DOMException("Upgrade status wait was cancelled", "AbortError"));
        }, { once: true });
      });
    }
  }
  throw new Error(`Upgrade did not reach ${expectedStatus} before the wait timed out`);
}
