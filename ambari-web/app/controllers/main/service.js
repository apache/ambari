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

App.MainServiceController = Em.ArrayController.extend({

  name: 'mainServiceController',

  /**
   * @type {Ember.Object[]}
   */
  content: function () {
    if (!App.router.get('clusterController.isLoaded')) {
      return [];
    }
    return App.Service.find();
  }.property('App.router.clusterController.isLoaded').volatile(),

  /**
   * Current cluster
   * @type {Ember.Object}
   */
  cluster: function () {
    if (!App.router.get('clusterController.isClusterDataLoaded')) {
      return null;
    }
    return App.Cluster.find().objectAt(0);
  }.property('App.router.clusterController.isClusterDataLoaded'),

  /**
   * Check if all services are installed
   * true - all installed, false - not all
   * @type {bool}
   */
  isAllServicesInstalled: function () {
    if (!this.get('content.content')) return false;
    var availableServices = App.StackService.find().mapProperty('serviceName');
    return this.get('content.content').length == availableServices.length;
  }.property('content.content.@each', 'content.content.length'),

  /**
   * Should "Start All"-button be disabled
   * @type {bool}
   */
  isStartAllDisabled: function () {
    if (this.get('isStartStopAllClicked') == true) {
      return true;
    }
    var stoppedServices = this.get('content').filter(function (_service) {
      return (_service.get('healthStatus') === 'red' && !App.get('services.clientOnly').contains(_service.get('serviceName')));
    });
    return (stoppedServices.length === 0); // all green status
  }.property('isStartStopAllClicked', 'content.@each.healthStatus'),

  /**
   * Should "Stop All"-button be disabled
   * @type {bool}
   */
  isStopAllDisabled: function () {
    if (this.get('isStartStopAllClicked') == true) {
      return true;
    }
    var startedServiceLength = this.get('content').filterProperty('healthStatus', 'green').length;
    return (startedServiceLength === 0);
  }.property('isStartStopAllClicked', 'content.@each.healthStatus'),

  /**
   * @type {bool}
   */
  isStartStopAllClicked: function () {
    return (App.router.get('backgroundOperationsController').get('allOperationsCount') !== 0);
  }.property('App.router.backgroundOperationsController.allOperationsCount'),

  /**
   * Callback for <code>start all service</code> button
   * @return {App.ModalPopup|null}
   * @method startAllService
   */
  startAllService: function (event) {
    return this.startStopAllService(event, 'STARTED');
  },

  /**
   * Callback for <code>stop all service</code> button
   * @return {App.ModalPopup|null}
   * @method stopAllService
   */
  stopAllService: function (event) {
    return this.startStopAllService(event, 'INSTALLED');
  },

  /**
   * Common method for "start-all", "stop-all" calls
   * @param {object} event
   * @param {string} state 'STARTED|INSTALLED'
   * @returns {App.ModalPopup|null}
   * @method startStopAllService
   */
  startStopAllService: function(event, state) {
    if ($(event.target).hasClass('disabled') || $(event.target.parentElement).hasClass('disabled')) {
      return null;
    }
    var self = this;
    var bodyMessage = Em.Object.create({
      confirmMsg: state == 'INSTALLED' ? Em.I18n.t('services.service.stopAll.confirmMsg') : Em.I18n.t('services.service.startAll.confirmMsg'),
      confirmButton: state == 'INSTALLED' ? Em.I18n.t('services.service.stop.confirmButton') : Em.I18n.t('services.service.start.confirmButton')
    });

    if (state == 'INSTALLED' && App.Service.find().filterProperty('serviceName', 'HDFS').someProperty('workStatus', App.HostComponentStatus.started)) {
      App.router.get('mainServiceItemController').checkNnLastCheckpointTime(function () {
        return App.showConfirmationFeedBackPopup(function (query) {
          self.allServicesCall(state, query);
        }, bodyMessage);
      });
    } else {
      return App.showConfirmationFeedBackPopup(function (query) {
        self.allServicesCall(state, query);
      }, bodyMessage);
    }
  },

  /**
   * Do request to server for "start|stop" all services
   * @param {string} state "STARTED|INSTALLED"
   * @param {object} query
   * @method allServicesCall
   * @return {$.ajax}
   */
  allServicesCall: function (state, query) {
    var context = (state == 'INSTALLED') ? App.BackgroundOperationsController.CommandContexts.STOP_ALL_SERVICES :
      App.BackgroundOperationsController.CommandContexts.START_ALL_SERVICES;
    return App.ajax.send({
      name: 'common.services.update',
      sender: this,
      data: {
        context: context,
        ServiceInfo: {
          state: state
        },
        query: query
      },
      success: 'allServicesCallSuccessCallback',
      error: 'allServicesCallErrorCallback'
    });
  },

  /**
   * Restart all services - stops all services, then starts them back
   */
  restartAllServices: function () {
    this.silentStopAllServices();
  },

  /**
   * Silent stop all services - without user confirmation
   * @returns {$.ajax}
   */
  silentStopAllServices: function () {
    return App.ajax.send({
      name: 'common.services.update',
      sender: this,
      data: {
        context: App.BackgroundOperationsController.CommandContexts.STOP_ALL_SERVICES,
        ServiceInfo: {
          state: 'INSTALLED'
        }
      },
      success: 'silentStopSuccess'
    });
  },

  isStopAllServicesFailed: function() {
    var workStatuses = App.Service.find().mapProperty('workStatus');
    for (var i = 0; i < workStatuses.length; i++) {
      if (workStatuses[i] != 'INSTALLED' && workStatuses[i] != 'STOPPING') {
        return true;
      }
    }
    return false;
  },

  /**
   * Success callback for silent stop
   */
  silentStopSuccess: function () {
    var self = this;

    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }

      Ember.run.later(function () {
        self.set('shouldStart', true);
      }, App.bgOperationsUpdateInterval);
    });
  },

  /**
   * Silent start all services - without user confirmation
   */
  silentStartAllServices: function () {
    if (
      !App.router.get('backgroundOperationsController').get('allOperationsCount')
      && this.get('shouldStart')
      && !this.isStopAllServicesFailed()
    ) {
      this.set('shouldStart', false);
      return App.ajax.send({
        name: 'common.services.update',
        sender: this,
        data: {
          context: App.BackgroundOperationsController.CommandContexts.START_ALL_SERVICES,
          ServiceInfo: {
            state: 'STARTED'
          }
        },
        success: 'silentCallSuccessCallback'
      });
    }
  }.observes('shouldStart', 'controllers.backgroundOperationsController.allOperationsCount'),

  /**
   * Success callback for silent start
   */
  silentCallSuccessCallback: function () {
    // load data (if we need to show this background operations popup) from persist
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  },

  /**
   * Success-callback for all-services request
   * @param {object} data
   * @param {object} xhr
   * @param {object} params
   * @method allServicesCallSuccessCallback
   */
  allServicesCallSuccessCallback: function (data, xhr, params) {
    params.query.set('status', 'SUCCESS');

    // load data (if we need to show this background operations popup) from persist
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  },

  /**
   * Error-callback for all-services request
   * @param {object} request
   * @param {object} ajaxOptions
   * @param {string} error
   * @param {object} opt
   * @param {object} params
   * @method allServicesCallErrorCallback
   */
  allServicesCallErrorCallback: function (request, ajaxOptions, error, opt, params) {
    params.query.set('status', 'FAIL');
  },

  /**
   * "Add-service"-click handler
   * @method gotoAddService
   */
  gotoAddService: function () {
    if (this.get('isAllServicesInstalled')) {
      return;
    }
    App.router.get('addServiceController').setDBProperty('onClosePath', 'main.services.index');
    App.router.transitionTo('main.serviceAdd');
  }
});
