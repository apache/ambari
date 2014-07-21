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
var graph = require('utils/graph');

App.MainAppsItemBarView = Em.View.extend({
  elementId:'bars',
  templateName:require('templates/main/apps/item/bar'),
  content:function () {
    return this.get('controller.content.jobs');
  }.property('controller.content.jobs'),
  onLoad:function () {
    if (!this.get('controller.content.loadAllJobs') || this.get('activeJob')) {
      return;
    }

    var self = this;
    Ember.run.next(function(){
      self.updateTasksView();
    });
  }.observes('controller.content.loadAllJobs'),
  didInsertElement:function () {
    this.onLoad();
  },
  draw:function () {
  },

  mapNodeLocal:false,
  mapRackLocal:false,
  mapOffSwitch:false,
  reduceOffSwitch:false,
  submit:false,
  finish:false,

  updateTasksView:function () {
    var url = App.get('testMode') ? '/data/apps/jobs/taskview.json' : App.get('apiPrefix') + "/jobhistory/tasklocality?workflowId=" + this.get('controller.content.id');
    var mapper = App.jobTasksMapper;
    mapper.set('model', this);
    var self = this;
    App.HttpClient.get(url, mapper,{
      complete:function(jqXHR, textStatus) {
        self.set('loadJobTasks', true);
      }
    });
  },

  drawJobTasks:function () {
    if (!this.get('mapNodeLocal') || !this.get('mapRackLocal') || !this.get('mapOffSwitch') || !this.get('reduceOffSwitch')) {return;}
    graph.drawJobTasks(this.get('mapNodeLocal'), this.get('mapRackLocal'), this.get('mapOffSwitch'), this.get('reduceOffSwitch'), this.get('controller.content.startTime'), this.get('controller.content.startTime')+this.get('controller.content.elapsedTime'), this.$().width(), 300, 'job_tasks');
  }.observes('loadJobTasks')

});
