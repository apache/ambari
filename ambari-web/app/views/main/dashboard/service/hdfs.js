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
  serviceName:'hdfs',

  Chart:App.ChartPieView.extend({
    data:function () {
      return [ this.get('_parentView.data.dfs_used_bytes') + this.get('_parentView.data.nondfs_used_bytes'), this.get('_parentView.data.dfs_free_bytes') ];
    }.property('_parentView.data')
  }),

  nodeUptime:function () {
    var uptime = this.get('data.namenode_starttime');
    var formatted = uptime.toDaysHoursMinutes();
    return this.t('dashboard.services.uptime').format(formatted.d, formatted.h, formatted.m);
  }.property("data"),

  nodeHeap:function () {

    var percent = this.get('data.namenode_heap_total') > 0 ? 100 * this.get('data.namenode_heap_used') / this.get('data.namenode_heap_total') : 0;

    return this.t('dashboard.services.hdfs.nodes.heapUsed').format(
      this.get('data.namenode_heap_used').bytesToSize(1, 'parseFloat'),
      this.get('data.namenode_heap_total').bytesToSize(1, 'parseFloat')
      , percent.toFixed(1));

  }.property('data'),

  summaryHeader:function () {
    var text = this.t("dashboard.services.hdfs.summary");
    return text.format(this.get('data.live_nodes'), this.get('data.total_nodes'), this.get('data.dfs_percent_remaining'));
  }.property('data'),

  capacity:function () {
    var text = this.t("dashboard.services.hdfs.capacityUsed");
    var total = this.get('data.dfs_total_bytes') + 0;
    var used = this.get('data.dfs_used_bytes') + this.get('data.nondfs_used_bytes');

    return text.format(used.bytesToSize(1), total.bytesToSize(1), this.get('data.dfs_percent_used'));
  }.property('data')
});