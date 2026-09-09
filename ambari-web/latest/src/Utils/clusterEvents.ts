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

function sameId(value: unknown, clusterId: number | string): boolean {
  return String(value) === String(clusterId);
}

function keyedClusterValue(value: unknown, clusterId: number | string) {
  if (!value || typeof value !== "object") return undefined;
  return (value as Record<string, unknown>)[String(clusterId)];
}

export function projectClusterEvent(
  message: any,
  destination: string,
  cluster: { cluster_id: number | string; cluster_name: string },
): any | null {
  const { cluster_id: clusterId, cluster_name: clusterName } = cluster;

  switch (destination) {
    case "/events/hostcomponents": {
      if (!Array.isArray(message.hostComponents)) return null;
      const hostComponents = message.hostComponents.filter(
        (component: any) => sameId(component?.clusterId, clusterId),
      );
      return hostComponents.length ? { ...message, hostComponents, destination } : null;
    }
    case "/events/alerts": {
      const summary = keyedClusterValue(message.summaries, clusterId);
      return summary
        ? { ...message, summaries: { [String(clusterId)]: summary }, destination }
        : null;
    }
    case "/events/ui_topologies":
    case "/events/alert_definitions": {
      const clusterValue = keyedClusterValue(message.clusters, clusterId);
      return clusterValue
        ? { ...message, clusters: { [String(clusterId)]: clusterValue }, destination }
        : null;
    }
    case "/events/configs": {
      if (message.clusterId != null && !sameId(message.clusterId, clusterId)) return null;
      const configs = Array.isArray(message.configs)
        ? message.configs.filter((config: any) => sameId(config?.clusterId, clusterId))
        : message.configs;
      const identified = message.clusterId != null || (Array.isArray(configs) && configs.length > 0);
      return identified ? { ...message, configs, destination } : null;
    }
    case "/events/services":
    case "/events/hosts":
      return (message.cluster_name === clusterName || message.clusterName === clusterName)
        ? { ...message, destination }
        : null;
    case "/events/alert_group": {
      if (!Array.isArray(message.groups)) return null;
      const groups = message.groups.filter(
        (group: any) => sameId(group?.cluster_id, clusterId),
      );
      return groups.length ? { ...message, groups, destination } : null;
    }
    case "/events/upgrade":
      return sameId(message.clusterId ?? message.cluster_id, clusterId)
        ? { ...message, destination }
        : null;
    case "/events/requests":
      return (message.clusterName === clusterName || message.cluster_name === clusterName)
        ? { ...message, destination }
        : null;
    default:
      return null;
  }
}
