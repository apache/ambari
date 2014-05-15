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

App.SliderAppTypeComponent = DS.Model.extend({

  /**
   * @type {string}
   */
  index: DS.attr('string'), // (app-type + name)

  /**
   * @type {string}
   */
  name: DS.attr('string'),

  /**
   * @type {string}
   */
  displayName: DS.attr('string'),

  /**
   * @type {number}
   */
  defaultNumInstances: DS.attr('number'),

  /**
   * @type {number}
   */
  defaultYARNMemory: DS.attr('number'),

  /**
   * @type {number}
   */
  defaultYARNCPU: DS.attr('number')

});

App.SliderAppTypeComponent.FIXTURES = [
  {
    id: 1,
    index: 'indx1',
    name: 'name1',
    displayName: 'Name 1',
    defaultNumInstances: 10,
    defaultYARNMemory: 1000,
    defaultYARNCPU: 2
  },
  {
    id: 2,
    index: 'indx2',
    name: 'name2',
    displayName: 'Name 2',
    defaultNumInstances: 10,
    defaultYARNMemory: 1000,
    defaultYARNCPU: 2
  },
  {
    id: 3,
    index: 'indx3',
    name: 'name3',
    displayName: 'Name 3',
    defaultNumInstances: 10,
    defaultYARNMemory: 1000,
    defaultYARNCPU: 2
  },
  {
    id: 4,
    index: 'indx4',
    name: 'name4',
    displayName: 'Name 4',
    defaultNumInstances: 10,
    defaultYARNMemory: 1000,
    defaultYARNCPU: 2
  },
  {
    id: 5,
    index: 'indx5',
    name: 'name5',
    displayName: 'Name 5',
    defaultNumInstances: 10,
    defaultYARNMemory: 1000,
    defaultYARNCPU: 2
  }
];