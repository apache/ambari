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
var service_components = require('data/service_components');

App.MainServiceItemController = Em.Controller.extend({
  name: 'mainServiceItemController',

  /**
   * Callback functions for start and stop service have few differences
   *
   * Used with currentCallBack property
   */
  callBackConfig: {
    'STARTED': {
      'c': 'STARTING',
      'f': 'starting',
      'c2': 'live',
      'hs': 'started',
      's': 'start'
    },
    'INSTALLED': {
      'c': 'STOPPING',
      'f': 'stopping',
      'c2': 'dead',
      'hs': 'stopped',
      's': 'stop'
    }
  },
  /**
   * Success ajax response processing
   * @param data
   * @param ajaxOptions
   */
  ajaxSuccess: function(data, ajaxOptions) {
    if(data && data.Requests) {
      this.ajaxCallBack(data.Requests.id, (JSON.parse(ajaxOptions.data)).Body.ServiceInfo.state);
    }
    else {
      console.log('cannot get request id from ', data);
    }
  },
  /**
   * Common method for ajax (start/stop service) responses
   * @param requestId
   * @param serviceHealth
   */
  ajaxCallBack: function(requestId, serviceHealth) {
    var config = this.get('callBackConfig')[serviceHealth];
    var self = this;
    console.log('Send request for ' + config.c + ' successfully');
    if (App.testMode) {
      self.set('content.workStatus', App.Service.Health[config.f]);
      self.get('content.hostComponents').setEach('workStatus', App.HostComponentStatus[config.f]);
      setTimeout(function () {
        self.set('content.workStatus', App.Service.Health[config.c2]);
        self.get('content.hostComponents').setEach('workStatus', App.HostComponentStatus[config.hs]);
      }, App.testModeDelayForActions);
    }
    else {
      App.router.get('clusterController').loadUpdatedStatusDelayed(500);// @todo check working without param 500
    }
    // load data (if we need to show this background operations popup) from persist
    App.router.get('applicationController').dataLoading().done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  },
  /**
   * Confirmation popup for start/stop services
   * @param event
   * @param serviceHealth - 'STARTED' or 'INSTALLED'
   */
  startStopPopup: function(event, serviceHealth) {
    if ($(event.target).hasClass('disabled') || $(event.target.parentElement).hasClass('disabled')) {
      return;
    }
    var self = this;
    App.showConfirmationPopup(function() {
      self.set('isPending', true);
      self.startStopPopupPrimary(serviceHealth);
    });
  },

  startStopPopupPrimary: function (serviceHealth) {
    var requestInfo = "";
    if (serviceHealth == "STARTED") {
      requestInfo = '_PARSE_.START.' + this.get('content.serviceName');
    } else {
      requestInfo = '_PARSE_.STOP.' + this.get('content.serviceName');
    }

    App.ajax.send({
      'name': 'service.item.start_stop',
      'sender': this,
      'success': 'ajaxSuccess',
      'data': {
        'requestInfo': requestInfo,
        'serviceName': this.get('content.serviceName').toUpperCase(),
        'state': serviceHealth
      }
    });
    this.set('isStopDisabled', true);
    this.set('isStartDisabled', true);
  },

  /**
   * On click callback for <code>start service</code> button
   * @param event
   */
  startService: function (event) {
    this.startStopPopup(event, App.HostComponentStatus.started);
  },

  /**
   * On click callback for <code>stop service</code> button
   * @param event
   */
  stopService: function (event) {
    this.startStopPopup(event, App.HostComponentStatus.stopped);
  },

  /**
   * On click callback for <code>run rebalancer</code> button
   * @param event
   */
  runRebalancer: function (event) {
    var self = this;
    App.showConfirmationPopup(function() {
      self.content.set('runRebalancer', true);
      // load data (if we need to show this background operations popup) from persist
      App.router.get('applicationController').dataLoading().done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    });
  },

  /**
   * On click callback for <code>run compaction</code> button
   * @param event
   */
  runCompaction: function (event) {
    var self = this;
    App.showConfirmationPopup(function() {
      self.content.set('runCompaction', true);
      // load data (if we need to show this background operations popup) from persist
      App.router.get('applicationController').dataLoading().done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    });
  },

  /**
   * On click callback for <code>run smoke test</code> button
   * @param event
   */
  runSmokeTest: function (event) {
    var self = this;
    if (this.get('content.serviceName') === 'MAPREDUCE2' && !App.Service.find('YARN').get('isStarted')) {
      App.showAlertPopup(Em.I18n.t('common.error'), Em.I18n.t('services.mapreduce2.smokeTest.requirement'));
      return;
    }
    App.showConfirmationPopup(function() {
      self.runSmokeTestPrimary();
    });
  },

  runSmokeTestPrimary: function() {
    App.ajax.send({
      'name': 'service.item.smoke',
      'sender': this,
      'success':'runSmokeTestSuccessCallBack',
      'data': {
        'serviceName': this.get('content.serviceName'),
        'displayName': this.get('content.displayName'),
        'actionName': this.get('content.serviceName') === 'ZOOKEEPER' ? 'ZOOKEEPER_QUORUM_SERVICE_CHECK' : this.get('content.serviceName') + '_SERVICE_CHECK'
      }
    });
  },

  runSmokeTestSuccessCallBack: function(data) {
    if (data.Requests.id) {
      // load data (if we need to show this background operations popup) from persist
      App.router.get('applicationController').dataLoading().done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    }
    else {
      console.warn('error during runSmokeTestSuccessCallBack');
    }
  },

  /**
   * On click callback for <code>Reassign <master component></code> button
   * @param hostComponent
   */
  reassignMaster: function (hostComponent) {
    var component = App.HostComponent.find().findProperty('componentName', hostComponent);
    console.log('In Reassign Master', hostComponent);
    var reassignMasterController = App.router.get('reassignMasterController');
    reassignMasterController.saveComponentToReassign(component);
    reassignMasterController.getSecurityStatus();
    reassignMasterController.setCurrentStep('1');
    App.router.transitionTo('reassign');
  },

  manageConfigurationGroups: function () {
    var serviceName = this.get('content.serviceName');
    var displayName = this.get('content.displayName');
    App.ModalPopup.show({
      header: Em.I18n.t('services.service.config_groups_popup.header').format(displayName),
      bodyClass: App.MainServiceManageConfigGroupView.extend({
        serviceName: serviceName,
        controllerBinding: 'App.router.manageConfigGroupsController'
      }),
      classNames: ['sixty-percent-width-modal', 'manage-configuration-group-popup'],
      primary: Em.I18n.t('common.save'),
      onPrimary: function() {
        // Save modified config-groups
        var modifiedConfigGroups = this.get('subViewController.hostsModifiedConfigGroups');
        console.log("manageConfigurationGroups(): Saving modified config-groups: ", modifiedConfigGroups);
        var self = this;
        var errors = [];
        var clearHostsPutCount = modifiedConfigGroups.toClearHosts.length;
        var setHostsPutCount = modifiedConfigGroups.toSetHosts.length;
        var finishFunction = function(error) {
          if (error != null) {
            errors.push(error);
          }
          if (--clearHostsPutCount <= 0) {
            // Done with all the clear hosts PUTs
            if (--setHostsPutCount < 0) {
              // Done with all the PUTs
              if (errors.length > 0) {
                console.log(errors);
                self.get('subViewController').set('errorMessage',
                    errors.join(". "));
              } else {
                self.hide();
              }
            } else {
              App.config.updateConfigurationGroup(modifiedConfigGroups.toSetHosts[setHostsPutCount], finishFunction, finishFunction);
            }
          }
        };
        this.updateConfigGroupOnServicePage();
        modifiedConfigGroups.toClearHosts.forEach(function (cg) {
          App.config.clearConfigurationGroupHosts(cg, finishFunction, finishFunction);
        });
      },
      onSecondary: function () {
        this.updateConfigGroupOnServicePage();
        this.hide();
      },
      onClose: function () {
        this.updateConfigGroupOnServicePage();
        this.hide();
      },
      subViewController: function(){
        return App.router.get('manageConfigGroupsController');
      }.property('App.router.manageConfigGroupsController'),
      updateConfigGroupOnServicePage: function () {
        var mainServiceInfoConfigsController = App.get('router.mainServiceInfoConfigsController');
        var selectedConfigGroup = mainServiceInfoConfigsController.get('selectedConfigGroup');
        var managedConfigGroups = this.get('subViewController.configGroups');

        //check whether selectedConfigGroup was selected
        if(!selectedConfigGroup){
          return;
        }

        if(selectedConfigGroup.isDefault) {
          mainServiceInfoConfigsController.set('selectedConfigGroup',  managedConfigGroups.findProperty('isDefault', true));
        }else{
          selectedConfigGroup = managedConfigGroups.findProperty('id', selectedConfigGroup.id);
          if(selectedConfigGroup){
            mainServiceInfoConfigsController.set('selectedConfigGroup', selectedConfigGroup);
          }else{
            mainServiceInfoConfigsController.set('selectedConfigGroup',  managedConfigGroups.findProperty('isDefault', true));
          }
        }
        mainServiceInfoConfigsController.set('configGroups',this.get('subViewController.configGroups'));
      },
      updateButtons: function(){
        var modified = this.get('subViewController.isHostsModified');
        this.set('enablePrimary', modified);
      }.observes('subViewController.isHostsModified'),
      secondary : Em.I18n.t('common.cancel'),
      didInsertElement: function () {}
    });
  },

  /**
   * On click callback for <code>action</code> dropdown menu
   * Calls runSmokeTest, runRebalancer, runCompaction or reassignMaster depending on context
   * @param event
   */
  doAction: function (event) {
    if ($(event.target).hasClass('disabled') || $(event.target.parentElement).hasClass('disabled')) {
      return;
    }
    var methodName = event.context.action;
    var context = event.context.context;
    if (methodName) {
      this[methodName](context);
    }
  },

  setStartStopState: function () {
    var serviceName = this.get('content.serviceName');
    var backgroundOperations = App.router.get('backgroundOperationsController.services');
    if (backgroundOperations.length > 0) {
      for (var i = 0; i < backgroundOperations.length; i++) {
        if (backgroundOperations[i].isRunning &&
            (backgroundOperations[i].dependentService === "ALL_SERVICES" ||
             backgroundOperations[i].dependentService === serviceName)) {
          this.set('isPending', true);
          return;
        }
      }
      this.set('isPending', false);
    } else {
      this.set('isPending', true);
    }
  }.observes('App.router.backgroundOperationsController.serviceTimestamp'),

  isServiceRestartable: function() {
    return this.get('content.serviceName') !== "FLUME";
  }.property('content.serviceName'),

  isStartDisabled: function () {
    if(this.get('isPending')) return true;
    return !(this.get('content.healthStatus') == 'red');
  }.property('content.healthStatus','isPending'),

  isStopDisabled: function () {
    if(this.get('isPending')) return true;
    if (!App.HostComponent.find().someProperty('componentName', 'SECONDARY_NAMENODE') && this.get('content.serviceName') == 'HDFS' && this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
      return false;
    }
    return (this.get('content.healthStatus') != 'green');
  }.property('content.healthStatus','isPending'),

  isPending:true

});
