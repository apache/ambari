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

App.TypedProperty = DS.Model.extend({

  /**
   * @type {string}
   */
  key: DS.attr('string'),

  /**
   * @type {string}
   */
  value: DS.attr('string'),

  /**
   * @type {string}
   */
  type: DS.attr('string') // (one of 'date', 'host')

});

App.TypedProperty.FIXTURES = [
  {
    id: 1,
    key: 'k1',
    value: 'v1',
    type: 'host'
  },
  {
    id: 2,
    key: 'k2',
    value: 'v2',
    type: 'host'
  },
  {
    id: 3,
    key: 'k3',
    value: 'v3',
    type: 'date'
  },
  {
    id: 4,
    key: 'k4',
    value: 'v4',
    type: 'date'
  },
  {
    id: 5,
    key: 'k5',
    value: 'v5',
    type: 'host'
  }
];