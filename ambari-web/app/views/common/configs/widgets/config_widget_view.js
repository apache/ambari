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
 * Common view for config widgets
 * @type {Em.View}
 */
App.ConfigWidgetView = Em.View.extend({

  /**
   * @type {App.ConfigProperty}
   */
  config: null,

  /**
   * Determines if config-value was changed
   * @type {boolean}
   */
  valueIsChanged: function () {
    return this.get('config.value') != this.get('config.defaultValue');
  }.property('config.value'),

  /**
   * @type {boolean}
   */
  isComparison: false,

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
    return config.get('isOriginalSCP') && config.get('isPropertyOverridable') && !this.get('isComparison');
  }.property('config.isOriginalSCP', 'config.isPropertyOverridable', 'isComparison'),

  /**
   * Determines if undo is allowed for <code>config</code>
   * @type {boolean}
   */
  undoAllowed: function () {
    var config = this.get('config');
    if (!config) return false;
    return !config.get('cantBeUndone') && config.get('isNotDefaultValue');
  }.property('config.cantBeUndone', 'config.isNotDefaultValue')

});
