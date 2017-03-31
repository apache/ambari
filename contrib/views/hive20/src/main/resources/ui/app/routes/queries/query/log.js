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

  model(){
    return this.modelFor('queries.query');
  },

  getLogsTillJobSuccess(jobId, model, controller){
    let self = this;
    this.get('jobs').waitForJobStatus(jobId)
      .then((status) => {
        console.log('status', status);
        if(status !== "succeeded"){

          self.fetchLogs(model).then((logFileContent) => {
            controller.set('logResults', logFileContent );
          }, (error) => {
            console.log('error',error);
          });

          Ember.run.later(() => {
            self.getLogsTillJobSuccess(jobId, model, controller);
          }, 5 * 1000);

        } else {

          self.fetchLogs(model).then((logFileContent) => {
            controller.set('logResults', logFileContent );
          }, (error) => {
            console.log('error',error);
          });

        }
      }, (error) => {
        console.log('error',error);
      });
  },

  fetchLogs(model){
    let logFile = model.get('logFile');
    return new Ember.RSVP.Promise( (resolve, reject) => {
      this.get('query').retrieveQueryLog(logFile).then(function(data) {
        resolve(data.file.fileContent);
      }, function(error){
        reject(error);
      });
    });
  },

  jobStatus(jobId){
    return new Ember.RSVP.Promise( (resolve, reject) => {
      this.get('jobs').waitForJobStatus(jobId).then(function(status) {
        resolve(status);
      }, function(error){
        reject(error);
      });
    });

  },

  setupController(controller, model){
    this._super(...arguments);

    model.set('lastResultRoute', ".log");

    if(!Ember.isEmpty(model.get('currentJobData'))){
      let jobId = model.get('currentJobData').job.id;
      this.controller.set('jobId', jobId);
      this.controller.set('logResults', model.get('logResults'));
      this.getLogsTillJobSuccess(jobId, model, controller);
      this.controller.set('hasJobAssociated', true);

    } else {
      this.controller.set('hasJobAssociated', false);
    }
  },

  actions:{

  }

});
