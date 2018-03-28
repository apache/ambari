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

var App = require('app');

/**
 * Example:
 *
 * stompClient.connect();
 * stompClient.subscribe('topic1', handlerFunc1);
 * stompClient.addHandler('topic1', 'handler2-name', handlerFunc2);
 * stompClient.removeHandler('topic1', 'handler2-name');
 * stompClient.unsubscribe('topic1');
 * stompClient.disconnect();
 *
 */

module.exports = Em.Object.extend({
  /**
   * @type {Stomp}
   */
  client: null,

  /**
   * @type {string}
   */
  webSocketUrl: '{protocol}://{hostname}:8080/api/stomp/v1/websocket',

  /**
   * @type {string}
   */
  sockJsUrl: '{protocol}://{hostname}:8080/api/stomp/v1',

  /**
   * sockJs should use only alternative options as transport in case when websocket supported but connection fails
   * @const
   * @type {Array}
   */
  sockJsTransports: ['eventsource', 'xhr-polling', 'iframe-xhr-polling', 'jsonp-polling'],

  /**
   * @type {boolean}
   */
  isConnected: false,

  /**
   * @type {boolean}
   */
  isWebSocketSupported: true,

  /**
   * @type {number}
   * @const
   */
  RECONNECT_TIMEOUT: 6000,

  /**
   * @type {object}
   */
  subscriptions: {},

  /**
   * default headers
   * @type {object}
   */
  headers: {},

  /**
   *
   * @param {boolean} useSockJS
   * @returns {$.Deferred}
   */
  connect: function(useSockJS) {
    const dfd = $.Deferred();
    const socket = this.getSocket(useSockJS);
    const client = Stomp.over(socket);
    const headers = this.get('headers');

    client.connect(headers, () => {
      this.onConnectionSuccess();
      dfd.resolve();
    }, () => {
      this.onConnectionError(useSockJS);
      dfd.reject();
    });
    client.debug = Em.K;
    this.set('client', client);
    return dfd.promise();
  },

  /**
   *
   * @param {boolean} useSockJS
   * @returns {*}
   */
  getSocket: function(useSockJS) {
    const hostname = window.location.hostname;
    const isSecure = window.location.protocol === 'https:';

    if (!WebSocket || useSockJS) {
      this.set('isWebSocketSupported', false);
      const protocol = isSecure ? 'https' : 'http';
      const sockJsUrl = this.get('sockJsUrl').replace('{hostname}', hostname).replace('{protocol}', protocol);
      return new SockJS(sockJsUrl, null, {transports: this.get('sockJsTransports')});
    } else {
      const protocol = isSecure ? 'wss' : 'ws';
      return new WebSocket(this.get('webSocketUrl').replace('{hostname}', hostname).replace('{protocol}', protocol));
    }
  },

  onConnectionSuccess: function() {
    this.set('isConnected', true);
  },

  onConnectionError: function(useSockJS) {
    if (this.get('isConnected')) {
      this.reconnect(useSockJS);
    } else if (!useSockJS) {//if SockJs connection failed too the stop trying to connect
      //if webSocket failed on initial connect then switch to SockJS
      this.connect(true);
    }
  },

  reconnect: function(useSockJS) {
    const subscriptions = this.get('subscriptions');
    setTimeout(() => {
      console.debug('Reconnecting to WebSocket...');
      this.connect(useSockJS).done(() => {
        for (var i in subscriptions) {
          subscriptions[i].unsubscribe();
          this.subscribe(subscriptions[i].destination, subscriptions[i].handlers['default']);
          for (var key in subscriptions[i].handlers) {
            key !== 'default' && this.addHandler(subscriptions[i].destination, key, subscriptions[i].handlers[key]);
          }
        }
      });
    }, this.RECONNECT_TIMEOUT);
  },

  disconnect: function () {
    this.get('client').disconnect();
  },

  /**
   *
   * @param {string} destination
   * @param {string} body
   * @param {object} headers
   */
  send: function(destination, body, headers = {}) {
    if (this.get('client.connected')) {
      this.get('client').send(destination, headers, body);
      return true;
    }
    return false;
  },

  /**
   *
   * @param destination
   * @param {function} handler
   * @returns {*}
   */
  subscribe: function(destination, handler = Em.K) {
    const handlers = {
      default: handler
    };
    if (!this.get('client.connected')) {
      return null;
    }
    if (this.get('subscriptions')[destination]) {
      console.error(`Subscription with default handler for ${destination} already exists`);
      return this.get('subscriptions')[destination];
    } else {
      const subscription = this.get('client').subscribe(destination, (message) => {
        for (const i in handlers) {
          handlers[i](JSON.parse(message.body));
        }
      });
      subscription.destination = destination;
      subscription.handlers = handlers;
      this.get('subscriptions')[destination] = subscription;
      return subscription;
    }
  },

  /**
   * If trying to add handler to not existing subscription then it will be created and handler added as default
   * @param {string} destination
   * @param {string} key
   * @param {function} handler
   */
  addHandler: function(destination, key, handler) {
    const subscription = this.get('subscriptions')[destination];
    if (!subscription) {
      this.subscribe(destination);
      return this.addHandler(destination, key, handler);
    }
    if (subscription.handlers[key]) {
      console.error('You can\'t override subscription handler');
      return;
    }
    subscription.handlers[key] = handler;
  },

  /**
   * If removed handler is last and subscription have zero handlers then topic will be unsubscribed
   * @param {string} destination
   * @param {string} key
   */
  removeHandler: function(destination, key) {
    const subscription = this.get('subscriptions')[destination];
    delete subscription.handlers[key];
    if (Em.keys(subscription.handlers).length === 0) {
      this.unsubscribe(destination);
    }
  },

  /**
   *
   * @param {string} destination
   * @returns {boolean}
   */
  unsubscribe: function(destination) {
    if (this.get('subscriptions')[destination]) {
      this.get('subscriptions')[destination].unsubscribe();
      delete this.get('subscriptions')[destination];
      return true;
    }
    return false;
  }
});
