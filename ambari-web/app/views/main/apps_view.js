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
var misc = require('utils/misc');

App.MainAppsView = Em.View.extend({
  templateName:require('templates/main/apps'),
  /**
   * List of runs
   */
  content:function () {
    return this.get('controller.content');
  }.property('controller.content'),

  /**
   * Choose view type for apps list:
   * all - show all runs
   * filtered - show only filtered runs
   * starred - show only filtered runs with stars selected
   */
  viewType : 'all',
  defaultViewType: 'all',
  /**
   * List of users
   */
  users: function() {
    var result = [];
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

  stared: function() {
    return this.get('controller.staredRunsLength');
  }.property('controller.staredRunsLength'),
  /**
   * Count of filtered runs
   */
  filtered: null,
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
    return this.getAvgData(this.get('controller.content').filterProperty('isFiltered', true));
  }.property('controller.filteredRunsLength'),
  /**
   * Avg data of stared runs
   */
  staredData: function() {
    return this.getAvgData(this.get('controller.content').filterProperty('isStared', true));
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
    avgInput = misc.formatBandwidth(avgInput);
    minInput = misc.formatBandwidth(minInput);
    maxInput = misc.formatBandwidth(maxInput);
    avgOutput = misc.formatBandwidth(avgOutput);
    minOutput = misc.formatBandwidth(minOutput);
    maxOutput = misc.formatBandwidth(maxOutput);
    ret = {
      'jobs': {
        'avg': avgJobs.toFixed(2),
        'min': minJobs,
        'max': maxJobs
      },
      'input': {
        'avg': avgInput,
        'min': minInput,
        'max': maxInput
      },
      'output': {
        'avg': avgOutput,
        'min': minOutput,
        'max': maxOutput
      },
      'duration': {
        'avg': date.timingFormat(Math.round(avgDuration)),
        'min': date.timingFormat(minDuration),
        'max': date.timingFormat(maxDuration)
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
    if (this.get('viewType') === 'starred') return;
    this.set('whatAvgShow', !this.get('whatAvgShow'));
    $('a.icon-star.a').toggleClass('active');
  },
  /**
   *
   */
  starClicked: function() {
    var runIndex = this.get('controller.lastStarClicked');
    if (!this.get('oTable')) return;
    var rowIndex = -1;
    // Get real row index
    var column = this.get('oTable').fnGetColumnData(1);  // 1 - number of column with full runId (hidden)
    for (var i = 0; i < column.length; i++) {
      if (runIndex == column[i]) {
        rowIndex = i;
        break;
      }
    }
    var perPage = this.get('oTable').fnSettings()['_iDisplayLength']; // How many rows show per page
    var rowIndexVisible = rowIndex; // rowIndexVisible - rowIndex in current visible part of the table
    if (perPage !== -1) { // If not show All is selected we should recalculate row index according to other visible rows
      rowIndexVisible = rowIndex % perPage;
    }
    // Update inner dataTable value
    this.get('oTable').fnUpdate( $('#dataTable tbody tr:eq(' + rowIndexVisible + ') td:eq(0)').html(), this.get('oTable').fnSettings()['aiDisplay'][rowIndex], 0);
    if (perPage !== -1) { // Change page after reDraw (if show All is selected this will not happens)
      this.get('oTable').fnPageChange(Math.floor(rowIndex / perPage));
    }
  }.observes('controller.lastStarClicked'),
  /**
   * Flush all starred runs
   */
  clearStars: function() {
    this.get('controller').get('content').setEach('isStared', false);
    this.get('controller').set('staredRunsLength', 0);
    this.set('viewType', this.get('defaultViewType'));
    this.set('whatAvgShow', true);
    this.get('smallStarsIcons').removeClass('stared');
    $('#dataTable .icon-star').removeClass('stared');
    $('a.icon-star.a').removeClass('active');
  },
  /**
   * "turn off" stars in the table
   */
  resetStars: function() {
    /*var self = this;
    if (this.get('controller.staredRunsLength') != 0 && this.get('smallStarsIcons') != null) {
      this.get('smallStarsIcons').removeClass('stared');
      $('#dataTable .icon-star').removeClass('stared');
      $('a.icon-star.a').removeClass('active');
      this.get('starFilterViewInstance').set('value', '');
      this.updateStars();
    }*/
  }.observes('controller.staredRunsLength'),
  /**
   * Update stars data in dataTable. data taken from page
   * Experimental. Need to be tested.
   */
  updateStars: function() {
    var self = this;
    $('#dataTable tbody tr').each(function(index) {
      self.get('oTable').fnUpdate( $('#dataTable tbody tr:eq(' + index + ') td:eq(0)').html(), self.get('oTable').fnSettings()['aiDisplay'][index], 0);
    });
  },
  /**
   * Reset filters and "turn off" stars
   */
  showAll: function() {
    this.clearFilters();
    this.clearStars();
  },
  /**
   * Display only stared rows
   */
  showStared: function() {
    this.updateStars();
    this.get('starFilterViewInstance').set('value', 'stared');
    this.set('whatAvgShow', false);
    $('a.icon-star.a').addClass('active');
  },
  /**
   * Onclick handler for <code>Show All/Filtered/Starred</code> links
   */
  clickViewType: function(event) {
    this.set('viewType', $(event.target).data('view-type'));
  },
  /**
   *
   */
  onChangeViewType: function(){
    var viewType = this.get('viewType');
    var table = this.get('oTable');
    var filterButtons = $("#filter_buttons").children();
    filterButtons.each(function(index, element){
      $(element).removeClass('selected');
      if(viewType == $(element).data('view-type')){
        $(element).addClass('selected');
      }
    });
    switch(viewType) {
      case 'all':
        table.fnSettings().oFeatures.bFilter = false;
        table.fnDraw();
        table.fnSettings().oFeatures.bFilter = true;
        break;
      case 'starred':
        this.showStared();
        break;
      case 'filtered':
        table.fnSettings().oFeatures.bFilter = true;
        table.fnDraw();
        break;
      };


  }.observes('viewType'),

  /**
   * jQuery dataTable init
   */
  createDataTable: function () {
    var smallStars = $('#dataTable .icon-star');
    var self = this;
    this.set('smallStarsIcons', smallStars);
    var d = new Date();
    var oTable = this.$('#dataTable').dataTable({
      "sDom": '<"search-bar"f><"clear">rt<"page-bar"lip><"clear">',
      "fnDrawCallback": function( oSettings ) {
        if(!self.get('oTable')){return}
        if(self.get('viewType') !== 'all') {
          if (self.get('viewType') !== 'starred') {
            self.set('filtered', this.fnSettings().fnRecordsDisplay());
          }
        }
        if(self.get('viewType') === 'all' && self.get('oTable').fnSettings().oFeatures.bFilter){
          self.set('viewType', 'filtered');
          self.set('filtered', this.fnSettings().fnRecordsDisplay());
        }
      },
      "aaSorting": [],
      "oLanguage": {
        "sSearch": "Search:",
        "sLengthMenu": "Show: _MENU_",
        "sInfo": "_START_ - _END_ of _TOTAL_",
        "sInfoEmpty": "0 - _END_ of _TOTAL_",
        "sInfoFiltered": "",
        "oPaginate":{
          "sPrevious": "<i class='icon-arrow-left'></i>",
          "sNext": "<i class='icon-arrow-right'></i>"
        }
      },
      "bSortCellsTop": true,
      "iDisplayLength": -1,
      "aLengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
      "aoColumns":[
        { "bSortable": false  },
        { "bVisible": false },
        null,
        null,
        null,
        null,
        null,
        { "sType":"ambari-bandwidth" },
        { "sType":"ambari-bandwidth" },
        null,
        { "sType":"ambari-datetime" }
      ]
    }).css('visibility', 'visible');
    this.set('oTable', oTable);
    this.set('filtered', oTable.fnSettings().fnRecordsDisplay());

    // If we have some starred runs, mark them again
    var staredRuns = [];
    this.get('controller.').get('content').filterProperty('isStared', true).forEach(function(run) {
      staredRuns.push(run.get('id'));
    });
    this.clearStars();
    var tr = $('#dataTable').find('tr');
    staredRuns.forEach(function(item) {
      tr.each(function() {
        if ($(this).find('td.appId:eq(0)').attr('title') == item) {
          return $(this).find('td a:eq(0) span').trigger('click');
        }
      });
    });
    this.get('oTable').fnSettings()._iDisplayLength = 10;
    $('#dataTable_length select option:eq(0)').attr('selected', 'selected');
    this.get('oTable').fnDraw(false);

    console.log('Rendering Apps Table:');
    console.log('Start - ', d.toLocaleTimeString());
    console.log('End   - ', (new Date()).toLocaleTimeString());
  },

  loaded: false,
  inserted: false,

  onLoad: function(){
    if(this.get('loaded')){
      return;
    }
    if(!App.router.get('clusterController.postLoadList.runs')){
      return;
    }
    if(!this.get('inserted')){
      return;
    }

    this.set('loaded', true);
    var self = this;
    Ember.run.next(function(){
      self.createDataTable();
    });
  }.observes('App.router.clusterController.postLoadList.runs'),

  didInsertElement: function () {
    $('#dataTable').css('visibility', 'hidden'); // hide table before dataTables id initialized
    this.set('inserted', true);
    this.onLoad();
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
    //this.get('controller').clearFilteredRuns();
    this.set('viewType', this.get('defaultViewType'));
    this.set('filtered',this.get('oTable').fnSettings().fnRecordsDisplay());
    this.setFilteredRuns(this.get('oTable')._('tr', {"filter":"applied"}));
  },
  /**
   * Clear selected filter
   * @param event
   */
  clearFilterButtonClick: function(event) {
    var viewName = event.target.id.replace('view_', '');
    var elementId = this.get(viewName).get('elementId');
    if(this.get(viewName).get('tagName') === 'input') {
      this.get(viewName).set('value', '');
    }
    if(this.get(viewName).get('tagName') === 'select') {
      this.get(viewName).set('value', 'Any');
      this.get(viewName).change();
    }
    if(this.get(viewName).get('multiple')) {
      this.get(viewName).get('clearFilter')(this.get(viewName));
    }
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
  },

  /**
   * refresh average info in top block when filtered changes
   */
  averageRefresh:function() {
    var rows = this.get('oTable')._('tr', {"filter":"applied"});
    this.get('controller').clearFilteredRuns();
    var ids = [];
    for(var i = 0; i < rows.length; i++) {
      ids.push(rows[i][1]);
    }
    this.get('controller').filterFilteredRuns(ids);
  }.observes('filtered'),
  /**
   * dataTable filter views
   */
  typeSelectView: Em.Select.extend({
    classNames: ['input-small'],
    selected: 'Any',
    content:['Any', 'Pig', 'Hive', 'MapReduce'],
    change:function(event){
      if(this.get('selection') === 'Any') {
        this.$().closest('th').addClass('notActive');
        this.get('parentView').get('oTable').fnFilter('', 4);
      }
      else {
        this.$().closest('th').removeClass('notActive');
        this.get('parentView').get('oTable').fnFilter(this.get('selection'), 4);
      }
      this.get('parentView').set('filtered',this.get('parentView').get('oTable').fnSettings().fnRecordsDisplay());
    }
  }),
  /**
   * Filter-field for RunDate
   */
  rundateSelectView: Em.Select.extend({
    content: ['Any', 'Running Now', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days', 'Custom'],
    selected: 'Any',
    classNames:['input-medium'],
    elementId: 'rundate_filter',
    change:function(event) {
      if (this.get('selection') == 'Any') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }

      if (this.get('selection') == 'Custom') {
        this.customFilter();
      } else {
        this.get('parentView').get('applyFilter')(this.get('parentView'), 10);
      }
    },

    /**
     * Custom filter view for RunDate
     */
    customFilter: function() {
      var rundateSelect = this;

      /**
       * Show popup with custom filters
       */
      App.ModalPopup.show({
        lowerBound: null,
        upperBound: null,
        equal: null,
        header: Em.I18n.t('apps.filters.customRunDate'),
        bodyClass: Ember.View.extend({
          templateName: require('templates/main/apps/custom_rundate_popup'),

          /**
           * Lower bound view for date-time range on custom filter popup.
           */
          lowerBoundView: Ember.TextField.extend({
            elementId: 'lowerBound',
            classNames: 'lowerBound',
            attributeBindings:['readonly'],
            readonly: true,
            didInsertElement: function() {
              var textField = this;
              var popupBody = this.get('parentView');
              this.$().datetimepicker({
                dateFormat: 'D, M dd, yy',
                timeFormat: 'hh:mm',
                maxDate: new Date(),
                onClose:function (dateText, inst) {
                  var endDateTextBox = $('#upperBound');
                  if (endDateTextBox.val() != '') {
                    var testStartDate = new Date(dateText);
                    var testEndDate = new Date(endDateTextBox.val());
                    if (testStartDate > testEndDate)
                      endDateTextBox.val(dateText);
                  } else {
                    endDateTextBox.val(dateText);
                  }
                },
                onSelect:function (selectedDateTime) {
                  var start = $(this).datetimepicker('getDate');
                  $('#upperBound').datetimepicker('option', 'minDate', new Date(start.getTime()));
                  popupBody.get('parentView').set('lowerBound', selectedDateTime);
                }
              });
            }
          }),

          /**
           * Upper bound view for date-time range on custom filter popup.
           */
          upperBoundView: Ember.TextField.extend({
            elementId: 'upperBound',
            classNames: 'upperBound',
            attributeBindings:['readonly'],
            readonly: true,
            didInsertElement: function() {
              var textField = this;
              var popupBody = this.get('parentView');
              this.$().datetimepicker({
                dateFormat: 'D, M dd, yy',
                timeFormat: 'hh:mm',
                maxDate: new Date(),
                onClose:function (dateText, inst) {
                  var startDateTextBox = $('#lowerBound');
                  if (startDateTextBox.val() != '') {
                    var testStartDate = new Date(startDateTextBox.val());
                    var testEndDate = new Date(dateText);
                    if (testStartDate > testEndDate)
                      startDateTextBox.val(dateText);
                  } else {
                    startDateTextBox.val(dateText);
                  }
                },
                onSelect:function (selectedDateTime) {
                  var end = $(this).datetimepicker('getDate');
                  $('#lowerBound').datetimepicker('option', 'maxDate', new Date(end.getTime()));
                  popupBody.get('parentView').set('upperBound', selectedDateTime);
                }
              });
            }
          }),

          /**
           * Equal view for custom filter popup.
           */
          equalView: Ember.TextField.extend({
            attributeBindings:['readonly'],
            readonly: true,
            didInsertElement: function() {
              var textField = this;
              var popupBody = this.get('parentView');
              this.$().datetimepicker({
                dateFormat: 'D, M dd, yy',
                timeFormat: 'hh:mm',
                maxDate: new Date(),
                onSelect:function (selectedDateTime) {
                  popupBody.get('parentView').set('equal', selectedDateTime);
                }
              });
            }
          })
        }),

        /**
         * apply each filter to dataTable
         * @param lowerBound date-time
         * @param upperBound date-time
         * @param equal date-time
         */
        applyFilter:function(lowerBound, upperBound, equal) {
          if (arguments.length == 1) {
            //do if filtered by equal value
            equal = new Date(arguments[0]).getTime();
            jQuery('#custom_rundate_filter').val(equal);
          } else if (arguments.length == 2) {
            //do if filtered by date-time range
            lowerBound = new Date(arguments[0]).getTime();
            upperBound = new Date(arguments[1]).getTime();
            var range = [lowerBound, upperBound];
            jQuery('#custom_rundate_filter').val(range);
          }
          rundateSelect.get('parentView').get('applyFilter')(rundateSelect.get('parentView'), 10);
          if (arguments.length == 0) {
            rundateSelect.$().closest('th').addClass('notActive');
          }
          else {
            rundateSelect.$().closest('th').removeClass('notActive');
          }
        },
        onPrimary: function() {
          if (this.get('lowerBound') && this.get('upperBound')) {
            if (this.get('lowerBound') === this.get('upperBound')) {
              alert('Range points must be different!');
            } else {
              this.applyFilter(this.get('lowerBound'), this.get('upperBound'));
              this.hide();
            }
          } else if (this.get('equal')) {
            this.applyFilter(this.get('equal'));
            this.hide();
          } else {
            alert('Please specify date and time range or exact date and time!');
          }
        },
        secondary : null
      });
    }
  }),
  /**
   * Filter-field for Stars. hidden
   */
  starFilterView: Em.TextField.extend({
    classNames:['input-small'],
    type:'hidden',
    placeholder: '',
    elementId:'star_filter',
    filtering:function() {
      this.get('parentView').get('applyFilter')(this.get('parentView'), 0);
    }.observes('value')
  }),
  /**
   * Filter-field for AppId
   */
  appidFilterView: Em.TextField.extend({
    classNames:['input-medium'],
    type:'text',
    placeholder: 'Any ID',
    elementId:'appid_filter',
    filtering:function() {
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
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
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
      this.get('parentView').get('applyFilter')(this.get('parentView'), 3, this.get('value'));
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
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
      this.get('parentView').get('applyFilter')(this.get('parentView'), 6);
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
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
      this.get('parentView').get('applyFilter')(this.get('parentView'), 7);
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
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
      this.get('parentView').get('applyFilter')(this.get('parentView'), 8);
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
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
      this.get('parentView').get('applyFilter')(this.get('parentView'), 9);
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
    users:function(){
      var users = [];
      for(var i = 0; i < this.get('parentView').get('users').length; i++)
        users.push(Ember.Object.create({
          name:this.get('parentView').get('users')[i],
          checked:false
        }));
      return users;
    }.property('parentView.users'),
    template: Ember.Handlebars.compile(
      '<button class="btn btn-info single-btn-group"'+
      '{{action "clickFilterButton" target="view"}}>'+
      'User&nbsp;<span class="caret"></span></button>'+
      '<ul class="dropdown-menu filter-components">'+
      '<li><label class="checkbox">' +
      '{{view Ember.Checkbox checkedBinding="view.allComponentsChecked"}} All</label></li>'+
      '{{#each user in view.users}}<li><label class="checkbox">' +
      '{{view Ember.Checkbox checkedBinding="user.checked"}} {{user.name}}'+
      '</label></li>{{/each}}'+
      '<li>' +
      '<button class="btn" {{action "closeFilter" target="view"}}>' +
      'Cancel</button>' +
      '<button class="btn btn-primary" {{action "applyFilter" target="view"}}>'+
      'Apply</button>'+
      '</li></ul>'
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
    },
    clearFilter:function(self) {
      self.set('allComponentsChecked', true);
      self.set('allComponentsChecked', false);
      jQuery('#user_filter').val([]);
      self.get('parentView').get('oTable').fnFilter('', 5);
      jQuery('#user_filter').closest('th').addClass('notActive');
    },
    closeFilter: function(){
      this.set('open', false);
    },
    applyFilter:function() {
      var chosenUsers = new Array();
      this.set('open', !this.get('open'));
      this.get('users').forEach(function(item){
          if(item.get('checked')) chosenUsers.push(item.get('name'));
      });
      jQuery('#user_filter').val(chosenUsers);
      this.get('parentView').get('applyFilter')(this.get('parentView'), 5);
      if (chosenUsers.length == 0) {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
    }
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
     * Show/hide additional content appropriated for this row
     */
    expandToggle : function(){
      App.router.get('mainAppsItemController').set('content', this.get('run'));
      App.ModalPopup.show({
        classNames: ['big-modal'],
        header: Em.I18n.t('apps.dagCharts.popup'),
        bodyClass: App.MainAppsItemView.extend({
          controllerBinding: 'App.router.mainAppsItemController'
        }),
        onPrimary: function() {
          this.hide();
        },
        secondary : null
      });
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