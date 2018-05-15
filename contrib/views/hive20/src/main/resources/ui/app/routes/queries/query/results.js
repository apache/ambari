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
import UILoggerMixin from '../../../mixins/ui-logger';

export default Ember.Route.extend(UILoggerMixin, {

  jobs: Ember.inject.service(),
  query: Ember.inject.service(),

  beforeModel() {
  },

  model(){
    return this.modelFor('queries.query');
  },

  setupController(controller, model){
    this._super(...arguments);

    model.set('lastResultRoute', ".results");

    if(!Ember.isEmpty(model.get('currentJobData'))){
      let jobId = model.get('currentJobData').job.id;
      this.controller.set('model', model);
      this.controller.set('jobId', jobId);
      this.controller.set('payloadTitle',  model.get('currentJobData').job.title);
      this.controller.set('isQueryRunning', model.get('isQueryRunning'));
      this.controller.set('previousPage', model.get('previousPage'));
      this.controller.set('hasNext', model.get('hasNext'));
      this.controller.set('hasPrevious', model.get('hasPrevious'));
      this.controller.set('queryResult', model.get('queryResult'));
      this.controller.set('isExportResultSuccessMessege', false);
      this.controller.set('isExportResultFailureMessege', false);
      this.controller.set('showSaveHdfsModal', false);
      this.controller.set('showDownloadCsvModal', false);
      this.controller.set('hasJobAssociated', true);
    } else {
      this.controller.set('hasJobAssociated', false);
    }
  },

  actions:{

    saveToHDFS(jobId, path){

      var self = this;

      console.log('saveToHDFS query route with jobId == ', jobId);
      console.log('saveToHDFS query route with path == ', path);

      this.get('query').saveToHDFS(jobId, path)
        .then((data) => {

          console.log('successfully saveToHDFS', data);
          this.get('controller').set('isExportResultSuccessMessege', true);
          this.get('controller').set('isExportResultFailureMessege', false);

          Ember.run.later(() => {
            this.get('controller').set('showSaveHdfsModal', false);
            this.get('logger').success('Successfully Saved to HDFS.');

          }, 2 * 1000);

        }, (error) => {

          console.log("Error encountered", error);
          this.get('controller').set('isExportResultFailureMessege', true);
          this.get('controller').set('isExportResultSuccessMessege', false);

          Ember.run.later(() => {
            this.get('controller').set('showSaveHdfsModal', false);
            this.get('logger').danger('Failed to save to HDFS.', this.extractError(error));
          }, 2 * 1000);


        });
    },


    downloadAsCsv(jobId, path){

      console.log('downloadAsCsv query route with jobId == ', jobId);
      console.log('downloadAsCsv query route with path == ', path);

      let downloadAsCsvUrl = this.get('query').downloadAsCsv(jobId, path) || '';

      this.get('controller').set('showDownloadCsvModal', false);
      this.get('logger').success('Successfully downloaded as CSV.');
      window.open(downloadAsCsvUrl);

    },

  }

});
