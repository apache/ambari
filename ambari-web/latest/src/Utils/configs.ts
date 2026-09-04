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

// NnHaConfigInitializer.tsx
// interfaces.ts
export interface ConfigProperty {
  name: string;
  displayName: string;
  value: string;
  recommendedValue: string;
  isVisible?: boolean;
  isOverridable?: boolean;
  filename?: string;
}

export interface ExtendedTopologyLocalDB {
  installedServices: string[];
  masterComponentHosts: MasterComponentHost[];
  nameserviceId: string;
}

export interface MasterComponentHost {
  component: string;
  hostName: string;
  isInstalled: boolean;
}

export interface NnHaConfigDependencies {
  namespaceId: string;
  serverConfigs: ServerConfig[];
}

export interface ServerConfig {
  type: string;
  properties: { [key: string]: string };
}

export interface Initializer {
  type: any;
  toReplace?: string;
  modifier?: {
    prefix?: string;
    suffix?: string;
    regex?: string;
    delimiter?: string;
  };
  component?: any;
  components?: any;
  componentExists?: boolean;
  portKey?: string;
  port?: string;
  asArray?: boolean;
  isInstalled?: boolean;
}
// NnHaConfigInitializer.tsx

// NnHaConfigInitializer.tsx

class NnHaConfigInitializer {
  static getRenameWithNamespaceConfig(toReplace: string): Initializer {
    return {
      type: 'rename',
      toReplace: toReplace
    };
  }

  static getNamespaceConfig(prefix: string = '', suffix: string = ''): Initializer {
    return {
      type: 'namespace',
      modifier: {
        prefix: prefix,
        suffix: suffix
      }
    };
  } 

  static getReplaceNamespaceConfig(toReplace: string): Initializer {
    return {
      type: 'replace_namespace',
      toReplace: toReplace
    };
  }

  static getHostWithPortConfig(component: string, componentExists: boolean, prefix: string = '', suffix: string = '', port: string, portFromDependencies: boolean = false): Initializer {
    return {
      type: 'host_with_port',
      component: component,
      componentExists: componentExists,
      modifier: {
        prefix: prefix,
        suffix: suffix
      },
      portKey: portFromDependencies ? port : undefined,
      port: portFromDependencies ? undefined : port
    };
  }

  static getHostsWithPortConfig(component: string | string[], prefix: string = '', suffix: string = '', delimiter: string = ',', port: string, portFromDependencies: boolean = false): Initializer {
    return {
      type: 'hosts_with_port',
      component: component,
      modifier: {
        prefix: prefix,
        suffix: suffix,
        delimiter: delimiter
      },
      portKey: portFromDependencies ? port : undefined,
      port: portFromDependencies ? undefined : port
    };
  }

  static getComponentsHostsConfig(components: string | string[], asArray: boolean = false, isInstalled: boolean | undefined = undefined): Initializer {
    return {
      type: 'hosts_with_components',
      components: Array.isArray(components) ? components : [components],
      asArray: asArray,
      isInstalled: isInstalled
    };
  }

  static getSimpleComponentConfig(component: string, withModifier: boolean = true): Initializer {
    return {
      type: 'host_with_component',
      component: component,
      modifier: withModifier ? {
        type: 'regexp',
        regex: "([\\w|\\.]*)(?=:)"
      } : undefined
    } as any;
  }

  static getComponentConfigWithAffixes(component: string, prefix: string = '', suffix: string = ''): Initializer {
    return {
      type: 'host_with_component',
      component: component,
      modifier: {
        type: 'regexp',
        regex: ":\\/\\/" + "([\\w|\\.]*)(?=:)",
        prefix: prefix,
        suffix: suffix
      } as any
    };
  }

  static getHostsListComponentConfig(component: string, componentExists: boolean, delimiter: string = ','): Initializer {
    return {
      type: 'hosts_list_with_component',
      component: component,
      componentExists: componentExists,
      modifier: {
        delimiter: delimiter
      }
    };
  }

  uniqueInitializers: { [key: string]: (config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies) => ConfigProperty };

  constructor() {
    this.uniqueInitializers = {
      'hbase.rootdir': this._initHbaseRootDir.bind(this),
      'hawq_dfs_url': this._initHawqDfsUrl.bind(this),
      'instance.volumes': this._initInstanceVolumes.bind(this),
      'instance.volumes.replacements': this._initInstanceVolumesReplacements.bind(this),
      'dfs.journalnode.edits.dir': this._initDfsJnEditsDir.bind(this),
      'xasecure.audit.destination.hdfs.dir': this._initXasecureAuditDestinationHdfsDir.bind(this)
    };
  }

  initialValue(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies): ConfigProperty {
    
    const initializers: { [key: string]: Initializer[] } = {
      'dfs.ha.namenodes.${dfs.nameservices}': [NnHaConfigInitializer.getRenameWithNamespaceConfig('${dfs.nameservices}')],
      'dfs.namenode.rpc-address.${dfs.nameservices}.nn1': [
        NnHaConfigInitializer.getHostWithPortConfig('NAMENODE', true, '', '', 'nnRpcPort', true),
        NnHaConfigInitializer.getRenameWithNamespaceConfig('${dfs.nameservices}')
      ],
      'dfs.namenode.rpc-address.${dfs.nameservices}.nn2': [
        NnHaConfigInitializer.getHostWithPortConfig('NAMENODE', false, '', '', '8020', false),
        NnHaConfigInitializer.getRenameWithNamespaceConfig('${dfs.nameservices}')
      ],
      'dfs.namenode.http-address.${dfs.nameservices}.nn1': [
        NnHaConfigInitializer.getHostWithPortConfig('NAMENODE', true, '', '', 'nnHttpPort', true),
        NnHaConfigInitializer.getRenameWithNamespaceConfig('${dfs.nameservices}')
      ],
      'dfs.namenode.http-address.${dfs.nameservices}.nn2': [
        NnHaConfigInitializer.getHostWithPortConfig('NAMENODE', false, '', '', '50070', false),
        NnHaConfigInitializer.getRenameWithNamespaceConfig('${dfs.nameservices}')
      ],
      'dfs.namenode.https-address.${dfs.nameservices}.nn1': [
        NnHaConfigInitializer.getHostWithPortConfig('NAMENODE', true, '', '', 'nnHttpsPort', true),
        NnHaConfigInitializer.getRenameWithNamespaceConfig('${dfs.nameservices}')
      ],
      'dfs.namenode.https-address.${dfs.nameservices}.nn2': [
        NnHaConfigInitializer.getHostWithPortConfig('NAMENODE', false, '', '', '50470', false),
        NnHaConfigInitializer.getRenameWithNamespaceConfig('${dfs.nameservices}')
      ],
      'dfs.client.failover.proxy.provider.${dfs.nameservices}': [NnHaConfigInitializer.getRenameWithNamespaceConfig('${dfs.nameservices}')],
      'dfs.nameservices': [NnHaConfigInitializer.getNamespaceConfig()],
      'dfs.internal.nameservices': [NnHaConfigInitializer.getNamespaceConfig()],
      'fs.defaultFS': [NnHaConfigInitializer.getNamespaceConfig('hdfs://')],
      'dfs.namenode.shared.edits.dir': [
        NnHaConfigInitializer.getHostsWithPortConfig('JOURNALNODE', 'qjournal://', '/${dfs.nameservices}', ';', '8485', false),
        NnHaConfigInitializer.getReplaceNamespaceConfig('${dfs.nameservices}')
      ],
      'ha.zookeeper.quorum': [NnHaConfigInitializer.getHostsWithPortConfig('ZOOKEEPER_SERVER', '', '', ',', 'zkClientPort', true)]
    };

    const uniqueInitializer = {
      'hbase.rootdir': this._initHbaseRootDir.bind(this),
      'hawq_dfs_url': this._initHawqDfsUrl.bind(this),
      'instance.volumes': this._initInstanceVolumes.bind(this),
      'instance.volumes.replacements': this._initInstanceVolumesReplacements.bind(this),
      'dfs.journalnode.edits.dir': this._initDfsJnEditsDir.bind(this),
      'xasecure.audit.destination.hdfs.dir': this._initXasecureAuditDestinationHdfsDir.bind(this)
    }[config.name];
    if (uniqueInitializer) {
      uniqueInitializer(config, localDB, dependencies);
    }

    const initializersForConfig = initializers[config.name];
    if (initializersForConfig) {
      initializersForConfig.forEach(initializer => {
        switch (initializer.type) {
          case 'rename':
            this._initWithRename(config, localDB, dependencies, initializer);
            break;
          case 'namespace':
            this._initAsNamespace(config, localDB, dependencies, initializer);
            break;
          case 'replace_namespace':
            this._initWithNamespace(config, localDB, dependencies, initializer);
            break;
          case 'host_with_port':
            this._initAsHostWithPort(config, localDB, dependencies, initializer);
            break;
          case 'hosts_with_port':
            this._initAsHostsWithPort(config, localDB, dependencies, initializer);
            break;
          case 'hosts_with_components':
            this._initAsHostsWithComponents(config, localDB, dependencies, initializer);
            break;
          case 'host_with_component':
            this._initAsHostWithComponent(config, localDB, dependencies, initializer);
            break;
          case 'hosts_list_with_component':
            this._initAsHostsListWithComponent(config, localDB, dependencies, initializer);
            break;
          default:
            break;
        }
      });
    }

    config.isOverridable = false;
    return config;
  }

  private _initWithRename(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies, initializer: Initializer): ConfigProperty {
    console.log("Local DB is",localDB)
    const replaceWith = dependencies.namespaceId;
    const toReplace:any = initializer.toReplace;
    if (!replaceWith) {
      throw new Error('`dependencies.namespaceId` should be not empty string');
    }
    config.name = config.name.replace(toReplace, replaceWith);
    config.displayName = config.displayName.replace(toReplace, replaceWith);
    return config;
  }

  private _initAsNamespace(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies, initializer: any): ConfigProperty {
    console.log("Local DB is",localDB)
    const value = dependencies.namespaceId;
    if (!value) {
      throw new Error('`dependencies.namespaceId` should be not empty string');
    }
    config.value = initializer.modifier.prefix + value + initializer.modifier.suffix;
    config.recommendedValue = config.value;
    return config;
  }

  private _initWithNamespace(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies, initializer: Initializer): ConfigProperty {
    console.log("Local DB is",localDB)
    const replaceWith = dependencies.namespaceId;
    const toReplace:any = initializer.toReplace;
    if (!replaceWith) {
      throw new Error('`dependencies.namespaceId` should be not empty string');
    }
    config.value = config.value.replace(toReplace, replaceWith);
    config.recommendedValue = config.recommendedValue.replace(toReplace, replaceWith);
    return config;
  }

  private _initAsHostWithPort(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: any, initializer: any): ConfigProperty {
    const hostName = localDB.masterComponentHosts.find(host => host.component === initializer.component && host.isInstalled === initializer.componentExists)?.hostName;
    const port = initializer.portKey ? dependencies[initializer.portKey] : initializer.port;
    config.value = initializer.modifier.prefix + (hostName || '') + (port ? ':' + port : '') + initializer.modifier.suffix;
    config.recommendedValue = config.value;
    return config;
  }

  private _initAsHostsWithPort(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: any, initializer: any): ConfigProperty {
    const hostNames = localDB.masterComponentHosts
      .filter(host => initializer.component.includes(host.component) && (initializer.componentExists === undefined || host.isInstalled === initializer.componentExists))
      .map(host => host.hostName);
    const port = initializer.portKey ? dependencies[initializer.portKey] : initializer.port;
    config.value = initializer.modifier.prefix + hostNames.map(host => host + (port ? ':' + port : '')).join(initializer.modifier.delimiter) + initializer.modifier.suffix;
    config.recommendedValue = config.value;
    return config;
  }

  private _initAsHostsWithComponents(config: any, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies, initializer: Initializer): ConfigProperty {
    console.log("Dependencies is",dependencies);
    const hostNames = localDB.masterComponentHosts
      .filter(host => initializer.components.includes(host.component) && (initializer.isInstalled === undefined || host.isInstalled === initializer.isInstalled))
      .map(host => host.hostName);
    config.value = initializer.asArray ? hostNames : hostNames.join(',');
    config.recommendedValue = config.value;
    return config;
  }

  private _initAsHostWithComponent(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies, initializer: any): ConfigProperty {
    console.log("Dependencies is",dependencies);
    const component = localDB.masterComponentHosts.find(host => host.component === initializer.component);
    if (!component) {
      return config;
    }
    if (initializer.modifier) {
      const replaceWith = initializer.modifier.prefix + component.hostName + initializer.modifier.suffix;
      config.value = config.value.replace(initializer.modifier.regex, replaceWith);
      config.recommendedValue = config.recommendedValue.replace(initializer.modifier.regex, replaceWith);
    } else {
      config.value = component.hostName;
      config.recommendedValue = component.hostName;
    }
    return config;
  }

  private _initAsHostsListWithComponent(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies, initializer: any): ConfigProperty {
    console.log("Dependencies is",dependencies);
    const hostNames = localDB.masterComponentHosts
      .filter(host => host.component === initializer.component && host.isInstalled === initializer.componentExists)
      .map(host => host.hostName)
      .join(initializer.modifier.delimiter);
    config.value = hostNames;
    config.recommendedValue = hostNames;
    return config;
  }

  private _initHbaseRootDir(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies): ConfigProperty {
    const fileName = config.filename;
    if (fileName === 'hbase-site') {
      return this._initHbaseRootDirForHbase(config, localDB, dependencies);
    }
    if (fileName === 'ams-hbase-site') {
      return this._initHbaseRootDirForAMS(config, localDB, dependencies);
    }
    return config;
  }

  private _initHbaseRootDirForHbase(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies): ConfigProperty {
    if (localDB.installedServices.includes('HBASE')) {
      const value = dependencies.serverConfigs.find(config => config.type === 'hbase-site')?.properties['hbase.rootdir'].replace(/\/\/[^\/]*/, '//' + dependencies.namespaceId);
      config.value = value || '';
      config.recommendedValue = value || '';
    }
    return config;
  }

  private _initHbaseRootDirForAMS(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies): ConfigProperty {
    if (localDB.installedServices.includes('AMBARI_METRICS')) {
      const value = dependencies.serverConfigs.find(config => config.type === 'ams-hbase-site')?.properties['hbase.rootdir'];
      const currentNameNodeHost = localDB.masterComponentHosts.find(host => host.component === 'NAMENODE' && host.isInstalled)?.hostName;
      if (value && value.includes('hdfs://' + currentNameNodeHost)) {
        config.value = value.replace(/\/\/[^\/]*/, '//' + dependencies.namespaceId);
        config.recommendedValue = config.value;
      }
      else{
        config.value=value as any,
        config.recommendedValue=value as any
      }
    }
    return config;
  }

  private _initHawqDfsUrl(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies): ConfigProperty {
    if (localDB.installedServices.includes('HAWQ')) {
      const value = dependencies.serverConfigs.find(config => config.type === 'hawq-site')?.properties['hawq_dfs_url'].replace(/(^.*:[0-9]+)(?=\/)/, dependencies.namespaceId);
      config.value = value || '';
      config.recommendedValue = value || '';
    }
    return config;
  }

  private _initInstanceVolumes(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies): ConfigProperty {
    if (localDB.installedServices.includes('ACCUMULO')) {
      const oldValue = dependencies.serverConfigs.find(config => config.type === 'accumulo-site')?.properties['instance.volumes'];
      const value = oldValue?.replace(/\/\/[^\/]*/, '//' + dependencies.namespaceId);
      config.value = value || '';
      config.recommendedValue = value || '';
    }
    return config;
  }

  private _initInstanceVolumesReplacements(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies): ConfigProperty {
    if (localDB.installedServices.includes('ACCUMULO')) {
      const oldValue = dependencies.serverConfigs.find(config => config.type === 'accumulo-site')?.properties['instance.volumes'];
      const value = oldValue?.replace(/\/\/[^\/]*/, '//' + dependencies.namespaceId);
      const replacements = oldValue + " " + value;
      config.value = replacements || '';
      config.recommendedValue = replacements || '';
    }
    return config;
  }

  private _initDfsJnEditsDir(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies): ConfigProperty {
    console.log("Dependencies is",dependencies,localDB);
    // if (localDB.installedServices.includes('HDFS')) {
    //   const value = dependencies.serverConfigs.find(config => config.type === 'hdfs-site')?.properties['dfs.journalnode.edits.dir'];
    //   config.value = value || '';
    //   config.recommendedValue = value || '';
    // }
    return config;
  }

  private _initXasecureAuditDestinationHdfsDir(config: ConfigProperty, localDB: ExtendedTopologyLocalDB, dependencies: NnHaConfigDependencies): ConfigProperty {
    if (localDB.installedServices.includes('RANGER')) {
      const oldValue = dependencies.serverConfigs.find(config => config.type === 'ranger-env')?.properties['xasecure.audit.destination.hdfs.dir'];
      const valueArray = oldValue?.split('/');
      if (valueArray && valueArray.length > 2) {
        valueArray[2] = dependencies.namespaceId;
        const newValue = valueArray.join('/');
        config.value = newValue;
        config.recommendedValue = newValue;
      }
    }
    return config;
  }
}

export default NnHaConfigInitializer;
