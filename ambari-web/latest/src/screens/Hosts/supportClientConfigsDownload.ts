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

export enum ResourceTypeEnum {
  CLUSTER = "ClusterResource",
  HOST = "HostResource",
  SERVICE = "ServiceResource",
  SERVICE_COMPONENT = "ServiceComponentResource",
  HOST_COMPONENT = "HostComponentResource",
}

export const downloadClientConfigsCall = (data: any) => {
  const url = getUrl(
    get(data, "clusterName"),
    get(data, "hostName", ""),
    get(data, "serviceName", ""),
    get(data, "componentName", ""),
    get(data, "resourceType")
  );
  const newWindow = window.open(url);
  if (newWindow) {
    newWindow.focus();
  }
};

const getUrl = (
  clusterName: string,
  hostName: string,
  serviceName: string,
  componentName: string,
  resourceType: ResourceTypeEnum
) => {
  const apiPrefix = "/api/v1";
  let result = `${apiPrefix}/clusters/${clusterName}/`;

  switch (resourceType) {
    case ResourceTypeEnum.SERVICE_COMPONENT:
      result += `services/${serviceName}/components/${componentName}`;
      break;
    case ResourceTypeEnum.HOST_COMPONENT:
      result += `hosts/${hostName}/host_components/${componentName}`;
      break;
    case ResourceTypeEnum.HOST:
      result += `hosts/${hostName}/host_components`;
      break;
    case ResourceTypeEnum.SERVICE:
      result += `services/${serviceName}/components`;
      break;
    case ResourceTypeEnum.CLUSTER:
    default:
      result += "components";
  }

  result += "?format=client_config_tar";
  return result;
};
