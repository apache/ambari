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
  data:{
    "namenode_addr":"ec2-23-21-1-25.compute-1.amazonaws.com:50070",
    "secondary_namenode_addr":"ec2-23-21-1-25.compute-1.amazonaws.com:50090",
    "namenode_starttime":1348935028,
    "total_nodes":"1",
    "live_nodes":1,
    "dead_nodes":0,
    "decommissioning_nodes":0,
    "dfs_blocks_underreplicated":145,
    "safemode":false,
    "pending_upgrades":false,
    "dfs_configured_capacity":885570207744,
    "dfs_percent_used":0.01,
    "dfs_percent_remaining":95.09,
    "dfs_total_bytes":885570207744,
    "dfs_used_bytes":104898560,
    "nondfs_used_bytes":43365113856,
    "dfs_free_bytes":842100195328
  },

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
    return this.t('dashboard.services.hdfs.nodes.heapUsed').format("?", "?", "?");
  }.property('data'),

  summaryHeader:function () {
    var text = this.t("dashboard.services.hdfs.summary");
    return text.format(this.get('data.live_nodes'), this.get('data.total_nodes'), this.get('data.dfs_percent_remaining'));
  }.property('data'),

  capacity:function () {
    var text = this.t("dashboard.services.hdfs.capacityUsed");
    var total = this.get('data.dfs_total_bytes') + 0;
    var used = this.get('data.dfs_used_bytes') + this.get('data.nondfs_used_bytes');

    return text.format(used.bytesToSize(2), total.bytesToSize(2), this.get('data.dfs_percent_used'));
  }.property('data')
});