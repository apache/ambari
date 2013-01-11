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

  nameFilterView: Em.TextField.extend({
    classNames:['input-medium'],
    type:'text',
    placeholder: 'Any Name',
    filtering:function(){
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
      this.get('parentView').get('applyFilter')(this.get('parentView'), 8, this.get('value'));
    }.observes('value')
  }),

  rackFilterView: Em.TextField.extend({
    classNames:['input-medium'],
    type:'text',
    placeholder: 'Any Name',
    filtering:function(){
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
      this.get('parentView').get('applyFilter')(this.get('parentView'), 2, this.get('value'));
    }.observes('value')
  }),
  /**
   * Filter-field for cpu
   */
  cpuFilterView: Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder: 'Any ',
    elementId:'cpu_filter',
    filtering:function(){
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
      this.get('parentView').get('applyFilter')(this.get('parentView'), 3);
    }.observes('value')
  }),
  /**
   * Filter-field for load avg
   */
  loadAvgFilterView: Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder: 'Any ',
    elementId:'load_avg_filter',
    filtering:function(){
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
      this.get('parentView').get('applyFilter')(this.get('parentView'), 5);
    }.observes('value')
  }),
  /**
   * Filter-field for RAM
   */
  ramFilterView: Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder: 'Any ',
    elementId: 'ram_filter',
    filtering:function(){
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
      this.get('parentView').get('applyFilter')(this.get('parentView'), 4);
    }.observes('value')
  }),
  /**
   * Filter-list for Components
   */
  componentsFilterView: Em.View.extend({
    classNames:['btn-group'],
    classNameBindings: ['open'],
    multiple:true,
    open: false,

    isFilterOpen:false,

    btnGroupClass:function () {
      return this.get('isFilterOpen') ? 'btn-group open' : 'btn-group';
    }.property('isFilterOpen'),

    allComponentsChecked:false,
    toggleAllComponents:function () {
      var checked = this.get('allComponentsChecked');
      this.set('masterComponentsChecked', checked);
      this.set('slaveComponentsChecked', checked);
      this.set('clientComponentsChecked', checked);
    }.observes('allComponentsChecked'),

    masterComponentsChecked:false,
    toggleMasterComponents:function () {
      this.get('masterComponents').setEach('checkedForHostFilter', this.get('masterComponentsChecked'));
    }.observes('masterComponentsChecked'),

    slaveComponentsChecked:false,
    toggleSlaveComponents:function () {
      this.get('slaveComponents').setEach('checkedForHostFilter', this.get('slaveComponentsChecked'));
    }.observes('slaveComponentsChecked'),

    clientComponentsChecked: false,
    toggleClientComponents: function() {
      this.get('clientComponents').setEach('checkedForHostFilter', this.get('clientComponentsChecked'));
    }.observes('clientComponentsChecked'),

    masterComponents:function(){
      return this.get('parentView.controller.masterComponents');
    }.property('parentView.controller.masterComponents'),

    slaveComponents:function(){
      return this.get('parentView.controller.slaveComponents');
    }.property('parentView.controller.slaveComponents'),

    clientComponents: function() {
      return this.get('parentView.controller.clientComponents');
    }.property('parentView.controller.clientComponents'),

    template: Ember.Handlebars.compile('<div {{bindAttr class="view.btnGroupClass"}} >'+
      '<button class="btn btn-info single-btn-group" {{action "clickFilterButton" target="view"}}>' +
        'Components ' +
        '<span class="caret"></span>' +
       '</button>' +
        '<ul class="dropdown-menu filter-components" id="filter-dropdown">' +
          '<li>' +
            '<ul>' +
              '<li>' +
                  '<label class="checkbox">' +
                    '{{view Ember.Checkbox checkedBinding="view.allComponentsChecked"}} All' +
                  '</label>' +
                '</li>' +
                '<li>' +
                  '<label class="checkbox">' +
                    '{{view Ember.Checkbox checkedBinding="view.masterComponentsChecked"}} Master Components:' +
                  '</label>' +
                  '<ul>' +
                    '{{#each component in masterComponents}}' +
                      '<li>' +
                        '<label class="checkbox">' +
                          '{{view Ember.Checkbox checkedBinding="component.checkedForHostFilter" }} {{unbound component.displayName}}' +
                        '</label>' +
                      ' </li>' +
                    '{{/each}}' +
                  '</ul>' +
                '</li>' +
                '<li>' +
                  '<label class="checkbox">' +
                    '{{view Ember.Checkbox checkedBinding="view.slaveComponentsChecked"}} Slave Components:' +
                  '</label>' +
                  '<ul>' +
                    '{{#each component in slaveComponents}}' +
                      '<li>' +
                        '<label class="checkbox">' +
                          '{{view Ember.Checkbox checkedBinding="component.checkedForHostFilter" }} {{unbound component.displayName}}' +
                        '</label>' +
                      '</li>' +
                    '{{/each}}' +
                  '</ul>' +
                '</li>' +
                '<li>' +
                  '<label class="checkbox">' +
                    '{{view Ember.Checkbox checkedBinding="view.clientComponentsChecked"}} Client Components:' +
                  '</label>' +
                  '<ul>' +
                    '{{#each component in clientComponents}}' +
                      '<li>' +
                        '<label class="checkbox">' +
                          '{{view Ember.Checkbox checkedBinding="component.checkedForHostFilter" }} {{unbound component.displayName}}' +
                        '</label>' +
                      '</li>' +
                    '{{/each}}' +
                  '</ul>' +
                '</li>' +
            '</ul>' +
          '</li>' +
          '<li>' +
            '<button class="btn" {{action "closeFilters" target="view"}}>' +
              'Cancel' +
            '</button> ' +
            '<button class="btn btn-primary" {{action "applyFilter" target="view"}}>' +
              'Apply' +
            '</button>' +
          '</li>' +
        '</ul>' +
      '</div>'),

    clearFilter:function(self) {
      self.set('allComponentsChecked', true);
      self.set('allComponentsChecked', false);
      jQuery('#components_filter').val([]);
      self.get('parentView').get('oTable').fnFilter('', 6);
      jQuery('#components_filter').closest('th').addClass('notActive');
    },
    closeFilters:function () {
      $(document).unbind('click');
      this.clickFilterButton();
    },

    clickFilterButton:function () {
      var self = this;
      this.set('isFilterOpen', !this.get('isFilterOpen'));
      if (this.get('isFilterOpen')) {

        var dropDown = $('#filter-dropdown');
        var firstClick = true;
        $(document).bind('click', function (e) {
          if (!firstClick && $(e.target).closest(dropDown).length == 0) {
            self.set('isFilterOpen', false);
            $(document).unbind('click');
          }
          firstClick = false;
        });
      }
    },

    didInsertElement:function () {
      if (this.get('controller.comeWithFilter')) {
        this.applyFilter();
        this.closeFilters();
        this.set('controller.comeWithFilter', false);
      } else {
        this.clearFilter(this);
      }
    },

    applyFilter:function() {
      var chosenComponents = new Array();

      this.set('isFilterOpen', !this.get('isFilterOpen'));
      this.get('masterComponents').forEach(function(item){
        if(item.get('checkedForHostFilter')) chosenComponents.push(item.get('displayName'));
      });
      this.get('slaveComponents').forEach(function(item){
        if(item.get('checkedForHostFilter')) chosenComponents.push(item.get('displayName'));
      });
      this.get('clientComponents').forEach(function(item){
        if(item.get('checkedForHostFilter')) chosenComponents.push(item.get('displayName'));
      });
      jQuery('#components_filter').val(chosenComponents);
      this.get('parentView').get('applyFilter')(this.get('parentView'), 9);
      if (chosenComponents.length == 0) {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
    }
  }),
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
  }

});