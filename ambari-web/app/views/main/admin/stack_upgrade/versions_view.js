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

App.MainAdminStackVersionsView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/versions'),

  /**
   * update timer
   * @type {number|null}
   * @default null
   */
  updateTimer: null,

  /**
   * Not Installed = the version is not installed or out of sync
   * Upgrade Ready = the version is installed and ready for upgrade
   * Current = the version currently being used
   * Upgrade in Process = UPGRADING
   * Ready to Finalize = UPGRADED
   * Installed = All the versions that are installed BUT cannot be upgraded to (meaning: they are lower than the current)
   * @type {Array}
   */
  categories: [
    Em.Object.create({
      labelKey: 'admin.stackVersions.filter.all',
      value: '',
      isSelected: true
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.filter.notInstalled',
      value: 'NOT_INSTALLED',
      isSelected: false
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.filter.upgradeReady',
      value: 'UPGRADE_READY',
      isSelected: false
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.filter.current',
      value: 'CURRENT',
      isSelected: false
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.filter.installed',
      value: 'INSTALLED',
      isSelected: false
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.filter.upgrading',
      value: 'UPGRADING',
      isSelected: false
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.filter.upgraded',
      value: 'UPGRADED',
      isSelected: false
    })
  ],

  didInsertElement: function () {
    this.observesCategories();
  },

  /**
   * update categories labels
   */
  observesCategories: function () {
    this.get('categories').forEach(function (category) {
      category.set('label', Em.I18n.t(category.labelKey).format(this.filterBy(this.get('repoVersions'), category).length));
    }, this);
    this.filterVersions(this.get('selectedCategory'));
  }.observes('repoVersions.@each.stackVersion.state', 'controller.isLoaded'),

  /**
   * select category
   * @param event
   */
  selectCategory: function (event) {
    this.get('categories').filterProperty('isSelected').setEach('isSelected', false);
    event.context.set('isSelected', true);
    this.filterVersions(event.context);
  },

  /**
   * filter versions that match category
   * @param {Em.Object} category
   */
  filterVersions: function (category) {
    var filteredVersionIds = this.filterBy(this.get('repoVersions'), category).mapProperty('id');
    this.get('repoVersions').forEach(function (version) {
      version.set('isVisible', filteredVersionIds.contains(version.get('id')));
    });
  },

  /**
   * @type {object}
   */
  selectedCategory: function () {
    return this.get('categories').findProperty('isSelected');
  }.property('categories.@each.isSelected'),

  /**
   * @type {Em.Array}
   */
  repoVersions: App.RepositoryVersion.find(),

  /**
   * @type {Em.Array}
   */
  stackVersions: App.StackVersion.find(),

  /**
   * filter versions by category
   * @param versions
   * @param filter
   * @return {Array}
   */
  filterBy: function (versions, filter) {
    var currentVersion = this.get('controller.currentVersion');
    if (filter && filter.get('value')) {
      versions = versions.filter(function (version) {
        var status = version.get('status');
        if (status === 'INSTALLED' && ['UPGRADE_READY', 'INSTALLED'].contains(filter.get('value'))) {
          if (filter.get('value') === 'UPGRADE_READY') {
            return stringUtils.compareVersions(version.get('repositoryVersion'), Em.get(currentVersion, 'repository_version')) === 1;
          } else if (filter.get('value') === 'INSTALLED') {
            return stringUtils.compareVersions(version.get('repositoryVersion'), Em.get(currentVersion, 'repository_version')) < 1;
          }
        } else if (filter.get('value') === 'NOT_INSTALLED') {
          return ['INIT', 'INSTALL_FAILED', 'INSTALLING', 'OUT_OF_SYNC'].contains(status);
        } else {
          return status === filter.get('value');
        }
      }, this);
    }
    if (App.get('supports.displayOlderVersions') || Em.isNone(currentVersion)) {
      return versions.toArray();
    } else {
      return versions.filter(function(v) {
        return stringUtils.compareVersions(v.get('repositoryVersion'), Em.get(currentVersion, 'repository_version')) >= 0;
      }).toArray();
    }
  },

  /**
   * route to versions in Admin View
   * @return {App.ModalPopup}
   */
  goToVersions: function () {
    return App.showConfirmationPopup(function () {
      App.ajax.send({
        name: 'ambari.service.load_server_version',
        sender: this
      }).then(function(data) {
        var components = Em.get(data,'components');
        if (Em.isArray(components)) {
          var mappedVersions = components.map(function(component) {
              if (Em.get(component, 'RootServiceComponents.component_version')) {
                return Em.get(component, 'RootServiceComponents.component_version');
              }
            }),
            sortedMappedVersions = mappedVersions.sort(),
            latestVersion = sortedMappedVersions[sortedMappedVersions.length-1];
            console.log('/views/ADMIN_VIEW/' + latestVersion + '/INSTANCE/#/stackVersions');
            window.location.replace('/views/ADMIN_VIEW/' + latestVersion + '/INSTANCE/#/stackVersions');
        }
      });
    },
    Em.I18n.t('admin.stackVersions.manageVersions.popup.body'),
    null,
    Em.I18n.t('admin.stackVersions.manageVersions'));
  },

  /**
   * load ClusterStackVersions data
   */
  willInsertElement: function () {
    this.poll();
  },

  /**
   * stop polling upgrade state
   */
  willDestroyElement: function () {
    window.clearTimeout(this.get('updateTimer'));
    App.ajax.abortRequests(this.get('controller.runningCheckRequests'));
  },

  /**
   * set timer for polling
   */
  doPolling: function () {
    var self = this;
    this.set('updateTimer', window.setTimeout(function () {
      self.poll.apply(self);
    }, App.bgOperationsUpdateInterval));
  },

  /**
   * poll data
   */
  poll: function () {
    var self = this;
    //skip call if Upgrade wizard opened
    if (App.router.get('updateController').get('isWorking')) {
      this.get('controller').load().done(function () {
        self.set('controller.isLoaded', true);
        self.doPolling();
      });
    }
  }

});
