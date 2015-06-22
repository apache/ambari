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

App.CreateAppWizardStep1Controller = Ember.Controller.extend(App.AjaxErrorHandler, {

  needs: "createAppWizard",

  appWizardController: Ember.computed.alias("controllers.createAppWizard"),

  /**
   * New App object
   * @type {App.SliderApp}
   */
  newApp: null,

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
   * Define if <code>frequency</code> value is valid
   * @type {Boolean}
   */
  isFrequencyError: false,

  /**
   * Error message describing frequency validation error
   * @type {String}
   */
  frequencyErrorMessage: '',

  /**
   * Determines if request for validating new App name is sent
   * If true - "Next" button should be disabled
   * Set to false after request is finished
   * @type {bool}
   */
  validateAppNameRequestExecuting: false,

  /**
   * Define if there are existing App types
   * @type {Boolean}
   */
  isAppTypesError: Em.computed.equal('availableTypes.content.length', 0),

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
   * <code>newApp.name</code> should pass validation and be not empty
   * @type {bool}
   */
  isSubmitDisabled: function () {
    return this.get('validateAppNameRequestExecuting') || !this.get('newApp.name') || this.get('isNameError') ||
      this.get('isFrequencyError') || this.get('isAppTypesError');
  }.property('newApp.name', 'isNameError', 'isAppTypesError', 'validateAppNameRequestExecuting', 'isFrequencyError'),

  /**
   * Initialize new App and set it to <code>newApp</code>
   * @method initializeNewApp
   */
  initializeNewApp: function () {
    var app = this.get('appWizardController.newApp'),
      properties = Em.A(['name', 'includeFilePatterns', 'excludeFilePatterns', 'frequency', 'queueName', 'specialLabel', 'selectedYarnLabel']),
      newApp = Ember.Object.create({
        appType: null,
        twoWaySSLEnabled: false,
        configs: {}
      });

    properties.forEach(function(p) {
      newApp.set(p, '');
    });
    newApp.set('selectedYarnLabel', 0);

    if (app) {
      properties.forEach(function(p) {
        newApp.set(p, app.get(p));
      });
    }

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
    var newAppName = this.get('newApp.name');
    if (newAppName) {
      // new App name should consist only of letters, numbers, '-', '_' and first character should be a letter
      if (!/^[a-z][a-z0-9_-]*$/.test(newAppName)) {
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
  }.observes('newApp.name'),

  /**
   * Validate <code>frequency</code> value
   * It should be numeric
   * @method frequencyValidator
   * @return {Boolean}
   */
  frequencyValidator: function () {
    var frequency = this.get('newApp.frequency');
    var isFrequencyError = frequency && /\D/.test(frequency);
    this.setProperties({
      isFrequencyError: isFrequencyError,
      frequencyErrorMessage: isFrequencyError ? Em.I18n.t('wizard.step1.frequencyError') : ''
    });
    return !isFrequencyError;
  }.observes('newApp.frequency'),

  /**
   * Proceed if app name has passed server validation
   * @method {validateAppNameSuccessCallback}
   */
  validateAppNameSuccessCallback: function () {
    var self = this;
    Em.run(function () {
      self.saveApp();
      self.get('appWizardController').nextStep();
    });
  },

  /**
   * Proceed if app name has failed server validation
   * @method {validateAppNameErrorCallback}
   */
  validateAppNameErrorCallback: function (request, ajaxOptions, error, opt, params) {
    if (request.status == 409) {
      Bootstrap.ModalManager.open(
        'app-name-conflict',
        Em.I18n.t('common.error'),
        Em.View.extend({
          classNames: ['alert', 'alert-danger'],
          template: Em.Handlebars.compile(Em.I18n.t('wizard.step1.validateAppNameError').format(params.name))
        }),
        [
          Em.Object.create({
            title: Em.I18n.t('ok'),
            dismiss: 'modal',
            type: 'success'
          })
        ],
        this
      );
    } else {
      this.defaultErrorHandler(request, opt.url, opt.type, true);
    }
  },

  /**
   * Complete-callback for validating newAppName request
   * @method validateAppNameCompleteCallback
   */
  validateAppNameCompleteCallback: function() {
    this.set('validateAppNameRequestExecuting', false);
  },

  /**
   * Save new application data to wizard controller
   * @method saveApp
   */
  saveApp: function () {
    var newApp = this.get('newApp');
    newApp.set('appType', this.get('selectedType'));
    newApp.set('configs', this.get('selectedType.configs'));
    newApp.set('predefinedConfigNames', Em.keys(this.get('selectedType.configs')));
    this.set('appWizardController.newApp', newApp);
  },

  actions: {
    submit: function () {
      this.set('validateAppNameRequestExecuting', true);
      return App.ajax.send({
        name: 'validateAppName',
        sender: this,
        data: {
          name: this.get('newApp.name')
        },
        success: 'validateAppNameSuccessCallback',
        error: 'validateAppNameErrorCallback',
        complete: 'validateAppNameCompleteCallback'
      });
    }
  }
});
