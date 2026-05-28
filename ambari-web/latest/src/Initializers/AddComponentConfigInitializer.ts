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

import HaConfigInitializer from "./HaConfigInitializer";
import { HostsBasedInitializer } from "./HostsBasedInitializer";
import ControlFlowInitializer from "./ControlFlowInitializer";
import UtilitiesForInitialziers from "./UtilitiesForInitialziers";

type ConfigProperty = {
  name: string;
  value: string | number;
  recommendedValue?: string | number;
};

type LocalDB = Record<string, any>;
type Dependencies = Record<string, any>;

//@ts-ignore
class AddComponentConfigInitializer extends HaConfigInitializer {
  protected defaultInitializers: Record<string, any>;
  protected defaultUniqueInitializers: Record<string, any>;
  public initializers: Record<string, any>;
  public uniqueInitializers: Record<string, any>;
  private __copyInitializers: Record<string, any> = {};
  private __copyUniqueInitializers: Record<string, any> = {};
  //@ts-ignore
  private utilitiesForInitialziers: any;

  constructor() {
    super();

    this.defaultInitializers = {
      "zookeeper.connect": this.getHDPStackOnlyHostsPortConfig(
        "2.2",
        "ZOOKEEPER_SERVER",
        "",
        "",
        ",",
        "zkClientPort",
        true
      ),
      "ha.zookeeper.quorum": this.getHDPStackOnlyHostsPortConfig(
        "2.2",
        "ZOOKEEPER_SERVER",
        "",
        "",
        ",",
        "zkClientPort",
        true
      ),
      "hbase.zookeeper.quorum":
        HostsBasedInitializer.getHostsListComponentConfig(
          "ZOOKEEPER_SERVER",
          true
        ),
      "instance.zookeeper.host": this.getHostsWithPortConfig(
        "ZOOKEEPER_SERVER",
        "",
        "",
        ",",
        "zkClientPort",
        true
      ),
      "templeton.zookeeper.hosts": this.getHostsWithPortConfig(
        "ZOOKEEPER_SERVER",
        "",
        "",
        ",",
        "zkClientPort",
        true
      ),
      "hive.cluster.delegation.token.store.zookeeper.connectString":
        this.getHostsWithPortConfig(
          "ZOOKEEPER_SERVER",
          "",
          "",
          ",",
          "zkClientPort",
          true
        ),
      "hive.zookeeper.quorum": this.getHDPStackOnlyHostsPortConfig(
        "2.2",
        "ZOOKEEPER_SERVER",
        "",
        "",
        ",",
        "zkClientPort",
        true
      ),
      "hadoop.registry.zk.quorum": this.getHDPStackOnlyHostsPortConfig(
        "2.2",
        "ZOOKEEPER_SERVER",
        "",
        "",
        ",",
        "zkClientPort",
        true
      ),
      "hadoop.proxyuser.{{hiveUser}}.hosts": this.getComponentsHostsConfig(
        [
          "HIVE_SERVER",
          "WEBHCAT_SERVER",
          "HIVE_METASTORE",
          "HIVE_SERVER_INTERACTIVE",
        ],
        false,
        true
      ),
      "hive.metastore.uris": this.getHostsWithPortConfig(
        "HIVE_METASTORE",
        "thrift://",
        "",
        ",thrift://",
        "hiveMetastorePort",
        true
      ),
    };

    this.defaultUniqueInitializers = {
      "yarn.resourcemanager.zk-address": this.initYarnRMZkAddress,
      "templeton.hive.properties": this._initTempletonHiveProperties,
      "atlas.graph.index.search.solr.zookeeper-url":
        this._initAtlasGraphIndexSearchSolrZkUrl,
    };

    this.initializers = {};
    this.uniqueInitializers = {};
  }

  setup(): void;

  setup(settings?: Record<string, any>) {
    if (!settings) {
      this._updateInitializers();
    } else {
      //@ts-ignore
      this._updateInitializers(settings);
    }
  }

  updateSiteObj(
    siteConfigs: Record<string, any>,
    configProperty: ConfigProperty
  ): boolean {
    if (!siteConfigs || !configProperty) return false;

    const initializer = this.initializers[configProperty.name];
    const isArray = !!(
      initializer &&
      (initializer.type === "json_stringified_value" ||
        (Array.isArray(initializer) &&
          initializer.some(
            (item: any) => item.type === "json_stringified_value"
          )))
    );

    UtilitiesForInitialziers.updateHostsListValue(
      siteConfigs,
      //@ts-ignore
      configProperty.filename,
      configProperty.name,
      //@ts-ignore
      configProperty.value,
      isArray
    );
    return true;
  }

  getNameNodeHAOnlyHostsConfig(components: string[], asArray: boolean): any[] {
    return [
      ControlFlowInitializer.getNameNodeHAControl(),
      //@ts-ignore
      this.getComponentsHostsConfig(components, asArray),
    ];
  }

  // type ComponentsHostsConfig = {
  //     type: string;
  //     components: string[];
  //     asArray: boolean;
  //     isInstalled?: boolean;
  // };

  getComponentsHostsConfig(
    components: string | string[],
    asArray: boolean = false,
    isInstalled?: boolean
  ) {
    const ret = {
      type: "hosts_with_components",
      components: Array.isArray(components) ? components : [components],
      asArray,
      isInstalled,
    };
    return ret;
  }

  getHostsWithPortConfig(
    component: string,
    prefix: string,
    suffix: string,
    delimiter: string,
    port: string,
    portFromDependencies: boolean
  ): {
    type: string;
    component: string;
    modifier: {
      prefix: string;
      suffix: string;
      delimiter: string;
    };
    portKey?: string;
    port?: string;
    componentExists?: boolean | undefined;
  } {
    const ret = HostsBasedInitializer.getHostsWithPortConfig(
      component,
      prefix,
      suffix,
      delimiter,
      port,
      portFromDependencies
    ) as {
      type: string;
      component: string;
      modifier: {
        prefix: string;
        suffix: string;
        delimiter: string;
      };
      portKey?: string;
      port?: string;
      componentExists?: boolean;
    };
    ret.componentExists = true;
    return ret;
  }

  getNameNodeHAOnlyHostsPortConfig(
    component: string,
    prefix: string,
    suffix: string,
    delimiter: string,
    port: string,
    portFromDependencies: boolean
  ): any[] {
    return [
      ControlFlowInitializer.getNameNodeHAControl(),
      HostsBasedInitializer.getHostsWithPortConfig(
        component,
        prefix,
        suffix,
        delimiter,
        port,
        portFromDependencies
      ),
    ];
  }

  getResourceManagerHAOnlyHostsPortConfig(
    component: string,
    prefix: string,
    suffix: string,
    delimiter: string,
    port: string,
    portFromDependencies: boolean
  ): any[] {
    return [
      ControlFlowInitializer.getResourceManagerHAControl(),
      this.getHostsWithPortConfig(
        component,
        prefix,
        suffix,
        delimiter,
        port,
        portFromDependencies
      ),
    ];
  }

  getHostsListComponentJSONStringifiedConfig(
    component: string,
    componentExists: boolean,
    //@ts-ignore
    delimiter: string
  ): any[] {
    return [
      //@ts-ignore
      this.getHostsListComponentConfig(component, componentExists),
      //@ts-ignore
      this.getJSONStringifiedValueConfig(),
    ];
  }

  getHDPStackOnlyHostsPortConfig(
    minStackVersion: string,
    component: string,
    prefix: string,
    suffix: string,
    delimiter: string,
    port: string,
    portFromDependencies: boolean
  ): any[] {
    return [
      ControlFlowInitializer.getHDPStackVersionControl(minStackVersion),
      this.getHostsWithPortConfig(
        component,
        prefix,
        suffix,
        delimiter,
        port,
        portFromDependencies
      ),
    ];
  }

  cleanup() {
    this._restoreInitializers();
  }

  // private _updateInitializers(settings: Record<string, any>): void {
  //     this.__copyInitializers = { ...this.initializers };
  //     this.__copyUniqueInitializers = { ...this.uniqueInitializers };
  //
  //     this.initializers = this._updateNames(this.initializers, settings);
  //     this.uniqueInitializers = this._updateNames(this.uniqueInitializers, settings);
  // }

  private deepCopy(obj: any): any {
    return JSON.parse(JSON.stringify(obj));
  }

  // Updates the initializers and uniqueInitializers based on settings

  private _updateInitializers(): void;

  private _updateInitializers(settings?: Record<string, any>) {
    if (!settings) {
      this._bootstrapInitializers(Object.keys(this.defaultInitializers));
    } else {
      this.__copyInitializers = this.deepCopy(this.defaultInitializers);
      const updatedInitializers = this._updateNames(
        "defaultInitializers",
        settings
      );
      this._setForComputed("defaultInitializers", updatedInitializers);

      this.__copyUniqueInitializers = this.deepCopy(
        this.defaultUniqueInitializers
      );
      const updatedUniqueInitializers = this._updateNames(
        "defaultUniqueInitializers",
        settings
      );
      this._setForComputed(
        "defaultUniqueInitializers",
        updatedUniqueInitializers
      );
    }
  }

  // private _restoreInitializers() {
  //     this.initializers = { ...this.defaultInitializers };
  //     this.uniqueInitializers = { ...this.defaultUniqueInitializers };
  // }

  _updateNames(sourceKey: any, settings: any) {
    settings = settings || {};
    //@ts-ignore
    var source = this[sourceKey];
    let updatedSource: Record<string, any> = {};
    Object.keys(source).forEach((configName) => {
      const initializer = source[configName];

      // Use regex to replace any placeholder in the format {{...}} with the settings string
      const updatedName = configName.replace(/{{.*?}}/g, settings);
      updatedSource[updatedName] = initializer;
    });
    return updatedSource;
  }

  // Updates the initializers and uniqueInitializers based on settings
  // public _updateInitializers(settings: Record<string, any> = {}): void {
  //     this.__copyInitializers = this.deepCopy(this.initializers);
  //     const updatedInitializers = this._updateNames('initializers', settings);
  //     console.log("time to set computed properties for default initializers");
  //     this._setForComputed('initializers', updatedInitializers);
  //
  //     this.__copyUniqueInitializers = this.deepCopy(this.uniqueInitializers);
  //     const updatedUniqueInitializers = this._updateNames('uniqueInitializers', settings);
  //     this._setForComputed('uniqueInitializers', updatedUniqueInitializers);
  // }

  // Restores the initializers to their original state
  public _restoreInitializers(): void {
    this.__copyInitializers = this.deepCopy(this.defaultInitializers);
    this.__copyUniqueInitializers = this.deepCopy(
      this.defaultUniqueInitializers
    );

    if (this.__copyInitializers) {
      this._setForComputed(
        "defaultInitializers",
        this.deepCopy(this.__copyInitializers)
      );
    }
    if (this.__copyUniqueInitializers) {
      this._setForComputed(
        "defaultUniqueInitializers",
        this.deepCopy(this.__copyUniqueInitializers)
      );
    }
  }

  // Sets a value for a computed property
  private _setForComputed(key: string, value: any): void {
    //@ts-ignore
    this[key] = value;
  }

  // /**
  //  * Initializes the Yarn ResourceManager ZooKeeper address.
  //  * @param configProperty The configuration property to be initialized.
  //  * @param localDB The local database containing master component hosts.
  //  * @param dependencies The dependencies containing the ZooKeeper client port.
  //  * @returns The updated configuration property with the ZooKeeper address.
  //  */
  // private _initYarnRMZkAddress(configProperty: ConfigProperty, localDB: LocalDB, dependencies: Dependencies): ConfigProperty {
  //     const value = localDB.masterComponentHosts
  //         .filter((host: any) => host.component === 'ZOOKEEPER_SERVER' && host.isInstalled)
  //         .map((host: any) => `${host.hostName}:${dependencies.zkClientPort}`)
  //         .join(',');
  //
  //     configProperty.value = value;
  //     configProperty.recommendedValue = value;
  //
  //     return configProperty;
  // }

  private initYarnRMZkAddress(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies
  ): ConfigProperty {
    //@ts-ignore
    return HostsBasedInitializer._initAsHostsWithPort(
      //@ts-ignore
      configProperty,
      localDB,
      dependencies,
      {
        component: "ZOOKEEPER_SERVER",
        componentExists: true,
        modifier: {
          prefix: "",
          suffix: "",
          delimiter: ",",
        },
        portKey: "zkClientPort",
      }
    );
  }

  private _initTempletonHiveProperties(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies
  ) {
    const hostNames = localDB.masterComponentHosts
      .filter(
        (host: any) => host.component === "HIVE_METASTORE" && host.isInstalled
      )
      .map(
        (host: any) =>
          `thrift://${host.hostName}:${dependencies.hiveMetastorePort}`
      )
      .join(",");
    configProperty.value = hostNames;
    configProperty.recommendedValue = hostNames;
    return configProperty;
  }

  private _initAtlasGraphIndexSearchSolrZkUrl(
    configProperty: ConfigProperty,
    localDB: LocalDB,
    dependencies: Dependencies
  ) {
    const solr = dependencies.infraSolrZnode;
    const hostNames = localDB.masterComponentHosts
      .filter(
        (host: any) => host.component === "ZOOKEEPER_SERVER" && host.isInstalled
      )
      .map(
        (host: any) => `${host.hostName}:${dependencies.zkClientPort}${solr}`
      )
      .join(",");
    configProperty.value = hostNames;
    configProperty.recommendedValue = hostNames;
    return configProperty;
  }

  //@ts-ignore
  private _bootstrapInitializers(properties: string[] | null) {
    const initializers: Record<string, any> = {};
    const uniqueInitializers: Record<string, any> = {};
    const defaultInitializers = this.defaultInitializers;
    const defaultUniqueInitializers = this.defaultUniqueInitializers;

    if (!properties) {
      this.initializers = { ...defaultInitializers };
      this.uniqueInitializers = { ...defaultUniqueInitializers };
    } else {
      properties.forEach((propertyName) => {
        if (defaultInitializers[propertyName]) {
          initializers[propertyName] = defaultInitializers[propertyName];
        } else if (defaultUniqueInitializers[propertyName]) {
          uniqueInitializers[propertyName] =
            defaultUniqueInitializers[propertyName];
        }
      });
      this.initializers = initializers;
      this.uniqueInitializers = uniqueInitializers;
    }
  }
}
//@ts-ignore
// Apply mixins
interface AddComponentConfigInitializer
  extends HostsBasedInitializer,
    ControlFlowInitializer {}
Object.assign(
  AddComponentConfigInitializer.prototype,
  HostsBasedInitializer.prototype
);
Object.assign(
  AddComponentConfigInitializer.prototype,
  ControlFlowInitializer.prototype
);

export default AddComponentConfigInitializer;
