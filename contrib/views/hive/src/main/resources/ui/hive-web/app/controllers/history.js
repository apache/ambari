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

import Ember from 'ember';
import FilterableMixin from 'hive/mixins/filterable';
import constants from 'hive/utils/constants';

export default Ember.ArrayController.extend(FilterableMixin, {
  jobService: Ember.inject.service('job'),
  fileService: Ember.inject.service('file'),

  sortAscending: false,
  sortProperties: ['dateSubmittedTimestamp'],

  init: function () {
    var oneMonthAgo = new Date();
    oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1);

    this._super();

    this.set('columns', Ember.ArrayProxy.create({ content: Ember.A([
      Ember.Object.create({
        caption: 'columns.title',
        property: 'title',
        link: constants.namingConventions.subroutes.historyQuery
      }),
      Ember.Object.create({
        caption: 'columns.status',
        property: 'status'
      }),
      Ember.Object.create({
        caption: 'columns.date',
        property: 'dateSubmittedTimestamp',
        dateRange: Ember.Object.create({
          min: oneMonthAgo,
          max: new Date()
        })
      }),
      Ember.Object.create({
        caption: 'columns.duration',
        property: 'duration',
        numberRange: Ember.Object.create({
          min: 0,
          max: 10,
          units: 'sec'
        })
      })
    ])}));
  },

  model: function () {
    return this.filter(this.get('history'));
  }.property('history', 'filters.@each'),

  updateIntervals: function () {
    var durationColumn;
    var maxDuration;
    var minDuration;

    if (this.get('columns')) {
      durationColumn = this.get('columns').find(function (column) {
        return column.get('caption') === 'columns.duration';
      });

      var items = this.get('history').map(function (item) {
        return item.get(durationColumn.get('property'));
      });

      minDuration = items.length ? Math.min.apply(Math, items) : 0;
      maxDuration = items.length ? Math.max.apply(Math, items) : 60; //Default 1 min

      durationColumn.set('numberRange.min', minDuration);
      durationColumn.set('numberRange.max', maxDuration);
    }
  }.observes('history'),

  updateDateRange: function () {
    var dateColumn;
    var maxDate;
    var minDate;

    if (this.get('columns')) {
      dateColumn = this.get('columns').find(function (column) {
        return column.get('caption') === 'columns.date';
      });

      var items = this.get('history').map(function (item) {
        return item.get(dateColumn.get('property'));
      });

      minDate = items.length ? Math.min.apply(Math, items) : new Date();
      maxDate = items.length ? Math.max.apply(Math, items) : new Date();

      dateColumn.set('dateRange.min', minDate);
      dateColumn.set('dateRange.max', maxDate);
    }
  }.observes('history'),

  filterBy: function (filterProperty, filterValue, exactMatch) {
    var column = this.get('columns').find(function (column) {
      return column.get('property') === filterProperty;
    });

    if (column) {
      column.set('filterValue', filterValue, exactMatch);
    } else {
      this.updateFilters(filterProperty, filterValue, exactMatch);
    }
  },

  actions: {
    sort: function (property) {
      //if same column has been selected, toggle flag, else default it to true
      if (this.get('sortProperties').objectAt(0) === property) {
        this.set('sortAscending', !this.get('sortAscending'));
      } else {
        this.set('sortAscending', true);
        this.set('sortProperties', [ property ]);
      }
    },

    interruptJob: function (job) {
      this.get('jobService').stopJob(job);
    },

    loadFile: function (job) {
      this.get('fileService').loadFile(job.get('queryFile')).then(function (file) {
        job.set('file', file);
      });
    },

    clearFilters: function () {
      var columns = this.get('columns');

      if (columns) {
        columns.forEach(function (column) {
          var filterValue = column.get('filterValue');
          var rangeFilter;

          if (filterValue) {
            if (typeof filterValue === 'string') {
              column.set('filterValue');
            } else {
              rangeFilter = column.get('numberRange') || column.get('dateRange');

              rangeFilter.set('from', rangeFilter.get('min'));
              rangeFilter.set('to', rangeFilter.get('max'));
            }
          }
        });
      }

      //call clear filters from Filterable mixin
      this.clearFilters();
    }
  }
});
