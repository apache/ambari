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
import { moduleForComponent, test } from 'ember-qunit';

moduleForComponent('udf-tr-view', 'UdfTrViewComponent', {
  unit: true,
  needs: ['component:select-widget']
});

test('Can send actions', function (assert) {
  assert.expect(6);

  var targetObject = {
    onDeleteUdf: function () {
      assert.ok(true, 'onDeleteUdf called');
    },
    onAddFileResource: function () {
      assert.ok(true, 'onAddFileResource called');
    },
    onDeleteFileResource: function () {
      assert.ok(true, 'onDeleteFileResource called');
    },
    onSaveUdf: function () {
      assert.ok(true, 'onSaveUdf called');
    },
  };

  var component = this.subject({
    onDeleteUdf: 'onDeleteUdf',
    onAddFileResource: 'onAddFileResource',
    onDeleteFileResource: 'onDeleteFileResource',
    onSaveUdf: 'onSaveUdf',

    targetObject: targetObject,

    udf: Ember.Object.create()
  });

  Ember.run(function () {
    component.send('deleteUdf');
    component.send('addFileResource');
    component.send('deleteFileResource');
    component.send('save');

    component.send('editUdf');
    component.send('editFileResource', {});
  });

  assert.ok(component.get('udf.isEditing'), 'Can edit udf');
  assert.ok(component.get('udf.isEditingResource'), 'Can edit resource');
});

test('It sets isEditing to true if udf.isNew', function (assert) {
  assert.expect(1);

  var component = this.subject({
    udf: Ember.Object.create({
      isNew: true,
      isEditing: false
    })
  });

  var $component = this.render();
  assert.ok(component.get('udf.isEditing'), 'isEditing set to true');
});

test('Cancel edit whould rollback changes', function (assert) {
  assert.expect(5);

  var backup = 'fileResource backup';
  var file = Ember.Object.create({
    rollback: function () {
      assert.ok(true, 'file.rollback() called');
    }
  });

  var udf = Ember.Object.create({
    isEditing: true,
    isEditingResource: true,
    get: function () {
      var defer = new Ember.RSVP.defer;
      defer.resolve(file);

      return defer.promise;
    },
    rollback: function () {
      assert.ok(true, 'udf.rollback() called');
    }
  });

  var component = this.subject({
    file: file,
    udf: udf,
    fileBackup: backup
  });

  Ember.run(function () {
    component.send('cancel');
  });

  assert.ok(!component.get('udf.isEditing'), 'isEditing set to false');
  assert.ok(!component.get('udf.isEditingResource'), 'isEditingResource set to false');

  assert.equal(component.get('udf.fileResource'), backup, 'backup is set as file resource');
});
