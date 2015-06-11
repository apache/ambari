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

export default Ember.Route.extend({
  notifyService: Ember.inject.service(constants.namingConventions.notify),

  model: function () {
    var self = this;

    return this.store.find(constants.namingConventions.job).catch(function (error) {
      self.get('notifyService').error(error);
    });
  },

  setupController: function (controller, model) {
    if (!model) {
      return;
    }

    var filteredModel = model.filter(function (job) {
       //filter out jobs with referrer type of sample, explain and visual explain
       return (!job.get('referrer') || job.get('referrer') === constants.jobReferrer.job) &&
              !!job.get('id');
    });

    controller.set('history', filteredModel);
  }
});
