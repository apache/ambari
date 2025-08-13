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
import {ambariApi} from "./config/axiosConfig.ts";
import {misc} from "../Utils/misc.ts";
import { db } from "../Utils/db";
import Cookies from 'js-cookie';

interface LoginParams {
  usr: string;
  loginName: string;
}
interface dataLoginType {
  Users: any;
  loginData: any;
}
interface LoginDataParamsType {
  loginName: string;
  loginData: any;
}
const LoginApi = {

  authenticate: async function (username: string, password: string) {
    const hashForUserNamePassword = misc.utf8ToB64(username + ":" + password);
    const response = await ambariApi.request({
      url: "/auth",
      method: "POST",
      headers: {
        'Content-Type': 'text/plain',
        Authorization: "Basic " + hashForUserNamePassword,
      },
    });
    return response;
  },
  handleSuccessfulLogin: async function (params: LoginParams) {
    const url = `/users/${encodeURIComponent(params.loginName)}?fields=*,privileges/PrivilegeInfo/cluster_name,privileges/PrivilegeInfo/permission_name`;
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
  afterLoginSuccessCallback: async function(data: dataLoginType) {
    const response = await ambariApi.request({
      url: "/settings/motd",
      method: "GET",
      data: {
        loginName: data.Users.user_name,
        loginData: data
      }
    });
    return response.data;
  },
  setClusterDataCallback: async function(params: LoginDataParamsType) {
    const requestData = {
      loginName: params.loginName,
      loginData: params.loginData
    };
    const response = await ambariApi.request({
      url: "/clusters?fields=Clusters/provisioning_state,Clusters/security_type,Clusters/version,Clusters/cluster_id",
      data: requestData,
    })
    return response;
  },
  logout: async () => {
    Cookies.remove('AMBARISESSIONID', { path: '/' ,domain: 'localhost', secure: true })    
    console.log('After logout cookies:', document.cookie);
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

      // Only clean up and redirect on successful logout
      if (response.status === 200) {
        db.cleanUp();
            // Remove AMBARI-SESSION-ID cookie
        // // Clear auth header
        // delete ambariApi.defaults.headers.common['Authorization'];
        // // Clear JWT cookie
        // document.cookie = "jwt=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        window.location.replace('/#/login');
      }
      return response;
    } catch (error) {
      console.error('Logout failed:', error);
      throw error; // Let the UI handle the error
    }
  },
  
}
export default LoginApi;

