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
import ApplicationAdapter from './application';

export default ApplicationAdapter.extend({
  fetchResult(jobId) {
    let resultUrl = this.urlForFindRecord(jobId, 'job') + "/results";
    return this.ajax(resultUrl, 'GET');
  },

  getQuery(job) {
    let queryUrl = this.buildURL() + "/file" + encodeURI(job.get('queryFile'));
  },

  saveToHDFS(jobId, path){
    let url = this.urlForFindRecord(jobId, 'job') + "/results/csv/saveToHDFS?commence=true&file=" + path + ".csv";

    return new Ember.RSVP.Promise((resolve, reject) => {
      this.ajax(url).then((response) => {
        if (response.status.toLowerCase() !== "TERMINATED".toLowerCase()) {
          this.pollSaveToHDFS(response).then( (response) => {
            resolve(response);
          },  (error) => {
            reject(error);
          });
        } else {
          resolve(response);
        }
      }, (error) => {
        reject(error);
      });
    });

  },

  pollSaveToHDFS: function (data) {
    let url = this.urlForFindRecord(data.jobId, 'job') + "/results/csv/saveToHDFS";

    return new Ember.RSVP.Promise((resolve, reject) => {

      this.ajax(url).then( (response) => {
        if (response.status.toLowerCase() !== "TERMINATED".toLowerCase()) {
          Ember.run.later( () => {
            this.pollSaveToHDFS(response)
              .then((data) => { resolve(data); }, (error) => {
                reject(error);
              });
          }, 2000);
        } else {
            resolve(response);
        }
      }, (error) => {
        reject(error);
      });
    });
  },

  downloadAsCsv(jobId, path){
    let resultUrl = this.urlForFindRecord(jobId, 'job') + "/results/csv/" + path + ".csv";
    return resultUrl;
  }

});
