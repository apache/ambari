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
 * @type {HaConfigInitializerClass}
 */
App.HaConfigInitializerClass = App.ConfigInitializerClass.extend({

  /**
   * Initializer for configs with value equal to the hostName where some component exists
   * Value may be customized with prefix and suffix (see <code>initializer.modifier</code>)
   * Port-value is calculated according to <code>initializer.portKey</code> or <code>initializer.port</code> values
   * If calculated port-value is empty, it will be skipped (and ':' too)
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
    var value = initializer.modifier.prefix + hostName + (port ? ':' + port : '') + initializer.modifier.suffix;
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
   * If calculated port-value is empty, it will be skipped (and ':' too)
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
        return hostName + (port ? ':' + port : '');
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
  }

});

App.HaConfigInitializerClass.reopenClass({

  /**
   * Settings for <code>host_with_port</code>-initializer
   * Used for configs with value equal to hostName where some component exists concatenated with port-value
   * Port-value is calculated according to <code>port</code> and <code>portFromDependencies</code> values
   * If <code>portFromDependencies</code> is <code>true</code>, <code>port</code>-value is used as key of the <code>dependencies</code> (where real port-value is)
   * Otherwise - <code>port</code>-value used as is
   * If calculated port-value is empty, it will be skipped (and ':' too)
   * Value also may be customized with prefix and suffix
   *
   * @param {string} component needed component
   * @param {boolean} componentExists component already exists or just going to be installed
   * @param {string} prefix=''
   * @param {string} suffix=''
   * @param {string} port
   * @param {boolean} [portFromDependencies=false]
   * @returns {{type: string, component: string, componentExists: boolean, modifier: {prefix: (string), suffix: (string)}}}
   * @method getHostWithPortConfig
   * @static
   */
  getHostWithPortConfig: function (component, componentExists, prefix, suffix, port, portFromDependencies) {
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
  },

  /**
   * Settings for <code>hosts_with_port</code>-initializer
   * Used for configs with value equal to the list of hostNames with port
   * Value also may be customized with prefix, suffix and delimiter between host:port elements
   * Port-value is calculated according to <code>port</code> and <code>portFromDependencies</code> values
   * If <code>portFromDependencies</code> is <code>true</code>, <code>port</code>-value is used as key of the <code>dependencies</code> (where real port-value is)
   * Otherwise - <code>port</code>-value used as is
   * If calculated port-value is empty, it will be skipped (and ':' too)
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
   * @method getHostsWithPortConfig
   * @static
   */
  getHostsWithPortConfig: function (component, prefix, suffix, delimiter, port, portFromDependencies) {
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

});