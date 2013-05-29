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

var Ember = require('ember');
var App = require('app');

var validator = require('utils/validator');
var date = require('utils/date');

require('views/common/modal_popup');
require('controllers/global/background_operations_controller');
require('utils/helper');
require('utils/host_progress_popup');

describe('date', function () {

  var correct_tests = [
    {t: 1349752195000, e: 'Tue, Oct 09, 2012 07:09', e2: 'Tue Oct 09 2012'},
    {t: 1367752195000, e: 'Sun, May 05, 2013 15:09', e2: 'Sun May 05 2013'},
    {t: 1369952195000, e: 'Fri, May 31, 2013 02:16', e2: 'Fri May 31 2013'},
    {t: 1369052195000, e: 'Mon, May 20, 2013 16:16', e2: 'Mon May 20 2013'},
    {t: 1369792195000, e: 'Wed, May 29, 2013 05:49', e2: 'Wed May 29 2013'},
    {t: 1389752195000, e: 'Wed, Jan 15, 2014 05:16', e2: 'Wed Jan 15 2014'}
  ];

  var incorrect_tests = [
    {t: null},
    {t: ''},
    {t: false},
    {t: []},
    {t: {}},
    {t: undefined},
    {t: function(){}}
  ];

  describe('#dateFormat', function() {
    it('Correct timestamps', function(){
      correct_tests.forEach(function(test) {
        expect(date.dateFormat(test.t)).to.equal(test.e);
      });
    });
    it('Incorrect timestamps', function() {
      incorrect_tests.forEach(function(test) {
        expect(date.dateFormat(test.t)).to.equal(test.t);
      });
    });
  });

  describe('#dateFormatShort', function() {
    it('Correct timestamps', function(){
      correct_tests.forEach(function(test) {
        expect(date.dateFormatShort(test.t)).to.equal(test.e2);
      });
    });
    it('Today timestamp', function() {
      var now = new Date();
      var then = new Date(now.getFullYear(),now.getMonth(),now.getDate(),0,0,0);
      expect(date.dateFormatShort(then.getTime() + 10*3600*1000)).to.equal('Today 10:00:00');
    });
    it('Incorrect timestamps', function() {
      incorrect_tests.forEach(function(test) {
        expect(date.dateFormatShort(test.t)).to.equal(test.t);
      });
    });
  });

  describe('#timingFormat', function() {
    var tests = [
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
    ];

    it('Correct data', function(){
      tests.forEach(function(test) {
        expect(date.timingFormat(test.i)).to.equal(test.e);
      });
    });

    it('Incorrect data', function(){
      incorrect_tests.forEach(function(test) {
        expect(date.timingFormat(test.t)).to.equal(null);
      });
    });

  });

});