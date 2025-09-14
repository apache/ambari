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

export const isHostname = (hostname: string): boolean => {
  const regex = new RegExp(
    /(?=^.{3,254}$)(^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])(\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9]))*(\.[a-zA-Z]{1,62})$)/
  );
  return hostname === "localhost" || regex.test(hostname);
};

export const isValidUserName = (username: string): boolean => {
  const regex = new RegExp(/^[a-z]([-a-z0-9]{0,30})$/);
  return regex.test(username);
};

export const getDependentConfigChanges = (
  changedConfig: any,
  selectedServices: string[],
  allConfigs: any
) => {
  const propertyName = get(changedConfig, "propertyName");
  const propertyValue = get(changedConfig, "value");
  let allAffectedProperties: any[] = [];
  if (propertyName === "hdfs_user") {
    const affectedPropertyNames = [
      // "dfs.permissions.superusergroup",
      "dfs.cluster.administrators",
    ];
    allConfigs.forEach((config: any) => {
      if (
        get(config, "serviceName") === "HDFS" &&
        affectedPropertyNames.includes(get(config, "propertyName")) &&
        propertyValue.trim() !== get(config, "value", "").trim()
      ) {
        allAffectedProperties.push({ ...config, new_value: propertyValue });
      }
    });
  } else if (propertyName === "yarn_user") {
    const affectedPropertyNames = ["yarn.admin.acl"];
    allConfigs.forEach((config: any) => {
      if (
        get(config, "serviceName") === "YARN" &&
        affectedPropertyNames.includes(get(config, "propertyName")) &&
        propertyValue.trim() !== get(config, "value").trim()
      ) {
        allAffectedProperties.push({ ...config, new_value: propertyValue });
      }
    });
  } else if (propertyName === "user_group") {
    if (!selectedServices.includes("YARN")) {
      return;
    }
    if (selectedServices.includes("MAPREDUCE2")) {
      const affectedPropertyNames = ["mapreduce.cluster.administrators"];
      allConfigs.forEach((config: any) => {
        if (
          get(config, "serviceName") === "MAPREDUCE2" &&
          affectedPropertyNames.includes(get(config, "propertyName")) &&
          propertyValue.trim() !== get(config, "value").trim()
        ) {
          allAffectedProperties.push({ ...config, new_value: propertyValue });
        }
      });
    }
    if (selectedServices.includes("YARN")) {
      const affectedPropertyNames = [
        "yarn.nodemanager.linux-container-executor.group",
      ];
      allConfigs.forEach((config: any) => {
        if (
          get(config, "serviceName") === "YARN" &&
          affectedPropertyNames.includes(get(config, "propertyName")) &&
          propertyValue.trim() !== get(config, "value").trim()
        ) {
          allAffectedProperties.push({ ...config, new_value: propertyValue });
        }
      });
    }
  }

  return allAffectedProperties;
};
