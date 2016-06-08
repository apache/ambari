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

module('Integration: Udfs', {
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

  visit("/udfs");

  andThen(function() {
    equal(find('#content .table tbody tr').length, 2);
  });
});

test('User should be able to filter the udfs', function() {
  expect(4);

  visit("/udfs");

  fillIn('column-filter input[placeholder="udf name"]', "TestColumn");
  keyEvent('column-filter input[placeholder="udf name"]', 'keyup');

  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 1, 'User is able to filter by name');
  });

  click('.clear-filters');

  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 2);
  });

  fillIn('column-filter input[placeholder="udf class name"]', "TestClassName");
  keyEvent('column-filter input[placeholder="udf class name"]', 'keyup');

  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 1, 'User is able to filter by class name');
  });

  click('.clear-filters');

  andThen(function() {
    equal(find('#content .table tbody tr:visible').length, 2);
  });
});

test('User is able to add udf', function() {
  expect(1);

  visit("/udfs");
  click('.add-udf');

  andThen(function() {
    equal(find('#content .table tbody tr').length, 3);
  });
});


test('Can delete file resource', function (assert) {
  assert.expect(1);

  visit('/udfs');
  click('.fa-gear:first');
  click('.dropdown-menu li:first');
  click('.dropdown-toggle:first');
  click('.fa-remove:first');

  andThen(function () {
    click('.modal-footer .btn-success');
    click('tr.ember-view:first .btn-success');
  });

  assert.equal($('tr.ember-view:first td:first').text().trim().length, 0, 'File Resource Deleted');
});
