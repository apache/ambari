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

App.MapReduceLinksView = App.DashboardWidgetView.extend({

  title: Em.I18n.t('dashboard.widgets.MapReduceLinks'),
  id: '18',

  isPieChart: false,
  isText: false,
  isProgressBar: false,
  isLinks: true,
  model_type: 'mapreduce',

  template: Ember.Handlebars.compile([
    '<div class="links">',
    '<li class="thumbnail row">',
      '<a class="corner-icon" href="#" {{action deleteWidget target="view"}}>','<i class="icon-remove-sign icon-large"></i>','</a>',
    '<div class="caption span8">', '{{view.title}}','</div>',
    '<div class="span3 link-button">',
    '{{#if view.model.quickLinks.length}}',
      '{{#view App.QuickViewLinks contentBinding="view.model"}}',
        '<div class="btn-group">',
        '<a class="btn btn-mini dropdown-toggle" data-toggle="dropdown" href="#">',
          '{{t common.more}}',
          '<span class="caret"></span>',
        '</a>',
        '<ul class="dropdown-menu">',
          '{{#each view.quickLinks}}',
          '<li><a {{bindAttr href="url"}} target="_blank">{{label}}</a></li>',
          '{{/each}}',
        '</ul>',
        '</div>',
      '{{/view}}',
    '{{/if}}',

    '</div>',
    '<div class="widget-content" >',
    '<table>',
    //jobTracker
    '<tr>',
    '<td>{{t services.service.summary.jobTracker}}</td>',
    '<td><a href="#" {{action showDetails view.model.jobTracker}}>{{view.model.jobTracker.publicHostName}}</a></td>',
    '</tr>',
    //taskTrackers
    '<tr>',
    '<td>{{t dashboard.services.mapreduce.taskTrackers}}</td>',
    '<td><a href="#" {{action filterHosts view.taskTrackerComponent}}>{{view.model.taskTrackers.length}} {{t dashboard.services.mapreduce.taskTrackers}}</a></td>',
    '</tr>',

    // jobTracker Web UI
    '<tr>',
    '<td>{{t services.service.summary.jobTrackerWebUI}}</td>',
    '<td><a {{bindAttr href="view.jobTrackerWebUrl"}} target="_blank">{{view.model.jobTracker.publicHostName}}:50030</a>','</td>',
    '</tr>',
    '</table>',

    '</div>',
    '</li>',
    '</div>'


  ].join('\n')),

  taskTrackerComponent: function () {
    return App.HostComponent.find().findProperty('componentName', 'TASKTRACKER');
  }.property(),

  jobTrackerWebUrl: function () {
    return "http://" + this.get('model').get('jobTracker').get('publicHostName') + ":50030";
  }.property('model.nameNode')

})

App.MapReduceLinksView. reopenClass({
  isLinks: true
})
