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
var date = require('utils/date');

App.MainDashboardServiceHdfsView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/dashboard/service/hdfs'),
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
      var diff = (new Date()).getTime() - uptime;
      if (diff < 0) {
        diff = 0;
      }
      var formatted = date.timingFormat(diff);
      return this.t('dashboard.services.uptime').format(formatted);
    }
    return this.t('services.service.summary.notRunning');
  }.property("service.nameNodeStartTime"),

  nodeWebUrl: function () {
    return "http://" + this.get('service').get('nameNode').get('publicHostName') + ":50070";
  }.property('service.nameNode'),

  nodeHeap: function () {
    var memUsed = this.get('service').get('jvmMemoryHeapUsed') * 1000000;
    var memCommitted = this.get('service').get('jvmMemoryHeapCommitted') * 1000000;
    var percent = memCommitted > 0 ? ((100 * memUsed) / memCommitted) : 0;
    return this.t('dashboard.services.hdfs.nodes.heapUsed').format(memUsed.bytesToSize(1, 'parseFloat'), memCommitted.bytesToSize(1, 'parseFloat'), percent.toFixed(1));

  }.property('service.jvmMemoryHeapUsed', 'service.jvmMemoryHeapCommitted'),

  summaryHeader: function () {
    var text = this.t("dashboard.services.hdfs.summary");
    var svc = this.get('service');
    var liveCount = svc.get('liveDataNodes').get('length');
    var totalCount = svc.get('dataNodes').get('length');
    var total = this.get('service.capacityTotal') + 0;
    var remaining = this.get('service.capacityRemaining') + 0;
    var used = total - remaining;
    var percent = total > 0 ? ((used * 100) / total).toFixed(1) : 0;
    if (percent == "NaN" || percent < 0) {
      percent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    return text.format(liveCount, totalCount, percent);
  }.property('service.liveDataNodes', 'service.dataNodes', 'service.capacityUsed', 'service.capacityTotal'),

  capacity: function () {
    var text = this.t("dashboard.services.hdfs.capacityUsed");
    var total = this.get('service.capacityTotal') + 0;
    var remaining = this.get('service.capacityRemaining') + 0;
    var used = total - remaining;
    var percent = total > 0 ? ((used * 100) / total).toFixed(1) : 0;
    if (percent == "NaN" || percent < 0) {
      percent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    if (used < 0) {
      used = 0;
    }
    if (total < 0) {
      total = 0;
    }
    return text.format(used.bytesToSize(1, 'parseFloat'), total.bytesToSize(1, 'parseFloat'), percent);
  }.property('service.capacityUsed', 'service.capacityTotal'),

  dataNodeComponent: function () {
    return App.HostComponent.find().findProperty('componentName', 'DATANODE');
  }.property(),

  isSafeMode: function () {
    var safeMode = this.get('service.safeModeStatus');
    return safeMode != null && safeMode.length > 0;
  }.property('service.safeModeStatus')
});