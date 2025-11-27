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

import { filter, forEach, get, isEmpty, map, set } from "lodash";
import { HostsApi } from "../../api/hostsApi";
import { computeParameters } from "../../globals/updateControl";
import { getQueryParameters } from "./host";
import modalManager from "../../store/ModalManager";
import {
  doDecommissionRegionServer,
  doRecommissionAndStart,
  getAllComponents,
  getHostComponentsInfo,
  getPopulatedHostComponentObject,
  isClientUsingComponentName,
  setRackInfo,
  showHbaseActiveWarning,
  showRegionServerWarning,
} from "./utils";
import {
  getComponentsFromServer,
  infoPassiveState,
  restartHostComponents,
  showRollingRestartPopup,
} from "./batchUtils";
import { checkNnLastCheckpointTime } from "./actions";
//TODO: Uncomment the below import and its usage once BackgroundOperations component is available
// import BackgroundOperations from "../BackgroundOperations";
import { ComponentStatus } from "./enums";
import {
  showErrorModal,
  showRollingNothingToDoModal,
  translate,
  translateWithVariables,
} from "../../Utils/Utility";
import DeleteHostComponentsModal from "./DeleteHostComponentsModal";
import { Alert } from "react-bootstrap";
import ConfirmDeleteHostModal from "./ConfirmDeleteHostModal";
import { serviceNameDisplayMapping } from "../../constants";
import { SelectedFilters } from "./HostComboSearch";

export const bulkOperationConfirm = (
  operationData: any,
  hostsNames: string[],
  selection: string,
  clusterName: string,
  stackVersionList: any[],
  serviceComponentInfo: any,
  serviceModels: any,
  getKDCSessionState: (callback: () => Promise<void>) => Promise<void>,
  selectedFilters: SelectedFilters
) => {
  let queryParams: any[] = [];
  if (selection === "selected") {
    if (hostsNames.length) {
      queryParams.push({
        key: "Hosts/host_name",
        value: hostsNames,
        type: "MULTIPLE",
      });
    }
  } else if (selection === "filtered") {
    queryParams = getQueryParameters(selectedFilters);
  }
  getHostsForBulkOperations(
    queryParams,
    operationData,
    clusterName,
    stackVersionList,
    serviceComponentInfo,
    serviceModels,
    getKDCSessionState
  );
};

const getHostsForBulkOperations = async (
  queryParams: any,
  operationData: any,
  clusterName: string,
  stackVersionList: any[],
  serviceComponentInfo: any,
  serviceModels: any,
  getKDCSessionState: (callback: () => Promise<void>) => Promise<void>
) => {
  const data = {
    parameters: computeParameters(queryParams),
    operationData: operationData,
  };
  const response = await HostsApi.getHostsBulkOperations(clusterName, data);
  getHostsForBulkOperationSuccessCallback(
    response,
    data,
    stackVersionList,
    clusterName,
    serviceComponentInfo,
    serviceModels,
    getKDCSessionState
  );
};

const getHostsForBulkOperationSuccessCallback = (
  json: any,
  param: any,
  stackVersionList: any[],
  clusterName: string,
  serviceComponentInfo: any,
  serviceModels: any,
  getKDCSessionState: (callback: () => Promise<void>) => Promise<void>
) => {
  const operationData = param.operationData;
  const hosts: any[] = convertHostsObjects(json);
  let repoVersion = null;

  if (!hosts.length) {
    return;
  }

  if (["SET_RACK_INFO", "ADD", "DELETE"].includes(operationData.action)) {
    return bulkOperation(
      operationData,
      hosts,
      clusterName,
      serviceComponentInfo,
      serviceModels,
      getKDCSessionState
    );
  }

  const hostNames = hosts.map((host: any) => get(host, "hostName"));
  let hostNamesSkipped = [];
  if (operationData.action === "DECOMMISSION") {
    hostNamesSkipped = getSkippedForDecommissionHosts(
      json,
      hosts,
      operationData
    );
  }
  if (operationData.action === "PASSIVE_STATE") {
    repoVersion = filter(stackVersionList, { state: "CURRENT" })[0];
    if (!repoVersion && stackVersionList.length === 1) {
      repoVersion = filter(stackVersionList, { state: "OUT_OF_SYNC" })[0];
    }
    if (!repoVersion) {
      console.error(
        "CLUSTER STACK VERSIONS ERROR: multiple clusters in OUT_OF_SYNC state OR none in CURRENT or OUT_OF_SYNC state"
      );
      return;
    }
    hostNamesSkipped = getSkippedForPassiveStateHosts(hosts, repoVersion);
  }

  let message = "";
  if (get(operationData, "componentNameFormatted", false)) {
    message = `Are you sure you want to ${operationData.message} ${operationData.componentNameFormatted} on the following ${hostNames.length} hosts?`;
  } else {
    message = `Are you sure you want to ${operationData.message} on the following ${hostNames.length} hosts?`;
  }

  let modalProps = {
    isOpen: true,
    onClose: () => {},
    modalTitle: "Confirm Bulk Operation",
    modalBody: getBulkOperationConfirmPopupModalBody(
      message,
      hostNames,
      hostNamesSkipped,
      operationData
    ),
    successCallback: () => {
      bulkOperation(
        operationData,
        hosts,
        clusterName,
        serviceComponentInfo,
        serviceModels,
        getKDCSessionState
      );
      modalManager.hide();
    },
    options: {
      buttonSize: "sm" as "sm" | "lg" | undefined,
      cancelableViaIcon: true,
      cancelableViaBtn: true,
      okButtonVariant: "primary",
    },
  };

  modalManager.show(modalProps);
};

const getBulkOperationConfirmPopupModalBody = (
  message: string,
  hostNames: string[],
  hostNamesSkipped: string[],
  operationData: any
) => {
  const bodyMessage = message;
  let bodyMessageExtended = "";
  if (hostNamesSkipped.length) {
    switch (operationData.action) {
      case "DECOMMISSION":
        bodyMessageExtended =
          "Components on these hosts are stopped so decommission will be skipped.";
        break;
      case "PASSIVE_STATE":
        bodyMessageExtended = `Some hosts have components from a stack which is not current. Before bringing these hosts out of maintenance mode, it is recommended that you upgrade their components to {target version}`; //TODO: determine the target version
        break;
      default:
        bodyMessageExtended = "";
        break;
    }
  }
  return (
    <div>
      <div className="fs-12 mb-2">{bodyMessage}</div>
      <div className="scrollable-h15 border rounded-1 p-1">
        {hostNames.length
          ? map(hostNames, (hostName: string, index: number) => (
              <div key={index} className="fs-12 p-1">
                {hostName}
              </div>
            ))
          : null}
      </div>
      {bodyMessageExtended && (
        <Alert variant="warning mt-2">
          <div className="fs-12 mb-2">{bodyMessageExtended}</div>
          <div className="scrollable-h15 border rounded-1 p-1">
            {hostNamesSkipped.length
              ? map(hostNamesSkipped, (hostName: string, index: number) => (
                  <div key={index} className="fs-12 p-1">
                    {hostName}
                  </div>
                ))
              : null}
          </div>
        </Alert>
      )}
    </div>
  );
};

const convertHostsObjects = (json: any) => {
  let hosts: any[] = [];
  forEach(json.items, (item: any, index: number) => {
    hosts.push({
      index: index,
      id: get(item, "Hosts.host_name"),
      clusterId: get(item, "stack_versions[0].HostStackVersions.cluster_name"),
      passiveState: get(item, "Hosts.maintenance_state"),
      state: get(item, "Hosts.host_state"),
      hostName: get(item, "Hosts.host_name"),
      hostComponents: get(item, "host_components", []).map((component: any) => {
        let id =
          get(component, "HostRoles.component_name") +
          "_" +
          get(item, "Hosts.host_name");
        set(component, "id", id);
        return id;
      }),
      allHostComponents: get(item, "host_components", []),
    });
  });
  return hosts;
};

const getSkippedForDecommissionHosts = (
  json: any,
  hosts: any,
  operationData: any
) => {
  const hostComponentStatusMap: any = {};
  const hostComponentIdMap: any = {};

  if (json.items) {
    json.items.forEach((host: any) => {
      if (host.host_components) {
        host.host_components.forEach((component: any) => {
          hostComponentStatusMap[component.id] = component.HostRoles.state;
          hostComponentIdMap[component.id] = component.HostRoles.component_name;
        });
      }
    });
  }

  return hosts
    .filter((host: any) =>
      host.hostComponents.some(
        (component: any) =>
          hostComponentIdMap[component] === operationData.realComponentName &&
          hostComponentStatusMap[component] === "INSTALLED"
      )
    )
    .map((host: any) => host.hostName);
};

const getSkippedForPassiveStateHosts = (hosts: any, repoVersion: any) => {
  const hostNames = hosts.map((host: any) => host.hostName);
  const outOfSyncHosts = get(repoVersion, "outOfSyncHosts", []);
  const hostNamesSkipped = outOfSyncHosts.filter((host: any) =>
    hostNames.includes(host)
  );
  return hostNamesSkipped;
};

const bulkOperation = (
  operationData: any,
  hosts: any[],
  clusterName: string,
  serviceComponentInfo: any,
  serviceModels: any,
  getKDCSessionState: (callback: () => Promise<void>) => Promise<void>
) => {
  if (operationData.componentNameFormatted) {
    if (operationData.action === "RESTART") {
      bulkOperationForHostComponentsRestart(
        operationData,
        hosts,
        clusterName,
        serviceModels
      );
    } else if (operationData.action === "ADD") {
      bulkOperationForHostComponentsAdd(
        operationData,
        hosts,
        clusterName,
        getKDCSessionState
      );
    } else if (operationData.action === "DELETE") {
      bulkOperationForHostComponentsDelete(
        operationData,
        hosts,
        clusterName,
        getKDCSessionState
      );
    } else {
      if (operationData.action.indexOf("DECOMMISSION") == -1) {
        bulkOperationForHostComponents(operationData, hosts, clusterName);
      } else {
        bulkOperationForHostComponentsDecommission(
          operationData,
          hosts,
          clusterName
        );
      }
    }
  } else {
    if (operationData.action === "SET_RACK_INFO") {
      bulkOperationForHostsSetRackInfo(operationData, hosts, clusterName);
    } else {
      if (operationData.action === "RESTART") {
        bulkOperationForHostsRestart(
          hosts,
          clusterName,
          serviceModels,
          serviceComponentInfo
        );
      } else if (operationData.action === "REINSTALL") {
        bulkOperationForHostsReinstall(
          operationData,
          hosts,
          clusterName,
          getKDCSessionState
        );
      } else if (operationData.action === "DELETE") {
        bulkOperationForHostsDelete(
          operationData,
          hosts,
          clusterName,
          getKDCSessionState,
          serviceComponentInfo,
          serviceModels
        );
      } else if (operationData.action === "CONFIGURE") {
        bulkOperationForHostsRefreshConfig(
          hosts,
          clusterName,
          serviceComponentInfo
        );
      } else {
        if (operationData.action === "PASSIVE_STATE") {
          bulkOperationForHostsPassiveState(operationData, hosts, clusterName);
        } else {
          bulkOperationForHosts(
            operationData,
            hosts,
            clusterName,
            serviceComponentInfo,
            serviceModels
          );
        }
      }
    }
  }
};

const bulkOperationForHostsSetRackInfo = (
  operationData: any,
  hosts: any[],
  clusterName: string
) => {
  setRackInfo(operationData, hosts, clusterName, operationData.callback);
};

const bulkOperationForHosts = (
  operationData: any,
  hosts: any[],
  clusterName: string,
  serviceComponentInfo: any,
  serviceModels: any
) => {
  const data = {
    hosts: hosts.map((host: any) => host.hostName),
    passiveState: "OFF",
    displayParams: ["host_components/HostRoles/component_name"],
  };
  getComponentsFromServer(data, clusterName, (responseData: any) => {
    getComponentsFromServerForHostsCallback(
      operationData,
      responseData,
      serviceComponentInfo,
      clusterName,
      serviceModels
    );
  });
};

const getComponentsFromServerForHostsCallback = async (
  operationData: any,
  data: any,
  serviceComponentInfo: any,
  clusterName: string,
  serviceModels: any
) => {
  const query = [];
  const hostNames = [];
  const hostsMap: any = {};

  data.items.forEach((host: any) => {
    host.host_components.forEach((hostComponent: any) => {
      if (
        !isClientUsingComponentName(
          hostComponent.HostRoles.component_name,
          serviceComponentInfo
        )
      ) {
        if (hostsMap[host.Hosts.host_name]) {
          hostsMap[host.Hosts.host_name].push(
            hostComponent.HostRoles.component_name
          );
        } else {
          hostsMap[host.Hosts.host_name] = [
            hostComponent.HostRoles.component_name,
          ];
        }
      }
    });
  });

  const nn_hosts = [];
  for (const hostName in hostsMap) {
    if (hostsMap.hasOwnProperty(hostName)) {
      const subQuery = `(HostRoles/component_name.in(${hostsMap[hostName].join(
        ","
      )})&HostRoles/host_name=${hostName})`;
      const components = hostsMap[hostName];

      if (components.length) {
        if (components.includes("NAMENODE")) {
          nn_hosts.push(hostName);
        }
        query.push(subQuery);
      }
      hostNames.push(hostName);
    }
  }

  const hostNamesStr = hostNames.join(",");
  if (query.length) {
    const queryStr = query.join("|");
    const isHDFSStarted =
      get(serviceModels, "hdfs.workStatus", "") === ComponentStatus.STARTED;
    const data = {
      query: queryStr,
      HostRoles: {
        state: operationData.action,
      },
      clusterName: clusterName,
      context: operationData.message,
      hostName: hostNamesStr,
      noOpsMessage: translate(
        "hosts.host.maintainance.allComponents.context"
      ) as string,
    };

    const request = async () => {
      const response = await HostsApi.updateHostComponents(
        clusterName,
        queryStr,
        data
      );
      bulkOperationForHostComponentsSuccessCallback(
        response,
        data.noOpsMessage
      );
    };

    if (operationData.action === "INSTALLED" && isHDFSStarted) {
      if (nn_hosts.length === 1) {
        checkNnLastCheckpointTime(request, nn_hosts[0], clusterName);
      }
      if (nn_hosts.length > 1) {
        //checkNnLastCheckpointTime(request); TODO: yet to be implemented from service side
      }
    } else {
      request();
    }
  } else {
    modalManager.show({
      modalTitle: "Nothing to do",
      modalBody:
        "All Host Components on selected hosts are already in selected state or in Maintenance Mode.",
      onClose: () => {},
      successCallback: () => {
        modalManager.hide();
      },
      options: {
        buttonSize: "sm" as "sm" | "lg" | undefined,
        cancelableViaIcon: true,
        cancelableViaBtn: false,
        okButtonVariant: "primary",
      },
    });
  }
};

const bulkOperationForHostComponentsSuccessCallback = (
  response: any,
  noOpsMessage: string
) => {
  if (
    isEmpty(get(response, "data", "")) &&
    get(response, "status", "") === 200
  ) {
    showRollingNothingToDoModal(
      translateWithVariables("rolling.nothingToDo.body", {
        "0":
          noOpsMessage ||
          (translate(
            "hosts.host.maintainance.allComponents.context"
          ) as string),
      })
    );
  } else {
    // modalManager.show(
    //   <BackgroundOperations
    //     isOpen={true}
    //     onClose={() => {
    //       modalManager.hide();
    //     }}
    //     requestId={get(response, "data.Requests.id", "")}
    //   />
    // );
  }
};

const bulkOperationForHostsRestart = (
  hosts: any,
  clusterName: string,
  serviceModels: any,
  serviceComponentInfo: any
) => {
  const data = {
    passiveState: "OFF",
    hosts: hosts.map((host: any) => host.hostName),
    displayParams: ["host_components/HostRoles/component_name"],
  };
  getComponentsFromServer(data, clusterName, (responseData: any) => {
    getComponentsFromServerForRestartCallback(
      responseData,
      serviceModels,
      clusterName,
      serviceComponentInfo
    );
  });
};

const getComponentsFromServerForRestartCallback = (
  data: any,
  serviceModels: any,
  clusterName: string,
  serviceComponentInfo: any
) => {
  const allComponents = getAllComponents(serviceComponentInfo);
  const hostComponents = data.items.flatMap((host: any) =>
    host.host_components.map((hostComponent: any) => ({
      componentName: hostComponent.HostRoles.component_name,
      hostName: host.Hosts.host_name,
      serviceName: allComponents.filter((c) => {
        return (
          c.HostRoles.component_name === hostComponent.HostRoles.component_name
        );
      })?.[0].HostRoles.service_name,
      clusterName: clusterName,
    }))
  );

  const isHDFSStarted =
    get(serviceModels, "hdfs.workStatus", "") === ComponentStatus.STARTED;
  const namenodes = hostComponents.filter(
    (hc: any) => hc.componentName === "NAMENODE"
  );
  const nn_count = namenodes.length;

  if (nn_count === 1 && isHDFSStarted) {
    const hostName = namenodes[0].hostName;
    checkNnLastCheckpointTime(
      restartHostComponents(
        hostComponents,
        "Restart all components on the selected hosts",
        "HOST"
      ),
      hostName,
      clusterName
    );
  } else if (nn_count > 1 && isHDFSStarted) {
    // checkNnLastCheckpointTime(restartHostComponents(hostComponents, "Restart all components on the selected hosts", "HOST")); TODO: yet to be implemented from service side
  } else {
    restartHostComponents(
      hostComponents,
      "Restart all components on the selected hosts",
      "HOST"
    );
  }
};

const bulkOperationForHostsRefreshConfig = (
  hosts: any,
  clusterName: string,
  serviceComponentInfo: any
) => {
  const data = {
    passiveState: "OFF",
    hosts: hosts.map((host: any) => host.hostName),
    displayParams: ["host_components/HostRoles/component_name"],
  };
  getComponentsFromServer(data, clusterName, (responseData: any) => {
    getComponentsFromServerForRefreshConfigsCallback(
      responseData,
      clusterName,
      serviceComponentInfo
    );
  });
};

const getComponentsFromServerForRefreshConfigsCallback = (
  data: any,
  clusterName: string,
  serviceComponentInfo: any
) => {
  const hostComponents: any[] = [];
  const allComponents = getAllComponents(serviceComponentInfo);
  const clients = allComponents
    .filter((c) => get(c, "HostRoles.component_category") === "CLIENT")
    .map((c) => c.HostRoles.component_name);
  data.items.forEach((host: any) => {
    host.host_components.forEach((hostComponent: any) => {
      if (clients.includes(hostComponent.HostRoles.component_name)) {
        hostComponents.push({
          componentName: hostComponent.HostRoles.component_name,
          hostName: host.Hosts.host_name,
          serviceName: allComponents.filter((c) => {
            return (
              c.HostRoles.component_name ===
              hostComponent.HostRoles.component_name
            );
          })?.[0].HostRoles.service_name,
          clusterName: clusterName,
        });
      }
    });
  });
  restartHostComponents(
    hostComponents,
    translate("rollingrestart.context.configs.allOnSelectedHosts") as string,
    "HOST"
  );
};

const bulkOperationForHostsPassiveState = (
  operationData: any,
  hosts: any,
  clusterName: string
) => {
  const data = {
    hosts: hosts.map((host: any) => host.hostName),
    displayParams: ["Hosts/maintenance_state"],
  };
  getComponentsFromServer(data, clusterName, (responseData: any) => {
    getComponentsFromServerForPassiveStateCallback(
      operationData,
      responseData,
      clusterName
    );
  });
};

const getComponentsFromServerForPassiveStateCallback = async (
  operationData: any,
  data: any,
  clusterName: string
) => {
  const hostNames = data.items
    .filter((host: any) => host.Hosts.maintenance_state !== operationData.state)
    .map((host: any) => host.Hosts.host_name);

  if (hostNames.length) {
    const requestData = {
      RequestInfo: {
        context: operationData.message,
        query: "Hosts/host_name.in(" + hostNames.join(",") + ")",
      },
      Body: {
        Hosts: {
          maintenance_state: operationData.state,
        },
      },
    };
    await HostsApi.updateHost(clusterName, requestData);
    updateHostPassiveState(operationData.state);
  } else {
    showRollingNothingToDoModal(
      translate("hosts.bulkOperation.passiveState.nothingToDo.body")
    );
  }
};

const updateHostPassiveState = (state: any) => {
  infoPassiveState(state);
};

const bulkOperationForHostsReinstall = async (
  operationData: any,
  hosts: any,
  clusterName: string,
  getKDCSessionState: (callback: () => Promise<void>) => Promise<void>
) => {
  await getKDCSessionState(async () => {
    const data = {
      HostRoles: {
        state: "INSTALLED",
      },
      query: `HostRoles/host_name.in(${hosts
        .map((host: any) => host.hostName)
        .join(",")})&HostRoles/state=INSTALL_FAILED`,
      context: operationData.message,
      clusterName: clusterName,
      noOpsMessage: translate(
        "hosts.host.maintainance.reinstallFailedComponents.context"
      ) as string,
    };
    try {
      const response = await HostsApi.updateHostComponents(
        clusterName,
        data.query,
        data
      );
      bulkOperationForHostComponentsSuccessCallback(
        response,
        data.noOpsMessage
      );
    } catch (error) {
      const message = operationData.message + get(error, "message", "");
      showErrorModal(message);
    }
  });
};

const bulkOperationForHostComponents = (
  operationData: any,
  hosts: any,
  clusterName: string
) => {
  const data = {
    components: [operationData.componentName],
    hosts: hosts.map((host: any) => host.hostName),
    passiveState: "OFF",
  };
  getComponentsFromServer(data, clusterName, (responseData: any) => {
    getComponentsFromServerForHostComponentsCallback(
      operationData,
      responseData,
      clusterName
    );
  });
};

const getComponentsFromServerForHostComponentsCallback = async (
  operationData: any,
  responseData: any,
  clusterName: string
) => {
  const hostsWithComponentInProperState = responseData.items.map(
    (item: any) => item.Hosts.host_name
  );
  const data = {
    HostRoles: {
      state: operationData.action,
    },
    query: `HostRoles/component_name=${
      operationData.componentName
    }&HostRoles/host_name.in(${hostsWithComponentInProperState.join(
      ","
    )})&HostRoles/maintenance_state=OFF`,
    clusterName: clusterName,
    context: `${operationData.message} ${operationData.componentNameFormatted}`,
    level: "SERVICE",
    noOpsMessage: operationData.componentNameFormatted,
  };
  try {
    const response = await HostsApi.updateHostComponents(
      clusterName,
      data.query,
      data
    );
    bulkOperationForHostComponentsSuccessCallback(response, data.noOpsMessage);
  } catch (error) {
    const message = operationData.message + " " + get(error, "message", "");
    showErrorModal(message);
  }
};

const bulkOperationForHostComponentsDelete = (
  operationData: any,
  hosts: any,
  clusterName: string,
  getKDCSessionState: (callback: () => Promise<void>) => Promise<void>
) => {
  const data = {
    components: [operationData.componentName],
    hosts: hosts.map((host: any) => host.hostName),
    displayParams: ["host_components/HostRoles/state"],
  };
  getComponentsFromServer(data, clusterName, (responseData: any) => {
    getComponentsFromServerForHostComponentsDeleteCallback(
      operationData,
      responseData,
      hosts,
      clusterName,
      getKDCSessionState
    );
  });
};

const getComponentsFromServerForHostComponentsDeleteCallback = (
  operationData: any,
  data: any,
  requestedHosts: any,
  clusterName: string,
  getKDCSessionState: (callback: () => Promise<void>) => Promise<void>
) => {
  const minToInstall = get(operationData, "minToInstall", 0);
  const installedCount = get(operationData, "installedCount", 0);
  const installedHosts = get(data, "items", []).map((item: any) =>
    get(item, "Hosts.host_name", "")
  );
  const hostsToDelete = get(data, "items", [])
    .filter((item: any) => {
      const state = get(item, "host_components[0].HostRoles.state");
      return [
        ComponentStatus.STOPPED,
        ComponentStatus.UNKNOWN,
        ComponentStatus.INSTALL_FAILED,
        ComponentStatus.UPGRADE_FAILED,
        ComponentStatus.INIT,
      ].includes(state);
    })
    .map((item: any) => get(item, "Hosts.host_name", ""));
  if (installedCount - hostsToDelete.length < minToInstall) {
    showRollingNothingToDoModal(
      translateWithVariables(
        "hosts.bulkOperation.confirmation.delete.component.minimum.body",
        {
          "0": minToInstall,
          "1": operationData.componentNameFormatted,
        }
      )
    );
  } else {
    const hostsNotToDelete: any = [];
    requestedHosts
      .map((host: any) => get(host, "hostName"))
      .forEach((host: string) => {
        if (!hostsToDelete.includes(host)) {
          const hostToSkip = {
            error: {
              key: host,
              message: null,
            },
            isCollapsed: true,
          };
          if (installedHosts.includes(host)) {
            hostToSkip.error.message = translateWithVariables(
              "hosts.bulkOperation.confirmation.delete.component.notStopped",
              {
                "0": operationData.componentNameFormatted,
              }
            ) as any;
          } else {
            hostToSkip.error.message = translateWithVariables(
              "hosts.bulkOperation.confirmation.delete.component.notInstalled",
              {
                "0": operationData.componentNameFormatted,
              }
            ) as any;
          }
          hostsNotToDelete.push(hostToSkip);
        }
      });

    const modalMessages = {
      header: hostsToDelete.length
        ? translate("hosts.bulkOperation.confirmation.header")
        : translate("rolling.nothingToDo.header"),
      bodyModifyMessage: translateWithVariables(
        "hosts.bulkOperation.confirmation.delete.component",
        {
          "0": operationData.componentNameFormatted,
        }
      ),
      bodySkipMessage: hostsToDelete.length
        ? translate("hosts.bulkOperation.confirmation.delete.component.cannot1")
        : translateWithVariables(
            "hosts.bulkOperation.confirmation.delete.component.cannot2",
            {
              "0": operationData.componentNameFormatted,
            }
          ),
    };
    modalManager.show(
      <DeleteHostComponentsModal
        isOpen={true}
        onClose={() => {
          modalManager.hide();
        }}
        hostsToDelete={hostsToDelete}
        hostsNotToDelete={hostsNotToDelete}
        modalMessages={modalMessages}
        successCallback={() => {
          if (hostsToDelete.length) {
            bulkDeleteHostComponents(
              operationData,
              hostsToDelete,
              clusterName,
              getKDCSessionState
            );
          }
          modalManager.hide();
        }}
        okButtonText={
          translate("hosts.host.deleteComponent.popup.confirm") as string
        }
        okButtonVariant={"warning"}
      />
    );
  }
};

const bulkDeleteHostComponents = async (
  operationData: any,
  hostNames: string[],
  clusterName: string,
  getKDCSessionState: (callback: () => Promise<void>) => Promise<void>
) => {
  await getKDCSessionState(async () => {
    const data = JSON.stringify({
      RequestInfo: {
        query:
          "HostRoles/host_name.in(" +
          hostNames.join(",") +
          ")&HostRoles/component_name.in(" +
          operationData.componentName +
          ")",
      },
    });
    try {
      const response = await HostsApi.deleteHostComponents(data, clusterName);
      bulkOperationForHostComponentsDeleteCallback(
        response,
        operationData,
        hostNames
      );
    } catch (error) {
      showErrorModal(operationData.message + " " + get(error, "message", ""));
    }
  });
};

const bulkOperationForHostComponentsDeleteCallback = (
  response: any,
  operationData: any,
  hostNames: any
) => {
  let deletedHosts = [];
  let undeletableHosts = [];
  if (get(response, "statusText", "").toLowerCase() === "error") {
    const host = {
      error: {
        key: hostNames.join(","),
        code: get(response, "status", ""),
        message: get(response, "data.message", ""),
      },
      isCollapsed: true,
    };
    undeletableHosts.push(host);
  } else {
    deletedHosts = hostNames;
    const data = get(response, "data", {});
    if (!isEmpty(data)) {
      get(data, "deleteResult", []).forEach((host: any) => {
        if (!get(host, "deleted", "")) {
          undeletableHosts.push({
            error: get(host, "error", ""),
            isCollapsed: true,
          });
        }
      });
    }
  }

  const modalMessages = {
    header: translate("hosts.bulkOperation.delete.component.result.header"),
    bodyModifyMessage:
      operationData.componentNameFormatted +
      " " +
      translate("hosts.bulkOperation.delete.component.result.body"),
    bodySkipMessage: translateWithVariables(
      "hosts.bulkOperation.delete.component.dryRun.message",
      {
        "0": operationData.componentNameFormatted,
      }
    ),
  };
  modalManager.show(
    <DeleteHostComponentsModal
      isOpen={true}
      onClose={() => {
        modalManager.hide();
      }}
      hostsToDelete={deletedHosts}
      hostsNotToDelete={undeletableHosts}
      modalMessages={modalMessages}
      successCallback={() => {
        window.location.reload();
        modalManager.hide();
      }}
      okButtonText={"OK"}
      okButtonVariant={"success"}
    />
  );
};

const bulkOperationForHostComponentsAdd = (
  operationData: any,
  hosts: any,
  clusterName: string,
  getKDCSessionState: (callback: () => Promise<void>) => Promise<void>
) => {
  const data = {
    components: [operationData.componentName],
    hosts: hosts.map((host: any) => host.hostName),
  };
  getComponentsFromServer(data, clusterName, (responseData: any) => {
    getComponentsFromServerForHostComponentsAddCallback(
      operationData,
      responseData,
      hosts,
      clusterName,
      getKDCSessionState
    );
  });
};

const getComponentsFromServerForHostComponentsAddCallback = (
  operationData: any,
  data: any,
  hosts: any,
  clusterName: string,
  getKDCSessionState: (callback: () => Promise<void>) => Promise<void>
) => {
  const allHostsWithComponent = data.items.map((item: any) =>
    get(item, "Hosts.host_name", "")
  );
  const hostsWithComponent: any[] = [];
  hosts.forEach((host: any) => {
    const isNotHeartBeating = host.state === "HEARTBEAT_LOST";
    if (allHostsWithComponent.includes(host.hostName) || isNotHeartBeating) {
      hostsWithComponent.push({
        error: {
          key: host.hostName,
          message: isNotHeartBeating
            ? translate(
                "hosts.bulkOperation.confirmation.add.component.noHeartBeat.skip"
              )
            : translateWithVariables(
                "hosts.bulkOperation.confirmation.add.component.skip",
                {
                  "0": operationData.componentNameFormatted,
                }
              ),
        },
        isCollapsed: true,
      });
    }
  });

  const hostsWithOutComponent = hosts
    .filter(
      (host: any) =>
        !hostsWithComponent.find((h) => h.error.key === host.hostName)
    )
    .map((host: any) => host.hostName);

  const modalMessages = {
    header: hostsWithOutComponent.length
      ? translate("hosts.bulkOperation.confirmation.header")
      : translate("rolling.nothingToDo.header"),
    bodyModifyMessage: translateWithVariables(
      "hosts.bulkOperation.confirmation.add.component",
      {
        "0": operationData.componentNameFormatted,
      }
    ),
    bodySkipMessage: hostsWithOutComponent.length
      ? translate("hosts.bulkOperation.confirmation.cannot.add1")
      : translateWithVariables("hosts.bulkOperation.confirmation.cannot.add2", {
          "0": operationData.componentNameFormatted,
        }),
  };

  modalManager.show(
    <DeleteHostComponentsModal
      isOpen={true}
      onClose={() => {
        modalManager.hide();
      }}
      hostsToDelete={hostsWithOutComponent}
      hostsNotToDelete={hostsWithComponent}
      modalMessages={modalMessages}
      successCallback={() => {
        if (hostsWithOutComponent.length) {
          bulkAddHostComponents(
            operationData,
            hostsWithOutComponent,
            clusterName,
            getKDCSessionState
          );
        }
        modalManager.hide();
      }}
      okButtonText={
        translate("hosts.host.addComponent.popup.confirm") as string
      }
      okButtonVariant={"warning"}
    />
  );
};

const bulkAddHostComponents = async (
  operationData: any,
  hostNames: any,
  clusterName: string,
  getKDCSessionState: (callback: () => Promise<void>) => Promise<void>
) => {
  await getKDCSessionState(async () => {
    const context =
      operationData.message + " " + operationData.componentNameFormatted;
    const data = JSON.stringify({
      RequestInfo: {
        query: "Hosts/host_name.in(" + hostNames.join(",") + ")",
      },
      Body: {
        host_components: [
          {
            HostRoles: {
              component_name: operationData.componentName,
            },
          },
        ],
      },
    });
    try {
      await HostsApi.registerHostToComponent(clusterName, data);
      bulkOperationForHostComponentsAddSuccessCallback(context, clusterName);
    } catch (error) {
      showErrorModal(context + " " + get(error, "message", ""));
    }
  });
};

const bulkOperationForHostComponentsAddSuccessCallback = async (
  context: string,
  clusterName: string
) => {
  const data = {
    query: "HostRoles/state=INIT",
    HostRoles: {
      state: "INSTALLED",
    },
    context: context,
  };
  try {
    const response = await HostsApi.updateHostComponents(
      clusterName,
      data.query,
      data
    );
    bulkOperationForHostComponentsSuccessCallback(response, context);
  } catch (error) {
    showErrorModal(context + " " + get(error, "message", ""));
  }
};

const bulkOperationForHostsDelete = (
  operationData: any,
  hosts: any,
  clusterName: string,
  getKDCSessionState: (callback: () => Promise<void>) => Promise<void>,
  serviceComponentInfo: any,
  serviceModels: any
) => {
  let hostNamesToDelete: string[] = [];
  let hostsNotToDelete: any[] = [];
  const createNonDeletableComponents = (hostName: string, message: string) => {
    return {
      error: {
        key: hostName,
        message: message,
      },
      isCollapsed: true,
    };
  };

  const clusterComponents = get(operationData, "clusterComponents", []);
  const allComponents = getAllComponents(serviceComponentInfo);

  hosts.forEach((host: any) => {
    const hostComponents = get(host, "allHostComponents", []).map((c: any) => {
      const componentData = allComponents.find(
        (component: any) =>
          component.HostRoles.component_name ===
          get(c, "HostRoles.component_name", "")
      );
      return getPopulatedHostComponentObject({
        HostRoles: {
          ...get(componentData, "HostRoles", {}),
          ...get(c, "HostRoles", {}),
        },
      });
    });

    const hostInfo = getHostComponentsInfo(
      hostComponents,
      clusterComponents,
      serviceModels
    );
    if (hostInfo.nonDeletableComponents.length > 0) {
      hostsNotToDelete.push(
        createNonDeletableComponents(
          host.hostName,
          translateWithVariables(
            "hosts.bulkOperation.deleteHosts.nonDeletableComponents",
            {
              "0": hostInfo.nonDeletableComponents.join(", "),
            }
          ) as string
        )
      );
    } else if (hostInfo.nonAddableMasterComponents.length > 0) {
      hostsNotToDelete.push(
        createNonDeletableComponents(
          host.hostName,
          translateWithVariables(
            "hosts.bulkOperation.deleteHosts.nonAddableMasterComponents",
            {
              "0": hostInfo.nonAddableMasterComponents.join(", "),
            }
          ) as string
        )
      );
    } else if (hostInfo.lastMasterComponents.length > 0) {
      hostsNotToDelete.push(
        createNonDeletableComponents(
          host.hostName,
          translateWithVariables(
            "hosts.bulkOperation.deleteHosts.lastMasterComponents",
            {
              "0": hostInfo.lastMasterComponents.join(", "),
            }
          ) as string
        )
      );
    } else if (hostInfo.runningComponents.length > 0) {
      hostsNotToDelete.push(
        createNonDeletableComponents(
          host.hostName,
          translateWithVariables(
            "hosts.bulkOperation.deleteHosts.runningComponents",
            {
              "0": hostInfo.runningComponents.join(", "),
            }
          ) as string
        )
      );
    } else {
      hostNamesToDelete.push(host.hostName);
    }
  });

  const modalMessages = {
    header: hostNamesToDelete.length
      ? translate("hosts.bulkOperation.deleteHosts.confirm.header")
      : translate("rolling.nothingToDo.header"),
    bodyModifyMessage: translate(
      "hosts.bulkOperation.deleteHosts.confirm.delete"
    ),
    bodySkipMessage: hostNamesToDelete.length
      ? translate("hosts.bulkOperation.deleteHosts.cannot.delete1")
      : translate("hosts.bulkOperation.deleteHosts.cannot.delete2"),
  };

  modalManager.show(
    <DeleteHostComponentsModal
      isOpen={true}
      onClose={() => {
        modalManager.hide();
      }}
      hostsToDelete={hostNamesToDelete}
      hostsNotToDelete={hostsNotToDelete}
      modalMessages={modalMessages}
      successCallback={() => {
        modalManager.hide();
        if (hostNamesToDelete.length) {
          bulkOperationForHostsDeleteSuccess(
            operationData,
            hostNamesToDelete,
            clusterName,
            getKDCSessionState
          );
        }
      }}
      okButtonText={
        translate("hosts.host.deleteComponent.popup.confirm") as string
      }
      okButtonVariant={"warning"}
    />
  );
};

const bulkOperationForHostsDeleteSuccess = async (
  operationData: any,
  hosts: string[],
  clusterName: string,
  getKDCSessionState: (callback: () => Promise<void>) => Promise<void>
) => {
  await getKDCSessionState(async () => {
    modalManager.show(
      <ConfirmDeleteHostModal
        isOpen={true}
        onClose={() => {
          modalManager.hide();
        }}
        successCallback={() => {
          modalManager.hide();
          bulkOperationForHostsDeleteSuccessApi(
            operationData,
            hosts,
            clusterName
          );
        }}
        confirmKey="delete"
      />
    );
  });
};

const bulkOperationForHostsDeleteSuccessApi = async (
  operationData: any,
  hosts: string[],
  clusterName: string
) => {
  const data = {
    query: "Hosts/host_name.in(" + hosts.join(",") + ")",
    hosts: hosts,
  };
  try {
    const response = await HostsApi.deleteHosts(clusterName, data.query, data);
    bulkOperationForHostsDeleteSuccessCallback(response, hosts);
  } catch (error) {
    showErrorModal(operationData.message + " " + get(error, "message", ""));
  }
};

const bulkOperationForHostsDeleteSuccessCallback = (
  response: any,
  hosts: string[]
) => {
  let deletedHosts: any[] = [];
  let undeletableHosts: any[] = [];
  if (get(response, "statusText", "").toLowerCase() === "error") {
    const host = {
      error: {
        key: hosts.join(","),
        code: get(response, "status", ""),
        message: get(response, "data.message", ""),
      },
      isCollapsed: true,
    };
    undeletableHosts.push(host);
  } else {
    deletedHosts = hosts;
    const data = get(response, "data", {});
    if (!isEmpty(data)) {
      get(data, "deleteResult", []).forEach((host: any) => {
        if (!get(host, "deleted", "")) {
          undeletableHosts.push({
            error: get(host, "error", ""),
            isCollapsed: true,
          });
        }
      });
    }
  }

  const modalMessages = {
    header: translate("hosts.bulkOperation.deleteHosts.result.header"),
    bodyModifyMessage: translate("hosts.bulkOperation.deleteHosts.result.body"),
    bodySkipMessage: translateWithVariables(
      "hosts.bulkOperation.deleteHosts.dryRun.message",
      {
        "0": undeletableHosts.length.toString(),
      }
    ) as string,
  };
  modalManager.show(
    <DeleteHostComponentsModal
      isOpen={true}
      onClose={() => {
        window.location.reload();
        modalManager.hide();
      }}
      hostsToDelete={deletedHosts}
      hostsNotToDelete={undeletableHosts}
      modalMessages={modalMessages}
      successCallback={() => {
        window.location.reload();
        modalManager.hide();
      }}
      okButtonText={"OK"}
      okButtonVariant={"success"}
    />
  );
};

const bulkOperationForHostComponentsDecommission = (
  operationData: any,
  hosts: any,
  clusterName: string
) => {
  const data = {
    components: [operationData.realComponentName],
    hosts: hosts.map((host: any) => host.hostName),
    passiveState: "OFF",
    displayParams: ["host_components/HostRoles/state"],
  };
  getComponentsFromServer(data, clusterName, (responseData: any) => {
    getComponentsFromServerForHostComponentsDecommissionCallBack(
      operationData,
      responseData,
      clusterName
    );
  });
};

const getComponentsFromServerForHostComponentsDecommissionCallBack = async (
  operationData: any,
  data: any,
  clusterName: string
) => {
  const svcName = get(operationData, "serviceName", "");
  const componentName = get(operationData, "componentName", "");
  let components: any[] = [];
  get(data, "items", []).forEach((host: any) => {
    get(host, "host_components", []).forEach((hostComponent: any) => {
      components.push({
        componentName: get(hostComponent, "HostRoles.component_name", ""),
        hostName: get(host, "Hosts.host_name", ""),
        workStatus: get(hostComponent, "HostRoles.state", ""),
      });
    });
  });

  if (components.length) {
    let hostsWithComponentInProperState = components.map(
      (component: any) => component.hostName
    );
    const turnOff = get(operationData, "action", "").indexOf("OFF") !== -1;
    const masterName = get(operationData, "componentName", "");
    const slaveName = get(operationData, "realComponentName", "");
    const hostNames = hostsWithComponentInProperState.join(",");
    if (turnOff) {
      if (["YARN", "HBASE", "HDFS"].includes(svcName)) {
        doRecommissionAndStart(
          hostNames,
          svcName,
          masterName,
          slaveName,
          clusterName
        );
      }
    } else {
      hostsWithComponentInProperState = components
        .filter(
          (component: any) => component.workStatus === ComponentStatus.STARTED
        )
        .map((component: any) => component.hostName);
      if (svcName === "HBASE") {
        warnBeforeDecommission(hostNames, clusterName);
      } else {
        const parameters = {
          slave_type: slaveName,
        };
        const contextString = turnOff
          ? "hosts.host." + slaveName.toLowerCase() + ".recommission"
          : "hosts.host." + slaveName.toLowerCase() + ".decommission";
        if (turnOff) {
          set(
            parameters,
            "included_hosts",
            hostsWithComponentInProperState.join(",")
          );
        } else {
          set(
            parameters,
            "excluded_hosts",
            hostsWithComponentInProperState.join(",")
          );
        }
        const data = JSON.stringify({
          RequestInfo: {
            context: translate(contextString),
            command: "DECOMMISSION",
            parameters: parameters,
            operation_level: {
              level: "CLUSTER",
              cluster_name: clusterName,
            },
          },
          "Requests/resource_filters": [
            { service_name: svcName, component_name: componentName },
          ],
        });
        try {
          const response = await HostsApi.clusterRequests(clusterName, data);
          bulkOperationForHostComponentsSuccessCallback(
            response,
            operationData.componentNameFormatted
          );
        } catch (error) {
          showErrorModal(
            operationData.message + " " + get(error, "message", "")
          );
        }
      }
    }
  } else {
    showRollingNothingToDoModal(
      translateWithVariables("rolling.nothingToDo.body", {
        "0": operationData.componentNameFormatted,
      })
    );
  }
};

const warnBeforeDecommission = async (
  hostNames: string,
  clusterName: string
) => {
  try {
    const response = await HostsApi.getRegionServerPassiveState(
      clusterName,
      hostNames
    );
    warnBeforeDecommissionSuccess(response, hostNames, clusterName);
  } catch (error) {
    showErrorModal(get(error, "message", ""));
  }
};

const warnBeforeDecommissionSuccess = (
  data: any,
  hostNames: string,
  clusterName: string
) => {
  if (get(data, "items", []).length) {
    showHbaseActiveWarning();
  } else {
    checkRegionServerState(hostNames, clusterName);
  }
};

const checkRegionServerState = async (
  hostNames: string,
  clusterName: string
) => {
  try {
    const response = await HostsApi.getResgionServerInService(clusterName);
    checkRegionServerStateSuccessCallback(response, hostNames, clusterName);
  } catch (error) {
    showErrorModal(get(error, "message", ""));
  }
};

const checkRegionServerStateSuccessCallback = (
  data: any,
  hostNames: string,
  clusterName: string
) => {
  const hostArray = hostNames.split(",");
  const decommissionPossible =
    get(data, "items", [])
      .map((item: any) => get(item, "Hosts.host_name", ""))
      .filter((host: string) => !hostArray.includes(host)).length >= 1;
  if (decommissionPossible) {
    doDecommissionRegionServer(
      hostNames,
      "HBASE",
      "HBASE_MASTER",
      "HBASE_REGIONSERVER",
      clusterName
    );
  } else {
    showRegionServerWarning();
  }
};

const bulkOperationForHostComponentsRestart = (
  operationData: any,
  hosts: any,
  clusterName: string,
  serviceModels: any
) => {
  const data = {
    components: [operationData.componentName],
    hosts: hosts.map((host: any) => host.hostName),
    passiveState: "OFF",
    displayParams: [
      "Hosts/maintenance_state",
      "host_components/HostRoles/stale_configs",
      "host_components/HostRoles/maintenance_state",
    ],
  };
  getComponentsFromServer(data, clusterName, (responseData: any) => {
    getComponentsFromServerForHostComponentsRestartCallback(
      operationData,
      responseData,
      clusterName,
      serviceModels
    );
  });
};

const getComponentsFromServerForHostComponentsRestartCallback = (
  operationData: any,
  data: any,
  clusterName: string,
  serviceModels: any
) => {
  const wrappedHostComponents: any = [];
  const serviceName = get(
    serviceNameDisplayMapping,
    operationData.serviceName,
    ""
  );
  const service = filter(
    serviceModels,
    (service: any) => service.serviceName === serviceName
  )?.[0];

  data.items.forEach((host: any) => {
    host.host_components.forEach((hostComponent: any) => {
      wrappedHostComponents.push({
        componentName: hostComponent.HostRoles.component_name,
        serviceName: serviceName,
        hostName: host.Hosts.host_name,
        hostPassiveState: host.Hosts.maintenance_state,
        staleConfigs: hostComponent.HostRoles.stale_configs,
        passiveState: hostComponent.HostRoles.maintenance_state,
      });
    });
  });

  if (wrappedHostComponents.length) {
    return showRollingRestartPopup(
      operationData.componentDisplayName,
      serviceName,
      get(service, "passiveState", "OFF") === "ON",
      false,
      wrappedHostComponents,
      clusterName
    );
  } else {
    showRollingNothingToDoModal(
      translateWithVariables("rolling.nothingToDo.body", {
        "0": operationData.componentNameFormatted,
      })
    );
  }
};
