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
var stringUtils = require('utils/string_utils');

App.MainAdminClusterController = Em.Controller.extend({
  name:'mainAdminClusterController',
  services: [],
  repositories: [],
  upgradeVersion: '',
  /**
   * get the newest version of HDP from server
   */
  updateUpgradeVersion: function(){
    if(App.router.get('clusterController.isLoaded')){
      App.ajax.send({
        name: 'cluster.update_upgrade_version',
        sender: this,
        success: 'updateUpgradeVersionSuccessCallback',
        error: 'updateUpgradeVersionErrorCallback'
      });
    }
  }.observes('App.router.clusterController.isLoaded', 'App.currentStackVersion'),

  updateUpgradeVersionSuccessCallback: function(data) {
    var upgradeVersion = this.get('upgradeVersion') || App.defaultStackVersion;
    var currentStack = {};
    var upgradeStack = {};
    var currentVersion = App.get('currentStackVersionNumber');
    var minUpgradeVersion = currentVersion;
    upgradeVersion = upgradeVersion.replace(/HDP-/, '');
    data.items.mapProperty('Versions.stack_version').forEach(function(version){
      upgradeVersion = (stringUtils.compareVersions(upgradeVersion, version) === -1) ? version : upgradeVersion;
    });
    currentStack = data.items.findProperty('Versions.stack_version', currentVersion);
    upgradeStack = data.items.findProperty('Versions.stack_version', upgradeVersion);
    minUpgradeVersion = upgradeStack.Versions.min_upgrade_version;
    if(minUpgradeVersion && (stringUtils.compareVersions(minUpgradeVersion, currentVersion) === 1)){
      upgradeVersion = currentVersion;
      upgradeStack = currentStack;
    }
    upgradeVersion = 'HDP-' + upgradeVersion;
    this.set('upgradeVersion', upgradeVersion);
    if(currentStack && upgradeStack) {
      this.parseServicesInfo(currentStack, upgradeStack);
    }
    else {
      console.log('HDP stack doesn\'t have services with defaultStackVersion');
    }
  },

  updateUpgradeVersionErrorCallback: function(request, ajaxOptions, error) {
    console.log('Error message is: ' + request.responseText);
    console.log('HDP stack doesn\'t have services with defaultStackVersion');
  },

  /**
   * get the installed repositories of HDP from server
   */
  loadRepositories: function(){
    if(App.router.get('clusterController.isLoaded')){
      var nameVersionCombo = App.get('currentStackVersion');
      var stackName = nameVersionCombo.split('-')[0];
      var stackVersion = nameVersionCombo.split('-')[1];
      App.ajax.send({
        name: 'cluster.load_repositories',
        sender: this,
        data: {
          stackName: stackName,
          stackVersion: stackVersion
        },
        success: 'loadRepositoriesSuccessCallback',
        error: 'loadRepositoriesErrorCallback'
      });
    }
  }.observes('App.router.clusterController.isLoaded'),

  loadRepositoriesSuccessCallback: function (data) {
    var allRepos = [];
    data.items.forEach(function(os) {
      var repo = Em.Object.create({
        baseUrl: os.repositories[0].Repositories.base_url,
        osType: os.repositories[0].Repositories.os_type
      });
      allRepos.push(repo);
    }, this);
    allRepos.stackVersion = App.get('currentStackVersionNumber');
    this.set('repositories', allRepos);
  },

  loadRepositoriesErrorCallback: function(request, ajaxOptions, error) {
    console.log('Error message is: ' + request.responseText);
  },

  /**
   * parse services info(versions, description) by version
   */
  parseServicesInfo: function (currentStack, upgradeStack) {
    var result = [];
    var installedServices = App.Service.find().mapProperty('serviceName');
    var displayOrderConfig = require('data/services');
    if(currentStack.stackServices.length && upgradeStack.stackServices.length){
      // loop through all the service components
      for (var i = 0; i < displayOrderConfig.length; i++) {
        var entry = currentStack.stackServices.
          findProperty("StackServices.service_name", displayOrderConfig[i].serviceName);
        if (entry) {
          entry = entry.StackServices;
          if (installedServices.contains(entry.service_name)) {
            var myService = Em.Object.create({
              serviceName: entry.service_name,
              displayName: displayOrderConfig[i].displayName,
              isDisabled: displayOrderConfig[i].isDisabled,
              isSelected: true,
              isInstalled: false,
              isHidden: displayOrderConfig[i].isHidden,
              description: entry.comments,
              version: entry.service_version,
              newVersion: ''
            });
            // it's possible that there is no corresponding service in the new stack
            var matchedService = upgradeStack.stackServices.findProperty("StackServices.service_name", displayOrderConfig[i].serviceName);
            if (matchedService) {
              myService.newVersion = matchedService.StackServices.service_version;
            }
            //From 1.3.0 for Hive we display only "Hive" (but it install HCat and WebHCat as well)
            if (this.get('upgradeVersion').replace(/HDP-/, '') >= '1.3.0' && displayOrderConfig[i].serviceName == 'HIVE') {
              myService.set('displayName', 'Hive');
            }
            result.push(myService);
          }
        }
        else {
          console.warn('Service not found - ', displayOrderConfig[i].serviceName);
        }
      }
    }
    this.set('services', result);
  }
});