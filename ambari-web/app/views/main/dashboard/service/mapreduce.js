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

App.MainDashboardServiceMapreduceView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/dashboard/service/mapreduce'),
  serviceName: 'MAPREDUCE',

  service: function () {
    return App.MapReduceService.find().objectAt(0);
  }.property('App.router.clusterController.dataLoadList.services'),

  jobTrackerWebUrl: function () {
    return "http://" + this.get('service').get('jobTracker').get('hostName') + ":50030";
  }.property('service.nameNode'),

  Chart: App.ChartLinearView.extend({
    data: function () {
      return this.get('_parentView.data.chart');
    }.property('_parentView.data.chart')
  }),

  jobTrackerUptime: function () {
    var uptime = this.get('service').get('jobTrackerStartTime');
    var formatted = (new Date().getTime() - uptime).toDaysHoursMinutes();
    return this.t('dashboard.services.uptime').format(formatted.d, formatted.h, formatted.m);
  }.property("service.jobTrackerStartTime"),

  summaryHeader: function () {
    var svc = this.get('service');
    var liveCount = svc.get('aliveTrackers').get('length');
    var allCount = svc.get('taskTrackers').get('length');
    var runningCount = svc.get('mapsRunning') + svc.get('reducesRunning');
    var waitingCount = svc.get('mapsWaiting') + svc.get('reducesWaiting');
    var template = this.t('dashboard.services.mapreduce.summary');
    return template.format(liveCount, allCount, runningCount, waitingCount);
  }.property('service'),

  trackersSummary: function () {
    var svc = this.get('service');
    var liveCount = svc.get('aliveTrackers').get('length');
    var totalCount = svc.get('taskTrackers').get('length');
    var template = this.t('dashboard.services.mapreduce.trackersSummary');
    return template.format(liveCount, totalCount);
  }.property('service'),

  trackersHeapSummary: function () {
    var heapUsed = this.get('service').get('jobTrackerHeapUsed');
    var heapMax = this.get('service').get('jobTrackerHeapMax');
    var percent = heapMax > 0 ? 100 * heapUsed / heapMax : 0;
    return this.t('dashboard.services.mapreduce.jobTrackerHeapSummary').format(heapUsed.bytesToSize(1, "parseFloat"), heapMax.bytesToSize(1, "parseFloat"), percent.toFixed(1));
  }.property('service'),

  jobsSummary: function () {
    var template = this.t('dashboard.services.mapreduce.jobsSummary');
    return template.format(0, 0, 0);
  }.property('service'),

  mapSlotsSummary: function () {
    return this.t('dashboard.services.mapreduce.mapSlotsSummary').format(0,0);
  }.property('service'),

  reduceSlotsSummary: function () {
    return this.t('dashboard.services.mapreduce.reduceSlotsSummary').format(0,0);
  }.property('service'),

  mapTasksSummary: function () {
    return this.t('dashboard.services.mapreduce.tasksSummary').format(0,0);
  }.property('service'),

  reduceTasksSummary: function () {
    return this.t('dashboard.services.mapreduce.tasksSummary').format(0,0);
  }.property('service'),

  slotsCapacitySummary: function () {
    var mapSlots = this.get('service').get('mapSlots');
    var reduceSlots = this.get('service').get('reduceSlots');
    var liveNodeCount = this.get('service').get('aliveTrackers').get('length');
    var avg = (mapSlots + reduceSlots)/liveNodeCount;
    return this.t('dashboard.services.mapreduce.slotCapacitySummary').format(mapSlots, reduceSlots, avg);
  }.property('service'),

  taskTrackerComponent: function () {
    return App.Component.find().findProperty('componentName', 'TASKTRACKER');
  }.property('components')
});