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

moduleFor('controller:open-queries', 'OpenQueriesController', {
  needs: [ 'controller:index/history-query/results',
           'controller:index/history-query/explain',
           'controller:index',
           'controller:settings',
           'service:file',
           'service:database'
         ]
});

test('when initialized, controller sets the queryTabs.', function () {
  expect(1);

  var controller = this.subject();

  ok(controller.get('queryTabs', 'queryTabs is initialized.'));
});

test('pushObject override creates a new queryFile mock and adds it to the collection if none provided.', function () {
  expect(3);

  var controller = this.subject();

  var model = Ember.Object.create({
    id: 5
  });

  controller.pushObject(null, model);

  equal(controller.get('length'), 1, 'a new object was added to the open queries collection.');
  equal(controller.objectAt(0).id, model.get('id'), 'the object id was set to the model id.');
  equal(controller.objectAt(0).get('fileContent'), '', 'the object fileContent is initialized with empty string.');
});

test('getTabForModel retrieves the tab that has the id and the type equal to the ones of the given model.', function () {
  expect(1);

  var controller = this.subject();

  var model = Ember.Object.create({
    id: 1
  });

  controller.get('queryTabs').pushObject(Ember.Object.create({
    id: model.get('id')
  }));

  equal(controller.getTabForModel(model), controller.get('queryTabs').objectAt(0), 'retrieves correct tab for the given model.');
});

test('getQueryForModel retrieves the query by id equality if a new record is given', function () {
  expect(1);

  var controller = this.subject();

  var model = Ember.Object.create({
    id: 1,
    isNew: true
  });

  controller.pushObject(null, model);

  equal(controller.getQueryForModel(model).get('id'), model.get('id'), 'a new record was given, the method retrieves the query by id equality');
});

test('getQueryForModel retrieves the query by record id equality with model queryFile path if a saved record is given', function () {
  expect(1);

  var controller = this.subject();

  var model = Ember.Object.create({
    id: 1,
    queryFile: 'some/path'
  });

  controller.pushObject(Ember.Object.create({
    id: model.get('queryFile')
  }));

  equal(controller.getQueryForModel(model).get('id'), model.get('queryFile'), 'a saved record was given, the method retrieves the query by id equality with record queryFile path.');
});
