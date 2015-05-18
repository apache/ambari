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

App.configVersionsMapper = App.QuickDataMapper.create({
  model: App.ConfigVersion,
  propertyModel: App.ConfigProperty,
  config: {
    service_name: 'service_name',
    service_id: 'service_name',
    version: "service_config_version",
    create_time: 'createtime',
    group_id: 'group_id',
    group_name: 'group_name',
    hosts: 'hosts',
    author: 'user',
    notes: 'service_config_version_note',
    is_current: 'is_current',
    index: 'index',
    is_for_compare: 'is_for_compare'
  },

  map: function (json, isForCompare) {
    console.time("App.configVersionsMapper execution time");
    var configVersions = [];
    var itemIds = {};
    var serviceToHostMap = {};
    var requestedProperties = [];

    if (json && json.items) {
      json.items.forEach(function (item, index) {
        var parsedItem = this.parseIt(item, this.get('config'));
        parsedItem.id = parsedItem.service_name + '_' + parsedItem.version;
        parsedItem.is_requested = true;
        itemIds[parsedItem.id] = true;
        parsedItem.index = index;
        parsedItem.config_properties = [];
        parsedItem.is_for_compare = Boolean(isForCompare);

        if (serviceToHostMap[item.service_name]) {
          serviceToHostMap[item.service_name] = serviceToHostMap[item.service_name].concat(item.hosts);
        } else {
          serviceToHostMap[item.service_name] = item.hosts;
        }

        /**
         * parsing config properties for config version
         * @type {Array}
         */
        var properties = [];
        if (item.configurations) {
          item.configurations.forEach(function(config) {
            var type = config.type;
            var properties_array = Object.keys(config.properties);
            for (var i = 0; i < properties_array.length; i++) {
              var key = properties_array[i];

              var property = {
                id: key  + '_' + type + '_' + item.service_config_version,
                name: key,
                file_name: App.config.getOriginalFileName(type),
                config_version_id: parsedItem.id,
                stack_config_property_id: key  + '_' + type
              };
              property.value = property.recommended_value = config.properties[key];
              property.is_final = property.recommended_is_final = !!item.properties_attributes && item.properties_attributes.final[key] === "true";

              properties.push(property);
              requestedProperties.push(property.id);
            }
          }, this);
        }
        parsedItem.config_properties = parsedItem.config_properties.concat(properties.mapProperty('id'));

        App.store.loadMany(this.get('propertyModel'), properties);

        configVersions.push(parsedItem);
      }, this);

      this.get('model').find().forEach(function (item) {
        if (!itemIds[item.get('id')]) {
          item.set('isRequested', false);
        }
      });
      var itemTotal = parseInt(json.itemTotal);
      if (!isNaN(itemTotal)) {
        App.router.set('mainConfigHistoryController.filteredCount', itemTotal);
      }

      /**
       * this code sets hostNames for default config group
       * by excluding hostNames that belongs to not default groups
       * from list of all hosts
       */
      Object.keys(serviceToHostMap).forEach(function(sName) {
        var defaultHostNames = App.get('allHostNames');
        for (var i = 0; i < serviceToHostMap[sName].length; i++) {
          defaultHostNames = defaultHostNames.without(serviceToHostMap[sName][i]);
        }
        var defVer = configVersions.find(function(v) {
          return v.is_current && v.group_id == -1 && v.service_name == sName;
        });
        if (defVer) {
          defVer.hosts = defaultHostNames;
        }
      });
      App.store.commit();
      App.store.loadMany(this.get('model'), configVersions);

      this.deleteUnusedProperties(requestedProperties);
    }
    console.timeEnd("App.configVersionsMapper execution time");
  },


  /**
   * delete unused Properties
   * @param requestedProperties
   */
  deleteUnusedProperties: function(requestedProperties) {
    App.ConfigProperty.find().filterProperty('id').forEach(function(p) {
      if (!requestedProperties.contains(p.get('id'))) {
        this.deleteRecord(p);
      }
    }, this);
  }
});
