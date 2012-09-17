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

var popover = function (view) {
  view.$().popover({
    title: view.get('serviceConfig.displayName') + '<br><small>' + view.get('serviceConfig.name') + '</small>',
    content: view.get('serviceConfig.description'),
    placement: 'right',
    trigger: 'hover'
  });

};

App.ServiceConfigTextField = Ember.TextField.extend({

  serviceConfig: null,
  isPopoverEnabled: true,
  valueBinding: 'serviceConfig.value',
  classNames: [ 'input-xlarge' ],

  disabled: function() {
    return !this.get('serviceConfig.isEditable');
  }.property('serviceConfig.isEditable'),

  didInsertElement: function() {
    if (this.get('isPopoverEnabled')) {
      popover(this);
    }
  }
});

App.ServiceConfigTextFieldWithUnit = Ember.View.extend({
  serviceConfig: null,
  valueBinding: 'serviceConfig.value',
  classNames: [ 'input-append' ],

  template: Ember.Handlebars.compile('{{view App.ServiceConfigTextField serviceConfigBinding="view.serviceConfig" isPopoverEnabledBinding="false"}}<span class="add-on">{{view.serviceConfig.unit}}</span>'),

  disabled: function() {
    return !this.get('serviceConfig.isEditable');
  }.property('serviceConfig.isEditable'),

  didInsertElement: function() {
    popover(this);
  }
});

App.ServiceConfigPasswordField = Ember.TextField.extend({
  serviceConfig: null,
  type: 'password',
  valueBinding: 'serviceConfig.value',
  classNames: [ 'input-medium' ],

  template: Ember.Handlebars.compile('{{view view.retypePasswordView placeholder="Retype password"}}'),

  retypePasswordView: Ember.TextField.extend({
    type: 'password',
    classNames: [ 'input-medium', 'retyped-password' ],
    valueBinding: 'parentView.serviceConfig.retypedPassword'
  })

});

App.ServiceConfigTextArea = Ember.TextArea.extend({

  serviceConfig: null,
  valueBinding: 'serviceConfig.value',
  rows: 4,
  classNames: ['span6'],

  didInsertElement: function() {
    popover(this);
  }

});

App.ServiceConfigBigTextArea = App.ServiceConfigTextArea.extend({
  rows: 10
});

var hostPopover = function (view) {
  view.$().popover({
    title: view.get('serviceConfig.displayName'),
    content: view.get('serviceConfig.description'),
    placement: 'right',
    trigger: 'hover'
  });
};

App.ServiceConfigCheckbox = Ember.Checkbox.extend({

  serviceConfig: null,
  checkedBinding: 'serviceConfig.value',

  diInsertElement: function() {
    popover(this);
  }

});

App.ServiceConfigMasterHostView = Ember.View.extend({

  serviceConfig: null,
  classNames: ['master-host', 'span6'],
  valueBinding: 'serviceConfig.value',

  template: Ember.Handlebars.compile('{{value}}'),

  didInsertElement: function() {
    hostPopover(this);
  }
 });

App.ServiceConfigSlaveHostsView = Ember.View.extend({

  classNames: ['slave-hosts', 'span6'],
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
    hostPopover(this);
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
      placement: 'right',
      trigger: 'hover'
    });
  }

});
