/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
var date = require('utils/date/date');
var numberUtils = require('utils/number_utils');

App.MainDashboardServiceHdfsView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/service/services/hdfs'),
  serviceName: 'HDFS',
  Chart: App.ChartPieView.extend({
    service: null,
    color: '#0066B3',
    stroke: '#0066B3',
    palette: new Rickshaw.Color.Palette({
      scheme: [ 'rgba(0,102,179,0)', 'rgba(0,102,179,1)'].reverse()
    }),
    data: function () {
      var total = this.get('service.capacityTotal') + 0;
      var remaining = (this.get('service.capacityRemaining') + 0);
      var used = total - remaining;
      return [ used, remaining ];
    }.property('service.capacityUsed', 'service.capacityTotal')
  }),

  dashboardMasterComponentView: Em.View.extend({
    didInsertElement: function() {
      App.tooltip($('[rel=healthTooltip]'));
    },
    templateName: require('templates/main/service/info/summary/master_components'),
    mastersComp: function() {
      var masterComponents = [];
      var zkfcs = this.get('parentView.service.hostComponents').filterProperty('componentName', 'ZKFC');

      this.get('parentView.service.hostComponents').forEach(function (comp) {
        if (comp.get('isMaster') && comp.get('componentName') !== 'JOURNALNODE') {
          masterComponents.push(comp);
          var zkfc = zkfcs.findProperty('hostName', comp.get('hostName'));
          if (zkfc) {
            zkfc.set('isSubComponent', true);
            masterComponents.push(zkfc);
          }
        }
      });
      return masterComponents;
    }.property('parentView.service.hostComponents.length'),
    willDestroyElement: function() {
      $('[rel=healthTooltip]').tooltip('destroy')
    }
  }),

  didInsertElement: function() {
    App.tooltip($("[rel='tooltip']"));
  },

  willDestroyElement: function() {
    $("[rel='tooltip']").tooltip('destroy');
  },

  dataNodesDead: function () {
    return this.get('service.dataNodesInstalled');
  }.property('service.dataNodesInstalled'),

  showJournalNodes: function () {
    return this.get('service.journalNodes.length') > 0;
  }.property('service.journalNodes.length'),

  journalNodesLive: function () {
    return this.get('service.journalNodes').filterProperty("workStatus", "STARTED").get("length");
  }.property("service.journalNodes.@each.workStatus"),

  journalNodesTotal: function () {
    return this.get('service.journalNodes').get("length");
  }.property("service.journalNodes.length"),

  dfsTotalBlocks: function(){
    return this.formatUnavailable(this.get('service.dfsTotalBlocks'));
  }.property('service.dfsTotalBlocks'),
  dfsTotalFiles: function(){
    return this.formatUnavailable(this.get('service.dfsTotalFiles'));
  }.property('service.dfsTotalFiles'),
  dfsCorruptBlocks: function(){
    return this.formatUnavailable(this.get('service.dfsCorruptBlocks'));
  }.property('service.dfsCorruptBlocks'),
  dfsMissingBlocks: function(){
    return this.formatUnavailable(this.get('service.dfsMissingBlocks'));
  }.property('service.dfsMissingBlocks'),
  dfsUnderReplicatedBlocks: function(){
    return this.formatUnavailable(this.get('service.dfsUnderReplicatedBlocks'));
  }.property('service.dfsUnderReplicatedBlocks'),

  blockErrorsMessage: function() {
    return Em.I18n.t('dashboard.services.hdfs.blockErrors').format(this.get('dfsCorruptBlocks'), this.get('dfsMissingBlocks'), this.get('dfsUnderReplicatedBlocks'));
  }.property('dfsCorruptBlocks','dfsMissingBlocks','dfsUnderReplicatedBlocks'),

  nodeUptime: function () {
    var uptime = this.get('service').get('nameNodeStartTime');
    if (uptime && uptime > 0){
      var diff = App.dateTime() - uptime;
      if (diff < 0) {
        diff = 0;
      }
      var formatted = date.timingFormat(diff);
      return this.t('dashboard.services.uptime').format(formatted);
    }
    return this.t('services.service.summary.notRunning');
  }.property("service.nameNodeStartTime"),

  nodeWebUrl: function () {
    return "http://" + (App.singleNodeInstall ? App.singleNodeAlias :  this.get('service').get('nameNode').get('publicHostName')) + ":50070";
  }.property('service.nameNode'),

  nodeHeap: function () {
    var memUsed = this.get('service').get('jvmMemoryHeapUsed');
    var memMax = this.get('service').get('jvmMemoryHeapMax');
    var percent = memMax > 0 ? ((100 * memUsed) / memMax) : 0;
    return this.t('dashboard.services.hdfs.nodes.heapUsed').format(
      numberUtils.bytesToSize(memUsed, 1, 'parseFloat'),
      numberUtils.bytesToSize(memMax, 1, 'parseFloat'),
      percent.toFixed(1));
  }.property('service.jvmMemoryHeapUsed', 'service.jvmMemoryHeapMax'),

  dfsUsedDisk: function () {
    var text = this.t("dashboard.services.hdfs.capacityUsed");
    var total = this.get('service.capacityTotal');
    var dfsUsed = this.get('service.capacityUsed');
    var percent = total > 0 ? ((dfsUsed * 100) / total).toFixed(2) : 0;
    if (percent == "NaN" || percent < 0) {
      percent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    return text.format(numberUtils.bytesToSize(dfsUsed, 1, 'parseFloat'), numberUtils.bytesToSize(total, 1, 'parseFloat'), percent);
  }.property('service.capacityUsed', 'service.capacityTotal'),
  nonDfsUsedDisk: function () {
    var text = this.t("dashboard.services.hdfs.capacityUsed");
    var total = this.get('service.capacityTotal');
    var remaining = this.get('service.capacityRemaining');
    var dfsUsed = this.get('service.capacityUsed');
    var nonDfsUsed = total !== null && remaining !== null && dfsUsed !== null ? total - remaining - dfsUsed : null;
    var percent = total > 0 ? ((nonDfsUsed * 100) / total).toFixed(2) : 0;
    if (percent == "NaN" || percent < 0) {
      percent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    return text.format(numberUtils.bytesToSize(nonDfsUsed, 1, 'parseFloat'), numberUtils.bytesToSize(total, 1, 'parseFloat'), percent);
  }.property('service.capacityUsed', 'service.capacityRemaining', 'service.capacityTotal'),
  remainingDisk: function () {
    var text = this.t("dashboard.services.hdfs.capacityUsed");
    var total = this.get('service.capacityTotal');
    var remaining = this.get('service.capacityRemaining');
    var percent = total > 0 ? ((remaining * 100) / total).toFixed(2) : 0;
    if (percent == "NaN" || percent < 0) {
      percent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    return text.format(numberUtils.bytesToSize(remaining, 1, 'parseFloat'), numberUtils.bytesToSize(total, 1, 'parseFloat'), percent);
  }.property('service.capacityRemaining', 'service.capacityTotal'),

  dataNodeComponent: function () {
    return Em.Object.create({
      componentName: 'DATANODE'
    });
    //return this.get('service.dataNodes').objectAt(0);
  }.property(),

  nfsGatewayComponent: function () {
    return Em.Object.create({
      componentName: 'NFS_GATEWAY'
    });
    //return this.get('service.dataNodes').objectAt(0);
  }.property(),

  /**
   * Define if NFS_GATEWAY is present in the installed stack
   * @type {Boolean}
   */
  isNfsInStack: function () {
    return App.StackServiceComponent.find().someProperty('componentName', 'NFS_GATEWAY');
  }.property(),
  
  journalNodeComponent: function () {
    return this.get('service.journalNodes').objectAt(0);
  }.property(),

  safeModeStatus: function () {
    var safeMode = this.get('service.safeModeStatus');
    if (safeMode == null) {
      return Em.I18n.t("services.service.summary.notAvailable");
    } else if (safeMode.length == 0) {
      return Em.I18n.t("services.service.summary.safeModeStatus.notInSafeMode");
    } else {
      return Em.I18n.t("services.service.summary.safeModeStatus.inSafeMode");
    }
  }.property('service.safeModeStatus'),
  upgradeStatus: function () {
    var upgradeStatus = this.get('service.upgradeStatus');
    var healthStatus = this.get('service.healthStatus');
    if (upgradeStatus == 'true') {
      return Em.I18n.t('services.service.summary.pendingUpgradeStatus.notPending');
    } else if (upgradeStatus == 'false' && healthStatus == 'green') {
      return Em.I18n.t('services.service.summary.pendingUpgradeStatus.notFinalized');
    } else {
      // upgrade status == null
      return Em.I18n.t("services.service.summary.notAvailable");
    }
  }.property('service.upgradeStatus', 'service.healthStatus'),
  isUpgradeStatusWarning: function () {
    var upgradeStatus = this.get('service.upgradeStatus');
    var healthStatus = this.get('service.healthStatus');
    return upgradeStatus == 'false' && healthStatus == 'green';
  }.property('service.upgradeStatus', 'service.healthStatus')

});
