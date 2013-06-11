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
var appGraph = require('utils/app_graph');

App.MainAppsItemAppView = Em.View.extend({
  templateName: require('templates/main/apps/item/app'),
  elementId : 'jobs',
  content:function(){
    return this.get('controller.content.jobs');
  }.property('controller.content.jobs'),

  classNames:['table','dataTable'],
  /**
   * convert content to special jobs object
   */
  jobs: function(){
    var c = this.get('content');
    var result = [];
    c.forEach(function(item, index){
      result[index] = new Object({
        'name' : item.get('id'),
        'type' : item.get('app_type'),
        'entityName' : item.get('workflow_entity_name'),
        'status' : item.get('status'),
        'submitTime' : item.get('submit_time'),
        'finishTime' : item.get('finish_time'),
        'elapsedTime' : appGraph.formatDuration(item.get('elapsed_time')),
        'numStages' : item.get('num_stages'),
        'stages' : item.get('stages')
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

  draw: function(){
    var dagSchema = this.get('controller.content.workflowContext');
    var jobs = this.get('jobs');
    this.resizeModal();
    appGraph.create('app_graph', dagSchema, jobs, this.$().width(), 300);
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
  typeSort: sort.fieldView.extend({
    name:'app_type',
    displayName: Em.I18n.t('apps.item.dag.type')
  }),
  statusSort: sort.fieldView.extend({
    name:'status',
    displayName: Em.I18n.t('apps.item.dag.status')
  }),
  numStagesSort: sort.fieldView.extend({
    name:'num_stages',
    displayName: Em.I18n.t('apps.item.dag.num_stages')
  }),
  stagesSort: sort.fieldView.extend({
    name:'stages',
    displayName: Em.I18n.t('apps.item.dag.stages')
  }),
  durationSort: sort.fieldView.extend({
    name:'elapsed_time',
    displayName: Em.I18n.t('apps.item.dag.duration')
  })
});
