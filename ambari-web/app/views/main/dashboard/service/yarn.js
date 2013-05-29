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

App.MainDashboardServiceYARNView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/dashboard/service/yarn'),
  serviceName: 'YARN',

  nodeWebUrl: function () {
    return "http://" + this.get('service.resourceManagerNode').get('publicHostName') + ":8088";
  }.property('service.resourceManagerNode'),

  nodeHeap: function () {
    var memUsed = this.get('service').get('jvmMemoryHeapUsed') * 1000000;
    var memCommitted = this.get('service').get('jvmMemoryHeapCommitted') * 1000000;
    var percent = memCommitted > 0 ? ((100 * memUsed) / memCommitted) : 0;
    return this.t('dashboard.services.hdfs.nodes.heapUsed').format(memUsed.bytesToSize(1, 'parseFloat'), memCommitted.bytesToSize(1, 'parseFloat'), percent.toFixed(1));

  }.property('service.jvmMemoryHeapUsed', 'service.jvmMemoryHeapCommitted'),

  summaryHeader: function () {
    var text = this.t("dashboard.services.yarn.summary");
    var svc = this.get('service');
    var totalCount = svc.get('nodeManagerNodes').get('length');
    return text.format(totalCount, totalCount);
  }.property('service.nodeManagerNodes'),
  
  nodeManagerComponent: function () {
    return App.HostComponent.find().findProperty('componentName', 'NODEMANAGER');
  }.property(),
  
  yarnClientComponent: function () {
    return App.HostComponent.find().findProperty('componentName', 'YARN_CLIENT');
  }.property(),
});