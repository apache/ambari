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

moduleFor('controller:index', 'IndexController', {
  needs: ['controller:databases', 'controller:open-queries', 'controller:insert-udfs',
          'controller:udfs', 'controller:loaded-files',
          'controller:index/history-query/logs',
          'controller:index/history-query/results',
          'controller:index/history-query/explain',
          'controller:settings',
          'adapter:database', 'controller:tables', 'controller:columns']
});

test('when initialized, controller sets the queryProcessTabs.', function () {
  expect(1);

  var controller = this.subject();

  ok(controller.get('queryProcessTabs', 'queryProcessTabs is initialized.'));
});

test('databasesChanged sets null the selectedTables property of open-queries if databases controller has not set its selectedDatabase.tables property', function () {
  expect(1);

  var controller = this.subject();

  equal(controller.get('databases.selectedTables'), null, 'databases controller property selectedDatabase.tables not set. open-queries selectedTables returns null');
});

test('modelChanged calls update on the open-queries cotnroller.', function () {
  expect(1);

  var controller = this.subject();

  controller.set('openQueries.update', function () {
    var defer = Ember.RSVP.defer();

    ok(true, 'index model has changed. update was called on open-queries controller.');

    defer.resolve();

    return defer.promise;
  });

  Ember.run(function () {
    controller.set('model', Ember.Object.create());
  });
});

test('bindQueryParams replaces param placeholder with values', function() {
  expect(1);

  var controller = this.subject();
  var queryParams = [
    { name: '$what', value: 'color' },
    { name: '$where', value: 'z'}
  ];

  var query = "select $what from $where";
  var replacedQuery = "select color from z";

  Ember.run(function() {
    controller.get('queryParams').setObjects(queryParams);
  });

  equal(controller.bindQueryParams(query), replacedQuery, 'Params replaced correctly');
});

test('bindQueryParams replaces same param multiple times', function() {
  expect(1);

  var controller = this.subject();
  var queryParams = [
    { name: '$what', value: 'color' },
    { name: '$where', value: 'z'}
  ];

  var query = "select $what from $where as $what";
  var replacedQuery = "select color from z as color";

  Ember.run(function() {
    controller.get('queryParams').setObjects(queryParams);
  });

  equal(controller.bindQueryParams(query), replacedQuery, 'Params replaced correctly');
});

test('parseQueryParams sets queryParams when query changes', function() {
  expect(3);

  var controller = this.subject();

  var query = "select $what from $where";

  Ember.run(function() {
    controller.set('openQueries.currentQuery', {
        'fileContent': query
    });
  });

  equal(controller.get('queryParams.length'), 2, '2 queryParams parsed');
  equal(controller.get('queryParams').objectAt(0).name, '$what', 'First param parsed correctly');
  equal(controller.get('queryParams').objectAt(1).name, '$where', 'Second param parsed correctly');
});

test('canExecute return false if query is executing', function() {
  expect(2);
  var controller = this.subject();

  Ember.run(function() {
    controller.set('openQueries.update', function () {
      var defer = Ember.RSVP.defer();
      defer.resolve();
      return defer.promise;
    });

    controller.set('model', Ember.Object.create({ 'isRunning': false }));
    controller.set('queryParams', []);
  });

  ok(controller.get('canExecute'), 'Query is not executing => canExecute return true');

  Ember.run(function() {
    controller.set('model', Ember.Object.create({ 'isRunning': true }));
  });

  ok(!controller.get('canExecute'), 'Query is executing => canExecute return false');
});

test('canExecute return false if queryParams doesnt\'t have values', function() {
  expect(2);
  var controller = this.subject();

  var paramsWithoutValues = [
    { name: '$what', value: '' },
    { name: '$where', value: '' }
  ];

  var paramsWithValues = [
    { name: '$what', value: 'value1' },
    { name: '$where', value: 'value2' }
  ];

  Ember.run(function() {
    controller.set('openQueries.update', function () {
      var defer = Ember.RSVP.defer();
      defer.resolve();
      return defer.promise;
    });
    controller.set('model', Ember.Object.create({ 'isRunning': false }));
    controller.get('queryParams').setObjects(paramsWithoutValues);
  });

  ok(!controller.get('canExecute'), 'Params without value => canExecute return false');

  Ember.run(function() {
    controller.get('queryParams').setObjects(paramsWithValues);
  });

  ok(controller.get('canExecute'), 'Params with values => canExecute return true');
});
