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

moduleFor('controller:insert-udfs', 'InsertUdfsController', {
  needs: 'controller:udfs'
});

test('controller is initialized correctly', function () {
  expect(1);

  var udfs = Ember.A([
    Ember.Object.create({ fileResource: { id: 1 } }),
    Ember.Object.create({ fileResource: { id: 1 } }),
    Ember.Object.create({ fileResource: { id: 2 } }),
    Ember.Object.create({ fileResource: { id: 2 } })
  ]);

  var component = this.subject();

  Ember.run(function() {
    component.set('udfs', udfs);
  });

  equal(component.get('length'), 2, 'should contain unique file resources');
});

test('controller updates on new udfs', function () {
  expect(2);

  var udfs = Ember.A([
    Ember.Object.create({ fileResource: { id: 1 } }),
    Ember.Object.create({ fileResource: { id: 2 } }),
  ]);

  var component = this.subject();

  Ember.run(function() {
    component.set('udfs', udfs);
  });

  equal(component.get('length'), 2, '');

  var newUdf = Ember.Object.create({ isNew: true, fileResource: { id: 3 } });

  Ember.run(function() {
    component.get('udfs').pushObject(newUdf);
  });

  equal(component.get('length'), 3, '');
});
