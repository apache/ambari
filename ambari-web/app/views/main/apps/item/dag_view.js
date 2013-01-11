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
        'entityName' : item.get('workflowEntityName'),
        'status' : item.get('status') == 'SUCCESS',
        'info' : [],
        'input' : item.get('inputBytes'),
        'output' : item.get('outputBytes')
      })
    });
    return result;
  }.property('content'),

  loaded : false,

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

    var innerTable = this.$('#innerTable').dataTable({
      "sDom": 'rt<"page-bar"lip><"clear">',
      "oLanguage": {
        "sSearch": "<i class='icon-question-sign'>&nbsp;Search</i>",
        "sLengthMenu": "Show: _MENU_",
        "sInfo": "_START_ - _END_ of _TOTAL_",
        "oPaginate":{
          "sPrevious": "<i class='icon-arrow-left'></i>",
          "sNext": "<i class='icon-arrow-right'></i>"
        }
      },
      "iDisplayLength": 5,
      "aLengthMenu": [[5, 10, 25, 50, -1], [5, 10, 25, 50, "All"]],
      "aaSorting": [],
      "aoColumns":[
        null,
        null,
        null,
        null,
        null,
        { "sType":"ambari-bandwidth" },
        { "sType":"ambari-bandwidth" },
        null
      ]
    });

    // Hard reset filter settings
    innerTable.fnSettings().aiDisplay = innerTable.fnSettings().aiDisplayMaster.slice();
    // Redraw table
    innerTable.fnDraw(false);
    innerTable.fnSettings().oFeatures.bFilter = false;
    var dagSchema = this.get('controller.content.workflowContext');
    var jobs = this.get('jobs');
    this.resizeModal();
    var graph = new DagViewer(false, 'dag_viewer')
        .setPhysicalParametrs(this.$().width(), 300, -800, 0.01)
        .setData(dagSchema, jobs)
        .drawDag(10, 20, 100);
  }
});
