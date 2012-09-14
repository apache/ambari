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

App.InstallerStep7View = Em.View.extend({

  templateName: require('templates/installer/step7')

});

App.ServiceConfigsByCategoryView = Ember.View.extend({
  viewName: 'serviceConfigs',
  content: null,

  category: null,
  serviceConfigs: null,  // General, Advanced, NameNode, SNameNode, DataNode, etc.

  categoryConfigs: function() {
    return this.get('serviceConfigs').filterProperty('category', this.get('category.name'))
  }.property('serviceConfigs.@each').cacheable()
});

App.ServiceConfigTabs = Ember.View.extend({

  selectService: function(event) {
    this.set('controller.selectedService', event.context);
  },

  didInsertElement: function() {
    this.$('a:first').tab('show');
  }

});

App.ServiceConfigTextField = Ember.TextField.extend({

  serviceConfig: null,
  valueBinding: 'serviceConfig.value',
  classNames: ['span6'],

  disabled: function() {
    return !this.get('serviceConfig.isEditable');
  }.property('serviceConfig.isEditable'),

  didInsertElement: function() {
    this.$().popover({
      title: this.get('serviceConfig.displayName') + '<br><small>' + this.get('serviceConfig.name') + '</small>',
      content: this.get('serviceConfig.description'),
      placement: 'left',
      trigger: 'hover'
    });
  }

});

App.ServiceConfigTextArea = Ember.TextArea.extend({

  serviceConfig: null,
  valueBinding: 'serviceConfig.value',
  rows: 4,
  classNames: ['span6'],

  didInsertElement: function() {
    this.$().popover({
      title: this.get('serviceConfig.displayName') + '<br><small>' + this.get('serviceConfig.name') + '</small>',
      content: this.get('serviceConfig.description'),
      placement: 'left',
      trigger: 'hover'
    });
  }

});

App.ServiceConfigBigTextArea = App.ServiceConfigTextArea.extend({
  rows: 10
});

App.ServiceConfigMasterHostView = Ember.View.extend({

  serviceConfig: null,
  classNames: ['master-host'],
  valueBinding: 'serviceConfig.value',

  template: Ember.Handlebars.compile('{{value}}'),

  didInsertElement: function() {
    this.$().popover({
      title: this.get('serviceConfig.displayName'),
      content: this.get('serviceConfig.description'),
      placement: 'left',
      trigger: 'hover'
    });
  }
 });

App.ServiceConfigSlaveHostsView = Ember.View.extend({

  classNames: ['slave-hosts'],
  valueBinding: 'serviceConfig.value',

  templateName: require('templates/installer/slaveHosts'),

  hasNoHosts: function() {
    return this.get('value').length === 0;
  }.property('value'),

  hasOneHost: function() {
    return this.get('value').length === 1;
  }.property('value'),

  hasMultipleHosts: function() {
    return this.get('value').length > 1;
  }.property('value'),

  otherLength: function() {
    return this.get('value').length - 1;
  }.property('value'),

  didInsertElement: function() {
    this.$().popover({
      title: this.get('serviceConfig.displayName'),
      content: this.get('serviceConfig.description'),
      placement: 'left',
      trigger: 'hover'
    });
  }

});

App.AddSlaveComponentGroupButton = Ember.View.extend({

  tagName: 'span',

  slaveComponentName: null,

  didInsertElement: function () {
    this.$().popover({
      title: 'Add a ' + this.get('slaveComponentName') + ' Group',
      content: 'If you need different settings on certain ' + this.get('slaveComponentName') + 's, you can add a ' + this.get('slaveComponentName') + ' group.<br>' +
        'All ' + this.get('slaveComponentName') + 's within the same group will have the same set of settings.  You can create multiple groups.',
      placement: 'left',
      trigger: 'hover'
    });
  }

});
