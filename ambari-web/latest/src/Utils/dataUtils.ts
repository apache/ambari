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

import {  get, has } from "lodash";

export const groupPropertyValues = (collection: any[], key: string) => {
  const group: any = {};
  for (let item of collection) {
    const value: any = get(item, key, "");
    if (!has(group, value)) {
      //@ts-ignore  
      group[value] = [item];
      // set(group, value, [item]);
    } else {
        group[value] = [...group[value], item];
    }
  }
  return group;
};
