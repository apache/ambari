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

App.MapReduceService = App.Service.extend({
  version: DS.attr('string'),
  jobHistoryServer: function () {
    return this.get('hostComponents').findProperty('componentName', 'HISTORYSERVER');
  }.property('hostComponents'),
  jobTracker: function () {
    return this.get('hostComponents').findProperty('componentName', 'JOBTRACKER');
  }.property('hostComponents'),
  taskTrackersStarted: DS.attr('number'),
  taskTrackersInstalled: DS.attr('number'),
  taskTrackersTotal: DS.attr('number'),
  jobTrackerStartTime: DS.attr('number'),
  jobTrackerHeapUsed: DS.attr('number'),
  jobTrackerHeapMax: DS.attr('number'),
  aliveTrackers: DS.hasMany('App.HostComponent'),
  blackListTrackers: DS.hasMany('App.HostComponent'),
  grayListTrackers: DS.hasMany('App.HostComponent'),
  mapSlots: DS.attr('number'),
  reduceSlots: DS.attr('number'),
  jobsSubmitted: DS.attr('number'),
  jobsCompleted: DS.attr('number'),
  jobsRunning: DS.attr('number'),
  mapSlotsOccupied: DS.attr('number'),
  mapSlotsReserved: DS.attr('number'),
  reduceSlotsOccupied: DS.attr('number'),
  reduceSlotsReserved: DS.attr('number'),
  mapsRunning: DS.attr('number'),
  mapsWaiting: DS.attr('number'),
  reducesRunning: DS.attr('number'),
  reducesWaiting: DS.attr('number'),
  trackersDecommissioned: DS.attr('number'),
  jobTrackerCpu: DS.attr('number'),
  jobTrackerRpc: DS.attr('number')
});

App.MapReduceService.FIXTURES = [];
