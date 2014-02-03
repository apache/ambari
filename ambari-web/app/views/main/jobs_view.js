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
var sort = require('views/common/sort_view');

App.MainJobsView = App.TableView.extend({
  templateName: require('templates/main/jobs'),

  content: function () {
    return this.get('controller.content');
  }.property('controller.content.length'),

  didInsertElement: function () {
    this.set('filteredContent', this.get('controller.content'));
  },

  sortView: sort.wrapperView,
  idSort: sort.fieldView.extend({
    column: 0,
    name: 'id',
    displayName: Em.I18n.t('jobs.column.id'),
    type: 'string'
  }),
  userSort: sort.fieldView.extend({
    column: 1,
    name: 'user',
    displayName: Em.I18n.t('jobs.column.user'),
    type: 'string'
  }),
  startTimeSort: sort.fieldView.extend({
    column: 2,
    name: 'startTime',
    displayName: Em.I18n.t('jobs.column.start.time'),
    type: 'number'
  }),
  endTimeSort: sort.fieldView.extend({
    column: 3,
    name: 'endTime',
    displayName: Em.I18n.t('jobs.column.end.time'),
    type: 'number'
  }),
  durationSort: sort.fieldView.extend({
    column: 4,
    name: 'duration',
    displayName: Em.I18n.t('jobs.column.duration'),
    type: 'number'
  }),

  /**
   * Select View with list of "rows-per-page" options
   * @type {Ember.View}
   */
  rowsPerPageSelectView: Em.Select.extend({
    content: ['10', '25', '50', '100', "250", "500"],
    change: function () {
      this.get('parentView').saveDisplayLength();
    }
  }),

  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredJobs: function () {
    return Em.I18n.t('jobs.filtered.jobs').format(this.get('filteredContent.length'), this.get('content').get('length'));
  }.property('content.length', 'filteredContent.length'),

  /**
   * Filter-field for Jobs ID.
   * Based on <code>filters</code> library
   */
  /*jobsIdFilterView: filters.createTextView({
    valueBinding: "controller.filterObject.id"
  }),*/

  jobsIdFilterView: filters.createTextView({
    column: 0,
    fieldType: 'width70',
    onChangeValue: function(){
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  /**
   * Filter-list for User.
   * Based on <code>filters</code> library
   */
  userFilterView: filters.createComponentView({

    column: 1,

    /**
     * Inner FilterView. Used just to render component. Value bind to <code>mainview.value</code> property
     * Base methods was implemented in <code>filters.componentFieldView</code>
     */
    filterView: filters.componentFieldView.extend({
      templateName:require('templates/main/apps/user_filter'),

      usersBinding: 'controller.users',

      clearFilter:function() {
        this.get('users').setEach('checked', false);
        this._super();
      },

      applyFilter:function() {
        this._super();
        var chosenUsers = this.get('users').filterProperty('checked', true).mapProperty('name');
        this.set('value', chosenUsers.toString());
      },

      /**
       * Verify that checked checkboxes are equal to value
       */
      checkUsers: function() {
        var users = this.get('value').split(',');
        var self = this;
        if (users) {
          users.forEach(function(userName) {
            var u = self.get("users").findProperty('name', userName);
            if (u) {
              if (!u.checked) {
                u.checked = true;
              }
            }
          });
        }
      }.observes('users.length')
    }),

    onChangeValue: function(){
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'multiple');
    }
  }),

  /**
   * Filter-field for Start Time.
   * Based on <code>filters</code> library
   */
  startTimeFilterView: filters.createSelectView({
    fieldType: 'input-medium',
    column: 2,
    content: ['Any', 'Past 1 hour',  'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
    }
  }),

  /**
   * Filter-field for End Time.
   * Based on <code>filters</code> library
   */
  endTimeFilterView: filters.createSelectView({
    fieldType: 'input-medium',
    column: 3,
    content: ['Any'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
    }
  }),

  /**
   * associations between content (jobs list) property and column index
   */
  colPropAssoc: function () {
    var associations = [];
    associations[0] = 'id';
    associations[1] = 'user';
    associations[2] = 'startTime';
    associations[3] = 'endTime';
    return associations;
  }.property()

})
