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

export function getMockPayload() {
  return {
    data: [
      {
        id: 1,
        type: 'query',
        attributes: {
          'database-name': 'template1',
          'pid': 90201,
          'user-name': 'gpadmin',
          'waiting': false,
          'waiting-resource': false,
          'duration': '00:00:12',
          'query-start-time': '2016-02-16T16:41:13-08:00',
          'formatted-query-start-time': '2016-02-16T16:41:13-08:00',
          'client-host': '127.0.0.1',
          'client-port': 9999,
          'application-name': 'psql'
        }
      }, {
        id: 2,
        type: 'query',
        attributes: {
          'database-name': 'gpadmin',
          'pid': 13345,
          'user-name': 'foo',
          'waiting': true,
          'waiting-resource': true,
          'duration': '00:20:12',
          'query-start-time': '1963-10-21T00:00:00-08:00',
          'formatted-query-start-time': '2016-02-16T16:41:13-08:00',
          'client-port': -1,
          'application-name': 'mock'
        }
      }],
    'server-time': '1963-10-21T00:43:15-08:00'
  };
}

export function makeQueryObjects() {
  var mockQueries = [
    {
      'id': 1,
      'databaseName': 'template1',
      'pid': 90210,
      'userName': 'gpadmin',
      'status': 'Running',

      // Used to verify the CSS class for the status because the class is generated in the hbs
      'waiting': false,
      'waitingResource': false,
      'statusClass': 'green',
      'duration': '02:30:57',
      'queryStartTime': '2016-02-16T16:41:13-08:00',
      'formattedQueryStartTime': '2016-02-16T16:41:13-08:00',
      'clientAddress': 'local'
    }, {
      'id': 2,
      'databaseName': 'DB2',
      'pid': 421,
      'userName': 'thor',
      'status': 'Running',

      // Used to verify the CSS class for the status because the class is generated in the hbs
      'waiting': true,
      'waitingResource': false,
      'statusClass': 'orange',
      'duration': '01:20:12',
      'queryStartTime': '2016-02-16T16:41:13-08:00',
      'formattedQueryStartTime': '2016-02-16T16:41:13-08:00',
      'clientAddress': 'local'
    }, {
      'id': 3,
      'databaseName': 'FoxPro',
      'pid': 3221,
      'userName': 'batman',
      'status': 'Running',

      // Used to verify the CSS class for the status because the class is generated in the hbs
      'waiting': false,
      'waitingResource': true,
      'statusClass': '',
      'duration': '00:20:12',
      'queryStartTime': '2016-02-16T16:41:13-08:00',
      'formattedQueryStartTime': '2016-02-16T16:41:13-08:00',
      'clientAddress': 'local'
    }];

  var queries = [];

  for (var i = 0, ii = mockQueries.length; i < ii; ++i) {
    queries.push(Ember.Object.extend(mockQueries[i]).create());
  }

  return queries;
}

export { getMockPayload, makeQueryObjects };