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
import constants from 'hive/utils/constants';

moduleFor('controller:index', 'IndexController', {
  needs: [
          'controller:open-queries',
          'controller:udfs',
          'controller:index/history-query/logs',
          'controller:index/history-query/results',
          'controller:index/history-query/explain',
          'controller:settings',
          'controller:visual-explain',
          'controller:tez-ui',
          'service:job',
          'service:file',
          'service:database',
          'service:notify',
          'service:job-progress',
          'service:session',
          'service:settings',
          'adapter:application',
          'adapter:database'
        ]
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
  expect(4);


  var query = Ember.Object.create({
    id: 1,
    fileContent: "select $what from $where"
  });
  var updatedQuery = "select $what from $where and $where";

  var controller = this.subject({
    model: query
  });

  Ember.run(function() {
    controller.set('openQueries.queryTabs', [query]);
    controller.set('openQueries.currentQuery', query);
  });

  equal(controller.get('queryParams.length'), 2, '2 queryParams parsed');
  equal(controller.get('queryParams').objectAt(0).name, '$what', 'First param parsed correctly');
  equal(controller.get('queryParams').objectAt(1).name, '$where', 'Second param parsed correctly');

  Ember.run(function() {
    controller.set('openQueries.currentQuery.fileContent', updatedQuery);
  });

  equal(controller.get('queryParams.length'), 2, 'Can use same param multiple times');
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

test('Execute EXPLAIN type query', function() {
  expect(1);

  var query = Ember.Object.create({
    id: 1,
    fileContent: "explain select 1" // explain type query
  });

  var controller = this.subject({
    model: query,
    _executeQuery: function (referer) {
      equal(referer, constants.jobReferrer.explain, 'Explain type query successful.');
      return {then: function() {}};
    }
  });

  Ember.run(function() {
      controller.set('openQueries.queryTabs', [query]);
      controller.set('openQueries.currentQuery', query);
      controller.send('executeQuery');
  });

});

test('Execute non EXPLAIN type query', function() {
  expect(1);

  var query = Ember.Object.create({
    id: 1,
    fileContent: "select 1" //non explain type query
  });

  var controller = this.subject({
    model: query,
    _executeQuery: function (referer) {
      equal(referer, constants.jobReferrer.job , 'non Explain type query successful.');
      return {then: function() {}};
    }
  });

  Ember.run(function() {
      controller.set('openQueries.queryTabs', [query]);
      controller.set('openQueries.currentQuery', query);
      controller.send('executeQuery');
  });

});


test('csvUrl returns if the current query is not a job', function() {
  expect(1);
  var content = Ember.Object.create({
      constructor: {
        typeKey: 'notJob'
      }
  });

  var controller = this.subject({ content: content });
  ok(!controller.get('csvUrl'), 'returns if current query is not a job');
});

test('csvUrl returns is status in not SUCCEEDED', function() {
  expect(1);
  var content= Ember.Object.create({
      constructor: {
        typeKey: 'job'
      },
      status: 'notSuccess'
  });

  var controller = this.subject({ content: content });
  ok(!controller.get('csvUrl'), 'returns if current status is not success');
});

test('csvUrl return the download results as csv link', function() {
  expect(1);
  var content = Ember.Object.create({
      constructor: {
        typeKey: 'job'
      },
      status: 'SUCCEEDED',
      id: 1
  });

  var controller = this.subject({ content: content });
  ok(controller.get('csvUrl'));
});

test('donwloadMenu returns null if status is not succes and results are not visible ', function() {
  expect(1);
  var content = Ember.Object.create({
      status: 'notSuccess',
      queryProcessTabs: [{
        path: 'index.historyQuery.results',
        visible: false
      }]
  });

  var controller = this.subject({ content: content });
  ok(!controller.get('downloadMenu'), 'Returns null');
});

test('donwloadMenu returns only saveToHDFS if csvUrl is false', function() {
  expect(1);
  var content = Ember.Object.create({
      constructor: {
        typeKey: 'notjob'
      },
      status: 'SUCCEEDED',
  });

  var controller = this.subject({ content: content });
  Ember.run(function() {
    var tabs = controller.get('queryProcessTabs');
    var results = tabs.findBy('path', 'index.historyQuery.results');
    results.set('visible', true);
  });

  equal(controller.get('downloadMenu.length'), 1, 'Returns only saveToHDFS');
});

test('donwloadMenu returns saveToHDFS and csvUrl', function() {
  expect(1);
  var content = Ember.Object.create({
      constructor: {
        typeKey: 'job'
      },
      status: 'SUCCEEDED',
  });

  var controller = this.subject({ content: content });
  Ember.run(function() {
    var tabs = controller.get('queryProcessTabs');
    var results = tabs.findBy('path', 'index.historyQuery.results');
    results.set('visible', true);
  });

  equal(controller.get('downloadMenu.length'), 2, 'Returns saveToHDFS and csvUrl');
});
