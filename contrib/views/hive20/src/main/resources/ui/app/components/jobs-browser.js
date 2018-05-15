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

export default Ember.Component.extend({
  startTime: null,
  endTime: null,
  maxEndTime: null,
  statusFilter: null,
  titleFilter: null,
  jobId: {'asc':true},
  title: {'noSort':true},
  status: {'noSort':true},
  dateSubmitted: {'noSort':true},
  duration: {'noSort':true},
  sortProp:['id:desc'],
  sortedJobs: Ember.computed.sort('jobs', function (m1, m2) {
    if (m1.get('dateSubmitted') < m2.get('dateSubmitted')) {
      return 1;
    } else if (m1.get('dateSubmitted') > m2.get('dateSubmitted')) {
      return -1;
    }
    return 0;
  }),

  titleFilteredJobs: Ember.computed('sortedJobs', 'titleFilter', function() {
    if (!Ember.isEmpty(this.get('titleFilter'))) {
      return (this.get('sortedJobs').filter((entry) => entry.get('title').toLowerCase().indexOf(this.get('titleFilter').toLowerCase()) >= 0));
    } else {
      return this.get('sortedJobs');
    }
  }),

  filteredJobs: Ember.computed('titleFilteredJobs', 'statusFilter', 'sortProp', function () {
    if (this.get('statusFilter')) {
      return  this.get('titleFilteredJobs').filter((entry) => entry.get('status').toLowerCase() === this.get('statusFilter'));
    } else {
      return this.get('titleFilteredJobs');
    }
  }),

  filteredJobsSorted: Ember.computed.sort('filteredJobs', 'sortProp'),

  statusCounts: Ember.computed('titleFilteredJobs', function () {
    return this.get('titleFilteredJobs').reduce((acc, item, index) => {
      let status = item.get('status').toLowerCase();
      if (Ember.isEmpty(acc[status])) {
        acc[status] = 1;
      } else {
        acc[status] = acc[status] + 1;
      }
      return acc;
    }, {});
  }),


  actions: {
    sort(sortProp, sortField, key) {
      let perm = {};
      perm[key] = true;
      this.set(sortField, perm);
      this.set('sortProp', [sortProp]);
    },

    setDateRange(startDate, endDate) {
      this.sendAction('filterChanged', startDate, endDate);
    },

    selectJobForStatus(status) {
      let s = status.toLowerCase();
      if (s === 'all') {
        this.set('statusFilter');
      } else {
        this.set('statusFilter', s);
      }
    },

    clearTitleFilter() {
      this.set('titleFilter');
    },
    openWorksheet(worksheet, isExisitingWorksheet){
      this.sendAction("openWorksheet", worksheet, isExisitingWorksheet);
    }
  }
});
