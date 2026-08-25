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

export interface RangerAdminHaOperation {
  [key: string]: unknown;
  id: string;
  label: string;
  skippable: false;
  callback: () => Promise<unknown>;
  status?: string;
  requestId?: string | number;
  progress?: number;
  error?: string;
}

export interface RangerAdminHaOperationCallbacks {
  stopAllServices: () => Promise<unknown>;
  installAdditionalRangerAdmins: () => Promise<unknown>;
  reconfigureServices: () => Promise<unknown>;
  startAllServices: () => Promise<unknown>;
}

export function createRangerAdminHaOperations(
  callbacks: RangerAdminHaOperationCallbacks,
): RangerAdminHaOperation[] {
  return [
    {
      id: "stopAllServices",
      label: "Stop All Services",
      skippable: false,
      callback: callbacks.stopAllServices,
    },
    {
      id: "installRangerAdmins",
      label: "Install Additional Ranger Admin",
      skippable: false,
      callback: callbacks.installAdditionalRangerAdmins,
    },
    {
      id: "reconfigureServices",
      label: "Reconfigure Services",
      skippable: false,
      callback: callbacks.reconfigureServices,
    },
    {
      id: "startAllServices",
      label: "Start All Services",
      skippable: false,
      callback: callbacks.startAllServices,
    },
  ];
}
