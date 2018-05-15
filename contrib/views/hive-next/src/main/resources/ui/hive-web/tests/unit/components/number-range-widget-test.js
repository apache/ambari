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

/* global moment */

import Ember from 'ember';
import { moduleForComponent, test } from 'ember-qunit';

moduleForComponent('number-range-widget', 'NumberRangeWidgetComponent', {
  needs: ['component:extended-input']
});


test('Component is initialized correctly', function() {
  expect(2);

  var numberRange = Ember.Object.create({
    max: 1,
    min: 0
  });

  var component = this.subject({ numberRange: numberRange });
  var $component = this.$();

  equal(component.get('numberRange.from'), numberRange.get('min'), 'from is set to min');
  equal(component.get('numberRange.to'), numberRange.get('max'), 'to is set to max');

});

test('external change action is called', function() {
  expect(1);

  var targetObject = {
    rangeChanged: function() {
      ok(true, 'rangeChanged external action called');
    }
  };

  var numberRange = Ember.Object.create({
    max: 1,
    min: 0
  });

  var component = this.subject({
    numberRange: numberRange,
    targetObject: targetObject,
    rangeChanged: 'rangeChanged'
  });

  var $component = this.$();

  Ember.run(function() {
    $component.find('.slider').slider('value', 1);
  });
});
