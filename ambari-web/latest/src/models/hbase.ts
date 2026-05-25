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

type HostComponent = {
  componentName: string;
  hostName: string;
  haNameSpace?: string;
  clusterIdValue?: string;
  haStatus?: string;
};

type HBaseServiceData = {
  // master: HostComponent | null;
  activeHbaseMasters: HostComponent[];
  standbyHbaseMasters: HostComponent[];
  nonActiveStandbyHbaseMasters: HostComponent[];
  regionServersStarted: number;
  regionServersInstalled: number;
  regionServersTotal: number;
  phoenixServersStarted: number;
  phoenixServersInstalled: number;
  phoenixServersTotal: number;
  masterStartTime: string;
  masterActiveTime: string;
  averageLoad: string;
  regionsInTransition: number;
  heapMemoryUsed: number;
  heapMemoryMax: number;
  masterComponents: [];
  slaveComponents: [];
  clientComponents: [];
  diskPartHbaseMasterHeap: string;
  percentHbaseMasterHeap: string;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class HBaseService extends Service {
  // master: HostComponent | null;
  activeHbaseMasters: HostComponent[];
  standbyHbaseMasters: HostComponent[];
  nonActiveStandbyHbaseMasters: HostComponent[];
  regionServersStarted: number;
  regionServersInstalled: number;
  regionServersTotal: number;
  phoenixServersStarted: number;
  phoenixServersInstalled: number;
  phoenixServersTotal: number;
  masterStartTime: string;
  masterActiveTime: string;
  averageLoad: string;
  regionsInTransition: number;
  heapMemoryUsed: number;
  heapMemoryMax: number;
  masterComponents: [];
  slaveComponents: [];
  clientComponents: [];
  diskPartHbaseMasterHeap: string;
  percentHbaseMasterHeap: string;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;



  constructor(data: HBaseServiceData) {
    //@ts-ignore
    super(data);
    // this.master = data.master || null;
    this.regionServersStarted = data.regionServersStarted || 0;
    this.regionServersInstalled = data.regionServersInstalled || 0;
    this.regionServersTotal = data.regionServersTotal || 0;
    this.phoenixServersStarted = data.phoenixServersStarted || 0;
    this.phoenixServersInstalled = data.phoenixServersInstalled || 0;
    this.phoenixServersTotal = data.phoenixServersTotal || 0;
    this.masterStartTime = data.masterStartTime || "Not Running";
    this.masterActiveTime = data.masterActiveTime || "Not Running";
    this.averageLoad = data.averageLoad || "n/a";
    this.regionsInTransition = data.regionsInTransition || 0;
    this.heapMemoryUsed = data.heapMemoryUsed || 0;
    this.heapMemoryMax = data.heapMemoryMax || 0;
    this.masterComponents = data.masterComponents || [];
    this.slaveComponents = data.slaveComponents || [];
    this.clientComponents = data.clientComponents || [];
    this.activeHbaseMasters = data.activeHbaseMasters || [];
    this.standbyHbaseMasters = data.standbyHbaseMasters || [];
    this.nonActiveStandbyHbaseMasters = data.nonActiveStandbyHbaseMasters || [];
    this.diskPartHbaseMasterHeap = data.diskPartHbaseMasterHeap || "N/A";
    this.percentHbaseMasterHeap = data.percentHbaseMasterHeap || "N/A";
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  //@ts-ignore
  findCapacityPercentage(capacity, capacityTotal) {
    let percent =
      capacityTotal && capacity && capacityTotal > 0
        ? ((capacity * 100) / capacityTotal).toFixed(2)
        : 0;
    if (isNaN(<number>percent) || <number>percent < 0) {
      percent = "N/A";
    }
    return `${percent}%`;
  }

  diskPart(capacity: number, capacityTotal: number) {
    return `${bytesToSize(capacity, 1, "parseFloat")} / ${bytesToSize(capacityTotal, 1, "parseFloat")}`;
  }

  updateConfig(updates: Partial<HBaseService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): HBaseService {
    return this;
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
}

export default HBaseService;
