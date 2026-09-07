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

type MapReduce2ServiceData = {
  jobHistoryServer: HostComponent | null;
  masterComponents: [];
  clientComponents: [];
  mapReduce2Clients: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class MapReduce2Service extends Service {
  jobHistoryServer: HostComponent | null;
  masterComponents: [];
  clientComponents: [];
  mapReduce2Clients: number;
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: MapReduce2ServiceData) {
    super(data as any);
    this.jobHistoryServer = data.jobHistoryServer || null;
    this.masterComponents = data.masterComponents || [];
    this.clientComponents = data.clientComponents || [];
    this.mapReduce2Clients = data.mapReduce2Clients || 0;
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<MapReduce2Service>) {
    Object.assign(this, updates);
  }

  getServiceObject(): MapReduce2Service {
    return this;
  }
}

export default MapReduce2Service;
