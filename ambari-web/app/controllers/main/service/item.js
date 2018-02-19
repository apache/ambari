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
var blueprintUtils = require('utils/blueprint');
var stringUtils = require('utils/string_utils');

App.MainServiceItemController = Em.Controller.extend(App.SupportClientConfigsDownload, App.InstallComponent, App.ConfigsSaverMixin, App.EnhancedConfigsMixin, App.GroupsMappingMixin, {
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
   * Map of service names and lists of sites they need to load
   */
  serviceConfigsMap: {
    'OOZIE': ['oozie-env']
  },

  /**
   * Configs loaded to use for service actions menu
   *
   * format: {config-type: {property-name1: property-value1, property-name2: property-value2, ...}}
   */
  configs: {},

  /**
   * @type {boolean}
   * @default true
   */
  isPending: true,

  /**
   * @type {boolean}
   * @default false
   */
  isServicesInfoLoaded: false,

  /**
   * Define whether configs for service actions menu were loaded
   * @type {Boolean}
   */
  isServiceConfigsLoaded: false,

  /**
   * flag to control router switch between service summary and configs
   * @type {boolean}
   */
  routeToConfigs: false,

  deleteServiceProgressPopup: null,

  isRecommendationInProgress: false,

  isClientsOnlyService: function() {
    return App.get('services.clientOnly').contains(this.get('content.serviceName'));
  }.property('content.serviceName'),

  isConfigurable: function () {
    return !App.get('services.noConfigTypes').contains(this.get('content.serviceName'));
  }.property('App.services.noConfigTypes','content.serviceName'),

  clientComponents: function () {
    var clientNames = [{
      action:  'downloadAllClientConfigs',
      context: {
        label: Em.I18n.t('common.all.clients')
      }
    }];
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
   * Returns interdependent services
   *
   * @returns {string[]}
   */
  interDependentServices: function() {
    var serviceName = this.get('content.serviceName'), interDependentServices = [];
    App.StackService.find(serviceName).get('requiredServices').forEach(function(requiredService) {
      if (App.StackService.find(requiredService).get('requiredServices').contains(serviceName)) {
        interDependentServices.push(requiredService);
      }
    });
    return interDependentServices;
  }.property('content.serviceName'),

  /**
   * collection of serviceConfigs
   *
   * @type {Object[]}
   */
  stepConfigs: [],

  /**
   * List of service names that have configs dependent on current service configs
   *
   * @type {String[]}
   */
  dependentServiceNames: function() {
    return App.get('router.clusterController.isConfigsPropertiesLoaded') ?
      App.StackService.find(this.get('content.serviceName')).get('dependentServiceNames') : [];
  }.property('content.serviceName', 'App.router.clusterController.isConfigsPropertiesLoaded'),

  configDependentServiceNames: function() {
    return this.get('dependentServiceNames').concat(App.StackService.find(this.get('content.serviceName')).get('requiredServices'))
  }.property('dependentServiceNames'),

  /**
   * List of service names that could be deleted
   * Common case when there is only current service should be removed
   * But for some services there is <code>interDependentServices<code> services
   * Like 'YARN' depends on 'MAPREDUCE2' and 'MAPREDUCE2' depends on 'YARN'
   * So these services can be removed only together
   *
   * @type {String[]}
   */
  serviceNamesToDelete: function() {
    return [this.get('content.serviceName')].concat(this.get('interDependentServices'));
  }.property('content.serviceName'),

  /**
   * List of config types that should be loaded
   * Includes
   * 1. Dependent services config-types
   * 2. Some special cases from <code>serviceConfigsMap<code>
   * 3. 'cluster-env'
   *
   * @type {String[]}
   */
  sitesToLoad: function() {
    var services = this.get('configDependentServiceNames'), configTypeList = [];
    if (services.length) {
      configTypeList = App.StackService.find().filter(function(s) {
        return services.contains(s.get('serviceName'));
      }).mapProperty('configTypeList').reduce(function(p, v) {
        return p.concat(v);
      });
    }
    if (this.get('serviceConfigsMap')[this.get('content.serviceName')]) {
      configTypeList = configTypeList.concat(this.get('serviceConfigsMap')[this.get('content.serviceName')]);
    }
    configTypeList.push('cluster-env');
    return configTypeList.uniq();
  }.property('content.serviceName'),

  /**
   * Load all config tags for loading configs
   */
  loadConfigs: function(){
    this.set('isServiceConfigsLoaded', false);
    this.set('stepConfigs', []);
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'onLoadConfigsTags',
      error: 'onTaskError'
    });
  },

  /**
   * Load all configs for sites from <code>serviceConfigsMap</code> for current service
   * @param data
   */
  onLoadConfigsTags: function (data) {
    var self = this;
    App.get('router.mainController.isLoading').call(App.get('router.clusterController'), 'isConfigsPropertiesLoaded').done(function () {
      var sitesToLoad = self.get('sitesToLoad'),
        allConfigs = [],
        loadedSites = data.Clusters.desired_configs,
        siteTagsToLoad = [];
      for (var site in loadedSites) {
        if (sitesToLoad.contains(site)) {
          siteTagsToLoad.push({
            siteName: site,
            tagName: loadedSites[site].tag
          });
        }
      }
      App.router.get('configurationController').getConfigsByTags(siteTagsToLoad).done(function (configs) {
        configs.forEach(function (site) {
          self.get('configs')[site.type] = site.properties;
          allConfigs = allConfigs.concat(App.config.getConfigsFromJSON(site, true));
        });

        self.get('configDependentServiceNames').forEach(function(serviceName) {
          var configTypes = App.StackService.find(serviceName).get('configTypeList');
          var configsByService = allConfigs.filter(function (c) {
            return configTypes.contains(App.config.getConfigTagFromFileName(c.get('filename')));
          });
          if (App.config.get('preDefinedServiceConfigs').someProperty('serviceName', serviceName)) {
            self.get('stepConfigs').pushObject(App.config.createServiceConfig(serviceName, [], configsByService));
          }
        });

        self.set('isServiceConfigsLoaded', true);
      });
    });
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
      if (App.get('testMode')) {
        self.set('content.workStatus', App.Service.Health[config.f]);
        self.get('content.hostComponents').setEach('workStatus', App.HostComponentStatus[config.f]);
        setTimeout(function () {
          self.set('content.workStatus', App.Service.Health[config.c2]);
          self.get('content.hostComponents').setEach('workStatus', App.HostComponentStatus[config.hs]);
        }, App.get('testModeDelayForActions'));
      }
      // load data (if we need to show this background operations popup) from persist
      App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    } else {
      params.query.set('status', 'FAIL');
    }
  },
  startStopPopupErrorCallback: function(request, ajaxOptions, error, opt, params){
    App.ajax.defaultErrorHandler(request, opt.url, opt.type, request.status);
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

    var msg = isMaintenanceOFF && serviceHealth == 'INSTALLED'? Em.I18n.t('services.service.stop.warningMsg.turnOnMM').format(serviceDisplayName) : null;
    msg = self.addAdditionalWarningMessage(serviceHealth, msg, serviceDisplayName);

    var bodyMessage = Em.Object.create({
      putInMaintenance: (serviceHealth == 'INSTALLED' && isMaintenanceOFF) || (serviceHealth == 'STARTED' && !isMaintenanceOFF),
      turnOnMmMsg: serviceHealth == 'INSTALLED' ? Em.I18n.t('passiveState.turnOnFor').format(serviceDisplayName) : Em.I18n.t('passiveState.turnOffFor').format(serviceDisplayName),
      confirmMsg: serviceHealth == 'INSTALLED'? Em.I18n.t('services.service.stop.confirmMsg').format(serviceDisplayName) : Em.I18n.t('services.service.start.confirmMsg').format(serviceDisplayName),
      confirmButton: serviceHealth == 'INSTALLED'? Em.I18n.t('services.service.stop.confirmButton') : Em.I18n.t('services.service.start.confirmButton'),
      additionalWarningMsg:  msg
    });

    // check HDFS NameNode checkpoint before stop service
    if (this.get('content.serviceName') == 'HDFS' && serviceHealth == 'INSTALLED' &&
      this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
      this.checkNnLastCheckpointTime(function () {
        return App.showConfirmationFeedBackPopup(function(query, runMmOperation) {
          self.set('isPending', true);
          self.startStopWithMmode(serviceHealth, query, runMmOperation);
        }, bodyMessage);
      });
    } else {
      return App.showConfirmationFeedBackPopup(function(query, runMmOperation) {
        self.set('isPending', true);
        self.startStopWithMmode(serviceHealth, query, runMmOperation);
      }, bodyMessage);
    }
  },


  /**
   * this function will be called from :1) stop HDFS 2) restart all for HDFS 3) restart all affected for HDFS
   * @param callback - callback function to continue next operation
   */
  checkNnLastCheckpointTime: function(callback) {
    var self = this;
    this.pullNnCheckPointTime().complete(function () {
      var isNNCheckpointTooOld = self.get('isNNCheckpointTooOld');
      self.set('isNNCheckpointTooOld', null);
      if (isNNCheckpointTooOld) {
        // too old
        self.getHdfsUser().done(function() {
          var msg = Em.Object.create({
            confirmMsg: Em.I18n.t('services.service.stop.HDFS.warningMsg.checkPointTooOld').format(App.nnCheckpointAgeAlertThreshold) +
              Em.I18n.t('services.service.stop.HDFS.warningMsg.checkPointTooOld.instructions').format(isNNCheckpointTooOld, self.get('hdfsUser')),
            confirmButton: Em.I18n.t('common.next')
          });
          return App.showConfirmationFeedBackPopup(callback, msg);
        });
      } else if (isNNCheckpointTooOld == null) {
        // not available
        return App.showConfirmationPopup(
          callback, Em.I18n.t('services.service.stop.HDFS.warningMsg.checkPointNA'), null,
          Em.I18n.t('common.warning'), Em.I18n.t('common.proceedAnyway'), 'danger'
        );
      } else {
        // still young
        callback();
      }
    });
  },

  pullNnCheckPointTime: function () {
    return App.ajax.send({
      name: 'common.service.hdfs.getNnCheckPointTime',
      sender: this,
      success: 'parseNnCheckPointTime'
    });
  },

  parseNnCheckPointTime: function (data) {
    var nameNodesStatus = [];
    var lastCheckpointTime, hostName;
    if (data.host_components.length <= 1) {
      lastCheckpointTime = Em.get(data.host_components[0], 'metrics.dfs.FSNamesystem.LastCheckpointTime');
      hostName = Em.get(data.host_components[0], 'HostRoles.host_name');
    } else {
      // HA enabled
      data.host_components.forEach(function(namenode) {
        nameNodesStatus.pushObject( Em.Object.create({
          LastCheckpointTime: Em.get(namenode, 'metrics.dfs.FSNamesystem.LastCheckpointTime'),
          HAState: Em.get(namenode, 'metrics.dfs.FSNamesystem.HAState'),
          hostName: Em.get(namenode, 'HostRoles.host_name')
        }));
      });
      if (nameNodesStatus.someProperty('HAState', 'active')) {
        if (nameNodesStatus.findProperty('HAState', 'active').get('LastCheckpointTime')) {
          lastCheckpointTime = nameNodesStatus.findProperty('HAState', 'active').get('LastCheckpointTime');
          hostName = nameNodesStatus.findProperty('HAState', 'active').get('hostName');
        } else if (nameNodesStatus.someProperty('LastCheckpointTime')) {
          lastCheckpointTime = nameNodesStatus.findProperty('LastCheckpointTime').get('LastCheckpointTime');
          hostName = nameNodesStatus.findProperty('LastCheckpointTime').get('hostName');
        }
      } else if (nameNodesStatus.someProperty('HAState', 'standby')) {
        lastCheckpointTime = nameNodesStatus.findProperty('HAState', 'standby').get('LastCheckpointTime');
        hostName = nameNodesStatus.findProperty('HAState', 'standby').get('hostName')
      }
    }

    if (!lastCheckpointTime) {
      this.set("isNNCheckpointTooOld", null);
    } else {
      var time_criteria = App.nnCheckpointAgeAlertThreshold; // time in hours to define how many hours ago is too old
      var time_ago = (Math.round(App.dateTime() / 1000) - (time_criteria * 3600)) *1000;
      if (lastCheckpointTime <= time_ago) {
        // too old, set the effected hostName
        this.set("isNNCheckpointTooOld", hostName);
      } else {
        // still young
        this.set("isNNCheckpointTooOld", false);
      }
    }
  },

  /**
   * Return true if hdfs user data is loaded via App.MainServiceInfoConfigsController
   */
  getHdfsUser: function () {
    var self = this;
    var dfd = $.Deferred();
    var miscController = App.MainAdminServiceAccountsController.create();
    miscController.loadUsers();
    var interval = setInterval(function () {
      if (miscController.get('dataIsLoaded') && miscController.get('users')) {
        self.set('hdfsUser', miscController.get('users').findProperty('name', 'hdfs_user').get('value'));
        dfd.resolve();
        clearInterval(interval);
      }
    }, 10);
    return dfd.promise();
  },

  addAdditionalWarningMessage: function(serviceHealth, msg, serviceDisplayName){
    var servicesAffectedDisplayNames = [];
    var servicesAffected = [];

    if(serviceHealth == 'INSTALLED'){
      //To stop a service, display dependencies message...
      var currentService = this.get('content.serviceName');

      var stackServices = App.StackService.find();
      stackServices.forEach(function(service){
        if(service.get('isInstalled') || service.get('isSelected')){ //only care about services installed...
          var stackServiceDisplayName = service.get("displayName");
          var requiredServices = service.get('requiredServices'); //services required in order to have the current service be functional...
          if (!!requiredServices && requiredServices.length) { //only care about services with a non-empty requiredServices list.

            requiredServices.forEach(function(_requiredService){
              if (currentService === _requiredService) { //the service to be stopped is a required service by some other services...
                if(servicesAffected.indexOf(service) == -1 ) {
                  servicesAffected.push(service);
                  servicesAffectedDisplayNames.push(stackServiceDisplayName);
                }
              }
            },this);
          }
        }
      },this);

      var names = servicesAffectedDisplayNames.join();
      if(names){
        //only display this line with a non-empty dependency list
        var dependenciesMsg = Em.I18n.t('services.service.stop.warningMsg.dependent.services').format(serviceDisplayName, names);
        msg = msg ? msg + " " + dependenciesMsg : dependenciesMsg;
      }
    }

    return msg;
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
      'data': data,
      'showLoadingPopup': true
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
      App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
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
      error : 'refreshYarnQueuesErrorCallback',
      showLoadingPopup: true
    });
    });
  },
  refreshYarnQueuesSuccessCallback  : function(data, ajaxOptions, params) {
    if (data.Requests.id) {
      App.router.get('backgroundOperationsController').showPopup();
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
        error: 'startStopLdapKnoxErrorCallback',
        showLoadingPopup: true
      });
    });
  },

  startStopLdapKnoxSuccessCallback  : function(data, ajaxOptions, params) {
    if (data.Requests.id) {
      App.router.get('backgroundOperationsController').showPopup();
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
  },

  restartLLAP: function () {
    var isRefreshQueueRequired, self = this;
    return App.showConfirmationPopup(function () {
      // regresh queue request is sending only if YARN service has stale configs
      isRefreshQueueRequired = App.Service.find().findProperty('serviceName', 'YARN').get('isRestartRequired');
      if (isRefreshQueueRequired) {
        self.restartLLAPAndRefreshQueueRequest();
      } else {
        self.restartLLAPRequest();
      }
    });
  },

  restartLLAPRequest: function () {
    var host = App.HostComponent.find().findProperty('componentName', 'HIVE_SERVER_INTERACTIVE').get('hostName');
    App.ajax.send({
      name: 'service.item.executeCustomCommand',
      sender: this,
      data: {
        command: 'RESTART_LLAP',
        context: Em.I18n.t('services.service.actions.run.restartLLAP'),
        hosts: host,
        serviceName: "HIVE",
        componentName: "HIVE_SERVER_INTERACTIVE"
      },
      success: 'requestSuccessCallback',
      error: 'requestErrorCallback',
      showLoadingPopup: true
    });
  },

  restartLLAPAndRefreshQueueRequest: function () {
    var hiveServerInteractiveHost = App.HostComponent.find().findProperty('componentName', 'HIVE_SERVER_INTERACTIVE').get('hostName');
    var resourceManagerHost = App.HostComponent.find().findProperty('componentName', 'RESOURCEMANAGER').get('hostName');
    var batches = [{
      "order_id": 1,
      "type": "POST",
      "uri": "/clusters/" + App.get('clusterName') + "/requests",
      "RequestBodyInfo": {
        "RequestInfo": {
          "context": "Refresh YARN Capacity Scheduler",
          "command": "REFRESHQUEUES",
          "parameters/forceRefreshConfigTags": "capacity-scheduler"
        },
        "Requests/resource_filters": [{
          "service_name": "YARN",
          "component_name": "RESOURCEMANAGER",
          "hosts": resourceManagerHost
        }]
      }
    }, {
      "order_id": 2,
      "type": "POST",
      "uri": "/clusters/" + App.get('clusterName') + "/requests",
      "RequestBodyInfo": {
        "RequestInfo": {"context": "Restart LLAP", "command": "RESTART_LLAP"},
        "Requests/resource_filters": [{
          "service_name": "HIVE",
          "component_name": "HIVE_SERVER_INTERACTIVE",
          "hosts": hiveServerInteractiveHost
        }]
      }
    }];
    App.ajax.send({
      name: 'common.batch.request_schedules',
      sender: this,
      data: {
        intervalTimeSeconds: 1,
        tolerateSize: 0,
        batches: batches
      },
      success: 'requestSuccessCallback',
      error: 'requestErrorCallback',
      showLoadingPopup: true
    });
  },

  requestSuccessCallback: function () {
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
      if (initValue) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  },

  requestErrorCallback : function(data) {
    var error = Em.I18n.t('services.service.actions.run.executeCustomCommand.error');
    if (data && data.responseText) {
      try {
        var json = $.parseJSON(data.responseText);
        error += json.message;
      } catch (err) {}
    }
    App.showAlertPopup(Em.I18n.t('common.error'), error);
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
      disablePrimary: Em.computed.alias('isInvalid'),
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
          error : 'rebalanceHdfsNodesErrorCallback',
          showLoadingPopup: true
        });
        this.hide();
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/common/modal_popups/prompt_popup'),
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
  },

  regenerateKeytabFileOperations: function () {
    var self = this;
    var serviceName = this.content.get('serviceName');
    var clusterName = App.get('clusterName');
    return App.showConfirmationPopup(function() {
      return App.ajax.send({
        name: "admin.kerberos_security.regenerate_keytabs.service",
        sender: self,
        data: {
          clusterName: clusterName,
          serviceName: serviceName
        },
        success: 'regenerateKeytabFileOperationsRequestSuccess',
        error: 'regenerateKeytabFileOperationsRequestError'
      });
    }, Em.I18n.t('question.sure.regenerateKeytab.service').format(serviceName));
  },

  regenerateKeytabFileOperationsRequestSuccess: function(){
    App.router.get('backgroundOperationsController').showPopup();
  },

  regenerateKeytabFileOperationsRequestError: function () {
    App.showAlertPopup(Em.I18n.t('common.error'), Em.I18n.t('alerts.notifications.regenerateKeytab.service.error').format(this.content.get('serviceName')));
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
      App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
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

    // check HDFS NameNode checkpoint before stop service
    if (this.get('content.serviceName') == 'HDFS' &&
      this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
      this.checkNnLastCheckpointTime(function () {
        return App.showConfirmationFeedBackPopup(function(query, runMmOperation) {
          batchUtils.restartAllServiceHostComponents(serviceDisplayName, serviceName, false, query, runMmOperation);
        }, bodyMessage);
      });
    } else {
      return App.showConfirmationFeedBackPopup(function(query, runMmOperation) {
        batchUtils.restartAllServiceHostComponents(serviceDisplayName, serviceName, false, query, runMmOperation);
      }, bodyMessage);
    }
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
    var clusterLevelRequired = ['KERBEROS'];
    var requestData = {
        'serviceName': this.get('content.serviceName'),
        'displayName': this.get('content.displayName'),
        'actionName': this.get('content.serviceName') === 'ZOOKEEPER' ? 'ZOOKEEPER_QUORUM_SERVICE_CHECK' : this.get('content.serviceName') + '_SERVICE_CHECK',
        'query': query
    };
    if (clusterLevelRequired.contains(this.get('content.serviceName'))) {
      requestData.operationLevel = {
        "level": "CLUSTER",
        "cluster_name": App.get('clusterName')
      };
    }
    App.ajax.send({
      'name': 'service.item.smoke',
      'sender': this,
      'success':'runSmokeTestSuccessCallBack',
      'error':'runSmokeTestErrorCallBack',
      'data': requestData,
      showLoadingPopup: true
    });
  },

  runSmokeTestSuccessCallBack: function (data, ajaxOptions, params) {
    if (data.Requests.id) {
      // load data (if we need to show this background operations popup) from persist
      App.router.get('userSettingsController').dataLoading('show_bg').done(function (initValue) {
        params.query.set('status', 'SUCCESS');
        if (initValue) {
          App.router.get('backgroundOperationsController').showPopup();
        }
      });
    }
    else {
      params.query.set('status', 'FAIL');
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
    if (component) {
      var reassignMasterController = App.router.get('reassignMasterController');
      reassignMasterController.saveComponentToReassign(component);
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
    var component = App.StackServiceComponent.find().findProperty('componentName', componentName);

    App.get('router.mainAdminKerberosController').getKDCSessionState(function () {
      App.router.get('mainHostDetailsController').addComponentWithCheck(
        {
          context: component,
          fromServiceSummary: true
        }
      );
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

  nonClientServiceComponents: function () {
    return App.MasterComponent.find().toArray().concat(App.SlaveComponent.find().toArray()).filterProperty('service.serviceName', this.get('content.serviceName'));
  }.property('content.serviceName'),

  isStartDisabled: function () {
    if(this.get('isPending')) return true;

    var isDisabled = true;
    this.get('nonClientServiceComponents').forEach(function(component) {
      isDisabled = isDisabled ? !(component.get('installedAndMaintenanceOffCount') > 0) : false;
    });
    return isDisabled;
  }.property('isPending', 'nonClientServiceComponents'),

  isStopDisabled: function () {
    if(this.get('isPending')) return true;
    if (App.get('isHaEnabled') && this.get('content.serviceName') == 'HDFS' && this.get('content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
      return false;
    }
    if (this.get('content.serviceName') == 'HAWQ' && this.get('content.hostComponents').filterProperty('componentName', 'HAWQMASTER').someProperty('workStatus', App.HostComponentStatus.started)) {
      return false;
    }
    if (this.get('content.serviceName') == 'PXF' && App.HostComponent.find().filterProperty('componentName', 'PXF').someProperty('workStatus', App.HostComponentStatus.started)) {
      return false;
    }
    return (this.get('content.healthStatus') != 'green');
  }.property('content.healthStatus','isPending', 'App.isHaEnabled'),

  isSmokeTestDisabled: function () {
    if (this.get('isClientsOnlyService')) return false;
    // Disable PXF service check if at least one PXF is down
    if (this.get('content.serviceName') === 'PXF')
      return App.HostComponent.find().filterProperty('componentName', 'PXF').someProperty('workStatus','INSTALLED');
    return this.get('isStopDisabled');
  }.property('content.serviceName'),

  /**
   * Determine if service has than one service client components
   */
  isSeveralClients: function () {
    return App.StackServiceComponent.find().filterProperty('serviceName', this.get('content.serviceName')).filterProperty('isClient').length > 1;
  }.property('content.serviceName'),

  enableHighAvailability: function() {
    var highAvailabilityController = App.router.get('mainAdminHighAvailabilityController');
    highAvailabilityController.enableHighAvailability();
  },

  manageJournalNode: function() {
    var highAvailabilityController = App.router.get('mainAdminHighAvailabilityController');
    highAvailabilityController.manageJournalNode();
  },

  disableHighAvailability: function() {
    var highAvailabilityController = App.router.get('mainAdminHighAvailabilityController');
    highAvailabilityController.disableHighAvailability();
  },

  enableRMHighAvailability: function() {
    var highAvailabilityController = App.router.get('mainAdminHighAvailabilityController');
    highAvailabilityController.enableRMHighAvailability();
  },

  addHawqStandby: function() {
    var highAvailabilityController = App.router.get('mainAdminHighAvailabilityController');
    highAvailabilityController.addHawqStandby();
  },

  removeHawqStandby: function() {
    var highAvailabilityController = App.router.get('mainAdminHighAvailabilityController');
    highAvailabilityController.removeHawqStandby();
  },

  activateHawqStandby: function() {
    var highAvailabilityController = App.router.get('mainAdminHighAvailabilityController');
    highAvailabilityController.activateHawqStandby();
  },

  enableRAHighAvailability: function() {
    var highAvailabilityController = App.router.get('mainAdminHighAvailabilityController');
    highAvailabilityController.enableRAHighAvailability();
  },

  downloadClientConfigs: function (event) {
    var component = this.get('content.clientComponents').rejectProperty('totalCount', 0)[0];
    this.downloadClientConfigsCall({
      serviceName: this.get('content.serviceName'),
      componentName: (event && event.name) || component.get('componentName'),
      resourceType: this.resourceTypeEnum.SERVICE_COMPONENT
    });
  },

  openNameNodeFederationWizard: function () {
    var highAvailabilityController = App.router.get('mainAdminHighAvailabilityController');
    highAvailabilityController.enableNameNodeFederation();
  },

  /**
   * This method is called when user event to download configs for "All Clients"
   * is made from service action menu
   */
  downloadAllClientConfigs: function() {
    this.downloadClientConfigsCall({
      serviceName: this.get('content.serviceName'),
      resourceType: this.resourceTypeEnum.SERVICE
    });
  },

  /**
   * On click handler for custom hawq command from items menu
   * @param context
   */
  executeHawqCustomCommand: function(context) {
    var controller = this;
    return App.showConfirmationPopup(function() {
      App.ajax.send({
        name : 'service.item.executeCustomCommand',
        sender: controller,
        data : {
          command : context.command,
          context : context.label,
          hosts : App.Service.find(context.service).get('hostComponents').findProperty('componentName', context.component).get('hostName'),
          serviceName : context.service,
          componentName : context.component
        },
        success : 'executeCustomCommandSuccessCallback',
        error : 'executeCustomCommandErrorCallback',
        showLoadingPopup: true
      });
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
        error : 'executeCustomCommandErrorCallback',
        showLoadingPopup: true
      });
    });
  },

  executeCustomCommandSuccessCallback  : function(data, ajaxOptions, params) {
    if (data.Requests.id) {
      App.router.get('backgroundOperationsController').showPopup();
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
  },

  /**
   * find dependent services
   * @param {string[]} serviceNamesToDelete
   * @returns {Array}
   */
  findDependentServices: function (serviceNamesToDelete) {
    var dependentServices = [];

    App.Service.find().forEach(function (service) {
      if (!serviceNamesToDelete.contains(service.get('serviceName'))) {
        var requiredServices = App.StackService.find(service.get('serviceName')).get('requiredServices');
        serviceNamesToDelete.forEach(function (dependsOnService) {
          if (requiredServices.contains(dependsOnService)) {
            dependentServices.push(service.get('serviceName'));
          }
        });
      }
    }, this);
    return dependentServices;
  },

  /**
   * @param serviceNames
   * @returns {string}
   */
  servicesDisplayNames: function(serviceNames) {
    return serviceNames.map(function(serviceName) {
      return App.format.role(serviceName, true);
    }).join(',');
  },

  /**
   * Is services can be removed based on work status
   * @param serviceNames
   */
  allowUninstallServices: function(serviceNames) {
    return App.Service.find().filter(function (service) {
      return serviceNames.contains(service.get('serviceName'));
    }).everyProperty('allowToDelete');
  },

  /**
   * delete service action
   * @param {string} serviceName
   */
  deleteService: function(serviceName) {
    var self = this,
      interDependentServices = this.get('interDependentServices'),
      serviceNamesToDelete = this.get('serviceNamesToDelete'),
      dependentServices = this.findDependentServices(serviceNamesToDelete),
      displayName = App.format.role(serviceName, true),
      popupHeader = Em.I18n.t('services.service.delete.popup.header'),
      dependentServicesToDeleteFmt = this.servicesDisplayNames(interDependentServices);

    if (serviceName === 'KERBEROS') {
      this.kerberosDeleteWarning(popupHeader);
      return;
    }
    if (serviceName === 'RANGER' && this.isRangerPluginEnabled()) {
      App.ModalPopup.show({
        secondary: null,
        header: popupHeader,
        encodeBody: false,
        body: Em.I18n.t('services.service.delete.popup.ranger')
      });
      return;
    }

    if (App.Service.find().get('length') === 1) {
      //at least one service should be installed
      App.ModalPopup.show({
        secondary: null,
        header: popupHeader,
        encodeBody: false,
        body: Em.I18n.t('services.service.delete.lastService.popup.body').format(displayName)
      });
    } else if (dependentServices.length > 0) {
      this.dependentServicesWarning(serviceName, dependentServices);
    } else {
      var isServiceInRemovableState = this.allowUninstallServices(serviceNamesToDelete);
      if (isServiceInRemovableState) {
        if (serviceName === 'RANGER_KMS') {
          App.showConfirmationPopup(
            function () {
              self.showLastWarning(serviceName, interDependentServices, dependentServicesToDeleteFmt)
            },
            Em.I18n.t('services.service.delete.popup.warning.ranger_kms'),
            null,
            popupHeader,
            Em.I18n.t('common.delete'),
            'danger'
          );
        } else {
          this.showLastWarning(serviceName, interDependentServices, dependentServicesToDeleteFmt);
        }
      } else {
        var body = Em.I18n.t('services.service.delete.popup.mustBeStopped').format(displayName);
        if (interDependentServices.length) {
          body += Em.I18n.t('services.service.delete.popup.mustBeStopped.dependent').format(dependentServicesToDeleteFmt)
        }
        App.ModalPopup.show({
          secondary: null,
          header: popupHeader,
          encodeBody: false,
          body: body
        });
      }
    }
  },

  /**
   * show dialog with Kerberos warning prior to service delete
   * @param {string} header
   * @returns {App.ModalPopup}
   */
  kerberosDeleteWarning: function(header) {
    return App.ModalPopup.show({
      primary: Em.I18n.t('ok'),
      secondary: Em.I18n.t('services.alerts.goTo').format('Kerberos'),
      header: header,
      encodeBody: false,
      body: Em.I18n.t('services.service.delete.popup.kerberos'),
      onSecondary: function() {
        this._super();
        App.router.transitionTo('main.admin.adminKerberos.index');
      }
    });
  },

  /**
   * @returns {Boolean}
   */
  isRangerPluginEnabled: function() {
    return App.router.get('mainServiceInfoSummaryController.rangerPlugins')
          .filterProperty('isDisplayed').someProperty('status', 'Enabled');
  },

  /**
   * warning that show dependent services which must be deleted prior to chosen service deletion
   * @param {string} origin
   * @param {string[]} dependent
   * @returns {App.ModalPopup}
   */
  dependentServicesWarning: function(origin, dependent) {
    return App.ModalPopup.show({
      secondary: null,
      header: Em.I18n.t('services.service.delete.popup.header'),
      dependentMessage: Em.I18n.t('services.service.delete.popup.dependentServices').format(App.format.role(origin, true)),
      dependentServices: dependent,
      bodyClass: Em.View.extend({
        templateName: require('templates/main/service/info/dependent_services_warning')
      })
    });
  },

  showLastWarning: function (serviceName, interDependentServices, dependentServicesToDeleteFmt) {
    var self = this,
      displayName = App.format.role(serviceName, true),
      popupHeader = Em.I18n.t('services.service.delete.popup.header'),
      popupPrimary = Em.I18n.t('common.delete'),
      warningMessage = Em.I18n.t('services.service.delete.popup.warning').format(displayName) +
        (interDependentServices.length ? Em.I18n.t('services.service.delete.popup.warning.dependent').format(dependentServicesToDeleteFmt) : ''),
      callback = this.loadConfigRecommendations.bind(this, null, function () {
        var serviceNames = self.get('changedProperties').mapProperty('serviceName').uniq();
        self.loadConfigGroups(serviceNames).done(function () {
          self.set('isRecommendationInProgress', false);
        })
      });
    this.clearRecommendations();
    this.setProperties({
      isRecommendationInProgress: true,
      selectedConfigGroup: Em.Object.create({
        isDefault: true
      })
    });
    this.loadConfigs();
    App.get('router.mainController.isLoading').call(this, 'isServiceConfigsLoaded').done(callback);
    return App.ModalPopup.show({
      controller: self,
      header: popupHeader,
      primary: popupPrimary,
      primaryClass: 'btn-danger',
      disablePrimary: Em.computed.alias('controller.isRecommendationInProgress'),
      classNameBindings: ['controller.changedProperties.length:common-modal-wrapper', 'controller.changedProperties.length:modal-full-width'],
      modalDialogClasses: Em.computed.ifThenElse('controller.changedProperties.length', ['modal-lg'], []),
      bodyClass: Em.View.extend({
        templateName: require('templates/main/service/info/delete_service_warning_popup'),
        warningMessage: new Em.Handlebars.SafeString(warningMessage)
      }),
      onPrimary: function () {
        self.confirmDeleteService(serviceName, interDependentServices, dependentServicesToDeleteFmt);
        this._super();
      },
      onSecondary: function () {
        self.clearRecommendations();
        this._super();
      },
      onClose: function () {
        self.clearRecommendations();
        this._super();
      }
    });
  },

  /**
   * Confirmation popup of service deletion
   * @param {string} serviceName
   * @param {string[]} [dependentServiceNames]
   * @param {string} [servicesToDeleteFmt]
   */
  confirmDeleteService: function (serviceName, dependentServiceNames, servicesToDeleteFmt) {
    var confirmKey = 'delete',
        self = this,
        message = Em.I18n.t('services.service.confirmDelete.popup.body').format(App.format.role(serviceName, true), confirmKey);

    if (dependentServiceNames.length > 0) {
      message = Em.I18n.t('services.service.confirmDelete.popup.body.dependent')
                .format(App.format.role(serviceName, true), servicesToDeleteFmt, confirmKey);
    }

    App.ModalPopup.show({

      /**
       * @function onPrimary
       */
      onPrimary: function() {
        var serviceNames = [serviceName].concat(dependentServiceNames),
          serviceDisplayNames = serviceNames.map(function (serviceName) {
            return App.Service.find(serviceName).get('displayName');
          }),
          progressPopup = App.ModalPopup.show({
            classNames: ['delete-service-progress'],
            header: Em.I18n.t('services.service.delete.popup.header'),
            showFooter: false,
            message: Em.I18n.t('services.service.delete.progressPopup.message').format(stringUtils.getFormattedStringFromArray(serviceDisplayNames)),
            bodyClass: Em.View.extend({
              classNames: ['delete-service-progress-body'],
              template: Em.Handlebars.compile('{{view App.SpinnerView}}<div class="progress-message">{{message}}</div>')
            }),
            onClose: function () {
              self.set('deleteServiceProgressPopup', null);
              this._super();
            }
          });
        App.get('router.mainAdminKerberosController').getKDCSessionState(function() {
          self.set('deleteServiceProgressPopup', progressPopup);
          self.deleteServiceCall(serviceNames);
        });
        this._super();
      },

      /**
       * @type {string}
       */
      primary: Em.I18n.t('common.delete'),

      /**
       * @type {string}
       */
      primaryClass: 'btn-danger',

      /**
       * @type {string}
       */
      header: Em.I18n.t('services.service.confirmDelete.popup.header'),

      /**
       * @type {string}
       */
      confirmInput: '',

      /**
       * @type {boolean}
       */
      disablePrimary: Em.computed.notEqual('confirmInput', confirmKey),

      message: message,

      /**
       * @type {Em.View}
       */
      bodyClass: Em.View.extend({
        confirmKey: confirmKey,
        typeMessage: Em.I18n.t('services.service.confirmDelete.popup.body.type').format(confirmKey),
        templateName: require('templates/main/service/info/confirm_delete_service')
      }),

      enterKeyPressed: function() {
        if (this.get('disablePrimary')) return;
        this.onPrimary();
      }
    });
  },

  /**
   * All host names
   * This property required for request for recommendations
   *
   * @type {String[]}
   * @override
   */
  hostNames: Em.computed.alias('App.allHostNames'),

  /**
   * Recommendation object
   * This property required for request for recommendations
   *
   * @type {Object}
   * @override
   */
  hostGroups: function() {
    var hostGroup = blueprintUtils.generateHostGroups(App.get('allHostNames'));
    return blueprintUtils.removeDeletedComponents(hostGroup, [this.get('serviceNamesToDelete')]);
  }.property('serviceNamesToDelete', 'App.allHostNames', 'App.componentToBeAdded', 'App.componentToBeDeleted'),

  /**
   * List of services without removed
   * This property required for request for recommendations
   *
   * @type {String[]}
   * @override
   */
  serviceNames: function() {
    return App.Service.find().filter(function(s) {
      return !this.get('serviceNamesToDelete').contains(s.get('serviceName'));
    }, this).mapProperty('serviceName');
  }.property('serviceNamesToDelete'),

  /**
   * This property required for request for recommendations
   *
   * @return {Boolean}
   * @override
   */
  isConfigHasInitialState: function() { return false; },

  /**
   * Describes condition when recommendation should be applied
   * Unfortunately for removing services it's not always true
   * Property should be updated only if it depends on service that will be removed
   * (similar as on add service)
   *
   * @return {Boolean}
   * @override
   */
  allowUpdateProperty: function (parentProperties, name, fileName, configGroup, savedValue) {
    var stackProperty = App.configsCollection.getConfigByName(name, fileName);
    if (!stackProperty || (stackProperty.serviceName === this.get('content.serviceName'))) {
      /**
       * update properties for current service (in case will be used not only for removing service)
       * and properties that are not defined in stack
       */
      return true;
    }
    if (stackProperty.propertyDependsOn.length) {
      /**
       * update properties that depends on current service
       */
      return stackProperty.propertyDependsOn.some(function (p) {
        var service = App.config.get('serviceByConfigTypeMap')[p.type];
        return service && (this.get('content.serviceName') === service.get('serviceName'));
      }, this);
    }
    return !Em.isNone(savedValue) && stackProperty.recommendedValue === savedValue;
  },

  /**
   * Just config version note
   *
   * @type {String}
   */
  serviceConfigVersionNote: function() {
    var services = this.get('serviceNamesToDelete').join(',');
    if (this.get('serviceNamesToDelete.length') === 1) {
      return Em.I18n.t('services.service.delete.configVersionNote').format(services);
    }
    return Em.I18n.t('services.service.delete.configVersionNote.plural').format(services);
  }.property('serviceNamesToDelete'),

  /**
   * Method ot save configs after service have been removed
   * @override
   */
  saveConfigs: function() {
    var data = [],
        progressPopup = this.get('deleteServiceProgressPopup'),
        stepConfigs = this.get('stepConfigs');

    this.applyRecommendedValues(stepConfigs);

    stepConfigs.forEach(function (stepConfig) {
      var serviceConfig = this.getServiceConfigToSave(stepConfig.get('serviceName'), stepConfig.get('configs'));

      if (serviceConfig)  {
        data.push(serviceConfig);
      }
    }, this);

    if (Em.isArray(data) && data.length) {
      this.putChangedConfigurations(data, 'confirmServiceDeletion', function () {
        if (progressPopup) {
          progressPopup.onClose();
        }
      });
    } else {
      this.confirmServiceDeletion();
    }
  },

  applyRecommendedValues: function (stepConfigs) {
    var changedProperties = this.get('changedProperties');
    changedProperties.forEach(function (property) {
      var serviceConfigs = stepConfigs.findProperty('serviceName', property.serviceName);
      if (serviceConfigs) {
        var prop = serviceConfigs.get('configs').findProperty('name', property.propertyName);
        if (prop) {
          prop.set('value', property.saveRecommended ? property.recommendedValue : property.initialValue);
        }
      }
    });
    return stepConfigs;
  },

  confirmServiceDeletion: function() {
    let serviceNames, msg;
    if (this.get('interDependentServices.length')) {
      serviceNames = this.get('serviceNamesToDelete').map(serviceName => App.format.role(serviceName, true)).join(', ');
      msg = Em.I18n.t('services.service.delete.service.success.confirmation.plural').format(serviceNames);
    }
    else {
      serviceNames = App.format.role(this.get('content.serviceName'), true);
      msg = Em.I18n.t('services.service.delete.service.success.confirmation').format(serviceNames);
    }

    var progressPopup = this.get('deleteServiceProgressPopup');
    if (progressPopup) {
      progressPopup.onClose();
    }
    return App.showAlertPopup(Em.I18n.t('popup.confirmation.commonHeader'), msg, function() {
      window.location.reload();
    })
  },

  /**
   * Ajax call to delete service
   * @param {string[]} serviceNames
   * @returns {$.ajax}
   */
  deleteServiceCall: function(serviceNames) {
    var serviceToDeleteNow = serviceNames[0];
    if (serviceNames.length > 1) {
      var servicesToDeleteNext = serviceNames.slice(1);
    }
    App.Service.find().findProperty('serviceName', serviceToDeleteNow).set('deleteInProgress', true);
    return App.ajax.send({
      name : 'common.delete.service',
      sender: this,
      data : {
        serviceName : serviceToDeleteNow,
        servicesToDeleteNext: servicesToDeleteNext
      },
      success : 'deleteServiceCallSuccessCallback',
      error: 'deleteServiceCallErrorCallback',
      showLoadingPopup: true
    });
  },

  deleteServiceCallSuccessCallback: function(data, ajaxOptions, params) {
    if (params.servicesToDeleteNext) {
      this.deleteServiceCall(params.servicesToDeleteNext);
    } else {
      this.saveConfigs();
    }
  },

  deleteServiceCallErrorCallback: function (jqXHR, ajaxOptions, error, opt) {
    var progressPopup = this.get('deleteServiceProgressPopup');
    if (progressPopup) {
      progressPopup.onClose();
    }
    App.ajax.defaultErrorHandler(jqXHR, opt.url, opt.type, jqXHR.status);
  }

});
