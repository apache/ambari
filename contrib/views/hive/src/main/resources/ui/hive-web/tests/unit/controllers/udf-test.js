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

moduleFor('controller:udf', 'UdfController', {
  needs: [ 'controller:udfs', 'controller:file-resources' ]
});

test('controller is initialized', function() {
  expect(2);

  var model = Ember.Object.create({
    id: 1,
    name: 'udfmodel',
    classname: 'cls',
    isNew: true,
    fileResource: 'fileresource'
  });

  var component = this.subject({ model: model});

  ok(component.get('isEditing'), 'Model is new, isEditing is set to true');
  equal(component.get('fileBackup'), model.get('fileResource'), 'fileBackup is set to model.fileResource');
});

test('addFileResource', function() {
  expect(3);

  var model = Ember.Object.create({
    id: 1,
    name: 'udfmodel',
    classname: 'cls',
    isNew: true,
    fileResource: null
  });

  var store = {
    createRecord: function() {
      return 'newfileresource';
    }
  };

  var component = this.subject({model: model, store: store });

  equal(component.get('model.fileResource'), null, 'fileResource is set to null');

  Ember.run(function () {
    component.send('addFileResource');
  });

  ok(component.get('isEditingResource'), 'isEditingResource is set to true');
  equal(component.get('model.fileResource'), store.createRecord(), 'New file resource is set on the model');
});

test('editFileResource', function() {
  expect(3);

  var model = Ember.Object.create({
    id: 1,
    name: 'udfmodel',
    classname: 'cls',
    isNew: true,
    fileResource: null
  });

  var file = 'fileresource';
  var component = this.subject({model: model });

  equal(component.get('model.fileResource'), null, 'fileResource is set to null');

  Ember.run(function () {
    component.send('editFileResource', file);
  });

  ok(component.get('isEditingResource'), 'isEditingResource is set to true');
  equal(component.get('model.fileResource'), file, 'New file resource is set on the model');
});
