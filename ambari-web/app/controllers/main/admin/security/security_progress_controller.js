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

App.MainAdminSecurityProgressController = Em.Controller.extend({
  name: 'mainAdminSecurityProgressController',

  commands: [],
  configs: [],
  serviceConfigTags: [],
  totalSteps: 3,
  isSubmitDisabled: true,
  hasHostPopup: true,
  services: [],
  serviceTimestamp: null,
  operationsInfo: [
    {
      name: 'STOP_SERVICES',
      realUrl: '/services',
      testUrl: '/data/wizard/deploy/2_hosts/poll_1.json',
      data: '{"RequestInfo": {"context" :"' + Em.I18n.t('requestInfo.stopAllServices') + '"}, "Body": {"ServiceInfo": {"state": "INSTALLED"}}}'
    },
    {
      name: 'START_SERVICES',
      realUrl: '/services?params/run_smoke_test=true',
      testUrl: '/data/wizard/deploy/2_hosts/poll_1.json',
      data: '{"RequestInfo": {"context": "' + Em.I18n.t('requestInfo.startAllServices') + '"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}'
    }
  ],

  secureMapping: require('data/HDP2/secure_mapping'),

  secureProperties: require('data/HDP2/secure_properties').configProperties,

  /**
   * prepare and restart failed command
   */
  retry: function () {
    var failedCommand = this.get('commands').findProperty('isError');
    if (failedCommand) {
      failedCommand.set('requestId', null);
      failedCommand.set('isStarted', false);
      failedCommand.set('isError', false);
      this.startCommand(failedCommand);
    }
  },

  /**
   * start updating current task in parallel
   * @param requestId
   * @param taskId
   * @return {Boolean}
   */
  startUpdatingTask: function (requestId, taskId) {
    if (!requestId || !taskId) return false;
    var command = this.get('commands').findProperty('requestId', requestId);
    command.updateTaskLog(taskId);
    return true;
  },

  /**
   * stop updating current task
   * @param requestId
   * @return {Boolean}
   */
  stopUpdatingTask: function (requestId) {
    if (!requestId) return false;
    var command = this.get('commands').findProperty('requestId', requestId);
    command.set('currentTaskId', null);
    return true;
  },

  /**
   * update info about progress of operation of commands
   */
  updateServices: function () {
    this.services.clear();
    var services = this.get("services");
    this.get("commands").forEach(function (command) {
      var polledData = command.get('polledData');
      var newService = Ember.Object.create({
        name: command.get('label'),
        hosts: []
      });
      if (polledData) {
        var hostNames = polledData.mapProperty('Tasks.host_name').uniq();
        hostNames.forEach(function (name) {
          newService.hosts.push({
            name: name,
            publicName: name,
            logTasks: polledData.filterProperty("Tasks.host_name", name)
          });
        });
        services.push(newService);
      }
    });
    this.set('serviceTimestamp', App.dateTime());
  }.observes('commands.@each.polledData'),
  /**
   * initialize default commands
   */
  loadCommands: function () {
    this.get('commands').pushObjects([
      App.Poll.create({name: 'STOP_SERVICES', label: Em.I18n.translations['admin.addSecurity.apply.stop.services'], isPolling: true }),
      App.Poll.create({name: 'APPLY_CONFIGURATIONS', label: Em.I18n.translations['admin.addSecurity.apply.save.config'], isPolling: false }),
      App.Poll.create({name: 'START_SERVICES', label: Em.I18n.translations['admin.addSecurity.apply.start.services'], isPolling: true })
    ]);
  },

  addObserverToCommands: function () {
    this.setIndex(this.get('commands'));
    this.addObserver('commands.@each.isSuccess', this, 'onCompleteCommand');
  },
  /**
   * set index to each command
   * @param commandArray
   */
  setIndex: function (commandArray) {
    commandArray.forEach(function (command, index) {
      command.set('index', index + 1);
    }, this);
    this.set('totalSteps', commandArray.length);
  },

  startCommand: function (command) {
    if (this.get('commands').length === this.get('totalSteps')) {
      if (!command) {
        var startedCommand = this.get('commands').filterProperty('isStarted', true);
        command = startedCommand.findProperty('isCompleted', false);
      }
      if (command) {
        if (command.get('isPolling')) {
          command.set('isStarted', true);
          command.start();
        } else if (command.get('name') === 'APPLY_CONFIGURATIONS') {
          command.set('isStarted', true);
          if (App.get('testMode')) {
            command.set('isError', false);
            command.set('isSuccess', true);
          } else {
            this.loadClusterConfigs();
          }
        } else if (command.get('name') === 'DELETE_ATS') {
          command.set('isStarted', true);
          if (App.get('testMode')) {
            command.set('isError', false);
            command.set('isSuccess', true);
          } else {
            var timeLineServer = App.HostComponent.find().findProperty('componentName', 'APP_TIMELINE_SERVER');
            if (timeLineServer) {
              this.deleteComponents('APP_TIMELINE_SERVER', timeLineServer.get('hostName'));
            } else {
              this.onDeleteComplete();
            }
          }
        }
        return true;
      }
    }
    return false;
  },
  /**
   * on command completion move to next command
   * @return {Boolean}
   */
  onCompleteCommand: function () {
    if (this.get('commands').length === this.get('totalSteps')) {
      var index = this.get('commands').filterProperty('isSuccess', true).length;
      if (index > 0) {
        var lastCompletedCommandResult = this.get('commands').objectAt(index - 1).get('isSuccess');
        if (lastCompletedCommandResult) {
          var nextCommand = this.get('commands').objectAt(index);
          this.moveToNextCommand(nextCommand);
          return true;
        }
      }
    }
    return false;
  },
  /**
   * move to next command
   * @param nextCommand
   */
  moveToNextCommand: function (nextCommand) {
    nextCommand = nextCommand || this.get('commands').findProperty('isStarted', false);
    if (nextCommand) {
      this.startCommand(nextCommand);
      return true;
    }
    return false;
  },

  /**
   * add query information(url, data) to commands
   */
  addInfoToCommands: function () {
    var operationsInfo = this.get('operationsInfo');
    var urlPrefix = App.apiPrefix + '/clusters/' + App.get('clusterName');
    operationsInfo.forEach(function (operation) {
      var command = this.get('commands').findProperty('name', operation.name);
      var url = (App.get('testMode')) ? operation.testUrl : urlPrefix + operation.realUrl;
      command.set('url', url);
      command.set('data', operation.data);
    }, this);
  },

  loadClusterConfigs: function () {
    App.ajax.send({
      name: 'admin.security.add.cluster_configs',
      sender: this,
      success: 'loadClusterConfigsSuccessCallback',
      error: 'loadClusterConfigsErrorCallback'
    });
  },

  loadClusterConfigsSuccessCallback: function (data) {
    //prepare tags to fetch all configuration for a service
    this.get('secureServices').forEach(function (_secureService) {
      this.setServiceTagNames(_secureService, data.Clusters.desired_configs);
    }, this);
    this.getAllConfigurations();
  },

  loadClusterConfigsErrorCallback: function (request, ajaxOptions, error) {
    var command = this.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS');
    command.set('isSuccess', false);
    command.set('isError', true);
    console.log("TRACE: error code status is: " + request.status);
  },

  /**
   * set tag names according to installed services and desired configs
   * @param secureService
   * @param configs
   * @return {Object}
   */
  setServiceTagNames: function (secureService, configs) {
    //var serviceConfigTags = this.get('serviceConfigTags');
    for (var index in configs) {
      if (secureService.sites && secureService.sites.contains(index)) {
        var serviceConfigObj = {
          siteName: index,
          tagName: configs[index].tag,
          newTagName: null,
          configs: {}
        };
        this.get('serviceConfigTags').pushObject(serviceConfigObj);
      }
    }
    return serviceConfigObj;
  },

  /**
   * form query data and apply security configurations to server
   */
  applyConfigurationsToCluster: function () {
    var configData = this.get('serviceConfigTags').map(function (_serviceConfig) {
      var res = {
        type: _serviceConfig.siteName,
        tag: _serviceConfig.newTagName,
        properties: _serviceConfig.configs,
        service_config_version_note: Em.I18n.t('admin.security.step4.save.configuration.note')
      };
      if (_serviceConfig.properties_attributes) {
        res['properties_attributes'] = _serviceConfig.properties_attributes
      }
      return res;
    }, this);

    var selectedServices = this.get('secureServices');
    var allConfigData = [];
    selectedServices.forEach(function (service) {
      var stackService = App.StackService.find(service.serviceName);
      if (stackService) {
        var serviceConfigData = [];
        Object.keys(stackService.get('configTypesRendered')).forEach(function (type) {
          var serviceConfigTag = configData.findProperty('type', type);
          if (serviceConfigTag) {
            serviceConfigData.pushObject(serviceConfigTag);
          }
        }, this);
        allConfigData.pushObject(JSON.stringify({
          Clusters: {
            desired_config: serviceConfigData
          }
        }));
      }
    }, this);

    var clusterConfig = configData.findProperty('type', 'cluster-env');
    if (clusterConfig) {
      allConfigData.pushObject(JSON.stringify({
        Clusters: {
          desired_config: [clusterConfig]
        }
      }));
    }
    App.ajax.send({
      name: 'common.across.services.configurations',
      sender: this,
      data: {
        data: '[' + allConfigData.toString() + ']'
      },
      success: 'applyConfigurationToClusterSuccessCallback',
      error: 'applyConfigurationToClusterErrorCallback'
    });
  },

  applyConfigurationToClusterSuccessCallback: function (data) {
    var command = this.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS');
    command.set('isSuccess', true);
    command.set('isError', false);
  },

  applyConfigurationToClusterErrorCallback: function (request, ajaxOptions, error) {
    var command = this.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS');
    command.set('isSuccess', false);
    command.set('isError', true);
  },

  /**
   * gets site config properties from server and sets it for every configuration
   */
  getAllConfigurations: function () {
    var urlParams = [];
    this.get('serviceConfigTags').forEach(function (_tag) {
      urlParams.push('(type=' + _tag.siteName + '&tag=' + _tag.tagName + ')');
    }, this);
    if (urlParams.length > 0) {
      App.ajax.send({
        name: 'admin.get.all_configurations',
        sender: this,
        data: {
          urlParams: urlParams.join('|')
        },
        success: 'getAllConfigurationsSuccessCallback',
        error: 'getAllConfigurationsErrorCallback'
      });
    }
  },

  getAllConfigurationsSuccessCallback: function (data) {
    console.log("TRACE: In success function for the GET getServiceConfigsFromServer call");
    var command = this.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS');
    this.get('serviceConfigTags').forEach(function (_tag) {
      if (!data.items.someProperty('type', _tag.siteName)) {
        console.log("Error: Metadata for secure services (secure_configs.js) is having config tags that are not being retrieved from server");
        command.set('isSuccess', false);
        command.set('isError', true);
      }
      var cfg = data.items.findProperty('type', _tag.siteName);
      _tag.configs = this.modifyConfigsForSecure(_tag.siteName, cfg);
      if (cfg.properties_attributes) {
        _tag.properties_attributes = cfg.properties_attributes;
      }
    }, this);
    if (this.manageSecureConfigs()) {
      this.applyConfigurationsToCluster();
    }
  },

  propertiesToUpdate: [
    {
      siteName: 'storm-site',
      name: 'ui.childopts',
      append: ' -Djava.security.auth.login.config=/etc/storm/conf/storm_jaas.conf'
    },
    {
      siteName: 'storm-site',
      name: 'supervisor.childopts',
      append: ' -Djava.security.auth.login.config=/etc/storm/conf/storm_jaas.conf'
    },
    {
      siteName: 'storm-site',
      name: 'nimbus.childopts',
      append: ' -Djava.security.auth.login.config=/etc/storm/conf/storm_jaas.conf'
    }
  ],

  /**
   * updates some configs for correct work in secure mode
   * @method modifyConfigsForSecure
   * @param {String} siteName
   * @param {Object} cfg
   * {
   *   properties: {
   *    'ui.childopts': 'value1'
   *    'property2': 'value2'
   *   }
   * };
   * has other properties but required filed is "properties";
   * @returns {Object}
   *   properties: {
   *    'ui.childopts': 'value1 -Djava.security.auth.login.config=/etc/storm/conf/storm_jaas.conf'
   *    'property2': 'value2'
   *   }
   */
  modifyConfigsForSecure: function(siteName, cfg) {
    var propertiesToUpdate = this.get('propertiesToUpdate').filterProperty('siteName', siteName);
    if (propertiesToUpdate.length) {
      propertiesToUpdate.forEach(function(p) {
        cfg.properties[p.name] += p.append;
      }, this);
    }
    return cfg.properties
  },

  getAllConfigurationsErrorCallback: function (request, ajaxOptions, error) {
    var command = this.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS');
    command.set('isSuccess', false);
    command.set('isError', true);
    console.log("TRACE: In error function for the getServiceConfigsFromServer call");
    console.log("TRACE: error code status is: " + request.status);
  },

  /**
   * save commands to server and local storage
   */
  saveCommands: function () {
    var commands = [];
    if (this.get('commands').length === this.get('totalSteps')) {
      this.get('commands').forEach(function (_command) {
        var command = {
          name: _command.get('name'),
          label: _command.get('label'),
          isPolling: _command.get('isPolling'),
          isVisible: _command.get('isVisible'),
          isStarted: _command.get('isStarted'),
          requestId: _command.get('requestId'),
          isSuccess: _command.get('isSuccess'),
          isError: _command.get('isError'),
          url: _command.get('url'),
          polledData: _command.get('polledData'),
          data: _command.get('data')
        };
        commands.pushObject(command);
      }, this);
      App.db.setSecurityDeployCommands(commands);
      if (!App.get('testMode')) {
        App.clusterStatus.setClusterStatus({
          clusterName: this.get('clusterName'),
          clusterState: 'ADD_SECURITY_STEP_4',
          wizardControllerName: App.router.get('addSecurityController.name'),
          localdb: App.db.data
        });
      }
    }
  }.observes('commands.@each.isCompleted', 'commands.@each.requestId')
});
