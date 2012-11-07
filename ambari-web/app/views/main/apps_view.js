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
      item.set('duration', date.dateFormatInterval(((item.get('lastUpdateTime') - item.get('startTime'))/1000)));
      item.set('lastUpdateTime', date.dateFormat(item.get('lastUpdateTime')));
      item.set('numJobsTotal' ,item.get('jobs').get('content').length);
      item.get('jobs').forEach(function(item){

      });
    });
    return content;
  }.property('App.router.mainAppsController.content'),
  users: function(){
    var result = new Array();
    this.get('content').forEach(function(item){
       result.push(item.get('userName'));
    });
    return jQuery.unique(result);
  }.property('content'),
  oTable:null,
  filtered:null,
  stared: function() {
    var content =  this.get('controller.staredRuns');
    var avgJobs = 0.0, minJobs = 0, maxJobs = 0, avgInput = 0, minInput = 0, maxInput = 0, avgOutput = 0, minOutput = 0, maxOutput = 0, avgDuration = 0.0, minDuration = 0, maxDuration = 0, oldest = 0, youngest = 0;
    if (content.length > 0) {
      minJobs = content[0].get('numJobsTotal');
      minInput = content[0].get('input');
      minOutput = content[0].get('output');
      oldest = date.dateUnformat(content[0].get('lastUpdateTime'));
      youngest = date.dateUnformat(content[0].get('lastUpdateTime'));
      minDuration = date.dateUnformatInterval(content[0].get('duration'));
    }
    content.forEach(function(item) {
      avgJobs += item.get('numJobsTotal') / content.length;
      avgInput += item.get('input') / content.length;
      avgOutput += item.get('output') / content.length;
      avgDuration += date.dateUnformatInterval(item.get('duration')) / content.length;
      if (item.get('numJobsTotal') < minJobs) {
        minJobs = item.get('numJobsTotal');
      }
      else {
        if (item.get('numJobsTotal') > maxJobs) {
          maxJobs = item.get('numJobsTotal');
        }
      }
      if (item.get('input') < minInput) {
        minInput = item.get('input');
      }
      else {
        if (item.get('input') > maxInput) {
          maxInput = item.get('input');
        }
      }
      if (item.get('output') < minOutput) {
        minOutput = item.get('output');
      }
      else {
        if (item.get('output') > maxOutput) {
          maxOutput = item.get('output');
        }
      }
      if (date.dateUnformatInterval(item.get('duration')) < minDuration) {
        minDuration = date.dateUnformatInterval(item.get('duration'));
      }
      else {
        if (date.dateUnformatInterval(item.get('duration')) > maxDuration) {
          maxDuration = date.dateUnformatInterval(item.get('duration'));
        }
      }
      if (date.dateUnformat(item.get('lastUpdateTime')) < oldest) {
        oldest = date.dateUnformat(item.get('lastUpdateTime'));
      }
      else {
        if (date.dateUnformat(item.get('lastUpdateTime')) > youngest) {
          youngest = date.dateUnformat(item.get('lastUpdateTime'));
        }
      }
    });
    oldest = oldest != 0 ? oldest.substring(0, 4) + '-' + oldest.substring(4, 6) + '-' + oldest.substring(6, 8) : '';
    youngest = youngest != 0 ? youngest.substring(0, 4) + '-' + youngest.substring(4, 6) + '-' + youngest.substring(6, 8) : '';
    ret = {
      'count': this.get('controller.staredRuns').length,
      'jobs': {
        'avg': avgJobs.toFixed(2),
        'min': minJobs,
        'max': maxJobs
      },
      'input': {
        'avg': avgInput.toFixed(2),
        'min': minInput,
        'max': maxInput
      },
      'output': {
        'avg': avgOutput.toFixed(2),
        'min': minOutput,
        'max': maxOutput
      },
      'duration': {
        'avg': date.dateFormatInterval(Math.round(avgDuration)),
        'min': date.dateFormatInterval(minDuration),
        'max': date.dateFormatInterval(maxDuration)
      },
      'times': {
        'oldest': oldest,
        'youngest': youngest
      }
    };
    return ret;
  }.property('controller.staredRunsLength'),
  /*starsStats: function() {
    var content =  this.get('controller.staredRuns');
    var avgJobs = 0.0, minJobs = 0, maxJobs = 0, avgInput = 0, minInput = 0, maxInput = 0, avgOutput = 0, minOutput = 0, maxOutput = 0;
    content.forEach(function(item) {
      avgJobs += item.get('numJobsTotal') / content.length;
    });
    this.set('avgJobs', 1);
  }.property('controller.staredRunsLength'),*/
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
  /**
   * reset all filters in dataTable
   *
   * @param event
   */
  clearFilters:function(event){
    this._childViews.forEach(function(item){
    if(item.get('tagName') === 'input') {
      item.set('value','');
    }
    if(item.get('tagName') === 'select') {
      item.set('value','Any');
    }
    if(item.get('multiple')) {
      item.get('clearFilter')(item);
    }
    });
    this.get('oTable').fnFilterClear();
    this.set('filtered',this.get('oTable').fnSettings().fnRecordsDisplay());
  },
  /**
   * apply each filter to dataTable
   *
   * @param {parentView}
   * @param {iColumn} number of column by which filter
   * @param {value}
   */
  applyFilter:function(parentView, iColumn, value){
      value = (value) ? value : '';
      parentView.get('oTable').fnFilter(value, iColumn);
      parentView.set('filtered',parentView.get('oTable').fnSettings().fnRecordsDisplay());
  },
  /**
   * refresh average info in top block when filtered changes
   */
  averageRefresh:function(){

  }.observes('filtered'),
  /**
   * dataTable filter views
   */
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
        this.get('parentView').get('applyFilter')(this.get('parentView'), 6);
    }.observes('value')
  }),
  durationFilterView: Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder: 'Any ',
    elementId: 'duration_filter',
    filtering:function(){
      this.get('parentView').get('applyFilter')(this.get('parentView'), 7);
    }.observes('value')
  }),
  userFilterView: Em.View.extend({
    classNames:['btn-group'],
    classNameBindings: ['open'],
    multiple:true,
    open: false,
    isApplyDisabled:true,
    users:function(){
      var users = new Array();
      for(var i = 0; i < this.get('parentView').get('users').length; i++)
        users.push(Ember.Object.create({
          name:this.get('parentView').get('users')[i],
          checked:false
        }));
      return users;
    }.property('parentView.users'),
    template: Ember.Handlebars.compile(
      '<button class="btn btn-info" '+
      '{{action "clickFilterButton" target="view"}}>'+
      'User&nbsp;<span class="caret"></span></button>'+
      '<ul class="dropdown-menu filter-components">'+
      '<li><label class="checkbox">' +
      '{{view Ember.Checkbox checkedBinding="view.allComponentsChecked"}} All</label></li>'+
      '{{#each user in view.users}}<li><label class="checkbox">' +
      '{{view Ember.Checkbox checkedBinding="user.checked"}}{{user.name}}'+
      '</label></li>{{/each}}</ul>'+
      '<button {{bindAttr disabled="view.isApplyDisabled"}}'+
      'class="btn" {{action "applyFilter" target="view"}}>'+
      'Apply</button>'
    ),
    allComponentsChecked:false,
    toggleAllComponents: function(){
      var checked = this.get('allComponentsChecked');
      this.get('users').forEach(function(item){
        item.set('checked',checked);
      });
    }.observes('allComponentsChecked'),
    clickFilterButton:function(event){
      this.set('open', !this.get('open'));
      this.set('isApplyDisabled', !this.get('isApplyDisabled'));
    },
    clearFilter:function(self){
      self.set('allComponentsChecked', true);
      self.set('allComponentsChecked', false);
      jQuery('#user_filter').val([]);
      self.get('parentView').get('oTable').fnFilter('', 3);
    },
    applyFilter:function(){
      var chosenUsers = new Array();
      this.set('open', !this.get('open'));
      this.set('isApplyDisabled', !this.get('isApplyDisabled'));
      this.get('users').forEach(function(item){
          if(item.get('checked')) chosenUsers.push(item.get('name'));
      });
      jQuery('#user_filter').val(chosenUsers);
      this.get('parentView').get('applyFilter')(this.get('parentView'), 3);
    }
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
    currentId: null,
    /**
     * Variable for dynamic view
     */
    contentView : null,

    /**
     * Show additional content appropriated for this row
     */
    expand : function(){
      var view = App.MainAppsItemView.create();
      this.set('contentView', view);
      this.get('childViews').pushObject(view);
      this.set('currentId', this.get('id'));
      //this.set('controller.expandedRowId', this.get('id'));
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
    templateName : require('templates/main/apps/list_row'),
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

    },
    rowClass: function () {
      return this.get('rowOpened') ? "row-opened" : "row-closed";
    }.property('rowOpened')

  })

});
