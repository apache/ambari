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
import federationApi from "../../../../api/federationApi";
import { AppContext } from "../../../../store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import {
  evaluateHawqCapabilities,
  HawqCapabilities,
  HawqCapabilityInput,
} from "../Federation/workflowUtils";

const emptyCapabilities: HawqCapabilities = {
  supported: false,
  canAdd: false,
  canRemove: false,
  canActivate: false,
};

interface ComponentSnapshot {
  hostName?: string;
  name: string;
  state?: string;
}

function record(value: unknown): Record<string, unknown> | null {
  return value && typeof value === "object" && !Array.isArray(value)
    ? value as Record<string, unknown>
    : null;
}

function stringProperty(value: unknown, key: string) {
  const candidate = record(value)?.[key];
  return typeof candidate === "string" ? candidate : "";
}

function serviceNames(services: unknown): string[] {
  if (!Array.isArray(services)) return [];
  return services
    .map((service) => stringProperty(record(service)?.ServiceInfo, "service_name"))
    .filter(Boolean)
    .sort();
}

function componentItems(value: unknown): unknown[] {
  if (Array.isArray(value)) return value;
  const valueRecord = record(value);
  if (!valueRecord) return [];
  if (Array.isArray(valueRecord.items)) return valueRecord.items;
  return Object.values(valueRecord);
}

function componentSnapshots(value: unknown): ComponentSnapshot[] {
  return componentItems(value).flatMap((component) => {
    const componentRecord = record(component);
    const name = stringProperty(
      componentRecord?.ServiceComponentInfo,
      "component_name",
    );
    if (!name) return [];
    const hostComponents = componentRecord?.host_components;
    if (!Array.isArray(hostComponents) || !hostComponents.length) {
      return [{ name, hostName: undefined, state: undefined }];
    }
    return hostComponents.map((hostComponent): ComponentSnapshot => {
      const roles = record(record(hostComponent)?.HostRoles);
      return {
        name,
        hostName: stringProperty(roles, "host_name") || undefined,
        state: stringProperty(roles, "state") || undefined,
      };
    });
  }).sort((left, right) =>
    `${left.name}\0${left.hostName || ""}\0${left.state || ""}`.localeCompare(
      `${right.name}\0${right.hostName || ""}\0${right.state || ""}`,
    ),
  );
}

function configTypeNames(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value
      .map((item) => {
        if (typeof item === "string") return item;
        return stringProperty(item, "type") || stringProperty(item, "name");
      })
      .filter(Boolean);
  }
  return Object.keys(record(value) || {});
}

function stackComponents(value: unknown): HawqCapabilityInput["stackComponents"] {
  const components = record(value)?.components;
  if (!Array.isArray(components)) return [];
  return components.flatMap((component) => {
    const componentRecord = record(component);
    const info = record(componentRecord?.StackServiceComponents) || componentRecord;
    const name = stringProperty(info, "component_name") ||
      stringProperty(info, "componentName");
    if (!name) return [];
    const commands = info?.custom_commands ?? info?.customCommands;
    return [{
      name,
      customCommands: Array.isArray(commands)
        ? commands.filter((command): command is string => typeof command === "string")
        : Object.keys(record(commands) || {}),
    }];
  });
}

function capabilityError(caught: unknown) {
  const responseMessage = stringProperty(
    record(record(caught)?.response)?.data,
    "message",
  );
  return responseMessage ||
    (caught instanceof Error ? caught.message : "") ||
    "Ambari could not verify the HAWQ stack capability.";
}

export default function useHawqStandbyCapabilities(enabled = true) {
  const {
    cluster: { stack, versionNum },
    allHostNames,
    services,
  } = useContext(AppContext);
  const { masterSlaveClientsData } = useContext(ServiceContext);
  const [capabilities, setCapabilities] = useState(emptyCapabilities);
  const [isLoading, setIsLoading] = useState(enabled);
  const [error, setError] = useState("");
  const [retryCount, setRetryCount] = useState(0);
  const servicesRevision = JSON.stringify(serviceNames(services));
  const hostsRevision = JSON.stringify([...allHostNames].sort());
  const componentRevision = JSON.stringify(
    componentSnapshots(masterSlaveClientsData),
  );
  const hawqInstalled = (JSON.parse(servicesRevision) as string[]).includes("HAWQ");

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setCapabilities(emptyCapabilities);
      setError("");
      if (!enabled) {
        setIsLoading(false);
        return;
      }
      setIsLoading(true);
      if (!hawqInstalled) {
        setError("HAWQ is not installed in this cluster.");
        setIsLoading(false);
        return;
      }
      const hosts = JSON.parse(hostsRevision) as string[];
      const topology = JSON.parse(componentRevision) as ComponentSnapshot[];
      if (!stack || !versionNum) {
        setError("Ambari has not loaded the active stack and version.");
        setIsLoading(false);
        return;
      }
      if (!hosts.length || !topology.length) {
        setError(
          "Ambari has not loaded the cluster host-component topology. Retry after the cluster model refreshes.",
        );
        setIsLoading(false);
        return;
      }
      try {
        const stackService = await federationApi.getStackService(
          stack,
          versionNum,
          "HAWQ",
        );
        const stackServiceInfo = record(record(stackService)?.StackServices);
        const installedComponents = topology.filter((component) =>
          component.name.startsWith("HAWQ"),
        );
        const evaluated = evaluateHawqCapabilities({
          serviceInstalled: hawqInstalled,
          hostCount: hosts.length,
          configTypes: configTypeNames(stackServiceInfo?.config_types),
          stackComponents: stackComponents(stackService),
          installedComponents,
        });
        if (!cancelled) setCapabilities(evaluated);
      } catch (caught: unknown) {
        if (!cancelled) {
          setError(capabilityError(caught));
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    };
    void load();
    return () => {
      cancelled = true;
    };
  }, [
    componentRevision,
    enabled,
    hawqInstalled,
    hostsRevision,
    retryCount,
    servicesRevision,
    stack,
    versionNum,
  ]);

  return {
    capabilities,
    error,
    hawqInstalled,
    isLoading,
    retry: () => setRetryCount((value) => value + 1),
  };
}
