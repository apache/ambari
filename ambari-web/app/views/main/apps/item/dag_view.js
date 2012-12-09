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
    var content = this.get('parentView.jobs');
    content.forEach(function(job){
      job.set('mapsProgress', Math.round((job.get('finishedMaps') / job.get('maps'))*100));
      job.set('reducesProgress', Math.round((job.get('finishedReduces') / job.get('reduces'))*100));
    });
    return content;
  }.property('parentView.jobs'),
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
        'status' : item.get('status') == 'COMPLETE',
        'info' : [],
        'input' : 2,
        'output' : 3
      })
    });
    return result;
  }.property('content'),
  didInsertElement:function (){
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
    var dagSchema = this.get('parentView.parentView.content').get('workflowContext');
    var jobs = this.get('jobs');
    var graph = new DagViewer(false, 'dag_viewer')
        .setPhysicalParametrs(800, 300, -800, 0.01)
        .setData(dagSchema, jobs)
        .drawDag(10, 20, 100);
  }
});
