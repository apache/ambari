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

import Service from "./service.ts";

type SqoopServiceData = {
  sqoopClientsStarted: number;
  sqoopClientsInstalled: number;
  sqoopClientsTotal: number;
  clientComponents: [];
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts?: boolean;
  isClientOnlyService: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;
};

class SqoopService extends Service {
  sqoopClientsStarted: number;
  sqoopClientsInstalled: number;
  sqoopClientsTotal: number;
  clientComponents: [];
  alertsCount: number;
  serviceState: string;
  hasCriticalAlerts: boolean;
  isClientOnlyService: boolean;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: SqoopServiceData) {
    super(data as any);
    this.sqoopClientsStarted = data.sqoopClientsStarted || 0;
    this.sqoopClientsInstalled = data.sqoopClientsInstalled || 0;
    this.sqoopClientsTotal = data.sqoopClientsTotal || 0;
    this.clientComponents = data.clientComponents || [];
    this.alertsCount = data.alertsCount || 0;
    this.serviceState = data.serviceState || "";
    this.hasCriticalAlerts = data.hasCriticalAlerts || false;
    this.isClientOnlyService = true;
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<SqoopService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): SqoopService {
    return this;
  }
}

export default SqoopService;
