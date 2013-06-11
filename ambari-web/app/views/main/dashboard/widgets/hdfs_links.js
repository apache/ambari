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

App.HDFSLinksView = App.DashboardWidgetView.extend({

  title: Em.I18n.t('dashboard.widgets.HDFSLinks'),
  id: '17',

  isPieChart: false,
  isText: false,
  isProgressBar: false,
  isLinks: true,
  model_type: 'hdfs',

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
    //NameNode
    '<tr>',
    '<td>{{t dashboard.services.hdfs.nanmenode}}</td>',
    '<td><a href="#" {{action showDetails view.model.nameNode}}>{{view.model.nameNode.publicHostName}}</a></td>',
    '</tr>',
    //SecondaryNameNode
    '<tr>',
    '<td>{{t dashboard.services.hdfs.snanmenode}}</td>',
    '<td><a href="#" {{action showDetails view.model.snameNode}}>{{view.model.snameNode.publicHostName}}</a></td>',
    '</tr>',
    //Data Nodes
    '<tr>',
    '<td>{{t dashboard.services.hdfs.datanodes}}</td>',
    '<td>',
    '<a href="#" {{action filterHosts view.dataNodeComponent}}>{{view.model.dataNodes.length}} {{t dashboard.services.hdfs.datanodes}}</a>',
    '</td>',
    '</tr>',
    // NameNode Web UI
    //    '<tr>',
    //    '<td>{{t dashboard.services.hdfs.nameNodeWebUI}}</td>',
    //    '<td><a {{bindAttr href="view.nodeWebUrl"}} target="_blank">{{view.model.nameNode.publicHostName}}:50070</a>',
    //    '</td>',
    //    '</tr>',
    '</table>',

    '</div>',
    '</li>',
    '</div>'


  ].join('\n')),

  dataNodeComponent: function () {
    return App.HostComponent.find().findProperty('componentName', 'DATANODE');
  }.property(),

  nodeWebUrl: function () {
    return "http://" + this.get('model').get('nameNode').get('publicHostName') + ":50070";
  }.property('model.nameNode')

})

App.HDFSLinksView. reopenClass({
  isLinks: true
})
