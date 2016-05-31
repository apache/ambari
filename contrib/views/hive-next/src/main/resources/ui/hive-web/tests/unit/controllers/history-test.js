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
import { moduleFor, test } from 'ember-qunit';

moduleFor('controller:history', 'HistoryController', {
  needs: [ 'service:file', 'service:job' ]
});

test('controller is initialized correctly', function () {
  expect(1);

  var component = this.subject();

  equal(component.get('columns.length'), 4, 'Columns are initialized');
});

test('date range is set correctly', function () {
  expect(2);

  var component = this.subject();
  var min = parseInt(Date.now() / 1000) - (60 * 60 * 24 * 60);
  var max = parseInt(Date.now() / 1000);

  var history = Ember.ArrayProxy.create({ content: [
    Ember.Object.create({
      dateSubmittedTimestamp: min
    }),
    Ember.Object.create({
      dateSubmittedTimestamp: max
    })
  ]});

  Ember.run(function() {
    component.set('history', history);
  });

  var dateColumn = component.get('columns').find(function (column) {
    return column.get('caption') === 'columns.date';
  });

  equal(dateColumn.get('dateRange.min'), min, 'Min date is set correctly');
  equal(dateColumn.get('dateRange.max'), max, 'Max date is set correctly');
});

test('interval duration is set correctly', function () {
  expect(2);

  var component = this.subject();

  var history = Ember.ArrayProxy.create({ content: [
    Ember.Object.create({
      duration: 20
    }),
    Ember.Object.create({
      duration: 300
    })
  ]});

  Ember.run(function() {
    component.set('history', history);
  });

  var durationColumn = component.get('columns').find(function (column) {
    return column.get('caption') === 'columns.duration';
  });

  equal(durationColumn.get('numberRange.min'), 20, 'Min value is set correctly');
  equal(durationColumn.get('numberRange.max'), 300, 'Max value is set correctly');
});

test('history filtering', function() {
  expect(2);

  var component = this.subject();

  var history = Ember.ArrayProxy.create({
    content: [
      Ember.Object.create({
        name: 'HISTORY',
        status: 1
      }),
      Ember.Object.create({
        name: '1HISTORY',
        status: 2
      })
    ]
  });

  Ember.run(function() {
    component.set('history', history);
  });

  equal(component.get('model.length'), 2, 'No filters applied we have 2 models');

  Ember.run(function() {
    component.filterBy('name', 'HISTORY', true);
  });

  equal(component.get('model.length'), 1, 'Filter by name we have 1 filtered model');
});
