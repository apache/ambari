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

App.jobsMapper = App.QuickDataMapper.create({
  model : App.Job,
  map : function(json) {
    if(!this.get('model')) {
      return;
    }
    if(json.jobs) {
      var result = [];
      json.jobs.forEach(function(item) {
        result.push(this.parseIt(item, this.config));
      }, this);
      App.store.loadMany(this.get('model'), result);
    }
  },
  config : {
    job_id : 'jobId',
    $workflow : 1,
    job_name : 'jobName',
    workflow_entity_name : 'workflowEntityName',
    user_name : 'userName',
    $queue : 'default',
    $acls : null,
    conf_path : 'confPath',
    submit_time : 'submitTime',
    //"launch_time":1348174627650,
    //"finish_time":1348174669539,
    maps : 'maps',
    reduces : 'reduces',
    status : 'status',
    $priority : null,
    //"finished_maps":12,
    //"finished_reduces":5,
    $failed_maps : null,
    $failed_reduces : null,
    //"maps_runtime":22299,
    //"reduces_runtime":11470,
    $map_counters : null,
    $reduce_counters : null,
    $job_counters : null,
    input : 'inputBytes',
    output : 'outputBytes'
  }
});
