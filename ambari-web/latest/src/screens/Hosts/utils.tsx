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

import { cloneDeep, filter, get, isEmpty, set } from "lodash";
import { ComponentStatus, ComponentType } from "./enums";
import { normalizeNameBySeparators } from "./helpers";
import { nnCheckpointAgeAlertThreshold } from "../../data/configs/services/config";
import ClusterApi from "../../api/clusterApi";
import modalManager from "../../store/ModalManager";
import SetRackInfoModal from "./SetRackInfoModal";
import HostComponent, { IHostComponent } from "../../models/hostComponent";
import { hostMapper } from "../../mappers/hostsMapper";
import HostStackVersion, {
  IHostStackVersion,
} from "../../models/hostStackVersion";
import { Alert } from "react-bootstrap";
import { ActionsApi } from "../../api/actionsApi";
import {
  showErrorModal,
  translate,
  translateWithVariables,
} from "../../Utils/Utility";
import BackgroundOperations from "../BackgroundOperations";
import { HostsApi } from "../../api/hostsApi";
import { defaultSuccessCallbackWithoutReload } from "./batchUtils";
import { IHost } from "../../models/host";

export const hostComponentCustomCommandMap = {
  REFRESHQUEUES: {
    action: "refreshYarnQueues",
    customCommand: "REFRESHQUEUES",
    context: translate(
      "services.service.actions.run.yarnRefreshQueues.context"
    ),
    label: translate("services.service.actions.run.yarnRefreshQueues.menu"),
  },
  STARTDEMOLDAP: {
    action: "startLdapKnox",
    customCommand: "STARTDEMOLDAP",
    context: translate("services.service.actions.run.startLdapKnox.context"),
    label: translate("services.service.actions.run.startLdapKnox.context"),
  },
  STOPDEMOLDAP: {
    action: "stopLdapKnox",
    customCommand: "STOPDEMOLDAP",
    context: translate("services.service.actions.run.stopLdapKnox.context"),
    label: translate("services.service.actions.run.stopLdapKnox.context"),
  },
  RESTART_LLAP: {
    action: "restartLLAP",
    customCommand: "RESTART_LLAP",
    context: translate("services.service.actions.run.restartLLAP"),
    label: translate("services.service.actions.run.restartLLAP") + " ∞",
  },
  REBALANCEHDFS: {
    action: "rebalanceHdfsNodes",
    customCommand: "REBALANCEHDFS",
    context: translate(
      "services.service.actions.run.rebalanceHdfsNodes.context"
    ),
    label: translate("services.service.actions.run.rebalanceHdfsNodes"),
  },
  MAKEOBSERVER: {
    action: "makeObserver",
    customCommand: "MAKEOBSERVER",
    context: translate("services.service.actions.run.makeObserver.context"),
    label: translate("services.service.actions.run.makeObserver"),
  },
  IMMEDIATE_STOP_HAWQ_SERVICE: {
    action: "executeHawqCustomCommand",
    customCommand: "IMMEDIATE_STOP_HAWQ_SERVICE",
    context: translate(
      "services.service.actions.run.immediateStopHawqService.context"
    ),
    label: translate(
      "services.service.actions.run.immediateStopHawqService.label"
    ),
  },
  IMMEDIATE_STOP_HAWQ_SEGMENT: {
    customCommand: "IMMEDIATE_STOP_HAWQ_SEGMENT",
    context: translate(
      "services.service.actions.run.immediateStopHawqSegment.context"
    ),
    label: translate(
      "services.service.actions.run.immediateStopHawqSegment.label"
    ),
  },
  RESYNC_HAWQ_STANDBY: {
    action: "executeHawqCustomCommand",
    customCommand: "RESYNC_HAWQ_STANDBY",
    context: translate(
      "services.service.actions.run.resyncHawqStandby.context"
    ),
    label: translate("services.service.actions.run.resyncHawqStandby.label"),
  },
  HAWQ_CLEAR_CACHE: {
    action: "executeHawqCustomCommand",
    customCommand: "HAWQ_CLEAR_CACHE",
    context: translate("services.service.actions.run.clearHawqCache.label"),
    label: translate("services.service.actions.run.clearHawqCache.label"),
  },
  RUN_HAWQ_CHECK: {
    action: "executeHawqCustomCommand",
    customCommand: "RUN_HAWQ_CHECK",
    context: translate("services.service.actions.run.runHawqCheck.label"),
    label: translate("services.service.actions.run.runHawqCheck.label"),
  },
  UPDATE_REPLICATION: {
    action: "updateHBaseReplication",
    customCommand: "UPDATE_REPLICATION",
    context: translate(
      "services.service.actions.run.updateHBaseReplication.context"
    ),
    label: translate(
      "services.service.actions.run.updateHBaseReplication.label"
    ),
  },
  STOP_REPLICATION: {
    action: "stopHBaseReplication",
    customCommand: "STOP_REPLICATION",
    context: translate(
      "services.service.actions.run.stopHBaseReplication.context"
    ),
    label: translate("services.service.actions.run.stopHBaseReplication.label"),
  },
};

export const addDeleteComponentsMap: any = {
  ZOOKEEPER_SERVER: {
    addPropertyName: "addZooKeeperServer",
    deletePropertyName: "fromDeleteZkServer",
    configTagsCallbackName: "loadZookeeperConfigs",
    configsCallbackName: "saveZkConfigs",
  },
  HIVE_METASTORE: {
    deletePropertyName: "deleteHiveMetaStore",
    hostPropertyName: "hiveMetastoreHost",
    configTagsCallbackName: "loadHiveConfigs",
    configsCallbackName: "onLoadHiveConfigs",
  },
  WEBHCAT_SERVER: {
    deletePropertyName: "deleteWebHCatServer",
    hostPropertyName: "webhcatServerHost",
    configTagsCallbackName: "loadWebHCatConfigs",
    configsCallbackName: "onLoadHiveConfigs",
  },
  HIVE_SERVER: {
    addPropertyName: "addHiveServer",
    deletePropertyName: "deleteHiveServer",
    configTagsCallbackName: "loadHiveConfigs",
    configsCallbackName: "onLoadHiveConfigs",
  },
  NIMBUS: {
    deletePropertyName: "deleteNimbusHost",
    hostPropertyName: "nimbusHost",
    configTagsCallbackName: "loadStormConfigs",
    configsCallbackName: "onLoadStormConfigs",
  },
  ATLAS_SERVER: {
    deletePropertyName: "deleteAtlasServer",
    hostPropertyName: "atlasServer",
    configTagsCallbackName: "loadAtlasConfigs",
    configsCallbackName: "onLoadAtlasConfigs",
  },
  RANGER_KMS_SERVER: {
    deletePropertyName: "deleteRangerKMSServer",
    hostPropertyName: "rangerKMSServerHost",
    configTagsCallbackName: "loadRangerConfigs",
    configsCallbackName: "onLoadRangerConfigs",
  },
};

export const populateHostComponentModels = (hostComponent: any) => {
  const hostComponentModel = new HostComponent({} as IHostComponent);
  (
    Object.keys(hostMapper.hostComponentConfig) as Array<
      keyof typeof hostMapper.hostComponentConfig
    >
  ).map((key) => {
    set(
      hostComponentModel,
      key,
      get(hostComponent, hostMapper.hostComponentConfig[key])
    );
  });
  return hostComponentModel;
};

export const apiDataToHostComponentModel = (components: any[]) => {
  let hostComponents: IHostComponent[] = [];
  components.forEach((component: any) => {
    hostComponents.push(populateHostComponentModels(component));
  });
  return hostComponents;
};

export const isClientUsingComponentName = (
  componentName: string,
  serviceComponentInfo: any
) => {
  get(serviceComponentInfo, "items", []).forEach((item: any) => {
    get(item, "components", []).forEach((component: any) => {
      if (
        get(component, "ServiceComponentInfo.component_name", "") ===
        componentName
      ) {
        return (
          get(component, "ServiceComponentInfo.component_category", "") ===
          ComponentType.CLIENT
        );
      }
    });
  });
  return false;
};

export const isSlave = (component: IHostComponent) => {
  return get(component, "componentCategory", "") === ComponentType.SLAVE;
};

export const isClient = (component: IHostComponent) => {
  return get(component, "componentCategory", "") === ComponentType.CLIENT;
};

export const isMaster = (component: IHostComponent) => {
  return get(component, "componentCategory", "") === ComponentType.MASTER;
};

export const getComponentDisplayName = (component: IHostComponent) => {
  return get(component, "displayName", "");
};

export const getComponentName = (component: IHostComponent) => {
  return get(component, "componentName", "");
};

export const isHAComponentOnly = (component: IHostComponent) => {
  return ["ZKFC", "JOURNALNODE"].includes(getComponentName(component));
};

export const isNotAddableOnlyInInstall = (component: IHostComponent) => {
  return [
    "HIVE_METASTORE",
    "HIVE_SERVER",
    "RANGER_KMS_SERVER",
    "OOZIE_SERVER",
    "TIMELINE_READER",
    "YARN_REGISTRY_DNS",
  ].includes(getComponentName(component));
};

export const isHaEnabled = (serviceModels: any) => {
  return get(serviceModels, "hdfs.isNameNodeHaEnabled", false);
};

export const isMasterAddableInstallerWizard = (component: IHostComponent) => {
  return (
    isMaster(component) &&
    isMultipleAllowed(component) &&
    !isMasterAddableOnlyOnHA(component) &&
    !isNotAddableOnlyInInstall(component)
  );
};

export const isMasterAddableOnlyOnHA = (component: IHostComponent) => {
  return ["NAMENODE", "RESOURCEMANAGER", "RANGER_ADMIN"].includes(
    getComponentName(component)
  );
};

export const isAddableToHost = (
  component: IHostComponent,
  serviceModels: any
) => {
  return (
    isMasterAddableInstallerWizard(component) ||
    ((isNotAddableOnlyInInstall(component) ||
      isSlave(component) ||
      isClient(component)) &&
      (!isHAComponentOnly(component) ||
        (isHaEnabled(serviceModels) &&
          getComponentName(component) === "JOURNALNODE")))
  );
};

export const isMultipleAllowed = (component: IHostComponent) => {
  return maxToInstall(component) > 1;
};

export const maxToInstall = (component: IHostComponent) => {
  return getCardinalityValue(get(component, "cardinality", ""), true);
};

export const minToInstall = (component: IHostComponent) => {
  return getCardinalityValue(get(component, "cardinality", ""), false);
};

export const getCardinalityValue = (
  cardinality: string,
  isMax: boolean
): number => {
  if (cardinality) {
    const isOptional = cardinality.toString().split("-").length > 1;
    if (isOptional) {
      return parseInt(cardinality.split("-")[isMax ? 1 : 0]);
    } else {
      if (isMax)
        return /^\d+\+/.test(cardinality) || cardinality === "ALL"
          ? Infinity
          : parseInt(cardinality);
      return cardinality === "ALL"
        ? Infinity
        : parseInt(cardinality.toString().replace("+", ""));
    }
  } else {
    return 0;
  }
};

export const sortBasedOnMasterSlave = (data: any, key: string) => {
  const masterComponents = data.filter(
    (component: any) => get(component, key, "") === ComponentType.MASTER
  );
  const slaveComponents = data.filter(
    (component: any) => get(component, key, "") === ComponentType.SLAVE
  );
  const clientComponents = data.filter(
    (component: any) => get(component, key, "") === ComponentType.CLIENT
  );
  return masterComponents.concat(slaveComponents).concat(clientComponents);
};

export const isRestartComponentDisabled = (component: IHostComponent) => {
  return get(component, "workStatus", "") !== ComponentStatus.STARTED;
};

export const isRestartable = (component: IHostComponent) => {
  return !isClient(component);
};

export const isStart = (component: IHostComponent) => {
  return [ComponentStatus.STARTED, ComponentStatus.STARTING].includes(
    get(component, "workStatus") as ComponentStatus
  );
};

export const isInit = (component: IHostComponent) => {
  return get(component, "workStatus", "") === ComponentStatus.INIT;
};

export const isReassignable = (
  component: IHostComponent,
  hostsCount: number
) => {
  return get(component, "reassignAllowed", false) && hostsCount > 1;
};

export const getInstalledComponentHostList = (
  componentName: string,
  clusterComponents: any
) => {
  return get(
    clusterComponents.filter(
      (c: any) =>
        get(c, "ServiceComponentInfo.component_name") === componentName
    ),
    "[0].host_components",
    []
  );
};

export const getInstalledComponentCount = (
  componentName: string,
  clusterComponents: any
) => {
  return getInstalledComponentHostList(componentName, clusterComponents).length;
};

export const isMoveComponentDisabled = (
  component: IHostComponent,
  hostsCount: number,
  clusterComponents: any
) => {
  return (
    hostsCount ===
    getInstalledComponentCount(getComponentName(component), clusterComponents)
  );
};

export const isActive = (component: IHostComponent) => {
  return get(component, "passiveState", "OFF") === "OFF";
};

export const isDeletable = (component: IHostComponent, serviceModels: any) => {
  const ignored: string[] = [];
  const componentName = getComponentName(component);
  return (
    (isAddableToHost(component, serviceModels) &&
      !ignored.includes(componentName)) ||
    componentName === "MYSQL_SERVER"
  );
};

export const isDeleteComponentDisabled = (
  component: IHostComponent,
  clusterComponents: any,
  hiveDatabaseType = "",
) => {
  const stackComponentCount = minToInstall(component);
  const componentName = getComponentName(component);
  const installedCount = getInstalledComponentCount(
    componentName,
    clusterComponents
  );
  const status = [
    ComponentStatus.STOPPED,
    ComponentStatus.UNKNOWN,
    ComponentStatus.INSTALL_FAILED,
    ComponentStatus.UPGRADE_FAILED,
    ComponentStatus.INIT,
  ].includes(get(component, "workStatus", "") as ComponentStatus);

  if (
    componentName === "MYSQL_SERVER" &&
    get(component, "serviceName", "") === "HIVE"
  ) {
    return !(hiveDatabaseType.includes("Existing") && status);
  }

  if (componentName === "JOURNALNODE") {
    const countJN = getInstalledComponentCount(
      "JOURNALNODE",
      clusterComponents
    );
    return countJN <= 3; //TODO: get 3 from stack (Hardcoded in existing ambari also)
  }
  return installedCount <= stackComponentCount || !status;
};

export const isRefreshConfigsAllowed = (component: IHostComponent) => {
  return ["FLUME_HANDLER"].includes(getComponentName(component));
};

export const runningComponentCounter = (
  componentName: string,
  clusterComponents: any
) => {
  const hostsComponent = getInstalledComponentHostList(
    componentName,
    clusterComponents
  );
  return hostsComponent.filter((host: any) => {
    return isStart(host);
  }).length;
};

export const meetsCustomCommandReq = (
  component: IHostComponent,
  command: string,
  clusterComponents: any
) => {
  const excludedMasterCommands = ["DECOMMISSION", "RECOMMISSION"];
  if (excludedMasterCommands.includes(command)) {
    return false;
  }
  if (get(component, "cardinality", "") !== "1") {
    if (!isStart(component)) {
      if (
        getInstalledComponentCount(
          getComponentName(component),
          clusterComponents
        ) > 1
      ) {
        if (
          runningComponentCounter(
            getComponentName(component),
            clusterComponents
          )
        ) {
          return false;
        }
      } else {
        return false;
      }
    }
  }
  return true;
};

export const getClientCustomCommands = (component: IHostComponent) => {
  if (getComponentName(component) === "KERBEROS_CLIENT") {
    return [];
  }
  const commands = get(component, "customCommands", []);
  let customCommands: any[] = [];
  commands.forEach((command: string) => {
    customCommands.push({
      label: normalizeNameBySeparators(command, ["_", "-", " "]),
      command: command,
    });
  });
  return customCommands;
};

export const getCustomCommands = (
  component: IHostComponent,
  clusterComponents: any
) => {
  const commands: string[] = get(component, "customCommands", []);
  let customCommands: any[] = [];

  commands.forEach((command: string) => {
    if (
      !isSlave(component) &&
      !meetsCustomCommandReq(component, command, clusterComponents)
    ) {
      return;
    }
    customCommands.push({
      label: get(hostComponentCustomCommandMap, command + ".label", "")
        ? get(hostComponentCustomCommandMap, command + ".label", "")
        : normalizeNameBySeparators(command, ["_", "-", " "]),
      service: get(component, "serviceName", ""),
      component: get(component, "componentName", ""),
      command: command,
      context: get(hostComponentCustomCommandMap, command + ".context", ""),
      disabled: get(hostComponentCustomCommandMap, command + ".disabled", false),
    });
  });
  return customCommands;
};

export const getClockDistance = async () => {
  const data = await ClusterApi.loadAmbariProperties();
  const clientClock = new Date().getTime();
  let serverClock = get(
    data,
    "RootServiceComponents.server_clock",
    ""
  ).toString();
  serverClock = serverClock.length < 13 ? serverClock + "000" : serverClock;
  return parseInt(serverClock) - clientClock;
};

export const dateTime = async () => {
  const clockDistance = await getClockDistance();
  return new Date().getTime() + clockDistance;
};

export const parseNnCheckPointTime = async (data: any) => {
  const lastCheckpointTime = get(
    data,
    "metrics.dfs.FSNamesystem.LastCheckpointTime",
    0
  );
  const hostName = get(data, "HostRoles.host_name", "");
  const haState = get(data, "metrics.dfs.FSNamesystem.HAState", "");

  if (haState === "active") {
    if (!lastCheckpointTime) {
      return null;
    } else {
      const timeCriteria = nnCheckpointAgeAlertThreshold;
      const currDateTime = await dateTime();
      const timeAgo =
        (Math.round(currDateTime / 1000) - timeCriteria * 3600) * 1000;
      if (lastCheckpointTime <= timeAgo) {
        return hostName;
      } else {
        return false;
      }
    }
  } else if (haState === "standby") {
    return false;
  }
};

export const setRackInfo = async (
  operationData: any,
  hosts: any[],
  clusterName: string,
  callback: Function,
  rackId: string = ""
) => {
  const hostNames = hosts.map((host: any) => get(host, "hostName"));
  if (hostNames.length) {
    const data = {
      RequestInfo: {
        context: operationData.message,
        query: `Hosts/host_name.in(${hostNames.join(",")})`,
      },
      Body: {
        Hosts: {
          rack_info: rackId,
        },
      },
    };

    modalManager.show(
      <SetRackInfoModal
        clusterName={clusterName}
        data={data}
        callback={callback}
        hostNames={hostNames}
      />
    );
  }
};

export const getAllComponents = (serviceComponentInfo: any) => {
  if (!isEmpty(serviceComponentInfo)) {
    let allComponentsCopy: any[] = [];
    get(serviceComponentInfo, "items", []).forEach((service: any) => {
      allComponentsCopy = allComponentsCopy.concat(
        get(service, "components", []).map((component: any) => {
          return {
            HostRoles: {
              ...get(component, "StackServiceComponents"),
              dependencies: get(component, "dependencies", []).map(
                (d: any) => d.Dependencies.component_name
              ),
            },
          };
        })
      );
    });
    return allComponentsCopy;
  }
  return [];
};

export const getClusterComponentsCount = (clusterComponents: any) => {
  const clusterComponentsCount = {};
  get(clusterComponents, "items", []).forEach((component: any) => {
    const componentName = get(
      component,
      "ServiceComponentInfo.component_name",
      ""
    );
    if (!isEmpty(get(component, "host_components", []))) {
      set(
        clusterComponentsCount,
        componentName,
        get(component, "host_components", []).length
      );
    }
  });
  return clusterComponentsCount;
};

export const getPopulatedHostComponentObject = (hostComponent: any) => {
  const hostComponentObject = new HostComponent({} as IHostComponent);
  (
    Object.keys(hostMapper.hostComponentConfig) as Array<
      keyof typeof hostMapper.hostComponentConfig
    >
  ).map((key) => {
    set(
      hostComponentObject,
      key,
      get(hostComponent, hostMapper.hostComponentConfig[key])
    );
  });
  return hostComponentObject;
};

export const getPopulatedHostStackVersionObject = (hostStackVersion: any) => {
  const hostStackVersionObject = new HostStackVersion({} as IHostStackVersion);
  (
    Object.keys(hostMapper.hostStackVersionConfig) as Array<
      keyof typeof hostMapper.hostStackVersionConfig
    >
  ).map((key) => {
    set(
      hostStackVersionObject,
      key,
      get(hostStackVersion, hostMapper.hostStackVersionConfig[key])
    );
  });
  return hostStackVersionObject;
};

export const showHbaseActiveWarning = () => {
  const modalProps = {
    modalTitle: translate("common.warning"),
    modalBody: (
      <Alert variant="warning">
        {translateWithVariables("hostPopup.recommendation.beforeDecommission", {
          "0": "RegionServer",
        })}
      </Alert>
    ),
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
  };
  modalManager.show(modalProps);
};

export const showRegionServerWarning = () => {
  modalManager.show({
    modalTitle: translate("common.warning"),
    modalBody: (
      <Alert variant="warning">
        {translate("hosts.host.hbase_regionserver.decommission.warning")}
      </Alert>
    ),
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
};

export const doDecommissionRegionServer = async (
  hostNames: string,
  serviceName: string,
  componentName: string,
  slaveType: string,
  clusterName: string,
  data?: any
) => {
  let batches: any[] = [
    {
      order_id: 1,
      type: "POST",
      uri: `/clusters/${clusterName}/requests`,
      RequestBodyInfo: {
        RequestInfo: {
          context: translate("hosts.host.regionserver.decommission.batch1"),
          command: "DECOMMISSION",
          exclusive: "true",
          parameters: {
            slave_type: slaveType,
            excluded_hosts: hostNames,
          },
          operation_level: {
            level: "HOST_COMPONENT",
            cluster_name: clusterName,
            host_name: hostNames,
            service_name: serviceName,
          },
        },
        "Requests/resource_filters": [
          { service_name: serviceName, component_name: componentName },
        ],
      },
    },
  ];

  let id = 2;
  const hAray = hostNames.split(",");
  for (var i = 0; i < hAray.length; i++) {
    batches.push({
      order_id: id,
      type: "PUT",
      uri: `/clusters/${clusterName}/hosts/${hAray[i]}/host_components/${slaveType}`,
      RequestBodyInfo: {
        RequestInfo: {
          context: translate("hosts.host.regionserver.decommission.batch2"),
          exclusive: "true",
          operation_level: {
            level: "HOST_COMPONENT",
            cluster_name: clusterName,
            host_name: hostNames,
            service_name: serviceName,
          },
        },
        Body: {
          HostRoles: {
            state: "INSTALLED",
          },
        },
      },
    });
    id++;
  }
  batches.push({
    order_id: id,
    type: "POST",
    uri: `/clusters/${clusterName}/requests`,
    RequestBodyInfo: {
      RequestInfo: {
        context: translate("hosts.host.regionserver.decommission.batch3"),
        command: "DECOMMISSION",
        service_name: serviceName,
        component_name: componentName,
        parameters: {
          slave_type: slaveType,
          excluded_hosts: hostNames,
          mark_draining_only: true,
        },
        operation_level: {
          level: "HOST_COMPONENT",
          cluster_name: clusterName,
          host_name: hostNames,
          service_name: serviceName,
        },
      },
      "Requests/resource_filters": [
        { service_name: serviceName, component_name: componentName },
      ],
    },
  });

  const requestData = JSON.stringify([
    {
      RequestSchedule: {
        batch: [
          {
            requests: batches,
          },
          {
            batch_settings: {
              batch_separation_in_seconds: 1,
              task_failure_tolerance: 0,
            },
          },
        ],
      },
    },
  ]);

  try {
    const response = await ActionsApi.actionRequest(clusterName, requestData);
      if(data && data.setAllHostModels){
      data.setAllHostModels((prevModels: IHost[]) => {
        return prevModels.map((host) => {
          if (hostNames.includes(host.hostName)) {
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
    decommissionSuccessCallback(response);
  } catch (error) {
    showErrorModal(get(error, "message", ""));
  }
};

const decommissionSuccessCallback = (data: any) => {
  const requestId = get(data, "Requests.id", "");
  const scheduleId = get(data, "data.resources.[0].RequestSchedule.id", -1);
  if (requestId || scheduleId !== -1) {
    modalManager.show(
      <BackgroundOperations
        isOpen={true}
        onClose={() => {
          modalManager.hide();
        }}
        requestId={requestId || undefined}
      />
    );
  }
};

export const doRecommissionAndStart = async (
  hostNames: string,
  serviceName: string,
  componentName: string,
  slaveType: string,
  clusterName: string,
  data?: any
) => {
  const contextNameString1 =
    "hosts.host." + slaveType.toLowerCase() + ".recommission";
  const context1 = translate(contextNameString1);
  const contextNameString2 =
    "requestInfo.startHostComponent." + slaveType.toLowerCase();
  const startContext = translate(contextNameString2);
  let params = {
    slave_type: slaveType,
    included_hosts: hostNames,
  };
  if (serviceName === "HBASE") {
    set(params, "mark_draining_only", true);
  }

  let batches: any[] = [
    {
      order_id: 1,
      type: "POST",
      uri: `/clusters/${clusterName}/requests`,
      RequestBodyInfo: {
        RequestInfo: {
          context: context1,
          command: "DECOMMISSION",
          exclusive: "true",
          parameters: params,
          operation_level: {
            level: "HOST_COMPONENT",
            cluster_name: clusterName,
            host_name: hostNames,
            service_name: serviceName,
          },
        },
        "Requests/resource_filters": [
          { service_name: serviceName, component_name: componentName },
        ],
      },
    },
  ];
  let id = 2;
  const hAray = hostNames.split(",");
  for (var i = 0; i < hAray.length; i++) {
    batches.push({
      order_id: id,
      type: "PUT",
      uri: `/clusters/${clusterName}/hosts/${hAray[i]}/host_components/${slaveType}`,
      RequestBodyInfo: {
        RequestInfo: {
          context: startContext,
          operation_level: {
            level: "HOST_COMPONENT",
            cluster_name: clusterName,
            host_name: hostNames,
            service_name: serviceName || null,
          },
        },
        Body: {
          HostRoles: {
            state: "STARTED",
          },
        },
      },
    });
    id++;
  }

  const requestData = JSON.stringify([
    {
      RequestSchedule: {
        batch: [
          {
            requests: batches,
          },
          {
            batch_settings: {
              batch_separation_in_seconds: 1,
              task_failure_tolerance: 0,
            },
          },
        ],
      },
    },
  ]);

  try {
    const response = await ActionsApi.actionRequest(clusterName, requestData);
    if (data && data.setAllHostModels) {
      data.setAllHostModels((prevModels: IHost[]) => {
        return prevModels.map((host) => {
          if (hostNames.includes(host.hostName)) {
            const hostModel = cloneDeep(host);
            const hostComponents = get(hostModel, "hostComponents", []);
            const updatedComponents = hostComponents.map(
              (hc: IHostComponent) => {
                if (getComponentName(hc) === slaveType) {
                  return { ...hc, adminState: "INSERVICE" };
                }
                return hc;
              }
            );
            set(hostModel, "hostComponents", updatedComponents);

            return hostModel;
          }
          return host;
        });
      });
    }
    decommissionSuccessCallback(response);
  } catch (error) {
    showErrorModal(get(error, "message", ""));
  }
};

export const getTotalComponent = (
  component: HostComponent,
  clusterComponents: any
) => {
  const componentName = component.componentName;
  return clusterComponents.filter((component: any) => {
    return (
      get(component, "ServiceComponentInfo.component_name", "") ===
      componentName
    );
  })?.[0]?.host_components?.length;
};

export const getHostComponentsInfo = (
  hostComponents: HostComponent[],
  clusterComponents: any[],
  serviceModels: any
) => {
  const stoppedStates = [
    ComponentStatus.STOPPED,
    ComponentStatus.INSTALL_FAILED,
    ComponentStatus.UPGRADE_FAILED,
    ComponentStatus.INIT,
    ComponentStatus.UNKNOWN,
  ];
  const container: any = {
    isReconfigureRequired: false,
    lastComponents: [],
    masterComponents: [],
    nonAddableMasterComponents: [],
    lastMasterComponents: [],
    runningComponents: [],
    nonDeletableComponents: [],
    unknownComponents: [],
    toDecommissionComponents: [],
  };

  if (hostComponents && hostComponents.length > 0) {
    hostComponents.forEach((cInstance: HostComponent) => {
      if (addDeleteComponentsMap[cInstance.componentName]) {
        container.isReconfigureRequired = true;
      }
      let isLastComponent = false;
      if (getTotalComponent(cInstance, clusterComponents) === 1) {
        container.lastComponents.push(cInstance.displayName);
        isLastComponent = true;
      }
      const workStatus = cInstance.workStatus;

      if (cInstance.isMaster) {
        const displayName = cInstance.displayName;
        container.masterComponents.push(displayName);
        if (!isMasterAddableInstallerWizard(cInstance)) {
          container.nonAddableMasterComponents.push(displayName);
        }
        if (isLastComponent) {
          container.lastMasterComponents.push(displayName);
        }
      }
      if (!stoppedStates.includes(workStatus as ComponentStatus)) {
        container.runningComponents.push(cInstance.displayName);
      }
      if (!isDeletable(cInstance, serviceModels)) {
        container.nonDeletableComponents.push(cInstance.displayName);
      }
      if (workStatus === ComponentStatus.UNKNOWN) {
        container.unknownComponents.push(cInstance.displayName);
      }
      if (
        get(cInstance, "decommissionAllowed", false) &&
        !(cInstance.workStatus === "INSERVICE")
      ) {
        container.toDecommissionComponents.push(cInstance.displayName);
      }
    });
  }
  return container;
};

export const serviceActiveComponents = (hostComponents: IHostComponent[]) => {
  return filter(hostComponents, (component: IHostComponent) =>
    component.isActive()
  );
};

export const serviceNonClientActiveComponents = (
  hostComponents: IHostComponent[]
) => {
  const activeComponents = serviceActiveComponents(hostComponents);
  return filter(activeComponents, (component: IHostComponent) => {
    return !isClient(component);
  });
};

export const showConfirmationPopup = (body: string, callback: any) => {
  let modalProps = {
    isOpen: true,
    onClose: () => {},
    modalTitle: translate("popup.confirmation.commonHeader"),
    modalBody: body,
    successCallback: () => {
      callback();
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

export const pluralize = (name: string) => {
  return name + "s";
};

export const validateInteger = (
  str: string | number,
  min?: number,
  max?: number
): string => {
  if (typeof str === "number") {
    str = str.toString();
  }
  if (str === "" || str.trim().length < 1) {
    return translate("number.validate.empty") as string;
  }
  str = str.trim();
  const number = parseInt(str);
  if (isNaN(number)) {
    return translate("number.validate.notValidNumber") as string;
  }
  if (str.length !== number.toString().length) {
    return translate("number.validate.notValidNumber") as string;
  }
  if (min && number < min) {
    return translateWithVariables("number.validate.lessThanMinimum", {
      "0": min.toString(),
    }) as string;
  }
  if (max && number > max) {
    return translateWithVariables("number.validate.moreThanMaximum", {
      "0": max.toString(),
    }) as string;
  }
  return "";
};

export const installHostComponentCall = async (
  hostName: any,
  component: IHostComponent,
  data: any,
  setAllHostModels?: (
      data: IHost[] | ((prevModels: IHost[]) => IHost[])
    ) => void
) => {
  const componentName = getComponentName(component);
  const displayName = getComponentDisplayName(component);
  const clusterName = get(component, "clusterName", "");

  // Ensure the component has the correct hostname before proceeding
  const updatedComponent = { ...component, hostName: hostName };

  try {
    await ensureServiceComponent(
      componentName,
      get(component, "serviceName", ""),
      data,
      clusterName
    );
    const payload = {
      RequestInfo: {
        context:
          translate("requestInfo.installHostComponent") + " " + displayName,
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
    const res = await HostsApi.hostComponentAddNewComponent(
      clusterName,
      hostName,
      payload
    );
    await addNewComponentSuccessCallback(
      res,
      {},
      { component: updatedComponent },
      setAllHostModels
    );
  } catch (error) {
    showErrorModal(
      get(error, "response.data.message", get(error, "message", "Unable to add the host component."))
    );
    throw error;
  }
};

const addNewComponentSuccessCallback = async (
  _data: any,
  _opt: any,
  params: any,
  setAllHostModels?: (
      data: IHost[] | ((prevModels: IHost[]) => IHost[])
    ) => void
) => {
  const component = cloneDeep(params.component);
  const hostName = get(component, "hostName");
  const componentName = getComponentName(component);
  const clusterName = get(component, "clusterName");
  const serviceName = get(component, "serviceName");
  const displayName = get(component, "displayName");
  const context =
    translate("requestInfo.installNewHostComponent") + " " + displayName;
  const urlParams = "HostRoles/state=INIT";
  const HostRoles = {
    state: "INSTALLED",
  };

  const payload = {
    RequestInfo: {
      context: context,
      operation_level: {
        level: "HOST_COMPONENT",
        cluster_name: clusterName,
        host_name: hostName,
        service_name: serviceName || null,
      },
    },
    Body: {
      HostRoles: HostRoles,
    },
  };
  var response: any = await HostsApi.commonHostComponentUpdate(
    clusterName,
    hostName,
    componentName,
    urlParams,
    payload
  );
  if (typeof response === "string") {
    response = JSON.parse(response);
  }
  if (!response || !response.Requests || !response.Requests.id) {
    return false;
  }

  if (setAllHostModels) {
    setAllHostModels((prevModels: IHost[]) => {
      return prevModels.map((host: IHost) => {
        if (get(host, "hostName") === hostName) {
          const hostModel = cloneDeep(host);
          const hostComponents = get(
            hostModel,
            "hostComponents",
            [] as IHostComponent[]
          );
          hostComponents.push(component);
          set(
            hostModel,
            "hostComponents",
            sortBasedOnMasterSlave(hostComponents, "componentCategory")
          );
          return hostModel;
        }
        return host;
      });
    });
  }

  const requestId = get(response, "Requests.id", -1);
  defaultSuccessCallbackWithoutReload(requestId);
};

const ensureServiceComponent = async (
  componentName: string,
  serviceName: string,
  data: any,
  clusterName: string
) => {
  const allServiceComponents = get(data, "clusterComponents", {});
  const isPresent = get(allServiceComponents, "items", []).some((item: any) => {
    return get(item, "ServiceComponentInfo.component_name") === componentName;
  });
  if (isPresent) {
    return;
  }
  if (!serviceName) {
    throw new Error(`Unable to determine the service for ${componentName}.`);
  }
  const payload = {
    components: [
      {
        ServiceComponentInfo: {
          component_name: componentName,
        },
      },
    ],
  };
  await HostsApi.commonCreateComponent(clusterName, serviceName, payload);
};

export function getServiceByConfigTypeMap(stackServices: any) {
  let ret: any = {};
  stackServices?.forEach(function (s: any) {
    Object.keys(get(s, "StackServices.config_types", {})).forEach(function (
      ct
    ) {
      ret[ct] = s;
    });
  });
  return ret;
}

export const getClusterUpgradeStatusForHost = (upgradeState: string) => {
  return upgradeState === "IN_PROGRESS" || upgradeState.includes("HOLDING");
};
