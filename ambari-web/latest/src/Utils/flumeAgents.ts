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

export type FlumeAgentStatus = "RUNNING" | "NOT_RUNNING" | "UNKNOWN";

export type FlumeAgent = {
  id: string;
  hostName: string;
  name: string;
  status: FlumeAgentStatus;
};

type FlumeProcess = {
  HostComponentProcess?: {
    host_name?: string;
    name?: string;
    status?: unknown;
  };
};

type FlumeHostComponent = {
  HostRoles?: { host_name?: string };
  processes?: FlumeProcess[];
};

type ServiceComponent = {
  ServiceComponentInfo?: {
    component_name?: string;
    service_name?: string;
  };
  host_components?: FlumeHostComponent[];
};

const normalizeStatus = (status: unknown): FlumeAgentStatus => {
  if (status === "RUNNING" || status === "NOT_RUNNING") {
    return status;
  }
  return "UNKNOWN";
};

export function extractFlumeAgents(componentData: unknown): FlumeAgent[] {
  const items = Array.isArray(componentData)
    ? componentData as ServiceComponent[]
    : Array.isArray((componentData as { items?: unknown[] })?.items)
      ? (componentData as { items: ServiceComponent[] }).items
      : [];

  const flumeComponent = items.find(
    (item) =>
      item?.ServiceComponentInfo?.service_name === "FLUME" &&
      item?.ServiceComponentInfo?.component_name === "FLUME_HANDLER"
  );

  return (flumeComponent?.host_components || [])
    .flatMap((hostComponent) => {
      const componentHostName = hostComponent?.HostRoles?.host_name;
      return (hostComponent?.processes || []).map((process) => {
        const processInfo = process?.HostComponentProcess || {};
        const hostName = processInfo.host_name || componentHostName || "";
        const name = processInfo.name || "";
        return {
          id: `${name}-${hostName}`,
          hostName,
          name,
          status: normalizeStatus(processInfo.status),
        };
      });
    })
    .filter((agent: FlumeAgent) => agent.hostName && agent.name)
    .sort((left: FlumeAgent, right: FlumeAgent) =>
      left.hostName.localeCompare(right.hostName) ||
      left.name.localeCompare(right.name)
    );
}

export function canStartFlumeAgent(status: FlumeAgentStatus): boolean {
  return status === "NOT_RUNNING";
}

export function canStopFlumeAgent(status: FlumeAgentStatus): boolean {
  return status === "RUNNING";
}
