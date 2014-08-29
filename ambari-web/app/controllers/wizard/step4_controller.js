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
   * Check whether all properties are selected
   * @type {bool}
   */
  isAll: function () {
    return this.filterProperty('isInstalled', false).
      filterProperty('isHiddenOnSelectServicePage', false).
      everyProperty('isSelected', true);
  }.property('@each.isSelected'),

  /**
   * Check whether none properties(minimum) are selected
   * @type {bool}
   */
  isMinimum: function () {
    return this.filterProperty('isInstalled', false).
      filterProperty('isHiddenOnSelectServicePage', false).
      everyProperty('isSelected', false);
  }.property('@each.isSelected'),

  /**
   * Drop errorStack content on selected state changes.
   **/
  clearErrors: function() {
    this.set('errorStack', []);
  }.observes('@each.isSelected'),
  /**
   * Onclick handler for <code>select all</code> link
   * @method selectAll
   */
  selectAll: function () {
    this.filterProperty('isInstalled', false).setEach('isSelected', true);
  },

  /**
   * Onclick handler for <code>select minimum</code> link
   * @method selectMinimum
   */
  selectMinimum: function () {
    this.filterProperty('isInstalled', false).setEach('isSelected', false);
  },

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
   * Check whether user turned on monitoring service and go to next step
   * @method validateMonitoring
   */
  serviceMonitoringValidation: function () {
    var monitoringServices =  this.filterProperty('isMonitoringService', true);
    var notSelectedService = monitoringServices.filterProperty('isSelected', false);
    if (!!notSelectedService.length) {
      notSelectedService = stringUtils.getFormattedStringFromArray(notSelectedService.mapProperty('displayNameOnSelectServicePage'));
      monitoringServices = stringUtils.getFormattedStringFromArray(monitoringServices.mapProperty('displayNameOnSelectServicePage'));
      this.addValidationError({
        id: 'monitoringCheck',
        type: 'WARNING',
        callback: this.monitoringCheckPopup,
        callbackParams: [notSelectedService, monitoringServices]
      });
    }
  },

  /**
   * Onclick handler for <code>Next</code> button.
   * @method submit
   */
  submit: function () {
    if (!this.get('isSubmitDisabled')) {
      this.setGroupedServices();
      if (this.validate()) {
        this.set('errorStack', []);
        App.router.send('next');
      }
    }
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
    this.serviceDependencyValidation();
    this.fileSystemServiceValidation();
    this.serviceMonitoringValidation();
    if (!!this.get('errorStack').filterProperty('isShown', false).length) {
      this.showError(this.get('errorStack').findProperty('isShown', false));
      return false;
    }
    return true;
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
   *  @method onPrimaryPopupCallback
   **/
  onPrimaryPopupCallback: function() {
    if (this.get('errorStack').someProperty('isShown', false)) {
      this.get('errorStack').findProperty('isShown', false).isShown = true;
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
   * Checks if a filesystem is selected and only one filesystem is selected
   *
   * @method isFileSystemCheckFailed
   */
  fileSystemServiceValidation: function() {
    var primaryDFS = this.findProperty('isPrimaryDFS',true);
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
  },

  /**
   * Checks if a dependent service is selected without selecting the main service.
   *
   * @method serviceDependencyValidation
   */
  serviceDependencyValidation: function() {
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
          callbackParams: [{serviceName: missingDependencies[i], selected: true}, 'serviceCheck', missingDependenciesDisplayName[i]]
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
   * @return {App.ModalPopup}
   * @method needToAddServicePopup
   */

  needToAddServicePopup: function(services, i18nSuffix, serviceName) {
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
        self.onPrimaryPopupCallback();
        this.hide();
      }
    });
  },

  /**
   * Show popup with info about not selected (but should be selected) services
   * @return {App.ModalPopup}
   * @method monitoringCheckPopup
   */
  monitoringCheckPopup: function (notSelectedServiceNames,monitoringServicesNames) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step4.monitoringCheck.popup.header'),
      body: Em.I18n.t('installer.step4.monitoringCheck.popup.body').format(notSelectedServiceNames,monitoringServicesNames),
      onPrimary: function () {
        self.onPrimaryPopupCallback();
        this.hide();
      }
    });
  }
});
