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

module('Integration: Query Editor', {
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

test('Query Editor is visible', function() {
  expect(1);

  visit("/");

  andThen(function() {
    equal(find('.query-editor-panel').length, 1, 'Query Editor is visible');
  });
});

test('Can execute query', function() {
  expect(1);

  visit("/");

  Ember.run(function() {
    find('.CodeMirror').get(0).CodeMirror.setValue('select count(*) from table1');
  });

  click('.execute-query');

  andThen(function() {
    ok(find('.query-process-results-panel .nav-tabs li:nth-child(2)').hasClass('active'), 'Results tab is visible');
  });
});


test('Can save query', function() {
  expect(2);

  visit("/");

  andThen(function() {
    equal(find('.modal-dialog').length, 0, 'Modal dialog is hidden');
  });

  Ember.run(function() {
    find('.CodeMirror').get(0).CodeMirror.setValue('select count(*) from table1');
  });
  click('.save-query-as');

  andThen(function() {
    equal(find('.modal-dialog').length, 1, 'Modal dialog is shown');
  });

  click('.modal-dialog .modal-footer .btn-danger');
});

test('Can execute selection', function() {
  expect(2);
  visit('/');

  var query1 = "select color from z;",
      query2 = "select fruit from z;",
      query = query1 + "\n" + query2,
      editor;

  Ember.run(function() {
    editor = find('.CodeMirror').get(0).CodeMirror;
    editor.setValue(query);
    editor.setSelection({ line: 1, ch: 0 }, { line: 1, ch: 20 });
  });
  click('.execute-query');

  andThen(function() {
    equal(editor.getValue(), query, 'Editor value didn\'t change');
    equal(editor.getSelection(), query2, 'Query 2 is selected');
  });
});
