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

App.MainAppsItemController = Em.Controller.extend({
  name:'mainAppsItemController',
  /**
   * Was set outside in App.MainAppsView.
   * It's instance of App.Run model
   */
  content: [],
  jobsLoaded:false,

  gettingJobs:function(){
    var currentId = this.get('content.id');
    if(this.get('content.loadAllJobs')){
      return;
    }
    var self = this;

    var url = App.get('testMode') ? '/data/apps/jobs/'+ currentId +'.json' :
      App.apiPrefix + "/jobhistory/job?workflowId=" + currentId;

    var mapper = App.jobsMapper;
    mapper.set('controller', this);
    App.HttpClient.get(url, mapper,{
      complete:function(jqXHR, textStatus) {
        self.set('content.loadAllJobs', true);
      }
    });
  }.observes('content')

});
