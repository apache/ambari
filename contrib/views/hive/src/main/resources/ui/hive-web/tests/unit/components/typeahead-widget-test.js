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

moduleForComponent('typeahead-widget', 'TypeaheadWidgetComponent', {
  needs: ['component:ember-selectize']
});

test('Component is initialized correctly', function () {
  expect(2);

  var items = [
    {name: 'item 1', id: 1},
    {name: 'item 2', id: 2},
    {name: 'item 3', id: 3},
    {name: 'item 4', id: 4}
  ];

  var component = this.subject({
    content: items,
    optionValuePath: 'content.id',
    optionLabelPath: 'content.name'
  });

  this.$();

  equal(component.get('content.length'), items.length, 'Items are set');
  equal(component.get('selection'), items[0], 'First object is set as default value');
});
