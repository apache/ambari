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
 * Regexp for host with port ('hostName:1234')
 *
 * @type {string}
 */
var hostWithPort = "([\\w|\\.]*)(?=:)";

/**
 * Regexp for host with port and protocol ('://hostName:1234')
 *
 * @type {string}
 */
var hostWithPrefix = ":\/\/" + hostWithPort;

/**
 * Regexp used to determine if mount point is windows-like
 *
 * @type {RegExp}
 */
var winRegex = /^([a-z]):\\?$/;

/**
 * Settings for <code>host_with_component</code>-initializer
 * Used for configs with value equal to hostName that has <code>component</code>
 * Value may be modified with if <code>withModifier</code> is true (it is by default)
 * <code>hostWithPort</code>-regexp will be used in this case
 *
 * @see _initAsHostWithComponent
 * @param {string} component
 * @param {boolean} [withModifier=true]
 * @return {object}
 */
function getSimpleComponentConfig(component, withModifier) {
  if (arguments.length === 1) {
    withModifier = true;
  }
  var config = {
    type: 'host_with_component',
    component: component
  };
  if (withModifier) {
    config.modifier = {
      type: 'regexp',
      regex: hostWithPort
    }
  }
  return config;
}

/**
 * Zookeeper-based configs don't have any customization settings
 *
 * @see _initAsZookeeperServersList
 * @returns {{type: string}}
 */
function getZKBasedConfig() {
  return {
    type: 'zookeeper_based'
  };
}

/**
 * Almost the same to <code>getSimpleComponentConfig</code>, but with possibility to modify <code>replaceWith</code>-value
 * <code>prefix</code> is added before it
 * <code>suffix</code> is added after it
 * <code>hostWithPrefix</code>-regexp is used
 *
 * @see _initAsHostWithComponent
 * @param {string} component
 * @param {string} [prefix]
 * @param {string} [suffix]
 * @returns {object}
 */
function getComponentConfigWithAffixes (component, prefix, suffix) {
  prefix = prefix || '';
  suffix = suffix || '';
  return {
    type: 'host_with_component',
    component: component,
    modifier: {
      type: 'regexp',
      regex: hostWithPrefix,
      prefix: prefix,
      suffix: suffix
    }
  };
}

/**
 * Settings for <code>hosts_with_components</code>-initializer
 * Used for configs with value equal to the hosts list
 * May set value as array (if <code>asArray</code> is true) or as comma-sepratated string (if <code>asArray</code> is false)
 *
 * @see _initAsHostsWithComponents
 * @param {string|string[]} components
 * @param {boolean} [asArray=false]
 * @returns {{type: string, components: string[], asArray: boolean}}
 */
function getComponentsHostsConfig(components, asArray) {
  if (1 === arguments.length) {
    asArray = false;
  }
  return {
    type: 'hosts_with_components',
    components: Em.makeArray(components),
    asArray: asArray
  };
}

/**
 * Settings for <code>single_mountpoint</code>-initializer
 * Used for configs with value as one of the possible mount points
 *
 * @see _initAsSingleMountPoint
 * @param {string|string[]} components
 * @param {string} winReplacer
 * @returns {{components: string[], winReplacer: string, type: string}}
 */
function getSingleMountPointConfig(components, winReplacer) {
  winReplacer = winReplacer || 'default';
  return {
    components: Em.makeArray(components),
    winReplacer: winReplacer,
    type: 'single_mountpoint'
  }
}

/**
 * Settings for <code>multiple_mountpoints</code>-initializer
 * Used for configs with value as all of the possible mount points
 *
 * @see _initAsMultipleMountPoints
 * @param {string|string[]} components
 * @param {string} winReplacer
 * @returns {{components: string[], winReplacer: string, type: string}}
 */
function getMultipleMountPointsConfig(components, winReplacer) {
  winReplacer = winReplacer || 'default';
  return {
    components: Em.makeArray(components),
    winReplacer: winReplacer,
    type: 'multiple_mountpoints'
  }
}

/**
 * Initializer for configs
 * Used on the cluster install
 *
 * Usage:
 * <pre>
 *   var configProperty = Object.create({});
 *   var localDB = {
 *    hosts: [],
 *    masterComponentHosts: [],
 *    slaveComponentHosts: []
 *   };
 *   var dependencies = {};
 *   App.ConfigInitializer.initialValue(configProperty, localDB, dependencies);
 * </pre>
 *
 * @instance ConfigInitializer
 */
App.ConfigInitializer = App.ConfigInitializerClass.create({

  initializers: {
    'dfs.namenode.rpc-address': getSimpleComponentConfig('NAMENODE'),
    'dfs.http.address': getSimpleComponentConfig('NAMENODE'),
    'dfs.namenode.http-address': getSimpleComponentConfig('NAMENODE'),
    'dfs.https.address': getSimpleComponentConfig('NAMENODE'),
    'dfs.namenode.https-address': getSimpleComponentConfig('NAMENODE'),
    'dfs.secondary.http.address': getSimpleComponentConfig('SECONDARY_NAMENODE'),
    'dfs.namenode.secondary.http-address': getSimpleComponentConfig('SECONDARY_NAMENODE'),
    'yarn.resourcemanager.hostname': getSimpleComponentConfig('RESOURCEMANAGER', false),
    'yarn.resourcemanager.resource-tracker.address': getSimpleComponentConfig('RESOURCEMANAGER'),
    'yarn.resourcemanager.webapp.https.address': getSimpleComponentConfig('RESOURCEMANAGER'),
    'yarn.resourcemanager.webapp.address': getSimpleComponentConfig('RESOURCEMANAGER'),
    'yarn.resourcemanager.scheduler.address': getSimpleComponentConfig('RESOURCEMANAGER'),
    'yarn.resourcemanager.address': getSimpleComponentConfig('RESOURCEMANAGER'),
    'yarn.resourcemanager.admin.address': getSimpleComponentConfig('RESOURCEMANAGER'),
    'yarn.timeline-service.webapp.address': getSimpleComponentConfig('APP_TIMELINE_SERVER'),
    'yarn.timeline-service.webapp.https.address': getSimpleComponentConfig('APP_TIMELINE_SERVER'),
    'yarn.timeline-service.address': getSimpleComponentConfig('APP_TIMELINE_SERVER'),
    'mapred.job.tracker': getSimpleComponentConfig('JOBTRACKER'),
    'mapred.job.tracker.http.address': getSimpleComponentConfig('JOBTRACKER'),
    'mapreduce.history.server.http.address': getSimpleComponentConfig('HISTORYSERVER'),
    'hive_hostname': getSimpleComponentConfig('HIVE_SERVER', false),
    'oozie_hostname': getSimpleComponentConfig('OOZIE_SERVER', false),
    'oozie.base.url': getComponentConfigWithAffixes('OOZIE_SERVER', '://'),
    'hawq_dfs_url': getSimpleComponentConfig('NAMENODE'),
    'hawq_rm_yarn_address': getSimpleComponentConfig('RESOURCEMANAGER'),
    'hawq_rm_yarn_scheduler_address': getSimpleComponentConfig('RESOURCEMANAGER'),
    'fs.default.name': getComponentConfigWithAffixes('NAMENODE', '://'),
    'fs.defaultFS': getComponentConfigWithAffixes('NAMENODE', '://'),
    'hbase.rootdir': getComponentConfigWithAffixes('NAMENODE', '://'),
    'instance.volumes': getComponentConfigWithAffixes('NAMENODE', '://'),
    'yarn.log.server.url': getComponentConfigWithAffixes('HISTORYSERVER', '://'),
    'mapreduce.jobhistory.webapp.address': getSimpleComponentConfig('HISTORYSERVER'),
    'mapreduce.jobhistory.address': getSimpleComponentConfig('HISTORYSERVER'),
    'kafka.ganglia.metrics.host': getSimpleComponentConfig('GANGLIA_SERVER', false),
    'hive_master_hosts': getComponentsHostsConfig(['HIVE_METASTORE', 'HIVE_SERVER']),
    'hadoop_host': getSimpleComponentConfig('NAMENODE', false),
    'nimbus.host': getSimpleComponentConfig('NIMBUS', false),
    'nimbus.seeds': getComponentsHostsConfig('NIMBUS', true),
    'storm.zookeeper.servers': getComponentsHostsConfig('ZOOKEEPER_SERVER', true),
    'hawq_master_address_host': getSimpleComponentConfig('HAWQMASTER', false),
    'hawq_standby_address_host': getSimpleComponentConfig('HAWQSTANDBY', false),

    '*.broker.url': {
      type: 'host_with_component',
      component: 'FALCON_SERVER',
      modifier: {
        type: 'regexp',
        regex: 'localhost'
      }
    },

    'zookeeper.connect': getZKBasedConfig(),
    'hive.zookeeper.quorum': getZKBasedConfig(),
    'templeton.zookeeper.hosts': getZKBasedConfig(),
    'hadoop.registry.zk.quorum': getZKBasedConfig(),
    'hive.cluster.delegation.token.store.zookeeper.connectString': getZKBasedConfig(),
    'instance.zookeeper.host': getZKBasedConfig(),

    'dfs.name.dir': getMultipleMountPointsConfig('NAMENODE', 'file'),
    'dfs.namenode.name.dir': getMultipleMountPointsConfig('NAMENODE', 'file'),
    'dfs.data.dir': getMultipleMountPointsConfig('DATANODE', 'file'),
    'dfs.datanode.data.dir': getMultipleMountPointsConfig('DATANODE', 'file'),
    'yarn.nodemanager.local-dirs': getMultipleMountPointsConfig('NODEMANAGER'),
    'yarn.nodemanager.log-dirs': getMultipleMountPointsConfig('NODEMANAGER'),
    'mapred.local.dir': getMultipleMountPointsConfig(['TASKTRACKER', 'NODEMANAGER']),
    'log.dirs': getMultipleMountPointsConfig('KAFKA_BROKER'),

    'fs.checkpoint.dir': getSingleMountPointConfig('SECONDARY_NAMENODE', 'file'),
    'dfs.namenode.checkpoint.dir': getSingleMountPointConfig('SECONDARY_NAMENODE', 'file'),
    'yarn.timeline-service.leveldb-timeline-store.path': getSingleMountPointConfig('APP_TIMELINE_SERVER'),
    'yarn.timeline-service.leveldb-state-store.path': getSingleMountPointConfig('APP_TIMELINE_SERVER'),
    'dataDir': getSingleMountPointConfig('ZOOKEEPER_SERVER'),
    'oozie_data_dir': getSingleMountPointConfig('OOZIE_SERVER'),
    'storm.local.dir': getSingleMountPointConfig(['NODEMANAGER', 'NIMBUS']),
    '*.falcon.graph.storage.directory': getSingleMountPointConfig('FALCON_SERVER'),
    '*.falcon.graph.serialize.path': getSingleMountPointConfig('FALCON_SERVER')
  },

  uniqueInitializers: {
    'hive_database': '_initHiveDatabaseValue',
    'templeton.hive.properties': '_initTempletonHiveProperties',
    'hbase.zookeeper.quorum': '_initHBaseZookeeperQuorum',
    'yarn.resourcemanager.zk-address': '_initYarnRMzkAddress',
    'RANGER_HOST': '_initRangerHost',
    'hive.metastore.uris': '_initHiveMetastoreUris'
  },

  initializerTypes: [
    {name: 'host_with_component', method: '_initAsHostWithComponent'},
    {name: 'hosts_with_components', method: '_initAsHostsWithComponents'},
    {name: 'zookeeper_based', method: '_initAsZookeeperServersList'},
    {name: 'single_mountpoint', method: '_initAsSingleMountPoint'},
    {name: 'multiple_mountpoints', method: '_initAsMultipleMountPoints'}
  ],

  /**
   * Map for methods used as value-modifiers for configProperties with values as mount point(s)
   * Used if mount point is win-like (@see winRegex)
   * Key: id
   * Value: method-name
   *
   * @type {{default: string, file: string, slashes: string}}
   */
  winReplacersMap: {
    default: '_defaultWinReplace',
    file: '_winReplaceWithFile',
    slashes: '_defaultWinReplaceWithAdditionalSlashes'
  },

  /**
   * Initializer for configs with value equal to hostName with needed component
   * Value example: 'hostName'
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @param {object} initializer
   * @returns {Object}
   * @private
   */
  _initAsHostWithComponent: function (configProperty, localDB, dependencies, initializer) {
    var component = localDB.masterComponentHosts.findProperty('component', initializer.component);
    if (!component) {
      return configProperty;
    }
    if (initializer.modifier) {
      var replaceWith = Em.getWithDefault(initializer.modifier, 'prefix', '')
        + component.hostName
        + Em.getWithDefault(initializer.modifier, 'suffix', '');
      this.setRecommendedValue(configProperty, initializer.modifier.regex, replaceWith);
    }
    else {
      Em.setProperties(configProperty, {
        recommendedValue: component.hostName,
        value: component.hostName
      })
    }

    return configProperty;
  },

  /**
   * Initializer for configs with value equal to hostNames with needed components
   * May be array or comma-separated list
   * Depends on <code>initializer.asArray</code> (true - array, false - string)
   * Value example: 'hostName1,hostName2,hostName3' or ['hostName1', 'hostName2', 'hostName3']
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @param {object} initializer
   * @return {Object}
   * @private
   */
  _initAsHostsWithComponents: function (configProperty, localDB, dependencies, initializer) {
    var hostNames = localDB.masterComponentHosts.filter(function (masterComponent) {
      return initializer.components.contains(masterComponent.component);
    }).mapProperty('hostName');
    if (!initializer.asArray) {
      hostNames = hostNames.uniq().join(',');
    }
    Em.setProperties(configProperty, {
      value: hostNames,
      recommendedValue: hostNames
    });
    return configProperty;
  },

  /**
   * Unique initializer for <code>hive_database</code>-config
   *
   * @param {configProperty} configProperty
   * @returns {Object}
   * @private
   */
  _initHiveDatabaseValue: function (configProperty) {
    var newMySQLDBOption = Em.get(configProperty, 'options').findProperty('displayName', 'New MySQL Database');
    if (newMySQLDBOption) {
      var isNewMySQLDBOptionHidden = !App.get('supports.alwaysEnableManagedMySQLForHive') && App.get('router.currentState.name') != 'configs' &&
        !App.get('isManagedMySQLForHiveEnabled');
      if (isNewMySQLDBOptionHidden && Em.get(configProperty, 'value') == 'New MySQL Database') {
        Em.set(configProperty, 'value', 'Existing MySQL Database');
      }
      Em.set(newMySQLDBOption, 'hidden', isNewMySQLDBOptionHidden);
    }
    return configProperty;
  },

  /**
   * Initializer for configs with value equal to hostNames-list where ZOOKEEPER_SERVER is installed
   * Value example: 'host1:2020,host2:2020,host3:2020'
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @returns {Object}
   * @private
   */
  _initAsZookeeperServersList: function (configProperty, localDB) {
    var zkHosts = localDB.masterComponentHosts.filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName');
    var zkHostPort = zkHosts;
    var regex = '\\w*:(\\d+)';   //regex to fetch the port
    var portValue = Em.get(configProperty, 'recommendedValue') && Em.get(configProperty, 'recommendedValue').match(new RegExp(regex));
    if (!portValue) {
      return configProperty;
    }
    if (portValue[1]) {
      for ( var i = 0; i < zkHosts.length; i++ ) {
        zkHostPort[i] = zkHosts[i] + ':' + portValue[1];
      }
    }
    this.setRecommendedValue(configProperty, '(.*)', zkHostPort);
    return configProperty;
  },

  /**
   * Unique initializer for <code>templeton.hive.properties</code>
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @returns {Object}
   * @private
   */
  _initTempletonHiveProperties: function (configProperty, localDB, dependencies) {
    var hiveMSUris = this.getHiveMetastoreUris(localDB.masterComponentHosts, dependencies['hive.metastore.uris']).replace(',', '\\,');
    if (/\/\/localhost:/g.test(Em.get(configProperty, 'value'))) {
      Em.set(configProperty, 'recommendedValue', Em.get(configProperty, 'value') + ',hive.metastore.execute.setugi=true');
    }
    this.setRecommendedValue(configProperty, "(hive\\.metastore\\.uris=)([^\\,]+)", "$1" + hiveMSUris);
    return configProperty;
  },

  /**
   * Unique initializer for <code>hbase.zookeeper.quorum</code>
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @returns {Object}
   * @private
   */
  _initHBaseZookeeperQuorum: function (configProperty, localDB) {
    if ('hbase-site.xml' === Em.get(configProperty, 'filename')) {
      var zkHosts = localDB.masterComponentHosts.filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName');
      this.setRecommendedValue(configProperty, "(.*)", zkHosts);
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>RANGER_HOST</code>
   * If RANGER_ADMIN-component isn't installed, this config becomes unneeded (isVisible - false, isRequired - false)
   * Value example: 'hostName'
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @returns {Object}
   * @private
   */
  _initRangerHost: function (configProperty, localDB) {
    var rangerAdminHost = localDB.masterComponentHosts.findProperty('component', 'RANGER_ADMIN');
    if(rangerAdminHost) {
      Em.setProperties(configProperty, {
        value: rangerAdminHost.hostName,
        recommendedValue: rangerAdminHost.hostName
      });
    }
    else {
      Em.setProperties(configProperty, {
        isVisible: 'false',
        isRequired: 'false'
      });
    }
    return configProperty;
  },

  /**
   * Unique initializer for <code>yarn.resourcemanager.zk-address</code>
   * List of hosts where ZOOKEEPER_SERVER is installed
   * Port is taken from <code>dependencies.clientPort</code>
   * Value example: 'host1:111,host2:111,host3:111'
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @returns {Object}
   * @private
   */
  _initYarnRMzkAddress: function (configProperty, localDB, dependencies) {
    var value = localDB.masterComponentHosts.filterProperty('component', 'ZOOKEEPER_SERVER').map(function (component) {
      return component.hostName + ':' + dependencies.clientPort
    }).join(',');
    Em.setProperties(configProperty, {
      value: value,
      recommendedValue: value
    });
    return configProperty;
  },

  /**
   * Unique initializer for <code>hive.metastore.uris</code>
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @returns {Object}
   * @private
   */
  _initHiveMetastoreUris: function (configProperty, localDB, dependencies) {
    var hiveMSUris = this.getHiveMetastoreUris(localDB.masterComponentHosts, dependencies['hive.metastore.uris']);
    if (hiveMSUris) {
      this.setRecommendedValue(configProperty, "(.*)", hiveMSUris);
    }
    return configProperty;
  },

  /**
   * Get hive.metastore.uris initial value
   *
   * @param {object[]} hosts
   * @param {string} recommendedValue
   * @returns {string}
   */
  getHiveMetastoreUris: function (hosts, recommendedValue) {
    var hiveMSHosts = hosts.filterProperty('component', 'HIVE_METASTORE').mapProperty('hostName'),
      hiveMSUris = hiveMSHosts,
      regex = "\\w*:(\\d+)",
      portValue = recommendedValue && recommendedValue.match(new RegExp(regex));

    if (!portValue) {
      return '';
    }
    if (portValue[1]) {
      for (var i = 0; i < hiveMSHosts.length; i++) {
        hiveMSUris[i] = "thrift://" + hiveMSHosts[i] + ":" + portValue[1];
      }
    }
    return hiveMSUris.join(',');
  },

  /**
   * Set <code>value</code> and <code>recommendedValue</code> for <code>configProperty</code>
   * basing on <code>recommendedValue</code> with replacing <code>regex</code> for <code>replaceWith</code>
   *
   * @param {configProperty} configProperty
   * @param {string} regex
   * @param {string} replaceWith
   * @return {Object}
   */
  setRecommendedValue: function (configProperty, regex, replaceWith) {
    var recommendedValue = Em.get(configProperty, 'recommendedValue');
    recommendedValue = Em.isNone(recommendedValue) ? '' : recommendedValue;
    var re = new RegExp(regex);
    recommendedValue = recommendedValue.replace(re, replaceWith);
    Em.set(configProperty, 'recommendedValue', recommendedValue);
    Em.set(configProperty, 'value', Em.isNone(Em.get(configProperty, 'recommendedValue')) ? '' : recommendedValue);
    return configProperty;
  },

  /**
   * Initializer for configs with value as one of the possible mount points
   * Only hosts that contains on the components from <code>initializer.components</code> are processed
   * Hosts with Windows needs additional processing (@see winReplacersMap)
   * Value example: '/', '/some/cool/dir'
   *
   * @param {configProperty} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @param {object} initializer
   * @return {Object}
   */
  _initAsSingleMountPoint: function (configProperty, localDB, dependencies, initializer) {
    var hostsInfo = this._updateHostInfo(localDB.hosts);
    var setOfHostNames = this._getSetOfHostNames(localDB, initializer);
    var winReplacersMap = this.get('winReplacersMap');
    // In Add Host Wizard, if we did not select this slave component for any host, then we don't process any further.
    if (!setOfHostNames.length) {
      return configProperty;
    }
    var allMountPoints = this._getAllMountPoints(setOfHostNames, hostsInfo);

    var mPoint = allMountPoints[0].mountpoint;
    if (mPoint === "/") {
      mPoint = Em.get(configProperty, 'recommendedValue');
    }
    else {
      var mp = mPoint.toLowerCase();
      if (winRegex.test(mp)) {
        var methodName = winReplacersMap[initializer.winReplacer];
        mPoint = this[methodName].call(this, configProperty, mp);
      }
      else {
        mPoint = mPoint + Em.get(configProperty, 'recommendedValue');
      }
    }
    Em.setProperties(configProperty, {
      value: mPoint,
      recommendedValue: mPoint
    });

    return configProperty;
  },

  /**
   * Initializer for configs with value as all of the possible mount points
   * Only hosts that contains on the components from <code>initializer.components</code> are processed
   * Hosts with Windows needs additional processing (@see winReplacersMap)
   * Value example: '/\n/some/cool/dir' (`\n` - is divider)
   *
   * @param {Object} configProperty
   * @param {topologyLocalDB} localDB
   * @param {object} dependencies
   * @param {object} initializer
   * @return {Object}
   */
  _initAsMultipleMountPoints: function (configProperty, localDB, dependencies, initializer) {
    var hostsInfo = this._updateHostInfo(localDB.hosts);
    var self = this;
    var setOfHostNames = this._getSetOfHostNames(localDB, initializer);
    var winReplacersMap = this.get('winReplacersMap');
    // In Add Host Wizard, if we did not select this slave component for any host, then we don't process any further.
    if (!setOfHostNames.length) {
      return configProperty;
    }

    var allMountPoints = this._getAllMountPoints(setOfHostNames, hostsInfo);
    var mPoint = '';

    allMountPoints.forEach(function (eachDrive) {
      if (eachDrive.mountpoint === '/') {
        mPoint += Em.get(configProperty, 'recommendedValue') + "\n";
      }
      else {
        var mp = eachDrive.mountpoint.toLowerCase();
        if (winRegex.test(mp)) {
          var methodName = winReplacersMap[initializer.winReplacer];
          mPoint += self[methodName].call(this, configProperty, mp);
        }
        else {
          mPoint += eachDrive.mountpoint + Em.get(configProperty, 'recommendedValue') + "\n";
        }
      }
    }, this);

    Em.setProperties(configProperty, {
      value: mPoint,
      recommendedValue: mPoint
    });

    return configProperty;
  },

  /**
   * Replace drive-based windows-path with 'file:///'
   *
   * @param {configProperty} configProperty
   * @param {string} mountPoint
   * @returns {string}
   * @private
   */
  _winReplaceWithFile: function (configProperty, mountPoint) {
    var winDriveUrl = mountPoint.toLowerCase().replace(winRegex, 'file:///$1:');
    return winDriveUrl + Em.get(configProperty, 'recommendedValue') + '\n';
  },

  /**
   * Replace drive-based windows-path
   *
   * @param {configProperty} configProperty
   * @param {string} mountPoint
   * @returns {string}
   * @private
   */
  _defaultWinReplace: function (configProperty, mountPoint) {
    var winDrive = mountPoint.toLowerCase().replace(winRegex, '$1:');
    var winDir = Em.get(configProperty, 'recommendedValue').replace(/\//g, '\\');
    return winDrive + winDir + '\n';
  },

  /**
   * Same to <code>_defaultWinReplace</code>, but with extra-slash in the end
   *
   * @param {configProperty} configProperty
   * @param {string} mountPoint
   * @returns {string}
   * @private
   */
  _defaultWinReplaceWithAdditionalSlashes: function (configProperty, mountPoint) {
    var winDrive = mountPoint.toLowerCase().replace(winRegex, '$1:');
    var winDir = Em.get(configProperty, 'recommendedValue').replace(/\//g, '\\\\');
    return winDrive + winDir + '\n';
  },

  /**
   * Update information from localDB using <code>App.Host</code>-model
   *
   * @param {object} hostsInfo
   * @returns {object}
   * @private
   */
  _updateHostInfo: function (hostsInfo) {
    App.Host.find().forEach(function (item) {
      if (!hostsInfo[item.get('id')]) {
        hostsInfo[item.get('id')] = {
          name: item.get('id'),
          cpu: item.get('cpu'),
          memory: item.get('memory'),
          disk_info: item.get('diskInfo'),
          bootStatus: "REGISTERED",
          isInstalled: true
        };
      }
    });
    return hostsInfo;
  },

  /**
   * Determines if mount point is valid
   * Criterias:
   * <ul>
   *   <li>Should has available space</li>
   *   <li>Should not be home-dir</li>
   *   <li>Should not be docker-dir</li>
   *   <li>Should not be boot-dir</li>
   *   <li>Should not be dev-dir</li>
   * </ul>
   *
   * @param {{mountpoint: string, available: number}} mPoint
   * @returns {boolean} true - valid, false - invalid
   * @private
   */
  _filterMountPoint: function (mPoint) {
    var isAvailable = mPoint.available !== 0;
    if (!isAvailable) {
      return false;
    }

    var notHome = !['/', '/home'].contains(mPoint.mountpoint);
    var notDocker = !['/etc/resolv.conf', '/etc/hostname', '/etc/hosts'].contains(mPoint.mountpoint);
    var notBoot = mPoint.mountpoint && !(mPoint.mountpoint.startsWith('/boot') || mPoint.mountpoint.startsWith('/mnt'));
    var notDev = !(['devtmpfs', 'tmpfs', 'vboxsf', 'CDFS'].contains(mPoint.type));

    return notHome && notDocker && notBoot && notDev;
  },

  /**
   * Get list of hostNames from localDB which contains needed components
   *
   * @param {topologyLocalDB} localDB
   * @param {object} initializer
   * @returns {string[]}
   * @private
   */
  _getSetOfHostNames: function (localDB, initializer) {
    var masterComponentHostsInDB = Em.getWithDefault(localDB, 'masterComponentHosts', []);
    var slaveComponentHostsInDB = Em.getWithDefault(localDB, 'slaveComponentHosts', []);
    var hosts = masterComponentHostsInDB.filter(function (master) {
      return initializer.components.contains(master.component);
    }).mapProperty('hostName');

    var sHosts = slaveComponentHostsInDB.find(function (slave) {
      return initializer.components.contains(slave.componentName);
    });
    if (sHosts) {
      hosts = hosts.concat(sHosts.hosts.mapProperty('hostName'));
    }
    return hosts;
  },

  /**
   * Get list of all unique valid mount points for hosts
   *
   * @param {string[]} setOfHostNames
   * @param {object} hostsInfo
   * @returns {string[]}
   * @private
   */
  _getAllMountPoints: function (setOfHostNames, hostsInfo) {
    var allMountPoints = [];
    for (var i = 0; i < setOfHostNames.length; i++) {
      var hostname = setOfHostNames[i];
      var mountPointsPerHost = hostsInfo[hostname].disk_info;
      var mountPointAsRoot = mountPointsPerHost.findProperty('mountpoint', '/');

      // If Server does not send any host details information then atleast one mountpoint should be presumed as root
      // This happens in a single container Linux Docker environment.
      if (!mountPointAsRoot) {
        mountPointAsRoot = {
          mountpoint: '/'
        };
      }

      mountPointsPerHost.filter(this._filterMountPoint).forEach(function (mPoint) {
        if( !allMountPoints.findProperty("mountpoint", mPoint.mountpoint)) {
          allMountPoints.push(mPoint);
        }
      }, this);
    }

    if (!allMountPoints.length) {
      allMountPoints.push(mountPointAsRoot);
    }
    return allMountPoints;
  }

});