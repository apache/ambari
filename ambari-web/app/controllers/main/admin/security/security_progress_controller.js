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
  secureMapping: function () {
    if (App.get('isHadoop2Stack')) {
      return require('data/HDP2/secure_mapping');
    } else {
      return require('data/secure_mapping');
    }
  }.property(App.isHadoop2Stack),
  secureProperties: function () {
    if (App.get('isHadoop2Stack')) {
      return require('data/HDP2/secure_properties').configProperties;
    } else {
      return require('data/secure_properties').configProperties;
    }
  }.property(App.isHadoop2Stack),
  commands: [],
  configs: [],
  serviceConfigTags: [],
  globalProperties: [],
  totalSteps: 3,
  isSubmitDisabled: true,


  hasHostPopup: true,
  services: [],
  serviceTimestamp: null,


  retry: function () {
    var failedCommand = this.get('commands').findProperty('isError', true);
    if (failedCommand) {
      failedCommand.set('isStarted', false);
      failedCommand.set('isError', false);
      this.startCommand(failedCommand);
    }
  },

  updateServices: function () {
    this.services.clear();
    var services = this.get("services");
    this.get("commands").forEach(function (command) {
      var newService = Ember.Object.create({
        name: command.label,
        hosts: []
      });
      if (command && command.get("polledData")) {
        var hostNames = command.get("polledData").mapProperty('Tasks.host_name').uniq();
        hostNames.forEach(function (name) {
          newService.hosts.push({
            name: name,
            publicName: name,
            logTasks: command.polledData.filterProperty("Tasks.host_name", name)
          });
        });
        services.push(newService);
      }
    });
    this.set('serviceTimestamp', App.dateTime());
  }.observes('commands.@each.polledData'),

  loadCommands: function () {
    this.get('commands').pushObjects([
      App.Poll.create({name: 'STOP_SERVICES', label: Em.I18n.translations['admin.addSecurity.apply.stage2'], isPolling: true }),
      App.Poll.create({name: 'APPLY_CONFIGURATIONS', label: Em.I18n.translations['admin.addSecurity.apply.stage3'], isPolling: false }),
      App.Poll.create({name: 'START_SERVICES', label: Em.I18n.translations['admin.addSecurity.apply.stage4'], isPolling: true })
    ]);
  },

  startCommand: function (commnad) {
    if (this.get('commands').length === this.totalSteps) {
      if (!commnad) {
        var startedCommand = this.get('commands').filterProperty('isStarted', true);
        commnad = startedCommand.findProperty('isCompleted', false);
      }
      if (commnad && commnad.get('isPolling') === true) {
        commnad.set('isStarted', true);
        commnad.start();
      } else if (commnad && commnad.get('name') === 'APPLY_CONFIGURATIONS') {
        commnad.set('isStarted', true);
        if (App.testMode) {
          commnad.set('isError', false);
          commnad.set('isSuccess', true);
        } else {
          this.loadClusterConfigs();
        }
      } else if (commnad && commnad.get('name') === 'DELETE_ATS') {
        commnad.set('isStarted', true);
        if (App.testMode) {
          commnad.set('isError', false);
          commnad.set('isSuccess', true);
        } else {
          var timeLineServer = App.Service.find('YARN').get('hostComponents').findProperty('componentName', 'APP_TIMELINE_SERVER');
          this.deleteComponents('APP_TIMELINE_SERVER', timeLineServer.get('host.hostName'));
        }
      }
    }
  },


  onCompleteCommand: function () {
    if (this.get('commands').length === this.totalSteps) {
      var index = this.get('commands').filterProperty('isSuccess', true).length;
      if (index > 0) {
        var lastCompletedCommandResult = this.get('commands').objectAt(index - 1).get('isSuccess');
        if (lastCompletedCommandResult) {
          var nextCommand = this.get('commands').objectAt(index);
          this.moveToNextCommand(nextCommand);
        }
      }
    }
  },

  moveToNextCommand: function (nextCommand) {
    if (!nextCommand) {
      nextCommand = this.get('commands').findProperty('isStarted', false);
    }
    if (nextCommand) {
      this.startCommand(nextCommand);
    }
  },

  addInfoToCommands: function () {
    this.addInfoToStopService();
    this.addInfoToStartServices();
  },


  addInfoToStopService: function () {
    var command = this.get('commands').findProperty('name', 'STOP_SERVICES');
    var url = (App.testMode) ? '/data/wizard/deploy/2_hosts/poll_1.json' : App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/services';
    var data = '{"RequestInfo": {"context" :"' + Em.I18n.t('requestInfo.stopAllServices') + '"}, "Body": {"ServiceInfo": {"state": "INSTALLED"}}}';
    command.set('url', url);
    command.set('data', data);
  },

  addInfoToStartServices: function () {
    var command = this.get('commands').findProperty('name', 'START_SERVICES');
    var url = (App.testMode) ? '/data/wizard/deploy/2_hosts/poll_1.json' : App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/services?params/run_smoke_test=true';
    var data = '{"RequestInfo": {"context": "' + Em.I18n.t('requestInfo.startAllServices') + '"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}';
    command.set('url', url);
    command.set('data', data);
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
    var self = this;
    //prepare tags to fetch all configuration for a service
    this.get('secureServices').forEach(function (_secureService) {
      self.setServiceTagNames(_secureService, data.Clusters.desired_configs);
    },this);
    this.getAllConfigurations();
  },

  loadClusterConfigsErrorCallback: function (request, ajaxOptions, error) {
    var command = this.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS');
    command .set('isSuccess', false);
    command .set('isError', true);
    console.log("TRACE: error code status is: " + request.status);
  },

  /**
   * set tagnames for configuration of the *-site.xml
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

  applyConfigurationsToCluster: function () {
    var configData = [];
    this.get('serviceConfigTags').forEach(function (_serviceConfig) {
      var Clusters = {
        Clusters: {
          desired_config: {
            type: _serviceConfig.siteName,
            tag: _serviceConfig.newTagName,
            properties: _serviceConfig.configs
          }
        }
      };
      configData.pushObject(JSON.stringify(Clusters));
    }, this);

    var data = {
      configData: '[' + configData.toString() + ']'
    };

    App.ajax.send({
      name: 'admin.security.apply_configurations',
      sender: this,
      data: data,
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
      _tag.configs = data.items.findProperty('type', _tag.siteName).properties;
    }, this);
    if (this.manageSecureConfigs()) {
      this.escapeXMLCharacters(this.get('serviceConfigTags'));
      this.applyConfigurationsToCluster();
    }
  },

  getAllConfigurationsErrorCallback: function (request, ajaxOptions, error) {
    var command = this.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS');
    command.set('isSuccess', false);
    command.set('isError', true);
    console.log("TRACE: In error function for the getServiceConfigsFromServer call");
    console.log("TRACE: error code status is: " + request.status);
  },

  /*
   Iterate over keys of all configurations and escape xml characters in their values
   */
  escapeXMLCharacters: function (serviceConfigTags) {
    serviceConfigTags.forEach(function (_serviceConfigTags) {
      var configs = _serviceConfigTags.configs;
      for (var key in configs) {
        configs[key] = this.setServerConfigValue(key, configs[key]);
      }
    }, this);
  },

  setServerConfigValue: function(configName, value) {
    switch (configName) {
      case 'storm.zookeeper.servers':
        return value;
      default:
        return App.config.escapeXMLCharacters(value);
    }
  },

  saveCommandsOnRequestId: function () {
    this.saveCommands();
  }.observes('commands.@each.requestId'),

  saveCommandsOnCompleted: function () {
    this.saveCommands();
  }.observes('commands.@each.isCompleted'),

  saveCommands: function () {
    var commands = [];
    if (this.get('commands').length === this.totalSteps) {
      this.get('commands').forEach(function (_command) {
        var command = {
          name: _command.get('name'),
          label: _command.get('label'),
          isPolling: _command.get('isPolling'),
          isVisible:  _command.get('isVisible'),
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
      if (!App.testMode) {
        App.clusterStatus.setClusterStatus({
          clusterName: this.get('clusterName'),
          clusterState: 'ADD_SECURITY_STEP_4',
          wizardControllerName: App.router.get('addSecurityController.name'),
          localdb: App.db.data
        });
      }
    }
  }
});
