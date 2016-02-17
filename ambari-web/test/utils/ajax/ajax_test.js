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
require('utils/ajax/ajax');

describe('App.ajax', function() {

  beforeEach(function() {
    App.set('apiPrefix', '/api/v1');
    App.set('clusterName', 'tdk');
  });

  describe('#send', function() {
    beforeEach(function() {
      sinon.spy($, 'ajax');
    });

    afterEach(function() {
      $.ajax.restore();
    });
    it('Without sender', function() {
      expect(App.ajax.send({})).to.equal(null);
      expect($.ajax.called).to.equal(false);
    });

    it('Invalid config.name', function() {
      expect(App.ajax.send({name:'fake_name', sender: this})).to.equal(null);
      expect($.ajax.called).to.equal(false);
    });

    it('With proper data', function() {
      App.ajax.send({name: 'router.logoff', sender: this});
      expect($.ajax.calledOnce).to.equal(true);
    });

  });

  describe('#formatUrl', function() {

    var tests = [
      {
        url: null,
        data: {},
        e: null,
        m: 'url is null'
      },
      {
        url: 'site/{param}',
        data: null,
        e: 'site/',
        m: 'url with one param, but data is null'
      },
      {
        url: 'clean_url',
        data: {},
        e: 'clean_url',
        m: 'url without placeholders'
      },
      {
        url: 'site/{param}',
        data: {},
        e: 'site/',
        m: 'url with param, but there is no such param in the data'
      },
      {
        url: 'site/{param}/{param}',
        data: {param: 123},
        e: 'site/123/123',
        m: 'url with param which appears two times'
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        var r = App.ajax.fakeFormatUrl(test.url, test.data);
        expect(r).to.equal(test.e);
      });
    });
  });

  describe('Check "real" property for each url object', function() {
    var names = App.ajax.fakeGetUrlNames();
    names.forEach(function(name) {
      it('`' + name + '`', function() {
        var url = App.ajax.fakeGetUrl(name);
        expect(url.real).to.be.a('string');
      });
      it('`' + name + '` should not contain spaces', function () {
        var url = App.ajax.fakeGetUrl(name);
        expect(url.real.contains(' ')).to.be.false;
      });
    });
  });

  describe('#formatRequest', function() {

    it('App.testMode = true', function() {
      sinon.stub(App, 'get', function(k) {
        if ('testMode' === k) return true;
        return Em.get(App, k);
      });
      var r = App.ajax.fakeFormatRequest({real:'/', mock: '/some_url'}, {});
      expect(r.type).to.equal('GET');
      expect(r.url).to.equal('/some_url');
      expect(r.dataType).to.equal('json');
      App.get.restore();
    });
    var tests = [
      {
        urlObj: {
          real: '/real_url',
          format: function() {
            return {
              type: 'PUT'
            }
          }
        },
        data: {},
        m: '',
        e: {type: 'PUT', url: '/api/v1/real_url'}
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        sinon.stub(App, 'get', function(k) {
          if ('testMode' === k) return false;
          return Em.get(App, k);
        });
        var r = App.ajax.fakeFormatRequest(test.urlObj, test.data);
        expect(r.type).to.equal(test.e.type);
        expect(r.url).to.equal(test.e.url);
        App.get.restore();
      });
    });
  });

  describe("#doGetAsPost()", function () {
    beforeEach(function () {
      sinon.stub(App, 'dateTime').returns(1);
    });
    afterEach(function () {
      App.dateTime.restore();
    });
    it("url does not have '?'", function () {
      var opt = {
        type: 'GET',
        url: '',
        headers: {}
      };
      expect(App.ajax.fakeDoGetAsPost({}, opt)).to.eql({
        type: 'POST',
        url: '?_=1',
        headers: {"X-Http-Method-Override": "GET"}
      });
    });
    it("url has '?'", function () {
      var opt = {
        type: 'GET',
        url: 'root?params',
        headers: {}
      };
      expect(App.ajax.fakeDoGetAsPost({}, opt)).to.eql({
        type: 'POST',
        url: 'root?_=1',
        headers: {"X-Http-Method-Override": "GET"},
        data: "{\"RequestInfo\":{\"query\":\"params\"}}"
      });
    });
  });

  describe('#abortRequests', function () {

    var xhr = {
        abort: Em.K
      },
      requests;

    beforeEach(function () {
      sinon.spy(xhr, 'abort');
      xhr.isForcedAbort = false;
      requests = [xhr, xhr];
      App.ajax.abortRequests(requests);
    });

    afterEach(function () {
      xhr.abort.restore();
    });

    it('should abort all requests', function () {
      expect(xhr.abort.calledTwice).to.be.true;
    });

    it('should mark request as aborted', function () {
      expect(xhr.isForcedAbort).to.be.true;
    });

    it('should clear requests array', function () {
      expect(requests).to.have.length(0);
    });

  });
});
