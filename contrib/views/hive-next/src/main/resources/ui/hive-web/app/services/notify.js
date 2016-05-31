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

export default Ember.Service.extend({
  types: constants.notify,

  messages       : Ember.ArrayProxy.create({ content : [] }),
  notifications  : Ember.ArrayProxy.create({ content : [] }),
  unseenMessages : Ember.ArrayProxy.create({ content : [] }),

  add: function (type, message, body) {
    var formattedBody = this.formatMessageBody(body);

    var notification = Ember.Object.create({
      type    : type,
      message : message,
      body    : formattedBody
    });

    this.messages.pushObject(notification);
    this.notifications.pushObject(notification);
    this.unseenMessages.pushObject(notification);
  },

  info: function (message, body) {
    this.add(this.types.INFO, message, body);
  },

  warn: function (message, body) {
    this.add(this.types.WARN, message, body);
  },

  pushError: function (message, body) {
    this.add(this.types.ERROR, message, body);
  },

  error: function (error) {
    var message,
        body;

    if (error.responseJSON) {
      message = error.responseJSON.message;
      body = error.responseJSON.trace;
    } else if (error.errorThrown) {
      message = error.errorThrown;
    } else if (error.message) {
      message = error.message;
    } else {
      message = error;
    }

    this.add(this.types.ERROR, message, body);
  },

  success: function (message, body) {
    this.add(this.types.SUCCESS, message, body);
  },

  formatMessageBody: function (body) {
    if (!body) {
      return;
    }

    if (typeof body === "string") {
      return body;
    }

    if (typeof body === "object") {
      var formattedBody = "";
      for (var key in body) {
        formattedBody += "\n\n%@:\n%@".fmt(key, body[key]);
      }

      return formattedBody;
    }
  },

  removeMessage: function (message) {
    this.messages.removeObject(message);
    this.notifications.removeObject(message);
  },

  removeNotification: function (notification) {
    this.notifications.removeObject(notification);
  },

  removeAllMessages: function () {
    this.messages.clear();
  },

  markMessagesAsSeen: function () {
    if (this.unseenMessages.get('length')) {
      this.unseenMessages.removeAt(0, this.unseenMessages.get('length'));
    }
  }
});
