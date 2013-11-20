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
    var hostHostComponentNames = [];
    var hostComponents = this.get('host.hostComponents');
    if (hostComponents) {
      hostComponents.forEach(function (hc) {
        var name = hc.get('componentName');
        if (!hostHostComponentNames.contains(name)) {
          hostHostComponentNames.push(name);
        }
      });
    }
    newServiceConfigs.configCategories = serviceConfigs.configCategories.filter(function (category) {
      var hcNames = category.get('hostComponentNames');
      if (hcNames != null && hcNames.length > 0) {
        var show = false;
        hcNames.forEach(function (name) {
          if (hostHostComponentNames.contains(name)) {
            show = true;
          }
        });
        return show;
      }
      return true;
    });
    this._super(newServiceConfigs);
  },

  typeTagToHostMap: null,

  configKeyToConfigMap: null,

  /**
   * This method will *not load* the overridden properties. However it will
   * replace the value shown for properties which this host has override for.
   */
  loadServiceConfigHostsOverrides: function (serviceConfigs, loadedGroupToOverrideSiteToTagMap, configGroups) {
    var thisHostName = this.get('host.hostName');
    var configKeyToConfigMap = {};
    serviceConfigs.forEach(function (item) {
      configKeyToConfigMap[item.name] = item;
    });
    var typeTagToGroupMap = {};
    var urlParams = [];
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
    this.set('configKeyToConfigMap', configKeyToConfigMap);
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
  loadServiceConfigHostsOverridesSuccessCallback: function (data) {
    var typeTagToGroupMap = this.get('typeTagToGroupMap');
    var configKeyToConfigMap = this.get('configKeyToConfigMap');
    data.items.forEach(function (config) {
      var group = typeTagToGroupMap[config.type + "///" + config.tag];
      var properties = config.properties;
      for ( var prop in properties) {
        var serviceConfig = configKeyToConfigMap[prop];
        var hostOverrideValue = properties[prop];
        if (serviceConfig && serviceConfig.displayType === 'int') {
          if (/\d+m$/.test(hostOverrideValue)) {
            hostOverrideValue = hostOverrideValue.slice(0, hostOverrideValue.length - 1);
          }
        } else if (serviceConfig && serviceConfig.displayType === 'checkbox') {
          switch (hostOverrideValue) {
            case 'true':
              hostOverrideValue = true;
              break;
            case 'false':
              hostOverrideValue = false;
              break;
          }
        }
        if (serviceConfig) {
          // Value of this property is different for this host.
          console.log("loadServiceConfigHostsOverrides(" + group + "): [" + group + "] OVERRODE(" + serviceConfig.name + "): " + serviceConfig.value + " -> " + hostOverrideValue);
          serviceConfig.value = hostOverrideValue;
          serviceConfig.defaultValue = hostOverrideValue;
          serviceConfig.isOriginalSCP = false;
          serviceConfig.group = group;
        }
      }
    });
    console.log("loadServiceConfigHostsOverrides(" + this.get('host.hostName') + "): Finished loading.");
  },
  loadServiceConfigHostsOverridesErrorCallback: function (request, ajaxOptions, error) {
    console.log("TRACE: error code status is: " + request.status);
  },
  /**
   * invoke dialog for switching group of host
   */
  switchHostGroup: function () {
    var self = this;
    App.config.launchSwitchConfigGroupOfHostDialog(this.get('selectedConfigGroup'), this.get('configGroups'), this.get('host.hostName'), function(newGroup){
      self.set('selectedConfigGroup', newGroup);
    })
  }
});
