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
require('controllers/main/admin/security/security_progress_controller');

App.MainAdminSecurityDisableController = App.MainAdminSecurityProgressController.extend({
  name: 'mainAdminSecurityDisableController',
  secureServices: [],
  /**
   * values of site configs when security disabled.
   * Properties not defined in data/secure_mapping or data/HDP2/secure_mapping and needs to be changed on disabling
   * security should be defined in secureConfigValuesMap Object
   */
  secureConfigValuesMap: {
    'nimbus.childopts': function(value) {
     return value.replace (/-Djava.security.auth.login.config\s*=\s*\S*/g, "");
    },
    'ui.childopts': function(value) {
       return value.replace (/-Djava.security.auth.login.config\s*=\s*\S*/g, "");
    },
    'supervisor.childopts': function(value) {
      return value.replace (/-Djava.security.auth.login.config\s*=\s*\S*/g, "");
    }
  },

  isSubmitDisabled: function () {
    return !(this.get('commands').someProperty('isError') || this.get('commands').everyProperty('isSuccess'));
  }.property('commands.@each.isCompleted'),

  /**
   * clear step info
   */
  clearStep: function () {
    this.get('commands').clear();
    this.get('secureServices').clear();
    this.get('serviceConfigTags').clear();
  },

  /**
   * load info required by current step
   */
  loadStep: function () {
    this.clearStep();
    var commands = App.db.getSecurityDeployCommands();
    if (commands && commands.length > 0) {
      commands.forEach(function (_command, index) {
        commands[index] = App.Poll.create(_command);
      }, this);
      if (commands.someProperty('isError', true)) {
        this.get('commands').pushObjects(commands);
        this.loadSecureServices();
        this.addObserverToCommands();
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
      this.syncStopServicesCommand();
    }
    this.loadSecureServices();
    this.addObserverToCommands();
    this.moveToNextCommand();
  },

  /**
   * resume info about commands from local storage
   * @return {Boolean}
   */
  resumeCommands: function () {
    var commands = App.db.getSecurityDeployCommands();
    if (!commands || commands.length === 0) return false;

    commands.forEach(function (_command) {
      this.get('commands').pushObject(App.Poll.create(_command));
    }, this);
    var runningCommand = this.get('commands').filterProperty('isStarted').findProperty('isCompleted', false);
    if (runningCommand) {
      runningCommand.set('isStarted', false);
    }
    return true;
  },

  /**
   * synchronize existing background operation "Stop All Services" with command in Security wizard
   */
  syncStopServicesCommand: function () {
    var runningOperations = App.router.get('backgroundOperationsController.services').filterProperty('isRunning');
    var stopAllOperation = runningOperations.findProperty('name', 'Stop All Services');
    var stopCommand = this.get('commands').findProperty('name', 'STOP_SERVICES');
    if (stopCommand && stopAllOperation) {
      stopCommand.set('requestId', stopAllOperation.get('id'));
    }
  },

  /**
   * load secure configs of installed services
   */
  loadSecureServices: function () {
    var secureServices = require('data/HDP2/secure_configs');
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

  /**
   * manage configurations from serviceConfigTags
   * @return {Boolean}
   */
  manageSecureConfigs: function () {
    var serviceConfigTags = this.get('serviceConfigTags');
    var secureProperties = this.get('secureProperties');
    var secureMapping = this.get('secureMapping');
    if (!serviceConfigTags || !secureProperties || !secureMapping) {
      var command = this.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS');
      command.set('isSuccess', false);
      command.set('isError', true);
      return false;
    } else {
      serviceConfigTags.forEach(function (_serviceConfigTags) {
        _serviceConfigTags.newTagName = 'version' + (new Date).getTime();
        if (_serviceConfigTags.siteName.contains('-env')) {
          this.deleteDisabledConfigs(secureProperties, _serviceConfigTags);
          if (_serviceConfigTags.siteName === 'cluster-env') {
            _serviceConfigTags.configs.security_enabled = 'false';
          }
        } else {
          this.modifySiteConfigs(secureMapping, _serviceConfigTags);
        }
      }, this);
      return true;
    }
  },
  /**
   * delete configs, which aren't required when security disabled
   * @param secureProperties
   * @param _serviceConfigTags
   * @return {Boolean}
   */
  deleteDisabledConfigs: function (secureProperties, _serviceConfigTags) {
    if (!secureProperties || !_serviceConfigTags) return false;
    secureProperties.forEach(function (_config) {
      if (_config.name in _serviceConfigTags.configs) {
        delete _serviceConfigTags.configs[_config.name];
      }
    }, this);
    return true;
  },
  /**
   * delete unnecessary site configs and
   * change config values
   * @param secureMapping
   * @param _serviceConfigTags
   * @return {Boolean}
   */
  modifySiteConfigs: function (secureMapping, _serviceConfigTags) {
    var secureConfigValuesMap = this.get('secureConfigValuesMap');
    if (!secureMapping || !_serviceConfigTags) return false;

    // iterate over secureConfigValuesMap to update service-site configProperties not present in secureMapping metadata
    for (var key in secureConfigValuesMap) {
      if (key in _serviceConfigTags.configs) {
        var value = secureConfigValuesMap[key];
        if (typeof value == 'function') {
          _serviceConfigTags.configs[key] = value(_serviceConfigTags.configs[key]);
        }  else if (value) {
          _serviceConfigTags.configs[key] = value;
        }
      }
    }

    secureMapping.filterProperty('filename', _serviceConfigTags.siteName + '.xml').forEach(function (_config) {
      var configName = _config.name;
      var nonSecureConfigValue = _config.nonSecureValue;
      if (configName in _serviceConfigTags.configs) {
        if (nonSecureConfigValue) {
          _serviceConfigTags.configs[configName] = nonSecureConfigValue;
        } else {
          delete _serviceConfigTags.configs[configName]
        }
      }
    }, this);
    return true;
  }
});
