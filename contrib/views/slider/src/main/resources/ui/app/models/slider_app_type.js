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

App.SliderAppType = DS.Model.extend({

  /**
   * @type {string}
   */
  index: DS.attr('string'),

  /**
   * @type {string}
   */
  displayName: DS.attr('string'),

  /**
   * @type {App.SliderAppTypeComponent[]}
   */
  components: DS.hasMany('sliderAppTypeComponent', {async:true}),

  /**
   * @type {object}
   */
  configs: {}

});

App.SliderAppType.FIXTURES = [
  {
    id: 1,
    index: 'indx1',
    displayName: 'Index 1',
    components: [1, 2],
    configs: {
      c1: 'v1',
      c2: 'b1'
    }
  },
  {
    id: 2,
    index: 'indx2',
    displayName: 'Index 2',
    components: [2, 4, 5],
    configs: {
      c1: 'v2',
      c2: 'b2'
    }
  },
  {
    id: 3,
    index: 'indx3',
    displayName: 'Index 3',
    components: [1, 2, 4],
    configs: {
      c1: 'v3',
      c2: 'b3'
    }
  },
  {
    id: 4,
    index: 'indx4',
    displayName: 'Index 4',
    components: [5],
    configs: {
      c1: 'v4',
      c2: 'b4'
    }
  },
  {
    id: 5,
    index: 'indx5',
    displayName: 'Index 5',
    components: [1, 2, 3, 4, 5],
    configs: {
      c1: 'v5',
      c2: 'b5'
    }
  }
];