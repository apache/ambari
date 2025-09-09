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

interface IHostInfo {
  elementId: string;
  name: string;
  cpu: number | null;
  memory: number | null;
  message: string;
  barColor: string;
  isChecked: boolean;
  bootLog: string | null;
  bootStatus: string;
  bootStatusForDisplay(): string;
  bootBarColor(): string;
  bootStatusColor(): string;
  isBootDone(): boolean;
  updateObject(updates: Partial<IHostInfo>): void;
  getObject(): IHostInfo;
}

class HostInfo implements IHostInfo {
  elementId: string;
  name: string;
  cpu: number | null;
  memory: number | null;
  message: string;
  barColor: string;
  isChecked: boolean;
  bootLog: string | null;
  bootStatus: string;

  constructor(props: IHostInfo) {
    this.elementId = get(props, "elementId", "host");
    this.name = get(props, "name", "");
    this.cpu = get(props, "cpu", null);
    this.memory = get(props, "memory", null);
    this.message = get(props, "message", "Information");
    this.barColor = get(props, "barColor", "progress-bar-info");
    this.isChecked = get(props, "isChecked", false);
    this.bootLog = get(props, "bootLog", null);
    this.bootStatus = get(props, "bootStatus", "PENDING");
  }

  bootStatusForDisplay(): string {
    return HostInfo.bootStatusForDisplayMap[this.bootStatus] || "Registering";
  }

  bootBarColor(): string {
    return HostInfo.bootBarColorMap[this.bootStatus] || "progress-bar-info";
  }

  bootStatusColor(): string {
    return HostInfo.bootStatusColorMap[this.bootStatus] || "text-info";
  }

  isBootDone(): boolean {
    return ["REGISTERED", "FAILED"].includes(this.bootStatus);
  }

  updateObject(updates: Partial<IHostInfo>) {
    Object.assign(this, updates);
  }

  getObject(): IHostInfo {
    return this;
  }

  static bootStatusForDisplayMap: { [key: string]: string } = {
    PENDING: "Preparing",
    REGISTERED: "Success",
    FAILED: "Failed",
    RUNNING: "Installing",
    DONE: "Registering",
    REGISTERING: "Registering",
  };

  static bootBarColorMap: { [key: string]: string } = {
    REGISTERED: "progress-bar-success",
    FAILED: "progress-bar-danger",
    PENDING: "progress-bar-info",
    RUNNING: "progress-bar-info",
    DONE: "progress-bar-info",
    REGISTERING: "progress-bar-info",
  };

  static bootStatusColorMap: { [key: string]: string } = {
    REGISTERED: "text-success",
    FAILED: "text-danger",
    PENDING: "text-info",
    RUNNING: "text-info",
    DONE: "text-info",
    REGISTERING: "text-info",
  };
}

export default HostInfo;
