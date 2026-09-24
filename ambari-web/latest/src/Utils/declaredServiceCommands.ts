/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import HostComponent, { type IHostComponent } from "../models/hostComponent";
import { getCustomCommands } from "../screens/Hosts/utils";

export function declaredServiceCommands(clusterName: string, serviceName: string, definitions: any[], inventory: any) {
  const result: { key: string; label: string; command: any; component: IHostComponent }[] = [];
  for (const group of inventory?.items || []) {
    if (group.ServiceComponentInfo?.service_name !== serviceName) continue;
    for (const item of group.host_components || []) {
      const host = item.HostRoles;
      if (!host || typeof host.host_name !== "string" || typeof host.component_name !== "string") continue;
      const definition = definitions.find(value => value.component_name === host.component_name);
      if (!definition || !Array.isArray(definition.custom_commands) || !Array.isArray(host.custom_commands)) continue;
      const commands = host.custom_commands.filter((name: unknown) => typeof name === "string" && definition.custom_commands.includes(name));
      const component = new HostComponent({
        clusterName, serviceName, hostName: host.host_name, componentName: host.component_name,
        workStatus: host.state, passiveState: host.maintenance_state, nnHAState: host.ha_state,
        componentCategory: definition.component_category, cardinality: definition.cardinality,
        isMaster: definition.component_category === "MASTER", isClient: definition.component_category === "CLIENT",
        customCommands: commands,
      } as IHostComponent);
      for (const command of getCustomCommands(component, inventory)) {
        result.push({ key: JSON.stringify([host.host_name, host.component_name, command.command]),
          label: `${command.label} (${host.host_name})`, command, component });
      }
    }
  }
  return result;
}
