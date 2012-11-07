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
      item.set('numJobsTotal' ,item.get('jobs').get('content').length);
      item.get('jobs').forEach(function(item){

      });
    });
    return content;
  }.property('App.router.mainAppsController.content'),
  /*types: function(){
    var result = new Array();
    this.get('content').forEach(function(item){
      result.push(item.get('type'));
    });
    result = $.unique(result);
    return result;
  }.property('content'),*/
  oTable:null,
  filtered:null,
  clearFilters:function(event){
    this._childViews.forEach(function(item){
      if(item.get('tagName') === 'input') {
          item.set('value','');
      }
      if(item.get('tagName') === 'select') {
        item.set('value','Any');
      }
    });
    this.get('oTable').fnFilterClear();
    this.set('filtered',this.get('oTable').fnSettings().fnRecordsDisplay());
  },
  didInsertElement:function () {
    var oTable = this.$('#dataTable').dataTable({
      "sDom": '<"search-bar"f><"clear">rt<"page-bar"lip><"clear">',
      "fnDrawCallback": function( oSettings ) {
        //change average info after table filtering
      },
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
    this.set('filtered', oTable.fnSettings().fnRecordsDisplay());
  },
//Column filter views
  typeSelectView: Em.Select.extend({
    classNames:['input-small'],
    selected: 'Any',
    content:['Any', 'Pig', 'Hive', 'mapReduce'],
    change:function(event){
      if(this.get('selection') === 'Any') {
        this._parentView.get('oTable').fnFilter('', 2);
      } else {
      this._parentView.get('oTable').fnFilter(this.get('selection'), 2);
      }
      this._parentView.set('filtered',this._parentView.get('oTable').fnSettings().fnRecordsDisplay());
    }
  }),
  rundateSelectView: Em.Select.extend({
    change:function(e) {
      console.log(this.get('selection'));
    },
    content: ['Any', 'Running Now', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days', 'Custom'],
    selected: 'Any',
    classNames:['input-medium'],
    elementId: 'rundate_filter',
    change:function(e) {
      this._parentView.get('oTable').fnFilter('', 8);
    }
  }),
  appidFilterView: Em.TextField.extend({
    classNames:['input-small'],
    type:'text',
    placeholder: 'Any ID',
    filtering:function(){
      console.log(this.get('value'));
      this._parentView.get('oTable').fnFilter(this.get('value') ,0);
      this._parentView.set('filtered',this._parentView.get('oTable').fnSettings().fnRecordsDisplay());
    }.observes('value')
  }),
  nameFilterView: Em.TextField.extend({
    classNames:['input-small'],
    type:'text',
    placeholder: 'Any Name',
    filtering:function(){
      this._parentView.get('oTable').fnFilter(this.get('value') ,1);
      this._parentView.set('filtered',this._parentView.get('oTable').fnSettings().fnRecordsDisplay());
    }.observes('value')
  }),
  userFilterView: Em.View.extend({
    classNames:['btn-group'],
    template: Ember.Handlebars.compile(
        '<a class="btn dropdown-toggle" data-toggle="dropdown" href="#">'+
        'User<span class="caret"></span></a>'+
        '<ul class="dropdown-menu filter-components">'+
        '<li><a><input type="checkbox">1</a></li>'+
        '<li><a><input type="checkbox">1</a></li>'+
        '</ul>'
    )
  }),
  jobsFilterView: Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder: 'Any ',
    elementId:'jobs_filter',
    filtering:function(){
      this._parentView.get('oTable').fnFilter('', 4);
      this._parentView.set('filtered',this._parentView.get('oTable').fnSettings().fnRecordsDisplay());
    }.observes('value')
  }),
  inputFilterView: Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder: 'Any ',
    elementId: 'input_filter',
    filtering:function(){
      this._parentView.get('oTable').fnFilter('' ,5);
      this._parentView.set('filtered',this._parentView.get('oTable').fnSettings().fnRecordsDisplay());
    }.observes('value')
  }),
  outputFilterView: Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder: 'Any ',
    elementId: 'output_filter',
    filtering:function(){
      this._parentView.get('oTable').fnFilter('' ,6);
      this._parentView.set('filtered',this._parentView.get('oTable').fnSettings().fnRecordsDisplay());
    }.observes('value')
  }),
  durationFilterView: Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder: 'Any ',
    elementId: 'duration_filter',
    filtering:function(){
      this._parentView.get('oTable').fnFilter('' ,7);
      this._parentView.set('filtered',this._parentView.get('oTable').fnSettings().fnRecordsDisplay());
    }.observes('value')
  }),

  /**
   * This Container View is used to render static table row(appTableRow) and additional dynamic content
   */
  containerRow : Em.ContainerView.extend({

    /**
     * Uniq row id
     */
    id: function(){
      return this.get('run.id');
    }.property("run.id"),

    /**
     * Variable for dynamic view
     */
    contentView : null,

    /**
     * Show additional content appropriated for this row
     */
    expand : function(){
      var view = Ember.TextArea.create();
      this.set('contentView', view);
      this.get('childViews').pushObject(view);
      this.set('controller.expandedRowId', this.get('id'));
    },

    /**
     * Check whether user opens another row. If yes - hide current content
     */
    hideExpand : function(){
      var contentView = this.get('contentView');
      if(this.get('controller.expandedRowId') !== this.get('id') && contentView){
        this.get('childViews').removeObject(contentView);
        contentView.destroy();
        this.set('contentView', null);
      }
    }.observes('controller.expandedRowId')
  }),

  appTableRow: Em.View.extend({
    classNames:['app-table-row'],
    classNameBindings: ['rowClass'],
    tagName: "tr",
    rowOpened:0,
    mouseEnter: function(event, view){
      $(event.currentTarget).addClass("hover")
    },
    mouseLeave: function(event,view) {
      $(event.currentTarget).removeClass("hover");
    },
    click: function(event,view){
      var target=$(event.currentTarget);
      // if rowOpend=1 new value=0 and visaversa
      this.set("rowOpened",1- this.get("rowOpened"));

      this.get('parentView').expand();

      if(!target.next().hasClass("under-row")){
        $(".under-row").remove();
        this.drawUnderRow(target);
      }else{
        $(".under-row").remove();
      }
    },
    rowClass: function () {
      return this.get('rowOpened') ? "row-opened" : "row-closed";
    }.property('rowOpened'),
    drawUnderRow:function(elem){
     $("" +
      "<tr class='under-row'>" +
        "<td colspan='9'>" +
          'DAG & BAR placeholder'
         + "</td>" +
      "</tr>").insertAfter(elem);
      /*this.Appview = App.MainAppsRunsJobsView.create();
      this.Appview.appendTo('#hhhh');*/
    },
    deleteUnderRow: function(elem){
      elem.prev().remove();
    },

    templateName : require('templates/main/apps/list_row')

  })

});
