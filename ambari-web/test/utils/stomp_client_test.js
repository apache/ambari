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
var stompClientClass = require('utils/stomp_client');

describe('App.StompClient', function () {
  var stomp;

  beforeEach(function() {
    stomp = stompClientClass.create();
  });

  describe('#connect', function() {
    beforeEach(function() {
      sinon.stub(stomp, 'onConnectionSuccess');
      sinon.stub(stomp, 'onConnectionError');
      sinon.stub(stomp, 'getSocket');
      this.mockStomp = sinon.stub(Stomp, 'over');
    });
    afterEach(function() {
      stomp.onConnectionSuccess.restore();
      stomp.onConnectionError.restore();
      stomp.getSocket.restore();
      this.mockStomp.restore();
    });

    it('onConnectionSuccess should be called', function() {
      this.mockStomp.returns({connect: function(headers, success, error) {
        success();
      }});
      stomp.connect();
      expect(stomp.onConnectionSuccess.calledOnce).to.be.true;
    });
    it('onConnectionError should be called', function() {
      this.mockStomp.returns({connect: function(headers, success, error) {
        error();
      }});
      stomp.connect();
      expect(stomp.onConnectionError.calledOnce).to.be.true;
    });
    it('should set client', function() {
      this.mockStomp.returns({connect: Em.K});
      stomp.connect();
      expect(stomp.get('client')).to.be.eql({
        connect: Em.K,
        debug: Em.K
      });
    });
  });

  describe('#getSocket', function() {
    it('should return WebSocket instance', function() {
      expect(stomp.getSocket().URL).to.be.equal('ws://localhost:11080/stomp');
    });
    it('should return SockJS instance', function() {
      expect(stomp.getSocket(true).url).to.be.equal('http://localhost:11080/stomp');
    });
  });

  describe('#onConnectionSuccess', function() {
    it('isConnected should be true', function() {
      stomp.onConnectionSuccess();
      expect(stomp.get('isConnected')).to.be.true;
    });
  });

  describe('#onConnectionError', function() {
    beforeEach(function() {
      sinon.stub(stomp, 'reconnect');
    });
    afterEach(function() {
      stomp.reconnect.restore();
    });

    it('reconnect should be called when isConnected true', function() {
      stomp.set('isConnected', true);
      stomp.onConnectionError();
      expect(stomp.reconnect.calledOnce).to.be.true;
    });
  });

  describe('#reconnect', function() {
    beforeEach(function() {
      sinon.stub(stomp, 'connect').returns({done: Em.clb});
      sinon.stub(stomp, 'unsubscribe');
      sinon.stub(stomp, 'subscribe');
      this.clock = sinon.useFakeTimers();
    });
    afterEach(function() {
      stomp.connect.restore();
      stomp.unsubscribe.restore();
      stomp.subscribe.restore();
      this.clock.restore();
    });

    it('should connect and restore subscriptions', function() {
      var subscriptions = {
        'foo': {
          destination: 'foo',
          callback: Em.K,
          unsubscribe: sinon.spy()
        }
      };
      stomp.set('subscriptions', subscriptions);
      stomp.reconnect();
      this.clock.tick(stomp.RECONNECT_TIMEOUT);
      expect(subscriptions['foo'].unsubscribe.calledOnce).to.be.true;
      expect(stomp.subscribe.calledWith('foo', Em.K)).to.be.true;
    });
  });

  describe('#disconnect', function() {
    var client = {
      disconnect: sinon.spy()
    };

    it('disconnect should be called', function() {
      stomp.set('client', client);
      stomp.disconnect();
      expect(client.disconnect.calledOnce).to.be.true;
    });
  });

  describe('#send', function() {
    it('send should not be called', function() {
      var client = {connected: false, send: sinon.spy()};
      stomp.set('client', client);
      expect(stomp.send()).to.be.false;
      expect(client.send.called).to.be.false;
    });
    it('send should be called', function() {
      var client = {connected: true, send: sinon.spy()};
      stomp.set('client', client);
      expect(stomp.send('test', 'body', {})).to.be.true;
      expect(client.send.calledWith('test', {}, 'body')).to.be.true;
    });
  });

  describe('#subscribe', function() {
    it('should not subscribe when client disconnected', function() {
      var client = {connected: false};
      stomp.set('client', client);
      expect(stomp.subscribe('foo')).to.be.null;
      expect(stomp.get('subscriptions')).to.be.empty;
    });
    it('should subscribe when client connected', function() {
      var client = {
        connected: true,
        subscribe: sinon.stub().returns({id: 1})
      };
      stomp.set('client', client);
      expect(stomp.subscribe('foo')).to.be.eql({
        callback: Em.K,
        destination: 'foo',
        id: 1
      });
      expect(stomp.get('subscriptions')['foo']).to.be.eql({
        callback: Em.K,
        destination: 'foo',
        id: 1
      });
    });
  });

  describe('#unsubscribe', function() {
    it('should not unsubscribe when no subscription found', function() {
      stomp.set('subscriptions', {});
      expect(stomp.unsubscribe('foo')).to.be.false;
    });
    it('should unsubscribe when subscription found', function() {
      var subscriptions = {
        'foo': {
          unsubscribe: sinon.spy()
        }
      };
      stomp.set('subscriptions', subscriptions);
      expect(stomp.unsubscribe('foo')).to.be.true;
      expect(stomp.get('subscriptions')).to.be.empty;
    });
  });
});
