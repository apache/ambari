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
      this.ajaxCallBack(data.Requests.id, (JSON.parse(ajaxOptions.data)).ServiceInfo.state);
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
      App.router.get('backgroundOperationsController.eventsArray').push({
        "when": function (controller) {
          var result = (controller.getOperationsForRequestId(requestId).length == 0);
          console.log(config.s + 'Service.when = ', result)
          return result;
        },
        "do": function () {
          App.router.get('clusterController').loadUpdatedStatus();
        }
      });
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
      self.startStopPopupPrimary(serviceHealth);
    });
  },

  startStopPopupPrimary: function(serviceHealth) {
    App.ajax.send({
      'name': 'service.item.start_stop',
      'sender': this,
      'success': 'ajaxSuccess',
      'data': {
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
        'serviceName': this.get('content.serviceName').toUpperCase()
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
    console.log('In Reassign Master', hostComponent);
    App.router.get('reassignMasterController').saveComponentToReassign(hostComponent);
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
  }
})