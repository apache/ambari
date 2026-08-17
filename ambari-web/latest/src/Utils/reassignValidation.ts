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

import { get } from "lodash";

type HostComponent = {
  HostRoles?: { host_name?: string };
  hostName?: string;
};

type InstalledComponent = {
  componentName?: string;
  hostComponents?: HostComponent[];
};

type ServiceModel = {
  masterComponents?: InstalledComponent[];
};

type StackComponent = {
  StackServiceComponents?: {
    component_name?: string;
    reassign_allowed?: boolean;
  };
};

type StackService = {
  StackServices?: { service_name?: string };
  StackService?: { service_name?: string };
  components?: StackComponent[];
};

type ServiceComponentInfo = {
  items?: StackService[];
};

export const getComponentHostNames = (
  component?: InstalledComponent
): string[] =>
  (component?.hostComponents || [])
    .map((hostComponent) =>
      get(hostComponent, "HostRoles.host_name", hostComponent.hostName)
    )
    .filter((hostName): hostName is string => Boolean(hostName));

export const isMissingHostComponentError = (error: unknown) => {
  const status = get(error, "response.status");
  const message = String(
    get(error, "response.data.message", get(error, "response.data", ""))
  );
  return status === 404 || message.includes("NoSuchResourceException");
};

export const getReassignValidationErrors = ({
  componentName,
  serviceName,
  allHostNames,
  serviceComponentInfo,
  serviceModel,
}: {
  componentName?: string;
  serviceName: string;
  allHostNames: string[];
  serviceComponentInfo: ServiceComponentInfo;
  serviceModel: ServiceModel;
}) => {
  const errors: string[] = [];
  if (!componentName) {
    return ["No component was selected for reassignment."];
  }
  if (allHostNames.length < 2) {
    errors.push("You must have at least 2 hosts to run the Move Wizard.");
  }

  const stackService = (serviceComponentInfo?.items || []).find(
    (item) =>
      get(item, "StackServices.service_name") === serviceName ||
      get(item, "StackService.service_name") === serviceName
  );
  const stackComponent = (stackService?.components || []).find(
    (component) =>
      get(component, "StackServiceComponents.component_name") === componentName
  );
  if (!stackComponent ||
      get(stackComponent, "StackServiceComponents.reassign_allowed") !== true) {
    errors.push(`${componentName} cannot be reassigned for ${serviceName}.`);
  }

  const installedComponent = (serviceModel?.masterComponents || []).find(
    (component) => component.componentName === componentName
  );
  if (!installedComponent) {
    errors.push(`${componentName} is not installed for ${serviceName}.`);
  } else {
    const occupiedHosts = new Set(getComponentHostNames(installedComponent));
    if (
      allHostNames.length > 0 &&
      allHostNames.every((hostName) => occupiedHosts.has(hostName))
    ) {
      errors.push(`Every cluster host already has ${componentName}.`);
    }
  }

  return [...new Set(errors)];
};
