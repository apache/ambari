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

App.WizardMenuMixin = Em.Mixin.create({

  isStepDisabled: function (index) {
    return this.get('controller.isStepDisabled').findProperty('step', index).get('value');
  },

  isStep0Disabled: function () {
    return this.isStepDisabled(0);
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep1Disabled: function () {
    return this.isStepDisabled(1);
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep2Disabled: function () {
    return this.isStepDisabled(2);
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep3Disabled: function () {
    return this.isStepDisabled(3);
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep4Disabled: function () {
    return this.isStepDisabled(4);
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep5Disabled: function () {
    return this.isStepDisabled(5);
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep6Disabled: function () {
    return this.isStepDisabled(6);
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep7Disabled: function () {
    return this.isStepDisabled(7);
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep8Disabled: function () {
    return this.isStepDisabled(8);
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep9Disabled: function () {
    return this.isStepDisabled(9);
  }.property('controller.isStepDisabled.@each.value').cacheable(),

  isStep10Disabled: function () {
    return this.isStepDisabled(10);
  }.property('controller.isStepDisabled.@each.value').cacheable()

});
