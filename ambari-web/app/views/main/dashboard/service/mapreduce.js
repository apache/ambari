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

App.MainDashboardServiceMapreduceView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/dashboard/service/mapreduce'),
  serviceName: 'MAPREDUCE',
  jobTrackerWebUrl: function () {
    return "http://" + this.get('service').get('jobTracker').get('publicHostName') + ":50030";
  }.property('service.nameNode'),

  Chart: App.ChartLinearView.extend({
    data: function () {
      return this.get('_parentView.data.chart');
    }.property('_parentView.data.chart')
  }),

  jobTrackerUptime: function () {
    var uptime = this.get('service').get('jobTrackerStartTime');
    if (uptime && uptime > 0){
      var diff = (new Date()).getTime() - uptime;
      if (diff < 0) {
        diff = 0;
      }
      var formatted = date.timingFormat(diff);
      return this.t('dashboard.services.uptime').format(formatted);

    }
    return this.t('services.service.summary.notRunning');
  }.property("service.jobTrackerStartTime"),

  summaryHeader: function () {
    var svc = this.get('service');
    var liveCount = svc.get('aliveTrackers').get('length');
    var allCount = svc.get('taskTrackers').get('length');
    var runningCount = svc.get('mapsRunning') + svc.get('reducesRunning');
    var waitingCount = svc.get('mapsWaiting') + svc.get('reducesWaiting');
    var template = this.t('dashboard.services.mapreduce.summary');
    return template.format(liveCount, allCount, runningCount, waitingCount);
  }.property('service.aliveTrackers', 'service.taskTrackers','service.mapsRunning', 'service.mapsWaiting', 'service.reducesRunning', 'service.reducesWaiting'),

  trackersSummary: function () {
    var svc = this.get('service');
    var liveCount = svc.get('aliveTrackers').get('length');
    var totalCount = svc.get('taskTrackers').get('length');
    var template = this.t('dashboard.services.mapreduce.trackersSummary');
    return template.format(liveCount, totalCount);
  }.property('service.aliveTrackers.length', 'service.taskTrackers.length'),

  trackersHeapSummary: function () {
    var heapUsed = this.get('service').get('jobTrackerHeapUsed') || 0;
    var heapMax = this.get('service').get('jobTrackerHeapMax') || 0;
    var percent = heapMax > 0 ? 100 * heapUsed / heapMax : 0;
    return this.t('dashboard.services.mapreduce.jobTrackerHeapSummary').format(heapUsed.bytesToSize(1, "parseFloat"), heapMax.bytesToSize(1, "parseFloat"), percent.toFixed(1));
  }.property('service.jobTrackerHeapUsed', 'service.jobTrackerHeapMax'),

  jobsSummary: function () {
    var svc = this.get('service');
    var template = this.t('dashboard.services.mapreduce.jobsSummary');
    return template.format(this.formatUnavailable(svc.get('jobsSubmitted')), this.formatUnavailable(svc.get('jobsCompleted')));
  }.property('service.jobsSubmitted', 'service.jobsCompleted'),

  mapSlotsSummary: function () {
    var svc = this.get('service');
    var template = this.t('dashboard.services.mapreduce.mapSlotsSummary');
    return template.format(this.formatUnavailable(svc.get('mapSlotsOccupied')), this.formatUnavailable(svc.get('mapSlotsReserved')));
  }.property('service.mapSlotsOccupied', 'service.mapSlotsReserved'),

  reduceSlotsSummary: function () {
    var svc = this.get('service');
    var template = this.t('dashboard.services.mapreduce.reduceSlotsSummary');
    return template.format(this.formatUnavailable(svc.get('reduceSlotsOccupied')), this.formatUnavailable(svc.get('reduceSlotsReserved')));
  }.property('service.reduceSlotsOccupied', 'service.reduceSlotsReserved'),

  mapTasksSummary: function () {
    var svc = this.get('service');
    var template = this.t('dashboard.services.mapreduce.tasksSummary');
    return template.format(this.formatUnavailable(svc.get('mapsRunning')), this.formatUnavailable(svc.get('mapsWaiting')));
  }.property('service.mapsRunning', 'service.mapsWaiting'),

  reduceTasksSummary: function () {
    var svc = this.get('service');
    var template = this.t('dashboard.services.mapreduce.tasksSummary');
    return template.format(this.formatUnavailable(svc.get('reducesRunning')), this.formatUnavailable(svc.get('reducesWaiting')));
  }.property('service.reducesRunning', 'service.reducesWaiting'),

  slotsCapacitySummary: function () {
    var mapSlots = this.get('service').get('mapSlots');
    var reduceSlots = this.get('service').get('reduceSlots');
    var liveNodeCount = this.get('service').get('aliveTrackers').get('length');
    if(liveNodeCount != 0){
      var avg = (mapSlots + reduceSlots) / liveNodeCount;
    }else{
      avg = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    return this.t('dashboard.services.mapreduce.slotCapacitySummary').format(mapSlots, reduceSlots, avg);
  }.property('service.mapSlots', 'service.reduceSlots', 'service.aliveTrackers'),

  taskTrackerComponent: function () {
    return App.HostComponent.find().findProperty('componentName', 'TASKTRACKER');
  }.property()
});