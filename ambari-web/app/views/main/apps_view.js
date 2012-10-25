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
var date = require('utils/date');

App.MainAppsView = Em.View.extend({
  templateName:require('templates/main/apps'),
  classNames:['table', 'dataTable'],
  oTable:null,
  types:function () {
    var result = new Array();
    var content = this.get('content');
    content.forEach(function (item) {
      result.push(item.get('type'));
    });
    return result;
  }.property('content'),
  uniqueTypes:function () {
    return this.get('controller').get('arrayUnique')(this.get('types'));
  }.property('types'),
  filterTypesView:Em.CollectionView.extend({
    tagName:'span',
    parentView:null,
    content:function () {
      var content = new Array();
      this.set('parentView', this._parentView);
      content.push({label:'All', active:'active'});
      for (var i = 0; i < this._parentView.get('uniqueTypes').length; i++) {
        content.push({
          label:this._parentView.get('uniqueTypes')[i],
          active:''
        })
      }
      return content;
    }.property('view.uniqueTypes'),
    filterByType:function (event) {
      var type = (event.context.label === 'All') ? '' : event.context.label;
      event.view._parentView.get('parentView').get('oTable').fnFilter(type, 1);
    },
    itemViewClass:Em.View.extend({
      tagName:'span',
      classNames:['btn', 'btn-link'],

      filterByType:function (event) {
        event.view._parentView.get('filterByType')(event);
      },
      template:Ember.Handlebars.compile('<a {{action "filterByType" view.content target="view"}}>{{view.content.label}}</a><p></p>')
    })

  }),
  didInsertElement:function () {
    var oTable = this.$('#dataTable').dataTable({
      "aoColumns":[
        null,
        null,
        null,
        null,
        null,
        { "sType":"ambari-date" },
        null
      ]
    });
    this.set('oTable', oTable);
  },
  content:function () {
    var content = App.router.get('mainAppsController.content');
    content.forEach(function (item) {
      item.set('numRuns', item.get('runs').get('content').length);
      item.set('executionTime', date.dateFormat(item.get('executionTime')));
    });
    return content;
  }.property('App.router.mainAppsController.content')
});
