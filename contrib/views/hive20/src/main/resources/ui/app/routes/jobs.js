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
  query: Ember.inject.service(),
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
      let clone = now.clone();
      params.endTime = now.endOf('day').valueOf();
      params.startTime = clone.subtract('7', 'days').startOf('day').valueOf();
      this.set('startInitTo', params.startTime);
      this.set('endInitTo', params.endTime);
    }

    return this.store.query('job', params);
  },

  setupController(controller) {
    if(!(Ember.isEmpty(this.get('startInitTo')) || Ember.isEmpty(this.get('endInitTo')))) {

      controller.set('endTime', this.get('endInitTo'));
      controller.set('startTime', this.get('startInitTo'));
      //unset timeInitializedTo
      this.set('endInitTo');
      this.set('startInitTo');
    }

    this._super(...arguments);

  },

  actions: {
    dateFilterChanged(startTime, endTime) {
      this.controller.set('startTime', this.get('moment').moment(startTime, 'YYYY-MM-DD').startOf('day').valueOf());
      this.controller.set('endTime', this.get('moment').moment(endTime, 'YYYY-MM-DD').endOf('day').valueOf());
      this.refresh();
    },
    openWorksheet(worksheet, isExisitingWorksheet) {
      if(isExisitingWorksheet) {
       this.transitionTo('queries.query', worksheet.id);
       return;
      }
      this.get("store").createRecord('worksheet', worksheet );
      this.controllerFor('queries').set('worksheets', this.store.peekAll('worksheet'));
      this.transitionTo('queries.query', worksheet.id);
      this.controllerFor("queries.query").set('previewJobData', {id:worksheet.id, title:worksheet.title.toLowerCase()});
    }
  }



});
