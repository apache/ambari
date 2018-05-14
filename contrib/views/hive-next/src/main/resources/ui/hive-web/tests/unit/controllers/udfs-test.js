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

moduleFor('controller:udfs', 'UdfsController', {
  needs: [
    'model:file-resource'
  ]
});

test('controller is initialized', function() {
  expect(3);

  var component = this.subject();

  equal(component.get('columns.length'), 2, 'Columns are initialized correctly');
  ok(component.get('sortAscending'), 'Sort ascending is true');
  equal(component.get('sortProperties.length'), 0, 'sortProperties is empty');
});

test('sort', function() {
 expect(2);

  var component = this.subject();

  Ember.run(function () {
    component.send('sort', 'prop');
  });

  ok(component.get('sortAscending'), 'New sort prop sortAscending is set to true');
  equal(component.get('sortProperties').objectAt(0), "prop", 'sortProperties is set to prop');
});

test('add', function() {
  expect(1);

  var store = {
    createRecord: function(name) {
      ok(name, 'store.createRecord called');
    }
  };
  var component = this.subject({ store: store });

  Ember.run(function () {
    component.send('add');
  });
});

test('handleAddFileResource', function (assert) {
  assert.expect(2);

  var udf = Ember.Object.create({
    isEditingResource: false,
    fileResource: null
  });

  var controller = this.subject();

  Ember.run(function () {
    controller.send('handleAddFileResource', udf);
  });

  assert.ok(udf.get('fileResource'), 'File Resource created');
  assert.ok(udf.get('isEditingResource'), 'Editing mode in enabled');
});
