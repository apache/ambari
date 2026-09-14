/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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
  ManagedDependencyBindingSummary,
} from "../../api/serviceDependenciesApi";

export type DirectoryCluster = {
  clusterId: number;
  clusterName: string;
  provisioningState: string;
  version: string;
};

export type ServiceDeploymentRow = {
  clusterId: number;
  clusterName: string;
  dependencyLoadError?: string;
  dependencyLoading?: boolean;
  dependencySummaries?: ManagedDependencyBindingSummary[];
  maintenanceState: string;
  serviceName: string;
  state: string;
};

export const authorizedDirectoryClusters = (
  items: any[],
  canAccessCluster: (clusterName: string) => boolean,
): DirectoryCluster[] => (items || []).flatMap((item) => {
  const cluster = item?.Clusters || {};
  const clusterId = Number(cluster.cluster_id);
  const clusterName = String(cluster.cluster_name || "");
  if (!clusterName || !Number.isInteger(clusterId) || clusterId <= 0
    || !canAccessCluster(clusterName)) {
    return [];
  }
  return [{
    clusterId,
    clusterName,
    provisioningState: String(cluster.provisioning_state || "UNKNOWN"),
    version: String(cluster.version || ""),
  }];
});

export const serviceRowsForCluster = (
  cluster: DirectoryCluster,
  response: any,
): ServiceDeploymentRow[] => (response?.items || []).flatMap((item: any) => {
  const service = item?.ServiceInfo || {};
  const serviceName = String(service.service_name || "");
  if (!serviceName) return [];
  return [{
    clusterId: cluster.clusterId,
    clusterName: cluster.clusterName,
    maintenanceState: String(service.maintenance_state || "OFF"),
    serviceName,
    state: String(service.state || "UNKNOWN"),
  }];
});

export class DirectoryRequestCancelledError extends Error {
  constructor() {
    super("Directory request was cancelled before dispatch");
    this.name = "DirectoryRequestCancelledError";
  }
}

type QueuedDirectoryRequest<T> = {
  operation: () => Promise<T>;
  reject: (reason: unknown) => void;
  resolve: (value: T) => void;
};

export class DirectoryRequestLimiter {
  private active = 0;
  private cancelled = false;
  private readonly queue: QueuedDirectoryRequest<unknown>[] = [];

  constructor(private readonly concurrency: number) {}

  run<T>(operation: () => Promise<T>): Promise<T> {
    if (this.cancelled) return Promise.reject(new DirectoryRequestCancelledError());
    return new Promise<T>((resolve, reject) => {
      this.queue.push({ operation, reject, resolve } as QueuedDirectoryRequest<unknown>);
      this.drain();
    });
  }

  cancelPending() {
    this.cancelled = true;
    this.queue.splice(0).forEach(({ reject }) => reject(new DirectoryRequestCancelledError()));
  }

  private drain() {
    const limit = Math.max(1, this.concurrency);
    while (!this.cancelled && this.active < limit && this.queue.length) {
      const request = this.queue.shift();
      if (!request) return;
      this.active += 1;
      void request.operation()
        .then(request.resolve, request.reject)
        .finally(() => {
          this.active -= 1;
          this.drain();
        });
    }
  }
}

export function safeDirectoryReturnPath(value: unknown): string | null {
  if (typeof value !== "string" || !value.startsWith("/")) return null;
  try {
    const parsed = new URL(value, "http://ambari.local");
    if (parsed.origin !== "http://ambari.local" || parsed.hash
      || (parsed.pathname !== "/clusters" && parsed.pathname !== "/services")) {
      return null;
    }
    return `${parsed.pathname}${parsed.search}`;
  } catch {
    return null;
  }
}

export const serviceRowKey = (row: ServiceDeploymentRow) =>
  `${row.clusterId}:${row.serviceName}`;

export function clusterCreationDraftPhaseKey(phase: string) {
  const normalized = String(phase || "").toUpperCase();
  if (normalized === "START" || normalized === "NAME" || normalized === "VERSION") {
    return "directory.draftPhase.start";
  }
  if (normalized === "HOSTS" || normalized === "HOST_STATUS") {
    return "directory.draftPhase.hosts";
  }
  if (normalized === "SERVICES" || normalized === "MASTERS"
    || normalized === "SLAVES_AND_CLIENTS") {
    return "directory.draftPhase.services";
  }
  if (normalized === "CONFIGURATION") return "directory.draftPhase.configuration";
  if (normalized === "REVIEW") return "directory.draftPhase.review";
  if (normalized.includes("DEPLOY_PREP")) return "directory.draftPhase.preparing";
  if (normalized.includes("INSTALLING") || normalized === "SERVICE_STARTING_3") {
    return "directory.draftPhase.installing";
  }
  if (normalized.includes("INSTALLED")) return "directory.draftPhase.complete";
  return "directory.draftPhase.saved";
}
