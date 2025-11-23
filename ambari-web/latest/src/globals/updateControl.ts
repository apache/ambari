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

import { generateQueryParam } from "../screens/Hosts/comboSearchBox";

export const computeParameters = (queryParams: any) => {
  let params = "";

  queryParams.forEach((param: any) => {
    let customKey = param.key;

    switch (param.type) {
      case "EQUAL":
        if (Array.isArray(param.value)) {
          if (param.value.length > 1) {
            params += `${param.key}.in(${param.value.join(",")})`;
          } else {
            params += `${param.key}=${param.value[0]}`;
          }
        } else {
          params += `${param.key}=${param.value}`;
        }
        break;
      case "LESS":
        params += `${param.key}<${param.value}`;
        break;
      case "MORE":
        params += `${param.key}>${param.value}`;
        break;
      case "MATCH":
        if (Array.isArray(param.value)) {
          params += `(${param.value
            .map((v: any) => `${param.key}.matches(.*${v}.*)`)
            .join("|")})`;
        } else {
          params += `${param.key}.matches(${param.value})`;
        }
        break;
      case "MULTIPLE":
        params += `${param.key}.in(${param.value.join(",")})`;
        break;
      case "SORT":
        params += `sortBy=${param.key}.${param.value}`;
        break;
      case "CUSTOM":
        param.value.forEach((item: any, index: any) => {
          customKey = customKey.replace(`{${index}}`, item);
        });
        params += customKey;
        break;
      case "COMBO":
        params += generateQueryParam(param);
        break;
      default:
        break;
    }
    params += "&";
  });
  return params.substring(0, params.length - 1);
};
