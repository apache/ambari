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

import { find, isArray, map } from "lodash";
import { HostsApi } from "../api/hostsApi";
import { ServiceApi } from "../api/serviceApi";
import { role } from "./Utility";

export type InstallComponentTaskOptions = {
  reconcileHosts?: boolean;
};

export type StartAllServicesOptions = {
  runSmokeTest?: boolean;
  skipServiceChecks?: boolean;
};

export async function updateComponent(
  clusterName: string,
  componentName: string,
  hostName: string[] | string,
  serviceName: string,
  context: string = "Install",
  taskNum: number
) {
  if (hostName && !isArray(hostName)) {
    hostName = [hostName];
  }
  const state = context.toLowerCase() == "start" ? "STARTED" : "INSTALLED";
  return await HostsApi.updateHostComponents(clusterName, "", {
    HostRoles: {
      state: state,
    },
    query:
      "HostRoles/component_name=" +
      componentName +
      ( hostName
        ? "&HostRoles/host_name.in(" + (hostName as string[]).join(",") + ")"
        : "") +
      "&HostRoles/maintenance_state=OFF",
    context: context + " " + role(componentName, false),
    hostName: hostName,
    taskNum: taskNum || 1,
    componentName: componentName,
    serviceName: serviceName,
  });
}

export async function createInstallComponentTask(
  componentName: string,
  hostName: string | string[],
  serviceName: string,
  clusterName: string,
  services: string[],
  serviceObj: any,
  getKDCSessionState?: (callback: () => Promise<any>, errorCallback?: () => void) => Promise<void>,
  options: InstallComponentTaskOptions = {},
) {
  // Check for KDC session first if getKDCSessionState is provided
  if (getKDCSessionState) {
    return new Promise((resolve, reject) => {
      getKDCSessionState(
        async () => {
          try {
            const result = await performCreateInstallComponentTask(
              componentName,
              hostName,
              serviceName,
              clusterName,
              services,
              serviceObj,
              options,
            );
            resolve(result);
          } catch (err) {
            reject(err);
          }
        },
        () => {
          // Error callback - KDC session validation failed
          reject(new Error("KDC session validation failed"));
        }
      );
    });
  } else {
    // If no KDC session check is needed, proceed directly
    return performCreateInstallComponentTask(
      componentName,
      hostName,
      serviceName,
      clusterName,
      services,
      serviceObj,
      options,
    );
  }
}

function isAlreadyExistsError(error: any) {
  const status = error?.response?.status ?? error?.status;
  const message = String(
    error?.response?.data?.message || error?.message || "",
  );
  return status === 409 || /already exists|resourcealreadyexists/i.test(message);
}

async function getMissingComponentHosts(
  clusterName: string,
  componentName: string,
  hostNames: string[],
) {
  const data = await HostsApi.getInstalledHostsForHostComponents(
    clusterName,
    componentName,
    hostNames.join(","),
  );
  const installedHosts = new Set<string>(
    map(data?.items || [], "HostRoles.host_name").filter(Boolean),
  );
  return hostNames.filter((host) => !installedHosts.has(host));
}

async function performCreateInstallComponentTask(
  componentName: string,
  hostName: string | string[],
  serviceName: string,
  clusterName: string,
  services: string[],
  serviceObj: any,
  options: InstallComponentTaskOptions,
) {
  void services;
  const hostNames = [...new Set(Array.isArray(hostName) ? hostName : [hostName])];
  if (!hostNames.length) return { status: 200 };
  let hostsWithoutComponents = await getMissingComponentHosts(
    clusterName,
    componentName,
    hostNames,
  );
  const taskNum = 1;
  if (hostsWithoutComponents.length) {
    const allServiceComponents = [
      ...(serviceObj?.masterComponents || []),
      ...(serviceObj?.clientComponents || []),
      ...(serviceObj?.slaveComponents || []),
    ];
    const serviceComponentExists = allServiceComponents.some(
      (component: any) =>
        (component.component_name || component.componentName) === componentName,
    );
    if (!serviceComponentExists) {
      try {
        await ServiceApi.createComponent(clusterName, serviceName, componentName);
      } catch (error: any) {
        if (!isAlreadyExistsError(error)) {
          if (!options.reconcileHosts) throw error;
          hostsWithoutComponents = await getMissingComponentHosts(
            clusterName,
            componentName,
            hostNames,
          );
        }
      }
    }

    if (hostsWithoutComponents.length) {
      let registrationVerified = false;
      const requestData = {
        RequestInfo: {
          query: hostsWithoutComponents
            .map((item) => "Hosts/host_name=" + item)
            .join("|"),
        },
        Body: {
          host_components: [
            {
              HostRoles: {
                component_name: componentName,
              },
            },
          ],
        },
      };
      try {
        await HostsApi.registerHostToComponent(clusterName, requestData);
      } catch (error) {
        if (!options.reconcileHosts) throw error;
        hostsWithoutComponents = await getMissingComponentHosts(
          clusterName,
          componentName,
          hostNames,
        );
        if (hostsWithoutComponents.length) throw error;
        registrationVerified = true;
      }

      if (options.reconcileHosts && !registrationVerified) {
        hostsWithoutComponents = await getMissingComponentHosts(
          clusterName,
          componentName,
          hostNames,
        );
        if (hostsWithoutComponents.length) {
          throw new Error(
            `Ambari did not register ${componentName} on: ${hostsWithoutComponents.join(", ")}`,
          );
        }
      }
    }
  }

  const hostsToReconcile = options.reconcileHosts
    ? hostNames
    : hostsWithoutComponents;
  if (!hostsToReconcile.length) {
    return { status: 200 };
  }

  return await updateComponent(
    clusterName,
    componentName,
    hostsToReconcile,
    serviceName,
    "Install",
    taskNum
  );
}

export async function startServices(
  clusterName: string,
  runSmokeTest: boolean,
  services: string[],
  startListedServicesFlag: any,
  skipServiceChecks = false,
  allServices: string[] = [],
) {
  startListedServicesFlag = startListedServicesFlag || false;
  let data: any = {
    ServiceInfo: {
      state: "STARTED",
    },
  };
  let servicesList = "";
  if (services && services.length) {
    if (startListedServicesFlag) {
      servicesList = services.join(",");
    } else {
      servicesList = allServices
        .filter((service) => !services.includes(service))
        .join(",");
    }
    data.context = "Start required services";
    if (!servicesList) return { status: 200 };
    data.urlParams = "ServiceInfo/service_name.in(" + servicesList + ")";
  } else {
    data.context = "Start all services";
  }

  if (runSmokeTest) {
    data.urlParams = data.urlParams ? data.urlParams + "&" : "";
    data.urlParams += "params/run_smoke_test=" + !skipServiceChecks;
  }

  return await ServiceApi.updateService(
    clusterName,
    data,
    data.urlParams || "",
  );
}

export async function startAllServices(
  clusterName: string,
  options: StartAllServicesOptions = {},
) {
  return await startServices(
    clusterName,
    options.runSmokeTest || false,
    [],
    false,
    options.skipServiceChecks || false,
  );
}

export async function stopAllServices(clusterName: string) {
  let data: any = {
    ServiceInfo: {
      state: "INSTALLED",
    },
  };
  data.context = "Stop all services";

  return await ServiceApi.updateService(clusterName, data, "");
}

export async function restartAllRequired(clusterName: string) {
  const payload = {
    RequestInfo: {
      command: "RESTART",
      context: "Restart all required services",
      operation_level: "host_component",
    },
    "Requests/resource_filters": [
      {
        hosts_predicate:
          `HostRoles/stale_configs=true&HostRoles/cluster_name=${clusterName}`,
      },
    ],
  };

  return await HostsApi.clusterRequests(clusterName, payload);
}

export enum resourceTypeEnum {
  CLUSTER = "ClusterResource",
  HOST = "HostResource",
  SERVICE = "ServiceResource",
  SERVICE_COMPONENT = "ServiceComponentResource",
  HOST_COMPONENT = "HostComponentResource",
}

export function downloadClientConfigsCall(data: any, clusterName: string) {
  var url = _getUrl(
    data.hostName,
    data.serviceName,
    data.componentName,
    data.resourceType,
    clusterName
  );
  var newWindow = window.open(url);
  newWindow?.focus();
}
export function _getUrl(
  hostName: string,
  serviceName: string,
  componentName: string,
  resourceType: string,
  clusterName: string
) {
  var result;
  var prefix = "/api/v1" + "/clusters/" + clusterName + "/";

  switch (resourceType) {
    case resourceTypeEnum.SERVICE_COMPONENT:
      result =
        prefix + "services/" + serviceName + "/components/" + componentName;
      break;
    case resourceTypeEnum.HOST_COMPONENT:
      result =
        prefix + "hosts/" + hostName + "/host_components/" + componentName;
      break;
    case resourceTypeEnum.HOST:
      result = prefix + "hosts/" + hostName + "/host_components";
      break;
    case resourceTypeEnum.SERVICE:
      result = prefix + "services/" + serviceName + "/components";
      break;
    case resourceTypeEnum.CLUSTER:
    default:
      result = prefix + "components";
  }

  result += "?format=client_config_tar";
  return result;
}

export async function stopServices(
  clusterName: string,
  services: string[],
  stopListedServicesFlag: boolean,
  stopAllServices: boolean,
  allServices: string[]
) {
  stopAllServices = stopAllServices || false;
  stopListedServicesFlag = stopListedServicesFlag || false;
  services = services || [];
  allServices = allServices || [];
  let data: any = {
    ServiceInfo: {
      state: "INSTALLED",
    },
  };
  if (stopAllServices) {
    data.context = "Stop all services";
  } else {
    let servicesList;
    if (stopListedServicesFlag) {
      servicesList = services.join(",");
    } else {
      servicesList = allServices
        .filter((ser) => {
          return !services.includes(ser);
        })
        .join(",");
    }
    data.context = "Stop required services";
    if (!servicesList) return { status: 200 };
    data.urlParams = "ServiceInfo/service_name.in(" + servicesList + ")";
  }
  return await ServiceApi.updateService(clusterName, data, data.urlParams || "");
}

export async function deleteComponent(
  clusterName: string,
  componentName: string,
  hostName: string,
  serviceName: string,
  ignoreMissing = false,
) {
  try {
    // First stop the component
    await updateComponent(
      clusterName,
      componentName,
      hostName,
      serviceName,
      "INSTALLED",
      1
    );
    
    // Then delete the component from the host
    const response = await HostsApi.deleteHostComponent(
      clusterName,
      hostName,
      componentName
    );
    return response;
  } catch (err: any) {
    const message = String(
      err?.response?.data?.message ||
        err?.response?.data ||
        err?.message ||
        "",
    );
    if (
      ignoreMissing &&
      (err?.response?.status === 404 ||
        err?.status === 404 ||
        /NoSuchResourceException/i.test(message))
    ) {
      return { status: 200 };
    }
    console.error(`Error deleting component ${componentName} from host ${hostName}:`, err);
    throw err;
  }
}

export function reconfigureSites(siteNames: string[], data: any, note: string) {
  return siteNames.map(function (_siteName) {
    var config = find(data.items, ["type", _siteName]);
    var configToSave: any = {
      type: _siteName,
      properties: config && config.properties,
      service_config_version_note: note || "",
    };
    if (config && config.properties_attributes) {
      configToSave.properties_attributes = config.properties_attributes;
    }
    return configToSave;
  });
}
