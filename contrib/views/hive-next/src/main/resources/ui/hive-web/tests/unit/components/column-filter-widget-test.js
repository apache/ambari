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

moduleForComponent('column-filter-widget', 'ColumnFilterWidgetComponent', {
  needs: ['component:extended-input']
});

test('if a filterValue is set when the element is inserted, an action is being sent announcing a filter change', function () {
  expect(1);

  var column = Ember.Object.create({
    caption: 'missing.translation'
  });

  var component = this.subject({ column: column });

  Ember.run(function () {
    component.set('filterValue', 'initial filter value');
  });

  var targetObject = {
    externalAction: function(){
      ok(true, 'initial filterValue set. Action has been sent.');
    }
  };

  component.set('columnFiltered', 'externalAction');
  component.set('targetObject', targetObject);

  var $component = this.$();
});

test('isSorted returns true if the table is sorted by this column property', function () {
  expect(1);

  var component = this.subject();

  var column = Ember.Object.create({
    property: 'some prop'
  });

  Ember.run(function () {
    component.set('column', column);
    component.set('sortProperties', [column.property]);
  });

  ok(component.get('isSorted'));
});

test('isSorted returns false if the table is sorted by some other column', function () {
  expect(1);

  var component = this.subject();

  var column = Ember.Object.create({
    property: 'some prop'
  });

  Ember.run(function () {
    component.set('column', column);
    component.set('sortProperties', ['other prop']);
  });

  ok(!component.get('isSorted'));
});

test('isSorted returns false if the table is not sorted by any column', function () {
  expect(1);

  var component = this.subject();

  var column = Ember.Object.create({
    property: 'some prop'
  });

  Ember.run(function () {
    component.set('column', column);
    component.set('sortProperties', []);
  });

  ok(!component.get('isSorted'));
});

test('when sendSort gets called, the columnSorted action gets sent.', function () {
  expect(1);

  var component = this.subject();

  var targetObject = {
    externalAction: function(){
      ok(true, 'columnSorted action has been intercepted.');
    }
  };

  Ember.run(function () {
    component.set('targetObject', targetObject);
    component.set('columnSorted', 'externalAction');

    component.send('sendSort');
  });
});

test('when sendFilter gets called, the columnFiltered action gets sent.', function () {
  expect(1);

  var component = this.subject();

  var targetObject = {
    externalAction: function(){
      ok(true, 'columnFiltered action has been intercepted.');
    }
  };

  Ember.run(function () {
    component.set('targetObject', targetObject);
    component.set('columnFiltered', 'externalAction');

    component.send('sendFilter');
  });
});
