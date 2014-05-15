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

App.QuickLink = DS.Model.extend({

  /**
   * @type {string}
   */
  label: DS.attr('string'),

  /**
   * @type {string}
   */
  url: DS.attr('string')

});

App.QuickLink.FIXTURES = [
  {
    id: 1,
    label: 'link1',
    url: 'url1'
  },
  {
    id: 2,
    label: 'link2',
    url: 'url2'
  },
  {
    id: 3,
    label: 'link3',
    url: 'url3'
  },
  {
    id: 4,
    label: 'link4',
    url: 'url4'
  },
  {
    id: 5,
    label: 'link5',
    url: 'url5'
  }
];