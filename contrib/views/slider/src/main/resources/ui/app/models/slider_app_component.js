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

App.SliderAppComponent = DS.Model.extend({

  /**
   * @type {string}
   */
  index: DS.attr('string'), // (appid+component_name+index)

  /**
   * @type {string}
   */
  status: DS.attr('string'),

  /**
   * @type {App.Host}
   */
  host: DS.belongsTo('App.Host')

});


App.SliderAppComponent.FIXTURES = [
  {
    id: 1,
    index: 'indx1',
    status: 'st1',
    host: 1
  },
  {
    id: 2,
    index: 'indx2',
    status: 'st1',
    host: 2
  },
  {
    id: 3,
    index: 'indx3',
    status: 'st3',
    host: 3
  },
  {
    id: 4,
    index: 'indx4',
    status: 'st4',
    host: 4
  },
  {
    id: 5,
    index: 'indx5',
    status: 'st5',
    host: 1
  }
];