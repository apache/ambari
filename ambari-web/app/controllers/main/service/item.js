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
    App.router.get('backgroundOperationsController').showPopup();
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
      self.set('content.isPending', true);
      self.startStopPopupPrimary(serviceHealth);
    });
  },

  startStopPopupPrimary: function(serviceHealth) {
    var requestInfo = "";
    if(serviceHealth == "STARTED"){
      requestInfo = 'Start ' + this.get('content.displayName');
    }else{
      requestInfo = 'Stop ' + this.get('content.displayName');
    }

    App.ajax.send({
      'name': 'service.item.start_stop',
      'sender': this,
      'success': 'ajaxSuccess',
      'data': {
        'requestInfo':requestInfo,
        'serviceName': this.get('content.serviceName').toUpperCase(),
        'state': serviceHealth
      }
    });
    this.set('content.isStopDisabled',true);
    this.set('content.isStartDisabled',true);
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
      App.router.get('backgroundOperationsController').showPopup();
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
      App.router.get('backgroundOperationsController').showPopup();
    });
  },

  /**
   * On click callback for <code>run smoke test</code> button
   * @param event
   */
  runSmokeTest: function (event) {
    var self = this;
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
      App.router.get('backgroundOperationsController').showPopup();
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
    var component = App.HostComponent.find().findProperty('componentName', hostComponent.get('componentName'));
    console.log('In Reassign Master', hostComponent);
    var reassignMasterController = App.router.get('reassignMasterController');
    reassignMasterController.saveComponentToReassign(component);
    reassignMasterController.setCurrentStep('1');
    App.router.transitionTo('reassignMaster');
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
            this.set('content.isPending', false);
            backgroundOperations.forEach(function (services) {
                services.hosts.forEach(function (hosts) {
                    hosts.logTasks.forEach(function (logTasks) {
                        var service = service_components.findProperty('component_name', logTasks.Tasks.role);
                        if (service) {
                            if (serviceName == service.service_name) {
                                if (logTasks.Tasks.status == 'PENDING' || logTasks.Tasks.status == 'IN_PROGRESS') {
                                    this.set('content.isPending', true);
                                    return true;
                                }
                            }
                        }
                    }, this)
                }, this)
            }, this)
        }
        else {
            this.set('content.isPending', true);
        }
    }.observes('App.router.backgroundOperationsController.serviceTimestamp')

})