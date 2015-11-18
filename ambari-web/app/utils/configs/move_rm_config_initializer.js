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
require('utils/configs/move_component_config_initializer_class');

/**
 * Settings for <code>rm_ha_depended</code>-initializer
 * Used for configs with value equal to the host name (host where component is moved)
 *
 * @param {boolean} rmHaShouldBeEnabled
 * @returns {{type: string, rmHaShouldBeEnabled: boolean}}
 */
function getRmHaDependedConfig(rmHaShouldBeEnabled) {
  return {
    type: 'rm_ha_depended',
    rmHaShouldBeEnabled: Boolean(rmHaShouldBeEnabled)
  };
}

/**
 * Initializer for configs which should be affected when Resource Manager is moved from one host to another
 * If Resource Manager HA-mode is already activated, several configs are also updated
 *
 * @instance MoveComponentConfigInitializerClass
 */
App.MoveRmConfigInitializer = App.MoveComponentConfigInitializerClass.create({

  initializerTypes: [
    {name: 'rm_ha_depended', method: '_initAsRmHaDepended'}
  ],

  initializers: {
    'yarn.resourcemanager.hostname.{{suffix}}': getRmHaDependedConfig(true),
    'yarn.resourcemanager.webapp.address.{{suffix}}': getRmHaDependedConfig(true),
    'yarn.resourcemanager.webapp.https.address.{{suffix}}': getRmHaDependedConfig(true)
  },

  /**
   * Initializer for configs with value equal to the target hostName
   * and based on <code>App.isRMHaEnabled</code>
   * Value example: 'host1:port1'
   *
   * @param {configProperty} configProperty
   * @param {extendedTopologyLocalDB} localDB
   * @param {reassignComponentDependencies} dependencies
   * @param {object} initializer
   * @returns {object}
   * @private
   * @method _initAsRmHaDepended
   */
  _initAsRmHaDepended: function (configProperty, localDB, dependencies, initializer) {
    if (App.get('isRMHaEnabled') === initializer.rmHaShouldBeEnabled) {
      var value = Em.get(configProperty, 'value');
      var parts = value.split(':');
      parts[0] = dependencies.targetHostName;
      Em.set(configProperty, 'value', parts.join(':'));
    }
    return configProperty;
  }

});