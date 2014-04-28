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

var App = require('app');

App.MainHostServiceConfigsController = App.MainServiceInfoConfigsController.extend({
  name: 'mainHostServiceConfigsController',
  host: null,
  isHostsConfigsPage: true,
  typeTagToHostMap: null,
  configKeyToConfigMap: null,

  /**
   * On load function
   */
  loadStep: function () {
    var content = this.get('content');
    this.set('host', content.host);
    this._super();
  },

  /**
   * Removes categories which are not valid for this host. Ex: Remove JOBTRACKER
   * category on host which does not have it installed.
   */
  renderServiceConfigs: function (serviceConfigs) {
    var newServiceConfigs = jQuery.extend({}, serviceConfigs);
    newServiceConfigs.configCategories = this.filterServiceConfigs(serviceConfigs.configCategories);
    this._super(newServiceConfigs);
  },
  /**
   * filter config categories by host-component of host
   * @param configCategories
   * @return {Array}
   */
  filterServiceConfigs: function (configCategories) {
    var hostComponents = this.get('host.hostComponents');
    var hostHostComponentNames = (hostComponents) ? hostComponents.mapProperty('componentName') : [];

    return configCategories.filter(function (category) {
      var hcNames = category.get('hostComponentNames');
      if (hcNames && hcNames.length > 0) {
        for (var i = 0, l = hcNames.length; i < l; i++) {
          if (hostHostComponentNames.contains(hcNames[i])) {
            return true;
          }
        }
        return false;
      }
      return true;
    });
  },

  /**
   * This method will *not load* the overridden properties. However it will
   * replace the value shown for properties which this host has override for.
   * @param serviceConfigs
   * @param loadedGroupToOverrideSiteToTagMap
   * @param configGroups
   */
  loadServiceConfigHostsOverrides: function (serviceConfigs, loadedGroupToOverrideSiteToTagMap, configGroups) {
    var configKeyToConfigMap = {};
    serviceConfigs.forEach(function (item) {
      configKeyToConfigMap[item.name] = item;
    });
    this.set('configKeyToConfigMap', configKeyToConfigMap);
    var urlParams = this.constructUrlParams(loadedGroupToOverrideSiteToTagMap, configGroups);
    if (urlParams.length > 0) {
      App.ajax.send({
        name: 'host.service_config_hosts_overrides',
        sender: this,
        data: {
          urlParams: urlParams.join('|')
        },
        success: 'loadServiceConfigHostsOverridesSuccessCallback',
        error: 'loadServiceConfigHostsOverridesErrorCallback'
      });
    }
  },
  /**
   * construct URL parameters, based on groups
   * @param loadedGroupToOverrideSiteToTagMap
   * @param configGroups
   * @return {Array}
   */
  constructUrlParams: function (loadedGroupToOverrideSiteToTagMap, configGroups) {
    var typeTagToGroupMap = {};
    var urlParams = [];
    var thisHostName = this.get('host.hostName');
    for (var group in loadedGroupToOverrideSiteToTagMap) {
      var groupObj = configGroups.findProperty('name', group);
      if (groupObj.get('hosts').contains(thisHostName)) {
        var overrideTypeTags = loadedGroupToOverrideSiteToTagMap[group];
        for (var type in overrideTypeTags) {
          var tag = overrideTypeTags[type];
          typeTagToGroupMap[type + "///" + tag] = groupObj;
          urlParams.push('(type=' + type + '&tag=' + tag + ')');
        }
      }
    }
    this.set('typeTagToGroupMap', typeTagToGroupMap);
    return urlParams;
  },
  /**
   * handle configs, override their values
   * @param data
   */
  loadServiceConfigHostsOverridesSuccessCallback: function (data) {
    var typeTagToGroupMap = this.get('typeTagToGroupMap');
    var configKeyToConfigMap = this.get('configKeyToConfigMap');
    data.items.forEach(function (config) {
      var group = typeTagToGroupMap[config.type + "///" + config.tag];
      var properties = config.properties;
      for (var prop in properties) {
        var serviceConfig = configKeyToConfigMap[prop];
        var hostOverrideValue = properties[prop];
        if (serviceConfig) {
          serviceConfig.value = hostOverrideValue;
          serviceConfig.isOriginalSCP = false;
          serviceConfig.group = group;
          App.config.handleSpecialProperties(serviceConfig);
        }
      }
    });
  },
  loadServiceConfigHostsOverridesErrorCallback: function (request, ajaxOptions, error) {
    console.log("TRACE: error code status is: " + request.status);
  },
  /**
   * invoke dialog for switching group of host
   */
  switchHostGroup: function () {
    var self = this;
    App.config.launchSwitchConfigGroupOfHostDialog(this.get('selectedConfigGroup'), this.get('configGroups'), this.get('host.hostName'), function (newGroup) {
      self.set('selectedConfigGroup', newGroup);
    })
  }
});
