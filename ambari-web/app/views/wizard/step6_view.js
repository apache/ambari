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

App.WizardStep6View = Em.View.extend({

  templateName: require('templates/wizard/step6'),

  title: '',

  didInsertElement: function () {
    var controller = this.get('controller');
    if (controller.get('isMasters')) {
      this.set('label', Em.I18n.t('installer.step6.addHostWizard.body'));
      this.set('title', Em.I18n.t('installer.step5.header'));
    }
    else {
      this.set('title', Em.I18n.t('installer.step6.header'));
      this.setLabel();
    }
    $('body').tooltip({
      selector: '[rel=tooltip]'
    });
    controller.loadStep();
  },

  setLabel: function () {
    var label = Em.I18n.t('installer.step6.body');
    var clients = this.get('controller.content.clients');
    clients.forEach(function (_client) {
      if (clients.length === 1) {
        label = label + ' ' + _client.display_name;
      }
      else
        if (_client !== clients[clients.length - 1]) {           // [clients.length - 1]
          label = label + ' ' + _client.display_name;
          if(_client !== clients[clients.length - 2]) {
            label = label + ',';
          }
        }
        else {
          label = label + ' ' + Em.I18n.t('and') + ' ' + _client.display_name + '.';
        }
    }, this);
    this.set('label', label);
  }
});

App.WizardStep6HostView = Em.View.extend({

  host: null,
  tagName: 'td',

  didInsertElement: function () {
    if (!this.get('controller.isMasters')) {
      var components = this.get('controller').getMasterComponentsForHost(this.get('host.hostName'));
      if (components && components.length > 0) {
        components = components.map(function(_component) {
          return App.format.role(_component);
        });
        components = components.join(" /\n");
        this.$().popover({
          title: Em.I18n.t('installer.step6.wizardStep6Host.title').format(this.get('host.hostName')),
          content: components,
          placement: 'right',
          trigger: 'hover'
        });
      }
    }
  }
});

/**
 * Binding host property with dynamic name
 * @type {*}
 */
App.WizardStep6CheckboxView = Em.Checkbox.extend({
  /**
   * Header object with host property name
   */
  checkbox: null,

  checkedBinding: 'checkbox.checked',

  disabledBinding: 'checkbox.isInstalled',

  checkCallback: function() {
    var self = this;
    Ember.run.next(function(){
      self.get('controller').checkCallback(self.get('checkbox.title'));
    });

  }.observes('checked'),

  template: Ember.Handlebars.compile('{{checkbox.title}}')

});
