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
import { adminApi } from "./configs/axiosConfig";
import { GroupDataType, MembersDataType, UserDataType } from "./types";

const UserGroupApi = {

  // Permission APIs

  getPermissions: async function (fields: string) {
    const url = `permissions?PermissionInfo/resource_name.in(CLUSTER,AMBARI)&fields=${fields}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  // User APIs

  userNames: async function () {
    const url = `/users?Users/user_name.matches(.*.*)`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  usersList: async function (fields: string) {
    const url = `/users?fields=${fields}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  userData: async function (
    userName: string,
    fields: string,
  ) {
    const url = `/users/${userName}?fields=${fields}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  addUser: async function (userData: UserDataType) {
    const url = '/users';
    const response = await adminApi.request({
      url: url,
      method: "POST",
      data: userData
    });
    return response.data;
  },
  updateUser: async function (userName: string, userData: UserDataType) {
    const url = `/users/${userName}`;
    const response = await adminApi.request({
      url: url,
      method: "PUT",
      data: userData
    });
    return response.data;
  },
  removeUser: async function (userName: string) {
    const url = `/users/${userName}`;
    const response = await adminApi.request({
      url: url,
      method: "DELETE",
    });
    return response.data;
  },

  // Group APIs

  groupNames: async function () {
    const url = `/groups?Groups/group_name.matches(.*.*)`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  groupsList: async function (fields: string) {
    const url = `/groups?fields=${fields}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  groupData: async function (
    groupName: string,
    fields: string,
  ) {
    const url = `/groups/${groupName}?fields=${fields}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  addGroup: async function (groupData: GroupDataType[]) {
    const url = '/groups';
    const response = await adminApi.request({
      url: url,
      method: "POST",
      data: groupData
    });
    return response.data;
  },
  addMember: async function (groupName: string, userName: string) {
    const url = `/groups/${groupName}/members/${userName}`;
    const response = await adminApi.request({
      url: url,
      method: "POST",
    });
    return response.data;
  },
  removeMember: async function (groupName: string, userName: string) {
    const url = `/groups/${groupName}/members/${userName}`;
    const response = await adminApi.request({
      url: url,
      method: "DELETE",
    });
    return response.data;
  },
  updateMembers: async function (groupName: string, membersData: MembersDataType[]) {
    const url = `/groups/${groupName}/members`;
    const response = await adminApi.request({
      url: url,
      method: "PUT",
      data: membersData
    });
    return response.data;
  },
  removeGroup: async function (groupName: string) {
    const url = `/groups/${groupName}`;
    const response = await adminApi.request({
      url: url,
      method: "DELETE",
    });
    return response.data;
  }
};

export default UserGroupApi;
