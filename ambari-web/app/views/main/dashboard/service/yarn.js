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
var numberUtils = require('utils/number_utils');

App.MainDashboardServiceYARNView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/dashboard/service/yarn'),
  serviceName: 'YARN',

  nodeHeap: function () {
    var memUsed = this.get('service').get('jvmMemoryHeapUsed');
    var memCommitted = this.get('service').get('jvmMemoryHeapCommitted');
    var percent = memCommitted > 0 ? ((100 * memUsed) / memCommitted) : 0;
    return this.t('dashboard.services.hdfs.nodes.heapUsed').format(
        numberUtils.bytesToSize(memUsed, 1, 'parseFloat', 1024 * 1024), 
        numberUtils.bytesToSize(memCommitted, 1, 'parseFloat', 1024 * 1024), 
        percent.toFixed(1));

  }.property('service.jvmMemoryHeapUsed', 'service.jvmMemoryHeapCommitted'),

  summaryHeader: function () {
    var text = this.t("dashboard.services.yarn.summary");
    var svc = this.get('service');
    var totalCount = svc.get('nodeManagerNodes').get('length');
    return text.format(totalCount, totalCount);
  }.property('service.nodeManagerNodes'),
  
  nodeManagerComponent: function () {
    return App.HostComponent.find().findProperty('componentName', 'NODEMANAGER');
  }.property('service.hostComponents.@each'),
  
  yarnClientComponent: function () {
    return App.HostComponent.find().findProperty('componentName', 'YARN_CLIENT');
  }.property(),

  hasManyYarnClients: function () {
    return (this.get('service.yarnClientNodes.length') > 1);
  }.property('service.yarnClientNodes.length'),

  nodeUptime: function () {
    var uptime = this.get('service').get('resourceManagerStartTime');
    if (uptime && uptime > 0){
      var diff = (new Date()).getTime() - uptime;
      if (diff < 0) {
        diff = 0;
      }
      var formatted = date.timingFormat(diff);
      return this.t('dashboard.services.uptime').format(formatted);
    }
    return this.t('services.service.summary.notRunning');
  }.property("service.resourceManagerStartTime"),

  nodeManagersLive: function () {
    return App.HostComponent.find().filterProperty('componentName', 'NODEMANAGER').filterProperty("workStatus","STARTED");
  }.property('service.hostComponents.@each'),

  nodeManagerText: function () {
    if(!this.get("nodeManagerComponent") || this.get("nodeManagerComponent.length") == 0){
      return '';
    } else if(this.get("nodeManagerComponent.length") > 1){
      return Em.I18n.t('services.service.summary.viewHosts');
    } else {
      return Em.I18n.t('services.service.summary.viewHost');
    }
  }.property("nodeManagerComponent"),

  nodeManagersLiveTextView: App.ComponentLiveTextView.extend({
    liveComponents: function() {
      return App.HostComponent.find().filterProperty('componentName', 'NODEMANAGER').filterProperty("workStatus","STARTED").get("length");
    }.property("service.hostComponents.@each"),
    totalComponents: function() {
      return App.HostComponent.find().filterProperty('componentName', 'NODEMANAGER').get("length");
    }.property("service.hostComponents.@each")
  }),

  nodeManagersStatus: function () {
    var nmActive = this.get('service.nodeManagersCountActive');
    var nmLost = this.get('service.nodeManagersCountLost');
    var nmUnhealthy = this.get('service.nodeManagersCountUnhealthy');
    var nmRebooted = this.get('service.nodeManagersCountRebooted');
    var nmDecom = this.get('service.nodeManagersCountDecommissioned');
    return this.t('dashboard.services.yarn.nodeManagers.status.msg').format(
      this.formatUnavailable(nmActive),
      this.formatUnavailable(nmLost),
      this.formatUnavailable(nmUnhealthy),
      this.formatUnavailable(nmRebooted),
      this.formatUnavailable(nmDecom)
    );
  }.property('service.nodeManagersCountActive', 'service.nodeManagersCountLost', 
      'service.nodeManagersCountUnhealthy', 'service.nodeManagersCountRebooted', 'service.nodeManagersCountDecommissioned'),

  containers: function () {
    var allocated = this.get('service.containersAllocated');
    var pending = this.get('service.containersPending');
    var reserved = this.get('service.containersReserved');
    return this.t('dashboard.services.yarn.containers.msg').format(
      this.formatUnavailable(allocated),
      this.formatUnavailable(pending),
      this.formatUnavailable(reserved)
    );
  }.property('service.containersAllocated', 'service.containersPending', 'service.containersReserved'),

  apps: function () {
    var appsSubmitted = this.get('service.appsSubmitted');
    var appsRunning = this.get('service.appsRunning');
    var appsPending = this.get('service.appsPending');
    var appsCompleted = this.get('service.appsCompleted');
    var appsKilled = this.get('service.appsKilled');
    var appsFailed = this.get('service.appsFailed');
    return this.t('dashboard.services.yarn.apps.msg').format(
      this.formatUnavailable(appsSubmitted),
      this.formatUnavailable(appsRunning),
      this.formatUnavailable(appsPending),
      this.formatUnavailable(appsCompleted),
      this.formatUnavailable(appsKilled),
      this.formatUnavailable(appsFailed));
  }.property('service.appsSubmitted', 'service.appsRunning', 'service.appsPending', 'service.appsCompleted', 'service.appsKilled', 'service.appsFailed'),

  memory: function () {
    return Em.I18n.t('dashboard.services.yarn.memory.msg').format(
        numberUtils.bytesToSize(this.get('service.allocatedMemory'), 1, 'parseFloat', 1024 * 1024), 
        numberUtils.bytesToSize(this.get('service.reservedMemory'), 1, 'parseFloat', 1024 * 1024), 
        numberUtils.bytesToSize(this.get('service.availableMemory'), 1, 'parseFloat', 1024 * 1024));
  }.property('service.allocatedMemory', 'service.reservedMemory', 'service.availableMemory'),

  queues: function() {
    return Em.I18n.t('dashboard.services.yarn.queues.msg').format(this.formatUnavailable(this.get('service.queuesCount')));
  }.property('service.queuesCount'),
  
  didInsertElement: function(){
    $("[rel='queue-tooltip']").tooltip({html: true, placement: "right"});
  }

});
