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

module('Integration: Saved Queries', {
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

test('Save Queries should list saved queries', function() {
  expect(1);

  visit("/queries");


  andThen(function() {
    equal(find('#content .table tbody tr').length, 2);
  });
});

test('User should be able to filter the queries', function() {
  expect(8);

  visit("/queries");

  fillIn('column-filter input[placeholder=preview]', "select count");
  keyEvent('column-filter input[placeholder=preview]', 'keyup');

  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 1, 'User is able to filter by short query form.');
  });

  click('.clear-filters');
  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 2);
  });

  fillIn('column-filter input[placeholder=title]', "saved1");
  keyEvent('column-filter input[placeholder=title]', 'keyup');

  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 1, 'User is able to filter by title');
  });

  click('.clear-filters');
  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 2);
  });

  fillIn('column-filter input[placeholder=database]', "db1");
  keyEvent('column-filter input[placeholder=database]', 'keyup');

  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 1, 'User is able to filter by database');
  });

  click('.clear-filters');
  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 2);
  });

  fillIn('column-filter input[placeholder=owner]', "owner1");
  keyEvent('column-filter input[placeholder=owner]', 'keyup');

  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 1, 'User is able to filter by owner');
  });

  click('.clear-filters');
  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 2);
  });
});

test('User is able to load a query from saved queries', function() {
  expect(1);

  visit("/queries");
  click('#content .table tbody tr:first-child td:first-child a');

  andThen(function() {
    equal(currentURL(), "/queries/1", 'User is redirected');
  });
});

test('Saved Query options menu', function() {
  expect(2);

  visit("/queries");
  click('.fa-gear');

  andThen(function() {
    equal(find('.dropdown-menu:visible').length, 1, 'Query menu is visible');
    equal(find('.dropdown-menu:visible li').length, 2, 'Query menu has 2 options');
  });
});