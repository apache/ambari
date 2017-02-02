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
  startTime: null,
  endTime: null,
  maxEndTime: null,
  statusCounts: Ember.computed('jobs', function() {
    return this.get('jobs').reduce((acc, item, index) => {
      let status = item.get('status').toLowerCase();
      if(Ember.isEmpty(acc[status])) {
        acc[status] = 1;
      } else {
        acc[status] = acc[status] + 1;
      }

      return acc;
    }, {});
  }),


  actions: {
    setDateRange(startDate, endDate) {
      this.sendAction('filterChanged', startDate, endDate);
    }
  }
});
