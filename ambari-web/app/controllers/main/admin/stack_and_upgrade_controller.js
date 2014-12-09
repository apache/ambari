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

App.MainAdminStackAndUpgradeController = Em.Controller.extend(App.LocalStorage, {
  name: 'mainAdminStackAndUpgradeController',

  /**
   * @type {Object|null}
   */
  serviceToInstall: null,

  /**
   * @type {object}
   * @default null
   */
  upgradeData: null,

  /**
   * @type {number}
   * @default null
   */
  upgradeId: null,

  /**
   * @type {string}
   * @default null
   */
  upgradeVersion: null,

  /**
   * version that currently applied to server
   * @type {Object|null}
   */
  currentVersion: null,

  /**
   * versions to which cluster could be upgraded
   * @type {Array}
   */
  targetVersions: [],

  /**
   * restore data from localStorage
   */
  init: function () {
    ['upgradeId', 'upgradeVersion'].forEach(function (property) {
      if (this.getDBProperty(property)) {
        this.set(property, this.getDBProperty(property));
      }
    }, this);
  },

  /**
   * @type {Array}
   */
  services: function() {
    return App.StackService.find().map(function(s) {
      s.set('isInstalled', App.Service.find().someProperty('serviceName', s.get('serviceName')));
      return s;
    });
  }.property('App.router.clusterController.isLoaded'),

  /**
   * launch Add Service wizard
   * @param event
   */
  goToAddService: function (event) {
    this.set('serviceToInstall', event.context);
    App.get('router').transitionTo('main.serviceAdd');
  },

  /**
   * call to fetch cluster stack versions
   * @return {$.ajax}
   */
  loadVersionsInfo: function () {
    return App.ajax.send({
      name: 'admin.stack_versions.all',
      sender: this,
      data: {},
      success: 'loadVersionsInfoSuccessCallback'
    });
  },

  /**
   * parse stack versions and
   * set <code>currentVersion</code>
   * set <code>targetVersions</code>
   * @param data
   */
  loadVersionsInfoSuccessCallback: function (data) {
    var versions = this.parseVersionsData(data);
    var current = versions.findProperty('state', 'CURRENT');
    var targetVersions = versions.without(current).filter(function (version) {
      //Only higher versions that have already been installed to all the hosts are shown
      return (version.state === 'INSTALLED' &&
        stringUtils.compareVersions(version.repository_version, current.repository_version) === 1);
    });
    this.set('currentVersion', current);
    this.set('targetVersions', targetVersions);
  },

  /**
   * parse ClusterStackVersions data to form common structure
   * @param {object} data
   * @return {Array}
   */
  parseVersionsData: function (data) {
    return data.items.map(function (item) {
      item.ClusterStackVersions.repository_name = item.repository_versions[0].RepositoryVersions.display_name;
      item.ClusterStackVersions.repository_id = item.repository_versions[0].RepositoryVersions.id;
      item.ClusterStackVersions.repository_version = item.repository_versions[0].RepositoryVersions.repository_version;
      return item.ClusterStackVersions;
    });
  },

  /**
   * load upgrade tasks by upgrade id
   * @return {$.Deferred}
   * @param {boolean} onlyState
   */
  loadUpgradeData: function (onlyState) {
    var upgradeId = this.get('upgradeId');
    var deferred = $.Deferred();

    if (Em.isNone(upgradeId)) {
      deferred.resolve();
      console.log('Upgrade in INIT state');
    } else {
      App.ajax.send({
        name: (onlyState) ? 'admin.upgrade.state' : 'admin.upgrade.data',
        sender: this,
        data: {
          id: upgradeId
        },
        success: 'loadUpgradeDataSuccessCallback'
      }).then(deferred.resolve);
    }
    return deferred.promise();
  },

  /**
   * parse and push upgrade tasks to controller
   * @param data
   */
  loadUpgradeDataSuccessCallback: function (data) {
    App.set('upgradeState', data.Upgrade.request_status);
    if (data.upgrade_groups) {
      this.set("upgradeData", data);
    }
  },

  /**
   * make call to start downgrade process
   */
  downgrade: function () {
    //TODO start downgrade
  },

  /**
   * make call to start upgrade process and show popup with current progress
   * @param {object} version
   */
  upgrade: function (version) {
    App.ajax.send({
      name: 'admin.upgrade.start',
      sender: this,
      data: {
        version: version.value
      },
      success: 'upgradeSuccessCallback'
    });
    this.set('upgradeVersion', version.label);
    this.setDBProperty('upgradeVersion', version.label);
  },

  /**
   * success callback of <code>upgrade()</code>
   * @param {object} data
   */
  upgradeSuccessCallback: function (data) {
    this.set('upgradeId', data.resources[0].Upgrade.request_id);
    this.setDBProperty('upgradeId', data.resources[0].Upgrade.request_id);
    App.clusterStatus.setClusterStatus({
      clusterName: App.get('clusterName'),
      clusterState: 'DEFAULT',
      localdb: App.db.data
    });
    this.openUpgradeDialog();
  },

  /**
   * send request for pre upgrade check
   * @param version
   */
  runPreUpgradeCheck: function(version) {
    App.ajax.send({
      name: "admin.rolling_upgrade.pre_upgrade_check",
      sender: this,
      data: version,
      success: "runPreUpgradeCheckSuccess"
    })
  },

  /**
   * success callback of <code>runPreUpgradeCheckSuccess()</code>
   * if there are some fails - it shows popup else run upgrade
   * @param data {object}
   * @param opt {object}
   * @param params {object}
   * @returns {App.ModalPopup|void}
   */
  runPreUpgradeCheckSuccess: function(data, opt, params) {
    if (data.UpgradeChecks.someProperty('status',"FAIL")) {
      return App.ModalPopup.show({
        header: Em.I18n.t('admin.stackUpgrade.preupgradeCheck.header').format(params.label),
        primary: Em.I18n.t('common.dismiss'),
        secondary: false,
        bodyClass: Em.View.extend({
          templateName: require('templates/main/admin/stack_upgrade/pre_upgrade_check_dialog'),
          checks: function() {
            return data.UpgradeChecks.filterProperty('status',"FAIL");
          }.property()
        })
      })
    } else {
      this.upgrade(params);
    }
  },
  /**
   * make call to resume upgrade process and show popup with current progress
   */
  resumeUpgrade: function () {
    //TODO resume upgrade
  },

  /**
   * make call to stop upgrade process
   */
  stopUpgrade: function () {
    //TODO stop upgrade
  },

  /**
   * make call to finish upgrade process
   */
  finalize: function () {
    //TODO execute finalize
    this.finish();
  },

  /**
   * finish upgrade wizard
   * clean auxiliary data
   */
  finish: function () {
    this.set('upgradeId', null);
    this.setDBProperty('upgradeId', undefined);
    App.set('upgradeState', 'INIT');
    this.set('upgradeVersion', null);
    this.setDBProperty('upgradeVersion', undefined);
    App.clusterStatus.setClusterStatus({
      clusterName: App.get('clusterName'),
      clusterState: 'DEFAULT',
      localdb: App.db.data
    });
  },

  /**
   * show dialog with tasks of upgrade
   * @return {App.ModalPopup}
   */
  openUpgradeDialog: function () {
    App.router.transitionTo('admin.stackUpgrade');
  }
});
