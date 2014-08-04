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
var sort = require('views/common/sort_view');

App.MainAppsItemDagView = Em.View.extend({
  templateName: require('templates/main/apps/item/dag'),
  elementId : 'jobs',
  content:function(){
    return this.get('controller.content.jobs');
  }.property('controller.content.jobs'),

  classNames:['table','dataTable'],
  /**
   * convert content to special jobs object for DagViewer
   */
  jobs: function(){
    var c = this.get('content');
    var result = [];
    c.forEach(function(item, index){
      result[index] = new Object({
        'name' : item.get('id'),
        'entityName' : item.get('workflow_entity_name'),
        'status' : item.get('status'),
        'input' : item.get('input'),
        'output' : item.get('output'),
        'submitTime' : item.get('submit_time'),
        'elapsedTime' : item.get('elapsed_time')
      })
    });
    return result;
  }.property('content'),

  loaded : false,

  hasManyJobs: function(){
    return (this.get('content') && this.get('content').length > 1);
  }.property('content'),

  onLoad:function (){
    if(!this.get('controller.content.loadAllJobs') || this.get('loaded')){
      return;
    }

    this.set('loaded', true);

    var self = this;

    Ember.run.next(function(){
      self.draw();
      self.updateTimeline();
    });

  }.observes('controller.content.loadAllJobs'),

  resizeModal: function () {
    var modal = $('.modal');
    var body = $('body');
    modal.find('.modal-body').first().css('max-height', 'none');
    var modalHeight = modal.height() + 300;
    var bodyHeight = body.height();
    if (modalHeight > bodyHeight) {
      modal.css('top', '20px');
      $('.modal-body').height(bodyHeight - 180);
    } else {
      modal.css('top', (bodyHeight - modalHeight) / 2 + 'px');
    }

    var modalWidth = modal.width();
    var bodyWidth = body.width();
    if (modalWidth > bodyWidth) {
      modal.css('left', '10px');
      modal.width(bodyWidth - 20);
    } else {
      modal.css('left', (bodyWidth - modalWidth) / 2 + 'px');
    }
  },

  didInsertElement: function(){
    this.onLoad();
  },

  loadTaskTimeline:false,
  loadJobTimeline:false,
  map:false,
  shuffle:false,
  reduce:false,
  allmap:false,
  allshuffle:false,
  allreduce:false,

  updateTimeline:function () {
    var url = App.get('testMode') ? '/data/apps/jobs/timeline.json' : App.get('apiPrefix') + "/jobhistory/task?workflowId=" + this.get('controller.content.id') + "&width=" + Math.ceil(this.$().width()) + "&startTime=" + this.get('controller.content.startTime') + "&endTime=" + (this.get('controller.content.startTime')+this.get('controller.content.elapsedTime'));
    var mapper = App.jobTimeLineMapper;
    mapper.set('model', this);
    var self = this;
    App.HttpClient.get(url, mapper,{
      complete:function(jqXHR, textStatus) {
        self.set('loadJobTimeline', true);
      }
    });
    url = App.get('testMode') ? '/data/apps/jobs/timeline.json' : App.get('apiPrefix') + "/jobhistory/task?width=" + Math.ceil(this.$().width()) + "&startTime=" + this.get('controller.content.startTime') + "&endTime=" + (this.get('controller.content.startTime')+this.get('controller.content.elapsedTime'));
    var mapper = App.taskTimeLineMapper;
    mapper.set('model', this);
    var self = this;
    App.HttpClient.get(url, mapper,{
      complete:function(jqXHR, textStatus) {
        self.set('loadTaskTimeline', true);
      }
    });
  },

  drawJobTimeline:function () {
    if (this.get('loadJobTimeline') && this.get('loadTaskTimeline')) {
      this.daggraph.addTimeSeries([{"name":"allmap","color":"green","values":this.get('allmap')},
        {"name":"map","color":"green","values":this.get('map')}], 0, "Maps");
      this.daggraph.addTimeSeries([
        {"name":"allshuffle","color":"lightblue","values":this.get('allshuffle')},
        {"name":"allreduce","color":"steelblue","values":this.get("allreduce")},
        {"name":"shuffle","color":"lightblue","values":this.get('shuffle')},
        {"name":"reduce","color":"steelblue","values":this.get("reduce")}], 1, "Reduces");
    }
  }.observes('loadJobTimeline', 'loadTaskTimeline'),

  daggraph:false,

  draw: function(){
    var dagSchema = this.get('controller.content.workflowContext');
    var jobs = this.get('jobs');
    this.resizeModal();
    this.daggraph = new DagViewer('dag_viewer')
        .setData(dagSchema, jobs)
        .drawDag(this.$().width(), 300, 20);
  },
  sortView: sort.wrapperView,
  nameSort: sort.fieldView.extend({
    name:'workflow_entity_name',
    displayName: Em.I18n.t('apps.item.dag.job')
  }),
  idSort: sort.fieldView.extend({
    name:'id',
    displayName: Em.I18n.t('apps.item.dag.jobId')
  }),
  statusSort: sort.fieldView.extend({
    name:'status',
    displayName: Em.I18n.t('apps.item.dag.status')
  }),
  mapsSort: sort.fieldView.extend({
    name:'maps',
    displayName: Em.I18n.t('apps.item.dag.maps')
  }),
  reducesSort: sort.fieldView.extend({
    name:'reduces',
    displayName: Em.I18n.t('apps.item.dag.reduces')
  }),
  inputSort: sort.fieldView.extend({
    name:'input',
    displayName: Em.I18n.t('apps.item.dag.input')
  }),
  outputSort: sort.fieldView.extend({
    name:'output',
    displayName: Em.I18n.t('apps.item.dag.output')
  }),
  durationSort: sort.fieldView.extend({
    name:'elapsed_time',
    displayName: Em.I18n.t('apps.item.dag.duration')
  })
});
