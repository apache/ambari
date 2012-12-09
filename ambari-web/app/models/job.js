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

App.Job = DS.Model.extend({
  jobId:DS.attr('string'),
  workflow:DS.belongsTo('App.Run'),
  jobName:DS.attr('string'),
  workflowEntityName:DS.attr('string'),
  userName:DS.attr('string'),
  queue:DS.attr('string'),
  acls:DS.attr('string'),
  confPath:DS.attr('string'),
  submitTime:DS.attr('number'),
  launchTime:DS.attr('number'),
  finishTime:DS.attr('number'),
  maps:DS.attr('number'),
  reduces:DS.attr('number'),
  status:DS.attr('string'),
  priority:DS.attr('string'),
  finishedMaps:DS.attr('number'),
  finishedReduces:DS.attr('number'),
  failedMaps:DS.attr('number'),
  failedReduces:DS.attr('number'),
  mapsRuntime:DS.attr('number'),
  reducesRuntime:DS.attr('number'),
  mapCounters:DS.attr('number'),
  reduceCounters:DS.attr('number'),
  jobCounters:DS.attr('number'),
  input:DS.attr('number'),
  output:DS.attr('number'),
  duration: function() {
    return date.dateFormatInterval(parseInt((parseInt(this.get('finishTime')) - parseInt(this.get('launchTime')))/1000));
  }.property('launchTime', 'finishTime'),
  jobTimeline:DS.attr('string'),
  jobTaskview:DS.attr('string'),
  /**
   *  Sum of input bandwidth for all jobs with appropriate measure
   */
  inputFormatted: function () {
    var input = this.get('input');
    return misc.formatBandwidth(input);
  }.property('input'),
  /**
   *  Sum of output bandwidth for all jobs with appropriate measure
   */
  outputFormatted: function () {
    var output = this.get('output');
    return misc.formatBandwidth(output);
  }.property('output')
});

App.Job.FIXTURES = []
