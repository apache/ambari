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

App.MainAdminStackServicesView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/services'),

  isAddServiceAvailable: function () {
    return App.isAuthorized('CLUSTER.UPGRADE_DOWNGRADE_STACK');
  }.property('App.supports.opsDuringRollingUpgrade', 'App.upgradeState', 'App.isAdmin'),

  /**
   * @type {Array}
   */
  services: function() {
    var services = App.supports.installGanglia ? App.StackService.find() : App.StackService.find().without(App.StackService.find('GANGLIA'));
    var controller = this.get('controller');

    services.map(function(s) {
      s.set('serviceVersionDisplay', controller.get('serviceVersionsMap')[s.get('serviceName')]);
      s.set('isInstalled', App.Service.find().someProperty('serviceName', s.get('serviceName')));
      return s;
    });
    return services;
  }.property('App.router.clusterController.isLoaded', 'controller.serviceVersionsMap'),

  didInsertElement: function () {
    if (!App.get('stackVersionsAvailable')) {
      this.get('controller').loadStackVersionsToModel(true).done(function () {
        App.set('stackVersionsAvailable', App.StackVersion.find().content.length > 0);
      });
      this.get('controller').loadRepositories();
    }
  },

  /**
   * launch Add Service wizard
   * @param event
   */
  goToAddService: function (event) {
    if (!App.isAuthorized('SERVICE.ADD_DELETE_SERVICES') || !App.supports.enableAddDeleteServices) {
      return;
    } else if (event.context == "KERBEROS") {
      App.router.get('mainAdminKerberosController').checkAndStartKerberosWizard();
      App.router.get('kerberosWizardController').setDBProperty('onClosePath', 'main.admin.stackAndUpgrade.services');
    } else {
      App.router.get('addServiceController').set('serviceToInstall', event.context);
      App.router.get('addServiceController').setDBProperty('onClosePath', 'main.admin.stackAndUpgrade.services');
      App.get('router').transitionTo('main.serviceAdd');
    }
  },

  /**
   * List of all repo-groups
   * @type {Object[][]}
   */
  allRepositoriesGroups: function () {
    var repos = this.get('controller.allRepos');
    var reposGroup = [];
    var repositories = [];
    reposGroup.set('stackVersion', App.get('currentStackVersionNumber'));
    if (repos) {
      repos.forEach(function (group) {
        group.repositories.forEach (function(repo) {
          var cur_repo = Em.Object.create({
            'repoId': repo.repoId,
            'id': repo.repoId + '-' + repo.osType,
            'repoName' : repo.repoName,
            'stackName' : repo.stackName,
            'stackVersion' : repo.stackVersion,
            'baseUrl': repo.baseUrl,
            'originalBaseUrl': repo.baseUrl,
            'osType': repo.osType,
            'onEdit': false,
            'empty-error': !repo.baseUrl,
            'undo': false,
            'clearAll': repo.baseUrl
          });
          var cur_group = reposGroup.findProperty('name', group.name);
          if (!cur_group) {
            cur_group = Ember.Object.create({
              name: group.name,
              repositories: []
            });
            reposGroup.push(cur_group);
          }
          cur_group.repositories.push(cur_repo);
          repositories.push(cur_repo);
        });
      });
    }
    this.set('allRepos', repositories);
    return reposGroup;
  }.property('controller.allRepos'),

  /**
   * Onclick handler for edit action of each repo, enter edit mode
   * @param {object} event
   */
  onEditClick:function (event) {
    var targetRepo = this.get('allRepos').findProperty('id', event.context.get('id'));
    if (targetRepo) {
      targetRepo.set('onEdit', true);
    }
  },

  /**
   * Handler for clear icon click
   * @method clearGroupLocalRepository
   * @param {object} event
   */
  clearGroupLocalRepository: function (event) {
    this.doActionForGroupLocalRepository(event, '');
  },

  /**
   * Common handler for repo groups actions
   * @method doActionForGroupLocalRepository
   * @param {object} event
   * @param {string} newBaseUrlField
   */
  doActionForGroupLocalRepository: function (event, newBaseUrlField) {
    var targetRepo = this.get('allRepos').findProperty('id', event.context.get('id'));
    if (targetRepo) {
      targetRepo.set('baseUrl', Em.isEmpty(newBaseUrlField) ? '' : Em.get(targetRepo, newBaseUrlField));
    }
  },
});
