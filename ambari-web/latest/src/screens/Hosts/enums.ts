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

export enum HostStatus {
  HEALTHY = "HEALTHY",
  UNHEALTHY = "UNHEALTHY",
  UNKNOWN = "UNKNOWN",
  ALERT = "ALERT",
}

export enum ComponentType {
  MASTER = "MASTER",
  SLAVE = "SLAVE",
  CLIENT = "CLIENT",
}

export enum ComponentStatus {
  STARTED = "STARTED",
  STARTING = "STARTING",
  STOPPED = "INSTALLED",
  STOPPING = "STOPPING",
  INSTALL_FAILED = "INSTALL_FAILED",
  INSTALLING = "INSTALLING",
  UPGRADE_FAILED = "UPGRADE_FAILED",
  UNKNOWN = "UNKNOWN",
  DISABLED = "DISABLED",
  INIT = "INIT",
}

export enum PassiveStateOnFilters {
  ON = "ON",
  IMPLIED_FROM_HOST = "IMPLIED_FROM_HOST",
  IMPLIED_FROM_SERVICE = "IMPLIED_FROM_SERVICE",
  IMPLIED_FROM_SERVICE_AND_HOST = "IMPLIED_FROM_SERVICE_AND_HOST",
}
