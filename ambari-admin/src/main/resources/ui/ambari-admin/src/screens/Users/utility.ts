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
import { get, uniqBy } from "lodash";
import { PrivilegeType } from "./enums";

export const sortPrivileges = (privileges: any[]) => {
  const orderedRoles = [
    "AMBARI.ADMINISTRATOR",
    "CLUSTER.ADMINISTRATOR",
    "CLUSTER.OPERATOR",
    "SERVICE.ADMINISTRATOR",
    "SERVICE.OPERATOR",
    "CLUSTER.USER",
  ];
  return privileges.sort((a, b) => {
    const indexA = orderedRoles.indexOf(
      get(a, "PrivilegeInfo.permission_name")
    );
    const indexB = orderedRoles.indexOf(
      get(b, "PrivilegeInfo.permission_name")
    );
    return indexA - indexB;
  });
};

export const getHighestUserRole = (privileges: any[]) => {
  if (!privileges || privileges.length === 0) {
    return "";
  }
  const sortedPrivileges = sortPrivileges(privileges);
  return get(sortedPrivileges, "[0].PrivilegeInfo.permission_label", "");
};

export const getClusterPrivileges = (info: any) => {
  const sortedPrivileges = sortPrivileges(
    get(info, "privileges", []).filter((privilege: any) => {
      return get(privilege, "PrivilegeInfo.type") === PrivilegeType.CLUSTER;
    })
  );
  return get(sortedPrivileges, "[0]", "")
    ? [get(sortedPrivileges, "[0]", "")]
    : [];
};

export const getViewPrivileges = (info: any) => {
  const viewPrivileges = get(info, "privileges", []).filter(
    (privilege: any) =>
      get(privilege, "PrivilegeInfo.type") === PrivilegeType.VIEW
  );
  return uniqBy(viewPrivileges, (privilege: any) =>
    get(privilege, "PrivilegeInfo.instance_name")
  );
};
