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

App.App = DS.Model.extend({
  appName: DS.attr('string'),
  type: DS.attr('string'),
  numJobsTotal: DS.attr('number'),
  userName: DS.attr('string'),
  executionTime: DS.attr('string'),
  runs: DS.hasMany('App.Run')
});

App.App.FIXTURES = [
  {
    id: 1,
    app_name: 'pigs.sh',
    type: 'Hive',
    num_jobs_total: 5,
    user_name: 'root',
    execution_time: '1347629541543',
    runs: [1, 2, 3]
  },
  {
    id: 2,
    app_name: 'pigsm.sh',
    type: 'pig',
    num_jobs_total: 3,
    user_name: 'user1',
    execution_time: '1347656741515',
    runs: [6, 4, 5]
  },
  {
    id: 3,
    app_name: 'pigsmo.sh',
    type: 'pig',
    num_jobs_total: 4,
    user_name: 'user3',
    execution_time: '1347629587687',
    runs: [7, 8, 9, 10, 11]
  },
  {
    id: 4,
    app_name: 'pigsmok.sh',
    type: 'MapReduce',
    num_jobs_total: 0,
    user_name: 'root',
    execution_time: '134762957834',
    runs: []
  }
];
