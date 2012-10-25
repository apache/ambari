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
App.MainAppsRunsJobsBarView = Em.View.extend({
  templateName:require('templates/main/apps/runs/jobs/bar'),
  content:function () {
    return App.router.get('mainAppsRunsItemController.content').get('jobs');
  }.property('App.router.mainAppsRunsItemController.content'),
  firstJob:function () {
    return this.get('content').get('firstObject');
  }.property('content'),
  didInsertElement:function () {
    this.get('controller').set('activeJobId', this.get('firstJob').get('jobId'));
    this.get('controller').set('job', this.get('firstJob'));
    this.get('controller').set('job', null);
  },
  draw:function () {
    if (!this.get('controller').get('job')) {
      return;//when job is not defined
    }
    width = 500;
    height = 420;
    var desc1 = $('#graph1_desc');
    var desc2 = $('#graph2_desc');
    $('.rickshaw_graph, .rickshaw_legend, .rickshaw_annotation_timeline').html('');
    if (null == desc1.html() || null == desc2.html()) return;
    desc1.css('display', 'block');
    desc2.css('display', 'block');
    graph.drawJobTimeline(this.get('controller').get('job').get('jobTimeline'), width, height, '#chart', 'legend', 'timeline1');
    graph.drawJobTasks(this.get('controller').get('job').get('jobTaskview'), width, height, '#job_tasks', 'tasks_legend', 'timeline2');
  }.observes('controller.job')
});
