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

App.UpgradeVersionBoxView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/upgrade_version_box'),
  classNames: ['span4', 'version-box'],

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
   * @type {object}
   * @default null
   */
  content: null,

  /**
   * object that describes how content should be displayed
   * @type {Em.Object}
   */
  stateElement: function () {
    var currentVersion = this.get('controller.currentVersion');
    var upgradeVersion = this.get('controller.upgradeVersion');
    var element = Em.Object.create();

    if (this.get('content.status') === 'CURRENT') {
      element.set('isLabel', true);
      element.set('text', Em.I18n.t('common.current'));
      element.set('class', 'label label-success');
    } else if (['INIT', 'INSTALL_FAILED', 'OUT_OF_SYNC'].contains(this.get('content.status'))) {
      element.set('isButton', true);
      element.set('text', Em.I18n.t('admin.stackVersions.version.installNow'));
      element.set('action', 'installRepoVersion');
    } else if (this.get('content.status') === 'INSTALLING') {
      element.set('iconClass', 'icon-cog');
      element.set('isLink', true);
      element.set('text', Em.I18n.t('hosts.host.stackVersions.status.installing'));
      element.set('action', 'showProgressPopup');
    } else if (this.get('content.status') === 'INSTALLED') {
      if (this.get('content.displayName') === upgradeVersion) {
        element.set('isLink', true);
        element.set('action', 'openUpgradeDialog');
        if (['HOLDING', 'HOLDING_FAILED'].contains(App.get('upgradeState'))) {
          element.set('iconClass', 'icon-pause');
          element.set('text', Em.I18n.t('admin.stackVersions.version.upgrade.pause'));
        } else {
          element.set('iconClass', 'icon-cog');
          element.set('text', Em.I18n.t('admin.stackVersions.version.upgrade.running'));
        }
      } else if (stringUtils.compareVersions(this.get('content.repositoryVersion'), currentVersion.repository_version) === 1) {
        element.set('isButton', true);
        element.set('text', Em.I18n.t('admin.stackVersions.version.performUpgrade'));
        element.set('action', 'runPreUpgradeCheck');
      } else {
        element.set('iconClass', 'icon-ok');
        element.set('isLink', true);
        element.set('text', Em.I18n.t('common.installed'));
      }
    }
    return element;
  }.property('content.status'),

  /**
   * run custom action of controller
   * @param {object} event
   */
  runAction: function (event) {
    var stateElement = event.context;
    if (stateElement.get('action')) {
      this.get('controller')[stateElement.get('action')](this.get('content'));
    }
  },

  /**
   * show popup with repositories to edit
   * @return {App.ModalPopup}
   */
  editRepositories: function () {
    var self = this;
    var repo = App.RepositoryVersion.find(this.get('content.id'));

    return App.ModalPopup.show({
      bodyClass: Ember.View.extend({
        content: repo,
        templateName: require('templates/main/admin/stack_upgrade/edit_repositories'),
        skipValidation: false
      }),
      header: Em.I18n.t('common.repositories'),
      primary: Em.I18n.t('common.save'),
      disablePrimary: !(App.get('isAdmin') && !App.get('isOperator')),
      onPrimary: function () {
        this.hide();
        self.get('controller').saveRepoOS();
      }
    });
  },

  /**
   * shows popup with listed hosts wich has current state of hostStackVersion
   * @param {object} event
   * @returns {App.ModalPopup}
   * @method showHostsListPopup
   */
  showHosts: function (event) {
    var status = event.contexts[0];
    var version = event.contexts[1];
    var hosts = event.contexts[2];
    var self = this;
    if (hosts.length) {
      return App.ModalPopup.show({
        bodyClass: Ember.View.extend({
          title: Em.I18n.t('admin.stackVersions.hosts.popup.title').format(version, status.label, hosts.length),
          template: Em.Handlebars.compile('<h4>{{view.title}}</h4><span class="limited-height-2">' + hosts.join('<br/>') + '</span>')
        }),
        header: Em.I18n.t('admin.stackVersions.hosts.popup.header').format(status.label),
        primary: Em.I18n.t('admin.stackVersions.hosts.popup.primary'),
        secondary: Em.I18n.t('common.close'),
        onPrimary: function () {
          this.hide();
          self.filterHostsByStack(version, status.id);
        }
      });
    }
  },

  /**
   * goes to the hosts page with content filtered by repo_version_name and repo_version_state
   * @param version
   * @param state
   * @method filterHostsByStack
   */
  filterHostsByStack: function (version, state) {
    if (!version || !state)
      return;
    App.router.get('mainHostController').filterByStack(version, state);
    App.router.get('mainHostController').set('showFilterConditionsFirstLoad', true);
    App.router.transitionTo('hosts.index');
  }
});
