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

const date = require('utils/date/date');
const numberUtils = require('utils/number_utils');

function diskPart(i18nKey, totalKey, usedKey) {
  return Em.computed(totalKey, usedKey, function () {
    const text = Em.I18n.t(i18nKey),
      total = this.get(totalKey),
      used = this.get(usedKey);
    return text.format(numberUtils.bytesToSize(used, 1, 'parseFloat'), numberUtils.bytesToSize(total, 1, 'parseFloat'));
  });
}

function diskPartPercent(i18nKey, totalKey, usedKey) {
  return Em.computed(totalKey, usedKey, function () {
    const text = Em.I18n.t(i18nKey),
      total = this.get(totalKey),
      used = this.get(usedKey);
    let percent = total > 0 ? ((used * 100) / total).toFixed(2) : 0;
    if (percent == 'NaN' || percent < 0) {
      percent = Em.I18n.t('services.service.summary.notAvailable') + ' ';
    }
    return text.format(percent);
  });
}

App.HDFSSummaryWidgetsView = Em.View.extend(App.NameNodeWidgetMixin, {

  templateName: require('templates/main/service/info/summary/hdfs/widgets'),

  nameSpace: 'default',

  model: function () {
    return App.HDFSService.find().objectAt(0);
  }.property('controller.content.serviceName'),

  subGroupId: Em.computed.alias('nameSpace'),

  nodeUptime: function () {
    const uptime = this.get('model.nameNodeStartTimeValues')[this.get('hostName')];
    if (uptime && uptime > 0) {
      let diff = App.dateTime() - uptime;
      if (diff < 0) {
        diff = 0;
      }
      const formatted = date.timingFormat(diff);
      return this.t('dashboard.services.uptime').format(formatted);
    }
    return this.t('services.service.summary.notRunning');
  }.property('model.nameNodeStartTimeValues'),

  jvmMemoryHeapUsed: Em.computed.getByKey('model.jvmMemoryHeapUsedValues', 'hostName'),

  jvmMemoryHeapMax: Em.computed.getByKey('model.jvmMemoryHeapMaxValues', 'hostName'),

  nodeHeapPercent: App.MainDashboardServiceView.formattedHeapPercent(
    'dashboard.services.hdfs.nodes.heapUsedPercent', 'jvmMemoryHeapUsed', 'jvmMemoryHeapMax'
  ),

  nodeHeap: App.MainDashboardServiceView.formattedHeap(
    'dashboard.services.hdfs.nodes.heapUsed', 'jvmMemoryHeapUsed', 'jvmMemoryHeapMax'
  ),

  capacityTotal: Em.computed.getByKey('model.capacityTotalValues', 'hostName'),

  capacityUsed: Em.computed.getByKey('model.capacityUsedValues', 'hostName'),

  capacityRemaining: Em.computed.getByKey('model.capacityRemainingValues', 'hostName'),

  dfsUsedDiskPercent: diskPartPercent('dashboard.services.hdfs.capacityUsedPercent', 'capacityTotal', 'capacityUsed'),

  dfsUsedDisk: diskPart('dashboard.services.hdfs.capacityUsed', 'capacityTotal', 'capacityUsed'),

  nonDfsUsed: function () {
    const total = this.get('capacityTotal'),
      remaining = this.get('capacityRemaining'),
      dfsUsed = this.get('capacityUsed');
    return (Em.isNone(total) || Em.isNone(remaining) || Em.isNone(dfsUsed)) ? null : total - remaining - dfsUsed;
  }.property('capacityTotal', 'capacityRemaining', 'capacityUsed'),

  nonDfsUsedDiskPercent: diskPartPercent('dashboard.services.hdfs.capacityUsedPercent', 'capacityTotal', 'nonDfsUsed'),

  nonDfsUsedDisk: diskPart('dashboard.services.hdfs.capacityUsed', 'capacityTotal', 'nonDfsUsed'),

  remainingDiskPercent: diskPartPercent(
    'dashboard.services.hdfs.capacityUsedPercent', 'capacityTotal', 'capacityRemaining'
  ),

  remainingDisk: diskPart('dashboard.services.hdfs.capacityUsed', 'capacityTotal', 'capacityRemaining'),

  dfsTotalBlocksValue: Em.computed.getByKey('model.dfsTotalBlocksValues', 'hostName'),

  dfsTotalBlocks: Em.computed.formatUnavailable('dfsTotalBlocksValue'),

  dfsCorruptBlocksValue: Em.computed.getByKey('model.dfsTotalBlocksValues', 'hostName'),

  dfsCorruptBlocks: Em.computed.formatUnavailable('dfsCorruptBlocksValue'),

  dfsMissingBlocksValue: Em.computed.getByKey('model.dfsMissingBlocksValues', 'hostName'),

  dfsMissingBlocks: Em.computed.formatUnavailable('dfsMissingBlocksValue'),

  dfsUnderReplicatedBlocksValue: Em.computed.getByKey('model.dfsUnderReplicatedBlocksValues', 'hostName'),

  dfsUnderReplicatedBlocks: Em.computed.formatUnavailable('dfsUnderReplicatedBlocksValue'),

  dfsTotalFilesValue: Em.computed.getByKey('model.dfsTotalFilesValues', 'hostName'),

  dfsTotalFiles: Em.computed.formatUnavailable('model.dfsTotalFilesValue'),

  healthStatus: Em.computed.getByKey('model.healthStatusValues', 'hostName'),

  upgradeStatusValue: Em.computed.getByKey('model.upgradeStatusValues', 'hostName'),

  upgradeStatus: function () {
    const upgradeStatus = this.get('upgradeStatusValue'),
      healthStatus = this.get('healthStatus');
    if (upgradeStatus) {
      return Em.I18n.t('services.service.summary.pendingUpgradeStatus.notPending');
    } else if (upgradeStatus === false && healthStatus === 'green') {
      return Em.I18n.t('services.service.summary.pendingUpgradeStatus.notFinalized');
    } else {
      // upgrade status == null
      return Em.I18n.t('services.service.summary.notAvailable');
    }
  }.property('upgradeStatusValue', 'healthStatus'),

  isUpgradeStatusWarning: function () {
    return this.get('upgradeStatusValue') === false && this.get('healthStatus') === 'green';
  }.property('upgradeStatusValue', 'healthStatus'),

  safeModeStatusValue: Em.computed.getByKey('model.safeModeStatusValues', 'hostName'),

  safeModeStatus: function () {
    const safeMode = this.get('safeModeStatusValue');
    if (Em.isNone(safeMode)) {
      return Em.I18n.t('services.service.summary.notAvailable');
    } else if (safeMode.length === 0) {
      return Em.I18n.t('services.service.summary.safeModeStatus.notInSafeMode');
    } else {
      return Em.I18n.t('services.service.summary.safeModeStatus.inSafeMode');
    }
  }.property('safeModeStatusValue'),

  isNonFederatedHDFS: Em.computed.lt('model.masterComponentGroups.length', 2)

});
