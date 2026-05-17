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

export interface RmHaInitializer extends MoveInitializer {
  rmHaShouldBeEnabled: boolean;
}

/**
 * Initializer for configs which should be affected when Resource Manager is moved from one host to another
 * If Resource Manager HA-mode is already activated, several configs are also updated
 */
export class MoveRmConfigInitializer extends MoveComponentConfigInitializer {
  private static instance: MoveRmConfigInitializer;

  constructor() {
    super();
    this.initializerTypes = [
      ...this.initializerTypes,
      { name: 'rm_ha_depended', method: '_initAsRmHaDepended' },
      { name: 'rm_ha_hawq', method: '_initAsRmHaHawq' }
    ];

    this.initializers = {
      'yarn.resourcemanager.hostname.{{suffix}}': MoveRmConfigInitializer.getRmHaDependedConfig(true),
      'yarn.resourcemanager.webapp.address.{{suffix}}': MoveRmConfigInitializer.getRmHaDependedConfig(true),
      'yarn.resourcemanager.webapp.https.address.{{suffix}}': MoveRmConfigInitializer.getRmHaDependedConfig(true),
      'yarn.resourcemanager.resource-tracker.address.{{suffix}}': MoveRmConfigInitializer.getRmHaDependedConfig(true),
      'yarn.resourcemanager.address.{{suffix}}': MoveRmConfigInitializer.getRmHaDependedConfig(true),
      'yarn.resourcemanager.ha': MoveRmConfigInitializer.getRmHaHawqConfig(true),
      'yarn.resourcemanager.scheduler.ha': MoveRmConfigInitializer.getRmHaHawqConfig(true)
    };
  }

  /**
   * Check if ResourceManager HA is enabled
   */
  private _isRMHaEnabled(localDB: LocalDB): boolean {
    // Check if there are multiple ResourceManager instances
    const rmHosts = localDB.masterComponentHosts
      .filter((host: any) => host.component === 'RESOURCEMANAGER')
      .map((host: any) => host.hostName);
    return rmHosts.length > 1;
  }

  /**
   * Initializer for configs with value equal to the target hostName
   * and based on ResourceManager HA status
   * Value example: 'host1:port1'
   */
  protected _initAsRmHaDepended(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies,
    initializer: RmHaInitializer
  ): ConfigProperty {
    const isRMHaEnabled = this._isRMHaEnabled(localDB);
    
    if (isRMHaEnabled === initializer.rmHaShouldBeEnabled) {
      const value = String(configProperty.value);
      const parts = value.split(':');
      parts[0] = dependencies.targetHostName;
      const newValue = parts.join(':');
      configProperty.value = newValue;
      configProperty.recommendedValue = newValue;
    }
    return configProperty;
  }

  /**
   * This function generates "rm1_host:port,rm2_host:port" for HAWQ configuration (RM HA specific)
   * 
   * configProperty = {
   *   filename: "yarn-client",
   *   name: "yarn.resourcemanager.ha",
   *   value: "ip-10-32-36-162.ore1.vpc.pivotal.io:8050"
   * }
   *
   * dependencies = {
   *   rm1: "ip-10-32-38-225.ore1.vpc.pivotal.io",
   *   rm2: "ip-10-32-38-162.ore1.vpc.pivotal.io",
   *   sourceHostName: "ip-10-32-36-162.ore1.vpc.pivotal.io",
   *   targetHostName: "ip-10-32-38-34.ore1.vpc.pivotal.io"
   * }
   */
  protected _initAsRmHaHawq(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies,
    initializer: RmHaInitializer
  ): ConfigProperty {
    const isRMHaEnabled = this._isRMHaEnabled(localDB);
    
    if (isRMHaEnabled === initializer.rmHaShouldBeEnabled) {
      // Which of rm1 and rm2 is changed?
      let rm1: string, rm2: string;
      
      if (dependencies.rm1 === dependencies.sourceHostName) {
        rm1 = dependencies.targetHostName;
        rm2 = dependencies.rm2;
      } else {
        rm1 = dependencies.rm1;
        rm2 = dependencies.targetHostName;
      }

      // Default ports from Hadoop YARN configuration
      const DEFAULT_RM_PORT = 8032;
      const DEFAULT_RM_SCHEDULER_PORT = 8030;

      let newValue: string;
      if (configProperty.name === 'yarn.resourcemanager.ha') {
        newValue = `${rm1}:${DEFAULT_RM_PORT},${rm2}:${DEFAULT_RM_PORT}`;
      } else {
        newValue = `${rm1}:${DEFAULT_RM_SCHEDULER_PORT},${rm2}:${DEFAULT_RM_SCHEDULER_PORT}`;
      }

      configProperty.value = newValue;
      configProperty.recommendedValue = newValue;
    }
    return configProperty;
  }

  /**
   * Settings for rm_ha_depended-initializer
   * Used for configs with value equal to the host name (host where component is moved)
   */
  public static getRmHaDependedConfig(rmHaShouldBeEnabled: boolean): RmHaInitializer {
    return {
      type: 'rm_ha_depended',
      rmHaShouldBeEnabled: Boolean(rmHaShouldBeEnabled)
    };
  }

  /**
   * Settings for rm_ha_hawq-initializer
   * Used for HAWQ RM HA configurations
   */
  public static getRmHaHawqConfig(rmHaShouldBeEnabled: boolean): RmHaInitializer {
    return {
      type: 'rm_ha_hawq',
      rmHaShouldBeEnabled: Boolean(rmHaShouldBeEnabled)
    };
  }

  // Singleton pattern for compatibility with Step3
  public static getInstance(): MoveRmConfigInitializer {
    if (!MoveRmConfigInitializer.instance) {
      MoveRmConfigInitializer.instance = new MoveRmConfigInitializer();
    }
    return MoveRmConfigInitializer.instance;
  }

  public static setup(settings: any): void {
    const instance = MoveRmConfigInitializer.getInstance();
    instance.setup(settings);
  }

  public static cleanup(): void {
    const instance = MoveRmConfigInitializer.getInstance();
    instance.cleanup();
  }

  public static initialValue(configProperty: ConfigProperty, localDB: LocalDB, dependencies: Dependencies): ConfigProperty {
    const instance = MoveRmConfigInitializer.getInstance();
    return instance.initialValue(configProperty, localDB, dependencies);
  }
}

export default MoveRmConfigInitializer;
