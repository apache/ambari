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

App.MainAdminRepositoriesController = Em.Controller.extend({
  name: 'mainAdminRepositoriesController',
  services: [],
  allRepos: [],
  upgradeVersion: '',
  /**
   * get the newest version of HDP from server
   */
  updateUpgradeVersion: function () {
    if (App.router.get('clusterController.isLoaded')) {
      App.ajax.send({
        name: 'cluster.update_upgrade_version',
        data: {
          stackName: App.get('currentStackName')
        },
        sender: this,
        success: 'updateUpgradeVersionSuccessCallback',
        error: 'updateUpgradeVersionErrorCallback'
      });
    }
  }.observes('App.router.clusterController.isLoaded', 'App.currentStackVersion', 'App.router.mainServiceController.content.length'),

  updateUpgradeVersionSuccessCallback: function (data) {
    var upgradeVersion = this.get('upgradeVersion') || App.get('defaultStackVersion');
    var currentVersion = App.get('currentStackVersionNumber');
    var stackNamePrefix = App.get('currentStackName') + '-';
    upgradeVersion = upgradeVersion.replace(stackNamePrefix, '');
    var activeVersions = data.items.filterProperty('Versions.active');
    activeVersions.mapProperty('Versions.stack_version').forEach(function (version) {
      upgradeVersion = (stringUtils.compareVersions(upgradeVersion, version) === -1) ? version : upgradeVersion;
    });
    var currentStack = data.items.findProperty('Versions.stack_version', currentVersion);
    var upgradeStack = data.items.findProperty('Versions.stack_version', upgradeVersion);
    var minUpgradeVersion = upgradeStack.Versions.min_upgrade_version;
    if (minUpgradeVersion && (stringUtils.compareVersions(minUpgradeVersion, currentVersion) === 1)) {
      upgradeVersion = currentVersion;
      upgradeStack = currentStack;
    }
    upgradeVersion = stackNamePrefix + upgradeVersion;
    this.set('upgradeVersion', upgradeVersion);
    if (currentStack && upgradeStack) {
      this.parseServicesInfo(currentStack, upgradeStack);
    }
    else {
      console.log('HDP stack doesn\'t have services with defaultStackVersion');
    }
  },

  updateUpgradeVersionErrorCallback: function (request, ajaxOptions, error) {
    console.log('Error message is: ' + request.responseText);
    console.log('HDP stack doesn\'t have services with defaultStackVersion');
  },

  /**
   * get the installed repositories of HDP from server
   */
  loadRepositories: function () {
    if (App.router.get('clusterController.isLoaded')) {
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
    data.items.forEach(function (os) {
      if (!App.get('supports.ubuntu') && os.OperatingSystems.os_type == 'ubuntu12') return; // @todo: remove after Ubuntu support confirmation
      os.repositories.forEach(function (repository) {
        var osType = repository.Repositories.os_type;
        var repo = Em.Object.create({
          baseUrl: repository.Repositories.base_url,
          osType: osType,
          repoId: repository.Repositories.repo_id,
          repoName : repository.Repositories.repo_name,
          stackName : repository.Repositories.stack_name,
          stackVersion : repository.Repositories.stack_version,
          isFirst: false
        });
        var group = allRepos.findProperty('name', osType);
        if (!group) {
          group = {
            name: osType,
            repositories: []
          };
          repo.set('isFirst', true);
          allRepos.push(group);
        }
        group.repositories.push(repo);
      });
    }, this);
    allRepos.stackVersion = App.get('currentStackVersionNumber');
    this.set('allRepos', allRepos);
  },

  loadRepositoriesErrorCallback: function (request, ajaxOptions, error) {
    console.log('Error message is: ' + request.responseText);
  },

  /**
   * parse services info(versions, description) by version
   */
  parseServicesInfo: function (currentStack, upgradeStack) {
    var result = [];
    if (currentStack.stackServices.length || upgradeStack.stackServices.length) {
      // loop through all the service components
      currentStack.stackServices.forEach(function(currentStackService) {
        var stackService = App.StackService.find().findProperty('serviceName', currentStackService.StackServices.service_name);
        if (!!stackService) {
          var myService = Em.Object.create({
            serviceName: stackService.get('serviceName'),
            displayName: stackService.get('displayNameOnSelectServicePage'),
            isSelected: true,
            isInstalled: false,
            isHidden:  stackService.get('isHiddenOnSelectServicePage'),
            description: stackService.get('comments'),
            version: stackService.get('serviceVersion'),
            newVersion: ''
          });
          // it's possible that there is no corresponding service in the new stack
          var matchedService = upgradeStack.stackServices.findProperty("StackServices.service_name", stackService.get('serviceName'));
          if (matchedService) {
            myService.newVersion = matchedService.StackServices.service_version;
          }
          result.push(myService);
        }
      }, this);
    }
    this.set('services', result);
  }
});
