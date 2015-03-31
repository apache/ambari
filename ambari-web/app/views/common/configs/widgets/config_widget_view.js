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
require('views/common/controls_view');
/**
 * Common view for config widgets
 * @type {Em.View}
 */
App.ConfigWidgetView = Em.View.extend(App.SupportsDependentConfigs, {

  /**
   * @type {App.ConfigProperty}
   */
  config: null,

  /**
   * Alias to <code>config.isOriginalSCP</code>
   * Should be used in the templates
   * Don't use original <code>config.isOriginalSCP</code> in the widget-templates!!!
   * @type {boolean}
   */
  isOriginalSCPBinding: 'config.isOriginalSCP',

  /**
   * Alias to <code>config.isComparison</code>
   * Should be used in the templates
   * Don't use original <code>config.isComparison</code> in the widget-templates!!!
   * @type {boolean}
   */
  isComparisonBinding: 'config.isComparison',

  /**
   * Config name to display.
   * @type {String}
   */
  configLabel: function() {
    return this.get('config.displayName') || this.get('config.name');
  }.property('config.name', 'config.displayName'),


  /**
   * Error message computed in config property model
   * @type {String|Boolean}
   */
  configErrorMessage: function() {
    return this.get('config.errorMessage') || false;
  }.property('config.errorMessage'),

  /**
   * Determines if config-value was changed
   * @type {boolean}
   */
  valueIsChanged: function () {
    return this.get('config.value') != this.get('config.defaultValue');
  }.property('config.value'),

  /**
   * Reset config-value to its default
   * @method restoreValue
   */
  restoreValue: function () {
    this.set('config.value', this.get('config.defaultValue'));
  },

  /**
   * Determines if override is allowed for <code>config</code>
   * @type {boolean}
   */
  overrideAllowed: function () {
    var config = this.get('config');
    if (!config) return false;
    return config.get('isOriginalSCP') && config.get('isPropertyOverridable') && !this.get('config.isComparison');
  }.property('config.isOriginalSCP', 'config.isPropertyOverridable', 'config.isComparison'),

  /**
   * Determines if undo is allowed for <code>config</code>
   * @type {boolean}
   */
  undoAllowed: function () {
    var config = this.get('config');
    if (!config) return false;
    return !config.get('cantBeUndone') && config.get('isNotDefaultValue');
  }.property('config.cantBeUndone', 'config.isNotDefaultValue'),

  /**
   * sync widget value with config value when dependent properties
   * have been loaded or changed
   */
  syncValueWithConfig: function() {
    this.setValue(this.get('config.value'));
  }.observes('controller.recommendationTimeStamp'),

  /**
   * set widget value same as config value
   * useful for widgets that work with intermediate config value, not original
   * for now used in slider widget
   * @abstract
   */
  setValue: Em.K
});
