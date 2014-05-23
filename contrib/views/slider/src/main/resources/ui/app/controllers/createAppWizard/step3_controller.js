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

App.CreateAppWizardStep3Controller = Ember.ObjectController.extend({

  needs: "createAppWizard",

  appWizardController: Ember.computed.alias("controllers.createAppWizard"),

  /**
   * Configs entered in TextArea
   * @type {String}
   */
  configs: '',

  /**
   * Defines if <code>configs</code> are properly key-value formatted
   * @type {Boolean}
   */
  isError: false,

  /**
   * Config object converted from <code>configs</code>
   * @type {Object}
   */
  configsObject: {},

  /**
   * Load all data required for step
   */
  loadStep: function () {
    this.clearStep();
  },

  /**
   * Clear all initial data
   */
  clearStep: function () {
    this.set('isError', false);
  },

  /**
   * Validate <code>configs</code> to be key-value formatted amd convert it to object
   * @return {Boolean}
   */
  validateConfigs: function () {
    var self = this;
    var result = true;
    var configs = this.get('configs');
    try {
      var configsObject = JSON.parse('{' + configs + '}');
      self.set('configsObject', configsObject);
    } catch (e) {
      self.set('isError', true);
      result = false;
    }
    return result;
  },

  /**
   * Save converted configs to new App configs
   */
  saveConfigs: function () {
    this.set('appWizardController.newApp.configs', this.get('configsObject'));
  },

  actions: {
    /**
     * If <code>configs</code> is valid, than save it and proceed to the next step
     */
    submit: function () {
      if (this.validateConfigs()) {
        this.saveConfigs();
        this.get('appWizardController').nextStep();
      }
    }
  }
});
