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

App.Run = DS.Model.extend({
  appName: DS.attr('string'),
  userName:DS.attr('string'),
  numJobsTotal: DS.attr('number'),
  numJobsCompleted: DS.attr('number'),
  startTime:DS.attr('string'),
  elapsedTime:DS.attr('string'),
  workflowContext:DS.attr('string'),
  input: DS.attr('number'),
  output: DS.attr('number'),

  loadAllJobs : false,

  isStared: false,
  isFiltered: false,

  /**
   * runId  short part
   */
  idFormatted: function() {
    return this.get('id').substr(0, 20);
  }.property('id'),

  /**
   * Jobs in the current run
   */
  jobs: function() {
    return App.Job.find().filterProperty('run.id', this.get('id'));
  }.property('loadAllJobs'),

  /**
   * Run duration
   */
  duration: function() {
    return date.timingFormat(parseInt(this.get('elapsedTime')));
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
    var input = this.get('input');
    input = misc.formatBandwidth(input);
    return input;
  }.property('input'),

  /**
   *  Sum of output bandwidth for all jobs with appropriate measure
   */
  outputFormatted: function () {
    var output = this.get('output');
    output = misc.formatBandwidth(output);
    return output;
  }.property('output'),

  /**
   *
   */
  lastUpdateTime: function() {
    return parseInt(this.get('startTime')) + parseInt(this.get('elapsedTime'));
  }.property('elapsedTime', 'startTime'),
  /**
   *
   */
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
    return '';
  }.property('id')
});

App.Run.FIXTURES = [];
