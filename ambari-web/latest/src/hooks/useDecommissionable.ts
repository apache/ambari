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

import { useContext, useEffect, useState, useCallback, useRef } from "react";
import { IHost } from "../models/host";
import { get, isEmpty, set } from "lodash";
import { ServiceContext } from "../store/ServiceContext";
import { HostsApi } from "../api/hostsApi";
import { AppContext } from "../store/context";
import { IHostComponent } from "../models/hostComponent";
import { getComponentName } from "../screens/Hosts/utils";
import { ComponentStatus } from "../screens/Hosts/enums";
import { serviceNameModelMapping } from "../constants";

export const decommissionableComponents = [
  "DATANODE",
  "NODEMANAGER",
  "HBASE_REGIONSERVER",
  "TASKTRACKER",
];
const POLLING_INTERVAL = 6000;

interface DecommissionState {
  componentForCheckDecommission: string;
  isComponentRecommissionAvailable: boolean;
  isComponentDecommissionAvailable: boolean;
  isComponentDecommissioning: boolean;
}

interface DecommissionableState {
  DATANODE: DecommissionState;
  NODEMANAGER: DecommissionState;
  HBASE_REGIONSERVER: DecommissionState;
  TASKTRACKER: DecommissionState;
}

abstract class BaseDecommissionableComponent {
  protected serviceModels: any;
  protected clusterName: string;
  protected setDecommissionable: React.Dispatch<
    React.SetStateAction<DecommissionableState>
  >;
  protected startPolling: (component: IHostComponent) => void;
  protected stopPolling: (component: IHostComponent) => void;

  constructor(
    serviceModels: any,
    clusterName: string,
    setDecommissionable: React.Dispatch<
      React.SetStateAction<DecommissionableState>
    >,
    startPolling: (component: IHostComponent) => void,
    stopPolling: (component: IHostComponent) => void
  ) {
    this.serviceModels = serviceModels;
    this.clusterName = clusterName;
    this.setDecommissionable = setDecommissionable;
    this.startPolling = startPolling;
    this.stopPolling = stopPolling;
  }

  abstract loadDecommissionStatus(component: IHostComponent): Promise<void>;
  abstract setDesiredAdminState(
    desiredAdminState: string,
    component: IHostComponent
  ): void;

  protected setStatusAs(status: string, component: IHostComponent): void {
    const componentName = getComponentName(component);
    const isStart = [
      ComponentStatus.STARTED,
      ComponentStatus.STARTING,
    ].includes(get(component, "workStatus") as ComponentStatus);

    this.setDecommissionable((prevState) => {
      const updatedState: any = { ...prevState };

      switch (status) {
        case "INSERVICE":
          updatedState[componentName] = {
            ...updatedState[componentName],
            isComponentRecommissionAvailable: false,
            isComponentDecommissioning: false,
            isComponentDecommissionAvailable: isStart,
          };
          break;

        case "DECOMMISSIONING":
          updatedState[componentName] = {
            ...updatedState[componentName],
            isComponentRecommissionAvailable: true,
            isComponentDecommissioning: true,
            isComponentDecommissionAvailable: false,
          };
          break;

        case "DECOMMISSIONED":
          updatedState[componentName] = {
            ...updatedState[componentName],
            isComponentRecommissionAvailable: true,
            isComponentDecommissioning: false,
            isComponentDecommissionAvailable: false,
          };
          break;

        case "RS_DECOMMISSIONED":
          updatedState[componentName] = {
            ...updatedState[componentName],
            isComponentRecommissionAvailable: true,
            isComponentDecommissioning: isStart,
            isComponentDecommissionAvailable: false,
          };
          break;
      }

      return updatedState;
    });
  }

  protected async getDesiredAdminState(
    component: IHostComponent
  ): Promise<string | null> {
    if (!component) return null;

    try {
      const response = await HostsApi.getSlaveDesiredAdminState(
        this.clusterName,
        get(component, "hostName"),
        getComponentName(component)
      );
      const status = get(response, "HostRoles.desired_admin_state");
      if (status) {
        this.setDesiredAdminState(status, component);
        return status;
      }
      return null;
    } catch (error) {
      return null;
    }
  }
}

class DataNodeComponent extends BaseDecommissionableComponent {
  async loadDecommissionStatus(component: IHostComponent): Promise<void> {
    await this.getDNDecommissionStatus(component);
  }

  setDesiredAdminState(
    desiredAdminState: string,
    component: IHostComponent
  ): void {
    this.setStatusAs(desiredAdminState, component);
  }

  private async getDNDecommissionStatus(
    component: IHostComponent
  ): Promise<void> {
    const hdfs = get(this.serviceModels, "hdfs", {});
    let activeNNHostNames = "";

    if (
      !get(hdfs, "snameNode") &&
      get(hdfs, "activeNameNodes", []).length > 0
    ) {
      activeNNHostNames = get(hdfs, "activeNameNodes", [])
        .map((nn: any) => get(nn, "hostName"))
        .join(",");
    } else {
      activeNNHostNames = get(hdfs, "nameNode.hostName");
    }

    // Desired admin state is authoritative for the target; the live LiveNodes
    // view only tells us whether that target has been reached. Reconciling both
    // avoids settling on a transient live reading.
    let liveMetrics: any[] = [];
    let desiredAdminState: string | null = null;
    try {
      const response = await HostsApi.getDecommissionStatusForDataNode(
        this.clusterName,
        activeNNHostNames
      );
      if (response && response.items) {
        liveMetrics = response.items.map((item: any) =>
          get(item, "metrics.dfs.namenode")
        );
      }
    } catch (error) {
      console.error("Failed to get DataNode decommission status");
    }
    try {
      const dResp = await HostsApi.getSlaveDesiredAdminState(
        this.clusterName,
        get(component, "hostName"),
        getComponentName(component)
      );
      desiredAdminState = get(dResp, "HostRoles.desired_admin_state", null);
    } catch (error) {
      desiredAdminState = null;
    }

    this.reconcileStatus(liveMetrics, desiredAdminState, component);
  }

  private getLiveAdminState(
    metricObjects: any[],
    hostName: string
  ): string | null {
    for (const curObj of metricObjects) {
      if (!curObj) continue;
      const liveNodesJson = JSON.parse(curObj.LiveNodes || "{}");
      for (const hostPort in liveNodesJson) {
        if (hostPort.indexOf(hostName) === 0) {
          return liveNodesJson[hostPort].adminState;
        }
      }
    }
    return null;
  }

  private reconcileStatus(
    metricObjects: any[],
    desiredAdminState: string | null,
    component: IHostComponent
  ): void {
    const hostName = get(component, "hostName");
    const liveAdminState = this.getLiveAdminState(metricObjects, hostName);

    // Desired admin state reflects the operator's request and is what the icon
    // shows. Polling keeps running so a later backend change is picked up.
    if (desiredAdminState === "DECOMMISSIONED") {
      this.setStatusAs("DECOMMISSIONED", component);
      return;
    }

    if (desiredAdminState === "INSERVICE") {
      this.setStatusAs("INSERVICE", component);
      return;
    }

    // No desired admin state - fall back to the live reading.
    switch (liveAdminState) {
      case "Decommission In Progress":
        this.setStatusAs("DECOMMISSIONING", component);
        break;
      case "Decommissioned":
        this.setStatusAs("DECOMMISSIONED", component);
        break;
      case "In Service":
        this.setStatusAs("INSERVICE", component);
        break;
      default:
        this.setStatusAs("INSERVICE", component);
    }
  }
}

class NodeManagerComponent extends BaseDecommissionableComponent {
  async loadDecommissionStatus(component: IHostComponent): Promise<void> {
    await this.getDesiredAdminState(component);
  }

  setDesiredAdminState(
    desiredAdminState: string,
    component: IHostComponent
  ): void {
    switch (desiredAdminState) {
      case "INSERVICE":
        this.setStatusAs(desiredAdminState, component);
        break;
      case "DECOMMISSIONED":
        this.getDecommissionStatus(component);
        break;
    }
  }

  private async getDecommissionStatus(
    component: IHostComponent
  ): Promise<void> {
    if (!component) return;

    const serviceName = get(component, "serviceName");
    const componentName = "RESOURCEMANAGER";

    try {
      const response = await HostsApi.getDecommissionStatus(
        this.clusterName,
        serviceName,
        componentName
      );
      this.handleDecommissionStatusResponse(response, component);
    } catch (error) {}
  }

  private handleDecommissionStatusResponse(
    response: any,
    component: IHostComponent
  ): void {
    const statusObject = get(response, "ServiceComponentInfo");
    if (statusObject) {
      set(
        statusObject,
        "component_state",
        get(response, "host_components.[0].HostRoles.state")
      );
      this.setDecommissionStatusForNodeManager(statusObject, component);
    }
  }

  private setDecommissionStatusForNodeManager(
    curObj: any,
    component: IHostComponent
  ): void {
    const hostName = get(component, "hostName");

    const rmComponent = get(
      this.serviceModels,
      "yarn.masterComponents",
      []
    ).find((mc: any) => get(mc, "componentName") === "RESOURCEMANAGER");
    if (rmComponent) {
      set(rmComponent, "workStatus", curObj.component_state);
    }

    if (curObj.rm_metrics) {
      // Update RESOURCEMANAGER status
      const nodeManagersArray = JSON.parse(
        curObj.rm_metrics.cluster.nodeManagers
      );
      if (nodeManagersArray.find((nm: any) => nm.HostName === hostName)) {
        // decommissioning ..
        this.setStatusAs("DECOMMISSIONING", component);
      } else {
        // decommissioned ..
        this.setStatusAs("DECOMMISSIONED", component);
      }
    } else {
      // in this case ResourceManager not started. Set status to Decommissioned
      this.setStatusAs("DECOMMISSIONED", component);
    }
  }
}

class TaskTrackerComponent extends BaseDecommissionableComponent {
  async loadDecommissionStatus(component: IHostComponent): Promise<void> {
    await this.getDesiredAdminState(component);
  }

  setDesiredAdminState(
    desiredAdminState: string,
    component: IHostComponent
  ): void {
    switch (desiredAdminState) {
      case "INSERVICE":
        this.setStatusAs("INSERVICE", component);
        break;
      case "DECOMMISSIONED":
        this.getDecommissionStatus(component);
        break;
    }
  }

  private async getDecommissionStatus(
    component: IHostComponent
  ): Promise<void> {
    if (!component) return;

    const serviceName = get(component, "serviceName");
    const componentName = "JOBTRACKER";

    try {
      const response = await HostsApi.getDecommissionStatus(
        this.clusterName,
        serviceName,
        componentName
      );
      this.handleDecommissionStatusResponse(response, component);
    } catch (error) {}
  }

  private handleDecommissionStatusResponse(
    response: any,
    component: IHostComponent
  ): void {
    const statusObject = get(response, "ServiceComponentInfo");
    if (statusObject) {
      set(
        statusObject,
        "component_state",
        get(response, "host_components.[0].HostRoles.state")
      );
      this.setDecommissionStatusForTaskTracker(statusObject, component);
    }
  }

  private setDecommissionStatusForTaskTracker(
    curObj: any,
    component: IHostComponent
  ): void {
    const hostName = get(component, "hostName");

    if (curObj) {
      const aliveNodesArray = JSON.parse(curObj.AliveNodes || "[]");
      if (aliveNodesArray && Array.isArray(aliveNodesArray)) {
        if (aliveNodesArray.some((node) => node.hostname === hostName)) {
          // decommissioning ..
          this.setStatusAs("DECOMMISSIONING", component);
        } else {
          // decommissioned
          this.setStatusAs("DECOMMISSIONED", component);
        }
      }
    }
  }
}

class RegionServerComponent extends BaseDecommissionableComponent {
  async loadDecommissionStatus(component: IHostComponent): Promise<void> {
    await this.getDesiredAdminState(component);
  }

  setDesiredAdminState(
    desiredAdminState: string,
    component: IHostComponent
  ): void {
    this.getRSDecommissionStatus(desiredAdminState, component);
  }

  private async getRSDecommissionStatus(
    desiredAdminState: string,
    component: IHostComponent
  ): Promise<void> {
    const hostName = get(
      this.serviceModels,
      "hbase.masterComponents.[0].hostComponents.[0].HostRoles.host_name"
    );
    if (!hostName) {
      return;
    }

    try {
      const response = await HostsApi.getDecommissionStatusForRegionServer(
        this.clusterName,
        hostName
      );
      this.handleRSDecommissionStatusResponse(
        response,
        desiredAdminState,
        component
      );
    } catch (error) {
      this.setDesiredAdminStateDefault(desiredAdminState, component);
    }
  }

  private handleRSDecommissionStatusResponse(
    data: any,
    desiredAdminState: string,
    component: IHostComponent
  ): void {
    const hostName = get(component, "hostName");

    if (data) {
      const liveRSHostsMetrics = get(
        data,
        "items.[0].metrics.hbase.master.liveRegionServersHosts"
      );
      const deadRSHostsMetrics = get(
        data,
        "items.[0].metrics.hbase.master.deadRegionServersHosts"
      );

      const liveRSHosts = this.parseRegionServersHosts(liveRSHostsMetrics);
      const deadRSHosts = this.parseRegionServersHosts(deadRSHostsMetrics);

      const isLiveRS = liveRSHosts.includes(hostName);
      const isDeadRS = deadRSHosts.includes(hostName);

      const isInServiceDesired = desiredAdminState === "INSERVICE";
      const isDecommissionedDesired = desiredAdminState === "DECOMMISSIONED";

      if (
        liveRSHosts.length + deadRSHosts.length === 0 ||
        (isInServiceDesired && isLiveRS) ||
        (isDecommissionedDesired && isDeadRS)
      ) {
        this.setDesiredAdminStateDefault(desiredAdminState, component);
      } else if (isInServiceDesired) {
        this.setStatusAs("RS_DECOMMISSIONED", component);
      } else if (isDecommissionedDesired) {
        this.setStatusAs("INSERVICE", component);
      }
    } else {
      this.setDesiredAdminStateDefault(desiredAdminState, component);
    }
  }

  private parseRegionServersHosts(str: string): string[] {
    const items = str ? str.split(";") : [];
    return items.map((item) => item.split(",")[0]);
  }

  private setDesiredAdminStateDefault(
    desiredAdminState: string,
    component: IHostComponent
  ): void {
    switch (desiredAdminState) {
      case "INSERVICE":
        this.setStatusAs(desiredAdminState, component);
        break;
      case "DECOMMISSIONED":
        this.setStatusAs("RS_DECOMMISSIONED", component);
        break;
    }
  }
}

class DecommissionableComponentFactory {
  static createComponent(
    componentName: string,
    serviceModels: any,
    clusterName: string,
    setDecommissionable: React.Dispatch<
      React.SetStateAction<DecommissionableState>
    >,
    startPolling: (component: IHostComponent) => void,
    stopPolling: (component: IHostComponent) => void
  ): BaseDecommissionableComponent | null {
    switch (componentName) {
      case "DATANODE":
        return new DataNodeComponent(
          serviceModels,
          clusterName,
          setDecommissionable,
          startPolling,
          stopPolling
        );
      case "NODEMANAGER":
        return new NodeManagerComponent(
          serviceModels,
          clusterName,
          setDecommissionable,
          startPolling,
          stopPolling
        );
      case "HBASE_REGIONSERVER":
        return new RegionServerComponent(
          serviceModels,
          clusterName,
          setDecommissionable,
          startPolling,
          stopPolling
        );
      case "TASKTRACKER":
        return new TaskTrackerComponent(
          serviceModels,
          clusterName,
          setDecommissionable,
          startPolling,
          stopPolling
        );
      default:
        return null;
    }
  }
}

// Main hook
export const useDecommissionable = (host: IHost) => {
  const { allServiceModels: serviceModels } = useContext(ServiceContext);
  const { clusterName } = useContext(AppContext);
  const [decommissionable, setDecommissionable] =
    useState<DecommissionableState>({
      DATANODE: {
        componentForCheckDecommission: "NAMENODE",
        isComponentRecommissionAvailable: false,
        isComponentDecommissionAvailable: false,
        isComponentDecommissioning: false,
      },
      NODEMANAGER: {
        componentForCheckDecommission: "RESOURCEMANAGER",
        isComponentRecommissionAvailable: false,
        isComponentDecommissionAvailable: false,
        isComponentDecommissioning: false,
      },
      HBASE_REGIONSERVER: {
        componentForCheckDecommission: "HBASE_MASTER",
        isComponentRecommissionAvailable: false,
        isComponentDecommissionAvailable: false,
        isComponentDecommissioning: false,
      },
      TASKTRACKER: {
        componentForCheckDecommission: "JOBTRACKER",
        isComponentRecommissionAvailable: false,
        isComponentDecommissionAvailable: false,
        isComponentDecommissioning: false,
      },
    });

  const pollingTimers = useRef<Map<string, NodeJS.Timeout>>(new Map());
  // Active polling keys; a terminal state removes its key so the in-flight tick
  // knows not to re-arm.
  const activePolls = useRef<Set<string>>(new Set());
  // Ref to the latest loader so the polling closure never reads a stale snapshot.
  const loadStatusRef = useRef<((component: IHostComponent) => Promise<void>) | undefined>(undefined);

  useEffect(() => {
    return () => {
      pollingTimers.current.forEach((timer) => {
        clearTimeout(timer);
      });
      pollingTimers.current.clear();
      activePolls.current.clear();
    };
  }, []);

  const startDecommissionStatusPolling = useCallback(
    (component: IHostComponent) => {
      const componentKey = `${get(component, "hostName")}_${getComponentName(
        component
      )}`;

      if (!activePolls.current.has(componentKey)) {
        activePolls.current.add(componentKey);
        const pollStatus = async () => {
          try {
            await loadStatusRef.current?.(component);
          } catch (error) {
            console.error("Error during decommission status polling:", error);
          }
          // Re-arm while active; cleared only on unmount.
          if (activePolls.current.has(componentKey)) {
            const next = setTimeout(pollStatus, POLLING_INTERVAL);
            pollingTimers.current.set(componentKey, next);
          }
        };

        const timer = setTimeout(pollStatus, POLLING_INTERVAL);
        pollingTimers.current.set(componentKey, timer);
      }
    },
    []
  );

  const stopDecommissionStatusPolling = useCallback(
    (component: IHostComponent) => {
      const componentKey = `${get(component, "hostName")}_${getComponentName(
        component
      )}`;
      activePolls.current.delete(componentKey);
      const timer = pollingTimers.current.get(componentKey);

      if (timer) {
        clearTimeout(timer);
        pollingTimers.current.delete(componentKey);
      }
    },
    []
  );

  const isComponentDecommissionDisable = (
    component: IHostComponent
  ): boolean => {
    const componentName = getComponentName(component);
    const masterComponentName = get(
      decommissionable,
      `${componentName}.componentForCheckDecommission`
    );

    if (!masterComponentName) return false;

    const service = get(
      serviceModels,
      get(serviceNameModelMapping, component.serviceName)
    );

    const masterComponents = get(service, "masterComponents", []);
    const masterComponent = masterComponents.find(
      (mc: any) => get(mc, "componentName") === masterComponentName
    );

    if (masterComponent) {
      const masterHostComponents = get(masterComponent, "hostComponents", []);
      const hasStoppedMaster = masterHostComponents.some(
        (hc: any) => get(hc, "state") !== ComponentStatus.STARTED
      );
      if (hasStoppedMaster) return true;
    }

    const serviceWorkStatus = get(service, "serviceState");
    return serviceWorkStatus !== ComponentStatus.STARTED;
  };

  const loadComponentDecommissionStatus = async (
    component: IHostComponent
  ): Promise<void> => {
    const componentName = getComponentName(component);
    const componentHandler = DecommissionableComponentFactory.createComponent(
      componentName,
      serviceModels,
      clusterName,
      setDecommissionable,
      startDecommissionStatusPolling,
      stopDecommissionStatusPolling
    );

    if (componentHandler) {
      await componentHandler.loadDecommissionStatus(component);
    }
  };
  
  // Keep the polling closure pointed at the current loader.
  loadStatusRef.current = loadComponentDecommissionStatus;

  useEffect(() => {
    if (!isEmpty(host)) {
      get(host, "hostComponents", []).forEach((hostComponent: IHostComponent) => {
        loadComponentDecommissionStatus(hostComponent);
      });
    }
  }, [
    JSON.stringify(host),
    JSON.stringify(get(serviceModels, "hdfs.nameNode")),
    JSON.stringify(get(serviceModels, "hbase.masterComponents"))
  ]);

  return {
    decommissionable,
    isComponentDecommissionDisable,
    startDecommissionStatusPolling,
    stopDecommissionStatusPolling,
    loadComponentDecommissionStatus,
  };
};
