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
  width:300,
  height:210,

  content:function () {
    return this.get('controller.content.jobs');
  }.property('controller.content.jobs'),
  firstJob:function () {
    return this.get('content').get('firstObject');
  }.property('content'),
  activeJob:null,
  selectJob:function (event) {
    this.set('activeJob', event.context);

  },
  onLoad:function () {
    if (!this.get('controller.content.loadAllJobs') || this.get('activeJob')) {
      return;
    }

    this.set('activeJob', this.get('firstJob'));
  }.observes('controller.content.loadAllJobs'),
  didInsertElement:function () {
    this.onLoad();
  },
  draw:function () {
    var self = this;
    if (!this.get('activeJob')) {
      return;//when job is not defined
    }

    var desc1 = $('#graph1_desc');
    var desc2 = $('#graph2_desc');
    $('.rickshaw_graph, .rickshaw_legend, .rickshaw_annotation_timeline').html('');
    if (null == desc1.html() || null == desc2.html()) return;
    desc1.css('display', 'block');
    desc2.css('display', 'block');

    this.propertyDidChange('getChartData');

  }.observes('activeJob'),

  map:false,
  shuffle:false,
  reduce:false,

  mapNodeLocal:false,
  mapRackLocal:false,
  mapOffSwitch:false,
  reduceOffSwitch:false,
  submit:false,
  finish:false,

  updateTimeLine:function () {
    var url = App.testMode ? '/data/apps/jobs/timeline.json' : App.apiPrefix + "/jobhistory/task?jobId=" + this.get('activeJob').get('id') + 
      "&width=" + this.get('width');
    var mapper = App.jobTimeLineMapper;
    mapper.set('model', this);
    App.HttpClient.get(url, mapper);
  }.observes('getChartData'),

  updateTasksView:function () {
    var url = App.testMode ? '/data/apps/jobs/taskview.json' : App.apiPrefix + "/jobhistory/tasklocality?jobId=" + this.get('activeJob').get('id');
    var mapper = App.jobTasksMapper;
    mapper.set('model', this);
    App.HttpClient.get(url, mapper);
  }.observes('getChartData'),

  drawJobTimeline:function () {
    var map = JSON.stringify(this.get('map'));
    var shuffle = JSON.stringify(this.get('shuffle'));
    var reduce = JSON.stringify(this.get('reduce'));
    if (!this.get('map') || !this.get('shuffle') || !this.get('reduce')) {return;}
    $('#chart, #legend, #timeline1').html('');
    graph.drawJobTimeLine(map, shuffle, reduce, this.get('width'), this.get('height'), '#chart', 'legend', 'timeline1');
  }.observes('map', 'shuffle', 'reduce'),

  drawJobTasks:function () {
    var mapNodeLocal = JSON.stringify(this.get('mapNodeLocal'));
    var mapRackLocal = JSON.stringify(this.get('mapRackLocal'));
    var mapOffSwitch = JSON.stringify(this.get('mapOffSwitch'));
    var reduceOffSwitch = JSON.stringify(this.get('reduceOffSwitch'));
    if (!this.get('mapNodeLocal') || !this.get('mapRackLocal') || !this.get('mapOffSwitch') || !this.get('reduceOffSwitch')) {return;}
    $('#job_tasks, #tasks_legend, #timeline2').html('');
    graph.drawJobTasks(mapNodeLocal, mapRackLocal, mapOffSwitch, reduceOffSwitch, this.get('submit'), this.get('width'), this.get('height'), '#job_tasks', 'tasks_legend', 'timeline2');
  }.observes('mapNodeLocal', 'mapRackLocal', 'mapOffSwitch', 'reduceOffSwitch', 'submit')

});
