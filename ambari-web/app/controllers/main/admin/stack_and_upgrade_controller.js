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

App.MainAdminStackAndUpgradeController = Em.Controller.extend({
  name: 'mainAdminStackAndUpgradeController',

  /**
   * @type {Object|null}
   */
  serviceToInstall: null,

  /**
   * @type {Array}
   */
  upgradeGroups: [],

  /**
   * TODO should have actual value from call that start upgrade
   * @type {Number|null}
   */
  upgradeId: 1,

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
    var current = data.items.findProperty('ClusterStackVersions.state', 'CURRENT');
    var currentVersion = current.repository_versions[0].RepositoryVersions.repository_version;
    var targetVersions = data.items.without(current)
      .filter(function (version) {
        var repositoryVersion = version.repository_versions[0].RepositoryVersions.repository_version;
        //Only higher versions that have already been installed to all the hosts are shown
        return (version.ClusterStackVersions.state === 'INSTALLED' &&
               stringUtils.compareVersions(repositoryVersion, currentVersion) === 1);
      }).map(function (version) {
        return version.ClusterStackVersions;
      });

    this.set('currentVersion', current.ClusterStackVersions);
    this.set('targetVersions', targetVersions);
  },

  /**
   * load upgrade tasks by upgrade id
   * @return {$.ajax}
   */
  loadUpgradeData: function () {
    return App.ajax.send({
      name: 'admin.upgrade.data',
      sender: this,
      data: {
        id: this.get('upgradeId')
      },
      success: 'loadUpgradeDataSuccessCallback'
    });
  },

  /**
   * parse and push upgrade tasks to controller
   * @param data
   */
  loadUpgradeDataSuccessCallback: function (data) {
    this.set("upgradeGroups", data.upgrade_groups);
  },

  /**
   * make call to start downgrade process
   */
  downgrade: function () {
    //TODO start downgrade
  },

  /**
   * make call to start upgrade process and show popup with current progress
   */
  upgrade: function () {
    //TODO start upgrade
    this.loadUpgradeData();
    this.openUpgradeDialog();
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
    //TODO start finalize
  },

  /**
   * show dialog with tasks of upgrade
   * @return {App.ModalPopup}
   */
  openUpgradeDialog: function () {
    App.router.transitionTo('admin.stackUpgrade');
  }
});
