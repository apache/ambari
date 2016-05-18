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

App.WizardStep4Controller = Em.ArrayController.extend({

  name: 'wizardStep4Controller',

  /**
   * List of Services
   * @type {Object[]}
   */
  content: [],

  /**
   * Check / Uncheck 'Select All' checkbox with one argument; Check / Uncheck all other checkboxes with more arguments
   * @type {bool}
   */
  isAllChecked: function(key, value) {
    if (arguments.length > 1) {
      this.filterProperty('isInstalled', false).setEach('isSelected', value);
      return value;
    }
    return this.filterProperty('isInstalled', false).
      filterProperty('isHiddenOnSelectServicePage', false).
      everyProperty('isSelected', true);
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
    if (!this.get('errorStack').someProperty('isAccepted', false)) {
      this.set('errorStack', []);
    }
  }.observes('@each.isSelected'),

  /**
   * Check if multiple distributed file systems were selected
   * @return {bool}
   * @method multipleDFSs
   */
  multipleDFSs: function () {
    return this.filterProperty('isDFS',true).filterProperty('isSelected',true).length > 1;
  },

  /**
   * Check whether Ranger is selected and show installation requirements if yes
   * @param {function} callback
   * @method rangerValidation
   */
  rangerValidation: function (callback) {
    var rangerService = this.findProperty('serviceName', 'RANGER');
    if (rangerService && !rangerService.get('isInstalled')) {
      if(rangerService.get('isSelected')) {
        this.addValidationError({
          id: 'rangerRequirements',
          type: 'WARNING',
          callback: this.rangerRequirementsPopup,
          callbackParams: [callback]
        });
      }
      else {
        //Ranger is selected, remove the Ranger error from errorObject array
        var rangerError = this.get('errorStack').filterProperty('id',"rangerRequirements");
        if(rangerError)
        {
           this.get('errorStack').removeObject(rangerError[0]);
        }
      }
    }
  },

  /**
   * Warn user if he tries to install Spark with HDP 2.2
   * @param {function} callback
   * @method sparkValidation
   */
  sparkValidation: function (callback) {
    var sparkService = this.findProperty('serviceName', 'SPARK');
    if (sparkService && !sparkService.get('isInstalled') &&
      App.get('currentStackName') === 'HDP' && App.get('currentStackVersionNumber') === '2.2') {
      if(sparkService.get('isSelected')) {
        this.addValidationError({
          id: 'sparkWarning',
          type: 'WARNING',
          callback: this.sparkWarningPopup,
          callbackParams: [callback]
        });
      }
      else {
        //Spark is selected, remove the Spark error from errorObject array
        var sparkError = this.get('errorStack').filterProperty('id',"sparkWarning");
        if(sparkError)
        {
           this.get('errorStack').removeObject(sparkError[0]);
        }
      }
    }
  },

  /**
   * Onclick handler for <code>Next</code> button.
   * Disable 'Next' button while it is already under process. (using Router's property 'nextBtnClickInProgress')
   * @method submit
   */
  submit: function () {
    if(App.router.nextBtnClickInProgress){
      return;
    }
    if (!this.get('isSubmitDisabled')) {
      this.unSelectServices();
      this.setGroupedServices();
      if (this.validate()) {
        App.set('router.nextBtnClickInProgress', true);
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
  validate: function () {
    var result;
    var self = this;
    // callback function to reset `isAccepted` needs to be called everytime when a popup from errorStack is dismissed/proceed by user action
    var callback = function (id) {
      var check = self.get('errorStack').findProperty('id', id);
      if (check) {
        check.isAccepted = true;
      }
    };
    this.serviceDependencyValidation(callback);
    this.fileSystemServiceValidation(callback);
    if (this.get('wizardController.name') === 'installerController') {
      this.serviceValidation(callback, 'AMBARI_METRICS', 'ambariMetricsCheck');
      this.serviceValidation(callback, 'SMARTSENSE', 'smartSenseCheck');
    }
    this.rangerValidation(callback);
    this.sparkValidation(callback);
    if (!!this.get('errorStack').filterProperty('isShown', false).length) {
      var firstError = this.get('errorStack').findProperty('isShown', false);
      this.showError(firstError);
      result = false;
    } else {
      result = true;
    }
    return result;
  },

  /**
   * Check whether user selected service to install and go to next step
   * @param callback {Function}
   * @param serviceName {string}
   * @param id {string}
   * @method serviceValidation
   */
  serviceValidation: function(callback, serviceName, id) {
    var service = this.findProperty('serviceName', serviceName);
    if (service) {
      if (!service.get('isSelected')) {
        this.addValidationError({
          id: id,
          type: 'WARNING',
          callback: this.serviceCheckPopup,
          callbackParams: [callback]
        });
      }
      else {
        //metrics is selected, remove the metrics error from errorObject array
        var metricsError = this.get('errorStack').filterProperty('id', id);
        if (metricsError) {
          this.get('errorStack').removeObject(metricsError[0]);
        }
      }
    }
  },

  /**
   * Create error and push it to stack.
   *
   * @param {Object} errorObject - look to #createError
   * @return {Boolean}
   * @method addValidationError
   **/
  addValidationError: function (errorObject) {
    if (!this.get('errorStack').someProperty('id', errorObject.id)) {
      this.get('errorStack').push(this.createError(errorObject));
      return true;
    }
    return false;
  },

  /**
   * Show current error by passed error object.
   *
   * @param {Object} errorObject
   * @method showError
   **/
  showError: function (errorObject) {
    return errorObject.callback.apply(errorObject.callbackContext, errorObject.callbackParams.concat(errorObject.id));
  },

  /**
   * Default primary button("Ok") callback for warning popups.
   *  Change isShown state for last shown error.
   *  Call #submit() method.
   *
   *  @param {function} callback
   *  @param {string} id
   *  @method onPrimaryPopupCallback
   **/
  onPrimaryPopupCallback: function(callback, id) {
    var firstError = this.get('errorStack').findProperty('isShown', false);
    if (firstError) {
      firstError.isShown = true;
    }
    if (callback) {
      callback(id);
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
      // {Boolean} error was accepted by user
      isAccepted: false,
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
            return {
              serviceName: item,
              selected: item === primaryDfsServiceName
            };
          });
          this.addValidationError({
            id: 'multipleDFS',
            callback: this.needToAddServicePopup,
            callbackParams: [services, 'multipleDFS', primaryDfsDisplayName, callback]
          });
        }
        else
        {
          //if multiple DFS are not selected, remove the related error from the error array 
          var fsError = this.get('errorStack').filterProperty('id',"multipleDFS");
          if(fsError)
          {
             this.get('errorStack').removeObject(fsError[0]);
          }
        }
      }
    }
  },

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
      var requiredServices = service.get('requiredServices');
      if (!!requiredServices && requiredServices.length) {
        requiredServices.forEach(function(_requiredService){
          var requiredService = this.findProperty('serviceName', _requiredService);
          if (requiredService) {
            if(requiredService.get('isSelected') === false)
            {
               if(missingDependencies.indexOf(_requiredService) == -1 ) {
                 missingDependencies.push(_requiredService);
                 missingDependenciesDisplayName.push(requiredService.get('displayNameOnSelectServicePage'));
               }
            }
            else
            { 
               //required service is selected, remove the service error from errorObject array 
               var serviceName = requiredService.get('serviceName');
               var serviceError = this.get('errorStack').filterProperty('id',"serviceCheck_"+serviceName);
               if(serviceError)
               {
                  this.get('errorStack').removeObject(serviceError[0]);
               }
            } 
          }
        },this);
      }
    },this);

    //create a copy of the errorStack, reset it
    //and add the dependencies in the correct order
    var errorStackCopy = this.get('errorStack');
    this.set('errorStack', []);    
      
    if (missingDependencies.length > 0) {
      for(var i = 0; i < missingDependencies.length; i++) {
        this.addValidationError({
          id: 'serviceCheck_' + missingDependencies[i],
          callback: this.needToAddServicePopup,
          callbackParams: [{serviceName: missingDependencies[i], selected: true}, 'serviceCheck', missingDependenciesDisplayName[i], callback]
        });
      }      
    }
    
    //iterate through the errorStackCopy array and add to errorStack array, the error objects that have no matching entry in the errorStack 
    //and that are not related to serviceChecks since serviceCheck errors have already been added when iterating through the missing dependencies list
    //Only add Ranger, Ambari Metrics, Spark and file system service validation errors if they exist in the errorStackCopy array
    var ctr = 0;
    while(ctr < errorStackCopy.length) {
      //no matching entry in errorStack array
      if (!this.get('errorStack').someProperty('id', errorStackCopy[ctr].id)) {
        //not serviceCheck error
        if(!errorStackCopy[ctr].id.startsWith('serviceCheck_')) {
          this.get('errorStack').push(this.createError(errorStackCopy[ctr]));
        }        
      }
      ctr++;
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
   * @param {string} id
   * @return {App.ModalPopup}
   * @method needToAddServicePopup
   */

  needToAddServicePopup: function (services, i18nSuffix, serviceName, callback, id) {
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
        self.onPrimaryPopupCallback(callback, id);
        this.hide();
      },
      onSecondary: function () {
        if (callback) {
          callback(id);
        }
        this._super();
      },
      onClose: function () {
        if (callback) {
          callback(id);
        }
        this._super();
      }
    });
  },

  /**
   * Show popup with info about not selected service
   * @param {function} callback
   * @param {string} id
   * @return {App.ModalPopup}
   * @method serviceCheckPopup
   */
  serviceCheckPopup: function (callback, id) {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step4.limitedFunctionality.popup.header'),
      body: Em.I18n.t('installer.step4.' + id + '.popup.body'),
      primary: Em.I18n.t('common.proceedAnyway'),
      primaryClass: 'btn-warning',
      onPrimary: function () {
        self.onPrimaryPopupCallback(callback);
        this.hide();
      },
      onSecondary: function () {
        if (callback) {
          callback(id);
        }
        this._super();
      },
      onClose: function () {
        if (callback) {
          callback(id);
        }
        this._super();
      }
    });
  },

  /**
   * Show popup with installation requirements for Ranger service
   * @param {function} callback
   * @param {string} id
   * @return {App.ModalPopup}
   * @method rangerRequirementsPopup
   */
  rangerRequirementsPopup: function (callback, id) {
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
          callback(id);
        }
        this._super();
      },
      onClose: function () {
        if (callback) {
          callback(id);
        }
        this._super();
      }
    });
  },

  /**
   * Show popup with Spark installation warning
   * @param {function} callback
   * @param {string} id
   * @return {App.ModalPopup}
   * @method sparkWarningPopup
   */
  sparkWarningPopup: function (callback, id) {
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
          callback(id);
        }
        this._super();
      },
      onClose: function () {
        if (callback) {
          callback(id);
        }
       this._super();
      }
    });
  }
});
