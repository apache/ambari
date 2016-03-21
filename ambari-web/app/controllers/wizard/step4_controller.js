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
var stringUtils = require('utils/string_utils');

App.WizardStep4Controller = Em.ArrayController.extend({

  name: 'wizardStep4Controller',

  /**
   * List of Services
   * @type {Object[]}
   */
  content: [],

  isValidating: false,

  /**
   * Check / Uncheck 'Select All' checkbox with one argument; Check / Uncheck all other checkboxes with more arguments
   * @type {bool}
   */
  isAllChecked: function(key, value) {
    if (arguments.length > 1) {
      this.filterProperty('isInstalled', false).setEach('isSelected', value);
      return value;
    } else {
      return this.filterProperty('isInstalled', false).
        filterProperty('isHiddenOnSelectServicePage', false).
        everyProperty('isSelected', true);
    }
  }.property('@each.isSelected'),

  /**
   * Is Submit button disabled
   * @type {bool}
   */
  isSubmitDisabled: function () {
    return this.filterProperty('isSelected', true).filterProperty('isInstalled', false).length === 0;
  }.property("@each.isSelected"),

  /**
   * List of validation errors. Look to #createError method for information
   * regarding object structure.
   *
   * @type {Object[]}
   */
  errorStack: [],

  /**
   * Drop errorStack content on selected state changes.
   **/
  clearErrors: function() {
    if (!this.get('isValidating')) {
      this.set('errorStack', []);
    }
  }.observes('@each.isSelected'),

  /**
   * Check if multiple distributed file systems were selected
   * @return {bool}
   * @method multipleDFSs
   */
  multipleDFSs: function () {
    var dfsServices = this.filterProperty('isDFS',true).filterProperty('isSelected',true);
	  return  dfsServices.length > 1;
  },

  /**
   * Check whether user selected Ambari Metrics service to install and go to next step
   * @param callback {function}
   * @method ambariMetricsValidation
   */
  ambariMetricsValidation: function (callback) {
    var ambariMetricsService = this.findProperty('serviceName', 'AMBARI_METRICS');
    if (ambariMetricsService && !ambariMetricsService.get('isSelected')) {
      this.addValidationError({
        id: 'ambariMetricsCheck',
        type: 'WARNING',
        callback: this.ambariMetricsCheckPopup,
        callbackParams: [callback]
      });
    }
  },

  /**
   * Check whether Ranger is selected and show installation requirements if yes
   * @param {function} callback
   * @method rangerValidation
   */
  rangerValidation: function (callback) {
    var rangerService = this.findProperty('serviceName', 'RANGER');
    if (rangerService && rangerService.get('isSelected') && !rangerService.get('isInstalled')) {
      this.addValidationError({
        id: 'rangerRequirements',
        type: 'WARNING',
        callback: this.rangerRequirementsPopup,
        callbackParams: [callback]
      });
    }
  },

  /**
   * Warn user if he tries to install Spark with HDP 2.2
   * @param {function} callback
   * @method sparkValidation
   */
  sparkValidation: function (callback) {
    var sparkService = this.findProperty('serviceName', 'SPARK');
    if (sparkService && sparkService.get('isSelected') && !sparkService.get('isInstalled') &&
      App.get('currentStackName') == 'HDP' && App.get('currentStackVersionNumber') == '2.2') {
      this.addValidationError({
        id: 'sparkWarning',
        type: 'WARNING',
        callback: this.sparkWarningPopup,
        callbackParams: [callback]
      });
    }
  },

  /**
   * Onclick handler for <code>Next</code> button.
   * @method submit
   */
  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      this.unSelectServices();
      this.setGroupedServices();
      if (this.validate()) {
        this.set('isValidating', false);
        this.set('errorStack', []);
        App.router.send('next');
      }
    }
  },

  /**
   * Set isSelected based on property doNotShowAndInstall
   */
  unSelectServices: function () {
    this.filterProperty('isSelected',true).filterProperty('doNotShowAndInstall', true).setEach('isSelected', false);
  },

  /**
   * Check if validation passed:
   *  - required file system services selected
   *  - dependencies between services
   *  - monitoring services selected (not required)
   *
   * @return {Boolean}
   * @method validate
   **/
  validate: function() {
    var result;
    var self = this;
    // callback function to reset `isValidating` needs to be called everytime when a popup from errorStack is dismissed/proceed by user action
    var callback = function() {
      self.set('isValidating', false);
    };
    this.set('isValidating', true);
    this.serviceDependencyValidation(callback);
    this.fileSystemServiceValidation(callback);
    if (this.get('wizardController.name') == 'installerController') {
      this.ambariMetricsValidation(callback);
    }
    this.rangerValidation(callback);
    this.sparkValidation(callback);
    if (!!this.get('errorStack').filterProperty('isShown', false).length) {
      var firstError =  this.get('errorStack').findProperty('isShown', false);
      this.showError(firstError);
      result = false;
    } else {
      result = true;
    }
    return result;
  },

  /**
   * Create error and push it to stack.
   *
   * @param {Object} errorObject - look to #createError
   * @return {Boolean}
   * @method addValidationError
   **/
  addValidationError: function(errorObject) {
    if (!this.get('errorStack').mapProperty('id').contains(errorObject.id)) {
      this.get('errorStack').push(this.createError(errorObject));
      return true;
    } else {
      return false;
    }
  },

  /**
   * Show current error by passed error object.
   *
   * @param {Object} errorObject
   * @method showError
   **/
  showError: function(errorObject) {
    return errorObject.callback.apply(errorObject.callbackContext, errorObject.callbackParams);
  },

  /**
   * Default primary button("Ok") callback for warning popups.
   *  Change isShown state for last shown error.
   *  Call #submit() method.
   *
   *  @param {function} callback
   *  @method onPrimaryPopupCallback
   **/
  onPrimaryPopupCallback: function(callback) {
    var firstError =  this.get('errorStack').findProperty('isShown', false);
    if (firstError) {
      firstError.isShown = true;
    }
    if (callback) {
      callback();
    }
    this.submit();
  },

  /**
   * Create error object with passed options.
   * Available options:
   *  id - {String}
   *  type - {String}
   *  isShowed - {Boolean}
   *  callback - {Function}
   *  callbackContext
   *  callbackParams - {Array}
   *
   * @param {Object} opt
   * @return {Object}
   * @method createError
   **/
  createError: function(opt) {
    var options = {
      // {String} error identifier
      id: '',
      // {String} type of error CRITICAL|WARNING
      type: 'CRITICAL',
      // {Boolean} error was shown
      isShown: false,
      // {Function} callback to execute
      callback: null,
      // context which execute from
      callbackContext: this,
      // {Array} params applied to callback
      callbackParams: []
    };
    $.extend(options, opt);
    return options;
  },

  /**
   * Checks if a filesystem is present in the Stack
   *
   * @method isDFSStack
   */
  isDFSStack: function () {
	  var bDFSStack = false;
    var dfsServices = ['HDFS', 'GLUSTERFS'];
    var availableServices = this.filterProperty('isInstalled',false);
    availableServices.forEach(function(service){
      if (dfsServices.contains(service.get('serviceName')) || service.get('serviceType') == 'HCFS' ) {
        console.log("found DFS " + service.get('serviceName'));
        bDFSStack=true;
      }
    },this);
    return bDFSStack;
  },

  /**
   * Checks if a filesystem is selected and only one filesystem is selected
   * @param {function} callback
   * @method isFileSystemCheckFailed
   */
  fileSystemServiceValidation: function(callback) {
    if(this.isDFSStack()){
      var primaryDFS = this.findProperty('isPrimaryDFS',true);
      if (primaryDFS) {
        var primaryDfsDisplayName = primaryDFS.get('displayNameOnSelectServicePage');
        var primaryDfsServiceName = primaryDFS.get('serviceName');
        if (this.multipleDFSs()) {
          var dfsServices = this.filterProperty('isDFS',true).filterProperty('isSelected',true).mapProperty('serviceName');
          var services = dfsServices.map(function (item){
          return  {
                     serviceName: item,
                     selected: item === primaryDfsServiceName
                  };
          });
          this.addValidationError({
                     id: 'multipleDFS',
                     callback: this.needToAddServicePopup,
                     callbackParams: [services, 'multipleDFS', primaryDfsDisplayName]
          });
        }
    }
  }},

  /**
   * Checks if a dependent service is selected without selecting the main service.
   * @param {function} callback
   * @method serviceDependencyValidation
   */
  serviceDependencyValidation: function(callback) {
    var selectedServices = this.filterProperty('isSelected',true);
    var missingDependencies = [];
    var missingDependenciesDisplayName = [];
    selectedServices.forEach(function(service){
      var requiredServices =  service.get('requiredServices');
      if (!!requiredServices && requiredServices.length) {
        requiredServices.forEach(function(_requiredService){
          var requiredService = this.findProperty('serviceName', _requiredService);
          if (requiredService && requiredService.get('isSelected') === false) {
            if(missingDependencies.indexOf(_requiredService) == -1 ) {
              missingDependencies.push(_requiredService);
              missingDependenciesDisplayName.push(requiredService.get('displayNameOnSelectServicePage'));
            }
          }
        },this);
      }
    },this);

    if (missingDependencies.length > 0) {
      for(var i = 0; i < missingDependencies.length; i++) {
        this.addValidationError({
          id: 'serviceCheck_' + missingDependencies[i],
          callback: this.needToAddServicePopup,
          callbackParams: [{serviceName: missingDependencies[i], selected: true}, 'serviceCheck', missingDependenciesDisplayName[i], callback]
        });
      }
    }
  },

  /**
   * Select co hosted services which not showed on UI.
   *
   * @method setGroupedServices
   **/
  setGroupedServices: function() {
    this.forEach(function(service){
      var coSelectedServices = service.get('coSelectedServices');
      coSelectedServices.forEach(function(groupedServiceName) {
        var groupedService = this.findProperty('serviceName', groupedServiceName);
        if (groupedService.get('isSelected') !== service.get('isSelected')) {
          groupedService.set('isSelected',service.get('isSelected'));
        }
      },this);
    },this);
  },

  /**
   * Select/deselect services
   * @param services array of objects
   *  <code>
   *    [
   *      {
   *        service: 'HDFS',
   *        selected: true
   *      },
   *      ....
   *    ]
   *  </code>
   * @param {string} i18nSuffix
   * @param {string} serviceName
   * @param {function} callback
   * @return {App.ModalPopup}
   * @method needToAddServicePopup
   */

  needToAddServicePopup: function(services, i18nSuffix, serviceName, callback) {
    if (!(services instanceof Array)) {
      services = [services];
    }
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step4.' + i18nSuffix + '.popup.header').format(serviceName),
      body: Em.I18n.t('installer.step4.' + i18nSuffix + '.popup.body').format(serviceName),
      onPrimary: function () {
        services.forEach(function (service) {
          self.findProperty('serviceName', service.serviceName).set('isSelected', service.selected);
        });
        self.onPrimaryPopupCallback(callback);
        this.hide();
      },
      onSecondary: function () {
        if (callback) {
          callback();
        }
        this._super();
      },
      onClose: function () {
        if (callback) {
          callback();
        }
        this._super();
      }
    });
  },

  /**
   * Show popup with info about not selected Ambari Metrics service
   * @param {function} callback
   * @return {App.ModalPopup}
   * @method ambariMetricsCheckPopup
   */
  ambariMetricsCheckPopup: function (callback) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step4.ambariMetricsCheck.popup.header'),
      body: Em.I18n.t('installer.step4.ambariMetricsCheck.popup.body'),
      primary: Em.I18n.t('common.proceedAnyway'),
      onPrimary: function () {
        self.onPrimaryPopupCallback(callback);
        this.hide();
      },
      onSecondary: function () {
        if (callback) {
          callback();
        }
        this._super();
      },
      onClose: function () {
        if (callback) {
          callback();
        }
        this._super();
      }
    });
  },

  /**
   * Show popup with installation requirements for Ranger service
   * @param {function} callback
   * @return {App.ModalPopup}
   * @method rangerRequirementsPopup
   */
  rangerRequirementsPopup: function (callback) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step4.rangerRequirements.popup.header'),
      bodyClass: Em.View.extend({
        templateName: require('templates/wizard/step4/step4_ranger_requirements_popup')
      }),
      primary: Em.I18n.t('common.proceed'),
      isChecked: false,
      disablePrimary: function () {
        return !this.get('isChecked');
      }.property('isChecked'),
      onPrimary: function () {
        self.onPrimaryPopupCallback(callback);
        this.hide();
      },
      onSecondary: function () {
        if (callback) {
          callback();
        }
        this._super();
      },
      onClose: function () {
        if (callback) {
          callback();
        }
        this._super();
      }
    });
  },

  /**
   * Show popup with Spark installation warning
   * @param {function} callback
   * @return {App.ModalPopup}
   * @method sparkWarningPopup
   */
  sparkWarningPopup: function (callback) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('common.warning'),
      body: Em.I18n.t('installer.step4.sparkWarning.popup.body'),
      primary: Em.I18n.t('common.proceed'),
      onPrimary: function () {
        self.onPrimaryPopupCallback(callback);
        this.hide();
      },
      onSecondary: function () {
        if (callback) {
          callback();
        }
        this._super();
      },
      onClose: function () {
        if (callback) {
          callback();
        }
       this._super();
      }
    });
  }
});
