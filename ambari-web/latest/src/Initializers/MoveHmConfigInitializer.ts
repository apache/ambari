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

import { MoveHiveComponentConfigInitializer } from './MoveHiveComponentConfigInitializer';

type ConfigProperty = {
    name: string;
    value: string | number;
    filename: string;
    recommendedValue?: string | number;
};

type LocalDB = Record<string, any>;
type Dependencies = Record<string, any>;

/**
 * Initializer for configs which should be affected when Hive Metastore is moved from one host to another
 */
export class MoveHmConfigInitializer extends MoveHiveComponentConfigInitializer {
  private static instance: MoveHmConfigInitializer;

  constructor() {
    super();
    
    this.initializers = {
      'hadoop.proxyuser.{{hiveUser}}.hosts': MoveHiveComponentConfigInitializer.getHostsWithComponentsConfig(
        ['HIVE_SERVER', 'HIVE_METASTORE', 'HIVE_SERVER_INTERACTIVE'], 
        'HIVE_METASTORE'
      )
    };

    this.uniqueInitializers = {
      'hive.metastore.uris': '_initHiveMetastoreUris',
      'templeton.hive.properties': '_initTempletonHiveProperties'
    };
  }

  /**
   * Unique initializer for hive.metastore.uris-config
   * Value example: 'thrift://host1:1234,thrift://host2:1234,thrift://host3:1234'
   */
  protected _initHiveMetastoreUris(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies
  ): ConfigProperty {
    const hiveMSHosts = this._getHmHostsConsideringMoved(localDB, dependencies);
    const value = String(configProperty.value);

    // Extract port from existing value
    const portMatch = value.match(/:[0-9]{2,4}/);
    const port = portMatch ? portMatch[0].slice(1) : '9083';

    const newValue = hiveMSHosts
      .filter((host, index, arr) => arr.indexOf(host) === index) // unique hosts
      .map(hiveMSHost => `thrift://${hiveMSHost}:${port}`)
      .join(',');

    configProperty.value = newValue;
    configProperty.recommendedValue = newValue;
    return configProperty;
  }

  /**
   * Unique initializer for templeton.hive.properties-config
   * Replace existing hosts with new
   * Value example: 'hive.metastore.local=false,hive.metastore.uris=thrift://host1:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true'
   */
  protected _initTempletonHiveProperties(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies
  ): ConfigProperty {
    const hiveMSHosts = this._getHmHostsConsideringMoved(localDB, dependencies);
    let value = String(configProperty.value);
    
    // Replace the thrift URIs in the properties string
    const hostsString = hiveMSHosts.join('\\,');
    value = value.replace(/thrift.+[0-9]{2,},/i, `${hostsString},`);

    configProperty.value = value;
    configProperty.recommendedValue = value;
    return configProperty;
  }

  /**
   * Get list of hosts where HIVE_METASTORE exists considering component's moving 
   * (host where it was is removed and host where it will be is added)
   */
  private _getHmHostsConsideringMoved(localDB: LocalDB, dependencies: Dependencies): string[] {
    let hiveMSHosts = localDB.masterComponentHosts
      .filter((host: any) => host.component === 'HIVE_METASTORE')
      .map((host: any) => host.hostName);

    // Remove source host and add target host
    hiveMSHosts = hiveMSHosts.filter((host: string) => host !== dependencies.sourceHostName);
    if (!hiveMSHosts.includes(dependencies.targetHostName)) {
      hiveMSHosts.push(dependencies.targetHostName);
    }

    return Array.from(new Set(hiveMSHosts)); // ensure unique hosts
  }

  // Singleton pattern for compatibility with Step3
  public static getInstance(): MoveHmConfigInitializer {
    if (!MoveHmConfigInitializer.instance) {
      MoveHmConfigInitializer.instance = new MoveHmConfigInitializer();
    }
    return MoveHmConfigInitializer.instance;
  }

  public static setup(settings: any): void {
    const instance = MoveHmConfigInitializer.getInstance();
    instance.setup(settings);
  }

  public static cleanup(): void {
    const instance = MoveHmConfigInitializer.getInstance();
    instance.cleanup();
  }

  public static initialValue(configProperty: ConfigProperty, localDB: LocalDB, dependencies: Dependencies): ConfigProperty {
    const instance = MoveHmConfigInitializer.getInstance();
    return instance.initialValue(configProperty, localDB, dependencies);
  }
}

export default MoveHmConfigInitializer;
