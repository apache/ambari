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
    sinon.spy($, 'ajax');
  });

  afterEach(function() {
    $.ajax.restore();
  });

  describe('#send', function() {

    it('Without sender', function() {
      expect(App.ajax.send({})).to.equal(null);
      expect($.ajax.called).to.be.false;
    });

    it('Invalid config.name', function() {
      expect(App.ajax.send({name:'fake_name', sender: this})).to.equal(null);
      expect($.ajax.called).to.be.false;
    });

    it('With proper data', function() {
      App.ajax.send({name: 'router.logoff', sender: this});
      expect($.ajax.calledOnce).to.be.true;
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

  describe('Check "real" and "mock" properties for each url object', function() {
    var names = App.ajax.fakeGetUrlNames();
    names.forEach(function(name) {
      it(name, function() {
        var url = App.ajax.fakeGetUrl(name);
        expect(url.real).to.be.a('string');
        expect(url.real.length > 0).to.equal(true);
        expect(url.mock).to.be.a('string');
      });
    });
  });

  describe('#formatRequest', function() {

    beforeEach(function() {
      App.testMode = false;
    });
    afterEach(function() {
      App.testMode = true;
    });

    it('App.testMode = true', function() {
      App.testMode = true;
      var r = App.ajax.fakeFormatRequest({real:'/', mock: '/some_url'}, {});
      expect(r.type).to.equal('GET');
      expect(r.url).to.equal('/some_url');
      expect(r.dataType).to.equal('json');
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
        var r = App.ajax.fakeFormatRequest(test.urlObj, test.data);
        expect(r.type).to.equal(test.e.type);
        expect(r.url).to.equal(test.e.url);
      });
    });
  });

});
