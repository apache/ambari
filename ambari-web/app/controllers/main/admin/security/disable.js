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
App.MainAdminSecurityDisableController = Em.Controller.extend({

  name: 'mainAdminSecurityDisableController',
  secureMapping: require('data/secure_mapping'),
  configMapping: App.config.get('configMapping'),
  secureProperties: require('data/secure_properties').configProperties.slice(0),
  stages: [],
  configs: [],
  noOfWaitingAjaxCalls: 0,
  secureServices: [],
  serviceConfigTags: [],
  globalProperties: [],
  hasHostPopup: true,
  services: [],
  serviceTimestamp: null,
  isSubmitDisabled: true,

  clearStep: function () {
    this.get('stages').clear();
    this.get('secureServices').clear();
    this.get('serviceConfigTags').clear();
  },

  loadStep: function () {
    var stages = App.db.getSecurityDeployStages();
    this.clearStep();
    if (stages === undefined) {
      this.loadStages();
      this.addInfoToStages();
    } else {
      stages.forEach(function (_stage, index) {
        stages[index] = App.Poll.create(_stage);
      }, this);
      if (stages.someProperty('isError', true)) {
        var failedStages = stages.filterProperty('isError', true);
        failedStages.setEach('isError', false);
        failedStages.setEach('isStarted', false);
        failedStages.setEach('isCompleted', false);
      } else if (stages.filterProperty('isStarted', true).someProperty('isCompleted', false)) {
        var runningStage = stages.filterProperty('isStarted', true).findProperty('isCompleted', false);
        runningStage.set('isStarted', false);
      }
      this.get('stages').pushObjects(stages);
    }
    this.loadSecureServices();
    this.moveToNextStage();
  },


  loadStages: function () {
    this.get('stages').pushObjects([
      App.Poll.create({stage: 'stage2', label: Em.I18n.translations['admin.addSecurity.apply.stage2'], isPolling: true}),
      App.Poll.create({stage: 'stage3', label: Em.I18n.translations['admin.addSecurity.apply.stage3'], isPolling: false}),
      App.Poll.create({stage: 'stage4', label: Em.I18n.translations['admin.addSecurity.apply.stage4'], isPolling: true})
    ]);
  },


  moveToNextStage: function () {
    var nextStage = this.get('stages').findProperty('isStarted', false);
    if (nextStage) {
      nextStage.set('isStarted', true);
    }
  },

  enableSubmit: function () {
    if (this.get('stages').someProperty('isError', true) || this.get('stages').everyProperty('isSuccess', true)) {
      this.set('isSubmitDisabled', false);
    }
  }.observes('stages.@each.isCompleted'),

  startStage: function () {
    var startedStages = this.get('stages').filterProperty('isStarted', true);
    if (startedStages.length) {
      var currentStage = startedStages.findProperty('isCompleted', false);
      if (currentStage && currentStage.get('isPolling') === true) {
        currentStage.start();
      } else if (currentStage && currentStage.get('stage') === 'stage3') {
        if (App.testMode) {
          currentStage.set('isSuccess', true);
          currentStage.set('isCompleted', true);
          this.moveToNextStage();
        } else {
          this.loadClusterConfigs();
        }
      }
    }
  }.observes('stages.@each.isStarted'),

  onCompleteStage: function () {
    var index = this.get('stages').filterProperty('isCompleted', true).length;
    if (index > 0) {
      var lastCompletedStageResult = this.get('stages').objectAt(index - 1).get('isSuccess');
      if (lastCompletedStageResult) {
        this.moveToNextStage();
      }
    }
  }.observes('stages.@each.isCompleted'),

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
    this.set('serviceTimestamp', new Date().getTime());
  }.observes('stages.@each.polledData'),

  addInfoToStages: function () {
    this.addInfoToStage2();
    this.addInfoToStage4();
  },

  addInfoToStage1: function () {
    var stage1 = this.get('stages').findProperty('stage', 'stage1');
    if (App.testMode) {
      stage1.set('isSucces', true);
      stage1.set('isStarted', true);
      stage1.set('isCompleted', true);
    }
  },

  addInfoToStage2: function () {
    var stage2 = this.get('stages').findProperty('stage', 'stage2');
    var url = (App.testMode) ? '/data/wizard/deploy/2_hosts/poll_1.json' : App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/services';
    var data = '{"RequestInfo": {"context": "' + Em.I18n.t('requestInfo.stopAllServices') + '"}, "Body": {"ServiceInfo": {"state": "INSTALLED"}}}';
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

  /**
   * set tagnames for configuration of the *-site.xml
   */
  setServiceTagNames: function (secureService, configs) {
    for (var index in configs) {
      if (secureService.sites && secureService.sites.contains(index)) {
        var serviceConfigObj = {
          siteName: index,
          tagName: configs[index].tag,
          newTagName: null,
          configs: {}
        };
        console.log("The value of serviceConfigTags[index]: " + configs[index]);
        this.get('serviceConfigTags').pushObject(serviceConfigObj);
      }
    }
    return serviceConfigObj;
  },

  loadClusterConfigs: function () {
    App.ajax.send({
      name: 'admin.security.cluster_configs',
      sender: this,
      success: 'loadClusterConfigsSuccessCallback',
      error: 'loadClusterConfigsErrorCallback'
    });
  },

  loadClusterConfigsSuccessCallback: function (jsonData) {
    //prepare tags to fetch all configuration for a service
    this.get('secureServices').forEach(function (_secureService) {
      this.setServiceTagNames(_secureService, jsonData.Clusters.desired_configs);
    },this);
    this.getAllConfigurations();
  },

  loadClusterConfigsErrorCallback: function (request, ajaxOptions, error) {
    this.get('stages').findProperty('stage', 'stage3').set('isError', true);
    console.log("TRACE: error code status is: " + request.status);
  },

  getAllConfigurations: function () {
    var urlParams = [];
    this.get('serviceConfigTags').forEach(function (_tag) {
      urlParams.push('(type=' + _tag.siteName + '&tag=' + _tag.tagName + ')');
    }, this);
    if (urlParams.length > 0) {
      App.ajax.send({
        name: 'admin.security.all_configurations',
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
    this.get('serviceConfigTags').forEach(function (_tag) {
      if (!data.items.someProperty('type', _tag.siteName)) {
        console.log("Error: Metadata for secure services (secure_configs.js) is having config tags that are not being retrieved from server");
        this.get('stages').findProperty('stage', 'stage3').set('isError', true);
      }
      _tag.configs = data.items.findProperty('type', _tag.siteName).properties;
    }, this);
    this.removeSecureConfigs();
    this.applyConfigurationsToCluster();
  },

  getAllConfigurationsErrorCallback: function (request, ajaxOptions, error) {
    this.get('stages').findProperty('stage', 'stage3').set('isError', true);
    console.log("TRACE: In error function for the getServiceConfigsFromServer call");
    console.log("TRACE: error code status is: " + request.status);
  },


  loadSecureServices: function () {
    var secureServices = require('data/secure_configs');
    var installedServices = App.Service.find().mapProperty('serviceName');
    this.get('secureServices').pushObject(secureServices.findProperty('serviceName', 'GENERAL'));
    //General (only non service tab) tab is always displayed
    installedServices.forEach(function (_service) {
      var secureService = secureServices.findProperty('serviceName', _service);
      if (secureService) {
        this.get('secureServices').pushObject(secureService);
      }
    }, this);
  },

  applyConfigurationsToCluster: function () {
    this.set('noOfWaitingAjaxCalls', this.get('serviceConfigTags').length);
    this.get('serviceConfigTags').forEach(function (_serviceConfig) {
      this.applyConfigurationToCluster({type: _serviceConfig.siteName, tag: _serviceConfig.newTagName, properties: _serviceConfig.configs});
    }, this);
  },

  applyConfigurationToCluster: function (data) {
    var clusterData = {
      Clusters: {
        desired_config: data
      }
    };
    App.ajax.send({
      name: 'admin.security.apply_configuration',
      sender: this,
      data: {
        clusterData: clusterData
      },
      success: 'applyConfigurationToClusterSuccessCallback',
      error: 'applyConfigurationToClusterErrorCallback'
    });
  },

  applyConfigurationToClusterSuccessCallback: function (data) {
    this.set('noOfWaitingAjaxCalls', this.get('noOfWaitingAjaxCalls') - 1);
    if (this.get('noOfWaitingAjaxCalls') == 0) {
      var currentStage = this.get('stages').findProperty('stage', 'stage3');
      currentStage.set('isSuccess', true);
    }
  },

  applyConfigurationToClusterErrorCallback: function (request, ajaxOptions, error) {
    this.get('stages').findProperty('stage', 'stage3').set('isError', true);
  },


  removeSecureConfigs: function () {
    this.get('serviceConfigTags').forEach(function (_serviceConfigTags, index) {
      _serviceConfigTags.newTagName = 'version' + (new Date).getTime();
      if (_serviceConfigTags.siteName === 'global') {
        this.get('secureProperties').forEach(function (_config) {
          if (_config.name in _serviceConfigTags.configs) {
            delete _serviceConfigTags.configs[_config.name];
          }
        }, this);
        _serviceConfigTags.configs.security_enabled = 'false';
        _serviceConfigTags.configs.dfs_datanode_address = '50010';
        _serviceConfigTags.configs.dfs_datanode_http_address = '50075';
      } else {
        this.get('secureMapping').filterProperty('filename', _serviceConfigTags.siteName + '.xml').forEach(function (_config) {
          var configName = _config.name;
          if (configName in _serviceConfigTags.configs) {
            switch (configName) {
              case 'dfs.datanode.address':
                _serviceConfigTags.configs[configName] = '0.0.0.0:50010';
                break;
              case 'dfs.datanode.http.address':
                _serviceConfigTags.configs[configName] = '0.0.0.0:50075';
                break;
              case 'hbase.security.authentication':
                _serviceConfigTags.configs[configName] = 'simple';
                break;
              case 'hbase.rpc.engine':
                _serviceConfigTags.configs[configName] = 'org.apache.hadoop.hbase.ipc.WritableRpcEngine';
                break;
              case 'hbase.security.authorization':
                _serviceConfigTags.configs[configName] = 'false';
                break;
              case 'hbase.coprocessor.master.classes':
                _serviceConfigTags.configs[configName] = 'org.apache.hadoop.hbase.security.access.AccessController';
                break;
              case 'zookeeper.znode.parent':
                _serviceConfigTags.configs[configName] = '/hbase-unsecure';
                break;
              default:
                delete _serviceConfigTags.configs[configName];
            }
          }
          console.log("Not Deleted" + _config.name);
        }, this);
      }
    }, this);
  },

  saveStages: function () {
    var stages = [];
    this.get('stages').forEach(function (_stage) {
      var stage = {
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
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('clusterName'),
      clusterState: 'DISABLE_SECURITY',
      wizardControllerName: this.get('name'),
      localdb: App.db.data
    });
  }.observes('stages.@each.requestId', 'stages.@each.isStarted', 'stages.@each.isCompleted')

});
