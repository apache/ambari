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
  historyService: Ember.inject.service('history'),
  NUM_OF_DAYS: 5,
  REFRESH_INTERVAL_SEC: 30000,
  sortAscending: false,
  sortProperties: ['dateSubmittedTimestamp'],

  refresher: function () {
    var self = this;
    Ember.run.later(function () {
      if (self.get('isShowing')) {
        self.refresh();
      }
      self.refresher();
    }, self.get('REFRESH_INTERVAL_SEC'));
  },
  onLoadRoute: function () {
    this.set('isShowing', true);
  },
  onUnloadRoute: function () {
    this.set('isShowing', false);
  },
  init: function () {
    this._super();
    var self = this;
    var fromTime = moment().subtract(this.get('NUM_OF_DAYS'), 'days').startOf('day');
    var time = moment();
    var toTime = moment({
      years: time.year(),
      months: time.month(),
      date: time.date(),
      hours: 23,
      minutes: 59,
      seconds: 59,
      milliseconds: 999
    }); // next 12AM

    this.set('columns', Ember.ArrayProxy.create({
      content: Ember.A([
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
            min: fromTime.toDate(),
            max: toTime.toDate()
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
      ])
    }));

    return this.updateJobs(fromTime, toTime).then(function (data) {
      self.applyDurationFilter();
      self.refresher();
    });
  },
  applyDurationFilter: function () {
    var self = this;
    var durationColumn = this.get('columns').find(function (column) {
      return column.get('caption') === 'columns.duration';
    });
    var from = durationColumn.get('numberRange.from');
    var to = durationColumn.get('numberRange.to');
    self.filterBy("duration", {min: from, max: to});
  },
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
      var from = durationColumn.get('numberRange.from');
      var to = durationColumn.get('numberRange.to');
      if (from > maxDuration) {
        durationColumn.set("numberRange.from", maxDuration);
      }
      if (to < minDuration) {
        durationColumn.set("numberRange.to", minDuration);
      }
    }
  }.observes('history'),

  model: function () {
    return this.filter(this.get('history'));
  }.property('history', 'filters.@each'),

  updateJobs: function (fromDate, toDate) {
    var self = this;
    var fromTime = moment(fromDate).startOf('day').toDate().getTime();
    var time = moment(toDate);
    var toTime = moment({
      years: time.year(),
      months: time.month(),
      date: time.date(),
      hours: 23,
      minutes: 59,
      seconds: 59,
      milliseconds: 999
    }).toDate().getTime(); // next 12AM
    this.set("fromTime", fromTime);
    this.set("toTime", toTime);
    return this.get("historyService").getJobs(fromTime, toTime).then(function (data) {
      self.set('history', data);
    });
  },

  filterBy: function (filterProperty, filterValue, exactMatch) {
    var column = this.get('columns').find(function (column) {
      return column.get('property') === filterProperty;
    });

    if (column) {
      var isDateColumn = column.get('caption') === 'columns.date';
      column.set('filterValue', filterValue, exactMatch);
      if (isDateColumn) {
        return this.updateJobs(filterValue.min, filterValue.max);
      } else {
        this.updateFilters(filterProperty, filterValue, exactMatch);
      }
    } else {
      this.updateFilters(filterProperty, filterValue, exactMatch);
    }
  },

  refresh: function () {
    var self = this;
    this.get('historyService').getUpdatedJobList(this.get('toTime')).then(function (data) {
      self.set('history', data);
    });
  },

  actions: {

    refreshJobs: function () {
      this.refresh();
    },

    filterUpdated: function (filterProperty, filterValue) {
      var self = this;
      var column = this.get('columns').find(function (column) {
        return column.get('property') === filterProperty;
      });

      var isDateColumn = (column.get('caption') === 'columns.date');

      if (column) {
        column.set('filterValue', filterValue);
        if (isDateColumn) {
          return this.updateJobs(filterValue.min, filterValue.max).then(function (data) {
            self.updateFilters(filterProperty, filterValue);
          });
        } else {
          self.updateFilters(filterProperty, filterValue);
        }
      }
    },

    sort: function (property) {
      //if same column has been selected, toggle flag, else default it to true
      if (this.get('sortProperties').objectAt(0) === property) {
        this.set('sortAscending', !this.get('sortAscending'));
      } else {
        this.set('sortAscending', true);
        this.set('sortProperties', [property]);
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
