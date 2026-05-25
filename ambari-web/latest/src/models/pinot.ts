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
  state?: string;
  passiveState?: string;
};

type PinotServiceData = {
  pinotController: HostComponent[] | [];
  pinotBroker: HostComponent[] | [];
  pinotMinion: HostComponent[] | [];
  pinotServer: HostComponent[] | [];
  masterComponents: [];
  slaveComponents: [];
  pinotBrokerStartedCount: number;
  pinotBrokerInstalledCount: number;
  pinotBrokerTotalCount: number;
  pinotMinionStartedCount: number;
  pinotMinionInstalledCount: number;
  pinotMinionTotalCount: number;
  pinotServerStartedCount: number;
  pinotServerInstalledCount: number;
  pinotServerTotalCount: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts?: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class PinotService extends Service {
  pinotController: HostComponent[] | [];
  pinotBroker: HostComponent[] | [];
  pinotMinion: HostComponent[] | [];
  pinotServer: HostComponent[] | [];
  masterComponents: [];
  slaveComponents: [];
  pinotBrokerStartedCount: number;
  pinotBrokerInstalledCount: number;
  pinotBrokerTotalCount: number;
  pinotMinionStartedCount: number;
  pinotMinionInstalledCount: number;
  pinotMinionTotalCount: number;
  pinotServerStartedCount: number;
  pinotServerInstalledCount: number;
  pinotServerTotalCount: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: PinotServiceData) {
    super(data as any);
    this.pinotController = data.pinotController || [];
    this.pinotBroker = data.pinotBroker || [];
    this.pinotMinion = data.pinotMinion || [];
    this.pinotServer = data.pinotServer || [];
    this.masterComponents = data.masterComponents || [];
    this.slaveComponents = data.slaveComponents || [];
    this.pinotBrokerStartedCount = data.pinotBrokerStartedCount || 0;
    this.pinotBrokerInstalledCount = data.pinotBrokerInstalledCount || 0;
    this.pinotBrokerTotalCount = data.pinotBrokerTotalCount || 0;
    this.pinotMinionStartedCount = data.pinotMinionStartedCount || 0;
    this.pinotMinionInstalledCount = data.pinotMinionInstalledCount || 0;
    this.pinotMinionTotalCount = data.pinotMinionTotalCount || 0;
    this.pinotServerStartedCount = data.pinotServerStartedCount || 0;
    this.pinotServerInstalledCount = data.pinotServerInstalledCount || 0;
    this.pinotServerTotalCount = data.pinotServerTotalCount || 0;
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<PinotService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): PinotService {
    return this;
  }
}

export default PinotService;
