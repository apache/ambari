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
  model:App.Job,
  map:function (json) {
    if (!this.get('model')) {
      return;
    }
    if (json.jobs) {
      var result = [];
      json.jobs.forEach(function (item) {
        result.push(this.parseIt(item, this.config));
      }, this);
      App.store.loadMany(this.get('model'), result);
    }
  },
  config:{
    id:'jobId',
    run_id:'workflowId',
    job_name:'jobName',
    workflow_entity_name:'workflowEntityName',
    user_name:'userName',
    $queue:'default',
    $acls:5,
    conf_path:'confPath',
    submit_time:'submitTime',
    maps:'maps',
    reduces:'reduces',
    status:'status',
    $priority:null,
    $failed_maps:3,
    $failed_reduces:3,
    $job_counters:3,
    input:'inputBytes',
    output:'outputBytes',
    elapsed_time:'elapsedTime'
  }
});

App.jobTimeLineMapper = App.QuickDataMapper.create({
  config:{
    map:'map',
    shuffle:'shuffle',
    reduce:'reduce'
  },
  map:function (json) {
    var job = this.get('model'); // @model App.MainAppsItemBarView
    var parseResult = this.parseIt(json, this.config);

    $.each(parseResult, function (field, value) {
      job.set(field, value);
    });
  }
});

App.jobTasksMapper = App.QuickDataMapper.create({
  config:{
    mapNodeLocal:'mapNodeLocal',
    mapRackLocal:'mapRackLocal',
    mapOffSwitch:'mapOffSwitch',
    reduceOffSwitch:'reduceOffSwitch',
    submit:'submitTime',
    finish:'finishTime'
  },
  map:function (json) {
    var job = this.get('model'); // @model App.MainAppsItemBarView
    var parseResult = this.parseIt(json, this.config);
    $.each(parseResult, function (field, value) {
      job.set(field, value);
    });
  }
});