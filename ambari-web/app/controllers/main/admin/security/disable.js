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
          this.loadConfigsForAllServices();
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
    var data = '{"ServiceInfo": {"state": "INSTALLED"}}';
    stage2.set('url', url);
    stage2.set('data', data);
  },

  addInfoToStage3: function () {

  },

  addInfoToStage4: function () {
    var stage4 = this.get('stages').findProperty('stage', 'stage4');
    var url = (App.testMode) ? '/data/wizard/deploy/2_hosts/poll_1.json' : App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/services';
    var data = '{"ServiceInfo": {"state": "STARTED"}}';
    stage4.set('url', url);
    stage4.set('data', data);
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

  /**
   * gets site config properties from server and sets it for every configuration
   * @param serviceConfigTags
   */

  getServiceConfigsFromServer: function (serviceConfigTags) {
    var self = this;
    var properties = {};
    var url = App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/configurations/?type=' + serviceConfigTags.siteName + '&tag=' + serviceConfigTags.tagName;
    $.ajax({
      type: 'GET',
      url: url,
      async: true,
      timeout: 10000,
      dataType: 'json',
      success: function (data) {
        console.log("TRACE: In success function for the GET getServiceConfigsFromServer call");
        console.log("TRACE: The url is: " + url);
        serviceConfigTags.configs = data.items.findProperty('tag', serviceConfigTags.tagName).properties;
        self.set('noOfWaitingAjaxCalls', self.get('noOfWaitingAjaxCalls') - 1);

        if (self.get('noOfWaitingAjaxCalls') == 0) {
          self.removeSecureConfigs();
          self.createConfigurations();
        }
      },

      error: function (request, ajaxOptions, error) {
        self.get('stages').findProperty('stage', 'stage3').set('isError', true);
      },

      statusCode: require('data/statusCodes')
    });
  },
  loadConfigsForAllServices: function () {
    this.set('noOfWaitingAjaxCalls', this.get('secureServices').length);
    this.get('secureServices').forEach(function (_secureService, index) {
      this.getConfigDetailsFromServer(_secureService, index);
    }, this);
  },

  getConfigDetailsFromServer: function (secureService, id) {
    var self = this;
    var url = App.apiPrefix + '/clusters/' + App.router.getClusterName() + '/services/' + secureService.serviceName;
    $.ajax({
      type: 'GET',
      url: url,
      timeout: 10000,
      dataType: 'text',
      success: function (data) {
        console.log("TRACE: In success function for the GET getServciceConfigs call");
        console.log("TRACE: The url is: " + url);
        var jsonData = jQuery.parseJSON(data);

        //prepare tags to fetch all configuration for a service
        self.setServiceTagNames(secureService.serviceName, jsonData.ServiceInfo.desired_configs);
        self.set('noOfWaitingAjaxCalls', self.get('noOfWaitingAjaxCalls') - 1);

        if (self.get('noOfWaitingAjaxCalls') == 0) {
          self.getAllConfigsFromServer();
        }
      },

      error: function (request, ajaxOptions, error) {
        self.get('stages').findProperty('stage', 'stage3').set('isError', true);
        console.log("TRACE: In error function for the getServciceConfigs call");
        console.log("TRACE: value of the url is: " + url);
        console.log("TRACE: error code status is: " + request.status);
      },

      statusCode: require('data/statusCodes')
    });
  },

  /**
   * set tagnames for configuration of the *-site.xml
   */
  setServiceTagNames: function (secureServiceName, configs) {
    console.log("TRACE: In setServiceTagNames function:");
    //var serviceConfigTags = this.get('serviceConfigTags');
    for (var index in configs) {
      var serviceConfigObj = {
        serviceName: secureServiceName,
        siteName: index,
        tagName: configs[index],
        newTagName: null,
        configs: {}
      };
      console.log("The value of serviceConfigTags[index]: " + configs[index]);
      this.get('serviceConfigTags').pushObject(serviceConfigObj);
    }
    return serviceConfigObj;
  },


  getAllConfigsFromServer: function () {
    this.set('noOfWaitingAjaxCalls', this.get('serviceConfigTags').length - 1);
    this.get('serviceConfigTags').forEach(function (_serviceConfigTags) {
      if (_serviceConfigTags.serviceName !== 'MAPREDUCE' || _serviceConfigTags.siteName !== 'core-site') {   //skip MapReduce core-site configuration
        this.getServiceConfigsFromServer(_serviceConfigTags);
      }
    }, this);
  },


  createConfigurations: function () {
    this.set('noOfWaitingAjaxCalls', this.get('serviceConfigTags').length - 1);
    this.get('serviceConfigTags').forEach(function (_serviceConfigTags) {
      if (_serviceConfigTags.serviceName !== 'MAPREDUCE' || _serviceConfigTags.siteName !== 'core-site') {   //skip MapReduce core-site configuration
        this.createConfiguration(_serviceConfigTags);
      }
    }, this);
  },

  createConfiguration: function (serviceConfigTags) {
    var self = this;
    var clusterName = App.router.getClusterName();
    var url = App.apiPrefix + '/clusters/' + clusterName + '/configurations';
    var data = this.createConfigurationData(serviceConfigTags);
    $.ajax({
      type: 'POST',
      url: url,
      data: JSON.stringify(data),
      dataType: 'text',
      timeout: 5000,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        self.set('noOfWaitingAjaxCalls', self.get('noOfWaitingAjaxCalls') - 1);
        if (self.get('noOfWaitingAjaxCalls') == 0) {
          self.applyConfigurationToServices();
        }
      },

      error: function (request, ajaxOptions, error) {
        self.get('stages').findProperty('stage', 'stage3').set('isError', true);
        console.log('TRACE: In Error ');
        console.log('TRACE: Error message is: ' + request.responseText);
        console.log("TRACE: value of the url is: " + url);
      },

      statusCode: require('data/statusCodes')
    });

  },

  createConfigurationData: function (serviceConfigTags) {
    return {"type": serviceConfigTags.siteName, "tag": serviceConfigTags.newTagName, "properties": serviceConfigTags.configs};
  },

  applyConfigurationToServices: function () {
    this.applyHdfsCoretoMaprCore();
    this.set('noOfWaitingAjaxCalls', this.get('secureServices').length);
    this.get('secureServices').forEach(function (_service) {
      var data = {config: {}};
      this.get('serviceConfigTags').filterProperty('serviceName', _service.serviceName).forEach(function (_serviceConfig) {
        data.config[_serviceConfig.siteName] = _serviceConfig.newTagName;
      }, this);
      this.applyConfToService(_service.serviceName, data);

    }, this);
  },

  applyHdfsCoretoMaprCore: function () {
    this.get('serviceConfigTags').filterProperty('serviceName', 'MAPREDUCE').findProperty('siteName', 'core-site').newTagName = this.get('serviceConfigTags').filterProperty('serviceName', 'HDFS').findProperty('siteName', 'core-site').newTagName;
  },

  applyConfToService: function (serviceName, data) {
    var self = this;
    var clusterName = App.router.getClusterName();
    var url = App.apiPrefix + '/clusters/' + clusterName + '/services/' + serviceName;
    $.ajax({
      type: 'PUT',
      url: url,
      async: false,
      dataType: 'text',
      data: JSON.stringify(data),
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
    console.log("Exiting applyCreatedConfToService");

  },

  moveToNextStage: function () {
    var nextStage = this.get('stages').findProperty('isStarted', false);
    if (nextStage) {
      nextStage.set('isStarted', true);
    } else {
      this.set('isSubmitDisabled', false);
    }
  },

  removeSecureConfigs: function () {
    this.get('serviceConfigTags').forEach(function (_serviceConfigTags, index) {
      if (_serviceConfigTags.siteName === 'global') {
        _serviceConfigTags.newTagName = 'version' + (new Date).getTime() + index;
        this.get('secureProperties').forEach(function (_config) {
          if (_config.name in _serviceConfigTags.configs) {
            delete _serviceConfigTags.configs[_config.name];
          }
        }, this);
        _serviceConfigTags.configs.security_enabled = false;
      } else {
        _serviceConfigTags.newTagName = 'version' + (new Date).getTime();
        this.get('configMapping').filterProperty('filename', _serviceConfigTags.siteName + '.xml').forEach(function (_config) {
          if (_config.name in _serviceConfigTags.configs) {
            if (_config.name === 'dfs.datanode.address') {
              _serviceConfigTags.configs[_config.name] = '0.0.0.0:50010';
            } else if (_config.name === 'dfs.datanode.http.address') {
              _serviceConfigTags.configs[_config.name] = '0.0.0.0:50075';
            } else {
              delete _serviceConfigTags.configs[_config.name];
            }
          }
          console.log("Not Deleted" + _config.name);
        }, this);
      }
    }, this);
  }
});
