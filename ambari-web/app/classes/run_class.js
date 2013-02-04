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
var date = require('utils/date');
var misc = require('utils/misc');

App.Run2 = Ember.Object.extend({
  id: null, //string
  appName: null, //string
  userName: null, //string
  numJobsTotal: 0, //number
  numJobsCompleted: 0, //number
  startTime: 0, //number
  elapsedTime: 0, //number
  workflowContext: null, //string
  input: 0, //number
  output: 0, //number

  /**
   * Will set to true when we load all jobs related to this run
   */
  loadAllJobs : false,

  /**
   * runId  short part
   */
  idFormatted: function() {
    return this.get('id').substr(0, 20);
  }.property('id'),

  /**
   * Run duration
   */
  duration: function() {
    return date.timingFormat(this.get('elapsedTime'));
  }.property('elapsedTime'),

  /**
   * Status of running jobs
   */
  isRunning: function () {
    return !this.get('numJobsTotal') == this.get('numJobsCompleted');
  }.property('numJobsTotal', 'numJobsCompleted'),

  /**
   * Sum of input bandwidth for all jobs with appropriate measure
   */
  inputFormatted: function () {
    return misc.formatBandwidth(this.get('input'));
  }.property('input'),

  /**
   *  Sum of output bandwidth for all jobs with appropriate measure
   */
  outputFormatted: function () {
    return misc.formatBandwidth(this.get('output'));
  }.property('output'),

  lastUpdateTime: function() {
    return this.get('startTime') + this.get('elapsedTime');
  }.property('elapsedTime', 'startTime'),

  lastUpdateTimeFormatted: function() {
    return date.dateFormat(this.get('lastUpdateTime'));
  }.property('lastUpdateTime'),

  lastUpdateTimeFormattedShort: function(){
    return date.dateFormatShort(this.get('lastUpdateTime'));
  }.property('lastUpdateTime'),

  /**
   * Type value based on first part of id
   */
  type: function() {
    if (this.get('id').indexOf('pig_') === 0) {
      return 'Pig';
    }
    if (this.get('id').indexOf('hive_') === 0) {
      return 'Hive';
    }
    if (this.get('id').indexOf('mr_') === 0) {
      return 'MapReduce';
    }
    return 'Undefined';
  }.property('id')
});