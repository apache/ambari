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
    'controller:loaded-files',
    'controller:index/history-query/results',
    'controller:index/history-query/explain',
    'controller:columns',
    'controller:udfs',
    'controller:index/history-query/logs'
  ]
});

test('can add a setting', function() {
  var controller = this.subject();

  ok(!controller.get('currentSettings.settings.length'), 'No initial settings');

  Ember.run(function() {
    controller.send('add');
  });

  equal(controller.get('currentSettings.settings.length'), 1, 'Can add settings');
});

test('hasSettings return true if there are settings', function() {
  var controller = this.subject();

  ok(!controller.hasSettings(null), 'No settings => return false');

  Ember.run(function() {
    controller.send('add');
  });

  ok(controller.hasSettings(null), '1 setting => returns true');
});

test('setSettingForQuery', function() {
  var controller = this.subject();

  var settings = [ Ember.Object.create({key: 'key', value: 'value'}) ];

  Ember.run(function() {
    controller.setSettingForQuery(1, settings);
  });

  equal(controller.get('currentSettings.settings.firstObject.key'), settings.get('key'), 'It sets the settings for specified query');
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

  var settings = [
    Ember.Object.create({key: { name: 'some.key' }, value: 'value'}),
    Ember.Object.create({key: { name: 'some.key' }, value: '123'})
  ];

  Ember.run(function() {
    controller.setSettingForQuery(1, settings);
  });

  var currentSettings = controller.get('model.firstObject.settings');
  console.log(currentSettings);
  ok(!currentSettings.get('firstObject.valid'), "First setting doesn\' pass validataion");
  ok(currentSettings.get('lastObject.valid'), 'Second setting passes validation');
});
