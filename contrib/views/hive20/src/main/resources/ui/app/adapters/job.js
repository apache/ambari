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

import ApplicationAdapter from './application';

export default ApplicationAdapter.extend({
  fetchResult(jobId) {
    let resultUrl = this.urlForFindRecord(jobId, 'job') + "/results";
    return this.ajax(resultUrl, 'GET');
  },

  getQuery(job) {
    let queryUrl = this.buildURL() + "/file" + encodeURI(job.get('queryFile'));
    console.log(queryUrl);
  },

  saveToHDFS(jobId, path){
    let resultUrl = this.urlForFindRecord(jobId, 'job').replace('/resources','')  + "/results/csv/saveToHDFS?commence=true&file=" + path + ".csv";
    return this.ajax(resultUrl, 'GET');
  }

});
