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
    this.get('stages').clear();
    this.get('secureServices').clear();
    this.get('serviceConfigTags').clear();
  },

  loadStep: function () {
    this.clearStep();
    var stages = App.db.getSecurityDeployStages();
    if (stages && stages.length > 0) {
      stages.forEach(function (_stage, index) {
        stages[index] = App.Poll.create(_stage);
      }, this);
      if (stages.someProperty('isError', true)) {
        this.get('stages').pushObjects(stages);
        this.loadSecureServices();
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
    this.loadSecureServices();
    this.addObserver('stages.@each.isSuccess', this, 'onCompleteStage');
    this.moveToNextStage();
  },


  enableSubmit: function () {
    if (this.get('stages').someProperty('isError', true) || this.get('stages').everyProperty('isSuccess', true)) {
      this.set('isSubmitDisabled', false);
    } else {
      this.set('isSubmitDisabled', true);
    }
  }.observes('stages.@each.isCompleted'),


  loadSecureServices: function () {
    var secureServices = require('data/secure_configs');
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
          _serviceConfigTags.configs.dfs_datanode_address = '50010';
          _serviceConfigTags.configs.dfs_datanode_http_address = '50075';
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
      var stage3 = this.get('stages').findProperty('stage', 'stage3');
      if (stage3) {
        stage3.set('isSuccess', false);
        stage3.set('isError', true);
      }
      if (err) {
        console.log("Error: Error occurred while applying secure configs to the server. Error message: " + err);
      }
      return false;
    }
    return true;
  }

});
