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

App.WizardStep7View = Em.View.extend({

  templateName: require('templates/wizard/step7'),

  didInsertElement: function () {
    var controller = this.get('controller');
    var slaveController = App.router.get('slaveComponentGroupsController');
    controller.loadStep();
    //slaveController.loadStep();  // TODO: remove it to enable slaveConfiguration
  },
  onToggleBlock: function(event){
    $(document.getElementById(event.context.name)).toggle('blind', 500);
    event.context.set('isCollapsed', !event.context.get('isCollapsed'));
  }

});

/**
 * Since we need to use local Views and Controllers we should put them into separate context
 * @type {*|Object}
 */
App.WizardStep7 = App.WizardStep7 || {};

App.WizardStep7.ServiceConfigsByCategoryView = Ember.View.extend({

  content: null,

  category: null,
  serviceConfigs: null, // General, Advanced, NameNode, SNameNode, DataNode, etc.

  categoryConfigs: function () {
    return this.get('serviceConfigs').filterProperty('category', this.get('category.name'))
  }.property('serviceConfigs.@each').cacheable(),
  didInsertElement: function () {
    if (this.get('category.name') == 'Advanced') {
      this.set('category.isCollapsed', true);
      $("#Advanced").hide();
    } else {
      this.set('category.isCollapsed', false);
    }
  },
  layout: Ember.Handlebars.compile('<div {{bindAttr id="view.category.name"}} class="accordion-body collapse in"><div class="accordion-inner">{{yield}}</div></div>')
});

App.WizardStep7.ServiceConfigTab = Ember.View.extend({

  tagName: 'li',

  selectService: function (event) {
    this.set('controller.selectedService', event.context);
  },

  didInsertElement: function () {
    var serviceName = this.get('controller.selectedService.serviceName');
    this.$('a[href="#' + serviceName + '"]').tab('show');
  }
});
