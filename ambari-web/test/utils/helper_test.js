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
require('utils/helper');

describe('utils/helper', function() {
  describe('String helpers', function() {
    describe('#trim()', function(){
      it('should replace first space', function() {
        expect(' as d f'.trim()).to.eql('as d f');
      });
    });
    describe('#endsWith()', function() {
      it('`abcd` ends with `d`', function(){
        expect('abcd'.endsWith('d')).to.eql(true);
      });
      it('`abcd` doesn\'t end with `f`', function(){
        expect('abcd'.endsWith('f')).to.eql(false);
      });
    });
    describe('#contains()', function() {
      it('`abc` contains b', function(){
        expect('abc'.contains('b')).to.eql(true);
      });
      it('`abc` doesn\'t contain d', function() {
        expect('abc'.contains('d')).to.eql(false);
      });
    });
    describe('#capitalize()',function() {
      it('`abc d` should start with `A`', function() {
        expect('abc d'.capitalize()).to.eql('Abc d');
      });
    });
    describe('#findIn()', function(){
      var obj = {
        a: {
          a1: 'AVal1'
        },
        b: 'BVal',
        c: {
          c1: {
            c2: 'Cval2'
          },
          b: 'BVal'
        }
      };
      var testValue = function(key, value) {
        it('key `' + key + '` should have `' + JSON.stringify(value) + '` value', function() {
          expect(key.findIn(obj)).to.eql(value);
        });
      };
      it('expect return `null` on non-object input', function(){
        expect('a'.findIn('b')).to.null;
      });
      testValue('a', obj.a);
      testValue('c2', obj.c.c1.c2);
      testValue('b', obj.b);
      testValue('d', null);
    });
    describe('#format()', function(){
      it('should replace string correctly', function(){
        expect("{0} world{1}".format("Hello","!")).to.eql("Hello world!");
      });
    });
    describe('#highlight()', function() {
      var str = "Hello world! I want to highlight this word!";
      it('should highlight `word` with default template', function() {
        var result = str.highlight(['word']);
        expect(result).to.eql("Hello world! I want to highlight this <b>word</b>!");
      });
      it('should highlight `world` and `word` with template `<span class="yellow">{0}</span>`', function() {
        var result = str.highlight(["world", "word"], '<span class="yellow">{0}</span>');
        expect(result).to.eql('Hello <span class="yellow">world</span>! I want to highlight this <span class="yellow">word</span>!')
      });
      var str2 = "First word, second word";
      it('should highlight `word` multiply times with default template', function() {
        var result = str2.highlight(["word"]);
        expect(result).to.eql("First <b>word</b>, second <b>word</b>");
      });
    });
  });
  describe('Number helpers', function(){
    describe('#toDaysHoursMinutes()', function(){
      var time = 1000000000;
      var minute = 1000*60;
      var hour = 60*minute;
      var day = 24*hour;
      var result = time.toDaysHoursMinutes();
      var testDays = Math.floor(time/day);
      it('should correct convert days', function(){
        expect(testDays).to.eql(result.d);
      });
      it('should correct convert hours', function(){
        expect(Math.floor((time - testDays * day)/hour)).to.eql(result.h);
      });
      it('should correct convert minutes', function(){
        expect(((time - Math.floor((time - testDays*day)/hour)*hour - testDays*day)/minute).toFixed(2)).to.eql(result.m);
      });
    });
  });
  describe('Array helpers', function(){
    describe('#sortPropertyLight()', function(){
      var testable = [
        { a: 2 },
        { a: 1 },
        { a: 6},
        { a: 64},
        { a: 3},
        { a: 3}
      ];
      var result = testable.sortPropertyLight('a');
      it('should return array with same length', function(){
        expect(testable.length).to.eql(result.length);
      });
      it('should sort array', function() {
        result.forEach(function(resultObj, index, resultArr) {
          if (index > resultArr.length - 1)
            expect(resultObj.a < resultArr[index + 1].a).to.eql(false);
        });
      });
      it('should try to sort without throwing exception', function(){
        expect(testable.sortPropertyLight(['a'])).to.ok;
      });
    });
  });
  describe('App helpers', function(){
    var appendDiv = function() {
      $('body').append('<div id="tooltip-test"></div>');
    };
    var removeDiv = function() {
      $('body').remove('#tooltip-test');
    };
    describe('#isEmptyObject', function(){
      it('should return true on empty object', function() {
        expect(App.isEmptyObject({})).to.eql(true);
      });
      it('should return false on non-empty object', function() {
        expect(App.isEmptyObject({ a: 1 })).to.eql(false);
      });
    });
    describe('#parseJSON()', function(){
      var testable = '{"hello": "world"}';
      expect(App.parseJSON(testable).hello).to.eql('world');
    });
    describe('#tooltip()', function() {
      beforeEach(appendDiv);
      afterEach(removeDiv);
      it('should add tooltip', function() {
        var tooltip = App.tooltip($('#tooltip-test'));
        expect($('#tooltip-test').data('tooltip').enabled).to.eql(true);
      });
    });
    describe('#popover()', function() {
      beforeEach(appendDiv);
      afterEach(removeDiv);
      it('should add popover', function() {
        var tooltip = App.popover($('#tooltip-test'));
        expect($('#tooltip-test').data('popover').enabled).to.eql(true);
      });
    });
    describe('#App.format', function(){
      describe('#commandDetail()', function() {
        var command = "GANGLIA_MONITOR STOP";
        var ignored = "DECOMMISSION, NAMENODE";
        var removeString = "SERVICE/HDFS STOP";
        var nagiosState = "nagios_update_ignore ACTIONEXECUTE";
        it('should convert command to readable info', function() {
          expect(App.format.commandDetail(command)).to.eql(' Ganglia Monitor Stop');
        });
        it('should ignore decommission command', function(){
          expect(App.format.commandDetail(ignored)).to.eql('  NameNode');
        });
        it('should remove SERVICE string from command', function(){
          expect(App.format.commandDetail(removeString)).to.eql(' HDFS Stop');
        });
        it('should return maintenance message', function() {
          expect(App.format.commandDetail(nagiosState)).to.eql(' Toggle Maintenance Mode');
        });
      });
      describe('#taskStatus()', function(){
        var testable = [
          { status: 'PENDING', expectable: 'pending'},
          { status: 'QUEUED', expectable: 'queued'},
          { status: 'COMPLETED', expectable: 'completed'}
        ];
        testable.forEach(function(testObj){
          it('should convert `' + testObj.status + '` to `' + testObj.expectable + '`', function(){
            expect(App.format.taskStatus(testObj.status)).to.eql(testObj.expectable);
          });
        });
      });
    });
  });
});
