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

App.CreateAppWizardStep4Controller = Ember.ObjectController.extend(App.AjaxErrorHandler, {

  needs: "createAppWizard",

  appWizardController: Ember.computed.alias("controllers.createAppWizard"),

  /**
   * New App object
   * @type {App.SliderApp}
   */
  newApp: null,

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
      result = result.replace(/",/g, '",\n');
    }
    return result;
  }.property('newApp.configs'),

  /**
   * Return formatted object to send it in request to server
   * @type {Object[]}
   */
  componentsFormatted: function () {
    return this.get('newApp.components').map(function (component) {
      return {
        'id': component.get('name'),
        'instanceCount': component.get('numInstances'),
        'yarnMemory': component.get('yarnMemory'),
        'yarnCpuCores': component.get('yarnCPU'),
        'priority': component.get('priority')
      };
    });
  }.property('newApp.components.@each'),

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
  },

  /**
   * Send request to server to deploy new App
   * @return {$.ajax}
   * @method sendAppDataToServer
   */
  sendAppDataToServer: function () {
    var app = this.get('newApp');
    return App.ajax.send({
      name: 'createNewApp',
      sender: this,
      data: {
        data: {
          typeName: app.get('appType.index'),
          typeVersion: app.get('appType.version'),
          name: app.get('name'),
          typeComponents: this.get('componentsFormatted'),
          typeConfigs: app.get('configs')
        }
      },
      success: 'sendAppDataToServerSuccessCallback'
    });
  },

  /**
   * Success-callback for "create new app"-request
   * @method sendAppDataToServerSuccessCallback
   */
  sendAppDataToServerSuccessCallback: function() {
    this.get('appWizardController').hidePopup();
  },

  actions: {

    /**
     * Onclick handler for finish button
     * @method finish
     */
    finish: function () {
      this.sendAppDataToServer();
    }
  }
});
