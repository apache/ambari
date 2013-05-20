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
  dataset: DS.belongsTo('App.Dataset'),
  status: DS.attr('string'),
  startDate: DS.attr('number'),
  endDate: DS.attr('number'),
  duration: DS.attr('number'),
  startDateFormatted: function () {
    return date.dateFormatShort(this.get('startDate'));
  }.property('startDate')
  //data : DS.attr('string')
});


App.DataSetJob.FIXTURES = [/*
 {
 id: 1,
 cluster_name: 'cluster1',
 stack_name: 'HDP',
 hosts: [1, 2, 3, 4],
 racks: [1, 2, 3, 4, 5, 6],
 max_hosts_per_rack: 10
 }*/
];