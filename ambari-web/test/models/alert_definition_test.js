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

require('models/alert_definition');

var model;

describe('App.AlertDefinition', function () {

  beforeEach(function () {

    model = App.AlertDefinition.createRecord();

  });

  describe('#status', function () {

    Em.A([
      {
        summary: {OK: 1, UNKNOWN: 1, WARNING: 2},
        m: 'No CRITICAL',
        e: '<span class="label alert-state-OK">OK ( 1 )</span> ' +
        '<span class="label alert-state-WARNING">WARN ( 2 )</span> ' +
        '<span class="label alert-state-UNKNOWN">UNKN ( 1 )</span>'
      },
      {
        summary: {WARNING: 2, CRITICAL: 3, UNKNOWN: 1, OK: 1},
        m: 'All states exists',
        e: '<span class="label alert-state-OK">OK ( 1 )</span> ' +
        '<span class="label alert-state-WARNING">WARN ( 2 )</span> ' +
        '<span class="label alert-state-CRITICAL">CRIT ( 3 )</span> ' +
        '<span class="label alert-state-UNKNOWN">UNKN ( 1 )</span>'
      },
      {
        summary: {OK: 1},
        m: 'Single host',
        e: '<span class="alert-state-single-host label alert-state-OK">OK</span>'
      },
      {
        summary: {},
        m: 'Pending',
        e: '<span class="alert-state-single-host label alert-state-PENDING">PENDING</span>'
      }
    ]).forEach(function (test) {
      it(test.m, function () {
        model.set('summary', test.summary);
        expect(model.get('status')).to.equal(test.e);
      });
    });

  });

  describe('#isCriticalOrWarning', function () {

    Em.A([
      {summary: {CRITICAL: 1}, e: true},
      {summary: {WARNING: 1}, e: true},
      {summary: {OK: 1}, e: false},
      {summary: {UNKNOWN: 1}, e: false},
      {summary: {}, e: false}
    ]).forEach(function (test, i) {
      it('test ' + (i + 1), function () {
        model.set('summary', test.summary);
        expect(model.get('isCriticalOrWarning')).to.equal(test.e);
      });
    });

  });

  describe('#isCritical', function () {

    Em.A([
      {summary: {CRITICAL: 1}, e: true},
      {summary: {WARNING: 1}, e: false},
      {summary: {OK: 1}, e: false},
      {summary: {UNKNOWN: 1}, e: false},
      {summary: {}, e: false}
    ]).forEach(function (test, i) {
      it('test ' + (i + 1), function () {
        model.set('summary', test.summary);
        expect(model.get('isCritical')).to.equal(test.e);
      });
    });

  });

  describe('#isWarning', function () {

    Em.A([
      {summary: {CRITICAL: 1}, e: false},
      {summary: {WARNING: 1}, e: true},
      {summary: {OK: 1}, e: false},
      {summary: {UNKNOWN: 1}, e: false},
      {summary: {}, e: false}
    ]).forEach(function (test, i) {
      it('test ' + (i + 1), function () {
        model.set('summary', test.summary);
        expect(model.get('isWarning')).to.equal(test.e);
      });
    });

  });

  describe('#lastTriggeredAgoFormatted', function () {

    it('should be empty', function () {
      model.set('lastTriggered', 0);
      expect(model.get('lastTriggeredAgoFormatted')).to.equal('');
    });

    it('should not be empty', function () {
      model.set('lastTriggered', new Date().getTime() - 61000);
      expect(model.get('lastTriggeredAgoFormatted')).to.equal('about a minute ago');
    });

  });

  describe('#componentNameFormatted', function () {

    beforeEach(function () {
      sinon.stub(App.format, 'role', function (a) {
        return 'role ' + a;
      });
    });

    it('should wrap component name by App.format.role method', function () {
      model.set('componentName', 'test');
      var result = model.get('componentNameFormatted');
      expect(result).to.equal('role test');
    });

    afterEach(function () {
      App.format.role.restore();
    });


  });

});
