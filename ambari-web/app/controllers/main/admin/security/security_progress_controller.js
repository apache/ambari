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
  stages: [],
  configs: [],
  serviceConfigTags: [],
  globalProperties: [],
  totalSteps: 3,

  isSubmitDisabled: true,


  hasHostPopup: true,
  services: [],
  serviceTimestamp: null,


  retry: function () {
    var failedStage = this.get('stages').findProperty('isError', true);
    if (failedStage) {
      failedStage.set('isStarted', false);
      failedStage.set('isError', false);
      this.startStage(failedStage);
    }
  },

  updateServices: function () {
    this.services.clear();
    var services = this.get("services");
    this.get("stages").forEach(function (stage) {
      var newService = Ember.Object.create({
        name: stage.label,
        hosts: []
      });
      if (stage && stage.get("polledData")) {
        var hostNames = stage.get("polledData").mapProperty('Tasks.host_name').uniq();
        hostNames.forEach(function (name) {
          newService.hosts.push({
            name: name,
            publicName: name,
            logTasks: stage.polledData.filterProperty("Tasks.host_name", name)
          });
        });
        services.push(newService);
      }
    });
    this.set('serviceTimestamp', App.dateTime());
  }.observes('stages.@each.polledData'),

  loadStages: function () {
    this.get('stages').pushObjects([
      App.Poll.create({stage: 'stage2', label: Em.I18n.translations['admin.addSecurity.apply.stage2'], isPolling: true, name: 'STOP_SERVICES'}),
      App.Poll.create({stage: 'stage3', label: Em.I18n.translations['admin.addSecurity.apply.stage3'], isPolling: false, name: 'APPLY_CONFIGURATIONS'}),
      App.Poll.create({stage: 'stage4', label: Em.I18n.translations['admin.addSecurity.apply.stage4'], isPolling: true, name: 'START_SERVICES'})
    ]);
  },

  startStage: function (currentStage) {
    if (this.get('stages').length === this.totalSteps) {
      if (!currentStage) {
        var startedStages = this.get('stages').filterProperty('isStarted', true);
        currentStage = startedStages.findProperty('isCompleted', false);
      }
      if (currentStage && currentStage.get('isPolling') === true) {
        if (currentStage.get('name') === 'START_SERVICES' && !App.router.get('addSecurityController.securityEnabled')) {
          var timeLineServer = App.Service.find('YARN').get('hostComponents').findProperty('componentName', 'APP_TIMELINE_SERVER');
          if (timeLineServer) {
            this.deleteComponents('APP_TIMELINE_SERVER', timeLineServer.get('host.hostName'));
          }
        }
        currentStage.set('isStarted', true);
        currentStage.start();
      } else if (currentStage && currentStage.get('stage') === 'stage3') {
        currentStage.set('isStarted', true);
        if (App.testMode) {
          currentStage.set('isError', false);
          currentStage.set('isSuccess', true);
        } else {
          this.loadClusterConfigs();
        }
      }
    }
  },

  onCompleteStage: function () {
    if (this.get('stages').length === this.totalSteps) {
      var index = this.get('stages').filterProperty('isSuccess', true).length;
      if (index > 0) {
        var lastCompletedStageResult = this.get('stages').objectAt(index - 1).get('isSuccess');
        if (lastCompletedStageResult) {
          var nextStage = this.get('stages').objectAt(index);
          this.moveToNextStage(nextStage);
        }
      }
    }
  },

  moveToNextStage: function (nextStage) {
    if (!nextStage) {
      nextStage = this.get('stages').findProperty('isStarted', false);
    }
    if (nextStage) {
      this.startStage(nextStage);
    }
  },

  addInfoToStages: function () {
    this.addInfoToStage2();
    this.addInfoToStage4();
  },


  addInfoToStage2: function () {
    var stage2 = this.get('stages').findProperty('stage', 'stage2');
    var url = (App.testMode) ? '/data/wizard/deploy/2_hosts/poll_1.json' : App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/services';
    var data = '{"RequestInfo": {"context" :"' + Em.I18n.t('requestInfo.stopAllServices') + '"}, "Body": {"ServiceInfo": {"state": "INSTALLED"}}}';
    stage2.set('url', url);
    stage2.set('data', data);
  },

  addInfoToStage4: function () {
    var stage4 = this.get('stages').findProperty('stage', 'stage4');
    var url = (App.testMode) ? '/data/wizard/deploy/2_hosts/poll_1.json' : App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/services?params/run_smoke_test=true';
    var data = '{"RequestInfo": {"context": "' + Em.I18n.t('requestInfo.startAllServices') + '"}, "Body": {"ServiceInfo": {"state": "STARTED"}}}';
    stage4.set('url', url);
    stage4.set('data', data);
  },

  loadClusterConfigs: function () {
    App.ajax.send({
      name: 'admin.security.add.cluster_configs',
      sender: this,
      success: 'loadClusterConfigsSuccessCallback',
      error: 'loadClusterConfigsErrorCallback'
    });
  },

  deleteComponents: function(componentName, hostName) {
    App.ajax.send({
      name: 'admin.delete_component',
      sender: this,
      data: {
        componentName: componentName,
        hostName: hostName
      },
      success: 'onDeleteComplete',
      error: 'onDeleteError'
    });
  },

  onDeleteComplete: function () {
    console.warn('APP_TIMELINE_SERVER doesn\'t support security mode. It has been removed from YARN service ');
  },

  onDeleteError: function () {
    console.warn('Error: Can\'t delete APP_TIMELINE_SERVER');
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
    var stage3 = this.get('stages').findProperty('stage', 'stage3');
    if (stage3) {
      stage3.set('isSuccess', false);
      stage3.set('isError', true);
    }
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
    var currentStage = this.get('stages').findProperty('stage', 'stage3');
    currentStage.set('isSuccess', true);
    currentStage.set('isError', false);
  },

  applyConfigurationToClusterErrorCallback: function (request, ajaxOptions, error) {
    var stage3 = this.get('stages').findProperty('stage', 'stage3');
    if (stage3) {
      stage3.set('isSuccess', false);
      stage3.set('isError', true);
    }
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
    var stage3 = this.get('stages').findProperty('stage', 'stage3');
    this.get('serviceConfigTags').forEach(function (_tag) {
      if (!data.items.someProperty('type', _tag.siteName)) {
        console.log("Error: Metadata for secure services (secure_configs.js) is having config tags that are not being retrieved from server");
        if (stage3) {
          stage3.set('isSuccess', false);
          stage3.set('isError', true);
        }
      }
      _tag.configs = data.items.findProperty('type', _tag.siteName).properties;
    }, this);
    if (this.manageSecureConfigs()) {
      this.escapeXMLCharacters(this.get('serviceConfigTags'));
      this.applyConfigurationsToCluster();
    }
  },

  getAllConfigurationsErrorCallback: function (request, ajaxOptions, error) {
    var stage3 = this.get('stages').findProperty('stage', 'stage3');
    if (stage3) {
      stage3.set('isSuccess', false);
      stage3.set('isError', true);
    }
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
        configs[key] = App.config.escapeXMLCharacters(configs[key]);
      }
    }, this);
  },

  saveStagesOnRequestId: function () {
    this.saveStages();
  }.observes('stages.@each.requestId'),

  saveStagesOnCompleted: function () {
    this.saveStages();
  }.observes('stages.@each.isCompleted'),

  saveStages: function () {
    var stages = [];
    if (this.get('stages').length === this.totalSteps) {
      this.get('stages').forEach(function (_stage) {
        var stage = {
          name: _stage.get('name'),
          stage: _stage.get('stage'),
          label: _stage.get('label'),
          isPolling: _stage.get('isPolling'),
          isStarted: _stage.get('isStarted'),
          requestId: _stage.get('requestId'),
          isSuccess: _stage.get('isSuccess'),
          isError: _stage.get('isError'),
          url: _stage.get('url'),
          polledData: _stage.get('polledData'),
          data: _stage.get('data')
        };
        stages.pushObject(stage);
      }, this);
      App.db.setSecurityDeployStages(stages);
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
