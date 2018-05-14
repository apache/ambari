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
import DS from 'ember-data';
import { moduleFor, test } from 'ember-qunit';

var container;

moduleFor('controller:tez-ui', 'TezUIController', {
  needs: [
    'controller:index',
    'service:job',
    'service:file',
    'controller:open-queries',
    'controller:databases',
    'controller:udfs',
    'controller:index/history-query/logs',
    'controller:index/history-query/results',
    'controller:index/history-query/explain',
    'controller:settings',
    'controller:visual-explain',
    'adapter:database',
    'service:database',
    'service:notify',
    'service:job-progress',
    'service:session',
    'service:settings'
  ],

  setup: function() {
    container = new Ember.Container();
    container.register('store:main', Ember.Object.extend({
      find: Ember.K
    }));
  }
});

test('controller is initialized properly.', function () {
  expect(1);

  var controller = this.subject();

  ok(controller);
});

test('dagId returns false if there is  no tez view available', function() {
  var controller = this.subject();

  ok(!controller.get('dagId'), 'dagId is false without a tez view available');
});

// test('dagId returns the id if there is view available', function() {
//   var controller = this.subject({
//   });

//   Ember.run(function() {
//     controller.set('index.model', Ember.Object.create({
//       id: 2,
//       dagId: 3
//     }));

//     controller.set('isTezViewAvailable', true);
//   });

//   equal(controller.get('dagId'), 3, 'dagId is truthy');
// });

test('dagURL returns false if no dag id is available', function() {
  var controller = this.subject();

  ok(!controller.get('dagURL'), 'dagURL is false');
});

test('dagURL returns the url if dag id is available', function() {
  var controller = this.subject({
    tezViewURL: '1',
    tezDagPath: '2',
    dagId: '3'
  });

  equal(controller.get('dagURL'), '123');
});
