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

var validator = require('utils/validator');

describe('validator', function () {

  describe('#isValidEmail(value)', function () {
    it('should return false if value is null', function () {
      expect(validator.isValidEmail(null)).to.equal(false);
    })
    it('should return false if value is ""', function () {
      expect(validator.isValidEmail('')).to.equal(false);
    })
    it('should return false if value is "a.com"', function () {
      expect(validator.isValidEmail('a.com')).to.equal(false);
    })
    it('should return false if value is "@a.com"', function () {
      expect(validator.isValidEmail('@a.com')).to.equal(false);
    })
    it('should return false if value is "a@.com"', function () {
      expect(validator.isValidEmail('a@.com')).to.equal(false);
    })
    it('should return true if value is "a@a.com"', function () {
      expect(validator.isValidEmail('a@a.com')).to.equal(true);
    })
    it('should return true if value is "user@a.b.com"', function () {
      expect(validator.isValidEmail('user@a.b.com')).to.equal(true);
    })
  })

  describe('#isValidInt(value)', function () {
    it('should return false if value is null', function () {
      expect(validator.isValidInt(null)).to.equal(false);
    })
    it('should return false if value is ""', function () {
      expect(validator.isValidInt('')).to.equal(false);
    })
    it('should return false if value is "abc"', function () {
      expect(validator.isValidInt('abc')).to.equal(false);
    })
    it('should return false if value is "0xff"', function () {
      expect(validator.isValidInt('0xff')).to.equal(false);
    })
    it('should return false if value is " 1""', function () {
      expect(validator.isValidInt(' 1')).to.equal(false);
    })
    it('should return false if value is "1 "', function () {
      expect(validator.isValidInt('1 ')).to.equal(false);
    })
    it('should return true if value is "10"', function () {
      expect(validator.isValidInt('10')).to.equal(true);
    })
    it('should return true if value is "-123"', function () {
      expect(validator.isValidInt('-123')).to.equal(true);
    })
    it('should return true if value is "0"', function () {
      expect(validator.isValidInt('0')).to.equal(true);
    })
    it('should return true if value is 10', function () {
      expect(validator.isValidInt(10)).to.equal(true);
    })
    it('should return true if value is -123', function () {
      expect(validator.isValidInt(10)).to.equal(true);
    })
    it('should return true if value is 0', function () {
      expect(validator.isValidInt(10)).to.equal(true);
    })
  })

  describe('#isValidFloat(value)', function () {
    it('should return false if value is null', function () {
      expect(validator.isValidFloat(null)).to.equal(false);
    })
    it('should return false if value is ""', function () {
      expect(validator.isValidFloat('')).to.equal(false);
    })
    it('should return false if value is "abc"', function () {
      expect(validator.isValidFloat('abc')).to.equal(false);
    })
    it('should return false if value is "0xff"', function () {
      expect(validator.isValidFloat('0xff')).to.equal(false);
    })
    it('should return false if value is " 1""', function () {
      expect(validator.isValidFloat(' 1')).to.equal(false);
    })
    it('should return false if value is "1 "', function () {
      expect(validator.isValidFloat('1 ')).to.equal(false);
    })
    it('should return true if value is "10"', function () {
      expect(validator.isValidFloat('10')).to.equal(true);
    })
    it('should return true if value is "-123"', function () {
      expect(validator.isValidFloat('-123')).to.equal(true);
    })
    it('should return true if value is "0"', function () {
      expect(validator.isValidFloat('0')).to.equal(true);
    })
    it('should return true if value is 10', function () {
      expect(validator.isValidFloat(10)).to.equal(true);
    })
    it('should return true if value is -123', function () {
      expect(validator.isValidFloat(10)).to.equal(true);
    })
    it('should return true if value is 0', function () {
      expect(validator.isValidFloat(10)).to.equal(true);
    })
    it('should return true if value is "0.0"', function () {
      expect(validator.isValidFloat("0.0")).to.equal(true);
    })
    it('should return true if value is "10.123"', function () {
      expect(validator.isValidFloat("10.123")).to.equal(true);
    })
    it('should return true if value is "-10.123"', function () {
      expect(validator.isValidFloat("-10.123")).to.equal(true);
    })
    it('should return true if value is 10.123', function () {
      expect(validator.isValidFloat(10.123)).to.equal(true);
    })
    it('should return true if value is -10.123', function () {
      expect(validator.isValidFloat(-10.123)).to.equal(true);
    })

  })
  /*describe('#isIpAddress(value)', function () {
    it('"127.0.0.1" - valid IP', function () {
      expect(validator.isIpAddress('127.0.0.1')).to.equal(true);
    })
    it('"227.3.67.196" - valid IP', function () {
      expect(validator.isIpAddress('227.3.67.196')).to.equal(true);
    })
    it('"327.0.0.0" - invalid IP', function () {
      expect(validator.isIpAddress('327.0.0.0')).to.equal(false);
    })
    it('"127.0.0." - invalid IP', function () {
      expect(validator.isIpAddress('127.0.0.')).to.equal(false);
    })
    it('"127.0." - invalid IP', function () {
      expect(validator.isIpAddress('127.0.')).to.equal(false);
    })
    it('"127" - invalid IP', function () {
      expect(validator.isIpAddress('127')).to.equal(false);
    })
    it('"127.333.0.1" - invalid IP', function () {
      expect(validator.isIpAddress('127.333.0.1')).to.equal(false);
    })
    it('"127.0.333.1" - invalid IP', function () {
      expect(validator.isIpAddress('127.0.333.1')).to.equal(false);
    })
    it('"127.0.1.333" - invalid IP', function () {
      expect(validator.isIpAddress('127.0.1.333')).to.equal(false);
    })
    it('"127.0.0.0:45555" - valid IP', function () {
      expect(validator.isIpAddress('127.0.0.0:45555')).to.equal(true);
    })
    it('"327.0.0.0:45555" - invalid IP', function () {
      expect(validator.isIpAddress('327.0.0.0:45555')).to.equal(false);
    })
    it('"0.0.0.0" - invalid IP', function () {
      expect(validator.isIpAddress('0.0.0.0')).to.equal(false);
    })
    it('"0.0.0.0:12" - invalid IP', function () {
      expect(validator.isIpAddress('0.0.0.0:12')).to.equal(false);
    })
    it('"1.0.0.0:0" - invalid IP', function () {
      expect(validator.isIpAddress('1.0.0.0:0')).to.equal(false);
    })
  })*/
  describe('#isDomainName(value)', function () {
    it('"google.com" - valid Domain Name', function () {
      expect(validator.isDomainName('google.com')).to.equal(true);
    })
    it('"google" - invalid Domain Name', function () {
      expect(validator.isDomainName('google')).to.equal(false);
    })
    it('"123.123" - invalid Domain Name', function () {
      expect(validator.isDomainName('123.123')).to.equal(false);
    })
    it('"4goog.le" - valid Domain Name', function () {
      expect(validator.isDomainName('4goog.le')).to.equal(true);
    })
    it('"55454" - invalid Domain Name', function () {
      expect(validator.isDomainName('55454')).to.equal(false);
    })
  })
  describe('#isValidUserName(value)', function() {
    var tests = [
      {m:'"" - invalid',i:'',e:false},
      {m:'"abc123" - valid',i:'abc123',e:true},
      {m:'"1abc123" - invalid',i:'1abc123',e:false},
      {m:'"abc123$" - invalid',i:'abc123$',e:false},
      {m:'"~1abc123" - invalid',i: '~1abc123',e:false},
      {m:'"abc12345679abc1234567890abc1234567890$" - invalid',i:'abc12345679abc1234567890abc1234567890$',e:false},
      {m:'"1abc123$$" - invalid',i:'1abc123$$',e:false},
      {m:'"a" - valid',i:'a',e:true},
      {m:'"!" - invalid',i:'!',e:false},
      {m:'"root$" - invalid',i:'root$',e:false},
      {m:'"rootU" - invalid',i:'rootU',e:false},
      {m:'"rUoot" - invalid',i:'rUoot',e:false}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isValidUserName(test.i)).to.equal(test.e);
      })
    });
  })
  describe('#isValidUNIXUser(value)', function() {
    var tests = [
      {m:'"" - invalid',i:'',e:false},
      {m:'"abc123" - valid',i:'abc123',e:true},
      {m:'"1abc123" - invalid',i:'1abc123',e:false},
      {m:'"abc123$" - invalid',i:'abc123$',e:false},
      {m:'"~1abc123" - invalid',i: '~1abc123',e:false},
      {m:'"abc12345679abc1234567890abc1234567890$" - invalid',i:'abc12345679abc1234567890abc1234567890$',e:false},
      {m:'"1abc123$$" - invalid',i:'1abc123$$',e:false},
      {m:'"a" - valid',i:'a',e:true},
      {m:'"!" - invalid',i:'!',e:false},
      {m:'"abc_" - valid',i:'abc_',e:true},
      {m:'"_abc" - valid',i:'_abc',e:true},
      {m:'"abc_abc" - valid',i:'_abc',e:true}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isValidUNIXUser(test.i)).to.equal(test.e);
      })
    });
  })
  describe('#isValidDir(value)', function() {
    var tests = [
      {m:'"dir" - invalid',i:'dir',e:false},
      {m:'"/dir" - valid',i:'/dir',e:true},
      {m:'"/dir1,dir2" - invalid',i:'/dir1,dir2',e:false},
      {m:'"/dir1,/dir2" - valid',i:'/dir1,/dir2',e:true},
      {m:'"/123" - valid',i:'/111',e:true},
      {m:'"/abc" - valid',i:'/abc',e:true},
      {m:'"/1a2b3c" - valid',i:'/1a2b3c',e:true}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isValidDir(test.i)).to.equal(test.e);
      })
    });
  })
  describe('#isValidConfigKey(value)', function() {
    var tests = [
      {m:'"123" - valid',i:'123',e:true},
      {m:'"abc" - valid',i:'abc',e:true},
      {m:'"abc123" - valid',i:'abc123',e:true},
      {m:'".abc." - valid',i:'.abc.',e:true},
      {m:'"_abc_" - valid',i:'_abc_',e:true},
      {m:'"-abc-" - valid',i:'-abc-',e:true},
      {m:'"abc 123" - invalid',i:'abc 123',e:false},
      {m:'"a"b" - invalid',i:'a"b',e:false},
      {m:'"a\'b" - invalid',i:'a\'b',e:false}
    ];
    tests.forEach(function(test) {
      it(test.m + ' ', function () {
        expect(validator.isValidConfigKey(test.i)).to.equal(test.e);
      })
    });
  })
})