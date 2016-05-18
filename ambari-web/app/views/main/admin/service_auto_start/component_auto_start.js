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

App.MainAdminServiceAutoStartComponentView = Em.View.extend({
  templateName: require('templates/main/admin/service_auto_start/component_auto_start'),

  recoveryEnabled: false,
  savedRecoveryEnabled: false,
  tab: null,
  component: null,

  didInsertElement: function () {
    this.set('savedRecoveryEnabled', this.get('recoveryEnabled'));
    this.initSwitcher();
  },

  onValueChange: function () {
    this.get('switcher').bootstrapSwitch('state', this.get('component.recoveryEnabled'));
  }.observes('component.recoveryEnabled'),

  /**
   * Init switcher plugin.
   *
   * @method initSwitcher
   */
  initSwitcher: function () {
    var self = this;
    if (this.$()) {
      this.set('switcher', this.$("input:eq(0)").bootstrapSwitch({
        onText: Em.I18n.t('common.enabled'),
        offText: Em.I18n.t('common.disabled'),
        offColor: 'default',
        onColor: 'success',
        handleWidth: Math.max(Em.I18n.t('common.enabled').length, Em.I18n.t('common.disabled').length) * 8,
        onSwitchChange: function (event, state) {
          self.set('tab.enabledComponents', self.get('tab.enabledComponents') + (state ? 1 : -1));
          self.set('recoveryEnabled', state);
          self.set('component.valueChanged', self.get('savedRecoveryEnabled') !== state);
          self.get('parentView.controller').checkValuesChange();
        }
      }));
    }
  }
});