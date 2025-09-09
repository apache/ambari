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

type ConfigGroupItemType = {
  ConfigGroup: {
    href?: string;
    cluster_name?: string;
    description?: string;
    desired_configs?: [];
    group_name: string;
    hosts?: { host_name?: string }[];
    id?: number;
    tag?: string;
  };
};

type DesiredConfigsItemType = {
  href?: string;
  tag?: string;
  type?: string;
  version?: number;
  Config?: Object;
  properties?: Object;
  data?: Object;
};

type ConfigGroupType = {
  href?: string;
  items: ConfigGroupItemType[];
};

type DesiredConfigsType = {
  href?: string;
  items: DesiredConfigsItemType[];
};

type HostInfoType = {
  cpu_count: number;
  disk_info: [];
  host_name: string;
  ip: string;
  os_arch: string;
  os_type: string;
  public_host_name: string;
  total_mem: number;
};

type HostType = {
  Hosts: HostInfoType;
  host_components?: [];
  isChecked?: boolean;
  isShown?: boolean;
};

type HostDataType = {
  items: HostType[];
};

export type {
  ConfigGroupItemType,
  DesiredConfigsItemType,
  ConfigGroupType,
  DesiredConfigsType,
  HostInfoType,
  HostType,
  HostDataType,
};
