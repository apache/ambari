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

App.ServiceConfig = Ember.Object.extend({
  serviceName: '',
  configCategories: [],
  configs: null,
  restartRequired: false,
  restartRequiredMessage: '',
  restartRequiredHostsAndComponents: {},
  configGroups: [],
  dependentServiceNames: [],
  initConfigsLength: 0, // configs length after initialization in order to watch changes
  errorCount: function () {
    var overrideErrors = 0,
      masterErrors = 0,
      slaveErrors = 0,
      configs = this.get('configs'),
      configCategories = this.get('configCategories'),
      enhancedConfigsErrors = 0;
    configCategories.forEach(function (_category) {
      slaveErrors += _category.get('slaveErrorCount');
      _category.set('nonSlaveErrorCount', 0);
    });
    configs.forEach(function (item) {
      if (item.get('isVisible')) {
        var options = item.get('options');
        if (options && options.someProperty('foreignKeys')) {
          var options = options.filterProperty('foreignKeys');
          options.forEach(function (opt) {
            opt.foreignKeys.forEach(function (key) {
              var config = configs.findProperty('name', key);
              if (config) {
                config.set('isVisible', item.get('value') === opt.displayName);
              }
            });
          });
        }
      }
    });
    configs.forEach(function (item) {
      var category = configCategories.findProperty('name', item.get('category'));
      if (category && !item.get('isValid') && item.get('isVisible') && !item.get('widgetType')) {
        category.incrementProperty('nonSlaveErrorCount');
        masterErrors++;
      }
      if (!item.get('isValid') && item.get('widgetType') && item.get('isVisible') && !item.get('hiddenBySection')) {
        enhancedConfigsErrors++;
      }
      if (item.get('overrides')) {
        item.get('overrides').forEach(function (e) {
          if (e.error) {
            if (category && !Em.get(e, 'parentSCP.widget')) {
              category.incrementProperty('nonSlaveErrorCount');
            }
            overrideErrors++;
          }
        });
      }
    });
    return masterErrors + slaveErrors + overrideErrors + enhancedConfigsErrors;
  }.property('configs.@each.isValid', 'configs.@each.isVisible', 'configs.@each.hiddenBySection', 'configCategories.@each.slaveErrorCount', 'configs.@each.overrideErrorTrigger'),

  /**
   * checks if for example for kdc_type, the value isn't just the pretty version of the saved value, for example mit-kdc
   * and Existing MIT KDC are the same value, but they are interpreted as being changed. This function fixes that
   * @param configs
   * @returns {boolean} - checks
   */
  checkDefaultValues: function (configs) {
    var kdcType = configs.findProperty('name', 'kdc_type');

    if (!kdcType) {
      return configs.someProperty('isNotDefaultValue')
    }

    // if there is only one value changed and that value is for kdc_type, check if the value has really changed or just
    // the string shown to the user is different
    if (configs.filterProperty('isNotDefaultValue').length === 1) {
      if (configs.findProperty('isNotDefaultValue', true) === kdcType) {
        return App.router.get('mainAdminKerberosController.kdcTypesValues')[kdcType.get('savedValue')] !== kdcType.get('value');
      }
    }

    return configs.someProperty('isNotDefaultValue');
  },

  isPropertiesChanged: function() {
    var requiredByAgent = this.get('configs').filterProperty('isRequiredByAgent');
    var isNotSaved = requiredByAgent.someProperty('isNotSaved');
    var isNotDefaultValue = this.checkDefaultValues(requiredByAgent);
    var isOverrideChanged = requiredByAgent.someProperty('isOverrideChanged');
    var differentConfigLengths = this.get('configs.length') !== this.get('initConfigsLength');

    return  isNotSaved || isNotDefaultValue || isOverrideChanged || differentConfigLengths;
  }.property('configs.@each.isNotDefaultValue', 'configs.@each.isOverrideChanged', 'configs.length', 'configs.@each.isNotSaved', 'initConfigsLength'),

  init: function() {
    this._super();
    this.set('dependentServiceNames', App.StackService.find(this.get('serviceName')).get('dependentServiceNames') || []);
  }
});

App.SlaveConfigs = Ember.Object.extend({
  componentName: null,
  displayName: null,
  hosts: null,
  groups: null
});

App.Group = Ember.Object.extend({
  name: null,
  hostNames: null,
  properties: null,
  errorCount: function () {
    if (this.get('properties')) {
      return this.get('properties').filterProperty('isValid', false).filterProperty('isVisible', true).get('length');
    }
  }.property('properties.@each.isValid', 'properties.@each.isVisible')
});

App.ConfigSiteTag = Ember.Object.extend({
  site: DS.attr('string'),
  tag: DS.attr('string'),
  /**
   * Object map of hostname->override-tag for overrides.
   * <b>Creators should set new object here.<b>
   */
  hostOverrides: null
});
