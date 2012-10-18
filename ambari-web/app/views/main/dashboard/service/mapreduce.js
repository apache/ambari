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

App.MainDashboardServiceMapreduceView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/dashboard/service/mapreduce'),
  serviceName: 'mapreduce',
  data: {
    "jobtracker_addr": "ec2-23-21-1-25.compute-1.amazonaws.com:50030",
    "jobtracker_starttime": 1348935243,
    "running_jobs": 1,
    "waiting_jobs": 0,
    "trackers_total": "1",
    "trackers_live": 1,
    "trackers_graylisted": 0,
    "trackers_blacklisted": 0,
    "chart": [4,8,7,2,1,4,3,3,3]
  },

  Chart: App.ChartLinearView.extend({
    data: function(){
      return this.get('_parentView.data.chart');
    }.property('_parentView.data.chart')
  }),

  jobTrackerUptime: function(){
    var uptime = this.get('data.jobtracker_starttime') + 0;
    var formatted = uptime.toDaysHoursMinutes();
    return this.t('dashboard.services.uptime').format(formatted.d, formatted.h, formatted.m);
  }.property("data"),

  summaryHeader: function(){
    var template = this.t('dashboard.services.mapreduce.summary');
    return template.format(this.get('data.trackers_live'), this.get('data.trackers_total'), this.get('data.running_jobs'));
  }.property('data'),

  trackersSummary: function (){
    var template = this.t('dashboard.services.mapreduce.trackersSummary');
    return template.format(this.get('data.trackers_live'), this.get('data.trackers_total'));
  }.property('data'),

  jobsSummary: function (){
    var template = this.t('dashboard.services.mapreduce.jobsSummary');
    return template.format(this.get('data.running_jobs'), "?", "?");
  }.property('data')
});