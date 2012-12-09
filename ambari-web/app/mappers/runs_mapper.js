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
    console.log('json', json.workflows);
    if(!this.get('model')) {
      return;
    }
    if(json.workflows) {
      var result = [];
      json.workflows.forEach(function(item) {
        result.push(this.parseIt(item, this.config));
      }, this);
      console.log('result', result);
      App.store.loadMany(this.get('model'), result);
    }
  },
  config : {
    run_id: 'workflowName',
    $parent_run_id: null,
    //workflow_context:'{dag: {"1":["2","3"],"2":["3","4"],"4":["2","5"]}}',
    user_name:'userName',
    start_time: 'startTime',
    //last_update_time:'1347639541501',
    app_id: 'workflowId'
    //jobs:[1, 2, 3, 4, 5]
  }
});
