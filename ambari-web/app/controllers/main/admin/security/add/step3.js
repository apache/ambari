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
App.MainAdminSecurityAddStep3Controller = Em.Controller.extend({

  name: 'mainAdminSecurityAddStep3Controller',
  configMapping: require('data/secure_mapping'),
  stages: [],
  configs: [],
  noOfWaitingAjaxCalls: 0,
  secureServices: [],
  serviceConfigTags: [],
  globalProperties: [],

  clearStep: function () {
    this.get('stages').clear();
  },

  loadStep: function () {
    this.clearStep();
    this.loadStages();
    this.addInfoToStages();
    this.prepareSecureConfigs();
    // this.populateSuccededStages();
    this.moveToNextStage();
  },


  loadStages: function () {
    this.get('stages').pushObjects([
      App.Poll.create({stage: 'stage2', label: Em.I18n.translations['admin.addSecurity.apply.stage2'], isPolling: true}),
      App.Poll.create({stage: 'stage3', label: Em.I18n.translations['admin.addSecurity.apply.stage3'], isPolling: false}),
      App.Poll.create({stage: 'stage4', label: Em.I18n.translations['admin.addSecurity.apply.stage4'], isPolling: true})
    ]);
  },

  populateSuccededStages: function () {
    var currentStage = 'stage' + this.get('content').loadCurrentStage();
    var inc = 1;
    while (inc < currentStage) {
      var stage = 'stage' + inc;
      this.get('stages').findProperty('stage', stage).setProperties({ isStarted: true, isCompleted: true });
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
          this.loadConfigsForAllServices();
        }
      }
    }
  }.observes('stages.@each.isStarted'),

  onCompleteStage: function () {
    var index = this.get('stages').filterProperty('isSuccess', true).length;
    if (index > 0) {
      this.moveToNextStage();
    }
  }.observes('stages.@each.isSuccess'),

  addInfoToStages: function () {
    // this.addInfoToStage1();
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

  loadUiSideConfigs: function () {
    var uiConfig = [];
    var configs = this.get('configMapping').filterProperty('foreignKey', null);
    configs.forEach(function (_config) {
      var value = this.getGlobConfigValue(_config.templateName, _config.value, _config.name);
      uiConfig.pushObject({
        "id": "site property",
        "name": _config.name,
        "value": value,
        "filename": _config.filename
      });
    }, this);
    var dependentConfig = this.get('configMapping').filterProperty('foreignKey');
    dependentConfig.forEach(function (_config) {
      this.setConfigValue(uiConfig, _config);
      uiConfig.pushObject({
        "id": "site property",
        "name": _config.name,
        "value": _config.value,
        "filename": _config.filename
      });
    }, this);
    return uiConfig;
  },


  /**
   * Set all site property that are derived from other puppet-variable
   */

  getGlobConfigValue: function (templateName, expression, name) {
    var express = expression.match(/<(.*?)>/g);
    var value = expression;
    if (express == null) {
      return expression;
    }
    express.forEach(function (_express) {
      console.log("The value of template is: " + _express);
      if (_express.match(/\[([\d]*)(?=\])/ === null)) {
      }
      var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
      if (this.get('globalProperties').someProperty('name', templateName[index])) {
        //console.log("The name of the variable is: " + this.get('content.serviceConfigProperties').findProperty('name', templateName[index]).name);
        var globValue = this.get('globalProperties').findProperty('name', templateName[index]).value;
        if (value !== null) {   // if the property depends on more than one template name like <templateName[0]>/<templateName[1]> then don't proceed to the next if the prior is null or not found in the global configs
          value = value.replace(_express, globValue);
        }
      } else {
        /*
         console.log("ERROR: The variable name is: " + templateName[index]);
         console.log("ERROR: mapped config from configMapping file has no corresponding variable in " +
         "content.serviceConfigProperties. Two possible reasons for the error could be: 1) The service is not selected. " +
         "and/OR 2) The service_config metadata file has no corresponding global var for the site property variable");
         */
        value = null;
      }
    }, this);
    return value;
  },
  /**
   * Set all site property that are derived from other site-properties
   */
  setConfigValue: function (uiConfig, config) {
    if (config.value == null) {
      return;
    }
    var fkValue = config.value.match(/<(foreignKey.*?)>/g);
    if (fkValue) {
      fkValue.forEach(function (_fkValue) {
        var index = parseInt(_fkValue.match(/\[([\d]*)(?=\])/)[1]);
        if (uiConfig.someProperty('name', config.foreignKey[index])) {
          var globalValue = uiConfig.findProperty('name', config.foreignKey[index]).value;
          config.value = config.value.replace(_fkValue, globalValue);
        } else if (this.get('content.serviceConfigProperties').someProperty('name', config.foreignKey[index])) {
          var globalValue;
          if (this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).value === '') {
            globalValue = this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).defaultValue;
          } else {
            globalValue = this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).value;
          }
          config.value = config.value.replace(_fkValue, globalValue);
        }
      }, this);
    }
    if (fkValue = config.name.match(/<(foreignKey.*?)>/g)) {
      fkValue.forEach(function (_fkValue) {
        var index = parseInt(_fkValue.match(/\[([\d]*)(?=\])/)[1]);
        if (uiConfig.someProperty('name', config.foreignKey[index])) {
          var globalValue = uiConfig.findProperty('name', config.foreignKey[index]).value;
          config.name = config.name.replace(_fkValue, globalValue);
        } else if (this.get('content.serviceConfigProperties').someProperty('name', config.foreignKey[index])) {
          var globalValue;
          if (this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).value === '') {
            globalValue = this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).defaultValue;
          } else {
            globalValue = this.get('content.serviceConfigProperties').findProperty('name', config.foreignKey[index]).value;
          }
          config.name = config.name.replace(_fkValue, globalValue);
        }
      }, this);
    }
    //For properties in the configMapping file having foreignKey and templateName properties.

    var templateValue = config.value.match(/<(templateName.*?)>/g);
    if (templateValue) {
      templateValue.forEach(function (_value) {
        var index = parseInt(_value.match(/\[([\d]*)(?=\])/)[1]);
        if (this.get('globalProperties').someProperty('name', config.templateName[index])) {
          var globalValue = this.get('globalProperties').findProperty('name', config.templateName[index]).value;
          config.value = config.value.replace(_value, globalValue);
        } else {
          config.value = null;
        }
      }, this);
    }
  },


  prepareSecureConfigs: function () {
    this.loadGlobals();
    this.loadInstanceName();
    var storedConfigs = this.get('content.serviceConfigProperties').filterProperty('id', 'site property');
    var uiConfigs = this.loadUiSideConfigs();
    this.set('configs', storedConfigs.concat(uiConfigs));
  },

  loadGlobals: function () {
    var globals = this.get('content.serviceConfigProperties').filterProperty('id', 'puppet var');
    this.set('globalProperties', globals);
  },

  loadInstanceName: function () {
    var isInstanceName = this.get('globalProperties').findProperty('name', 'instance_name');
    if (isInstanceName) {
      this.get('globalProperties').forEach(function (_globalProperty) {
        if (/primary_name?$/.test(_globalProperty.name)) {
          if (!/_HOST?$/.test(_globalProperty.value)) {
            _globalProperty.value = _globalProperty.value + "/_HOST";
          }
        }
      }, this);
    }
  },

  loadConfigsForAllServices: function () {
    this.set('noOfWaitingAjaxCalls', this.get('content.services').length - 2);
    this.get('content.services').forEach(function (_secureService, index) {
      if (_secureService.serviceName !== 'GENERAL' && _secureService.serviceName !== 'NAGIOS') {
        this.getConfigDetailsFromServer(_secureService, index);
      }
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
    this.set('noOfWaitingAjaxCalls', this.get('content.services').length-2);
    this.get('content.services').forEach(function (_service) {
      if (_service.serviceName !== 'GENERAL' && _service.serviceName !== 'NAGIOS') {
        var data = {config: {}};
        this.get('serviceConfigTags').filterProperty('serviceName', _service.serviceName).forEach(function (_serviceConfig) {
          data.config[_serviceConfig.siteName] = _serviceConfig.newTagName;
        }, this);
        this.applyConfToService(_service.serviceName, data);
      }
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
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: In success function for the applyCreatedConfToService call");
        console.log("TRACE: value of the url is: " + url);
        self.set('noOfWaitingAjaxCalls', self.get('noOfWaitingAjaxCalls') - 1);
        if (self.get('noOfWaitingAjaxCalls') == 0) {
          var currentStage = self.get('stages').findProperty('stage', 'stage3');
          currentStage.set('isSuccess', true);
          currentStage.set('isCompleted', true);
        }
      },

      error: function (request, ajaxOptions, error) {
        self.get('stages').findProperty('stage', 'stage3').set('isError', true);
        console.log('Error: In Error of apply');
        console.log('Error: Error message is: ' + request.responseText);
      },

      statusCode: require('data/statusCodes')
    });
    console.log("Exiting applyCreatedConfToService");

  },

  moveToNextStage: function () {
    var nextStage = this.get('stages').findProperty('isStarted', false);
    if (nextStage) {
      // this.get('content').saveCurrentStage(nextStage.get('stage').charAt(nextStage.get('stage').length - 1));
      nextStage.set('isStarted', true);
    } else {
      this.set('isSubmitDisabled', false);
    }
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
          self.addSecureConfigs();
          self.createConfigurations();
        }
      },

      error: function (request, ajaxOptions, error) {
        self.get('stages').findProperty('stage', 'stage3').set('isError', true);
        console.log("TRACE: In error function for the getServiceConfigsFromServer call");
        console.log("TRACE: value of the url is: " + url);
        console.log("TRACE: error code status is: " + request.status);

      },

      statusCode: require('data/statusCodes')
    });
  },


  addSecureConfigs: function () {
    this.get('serviceConfigTags').forEach(function (_serviceConfigTags, index) {
      if (_serviceConfigTags.siteName === 'global') {
        _serviceConfigTags.newTagName = 'version' + (new Date).getTime() + index;
        this.get('globalProperties').forEach(function (_globalProperty) {
          _serviceConfigTags.configs[_globalProperty.name] = _globalProperty.value;
        }, this);
      } else {
        _serviceConfigTags.newTagName = 'version' + (new Date).getTime();
        this.get('configs').filterProperty('id', 'site property').filterProperty('filename', _serviceConfigTags.siteName + '.xml').forEach(function (_config) {
          _serviceConfigTags.configs[_config.name] = _config.value;
        }, this);
      }
    }, this);
  }
});
