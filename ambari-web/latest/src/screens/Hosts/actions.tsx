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

import { cloneDeep, get, set } from "lodash";
import { HostsApi } from "../../api/hostsApi";
import {
  doDecommissionRegionServer,
  doRecommissionAndStart,
  getComponentDisplayName,
  getComponentName,
  parseNnCheckPointTime,
  showHbaseActiveWarning,
  showRegionServerWarning,
} from "./utils";
import { ComponentStatus } from "./enums";
import { defaultSuccessCallback, restartHostComponents } from "./batchUtils";
import modalManager from "../../store/ModalManager";
import { nnCheckpointAgeAlertThreshold } from "../../data/configs/services/config";
import { IHostComponent } from "../../models/hostComponent";
import {
  showAlertModal,
  translate,
  translateWithVariables,
} from "../../Utils/Utility";
import ConfirmationModal from "../../components/ConfirmationModal";
import { IHost } from "../../models/host";

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
    // modalManager.show(
    //   <BackgroundOperations
    //     isOpen={true}
    //     onClose={() => {
    //       modalManager.hide();
    //     }}
    //     requestId={requestId}
    //   />
    // );
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
  callback: Function | Promise<any>,
  hostName: string,
  clusterName: string
) => {
  const isNNCheckpointTooOld = await pullNnCheckPointTime(
    hostName,
    clusterName
  );
  if (isNNCheckpointTooOld) {
    //TODO: get the hdfs user and remove the hardcoded hdfs user from below commands
    const msg =
      `The last HDFS checkpoint is older than ${nnCheckpointAgeAlertThreshold} hours. ` +
      "Make sure that you have taken a checkpoint before proceeding. Otherwise, the NameNode(s) can take a very long time to start up." +
      `\n\n1. Login to the NameNode host ${isNNCheckpointTooOld}` +
      "\n\n2. Put the NameNode in Safe Mode (read-only mode): \n\n" +
      "    sudo su hdfs -l -c 'hdfs dfsadmin -safemode enter'" +
      "\n\n3. Once in Safe Mode, create a Checkpoint:\n\n" +
      "    sudo su hdfs -l -c 'hdfs dfsadmin -saveNamespace" +
      "\n";
    const modalProps = {
      modalTitle: "Warning",
      modalBody: msg,
      onClose: () => { },
      successCallback: async () => {
        if (typeof callback === "function") {
          callback();
        } else if (callback instanceof Promise) {
          await callback;
        }
        modalManager.hide();
      },
      options: {
        okButtonText: "Next",
        okButtonVariant: "primary",
        buttonSize: "sm" as "sm" | "lg" | undefined,
        cancelableViaIcon: true,
        cancelableViaBtn: true,
      },
    };
    modalManager.show(modalProps);
  } else if (isNNCheckpointTooOld === null) {
    const modalProps = {
      modalTitle: "Warning",
      modalBody:
        "Could not determine the age of the last HDFS checkpoint. Please ensure that you have a recent checkpoint. Otherwise, the NameNode(s) can take a very long time to start up.",
      onClose: () => { },
      successCallback: async () => {
        if (typeof callback === "function") {
          callback();
        } else if (callback instanceof Promise) {
          await callback;
        }
        modalManager.hide();
      },
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
    if (typeof callback === "function") {
      callback();
    } else if (callback instanceof Promise) {
      await callback;
    }
  }
};

const pullNnCheckPointTime = async (hostName: string, clusterName: string) => {
  const response = await HostsApi.getNnCheckPointTime(clusterName, hostName);
  return parseNnCheckPointTime(response);
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
  if ((requestId != -1) && data && data.setAllHostModels) {
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
