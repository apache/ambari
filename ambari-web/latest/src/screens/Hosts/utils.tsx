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

import { get, isEmpty } from "lodash";
import { ComponentType } from "./enums";
import { translate, translateWithVariables } from "../../Utils/Utility";

export const sortBasedOnMasterSlave = (data: any, key: string) => {
  const masterComponents = data.filter(
    (component: any) => get(component, key, "") === ComponentType.MASTER
  );
  const slaveComponents = data.filter(
    (component: any) => get(component, key, "") === ComponentType.SLAVE
  );
  const clientComponents = data.filter(
    (component: any) => get(component, key, "") === ComponentType.CLIENT
  );
  return masterComponents.concat(slaveComponents).concat(clientComponents);
};

export const pluralize = (name: string) => {
  return name + "s";
};

export const validateInteger = (
  str: string | number,
  min?: number,
  max?: number
): string => {
  if (typeof str === "number") {
    str = str.toString();
  }
  if (str === "" || str.trim().length < 1) {
    return translate("number.validate.empty") as string;
  }
  str = str.trim();
  const number = parseInt(str);
  if (isNaN(number)) {
    return translate("number.validate.notValidNumber") as string;
  }
  if (str.length !== number.toString().length) {
    return translate("number.validate.notValidNumber") as string;
  }
  if (min && number < min) {
    return translateWithVariables("number.validate.lessThanMinimum", {
      "0": min.toString(),
    }) as string;
  }
  if (max && number > max) {
    return translateWithVariables("number.validate.moreThanMaximum", {
      "0": max.toString(),
    }) as string;
  }
  return "";
};

export const getAllComponents = (serviceComponentInfo: any) => {
  if (!isEmpty(serviceComponentInfo)) {
    let allComponentsCopy: any[] = [];
    get(serviceComponentInfo, "items", []).forEach((service: any) => {
      allComponentsCopy = allComponentsCopy.concat(
        get(service, "components", []).map((component: any) => {
          return {
            HostRoles: {
              ...get(component, "StackServiceComponents"),
              dependencies: get(component, "dependencies", []).map(
                (d: any) => d.Dependencies.component_name
              ),
            },
          };
        })
      );
    });
    return allComponentsCopy;
  }
  return [];
};
