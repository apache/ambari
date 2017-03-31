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

let resultsTabs = [
  Ember.Object.create({
    name: 'results',
    label: 'RESULTS',
    link: 'queries.query.results',
    faIcon: 'file-text-o'
  }),
  Ember.Object.create({
    name: 'log',
    label: 'LOG',
    link: 'queries.query.log',
    faIcon: 'list'
  }),
  Ember.Object.create({
    name: 'visual-explain',
    label: 'VISUAL EXPLAIN',
    link: 'queries.query.visual-explain',
    faIcon: 'link'
  }),
  Ember.Object.create({
    name: 'tez-ui',
    label: 'TEZ UI',
    link: 'queries.query.tez-ui',
    faIcon: 'paper-plane'
  })
];

export default resultsTabs;
