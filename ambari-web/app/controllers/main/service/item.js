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
   * Send specific command to server
   * @param url
   * @param data Object to send
   */
  sendCommandToServer : function(url, method, postData, callback){
    var url =  (App.testMode) ?
      '/data/wizard/deploy/poll_1.json' : //content is the same as ours
      App.apiPrefix + '/clusters/' + App.router.getClusterName() + url;

    method = App.testMode ? 'GET' : method;

    $.ajax({
      type: method,
      url: url,
      data: (postData != null) ? JSON.stringify(postData) : null,
      dataType: 'json',
      timeout: App.timeout,
      success: function(data){
        if(data && data.Requests){
          callback(data.Requests.id);
        } else{
          callback(null);
          console.log('cannot get request id from ', data);
        }
      },

      error: function (request, ajaxOptions, error) {
        //do something
        callback(null);
        console.log('error on change component host status')
      },

      statusCode: require('data/statusCodes')
    });
  },

  /**
   * On click callback for <code>start service</code> button
   * @param event
   */
  startService: function (event) {
    if ($(event.target).hasClass('disabled') || $(event.target.parentElement).hasClass('disabled')) {
      return;
    }

    var self = this;
    App.showConfirmationPopup(function(){
      self.sendCommandToServer('/services/' + self.get('content.serviceName').toUpperCase(), "PUT", {
        ServiceInfo: {
          state: 'STARTED'
        }
      }, function (requestId) {

        if (!requestId) {
          return;
        }
        console.log('Send request for STARTING successfully');

        if (App.testMode) {
          self.set('content.workStatus', App.Service.Health.starting);
          self.get('content.hostComponents').setEach('workStatus', App.HostComponentStatus.starting);
          setTimeout(function () {
            self.set('content.workStatus', App.Service.Health.live);
            self.get('content.hostComponents').setEach('workStatus', App.HostComponentStatus.started);
          }, App.testModeDelayForActions);
        } else {
          App.router.get('clusterController').loadUpdatedStatusDelayed(500);
          App.router.get('backgroundOperationsController.eventsArray').push({
            "when": function (controller) {
              var result = (controller.getOperationsForRequestId(requestId).length == 0);
              console.log('startService.when = ', result)
              return result;
            },
            "do": function () {
              App.router.get('clusterController').loadUpdatedStatus();
            }
          });
        }
        App.router.get('backgroundOperationsController').showPopup();
      });
      self.set('content.isStopDisabled',true);
      self.set('content.isStartDisabled',true);
    });
  },

  /**
   * On click callback for <code>stop service</code> button
   * @param event
   */
  stopService: function (event) {
    if ($(event.target).hasClass('disabled') || $(event.target.parentElement).hasClass('disabled')) {
      return;
    }

    var self = this;
    App.showConfirmationPopup(function(){
      self.sendCommandToServer('/services/' + self.get('content.serviceName').toUpperCase(), "PUT",{
        ServiceInfo:{
          state: 'INSTALLED'
        }
      }, function (requestId) {
        if (!requestId) {
          return
        }
        console.log('Send request for STOPPING successfully');
        if (App.testMode) {
          self.set('content.workStatus', App.Service.Health.stopping);
          self.get('content.hostComponents').setEach('workStatus', App.HostComponentStatus.stopping);
          setTimeout(function () {
            self.set('content.workStatus', App.Service.Health.dead);
            self.get('content.hostComponents').setEach('workStatus', App.HostComponentStatus.stopped);
          }, App.testModeDelayForActions);
        } else {
          App.router.get('clusterController').loadUpdatedStatusDelayed(500);
          App.router.get('backgroundOperationsController.eventsArray').push({
            "when": function (controller) {
              var result = (controller.getOperationsForRequestId(requestId).length == 0);
              console.log('stopService.when = ', result)
              return result;
            },
            "do": function () {
              App.router.get('clusterController').loadUpdatedStatus();
            }
          });
        }
        App.router.get('backgroundOperationsController').showPopup();
      });
      self.set('content.isStopDisabled',true);
      self.set('content.isStartDisabled',true);
    });
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
    App.showConfirmationPopup(function(){
      var serviceName = self.get('content.serviceName').toUpperCase();
      var smokeName = serviceName + "_SERVICE_CHECK";
      self.sendCommandToServer('/services/' + serviceName + '/actions/' + smokeName, "POST",
        null,
        function (requestId) {

          if (!requestId) {
            return;
          }
          self.content.set('runSmokeTest', true);
          App.router.get('backgroundOperationsController').showPopup();
        }
      );
    });
  },

  /**
   * On click callback for <code>Reassign <master component></code> button
   * @param hostComponent
   */
  reassignMaster: function (hostComponent) {
    console.log('In Reassign Master', hostComponent);
    App.ModalPopup.show({
      header: 'Reassign Master Wizard',
      body: 'Reassign Master Wizard',
      secondary: false,
      onPrimary: function() {
        this.hide();
      }
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
  }
})