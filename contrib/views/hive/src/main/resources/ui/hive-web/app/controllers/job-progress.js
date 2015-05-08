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

  index: Ember.computed.alias('controllers.' + constants.namingConventions.index),

  listenForProgress: function () {
    var self = this;
    var url = this.container.lookup('adapter:application').buildURL();
    var stages = [];
    var job = this.get('index.model');

    var reloadProgress = function () {
      Ember.run.later(function () {
        Ember.$.getJSON(url).then(function (data) {
          var total = 0;
          var length = Object.keys(data.vertexProgresses).length;

          if (!self.get('stages.length')) {
            data.vertexProgresses.forEach(function (vertexProgress) {
              var progress = vertexProgress.progress * 100;

              stages.pushObject(Ember.Object.create({
                name: vertexProgress.name,
                value: progress
              }));

              total += progress;
            });

            self.set('stages', stages);
          } else {
            data.vertexProgresses.forEach(function (vertexProgress) {
              var progress = vertexProgress.progress * 100;

              self.get('stages').findBy('name', vertexProgress.name).set('value', progress);

              total += progress;
            });
          }

          total /= length;

          self.set('totalProgress', total);

          if (job.get('isRunning')) {
            reloadProgress();
          }

        }, function (err) {
          reloadProgress();
        });
      }, 1000);
    };

    //reset stages
    this.set('stages', []);
    this.set('totalProgress', 0);

    if (!job.get('applicationId')) {
      return;
    }

    url += '/' + constants.namingConventions.jobs + '/' + job.get('id') + '/progress';

    reloadProgress();
  }.observes('index.model', 'index.model.applicationId'),

  displayProgress: function () {
    return this.get('index.model.constructor.typeKey') === constants.namingConventions.job;
  }.property('index.model')
});
