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
  secureMapping: require('data/secure_mapping'),
  secureProperties: require('data/secure_properties').configProperties,
  stages: [],
  configs: [],
  noOfWaitingAjaxCalls: 0,
  secureServices: [],
  serviceConfigTags: [],
  globalProperties: [],

  isSubmitDisabled: true,
  isBackBtnDisabled: true,

  isOozieSelected: function () {
    return this.get('content.services').someProperty('serviceName', 'OOZIE');
  }.property('content.services'),

  isHiveSelected: function () {
    return this.get('content.services').someProperty('serviceName', 'HIVE');
  }.property('content.services'),

  isNagiosSelected: function () {
    return this.get('content.services').someProperty('serviceName', 'NAGIOS');
  }.property('content.services'),

  isZkSelected: function () {
    return this.get('content.services').someProperty('serviceName', 'ZOOKEEPER');
  }.property('content.services'),

  isWebHcatSelected: function () {
    var installedServices = App.Service.find().mapProperty('serviceName');
    return installedServices.contains('WEBHCAT');
  },

  serviceUsersBinding: 'App.router.mainAdminSecurityController.serviceUsers',
  hasHostPopup: true,
  services: [],
  serviceTimestamp: null,

  isSecurityApplied: function () {
    return this.get('stages').someProperty('stage', 'stage3') && this.get('stages').findProperty('stage', 'stage3').get('isSuccess');
  }.property('stages.@each.isCompleted'),

  clearStep: function () {
    this.get('stages').clear();
    this.set('isSubmitDisabled', true);
    this.set('isBackBtnDisabled', true);
    this.get('serviceConfigTags').clear();
  },

  loadStep: function () {
    this.set('secureMapping', require('data/secure_mapping').slice(0));
    this.clearStep();
    var stages = App.db.getSecurityDeployStages();
    this.prepareSecureConfigs();
    if (stages && stages.length > 0) {
      stages.forEach(function (_stage, index) {
        stages[index] = App.Poll.create(_stage);
      }, this);
      if (stages.someProperty('isError', true)) {
        var failedStages = stages.filterProperty('isError', true);
        failedStages.setEach('isError', false);
        failedStages.setEach('isStarted', false);
      } else if (stages.filterProperty('isStarted', true).someProperty('isCompleted', false)) {
        var runningStage = stages.filterProperty('isStarted', true).findProperty('isCompleted', false);
        runningStage.set('isStarted', false);
      }
      this.get('stages').pushObjects(stages);
    } else {
      this.loadStages();
      this.addInfoToStages();
      var runningOperations = App.router.get('backgroundOperationsController.services').filterProperty('isRunning');
      var stopAllOperation = runningOperations.findProperty('name', 'Stop All Services');
      var stopStage = this.get('stages').findProperty('name', 'STOP_SERVICES');
      if (stopStage.get('name') === 'STOP_SERVICES' && stopAllOperation) {
        stopStage.set('requestId', stopAllOperation.get('id'));
      }
    }
    this.moveToNextStage();
  },

  enableSubmit: function () {
    if (this.get('stages').someProperty('isError', true) || this.get('stages').everyProperty('isSuccess', true)) {
      this.set('isSubmitDisabled', false);
      if (this.get('stages').someProperty('isError', true)) {
        this.set('isBackBtnDisabled', false);
        App.router.get('addSecurityController').setStepsEnable();
      }
    } else {
      this.set('isSubmitDisabled', true);
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

  loadStages: function () {
    this.get('stages').pushObjects([
      App.Poll.create({stage: 'stage2', label: Em.I18n.translations['admin.addSecurity.apply.stage2'], isPolling: true, name: 'STOP_SERVICES'}),
      App.Poll.create({stage: 'stage3', label: Em.I18n.translations['admin.addSecurity.apply.stage3'], isPolling: false, name: 'APPLY_CONFIGURATIONS'}),
      App.Poll.create({stage: 'stage4', label: Em.I18n.translations['admin.addSecurity.apply.stage4'], isPolling: true, name: 'START_SERVICES'})
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
          App.router.get('mainAdminSecurityController').setAddSecurityWizardStatus(null);
        } else {
          this.loadClusterConfigs()
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

  moveToNextStage: function () {
    var leftStages = this.get('stages').filterProperty('isStarted', false);
    var nextStage = leftStages.findProperty('isCompleted', false);
    if (nextStage) {
      nextStage.set('isStarted', true);
    }
  },

  addInfoToStages: function () {
    this.addInfoToStage2();
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

  loadUiSideConfigs: function () {
    var uiConfig = [];
    var configs = this.get('secureMapping').filterProperty('foreignKey', null);
    configs.forEach(function (_config) {
      var value = this.getGlobConfigValue(_config.templateName, _config.value, _config.name);
      uiConfig.pushObject({
        "id": "site property",
        "name": _config.name,
        "value": value,
        "filename": _config.filename
      });
    }, this);
    var dependentConfig = this.get('secureMapping').filterProperty('foreignKey');
    dependentConfig.forEach(function (_config) {
      if (App.Service.find().mapProperty('serviceName').contains( _config.serviceName)) {
        this.setConfigValue(uiConfig, _config);
        uiConfig.pushObject({
          "id": "site property",
          "name": _config._name || _config.name,
          "value": _config.value,
          "filename": _config.filename
        });
      }
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
      //console.log("The value of template is: " + _express);
      var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
      var globValue = this.get('globalProperties').findProperty('name', templateName[index]);
      if (globValue) {
        console.log('The template value of templateName ' + '[' + index + ']' + ': ' + templateName[index] + ' is: ' + globValue);
        if (value !== null) {   // if the property depends on more than one template name like <templateName[0]>/<templateName[1]> then don't proceed to the next if the prior is null or not found in the global configs
          value = value.replace(_express, globValue.value);
        }
      } else {
        /*
         console.log("ERROR: The variable name is: " + templateName[index]);
         console.log("ERROR: mapped config from secureMapping file has no corresponding variable in " +
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
    var fkValue = config.name.match(/<(foreignKey.*?)>/g);
    if (fkValue) {
      fkValue.forEach(function (_fkValue) {
        var index = parseInt(_fkValue.match(/\[([\d]*)(?=\])/)[1]);
        var globalValue;
        if (uiConfig.someProperty('name', config.foreignKey[index])) {
          globalValue = uiConfig.findProperty('name', config.foreignKey[index]).value;
          config._name = config.name.replace(_fkValue, globalValue);
        } else if (this.get('globalProperties').someProperty('name', config.foreignKey[index])) {
          globalValue = this.get('globalProperties').findProperty('name', config.foreignKey[index]).value;
          config._name = config.name.replace(_fkValue, globalValue);
        }
      }, this);
    }
    //For properties in the configMapping file having foreignKey and templateName properties.

    var templateValue = config.value.match(/<(templateName.*?)>/g);
    if (templateValue) {
      templateValue.forEach(function (_value) {
        var index = parseInt(_value.match(/\[([\d]*)(?=\])/)[1]);
        var globValue = this.get('globalProperties').findProperty('name', config.templateName[index]);
        if (globValue) {
          config.value = config.value.replace(_value, globValue.value);
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
    this.loadStaticGlobal(); //Hack for properties which are declared in config_properties.js and not able to retrieve values declared in secure_properties.js
    this.loadUsersToGlobal();
    this.loadHostNamesToGlobal();
    this.loadPrimaryNamesToGlobals();
  },

  loadUsersToGlobal: function () {
    if (!this.get('serviceUsers').length) {
      this.loadUsersFromServer();
    }
    App.router.get('mainAdminSecurityController.serviceUsers').forEach(function (_user) {
      this.get('globalProperties').pushObject(_user);
    }, this);
  },

  loadHostNamesToGlobal: function () {
    var oozieHostComponent = App.Service.find('OOZIE').get('hostComponents').findProperty('componentName', 'OOZIE_SERVER');
    if (this.get('isOozieSelected') && oozieHostComponent) {
      var oozieHostName = oozieHostComponent.get('host.hostName');
      this.get('globalProperties').pushObject({
        id: 'puppet var',
        name: 'oozieserver_host',
        value: oozieHostName
      });
    }
    var hiveHostComponent = App.Service.find('HIVE').get('hostComponents').findProperty('componentName', 'HIVE_METASTORE');
    if (this.get('isHiveSelected') && hiveHostComponent) {
      var hiveHostName = hiveHostComponent.get('host.hostName');
      this.get('globalProperties').pushObject({
        id: 'puppet var',
        name: 'hivemetastore_host',
        value: hiveHostName
      });
    }
  },

  loadStaticGlobal: function () {
    var globalProperties = this.get('globalProperties');
    this.get('globalProperties').forEach(function (_property) {
      switch (_property.name) {
        case 'security_enabled':
          _property.value = 'true';
          break;
        case 'dfs_datanode_address':
          _property.value = '1019';
          break;
        case 'dfs_datanode_http_address':
          _property.value = '1022';
          break;
      }
    }, this);
  },

  loadPrimaryNamesToGlobals: function () {
    var principalProperties = this.getPrincipalNames();
    principalProperties.forEach(function (_principalProperty) {
      var name = _principalProperty.name.replace('principal', 'primary');
      var value = _principalProperty.value.split('/')[0];
      this.get('globalProperties').pushObject({name: name, value: value});
    }, this);
  },

  getPrincipalNames: function () {
    var principalNames = [];
    var allPrincipalNames = [];
    this.get('globalProperties').forEach(function (_globalProperty) {
      if (/principal_name?$/.test(_globalProperty.name)) {
        principalNames.pushObject(_globalProperty);
      }
    }, this);
    this.get('secureProperties').forEach(function (_secureProperty) {
      if (/principal_name?$/.test(_secureProperty.name)) {
        var principalName = principalNames.findProperty('name', _secureProperty.name);
        if (!principalName) {
          _secureProperty.value = _secureProperty.defaultValue;
          principalNames.pushObject(_secureProperty);
        }
      }
    }, this);
    return principalNames;
  },

  loadUsersFromServer: function () {
    if (App.testMode) {
      var serviceUsers = this.get('serviceUsers');
      serviceUsers.pushObject({id: 'puppet var', name: 'hdfs_user', value: 'hdfs'});
      serviceUsers.pushObject({id: 'puppet var', name: 'mapred_user', value: 'mapred'});
      serviceUsers.pushObject({id: 'puppet var', name: 'hbase_user', value: 'hbase'});
      serviceUsers.pushObject({id: 'puppet var', name: 'hive_user', value: 'hive'});
    } else {
      App.router.get('mainAdminSecurityController').setSecurityStatus();
    }
  },


  loadClusterConfigs: function () {
    var self = this;
    var url = App.apiPrefix + '/clusters/' + App.router.getClusterName();

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
    this.get('content.services').forEach(function (_secureService) {
      self.setServiceTagNames(_secureService, data.Clusters.desired_configs);
    });
    this.getAllConfigurations();
  },

  loadClusterConfigsErrorCallback: function (request, ajaxOptions, error) {
    this.get('stages').findProperty('stage', 'stage3').set('isError', true);
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

  /**
   * gets site config properties from server and sets it for every configuration
   * @param serviceConfigTags
   */

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
    this.addSecureConfigs();
    this.applyConfigurationsToCluster();
  },

  getAllConfigurationsErrorCallback: function (request, ajaxOptions, error) {
    this.get('stages').findProperty('stage', 'stage3').set('isError', true);
    console.log("TRACE: In error function for the getServiceConfigsFromServer call");
    console.log("TRACE: error code status is: " + request.status);
  },

  addSecureConfigs: function () {
    this.get('serviceConfigTags').forEach(function (_serviceConfigTags) {
      _serviceConfigTags.newTagName = 'version' + (new Date).getTime();
      if (_serviceConfigTags.siteName === 'global') {
        var realmName = this.get('globalProperties').findProperty('name', 'kerberos_domain');
        if (this.get('isNagiosSelected')) {
          var nagiosPrincipalName = this.get('globalProperties').findProperty('name', 'nagios_principal_name');
          nagiosPrincipalName.value = nagiosPrincipalName.value + '@' + realmName.value;
        }
        if (this.get('isZkSelected')) {
          var zkPrincipalName = this.get('globalProperties').findProperty('name', 'zookeeper_principal_name');
          zkPrincipalName.value = zkPrincipalName.value + '@' + realmName.value;
        }
        this.get('globalProperties').forEach(function (_globalProperty) {
          _serviceConfigTags.configs[_globalProperty.name] = _globalProperty.value;
        }, this);
      }
      else {
        this.get('configs').filterProperty('id', 'site property').filterProperty('filename', _serviceConfigTags.siteName + '.xml').forEach(function (_config) {
          _serviceConfigTags.configs[_config.name] = _config.value;
        }, this);
      }
    }, this);
  },

  saveStages: function () {
    var stages = [];
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
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('clusterName'),
      clusterState: 'ADD_SECURITY_STEP_3',
      wizardControllerName: App.router.get('addSecurityController.name'),
      localdb: App.db.data
    });
  }.observes('stages.@each.requestId', 'stages.@each.isStarted', 'stages.@each.isCompleted')
});
