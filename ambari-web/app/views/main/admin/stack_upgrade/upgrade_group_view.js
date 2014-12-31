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
   * @type {Array}
   */
  failedStatuses: ['HOLDING_FAILED', 'HOLDING_TIMED_OUT', 'FAILED', 'TIMED_OUT'],

  /**
   * progress info is a box that show running UpgradeItem
   * @type {boolean}
   */
  showProgressInfo: function () {
    return Boolean(this.get('content.isRunning') && this.get('runningItem'));
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
    return Boolean(this.get('failedStatuses').contains(this.get('content.status')) && this.get('failedItem'));
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
    return this.get('content.upgradeItems').find(function (item) {
      return this.get('failedStatuses').contains(item.get('status'));
    }, this);
  }.property('content.upgradeItems.@each.status'),

  /**
   * if upgrade group is manual it should have manual item
   * @type {object|undefined}
   */
  manualItem: function () {
    return this.get('content.upgradeItems').findProperty('status', 'HOLDING');
  }.property('content.upgradeItems.@each.status'),

  /**
   * @type {boolean}
   */
  isManualOpened: function () {
    return Boolean(this.get('manualItem'));
  }.property('manualItem'),

  /**
   * indicate whether failed item can be skipped or retried in order to continue Upgrade
   * @type {boolean}
   */
  isHoldingState: function () {
    return Boolean(this.get('failedItem.status') && this.get('failedItem.status').contains('HOLDING'));
  }.property('failedItem.status'),

  /**
   * set status to Upgrade item
   * @param item
   * @param status
   */
  setUpgradeItemStatus: function(item, status) {
    App.ajax.send({
      name: 'admin.upgrade.upgradeItem.setState',
      sender: this,
      data: {
        upgradeId: item.get('request_id'),
        itemId: item.get('stage_id'),
        groupId: item.get('group_id'),
        status: status
      }
    });
  },

  /**
   * set current upgrade item state to FAILED (for HOLDING_FAILED) or TIMED_OUT (for HOLDING_TIMED_OUT)
   * in order to ignore fail and continue Upgrade
   * @param {object} event
   */
  continue: function (event) {
    this.setUpgradeItemStatus(event.context, event.context.get('status').slice(8));
  },

  /**
   * set current upgrade item state to PENDING in order to retry Upgrade
   * @param {object} event
   */
  retry: function (event) {
    this.setUpgradeItemStatus(event.context, 'PENDING');
  },

  /**
   * set current upgrade item state to COMPLETED in order to proceed
   * @param {object} event
   */
  complete: function (event) {
    this.setUpgradeItemStatus(event.context, 'COMPLETED');
  },

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
