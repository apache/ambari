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
App.MainAdminSecurityDisableController = App.MainAdminSecurityProgressController.extend({

  name: 'mainAdminSecurityDisableController',
  secureServices: [],

  clearStep: function () {
    this.get('commands').clear();
    this.get('secureServices').clear();
    this.get('serviceConfigTags').clear();
  },

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
    this.loadSecureServices();
    this.addObserver('commands.@each.isSuccess', this, 'onCompleteCommand');
    this.moveToNextCommand();
  },


  enableSubmit: function () {
    if (this.get('commands').someProperty('isError', true) || this.get('commands').everyProperty('isSuccess', true)) {
      this.set('isSubmitDisabled', false);
    } else {
      this.set('isSubmitDisabled', true);
    }
  }.observes('commands.@each.isCompleted'),


  loadSecureServices: function () {
    var secureServices = App.get('isHadoop2Stack')?require('data/HDP2/secure_configs'):require('data/secure_configs');
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


  manageSecureConfigs: function () {
    try {
      this.get('serviceConfigTags').forEach(function (_serviceConfigTags, index) {
        _serviceConfigTags.newTagName = 'version' + (new Date).getTime();
        if (_serviceConfigTags.siteName === 'global') {
          this.get('secureProperties').forEach(function (_config) {
            if (_config.name in _serviceConfigTags.configs) {
              delete _serviceConfigTags.configs[_config.name];
            }
          }, this);
          _serviceConfigTags.configs.security_enabled = 'false';
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
                case 'mapred.task.tracker.task-controller':
                  _serviceConfigTags.configs[configName] = 'org.apache.hadoop.mapred.DefaultTaskController';
                  break;
                case 'yarn.nodemanager.container-executor.class':
                  _serviceConfigTags.configs[configName] = 'org.apache.hadoop.yarn.server.nodemanager.DefaultContainerExecutor';
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
                case 'zookeeper.znode.parent':
                  _serviceConfigTags.configs[configName] = '/hbase-unsecure';
                  break;
                case 'hive.security.authorization.enabled':
                  _serviceConfigTags.configs[configName] = 'false';
                  break;
                default:
                  delete _serviceConfigTags.configs[configName];
              }
            }
            console.log("Not Deleted" + _config.name);
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
      return false;
    }
    return true;
  }

});
