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
  filterContent: [
    {
      label: Em.I18n.t('common.all'),
      value: ''
    },
    {
      label: Em.I18n.t('admin.stackVersions.filter.notInstalled'),
      value: 'NOT_INSTALLED'
    },
    {
      label: Em.I18n.t('common.installed'),
      value: 'INSTALLED'
    },
    {
      label: Em.I18n.t('admin.stackVersions.filter.upgradeReady'),
      value: 'UPGRADE_READY'
    },
    {
      label: Em.I18n.t('common.current'),
      value: 'CURRENT'
    }
  ],

  /**
   * @type {object}
   * @default null
   */
  filterSelected: null,

  /**
   * @type {Ember.Select}
   * @class
   */
  filterView: Ember.Select.extend({
    selectionBinding: 'parentView.filterSelected',
    contentBinding: 'parentView.filterContent',
    optionValuePath: "content.value",
    optionLabelPath: "content.label"
  }),

  /**
   * @type {Em.Array}
   */
  repoVersions: App.RepositoryVersion.find(),

  /**
   * @type {Em.Array}
   */
  stackVersions: App.StackVersion.find(),

  /**
   * @type {Array}
   */
  filteredVersions: function () {
    var filter = this.get('filterSelected');
    var currentVersion = this.get('controller.currentVersion');
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

    if (filter && filter.value) {
      return versions.filter(function (version) {
        if (version.get('status') === 'INSTALLED' && filter.value === 'UPGRADE_READY') {
          return stringUtils.compareVersions(version.get('repositoryVersion'), currentVersion.repository_version) === 1;
        } else if (filter.value === 'NOT_INSTALLED') {
          return ['INIT', 'INSTALL_FAILED', 'INSTALLING', 'OUT_OF_SYNC'].contains(version.get('status'));
        } else {
          return version.get('status') === filter.value;
        }
      }, this);
    }
    return versions;
  }.property('filterSelected', 'repoVersions.length', 'stackVersions.@each.state'),

  /**
   * route to versions in Admin View
   */
  goToVersions: function () {
    window.location.replace('/views/ADMIN_VIEW/1.0.0/INSTANCE/#/stackVersions');
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
