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
  showFailedInfo: function () {
    return this.get('content.status') === 'FAILED' && this.get('failedItem');
  }.property('content.status', 'failedItem'),

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
    var isExpanded = event.context.get('isExpanded');
    event.contexts[1].filterProperty('isExpanded').forEach(function (item) {
      this.collapseLowerLevels(item);
      item.set('isExpanded', false);
    }, this);
    this.collapseLowerLevels(event.context);
    event.context.set('isExpanded', !isExpanded);
  },

  /**
   * collapse sub-entities of current
   * @param {App.upgradeEntity} entity
   */
  collapseLowerLevels: function (entity) {
    if (entity.get('isExpanded')) {
      if (entity.type === 'ITEM') {
        entity.get('tasks').setEach('isExpanded', false);
      } else if (entity.type === 'GROUP') {
        entity.get('upgradeItems').forEach(function (item) {
          this.collapseLowerLevels(item);
          item.set('isExpanded', false);
        }, this);
      }
    }
  }
});
