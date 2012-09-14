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

  errorCount: function() {
    return this.get('configs').filterProperty('isValid', false).get('length');
  }.property('configs.@each.isValid')
});

App.ServiceConfigCategory = Ember.Object.extend({
  name: null,

  isForSlaveComponent: function () {
    return this.get('name') === 'DataNode' || this.get('name') === 'TaskTracker' ||
      this.get('name') === 'RegionServer';
  }.property('name')
});

App.ServiceConfigProperty = Ember.Object.extend({

  name: '',
  displayName: '',
  value: '',
  defaultValue: '',
  description: '',
  displayType: 'string',  // string, digits, number, directories, custom
  unit: '',
  category: 'General',
  isRequired: true,  // by default a config property is required
  isEditable: true, // by default a config property is editable
  errorMessage: '',
  serviceConfig: null, // points to the parent App.ServiceConfig object

  isValid: function() {
    return this.get('errorMessage') === '';
  }.property('errorMessage'),

  viewClass: function() {
    switch (this.get('displayType')) {
      case 'directories':
        return App.ServiceConfigTextArea;
      case 'custom':
        return App.ServiceConfigBigTextArea;
      case 'masterHost':
        return App.ServiceConfigMasterHostView;
      case 'slaveHosts':
        return App.ServiceConfigSlaveHostsView;
      default:
        return App.ServiceConfigTextField;
    }
  }.property('displayType'),

  validate: function() {
    var digitsRegex = /^\d+$/;
    var numberRegex = /^-?(?:\d+|\d{1,3}(?:,\d{3})+)?(?:\.\d+)?$/;

    var value = this.get('value');

    var isError = false;

    if (this.get('isRequired')) {
      if (typeof value === 'string' && value.trim().length === 0) {
        this.set('errorMessage', 'This is required');
        isError = true;
        console.log('required');
      }
    }

    if (!isError) {
      switch (this.get('displayType')) {
        case 'digits':
          if (!digitsRegex.test(value)) {
            this.set('errorMessage', 'Must contain digits only');
            isError = true;
          }
          break;
        case 'number':
          if (!numberRegex.test(value)) {
            this.set('errorMessage', 'Must be a valid number');
            isError = true;
          }
          break;
        case 'directories':
          break;
        case 'custom':
          break;
      }
    }
    if (!isError) {
      this.set('errorMessage', '');
      console.log('setting errorMessage to blank');
    }
  }.observes('value')

});
