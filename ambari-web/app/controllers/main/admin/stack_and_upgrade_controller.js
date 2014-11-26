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

App.MainAdminStackAndUpgradeController = Em.Controller.extend({
  name: 'mainAdminStackAndUpgradeController',

  serviceToInstall: null,
  upgradeTasks: [],

  /**
   * version that currently applied to server
   */
  currentVersion: null,
  /**
   * versions to which cluster could be upgraded
   */
  targetVersions: [],

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
    var target = data.items.without(current);

    this.set('currentVersion', current.ClusterStackVersions);
    this.set('targetVersions', target.map(function (ver) {
      return ver.ClusterStackVersions;
    }));
  },

  /**
   * load upgrade tasks by upgrade id
   * @return {$.ajax}
   */
  loadUpgradeTasks: function () {
    //TODO should make call with actual upgrade id
    var upgradeId = 1;
    return App.ajax.send({
      name: 'admin.upgrade.tasks',
      sender: this,
      data: {
        id: upgradeId
      },
      success: 'loadUpgradeTasksSuccessCallback'
    });
  },

  /**
   * parse and push upgrade tasks to controller
   * @param data
   */
  loadUpgradeTasksSuccessCallback: function (data) {
    this.set("upgradeTasks", data.items.map(function (item) {
      return item.UpgradeItem;
    }));
  },

  /**
   * start cluster downgrade
   */
  downgrade: function () {
    //TODO start actual downgrade
  },
  /**
   * start cluster upgrade
   */
  upgrade: function () {
    //TODO start actual upgrade
    this.loadUpgradeTasks();
    this.openUpgradeDialog();
  },
  /**
   * resume upgrade process
   */
  resumeUpgrade: function () {
    //TODO resume upgrade
  },
  /**
   * finish upgrade process
   */
  finalize: function () {
    //TODO start actual finalize
  },

  /**
   * show dialog with tasks of upgrade
   * @return {App.ModalPopup}
   */
  openUpgradeDialog: function () {
    var upgradeVersion = 'HDP-2.2.1';

    return App.ModalPopup.show({
      header: Em.I18n.t('admin.stackUpgrade.dialog.header').format(upgradeVersion),
      bodyClass: Em.View.extend({
        controller: this,
        templateName: require('templates/main/admin/stack_upgrade/stack_upgrade_dialog'),
        tasks: function () {
          return this.get('controller.upgradeTasks');
        }.property('controller.upgradeTasks')
      }),
      primary: null,
      secondary: null
    });
  }
});
