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

App.upgradeWizardView = Em.View.extend({
  controllerBinding: 'App.router.mainAdminStackAndUpgradeController',
  templateName: require('templates/main/admin/stack_upgrade/stack_upgrade_wizard'),

  /**
   * @type {Array}
   */
  failedStatuses: ['HOLDING_FAILED', 'HOLDING_TIMEDOUT', 'FAILED', 'TIMED_OUT'],

  /**
   * @type {Array}
   */
  activeStatuses: ['HOLDING_FAILED', 'HOLDING_TIMEDOUT', 'FAILED', 'TIMED_OUT', 'HOLDING', 'IN_PROGRESS'],

  /**
   * update timer
   * @type {number|null}
   * @default null
   */
  updateTimer: null,

  /**
   * update timer of Upgrade Item
   * @type {number|null}
   * @default null
   */
  upgradeItemTimer: null,

  /**
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * @type {boolean}
   */
  isDetailsOpened: false,

  /**
   * @type {boolean}
   */
  outsideView: true,

  /**
   * Downgrade should be available only if target version higher than current, so we can't downgrade
   * when downgrade already started
   * @type {boolean}
   */
  isDowngradeAvailable: function () {
    return !this.get('controller.isDowngrade') && this.get('controller.downgradeAllowed');
  }.property('controller.isDowngrade', 'controller.downgradeAllowed'),

  /**
   * progress value is rounded to floor
   * @type {number}
   */
  overallProgress: function () {
    return Math.floor(this.get('controller.upgradeData.Upgrade.progress_percent'));
  }.property('controller.upgradeData.Upgrade.progress_percent'),

  /**
   * upgrade groups, reversed and PENDING ones are hidden
   * @type {Array}
   */
  upgradeGroups: function () {
    return this.get('controller.upgradeData.upgradeGroups') || [];
  }.property('controller.upgradeData.upgradeGroups'),

  /**
   * currently active group
   * @type {object|undefined}
   */
  activeGroup: function () {
    return this.get('upgradeGroups').find(function (item) {
      return this.get('activeStatuses').contains(item.get('status'));
    }, this);
  }.property('upgradeGroups.@each.status'),

  /**
   * if upgrade group is in progress it should have currently running item
   * @type {object|undefined}
   */
  runningItem: function () {
    return this.get('activeGroup.upgradeItems') && this.get('activeGroup.upgradeItems').findProperty('status', 'IN_PROGRESS');
  }.property('activeGroup.upgradeItems.@each.status'),

  /**
   * if upgrade group is failed it should have failed item
   * @type {object|undefined}
   */
  failedItem: function () {
    return this.get('activeGroup.upgradeItems') && this.get('activeGroup.upgradeItems').find(function (item) {
      return this.get('failedStatuses').contains(item.get('status'));
    }, this);
  }.property('activeGroup.upgradeItems.@each.status'),

  /**
   * upgrade doesn't have any failed or manual or running item
   * @type {boolean}
   */
  noActiveItem: function () {
    return (Em.isNone(this.get('failedItem')) && Em.isNone(this.get('runningItem')) && Em.isNone(this.get('manualItem'))) &&
      !['INIT', 'COMPLETED', 'ABORTED'].contains(App.get('upgradeState'));
  }.property('failedItem', 'runningItem', 'manualItem', 'App.upgradeState'),

  /**
   * details of currently active task
   * @type {object|null}
   */
  taskDetails: function () {
    if (this.get('runningItem')) {
      return this.get('runningItem').get('tasks').findProperty('status', 'IN_PROGRESS');
    } else if (this.get('failedItem')) {
      return this.get('failedItem').get('tasks').find(function (task) {
        return this.get('failedStatuses').contains(task.get('status'));
      }, this);
    } else {
      return null;
    }
  }.property('failedItem.tasks.@each.status', 'runningItem.tasks.@each.status'),

  /**
   * indicate whether failed item can be skipped or retried in order to continue Upgrade
   * @type {boolean}
   */
  isHoldingState: function () {
    return Boolean(this.get('failedItem.status') && this.get('failedItem.status').contains('HOLDING'));
  }.property('failedItem.status'),

  /**
   * @type {boolean}
   */
  isManualDone: false,

  /**
   * if manualItem has been switched then isManualDone flag should be reset
   */
  resetManualDone: function() {
    this.set('isManualDone', false);
  }.observes('manualItem'),

  /**
   * @type {boolean}
   */
  isManualProceedDisabled: function () {
    return !this.get('isManualDone') || this.get('controller.requestInProgress');
  }.property('isManualDone'),

  /**
   * if upgrade group is manual it should have manual item
   * @type {object|undefined}
   */
  manualItem: function () {
    return this.get('activeGroup.upgradeItems') && this.get('activeGroup.upgradeItems').findProperty('status', 'HOLDING');
  }.property('activeGroup.upgradeItems.@each.status'),

  /**
   * plain manual item
   * @type {object|undefined}
   */
  plainManualItem: function () {
    return this.get('manualItem') && ![
        this.get('controller.finalizeContext'),
        this.get('controller.resolveHostsContext'),
        this.get("controller.slaveFailuresContext"),
        this.get("controller.serviceCheckFailuresContext")
      ].contains(this.get('manualItem.context'));
  }.property('manualItem.context'),

  /**
   * manualItem: indicate whether the step is "Slave component failures", a dialog with instructions will show up for manual steps
   * @type {boolean}
   */
  isSlaveComponentFailuresItem: function () {
    var item = this.get('activeGroup.upgradeItems') && this.get('activeGroup.upgradeItems').findProperty('context', this.get("controller.slaveFailuresContext"));
    return item && ['HOLDING', 'HOLDING_FAILED'].contains(item.get('status'));
  }.property('activeGroup.upgradeItems.@each.status', 'activeGroup.upgradeItems.@each.context'),

  /**
   * manualItem: indicate whether the step is "Service check failures", a dialog with instructions will show up for manual steps
   * @type {boolean}
   */
  isServiceCheckFailuresItem: function () {
    return this.get('manualItem.context') === this.get("controller.serviceCheckFailuresContext");
  }.property('manualItem.context'),

  /**
   * manualItem: indicate whether the step is Finalize
   * @type {boolean}
   */
  isFinalizeItem: function () {
    return this.get('manualItem.context') === this.get('controller.finalizeContext');
  }.property('manualItem.context'),

  /**
   * indicate whether the step is Resolve Hosts
   * @type {boolean}
   */
  isResolveHostsItem: function () {
    return this.get('manualItem.context') === this.get('controller.resolveHostsContext');
  }.property('manualItem.context'),

  /**
   * @type {string}
   */
  failedHostsMessage: function() {
    var count = this.get('controller.slaveComponentStructuredInfo.hosts.length') || 0;
    return Em.I18n.t('admin.stackUpgrade.failedHosts.showHosts').format(count);
  }.property('controller.slaveComponentStructuredInfo.hosts'),

  /**
   * label of Upgrade status
   * @type {string}
   */
  upgradeStatusLabel: function() {
    var labelKey = null;
    switch (this.get('controller.upgradeData.Upgrade.request_status')) {
      case 'QUEUED':
      case 'PENDING':
      case 'IN_PROGRESS':
        labelKey = 'admin.stackUpgrade.state.inProgress';
        break;
      case 'COMPLETED':
        labelKey = 'admin.stackUpgrade.state.completed';
        break;
      case 'ABORTED':
        labelKey = 'admin.stackUpgrade.state.paused';
        break;
      case 'TIMEDOUT':
      case 'FAILED':
      case 'HOLDING_FAILED':
      case 'HOLDING_TIMEDOUT':
      case 'HOLDING':
        labelKey = 'admin.stackUpgrade.state.paused';
        break;
    }
    if (labelKey) {
      labelKey += (this.get('controller.isDowngrade')) ? '.downgrade' : "";
      return Em.I18n.t(labelKey);
    } else {
      return "";
    }
  }.property('controller.upgradeData.Upgrade.request_status', 'controller.isDowngrade'),

  /**
   * load versions page
   */
  loadVersionPage: function () {
    var controller = this.get('controller');
    if (!controller.get('isLoaded') && App.get('clusterName')) {
      controller.load().done(function () {
        controller.set('isLoaded', true);
      });
    }
  }.observes('App.clusterName'),

  /**
   * toggle details box
   */
  toggleDetails: function () {
    this.toggleProperty('isDetailsOpened');
  },

  /**
   * close details block if no active task present
   */
  closeDetails: function () {
    if (this.get('noActiveItem')) {
      this.set('isDetailsOpened', false);
    }
  }.observes('noActiveItem'),

  /**
   * start polling upgrade data
   */
  startPolling: function () {
    var self = this;
    if (App.get('clusterName')) {
      this.get('controller').loadUpgradeData().done(function () {
        self.set('isLoaded', true);
        self.doPolling();
      });
    }
  }.observes('App.clusterName'),

  getSkippedServiceChecks: function () {
    if (this.get('isFinalizeItem')) {
      if (!this.get('controller.areSkippedServiceChecksLoaded')) {
        var self = this;
        App.ajax.send({
          name: 'admin.upgrade.service_checks',
          sender: this,
          data: {
            upgradeId: this.get('controller.upgradeId')
          },
          success: 'getSkippedServiceChecksSuccessCallback'
        }).complete(function () {
            self.set('controller.areSkippedServiceChecksLoaded', true);
          });
      }
    } else {
      this.set('controller.areSkippedServiceChecksLoaded', false);
    }
  }.observes('isFinalizeItem'),

  getSkippedServiceChecksSuccessCallback: function (data) {
    if (data.items && data.items.length) {
      var lastItemWithChecks = data.items[data.items.length - 1];
      if (lastItemWithChecks && lastItemWithChecks.upgrade_items && lastItemWithChecks.upgrade_items.length) {
        var skippedServiceChecks = [];
        lastItemWithChecks.upgrade_items.forEach(function (item) {
          if (item.tasks && item.tasks.length) {
            item.tasks.forEach(function (task) {
              var detail = Em.get(task, 'Tasks.command_detail');
              if (detail && detail.startsWith('SERVICE_CHECK ')) {
                skippedServiceChecks.push(App.format.role(detail.replace('SERVICE_CHECK ', ''), true));
              }
            });
          }
        });
        skippedServiceChecks = skippedServiceChecks.uniq();
        this.set('controller.skippedServiceChecks', skippedServiceChecks);
      }
    }
  },

  /**
   * get slave-component failure hosts
   */
  getSlaveComponentItem: function() {
    var controller = this.get('controller');
    if (this.get('isSlaveComponentFailuresItem')) {
      if (!this.get('controller.areSlaveComponentFailuresHostsLoaded')) {
        var item = this.get('activeGroup.upgradeItems') && this.get('activeGroup.upgradeItems').findProperty('context', this.get("controller.slaveFailuresContext"));
        controller.getUpgradeItem(item, 'getSlaveComponentItemSuccessCallback').complete(function () {
          controller.set('areSlaveComponentFailuresHostsLoaded', true);
        });
      }
    } else {
      controller.set('areSlaveComponentFailuresHostsLoaded', false);
    }
  }.observes('isSlaveComponentFailuresItem'),

  /**
   * get service names of Service Check failures
   */
  getServiceCheckItem: function() {
    var controller = this.get('controller');
    if (this.get('isServiceCheckFailuresItem')) {
      if (!this.get('controller.areServiceCheckFailuresServicenamesLoaded')) {
        controller.getUpgradeItem(this.get('manualItem'), 'getServiceCheckItemSuccessCallback').complete(function () {
            controller.set('areServiceCheckFailuresServicenamesLoaded', true);
          });
      }
    } else {
      controller.set('areServiceCheckFailuresServicenamesLoaded', false);
    }
  }.observes('isServiceCheckFailuresItem'),

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
    clearTimeout(this.get('upgradeItemTimer'));
    this.set('isLoaded', false);
  },

  /**
   * load upgrade data with time interval
   */
  doPolling: function () {
    var self = this;
    this.set('updateTimer', setTimeout(function () {
      self.get('controller').loadUpgradeData().done(function() {
        self.doPolling();
      });
    }, App.bgOperationsUpdateInterval));
  },

  /**
   * poll for tasks when item is expanded
   */
  doUpgradeItemPolling: function () {
    var self = this;
    var item = this.get('runningItem') || this.get('failedItem');

    if (item && this.get('isDetailsOpened')) {
      this.get('controller').getUpgradeItem(item).complete(function () {
        self.set('upgradeItemTimer', setTimeout(function () {
          self.doUpgradeItemPolling();
        }, App.bgOperationsUpdateInterval));
      });
    } else {
      clearTimeout(this.get('upgradeItemTimer'));
    }
  }.observes('isDetailsOpened'),

  /**
   * set current upgrade item state to FAILED (for HOLDING_FAILED) or TIMED_OUT (for HOLDING_TIMED_OUT)
   * in order to ignore fail and continue Upgrade
   * @param {object} event
   */
  continue: function (event) {
    this.get('controller').setUpgradeItemStatus(event.context, event.context.get('status').slice(8));
    this.set('isDetailsOpened', false);
  },

  /**
   * set current upgrade item state to PENDING in order to retry Upgrade
   * @param {object} event
   */
  retry: function (event) {
    this.get('controller').setUpgradeItemStatus(event.context, 'PENDING');
    this.set('isDetailsOpened', false);
  },

  /**
   * set current upgrade item state to COMPLETED in order to proceed
   * @param {object} event
   */
  complete: function (event) {
    this.get('controller').setUpgradeItemStatus(event.context, 'COMPLETED');
    this.set('isManualDone', false);
  },

  pauseUpgrade: function() {
    var self = this;
    this.get('controller').suspendUpgrade().done(function() {
      self.get('parentView').closeWizard();
    });
  },

  /**
   * pause upgrade confirmation popup
   * @param {object} event
   */
  confirmPauseUpgrade: function () {
    var self = this;
    return App.showConfirmationPopup(
        function() {
          self.pauseUpgrade();
        },
        Em.I18n.t('admin.stackUpgrade.pauseUpgrade.warning').format("upgrade"),
        null,
        Em.I18n.t('common.warning'),
        Em.I18n.t('admin.stackUpgrade.pauseUpgrade')
    );
  },

  /**
   * pause downgrade confirmation popup
   * @param {object} event
   */
  confirmPauseDowngrade: function () {
    var self = this;
    return App.showConfirmationPopup(
        function() {
          self.pauseUpgrade();
        },
        Em.I18n.t('admin.stackUpgrade.pauseUpgrade.warning').format("downgrade"),
        null,
        Em.I18n.t('common.warning'),
        Em.I18n.t('admin.stackUpgrade.pauseDowngrade')
    );
  },

  /**
   * Popup that show
   * @returns {App.ModalPopup}
   */
  showFailedHosts: function() {
    return App.ModalPopup.show({
      header: Em.I18n.t('admin.stackUpgrade.failedHosts.header'),
      bodyClass: App.FailedHostsPopupBodyView,
      secondary: null,
      primary: Em.I18n.t('common.close'),
      content: this.get('controller.slaveComponentStructuredInfo')
    });
  }
});
