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

import DS from 'ember-data';

export default DS.Model.extend({
  title: DS.attr('string'),
  queryId: DS.attr(),
  hiveQueryId: DS.attr('string'),
  queryFile: DS.attr('string'),
  owner: DS.attr('string'),
  dataBase: DS.attr('string'),
  duration: DS.attr(),
  status: DS.attr('string'),
  statusMessage: DS.attr('string'),
  dateSubmitted: DS.attr('date'),
  forcedContent: DS.attr('string'),
  logFile: DS.attr('string'),
  dagName:  DS.attr('string'),
  dagId: DS.attr('string'),
  sessionTag: DS.attr('string'),
  page: DS.attr(),
  statusDir: DS.attr('string'),
  applicationId: DS.attr(),
  referrer: DS.attr('string'),
  confFile: DS.attr('string'),
  globalSettings: DS.attr('string'),

  dateSubmittedTimestamp: function () {
    var date = this.get('dateSubmitted');

    return date; // ? date * 1000 : date; now dateSubmitted itself is in miliseconds. so conversion not required.
  }.property('dateSubmitted'),

  uppercaseStatus: function () {
    var status = this.get('status');

    return status ? status.toUpperCase() : status;
  }.property('status')
});
