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

export default Ember.Service.extend({
  stopJob: function (job) {
    var self = this;
    var id = job.get('id');
    var url = this.container.lookup('adapter:application').buildURL();
    url +=  "/jobs/" + id;

    job.set('isCancelling', true);

    Ember.$.ajax({
       url: url,
       type: 'DELETE',
       headers: {
        'X-Requested-By': 'ambari',
       },
       success: function () {
         job.reload();
       }
    });
  },

  fetchJobStatus: function (jobId) {
    console.log("finding status of job : ", jobId);
    var self = this;
    var url = this.container.lookup('adapter:application').buildURL();
    url +=  "/jobs/" + jobId + "/status";

    return Ember.$.ajax({
      url: url,
      type: 'GET',
      headers: {
        'X-Requested-By': 'ambari'
      }
    });
  }
});
