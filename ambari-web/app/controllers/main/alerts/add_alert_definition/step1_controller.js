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

App.AddAlertDefinitionStep1Controller = Em.Controller.extend({

  name: 'addAlertDefinitionStep1',

  /**
   * List of available alert definition types
   * @type {{value: string, isActive: boolean}[]}
   */
  alertDefinitionsTypes: [
    Em.Object.create({value: 'PORT', isActive: false, icon: 'icon-signal'}),
    Em.Object.create({value: 'METRIC', isActive: false, icon: 'icon-bolt'}),
    Em.Object.create({value: 'WEB', isActive: false, icon: 'icon-globe'}),
    Em.Object.create({value: 'AGGREGATE', isActive: false, icon: 'icon-plus-sign-alt'}),
    Em.Object.create({value: 'SCRIPT', isActive: false, icon: 'icon-code'}),
    Em.Object.create({value: 'SERVER', isActive: false, icon: 'icon-desktop'}),
    Em.Object.create({value: 'RECOVERY', isActive: false, icon: 'icon-desktop'})
  ],

  /**
   * "Next"-button is disabled if user doesn't select any alert definition type
   * @type {boolean}
   */
  isSubmitDisabled: function() {
    return this.get('alertDefinitionsTypes').everyProperty('isActive', false);
  }.property('alertDefinitionsTypes.@each.isActive'),

  /**
   * Set selectedType if it exists in the wizard controller
   * @method loadStep
   */
  loadStep: function() {
    this.get('alertDefinitionsTypes').setEach('isActive', false);
    var selectedType = this.get('content.selectedType');
    if(selectedType) {
      this.selectType({context: {value: selectedType}});
    }
  },

  /**
   * Handler for select alert definition type selection
   * @param {object} e
   * @method selectType
   */
  selectType: function(e) {
    var type = e.context,
      types = this.get('alertDefinitionsTypes');
    types.setEach('isActive', false);
    types.findProperty('value', type.value).set('isActive', true);
    this.set('content.selectedType', type.value);
  }

});
