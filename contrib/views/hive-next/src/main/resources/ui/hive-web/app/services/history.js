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
import Job from 'hive/models/job'
import constants from 'hive/utils/constants';

export default Ember.Service.extend({
  historyJobsMap: {},
  store: Ember.inject.service(),
  fromDate: null,
  toDate: null,

  getJobs: function (fromDate, toDate) {
    var self = this;
    console.log("getJobs : fromDate : ", fromDate, ", toDate : ", toDate);

    if (Ember.isEmpty(fromDate) || Ember.isEmpty(toDate)) {
      throw new Error("Dates cannot be empty.");
    }
    if (toDate < fromDate) {
      throw new Error("toDate cannot be smaller than fromDate");
    }

    var currFromDate = this.get("fromDate");
    var currToDate = this.get("toDate");
    var currJobsMap = this.get("historyJobsMap");

    if (!Ember.isEmpty(currFromDate) && !Ember.isEmpty(currToDate)
      && currFromDate <= fromDate && currToDate >= toDate
      && !Ember.isEmpty(currJobsMap)
    ) {
      // filter current jobs and return
      var validJobs = [];
      Object.keys(currJobsMap).forEach(function (id) {
        var job = currJobsMap[id];
        if (job.get('dateSubmitted') >= fromDate && job.get('dateSubmitted') < toDate) {
          validJobs.push(job);
        }
      });

      return Ember.RSVP.Promise.resolve(validJobs);
    }

    return this.fetchJobs(fromDate, toDate).then(function (data) {
      var jobMap = {};
      var jobs = data.map(function (j) {
        var job = this.get('store').push('job', j);
        jobMap[job.id] = job;
        return job;
      }, self);
      self.set('fromDate', fromDate);
      self.set('toDate', toDate);
      self.set('historyJobsMap', jobMap);
      return jobs;
    });
  },

  fetchJobs: function (fromDate, toDate) {
    console.log("getJobs : fromDate : ", fromDate, ", toDate : ", toDate);

    if (Ember.isEmpty(fromDate) || Ember.isEmpty(toDate)) {
      throw new Error("Dates cannot be empty.");
    }
    if (toDate < fromDate) {
      throw new Error("toDate cannot be smaller than fromDate");
    }

    var self = this;
    var url = this.container.lookup('adapter:application').buildURL();
    url += "/jobs";
    var jobMap = {};
    return Ember.$.ajax({
      url: url,
      type: 'GET',
      data: {
        "startTime": fromDate,
        "endTime": toDate
      },
      headers: {
        'X-Requested-By': 'ambari'
      }
    });
  },

  fetchAndMergeNew: function (toTime) {
    var self = this;
    return this.fetchNew(toTime).then(function (data) {
      var jobMap = self.get('historyJobsMap');
      var jobs = data.map(function (j) {
        var job = this.get('store').push('job', j);
        jobMap[job.id] = job;
        return job;
      }, self);
      self.set('toDate', toTime);
      return jobs;
    });
  },

  getUpdatedJobList: function (toTime) {
    var self = this;
    return this.refreshAndFetchNew(toTime).then(function (data) {
      var jobMap = self.get('historyJobsMap');
      var allJobs = Object.keys(jobMap).map(function (id) {
        return jobMap[id];
      });
      return allJobs;
    });
  },

  fetchNew: function (toTime) {
    var self = this;
    var jobMap = this.get('historyJobsMap');
    var fromTime = 0;
    if (this.get('fromDate')) {
      fromTime = this.get('fromDate');
    }

    Object.keys(jobMap).forEach(function (id) {
      var job = jobMap[id];
      fromTime = Math.max(fromTime, job.get('dateSubmitted'));
    });

    if (fromTime > toTime) {
      // we already have latest data.
      return Ember.RSVP.Promise.resolve([]);
    }
    return this.fetchJobs(fromTime, toTime);
  },

  refresh: function () {
    var self = this;
    var url = this.container.lookup('adapter:application').buildURL();
    url += "/jobs/getList";
    var jobMap = this.get('historyJobsMap');
    var statuses = constants.statuses;
    var jobIds = [];
    Object.keys(jobMap).forEach(function (id) {
      var job = jobMap[id];
      var jobStatus = job.get('uppercaseStatus');
      if (jobStatus === statuses.initialized
        || jobStatus === statuses.pending
        || jobStatus === statuses.running
        || jobStatus === statuses.unknown
      ) {
        // note jobId will either have DB's id or hiveId
        jobIds.push({
          jobId: job.get('id'),
          hiveId: job.get('hiveQueryId'),
          dagId: job.get('dagId'),
          operationId: job.get('operationId')
        });
      }
    });

    if (Ember.isEmpty(jobIds)) {
      return Ember.RSVP.Promise.resolve([]);
    }
    console.log("refresh jobIds to refresh : ", jobIds);
    return Ember.$.ajax({
      url: url,
      type: 'POST',
      data: JSON.stringify(jobIds),
      headers: {
        'X-Requested-By': 'ambari'
      },
      contentType: "application/json"
    }).then(function (data) {
      var jobs = data.map(function (j) {
        var job = this.get('store').push('job', j);
        jobMap[job.id] = job;
        return job;
      }, self);
      self.set('historyJobsMap', jobMap);
      // return all the jobs
      var allJobs = Object.keys(jobMap).map(function (id) {
        return jobMap[id];
      });
      return allJobs;
    });
  },

  refreshAndFetchNew: function (toTime) {
    var self = this;
    return this.refresh().then(function (data) {
      return self.fetchAndMergeNew(toTime);
    })
  }
});
