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
import { moduleForComponent, test } from 'ember-qunit';

moduleForComponent('job-tr-view', 'JobTrViewComponent', {
  unit: true
});

test('Statuses are computed correctly', function (assert) {
  assert.expect(5);

  var component = this.subject();

  Ember.run(function() {
    component.set('job', Ember.Object.create());
    component.set('job.status', constants.statuses.running);
  });

  assert.equal(component.get('canStop'), true, 'Status is running canStop returns true');

  Ember.run(function() {
    component.set('job.status', constants.statuses.initialized);
  });

  assert.equal(component.get('canStop'), true, 'Status is initialized canStop returns true');

  Ember.run(function() {
    component.set('job.status', constants.statuses.pending);
  });

  assert.equal(component.get('canStop'), true, 'Status is pending canStop returns true');

  Ember.run(function() {
    component.set('job.status', constants.statuses.canceled);
  });

  assert.equal(component.get('canStop'), false, 'Status is canceled canStop returns false');

  Ember.run(function() {
    component.set('job.status', constants.statuses.unknown);
  });

  assert.equal(component.get('canStop'), false, 'Status is unknown canStop returns false');
});
