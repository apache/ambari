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
var date = require('utils/date/date');

describe('date', function () {

  var incorrectTests = Em.A([
    {m: 'null', t: null},
    {m: 'empty', t: ''},
    {m: 'false', t: false},
    {m:'[]' , t: []},
    {m: '{}', t: {}},
    {m: 'undefined', t: undefined},
    {m: 'empty function', t: function(){}}
  ]);

  describe('#dateFormatZeroFirst()', function() {
    var tests = [
      {
        t: 2,
        e: '02',
        m: 'should convert to `02`'
      },
      {
        t: 10,
        e: '10',
        m: 'should convert to `10`'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        expect(date.dateFormatZeroFirst(test.t)).to.eql(test.e);
      });
    });
  });

  describe('#startTime()', function() {
    var today = new Date();
    var testDate = new Date(1349752195000);
    var tests = [
      { t: 1349752195000, e: testDate.toDateString() + ' {0}:{1}'.format(date.dateFormatZeroFirst(testDate.getHours()), date.dateFormatZeroFirst(testDate.getMinutes())) },
      { t: -10000000, e: 'Not started' },
      { t: today.getTime(), e: 'Today {0}:{1}'.format(date.dateFormatZeroFirst(today.getHours()), date.dateFormatZeroFirst(today.getMinutes())) },
      { t: today, e: ''}
    ];
    tests.forEach(function(test) {
      var testMessage = 'should convert {0} to {1}'.format(test.t, test.e);
      it(testMessage, function() {
        expect(date.startTime(test.t)).to.be.eql(test.e);
      });
    });
  });

  describe('#endTime()', function() {
    var today = new Date();
    var testDate = new Date(1349752195000);
    var tests = [
      { t: 1349752195000, e: testDate.toDateString() + ' {0}:{1}'.format(date.dateFormatZeroFirst(testDate.getHours()), date.dateFormatZeroFirst(testDate.getMinutes())) },
      { t: -10000000, e: 'Not finished' },
      { t: today.getTime(), e: 'Today {0}:{1}'.format(date.dateFormatZeroFirst(today.getHours()), date.dateFormatZeroFirst(today.getMinutes())) },
      { t: today, e: ''}
    ];
    tests.forEach(function(test) {
      var testMessage = 'should convert {0} to {1}'.format(test.t, test.e);
      it(testMessage, function() {
        expect(date.endTime(test.t)).to.be.eql(test.e);
      });
    });
  });

  describe('#timingFormat', function() {
    var tests = Em.A([
      {i: '30', e:'30 ms'},
      {i: '300', e:'300 ms'},
      {i: '999', e:'999 ms'},
      {i: '1000', e:'1.00 secs'},
      {i: '3000', e:'3.00 secs'},
      {i: '35000', e:'35.00 secs'},
      {i: '350000', e:'350.00 secs'},
      {i: '999999', e:'1000.00 secs'},
      {i: '1000000', e:'16.67 mins'},
      {i: '3500000', e:'58.33 mins'},
      {i: '35000000', e:'9.72 hours'},
      {i: '350000000', e:'4.05 days'},
      {i: '3500000000', e:'40.51 days'},
      {i: '35000000000', e:'405.09 days'}
    ]);

    describe('Correct data', function(){
      tests.forEach(function(test) {
        it(test.i, function() {
          expect(date.timingFormat(test.i)).to.equal(test.e);
        });
      });
    });

    describe('Incorrect data', function(){
      incorrectTests.forEach(function(test) {
        it(test.m, function() {
          expect(date.timingFormat(test.t)).to.equal(null);
        });
      });
    });

  });

  describe('#duration', function() {
    var tests = Em.A([
      {startTime: 1, endTime: 2, e: 1},
      {startTime: 0, endTime: 2000, e: 0},
      {startTime: 200, endTime: 0, e: 19800}
    ]);

    beforeEach(function() {
      sinon.stub(App, 'dateTime', function () { return 20000; });
    });

    tests.forEach(function(test) {
      it(test.startTime + ' ' + test.endTime, function() {
        expect(date.duration(test.startTime, test.endTime)).to.equal(test.e);
      });
    });

    afterEach(function() {
      App.dateTime.restore();
    });
  });

  describe('#durationSummary()', function() {
    var tests = [
      {
        startTimestamp: 1349752195000,
        endTimestamp: 1349752199000,
        e: '4.00 secs'
      },
      {
        startTimestamp: 1349752195000,
        endTimestamp: 1367752195000,
        e: '208.33 days'
      },
      {
        startTimestamp: -10000000,
        endTimestamp: 1367752195000,
        e: Em.I18n.t('common.na')
      },
      {
        startTimestamp: 1349752195000,
        endTimestamp: -1,
        stubbed: true,
        e: '0 secs'
      },
      {
        startTimestamp: 100000000,
        endTimestamp: -1,
        stubbed: true,
        e: '19.00 secs'
      }
    ];

    beforeEach(function() {
      sinon.stub(App, 'dateTimeWithTimeZone', function () { return 100019000; });
    });

    tests.forEach(function(test) {
      var testMessage = 'duration between {0} and {1} is {2}'.format(test.startTimestamp, test.endTimestamp, test.e) + (test.stubbed ? " App.dateTime() is stubbed" : "");
      it(testMessage, function() {
        expect(date.durationSummary(test.startTimestamp, test.endTimestamp)).to.be.eql(test.e);
      });
    });

    afterEach(function() {
      App.dateTimeWithTimeZone.restore();
    });
  });

});