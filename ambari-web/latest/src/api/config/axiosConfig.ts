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
import axios from "axios";
import { toast } from "react-hot-toast";
import { get } from "lodash";

const config = {
  development: {
    VITE_API_PROXY_TARGET: "<PROXY URL HERE>",
    //VITE_TOKEN: "<PROXY TOKEN HERE>",
  },
  production: {
    VITE_API_PROXY_TARGET: "",
  },
};

let currentEnv = "development"; // however you determine the current environment

if (process.env.NODE_ENV) {
  currentEnv = process.env.NODE_ENV;
}

const createAxiosInstance = (baseURL: string, headers = {}) => {
  if (currentEnv != undefined) {
    if (currentEnv == "development") {
      headers = {
        "Content-Type": "application/json",
        // Authorization: `Basic ${btoa(localStorage.getItem("proxy_token")||"")}`,
        ...headers,
      };
    } else {
      headers = {
        "Content-Type": "application/json",
        // Authorization: `Basic ${btoa(localStorage.getItem("proxy_token")||"")}`,
        ...headers,
      };
    }
  } else {
    console.error(`No configuration found for target: ${currentEnv}`);
  }

  const instance = axios.create({
    baseURL,
    withCredentials: true,
    headers: headers,
  });

  instance.interceptors.response.use(undefined, (error) => {
    const responseMessage = get(error, "response.data.message", undefined);
    // Check for 403 Forbidden status
    if (error.response && error.response.status === 403) {
      // Redirect to login page
      window.location.href = "/#/login";
      return Promise.reject(error);
    }
    if (responseMessage && error.response.status !== 400) {
      toast.error(responseMessage);
    }
    return Promise.reject(error);
  });

  return instance;
};

const createSupressErrorAxiosInstance = (baseURL: string, headers = {}) => {
  if (currentEnv != undefined) {
    headers = {
      "Content-Type": "application/json",
      ...headers,
    };
  }

  const instance = axios.create({
    baseURL,
    withCredentials: true,
    headers: headers,
  });

  instance.interceptors.response.use(undefined, (error) => {
    // Check for 403 Forbidden status
    if (error.response && error.response.status === 403) {
      // Redirect to login page
      window.location.href = "/#/login";
      return Promise.reject(error);
    }
    return Promise.reject(error);
  });

  return instance;
};

let endpoint = "";
if (config.development.VITE_API_PROXY_TARGET != undefined) {
  if (config.development.VITE_API_PROXY_TARGET != "") {
    endpoint = "/api/v1";
  } else {
    endpoint = `${config.production.VITE_API_PROXY_TARGET}/api/v1`;
  }
}

export const ambariApi = createAxiosInstance(endpoint);
export const supressErrorAmbariApi = createSupressErrorAxiosInstance(endpoint);
