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
  state?: string;
  isActiveMaster?: string;
};

type HBaseServiceData = Partial<ServiceData> & {
  activeHbaseMasters?: HostComponent[];
  standbyHbaseMasters?: HostComponent[];
  nonActiveStandbyHbaseMasters?: HostComponent[];
  serviceState?: string;
  isRestartRequiredForService?: boolean;
  isInPassiveForService?: boolean;
};

class HBaseService extends Service {
  activeHbaseMasters: HostComponent[];
  standbyHbaseMasters: HostComponent[];
  nonActiveStandbyHbaseMasters: HostComponent[];
  serviceState: string;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: HBaseServiceData) {
    super(data as ServiceData);
    this.activeHbaseMasters = data.activeHbaseMasters || [];
    this.standbyHbaseMasters = data.standbyHbaseMasters || [];
    this.nonActiveStandbyHbaseMasters = data.nonActiveStandbyHbaseMasters || [];
    this.serviceState = data.serviceState || "";
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<HBaseService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): HBaseService {
    return this;
  }
}

export default HBaseService;
