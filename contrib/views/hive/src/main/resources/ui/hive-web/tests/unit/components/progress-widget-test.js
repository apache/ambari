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

moduleForComponent('progress-widget', 'ProgressWidgetComponent');

test('Setting progress attributes', function () {
  expect(3);

  var component = this.subject({
    stages: [
      Ember.Object.create({ className: 'progress-bar-success', name: 'Execution', value: 10 }),
      Ember.Object.create({ className: 'progress-bar-danger', name: 'Queued', value: 30 }),
      Ember.Object.create({ className: 'progress-bar-warning', name: 'Compile', value: 30 })
    ],
    formattedStages: Ember.ArrayProxy.create({ content: [] })
  });

  var $component = this.append();

  Ember.run(function() {
    component.formatStages();

    equal(component.get('stages').get('firstObject').get('className'), 'progress-bar-success', 'ClassName was set correctly');
    equal(component.get('stages').get('firstObject').get('name'), 'Execution', 'Name was set correctly');
    equal(component.get('stages').get('firstObject').get('value'), 10, 'Value was set correctly');
  });
});
