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

import ConfigInitializer from './ConfigInitializer';

type ConfigProperty = {
    name: string;
    value: string | number;
    filename: string;
    recommendedValue?: string | number;
};

export interface ReassignComponentDependencies {
  targetHostName: string;
  sourceHostName: string;
  namespaceId?: string;
  [key: string]: any;
}

type LocalDB = Record<string, any>;
type Dependencies = Record<string, any>;

export interface MoveInitializer {
  type: string;
  component?: string;
  port?: string | number;
}

/**
 * Basic class for all Initializers used for configs which are affected by some component's moving from
 * one host to another
 */
export class MoveComponentConfigInitializer extends ConfigInitializer {
  protected initializers: Record<string, MoveInitializer | MoveInitializer[]> = {};
  protected uniqueInitializers: Record<string, string> = {};
  private __copyInitializers?: Record<string, MoveInitializer | MoveInitializer[]>;
  private __copyUniqueInitializers?: Record<string, string>;

  constructor() {
    super();
    this.initializerTypes = [
      ...this.initializerTypes,
      { name: 'hosts_with_component', method: '_initAsHostsWithComponentConsideringMoving' },
      { name: 'target_host', method: '_initAsTargetHost' }
    ];
  }

  /**
   * Setup method to update initializers with settings
   */
  public setup(settings: any): void {
    this._updateInitializers(settings);
  }

  /**
   * Cleanup method to restore initializers
   */
  public cleanup(): void {
    this._restoreInitializers();
  }

  /**
   * Get initial value for a config property
   */
  public initialValue(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies
  ): ConfigProperty {
    const configName = configProperty.name;
    const initializer = this.initializers[configName];

    if (initializer) {
      return this._moveDefaultInitializer(configProperty, localDB, dependencies);
    }

    const uniqueInitializerMethod = this.uniqueInitializers[configName];
    if (uniqueInitializerMethod && typeof (this as any)[uniqueInitializerMethod] === 'function') {
      return (this as any)[uniqueInitializerMethod](configProperty, localDB, dependencies);
    }

    configProperty.recommendedValue = configProperty.value;
    return configProperty;
  }

  /**
   * Initializer for configs with value equal to the hostName where component will be moved (with port)
   */
  protected _initAsTargetHost(
    configProperty: ConfigProperty,
    _localDB: LocalDB,
    dependencies: Dependencies,
    initializer: MoveInitializer
  ): ConfigProperty {
    const hostName = dependencies.targetHostName;
    const port = initializer.port || '';
    const value = `${hostName}${port ? ':' + port : ''}`;
    
    configProperty.value = value;
    configProperty.recommendedValue = value;
    return configProperty;
  }

  /**
   * Initializer for configs with value equal to the hosts list where needed component exists
   * Value considers component-moving, so targetHostName will be added to the list
   * and sourceHostName will be removed
   */
  protected _initAsHostsWithComponentConsideringMoving(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies,
    initializer: MoveInitializer
  ): ConfigProperty {
    if (!initializer.component) {
      return configProperty;
    }

    let hosts = localDB.masterComponentHosts
      .filter((host: any) => host.component === initializer.component)
      .map((host: any) => host.hostName);

    // Remove source host and add target host
    hosts = hosts.filter((host: string) => host !== dependencies.sourceHostName);
    if (!hosts.includes(dependencies.targetHostName)) {
      hosts.push(dependencies.targetHostName);
    }

    const uniqueHosts = Array.from(new Set(hosts));
    const value = uniqueHosts.sort().join(',');
    configProperty.value = value;
    configProperty.recommendedValue = value;
    return configProperty;
  }

  /**
   * Default initializer that processes the initializer configuration
   */
  private _moveDefaultInitializer(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies
  ): ConfigProperty {
    const initializer = this.initializers[configProperty.name];
    if (initializer) {
      const initializerArray = Array.isArray(initializer) ? initializer : [initializer];
      
      for (const init of initializerArray) {
        const initializerType = this.initializerTypes.find(type => type.name === init.type);
        if (initializerType) {
          const method = (this as any)[initializerType.method];
          if (typeof method === 'function') {
            configProperty = method.call(this, configProperty, localDB, dependencies, init);
          }
        }
      }
    }
    return configProperty;
  }

  /**
   * Setup initializers with settings to replace placeholders like {{namespaceId}}, {{suffix}}
   * This allows config names with dynamic parts to be resolved at runtime
   * 
   * Example:
   * - Original: 'dfs.namenode.http-address.{{namespaceId}}.{{suffix}}'
   * - With settings {namespaceId: 'mycluster', suffix: 'nn1'}
   * - Becomes: 'dfs.namenode.http-address.mycluster.nn1'
   */
  private _updateInitializers(settings: any): void {
    settings = settings || {};
    
    // Backup original initializers
    this.__copyInitializers = JSON.parse(JSON.stringify(this.initializers));
    this.__copyUniqueInitializers = JSON.parse(JSON.stringify(this.uniqueInitializers));
    
    // Update initializers with placeholder replacement
    this.initializers = this._updateNames(this.initializers, settings);
    this.uniqueInitializers = this._updateNames(this.uniqueInitializers, settings);
  }

  /**
   * Restore initializers to their original state before placeholder replacement
   */
  private _restoreInitializers(): void {
    if (this.__copyInitializers) {
      this.initializers = JSON.parse(JSON.stringify(this.__copyInitializers));
      this.__copyInitializers = undefined;
    }
    if (this.__copyUniqueInitializers) {
      this.uniqueInitializers = JSON.parse(JSON.stringify(this.__copyUniqueInitializers));
      this.__copyUniqueInitializers = undefined;
    }
  }

  /**
   * Replace placeholders in object keys with actual values from settings
   * 
   * @param source - Object with keys that may contain placeholders
   * @param settings - Object with replacement values
   * @returns Updated object with placeholders replaced
   */
  private _updateNames<T>(source: Record<string, T>, settings: any): Record<string, T> {
    settings = settings || {};
    const result: Record<string, T> = {};
    
    Object.keys(source).forEach(configName => {
      const initializer = source[configName];
      let updatedConfigName = configName;
      
      // Replace each placeholder with its value from settings
      Object.keys(settings).forEach(key => {
        const replaceWith = String(settings[key]);
        const toReplace = `{{${key}}}`;
        updatedConfigName = updatedConfigName.replace(new RegExp(toReplace.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'), replaceWith);
      });
      
      result[updatedConfigName] = initializer;
    });
    return result;
  }

  /**
   * Static method to get target host config
   */
  public static getTargetHostConfig(port?: string | number): MoveInitializer {
    return {
      type: 'target_host',
      port: port || ''
    };
  }

  /**
   * Static method to get hosts with component config
   */
  public static getHostsWithComponentConfig(component: string): MoveInitializer {
    return {
      type: 'hosts_with_component',
      component: component
    };
  }
}

export default MoveComponentConfigInitializer;
