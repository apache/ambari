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
   * Check whether we should turn on <code>HDFS or GLUSTERFS</code> service
   * @return {bool}
   * @method noDFSs
   */
  noDFSs: function () {
    return  !this.filterProperty('isDFS',true).someProperty('isSelected',true);
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
   * Check do we have any monitoring service turned on
   * @return {bool}
   * @method gangliaOrNagiosNotSelected
   */
  isMonitoringServiceNotSelected: function () {
    var stackMonitoringServices = this.filterProperty('isMonitoringService',true);
    return stackMonitoringServices.someProperty('isSelected',false);
  },

  /**
   * Check whether user turned on monitoring service and go to next step
   * @method validateMonitoring
   */
  validateMonitoring: function () {
    var monitoringServices =  this.filterProperty('isMonitoringService',true);
    var notSelectedService = monitoringServices.filterProperty('isSelected',false);
    if (!!notSelectedService.length) {
      notSelectedService = stringUtils.getFormattedStringFromArray(notSelectedService.mapProperty('displayNameOnSelectServicePage'));
      monitoringServices = stringUtils.getFormattedStringFromArray(monitoringServices.mapProperty('displayNameOnSelectServicePage'));
      this.monitoringCheckPopup(notSelectedService,monitoringServices);
    } else {
      App.router.send('next');
    }
  },

  /**
   * Onclick handler for <code>Next</code> button
   * @method submit
   */
  submit: function () {
    this.setGroupedServices();
    if (!this.get("isSubmitDisabled") && !this.isSubmitChecksFailed()) {
      this.validateMonitoring();
    }
  },

  /**
   * @method  {isSubmitChecksFailed} Do the required checks on Next button click event
   * @returns {boolean}
   */
  isSubmitChecksFailed: function() {
    return this.isFileSystemCheckFailed() || this.isServiceDependencyCheckFailed();
  },

  /**
   * @method: isFileSystemCheckFailed - Checks if a filesystem is selected and only one filesystem is selected
   * @return: {boolean}
   */
  isFileSystemCheckFailed: function() {
    var isCheckFailed = false;
    var primaryDFS = this.findProperty('isPrimaryDFS',true);
    var primaryDfsDisplayName = primaryDFS.get('displayNameOnSelectServicePage');
    var primaryDfsServiceName = primaryDFS.get('serviceName');
     if (this.noDFSs()) {
       isCheckFailed = true;
       this.needToAddServicePopup.apply(this, [{serviceName: primaryDfsServiceName, selected: true},'fsCheck',primaryDfsDisplayName]);
     } else if (this.multipleDFSs()) {
       var dfsServices = this.filterProperty('isDFS',true).filterProperty('isSelected',true).mapProperty('serviceName');
       var services = dfsServices.map(function (item){
         var mappedObj = {
           serviceName: item,
           selected: false
         };
         if (item ===  primaryDfsServiceName) {
           mappedObj.selected = true;
         }
         return mappedObj;
       });
       isCheckFailed = true;
       this.needToAddServicePopup.apply(this, [services,'multipleDFS',primaryDfsDisplayName]);
     }
    return isCheckFailed;
  },

  /**
   * @method: isServiceDependencyCheckFailed - Checks if a dependent service is selected without selecting the main service
   * @return {boolean}
   */
  isServiceDependencyCheckFailed: function() {
    var isCheckFailed = false;
    var notSelectedServices = this.filterProperty('isSelected',false);
    notSelectedServices.forEach(function(service){
      var showWarningPopup;
      var dependentServices =  service.get('dependentServices');
      if (!!dependentServices) {
        showWarningPopup = false;
        dependentServices.forEach(function(_dependentService){
          var dependentService = this.findProperty('serviceName', _dependentService);
          if (dependentService && dependentService.get('isSelected') === true) {
            showWarningPopup = true;
            isCheckFailed = true;
          }
        },this);
        if (showWarningPopup) {
          this.needToAddServicePopup.apply(this, [{serviceName: service.get('serviceName'), selected: true},'serviceCheck',service.get('displayNameOnSelectServicePage')]);
        }
      }
    },this);
    return isCheckFailed;
  },

  setGroupedServices: function() {
    this.forEach(function(service){
      var coSelectedServices = service.get('coSelectedServices');
      coSelectedServices.forEach(function(groupedServiceName) {
        var groupedService = this.findProperty('serviceName', groupedServiceName);
        groupedService.set('isSelected',service.get('isSelected'));
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
        this.hide();
        self.submit();
      }
    });
  },

  /**
   * Show popup with info about not selected (but should be selected) services
   * @return {App.ModalPopup}
   * @method monitoringCheckPopup
   */
  monitoringCheckPopup: function (notSelectedServiceNames,monitoringServicesNames) {
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step4.monitoringCheck.popup.header'),
      body: Em.I18n.t('installer.step4.monitoringCheck.popup.body').format(notSelectedServiceNames,monitoringServicesNames),
      onPrimary: function () {
        this.hide();
        App.router.send('next');
      }
    });
  }
});
