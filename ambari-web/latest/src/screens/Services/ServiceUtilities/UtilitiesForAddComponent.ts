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
import { getComponentName } from "../../Hosts/utils";
import { IHostComponent } from "../../../models/hostComponent";
import {
  CompatibleComponent,
  ComponentDependency,
} from "../../../screens/Hosts/utils/ComponentDependency";
import { addDeleteComponentsMap } from "../../../Utils/Utility";

class UtilitiesForAddComponent {
  assert(condition: any, message: any) {
    if (!condition) {
      throw new Error(message);
    }
  }
  
  compatibleWith(
    component: any,
    compName: string,
    compType: string
  ) {
    return (
      component.componentName === compName ||
      (component.componentType && component.componentType === compType)
    );
  }
  
  missingDependencies(
    data: any,
    component: IHostComponent,
    installedComponents: any,
    opt: any
  ) {
    opt = opt || {};
    opt.scope = opt.scope || "*";
    var dependencies: any = get(component, "dependencies", []);
    dependencies =
      opt.scope === "*"
        ? dependencies
        : dependencies.filter((item: any) => {
            return item.Dependencies.scope === opt.scope;
          });
    if (dependencies.length === 0) return [];

    var missingComponents = dependencies.filter((dependency: any) => {
      return !installedComponents.some((installedComponent: IHostComponent) => {
        const dependencyComponent = data.allComponents.find(
          (host: IHostComponent) => {
            return (
              host.componentName === dependency.Dependencies.component_name
            );
          }
        );
        return this.compatibleWith(
          installedComponent,
          dependencyComponent.componentName,
          dependencyComponent.componentType
        );
      });
    });
    return missingComponents.map((missingComponent: any) => {
      var componentFound = data.allComponents.find(
        (hostComponent: IHostComponent) => {
          return (
            hostComponent.componentName ===
            missingComponent.Dependencies.component_name
          );
        }
      );
      const compatibleComponents: CompatibleComponent[] = componentFound
        ? [
            {
              componentName: componentFound.componentName,
              serviceName: componentFound.serviceName,
            },
          ]
        : [];

      return new ComponentDependency(
        missingComponent.Dependencies.component_name,
        compatibleComponents
      );
    });
  }
  
  checkComponentDependencies(
    data: any,
    component: IHostComponent,
    opt: any
  ) {
    var opt = opt || {};
    opt.scope = opt.scope || "*";
    var installedComponents;
    switch (opt.scope) {
      case "host":
        this.assert(
          "You should pass at least `hostName` or `installedComponents` to options.",
          opt.hostName || opt.installedComponents
        );
        installedComponents = opt.installedComponents || [];
        break;
      default:
        installedComponents = opt.installedComponents || [];
        break;
    }
    return this.missingDependencies(data, component, installedComponents, opt)?.map(
      (componentDependency: { chooseCompatible: (arg0: any) => any }) => {
        return componentDependency.chooseCompatible(data.services);
      }
    );
  }

  getComponentRelatedDataForAddComponent(
    component: any,
    data: any
  ) {
    const hostName = get(component, "hostName");
    const componentName = getComponentName(component);

    const missedComponents = get(data, "fromServiceSummary", false)
      ? []
      : this.checkComponentDependencies(data, component, {
          scope: "host",
          installedComponents: get(data, "host.hostComponents", []),
        });
    const componentsMapItem = get(addDeleteComponentsMap, componentName, null);
    return {
      hostName,
      missedComponents,
      componentsMapItem,
    };
  }
}

export default UtilitiesForAddComponent;
