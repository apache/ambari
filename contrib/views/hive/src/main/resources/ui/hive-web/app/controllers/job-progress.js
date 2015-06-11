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

export default Ember.Controller.extend({
  needs: [ constants.namingConventions.index ],

  jobs: [],

  index: Ember.computed.alias('controllers.' + constants.namingConventions.index),

  modelChanged: function () {
    var model = this.get('index.model');
    var job;

    if (!this.isJob(model)) {
      return;
    }

    job = this.jobs.findBy('model', model);

    if (!job) {
      job = this.jobs.pushObject(Ember.Object.create({
        model: model,
        stages: [],
        totalProgress: 0,
        retrievingProgress: false,
      }));
    }

    this.set('currentJob', job);
  }.observes('index.model'),

  updateProgress: function () {
    var job = this.get('currentJob');

    if (!job.get('model.dagId')) {
      return;
    }

    if (this.get('totalProgress') < 100 && !job.get('retrievingProgress')) {
      this.reloadProgress(job);
    }
  }.observes('currentJob.model.dagId'),

  totalProgress: function () {
    if (!this.isJob(this.get('index.model'))) {
      return;
    }

    return this.get('currentJob.totalProgress');
  }.property('index.model', 'currentJob.totalProgress'),

  stages: function () {
    if (!this.isJob(this.get('index.model'))) {
      return;
    }

    return this.get('currentJob.stages');
  }.property('index.model', 'currentJob.stages.@each.value'),

  reloadProgress: function (job) {
    var self = this;
    var url = '%@/%@/%@/progress'.fmt(this.container.lookup('adapter:application').buildURL(),
                                         constants.namingConventions.jobs,
                                         job.get('model.id'));

    job.set('retrievingProgress', true);

    Ember.$.getJSON(url).then(function (data) {
      var total = 0;
      var length = Object.keys(data.vertexProgresses).length;

      if (!job.get('stages.length')) {
        data.vertexProgresses.forEach(function (vertexProgress) {
          var progress = vertexProgress.progress * 100;

          job.get('stages').pushObject(Ember.Object.create({
            name: vertexProgress.name,
            value: progress
          }));

          total += progress;
        });
      } else {
        data.vertexProgresses.forEach(function (vertexProgress) {
          var progress = vertexProgress.progress * 100;

          job.get('stages').findBy('name', vertexProgress.name).set('value', progress);

          total += progress;
        });
      }

      total /= length;

      job.set('totalProgress', total);

      if (job.get('model.isRunning') && total < 100) {
        Ember.run.later(function () {
          self.reloadProgress(job);
        }, 1000);
      } else {
        job.set('retrievingProgress');
      }
    });
  },

  isJob: function (model) {
    return model.get('constructor.typeKey') === constants.namingConventions.job;
  }
});
