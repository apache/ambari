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

  isSubmitDisabled: true,

  isOozieSelected: function () {
    return this.get('content.services').someProperty('serviceName', 'OOZIE');
  }.property('content.services'),

  isWebHcatSelected: function () {
    return this.get('content.services').someProperty('serviceName', 'WEBHCAT');
  }.property('content.services'),

  serviceUsersBinding: 'App.router.mainAdminController.serviceUsers',
  hasHostPopup: true,
  services: [],
  serviceTimestamp: null,

  clearStep: function () {
    this.get('stages').clear();
    this.set('isSubmitDisabled', true);
    this.get('serviceConfigTags').clear();
  },

  loadStep: function () {
    this.clearStep();
    this.loadStages();
    this.addInfoToStages();
    this.prepareSecureConfigs();
    this.moveToNextStage();
  },

  enableSubmit: function () {
    if (this.get('stages').someProperty('isError', true) || this.get('stages').everyProperty('isSuccess', true)) {
      this.set('isSubmitDisabled', false);
      App.router.get('addSecurityController').setStepsEnable();
    }
  }.observes('stages.@each.isCompleted'),

  updateServices: function () {
    this.services.clear();
    var services = this.get("services");
    this.get("stages").forEach(function (stages) {
      var newService = Ember.Object.create({
        name: stages.label,
        hosts: []
      });
      var hostNames = stages.get("polledData").mapProperty('Tasks.host_name').uniq();
      hostNames.forEach(function (name) {
        newService.hosts.push({
          name: name,
          publicName: name,
          logTasks: stages.polledData
        });
      });
      services.push(newService);
    });
    this.set('serviceTimestamp', new Date().getTime());
  }.observes("stages.@each.polledData"),

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
          this.loadClusterConfigs();
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
        window.setTimeout(function () {
          self.moveToNextStage();
        }, 50);
      }
    }
  }.observes('stages.@each.isCompleted'),

  moveToNextStage: function () {
    var nextStage = this.get('stages').findProperty('isStarted', false);
    if (nextStage) {
      nextStage.set('isStarted', true);
    }
  },

  addInfoToStages: function () {
    this.addInfoToStage2();
    this.addInfoToStage3();
    this.addInfoToStage4();
  },

  addInfoToStage1: function () {
    var stage1 = this.get('stages').findProperty('stage', 'stage1');
    if (App.testMode) {
      stage1.set('isSuccess', true);
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

  appendInstanceName: function (name, property) {
    var newValue;
    if (this.get('globalProperties').someProperty('name', name)) {
      var globalProperty = this.get('globalProperties').findProperty('name', name);
      newValue = globalProperty.value;
      var isInstanceName = this.get('globalProperties').findProperty('name', 'instance_name');
      if (isInstanceName) {
        if (/primary_name?$/.test(globalProperty.name) && property !== 'hadoop.security.auth_to_local' && property !== 'oozie.authentication.kerberos.name.rules') {
          if (this.get('isOozieSelected') && (property === 'oozie.service.HadoopAccessorService.kerberos.principal' || property === 'oozie.authentication.kerberos.principal')) {
            var oozieServerName = App.Service.find('OOZIE').get('hostComponents').findProperty('componentName', 'OOZIE_SERVER').get('host.hostName');
            newValue = newValue + '/' + oozieServerName;
          } else if (this.get('isWebHcatSelected') && property === 'templeton.kerberos.principal') {
            var webHcatName = App.Service.find('WEBHCAT').get('hostComponents').findProperty('componentName', 'WEBHCAT_SERVER').get('host.hostName');
            newValue = newValue + '/' + webHcatName;
          } else {
            if (!/_HOST?$/.test(newValue)) {
              newValue = newValue + '/_HOST';
            }
          }
        }
      }
    } else {
      console.log("The template name does not exist in secure_properties file");
      newValue = null;
    }
    return newValue;
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
      //console.log("The value of template is: " + _express);
      var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
      if (this.get('globalProperties').someProperty('name', templateName[index])) {
        //console.log("The name of the variable is: " + this.get('content.serviceConfigProperties').findProperty('name', templateName[index]).name);
        var globValue = this.appendInstanceName(templateName[index], name);
        console.log('The template value of templateName ' + '[' + index + ']' + ': ' + templateName[index] + ' is: ' + globValue);
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
          var globValue = this.appendInstanceName(config.templateName[index]);
          config.value = config.value.replace(_value, globValue);
        } else {
          config.value = null;
        }
      }, this);
    }
  },

  prepareSecureConfigs: function () {
    this.loadGlobals();
    var storedConfigs = this.get('content.serviceConfigProperties').filterProperty('id', 'site property');
    var uiConfigs = this.loadUiSideConfigs();
    this.set('configs', storedConfigs.concat(uiConfigs));
  },

  loadGlobals: function () {
    var globals = this.get('content.serviceConfigProperties').filterProperty('id', 'puppet var');
    this.set('globalProperties', globals);
    this.loadUsersToGlobal();
  },

  loadUsersToGlobal: function () {
    if (!this.get('serviceUsers').length) {
      this.loadUsersFromServer();
    }
    App.router.get('mainAdminController.serviceUsers').forEach(function (_user) {
      this.get('globalProperties').pushObject(_user);
    }, this);
  },

  loadUsersFromServer: function () {
    var self = this;
    if (App.testMode) {
      var serviceUsers = this.get('serviceUsers');
      serviceUsers.pushObject({id: 'puppet var', name: 'hdfs_user', value: 'hdfs'});
      serviceUsers.pushObject({id: 'puppet var', name: 'mapred_user', value: 'mapred'});
      serviceUsers.pushObject({id: 'puppet var', name: 'hbase_user', value: 'hbase'});
      serviceUsers.pushObject({id: 'puppet var', name: 'hive_user', value: 'hive'});
    } else {
      App.router.get('mainAdminController').getSecurityStatusFromServer();
    }
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
        self.get('content.services').forEach(function (_secureService) {
          if (_secureService.serviceName !== 'GENERAL') {
            self.setServiceTagNames(_secureService, jsonData.Clusters.desired_configs);
          }
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


  /**
   * set tagnames for configuration of the *-site.xml
   */
  setServiceTagNames: function (secureService, configs) {
    console.log("TRACE: In setServiceTagNames function:");
    //var serviceConfigTags = this.get('serviceConfigTags');
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


  /**
   * gets site config properties from server and sets it for every configuration
   * @param serviceConfigTags
   */

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
          self.addSecureConfigs();
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

  addSecureConfigs: function () {
    this.get('serviceConfigTags').forEach(function (_serviceConfigTags, index) {
      _serviceConfigTags.newTagName = 'version' + (new Date).getTime();
      if (_serviceConfigTags.siteName === 'global') {
        this.get('globalProperties').forEach(function (_globalProperty) {
          _serviceConfigTags.configs[_globalProperty.name] = _globalProperty.value;
        }, this);
      } else {
        this.get('configs').filterProperty('id', 'site property').filterProperty('filename', _serviceConfigTags.siteName + '.xml').forEach(function (_config) {
          _serviceConfigTags.configs[_config.name] = _config.value;
        }, this);
      }
    }, this);
  }

});
