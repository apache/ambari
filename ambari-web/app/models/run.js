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

App.Run = DS.Model.extend({
  workflowId:DS.attr('string'),
  parentWorkflowId:DS.attr('string'),
  workflowContext:DS.attr('string'),
  userName:DS.attr('string'),
  startTime:DS.attr('string'),
  lastUpdateTime:DS.attr('string'),
  numJobsTotal:DS.attr('number'),
  numJobsCompleted:DS.attr('number'),
  app:DS.belongsTo('App.App'),
  jobs:DS.hasMany('App.Job')
});

App.Run.FIXTURES = [
  {
    id:1,
    workflow_id:'pig_1',
    parent_workflow_id:null,
    workflow_context:'{dag: {"1":["2","3"],"2":["3","4"],"4":["2","5"]}}',
    user_name:'user3',
    start_time:1347629541501,
    last_update_time:1347639541501,
    num_jobs_total:5,
    num_jobs_completed:0,
    app:1,
    jobs:[1, 2, 3, 4, 5]
  },
  {
    id:2,
    workflow_id:'pig_3',
    parent_workflow_id:null,
    workflow_context:'{dag:{"4":["5","1"],"3":["6"],"6":["4"],"1":["5"]}}',
    user_name:'user1',
    start_time:1347629951502,
    last_update_time:1347639951502,
    num_jobs_total:5,
    num_jobs_completed:2,
    app:3,
    jobs:[4, 5, 1, 3, 6]
  },
  {
    id:3,
    workflow_id:'pig_5',
    parent_workflow_id:null,
    workflow_context:'{dag:{"6":["7","8"],"9":["8"],"10":["6"],"9":["7"]}}',
    user_name:'user1',
    start_time:1347629841503,
    last_update_time:1347639841503,
    num_jobs_total:5,
    num_jobs_completed:0,
    jobs:[6, 7, 8, 9, 10]
  },
  {
    id:4,
    workflow_id:'pig_5',
    parent_workflow_id:null,
    workflow_context:'{dag:{"8":["9","10"],"9":["10"]}}',
    user_name:'user1',
    start_time:1347629541504,
    last_update_time:1347639541504,
    num_jobs_total:3,
    num_jobs_completed:0,
    app:2,
    jobs:[8, 9, 10]
  },
  {
    id:5,
    workflow_id:'pig_5',
    parent_workflow_id:null,
    workflow_context:'{dag:{"8":["9","10"],"8":["10","9"]}}',
    user_name:'user1',
    start_time:1347629541505,
    last_update_time:1347639541505,
    num_jobs_total:3,
    num_jobs_completed:0,
    app:2,
    jobs:[8, 9, 10]
  },
  {
    id:6,
    workflow_id:'pig_5',
    parent_workflow_id:null,
    workflow_context:'{dag:{"8":["9","10"],"9":["10","8"]}}',
    user_name:'user1',
    start_time:1347629541506,
    last_update_time:1347639541506,
    num_jobs_total:3,
    num_jobs_completed:0,
    app:2,
    jobs:[8, 9, 10]
  },
  {
    id:7,
    workflow_id:'pig_5',
    parent_workflow_id:null,
    workflow_context:'{dag:{"1":["3","5"],"5":["7"],"3":["1"]}}',
    user_name:'user1',
    start_time:1347629541507,
    last_update_time:1347639541507,
    num_jobs_total:4,
    num_jobs_completed:0,
    app:3,
    jobs:[1, 3, 5, 7]
  },
  {
    id:8,
    workflow_id:'pig_5',
    parent_workflow_id:null,
    workflow_context:'{dag:{"1":["3","5"],"5":["7"],"3":["1"]}}',
    user_name:'user1',
    start_time:1347629541508,
    last_update_time:1347639541508,
    num_jobs_total:4,
    num_jobs_completed:0,
    app:3,
    jobs:[1, 3, 5, 7]
  },
  {
    id:9,
    workflow_id:'pig_5',
    parent_workflow_id:null,
    workflow_context:'{dag:{"1":["3","5"],"5":["7"],"3":["1"]}}',
    user_name:'user1',
    start_time:1347629541509,
    last_update_time:1347639541509,
    num_jobs_total:4,
    num_jobs_completed:0,
    app:3,
    jobs:[1, 3, 5, 7]
  },
  {
    id:10,
    workflow_id:'pig_5',
    parent_workflow_id:null,
    workflow_context:'{dag:{"1":["3","5"],"5":["7"],"3":["1"]}}',
    user_name:'user1',
    start_time:1347629541510,
    last_update_time:1347639541510,
    num_jobs_total:4,
    num_jobs_completed:0,
    app:3,
    jobs:[1, 3, 5, 7]
  },
  {
    id:11,
    workflow_id:'pig_5',
    parent_workflow_id:null,
    workflow_context:'{dag:{"1":["3","5"],"5":["7"],"3":["1"]}}',
    user_name:'user1',
    start_time:1347629541511,
    last_update_time:1347639541511,
    num_jobs_total:4,
    num_jobs_completed:0,
    app:3,
    jobs:[1, 3, 5, 7]
  }
];
