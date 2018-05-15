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

export default Ember.Service.extend({
  store: Ember.inject.service(),
  isCurrentQueryCancelled: false,
  getQuery(jobId) {
    let job = this.get('store').peekRecord('job', jobId);
    if (job) {
      return this.get('store').findRecord('file', job.get('queryFile'));
    }
  },

  waitForJobToComplete(jobId, after, fetchDummyResult = true) {

    return new Ember.RSVP.Promise((resolve, reject) => {
      Ember.run.later(() => {
        if(this.get('isCurrentQueryCancelled')) {
         this.resetCurrentQueryStatus();
         reject('error');
         return;
        }
        this.get('store').findRecord('job', jobId, {reload: true})
          .then((job) => {
            let status = job.get('status').toLowerCase();
            if (status === 'succeeded') {
              if (fetchDummyResult) {
                this._fetchDummyResult(jobId);
              }
              resolve(status);
            } else if (status === 'error') {
              reject(status);
            } else {
              resolve(this.waitForJobToComplete(jobId, after, fetchDummyResult));
            }
          }, (error) => {
            reject(error);
          });
      }, after);
    });
  },

  waitForJobStatus: function (jobId) {
    return new Ember.RSVP.Promise((resolve, reject) => {
      this.get('store').findRecord('job', jobId, {reload: true})
        .then((job) => {
          let status = job.get('status').toLowerCase();
          resolve(status);
        }, (error) => {
          reject(error);
        });
    });
  },

  stopJob : function(jobId) {
    this.setCurrentQueryAsCancelled();
    return new Ember.RSVP.Promise((resolve, reject) => {
      let job = this.get('store').peekRecord('job', jobId);
      if(job) {
       job.destroyRecord();
      }
       else {
        this.get('store').findRecord('job', jobId, { reload: true })
          .then(job => {
           job.deleteRecord();
           return resolve("");
         }).catch(function (response) {
           return resolve("");
         });
      }
    });
  },
  setCurrentQueryAsCancelled() {
    this.set('isCurrentQueryCancelled', true);
  },
  resetCurrentQueryStatus() {
    this.set('isCurrentQueryCancelled', false);
  },
  _fetchDummyResult(jobId) {
    this.get('store').adapterFor('job').fetchResult(jobId);
  },

  getJob: function (jobId) {
    return this.get('store').findRecord('job', jobId, {reload: true});
  }
});
