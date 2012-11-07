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
  content:function(){
    return this.get('parentView.jobs');
  }.property('parentView.jobs'),
  classNames:['table','dataTable'],
  jobs: function(){
    var c = this.get('content');
    var result = new Array();
    c.forEach(function(item, index){
      result[index] = new Object({
        'name' : item.get('id'),
        'status' : (item.get('status') == 'COMPLETE')? true : false,
        'info' : [],
        'input' : 2,
        'output' : 3
      })
    });
    return result;
  }.property('content'),
  didInsertElement:function (){
    var innerTable = this.$('#innerTable').dataTable({});
    var dagSchema = this.get('parentView.parentView.run').get('workflowContext');
    var jobs = this.get('content');
    var graph = new DagViewer(false,'dag_viewer')
        .setPhysicalParametrs(800,250,-800,0.01)
        .setData(dagSchema,jobs)
        .drawDag(10,20,100);
  }
});
