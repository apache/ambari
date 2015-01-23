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
      isSelected: true
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.filter.notInstalled',
      isSelected: false,
      statuses: ["INIT", "INSTALLING", "INSTALL_FAILED", "OUT_OF_SYNC"]
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.filter.upgradeReady',
      isSelected: false,
      statuses: ["INSTALLED"]
    }),
    Em.Object.create({
      labelKey: 'admin.stackVersions.filter.current',
      isSelected: false,
      statuses: ["CURRENT"]
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
    if (filter && filter.get('statuses')) {
      return versions.filter(function (version) {
        if (version.get('status') === 'INSTALLED' && filter.get('statuses').contains("INSTALLED")) {
          return stringUtils.compareVersions(version.get('repositoryVersion'), Em.get(currentVersion, 'repository_version')) === 1;
        } else {
          return filter.get('statuses').contains(version.get('status'));
        }
      }, this);
    }
    return versions.toArray();
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
