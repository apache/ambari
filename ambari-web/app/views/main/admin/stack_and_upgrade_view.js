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

App.MainAdminStackAndUpgradeView = Em.View.extend({
  templateName: require('templates/main/admin/stack_and_upgrade'),

  /**
   * label with number of HEALTHY hosts
   * @type {String}
   */
  hostsOnlineLabel: function () {
    var hostsCountMap = App.router.get('mainHostController.hostsCountMap');
    return Em.I18n.t('admin.stackUpgrade.hostsOnline').format(hostsCountMap.HEALTHY, hostsCountMap.TOTAL);
  }.property('App.router.mainHostController.hostsCountMap'),

  /**
   * label that depict current upgrade process state
   * @type {String}
   */
  upgradeStateLabel: function () {
    switch (App.get('upgradeState')) {
      case 'INIT':
        return (this.get('controller.targetVersions.length') > 0) ? Em.I18n.t('admin.stackUpgrade.state.available') : "";
      case 'IN_PROGRESS':
        return Em.I18n.t('admin.stackUpgrade.state.inProgress');
      case 'STOPPED':
        return Em.I18n.t('admin.stackUpgrade.state.stopped');
      case 'COMPLETED':
        return Em.I18n.t('admin.stackUpgrade.state.completed');
      default:
        return "";
    }
  }.property('App.upgradeState', 'controller.targetVersions'),

  willInsertElement: function () {
    if (App.get('supports.stackUpgrade')) this.get('controller').loadVersionsInfo();
  },

  /**
   * box that display info about current version
   * @type {Em.View}
   */
  sourceVersionView: App.UpgradeVersionBoxView.extend({
    version: function () {
      return this.get('controller.currentVersion');
    }.property('controller.currentVersion'),
    btnClass: 'btn-danger',
    action: function () {
      return {
        method: ['UPGRADING', 'UPGRADED', 'UPGRADE_FAILED'].contains(this.get('version.state')) && 'downgrade',
        label: ['UPGRADING', 'UPGRADED', 'UPGRADE_FAILED'].contains(this.get('version.state')) && Em.I18n.t('common.downgrade')
      };
    }.property('version.state'),
    hostsCount: function () {
      return this.get('version.current_hosts.length');
    }.property('version.current_hosts.length')
  }),

  /**
   * box that display info about target versions
   * @type {Em.View}
   */
  targetVersionView: App.UpgradeVersionBoxView.extend({
    versions: function () {
      return this.get('controller.targetVersions');
    }.property('controller.targetVersions'),
    btnClass: 'btn-success',
    versionName: function () {
      if (!this.get('hasVersionsToUpgrade')) return Em.I18n.t('admin.stackUpgrade.state.notAvailable');
      return this.get('version.stack') + "-" + this.get('version.version');
    }.property('version.stack', 'version.version', 'hasVersionsToUpgrade'),
    hasVersionsToUpgrade: function () {
      return this.get('versions.length') > 0;
    }.property('versions.length'),
    selectedVersion: null,
    versionsSelectContent: function () {
      return this.get('versions').map(function (version) {
        return {
          label: version.stack + "-" + version.version,
          value: version.id
        }
      });
    }.property('versions.length'),

    /**
     * button properties:
     * - method of controller which will be called on click <code>method</code>
     * - label <code>label</code>
     * @type {Object}
     */
    action: function () {
      var method = null,
          label = "",
          versions = this.get('versions');
      switch (App.get('upgradeState')) {
        case 'INIT':
          if (this.get('versions.length') > 0) {
            label = Em.I18n.t('common.upgrade');
            method = 'upgrade';
          }
          break;
        case 'IN_PROGRESS':
          label = Em.I18n.t('admin.stackUpgrade.state.upgrading');
          method = 'openUpgradeDialog';
          break;
        case 'STOPPED':
          label = Em.I18n.t('admin.stackUpgrade.state.resume');
          method = 'resumeUpgrade';
          break;
        case 'COMPLETED':
          label = Em.I18n.t('common.finalize');
          method = 'finalize';
          break;
      }
      return {
        method: method,
        label: label
      };
    }.property('versions.length', 'App.upgradeState')
  })
});

