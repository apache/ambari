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

moduleForComponent('alert-message-widget', 'AlertMessageWidgetComponent', {
  needs: []
});

test('isExpanded is toggled on click', function() {
  expect(2);

  var message = Ember.Object.create({ isExpanded: false});

  var component = this.subject({
    message: message
  });

  Ember.run(function() {
    component.send('toggleMessage');
  });

  equal(component.get('message.isExpanded'), true, 'isExpanded is set to true');

  Ember.run(function() {
    component.send('toggleMessage');
  });

  equal(component.get('message.isExpanded'), false, 'isExpanded is set to false');
});

test('removeLater should be called when the message is toggled', function() {
  expect(1);

  var message = Ember.Object.create({ isExpanded: false});

  var targetObject = {
    removeLater: function() {
      ok(true, 'External removeLater called');
    }
  };

  var component = this.subject({
    targetObject: targetObject,
    removeLater: 'removeLater',
    message: message
  });

  Ember.run(function() {
    component.send('toggleMessage');
  });

  Ember.run(function() {
    component.send('toggleMessage');
  });
});

test('remove action should call external removeMessage', function() {
  expect(1);

  var targetObject = {
    removeMessage: function() {
      ok(true, 'External removeMessage called');
    }
  };

  var component = this.subject({
    targetObject: targetObject,
    removeMessage: 'removeMessage'
  });

  Ember.run(function() {
    component.send('remove', {});
  });
});
