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

moduleForComponent('select-widget', 'SelectWidgetComponent', {
  needs: ['helper:path-binding']
});

test('selectedLabel returns the selectedValue property indicated by labelPath if selectedValue and labelPath are set.', function () {
  expect(1);

  var component = this.subject();

  var selectedValue = Ember.Object.extend({
    label: 'db'
  }).create();

  var labelPath = 'label';

  Ember.run(function () {
    component.set('labelPath', labelPath);
    component.set('selectedValue', selectedValue);
  });

  equal(component.get('selectedLabel'), selectedValue.label, 'selectedValue and labelPath are set. selectedLabel returns selectedValue[labelPath].');
});

test('selectedLabel returns defaultLabel if selectedValue is falsy and defaultLabel is set.', function () {
  expect(1);

  var component = this.subject();

  var defaultLabel = 'select...';

  Ember.run(function () {
    component.set('defaultLabel', defaultLabel);
  });

  equal(component.get('selectedLabel'), defaultLabel, 'selectedValue falsy and defaultLabel set. selectedLabel returns defaultLabel.');
});

test('selectedLabel returns undefined if neither selectedValue nor defaultLabel are set.', function () {
  expect(1);

  var component = this.subject();

  equal(component.get('selectedLabel'), undefined, 'selectedValue and defaultLabel are falsy. selectedLabel returns undefined.');
});

test('selectedLabel is computed when selectedValue changes.', function () {
  expect(2);

  var component = this.subject();

  var selectedValue = Ember.Object.extend({
    label: 'db'
  }).create();

  var labelPath = 'label';

  equal(component.get('selectedLabel'), undefined, 'selectedValue and defaultLabel are falsy. selectedLabel returns undefined.');

  Ember.run(function () {
    component.set('labelPath', labelPath);
    component.set('selectedValue', selectedValue);
  });

  equal(component.get('selectedLabel'), selectedValue.label, 'selectedValue and labelPath are set. selectedLabel returns selectedValue[labelPath].');
});

test('renders an li tag for each item in the items collection.', function () {
  expect(2);

  var component = this.subject();
  var $component = this.$();

  equal($component.find('li').length, 0, 'items collection is not set. No li tags are rendered.');

  Ember.run(function() {
    var items = Ember.ArrayProxy.create({ content: Ember.A([Ember.Object.create(), Ember.Object.create()])});
    component.set('labelPath', 'name');
    component.set('items', items);
  });

  equal($component.find('li').length, 2, 'items collection is set containing one item. One li tag is rendered.');
});

test('if no selected item nor defaultLabel set the selected value with first item', function () {
  expect(1);

  var items = [
    'item1',
    'item2'
  ];

  var component = this.subject({ items: items });
  var $component = this.$();

  equal(component.get('selectedValue'), 'item1', 'selectedValue is set to first item')
});

test('component actions', function() {
  expect(7);

  var targetObject = {
    itemAdded: function() {
      ok(true, 'External action itemAdded called')
    },
    itemEdited: function(item) {
      ok(true, 'External action itemEdited called');
      equal(item, 'editedItem', 'Data is sent with action');
    },
    itemRemoved: function(item) {
      ok(true, 'External action itemRemoved called');
      equal(item, 'removedItem', 'Data is sent with action');
    }
  };
  var component = this.subject({
    items: ['item'],
    itemAdded: 'itemAdded',
    itemEdited: 'itemEdited',
    itemRemoved: 'itemRemoved',
    targetObject: targetObject
  });

  var $component = this.$();

  equal(component.get('selectedValue'), 'item', 'selectedValue is set to first item');

  Ember.run(function() {
    component.send('select', 'newItem');
    component.send('add');
    component.send('edit', 'editedItem');
    component.send('remove', 'removedItem');
  });

  equal(component.get('selectedValue'), 'newItem', 'selectedValue is set to newItem');



});
