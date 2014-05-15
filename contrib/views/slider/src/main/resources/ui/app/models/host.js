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

App.Host = DS.Model.extend({

  /**
   * @type {string}
   */
  hostName: DS.attr('string'),

  /**
   * @type {string}
   */
  publicHostName: DS.attr('string')

});

App.Host.FIXTURES = [
  {
    id: 1,
    hostName: 'host1',
    publicHostName: 'Host 1'
  },
  {
    id: 2,
    hostName: 'host2',
    publicHostName: 'Host 2'
  },
  {
    id: 3,
    hostName: 'host3',
    publicHostName: 'Host 3'
  },
  {
    id: 4,
    hostName: 'host 4',
    publicHostName: 'Host 4'
  }
];