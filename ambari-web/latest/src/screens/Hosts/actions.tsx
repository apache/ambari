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

import { capitalize, cloneDeep, get, set, uniq } from "lodash";
import { HostsApi } from "../../api/hostsApi";
import {
  doDecommissionRegionServer,
  doRecommissionAndStart,
  getComponentDisplayName,
  getComponentName,
  installHostComponentCall,
  parseNnCheckPointTime,
  showHbaseActiveWarning,
  showRegionServerWarning,
} from "./utils";
import { ComponentStatus } from "./enums";
import { defaultSuccessCallback, defaultSuccessCallbackWithoutReload, restartHostComponents } from "./batchUtils";
import modalManager from "../../store/ModalManager";
import { nnCheckpointAgeAlertThreshold } from "../../data/configs/services/config";
import { IHostComponent } from "../../models/hostComponent";
import BackgroundOperations from "../BackgroundOperations";
import { t } from "i18next";
import {
  CompatibleComponent,
  ComponentDependency,
} from "./utils/ComponentDependency";
import {
  showAlertModal,
  showErrorModal,
  translate,
  translateWithVariables,
} from "../../Utils/Utility";
import { addDeleteComponentsMap } from "../../Utils/Utility";
import RecommendationModal from "../../components/RecommendationModal";
import ConfirmationModal from "../../components/ConfirmationModal";
import { IHost } from "../../models/host";
import ConfigsApi from "../../api/configsApi";

export const sendComponentCommand = async (
  component: IHostComponent,
  context: string,
  state: string
) => {
  const clusterName = get(component, "clusterName", "");
  const hostName = get(component, "hostName", "");
  const componentName = getComponentName(component);
  const data = {
    RequestInfo: {
      context: context,
      operation_level: {
        level: "HOST_COMPONENT",
        cluster_name: clusterName,
        host_name: hostName,
        service_name: get(component, "serviceName", null),
      },
    },
    Body: {
      HostRoles: {
        state: state,
      },
    },
  };

  const response = await HostsApi.updateHostComponentForHost(
    clusterName,
    hostName,
    componentName,
    data
  );
  const requestId = get(response, "Requests.id", -1);
  if (requestId !== -1) {
    modalManager.show(
      <BackgroundOperations
        isOpen={true}
        onClose={() => {
          modalManager.hide();
        }}
        requestId={requestId}
      />
    );
  }
};

export const startComponent = async (component: IHostComponent) => {
  const context = "Start " + getComponentDisplayName(component);
  await sendComponentCommand(component, context, ComponentStatus.STARTED);
};

export const stopComponent = async (component: IHostComponent) => {
  const context = "Stop " + getComponentDisplayName(component);
  await sendComponentCommand(component, context, ComponentStatus.STOPPED);
};

export const restartComponent = async (component: IHostComponent) => {
  const context = "Restart " + getComponentDisplayName(component);
  await restartHostComponents([component], context, "HOST_COMPONENT");
};

export const restartAllStaleConfigComponents = async (
  staleComponents: IHostComponent[]
) => {
  const hostName = get(staleComponents, "[0].hostName", "");
  const context = `Restart components with Stale Configs on ${hostName}`;
  await restartHostComponents(staleComponents, context, "HOST");
};

export const checkNnLastCheckpointTime = async (
  callback: () => void | Promise<void>,
  hostNames: string | string[],
  clusterName: string
) => {
  const names = Array.isArray(hostNames) ? hostNames : [hostNames];
  const checkpointStates = await Promise.all(
    names.map(async (hostName) => {
      try {
        return await pullNnCheckPointTime(hostName, clusterName);
      } catch {
        return null;
      }
    })
  );
  const oldCheckpointHosts = checkpointStates.filter(
    (hostName): hostName is string => typeof hostName === "string" && Boolean(hostName)
  );
  const isCheckpointUnavailable = checkpointStates.some(
    (checkpointState) => checkpointState == null
  );

  const continueOperation = async () => {
    modalManager.hide();
    await callback();
  };

  if (oldCheckpointHosts.length) {
    let hdfsUser = "<hdfs-user>";
    try {
      const currentConfigs = await ConfigsApi.getConfigValues(clusterName, "HDFS");
      const hadoopEnv = get(currentConfigs, "items", [])
        .flatMap((item: Record<string, unknown>) => get(item, "configurations", []))
        .find((config: Record<string, unknown>) => config.type === "hadoop-env");
      hdfsUser = get(hadoopEnv, "properties.hdfs_user", hdfsUser);
    } catch {
      // The operation can still proceed with the same placeholder used by classic Ambari.
    }
    const hosts = oldCheckpointHosts.join(", ");
    const msg =
      `The last HDFS checkpoint is older than ${nnCheckpointAgeAlertThreshold} hours. ` +
      "Make sure that you have taken a checkpoint before proceeding. Otherwise, the NameNode(s) can take a very long time to start up." +
      `\n\n1. Login to the NameNode host${oldCheckpointHosts.length > 1 ? "s" : ""} ${hosts}` +
      "\n\n2. Put the NameNode in Safe Mode (read-only mode): \n\n" +
      `    sudo su ${hdfsUser} -l -c 'hdfs dfsadmin -safemode enter'` +
      "\n\n3. Once in Safe Mode, create a Checkpoint:\n\n" +
      `    sudo su ${hdfsUser} -l -c 'hdfs dfsadmin -saveNamespace'` +
      "\n";
    const modalProps = {
      modalTitle: "Warning",
      modalBody: msg,
      onClose: () => {},
      successCallback: continueOperation,
      options: {
        okButtonText: "Next",
        okButtonVariant: "primary",
        buttonSize: "sm" as "sm" | "lg" | undefined,
        cancelableViaIcon: true,
        cancelableViaBtn: true,
      },
    };
    modalManager.show(modalProps);
  } else if (isCheckpointUnavailable) {
    const modalProps = {
      modalTitle: "Warning",
      modalBody:
        "Could not determine the age of the last HDFS checkpoint. Please ensure that you have a recent checkpoint. Otherwise, the NameNode(s) can take a very long time to start up.",
      onClose: () => {},
      successCallback: continueOperation,
      options: {
        okButtonText: "Proceed Anyway",
        okButtonVariant: "danger",
        buttonSize: "sm" as "sm" | "lg" | undefined,
        cancelableViaIcon: true,
        cancelableViaBtn: true,
      },
    };
    modalManager.show(modalProps);
  } else {
    await callback();
  }
};

const pullNnCheckPointTime = async (hostName: string, clusterName: string) => {
  const response = await HostsApi.getNnCheckPointTime(clusterName, hostName);
  return parseNnCheckPointTime(response);
};

export const toggleMaintenanceMode = async (component: IHostComponent) => {
  if (component.isImpliedState()) return null;

  const hostname = get(component, "hostName");
  const clusterName = get(component, "clusterName");
  const componentName = get(component, "componentName");
  const state = get(component, "passiveState") === "ON" ? "OFF" : "ON";
  const displayName = get(component, "displayName");
  let message = t(
    "passiveState.turn" + capitalize(state.toString()) + "For"
  ).replace("{0}", displayName);

  const data = JSON.stringify({
    RequestInfo: {
      context: message,
    },
    Body: {
      HostRoles: {
        maintenance_state: state,
      },
    },
  });
  await HostsApi.updateHostComponentForHost(
    clusterName,
    hostname,
    componentName,
    data
  );
};

function assert(condition: any, message: any) {
  if (!condition) {
    throw new Error(message);
  }
}

const checkComponentDependencies = (
  data: any,
  component: IHostComponent,
  opt: any
) => {
  var opt = opt || {};
  opt.scope = opt.scope || "*";
  var installedComponents;
  switch (opt.scope) {
    case "host":
      assert(
        "You should pass at least `hostName` or `installedComponents` to options.",
        opt.hostName || opt.installedComponents
      );
      installedComponents = opt.installedComponents || [];
      break;
    default:
      installedComponents = opt.installedComponents || [];
      break;
  }
  return missingDependencies(data, component, installedComponents, opt)?.map(
    (componentDependency: { chooseCompatible: (arg0: any) => any }) => {
      return componentDependency.chooseCompatible(data.services);
    }
  );
};

const missingDependencies = (
  data: any,
  component: IHostComponent,
  installedComponents: any,
  opt: any
) => {
  opt = opt || {};
  opt.scope = opt.scope || "*";
  var dependencies: any = get(component, "dependencies", []);
  dependencies =
    opt.scope === "*"
      ? dependencies
      : dependencies.filter((item: any) => {
          return item.Dependencies.scope === opt.scope;
        });
  if (dependencies.length === 0) return [];

  var missingComponents = dependencies.filter((dependency: any) => {
    return !installedComponents.some((installedComponent: IHostComponent) => {
      const dependencyComponent = data.allComponents.find(
        (host: IHostComponent) => {
          return host.componentName === dependency.Dependencies.component_name;
        }
      );
      return compatibleWith(
        installedComponent,
        dependencyComponent.componentName,
        dependencyComponent.componentType
      );
    });
  });
  return missingComponents.map((missingComponent: any) => {
    var componentFound = data.allComponents.find(
      (hostComponent: IHostComponent) => {
        return (
          hostComponent.componentName ===
          missingComponent.Dependencies.component_name
        );
      }
    );
    const compatibleComponents: CompatibleComponent[] = componentFound
      ? [
          {
            componentName: componentFound.componentName,
            serviceName: componentFound.serviceName,
          },
        ]
      : [];

    return new ComponentDependency(
      missingComponent.Dependencies.component_name,
      compatibleComponents
    );
  });
};

const compatibleWith = (component: any, compName: string, compType: string) => {
  return (
    component.componentName === compName ||
    (component.componentType && component.componentType === compType)
  );
};

export const installClients = async (
  components: IHostComponent[],
  data: any
) => {
  var clientsToInstall: IHostComponent[] = [],
    clientsToAdd: IHostComponent[] = [],
    missedComponents: any = [],
    dependentComponents: any = [];

  components.forEach((component) => {
    if (["INIT", "INSTALL_FAILED"].includes(get(component, "workStatus"))) {
      clientsToInstall.push(component);
    } else if (typeof get(component, "workStatus") == "undefined") {
      clientsToAdd.push(component);
    }
  });
  clientsToAdd.forEach((component, _index, array) => {
    var dependencies;
    try {
      dependencies = checkComponentDependencies(data, component, {
        scope: "host",
        installedComponents: get(data, "host.hostComponents", []),
      });
    } catch (error) {
      dependencies = array.map((component) => {
        get(component, "componentName").includes(getComponentName(component));
      });
    }
    if (dependencies && dependencies.length > 0) {
      missedComponents.push(dependencies);
      dependentComponents.push(getComponentDisplayName(component));
    }
  });

  missedComponents = uniq(missedComponents);
  if (missedComponents && missedComponents.length) {
    var popupMessage = t(
      "host.host.addComponent.popup.clients.dependedComponents.body"
    )
      .replace("{0}", dependentComponents.join(", "))
      .replace(
        "{1}",
        missedComponents
          .map((component: IHostComponent) => getComponentDisplayName(component))
          .join(", ")
      );
    showAlertModal(
      t("host.host.addComponent.popup.dependedComponents.header"),
      popupMessage
    );
  } else {
    await data.getKDCSessionState(async () => {
      var sendInstallCommand = function () {
        if (clientsToInstall && clientsToInstall.length) {
          sendComponentCommand(
            clientsToInstall[0],
            t("host.host.details.installClients"),
            "INSTALLED"
          );
        }
      };

      if (clientsToAdd && clientsToAdd.length) {
        // var message = clientsToAdd.map((component: IHostComponent) => {
        //     return getComponentDisplayName(component)
        //   }).join(", ");
        // var componentObject = Object.create({
        //   displayName: message
        // });

        // popup for add component modal.
        sendInstallCommand();
        clientsToAdd.forEach((component: IHostComponent) => {
          installHostComponentCall(get(component, "hostName"), component, data, data?.setAllHostModels);
        });
      } else {
        sendInstallCommand();
      }
    });
  }
};
export const refreshComponentConfigs = async (component: IHostComponent) => {
  const clusterName = get(component, "clusterName", "");
  const context = t("requestInfo.refreshComponentConfigs").replace(
    "{0}",
    getComponentDisplayName(component)
  );
  const resource_filters = [
    {
      service_name: get(component, "serviceName"),
      component_name: getComponentName(component),
      hosts: get(component, "hostName"),
    },
  ];
  const data = JSON.stringify({
    RequestInfo: {
      command: "CONFIGURE",
      context: context,
    },
    "Requests/resource_filters": resource_filters,
  });

  await HostsApi.clusterRequests(clusterName, data);
};
export const refreshConfigs = async (component: IHostComponent) => {
  const message = t("rollingrestart.context.ClientOnSelectedHost")
    .replace("{0}", getComponentDisplayName(component))
    .replace("{1}", get(component, "hostName"));

  restartHostComponents([component], message, "HOST");
};

export const decommission = (component: IHostComponent, data: any) => {
  runDecommission(component, data);
  const callback = get(data, "callback", null);
  if (callback && typeof callback === "function") {
    callback();
  }
};

const runDecommission = (component: IHostComponent, data?: any) => {
  const svcName = get(component, "serviceName");
  const hostName = get(component, "hostName");
  const clusterName = get(component, "clusterName", "");
  switch (svcName) {
    case "HDFS":
      doDecommission(clusterName, hostName, svcName, "NAMENODE", "DATANODE", data);
      break;
    case "YARN":
      doDecommission(
        clusterName,
        hostName,
        svcName,
        "RESOURCEMANAGER",
        "NODEMANAGER",
        data
      );
      break;
    case "HBASE":
      warnBeforeDecommission(component, data);
  }
};

const doDecommission = async (
  clusterName: string,
  hostName: string,
  serviceName: string,
  componentName: string,
  slaveType: string,
  data?: any
) => {
  const contextNameString =
    "hosts.host." + slaveType.toLowerCase() + ".decommission";
  const context = translate(contextNameString);
  const requestData = {
    context: context,
    command: "DECOMMISSION",
    hostName: hostName,
    serviceName: serviceName,
    componentName: componentName,
    slaveType: slaveType,
    clusterName: clusterName,
  };
  const response = await HostsApi.decommissionSlave(clusterName, requestData);
  const requestId = get(response, "Requests.id", -1);
  if((requestId != -1) && data && data.setAllHostModels){
    data.setAllHostModels((prevModels: IHost[]) => {
      return prevModels.map((host) => {
        if (host.hostName === hostName) {
          const hostModel = cloneDeep(host);
          const hostComponents = get(hostModel, "hostComponents", []);
          const updatedComponents = hostComponents.map((hc: IHostComponent) => {
            if (getComponentName(hc) === slaveType) {
              return { ...hc, adminState: "DECOMMISSIONED" };
            }
            return hc;
          });
          set(hostModel, "hostComponents", updatedComponents);
          
          return hostModel;
        }
        return host;
      });
    });
  }
  defaultSuccessCallback(requestId);
};

const warnBeforeDecommission = (component: IHostComponent, data?: any) => {
  if (get(component, "passiveState") !== "ON") {
    showHbaseActiveWarning();
  } else {
    checkRegionServerState(component, data);
  }
};

const checkRegionServerState = async (component: IHostComponent, data?: any) => {
  const clusterName = get(component, "clusterName");
  const response = await HostsApi.getResgionServerInService(clusterName);
  checkRegionServerStateSuccessCallback(response, component, data);
};

const checkRegionServerStateSuccessCallback = (
  response: any,
  component: IHostComponent,
  data?: any
) => {
  const hostArray = [get(component, "hostName")];
  const hostNames = hostArray.join(",");
  const clusterName = get(component, "clusterName", "");

  const decommissionPossible =
    response.items
      .map((item: any) => item.HostRoles.host_name)
      .filter((hostName: string) => !hostArray.includes(hostName)).length >= 1;

  if (decommissionPossible) {
    doDecommissionRegionServer(
      hostNames,
      "HBASE",
      "HBASE_MASTER",
      "HBASE_REGIONSERVER",
      clusterName,
      data
    );
  } else {
    showRegionServerWarning();
  }
};

export const recommission = (component: IHostComponent, data: any) => {
  runRecommission(component, data);
  const callback = get(data, "callback", null);
  if (callback && typeof callback === "function") {
    callback();
  }
};

const runRecommission = (component: IHostComponent, data?: any) => {
  const svcName = get(component, "serviceName");
  const hostName = get(component, "hostName");
  const clusterName = get(component, "clusterName", "");
  switch (svcName) {
    case "HDFS":
      doRecommissionAndStart(
        hostName,
        svcName,
        "NAMENODE",
        "DATANODE",
        clusterName,
        data
      );
      break;
    case "YARN":
      doRecommissionAndStart(
        hostName,
        svcName,
        "RESOURCEMANAGER",
        "NODEMANAGER",
        clusterName,
        data
      );
      break;
    case "HBASE":
      doRecommissionAndStart(
        hostName,
        svcName,
        "HBASE_MASTER",
        "HBASE_REGIONSERVER",
        clusterName,
        data
      );
  }
};

export const deleteComponent = async (component: IHostComponent, data: any) => {
  const componentName = get(component, "componentName");
  const componentsMapItem = get(addDeleteComponentsMap, componentName);

  if (componentsMapItem) {
    await data.deleteAndReconfigureComponent(componentsMapItem, component);
  } else if (componentName === "JOURNALNODE") {
    data.navigate("/main/services/highAvailability/JournalNode/manage/step1");
  } else {
    modalManager.show(
      <RecommendationModal
        isOpen={true}
        onClose={() => {
          modalManager.hide();
        }}
        componentDisplayName={getComponentDisplayName(component)}
        add={false}
        callback={async () => {
          try {
            await data._doDeleteHostComponent(component);
          } catch (error) {
            showErrorModal(
              get(
                error,
                "response.data.message",
                get(error, "message", "Unable to delete the host component.")
              )
            );
          }
        }}
      />
    );
  }
};

export const addComponentWithCheck = async (
  component: IHostComponent,
  data: any
) => {
  await data.getKDCSessionState(async () => {
    await addComponent(component, data);
  });
};

export const addComponent = async (component: IHostComponent, data: any) => {
  const hostName = get(component, "hostName");
  const componentName = getComponentName(component);

  const missedComponents = get(data, "fromServiceSummary", false)
    ? []
    : checkComponentDependencies(data, component, {
        scope: "host",
        installedComponents: get(data, "host.hostComponents", []),
      });
  const componentsMapItem = get(addDeleteComponentsMap, componentName, null);
  if (missedComponents && missedComponents.length) {
    var popupMessage = translateWithVariables(
      "host.host.addComponent.popup.clients.dependedComponents.body",
      {
        "0": get(component, "displayName"),
        "1": missedComponents.join(", "),
      }
    );
    return showAlertModal(
      translate("host.host.addComponent.popup.dependedComponents.header"),
      popupMessage
    );
  }

  if (componentsMapItem) {
    await data.addAndReconfigureComponent(
      componentsMapItem,
      hostName,
      component,
      data
    );
  } else if (componentName === "JOURNALNODE") {
    data.navigate("/main/services/highAvailability/JournalNode/manage/step1");
  } else {
    // The component metadata is authoritative when this action is reused across services.
    const serviceName = get(component, "serviceName") || data.serviceName || "";
    const componentNameForModal = data.componentNameFromService || get(component, "componentName") || "";
    
    modalManager.show(
      <RecommendationModal
        isOpen={true}
        onClose={() => {
          modalManager.hide();
        }}
        componentDisplayName={getComponentDisplayName(component)}
        add={true}
        callback={() => installHostComponentCall(hostName, component, data, data?.setAllHostModels)}
        fromService={data.fromServiceSummary ? data.fromServiceSummary : false}
        serviceName={serviceName}
        componentName={componentNameForModal}
        validDropDownHosts={
          data.validDropDownHosts ? data.validDropDownHosts : []
        }
        handleHostChange={async (selectedHost) => {
          modalManager.hide();
          component.hostName = selectedHost; // Update the hostName in the component
          await addComponent(component, data);
        }}
      />
    );
  }
};

export const installComponent = async (
  component: IHostComponent,
  data: any
) => {
  const clusterName = get(component, "clusterName");
  const hostName = get(component, "hostName");
  const displayName = get(component, "displayName");
  const serviceName = get(component, "serviceName");
  const componentName = get(component, "componentName");

  await data.getKDCSessionState(async () => {
    const urlParams = "";

    const data = JSON.stringify({
      RequestInfo: {
        context:
          translate("requestInfo.installHostComponent") + " " + displayName,
        operation_level: {
          level: "HOST_COMPONENT",
          cluster_name: clusterName,
          host_name: hostName,
          service_name: serviceName || null,
        },
      },
      Body: {
        HostRoles: {
          state: "INSTALLED",
        },
      },
    });
    const response = await HostsApi.commonHostHostComponentUpdate(
      clusterName,
      hostName,
      componentName,
      urlParams,
      data
    );
    const requestId = get(response, "Requests.id", -1);
    defaultSuccessCallbackWithoutReload(requestId);
  });
};

export const executeCustomCommand = (cmd: any, component: IHostComponent) => {
  modalManager.show(
    <ConfirmationModal
      isOpen={true}
      onClose={() => modalManager.hide()}
      modalTitle={translate("popup.confirmation.commonHeader")}
      modalBody={translate("question.sure")}
      successCallback={() => {
        executeCustomCommandCall(cmd, component);
        modalManager.hide();
      }}
    />
  );
};

const executeCustomCommandCall = async (
  cmd: any,
  component: IHostComponent
) => {
  let context = get(cmd, "context", "");
  if (!context) {
    context = translateWithVariables(
      "services.service.actions.run.executeCustomCommand.context",
      {
        "0": get(cmd, "command", ""),
      }
    );
  }
  const data = {
    command: get(cmd, "command", ""),
    context: context,
    hosts: get(component, "hostName", ""),
    serviceName: get(component, "serviceName", ""),
    componentName: get(component, "componentName", ""),
  };
  const clusterName = get(component, "clusterName", "");
  try {
    const response = await HostsApi.executeCustomCommand(clusterName, data);
    executeCustomCommandSuccessCallback(response);
  } catch (error) {
    executeCustomCommandErrorCallback(error);
  }
};

const executeCustomCommandSuccessCallback = (response: any) => {
  const requestId = get(response, "Requests.id", -1);
  defaultSuccessCallback(requestId);
};

const executeCustomCommandErrorCallback = (error: any) => {
  showAlertModal(
    translate("services.service.actions.run.executeCustomCommand.error"),
    translate("services.service.actions.run.executeCustomCommand.error") +
      error.message
  );
};

export const transitionToObserver = async (component: IHostComponent) => {
  const clusterName = get(component, "clusterName", "");
  const hostName = get(component, "hostName", "");
  const componentName = getComponentName(component);

  // Validation - only allow for NAMENODE components
  if (componentName !== "NAMENODE") {
    showAlertModal(
      translate("common.error"),
      "Transition to Observer is only available for NameNode components."
    );
    return;
  }

  // Validation - component must be started
  const workStatus = get(component, "workStatus");
  if (workStatus !== "STARTED") {
    showAlertModal(
      translate("common.error"),
      "NameNode must be in STARTED state to transition to Observer."
    );
    return;
  }

  modalManager.show(
    <ConfirmationModal
      isOpen={true}
      onClose={() => modalManager.hide()}
      modalTitle={translate("services.service.actions.run.makeObserver.context")}
      modalBody={translateWithVariables(
        "question.sure.makeObserver",
        {
          "0": get(component, "displayName", "NameNode"),
        }
      )}
      successCallback={async () => {
        try {
          const context = translate("services.service.actions.run.makeObserver.context");
          const response = await HostsApi.transitionToObserver(clusterName, {
            hostName: hostName,
            context: context,
          });
          
          const requestId = get(response, "Requests.id", -1);
          if (requestId !== -1) {
            modalManager.show(
              <BackgroundOperations
                isOpen={true}
                onClose={() => {
                  modalManager.hide();
                }}
                requestId={requestId}
              />
            );
          }
        } catch (error: any) {
          showAlertModal(
            translate("common.error"),
            translate("services.service.actions.run.makeObserver.error") + 
            (error.message || "")
          );
        }
        modalManager.hide();
      }}
    />
  );
};
