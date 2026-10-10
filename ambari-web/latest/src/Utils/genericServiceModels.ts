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
import Service, { type ServiceData } from "../models/service";
import { builtinServiceNames, registerServiceModelIdentity } from "../constants";

/** Preserves authoritative service state while grouping components by stack metadata. */
export function genericServiceModels(services: any[], definitions: any, observations: any, clusterName: string,
  serviceStates: Map<string, any> = new Map()) {
  const models: Record<string, Service & { isClientOnlyService: boolean; isInPassiveForService: boolean;
    isRestartRequiredForService: boolean }> = {};
  for (const item of services || []) {
    let info = item.ServiceInfo;
    if (!info || typeof info.service_name !== "string" || builtinServiceNames.has(info.service_name)) continue;
    const latest = serviceStates.get(info.service_name);
    if (latest) info = { ...info,
      ...(latest.state === undefined ? {} : { state: latest.state }),
      ...(latest.maintenance_state === undefined ? {} : { maintenance_state: latest.maintenance_state }),
    };
    const name = info.service_name;
    const definition = definitions?.items?.find((value: any) => value.StackServices?.service_name === name);
    const key = registerServiceModelIdentity(name, definition?.StackServices?.display_name || name);
    if (!definition || !Array.isArray(definition.components) || !definition.components.length ||
      !Object.values(Service.statesMap).includes(info.state) || !["ON", "OFF"].includes(info.maintenance_state)) continue;
    const components = definition.components.map((value: any) => value.StackServiceComponents);
    if (components.some((value: any) => !value || !["MASTER", "SLAVE", "CLIENT"].includes(value.component_category))) continue;
    const groups = components.map((component: any) => {
      const observed = observations?.items?.find((value: any) => value.ServiceComponentInfo?.service_name === name
        && value.ServiceComponentInfo?.component_name === component.component_name);
      const hosts = (observed?.host_components || []).map((value: any) => ({ ...value,
        hostName: value.HostRoles?.host_name, componentName: component.component_name, serviceName: name, clusterName,
        state: value.HostRoles?.state, workStatus: value.HostRoles?.state, passiveState: value.HostRoles?.maintenance_state,
      }));
      return { componentName: component.component_name, displayName: component.display_name || component.component_name,
        componentCategory: component.component_category, hostComponents: hosts, totalCount: hosts.length,
        startedCount: hosts.filter((host: any) => host.state === "STARTED").length,
        installedCount: hosts.filter((host: any) => host.state === "INSTALLED").length,
        staleConfigHosts: hosts.filter((host: any) => host.HostRoles?.stale_configs === true).map((host: any) => host.hostName),
        allowToDelete: hosts.length > 0 && hosts.every((host: any) => ["INIT", "INSTALL_FAILED", "INSTALLED"].includes(host.state)),
      };
    });
    const model = new Service({ serviceName: name, displayName: definition.StackServices.display_name || name,
      passiveState: info.maintenance_state, workStatus: info.state,
      desiredRepositoryVersionId: info.desired_repository_version_id ?? null,
      masterComponents: groups.filter((group: any) => group.componentCategory === "MASTER"),
      slaveComponents: groups.filter((group: any) => group.componentCategory === "SLAVE"),
      clientComponents: groups.filter((group: any) => group.componentCategory === "CLIENT"),
      hostComponents: groups.flatMap((group: any) => group.hostComponents),
    } as ServiceData);
    models[key] = Object.assign(model, {
      isClientOnlyService: components.every((component: any) => component.component_category === "CLIENT"),
      isInPassiveForService: info.maintenance_state === "ON",
      isRestartRequiredForService: groups.some((group: any) => group.staleConfigHosts.length > 0),
    });
  }
  return models;
}
