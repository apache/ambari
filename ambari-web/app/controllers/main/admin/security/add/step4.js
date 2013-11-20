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
App.MainAdminSecurityAddStep4Controller = Em.Controller.extend({

  name: 'mainAdminSecurityAddStep4Controller',
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
  noOfWaitingAjaxCalls: 0,
  secureServices: [],
  serviceUsersBinding: 'App.router.mainAdminSecurityController.serviceUsers',
  serviceConfigTags: [],
  globalProperties: [],
  totalSteps: 3,

  isSubmitDisabled: true,
  isBackBtnDisabled: function () {
    return !this.get('stages').someProperty('isError', true);
  }.property('stages.@each.isCompleted'),

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

  retry: function () {
    var failedStage = this.get('stages').findProperty('isError', true);
    if (failedStage) {
      failedStage.set('isStarted', false);
      failedStage.set('isError', false);
      this.startStage(failedStage);
    }
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
        this.get('stages').pushObjects(stages);
        this.addObserver('stages.@each.isSuccess', this, 'onCompleteStage');
        return;
      } else if (stages.filterProperty('isStarted', true).someProperty('isCompleted', false)) {
        var runningStage = stages.filterProperty('isStarted', true).findProperty('isCompleted', false);
        runningStage.set('isStarted', false);
        this.get('stages').pushObjects(stages);
      } else {
        this.get('stages').pushObjects(stages);
      }
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
    this.addObserver('stages.@each.isSuccess', this, 'onCompleteStage');
    this.moveToNextStage();
  },

  enableSubmit: function () {
    var addSecurityController = App.router.get('addSecurityController');
    if (this.get('stages').someProperty('isError', true) || this.get('stages').everyProperty('isSuccess', true)) {
      this.set('isSubmitDisabled', false);
      if (this.get('stages').someProperty('isError', true)) {
        addSecurityController.setStepsEnable();
      }
    } else {
      this.set('isSubmitDisabled', true);
      addSecurityController.setLowerStepsDisable(4);
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

  startStage: function (currentStage) {
    if (this.get('stages').length === this.totalSteps) {
      if (!currentStage) {
        var startedStages = this.get('stages').filterProperty('isStarted', true);
        currentStage = startedStages.findProperty('isCompleted', false);
      }
      if (currentStage && currentStage.get('isPolling') === true) {
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
      var value = _config.value;
      if (_config.hasOwnProperty('dependedServiceName')) {
        value = this.checkServiceForConfigValue(value, _config.dependedServiceName);
      }
      value = this.getGlobConfigValue(_config.templateName, value, _config.name);
      uiConfig.pushObject({
        "id": "site property",
        "name": _config.name,
        "value": value,
        "filename": _config.filename
      });
    }, this);
    var dependentConfig = this.get('secureMapping').filterProperty('foreignKey');
    dependentConfig.forEach(function (_config) {
      if (App.Service.find().mapProperty('serviceName').contains(_config.serviceName)) {
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


  checkServiceForConfigValue: function (value, serviceNames) {
    serviceNames.forEach(function (_serviceName) {
      if (!App.Service.find().mapProperty('serviceName').contains(_serviceName.name)) {
        value = value.replace(_serviceName.replace, '');
      }
    }, this);

    return value;
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
    this.loadStaticGlobal(); //Hack for properties which are declared in global_properties.js and not able to retrieve values declared in secure_properties.js
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
    var webHcatComponent = App.Service.find('WEBHCAT').get('hostComponents').findProperty('componentName', 'WEBHCAT_SERVER');
    if (this.isWebHcatSelected() && webHcatComponent) {
      var webHcatHostName = webHcatComponent.get('host.hostName');
      this.get('globalProperties').pushObject({
        id: 'puppet var',
        name: 'webhcat_server',
        value: webHcatHostName
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
      App.router.set('mainAdminSecurityController.serviceUsers', App.db.getSecureUserInfo());
    }
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
    this.get('content.services').forEach(function (_secureService) {
      self.setServiceTagNames(_secureService, data.Clusters.desired_configs);
    });
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
        console.log("The value of serviceConfigTags[index]: " + configs[index]);
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
    if (this.addSecureConfigs()) {
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

  addSecureConfigs: function () {
    try {
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
            if (!/_hosts?$/.test(_globalProperty.name)) {
              _serviceConfigTags.configs[_globalProperty.name] = _globalProperty.value;
            }
          }, this);
        }
        else {
          this.get('configs').filterProperty('id', 'site property').filterProperty('filename', _serviceConfigTags.siteName + '.xml').forEach(function (_config) {
            _serviceConfigTags.configs[_config.name] = _config.value;
          }, this);
        }
      }, this);
    } catch (err) {
      var stage3 = this.get('stages').findProperty('stage', 'stage3');
      if (stage3) {
        stage3.set('isSuccess', false);
        stage3.set('isError', true);
      }
      if (err) {
        console.log("Error: Error occurred while applying secure configs to the server. Error message: " + err);
      }
      this.onJsError();
      return false;
    }
    return true;
  },

  onJsError: function () {
    App.ModalPopup.show({
      header: Em.I18n.t('common.error'),
      secondary: false,
      onPrimary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile('<p>{{t admin.security.apply.configuration.error}}</p>')
      })
    });
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
