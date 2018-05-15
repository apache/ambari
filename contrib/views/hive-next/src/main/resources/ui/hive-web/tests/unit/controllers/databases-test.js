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

var controller;
var store;

moduleFor('controller:databases', 'DatabasesController', {
  needs: [ 'adapter:database',
           'service:database',
           'service:notify',
           'model:database' ],

  setup: function () {
    //mock getDatabases which is called on controller init
    this.container.lookup('service:database').getDatabases = function () {
      var defer = Ember.RSVP.defer();

      defer.resolve();

      return defer.promise;
    };

    //mock getDatabasesFromServer which is called by the poller
    this.container.lookup('service:database').getDatabasesFromServer = function () {
     var defer = Ember.RSVP.defer();

     var databases = [ "database_a", "database_b"];

     defer.resolve(databases);
     return defer.promise;
     };

    store = this.container.lookup('store:main');
    controller = this.subject();
    controller.store = store;

  },

  teardown: function () {
    Ember.run(controller, controller.destroy);
  }
});

test('controller is initialized properly.', function () {
  expect(5);

  var controller = this.subject();

  ok(controller.get('tableSearchResults'), 'table search results collection was initialized.');
  ok(controller.get('tabs'), 'tabs collection was initialized.');
  equal(controller.get('tabs.length'), 2, 'tabs collection contains two tabs');
  equal(controller.get('tabs').objectAt(0).get('name'), Ember.I18n.t('titles.explorer'), 'first tab is database explorer.');
  equal(controller.get('tabs').objectAt(1).get('name'), Ember.I18n.t('titles.results'), 'second tab is search results');
});

test('setTablePageAvailability sets canGetNextPage true if given database hasNext flag is true.', function () {
  expect(1);

  var database = Ember.Object.create( { hasNext: true } );

  controller.setTablePageAvailability(database);

  equal(database.get('canGetNextPage'), true);
});

test('setTablePageAvailability sets canGetNextPage true if given database has more loaded tables than the visible ones.', function () {
  expect(1);

  var database = Ember.Object.create({
    tables: [1],
    visibleTables: []
  });

  controller.setTablePageAvailability(database);

  equal(database.get('canGetNextPage'), true);
});

test('setTablePageAvailability sets canGetNextPage falsy if given database hasNext flag is falsy and all loaded tables are visible.', function () {
  expect(1);

  var database = Ember.Object.create({
    tables: [1],
    visibleTables: [1]
  });

  controller.setTablePageAvailability(database);

  ok(!database.get('canGetNextPage'));
});

test('setColumnPageAvailability sets canGetNextPage true if given table hasNext flag is true.', function () {
  expect(1);

  var table = Ember.Object.create( { hasNext: true } );

  controller.setColumnPageAvailability(table);

  equal(table.get('canGetNextPage'), true);
});

test('setColumnPageAvailability sets canGetNextPage true if given table has more loaded columns than the visible ones.', function () {
  expect(1);

  var table = Ember.Object.create({
    columns: [1],
    visibleColumns: []
  });

  controller.setColumnPageAvailability(table);

  equal(table.get('canGetNextPage'), true);
});

test('setColumnPageAvailability sets canGetNextPage true if given database hasNext flag is falsy and all loaded columns are visible.', function () {
  expect(1);

  var table = Ember.Object.create({
    columns: [1],
    visibleColumns: [1]
  });

  controller.setColumnPageAvailability(table);

  ok(!table.get('canGetNextPage'));
});

test('getTables sets the visibleTables as the first page of tables if they are already loaded', function () {
  expect(2);

  var database = Ember.Object.create({
    name: 'test_db',
    tables: [1, 2, 3]
  });

  controller.get('databases').pushObject(database);
  controller.set('pageCount', 2);

  controller.send('getTables', 'test_db');

  equal(database.get('visibleTables.length'), controller.get('pageCount'), 'there are 2 visible tables out of 3.');
  equal(database.get('canGetNextPage'), true, 'user can get next tables page.');
});

test('getColumns sets the visibleColumns as the first page of columns if they are already loaded.', function () {
  expect(2);

  var table = Ember.Object.create({
    name: 'test_table',
    columns: [1, 2, 3]
  });

  var database = Ember.Object.create({
    name: 'test_db',
    tables: [ table ],
    visibleTables: [ table ]
  });

  controller.set('pageCount', 2);

  controller.send('getColumns', 'test_table', database);

  equal(table.get('visibleColumns.length'), controller.get('pageCount'), 'there are 2 visible columns out of 3.');
  equal(table.get('canGetNextPage'), true, 'user can get next columns page.');
});

test('showMoreTables pushes more tables to visibleTables if there are still hidden tables loaded.', function () {
  expect(2);

  var database = Ember.Object.create({
    name: 'test_db',
    tables: [1, 2, 3],
    visibleTables: [1]
  });

  controller.get('databases').pushObject(database);
  controller.set('pageCount', 1);

  controller.send('showMoreTables', database);

  equal(database.get('visibleTables.length'), controller.get('pageCount') * 2, 'there are 2 visible tables out of 3.');
  equal(database.get('canGetNextPage'), true, 'user can get next tables page.');
});

test('showMoreColumns pushes more columns to visibleColumns if there are still hidden columns loaded.', function () {
  expect(2);

  var table = Ember.Object.create({
    name: 'test_table',
    columns: [1, 2, 3],
    visibleColumns: [1]
  });

  var database = Ember.Object.create({
    name: 'test_db',
    tables: [ table ],
    visibleTables: [ table ]
  });

  controller.set('pageCount', 1);

  controller.send('showMoreColumns', table, database);

  equal(table.get('visibleColumns.length'), controller.get('pageCount') * 2, 'there are 2 visible columns out of 3.');
  equal(table.get('canGetNextPage'), true, 'user can get next columns page.');
});

test('syncDatabases pushed more databases when new databases are added in the backend', function() {
  expect(3);

  var databaseA = {
    id: "database_a",
    name: "database_a"
  };

  Ember.run(function() {
    store.createRecord('database', databaseA);
    controller.syncDatabases();
  });

  var latestDbNames = store.all('database').mapBy('name');
  equal(latestDbNames.length, 2, "There is 1 additional database added to hive");
  equal(latestDbNames.contains("database_a"), true, "New database list should contain the old database name.");
  equal(latestDbNames.contains("database_b"), true, "New database list should contain the new database name.");
});

test('syncDatabases removed database when old databases are removed in the backend', function() {
  expect(4);

  var latestDbNames;

  var databaseA = {
    id: "database_a",
    name: "database_a"
  };
  var databaseB = {
    id: "database_b",
    name: "database_b"
  };
  var databaseC = {
    id: "database_c",
    name: "database_c"
  };

  Ember.run(function() {
    store.createRecord('database', databaseA);
    store.createRecord('database', databaseB);
    store.createRecord('database', databaseC);
    controller.syncDatabases();
  });

  latestDbNames = store.all('database').mapBy('name');
  equal(latestDbNames.length, 2, "One database is removed from hive");
  equal(latestDbNames.contains("database_a"), true, "New database list should contain the old database name.");
  equal(latestDbNames.contains("database_b"), true, "New database list should contain the old database name.");
  equal(latestDbNames.contains("database_c"), false, "New database list should not contain the database name removed in the backend.");

});
