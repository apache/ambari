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

import { Masters } from "./types/AssignMastersTypes";

export type MasterValidationMessage = {
  errorMessage?: string;
  warnMessage?: string;
};

export type MasterValidationMessages = Record<string, MasterValidationMessage>;

type MasterValidationItem = {
  type?: string;
  level?: string;
  host?: string;
  "component-name"?: string;
  message?: string;
};

type MasterValidationResponse = {
  resources?: Array<{
    items?: MasterValidationItem[];
  }>;
};

export const masterValidationKey = (hostName: string, component: string) =>
  `${hostName}\u0000${component}`;

export function getMasterValidationMessages(
  response: MasterValidationResponse,
  masters: Masters[],
): MasterValidationMessages {
  const messages: MasterValidationMessages = {};
  const items = response?.resources?.[0]?.items;

  if (!Array.isArray(items)) {
    return messages;
  }

  items
    .filter(
      (item) =>
        item.type === "host-component" &&
        (item.level === "ERROR" || item.level === "WARN"),
    )
    .forEach((item) => {
      const master = masters.find(
        (candidate) =>
          !candidate.isInstalled &&
          candidate.component === item["component-name"] &&
          candidate.hostName === item.host,
      );

      if (!master) {
        return;
      }

      const key = masterValidationKey(master.hostName, master.component);
      messages[key] = {
        ...messages[key],
        ...(item.level === "ERROR"
          ? { errorMessage: item.message }
          : { warnMessage: item.message }),
      };
    });

  return messages;
}
