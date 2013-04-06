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
  configMapping: require('data/secure_mapping').slice(0),
  secureProperties: require('data/secure_properties').configProperties.slice(0),
  stages: [],
  configs: [],
  noOfWaitingAjaxCalls: 0,
  secureServices: [],
  serviceConfigTags: [],
  globalProperties: [],

  clearStep: function () {
    this.get('stages').clear();
    this.get('secureServices').clear();
    this.get('serviceConfigTags').clear();
  },

  loadStep: function () {
    this.clearStep();
    this.loadStages();
    this.loadSecureServices();
    this.addInfoToStages();
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
    } else {
      this.set('isSubmitDisabled', false);
    }
  },

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
          var self = this;
          window.setTimeout(function () {
            self.loadClusterConfigs();
          }, 12000);
        }
      }
    }
  }.observes('stages.@each.isStarted'),

  onCompleteStage: function () {
    var index = this.get('stages').filterProperty('isCompleted', true).length;
    if (index > 0) {
      var self = this;
      var lastCompletedStageResult = this.get('stages').objectAt(index - 1).get('isSuccess');
      if (lastCompletedStageResult) {
        self.moveToNextStage();
      }
    }
  }.observes('stages.@each.isCompleted'),


  addInfoToStages: function () {
    this.addInfoToStage2();
    this.addInfoToStage3();
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

  addInfoToStage3: function () {

  },

  addInfoToStage4: function () {
    var stage4 = this.get('stages').findProperty('stage', 'stage4');
    var url = (App.testMode) ? '/data/wizard/deploy/2_hosts/poll_1.json' : App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/services';
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
    var self = this;
    var url = App.apiPrefix + '/clusters/' + App.router.getClusterName();
    $.ajax({
      type: 'GET',
      url: url,
      timeout: 10000,
      dataType: 'text',
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        //prepare tags to fetch all configuration for a service
        self.get('secureServices').forEach(function (_secureService) {
          self.setServiceTagNames(_secureService, jsonData.Clusters.desired_configs);
        });
        self.getAllConfigurations();
      },

      error: function (request, ajaxOptions, error) {
        self.get('stages').findProperty('stage', 'stage3').set('isError', true);
        console.log("TRACE: error code status is: " + request.status);
      },

      statusCode: require('data/statusCodes')
    });
  },

  getAllConfigurations: function () {
    var self = this;
    var urlParams = [];
    this.get('serviceConfigTags').forEach(function (_tag) {
      urlParams.push('(type=' + _tag.siteName + '&tag=' + _tag.tagName + ')');
    }, this);
    var url = App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/configurations?' + urlParams.join('|');
    if (urlParams.length > 0) {
      $.ajax({
        type: 'GET',
        url: url,
        async: true,
        timeout: 10000,
        dataType: 'json',
        success: function (data) {
          console.log("TRACE: In success function for the GET getServiceConfigsFromServer call");
          console.log("TRACE: The url is: " + url);
          self.get('serviceConfigTags').forEach(function (_tag) {
            _tag.configs = data.items.findProperty('type', _tag.siteName).properties;
          });
          self.removeSecureConfigs();
          self.applyConfigurationsToCluster();
        },

        error: function (request, ajaxOptions, error) {
          self.get('stages').findProperty('stage', 'stage3').set('isError', true);
          console.log("TRACE: In error function for the getServiceConfigsFromServer call");
          console.log("TRACE: value of the url is: " + url);
          console.log("TRACE: error code status is: " + request.status);
        },

        statusCode: require('data/statusCodes')
      });
    }
  },

  loadSecureServices: function () {
    var secureServices = require('data/secure_configs');
    var installedServices = App.Service.find().mapProperty('serviceName');
    //General (only non service tab) tab is always displayed
    installedServices.forEach(function (_service) {
      var secureService = secureServices.findProperty('serviceName', _service);
      if (secureService) {
        this.get('secureServices').push(secureService);
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
    var self = this;
    var url = App.apiPrefix + '/clusters/' + App.router.getClusterName();
    var clusterData = {
      Clusters: {
        desired_config: data
      }
    };
    $.ajax({
      type: 'PUT',
      url: url,
      async: false,
      dataType: 'text',
      data: JSON.stringify(clusterData),
      timeout: 5000,
      success: function (data) {
        self.set('noOfWaitingAjaxCalls', self.get('noOfWaitingAjaxCalls') - 1);
        if (self.get('noOfWaitingAjaxCalls') == 0) {
          var currentStage = self.get('stages').findProperty('stage', 'stage3');
          currentStage.set('isSuccess', true);
          currentStage.set('isCompleted', true);
        }
      },
      error: function (request, ajaxOptions, error) {
        self.get('stages').findProperty('stage', 'stage3').set('isError', true);
      },
      statusCode: require('data/statusCodes')
    });
  },

  getAllConfigsFromServer: function () {
    this.set('noOfWaitingAjaxCalls', this.get('serviceConfigTags').length - 1);
    this.get('serviceConfigTags').forEach(function (_serviceConfigTags) {
      if (_serviceConfigTags.serviceName !== 'MAPREDUCE' || _serviceConfigTags.siteName !== 'core-site') {   //skip MapReduce core-site configuration
        this.getServiceConfigsFromServer(_serviceConfigTags);
      }
    }, this);
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
        _serviceConfigTags.configs.security_enabled = false;
      } else {
        this.get('configMapping').filterProperty('filename', _serviceConfigTags.siteName + '.xml').forEach(function (_config) {
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
              default:
                delete _serviceConfigTags.configs[configName];
            }
          }
          console.log("Not Deleted" + _config.name);
        }, this);
      }
    }, this);
  }
});
