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

  templateName: require('templates/installer/step7'),

  submit: function(e) {
    App.router.transitionTo('step8');
  }

});

App.ServiceConfigsByCategoryView = Ember.View.extend({
  viewName: 'serviceConfigs',
  content: null,

  category: null,
  serviceConfigs: null,  // General, Advanced, NameNode, SNameNode, DataNode, etc.

  categoryConfigs: function() {
    return this.get('serviceConfigs').filterProperty('category', this.get('category'))
  }.property('categoryConfigs.@each').cacheable()
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

  didInsertElement: function() {
    this.$().popover({
      title: this.get('serviceConfig.name'),
      content: this.get('serviceConfig.description'),
      placement: 'right',
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
      title: this.get('serviceConfig.name'),
      content: this.get('serviceConfig.description'),
      placement: 'right',
      trigger: 'hover'
    });
  }

});

App.ServiceConfigBigTextArea = App.ServiceConfigTextArea.extend({
  rows: 10
});