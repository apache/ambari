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

  jobs: Ember.inject.service(),
  query: Ember.inject.service(),

  beforeModel() {
  },

  model(){
    return this.modelFor('queries.query');
  },

  setupController(controller, model){
    this._super(...arguments);
    model.set('lastResultRoute', ".visual-explain");

    if(!Ember.isEmpty(model.get('currentJobData'))) {
      let jobId = model.get('currentJobData').job.id;
      this.controller.set('jobId', jobId);
      this.controller.set('payloadTitle',  model.get('currentJobData').job.title);
      this.controller.set('isQueryRunning', model.get('isQueryRunning'));
      try {
        if(!Ember.isEmpty(JSON.parse(model.get('queryResult').rows[0][0])['STAGE PLANS'])){
          this.controller.set('visualExplainJson', model.get('queryResult').rows[0][0]);
        }
      }catch(error) { }
      this.controller.set('hasJobAssociated', true);
    } else {
      this.controller.set('hasJobAssociated', false);
    }
  },

  actions:{

  }

});
