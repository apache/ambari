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
var filters = require('views/common/filter_view');

describe('filters.getFilterByType', function () {


  describe('ambari-bandwidth', function () {

    var filter = filters.getFilterByType('ambari-bandwidth');
    var testData = [
      {
        condition: '<',
        value: 'any value',
        result: true
      },
      {
        condition: '=',
        value: 'any value',
        result: true
      },
      {
        condition: '>',
        value: 'any value',
        result: true
      },
      {
        condition: '1',
        value: '1GB',
        result: true
      },
      {
        condition: '1g',
        value: '1GB',
        result: true
      },
      {
        condition: '=1g',
        value: '1GB',
        result: true
      },
      {
        condition: '<1g',
        value: '0.9GB',
        result: true
      },
      {
        condition: '>1g',
        value: '1.1GB',
        result: true
      },
      {
        condition: '=1k',
        value: '1KB',
        result: true
      },
      {
        condition: '<1k',
        value: '0.9KB',
        result: true
      },
      {
        condition: '>1k',
        value: '1.1KB',
        result: true
      },
      {
        condition: '=1m',
        value: '1MB',
        result: true
      },
      {
        condition: '<1m',
        value: '0.9MB',
        result: true
      },
      {
        condition: '>1m',
        value: '1.1MB',
        result: true
      },
      {
        condition: '=1024k',
        value: '1MB',
        result: true
      },
      {
        condition: '=1024m',
        value: '1GB',
        result: true
      }
    ];

    testData.forEach(function(item){
      it('Condition: ' + item.condition + ' - match value: ' + item.value, function () {
        expect(filter(item.value, item.condition)).to.equal(item.result);
      })
    });
  });

  describe('duration', function () {

    var filter = filters.getFilterByType('duration');
    var testData = [
      {
        condition: '<',
        value: 'any value',
        result: true
      },
      {
        condition: '=',
        value: 'any value',
        result: true
      },
      {
        condition: '>',
        value: 'any value',
        result: true
      },
      {
        condition: '1',
        value: '1000',
        result: true
      },
      {
        condition: '1s',
        value: '1000',
        result: true
      },
      {
        condition: '=1s',
        value: '1000',
        result: true
      },
      {
        condition: '>1s',
        value: '1001',
        result: true
      },
      {
        condition: '<1s',
        value: '999',
        result: true
      },
      {
        condition: '=1m',
        value: '60000',
        result: true
      },
      {
        condition: '>1m',
        value: '60001',
        result: true
      },
      {
        condition: '<1m',
        value: '59999',
        result: true
      },
      {
        condition: '=1h',
        value: '3600000',
        result: true
      },
      {
        condition: '>1h',
        value: '3600001',
        result: true
      },
      {
        condition: '<1h',
        value: '3599999',
        result: true
      }

    ];

    testData.forEach(function(item){
      it('Condition: ' + item.condition + ' - match value: ' + item.value, function () {
        expect(filter(item.value, item.condition)).to.equal(item.result);
      })
    });
  });

  describe('date', function () {

    var filter = filters.getFilterByType('date');
    var currentTime = new Date().getTime();
    var testData = [
      {
        condition: 'Past 1 Day',
        value: currentTime - 86300000,
        result: true
      },
      {
        condition: 'Past 2 Days',
        value: currentTime - 172700000,
        result: true
      },
      {
        condition: 'Past 7 Days',
        value: currentTime - 604700000,
        result: true
      },
      {
        condition: 'Past 14 Days',
        value: currentTime - 1209500000,
        result: true
      },
      {
        condition: 'Past 30 Days',
        value: currentTime - 2591900000,
        result: true
      },
      {
        condition: 'Any',
        value: 'any value',
        result: true
      }
    ];

    testData.forEach(function(item){
      it('Condition: ' + item.condition + ' - match value: ' + item.value, function () {
        expect(filter(item.value, item.condition)).to.equal(item.result);
      })
    });
  });

  describe('number', function () {

    var filter = filters.getFilterByType('number');
    var testData = [
      {
        condition: '<',
        value: 'any value',
        result: true
      },
      {
        condition: '=',
        value: 'any value',
        result: true
      },
      {
        condition: '>',
        value: 'any value',
        result: true
      },
      {
        condition: '1',
        value: '1',
        result: true
      },
      {
        condition: '=1',
        value: '1',
        result: true
      },
      {
        condition: '<1',
        value: '0',
        result: true
      },
      {
        condition: '>1',
        value: '2',
        result: true
      }
    ];

    testData.forEach(function(item){
      it('Condition: ' + item.condition + ' - match value: ' + item.value, function () {
        expect(filter(item.value, item.condition)).to.equal(item.result);
      })
    });
  });

  describe('multiple', function () {

    var filter = filters.getFilterByType('multiple');
    var commonValue = [
      {componentName: 'DATANODE'},
      {componentName: 'NAMENODE'},
      {componentName: 'JOBTRACKER'}
    ];
    var testData = [
      {
        condition: 'DATANODE',
        value: commonValue,
        result: true
      },
      {
        condition: 'DATANODE,NAMENODE',
        value: commonValue,
        result: true
      },
      {
        condition: 'DATANODE,NAMENODE,JOBTRACKER',
        value: commonValue,
        result: true
      },
      {
        condition: 'JOBTRACKER,TASKTRACKER',
        value: commonValue,
        result: true
      },
      {
        condition: 'TASKTRACKER',
        value: commonValue,
        result: false
      }
    ];

    testData.forEach(function(item){
      it('Condition: ' + item.condition + ((item.result) ? ' - match ' : ' - doesn\'t match ' + 'value: ') +
        item.value.mapProperty('componentName').join(" "), function () {
        expect(filter(item.value, item.condition)).to.equal(item.result);
      })
    });
  });

  describe('string', function () {

    var filter = filters.getFilterByType('string');

    var testData = [
      {
        condition: '',
        value: '',
        result: true
      },
      {
        condition: '',
        value: 'hello',
        result: true
      },
      {
        condition: 'hello',
        value: 'hello',
        result: true
      },
      {
        condition: 'HeLLo',
        value: 'hello',
        result: true
      },
      {
        condition: 'he',
        value: 'hello',
        result: true
      },
      {
        condition: 'lo',
        value: 'hello',
        result: true
      },
      {
        condition: 'lol',
        value: 'hello',
        result: false
      },
      {
        condition: 'hello',
        value: '',
        result: false
      }
    ];

    testData.forEach(function(item){
      it('Condition: ' + item.condition + ((item.result) ? ' - match ' : ' - doesn\'t match ' + 'value: ') + item.value, function () {
        expect(filter(item.value, item.condition)).to.equal(item.result);
      })
    });
  });
});
