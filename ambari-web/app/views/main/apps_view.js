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
var validator = require('utils/validator');

App.MainAppsView = Em.View.extend({
  templateName:require('templates/main/apps'),
  /**
   * List of runs
   */
  content:function () {
    var content =  this.get('controller').get('content');
    content.forEach(function(run){
      var app = App.store.find(App.App, run.get('appId'));
      run.set('appName', app.get('appName'));
      run.set('type', app.get('type'));
      run.set('numJobsTotal' ,run.get('jobs').get('content').length);
      run.get('jobs').forEach(function(job) {

      });
    });
    return content;
  }.property('App.router.mainAppsController.content'),

  /**
   * Choose view type for apps list:
   * all - show all runs
   * filtered - show only filtered runs
   * starred - show only filtered runs with stars selected
   */
  viewType : 'all',

  /**
   * List of users
   */
  users: function() {
    var result = new Array();
    this.get('content').forEach(function(item) {
       result.push(item.get('userName'));
    });
    return jQuery.unique(result);
  }.property('content'),
  /**
   * jQuery dataTable object
   */
  oTable:null,
  /**
   * jQuery collection of stars icons (in dataTables). Saved here for easy "turning off"
   */
  smallStarsIcons: null,
  /**
   * Count of filtered runs
   */
  filtered:null,
  /**
   * Flag for avgData
   */
  whatAvgShow: true, // true - for filtered data, false - for starred
  /**
   * avg data for display. Can be stared or filtered. Based on whatAvgShow
   */
  avgData: function() {
    if (this.get('whatAvgShow')) {
      return this.get('filteredData');
    }
    else {
      return this.get('staredData');
    }
  }.property('filteredData', 'staredData', 'whatAvgShow'),
  /**
   * Avg data of filtered runs
   */
  filteredData: function() {
    return this.getAvgData(this.get('controller.filteredRuns'));
  }.property('controller.filteredRunsLength'),
  /**
   * Avg data of stared runs
   */
  staredData: function() {
    return this.getAvgData(this.get('controller.staredRuns'));
  }.property('controller.staredRunsLength'),
  setFilteredRuns: function(data) {

  },
  /**
   * Common method for calculation avg data (filtered or stored - based on param content)
   * @param content data-array
   * @return {*} array with calculated data
   */
  getAvgData: function(content) {
    var avgJobs = 0.0, minJobs = 0, maxJobs = 0, avgInput = 0, minInput = 0, maxInput = 0, avgOutput = 0, minOutput = 0, maxOutput = 0, avgDuration = 0.0, minDuration = 0, maxDuration = 0, oldest = 0, youngest = 0;
    if (content.length > 0) {
      minJobs = content[0].get('numJobsTotal');
      minInput = content[0].get('input');
      minOutput = content[0].get('output');
      oldest = content[0].get('lastUpdateTime');
      youngest = content[0].get('lastUpdateTime');
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
      if (item.get('lastUpdateTime') < oldest) {
        oldest = item.get('lastUpdateTime');
      }
      else {
        if (item.get('lastUpdateTime') > youngest) {
          youngest = item.get('lastUpdateTime');
        }
      }
    });
    if (oldest != 0) {
      d = new Date(oldest*1);
      oldest = d.toDateString();
    }
    else {
      oldest = '0000-00-00';
    }
    if (youngest != 0) {
      d = new Date(youngest*1);
      youngest = d.toDateString();
    }
    else {
      youngest = '0000-00-00';
    }
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
  },
  /**
   * Click on big star on the avg block
   */
  avgStarClick: function() {
    this.set('whatAvgShow', !this.get('whatAvgShow'));
    $('a.icon-star.a').toggleClass('active');
  },
  /**
   * "turn off" stars in the table
   */
  clearStars: function() {
    if (this.get('controller.staredRunsLength') == 0 && this.get('smallStarsIcons') != null) {
      this.get('smallStarsIcons').removeClass('stared');
      this.set('whatAvgShow', true);
      $('a.icon-star.a').removeClass('active');
    }
  }.observes('controller.staredRunsLength'),
  /**
   * Reset filters and "turn off" stars
   */
  showAll: function() {
    this.clearFilters();
    this.clearStars();
  },

  /**
   * Onclick handler for <code>Show All/Filtered/Starred</code> links
   */
  changeViewType: function(event){
    if($(event.toElement).hasClass('selected')){
      return;
    }

    $(event.toElement).parent().children('.selected').removeClass('selected');
    $(event.toElement).addClass('selected');

    var viewType = $(event.toElement).data('view-type');
    console.log(viewType);

    switch(viewType){
      case 'all':
        //do stuff here
        break;

      case 'starred':
        break;

      case 'filtered':
      default:
        //do stuff here
        break;
    };

  },

  /**
   * jQuery dataTable init
   */
  didInsertElement:function () {
    var smallStars = $('#dataTable .icon-star');
    this.set('smallStarsIcons', smallStars);
    var oTable = this.$('#dataTable').dataTable({
      "sDom": '<"search-bar"f><"clear">rt<"page-bar"lip><"clear">',
      "fnDrawCallback": function( oSettings ) {
        //change average info after table filtering
        // no need more. 31.10.2012
      },
      "oLanguage": {
        "sSearch": "Search:",
        "sLengthMenu": "Show: _MENU_",
        "sInfo": "_START_ - _END_ of _TOTAL_ (_TOTAL_ total)",
        "sInfoEmpty": "0 - _END_ of _TOTAL_ (_TOTAL_ total)",
        "sInfoFiltered": "",
        "oPaginate":{
          "sPrevious": "<i class='icon-arrow-left'></i>",
          "sNext": "<i class='icon-arrow-right'></i>"
        }
      },
      "bSortCellsTop": true,
      "iDisplayLength": 10,
      "aLengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
      "aoColumns":[
        { "bSortable": false  },
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
  clearFilters:function(event) {
    this._childViews.forEach(function(item) {
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
    this.setFilteredRuns(this.get('oTable')._('tr', {"filter":"applied"}));
  },
  /**
   * apply each filter to dataTable
   *
   * @param {parentView}
   * @param {iColumn} number of column by which filter
   * @param {value}
   */
  applyFilter:function(parentView, iColumn, value) {
      value = (value) ? value : '';
      parentView.get('oTable').fnFilter(value, iColumn);
      parentView.set('filtered',parentView.get('oTable').fnSettings().fnRecordsDisplay());
  },
  /**
   * refresh average info in top block when filtered changes
   */
  averageRefresh:function() {
    var rows = this.get('oTable')._('tr', {"filter":"applied"});
    this.get('controller').clearFilteredRuns();
    for(var i = 0; i < rows.length; i++) {
      this.get('controller').addFilteredRun($(rows[i][0]).find('span.hidden').text());
    }
  }.observes('filtered'),
  /**
   * dataTable filter views
   */
  typeSelectView: Em.Select.extend({
    classNames: ['input-small'],
    selected: 'Any',
    content:['Any', 'Pig', 'Hive', 'mapReduce'],
    change:function(event){
      if(this.get('selection') === 'Any') {
        this._parentView.get('oTable').fnFilter('', 3);
      } else {
      this._parentView.get('oTable').fnFilter(this.get('selection'), 3);
      }
      this._parentView.set('filtered',this._parentView.get('oTable').fnSettings().fnRecordsDisplay());
    }
  }),
  /**
   * Filter-field for RunDate
   */
  rundateSelectView: Em.Select.extend({
    change:function(e) {
      console.log(this.get('selection'));
    },
    content: ['Any', 'Running Now', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    selected: 'Any',
    classNames:['input-medium'],
    elementId: 'rundate_filter',
    change:function(event) {
      this.get('parentView').get('applyFilter')(this.get('parentView'), 9);
    }
  }),
  /**
   * Filter-field for AppId
   */
  appidFilterView: Em.TextField.extend({
    classNames:['input-small'],
    type:'text',
    placeholder: 'Any ID',
    elementId:'appid_filter',
    filtering:function() {
      this.get('parentView').get('applyFilter')(this.get('parentView'), 1, this.get('value'));
    }.observes('value')
  }),
  /**
   * Filter-field for name
   */
  nameFilterView: Em.TextField.extend({
    classNames:['input-small'],
    type:'text',
    placeholder: 'Any Name',
    filtering:function(){
      this.get('parentView').get('applyFilter')(this.get('parentView'), 2, this.get('value'));
    }.observes('value')
  }),
  /**
   * Filter-field for jobs
   */
  jobsFilterView: Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder: 'Any ',
    elementId:'jobs_filter',
    filtering:function(){
      this.get('parentView').get('applyFilter')(this.get('parentView'), 5);
    }.observes('value')
  }),
  /**
   * Filter-field for Input
   */
  inputFilterView: Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder: 'Any ',
    elementId: 'input_filter',
    filtering:function(){
      this.get('parentView').get('applyFilter')(this.get('parentView'), 6);
    }.observes('value')
  }),
  /**
   * Filter-field for Output
   */
  outputFilterView: Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder: 'Any ',
    elementId: 'output_filter',
    filtering:function(){
        this.get('parentView').get('applyFilter')(this.get('parentView'), 7);
    }.observes('value')
  }),
  /**
   * Filter-field for Duration
   */
  durationFilterView: Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder: 'Any ',
    elementId: 'duration_filter',
    filtering:function(){
      this.get('parentView').get('applyFilter')(this.get('parentView'), 8);
    }.observes('value')
  }),
  /**
   * Filter-list for User
   */
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
    toggleAllComponents: function() {
      var checked = this.get('allComponentsChecked');
      this.get('users').forEach(function(item){
        item.set('checked',checked);
      });
    }.observes('allComponentsChecked'),
    clickFilterButton:function(event) {
      this.set('open', !this.get('open'));
      this.set('isApplyDisabled', !this.get('isApplyDisabled'));
    },
    clearFilter:function(self) {
      self.set('allComponentsChecked', true);
      self.set('allComponentsChecked', false);
      jQuery('#user_filter').val([]);
      self.get('parentView').get('oTable').fnFilter('', 3);
    },
    applyFilter:function() {
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
   *  Object contain views of expanded row
   */
  expandedRow:Ember.Object.create({
    rowView:null,
    rowChildView:null
  }),
  /**
   * This Container View is used to render static table row(appTableRow) and additional dynamic content
   */
  containerRow : Em.ContainerView.extend({

    /**
     * Unique row id
     */
    id: function() {
      return this.get('run.id');
    }.property("run.id"),
    /**
     * Delete expanded row from table
     *
     * @param row
     */
    deleteRow:function(row){
      row.get('rowChildView').destroy();
      row.set('rowView', null);
      row.set('rowChildView', null);
    },
    /**
     * Show/hide additional content appropriated for this row
     */
    expandToggle : function(){
      var newView = App.MainAppsItemView.create();
      var expandedView = this.get('parentView.expandedRow');
      App.router.get('mainAppsItemController').set('content', this.get('run'));
      if(expandedView.get('rowView')) {
        if(this === expandedView.get('rowView')) {
          this.get('deleteRow')(expandedView);
        } else {
          this.get('deleteRow')(expandedView);
          expandedView.set('rowView', this);
          expandedView.set('rowChildView', newView);
          this.get('childViews').pushObject(newView);
        }
      } else {
          expandedView.set('rowView', this);
          expandedView.set('rowChildView', newView);
          this.get('childViews').pushObject(newView);
      }
    }
  }),
  /**
   * Table-row view
   */
  appTableRow: Em.View.extend({
    templateName : require('templates/main/apps/list_row'),
    classNames:['app-table-row'],
    tagName: "tr",
    mouseEnter: function(event, view){
      $(event.currentTarget).addClass("hover")
    },
    mouseLeave: function(event,view) {
      $(event.currentTarget).removeClass("hover");
    },
    click: function(event,view){
      this.get('parentView').expandToggle();
    }


  })

});
