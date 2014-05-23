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

App.CreateAppWizardStep4Controller = Ember.ObjectController.extend({

  needs: "createAppWizard",

  appWizardController: Ember.computed.alias("controllers.createAppWizard"),

  /**
   * New App object
   * @type {App.SliderApp}
   */
  newApp: null,

  /**
   * Load all required data for step
   */
  loadStep: function () {
    this.initializeNewApp();
  },

  /**
   * Initialize new App to use it scope of controller
   */
  initializeNewApp: function () {
    var newApp = this.get('appWizardController.newApp');
    this.set('newApp', newApp);
  },

  /**
   * Return formatted configs to show them on preview page
   * @return {String}
   */
  configsFormatted: function () {
    var result = '';
    var configs = this.get('newApp.configs');
    if (configs) {
      result = JSON.stringify(configs);
      result = result.substring(1, result.length - 1);
      result = result.replace(/,/g, ',\n');
    }
    return result;
  }.property('newApp.configs'),

  /**
   * Return formatted object to send it in request to server
   * @type {Object}
   */
  componentsFormatted: function () {
    var result = {};
    this.get('newApp.components').forEach(function (component) {
      result[component.get('name')] = {
        'num_instances': component.get('numInstances'),
        'yarn_memory': component.get('yarnMemory'),
        'yarn_cpu': component.get('yarnCPU')
      };
    });
    return result;
  }.property('newApp.components.@each'),

  /**
   * Send request to server to deploy new App
   * @return {$.ajax}
   */
  sendAppDataToServer: function () {
    if (!App.get('testMode')) {
      var self = this;
      var app = this.get('newApp');
      var componentsFormatted = this.get('componentsFormatted');
      return $.ajax({
        url: App.get('urlPrefix') + 'apps/',
        method: 'POST',
        data: JSON.stringify({
          type: app.get('appType.index'),
          name: app.get('name'),
          components: componentsFormatted,
          configs: app.get('configs')
        }),
        complete: function () {
          self.get('appWizardController').hidePopup();
        }
      });
    } else {
      this.get('appWizardController').hidePopup();
      return true;
    }
  },

  actions: {
    /**
     * Onclick handler for finish button
     */
    finish: function () {
      this.sendAppDataToServer();
    }
  }
});
