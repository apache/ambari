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
 * Initializer for configs which should be affected when Hive Server is moved from one host to another
 */
export class MoveHsConfigInitializer extends MoveHiveComponentConfigInitializer {
  private static instance: MoveHsConfigInitializer;

  constructor() {
    super();
    
    this.initializers = {
      'hadoop.proxyuser.{{hiveUser}}.hosts': MoveHiveComponentConfigInitializer.getHostsWithComponentsConfig(
        ['HIVE_SERVER', 'HIVE_METASTORE', 'HIVE_SERVER_INTERACTIVE'], 
        'HIVE_SERVER'
      )
    };
  }

  // Singleton pattern for compatibility with Step3
  public static getInstance(): MoveHsConfigInitializer {
    if (!MoveHsConfigInitializer.instance) {
      MoveHsConfigInitializer.instance = new MoveHsConfigInitializer();
    }
    return MoveHsConfigInitializer.instance;
  }

  public static setup(settings: any): void {
    const instance = MoveHsConfigInitializer.getInstance();
    instance.setup(settings);
  }

  public static cleanup(): void {
    const instance = MoveHsConfigInitializer.getInstance();
    instance.cleanup();
  }

  public static initialValue(configProperty: ConfigProperty, localDB: LocalDB, dependencies: Dependencies): ConfigProperty {
    const instance = MoveHsConfigInitializer.getInstance();
    return instance.initialValue(configProperty, localDB, dependencies);
  }
}

export default MoveHsConfigInitializer;
