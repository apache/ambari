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
var view;

describe('App.MainHostMenuView', function () {

  beforeEach(function () {
    view = App.MainHostMenuView.create({});
  });

  describe('#content', function () {

    afterEach(function () {
      App.get.restore();
    });

    Em.A([
        {
          stackVersionsAvailable: true,
          stackUpgrade: true,
          m: '`versions` is visible',
          e: false
        },
        {
          stackVersionsAvailable: true,
          stackUpgrade: false,
          m: '`versions` is invisible (1)',
          e: true
        },
        {
          stackVersionsAvailable: false,
          stackUpgrade: true,
          m: '`versions` is invisible (2)',
          e: true
        },
        {
          stackVersionsAvailable: false,
          stackUpgrade: false,
          m: '`versions` is invisible (3)',
          e: true
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          var stub = sinon.stub(App, 'get');
          stub.withArgs('stackVersionsAvailable').returns(test.stackVersionsAvailable);
          stub.withArgs('supports.stackUpgrade').returns(test.stackUpgrade);
          view.propertyDidChange('content');
          expect(view.get('content').findProperty('name', 'versions').get('hidden')).to.equal(test.e);
        });
      });

  });

});