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
var sort = require('views/common/sort_view');
require('utils/misc');
require('utils/string_utils');

describe('#wrapperView', function () {

  describe('#getSortFunc', function () {

    describe('number', function () {

      var property = Em.Object.create({type: 'number', name: 'lastTriggered'});

      Em.A([
          {
            a: Em.Object.create({lastTriggered: 1}),
            b: Em.Object.create({lastTriggered: 0}),
            order: true,
            e: 1
          },
          {
            a: Em.Object.create({lastTriggered: 1}),
            b: Em.Object.create({lastTriggered: 0}),
            order: false,
            e: -1
          },
          {
            a: Em.Object.create({lastTriggered: null}),
            b: Em.Object.create({lastTriggered: 1}),
            order: true,
            e: -Infinity
          },
          {
            a: Em.Object.create({lastTriggered: null}),
            b: Em.Object.create({lastTriggered: 1}),
            order: false,
            e: Infinity
          }
        ]).forEach(function (test, i) {
          it('test #' + (i + 1), function () {
            var func = sort.wrapperView.create().getSortFunc(property, test.order);
            expect(func(test.a, test.b)).to.equal(test.e);
          });
      });

    });

    describe('default', function () {

      var property = Em.Object.create({type: 'string', name: 'serviceName'});

      Em.A([
          {
            a: Em.Object.create({serviceName: 's1'}),
            b: Em.Object.create({serviceName: 's2'}),
            order: true,
            e: 1
          },
          {
            a: Em.Object.create({serviceName: 's1'}),
            b: Em.Object.create({serviceName: 's2'}),
            order: false,
            e: -1
          },
          {
            a: Em.Object.create({serviceName: 's1'}),
            b: Em.Object.create({serviceName: 's1'}),
            order: true,
            e: 0
          },
          {
            a: Em.Object.create({serviceName: null}),
            b: Em.Object.create({serviceName: 's2'}),
            order: true,
            e: 1
          },
          {
            a: Em.Object.create({serviceName: null}),
            b: Em.Object.create({serviceName: 's2'}),
            order: false,
            e: -1
          }
        ]).forEach(function (test, i) {
          it('test #' + (i + 1), function () {
            var func = sort.wrapperView.create().getSortFunc(property, test.order);
            expect(func(test.a, test.b)).to.equal(test.e);
          });
      });

      it('test non-string values', function () {
        property = Em.Object.create({type: 'string', name: 'enabled'});
        var func = sort.wrapperView.create().getSortFunc(property, true),
        a = Em.Object.create({enabled: false}),
        b = Em.Object.create({enabled: true});
        expect(func(a, b)).to.equal(1);
      });

    });

  });

});