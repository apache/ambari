/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * THIS IS NOT USED FOR NOW
 * FOR CONFIG GROUPS WE ARE USING OLD MODELS AND LOGIC
 */

var App = require('app');

App.configGroupsMapper = App.QuickDataMapper.create({
  model: App.ServiceConfigGroup,
  config: {
    id: 'id',
    config_group_id: 'ConfigGroup.id',
    name: 'ConfigGroup.group_name',
    service_name: 'ConfigGroup.tag',
    description: 'ConfigGroup.description',
    host_names: 'hosts',
    service_id: 'ConfigGroup.tag'
  },

  /**
   * using this config when saving group from config_version api
   */
  config2: {
    id: 'id',
    config_group_id: 'group_id',
    name: 'group_name',
    service_name: 'service_name',
    host_names: 'hosts',
    service_id: 'service_name'
  },


  map: function (json, mapFromVersions, serviceNames) {
    if (serviceNames && serviceNames.length > 0) {
      var configGroups = [];

      /**
       * ex: { "HDFS": ["host1", "host2"], "YARN": ["host1"] }
       * this property is used to store host names for default config group.
       * While parsing data for not default groups host names will be excluded from this list.
       * In case there is no not default config groups for some service <code>hostNamesForService<code>
       * will not contain property for this service which mean all host belongs to default group
       */
      var hostNamesForService = {};

      if (json && json.items) {
        json.items.forEach(function (configGroup) {
          if (configGroup.group_name != 'default') {
            if (mapFromVersions) {
              configGroup.id = configGroup.service_name + configGroup.group_id;
            } else {
              configGroup.id = configGroup.ConfigGroup.tag + configGroup.ConfigGroup.id;
              configGroup.hosts = configGroup.ConfigGroup.hosts.mapProperty('host_name');
              configGroup.service_name = configGroup.ConfigGroup.tag;
            }

            /**
             * creating (if not exists) field in <code>hostNamesForService<code> with host names for default group
             */
            if (!hostNamesForService[configGroup.service_name]) {
              hostNamesForService[configGroup.service_name] = $.merge([], App.get('allHostNames'));
            }

            /**
             * excluding host names that belongs for current config group from default group
             */
            configGroup.hosts.forEach(function (host) {
              hostNamesForService[configGroup.service_name].splice(hostNamesForService[configGroup.service_name].indexOf(host), 1);
            });
            var template = mapFromVersions ? this.get('config2') : this.get('config');
            configGroups.push(this.parseIt(configGroup, template));
          }
        }, this);
      }

      /**
       * generating default config groups
       */
      serviceNames.forEach(function (serviceName) {
        var defaultGroup = App.ServiceConfigGroup.find().findProperty('id', serviceName + "0");
        if (!defaultGroup) {
          configGroups.push(this.generateDefaultGroup(serviceName, hostNamesForService[serviceName]));
        } else {
          defaultGroup.set('hostNames', hostNamesForService[serviceName] || App.get('allHostNames'));
        }
      }, this);


      configGroups.sort(function (configGroupA, configGroupB) {
        return configGroupA.config_group_id == -1 || (configGroupA.name > configGroupB.name);
      });
      App.store.loadMany(this.get('model'), configGroups);
      App.store.commit();
    }
  },

  /**
   * generate mock object for default config group
   * @param {string} serviceName
   * @param {string[]} [hostNames=null]
   * @returns {{id: string, config_group_id: string, name: string, service_name: string, description: string, host_names: [string], service_id: string}}
   */
  generateDefaultGroup: function (serviceName, hostNames) {
    var displayName = App.StackService.find(serviceName).get('displayName');
    return {
      id: serviceName + '0',
      config_group_id: '-1',
      name: displayName + ' Default',
      service_name: serviceName,
      description: 'Default cluster level ' + displayName + ' configuration',
      host_names: hostNames ? hostNames : App.get('allHostNames'),
      service_id: serviceName
    }
  }
});
