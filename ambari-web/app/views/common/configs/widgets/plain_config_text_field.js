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

/**
 * Default input control
 * @type {*}
 */

var App = require('app');
require('views/common/controls_view');

App.PlainConfigTextField = Ember.View.extend(App.SupportsDependentConfigs, App.WidgetPopoverSupport, {
  templateName: require('templates/common/configs/widgets/plain_config_text_field'),
  valueBinding: 'config.value',
  classNames: ['widget-config-plain-text-field'],
  placeholderBinding: 'config.savedValue',

  disabled: function() {
    return !this.get('config.isEditable');
  }.property('config.isEditable'),

  configLabel: function() {
    return this.get('config.stackConfigProperty.displayName') || this.get('config.displayName') || this.get('config.name');
  }.property('config.name', 'config.displayName'),

  /**
   * @type {string|boolean}
   */
  unit: function() {
    return Em.getWithDefault(this, 'config.stackConfigProperty.valueAttributes.unit', false);
  }.property('config.stackConfigProperty.valueAttributes.unit'),

  /**
   * @type {string}
   */
  displayUnit: function() {
    var unit = this.get('unit');
    if ('milliseconds' == unit) {
      unit = 'ms';
    }
    return unit;
  }.property('unit'),

  focusOut: function () {
    this.sendRequestRorDependentConfigs(this.get('config'));
  },

  insertNewline: function() {
    this.get('parentView').trigger('toggleWidgetView');
  },

  didInsertElement: function() {
    this._super();
    this.initPopover();
    this.set('config.displayType', Em.getWithDefault(this, 'config.stackConfigProperty.valueAttributes.type', 'string'));
  }

});
