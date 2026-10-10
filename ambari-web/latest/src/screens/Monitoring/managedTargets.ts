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

import type { PrometheusBatchResponse } from "../../api/metricsApi";
import type { PrometheusResult, PrometheusTarget } from "./types";

const record = (value: unknown): value is Record<string, unknown> =>
  value !== null && typeof value === "object" && !Array.isArray(value);
const labelsValid = (value: unknown): value is Record<string, string> =>
  record(value) && Object.values(value).every(item => typeof item === "string");

function identity(labels: Record<string, string>) {
  return JSON.stringify([labels.cluster, labels.ambari_cluster_id || "", labels.host, labels.ambari_target,
    labels.service || "", labels.component || ""]);
}

function sampleIndex(samples: PrometheusResult[], cluster: string) {
  const index = new Map<string, number | null>();
  for (const sample of samples) {
    if (!record(sample) || !labelsValid(sample.metric) || sample.metric.cluster !== cluster) continue;
    const value = sample.value;
    if (!Array.isArray(value) || value.length !== 2 || !Number.isFinite(value[0])
      || typeof value[1] !== "string" || value[1].trim() === "" || !Number.isFinite(Number(value[1]))) continue;
    const key = identity(sample.metric);
    // Multiple collectors may observe the same logical target. Conflicting values are unresolved.
    const numeric = Number(value[1]);
    index.set(key, index.has(key) && index.get(key) !== numeric ? null : numeric);
  }
  return index;
}

export function managedTargetObservations(
  discovery: unknown,
  response: PrometheusBatchResponse,
  cluster: string,
  now: number,
): PrometheusTarget[] {
  if (!Array.isArray(discovery) || !Array.isArray(response.data)
    || response.data.length !== 3 || response.status === "error") {
    throw new Error("Invalid managed target observations");
  }
  const refs = ["up", "timestamp", "duration"];
  const samples = refs.map(ref => {
    const items = response.data!.filter(item => item.refId === ref);
    if (items.length !== 1 || items[0].status !== "success" || !Array.isArray(items[0].result)) {
      throw new Error("Incomplete managed target observations: " + ref);
    }
    return sampleIndex(items[0].result, cluster);
  });
  const [up, timestamps, durations] = samples;
  return discovery.flatMap(group => {
    if (!record(group) || !labelsValid(group.labels) || !Array.isArray(group.targets)
      || group.targets.some(target => typeof target !== "string")) {
      throw new Error("Invalid service discovery response");
    }
    const labels = group.labels;
    if (labels.cluster !== cluster || !labels.host
      || !["host", "component"].includes(labels.ambari_target)) {
      throw new Error("Foreign or incomplete target identity");
    }
    const key = identity(labels);
    const timestamp = timestamps.get(key);
    const value = up.get(key);
    const recent = typeof timestamp === "number" && timestamp <= now + 30 && now - timestamp <= 300;
    const health = recent && value === 1 ? "up" : recent && value === 0 ? "down" : "unknown";
    return (group.targets as string[]).map(address => ({
      labels,
      discoveredLabels: labels,
      scrapePool: "ambari-prometheus-targets",
      scrapeUrl: `${labels.__scheme__ || "http"}://${address}${labels.__metrics_path__ || "/metrics"}`,
      health,
      lastScrape: typeof timestamp === "number" ? new Date(timestamp * 1000).toISOString() : undefined,
      lastScrapeDuration: typeof durations.get(key) === "number" ? durations.get(key)! : undefined,
    }));
  });
}
