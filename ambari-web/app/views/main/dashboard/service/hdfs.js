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

App.MainDashboardServiceHdfsView = App.MainDashboardServiceView.extend({
  templateName:require('templates/main/dashboard/service/hdfs'),
  serviceName:'HDFS',

  Chart:App.ChartPieView.extend({
    data:function () {
      return [ this.get('_parentView.service.capacityUsed'), this.get('_parentView.service.capacityTotal') ];
    }.property('_parentView.service')
  }),

  nodeUptime:function () {
    var uptime = this.get('service').get('nameNodeStartTime');
    var formatted = (new Date().getTime() - uptime).toDaysHoursMinutes();
    return this.t('dashboard.services.uptime').format(formatted.d, formatted.h, formatted.m);
  }.property("service.nameNodeStartTime"),
  
  service: function(){
    return App.HDFSService.find().objectAt(0);
  }.property('App.router.clusterController.dataLoadList.services'),
  
  nodeWebUrl: function(){
    return "http://"+this.get('service').get('nameNode').get('hostName')+":50070";
  }.property('service.nameNode'),

  nodeHeap:function () {

    var percent = this.get('data.namenode_heap_total') > 0 ? 100 * this.get('data.namenode_heap_used') / this.get('data.namenode_heap_total') : 0;

    return this.t('dashboard.services.hdfs.nodes.heapUsed').format(
      (this.get('service').get('jvmMemoryHeapUsed')*1000000).bytesToSize(1, 'parseFloat'),
      (this.get('service').get('jvmMemoryHeapCommitted')*1000000).bytesToSize(1, 'parseFloat')
      , percent.toFixed(1));

  }.property('service'),

  summaryHeader:function () {
    var text = this.t("dashboard.services.hdfs.summary");
    var svc = this.get('service');
    var liveCount = svc.get('liveDataNodes').get('length');
    var totalCount = svc.get('dataNodes').get('length');
    var total = svc.get('capacityTotal') + 0;
    var used = svc.get('capacityUsed') + 0;
    var percentRemaining = (100-Math.round((used*100)/total)).toFixed(1);
    return text.format(liveCount, totalCount, percentRemaining);
  }.property('service'),

  capacity:function () {
    var text = this.t("dashboard.services.hdfs.capacityUsed");
    var total = this.get('service').get('capacityTotal') + 0;
    var used = this.get('service').get('capacityUsed') + 0;
    var percent = Math.round((used*100)/total).toFixed(1);
    return text.format(used.bytesToSize(1), total.bytesToSize(1), percent);
  }.property('service'),
  
  dataNodeComponent: function(){
    return App.Component.find().findProperty('componentName', 'DATANODE');
  }.property('components')
});