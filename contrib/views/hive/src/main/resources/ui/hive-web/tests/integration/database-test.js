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
import { test } from 'ember-qunit';
import startApp from '../helpers/start-app';
import api from '../helpers/api-mock';

var App;
var server;

module('Integration: Databases', {
  setup: function() {
    App = startApp();
    /* global Pretender: true */
    server = new Pretender(api);
  },
  teardown: function() {
    Ember.run(App, App.destroy);
    server.shutdown();
  }
});

test('Database Explorer is displayed and populated with databases from server.', function (assert) {
  assert.expect(2);

  visit('/');

  andThen(function() {
    equal(find('.database-explorer').length, 1, 'Databases panel is visible.');
    equal(find('.database-explorer .databases').children().length, 3, 'Databases are listed.');
  });
});

test('Expanding a database will retrieve the first page of tables for that database.', function () {
  expect(1);

  visit('/');

  andThen(function () {
    var targetDB = find('.fa-database').first();

    click(targetDB);

    andThen(function () {
      equal(find('.fa-table').length, 3);
    });
  });
});

test('Expanding a table will retrieve the first page of columns for that table.', function () {
  expect(2);

  visit('/');

  andThen(function () {
    var targetDB = find('.fa-database').first();

    click(targetDB);

    andThen(function () {
      var targetTable = find('.fa-table').first();

      click(targetTable);

      andThen(function () {
        equal(find('.columns').length, 1, 'Columns container was loaded.');
        equal(find('.columns strong').length, 3, '3 columns were loaded for selected table.');
      });
    });
  });
});

test('Searching for a table will display table results and column search field', function () {
  expect(2);

  visit('/');

  andThen(function () {
    fillIn(find('input').first(), 'table');
    keyEvent(find('input').first(), 'keyup', 13);

    andThen(function () {
      equal(find('input').length, 2, 'Columns search input has been rendered.');
      equal(find('.nav-tabs li').length, 2, 'Results tab has been redendered.');
    });
  });
});
