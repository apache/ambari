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
 * Combo box widget view for config property.
 * @type {Em.View}
 */
App.ComboConfigWidgetView = App.ConfigWidgetView.extend({
  templateName: require('templates/common/configs/widgets/combo_config_widget'),
  classNames: ['widget-config', 'combo-widget'],
  supportSwitchToTextBox: true,
  /**
   * Object with following structure:
   * {String} .value - value in widget format
   * {Object[]} .valuesList - map of entries and entry_labels
   *   {String} .configValue - value in config format
   *   {String} .widgetValue - value in widget format
   *
   * @property content
   * @type {Em.Object}
   */
  content: null,

  didInsertElement: function() {
    this.initWidget();
    this._super();
    this.toggleWidgetState();
    this.initPopover();
  },

  /**
   * Generate content for view. Set values map and current value.
   *
   * @method generateContent
   */
  initWidget: function() {
    this.set('content', Em.Object.create({}));
    this.set('content.valuesList', this.convertToWidgetUnits(this.get('config.stackConfigProperty.valueAttributes')));
    this.set('content.value', this.generateWidgetValue(this.get('config.value')));
  },

  /**
   * Generate values map according to widget/value format.
   *
   * @method convertToWidgetUnits
   * @param {Object} valueAttributes
   * @returns {Object[]} - values list map @see content.valuesList
   */
  convertToWidgetUnits: function(valueAttributes) {
    return Em.get(valueAttributes, 'entries').map(function(item) {
      return Em.Object.create({
        configValue: item.value,
        widgetValue: item.label || item.value
      });
    });
  },

  /**
   * Get widget value by specified config value.
   *
   * @method generateWidgetValue
   * @param {String} value - value in config property format
   * @returns {String}
   */
  generateWidgetValue: function(value) {
    if (this.isValueCompatibleWithWidget()) {
      return this.get('content.valuesList').findProperty('configValue', value).get('widgetValue');
    }
    return null;
  },

  /**
   * Get config value by specified widget value.
   *
   * @method generateConfigValue
   * @param {String} value - value in widget property format
   * @returns {String}
   */
  generateConfigValue: function(value) {
    return this.get('content.valuesList').findProperty('widgetValue', value).get('configValue');
  },

  /**
   * Action to set config value.
   *
   * @method setConfigValue
   * @param {Object} e
   */
  setConfigValue: function(e) {
    this.set('config.value', e.context);
    this.set('content.value', this.generateWidgetValue(e.context));
    if (this.get('config.previousValue') != this.get('config.value')) {
      this.sendRequestRorDependentConfigs(this.get('config'));
    }
    this.set('config.previousValue', this.get('config.value'));
  },

  /**
   * @override App.ConfigWidgetView.restoreValue
   * @method restoreValue
   */
  restoreValue: function() {
    this.setConfigValue({ context: this.get('config.savedValue') });
    if (this.get('config.supportsFinal')) {
      this.get('config').set('isFinal', this.get('config.savedIsFinal'));
    }
  },

  /**
   * @method setRecommendedValue
   */
  setRecommendedValue: function () {
    this.setConfigValue({ context: this.get('config.recommendedValue')});
    if (this.get('config.supportsFinal')) {
      this.get('config').set('isFinal', this.get('config.recommendedIsFinal'));
    }
  },

  /**
   * Delegate event from text input in combo widget to trigger dropdown
   */
  click: function(event) {
    if (!this.get('disabled') && event.target.className.contains('ember-text-field')) {
      $(event.target).closest('.dropdown').toggleClass('open');
      return false;
    }
  },

  // setValue: function() {
  //   this.setConfigValue({ context: this.get('config.value') });
  // },

  isValueCompatibleWithWidget: function() {
    var res = this._super() && this.get('content.valuesList').someProperty('configValue', this.get('config.value'));
    if (!res) {
      this.updateWarningsForCompatibilityWithWidget(Em.I18n.t('config.infoMessage.wrong.value.for.widget'));
      return false;
    }
    this.updateWarningsForCompatibilityWithWidget('');
    return true;
  }

});
