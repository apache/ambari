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
import DS from 'ember-data';
import Utils from 'hawq-view/utils/utils';

export default DS.Model.extend({
  databaseName: DS.attr('string'),
  pid: DS.attr('number'),
  userName: DS.attr('string'),
  queryText: DS.attr('string'),
  waiting: DS.attr('boolean'),
  waitingResource: DS.attr('boolean'),
  duration: DS.attr('number'),
  queryStartTime: DS.attr('string'),
  clientHost: DS.attr('string'),
  clientPort: DS.attr('number'),
  applicationName: DS.attr('string'),

  clientAddress: Ember.computed('clientHost', 'clientPort', function () {
    return Utils.computeClientAddress(this.get('clientHost'), this.get('clientPort'));
  }),

  status: Ember.computed('waiting', 'waitingResource', function () {
    return Utils.generateStatusString(this.get('waiting'), this.get('waitingResource'));
  }),
  
  formattedDuration: Ember.computed('duration', function () {
    return Utils.formatDuration(this.get('duration'));
  })
});
