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

moduleForComponent('extended-input', 'ExtendedInputComponent', {
  unit: true
});

test('Component has dynamicValue and dynamicContext', function () {
  expect(1);

  var component = this.subject({
    dynamicValue: 'dynamicValue',
    dynamicContext: Ember.Object.create({ 'dynamicValue' : 'test' })
  });

  var $component = this.$();

  equal(component.get('value'), 'test', 'Value is set to dynamicValue value');
});


test('Component has no dynamicValue and dynamicContext', function () {
  expect(1);

  var component = this.subject();
  var $component = this.$();

  ok(!component.get('value'), 'Value is not set as dynamicValue value');
});

test("Component's dynamicValue is set", function () {
  expect(1);

  var component = this.subject({
    dynamicValue: 'dynamicValue',
    dynamicContext: Ember.Object.create({ 'dynamicValue' : 'test' })
  });

  var $component = this.$();

  Ember.run(function() {
    component.sendValueChanged();

    equal(component.get('value'), component.dynamicContext.get('dynamicValue'), "Value is set and dynamicValue is set");
  });
});

test("Component's dynamicValue is not set", function () {
  expect(1);

  var component = this.subject({
    dynamicValue: 'dynamicValue',
    dynamicContext: Ember.Object.create({ })
  });

  var $component = this.$();

  Ember.run(function() {
    component.sendValueChanged();

    equal(component.get('value'), undefined, "Value is not set");
  });
});
