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

moduleForComponent('tabs-widget', 'TabsWidgetComponent', {
  needs: []
});

test('First tab active by default', function() {
  expect(2);

  var tabs = Ember.ArrayProxy.create({content: Ember.A([
    Ember.Object.create(),
    Ember.Object.create()
  ])});

  var component = this.subject({ tabs: tabs });
  var $component = this.$();

  ok(component.get('tabs.firstObject.active'), 'First tab is active');
  ok(!component.get('tabs.lastObject.active'), 'Second tab is not active');
});


test('Set active tab on init', function() {
  expect(2);

  var tabs = Ember.ArrayProxy.create({content: Ember.A([
    Ember.Object.create(),
    Ember.Object.create(),
    Ember.Object.create({ active: true })
  ])});

  var component = this.subject({ tabs: tabs });

  ok(!component.get('tabs.firstObject.active'), 'First tab is not active');
  ok(component.get('tabs.lastObject.active'), 'Last tab is active');
});


test('Set active tab', function() {
  expect(3);

  var tabs = Ember.ArrayProxy.create({content: Ember.A([
    Ember.Object.create(),
    Ember.Object.create(),
    Ember.Object.create({ active: true })
  ])});

  var component = this.subject({ tabs: tabs });

  ok(!component.get('tabs.firstObject.active'), 'First tab is not active');
  ok(component.get('tabs.lastObject.active'), 'Last tab is active');

  Ember.run(function() {
    component.send('selectTab', tabs.objectAt(1));
  });

  ok(component.get('tabs').objectAt(1).get('active'), 'Second tab is active');
});

test('removeEnabled tabs', function() {
  expect(2);

  var tabs = Ember.ArrayProxy.create({content: Ember.A([
    Ember.Object.create(),
    Ember.Object.create(),
    Ember.Object.create({ active: true })
  ])});

  var component = this.subject({ tabs: tabs, canRemove: true });

  ok(component.get('removeEnabled'), 'More than one tab removeEnabled returns true');

  Ember.run(function() {
    component.get('tabs').popObject();
    component.get('tabs').popObject();
  });

  ok(!component.get('removeEnabled'), 'Only one tab removeEnabled returns false');
});

test('remove tab', function () {
  expect(1);

  var targetObject = {
    removeTabAction: function() {
      ok(true, 'External remove tab action called');
    }
  };

  var component = this.subject({
    'removeClicked': 'removeTabAction',
    'targetObject': targetObject
  });

  Ember.run(function() {
    component.send('remove', {});
  });
});
