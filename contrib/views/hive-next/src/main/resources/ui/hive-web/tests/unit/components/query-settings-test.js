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

moduleFor('controller:settings', 'SettingsController', {
  needs: [
    'controller:databases',
    'controller:index',
    'controller:open-queries',
    'controller:index/history-query/results',
    'controller:index/history-query/explain',
    'controller:udfs',
    'controller:index/history-query/logs',
    'controller:visual-explain',
    'controller:tez-ui',
    'adapter:database',
    'adapter:application',
    'service:settings',
    'service:notify',
    'service:database',
    'service:file',
    'service:session',
    'service:job',
    'service:job-progress'
  ]
});

test('can add a setting', function() {
  var controller = this.subject();

  ok(!controller.get('settings.length'), 'No initial settings');

  Ember.run(function() {
    controller.send('add');
  });

  equal(controller.get('settings.length'), 1, 'Can add settings');
});

test('validate', function() {
  var predefinedSettings = [
    {
      name: 'some.key',
      validate: new RegExp(/^\d+$/) // digits
    }
  ];

  var controller = this.subject({
    predefinedSettings: predefinedSettings
  });

  controller.set('openQueries.update', function () {
    var defer = Ember.RSVP.defer();
    defer.resolve();

    return defer.promise;
  });

  var settings = [
    Ember.Object.create({key: { name: 'some.key' }, value: 'value'}),
    Ember.Object.create({key: { name: 'some.key' }, value: '123'})
  ];

  Ember.run(function() {
    controller.set('settings', settings);
  });

  var currentSettings = controller.get('settings');
  ok(!currentSettings.get('firstObject.valid'), "First setting doesn\' pass validataion");
  ok(currentSettings.get('lastObject.valid'), 'Second setting passes validation');
});

test('Actions', function(assert) {
  assert.expect(5);

  var settingsService = Ember.Object.create({
    add: function() {
      assert.ok(true, 'add called');
    },
    remove: function(setting) {
      assert.ok(setting, 'Setting param is sent');
    },
    createKey: function(name) {
      assert.ok(name, 'Name param is sent');
    },
    removeAll: function() {
      assert.ok(true, 'removeAll called');
    },
    saveDefaultSettings: function() {
      assert.ok(true, 'saveDefaultSettings called');
    }
  });

  var controller = this.subject();
  controller.set('settingsService', settingsService);

  Ember.run(function() {
    controller.send('add');
    controller.send('remove', {});
    controller.send('addKey', {});
    controller.send('removeAll');
    controller.send('saveDefaultSettings');
  });
});


test('Excluded settings', function(assert) {
  var controller = this.subject();

  console.log(controller.get('predefinedSettings'));
  assert.equal(controller.get('excluded').length, 0, 'Initially there are no excluded settings');

  Ember.run(function() {
    controller.get('settings').pushObject(Ember.Object.create({ key: { name: 'hive.tez.container.size' }}));
    controller.get('settings').pushObject(Ember.Object.create({ key: { name: 'hive.prewarm.enabled' }}));
  });

  assert.equal(controller.get('excluded').length, 2, 'Two settings are excluded');
});
