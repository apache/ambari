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
var batchUtils = require('utils/batch_scheduled_requests');

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
   * Common method for ajax (start/stop service) responses
   * @param data
   * @param ajaxOptions
   * @param params
   */
  startStopPopupSuccessCallback: function (data, ajaxOptions, params) {
    if (data && data.Requests) {
      params.query.set('status', 'SUCCESS');
      var config = this.get('callBackConfig')[(JSON.parse(ajaxOptions.data)).Body.ServiceInfo.state];
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
      // load data (if we need to show this background operations popup) from persist
      App.router.get('applicationController').dataLoading().done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    } else {
      params.query.set('status', 'FAIL');
      console.log('cannot get request id from ', data);
    }
  },
  startStopPopupErrorCallback: function(request, ajaxOptions, error, opt, params){
    params.query.set('status', 'FAIL');
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
    var serviceDisplayName = this.get('content.displayName');
    var isMaintenanceOFF = this.get('content.passiveState') === 'OFF';
    var bodyMessage = Em.Object.create({
      confirmMsg: serviceHealth == 'INSTALLED'? Em.I18n.t('services.service.stop.confirmMsg').format(serviceDisplayName) : Em.I18n.t('question.sure'),
      confirmButton: serviceHealth == 'INSTALLED'? Em.I18n.t('services.service.stop.confirmButton') : Em.I18n.t('ok'),
      additionalWarningMsg:  isMaintenanceOFF && serviceHealth == 'INSTALLED'? Em.I18n.t('services.service.stop.warningMsg.turnOnMM').format(serviceDisplayName) : null
    });

    return App.showConfirmationFeedBackPopup(function(query) {
      self.set('isPending', true);
      self.startStopPopupPrimary(serviceHealth, query);
    }, bodyMessage);
  },

  startStopPopupPrimary: function (serviceHealth, query) {
    var requestInfo = "";
    if (serviceHealth == "STARTED") {
      requestInfo = App.BackgroundOperationsController.CommandContexts.START_SERVICE.format(this.get('content.serviceName'));
    } else {
      requestInfo = App.BackgroundOperationsController.CommandContexts.STOP_SERVICE.format(this.get('content.serviceName'));
    }

    App.ajax.send({
      'name': 'service.item.start_stop',
      'sender': this,
      'success': 'startStopPopupSuccessCallback',
      'error': 'startStopPopupErrorCallback',
      'data': {
        'requestInfo': requestInfo,
        'serviceName': this.get('content.serviceName').toUpperCase(),
        'state': serviceHealth,
        'query': query
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
    return App.showConfirmationPopup(function() {
      self.set("content.runRebalancer", true);
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
    return App.showConfirmationPopup(function() {
      self.set("content.runCompaction", true);
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
      return App.showAlertPopup(Em.I18n.t('common.error'), Em.I18n.t('services.mapreduce2.smokeTest.requirement'));
    }
    return App.showConfirmationFeedBackPopup(function(query) {
      self.runSmokeTestPrimary(query);
    });
  },

  restartAllHostComponents : function(serviceName) {
    var serviceDisplayName = this.get('content.displayName');
    var bodyMessage = Em.Object.create({
      confirmMsg: Em.I18n.t('services.service.restartAll.confirmMsg').format(serviceDisplayName),
      confirmButton: Em.I18n.t('services.service.restartAll.confirmButton'),
      additionalWarningMsg: this.get('content.passiveState') === 'OFF' ? Em.I18n.t('services.service.restartAll.warningMsg.turnOnMM').format(serviceDisplayName): null
     });
    var staleConfigsOnly = App.Service.find(serviceName).get('serviceTypes').contains('MONITORING');
    return App.showConfirmationFeedBackPopup(function(query) {
      batchUtils.restartAllServiceHostComponents(serviceName, staleConfigsOnly, query);
    }, bodyMessage);
  },

  turnOnOffPassive: function(label) {
    var self = this;
    var state = this.get('content.passiveState') == 'OFF' ? 'ON' : 'OFF';
    var onOff = state === 'ON' ? "On" : "Off";
    return App.showConfirmationPopup(function() {
          self.turnOnOffPassiveRequest(state, label)
        },
        Em.I18n.t('hosts.passiveMode.popup').format(onOff,self.get('content.displayName'))
    );
  },

  rollingRestart: function(hostComponentName) {
    batchUtils.launchHostComponentRollingRestart(hostComponentName, this.get('content.displayName'), this.get('content.passiveState') === "ON", false, this.get('content.passiveState') === "ON");
  },

  turnOnOffPassiveRequest: function(state,message) {
    App.ajax.send({
      'name': 'service.item.passive',
      'sender': this,
      'data': {
        'requestInfo': message,
        'serviceName': this.get('content.serviceName').toUpperCase(),
        'passive_state': state
      },
      'success':'updateService'
    });
  },

  updateService: function(data, opt, params) {
    this.set('content.passiveState', params.passive_state);
    batchUtils.infoPassiveState(params.passive_state);
  },

  runSmokeTestPrimary: function(query) {
    App.ajax.send({
      'name': 'service.item.smoke',
      'sender': this,
      'success':'runSmokeTestSuccessCallBack',
      'error':'runSmokeTestErrorCallBack',
      'data': {
        'serviceName': this.get('content.serviceName'),
        'displayName': this.get('content.displayName'),
        'actionName': this.get('content.serviceName') === 'ZOOKEEPER' ? 'ZOOKEEPER_QUORUM_SERVICE_CHECK' : this.get('content.serviceName') + '_SERVICE_CHECK',
        'query': query
      }
    });
  },

  runSmokeTestSuccessCallBack: function (data, ajaxOptions, params) {
    if (data.Requests.id) {
      // load data (if we need to show this background operations popup) from persist
      App.router.get('applicationController').dataLoading().done(function (initValue) {
        params.query.set('status', 'SUCCESS');
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    }
    else {
      params.query.set('status', 'FAIL');
      console.warn('error during runSmokeTestSuccessCallBack');
    }
  },
  runSmokeTestErrorCallBack: function (request, ajaxOptions, error, opt, params) {
    params.query.set('status', 'FAIL');
  },

  /**
   * On click callback for <code>Reassign <master component></code> button
   * @param hostComponent
   */
  reassignMaster: function (hostComponent) {
    var component = App.HostComponent.find().findProperty('componentName', hostComponent);
    console.log('In Reassign Master', hostComponent);
    if (component) {
      var reassignMasterController = App.router.get('reassignMasterController');
      reassignMasterController.saveComponentToReassign(component);
      reassignMasterController.getSecurityStatus();
      reassignMasterController.setCurrentStep('1');
      App.router.transitionTo('reassign');
    }
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

  /**
   * Restart clients host components to apply config changes
   */
  refreshConfigs: function () {
    var self = this;

    if (this.get('content.isClientsOnly')) {
      return App.showConfirmationFeedBackPopup(function (query) {
        batchUtils.getComponentsFromServer({
          services: [self.get('content.serviceName')]
        }, function (data) {
          var hostComponents = [];

          data.items.forEach(function (host) {
            host.host_components.forEach(function (hostComponent) {
              hostComponents.push(Em.Object.create({
                componentName: hostComponent.HostRoles.component_name,
                hostName: host.Hosts.host_name
              }))
            });
          });
          batchUtils.restartHostComponents(hostComponents, Em.I18n.t('rollingrestart.context.allForSelectedService').format(self.get('content.serviceName')), "SERVICE", query);
        })
      });
    }
  },

  /**
   * set property isPending (if this property is true - means that service has task in BGO)
   * and this makes start/stop button disabled
   */
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

  isStartDisabled: function () {
    if(this.get('isPending')) return true;
    return !(this.get('content.healthStatus') == 'red');
  }.property('content.healthStatus','isPending'),

  isStopDisabled: function () {
    if(this.get('isPending')) return true;
    if (App.get('isHaEnabled') && this.get('content.serviceName') == 'HDFS' && this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
      return false;
    }
    return (this.get('content.healthStatus') != 'green');
  }.property('content.healthStatus','isPending'),

  isPending:true

});
