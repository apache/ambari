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
  jobs: Ember.inject.service(),
  store: Ember.inject.service(),

  generateStatistics(databaseName, tableName, withColumns = false) {
    return new Ember.RSVP.Promise((resolve, reject) => {
      this.get('store').adapterFor('table').analyseTable(databaseName, tableName, withColumns).then((data) => {
        this.get('store').pushPayload(data);
        resolve(this.get('store').peekRecord('job', data.job.id));
      }, (err) => {
        reject(err);
      });
    });
  },

  generateColumnStatistics(databaseName, tableName, columnName) {
    return new Ember.RSVP.Promise((resolve, reject) => {
      this.get('store').adapterFor('table').generateColumnStats(databaseName, tableName, columnName).then((data) => {
        this.get('store').pushPayload(data);
        resolve(this.get('store').peekRecord('job', data.job.id));
      }, (err) => {
        reject(err);
      });
    });
  },

  waitForStatsGenerationToComplete(job, fetchDummyResult = true) {
    return new Ember.RSVP.Promise((resolve, reject) => {
      this.get('jobs').waitForJobToComplete(job.get('id'), 5 * 1000, fetchDummyResult).then((data) => {
        resolve(job);
      }, (err) => {
        reject(err);
      });
    });
  },

  fetchColumnStatsResult(databaseName, tableName, columnName, job) {
    return this.get('store').adapterFor('table').fetchColumnStats(databaseName, tableName, columnName, job.get('id')).then((data) => {
      let columnStats = data.columnStats;
      return columnStats;
    });
  }
});
