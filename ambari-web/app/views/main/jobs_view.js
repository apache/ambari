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

App.MainJobsView = App.MainAppsView.extend({
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
