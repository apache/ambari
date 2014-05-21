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

App.SliderApp = DS.Model.extend({

  /**
   * @type {string}
   */
  index: DS.attr('string'),

  /**
   * @type {string}
   */
  yarnId: DS.attr('string'),

  /**
   * @type {string}
   */
  name: DS.attr('string'),

  /**
   * @type {status}
   */
  status: DS.attr('string'),

  /**
   * @type {string}
   */
  user: DS.attr('string'),

  /**
   * @type {number}
   */
  started: DS.attr('number'),

  /**
   * @type {number}
   */
  ended: DS.attr('number'),

  /**
   * @type {App.SliderAppType}
   */
  appType: DS.belongsTo('sliderAppType'),

  /**
   * @type {string}
   */
  diagnostics: DS.attr('string'),

  /**
   * @type {App.SliderAppComponent[]}
   */
  components: DS.hasMany('sliderAppComponent', {async:true}),

  /**
   * @type {App.QuickLink[]}
   */
  quickLinks: DS.hasMany('quickLink', {async:true}),

  /**
   * @type {App.TypedProperty[]}
   */
  runtimeProperties: DS.hasMany('typedProperty', {async:true})
});

App.SliderApp.FIXTURES = [
  {
    id: 1,
    index: 'indx1',
    yarnId: 'y1',
    name: 'name1',
    status: 'FROZEN',
    user: 'u1',
    started: 1400132912,
    ended: 1400152912,
    appType: 1,
    diagnostics: 'd1',
    components: [3, 4, 5],
    quickLinks: [1, 2, 6],
    runtimeProperties: [1, 2, 3]
  },
  {
    id: 2,
    index: 'indx2',
    yarnId: 'y2',
    name: 'name2',
    status: 'Running',
    user: 'u2',
    started: 1400132912,
    ended: 1400152912,
    appType: 2,
    diagnostics: 'd2',
    components: [1, 3],
    quickLinks: [4, 5, 6],
    runtimeProperties: [3, 4]
  },
  {
    id: 3,
    index: 'indx3',
    yarnId: 'y3',
    name: 'name3',
    status: 'Running',
    user: 'u3',
    started: 1400132912,
    ended: 1400152912,
    appType: 3,
    diagnostics: 'd3',
    components: [1],
    quickLinks: [1, 2, 4, 5, 6],
    runtimeProperties: [2, 3]
  },
  {
    id: 4,
    index: 'indx4',
    yarnId: 'y4',
    name: 'name4',
    status: 'Running',
    user: 'u4',
    started: 1400132912,
    ended: 1400152912,
    appType: 4,
    diagnostics: 'd4',
    components: [1, 2, 3, 4, 5],
    quickLinks: [4, 6],
    runtimeProperties: [1, 2, 3, 4, 5]
  },
  {
    id: 5,
    index: 'indx5',
    yarnId: 'y5',
    name: 'name5',
    status: 'Running',
    user: 'u5',
    started: 1400132912,
    ended: 1400152912,
    appType: 5,
    diagnostics: 'd5',
    components: [2, 5],
    quickLinks: [3, 4, 6],
    runtimeProperties: [1, 2, 3]
  }
];

App.SliderApp.Status = {
  running: "Running",
  frozen: "Frozen",
  destroyed: "Destroyed",
  initialized: "Initialized"
};