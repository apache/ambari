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

import { MoveComponentConfigInitializer, MoveInitializer } from './MoveComponentConfigInitializer';

type ConfigProperty = {
    name: string;
    value: string | number;
    filename: string;
    recommendedValue?: string | number;
};

type LocalDB = Record<string, any>;
type Dependencies = Record<string, any>;

export interface HiveComponentInitializer extends MoveInitializer {
  components: string[];
  movedComponent: string;
}

/**
 * Common class for Initializers which are used when some HIVE's component is moved from one host to another
 */
export class MoveHiveComponentConfigInitializer extends MoveComponentConfigInitializer {
  constructor() {
    super();
    this.initializerTypes = [
      ...this.initializerTypes,
      { name: 'hive_hosts_with_components', method: '_initAsHostsWithComponentsConsideringMovedComponent' }
    ];
  }

  /**
   * Initializer for configs with value equal to the list of hosts where some components exist
   * This list is affected by component's moving.
   * Example: movedComponent is 'Component1', list of needed components is ['Component1', 'Component2']
   * So, when hosts for each needed component will be mapped, host where 'Component1' was before moving ('host1') will be skipped
   * and host where 'Component1' is moved will be added ('host2')
   * But, if 'Component2' exists of the 'host1', 'host1' will be added to the hosts list
   */
  protected _initAsHostsWithComponentsConsideringMovedComponent(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies,
    initializer: HiveComponentInitializer
  ): ConfigProperty {
    let allHosts: string[] = [];
    
    initializer.components.forEach(component => {
      let hosts = localDB.masterComponentHosts
        .filter((host: any) => host.component === component)
        .map((host: any) => host.hostName);
      
      if (component === initializer.movedComponent) {
        hosts = hosts.filter((host: string) => host !== dependencies.sourceHostName);
        if (!hosts.includes(dependencies.targetHostName)) {
          hosts.push(dependencies.targetHostName);
        }
      }
      allHosts = allHosts.concat(hosts);
    });

    const uniqueHosts = Array.from(new Set(allHosts));
    const value = uniqueHosts.sort().join(',');
    configProperty.value = value;
    configProperty.recommendedValue = value;
    return configProperty;
  }

  /**
   * Settings for hive_hosts_with_components-initializer
   * Used for configs with value equal to the hosts list where needed components exist.
   * This list is affected by component's moving
   */
  public static getHostsWithComponentsConfig(neededComponents: string | string[], movedComponent: string): HiveComponentInitializer {
    return {
      type: 'hive_hosts_with_components',
      components: Array.isArray(neededComponents) ? neededComponents : [neededComponents],
      movedComponent: movedComponent
    };
  }
}

export default MoveHiveComponentConfigInitializer;
