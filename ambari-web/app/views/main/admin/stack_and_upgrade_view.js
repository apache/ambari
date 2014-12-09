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
   * update timer
   * @type {number|null}
   * @default null
   */
  updateTimer: null,

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
      case 'PENDING':
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

  /**
   * load ClusterStackVersions data
   */
  willInsertElement: function () {
    var self = this;
    if (App.get('supports.stackUpgrade')) {
      self.get('controller').loadVersionsInfo();
      self.doPolling();
    }
  },

  /**
   * stop polling upgrade state
   */
  willDestroyElement: function () {
    clearTimeout(this.get('updateTimer'));
  },

  /**
   * poll upgrade state,
   */
  doPolling: function () {
    var self = this;
    this.set('updateTimer', setTimeout(function () {
      self.get('controller').loadUpgradeData(true);
      self.doPolling();
    }, App.bgOperationsUpdateInterval));
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

    /**
     * method of controller called on click of source version button
     * @type {string}
     * @default null
     */
    method: null,

    /**
     * label of source version button
     * @type {string}
     */
    label: "",
    buttonObserver: function () {
      this.set('method', App.get('upgradeState') !== 'INIT' && 'downgrade');
      this.set('label', App.get('upgradeState') !== 'INIT' && Em.I18n.t('common.downgrade'));
    }.observes('App.upgradeState'),
    hostsCount: function () {
      return this.get('version.host_states.CURRENT.length');
    }.property('version.host_states.CURRENT.length')
  }),

  /**
   * box that display info about target versions
   * @type {Em.View}
   */
  targetVersionView: App.UpgradeVersionBoxView.extend({
    /**
     * method of controller called on click of target version button
     * @type {string}
     * @default null
     */
    method: null,

    /**
     * label of target version button
     * @type {string}
     */
    label: "",
    versions: function () {
      return this.get('controller.targetVersions');
    }.property('controller.targetVersions'),
    btnClass: 'btn-success',
    versionName: function () {
      if (this.get('versions.length') === 0) return Em.I18n.t('admin.stackUpgrade.state.notAvailable');
      return this.get('controller.upgradeVersion');
    }.property('controller.upgradeVersion', 'showSelect'),
    showSelect: function () {
      return this.get('versions.length') > 0 && App.get('upgradeState') === 'INIT';
    }.property('versions.length', 'App.upgradeState'),

    /**
     * fix for Ember.Select
     * if Ember.Select initiated with empty content then after content is populated no option selected
     */
    initSelect: function () {
      if (this.get('versions.length') > 0) this.set('version', this.get('versionsSelectContent')[0]);
    }.observes('versions.length'),
    version: null,
    versionsSelectContent: function () {
      return this.get('versions').map(function (version) {
        return {
          label: version.repository_name,
          value: version.repository_version
        }
      });
    }.property('versions.length'),

    /**
     * button properties:
     * - method of controller which will be called on click <code>method</code>
     * - label <code>label</code>
     * @type {Object}
     */
    buttonObserver: function () {
      var method = null,
        label = "",
        versions = this.get('versions');
      switch (App.get('upgradeState')) {
        case 'INIT':
          if (this.get('versions.length') > 0) {
            label = Em.I18n.t('common.upgrade');
            method = 'runPreUpgradeCheck';
          }
          break;
        case 'PENDING':
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
      this.set('method', method);
      this.set('label',  label);
    }.observes('versions.length', 'App.upgradeState')
  })
});

