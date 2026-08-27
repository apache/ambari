/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
import type { ConfigurationPayload, Properties, VersionInfo } from "./types";

type ViewContext = { view: string; version: string; instance: string };
type ClusterPayload = { Clusters?: { cluster_name?: string; version?: string } };
type ConfigValuePayload = { configs?: Array<{ configValue?: string | null }> };
type NodeLabelPayload = {
  nodeLabelInfo?: Array<{ name?: string }> | { name?: string };
  nodeLabels?: Array<{ name?: string }> | { name?: string };
};

export type RuntimeQueue = { path: string; state: string };

export class CapacityApiError extends Error {
  status: number;
  detail: string;

  constructor(message: string, status: number, detail = "") {
    super(message);
    this.name = "CapacityApiError";
    this.status = status;
    this.detail = detail;
  }
}

export const parseViewContext = (pathname: string): ViewContext => {
  const parts = pathname.split("/").filter(Boolean);
  const viewsIndex = parts.lastIndexOf("views");
  const tail = viewsIndex >= 0 ? parts.slice(viewsIndex + 1) : parts;
  const [view = "CAPACITY-SCHEDULER", second = "", third = ""] = tail;
  const hasVersion = /^(?:\d+\.){2,3}\d+(?:[-.].*)?$/.test(second);
  return {
    view,
    version: hasVersion ? second : "",
    instance: hasVersion ? third : second,
  };
};

const parseBody = (text: string): unknown => {
  if (!text) return undefined;
  let body: unknown = text;
  for (let depth = 0; depth < 2 && typeof body === "string"; depth += 1) {
    try {
      body = JSON.parse(body);
    } catch {
      break;
    }
  }
  return body;
};

const errorMessage = (body: unknown, fallback: string) => {
  if (body && typeof body === "object") {
    const candidate = body as Record<string, unknown>;
    return String(candidate.message ?? candidate.error ?? fallback);
  }
  return typeof body === "string" && body ? body : fallback;
};

const nodeLabelNames = (payload: unknown): string[] => {
  if (!payload || typeof payload !== "object") return [];
  const object = payload as NodeLabelPayload;
  const source = object.nodeLabelInfo ?? object.nodeLabels ?? [];
  const labels = Array.isArray(source) ? source : [source];
  return [...new Set(labels.map((label) => label.name?.trim() ?? "").filter(Boolean))];
};

const runtimeQueues = (payload: unknown): RuntimeQueue[] => {
  if (!payload || typeof payload !== "object") return [];
  const root = (payload as { scheduler?: { schedulerInfo?: unknown } }).scheduler?.schedulerInfo;
  const queues: RuntimeQueue[] = [];
  const visit = (value: unknown, parentPath = "") => {
    if (!value || typeof value !== "object") return;
    const queue = value as {
      queueName?: string;
      queuePath?: string;
      state?: string;
      queues?: { queue?: unknown[] | unknown };
    };
    const name = queue.queueName || queue.queuePath?.split(".").pop() || (parentPath ? "" : "root");
    const path = (queue.queuePath || (parentPath ? `${parentPath}.${name}` : name)).toLowerCase();
    if (path) queues.push({ path, state: queue.state || "RUNNING" });
    const children = queue.queues?.queue;
    (Array.isArray(children) ? children : children ? [children] : []).forEach((child) => visit(child, path));
  };
  visit(root);
  return queues;
};

export const createCapacityApi = (
  context = parseViewContext(window.location.pathname),
  fetcher: typeof fetch = fetch,
) => {
  const versionSegment = context.version ? `/versions/${encodeURIComponent(context.version)}` : "";
  const root = `/api/v1/views/${encodeURIComponent(context.view)}${versionSegment}/instances/${encodeURIComponent(context.instance)}/resources/scheduler/configuration`;

  const request = async <T>(path = "", init: RequestInit = {}): Promise<T> => {
    const headers = new Headers(init.headers);
    headers.set("X-Requested-By", "view-capacity-scheduler");
    const response = await fetcher(`${root}${path}`, { ...init, headers, credentials: "same-origin" });
    const text = await response.text();
    const body = parseBody(text);
    if (!response.ok) {
      throw new CapacityApiError(errorMessage(body, response.statusText || "Request failed"), response.status, text);
    }
    return body as T;
  };

  const configValue = async (siteName: string, configName: string) => {
    const query = new URLSearchParams({ siteName, configName });
    const result = await request<ConfigValuePayload>(`/getConfig?${query}`);
    return result.configs?.[0]?.configValue ?? null;
  };

  return {
    root,
    cluster: () => request<ClusterPayload>("/cluster"),
    privilege: () => request<boolean>("/privilege").catch(() => false),
    latest: () => request<ConfigurationPayload>(),
    byTag: (tag: string) => request<ConfigurationPayload>(`/byTag/${encodeURIComponent(tag)}`),
    versions: async (): Promise<VersionInfo[]> => {
      const payload = await request<ConfigurationPayload>("/all");
      return (payload.items ?? []).map((item) => ({
        tag: item.tag ?? String(item.version ?? ""),
        version: item.version,
        created: /^version\d+$/.test(item.tag ?? "") ? Number((item.tag ?? "").slice(7)) : undefined,
      })).filter((version) => version.tag);
    },
    configValue,
    nodeLabels: async () => nodeLabelNames(await request<unknown>("/nodeLabels")),
    rmQueues: async () => runtimeQueues(await request<unknown>("/rmCurrentConfig")),
    save: (properties: Properties, note: string, tag = `version${Date.now()}`) => request<unknown>("", {
      method: "PUT",
      headers: { "Content-Type": "text/plain; charset=utf-8" },
      body: JSON.stringify({
        Clusters: {
          desired_config: [{
            type: "capacity-scheduler",
            tag,
            service_config_version_note: note,
            properties,
          }],
        },
      }),
    }),
    refresh: () => request<ConfigurationPayload>("/saveAndRefresh", {
      method: "PUT",
      headers: { "Content-Type": "application/json; charset=utf-8" },
      body: JSON.stringify({ save: true }),
    }),
    restart: () => request<ConfigurationPayload>("/saveAndRestart", {
      method: "PUT",
      headers: { "Content-Type": "application/json; charset=utf-8" },
      body: JSON.stringify({ save: true }),
    }),
  };
};

export type CapacityApi = ReturnType<typeof createCapacityApi>;
