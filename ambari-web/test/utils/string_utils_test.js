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

  describe('#compareVersions', function () {
    var tests = [
      {m: '1.2 equal to 1.2', v1:'1.2', v2:'1.2', e: 0},
      {m: '1.2 lower than 1.3', v1:'1.2', v2:'1.3', e: -1},
      {m: '1.3 higher than 1.2', v1:'1.3', v2:'1.2', e: 1},
      {m: '1.2.1 higher than 1.2', v1:'1.2.1', v2:'1.2', e: 1},
      {m: '11.2 higher than 2.2', v1:'11.2', v2:'2.2', e: 1},
      {m: '0.9 higher than 0.8', v1:'0.9', v2:'0.8', e: 1}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(string_utils.compareVersions(test.v1, test.v2)).to.equal(test.e);
      });
    });
  });

  describe('#isSingleLine', function () {
    var tests = [
      {m: 'is single line text', t: 'a b', e: true},
      {m: 'is single line text', t: 'a b\n', e: true},
      {m: 'is single line text', t: '\na b', e: true},
      {m: 'is not single line text', t: 'a\nb', e: false}
    ];
    tests.forEach(function(test) {
      it(test.t + ' ' + test.m + ' ', function () {
        expect(string_utils.isSingleLine(test.t)).to.equal(test.e);
      });
    });
  });

  describe('#arrayToCSV', function() {
    var test = [{a: 1, b:2, c:3}, {a: 1, b:2, c:3}, {a: 1, b:2, c:3}];
    it('array of object to csv-string', function () {
      expect(string_utils.arrayToCSV(test)).to.equal("1,2,3\n1,2,3\n1,2,3\n");
    });
  });

  describe('#getFileFromPath', function() {
    var tests = [
      {t: undefined, e: ''},
      {t: {}, e: ''},
      {t: [], e: ''},
      {t: '', e: ''},
      {t: function(){}, e: ''},
      {t: '/path/to/file.ext', e: 'file.ext'},
      {t: 'file.ext', e: 'file.ext'},
      {t: 'file', e: 'file'},
      {t: '/path/to/file', e: 'file'}
    ];
    tests.forEach(function(test) {
      it('Check ' + typeof test.t, function () {
        expect(string_utils.getFileFromPath(test.t)).to.equal(test.e);
      });
    });
  });

    describe('#getPath', function() {
        var tests = [
          {t: undefined, e: ''},
          {t: {}, e: ''},
          {t: [], e: ''},
          {t: '', e: ''},
          {t: function(){}, e: ''},
          {t: '/path/to/filename', e: '/path/to'},
          {t: '/path/to/', e: '/path/to'},
          {t: '/filename', e: '/'},
          {t: 'filename', e: ''},
          {t: '/path/', e: '/path'},
          {t: 'filename/', e: ''}
      ];
      tests.forEach(function(test) {
          it('Check ' + typeof test.t, function () {
            expect(string_utils.getPath(test.t)).to.equal(test.e);
          });
      });
  });

});
