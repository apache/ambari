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

export default Ember.Controller.extend({
  moment: Ember.inject.service(),
  queryParams: ['startTime', 'endTime'],
  startTime: null,
  endTime: null,


  startTimeText: Ember.computed('startTime', function() {
    let st = typeof(this.get('startTime')) === 'string' ? parseInt(this.get('startTime')) : this.get('startTime');
    return this.get('moment').moment(st).format('YYYY-MM-DD');
  }),

  endTimeText: Ember.computed('endTime', function() {
    let et = typeof(this.get('endTime')) === 'string' ? parseInt(this.get('endTime')) : this.get('endTime');
    return this.get('moment').moment(et).format('YYYY-MM-DD');
  })

});
