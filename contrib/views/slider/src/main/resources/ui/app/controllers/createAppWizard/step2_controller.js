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

App.CreateAppWizardStep2Controller = Ember.ArrayController.extend({

  needs: "createAppWizard",

  appWizardController: Ember.computed.alias("controllers.createAppWizard"),

  /**
   * List of app type components
   * @type {App.SliderAppTypeComponent}
   */
  content: [],

  /**
   * New App object
   * @type {App.SliderApp}
   */
  newApp: null,

  /**
   * Validate all input fields are integer
   * @type {Boolean}
   */
  isError: function () {
    var result = false;
    this.get('content').forEach(function (component) {
      if (!result && (this.isNotInteger(component.get('numInstances')) || this.isNotInteger(component.get('yarnMemory')) || this.isNotInteger(component.get('yarnCPU')))) {
        result = true;
      }
    }, this);
    return result;
  }.property('content.@each.numInstances', 'content.@each.yarnMemory', 'content.@each.yarnCPU'),

  /**
   * Define if submit button is disabled
   * <code>isError</code> should be true
   * @type {bool}
   */
  isSubmitDisabled: function () {
    return this.get('isError');
  }.property('isError'),

  /**
   * Load all required data for step
   * @method loadStep
   */
  loadStep: function () {
    this.initializeNewApp();
  },

  /**
   * Initialize new App to use it scope of controller
   * @method initializeNewApp
   */
  initializeNewApp: function () {
    var newApp = this.get('appWizardController.newApp');
    this.set('newApp', newApp);
    this.loadTypeComponents();
  },

  /**
   * Fill <code>content</code> with objects created from <code>App.SliderAppTypeComponent</code>
   * @method loadTypeComponents
   */
  loadTypeComponents: function () {
    var content = [];
    var allTypeComponents = this.get('newApp.appType.components');
    if (allTypeComponents && allTypeComponents.get('length')) {
      allTypeComponents.forEach(function (typeComponent) {
        content.push(Ember.Object.create({
          displayName: typeComponent.get('displayName'),
          name: typeComponent.get('name'),
          priority: typeComponent.get('priority'),
          numInstances: typeComponent.get('defaultNumInstances').toString(),
          yarnMemory: typeComponent.get('defaultYARNMemory').toString(),
          yarnCPU: typeComponent.get('defaultYARNCPU').toString()
        }));
      });
      this.set('content', content);
    }
  },

  /**
   * Check if param is integer
   * @param {string} value value to check
   * @return {Boolean}
   * @method isNotInteger
   */
  isNotInteger: function (value) {
    return !(value.trim().length && (value % 1 == 0));
  },

  /**
   * Save all data about components to <code>appWizardController.newApp.components</code>
   * @method saveComponents
   */
  saveComponents: function () {
    this.set('appWizardController.newApp.components', this.get('content'));
  },

  actions: {
    /**
     * Save data and proceed to the next step
     * @method submit
     */
    submit: function () {
      this.saveComponents();
      this.get('appWizardController').nextStep();
    }
  }
});
