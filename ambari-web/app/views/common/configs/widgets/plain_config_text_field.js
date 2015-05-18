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

App.PlainConfigTextField = Ember.View.extend(App.SupportsDependentConfigs, {
  templateName: require('templates/common/configs/widgets/plain_config_text_field'),
  valueBinding: 'serviceConfig.value',
  classNames: ['widget-config-plain-text-field'],
  placeholderBinding: 'serviceConfig.savedValue',
  unit: function() {
    return Em.getWithDefault(this, 'serviceConfig.stackConfigProperty.valueAttributes.unit', false);
  }.property('serviceConfig.stackConfigProperty.valueAttributes.unit'),

  focusOut: function () {
    this.sendRequestRorDependentConfigs(this.get('serviceConfig'));
  },

  didInsertElement: function() {
    this._super();
    this.set('serviceConfig.displayType', Em.getWithDefault(this, 'serviceConfig.stackConfigProperty.valueAttributes.type', 'string'));
  }

});
