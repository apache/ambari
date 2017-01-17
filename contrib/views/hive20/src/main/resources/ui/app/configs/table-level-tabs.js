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

let tableLevelTabs = [
  Ember.Object.create({
    name: 'columns',
    label: 'COLUMNS',
    link: 'databases.database.tables.table.columns',
    faIcon: 'list'
  }),
  Ember.Object.create({
    name: 'partitions',
    label: 'PARTITIONS',
    link: 'databases.database.tables.table.partitions',
    faIcon: 'file-text-o'
  }),
  Ember.Object.create({
    name: 'ddl',
    label: 'DDL',
    link: 'databases.database.tables.table.ddl',
    faIcon: 'file-text-o'
  }),
  Ember.Object.create({
    name: 'storage',
    label: 'STORAGE INFORMATION',
    link: 'databases.database.tables.table.storage',
    faIcon: 'file-text-o'
  }),
  Ember.Object.create({
    name: 'detailedInfo',
    label: 'DETAILED INFORMATION',
    link: 'databases.database.tables.table.details',
    faIcon: 'file-text-o'
  }),
  Ember.Object.create({
    name: 'viewInfo',
    label: 'VIEW INFORMATION',
    link: 'databases.database.tables.table.view',
    faIcon: 'file-text-o'
  }),
  Ember.Object.create({
    name: 'statistics',
    label: 'STATISTICS',
    link: 'databases.database.tables.table.stats',
    faIcon: 'line-chart'
  }),
  Ember.Object.create({
    name: 'authorization',
    label: 'AUTHORIZATION',
    link: 'databases.database.tables.table.auth',
    faIcon: 'users'
  })
];

export default tableLevelTabs;
