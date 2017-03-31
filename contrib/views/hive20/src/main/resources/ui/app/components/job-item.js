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

export default Ember.Component.extend({
  jobs: Ember.inject.service(),
  tagName: '',
  expanded: false,
  expandedValue: null,
  actions: {
    toggleExpandJob(jobId) {
      if(this.get('expanded')) {
        this.set('expanded', false);
      } else {
        this.set('expanded', true);
        this.set('valueLoading', true);
        this.get('jobs').getQuery(jobId).then((queryFile) => {
          this.set('queryFile', queryFile);
          this.set('valueLoading', false);
        }).catch((err) => {
          console.log('err', err);
          this.set('valueLoading', false);
        });
      }

    }
  }
});
