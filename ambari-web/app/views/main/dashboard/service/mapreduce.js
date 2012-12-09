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
  templateName:require('templates/main/dashboard/service/mapreduce'),
  serviceName:'MAPREDUCE',

  Chart:App.ChartLinearView.extend({
    data:function () {
      return this.get('_parentView.data.chart');
    }.property('_parentView.data.chart')
  }),

  jobTrackerUptime:function () {
    var uptime = this.get('data.jobtracker_starttime') + 0;
    var formatted = uptime.toDaysHoursMinutes();
    return this.t('dashboard.services.uptime').format(formatted.d, formatted.h, formatted.m);
  }.property("data"),

  summaryHeader:function () {
    var template = this.t('dashboard.services.mapreduce.summary');
    return template.format(this.get('data.trackers_live'), this.get('data.trackers_total'), this.get('data.running_jobs'));
  }.property('data'),

  trackersSummary:function () {
    var template = this.t('dashboard.services.mapreduce.trackersSummary');
    return template.format(this.get('data.trackers_live'), this.get('data.trackers_total'));
  }.property('data'),

  trackersHeapSummary:function () {
    var percent =
      this.get('data.trackers_heap_total') > 0
        ? 100 * this.get('data.trackers_heap_used') / this.get('data.trackers_heap_total')
        : 0;

    return this.t('dashboard.services.mapreduce.jobTrackerHeapSummary').format(
      this.get('data.trackers_heap_used').bytesToSize(1, "parseFloat"),
      this.get('data.trackers_heap_total').bytesToSize(1, "parseFloat"),
      percent.toFixed(1)
    );
  }.property('data'),

  jobsSummary:function () {
    var template = this.t('dashboard.services.mapreduce.jobsSummary');
    return template.format(this.get('data.running_jobs'), this.get('data.completed_jobs'), this.get('data.failed_jobs'));
  }.property('data'),

  mapSlotsSummary:function () {
    return this.t('dashboard.services.mapreduce.mapSlotsSummary').format(
      this.get('data.map_slots_occuped'),
      this.get('data.map_slots_reserved'),
      this.get('data.map_slots_total')
    );
  }.property('data'),

  reduceSlotsSummary:function () {
    return this.t('dashboard.services.mapreduce.reduceSlotsSummary').format(
      this.get('data.reduce_slots_occuped'),
      this.get('data.reduce_slots_reserved'),
      this.get('data.reduce_slots_total')
    );
  }.property('data')
});