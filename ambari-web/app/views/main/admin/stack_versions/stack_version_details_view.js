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

App.MainStackVersionsDetailsView = Em.View.extend({

  templateName: require('templates/main/admin/stack_versions/stack_version_details'),

  content: function() {
    return this.get('controller.content');
  }.property('controller.content'),

  /**
   * message on install button depending on status
   * <code>INSTALL_FAILED<code>/INIT
   * @type {String}
   */
  installButtonMsg: function() {
    return this.get('content.stackVersion.state') == 'INSTALL_FAILED'
      ? Em.I18n.t('admin.stackVersions.details.hosts.btn.reinstall')
      : Em.I18n.t('admin.stackVersions.details.hosts.btn.install').format(this.get('controller.hostsToInstall'));
  }.property('content.stackVersion.state', 'parentView.content.stackVersion.initHosts.length'),

  /**
   * class on install button depending on status
   * <code>INSTALL_FAILED<code>/INIT
   * @type {String}
   */
  installButtonClass: function() {
    return this.get('content.stackVersion.state') == 'INSTALL_FAILED' ? 'btn-danger' : 'btn-success';
  }.property('content.stackVersion.state'),

  /**
   * property is used as width for progres bar
   * @type {String}
   */
  progress: function() {
    return "width:" + this.get('controller.progress') + "%";
  }.property('controller.progress'),

  /**
   * hosts with stack versions in not installed state
   * when stack version for repoversion is not created returns all hosts in cluster
   */
  notInstalledHosts: function() {
    return this.get('content.stackVersion') ? this.get('content.stackVersion.notInstalledHosts') : App.get('allHostNames');
  }.property('content.stackVersion.notInstalledHosts.length', 'App.allHostNames'),

  /**
   * hosts with stack versions in installed state
   * when stack version for repoversion is not created returns an empty array
   */
  installedHosts: function() {
    return this.get('content.stackVersion') ? this.get('content.stackVersion.installedHosts') : [];
  }.property('content.stackVersion.installedHosts.length'),

  /**
   * hosts with stack versions in current state
   * when stack version for repoversion is not created returns an empty array
   */
  currentHosts: function() {
    return this.get('content.stackVersion') ? this.get('content.stackVersion.currentHosts') : [];
  }.property('content.stackVersion.currentHosts.length'),

  /**
   * true if there are no hosts without this repoversion
   * @type {boolean}
   */
  noInitHosts: function() {
    return this.get('content.stackVersion') ? this.get('content.stackVersion.noInitHosts') : false;
  }.property('content.stackVersion.noInitHosts'),

  /**
   * true if there are no hosts with this repoversion
   * @type {boolean}
   */
  noInstalledHosts:  function() {
    return this.get('content.stackVersion') ? this.get('content.stackVersion.noInstalledHosts') : true;
  }.property('content.stackVersion.noInstalledHosts'),

  /**
   * true if there are no hosts with this repoversion as current
   * @type {boolean}
   */
  noCurrentHosts: function() {
    return this.get('content.stackVersion') ? this.get('content.stackVersion.noCurrentHosts') : true;
  }.property('content.stackVersion.noCurrentHosts'),

  /**
   * map containing version (id, label)
   * this is used as param for <code>showHosts<code> method
   * @type {Object}
   */
  versionStateMap: {
    'current': {
      'id': 'current',
      'label': Em.I18n.t('admin.stackVersions.hosts.popup.header.current')
    },
    'installed': {
      'id': 'installed',
      'label': Em.I18n.t('admin.stackVersions.hosts.popup.header.installed')
    },
    'not_installed': {
      'id': 'installing',
      'label': Em.I18n.t('admin.stackVersions.hosts.popup.header.not_installed')
    }
  },

  /**
   * runs <code>showHostsListPopup<code> from <code>repoVersionsManagementController<code>
   * to open popup with hosts that has this repo version
   * in not_installed state
   */
  showNotInstalledHosts: function() {
    App.router.get('repoVersionsManagementController').showHostsListPopup(this.get('versionStateMap.not_installed'),
      this.get('content.repositoryVersion'), this.get('notInstalledHosts'));
  },

  /**
   * runs <code>showHostsListPopup<code> from <code>repoVersionsManagementController<code>
   * to open popup with hosts that has this repo version
   * in installed state
   */
  showInstalledHosts: function() {
    App.router.get('repoVersionsManagementController').showHostsListPopup(this.get('versionStateMap.installed'),
      this.get('content.repositoryVersion'), this.get('installedHosts'));
  },

  /**
   * runs <code>showHostsListPopup<code> from <code>repoVersionsManagementController<code>
   * to open popup with hosts that has this repo version
   * in current state
   */
  showCurrentHosts: function() {
    App.router.get('repoVersionsManagementController').showHostsListPopup(this.get('versionStateMap.current'),
      this.get('content.repositoryVersion'), this.get('currentHosts'));
  },

  didInsertElement: function() {
    App.get('router.mainStackVersionsController').set('isPolling', true);
    if (!App.RepositoryVersion.find().findProperty('id', this.get('content.id'))) {
      App.get('router.mainStackVersionsController').load();
    }
    App.get('router.mainStackVersionsController').doPolling();
    this.get('controller').doPolling();
  },

  willDestroyElement: function () {
    App.get('router.mainStackVersionsController').set('isPolling', false);
    clearTimeout(App.get('router.mainStackVersionsController.timeoutRef'));
    clearTimeout(this.get('controller.timeoutRef'));
  }
});
