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
import { IHostComponentLog } from "./hostComponentLog";
import { IHost } from "./host";
import { ComponentStatus } from "../screens/Hosts/enums";

export interface IHostComponent {
  workStatus: string;
  passiveState: string;
  componentName: string;
  displayName: string;
  haStatus: string;
  displayNameAdvanced: string;
  staleConfigs: boolean;
  host: IHost;
  componentLogs: IHostComponentLog;
  hostName: string;
  publicHostName: string;
  service: any;
  adminState: string;
  haNameSpace: string;
  clusterIdValue: string;
  cardinality: string;
  customCommands: string[];
  reassignAllowed: boolean;
  decommissionAllowed: boolean;
  hasBulkCommandsDefinition: boolean;
  bulkCommandsDisplayName: string;
  bulkCommandsMasterComponentName: string;
  dependencies: any[];
  serviceName: string;
  componentCategory: string;
  rollingRestartSupported: boolean;
  isMaster: boolean;
  isClient: boolean;
  componentType: string;
  stackName: string;
  stackVersion: string;
  recoveryEnabled: boolean;
  advertiseVersion: boolean;
  hasCriticalAlerts: boolean;
  alertsCount: number;
  clusterName: string;
  nnHAState: string;
  isRunning(): boolean;
  isNotInstalled(): boolean;
  isSlave(): boolean;
  isDecommissioning(): boolean;
  isActive(): boolean;
  serviceDisplayName(): string;
  getDisplayName(): string;
  getDisplayNameAdvanced(): string;
  summaryLabelClassName(): string;
  summaryValueClassName(): string;
  isImpliedState(): boolean;
  passiveTooltip(): string;
  updateObject(updates: Partial<IHostComponent>): void;
  getObject(): IHostComponent;
}

class HostComponent implements IHostComponent {
  workStatus: string;
  passiveState: string;
  componentName: string;
  displayName: string;
  haStatus: string;
  displayNameAdvanced: string;
  staleConfigs: boolean;
  host: IHost;
  componentLogs: IHostComponentLog;
  hostName: string;
  publicHostName: string;
  service: any;
  adminState: string;
  haNameSpace: string;
  clusterIdValue: string;
  hasCriticalAlerts: boolean;
  alertsCount: number;
  cardinality: string;
  customCommands: string[];
  reassignAllowed: boolean;
  decommissionAllowed: boolean;
  hasBulkCommandsDefinition: boolean;
  bulkCommandsDisplayName: string;
  bulkCommandsMasterComponentName: string;
  dependencies: any[];
  serviceName: string;
  componentCategory: string;
  rollingRestartSupported: boolean;
  isMaster: boolean;
  isClient: boolean;
  componentType: string;
  stackName: string;
  stackVersion: string;
  recoveryEnabled: boolean;
  advertiseVersion: boolean;
  clusterName: string;
  nnHAState: string;

  constructor(props: IHostComponent) {
    this.workStatus = get(props, "workStatus", ComponentStatus.INIT);
    this.passiveState = get(props, "passiveState", "OFF");
    this.componentName = get(props, "componentName", "");
    this.displayName = get(props, "displayName", "");
    this.haStatus = get(props, "haStatus", "");
    this.displayNameAdvanced = get(props, "displayNameAdvanced", "");
    this.staleConfigs = get(props, "staleConfigs", false);
    this.host = get(props, "host", {} as IHost);
    this.componentLogs = get(props, "componentLogs", {} as IHostComponentLog);
    this.hostName = get(props, "hostName", "");
    this.publicHostName = get(props, "publicHostName", "");
    this.service = get(props, "service", {});
    this.adminState = get(props, "adminState", "");
    this.haNameSpace = get(props, "haNameSpace", "");
    this.clusterIdValue = get(props, "clusterIdValue", "");
    this.cardinality = get(props, "cardinality", "");
    this.customCommands = get(props, "customCommands", []);
    this.reassignAllowed = get(props, "reassignAllowed", false);
    this.decommissionAllowed = get(props, "decommissionAllowed", false);
    this.hasBulkCommandsDefinition = get(
      props,
      "hasBulkCommandsDefinition",
      false
    );
    this.bulkCommandsDisplayName = get(props, "bulkCommandsDisplayName", "");
    this.bulkCommandsMasterComponentName = get(
      props,
      "bulkCommandsMasterComponentName",
      ""
    );
    this.dependencies = get(props, "dependencies", []);
    this.serviceName = get(props, "serviceName", "");
    this.componentCategory = get(props, "componentCategory", "");
    this.rollingRestartSupported = get(props, "rollingRestartSupported", false);
    this.isMaster = get(props, "isMaster", false);
    this.isClient = get(props, "isClient", false);
    this.componentType = get(props, "componentType", "");
    this.stackName = get(props, "stackName", "");
    this.stackVersion = get(props, "stackVersion", "");
    this.recoveryEnabled = get(props, "recoveryEnabled", false);
    this.advertiseVersion = get(props, "advertiseVersion", false);
    this.hasCriticalAlerts = false;
    this.alertsCount = 0;
    this.clusterName = get(props, "clusterName", "");
    this.nnHAState = get(props, "nnHAState", "");
  }

  isRunning(): boolean {
    return ["STARTED", "STARTING"].includes(this.workStatus);
  }

  isNotInstalled(): boolean {
    return ["INIT", "INSTALL_FAILED"].includes(this.workStatus);
  }

  isSlave(): boolean {
    return !this.isMaster && !this.isClient;
  }

  isDecommissioning(): boolean {
    const hdfsSvc = this.service;
    if (this.componentName === "DATANODE" && hdfsSvc) {
      const decomNodes = hdfsSvc.decommissionDataNodes;
      const decomNode = decomNodes
        ? decomNodes.find((node: any) => node.hostName === this.hostName)
        : null;
      return decomNode != null;
    }
    return false;
  }

  isActive(): boolean {
    let passiveState = this.passiveState;
    if (passiveState === "IMPLIED_FROM_HOST") {
      passiveState = this.host.passiveState;
    } else if (passiveState === "IMPLIED_FROM_SERVICE") {
      passiveState = this.service.passiveState;
    } else if (passiveState === "IMPLIED_FROM_SERVICE_AND_HOST") {
      return (
        this.service.passiveState === "OFF" && this.host.passiveState === "OFF"
      );
    }
    return passiveState === "OFF";
  }

  serviceDisplayName(): string {
    return this.service.displayName.length > 14
      ? this.service.displayName.substring(0, 11) + "..."
      : this.service.displayName;
  }

  getDisplayName(): string {
    return this.displayName.length > 30
      ? this.displayName.substring(0, 25) + "..."
      : this.displayName;
  }

  getDisplayNameAdvanced(): string {
    return this.displayNameAdvanced.length > 30
      ? this.displayNameAdvanced.substring(0, 25) + "..."
      : this.displayNameAdvanced;
  }

  summaryLabelClassName(): string {
    return "label_for_" + this.componentName.toLowerCase();
  }

  summaryValueClassName(): string {
    return "value_for_" + this.componentName.toLowerCase();
  }

  isImpliedState(): boolean {
    return [
      "IMPLIED_FROM_SERVICE_AND_HOST",
      "IMPLIED_FROM_HOST",
      "IMPLIED_FROM_SERVICE",
    ].includes(this.passiveState);
  }

  passiveTooltip(): string {
    return this.isActive() ? "" : "Component is in Maintenance Mode";
  }

  updateObject(updates: Partial<IHostComponent>) {
    Object.assign(this, updates);
  }

  getObject(): IHostComponent {
    return this;
  }

  static getTextStatus(workStatus: string): string {
    const statusMap: { [key: string]: string } = {
      STARTED: "Started",
      STARTING: "Starting...",
      INSTALLED: "Stopped",
      STOPPING: "Stopping...",
      INSTALL_FAILED: "Install Failed",
      INSTALLING: "Installing...",
      UPGRADE_FAILED: "Upgrade Failed",
      UNKNOWN: "Heartbeat Lost",
      DISABLED: "Disabled",
      INIT: "Install Pending...",
    };
    return statusMap[workStatus] || "Unknown";
  }

  static FIXTURES: IHostComponent[] = [];
}

export default HostComponent;
