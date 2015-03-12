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
import constants from 'hive/utils/constants';
import utils from 'hive/utils/functions';

export default Ember.ObjectController.extend({
  cachedResults: [],

  keepAlive: function (job) {
    Ember.run.later(this, function () {
      var self = this;
      var url = this.container.lookup('adapter:application').buildURL();
      url += '/' + constants.namingConventions.jobs + '/' + job.get('id') + '/results/keepAlive';

      var existingJob = self.cachedResults.findBy('id', job.get('id'));

      if (existingJob) {
        Ember.$.getJSON(url).fail(function (data) {
          //backend issue, this will be split in done and fail callbacks once its fixed.
          if (data.status === 404) {
            existingJob.set('results', []);
            self.send('getNextPage', true, job);
          } else if (data.status === 200) {
            self.keepAlive(job);
          }
        });
      }
    }, 1000 * 300);
  },

  clearCachedResultsSet: function (jobId) {
    this.set('cachedResults', this.get('cachedResults').without(this.get('cachedResults').findBy('id', jobId)));
  },

  initResults: function () {
    var existingJob;

    if (!utils.insensitiveCompare(this.get('content.status'), constants.statuses.succeeded)) {
      return;
    }

    existingJob = this.cachedResults.findBy('id', this.get('content.id'));

    if (existingJob) {
      this.set('results', existingJob.results.findBy('offset', existingJob.get('offset')));
    } else {
      this.send('getNextPage', true);
    }
  }.observes('content.status'),

  disableNext: function () {
    return !this.get('results.hasNext');
  }.property('results'),

  disablePrevious: function () {
    return this.cachedResults.findBy('id', this.get('content.id')).results.indexOf(this.get('results')) <= 0;
  }.property('results'),

  actions: {
    getNextPage: function (firstPage, job) {
      var self = this;
      var id = job ? job.get('id') : this.get('content.id');
      var existingJob = this.cachedResults.findBy('id', id);
      var resultsIndex;
      var url = this.container.lookup('adapter:application').buildURL();
      url += '/' + constants.namingConventions.jobs + '/' + id + '/results';

      if (firstPage) {
        url += '?first=true';
      }

      if (existingJob) {
        resultsIndex = existingJob.results.indexOf(this.get('results'));

        if (~resultsIndex && resultsIndex < existingJob.get('results.length') - 1) {
          this.set('results', existingJob.results.objectAt(resultsIndex + 1));
          return;
        }
      }

      Ember.$.getJSON(url).then(function (results) {
        if (existingJob) {
          existingJob.results.pushObject(results);
          existingJob.set('offset', results.offset);
        } else {
          self.cachedResults.pushObject(Ember.Object.create({
            id: id,
            results: [ results ],
            offset: results.offset
          }));
        }

        //only set results if the method was called for the current model, not after a keepAlive request.
        if (!job) {
          self.set('results', results);
        }

        if (firstPage) {
          self.keepAlive(job || self.get('content'));
        }
      }, function (err) {
        self.set('error', err.responseText);
      });
    },

    getPreviousPage: function () {
      var existingJob,
          resultsIndex;

      existingJob = this.cachedResults.findBy('id', this.get('content.id'));
      resultsIndex = existingJob.results.indexOf(this.get('results'));

      if (resultsIndex > 0) {
        this.set('results', existingJob.results.objectAt(resultsIndex - 1));
      }
    }
  }
});
