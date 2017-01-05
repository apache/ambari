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

export default Ember.Route.extend({
  moment: Ember.inject.service(),
  timeInitializedTo: null,
  queryParams: {
    startTime: {
      refreshModel: true
    },
    endTime: {
      refreshModel: true
    }
  },


  model(params) {
    let now = this.get('moment').moment();
    if(Ember.isEmpty(params.startTime) || Ember.isEmpty(params.endTime)) {
      let initialValue = now.clone();
      params.endTime = now.valueOf();
      params.startTime = now.subtract('7', 'days').valueOf();
      this.set('timeInitializedTo', initialValue);
    }

    return this.store.query('job', params);
  },

  setupController(controller, model) {
    if(!Ember.isEmpty(this.get('timeInitializedTo'))) {

      controller.set('endTime', this.get('timeInitializedTo').valueOf());
      controller.set('startTime', this.get('timeInitializedTo').subtract('7', 'days').valueOf());
      //unset timeInitializedTo
      this.set('timeInitializedTo');
    }

    this._super(...arguments);

  },

  actions: {
    dateFilterChanged(startTime, endTime) {
      this.controller.set('startTime', this.get('moment').moment(startTime, 'YYYY-MM-DD').valueOf())
      this.controller.set('endTime', this.get('moment').moment(endTime, 'YYYY-MM-DD').valueOf())
      this.refresh();
    }
  }



});
