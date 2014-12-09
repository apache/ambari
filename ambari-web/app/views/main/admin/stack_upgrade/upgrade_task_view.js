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

App.upgradeTaskView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/upgrade_task'),

  /**
   * relation map between status and icon class
   * @type {Object}
   */
  statusIconMap: {
    'COMPLETED': 'icon-ok',
    'WARNING': 'icon-warning-sign',
    'FAILED': 'icon-warning-sign',
    'PENDING': 'icon-cog',
    'IN_PROGRESS': 'icon-cogs'
  },

  /**
   * @type {Boolean}
   */
  isManualDone: false,

  /**
   * @type {Boolean}
   */
  isManualProceedDisabled: function () {
    return !this.get('isManualDone');
  }.property('isManualDone'),

  /**
   * @type {string}
   */
  iconClass: function () {
    return this.get('statusIconMap')[this.get('content.UpgradeGroup.status')] || 'icon-question-sign';
  }.property('content.UpgradeGroup.status'),

  /**
   * @type {Boolean}
   */
  isFailed: function () {
    return this.get('content.UpgradeGroup.status') === 'FAILED';
  }.property('content.UpgradeGroup.status'),

  /**
   * @type {Boolean}
   */
  showProgressBar: function () {
    return ['IN_PROGRESS', 'FAILED'].contains(this.get('content.UpgradeGroup.status')) && this.get('content.UpgradeGroup.type') !== 'manual';
  }.property('content.UpgradeGroup.status'),

  /**
   * @type {Boolean}
   */
  isInProgress: function () {
    return this.get('content.UpgradeGroup.status') === 'IN_PROGRESS' && this.get('content.UpgradeGroup.type') !== 'manual';
  }.property('content.UpgradeGroup.status'),

  /**
   * width style of progress bar
   * @type {String}
   */
  progressWidth: function () {
    return "width:" + Math.floor(this.get('progress')) + '%;';
  }.property('content.UpgradeGroup.progress_percent'),

  /**
   * @type {number}
   */
  progress: function () {
    return Math.floor(this.get('content.UpgradeGroup.progress_percent'))
  }.property('content.UpgradeGroup.progress_percent'),

  /**
   * if upgrade group is in progress it should have currently running item
   * @type {Object|null}
   */
  runningItem: function () {
    return this.get('content.upgrade_items').findProperty('UpgradeItem.status', 'IN_PROGRESS');
  }.property('content.upgrade_items.@each.UpgradeItem.status'),

  /**
   * if upgrade group is failed it should have failed item
   * @type {Object|null}
   */
  failedItem: function () {
    return this.get('content.upgrade_items').findProperty('UpgradeItem.status', 'FAILED');
  }.property('content.upgrade_items.@each.UpgradeItem.status'),

  /**
   * @type {Boolean}
   */
  isManualOpened: function () {
    //TODO modify logic according to actual API
    return this.get('content.UpgradeGroup.status') === 'IN_PROGRESS' && this.get('content.UpgradeGroup.type') === 'manual'
  }.property('content.UpgradeGroup.status', 'content.UpgradeGroup.type')
});
