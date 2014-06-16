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

App.MainAdminSecurityAddStep4Controller = App.MainAdminSecurityProgressController.extend({

  name: 'mainAdminSecurityAddStep4Controller',

  serviceUsersBinding: 'App.router.mainAdminSecurityController.serviceUsers',

  /**
   * component configs which should be added to global
   */
  componentsConfig: [
    {
      serviceName: 'OOZIE',
      componentName: 'OOZIE_SERVER',
      configName: 'oozieserver_host'
    },
    {
      serviceName: 'HIVE',
      componentName: 'HIVE_METASTORE',
      configName: 'hivemetastore_host'
    },
    {
      serviceName: 'WEBHCAT',
      componentName: 'WEBHCAT_SERVER',
      configName: 'webhcat_server'
    }
  ],

  /**
   * mock users used in testMode
   */
  testModeUsers: [
    {
      name: 'hdfs_user',
      value: 'hdfs'
    },
    {
      name: 'mapred_user',
      value: 'mapred'
    },
    {
      name: 'hbase_user',
      value: 'hbase'
    },
    {
      name: 'hive_user',
      value: 'hive'
    }
  ],

  /**
   * security configs, which values should be modified after APPLY CONFIGURATIONS stage
   */
  secureConfigs: [
    {
      name: 'nagios_principal_name',
      serviceName: 'NAGIOS'
    },
    {
      name: 'zookeeper_principal_name',
      serviceName: 'ZOOKEEPER'
    },
    {
      name: 'storm_principal_name',
      serviceName: 'STORM'
    }
  ],

  secureServices: function() {
    return  this.get('content.services');
  }.property('content.services'),

  isBackBtnDisabled: function () {
    return !this.get('commands').someProperty('isError');
  }.property('commands.@each.isCompleted'),

  isSecurityApplied: function () {
    return this.get('commands').someProperty('name', 'START_SERVICES') && this.get('commands').findProperty('name', 'START_SERVICES').get('isSuccess');
  }.property('commands.@each.isCompleted'),

  /**
   * control disabled property of completion button
   */
  enableSubmit: function () {
    var addSecurityController = App.router.get('addSecurityController');
    if (this.get('commands').someProperty('isError') || this.get('commands').everyProperty('isSuccess')) {
      this.set('isSubmitDisabled', false);
      if (this.get('commands').someProperty('isError')) {
        addSecurityController.setStepsEnable();
      }
    } else {
      this.set('isSubmitDisabled', true);
      addSecurityController.setLowerStepsDisable(4);
    }
  }.observes('commands.@each.isCompleted'),

  /**
   * clear step info
   */
  clearStep: function () {
    this.set('commands', []);
    this.set('isSubmitDisabled', true);
    this.get('serviceConfigTags').clear();
  },

  loadCommands: function () {
    this._super();
    // no need to remove ATS component if YARN and ATS are not installed
    if (this.get('secureServices').findProperty('serviceName', 'YARN') && App.Service.find('YARN').get('hostComponents').someProperty('componentName', 'APP_TIMELINE_SERVER')) {
      this.get('commands').splice(2, 0, App.Poll.create({name: 'DELETE_ATS', label: Em.I18n.translations['admin.addSecurity.apply.delete.ats'], isPolling: false}));
    }
  },

  /**
   * load step info
   */
  loadStep: function () {
    this.clearStep();
    this.prepareSecureConfigs();

    if (!this.resumeSavedCommands()) {
      this.loadCommands();
      this.addInfoToCommands();
      this.syncStopServicesOperation();
      this.addObserverToCommands();
      this.moveToNextCommand();
    }
  },

  /**
   * synchronize "STOP_SERVICES" operation from BO with command of step
   * @return {Boolean}
   */
  syncStopServicesOperation: function () {
    var runningOperations = App.router.get('backgroundOperationsController.services').filterProperty('isRunning');
    var stopAllOperation = runningOperations.findProperty('name', 'Stop All Services');
    var stopCommand = this.get('commands').findProperty('name', 'STOP_SERVICES');
    if (stopCommand && stopAllOperation) {
      stopCommand.set('requestId', stopAllOperation.get('id'));
      return true;
    }
    return false;
  },

  /**
   * resume previously saved commands
   * @return {Boolean}
   */
  resumeSavedCommands: function () {
    var commands = App.db.getSecurityDeployCommands();
    if (Em.isNone(commands) || commands.length === 0) return false;

    commands.forEach(function (_command, index) {
      commands[index] = App.Poll.create(_command);
    }, this);
    if (commands.someProperty('isError')) {
      this.get('commands').pushObjects(commands);
      this.addObserverToCommands();
      return true;
    } else if (commands.filterProperty('isStarted').someProperty('isCompleted', false)) {
      var runningCommand = commands.filterProperty('isStarted').findProperty('isCompleted', false);
      runningCommand.set('isStarted', false);
      this.get('commands').pushObjects(commands);
    } else {
      this.get('commands').pushObjects(commands);
    }
    this.addObserverToCommands();
    this.moveToNextCommand();
    return true;
  },

  /**
   * load configs from UI side
   * @return {Array}
   */
  loadUiSideConfigs: function () {
    var uiConfig = [];
    var configs = this.get('secureMapping').filterProperty('foreignKey', null);
    configs.forEach(function (_config) {
      var value = _config.value;
      if (_config.hasOwnProperty('dependedServiceName')) {
        value = this.checkServiceForConfigValue(value, _config.dependedServiceName);
      }
      value = this.getGlobConfigValue(_config.templateName, value);
      uiConfig.push({
        "id": "site property",
        "name": _config.name,
        "value": value,
        "filename": _config.filename
      });
    }, this);
    var dependentConfig = this.get('secureMapping').filterProperty('foreignKey');
    dependentConfig.forEach(function (_config) {
      if (App.Service.find().mapProperty('serviceName').contains(_config.serviceName)) {
        this.setConfigValue(_config);
        this.formatConfigName(uiConfig, _config);
        uiConfig.push({
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
   * erase template rules from config value if service is not loaded
   * @param value
   * @param services
   * @return {*}
   */
  checkServiceForConfigValue: function (value, services) {
    services.forEach(function (_service) {
      if (!App.Service.find(_service.name).get('isLoaded')) {
        value = value.replace(_service.replace, '');
      }
    }, this);
    return value;
  },

  /**
   * Set all site property that are derived from other puppet-variable
   * @param templateName
   * @param expression
   * @return {String|null}
   */
  getGlobConfigValue: function (templateName, expression) {
    var express = expression.match(/<(.*?)>/g);
    var value = expression;
    if (Em.isNone(express)) return expression;

    express.forEach(function (_express) {
      var index = parseInt(_express.match(/\[([\d]*)(?=\])/)[1]);
      var globalConfig = this.get('globalProperties').findProperty('name', templateName[index]);

      if (!!value) {
        value = (globalConfig) ? value.replace(_express, globalConfig.value) : null;
      }
    }, this);
    return value;
  },

  /**
   * format name of config values of global configs which match foreignKey
   * @param uiConfig
   * @param config
   * @return {Boolean}
   */
  formatConfigName: function (uiConfig, config) {
    if (Em.isNone(config.value)) return false;

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
      return true;
    }
    return false;
  },

  /**
   * Set config value with values of global configs which match template
   * @param config
   * @return {Boolean}
   */
  setConfigValue: function (config) {
    if (Em.isNone(config.value)) return false;

    //For properties in the configMapping file having foreignKey and templateName properties.
    var templateValue = config.value.match(/<(templateName.*?)>/g);
    if (templateValue) {
      templateValue.forEach(function (_value) {
        var index = parseInt(_value.match(/\[([\d]*)(?=\])/)[1]);
        var globValue = this.get('globalProperties').findProperty('name', config.templateName[index]);

        config.value = (globValue) ? config.value.replace(_value, globValue.value) : null;
      }, this);
      return true;
    }
    return false;
  },

  /**
   * prepare secure configs
   */
  prepareSecureConfigs: function () {
    this.loadGlobals();
    var storedConfigs = this.get('content.serviceConfigProperties').filterProperty('id', 'site property');
    var uiConfigs = this.loadUiSideConfigs();
    this.set('configs', storedConfigs.concat(uiConfigs));
  },

  /**
   * load global configs
   */
  loadGlobals: function () {
    var globals = this.get('content.serviceConfigProperties').filterProperty('id', 'puppet var');
    this.set('globalProperties', globals);
    this.loadStaticGlobal(); //Hack for properties which are declared in global_properties.js and not able to retrieve values declared in secure_properties.js
    this.loadUsersToGlobal();
    this.loadHostNamesToGlobal();
    this.loadPrimaryNamesToGlobals();
  },

  /**
   * push users to global configs
   */
  loadUsersToGlobal: function () {
    if (!this.get('serviceUsers').length)  {
      this.loadUsersFromServer();
    }
    App.router.get('mainAdminSecurityController.serviceUsers').forEach(function (_user) {
      this.get('globalProperties').pushObject(_user);
    }, this);
  },

  /**
   * add component config that contain host name as value
   * @param serviceName
   * @param componentName
   * @param configName
   * @return {Boolean}
   */
  addHostConfig: function(serviceName, componentName, configName) {
    var service = App.Service.find(serviceName);
    var isServiceSecure = this.get('secureServices').someProperty('serviceName', serviceName);

    if (service.get('isLoaded') && isServiceSecure) {
      var hostComponent = service.get('hostComponents').findProperty('componentName', componentName);
      if (hostComponent) {
        var hostName = hostComponent.get('hostName');
        this.get('globalProperties').push({
          id: 'puppet var',
          name: configName,
          value: hostName
        });
        return true;
      }
    }
    return false;
  },

  /**
   * add hosts' names to global configs
   */
  loadHostNamesToGlobal: function () {
    var componentsConfig = this.get('componentsConfig');
    componentsConfig.forEach(function (host) {
      this.addHostConfig(host.serviceName, host.componentName, host.configName);
    }, this);
  },

  /**
   * load static global
   */
  loadStaticGlobal: function () {
    this.get('globalProperties').forEach(function (_property) {
      switch (_property.name) {
        case 'security_enabled':
          _property.value = 'true';
          break;
      }
    }, this);
  },

  /**
   * add principals to global properties
   */
  loadPrimaryNamesToGlobals: function () {
    var principalProperties = this.getPrincipalNames();
    principalProperties.forEach(function (_principalProperty) {
      var name = _principalProperty.name.replace('principal', 'primary');
      var value = _principalProperty.value.split('/')[0];
      this.get('globalProperties').push({name: name, value: value});
    }, this);
  },

  /**
   * gather and return global properties with "principal_name"
   * @return {Array}
   */
  getPrincipalNames: function () {
    var principalNames = [];
    this.get('globalProperties').forEach(function (_globalProperty) {
      if (/principal_name?$/.test(_globalProperty.name)) {
        principalNames.push(_globalProperty);
      }
    }, this);
    this.get('secureProperties').forEach(function (_secureProperty) {
      if (/principal_name?$/.test(_secureProperty.name)) {
        var principalName = principalNames.findProperty('name', _secureProperty.name);
        if (!principalName) {
          _secureProperty.value = _secureProperty.defaultValue;
          principalNames.push(_secureProperty);
        }
      }
    }, this);
    return principalNames;
  },

  /**
   * load users from server
   */
  loadUsersFromServer: function () {
    if (App.testMode) {
      var serviceUsers = this.get('serviceUsers');
      this.get('testModeUsers').forEach(function (user) {
        user.id = 'puppet var';
        serviceUsers.push(user);
      }, this);
    } else {
      App.router.set('mainAdminSecurityController.serviceUsers', App.db.getSecureUserInfo());
    }
  },

  /**
   * manage secure configs
   * @return {Boolean}
   */
  manageSecureConfigs: function () {
    var serviceConfigTags = this.get('serviceConfigTags');
    var secureConfigs = this.get('secureConfigs');
    var siteProperties = this.get('configs').filterProperty('id', 'site property');
    var globalProperties = this.get('globalProperties');

    if (serviceConfigTags) {
      serviceConfigTags.forEach(function (_serviceConfigTags) {
        _serviceConfigTags.newTagName = 'version' + (new Date).getTime();

        if (_serviceConfigTags.siteName === 'global') {
          secureConfigs.forEach(function (config) {
            this.setPrincipalValue(config.serviceName, config.name);
          }, this);
          globalProperties.forEach(function (_globalProperty) {
            if (!/_hosts?$/.test(_globalProperty.name)) {
              _serviceConfigTags.configs[_globalProperty.name] = _globalProperty.value;
            }
          }, this);
        } else {
          siteProperties.filterProperty('filename', _serviceConfigTags.siteName + '.xml').forEach(function (_config) {
            _serviceConfigTags.configs[_config.name] = _config.value;
          }, this);
        }
      }, this);
      return true;
    } else {
      var command = this.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS');
      command.set('isSuccess', false);
      command.set('isError', true);
      this.onJsError();
      return false;
    }
  },

  /**
   * set value of principal property
   * @param serviceName
   * @param principalName
   * @return {Boolean}
   */
  setPrincipalValue: function (serviceName, principalName) {
    var globalProperties = this.get('globalProperties');
    var realmName = globalProperties.findProperty('name', 'kerberos_domain');

    if (this.get('secureServices').someProperty('serviceName', serviceName)) {
      var principalProperty = globalProperties.findProperty('name', principalName);
      principalProperty.value = principalProperty.value + '@' + realmName.value;
      return true;
    }
    return false;
  },

  /**
   * send DELETE command to server to delete component
   * @param componentName
   * @param hostName
   */
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

  /**
   * callback on successful deletion of component
   */
  onDeleteComplete: function () {
    var deleteAtsCommand = this.get('commands').findProperty('name', 'DELETE_ATS');
    console.warn('APP_TIMELINE_SERVER doesn\'t support security mode. It has been removed from YARN service ');
    deleteAtsCommand.set('isError', false);
    deleteAtsCommand.set('isSuccess', true);
  },

  /**
   * callback on failed deletion of component
   */
  onDeleteError: function () {
    console.warn('Error: Can\'t delete APP_TIMELINE_SERVER');
  },

  /**
   * show popup when js error occurred
   */
  onJsError: function () {
    App.ModalPopup.show({
      header: Em.I18n.t('common.error'),
      secondary: false,
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile('<p>{{t admin.security.apply.configuration.error}}</p>')
      })
    });
  }
});
