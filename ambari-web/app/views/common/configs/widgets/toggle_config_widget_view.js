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

/**
 * Toggle wiget view for config property.
 * @type {Em.View}
 */
App.ToggleConfigWidgetView = App.ConfigWidgetView.extend({
  templateName: require('templates/common/configs/widgets/toggle_config_widget'),
  classNames: ['widget', 'toggle-widget'],

  /**
   * Saved switcher for current config.
   *
   * @property switcher
   */
  switcher: null,

  /**
   * Value used in the checkbox.
   * <code>config.value</code> can't be used because it's string.
   *
   * @property switcherValue
   * @type {boolean}
   */
  switcherValue: false,

  /**
   * Update config value using <code>switcherValue</code>.
   * switcherValue is boolean, but config value should be a string 'true'|'false'.
   *
   * @method updateConfigValue
   */
  updateConfigValue: function () {
    this.set('config.value', '' + this.get('switcherValue'));
  },

  /**
   * Get value for <code>switcherValue</code> (boolean) using <code>config.value</code> (string).
   *
   * @param configValue
   * @returns {boolean} true for 'true', false for 'false'
   * @method getNewSwitcherValue
   */
  getNewSwitcherValue: function (configValue) {
    return 'true' === configValue;
  },

  didInsertElement: function () {
    this.set('switcherValue', this.getNewSwitcherValue(this.get('config.value')));
    // plugin should be initiated after applying binding for switcherValue
    Em.run.later('sync', function() {
      this.initSwitcher();
    }.bind(this), 10);
    this.addObserver('switcherValue', this.updateConfigValue);
  },

  /**
   * Init switcher plugin.
   *
   * @method initSwitcher
   */
  initSwitcher: function () {
    var labels = this.get('config.stackConfigProperty.valueAttributes.entry_labels'),
      self = this;
    var switcher = this.$("input").bootstrapSwitch({
      onText: labels[0],
      offText: labels[1],
      offColor: 'danger',
      handleWidth: 85,
      onSwitchChange: function (event, state) {
        self.set('switcherValue', state);
      }
    });
    this.set('switcher', switcher);
  },

  /**
   * Restore default config value and toggle switcher.
   *
   * @override App.ConfigWidgetView.restoreValue
   * @method restoreValue
   */
  restoreValue: function () {
    this._super();
    var value = this.getNewSwitcherValue(this.get('config.value'));
    this.get('switcher').bootstrapSwitch('toggleState', value);
    this.set('switcherValue', value);
  }

});
