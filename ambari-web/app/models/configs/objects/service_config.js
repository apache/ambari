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
  initConfigsLength: 0, // configs length after initialization in order to watch changes
  errorCount: function () {
    var overrideErrors = 0,
      masterErrors = 0,
      slaveErrors = 0,
      configs = this.get('configs'),
      configCategories = this.get('configCategories');
    configCategories.forEach(function (_category) {
      slaveErrors += _category.get('slaveErrorCount');
      _category.set('nonSlaveErrorCount', 0);
    });
    configs.forEach(function (item) {
      var category = configCategories.findProperty('name', item.get('category'));
      if (category && !item.get('isValid') && item.get('isVisible') && !item.get('widget')) {
        category.incrementProperty('nonSlaveErrorCount');
        masterErrors++;
      }
      if (item.get('overrides')) {
        item.get('overrides').forEach(function (e) {
          if (e.error) {
            if (category) {
              category.incrementProperty('nonSlaveErrorCount');
            }
            overrideErrors++;
          }
        });
      }
    });
    return masterErrors + slaveErrors + overrideErrors;
  }.property('configs.@each.isValid', 'configs.@each.isVisible', 'configCategories.@each.slaveErrorCount', 'configs.@each.overrideErrorTrigger'),

  isPropertiesChanged: function() {
    var requiredByAgent = this.get('configs').filterProperty('isRequiredByAgent');
    return requiredByAgent.someProperty('isNotSaved') ||
           requiredByAgent.someProperty('isNotDefaultValue') ||
           requiredByAgent.someProperty('isOverrideChanged') ||
           this.get('configs.length') !== this.get('initConfigsLength') ||
           (this.get('configs.length') === this.get('initConfigsLength') && this.get('configs').someProperty('defaultValue', null));
  }.property('configs.@each.isNotDefaultValue', 'configs.@each.isOverrideChanged', 'configs.length', 'configs.@each.isNotSaved')
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
