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

App.DataSetJob = DS.Model.extend({
  name: DS.attr('string'),
  dataset: DS.belongsTo('App.Dataset'),
  status: DS.attr('string'),
  startDate: DS.attr('number'),
  endDate: DS.attr('number'),

  statusFormatted: function () {
    return this.get('status').toLowerCase().capitalize();
  }.property('status'),

  isSuspended: function () {
    return this.get('status') === 'SUSPENDED';
  }.property('status'),

  startFormatted: function () {
    if (this.get('startDate')) {
      return $.timeago(this.get('startDate'));
    }
    return '';
  }.property('startDate'),

  endFormatted: function () {
    if (this.get('endDate')) {
      return $.timeago(this.get('endDate'));
    }
    return '';
  }.property('endDate'),

  healthClass: function () {
    var result = 'icon-question-sign';
    switch (this.get('status')) {
      case 'SUCCEEDED':
        result = 'icon-ok';
        break;
      case 'SUSPENDED':
        result = 'icon-cog';
        break;
      case 'WAITING':
        result = 'icon-time';
        break;
      case 'RUNNING':
        result = 'icon-play';
        break;
      case 'KILLED':
        result = 'icon-exclamation-sign';
        break;
      case 'FAILED':
        result = 'icon-warning-sign';
        break;
      case 'ERROR':
        result = 'icon-remove';
        break;
    }
    return result;
  }.property('status')
});


App.DataSetJob.FIXTURES = [];
