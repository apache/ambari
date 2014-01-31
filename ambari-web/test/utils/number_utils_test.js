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

var numberUtils = require('utils/number_utils');

describe('', function() {

  describe('#bytesToSize', function() {

    describe('check bytes', function() {
      var tests = Em.A([
        {
          bytes: null,
          precision: null,
          parseType: null,
          multiplyBy: null,
          e: 'n/a',
          m: '"n/a" if bytes is null'
        },
        {
          bytes: undefined,
          precision: null,
          parseType: null,
          multiplyBy: null,
          e: 'n/a',
          m: '"n/a" if bytes is undefined'
        }
      ]);

      tests.forEach(function(test) {
        it(test.m, function() {
          expect(numberUtils.bytesToSize(test.bytes, test.precision, test.parseType, test.multiplyBy)).to.equal(test.e);
        });
      });
    });

    describe('check sizes', function() {
      var tests = Em.A([
        {
          bytes: 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: 'Bytes',
          m: 'Bytes'
        },
        {
          bytes: 1024 + 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: 'KB',
          m: 'KB'
        },
        {
          bytes: 1024 * 1024 + 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: 'MB',
          m: 'MB'
        },
        {
          bytes: 1024 * 1024 * 1024 + 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: 'GB',
          m: 'GB'
        },
        {
          bytes: 1024 * 1024 * 1024 * 1024 + 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: 'TB',
          m: 'TB'
        },
        {
          bytes: 1024 * 1024 * 1024 * 1024 * 1024 + 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: 'PB',
          m: 'PB'
        }
      ]);

      tests.forEach(function(test) {
        it(test.m, function() {
          expect(numberUtils.bytesToSize(test.bytes, test.precision, test.parseType, test.multiplyBy).endsWith(test.e)).to.equal(true);
        });
      });
    });

    describe('check calculated result', function() {
      var tests = Em.A([
        {
          bytes: 42,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: '42',
          m: 'Bytes'
        },
        {
          bytes: 1024 * 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: '12',
          m: 'KB'
        },
        {
          bytes: 1024 * 1024 * 23,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: '23',
          m: 'MB'
        },
        {
          bytes: 1024 * 1024 * 1024 * 34,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: '34',
          m: 'GB'
        },
        {
          bytes: 1024 * 1024 * 1024 * 1024 * 45,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: '45',
          m: 'TB'
        },
        {
          bytes: 1024 * 1024 * 1024 * 1024 * 1024 * 56,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: '56',
          m: 'PB'
        }
      ]);

      tests.forEach(function(test) {
        it(test.m, function() {
          expect(numberUtils.bytesToSize(test.bytes, test.precision, test.parseType, test.multiplyBy).startsWith(test.e)).to.equal(true);
        });
      });
    });

  });

});