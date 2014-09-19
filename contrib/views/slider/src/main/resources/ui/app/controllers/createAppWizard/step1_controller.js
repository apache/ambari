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

App.CreateAppWizardStep1Controller = Ember.Controller.extend({

  needs: "createAppWizard",

  appWizardController: Ember.computed.alias("controllers.createAppWizard"),

  /**
   * New App object
   * @type {App.SliderApp}
   */
  newApp: null,

  /**
   * Name for new App
   * @type {String}
   */
  newAppName: '',

  /**
   * List of available types for App
   * @type {Array}
   */
  availableTypes: [],

  /**
   * Selected type for new App
   * @type {App.SliderAppType}
   */
  selectedType: null,

  /**
   * Define if <code>newAppName</code> pass validation
   * @type {Boolean}
   */
  isNameError: false,

  /**
   * Error message describing App name validation error
   * @type {String}
   */
  nameErrorMessage: '',

  /**
   * Define description depending on selected App type
   * @type {string}
   */
  typeDescription: function () {
    var selectedType = this.get('selectedType');
    return selectedType ? Em.I18n.t('wizard.step1.typeDescription').format(selectedType.get('displayName')) : '';
  }.property('selectedType'),

  /**
   * Define if submit button is disabled
   * <code>newAppName</code> should pass validation and be not empty
   * @type {bool}
   */
  isSubmitDisabled: function () {
    return !this.get('newAppName') || this.get('isNameError');
  }.property('newAppName', 'isNameError'),

  /**
   * Load all required data for step
   * @method loadStep
   */
  loadStep: function () {
    var sliderConfigs = App.SliderApp.store.all('sliderConfig'),
      gangliaClusters = sliderConfigs.findBy('viewConfigName', 'ganglia.custom.clusters'),
      gangliaHost = sliderConfigs.findBy('viewConfigName', 'ganglia.server.hostname');
    App.set('gangliaClusters', this.formatGangliaClusters(gangliaClusters.get('value')));
    App.set('gangliaHost', gangliaHost.get('value'));
    this.initializeNewApp();
    this.loadAvailableTypes();
  },

  /**
   * Format value for <code>gangliaClusters</code>
   * @param {string} prop
   * @returns {Array}
   * @method formatGangliaClusters
   */
  formatGangliaClusters: function(prop) {
    var gangliaCustomClusters = [];
    //parse CSV string with cluster names and ports
    if (!Em.isNone(prop)) {
      prop.replace(/\'/g, "").split(',').forEach(function(item, index){
        if (index % 2 === 0) {
          gangliaCustomClusters.push({
            name: item
          })
        }
        else {
          gangliaCustomClusters[gangliaCustomClusters.length - 1].port = parseInt(item);
        }
      });
    }
    return gangliaCustomClusters;
  },

  /**
   * Initialize new App and set it to <code>newApp</code>
   * @method initializeNewApp
   */
  initializeNewApp: function () {
    var newApp = Ember.Object.create({
      name: '',
      appType: null,
      configs: {}
    });
    this.set('newApp', newApp);
  },

  /**
   * Load all available types for App
   * @method loadAvailableTypes
   */
  loadAvailableTypes: function () {
    this.set('availableTypes', this.store.all('sliderAppType'));
  },

  /**
   * Validate <code>newAppName</code>
   * It should consist only of letters, numbers, '-', '_' and first character should be a letter
   * @method nameValidator
   * @return {Boolean}
   */
  nameValidator: function () {
    var newAppName = this.get('newAppName');
    if (newAppName) {
      // new App name should consist only of letters, numbers, '-', '_' and first character should be a letter
      if (!/^[A-Za-z][A-Za-z0-9_\-]*$/.test(newAppName)) {
        this.set('isNameError', true);
        this.set('nameErrorMessage', Em.I18n.t('wizard.step1.nameFormatError'));
        return false;
      }
      // new App name should be unique
      if (this.store.all('sliderApp').mapProperty('name').contains(newAppName)) {
        this.set('isNameError', true);
        this.set('nameErrorMessage', Em.I18n.t('wizard.step1.nameRepeatError'));
        return false;
      }
    }
    this.set('isNameError', false);
    return true;
  }.observes('newAppName'),

  /**
   * Save new application data to wizard controller
   * @method saveApp
   */
  saveApp: function () {
    var newApp = this.get('newApp');
    newApp.set('appType', this.get('selectedType'));
    newApp.set('name', this.get('newAppName'));
    newApp.set('configs', this.get('selectedType.configs'));
    newApp.set('predefinedConfigNames', Em.keys(this.get('selectedType.configs')));
    this.set('appWizardController.newApp', newApp);
  },

  actions: {
    submit: function () {
      this.saveApp();
      this.get('appWizardController').nextStep();
    }
  }
});
