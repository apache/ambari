/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.MainServiceInfoSummaryController = Em.Controller.extend({
  name: 'mainServiceInfoSummaryController',

  selectedFlumeAgent: null,

  /**
   * Send start command for selected Flume Agent
   * @method startFlumeAgent
   */
  startFlumeAgent: function () {
    var selectedFlumeAgent = arguments[0].context;
    if (selectedFlumeAgent && selectedFlumeAgent.get('status') === 'NOT_RUNNING') {
      var self = this;
      App.showConfirmationPopup(function () {
        var state = 'STARTED';
        var context = Em.I18n.t('services.service.summary.flume.start.context').format(selectedFlumeAgent.get('name'));
        self.sendFlumeAgentCommandToServer(state, context, selectedFlumeAgent);
      });
    }
  },

  /**
   * Send stop command for selected Flume Agent
   * @method stopFlumeAgent
   */
  stopFlumeAgent: function () {
    var selectedFlumeAgent = arguments[0].context;
    if (selectedFlumeAgent && selectedFlumeAgent.get('status') === 'RUNNING') {
      var self = this;
      App.showConfirmationPopup(function () {
        var state = 'INSTALLED';
        var context = Em.I18n.t('services.service.summary.flume.stop.context').format(selectedFlumeAgent.get('name'));
        self.sendFlumeAgentCommandToServer(state, context, selectedFlumeAgent);
      });
    }
  },

  /**
   * Send command for Flume Agent to server
   * @param {string} state
   * @param {string} context
   * @param {Object} agent
   * @method sendFlumeAgentCommandToServer
   */
  sendFlumeAgentCommandToServer: function (state, context, agent) {
    App.ajax.send({
      name: 'service.flume.agent.command',
      sender: this,
      data: {
        state: state,
        context: context,
        agentName: agent.get('name'),
        host: agent.get('hostName')
      },
      success: 'commandSuccessCallback'
    });
  },

  /**
   * Callback, that shows Background operations popup if request was successful
   */
  commandSuccessCallback: function () {
    console.log('Send request for refresh configs successfully');
    // load data (if we need to show this background operations popup) from persist
    App.router.get('applicationController').dataLoading().done(function (showPopup) {
      if (showPopup) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  },

  gotoConfigs: function () {
    App.router.get('mainServiceItemController').set('routeToConfigs', true);
    App.router.transitionTo('main.services.service.configs', this.get('content'));
    App.router.get('mainServiceItemController').set('routeToConfigs', false);
  },

  nagiosUrl: function () {
    return App.router.get('clusterController.nagiosUrl');
  }.property('App.router.clusterController.nagiosUrl'),

  isNagiosInstalled: function () {
    return App.router.get('clusterController.isNagiosInstalled');
  }.property('App.router.clusterController.isNagiosInstalled'),

  showServiceAlertsPopup: function (event) {
    var service = event.context;
    return App.ModalPopup.show({
      header: Em.I18n.t('services.service.summary.alerts.popup.header').format(service.get('displayName')),
      autoHeight: false,
      bodyClass: Em.View.extend({
        templateName: require('templates/main/service/info/service_alert_popup'),
        classNames: ['service-alerts'],
        controllerBinding: 'App.router.mainAlertDefinitionsController',
        didInsertElement: function () {
          Em.run.next(this, function () {
            App.tooltip($(".timeago"));
          });
        },
        alerts: function () {
          var serviceDefinitions = this.get('controller.content').filterProperty('service', service);
          // definitions should be sorted in order: critical, warning, all other
          var criticalDefinitions = [];
          var warningDefinitions = [];
          serviceDefinitions.forEach(function (definition) {
            if (definition.get('isCritical')) {
              criticalDefinitions.push(definition);
              serviceDefinitions = serviceDefinitions.without(definition);
            } else if (definition.get('isWarning')) {
              warningDefinitions.push(definition);
              serviceDefinitions = serviceDefinitions.without(definition);
            }
          });
          serviceDefinitions = criticalDefinitions.concat(warningDefinitions, serviceDefinitions);
          return serviceDefinitions;
        }.property('controller.content'),
        gotoAlertDetails: function (event) {
          if (event && event.context) {
            this.get('parentView').hide();
            App.router.transitionTo('main.alerts.alertDetails', event.context);
          }
        },
        closePopup: function () {
          this.get('parentView').hide();
        }
      }),
      isHideBodyScroll: false,
      primary: Em.I18n.t('common.close'),
      secondary: null
    });
  }

});