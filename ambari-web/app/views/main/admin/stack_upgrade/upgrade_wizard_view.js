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

App.upgradeWizardView = Em.View.extend({
  controllerBinding: 'App.router.mainAdminStackAndUpgradeController',
  templateName: require('templates/main/admin/stack_upgrade/stack_upgrade_dialog'),

  /**
   * update timer
   * @type {number|null}
   * @default null
   */
  updateTimer: null,

  /**
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * string format: width:<number>%;
   * @type {string}
   */
  progressWidth: function () {
    return "width:" + this.get('overallProgress') + "%;";
  }.property('overallProgress'),

  /**
   * progress value is rounded to floor
   * @type {number}
   */
  overallProgress: function () {
    return Math.floor(this.get('controller.upgradeData.Upgrade.progress_percent'));
  }.property('controller.upgradeData.Upgrade.progress_percent'),

  /**
   * label of Upgrade status
   * @type {string}
   */
  upgradeStatusLabel: function() {
    switch (this.get('controller.upgradeData.Upgrade.request_status')) {
      case 'PENDING':
      case 'IN_PROGRESS':
        return Em.I18n.t('admin.stackUpgrade.state.inProgress');
      case 'COMPLETED':
        return Em.I18n.t('admin.stackUpgrade.state.completed');
      case 'FAILED':
      case 'HOLDING':
        return Em.I18n.t('admin.stackUpgrade.state.paused');
      default:
        return ""
    }
  }.property('controller.upgradeData.Upgrade.request_status'),

  /**
   * start polling upgrade data
   */
  startPolling: function () {
    var self = this;
    if (App.get('clusterName')) {
      this.get('controller').loadUpgradeData().done(function () {
        self.set('isLoaded', true);
      });
      this.doPolling();
    }
  }.observes('App.clusterName'),

  /**
   * start polling upgrade data
   */
  willInsertElement: function () {
    this.startPolling();
  },

  /**
   * stop polling upgrade data
   */
  willDestroyElement: function () {
    clearTimeout(this.get('updateTimer'));
    this.set('isLoaded', false);
  },

  /**
   * load upgrade data with time interval
   */
  doPolling: function () {
    var self = this;
    this.set('updateTimer', setTimeout(function () {
      self.get('controller').loadUpgradeData();
      self.doPolling();
    }, App.bgOperationsUpdateInterval));
  }
});
