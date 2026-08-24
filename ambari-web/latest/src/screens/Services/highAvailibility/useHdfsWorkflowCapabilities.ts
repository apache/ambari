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

import { useContext, useEffect, useState } from "react";
import { isAxiosError } from "axios";
import federationApi from "../../../api/federationApi";
import { AppContext } from "../../../store/context";

export interface HdfsWorkflowCapabilities {
  nameNodeFederation: boolean;
  routerFederation: boolean;
}

const emptyCapabilities: HdfsWorkflowCapabilities = {
  nameNodeFederation: false,
  routerFederation: false,
};

interface CapabilitySnapshot {
  capabilities: HdfsWorkflowCapabilities;
  error: string;
  isLoading: boolean;
  loaded: boolean;
}

interface CapabilityEntry {
  listeners: Set<() => void>;
  request?: Promise<unknown>;
  snapshot: CapabilitySnapshot;
}

const idleSnapshot = (): CapabilitySnapshot => ({
  capabilities: emptyCapabilities,
  error: "",
  isLoading: true,
  loaded: false,
});

const capabilityEntries = new Map<string, CapabilityEntry>();

interface StackComponentMetadata {
  StackServiceComponents?: {
    component_name?: string;
    componentName?: string;
  };
  component_name?: string;
  componentName?: string;
}

interface HdfsStackServiceMetadata {
  StackServices?: {
    config_types?: unknown;
    service_type?: string;
  };
  components?: StackComponentMetadata[];
}

const componentName = (component: StackComponentMetadata) => {
  const info = component?.StackServiceComponents || component;
  return info?.component_name || info?.componentName || "";
};

function configTypeNames(configTypes: unknown): string[] {
  if (Array.isArray(configTypes)) {
    return configTypes
      .map((item: unknown) => {
        if (typeof item === "string") return item;
        if (item && typeof item === "object") {
          const descriptor = item as { name?: string; type?: string };
          return descriptor.type || descriptor.name || "";
        }
        return "";
      })
      .filter(Boolean);
  }
  return Object.keys((configTypes as Record<string, unknown>) || {});
}

export function evaluateHdfsWorkflowCapabilities(
  stackService: unknown,
): HdfsWorkflowCapabilities {
  const metadata = stackService as HdfsStackServiceMetadata | undefined;
  const stackServiceInfo = metadata?.StackServices || {};
  const serviceType = String(stackServiceInfo.service_type || "").toUpperCase();
  const configTypes = new Set(configTypeNames(stackServiceInfo.config_types));
  const components = new Set(
    (metadata?.components || []).map(componentName).filter(Boolean),
  );
  const nameNodeFederation =
    serviceType === "HDFS" &&
    ["hdfs-site", "core-site"].every((type) => configTypes.has(type)) &&
    ["NAMENODE", "JOURNALNODE", "ZKFC"].every((name) =>
      components.has(name),
    );

  return {
    nameNodeFederation,
    routerFederation:
      nameNodeFederation &&
      configTypes.has("hdfs-rbf-site") &&
      components.has("ROUTER"),
  };
}

function capabilityEntry(cacheKey: string) {
  let entry = capabilityEntries.get(cacheKey);
  if (!entry) {
    entry = { listeners: new Set(), snapshot: idleSnapshot() };
    capabilityEntries.set(cacheKey, entry);
  }
  return entry;
}

function updateEntry(entry: CapabilityEntry, snapshot: CapabilitySnapshot) {
  entry.snapshot = snapshot;
  entry.listeners.forEach((listener) => listener());
}

function capabilityError(error: unknown) {
  if (isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || error.message;
  }
  return error instanceof Error
    ? error.message
    : "Ambari could not verify HDFS workflow capabilities.";
}

function loadStackService(stack: string, version: string, force = false) {
  const cacheKey = `${stack}/${version}`;
  const entry = capabilityEntry(cacheKey);
  if (!force && (entry.request || entry.snapshot.loaded)) return;

  const request = federationApi.getStackService(stack, version, "HDFS");
  entry.request = request;
  updateEntry(entry, idleSnapshot());
  request
    .then((stackService) => {
      if (entry.request !== request) return;
      updateEntry(entry, {
        capabilities: evaluateHdfsWorkflowCapabilities(stackService),
        error: "",
        isLoading: false,
        loaded: true,
      });
    })
    .catch((caught: unknown) => {
      if (entry.request !== request) return;
      updateEntry(entry, {
        capabilities: emptyCapabilities,
        error: capabilityError(caught),
        isLoading: false,
        loaded: true,
      });
    })
    .finally(() => {
      if (entry.request === request) entry.request = undefined;
    });
}

const disabledSnapshot: CapabilitySnapshot = {
  capabilities: emptyCapabilities,
  error: "",
  isLoading: false,
  loaded: false,
};

export default function useHdfsWorkflowCapabilities(enabled = true) {
  const { cluster } = useContext(AppContext);
  const stack = cluster?.stack || cluster?.version?.split("-")[0];
  const version = cluster?.versionNum || cluster?.version?.split("-")[1];
  const cacheKey = stack && version ? `${stack}/${version}` : "";
  const [snapshot, setSnapshot] = useState<CapabilitySnapshot>(
    enabled ? idleSnapshot() : disabledSnapshot,
  );

  useEffect(() => {
    if (!enabled) {
      setSnapshot(disabledSnapshot);
      return;
    }
    if (!stack || !version || !cacheKey) {
      setSnapshot(idleSnapshot());
      return;
    }

    const entry = capabilityEntry(cacheKey);
    const update = () => setSnapshot(entry.snapshot);
    entry.listeners.add(update);
    update();
    loadStackService(stack, version);
    return () => {
      entry.listeners.delete(update);
    };
  }, [cacheKey, enabled, stack, version]);

  return {
    capabilities: snapshot.capabilities,
    error: snapshot.error,
    isLoading: snapshot.isLoading,
    retry: () => {
      if (stack && version) loadStackService(stack, version, true);
    },
  };
}
