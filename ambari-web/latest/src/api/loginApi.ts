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

import {ambariApi, supressErrorAmbariApi} from "./config/axiosConfig";
import {misc} from "../Utils/misc";
import Cookies from 'js-cookie';

interface LoginParams {
  usr: string;
  loginName: string;
}
export interface LoginMessage {
  text: string;
  buttonText: string;
}

const LoginApi = {

  authenticate: async function (username: string, password: string) {
    const hashForUserNamePassword = misc.utf8ToB64(username + ":" + password);
    const response = await ambariApi.request({
      url: "/auth",
      method: "POST",
      headers: {
        'Content-Type': 'text/plain',
        'X-Requested-By': 'X-Requested-By',
        Authorization: "Basic " + hashForUserNamePassword,
      },
      skipAuthRedirect: true,
    });
    return response;
  },
  handleSuccessfulLogin: async function (params: LoginParams) {
    const url = `/users/${encodeURIComponent(params.loginName)}?fields=*,privileges/PrivilegeInfo/*`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
      data: {
        usr: params.usr,
        loginName: params.loginName
      }
    });
    return response;
  },
  loadAuthorizationsCallback: async function(params: LoginParams) {
    const url = `/users/${encodeURIComponent(params.loginName)}/authorizations?fields=*`
    const response = await ambariApi.request({
      url: url,
      method: "GET",
      data: {userName: params.loginName},
    });
    return response;
  },
  loadPrivileges: async function(loginName: string) {
    return ambariApi.request({
      url: `/users/${encodeURIComponent(loginName)}/privileges?fields=*`,
      method: "GET",
    });
  },
  probeSession: async function() {
    return ambariApi.request({
      url: "/clusters?fields=Clusters/provisioning_state,Clusters/security_type,Clusters/version,Clusters/cluster_id",
      method: "GET",
    });
  },
  loadLoginMessage: async function(): Promise<LoginMessage | null> {
    try {
      const response = await supressErrorAmbariApi.request({
        url: "/settings/motd",
        method: "GET",
      });
      const content = response.data?.Settings?.content;
      if (typeof content !== "string") {
        return null;
      }
      const parsed = JSON.parse(content.replace(/\n/g, "\\n"));
      if (parsed?.status !== "true" || !parsed?.text) {
        return null;
      }
      return {
        text: String(parsed.text).replace(/(\r\n|\n|\r)/g, "\n"),
        buttonText: parsed.button ? String(parsed.button) : "OK",
      };
    } catch {
      return null;
    }
  },
  logout: async () => {
    Cookies.remove('AMBARISESSIONID', { path: '/' });
    const timestamp = Date.now();
    try {
      const response = await ambariApi.request({
        url: `/logout?_=${timestamp}`,
        method: "GET",
        headers: {
          'X-Requested-By': 'X-Requested-By'
        },
        withCredentials: true,
        auth: {
          username: timestamp.toString(),
          password: timestamp.toString()
        }
      });

      return response;
    } catch (error) {
      console.error('Logout failed:', error);
      throw error;
    }
  },
  
}
export default LoginApi;
