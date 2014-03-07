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


  /**
   * If no jobs table rows to show.
   */
  noDataToShow:true,

  /*
   If no jobs to display set noDataToShow to true, else set emptyData to false.
   */
  noDataToShowObserver: function () {
    if(this.get("controller.content.length") > 0){
      this.set("noDataToShow",false);
    }else{
      this.set("noDataToShow",true);
    }
  }.observes("controller.content.length"),

  onApplyIdFilter: function() {
    var isIdFilterApplied = this.get('controller.filterObject.isIdFilterApplied');
    this.get('childViews').forEach(function(childView) {
      if (childView['clearFilter'] && childView.get('column') != 0) {
        childView.clearFilter();
        var childOfChild = childView.get('childViews')[0];
        if(childOfChild){
          childOfChild.set('disabled', isIdFilterApplied);
        }
      }
    });
  }.observes('controller.filterObject.isIdFilterApplied'),

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
    valueBinding: "controller.filterObject.jobsLimit",
    change: function () {
      this.get('parentView').saveDisplayLength();
    }
  }),

  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredJobs: function () {
    return Em.I18n.t('jobs.filtered.jobs').format(this.get('content').get('length'), this.get('controller.totalOfJobs'));
  }.property('content.length', 'filteredContent.length', 'controller.totalOfJobs'),

  pageContentObserver: function () {
    Ember.run.later(this, function() {
      $('.job-link').tooltip();
    }, 1000);
  }.observes('pageContent', 'pageContent.length', 'pageContent.@each.id'),

  willDestroyElement : function() {
    $('.job-link').tooltip('destroy');
  },

  /**
   * Filter-field for Jobs ID.
   * Based on <code>filters</code> library
   */
  jobsIdFilterView: filters.createTextView({
    column: 0,
    showApply: true,
    setPropertyOnApply: 'controller.filterObject.id'
  }),

  /**
   * Filter-list for User.
   * Based on <code>filters</code> library
   */
  userFilterView: filters.createTextView({
    column: 1,
    fieldType: 'input-small',
    showApply: true,
    setPropertyOnApply: 'controller.filterObject.user'
  }),

  /**
   * Filter-field for Start Time.
   * Based on <code>filters</code> library
   */
  startTimeFilterView: filters.createSelectView({
    fieldType: 'input-120',
    column: 2,
    content: ['Any', 'Past 1 hour',  'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days', 'Custom'],
    valueBinding: "controller.filterObject.startTime"
  }),

  /**
   * associations between content (jobs list) property and column index
   */
  colPropAssoc: function () {
    var associations = [];
    associations[0] = 'id';
    associations[1] = 'user';
    associations[2] = 'startTime';
    return associations;
  }.property(),

  clearFilters: function() {
    this.get('childViews').forEach(function(childView) {
      if (childView['clearFilter']) {
        childView.clearFilter();
      }
    });
  }
})
