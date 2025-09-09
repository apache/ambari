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
import { IHostComponent } from "./hostComponent";

export interface IHostComponentLog {
  name: string;
  hostName: string;
  serviceName: string;
  hostComponent: IHostComponent;
  logFileNames: string[];
  updateObject(updates: Partial<IHostComponentLog>): void;
  getObject(): IHostComponentLog;
}

class HostComponentLog implements IHostComponentLog {
  name: string;
  hostName: string;
  serviceName: string;
  hostComponent: IHostComponent;
  logFileNames: string[];

  constructor(props: IHostComponentLog) {
    this.name = get(props, "name", "");
    this.hostName = get(props, "hostName", "");
    this.serviceName = get(props, "serviceName", "");
    this.hostComponent = get(props, "hostComponent", {} as IHostComponent);
    this.logFileNames = get(props, "logFileNames", []);
  }

  updateObject(updates: Partial<IHostComponentLog>) {
    Object.assign(this, updates);
  }

  getObject(): IHostComponentLog {
    return this;
  }

  static FIXTURES: IHostComponentLog[] = [];
}

export default HostComponentLog;
