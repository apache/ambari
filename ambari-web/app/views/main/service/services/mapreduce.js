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

App.MainDashboardServiceMapreduceView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/service/services/mapreduce'),
  serviceName: 'MAPREDUCE',
  jobTrackerWebUrl: function () {
    return "http://" + (App.singleNodeInstall ? App.singleNodeAlias : this.get('service').get('jobTracker').get('publicHostName')) + ":50030";
  }.property('service.jobTracker'),

  Chart: App.ChartLinearView.extend({
    data: function () {
      return this.get('_parentView.data.chart');
    }.property('_parentView.data.chart')
  }),

  jobTrackerUptime: function () {
    var uptime = this.get('service').get('jobTrackerStartTime');
    if (uptime && uptime > 0){
      var diff = App.dateTime() - uptime;
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
    var allCount = svc.get('taskTrackersTotal');
    var runningCount = svc.get('jobsRunning');
    if (runningCount === null) {
      runningCount = 'n/a';
    }
    var template = this.t('dashboard.services.mapreduce.summary');
    return template.format(liveCount, allCount, runningCount);
  }.property('service.aliveTrackers', 'service.taskTrackersTotal', 'service.jobsRunning'),

  trackersText: function () {
    if (this.get('service').get('taskTrackersTotal') == 0) {
      return '';
    } else if (this.get('service').get('taskTrackersTotal') > 1){
      return Em.I18n.t('services.service.summary.viewHosts');
    }else{
      return Em.I18n.t('services.service.summary.viewHost');
    }
  }.property("service.taskTrackersTotal"),

  trackersSummary: function () {
    var svc = this.get('service');
    var liveCount = svc.get('taskTrackersStarted');
    var totalCount = svc.get('taskTrackersTotal');
    var template = this.t('dashboard.services.mapreduce.trackersSummary');
    return template.format(liveCount, totalCount);
  }.property('service.taskTrackersTotal', 'service.taskTrackersStarted'),

  trackersHeapSummary: function () {
    var heapUsed = this.get('service').get('jobTrackerHeapUsed');
    var heapMax = this.get('service').get('jobTrackerHeapMax');
    var percent = heapMax > 0 ? 100 * heapUsed / heapMax : 0;
    return this.t('dashboard.services.mapreduce.jobTrackerHeapSummary').format(numberUtils.bytesToSize(heapUsed, 1, "parseFloat"), numberUtils.bytesToSize(heapMax, 1, "parseFloat"), percent.toFixed(1));
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
    return Em.Object.create({
      componentName: 'TASKTRACKER'
    });
    //return this.get('service.taskTrackers').objectAt(0);
  }.property()
});
