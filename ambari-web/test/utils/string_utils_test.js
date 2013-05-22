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

var string_utils = require('utils/string_utils');

describe('string_utils', function () {

  describe('#underScoreToCamelCase', function () {
    var tests = [
      {m:'a_b_c to aBC',i:'a_b_c',e:'aBC'},
      {m:'a_bc to aBc',i:'a_bc',e:'aBc'},
      {m:'ab_c to abC',i:'ab_c',e:'abC'},
      {m:'_b_c to BC',i:'_b_c',e:'BC'}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(string_utils.underScoreToCamelCase(test.i)).to.equal(test.e);
      });
    });
  });

  describe('#pad', function () {
    var tests = [
      {m: '"name" to "    name"', i: 'name', l: 8, a: 1, f: ' ', e: '    name'},
      {m: '"name" to "name    "', i: 'name', l: 8, a: 2, f: ' ', e: 'name    '},
      {m: '"name" to "  name  "', i: 'name', l: 8, a: 3, f: ' ', e: '  name  '},
      {m: '"name" to "name    "', i: 'name', l: 8, a: 0, f: ' ', e: 'name    '},
      {m: '"name" to "name    "', i: 'name', l: 8, a:-1, f: ' ', e: 'name    '},
      {m: '"name" to "name"', i: 'name', l: 4, a: 1, f: ' ', e: 'name'},
      {m: '"name" to "||||||||name"', i: 'name', l: 8, a:1, f: '||', e: '||||||||name'},
      {m: '"name" to "||||name||||"', i: 'name', l: 8, a:3, f: '||', e: '||||name||||'},
      {m: '"name" to "name||||||||"', i: 'name', l: 8, a:2, f: '||', e: 'name||||||||'}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(string_utils.pad(test.i, test.l, test.f, test.a)).to.equal(test.e);
      });
    });
  });

});
