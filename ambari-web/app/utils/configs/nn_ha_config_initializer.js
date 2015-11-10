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

var App = require('app');
require('utils/configs/config_initializer_class');

/**
 * @typedef {topologyLocalDB} extendedTopologyLocalDB
 * @property {string[]} installedServices list of installed service names
 */

/**
 * Setting for <code>rename</code>-initializer
 * Used for configs which should be renamed
 * Replace some part if their names with <code>namespaceId</code> (provided by user on the wizard's 1st step)
 *
 * @param {string} toReplace
 * @returns {{type: string, toReplace: string}}
 */
function getRenameWithNamespaceConfig(toReplace) {
  return {
    type: 'rename',
    toReplace: toReplace
  };
}

/**
 * Settings for <code>host_with_port</code>-initializer
 * Used for configs with value equal to hostName where some component exists concatenated with port-value
 * Port-value is calculated according to <code>port</code> and <code>portFromDependencies</code> values
 * If <code>portFromDependencies</code> is <code>true</code>, <code>port</code>-value is used as key of the <code>dependencies</code> (where real port-value is)
 * Otherwise - <code>port</code>-value used as is
 * Value also may be customized with prefix and suffix
 *
 * @param {string} component needed component
 * @param {boolean} componentExists component already exists or just going to be installed
 * @param {string} prefix=''
 * @param {string} suffix=''
 * @param {string} port
 * @param {boolean} portFromDependencies=false
 * @returns {{type: string, component: string, componentExists: boolean, modifier: {prefix: (string), suffix: (string)}}}
 */
function getHostWithPortConfig(component, componentExists, prefix, suffix, port, portFromDependencies) {
  if (arguments.length < 6) {
    portFromDependencies = false;
  }
  prefix = prefix || '';
  suffix = suffix || '';
  var ret = {
    type: 'host_with_port',
    component: component,
    componentExists: componentExists,
    modifier: {
      prefix: prefix,
      suffix: suffix
    }
  };
  if (portFromDependencies) {
    ret.portKey = port;
  }
  else {
    ret.port = port;
  }
  return ret;
}

/**
 * Settings for <code>hosts_with_port</code>-initializer
 * Used for configs with value equal to the list of hostNames with port
 * Value also may be customized with prefix, suffix and delimiter between host:port elements
 * Port-value is calculated according to <code>port</code> and <code>portFromDependencies</code> values
 * If <code>portFromDependencies</code> is <code>true</code>, <code>port</code>-value is used as key of the <code>dependencies</code> (where real port-value is)
 * Otherwise - <code>port</code>-value used as is
 *
 * @param {string} component hosts where this component exists are used as config-value
 * @param {string} prefix='' substring added before hosts-list
 * @param {string} suffix='' substring added after hosts-list
 * @param {string} delimiter=',' delimiter between hosts in the value
 * @param {string} port if <code>portFromDependencies</code> is <code>false</code> this value is used as port for hosts
 * if <code>portFromDependencies</code> is <code>true</code> `port` is used as key in the <code>dependencies</code> to get real port-value
 * @param {boolean} portFromDependencies=false true - use <code>port</code> as key for <code>dependencies</code> to get real port-value,
 * false - use <code>port</code> as port-value
 * @returns {{type: string, component: string, modifier: {prefix: (string), suffix: (string), delimiter: (string)}}}
 */
function getHostsWithPortConfig(component, prefix, suffix, delimiter, port, portFromDependencies) {
  if (arguments.length < 6) {
    portFromDependencies = false;
  }
  prefix = prefix || '';
  suffix = suffix || '';
  delimiter = delimiter || ',';
  var ret = {
    type: 'hosts_with_port',
    component: component,
    modifier: {
      prefix: prefix,
      suffix: suffix,
      delimiter: delimiter
    }
  };
  if (portFromDependencies) {
    ret.portKey = port;
  }
  else {
    ret.port = port;
  }
  return ret;
}

/**
 * Settings for <code>namespace</code>-initializer
 * Used for configs with value equal to the <code>namespaceId</code> (provided by user on the wizard's 1st step)
 * Value may be customized with prefix and suffix
 *
 * @param {string} [prefix=''] substring added before namespace in the replace
 * @param {string} [suffix=''] substring added after namespace in the replace
 * @returns {{type: string, modifier: {prefix: (string), suffix: (string)}}}
 */
function getNamespaceConfig (prefix, suffix) {
  prefix = prefix || '';
  suffix = suffix || '';
  return {
    type: 'namespace',
    modifier: {
      prefix: prefix,
      suffix: suffix
    }
  }
}

/**
 * Settings for <code>replace_namespace</code>
 * Used for configs with values that have to be modified with replacing some part of them
 * to the <code>namespaceId</code> (provided by user on the wizard's 1st step)
 *
 * @param {string} toReplace
 * @returns {{type: string, toReplace: *}}
 */
function getReplaceNamespaceConfig(toReplace) {
  return {
    type: 'replace_namespace',
    toReplace: toReplace
  };
}

/**
 *
 * @class {NnHaConfigInitializer}
 */
App.NnHaConfigInitializer = App.ConfigInitializerClass.create({

  initializers: {
    'dfs.ha.namenodes.${dfs.nameservices}': getRenameWithNamespaceConfig('${dfs.nameservices}'),
    'dfs.namenode.rpc-address.${dfs.nameservices}.nn1': [
      getHostWithPortConfig('NAMENODE', true, '', '', 'nnRpcPort', true),
      getRenameWithNamespaceConfig('${dfs.nameservices}')
    ],
    'dfs.namenode.rpc-address.${dfs.nameservices}.nn2': [
      getHostWithPortConfig('NAMENODE', false, '', '', '8020', false),
      getRenameWithNamespaceConfig('${dfs.nameservices}')
    ],
    'dfs.namenode.http-address.${dfs.nameservices}.nn1': [
      getHostWithPortConfig('NAMENODE', true, '', '', 'nnHttpPort', true),
      getRenameWithNamespaceConfig('${dfs.nameservices}')
    ],
    'dfs.namenode.http-address.${dfs.nameservices}.nn2': [
      getHostWithPortConfig('NAMENODE', false, '', '', '50070', false),
      getRenameWithNamespaceConfig('${dfs.nameservices}')
    ],
    'dfs.namenode.https-address.${dfs.nameservices}.nn1': [
      getHostWithPortConfig('NAMENODE', true, '', '', 'nnHttpsPort', true),
      getRenameWithNamespaceConfig('${dfs.nameservices}')
    ],
    'dfs.namenode.https-address.${dfs.nameservices}.nn2': [
      getHostWithPortConfig('NAMENODE', false, '', '', '50470', false),
      getRenameWithNamespaceConfig('${dfs.nameservices}')
    ],
    'dfs.client.failover.proxy.provider.${dfs.nameservices}': getRenameWithNamespaceConfig('${dfs.nameservices}'),
    'dfs.nameservices': getNamespaceConfig(),
    'fs.defaultFS': getNamespaceConfig('hdfs://'),
    'dfs.namenode.shared.edits.dir': [
      getHostsWithPortConfig('JOURNALNODE', 'qjournal://', '/${dfs.nameservices}', ';', '8485', false),
      getReplaceNamespaceConfig('${dfs.nameservices}')
    ],
    'ha.zookeeper.quorum': getHostsWithPortConfig('ZOOKEEPER_SERVER', '', '', ',', 'zkClientPort', true)
  },

  uniqueInitializers: {
    'hbase.rootdir': '_initHbaseRootDir',
    'instance.volumes': '_initInstanceVolumes',
    'instance.volumes.replacements': '_initInstanceVolumesReplacements',
    'dfs.journalnode.edits.dir': '_initDfsJnEditsDir'
  },

  initializerTypes: {
    rename: {
      method: '_initWithRename'
    },
    host_with_port: {
      method: '_initAsHostWithPort'
    },
    hosts_with_port: {
      method: '_initAsHostsWithPort'
    },
    namespace: {
      method: '_initAsNamespace'
    },
    replace_namespace: {
      method: '_initWithNamespace'
    }
  },

  /**
   * Initializer for configs that should be renamed
   * Some part of their names should be replaced with <code>namespaceId</code> (user input this value on the wizard's 1st step)
   * Affects both - name and displayName
   *
   * @param {object} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initWithRename
   */
  _initWithRename: function (configProperty, localDB, dependencies, initializer) {
    var replaceWith = dependencies.namespaceId;
    var toReplace = initializer.toReplace;
    Em.assert('`dependencies.namespaceId` should be not empty string', !!replaceWith);
    var name = Em.getWithDefault(configProperty, 'name', '');
    var displayName = Em.getWithDefault(configProperty, 'displayName', '');
    name = name.replace(toReplace, replaceWith);
    displayName = displayName.replace(toReplace, replaceWith);
    Em.setProperties(configProperty, {
      name: name,
      displayName: displayName
    });
    return configProperty;
  },

  /**
   * Initializer for configs wih value equal to the <code>namespaceId</code> (user input this value on the wizard's 1st step)
   * Value may be customized with prefix and suffix (see <code>initializer.modifier</code>)
   * Value-examples: 'SOME_COOL_PREFIXmy_namespaceSOME_COOL_SUFFIX', 'my_namespace'
   *
   * @param {object} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initAsNamespace
   */
  _initAsNamespace: function (configProperty, localDB, dependencies, initializer) {
    var value = dependencies.namespaceId;
    Em.assert('`dependencies.namespaceId` should be not empty string', !!value);
    value = initializer.modifier.prefix + value + initializer.modifier.suffix;
    Em.setProperties(configProperty, {
      value: value,
      recommendedValue: value
    });
    return configProperty;
  },

  /**
   * Initializer for configs with value that should be modified with replacing some substring
   * to the <code>namespaceId</code> (user input this value on the wizard's 1st step)
   *
   * @param {object} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initWithNamespace
   */
  _initWithNamespace: function (configProperty, localDB, dependencies, initializer) {
    var replaceWith = dependencies.namespaceId;
    var toReplace = initializer.toReplace;
    Em.assert('`dependencies.namespaceId` should be not empty string', !!replaceWith);
    var value = Em.get(configProperty, 'value').replace(toReplace, replaceWith);
    var recommendedValue = Em.get(configProperty, 'recommendedValue').replace(toReplace, replaceWith);
    Em.setProperties(configProperty, {
      value: value,
      recommendedValue: recommendedValue
    });
    return configProperty;
  },

  /**
   * Initializer for configs with value equal to the hostName where some component exists
   * Value may be customized with prefix and suffix (see <code>initializer.modifier</code>)
   * Port-value is calculated according to <code>initializer.portKey</code> or <code>initializer.port</code> values
   * Value-examples: 'SOME_COOL_PREFIXhost1:port1SOME_COOL_SUFFIX', 'host1:port2'
   *
   * @param {object} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initAsHostWithPort
   */
  _initAsHostWithPort: function (configProperty, localDB, dependencies, initializer) {
    var hostName = localDB.masterComponentHosts.filterProperty('component', initializer.component).findProperty('isInstalled', initializer.componentExists).hostName;
    var port = this.__getPort(dependencies, initializer);
    var value = initializer.modifier.prefix + hostName + ':' + port + initializer.modifier.suffix;
    Em.setProperties(configProperty, {
      value: value,
      recommendedValue: value
    });
    return configProperty;
  },

  /**
   * Initializer for configs with value equal to the list of hosts where some component exists
   * Value may be customized with prefix and suffix (see <code>initializer.modifier</code>)
   * Delimiter between hostNames also may be customized in the <code>initializer.modifier</code>
   * Port-value is calculated according to <code>initializer.portKey</code> or <code>initializer.port</code> values
   * Value examples: 'SOME_COOL_PREFIXhost1:port,host2:port,host2:portSOME_COOL_SUFFIX', 'host1:port|||host2:port|||host2:port'
   *
   * @param {object} configProperty
   * @param {topologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initAsHostsWithPort
   */
  _initAsHostsWithPort: function (configProperty, localDB, dependencies, initializer) {
    var hostNames = localDB.masterComponentHosts.filterProperty('component', initializer.component).mapProperty('hostName');
    var port = this.__getPort(dependencies, initializer);
    var value = initializer.modifier.prefix + hostNames.map(function (hostName) {
      return hostName + ':' + port;
    }).join(initializer.modifier.delimiter) + initializer.modifier.suffix;
    Em.setProperties(configProperty, {
      value: value,
      recommendedValue: value
    });
    return configProperty;
  },

  /**
   * Returns port-value from <code>dependencies</code> accorfing to <code>initializer.portKey</code> or <code>initializer.port</code> values
   *
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @returns {string|number}
   * @private
   * @method __getPort
   */
  __getPort: function (dependencies, initializer) {
    var portKey = initializer.portKey;
    if (portKey) {
      return  dependencies[portKey];
    }
    return initializer.port;
  },

  /**
   * Unique initializer for <code>hbase.rootdir</code>
   *
   * @param {object} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initHbaseRootDir
   * @return {object}
   * @private
   */
  _initHbaseRootDir: function (configProperty, localDB, dependencies, initializer) {
    var fileName = Em.get(configProperty, 'filename');
    var args = [].slice.call(arguments);
    if ('hbase-site' === fileName) {
      return this._initHbaseRootDirForHbase.apply(this, args);
    }
    if('ams-hbase-site' === fileName) {
      return this._initHbaseRootDirForAMS.apply(this, args);
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>hbase.rootdir</code> (HBASE-service)
   *
   * @param {object} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initHbaseRootDirForHbase
   * @return {object}
   * @private
   */
  _initHbaseRootDirForHbase: function (configProperty, localDB, dependencies, initializer) {
    if (localDB.installedServices.contains('HBASE')) {
      var value = dependencies.serverConfigs.findProperty('type', 'hbase-site').properties['hbase.rootdir'].replace(/\/\/[^\/]*/, '//' + dependencies.namespaceId);
      Em.setProperties(configProperty, {
        value: value,
        recommendedValue: value
      });
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>hbase.rootdir</code> (Ambari Metrics-service)
   *
   * @param {object} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initHbaseRootDirForAMS
   * @return {object}
   * @private
   */
  _initHbaseRootDirForAMS: function (configProperty, localDB, dependencies, initializer) {
    if (localDB.installedServices.contains('AMBARI_METRICS')) {
      var value = dependencies.serverConfigs.findProperty('type', 'ams-hbase-site').properties['hbase.rootdir'];
      var currentNameNodeHost = localDB.masterComponentHosts.filterProperty('component', 'NAMENODE').findProperty('isInstalled', true).hostName;
      value = (value == "hdfs://" + currentNameNodeHost) ? "hdfs://" + dependencies.namespaceId : value;
      configProperty.isVisible = configProperty.value != value;
      Em.setProperties(configProperty, {
        value: value,
        recommendedValue: value
      });
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>instance.volumes</code>
   *
   * @param {object} configProperty
   * @param {topologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initInstanceVolumes
   * @return {object}
   * @private
   */
  _initInstanceVolumes: function (configProperty, localDB, dependencies, initializer) {
    if (localDB.installedServices.contains('ACCUMULO')) {
      var oldValue = dependencies.serverConfigs.findProperty('type', 'accumulo-site').properties['instance.volumes'];
      var value = oldValue.replace(/\/\/[^\/]*/, '//' + dependencies.namespaceId);
      Em.setProperties(configProperty, {
        value: value,
        recommendedValue: value
      });
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>instance.volumes.replacements</code>
   *
   * @param {object} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initInstanceVolumesReplacements
   * @return {object}
   * @private
   */
  _initInstanceVolumesReplacements: function (configProperty, localDB, dependencies, initializer) {
    if (localDB.installedServices.contains('ACCUMULO')) {
      var oldValue = dependencies.serverConfigs.findProperty('type', 'accumulo-site').properties['instance.volumes'];
      var value = oldValue.replace(/\/\/[^\/]*/, '//' + dependencies.namespaceId);
      var replacements = oldValue + " " + value;
      Em.setProperties(configProperty, {
        value: replacements,
        recommendedValue: replacements
      });
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>dfs.journalnode.edits.dir</code>
   * Used only for Windows Stacks
   *
   * @param {object} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {nnHaConfigDependencies} dependencies
   * @param {object} initializer
   * @method _initDfsJnEditsDir
   * @return {object}
   * @private
   */
  _initDfsJnEditsDir: function (configProperty, localDB, dependencies, initializer) {
    if (App.get('isHadoopWindowsStack') && localDB.installedServices.contains('HDFS')) {
      var value = dependencies.serverConfigs.findProperty('type', 'hdfs-site').properties['dfs.journalnode.edits.dir'];
      Em.setProperties(configProperty, {
        value: value,
        recommendedValue: value
      });
    }
    return configProperty;
  }

});