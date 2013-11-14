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
App.ManageConfigGroupsController = App.WizardController.extend({

  name: 'manageConfigGroupsController',

  isLoaded: false,

  serviceName: null,

  configGroups: [],

  selectedConfigGroup: null,

  loadConfigGroups: function (serviceName) {
    this.set('serviceName', serviceName);
    App.ajax.send({
      name: 'service.load_config_groups',
      sender: this,
      data: {
        serviceName: serviceName
      },
      success: 'onLoadConfigGroupsSuccess',
      error: 'onLoadConfigGroupsError'
    });
  },

  onLoadConfigGroupsSuccess: function (data) {
    var usedHosts = [];
    var unusedHosts = [];
    var defaultConfigGroup = App.ConfigGroup.create({
      name: "Default",
      description: "Default cluster level " + this.get('serviceName') + " configuration",
      isDefault: true,
      parentConfigGroup: null,
      service: this.get('content'),
      configSiteTags: []
    });
    if (data && data.items) {
      var groupToTypeToTagMap = {};
      var configGroups = [];
      data.items.forEach(function (configGroup) {
        configGroup = configGroup.ConfigGroup;
        var newConfigGroup = App.ConfigGroup.create({
          id: configGroup.id,
          name: configGroup.group_name,
          description: configGroup.description,
          isDefault: false,
          parentConfigGroup: defaultConfigGroup,
          service: App.Service.find().findProperty('serviceName', configGroup.tag),
          hosts: configGroup.hosts.mapProperty('host_name'),
          configSiteTags: [],
          properties: []
        });
        usedHosts = usedHosts.concat(newConfigGroup.get('hosts'));
        configGroups.push(newConfigGroup);
        configGroup.desired_configs.forEach(function (config) {
          if (!groupToTypeToTagMap[configGroup.group_name]) {
            groupToTypeToTagMap[configGroup.group_name] = {}
          }
          groupToTypeToTagMap[configGroup.group_name][config.type] = config.tag;
        });
      }, this);
      unusedHosts = App.Host.find().mapProperty('hostName');
      usedHosts.uniq().forEach(function (host) {
        unusedHosts = unusedHosts.without(host);
      }, this);
      defaultConfigGroup.set('childConfigGroups', configGroups);
      defaultConfigGroup.set('hosts', unusedHosts);
      this.set('configGroups', [defaultConfigGroup].concat(configGroups));
      this.loadProperties(groupToTypeToTagMap);
      this.set('isLoaded', true);
    }
  },

  onLoadConfigGroupsError: function () {
    console.error('Unable to load config groups for service.');
  },

  loadProperties: function (groupToTypeToTagMap) {
    var typeTagToGroupMap = {};
    var urlParams = [];
    for (var group in groupToTypeToTagMap) {
      var overrideTypeTags = groupToTypeToTagMap[group];
      for (var type in overrideTypeTags) {
        var tag = overrideTypeTags[type];
        typeTagToGroupMap[type + "///" + tag] = group;
        urlParams.push('(type=' + type + '&tag=' + tag + ')');
      }
    }
    var params = urlParams.join('|');
    if (urlParams.length) {
      App.ajax.send({
        name: 'config.host_overrides',
        sender: this,
        data: {
          params: params,
          typeTagToGroupMap: typeTagToGroupMap
        },
        success: 'onLoadPropertiesSuccess'
      });
    }
  },

  onLoadPropertiesSuccess: function (data, opt, params) {
    data.items.forEach(function (configs) {
      var typeTagConfigs = [];
      App.config.loadedConfigurationsCache[configs.type + "_" + configs.tag] = configs.properties;
      var group = params.typeTagToGroupMap[configs.type + "///" + configs.tag];
      for (var config in configs.properties) {
        typeTagConfigs.push({
          name: config,
          value: configs.properties[config]
        });
      }
      this.get('configGroups').findProperty('name', group).get('properties').pushObjects(typeTagConfigs);
    }, this);
  },

  showProperties: function () {
    var properies = this.get('selectedConfigGroup.propertiesList');
    if (properies) {
      App.showAlertPopup(Em.I18n.t('services.service.config_groups_popup.properties'), properies);
    }
  }
});
