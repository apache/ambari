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

App.upgradeGroupView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/upgrade_group'),

  /**
   * @type {boolean}
   */
  isManualDone: false,

  /**
   * progress info is a box that show running UpgradeItem
   * @type {boolean}
   */
  showProgressInfo: function () {
    return this.get('content.isRunning') && this.get('runningItem');
  }.property('content.isRunning', 'runningItem'),

  /**
   * @type {boolean}
   */
  isManualProceedDisabled: function () {
    return !this.get('isManualDone');
  }.property('isManualDone'),

  /**
   * @type {boolean}
   */
  isFailed: function () {
    return this.get('content.status') === 'FAILED';
  }.property('content.status'),

  /**
   * if upgrade group is in progress it should have currently running item
   * @type {object|undefined}
   */
  runningItem: function () {
    return this.get('content.upgradeItems').findProperty('status', 'IN_PROGRESS');
  }.property('content.upgradeItems.@each.status'),

  /**
   * if upgrade group is failed it should have failed item
   * @type {object|undefined}
   */
  failedItem: function () {
    return this.get('content.upgradeItems').findProperty('status', 'FAILED');
  }.property('content.upgradeItems.@each.status'),

  /**
   * @type {boolean}
   */
  isManualOpened: function () {
    return this.get('content.status') === 'HOLDING';
  }.property('content.status'),

  /**
   * Only one UpgradeGroup or UpgradeItem could be expanded at a time
   * @param {object} event
   */
  toggleExpanded: function (event) {
    event.contexts[1].forEach(function (item) {
      if (item == event.context) {
        item.set('isExpanded', !event.context.get('isExpanded'));
      } else {
        item.set('isExpanded', false);
      }
    });
  },

  /**
   *
   * @param {object} event
   */
  copyErrLog: function(event) {
    event.context.toggleProperty('errorLogOpened');
  },

  /**
   *
   * @param {object} event
   */
  openLogWindow: function(event) {
    var newWindow = window.open();
    var newDocument = newWindow.document;
    newDocument.write(event.context);
    newDocument.close();
  },

  /**
   *
   * @param {object} event
   */
  copyOutLog: function(event) {
    event.context.toggleProperty('outputLogOpened');
  }
});
