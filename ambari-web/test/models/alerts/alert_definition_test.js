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

require('models/alerts/alert_definition');

var model;

describe('App.AlertDefinition', function () {

  beforeEach(function () {

    model = App.AlertDefinition.createRecord();

  });

  describe('#status', function () {

    Em.A([
      {
        summary: {OK: {count: 1, maintenanceCount: 0}, UNKNOWN: {count: 1, maintenanceCount: 0}, WARNING: {count: 2, maintenanceCount: 0}, CRITICAL: {count: 0, maintenanceCount: 0}},
        m: 'No CRITICAL',
        e: '<span class="alert-state-single-host label alert-state-OK">OK (1)</span> ' +
        '<span class="alert-state-single-host label alert-state-WARNING">WARN (2)</span> ' +
        '<span class="alert-state-single-host label alert-state-UNKNOWN">UNKWN (1)</span>'
      },
      {
        summary: {WARNING: {count: 2, maintenanceCount: 0}, CRITICAL: {count: 3, maintenanceCount: 0}, UNKNOWN: {count: 1, maintenanceCount: 0}, OK: {count: 1, maintenanceCount: 0}},
        m: 'All states exists',
        e: '<span class="alert-state-single-host label alert-state-OK">OK (1)</span> ' +
        '<span class="alert-state-single-host label alert-state-WARNING">WARN (2)</span> ' +
        '<span class="alert-state-single-host label alert-state-CRITICAL">CRIT (3)</span> ' +
        '<span class="alert-state-single-host label alert-state-UNKNOWN">UNKWN (1)</span>'
      },
      {
        summary: {OK: {count: 1, maintenanceCount: 0}, UNKNOWN: {count: 0, maintenanceCount: 0}, WARNING: {count: 0, maintenanceCount: 0}, CRITICAL: {count: 0, maintenanceCount: 0}},
        m: 'Single host',
        e: '<span class="alert-state-single-host label alert-state-OK">OK</span>'
      },
      {
        summary: {OK: {count: 0, maintenanceCount: 1}, UNKNOWN: {count: 0, maintenanceCount: 0}, WARNING: {count: 0, maintenanceCount: 0}, CRITICAL: {count: 0, maintenanceCount: 0}},
        m: 'Maintenance OK alert',
        e: '<span class="alert-state-single-host label alert-state-PENDING"><span class="icon-medkit"></span> OK</span>'
      },
      {
        summary: {},
        m: 'Pending',
        e: '<span class="alert-state-single-host label alert-state-PENDING">NONE</span>'
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
      {summary: {CRITICAL: {count: 1, maintenanceCount: 0}}, e: true},
      {summary: {CRITICAL: {count: 0, maintenanceCount: 1}}, e: false},
      {summary: {CRITICAL: {count: 1, maintenanceCount: 1}}, e: true},
      {summary: {WARNING: {count: 1, maintenanceCount: 0}}, e: true},
      {summary: {OK: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {UNKNOWN: {count: 1, maintenanceCount: 0}}, e: false},
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
      {summary: {CRITICAL: {count: 1, maintenanceCount: 0}}, e: true},
      {summary: {WARNING: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {OK: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {UNKNOWN: {count: 1, maintenanceCount: 0}}, e: false},
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
      {summary: {CRITICAL: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {WARNING: {count: 1, maintenanceCount: 0}}, e: true},
      {summary: {OK: {count: 1, maintenanceCount: 0}}, e: false},
      {summary: {UNKNOWN: {count: 1, maintenanceCount: 0}}, e: false},
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
      model.set('lastTriggeredRaw', 0);
      expect(model.get('lastTriggeredAgoFormatted')).to.equal('');
    });

    it('should not be empty', function () {
      model.set('lastTriggeredRaw', new Date().getTime() - 61000);
      expect(model.get('lastTriggeredAgoFormatted')).to.equal('about a minute ago');
    });

  });

  describe('#serviceDisplayName', function () {

    it('should get name for non-existing service', function () {
      model.set('serviceName', 'FOOBAR');
      expect(model.get('serviceDisplayName')).to.equal('Foobar');
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

  describe('REOPEN', function () {

    describe('#getSortDefinitionsByStatus', function () {

      Em.A([
          {
            a: App.AlertDefinition.createRecord({summary: {OK: {count: 1, maintenanceCount: 0}, WARNING: {count: 1, maintenanceCount: 0}}}),
            b: App.AlertDefinition.createRecord({summary: {WARNING: {count: 1, maintenanceCount: 0}}}),
            order: true,
            e: -1
          },
          {
            a: App.AlertDefinition.createRecord({summary: {OK: {count: 1, maintenanceCount: 0}, WARNING: {count: 2, maintenanceCount: 0}}}),
            b: App.AlertDefinition.createRecord({summary: {OK: {count: 1, maintenanceCount: 0}, WARNING: {count: 1, maintenanceCount: 0}}}),
            order: true,
            e: -1
          },
          {
            a: App.AlertDefinition.createRecord({summary: {OK: {count: 1, maintenanceCount: 0}, WARNING: {count: 1, maintenanceCount: 0}}}),
            b: App.AlertDefinition.createRecord({summary: {WARNING: {count: 1, maintenanceCount: 0}}}),
            order: false,
            e: 1
          },
          {
            a: App.AlertDefinition.createRecord({summary: {OK: {count: 1, maintenanceCount: 0}, WARNING: {count: 2, maintenanceCount: 0}}}),
            b: App.AlertDefinition.createRecord({summary: {OK: {count: 1, maintenanceCount: 0}, WARNING: {count: 1, maintenanceCount: 0}}}),
            order: false,
            e: 1
          }
        ]).forEach(function(test, i) {
          it('test #' + (i + 1), function () {
            var func = App.AlertDefinition.getSortDefinitionsByStatus(test.order);
            expect(func(test.a, test.b)).to.equal(test.e);
          });
        });

    });

  });

});
