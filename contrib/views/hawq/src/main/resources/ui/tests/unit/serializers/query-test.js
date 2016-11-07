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

import {moduleForModel, test} from 'ember-qunit';
import Pretender from 'pretender';
import {getMockPayload} from 'hawq-view/tests/helpers/test-helper';

var mockPayload = getMockPayload();
let server;

moduleForModel('query', 'Unit | Serializer | query', {
  needs: ['serializer:query'],

  beforeEach() {
    server = new Pretender(function () {
      this.get('/queries', function () {
        return [200, {'Content-Type': 'application/json'}, JSON.stringify(mockPayload)];
      });
    });
  },

  afterEach() {
    server.shutdown();
  }
});

test('it serializes all records', function (assert) {
  return this.store().query('query', {fields: '*'}).then(function (queries) {
    assert.equal(queries.get('length'), 2);
  });
});

test('transforms payload correctly', function (assert) {
  return this.store().query('query', {fields: '*'}).then(function (queries) {
    let expectedRecords = [
      {
        "applicationName": "",
        "clientHost": "192.168.64.101",
        "clientPort": 34941,
        "queryText": "SELECT * FROM pg_stat_activity",
        "databaseName": "template1",
        "pid": 116662,
        "duration": 0.0,
        "queryStartTime": "2016-10-25 19:31:04",
        "userName": "gpadmin",
        "waiting": false,
        "waitingResource": false
      },
      {
        "applicationName": "",
        "clientHost": "192.168.64.104",
        "clientPort": 42811,
        "queryText": "SELECT * FROM customers",
        "databaseName": "gpadmin",
        "pid": 12345,
        "duration": 0.0,
        "queryStartTime": "2016-10-25 19:31:24",
        "userName": "gpadmin",
        "waiting": true,
        "waitingResource": true
      }
    ];

    const keyList = [
      "applicationName",
      "clientHost",
      "clientPort",
      "queryText",
      "databaseName",
      "pid",
      "duration",
      "queryStartTime",
      "userName",
      "waiting",
      "waitingResource"
    ];

    for (var i = 0, ii = expectedRecords.length; i < ii; i++) {
      for (var j = 0; j < keyList.length; j++) {
        assert.equal(queries.objectAtContent(i).get(keyList[j]), expectedRecords[i][keyList[j]]);
      }
    }
  }, this);
});