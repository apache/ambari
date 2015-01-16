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
      labelKey: 'admin.stackVersions.filter.installed',
      value: 'INSTALLED',
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
    })
  ],

  didInsertElement: function () {
    this.observesCategories();
  },

  /**
   * update categories labels
   */
  observesCategories: function() {
    var versions = this.get('versions');
    this.get('categories').forEach(function (category) {
      category.set('label', Em.I18n.t(category.labelKey).format(this.filterBy(versions, category).length));
    }, this);
  }.observes('versions.@each.status'),

  /**
   * select category
   * @param event
   */
  selectCategory: function (event) {
    this.get('categories').filterProperty('isSelected').setEach('isSelected', false);
    event.context.set('isSelected', true);
  },

  /**
   * @type {object}
   */
  selectedCategory: function(){
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
   * formatted versions
   * @type {Array}
   */
  versions: function () {
    var versions = this.get('repoVersions').map(function (version) {
      var versionFormatted = Em.Object.create({
        id: version.get('id'),
        displayName: version.get('displayName'),
        repositoryVersion: version.get('repositoryVersion'),
        stackVersionType: version.get('stackVersionType'),
        stackVersionNumber: version.get('stackVersionNumber'),
        status: 'INIT',
        notInstalledHosts: [],
        installedHosts: [],
        currentHosts: []
      });
      if (version.get('stackVersion')) {
        versionFormatted.set('status', version.get('stackVersion.state'));
        versionFormatted.set('notInstalledHosts', version.get('stackVersion.notInstalledHosts'));
        versionFormatted.set('installedHosts', version.get('stackVersion.installedHosts'));
        versionFormatted.set('currentHosts', version.get('stackVersion.currentHosts'));
      }
      return versionFormatted;
    });

    versions.sort(function (a, b) {
      return stringUtils.compareVersions(a.get('repositoryVersion'), b.get('repositoryVersion'));
    });
    return versions;
  }.property('repoVersions.length', 'stackVersions.@each.state'),

  /**
   * @type {Array}
   */
  filteredVersions: function () {
    return this.filterBy(this.get('versions'), this.get('selectedCategory'))
  }.property('selectedCategory', 'versions.@each.status'),

  /**
   * filter versions by category
   * @param versions
   * @param filter
   * @return {Array}
   */
  filterBy: function (versions, filter) {
    var currentVersion = this.get('controller.currentVersion');
    if (filter && filter.get('value')) {
      return versions.filter(function (version) {
        if (version.get('status') === 'INSTALLED' && filter.get('value') === 'UPGRADE_READY') {
          return stringUtils.compareVersions(version.get('repositoryVersion'), currentVersion.repository_version) === 1;
        } else if (filter.get('value') === 'NOT_INSTALLED') {
          return ['INIT', 'INSTALL_FAILED', 'INSTALLING', 'OUT_OF_SYNC'].contains(version.get('status'));
        } else {
          return version.get('status') === filter.get('value');
        }
      }, this);
    }
    return versions;
  },

  /**
   * route to versions in Admin View
   */
  goToVersions: function () {
    App.showConfirmationPopup(function () {
      window.location.replace('/views/ADMIN_VIEW/1.0.0/INSTANCE/#/stackVersions');
    },
    Em.I18n.t('admin.stackVersions.manageVersions.popup.body'),
    null,
    Em.I18n.t('admin.stackVersions.manageVersions'));
  },

  /**
   * load ClusterStackVersions data
   */
  willInsertElement: function () {
    this.doPolling();
  },

  /**
   * stop polling upgrade state
   */
  willDestroyElement: function () {
    clearTimeout(this.get('updateTimer'));
  },

  /**
   * poll Upgrade state
   */
  doPolling: function () {
    var self = this;
    this.set('updateTimer', setTimeout(function () {
      //skip call if Upgrade wizard opened
      if (App.router.get('updateController').get('isWorking')) {
        self.get('controller').load().done(function () {
          self.set('controller.isLoaded', true);
          self.doPolling();
        });
      }
    }, App.bgOperationsUpdateInterval));
  }

});
