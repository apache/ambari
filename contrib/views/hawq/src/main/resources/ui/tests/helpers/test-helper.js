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
    "href": "http://c6401.ambari.apache.org:8080/api/v1/views/HAWQ/versions/1.0.0/instances/HAWQ/queries?fields=*",
    "items": [
      {
        "href": "http://c6401.ambari.apache.org:8080/api/v1/views/HAWQ/versions/1.0.0/instances/HAWQ/queries/116662",
        "id": "116662",
        "instance_name": "HAWQ",
        "type": "query",
        "version": "1.0.0",
        "view_name": "HAWQ",
        "attributes": {
          "application_name": "",
          "backend_start": "2016-10-25 19:24:19",
          "client_addr": "192.168.64.101",
          "client_port": 34941,
          "current_query": "SELECT * FROM pg_stat_activity",
          "datid": 1,
          "datname": "template1",
          "procpid": 116662,
          "query_duration": 0.0,
          "query_start": "2016-10-25 19:31:04",
          "sess_id": 420,
          "usename": "gpadmin",
          "usesysid": 10,
          "waiting": false,
          "waiting_resource": false,
          "xact_start": "2016-10-25 19:31:04"
        }
      },
      {
        "href": "http://c6401.ambari.apache.org:8080/api/v1/views/HAWQ/versions/1.0.0/instances/HAWQ/queries/12345",
        "id": "12345",
        "instance_name": "HAWQ",
        "type": "query",
        "version": "1.0.0",
        "view_name": "HAWQ",
        "attributes": {
          "application_name": "",
          "backend_start": "2016-10-25 19:24:19",
          "client_addr": "192.168.64.104",
          "client_port": 42811,
          "current_query": "SELECT * FROM customers",
          "datid": 1,
          "datname": "gpadmin",
          "procpid": 12345,
          "query_duration": 0.0,
          "query_start": "2016-10-25 19:31:24",
          "sess_id": 420,
          "usename": "gpadmin",
          "usesysid": 10,
          "waiting": true,
          "waiting_resource": true,
          "xact_start": "2016-10-25 19:31:24"
        }
      }
    ]
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
      'queryStartTime': '2016-02-16 16:41:13',
      'clientAddress': 'local',
      'formattedDuration': '00:00:00'
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
      'queryStartTime': '2016-02-16 16:41:13',
      'clientAddress': 'local',
      'formattedDuration': '01:00:00'
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
      'queryStartTime': '2016-02-16 16:41:13',
      'clientAddress': 'local',
      'formattedDuration': '00:00:01'
    }];

  var queries = [];

  for (var i = 0, ii = mockQueries.length; i < ii; ++i) {
    queries.push(Ember.Object.extend(mockQueries[i]).create());
  }

  return queries;
}

export {getMockPayload, makeQueryObjects};