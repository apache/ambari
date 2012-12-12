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

App.runsMapper = App.QuickDataMapper.create({
  model : App.Run,
  map : function(json) {
    if(!this.get('model')) {
      return;
    }
    if(json && json.workflows) {
      var result = [];
      json.workflows.forEach(function(item) {
        var o = this.parseIt(item, this.config);

        var r = '{dag: {';
        item.workflowContext.workflowDag.entries.forEach(function(item) {
          r += '"' + item.source + '": [';
          // if a standalone MapReduce job, there won't be any targets
          if (item.targets) {
            item.targets.forEach(function(target) {
              r += '"' + target + '",';
            });
            r = r.substr(0, r.length - 1);
          } else {
            r += item.source;
          }
          r += '],';
        });
        r = r.substr(0, r.length - 1);
        r += '}}';
        o.workflow_context = r;

        result.push(o);
      }, this);
      App.store.loadMany(this.get('model'), result);
    }
  },
  config : {
    id: 'workflowId',
    app_name: 'workflowName',
    num_jobs_total: 'numJobsTotal',
    num_jobs_completed: 'numJobsCompleted',
    user_name:'userName',
    start_time: 'startTime',
    elapsed_time: 'elapsedTime',
    input: 'inputBytes',
    output: 'outputBytes'
  }
});
