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
var filters = require('views/common/filter_view');

App.MainJobsView = Em.View.extend({
  templateName: require('templates/main/jobs'),

  showNumberOfJobs: Em.Select.extend({
    selected: '10',
    content: ['10', '25', '50', '100', "250", "500"]
  }),

  filteredJobs: function () {
    return Em.I18n.t('jobs.filtered.jobs').format(0,0);
  }.property(),

  /**
   * Filter-field for Jobs ID.
   * Based on <code>filters</code> library
   */
  jobsIdFilterView: filters.createTextView({
    valueBinding: "controller.filterObject.id"
  }),

  wrapSorting: Ember.View.extend({
    tagName: 'tr'
  }),

  sortingColumns: Ember.View.extend({
    tagName: 'th',
    classNameBindings: ['class', 'widthClass'],
    class: "sorting",
    widthClass: "",
    content: null,
    defaultColumn: 8,

    didInsertElement: function () {
      this.set("widthClass", "col" + this.get('content.index'));
      if (this.get('content.index') == this.get('defaultColumn')) {
        this.setControllerObj(this.content.index, "DESC");
        this.set("class", "sorting_desc");
      }
    },
    click: function (event) {
      console.log(this.get('class'));
      if (this.get('class') == "sorting") {
        this.resetSortClass();
        this.setControllerObj(this.get('content.index'), "ASC");
        this.set("class", "sorting_asc");
      } else if (this.get('class') == "sorting_asc") {
        this.setControllerObj(this.get('content.index'), "DESC");
        this.set("class", "sorting_desc");
      } else if (this.get('class') == "sorting_desc") {
        this.setControllerObj(this.get('content.index'), "ASC");
        this.set("class", "sorting_asc");
      }
    },
    resetSortClass: function () {
      this.get("parentView.childViews").map(function (a, e) {
        a.get("childViews")[0].set("class", "sorting")
      });
    },
    setControllerObj: function (col, dir) {
      this.set("controller.filterObject.iSortCol_0", col);
      this.set("controller.filterObject.sSortDir_0", dir);
    }
  }),

  /**
   * Filter-list for User.
   * Based on <code>filters</code> library
   */
  userFilterView: filters.createComponentView({
    /**
     * Inner FilterView. Used just to render component. Value bind to <code>mainview.value</code> property
     * Base methods was implemented in <code>filters.componentFieldView</code>
     */
    filterView: filters.componentFieldView.extend({
      templateName:require('templates/main/apps/user_filter'),

      usersBinding: 'controller.users',

      allComponentsChecked:false,
      toggleAllComponents:function () {
        var checked = this.get('allComponentsChecked');
        this.get('users').setEach('checked', checked);
      }.observes('allComponentsChecked'),

      clearFilter:function() {
        this.set('allComponentsChecked', false);
        this.get('users').setEach('checked', false);
        this._super();
      },

      applyFilter:function() {
        this._super();
        var chosenUsers = this.get('users').filterProperty('checked', true).mapProperty('name');
        this.set('value', chosenUsers.toString());
      }
    }),

    valueBinding: 'controller.filterObject.user'
  }),

  /**
   * Filter-field for Start Time.
   * Based on <code>filters</code> library
   */
  startTimeFilterView: filters.createSelectView({
    fieldType: 'input-medium',
    valueBinding: "controller.filterObject.startTime",
    content: ['Any', 'Past 1 hour',  'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days', 'Custom']
  }),

  /**
   * Filter-field for Start Time.
   * Based on <code>filters</code> library
   */
  endTimeFilterView: filters.createSelectView({
    fieldType: 'input-medium',
    valueBinding: "controller.filterObject.endTime",
    content: ['Any', 'Custom']
  })
})
