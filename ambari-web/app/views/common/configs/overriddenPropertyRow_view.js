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
// SCP means ServiceConfigProperty

var App = require('app');

App.ServiceConfigView.SCPOverriddenRowsView = Ember.View.extend({
  templateName: require('templates/common/configs/overriddenPropertyRow'),
  classNames: ['overriden-value'],
  serviceConfigProperty: null, // is passed dynamically at runtime where ever
  // we are declaring this from configs.hbs ( we are initializing this from UI )
  categoryConfigs: null, // just declared as viewClass need it

  init: function () {
    this._super();
    if (this.get('controller.name') != 'mainServiceInfoConfigsController') {
      this.addObserver('isDefaultGroupSelected', this, 'setSwitchText');
    }
  },

  didInsertElement: function () {
    this.setSwitchText();
  },

  willDestroyElement: function () {
    if (this.get('controller.name') != 'mainServiceInfoConfigsController') {
      this.removeObserver('isDefaultGroupSelected', this, 'setSwitchText');
    }
  },

  setSwitchText: function () {
    if (this.get('isDefaultGroupSelected')) {
      var overrides = this.get('serviceConfigProperty.overrides');
      if (!overrides) return;
      overrides.forEach(function(overriddenSCP) {
        overriddenSCP.get('group').set('switchGroupTextShort',
            Em.I18n.t('services.service.config_groups.switchGroupTextShort').format(overriddenSCP.get('group.displayName')));
        overriddenSCP.get('group').set('switchGroupTextFull',
            Em.I18n.t('services.service.config_groups.switchGroupTextFull').format(overriddenSCP.get('group.displayName')));
      });
      this.set('serviceConfigProperty.overrides', overrides);
    }
  },

  toggleFinalFlag: function (event) {
    var override = event.contexts[0];
    if (override.get('isNotEditable')) {
      return;
    }
    override.set('isFinal', !override.get('isFinal'));
  },

  removeOverride: function (event) {
    // arg 1 SCP means ServiceConfigProperty
    var scpToBeRemoved = event.contexts[0];
    var overrides = this.get('serviceConfigProperty.overrides');
    // remove override property from selectedService on installer 7-th step
    if (this.get('controller.name') == 'wizardStep7Controller') {
      var controller = this.get('controller');
      var group = controller.get('selectedService.configGroups').findProperty('name', controller.get('selectedConfigGroup.name'));
      group.get('properties').removeObject(scpToBeRemoved);
    }
    overrides = overrides.without(scpToBeRemoved);
    this.set('serviceConfigProperty.overrides', overrides);
  }
});