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
import constants from 'hive/utils/constants';
import { moduleFor, test } from 'ember-qunit';

moduleFor('controller:job', 'JobController', {
  needs: ['controller:history', 'controller:loaded-files']
});

test('Statuses are computed correctly', function () {
  expect(5);

  var component = this.subject();

  Ember.run(function() {
    component.set('content', Ember.Object.create());
    component.set('content.status', constants.statuses.running);
  });

  ok(component.get('canStop'), 'Status is running canStop returns true');

  Ember.run(function() {
    component.set('content.status', constants.statuses.initialized);
  });

  ok(component.get('canStop'), 'Status is initialized canStop returns true');

  Ember.run(function() {
    component.set('content.status', constants.statuses.pending);
  });

  ok(component.get('canStop'), 'Status is pending canStop returns true');

  Ember.run(function() {
    component.set('content.status', constants.statuses.canceled);
  });

  ok(!component.get('canStop'), 'Status is canceled canStop returns false');

  Ember.run(function() {
    component.set('content.status', constants.statuses.unknown);
  });

  ok(!component.get('canStop'), 'Status is unknown canStop returns false');
});
