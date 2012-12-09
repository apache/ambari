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
    elementId: 'bars',
    templateName: require('templates/main/apps/item/bar'),
    content:function(){
        return this.get('controller.content.jobs');
    }.property('controller.content.jobs'),
    firstJob: function() {
        return this.get('content').get('firstObject');
    }.property('content'),
    activeJob:null,
    selectJob: function(event) {
        this.set('activeJob', event.context);

    },
    onLoad:function () {
        if(!this.get('controller.content.loadAllJobs') || this.get('activeJob')){
          return;
        }

        this.set('activeJob', this.get('firstJob'));
    }.observes('controller.content.loadAllJobs'),
    didInsertElement: function(){
      this.onLoad();
    },
    draw: function() {
        if(!this.get('activeJob')){
            return;//when job is not defined
        }
        width = 300;
        height = 210;
        var desc1 = $('#graph1_desc');
        var desc2 = $('#graph2_desc');
        $('.rickshaw_graph, .rickshaw_legend, .rickshaw_annotation_timeline').html('');
        if (null == desc1.html() || null == desc2.html()) return;
        desc1.css('display','block');
        desc2.css('display','block');
        graph.drawJobTimeLine(this.get('activeJob').get('jobTimeLine'), width, height, '#chart', 'legend', 'timeline1');
        graph.drawJobTasks(this.get('activeJob').get('jobTaskView'), width, height, '#job_tasks', 'tasks_legend', 'timeline2');
    }.observes('activeJob')


});