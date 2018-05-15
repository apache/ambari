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

moduleFor('service:settings', 'SettingsService');

test('Init', function(assert) {
  var service = this.subject();
  assert.ok(service);
});

test('Can create a setting object', function(assert) {
  assert.expect(2);

  var service = this.subject();

  var setting = service._createSetting('sName', 'sValue');

  assert.equal(setting.get('key.name'), 'sName', 'Settign has the correct name');
  assert.equal(setting.get('value'), 'sValue', 'Settign has the correct value');

  service.removeAll();
});

test('Can create default settings', function(assert) {
  assert.expect(2);

  var service = this.subject();

  var settings = {
    'sName1': 'sValue1',
    'sName2': 'sValue2',
    'sName3': 'sValue3'
  };

  service._createDefaultSettings();

  assert.equal(service.get('settings.length'), 0, '0 settings created');

  service._createDefaultSettings(settings);

  assert.equal(service.get('settings.length'), 3, '3 settings created');

  service.removeAll();
});

test('Can add a setting', function(assert) {
  assert.expect(2);

  var service = this.subject();
  assert.equal(service.get('settings.length'), 0, 'No settings');
  service.add();
  service.add();
  assert.equal(service.get('settings.length'), 2, '2 settings added');

  service.removeAll();
});

test('Can remove a setting', function(assert) {
  assert.expect(2);

  var service = this.subject();

  service.add();
  service.add();

  assert.equal(service.get('settings.length'), 2, '2 settings added');
  var firstSetting = service.get('settings.firstObject');
  service.remove(firstSetting);
  assert.equal(service.get('settings.length'), 1, 'Setting removed');

  service.removeAll();
});

test('Can create key', function(assert) {
  assert.expect(2);
  var service = this.subject();

  assert.ok(!service.get('predefinedSettings').findBy('name', 'new.key.name'), 'Key doesn\'t exist');

  var setting = service._createSetting();
  setting.set('key', null);
  service.get('settings').pushObject(setting);
  service.createKey('new.key.name');

  assert.ok(service.get('predefinedSettings').findBy('name', 'new.key.name'), 'Key created');

  service.removeAll();
});

test('Can get settings string', function(assert) {
  var service = this.subject();

  var noSettings = service.getSettings();
  assert.equal(noSettings, "", 'An empty string is returned if there are no settings');

  var settings = {
    'sName1': 'sValue1',
    'sName2': 'sValue2'
  };

  service._createDefaultSettings(settings);

  var expectedWithSettings = "set sName1=sValue1;\nset sName2=sValue2;\n--Global Settings--\n\n";
  var withSettings = service.getSettings();

  assert.equal(withSettings, expectedWithSettings, 'Returns correct string');
});

test('It can parse global settings', function(assert) {
  var service = this.subject();

  assert.ok(!service.parseGlobalSettings(), 'It returns if query or model is not passed');

  var settings = {
    'sName1': 'sValue1',
    'sName2': 'sValue2'
  };


  var globalSettingsString = "set sName1=sValue1;\nset sName2=sValue2;\n--Global Settings--\n\n";

  var model = Ember.Object.create({
    globalSettings: globalSettingsString
  });

  var query = Ember.Object.create({
    fileContent: globalSettingsString + "{{match}}"
  });

  assert.ok(!service.parseGlobalSettings(query, model), 'It returns if current settings don\'t match models global settings');

  service._createDefaultSettings(settings);

  service.parseGlobalSettings(query, model);

  assert.equal(query.get('fileContent'), "{{match}}", 'It parsed global settings');
});
