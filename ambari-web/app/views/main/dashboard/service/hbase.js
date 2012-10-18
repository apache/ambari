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

App.MainDashboardServiceHbaseView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/dashboard/service/hbase'),
  serviceName: 'hbase',
  data: {
    "hbasemaster_addr": "hbasemaster:60010",
    "total_regionservers": "1",
    "hbasemaster_starttime": 1348935496,
    "live_regionservers": 1,
    "dead_regionservers": 0,
    "regions_in_transition_count": 0,
    "chart": [3,7,7,5,5,3,5,3,7]
  },

  Chart: App.ChartLinearView.extend({
    data: function(){
      return this.get('_parentView.data.chart');
    }.property('_parentView.data.chart')
  }),

  summaryHeader: function(){
    return this.t("dashboard.services.hbase.summary").format(
      this.get('data.live_regionservers'),
      this.get('data.total_regionservers'),
      "?"
    );
  }.property('data'),

  regionServers: function(){
    return this.t('dashboard.services.hbase.regionServersSummary').format(
      this.get('data.live_regionservers'), this.get('data.total_regionservers')
    );

  }.property('data'),

  masterServerUptime: function(){
    var uptime = this.get('data.hbasemaster_starttime');
    var formatted = uptime.toDaysHoursMinutes();
    return this.t('dashboard.services.uptime').format(formatted.d, formatted.h, formatted.m);
  }.property("data")
});