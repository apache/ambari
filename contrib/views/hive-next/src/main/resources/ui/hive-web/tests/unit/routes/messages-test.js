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
import constants from 'hive/utils/constants';
import { moduleFor, test } from 'ember-qunit';

moduleFor('controller:messages', 'MessagesController', {
});

test('Controller is initialized', function() {
  var controller = this.subject();

  ok(controller, 'Controller is initialized');
});

test('Controller action', function() {
  var controller = this.subject({
    notifyService: Ember.Object.create({
      removeMessage: function(message) {
        ok(1, 'removeMessage action called');
      },
      removeAllMessages: function() {
        ok(1, 'removeAllMessages action called');
      },
      markMessagesAsSeen: function(message) {
        ok(1, 'markMessagesAsSeen action called');
      }
    })
  });

  Ember.run(function() {
    controller.send('removeMessage');
    controller.send('removeAllMessages');
    controller.send('markMessagesAsSeen');
  });

});
