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

App.appsMapper = App.QuickDataMapper.create({
  model:App.App,
  map:function (json) {
    if (!this.get('model')) {
      return;
    }
    if (json.apps) {
      var result = [];
      json.apps.forEach(function (item) {
        var a = this.parseIt(item, this.config);
        // assume a nonzero elapsed time (otherwise axis labels are blank)
        if (a.finish_time < a.submit_time)
          a.finish_time = a.submit_time + 1000;
        a.elapsed_time = a.finish_time - a.submit_time;
        a.num_stages = a.stages.length;
        result.push(a);
      }, this);

      var r = Ember.ArrayProxy.create({"content":[]});
      result.forEach(function(item){
        r.content.push(App.App2.create(item));
      });

      this.set('controller.content.jobs', r.content);
    }
  },
  config:{
    id:'appId',
    run_id:'workflowId',
    app_name:'appName',
    app_type:'appType',
    workflow_entity_name:'workflowEntityName',
    user_name:'userName',
    queue:'queue',
    submit_time:'submitTime',
    launch_time:'launchTime',
    finish_time:'finishTime',
    num_stages:'numStages',
    stages:'stages',
    status:'status',
    elapsed_time:'elapsedTime'
  }
});
