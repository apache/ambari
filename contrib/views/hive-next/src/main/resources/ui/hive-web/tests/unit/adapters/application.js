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

import {
  moduleFor,
  test
} from 'ember-qunit';

import constants from 'hive/utils/constants';

moduleFor('adapter:application', 'ApplicationAdapter', {
  // Specify the other units that are required for this test.
  // needs: ['serializer:foo']
});

// Replace this with your real tests.
test('X-Requested-By header is set.', function() {
  expect(1);

  var adapter = this.subject();

  ok(adapter.get('headers.X-Requested-By'), 'X-Requested-By is set to a truthy value.');
});

test('buildUrl returns an url with default values for version and instance paramters if not running within an Ambari instance.', function () {
  expect(1);

  var adapter = this.subject();

  var url = adapter.buildURL();

  equal(url, constants.adapter.apiPrefix + constants.adapter.version + constants.adapter.instancePrefix + 'Hive');
});
