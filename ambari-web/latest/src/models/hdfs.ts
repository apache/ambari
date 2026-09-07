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

import Service, { type ServiceData } from "./service";

type HostComponent = {
  componentName: string;
  hostName: string;
  haNameSpace?: string;
  haStatus?: string;
  state?: string;
};

type NamespaceTopology = {
  name: string;
  title: string;
  hosts: string[];
  components: string[];
  clusterId: string;
};

type HDFSServiceData = Partial<ServiceData> & {
  nameNode?: HostComponent | null;
  snameNode?: HostComponent | null;
  activeNameNodes?: HostComponent[];
  standbyNameNodes?: HostComponent[];
  nonActiveStandbyNamenodes?: HostComponent[];
  datanodes?: HostComponent[];
  journalNodes?: HostComponent[];
  zookeeperFailoverControllers?: HostComponent[];
  isNameNodeHaEnabled?: boolean;
  namespaces?: unknown[];
  isNamespaceLoaded?: boolean;
  federationNamespaces?: NamespaceTopology[];
  serviceState?: string;
  isRestartRequiredForService?: boolean;
  isInPassiveForService?: boolean;
};

class HDFSService extends Service {
  nameNode: HostComponent | null;
  snameNode: HostComponent | null;
  activeNameNodes: HostComponent[];
  standbyNameNodes: HostComponent[];
  nonActiveStandbyNamenodes: HostComponent[];
  datanodes: HostComponent[];
  journalNodes: HostComponent[];
  zookeeperFailoverControllers: HostComponent[];
  isNameNodeHaEnabled: boolean;
  namespaces: unknown[];
  isNamespaceLoaded: boolean;
  federationNamespaces: NamespaceTopology[];
  serviceState: string;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: HDFSServiceData) {
    super(data as ServiceData);
    this.nameNode = data.nameNode || null;
    this.snameNode = data.snameNode || null;
    this.activeNameNodes = data.activeNameNodes || [];
    this.standbyNameNodes = data.standbyNameNodes || [];
    this.nonActiveStandbyNamenodes = data.nonActiveStandbyNamenodes || [];
    this.datanodes = data.datanodes || [];
    this.journalNodes = data.journalNodes || [];
    this.zookeeperFailoverControllers = data.zookeeperFailoverControllers || [];
    this.isNameNodeHaEnabled = data.isNameNodeHaEnabled || false;
    this.namespaces = data.namespaces || [];
    this.isNamespaceLoaded = data.isNamespaceLoaded || false;
    this.federationNamespaces = data.federationNamespaces || [];
    this.serviceState = data.serviceState || "";
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  findHealthStatusMapValueForSingleHost(hostState: string): string {
    const healthStatusMap: Record<string, string> = {
      STARTED: "green",
      STARTING: "green-blinking",
      INSTALLED: "red",
      STOPPING: "red-blinking",
      UNKNOWN: "yellow",
    };
    return healthStatusMap[hostState] || "yellow";
  }

  updateConfig(updates: Partial<HDFSService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): HDFSService {
    return this;
  }
}

export default HDFSService;
