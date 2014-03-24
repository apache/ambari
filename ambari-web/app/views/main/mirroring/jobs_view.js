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

App.MainDatasetJobsView = App.TableView.extend({
  templateName: require('templates/main/mirroring/jobs'),

  isLoaded: function () {
    return App.router.get('mainMirroringController.isLoaded');
  }.property('App.router.mainMirroringController.isLoaded'),

  content: function () {
    var datasetName = this.get('controller.content.name');
    return (this.get('isLoaded') && datasetName) ?
        App.Dataset.find().findProperty('name', datasetName).get('datasetJobs') : [];
  }.property('controller.content', 'isLoaded'),

  dataset: function () {
    return this.get('controller.content');
  }.property('controller.content'),

  showActions: function () {
    return this.get('isLoaded') && this.get('dataset.status') && !this.get('parentView.controller.isDatasetLoadingError');
  }.property('isLoaded', 'dataset.status', 'parentView.controller.isDatasetLoadingError'),

  sortView: sort.wrapperView.extend({
    loadSortStatuses: function(){
      this._super();
      var statuses = App.db.getSortingStatuses(this.get('controller.name'));
      if (!statuses || statuses.everyProperty('status', 'sorting')) {
        var sorting = this.get('childViews').findProperty('name', 'startDate');
        this.sort(sorting, true);
        sorting.set('status', 'sorting_desc');
      }
    }.observes('parentView.filteringComplete')
  }),
  idSort: sort.fieldView.extend({
    column: 1,
    name: 'name',
    displayName: Em.I18n.t('mirroring.table.jobId'),
    type: 'string'
  }),
  startSort: sort.fieldView.extend({
    column: 2,
    name: 'startDate',
    displayName: Em.I18n.t('mirroring.table.start'),
    type: 'number'
  }),
  endSort: sort.fieldView.extend({
    column: 3,
    name: 'endDate',
    displayName: Em.I18n.t('mirroring.table.end'),
    type: 'number'
  }),
  statusSort: sort.fieldView.extend({
    column: 4,
    name: 'statusFormatted',
    displayName: Em.I18n.t('mirroring.table.status'),
    type: 'string'
  }),

  /**
   * Filter view for name column
   * Based on <code>filters</code> library
   */
  idFilterView: filters.createTextView({
    fieldType: 'input-small',
    column: 1,
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'string');
    }
  }),

  startFilterView: filters.createSelectView({
    fieldType: 'input-small',
    column: 2,
    content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
    }
  }),

  endFilterView: filters.createSelectView({
    fieldType: 'input-small',
    column: 3,
    content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
    }
  }),

  statusFilterView: filters.createSelectView({
    fieldType: 'input-small',
    column: 4,
    content: ['Any', 'Waiting', 'Running', 'Suspended', 'Killed', 'Failed', 'Succeeded', 'Error'],
    onClearValue: function () {
      if (this.get('value') === '') {
        this.set('value', 'Any');
      }
    }.observes('value'),
    onChangeValue: function () {
      var value = this.get('value');
      if (value === 'Any') {
        value = '';
      }
      this.get('parentView').updateFilter(this.get('column'), value, 'string');
    }
  }),

  JobView: Em.View.extend({
    content: null,
    tagName: 'tr',

    showActions: function () {
      return ['RUNNING', 'SUSPENDED'].contains(this.get('content.status')) && App.get('isAdmin');
    }.property('content.status')
  }),

  /**
   * associations between dataset property and column index
   */
  colPropAssoc: function () {
    var associations = [];
    associations[1] = 'name';
    associations[2] = 'startDate';
    associations[3] = 'endDate';
    associations[4] = 'statusFormatted';
    return associations;
  }.property()
});
