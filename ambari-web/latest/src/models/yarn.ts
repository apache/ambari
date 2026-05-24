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

import Service from "./service";
import bytesToSize from "../Utils/numberUtils.ts";
//@ts-ignore
import objectUtils from "../Utils/objectUtils.ts";

type HostComponent = {
  componentName: string;
  hostName: string;
  //haNameSpace?: string;
  //clusterIdValue?: string;
};

type YARNServiceData = {
  resourceManager: HostComponent | null;
  activeResourceManager: HostComponent | null;
  activeResourceManagers: HostComponent[];
  standbyResourceManagers: HostComponent[];
  nonActiveStandbyResourceManagers: HostComponent[];
  appTimelineServer: HostComponent | null;
  nodeManagersStarted: number;
  nodeManagersInstalled: number;
  nodeManagersTotal: number;
  nodeManagersCountActive: number;
  nodeManagersCountLost: number;
  nodeManagersCountUnhealthy: number;
  nodeManagersCountRebooted: number;
  nodeManagersCountDecommissioned: number;
  containersAllocated: number;
  containersPending: number;
  containersReserved: number;
  appsSubmitted: number;
  appsRunning: number;
  appsPending: number;
  appsCompleted: number;
  appsKilled: number;
  appsFailed: number;
  resourceManagerStartTime: number;
  jvmMemoryHeapUsed: number;
  jvmMemoryHeapMax: number;
  diskPartResourceManagerHeapMemory: string;
  allocatedMemory: number;
  usedMemory: number;
  reservedMemory: number;
  availableMemory: number;
  queue: string;
  allQueueNames: string[];
  childQueueNames: string[];
  hostComponents: HostComponent[];
  masterComponents: [];
  slaveComponents: [];
  clientComponents: [];
  isRMHaEnabled: boolean;
  resourceManagerUptime: string;
  queueKeysPolledFormattedData: any;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts?: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class YARNService extends Service {
  resourceManager: HostComponent | null;
  activeResourceManager: HostComponent | null;
  activeResourceManagers: HostComponent[];
  standbyResourceManagers: HostComponent[];
  nonActiveStandbyResourceManagers: HostComponent[];
  appTimelineServer: HostComponent | null;
  nodeManagersStarted: number;
  nodeManagersInstalled: number;
  nodeManagersTotal: number;
  nodeManagersCountActive: number;
  nodeManagersCountLost: number;
  nodeManagersCountUnhealthy: number;
  nodeManagersCountRebooted: number;
  nodeManagersCountDecommissioned: number;
  containersAllocated: number;
  containersPending: number;
  containersReserved: number;
  appsSubmitted: number;
  appsRunning: number;
  appsPending: number;
  appsCompleted: number;
  appsKilled: number;
  appsFailed: number;
  resourceManagerStartTime: number;
  jvmMemoryHeapUsed: number;
  jvmMemoryHeapMax: number;
  diskPartResourceManagerHeapMemory: string;
  allocatedMemory: number;
  usedMemory: number;
  reservedMemory: number;
  availableMemory: number;
  queue: string;
  allQueueNames: string[];
  childQueueNames: string[];
  hostComponents: HostComponent[];
  masterComponents: [];
  slaveComponents: [];
  clientComponents: [];
  isRMHaEnabled: boolean;
  resourceManagerUptime: string;
  queueKeysPolledFormattedData: any;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: YARNServiceData) {
    //@ts-ignore
    super(data);
    this.resourceManager = data.resourceManager || null;
    this.activeResourceManager = data.activeResourceManager || null;
    this.activeResourceManagers = data.activeResourceManagers || [];
    this.standbyResourceManagers = data.standbyResourceManagers || [];
    this.nonActiveStandbyResourceManagers =
      data.nonActiveStandbyResourceManagers || [];
    this.appTimelineServer = data.appTimelineServer || null;
    this.nodeManagersStarted = data.nodeManagersStarted || 0;
    this.nodeManagersInstalled = data.nodeManagersInstalled || 0;
    this.nodeManagersTotal = data.nodeManagersTotal || 0;
    this.nodeManagersCountActive = data.nodeManagersCountActive || 0;
    this.nodeManagersCountLost = data.nodeManagersCountLost || 0;
    this.nodeManagersCountUnhealthy = data.nodeManagersCountUnhealthy || 0;
    this.nodeManagersCountRebooted = data.nodeManagersCountRebooted || 0;
    this.nodeManagersCountDecommissioned =
      data.nodeManagersCountDecommissioned || 0;
    this.containersAllocated = data.containersAllocated || 0;
    this.containersPending = data.containersPending || 0;
    this.containersReserved = data.containersReserved || 0;
    this.appsSubmitted = data.appsSubmitted || 0;
    this.appsRunning = data.appsRunning || 0;
    this.appsPending = data.appsPending || 0;
    this.appsCompleted = data.appsCompleted || 0;
    this.appsKilled = data.appsKilled || 0;
    this.appsFailed = data.appsFailed || 0;
    this.resourceManagerStartTime = data.resourceManagerStartTime || 0;
    this.jvmMemoryHeapUsed = data.jvmMemoryHeapUsed || 0;
    this.jvmMemoryHeapMax = data.jvmMemoryHeapMax || 0;
    this.diskPartResourceManagerHeapMemory = data.diskPartResourceManagerHeapMemory || "";
    this.allocatedMemory = data.allocatedMemory || 0;
    this.usedMemory = data.usedMemory || 0;
    this.reservedMemory = data.reservedMemory || 0;
    this.availableMemory = data.availableMemory || 0;
    this.queue = data.queue || "";
    this.allQueueNames = data.allQueueNames || [];
    this.childQueueNames = data.childQueueNames || [];
    this.hostComponents = data.hostComponents || [];
    this.masterComponents = data.masterComponents || [];
    this.slaveComponents = data.slaveComponents || [];
    this.clientComponents = data.clientComponents || [];
    this.resourceManagerUptime = data.resourceManagerUptime || "";
    this.isRMHaEnabled = data.isRMHaEnabled || false;
    this.queueKeysPolledFormattedData = data.queueKeysPolledFormattedData || {};
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  get isResourceManagerHaEnabled(): boolean {
    return (
      this.hostComponents.filter(
        (component) => component.componentName === "RESOURCEMANAGER"
      ).length > 1
    );
  }

  get ahsWebPort(): string {
    //@ts-ignore
    const yarnConf = App.db.getConfigs().findProperty("type", "yarn-site");
    if (yarnConf) {
      return yarnConf.properties["yarn.timeline-service.webapp.address"].match(
        /:(\d+)/
      )[1];
    }
    return "8188";
  }

  get queueFormatted(): any {
    const queue = JSON.parse(this.queue);
    return objectUtils.recursiveTree(queue);
  }

  get queuesCount(): number {
    const queue = JSON.parse(this.queue);
    return objectUtils.recursiveKeysCount(queue) ?? 0;
  }


  get maxMemory(): number {
    return this.allocatedMemory + this.availableMemory;
  }

  queueNames(): void {
    const queueString = this.queue;
    let allQueueNames: string[] = [];
    let childQueueNames: string[] = [];
    if (queueString != null) {
      const queues = JSON.parse(queueString);
      const addQueues = (queuesObj: any, path: string): string[] => {
        let names: string[] = [];
        for (const subQueue in queuesObj) {
          if (queuesObj[subQueue] instanceof Object) {
            const qFN = path === "" ? subQueue : `${path}/${subQueue}`;
            names.push(qFN);
            const subNames = addQueues(queuesObj[subQueue], qFN);
            names = names.concat(subNames);
            if (!subNames || subNames.length < 1) {
              childQueueNames.push(qFN);
            }
          }
        }
        return names;
      };
      allQueueNames = addQueues(queues, "");
    }
    this.allQueueNames = allQueueNames;
    this.childQueueNames = childQueueNames;
  }

  getServiceObject(): YARNService {
    return this;
  }

  updateConfig(updates: Partial<YARNService>) {
    Object.assign(this, updates);
  }

  diskPart(capacity: number, capacityTotal: number) {
    return `${bytesToSize(capacity, 1, "parseFloat")} / ${bytesToSize(capacityTotal, 1, "parseFloat")}`;
  }

  timingFormat(time: any) {
    if (!time) {
      return null;
    }

    time = parseInt(time);
    const fullTime = time;
    let duration = "";

    if (time === 0) {
      return "0s";
    }

    const oneSecMs = 1000;
    const oneMinMs = 60000;
    const oneHourMs = 3600000;
    const oneDayMs = 86400000;
    let days, hours, minutes, seconds;

    [days, time] = this.extractTimeUnit(time, oneDayMs, "d");
    [hours, time] = this.extractTimeUnit(time, oneHourMs, "h");
    [minutes, time] = this.extractTimeUnit(time, oneMinMs, "m");
    duration += days + hours + minutes;
    if (fullTime < oneDayMs) {
      [seconds, time] = this.extractTimeUnit(time, oneSecMs, "s");
      duration += seconds;
      if (fullTime < oneSecMs) {
        duration += "1s";
      }
    }
    return duration.trim();
  }

  extractTimeUnit(time: any, unitValue: any, unitSuffix: any) {
    let result = "";
    if (time >= unitValue) {
      result = Math.floor(time / unitValue) + `${unitSuffix} `;
      time -= Math.floor(time / unitValue) * unitValue;
    }
    return [result, time];
  }

  parseObject(obj: Record<string, any>): Record<string, any> {
    const res: Record<string, any> = {};
    for (const p in obj) {
      if (Object.prototype.hasOwnProperty.call(obj, p)) {
        if (obj[p] instanceof Object) {
          res[p] = this.parseObject(obj[p]);
        }
      }
    }
    return res;
  }
}

export default YARNService;
