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

  secureServices: function() {
    return  this.get('content.services');
  }.property('content.services'),

  isBackBtnDisabled: function () {
    return !this.get('commands').someProperty('isError', true);
  }.property('commands.@each.isCompleted'),

  isOozieSelected: function () {
    return this.get('secureServices').someProperty('serviceName', 'OOZIE');
  }.property('secureServices'),

  isHiveSelected: function () {
    return this.get('secureServices').someProperty('serviceName', 'HIVE');
  }.property('secureServices'),

  isNagiosSelected: function () {
    return this.get('secureServices').someProperty('serviceName', 'NAGIOS');
  }.property('secureServices'),

  isZkSelected: function () {
    return this.get('secureServices').someProperty('serviceName', 'ZOOKEEPER');
  }.property('secureServices'),

  isWebHcatSelected: function () {
    var installedServices = App.Service.find().mapProperty('serviceName');
    return installedServices.contains('WEBHCAT');
  },

  isSecurityApplied: function () {
    return this.get('commands').someProperty('name', 'START_SERVICES') && this.get('commands').findProperty('name', 'START_SERVICES').get('isSuccess');
  }.property('commands.@each.isCompleted'),

  clearStep: function () {
    this.set('commands',[]);
    this.set('isSubmitDisabled', true);
    this.set('isBackBtnDisabled', true);
    this.get('serviceConfigTags').clear();
  },

  loadCommands: function () {
    this._super();
    // no need to remove ATS component if YARN and ATS are not installed
    if (this.get('secureServices').findProperty('serviceName', 'YARN') && App.Service.find('YARN').get('hostComponents').someProperty('componentName', 'APP_TIMELINE_SERVER')) {
      this.get('commands').splice(2, 0, App.Poll.create({name: 'DELETE_ATS', label: Em.I18n.translations['admin.addSecurity.apply.delete.ats'], isPolling: false, isVisible: false}));
      this.set('totalSteps', 4);
    }
  },

  loadStep: function () {
    this.set('secureMapping', require('data/secure_mapping').slice(0));
    this.clearStep();
    var commands = App.db.getSecurityDeployCommands();
    this.prepareSecureConfigs();
    if (commands && commands.length > 0) {
      commands.forEach(function (_command, index) {
        commands[index] = App.Poll.create(_command);
      }, this);
      if (commands.someProperty('isError', true)) {
        this.get('commands').pushObjects(commands);
        this.addObserver('commands.@each.isSuccess', this, 'onCompleteCommand');
        return;
      } else if (commands.filterProperty('isStarted', true).someProperty('isCompleted', false)) {
        var runningCommand = commands.filterProperty('isStarted', true).findProperty('isCompleted', false);
        runningCommand.set('isStarted', false);
        this.get('commands').pushObjects(commands);
      } else {
        this.get('commands').pushObjects(commands);
      }
    } else {
      this.loadCommands();
      this.addInfoToCommands();
      var runningOperations = App.router.get('backgroundOperationsController.services').filterProperty('isRunning');
      var stopAllOperation = runningOperations.findProperty('name', 'Stop All Services');
      var stopCommand = this.get('commands').findProperty('name', 'STOP_SERVICES');
      if (stopCommand.get('name') === 'STOP_SERVICES' && stopAllOperation) {
        stopCommand.set('requestId', stopAllOperation.get('id'));
      }
    }
    this.addObserver('commands.@each.isSuccess', this, 'onCompleteCommand');
    this.moveToNextCommand();
  },

  enableSubmit: function () {
    var addSecurityController = App.router.get('addSecurityController');
    if (this.get('commands').someProperty('isError', true) || this.get('commands').everyProperty('isSuccess', true)) {
      this.set('isSubmitDisabled', false);
      if (this.get('commands').someProperty('isError', true)) {
        addSecurityController.setStepsEnable();
      }
    } else {
      this.set('isSubmitDisabled', true);
      addSecurityController.setLowerStepsDisable(4);
    }
  }.observes('commands.@each.isCompleted'),

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


  manageSecureConfigs: function () {
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
          if (this.get('secureServices').someProperty('serviceName', 'STORM')) {
            var stormPrincipalName = this.get('globalProperties').findProperty('name', 'storm_principal_name');
            stormPrincipalName.value = stormPrincipalName.value + '@' + realmName.value;
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
      var command = this.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS');
      command.set('isSuccess', false);
      command.set('isError', true);
      if (err) {
        console.log("Error: Error occurred while applying secure configs to the server. Error message: " + err);
      }
      this.onJsError();
      return false;
    }
    return true;
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
    var deleteAtsCommand = this.get('commands').findProperty('name', 'DELETE_ATS');
    console.warn('APP_TIMELINE_SERVER doesn\'t support security mode. It has been removed from YARN service ');
    deleteAtsCommand.set('isError', false);
    deleteAtsCommand.set('isSuccess', true);
  },

  onDeleteError: function () {
    console.warn('Error: Can\'t delete APP_TIMELINE_SERVER');
  },

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
