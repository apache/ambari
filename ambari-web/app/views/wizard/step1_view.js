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

App.WizardStep1View = Em.View.extend({
  templateName: require('templates/wizard/step1'),

  stacks: function() {
    return this.get('controller.content.stacks');
  }.property('controller.content.stacks'),

  stackRadioButton: Ember.Checkbox.extend({
    tagName: 'input',
    attributeBindings: ['type', 'checked'],
    checked: function () {
      return this.get('content.isSelected');
    }.property('content.isSelected'),
    type: 'radio',

    click: function () {
      this.get('parentView.stacks').setEach('isSelected', false);
      this.get('parentView.stacks').findProperty('name', this.get('content.name')).set('isSelected', true);
    }
  })
});
