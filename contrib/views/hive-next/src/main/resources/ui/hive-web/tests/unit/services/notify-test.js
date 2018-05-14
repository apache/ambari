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

moduleFor('service:notify', 'NotifyService');

test('Service initialized correctly', function () {
  expect(3);

  var service = this.subject();
  service.removeAllMessages();
  service.markMessagesAsSeen();

  equal(service.get('messages.length'), 0, 'No messages');
  equal(service.get('notifications.length'), 0, 'No notifications');
  equal(service.get('unseenMessages.length'), 0, 'No unseenMessages');
});

test('Can add notification', function() {
  expect(3);
  var service = this.subject();

  service.add('notif', 'message', 'body');

  equal(service.get('messages.length'), 1, 'one message added');
  equal(service.get('notifications.length'), 1, 'one notifications added');
  equal(service.get('unseenMessages.length'), 1, 'one unseenMessages added');
});

test('Can add info notification', function() {
  expect(1);
  var service = this.subject();

  service.info('message', 'body');
  equal(service.get('messages.lastObject.type.typeClass'), 'alert-info', 'Info notification added');
});

test('Can add warn notification', function() {
  expect(1);
  var service = this.subject();

  service.warn('message', 'body');
  equal(service.get('messages.lastObject.type.typeClass'), 'alert-warning', 'Warn notification added');
});

test('Can add error notification', function() {
  expect(1);
  var service = this.subject();

  service.error('message', 'body');
  equal(service.get('messages.lastObject.type.typeClass'), 'alert-danger', 'Error notification added');
});

test('Can add success notification', function() {
  expect(1);
  var service = this.subject();

  service.success('message', 'body');
  equal(service.get('messages.lastObject.type.typeClass'), 'alert-success', 'Success notification added');
});

test('Can format message body', function() {
  expect(3);

  var objectBody = {
    k1: 'v1',
    k2: 'v2'
  };
  var formatted = "\n\nk1:\nv1\n\nk2:\nv2";
  var service = this.subject();

  ok(!service.formatMessageBody(), 'Return nothing if no body is passed');
  equal(service.formatMessageBody('some string'), 'some string', 'Return the body if it is a string');
  equal(service.formatMessageBody(objectBody), formatted, 'Parse the keys and return a string if it is an object');
});

test('Can removeMessage', function() {
  expect(4);

  var service = this.subject();
  var messagesCount = service.get('messages.length');
  var notificationCount = service.get('notifications.length');

  service.add('type', 'message', 'body');

  equal(service.get('messages.length'), messagesCount + 1, 'Message added');
  equal(service.get('notifications.length'), notificationCount + 1, 'Notification added');

  var message = service.get('messages.lastObject');
  service.removeMessage(message);

  equal(service.get('messages.length'), messagesCount, 'Message removed');
  equal(service.get('notifications.length'), notificationCount, 'Notification removed');
});

test('Can removeNotification', function() {
  expect(2);

  var service = this.subject();
  var notificationCount = service.get('notifications.length');

  service.add('type', 'message', 'body');

  equal(service.get('notifications.length'), notificationCount + 1, 'Notification added');

  var notification = service.get('notifications.lastObject');
  service.removeNotification(notification);

  equal(service.get('notifications.length'), notificationCount, 'Notification removed');
});

test('Can removeAllMessages', function() {
  expect(2);

  var service = this.subject();

  service.add('type', 'message', 'body');
  service.add('type', 'message', 'body');
  service.add('type', 'message', 'body');

  ok(service.get('messages.length'), 'Messages are present');
  service.removeAllMessages();
  equal(service.get('messages.length'), 0, 'No messages found');
});

test('Can markMessagesAsSeen', function() {
  expect(2);

  var service = this.subject();

  service.add('type', 'message', 'body');
  service.add('type', 'message', 'body');
  service.add('type', 'message', 'body');

  ok(service.get('unseenMessages.length'), 'There are unseen messages');
  service.markMessagesAsSeen();
  equal(service.get('unseenMessages.length'), 0, 'No unseen messages');
});
