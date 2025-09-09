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
import { misc } from "../Utils/misc";
import { IHostComponent } from "./hostComponent";
import { IHostStackVersion } from "./hostStackVersion";
import { ComponentStatus } from "../screens/Hosts/enums";

export interface IHost {
  hostName: string;
  publicHostName: string;
  cluster: any;
  hostComponents: IHostComponent[];
  cpu: string;
  cpuPhysical: string;
  memory: number;
  diskTotal: number;
  diskFree: number;
  osArch: string;
  ip: string;
  rack: string;
  healthStatus: string;
  state: string;
  lastHeartBeatTime: number;
  hasJcePolicy: boolean;
  osType: string;
  diskInfo: any;
  loadOne: number;
  loadFive: number;
  loadFifteen: number;
  memTotal: number;
  memFree: number;
  cpuSystem: number;
  cpuUser: number;
  alertsSummary: any;
  passiveState: string;
  index: number;
  stackVersions: IHostStackVersion[];
  isActive(): boolean;
  criticalWarningAlertsCount(): number;
  currentVersion(): string | null;
  cpuUsage(): number;
  memoryUsage(): number;
  notStartedComponents(): IHostComponent[];
  componentsWithStaleConfigs(): IHostComponent[];
  componentsInPassiveState(): IHostComponent[];
  componentsInPassiveStateCount(): number;
  componentsWithStaleConfigsCount(): number;
  disksMounted(): number;
  coresFormatted(): string;
  diskUsed(): number;
  diskUsedFormatted(): string;
  diskTotalFormatted(): string;
  diskUsage(): number;
  diskUsageFormatted(): string;
  diskInfoBar(): string;
  memoryFormatted(): string;
  isNotHeartBeating(): boolean;
  loadAvg(): number | null;
  updateObject(updates: Partial<IHost>): void;
  getObject(): IHost;
}

class Host implements IHost {
  hostName: string;
  publicHostName: string;
  cluster: any;
  hostComponents: IHostComponent[];
  cpu: string;
  cpuPhysical: string;
  memory: number;
  diskTotal: number;
  diskFree: number;
  osArch: string;
  ip: string;
  rack: string;
  healthStatus: string;
  state: string;
  lastHeartBeatTime: number;
  hasJcePolicy: boolean;
  osType: string;
  diskInfo: any;
  loadOne: number;
  loadFive: number;
  loadFifteen: number;
  memTotal: number;
  memFree: number;
  cpuSystem: number;
  cpuUser: number;
  alertsSummary: any;
  passiveState: string;
  index: number;
  stackVersions: IHostStackVersion[];

  constructor(props: IHost) {
    this.hostName = get(props, "hostName", "");
    this.publicHostName = get(props, "publicHostName", "");
    this.cluster = get(props, "cluster", {});
    this.hostComponents = get(props, "hostComponents", []);
    this.cpu = get(props, "cpu", "");
    this.cpuPhysical = get(props, "cpuPhysical", "");
    this.memory = get(props, "memory", 0);
    this.diskTotal = get(props, "diskTotal", 0);
    this.diskFree = get(props, "diskFree", 0);
    this.osArch = get(props, "osArch", "");
    this.ip = get(props, "ip", "");
    this.rack = get(props, "rack", "");
    this.healthStatus = get(props, "healthStatus", "");
    this.state = get(props, "state", "");
    this.lastHeartBeatTime = get(props, "lastHeartBeatTime", 0);
    this.hasJcePolicy = get(props, "hasJcePolicy", true);
    this.osType = get(props, "osType", "");
    this.diskInfo = get(props, "diskInfo", {});
    this.loadOne = get(props, "loadOne", 0);
    this.loadFive = get(props, "loadFive", 0);
    this.loadFifteen = get(props, "loadFifteen", 0);
    this.memTotal = get(props, "memTotal", 0);
    this.memFree = get(props, "memFree", 0);
    this.cpuSystem = get(props, "cpuSystem", 0);
    this.cpuUser = get(props, "cpuUser", 0);
    this.alertsSummary = get(props, "alertsSummary", {});
    this.passiveState = get(props, "passiveState", "");
    this.index = get(props, "index", 0);
    this.stackVersions = get(props, "stackVersions", []);
  }

  isActive(): boolean {
    return this.passiveState === "OFF";
  }

  criticalWarningAlertsCount(): number {
    const alertsSummary = this.alertsSummary;
    return alertsSummary
      ? (alertsSummary.CRITICAL || 0) + (alertsSummary.WARNING || 0)
      : 0;
  }

  currentVersion() {
    const current = this.stackVersions.find((version) => version.isCurrent());
    return current ? current.repoVersion : null;
  }

  cpuUsage(): number {
    if (this.cpuSystem && this.cpuUser) {
      return this.cpuSystem + this.cpuUser;
    }
    return 0;
  }

  memoryUsage(): number {
    if (this.memFree && this.memTotal) {
      const memUsed = this.memTotal - this.memFree;
      return (100 * memUsed) / this.memTotal;
    }
    return 0;
  }

  notStartedComponents(): IHostComponent[] {
    return this.hostComponents.filter(
      (component) => component.workStatus !== ComponentStatus.STARTED
    );
  }
  componentsWithStaleConfigs(): IHostComponent[] {
    return this.hostComponents.filter((component) => component.staleConfigs);
  }
  componentsInPassiveState(): IHostComponent[] {
    return this.hostComponents.filter(
      (component) => component.passiveState !== "OFF"
    );
  }

  componentsInPassiveStateCount(): number {
    return this.componentsInPassiveState().length;
  }

  componentsWithStaleConfigsCount(): number {
    return this.componentsWithStaleConfigs().length;
  }

  disksMounted(): number {
    return this.diskInfo.length;
  }

  coresFormatted(): string {
    return `${this.cpu} (${this.cpuPhysical})`;
  }

  diskUsed(): number {
    return this.diskTotal - this.diskFree;
  }

  diskUsedFormatted(): string {
    return `${Math.round(this.diskUsed() * 100) / 100}GB`;
  }

  diskTotalFormatted(): string {
    return `${Math.round(this.diskTotal * 100) / 100}GB`;
  }

  diskUsage(): number {
    if (this.diskTotal === 0) {
      return 0;
    }
    return (this.diskUsed() / this.diskTotal) * 100;
  }

  diskUsageFormatted(): string {
    if (isNaN(this.diskUsage()) || this.diskUsage() < 0) {
      return "Data Unavailable";
    }
    return `${Math.round(this.diskUsage() * 100) / 100}%`;
  }

  diskInfoBar(): string {
    if (isNaN(this.diskUsage()) || this.diskUsage() < 0) {
      return this.diskUsageFormatted();
    }
    return `${this.diskUsedFormatted()}/${this.diskTotalFormatted()} (${this.diskUsageFormatted()} Used)`;
  }

  memoryFormatted() {
    return misc.formatBandwidth(Number(this.memory) * 1024);
  }

  isNotHeartBeating(): boolean {
    return this.state === "HEARTBEAT_LOST";
  }

  loadAvg(): number | null {
    if (this.loadOne != null) return parseFloat(this.loadOne.toFixed(2));
    if (this.loadFive != null) return parseFloat(this.loadFive.toFixed(2));
    if (this.loadFifteen != null)
      return parseFloat(this.loadFifteen.toFixed(2));
    return null;
  }

  updateObject(updates: Partial<IHost>) {
    Object.assign(this, updates);
  }

  getObject(): IHost {
    return this;
  }

  static FIXTURES: IHost[] = [];
}

export default Host;
