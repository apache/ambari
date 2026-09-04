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
  //haNameSpace?: string;
};

type Spark3ServiceData = {
  spark3JobHistoryServers: HostComponent[] | [];
  masterComponents: [];
  slaveComponents: [];
  clientComponents: [];
  spark3Clients: number;
  livyForSpark3ServerInstalledCount: number;
  livyForSpark3ServerStartedCount: number;
  livyForSpark3ServerTotalCount: number;
  spark3ThriftServerInstalledCount: number;
  spark3ThriftServerStartedCount: number;
  spark3ThriftServerTotalCount: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts?: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class Spark3Service extends Service {
  spark3JobHistoryServers: HostComponent[] | [];
  masterComponents: [];
  slaveComponents: [];
  clientComponents: [];
  spark3Clients: number;
  livyForSpark3ServerInstalledCount: number;
  livyForSpark3ServerStartedCount: number;
  livyForSpark3ServerTotalCount: number;
  spark3ThriftServerInstalledCount: number;
  spark3ThriftServerStartedCount: number;
  spark3ThriftServerTotalCount: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: Spark3ServiceData) {
    super(data as any);
    this.spark3JobHistoryServers = data.spark3JobHistoryServers || [];
    this.masterComponents = data.masterComponents || [];
    this.slaveComponents = data.slaveComponents || [];
    this.clientComponents = data.clientComponents || [];
    this.spark3Clients = data.spark3Clients || 0;
    this.livyForSpark3ServerInstalledCount =
      data.livyForSpark3ServerInstalledCount || 0;
    this.livyForSpark3ServerStartedCount =
      data.livyForSpark3ServerStartedCount || 0;
    this.livyForSpark3ServerTotalCount =
      data.livyForSpark3ServerTotalCount || 0;
    this.spark3ThriftServerInstalledCount =
      data.spark3ThriftServerInstalledCount || 0;
    this.spark3ThriftServerStartedCount =
      data.spark3ThriftServerStartedCount || 0;
    this.spark3ThriftServerTotalCount = data.spark3ThriftServerTotalCount || 0;
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<Spark3Service>) {
    Object.assign(this, updates);
  }

  getServiceObject(): Spark3Service {
    return this;
  }
}

export default Spark3Service;
