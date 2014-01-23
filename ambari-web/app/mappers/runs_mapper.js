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
    if(json && json.aaData) {
      var result = [];

      var pagination_info={
        iTotalDisplayRecords :json.iTotalDisplayRecords ,
        iTotalRecords:json.iTotalRecords,
        startIndex:parseInt(json.startIndex)+1,
        endIndex:parseInt(json.endIndex)+1
      };

      json.aaData.forEach(function(item, index) {
        result.push(this.generateWorkflow(item, index));
      }, this);

      var r = [];
      result.forEach(function(item){
        r.push(App.Run2.create(item));
      });

      App.router.get('mainAppsController').set('content', r);
      App.router.get('mainAppsController').set('serverData', json.summary);
      App.router.get('mainAppsController').set('paginationObject', pagination_info);
    }


  },

  generateWorkflow: function(item, index) {
    var o = this.parseIt(item, this.config);

    var r = '{dag: {';
    item.workflowContext.workflowDag.entries.forEach(function(item) {
      r += '"' + item.source + '": [';
      // if a standalone MapReduce job, there won't be any targets
      if (item.targets) {
        item.targets.forEach(function(target) {
          r += '"' + target + '",';
        });
        if(item.targets.length){
          r = r.substr(0, r.length - 1);
        }
      }
      else {
        r += item.source;
      }
      r += '],';
    });
    r = r.substr(0, r.length - 1);
    r += '}}';
    o.workflowContext = r;
    o.index = index + 1;
    return o;
  },

  config : {
    id: 'workflowId',
    appName: 'workflowName',
    numJobsTotal: 'numJobsTotal',
    numJobsCompleted: 'numJobsCompleted',
    userName:'userName',
    startTime: 'startTime',
    elapsedTime: 'elapsedTime',
    input: 'inputBytes',
    output: 'outputBytes'
  }
});
