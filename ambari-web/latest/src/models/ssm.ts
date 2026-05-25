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

type HostComponent = {
  componentName: string;
  hostName: string;
};

type SSMServiceData = {
  smartAgentsStartedCount: number;
  smartAgentsInstalledCount: number;
  smartAgentsTotalCount: number;
  masterComponents: [];
  slaveComponents: [];
  smartServers: HostComponent[];
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts?: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class SSMService extends Service {
  smartAgentsStartedCount: number;
  smartAgentsInstalledCount: number;
  smartAgentsTotalCount: number;
  masterComponents: [];
  slaveComponents: [];
  smartServers: HostComponent[];
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: SSMServiceData) {
    //@ts-ignore
    super(data);
    this.smartAgentsStartedCount = data.smartAgentsStartedCount || 0;
    this.smartAgentsInstalledCount = data.smartAgentsInstalledCount || 0;
    this.smartAgentsTotalCount = data.smartAgentsTotalCount || 0;
    this.masterComponents = data.masterComponents || [];
    this.slaveComponents = data.slaveComponents || [];
    this.smartServers = data.smartServers || null;
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<SSMService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): SSMService {
    return this;
  }
}

export default SSMService;
