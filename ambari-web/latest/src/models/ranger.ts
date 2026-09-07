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
};

type RangerServiceData = Partial<ServiceData> & {
  rangerAdmins?: HostComponent[];
  rangerHDFSPluginProperties?: string;
  rangerHbasePluginProperties?: string;
  rangerHivePluginProperties?: string;
  rangerYarnPluginProperties?: string;
  serviceState?: string;
  isRestartRequiredForService?: boolean;
  isInPassiveForService?: boolean;
};

class RangerService extends Service {
  rangerAdmins: HostComponent[];
  rangerHDFSPluginProperties: string;
  rangerHbasePluginProperties: string;
  rangerHivePluginProperties: string;
  rangerYarnPluginProperties: string;
  serviceState: string;
  isRestartRequiredForService: boolean;
  isInPassiveForService: boolean;

  constructor(data: RangerServiceData) {
    super(data as ServiceData);
    this.rangerAdmins = data.rangerAdmins || [];
    this.rangerHDFSPluginProperties = data.rangerHDFSPluginProperties || "";
    this.rangerHbasePluginProperties = data.rangerHbasePluginProperties || "";
    this.rangerHivePluginProperties = data.rangerHivePluginProperties || "";
    this.rangerYarnPluginProperties = data.rangerYarnPluginProperties || "";
    this.serviceState = data.serviceState || "";
    this.isRestartRequiredForService = data.isRestartRequiredForService || false;
    this.isInPassiveForService = data.isInPassiveForService || false;
  }

  updateConfig(updates: Partial<RangerService>) {
    Object.assign(this, updates);
  }

  getServiceObject(): RangerService {
    return this;
  }
}

export default RangerService;
