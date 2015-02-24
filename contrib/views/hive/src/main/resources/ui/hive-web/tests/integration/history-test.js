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

module('Integration: History', {
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

  visit("/history");

  andThen(function() {
    equal(find('#content .table tbody tr').length, 4);
  });
});

test('User should be able to filter the jobs', function() {
  expect(4);

  visit("/history");

  fillIn('column-filter input[placeholder=title]', "Query1");
  keyEvent('column-filter input[placeholder=title]', 'keyup');

  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 1, 'User is able to filter by title');
  });

  click('.clear-filters');
  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 4);
  });


  fillIn('column-filter input[placeholder=status]', "Finished");
  keyEvent('column-filter input[placeholder=status]', 'keyup');

  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 2, 'User is able to filter by status');
  });

  click('.clear-filters');
  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 4);
  });
});

test('A query item should expand to show the HQL', function() {
  expect(3);
  visit("/history");

  andThen(function() {
    equal(find('.table-expandable tbody .secondary-row').length, 0, 'All queries are collapsed');
  });

  click('.table-expandable tbody tr:first-child');

  andThen(function() {
    equal(find('.table-expandable tbody .secondary-row').length, 1, 'One query is expanded');
    ok(find('.table-expandable tbody tr:first-child').next().hasClass('secondary-row'), 'Clicked query is expanded');
  });
});
