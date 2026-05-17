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

// eslint-disable-next-line @typescript-eslint/no-unused-vars

import {
  MoveComponentConfigInitializer,
  ReassignComponentDependencies,
} from "./MoveComponentConfigInitializer";

type ConfigProperty = {
  name: string;
  value: string | number;
  filename: string;
  recommendedValue?: string | number;
};

type LocalDB = Record<string, any>;
type Dependencies = Record<string, any>;

/**
 * Initializer for configs which should be affected when NameNode is moved from one host to another
 * If NameNode HA-mode is already activated, several configs are also updated
 */
export class MoveNameNodeConfigInitializer extends MoveComponentConfigInitializer {
  private static instance: MoveNameNodeConfigInitializer;

  constructor() {
    super();

    // Initialize the standard initializers for NameNode configs
    this.initializers = {
      "dfs.namenode.http-address.{{namespaceId}}.{{suffix}}":
        MoveComponentConfigInitializer.getTargetHostConfig(50070),
      "dfs.namenode.https-address.{{namespaceId}}.{{suffix}}":
        MoveComponentConfigInitializer.getTargetHostConfig(50470),
      "dfs.namenode.rpc-address.{{namespaceId}}.{{suffix}}":
        MoveComponentConfigInitializer.getTargetHostConfig(8020),
      "dfs.namenode.servicerpc-address.{{namespaceId}}.{{suffix}}":
        MoveComponentConfigInitializer.getTargetHostConfig(8021),
      "dfs.namenode.lifeline.rpc-address.{{namespaceId}}.{{suffix}}":
        MoveComponentConfigInitializer.getTargetHostConfig(8050),

      // 'dfs.namenode.http-address': MoveComponentConfigInitializer.getTargetHostConfig(50070),
      // 'dfs.namenode.https-address': MoveComponentConfigInitializer.getTargetHostConfig(50470),
      // 'dfs.namenode.rpc-address': MoveComponentConfigInitializer.getTargetHostConfig(8020),
      // 'fs.defaultFS': MoveComponentConfigInitializer.getTargetHostConfig(8020, 'hdfs://')
    };

    // Initialize unique initializers for special handling
    this.uniqueInitializers = {
      "instance.volumes": "_initInstanceVolumes",
      "instance.volumes.replacements": "_initInstanceVolumesReplacements",
      "hbase.rootdir": "_initHbaseRootDir",
      hawq_dfs_url: "_initHawqDfsUrl",
    };
  }

  /**
   * Static method to get singleton instance
   */
  public static getInstance(): MoveNameNodeConfigInitializer {
    if (!MoveNameNodeConfigInitializer.instance) {
      MoveNameNodeConfigInitializer.instance =
        new MoveNameNodeConfigInitializer();
    }
    return MoveNameNodeConfigInitializer.instance;
  }

  /**
   * Static setup method for compatibility with Step3
   */
  public static setup(settings: any): void {
    const instance = MoveNameNodeConfigInitializer.getInstance();
    instance.setup(settings);
  }

  /**
   * Static cleanup method for compatibility with Step3
   */
  public static cleanup(): void {
    const instance = MoveNameNodeConfigInitializer.getInstance();
    instance.cleanup();
  }

  /**
   * Static method to get initial value for compatibility
   */
  public static initialValue(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies
  ): ConfigProperty {
    const instance = MoveNameNodeConfigInitializer.getInstance();
    const result = instance.initialValue(configProperty, localDB, dependencies);

    return result;
  }

  /**
   * Unique initializer for instance.volumes config
   * Value example: 'hdfs://host1:8020/apps/accumulo/data'
   */

  //@ts-ignore
  private _initInstanceVolumes(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies
  ): ConfigProperty {
    const reassignDeps = dependencies as ReassignComponentDependencies;

    // Process regardless of HA status
    if (this._isServiceInstalled(localDB, "ACCUMULO")) {
      let value = String(configProperty.value);
      value = value.replace(
        /\/\/[^\/]*/,
        "//" + reassignDeps.targetHostName + ":8020"
      );
      configProperty.value = value;
      configProperty.recommendedValue = value;
    }
    return configProperty;
  }

  /**
   * Unique initializer for instance.volumes.replacements config
   * Value example: 'hdfs://host1:8020/apps/accumulo/data hdfs://host2:8020/apps/accumulo/data'
   */
  //@ts-ignore
  private _initInstanceVolumesReplacements(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies
  ): ConfigProperty {
    const reassignDeps = dependencies as ReassignComponentDependencies;

    // Process regardless of HA status
    if (this._isServiceInstalled(localDB, "ACCUMULO")) {
      const target =
        "hdfs://" +
        reassignDeps.targetHostName +
        ":8020" +
        "/apps/accumulo/data";
      const source =
        "hdfs://" +
        reassignDeps.sourceHostName +
        ":8020" +
        "/apps/accumulo/data";
      const value = source + " " + target;
      configProperty.value = value;
      configProperty.recommendedValue = value;
    }
    return configProperty;
  }

  /**
   * Unique initializer for hbase.rootdir config (for HBASE service)
   */
  //@ts-ignore
  private _initHbaseRootDir(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies
  ): ConfigProperty {
    const reassignDeps = dependencies as ReassignComponentDependencies;

    // Process regardless of HA status
    if (
      this._isServiceInstalled(localDB, "HBASE") &&
      configProperty.filename === "hbase-site"
    ) {
      let value = String(configProperty.value);
      value = value.replace(
        /\/\/[^\/]*/,
        "//" + reassignDeps.targetHostName + ":8020"
      );
      configProperty.value = value;
      configProperty.recommendedValue = value;
    }
    return configProperty;
  }

  /**
   * Unique initializer for hawq_dfs_url config (for HAWQ service)
   */
  // @ts-ignore
  private _initHawqDfsUrl(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies
  ): ConfigProperty {
    const reassignDeps = dependencies as ReassignComponentDependencies;

    // Process regardless of HA status
    if (
      this._isServiceInstalled(localDB, "HAWQ") &&
      configProperty.filename === "hawq-site"
    ) {
      let value = String(configProperty.value);
      value = value.replace(/(.*):/, reassignDeps.targetHostName + ":");
      configProperty.value = value;
      configProperty.recommendedValue = value;
    }
    return configProperty;
  }

  /**
   * Helper method to check if HA is enabled
   */
  //@ts-ignore
  private _isHaEnabled(localDB: LocalDB): boolean {
    // Check if HA is enabled by looking at the localDB
    return localDB.isHaEnabled || false;
  }

  /**
   * Helper method to check if a service is installed
   */
  private _isServiceInstalled(localDB: LocalDB, serviceName: string): boolean {
    return (
      localDB.installedServices &&
      localDB.installedServices.includes(serviceName)
    );
  }
}

export default MoveNameNodeConfigInitializer;
