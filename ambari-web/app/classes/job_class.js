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

App.Job2 = Ember.Object.extend({

  id: "", //string
  jobName: "", //string
  workflowEntityName: "", //string
  maps: 0, //number
  reduces: 0, //number
  status: "", //string
  input: 0, //number
  output: 0, //number
  elapsed_time: 0, //number

  duration: function() {
    return date.timingFormat(parseInt(this.get('elapsed_time')));
  }.property('elapsed_time'),

  inputFormatted: function () {
    return misc.formatBandwidth(this.get('input'));
  }.property('input'),

  outputFormatted: function () {
    return misc.formatBandwidth(this.get('output'));
  }.property('output')

});
