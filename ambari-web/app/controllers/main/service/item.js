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
var componentsUtils = require('utils/components');

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

  initHosts: function() {
    if (App.get('components.masters').length !== 0) {
      var self = this;

      var hostNames = App.Host.find().mapProperty('hostName');
      this.set('allHosts', hostNames);

      ['HBASE_MASTER', 'ZOOKEEPER_SERVER', 'FLUME_HANDLER'].forEach(function(componentName) {
        self.loadHostsWithoutComponent(componentName);
      });
    }
  }.observes('App.components.masters', 'content.hostComponents.length'),

  loadHostsWithoutComponent: function (componentName) {
    var self = this;
    var hostsWithComponent = App.HostComponent.find().filterProperty('componentName', componentName).mapProperty('hostName');

    var hostsWithoutComponent = this.get('allHosts').filter(function(hostName) {
      return !hostsWithComponent.contains(hostName);
    });

    self.set('add' + componentName, function() {
      self.addComponent(componentName);
    });

    Em.defineProperty(self, 'addDisabledTooltip-' + componentName, Em.computed('isAddDisabled-' + componentName, 'addDisabledMsg-' + componentName, function() {
      if (self.get('isAddDisabled-' + componentName)) {
        return self.get('addDisabledMsg-' + componentName);
      }
    }));

    Em.defineProperty(self, 'isAddDisabled-' + componentName, Em.computed('hostsWithoutComponent-' + componentName, function() {
      return self.get('hostsWithoutComponent-' + componentName).length === 0 ? 'disabled' : '';
    }));

    var disabledMsg = Em.I18n.t('services.summary.allHostsAlreadyRunComponent').format(componentName);
    self.set('hostsWithoutComponent-' + componentName, hostsWithoutComponent);
    self.set('addDisabledMsg-' + componentName, disabledMsg);
  },

  /**
   * flag to control router switch between service summary and configs
   * @type {boolean}
   */
  routeToConfigs: false,

  isClientsOnlyService: function() {
    return App.get('services.clientOnly').contains(this.get('content.serviceName'));
  }.property('content.serviceName'),

  isConfigurable: function () {
    return !App.get('services.noConfigTypes').contains(this.get('content.serviceName'));
  }.property('App.services.noConfigTypes','content.serviceName'),

  allHosts: [],

  clientComponents: function () {
    var clientNames = [];
    var clients = App.StackServiceComponent.find().filterProperty('serviceName', this.get('content.serviceName')).filterProperty('isClient');
    clients.forEach(function (item) {
      clientNames.push({
        action: 'downloadClientConfigs',
        context: {
          name: item.get('componentName'),
          label: item.get('displayName')
        }
      });
    });
    return clientNames;
  }.property('content.serviceName'),

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
      if (App.get('testMode')) {
        self.set('content.workStatus', App.Service.Health[config.f]);
        self.get('content.hostComponents').setEach('workStatus', App.HostComponentStatus[config.f]);
        setTimeout(function () {
          self.set('content.workStatus', App.Service.Health[config.c2]);
          self.get('content.hostComponents').setEach('workStatus', App.HostComponentStatus[config.hs]);
        }, App.get('testModeDelayForActions'));
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
      putInMaintenance: (serviceHealth == 'INSTALLED' && isMaintenanceOFF) || (serviceHealth == 'STARTED' && !isMaintenanceOFF),
      turnOnMmMsg: serviceHealth == 'INSTALLED' ? Em.I18n.t('passiveState.turnOnFor').format(serviceDisplayName) : Em.I18n.t('passiveState.turnOffFor').format(serviceDisplayName),
      confirmMsg: serviceHealth == 'INSTALLED'? Em.I18n.t('services.service.stop.confirmMsg').format(serviceDisplayName) : Em.I18n.t('question.sure'),
      confirmButton: serviceHealth == 'INSTALLED'? Em.I18n.t('services.service.stop.confirmButton') : Em.I18n.t('ok'),
      additionalWarningMsg:  isMaintenanceOFF && serviceHealth == 'INSTALLED'? Em.I18n.t('services.service.stop.warningMsg.turnOnMM').format(serviceDisplayName) : null
    });

    return App.showConfirmationFeedBackPopup(function(query, runMmOperation) {
      self.set('isPending', true);
      self.startStopWithMmode(serviceHealth, query, runMmOperation);
    }, bodyMessage);
  },


  startStopWithMmode: function(serviceHealth, query, runMmOperation) {
    var self = this;
    if (runMmOperation) {
      if (serviceHealth == "STARTED") {
        this.startStopPopupPrimary(serviceHealth, query).complete(function() {
          batchUtils.turnOnOffPassiveRequest("OFF", Em.I18n.t('passiveState.turnOff'), self.get('content.serviceName').toUpperCase());
        });
      } else {
        batchUtils.turnOnOffPassiveRequest("ON", Em.I18n.t('passiveState.turnOn'), this.get('content.serviceName').toUpperCase()).complete(function() {
          self.startStopPopupPrimary(serviceHealth, query);
        })
      }
    } else {
      this.startStopPopupPrimary(serviceHealth, query);
    }

  },

  startStopPopupPrimary: function (serviceHealth, query) {
    var requestInfo = (serviceHealth == "STARTED")
        ? App.BackgroundOperationsController.CommandContexts.START_SERVICE.format(this.get('content.serviceName'))
        : App.BackgroundOperationsController.CommandContexts.STOP_SERVICE.format(this.get('content.serviceName'));

    var data = {
      'context': requestInfo,
      'serviceName': this.get('content.serviceName').toUpperCase(),
      'ServiceInfo': {
        'state': serviceHealth
      },
      'query': query
    };

    return App.ajax.send({
      'name': 'common.service.update',
      'sender': this,
      'success': 'startStopPopupSuccessCallback',
      'error': 'startStopPopupErrorCallback',
      'data': data
    });
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
   * On click handler for Yarn Refresh Queues command from items menu
   * @param event
   */
  refreshYarnQueues : function (event) {
    var controller = this;
    var hosts = App.Service.find('YARN').get('hostComponents').filterProperty('componentName', 'RESOURCEMANAGER').mapProperty('hostName');
    return App.showConfirmationPopup(function() {
    App.ajax.send({
      name : 'service.item.refreshQueueYarnRequest',
        sender: controller,
      data : {
        command : "REFRESHQUEUES",
        context : Em.I18n.t('services.service.actions.run.yarnRefreshQueues.context') ,
        hosts : hosts.join(','),
        serviceName : "YARN",
        componentName : "RESOURCEMANAGER",
        forceRefreshConfigTags : "capacity-scheduler"
      },
      success : 'refreshYarnQueuesSuccessCallback',
      error : 'refreshYarnQueuesErrorCallback'
    });
    });
  },
  refreshYarnQueuesSuccessCallback  : function(data, ajaxOptions, params) {
    if (data.Requests.id) {
      App.router.get('backgroundOperationsController').showPopup();
    } else {
      console.warn('Error during refreshYarnQueues');
    }
  },
  refreshYarnQueuesErrorCallback : function(data) {
    var error = Em.I18n.t('services.service.actions.run.yarnRefreshQueues.error');
    if(data && data.responseText){
      try {
        var json = $.parseJSON(data.responseText);
        error += json.message;
      } catch (err) {}
    }
    App.showAlertPopup(Em.I18n.t('services.service.actions.run.yarnRefreshQueues.error'), error);
    console.warn('Error during refreshYarnQueues:'+error);
  },

  startLdapKnox: function(event) {
    var context =  Em.I18n.t('services.service.actions.run.startLdapKnox.context');
    this.startStopLdapKnox('STARTDEMOLDAP',context);
  },

  stopLdapKnox: function(event) {
    var context = Em.I18n.t('services.service.actions.run.stopLdapKnox.context');
    this.startStopLdapKnox('STOPDEMOLDAP',context);
  },

  startStopLdapKnox: function(command,context) {
    var controller = this;
    var host = App.HostComponent.find().findProperty('componentName', 'KNOX_GATEWAY').get('hostName');
    return App.showConfirmationPopup(function() {
      App.ajax.send({
        name: 'service.item.startStopLdapKnox',
        sender: controller,
        data: {
          command: command,
          context: context,
          host: host,
          serviceName: "KNOX",
          componentName: "KNOX_GATEWAY"
        },
        success: 'startStopLdapKnoxSuccessCallback',
        error: 'startStopLdapKnoxErrorCallback'
      });
    });
  },

  startStopLdapKnoxSuccessCallback  : function(data, ajaxOptions, params) {
    if (data.Requests.id) {
      App.router.get('backgroundOperationsController').showPopup();
    } else {
      console.warn('Error during startStopLdapKnox');
    }
  },
  startStopLdapKnoxErrorCallback : function(data) {
    var error = Em.I18n.t('services.service.actions.run.startStopLdapKnox.error');
    if(data && data.responseText){
      try {
        var json = $.parseJSON(data.responseText);
        error += json.message;
      } catch (err) {}
    }
    App.showAlertPopup(Em.I18n.t('services.service.actions.run.yarnRefreshQueues.error'), error);
    console.warn('Error during refreshYarnQueues:'+ error);
  },

  /**
   * On click handler for rebalance Hdfs command from items menu
   */
  rebalanceHdfsNodes: function () {
    var controller = this;
    App.ModalPopup.show({
      classNames: ['fourty-percent-width-modal'],
      header: Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes.context'),
      primary: Em.I18n.t('common.start'),
      secondary: Em.I18n.t('common.cancel'),
      inputValue: 10,
      errorMessage: Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes.promptError'),
      isInvalid: function () {
        var intValue = Number(this.get('inputValue'));
        return this.get('inputValue')!=='DEBUG' && (isNaN(intValue) || intValue < 1 || intValue > 100);
      }.property('inputValue'),
      disablePrimary : function() {
        return this.get('isInvalid');
      }.property('isInvalid'),
      onPrimary: function () {
        if (this.get('isInvalid')) {
          return;
        }
        App.ajax.send({
          name : 'service.item.rebalanceHdfsNodes',
          sender: controller,
          data : {
            hosts : App.Service.find('HDFS').get('hostComponents').findProperty('componentName', 'NAMENODE').get('hostName'),
            threshold: this.get('inputValue')
          },
          success : 'rebalanceHdfsNodesSuccessCallback',
          error : 'rebalanceHdfsNodesErrorCallback'
        });
        this.hide();
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/common/prompt_popup'),
        text: Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes.prompt'),
        didInsertElement: function () {
          App.tooltip(this.$(".prompt-input"), {
            placement: "bottom",
            title: Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes.promptTooltip')
          });
        }
      })
    });
  },
  rebalanceHdfsNodesSuccessCallback: function (data) {
    if (data.Requests.id) {
      App.router.get('backgroundOperationsController').showPopup();
    } else {
      console.warn('Error during runRebalanceHdfsNodes');
    }
  },
  rebalanceHdfsNodesErrorCallback : function(data) {
    var error = Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes.error');
    if(data && data.responseText){
      try {
        var json = $.parseJSON(data.responseText);
        error += json.message;
      } catch (err) {
      }
    }
    App.showAlertPopup(Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes.error'), error);
    console.warn('Error during runRebalanceHdfsNodes:'+error);
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
      putInMaintenance: this.get('content.passiveState') === 'OFF',
      turnOnMmMsg: Em.I18n.t('passiveState.turnOnFor').format(serviceDisplayName),
      confirmMsg: Em.I18n.t('services.service.restartAll.confirmMsg').format(serviceDisplayName),
      confirmButton: Em.I18n.t('services.service.restartAll.confirmButton'),
      additionalWarningMsg: this.get('content.passiveState') === 'OFF' ? Em.I18n.t('services.service.restartAll.warningMsg.turnOnMM').format(serviceDisplayName): null
     });
    return App.showConfirmationFeedBackPopup(function(query, runMmOperation) {
      batchUtils.restartAllServiceHostComponents(serviceName, false, query, runMmOperation);
    }, bodyMessage);
  },

  turnOnOffPassive: function(label) {
    var self = this;
    var state = this.get('content.passiveState') == 'OFF' ? 'ON' : 'OFF';
    var onOff = state === 'ON' ? "On" : "Off";
    return App.showConfirmationPopup(function() {
          batchUtils.turnOnOffPassiveRequest(state, label, self.get('content.serviceName').toUpperCase(), function(data, opt, params) {
            self.set('content.passiveState', params.passive_state);
            batchUtils.infoPassiveState(params.passive_state);})
        },
        Em.I18n.t('hosts.passiveMode.popup').format(onOff,self.get('content.displayName'))
    );
  },

  rollingRestart: function(hostComponentName) {
    batchUtils.launchHostComponentRollingRestart(hostComponentName, this.get('content.displayName'), this.get('content.passiveState') === "ON", false, this.get('content.passiveState') === "ON");
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
    if (this.get('isClientsOnlyService') || this.get('content.serviceName') == "FLUME") {
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
   * Send command to server to install client on selected host
   * @param componentName
   */
  addComponent: function (componentName) {
    var self = this;
    var component = App.HostComponent.find().findProperty('componentName', componentName);
    var componentDisplayName = component.get('displayName');

    self.loadHostsWithoutComponent(componentName);

    return App.ModalPopup.show({
      primary: function() {
        if (this.get('anyHostsWithoutComponent')) {
          return Em.I18n.t('hosts.host.addComponent.popup.confirm')
        } else {
          return undefined;
        }
      }.property('anyHostsWithoutComponent'),

      header: Em.I18n.t('popup.confirmation.commonHeader'),

      addComponentMsg: function () {
        return Em.I18n.t('hosts.host.addComponent.msg').format(componentDisplayName);
      }.property(),

      selectHostMsg: function () {
        return Em.I18n.t('services.summary.selectHostForComponent').format(this.get('componentDisplayName'))
      }.property('componentDisplayName'),

      thereIsNoHostsMsg: function () {
        return Em.I18n.t('services.summary.allHostsAlreadyRunComponent').format(this.get('componentDisplayName'))
      }.property('componentDisplayName'),

      hostsWithoutComponent: function() {
        return self.get("hostsWithoutComponent-" + this.get('componentName'));
      }.property('componentName', 'self.hostsWithoutComponent-' + this.get('componentName')),

      anyHostsWithoutComponent: function() {
        return this.get('hostsWithoutComponent').length > 0
      }.property('hostsWithoutComponent'),

      selectedHost: null,

      componentName: function() {
        return componentName;
      }.property(),

      componentDisplayName: function() {
        return componentDisplayName;
      }.property(),

      bodyClass: Em.View.extend({
        templateName: require('templates/main/service/add_host_popup')
      }),

      restartNagiosMsg: Em.View.extend({
        template: Em.Handlebars.compile(Em.I18n.t('hosts.host.addComponent.note').format(componentDisplayName))
      }),

      onPrimary: function () {
        var selectedHost = this.get('selectedHost');

        // Install
        componentsUtils.installHostComponent(selectedHost, component);

        // Remove host from 'without' collection to immediate recalculate add menu item state
        var hostsWithoutComponent = this.get('hostsWithoutComponent');
        var index = hostsWithoutComponent.indexOf(this.get('selectedHost'));
        if (index > -1) {
          hostsWithoutComponent.splice(index, 1);
        }

        self.set('hostsWithoutComponent-' + this.get('componentName'), hostsWithoutComponent);
        this.hide();
      }
    });
  },

  /**
   * set property isPending (if this property is true - means that service has task in BGO)
   * and this makes start/stop button disabled
   */
  setStartStopState: function () {
    var serviceName = this.get('content.serviceName');
    var backgroundOperations = App.router.get('backgroundOperationsController.services');
    if (backgroundOperations && backgroundOperations.length > 0) {
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

  /**
   * Determine if service has than one service client components
   */
  isSeveralClients: function () {
    return App.StackServiceComponent.find().filterProperty('serviceName', this.get('content.serviceName')).filterProperty('isClient').length > 1;
  }.property('content.serviceName'),

  enableHighAvailability: function() {
    var ability_controller = App.router.get('mainAdminHighAvailabilityController');
    ability_controller.enableHighAvailability();
  },

  disableHighAvailability: function() {
    var ability_controller = App.router.get('mainAdminHighAvailabilityController');
    ability_controller.disableHighAvailability();
  },

  enableRMHighAvailability: function() {
    var ability_controller = App.router.get('mainAdminHighAvailabilityController');
    ability_controller.enableRMHighAvailability();
  },

  downloadClientConfigs: function (event) {
    var component = this.get('content.hostComponents').findProperty('isClient');
    componentsUtils.downloadClientConfigs.call(this, {
      serviceName: this.get('content.serviceName'),
      componentName: (event && event.name) || component.get('componentName'),
      displayName: (event && event.label) || component.get('displayName')
    });
  },

  /**
   * On click handler for custom command from items menu
   * @param context
   */
  executeCustomCommand: function(context) {
    var controller = this;
    return App.showConfirmationPopup(function() {
      App.ajax.send({
        name : 'service.item.executeCustomCommand',
        sender: controller,
        data : {
          command : context.command,
          context : 'Execute ' + context.command,
          hosts : App.Service.find(context.service).get('hostComponents').findProperty('componentName', context.component).get('hostName'),
          serviceName : context.service,
          componentName : context.component,
          forceRefreshConfigTags : "capacity-scheduler"
        },
        success : 'executeCustomCommandSuccessCallback',
        error : 'executeCustomCommandErrorCallback'
      });
    });
  },

  executeCustomCommandSuccessCallback  : function(data, ajaxOptions, params) {
    if (data.Requests.id) {
      App.router.get('backgroundOperationsController').showPopup();
    } else {
      console.warn('Error during execution of ' + params.command + ' custom command on' + params.componentName);
    }
  },

  executeCustomCommandErrorCallback : function(data) {
    var error = Em.I18n.t('services.service.actions.run.executeCustomCommand.error');
    if(data && data.responseText){
      try {
        var json = $.parseJSON(data.responseText);
        error += json.message;
      } catch (err) {}
    }
    App.showAlertPopup(Em.I18n.t('services.service.actions.run.executeCustomCommand.error'), error);
    console.warn('Error during executing custom command');
  },

  isPending:true

});
