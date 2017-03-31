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

let topLevelTabs = [
  Ember.Object.create({
    name: 'query',
    label: 'QUERY',
    link: 'queries',
    faIcon: 'paper-plane'
  }),
  Ember.Object.create({
    name: 'jobs',
    label: 'JOBS',
    link: 'jobs',
    faIcon: 'paper-plane'
  }),
  Ember.Object.create({
    name: 'tables',
    label: 'TABLES',
    link: 'databases',
    faIcon: 'table'
  }),
  Ember.Object.create({
    name: 'saves-queries',
    label: 'SAVED QUERIES',
    link: 'savedqueries',
    faIcon: 'paperclip'
  }),
  Ember.Object.create({
    name: 'udfs',
    label: 'UDFs',
    link: 'udfs',
    faIcon: 'puzzle-piece'
  }),
  Ember.Object.create({
    name: 'settings',
    label: 'SETTINGS',
    link: 'settings',
    faIcon: 'cog'
  }),
  Ember.Object.create({
    name: 'notifications',
    label: 'NOTIFICATIONS',
    link: 'messages',
    faIcon: 'bell',
    pullRight: true
  })
];

export default topLevelTabs;
