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
require('utils/data_table');
var filters = require('views/common/filter_view');

App.MainHostView = Em.View.extend({
  templateName:require('templates/main/host'),
  controller:function () {
    return App.router.get('mainHostController');
  }.property(),
  content:function () {
    return App.router.get('mainHostController.content');
  }.property('App.router.mainHostController.content'),
  oTable: null,

  didInsertElement:function () {
    var oTable = $('#hosts-table').dataTable({
      "sDom": '<"search-bar"f><"clear">rt<"page-bar"lip><"clear">',
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
      "iDisplayLength": 10,
      "aLengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "All"]],
      "oSearch": {"bSmart":false},
      "aoColumns":[
        { "bSortable": false },
        { "sType":"html" },
        { "sType":"html" },
        { "sType":"num-html" },
        { "sType":"ambari-bandwidth" },
        { "sType":"html" },
        { "sType":"num-html" },
        { "sType":"html", "bSortable": false  },
        { "bVisible": false }, // hidden column for raw public host name value
        { "bVisible": false } // hidden column for raw components list
      ]
    });
    this.set('oTable', oTable);
    this.set('allComponentsChecked', true); // select all components (checkboxes) on start.
  },

  HostView:Em.View.extend({
    content:null,

    shortLabels: function() {
      var labels = this.get('content.hostComponents').getEach('displayName');
      var shortLabels = '';
      var c = 0;
      labels.forEach(function(label) {
        if (label) {
          if (c < 2) {
            shortLabels += label.replace(/[^A-Z]/g, '') + ', ';
            c++;
          }
        }
      });
      shortLabels = shortLabels.substr(0, shortLabels.length - 2);
      if (labels.length > 2) {
        shortLabels += ' and ' + (labels.length - 2) + ' more';
      }
      return shortLabels;
    }.property('labels'),

    labels: function(){
      return this.get('content.hostComponents').getEach('displayName').join('\n');
    }.property('content.hostComponents.@each'),

    usageStyle:function () {
      return "width:" + this.get('content.diskUsage') + "%";
      //return "width:" + (25+Math.random()*50) + "%"; // Just for tests purposes
    }.property('content.diskUsage')

  }),

  nameFilterView: filters.createTextView({
    onChangeValue: function(){
      this.get('parentView').updateFilter(8, this.get('value'));
    }
  }),

  ipFilterView: filters.createTextView({
    onChangeValue: function(){
      this.get('parentView').updateFilter(2, this.get('value'));
    }
  }),

  cpuFilterView: filters.createTextView({
    fieldType: 'input-mini',
    fieldId: 'cpu_filter',
    onChangeValue: function(){
      this.get('parentView').updateFilter(3);
    }
  }),

  loadAvgFilterView: filters.createTextView({
    fieldType: 'input-mini',
    fieldId: 'load_avg_filter',
    onChangeValue: function(){
      this.get('parentView').updateFilter(5);
    }
  }),

  ramFilterView: filters.createTextView({
    fieldType: 'input-mini',
    fieldId: 'ram_filter',
    onChangeValue: function(){
      this.get('parentView').updateFilter(4);
    }
  }),

  componentsFilterView: filters.createComponentView({
    fieldId: 'components_filter',
    onChangeValue: function(){
      this.get('parentView').updateFilter(9);
    }
  }),

  startIndex : function(){
    return Math.random();
  }.property(),

  /**
   * Apply each filter to dataTable
   *
   * @param iColumn number of column by which filter
   * @param value
   */
  updateFilter: function(iColumn, value){
    this.get('oTable').fnFilter(value || '', iColumn);
  }

});
