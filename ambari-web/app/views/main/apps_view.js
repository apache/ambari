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
  content:function () {
    var content =  this.get('controller').get('content');
    content.forEach(function(item){
      var app = App.store.find(App.App, item.get('appId'));
      item.set('appName', app.get('appName'));
      item.set('type', app.get('type'));
      item.set('lastUpdateTime', date.dateFormat(item.get('lastUpdateTime')));
    });
    return content;
  }.property('App.router.mainAppsController.content'),
  types: function(){
    var result = new Array();
    this.get('content').forEach(function(item){
      result.push(item.get('type'));
    });
    result = $.unique(result);
    return result;
  }.property('content'),
  oTable:null,
  didInsertElement:function () {
    var oTable = this.$('#dataTable').dataTable({
      "sDom": '<"search-bar"f>rt<"page-bar"lip><"clear">',
      "oLanguage": {
        "sSearch": "<i class='icon-question-sign'>&nbsp;Search</i>",
        "sLengthMenu": "Show: _MENU_",
        "sInfo": "_START_ - _END_ of _TOTAL_",
        "oPaginate":{
          "sPrevious": "<i class='icon-arrow-left'></i>",
          "sNext": "<i class='icon-arrow-right'></i>"
        }
      },
      "bSortCellsTop": true,
      "iDisplayLength": 10,
      "aLengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
      "aoColumns":[
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        { "sType":"ambari-date" }
      ]
    });
    this.set('oTable', oTable);
  },
  typeSelectView: Em.Select.extend({
    classNames:['input-small'],
    selected: 'Any',
    content: function(){
      this._parentView.get('types').push('Any');
      return this._parentView.get('types');
    }.property('view.types'),

    /*types:function(){
        function stripTags( str ){
            return str.replace(/<\/?[^>]+>/gi, '');
        };
        var columnData = new Array('Any');
        var length = this._parentView.get('oTable').fnSettings().fnRecordsTotal();
        for(var i = 0; i < length; i++) {
            columnData.push(stripTags(this._parentView.get('oTable').fnGetData(i,2)));
        }
        return jQuery.unique(columnData);
    }.property(),*/
    change:function(event){
      if(this.get('selection') === 'Any') {
        this._parentView.get('oTable').fnFilter('', 2);
        return;
      }
      this._parentView.get('oTable').fnFilter(this.get('selection'), 2);
    }
  }),
  rundateSelectView: Em.Select.extend({
    change:function(e) {
      console.log(this.get('selection'));
    },
    content: ['Any', 'Running Now', 'Past 1 Day', 'Past 2 Day', 'Past 7 Day', 'Past 14 Day', 'Past 30 Day', 'Custom'],
    selected: 'Any',
    classNames:['input-medium']
  }),
  appidFilterView: Em.TextField.extend({
    classNames:['input-small'],
    type:'text',
    placeholder: 'Any ID',
    filtering:function(){
      console.log(this.get('value'));
      this._parentView.get('oTable').fnFilter(this.get('value') ,0);
    }.observes('value')
  })
});
