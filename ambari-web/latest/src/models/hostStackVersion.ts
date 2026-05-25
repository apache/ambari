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
import { IHost } from "./host";
import { upperUnderscoreToText } from "../Utils/stringUtils";
import { messages } from "../screens/messages";

export interface IHostStackVersion {
  stack: string;
  version: string;
  repo: any;
  repoVersion: string;
  displayName: string;
  isVisible: boolean;
  status: string;
  host: IHost;
  hostName: string;
  isCurrent(): boolean;
  isInstalling(): boolean;
  isOutOfSync(): boolean;
  displayStatus(): string;
  installEnabled(): boolean;
  installDisabled(): boolean;
  updateObject(updates: Partial<IHostStackVersion>): void;
  getObject(): IHostStackVersion;
}

class HostStackVersion implements IHostStackVersion {
  stack: string;
  version: string;
  repo: any;
  repoVersion: string;
  displayName: string;
  isVisible: boolean;
  status: string;
  host: IHost;
  hostName: string;

  constructor(props: IHostStackVersion) {
    this.stack = get(props, "stack", "");
    this.version = get(props, "version", "");
    this.repo = get(props, "repo", {});
    this.repoVersion = get(props, "repoVersion", "");
    this.displayName = get(props, "displayName", "");
    this.isVisible = get(props, "isVisible", true);
    this.status = get(props, "status", "");
    this.host = get(props, "host", {} as IHost);
    this.hostName = get(props, "hostName", "");
  }

  isCurrent(): boolean {
    return this.status === "CURRENT";
  }

  isInstalling(): boolean {
    return this.status === "INSTALLING";
  }

  isOutOfSync(): boolean {
    return this.status === "OUT_OF_SYNC";
  }

  displayStatus(): string {
    return HostStackVersion.formatStatus(this.status);
  }

  installEnabled(): boolean {
    return ["OUT_OF_SYNC", "INSTALL_FAILED"].includes(this.status);
  }

  installDisabled(): boolean {
    return !this.installEnabled;
  }

  updateObject(updates: Partial<IHostStackVersion>) {
    Object.assign(this, updates);
  }

  getObject(): IHostStackVersion {
    return this;
  }

  static formatStatus(status: string): string {
    return HostStackVersion.statusDefinition.includes(status)
      ? get(messages, `hosts.host.stackVersions.status.${status.toLowerCase()}`)
      : upperUnderscoreToText(status);
  }

  static statusDefinition = [
    "INSTALLED",
    "INSTALLING",
    "INSTALL_FAILED",
    "OUT_OF_SYNC",
    "CURRENT",
    "UPGRADING",
    "UPGRADE_FAILED",
  ];

  static FIXTURES: IHostStackVersion[] = [];
}

export default HostStackVersion;
