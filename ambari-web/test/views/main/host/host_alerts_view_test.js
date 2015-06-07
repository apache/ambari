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
require('views/main/host/host_alerts_view');

var view;

describe('App.MainHostAlertsView', function () {

  beforeEach(function () {
    view = App.MainHostAlertsView.create({
      controller: Em.Object.create()
    });
  });

  describe('#content', function () {
    var cases = [
      {
        m: 'return empty array',
        c: null,
        r: []
      },
      {
        m: 'return empty array',
        c: undefined,
        r: []
      },
      {
        m: 'sort CRITICAL and WARNING to be first',
        c: [
            Em.Object.create({
              state: 'OK'
            }),
            Em.Object.create({
              state: 'WARNING'
            }),
            Em.Object.create({
              state: 'CRITICAL'
            }),
            Em.Object.create({
              state: 'OK'
            })
        ],
        r: [
          Em.Object.create({
            state: 'CRITICAL'
          }),
          Em.Object.create({
            state: 'WARNING'
          }),
          Em.Object.create({
            state: 'OK'
          }),
          Em.Object.create({
            state: 'OK'
          })
        ]
      },
      {
        m: 'sort CRITICAL and WARNING to be first',
        c: [
          Em.Object.create({
            state: 'OTHER'
          }),
          Em.Object.create({
            state: 'WARNING'
          }),
          Em.Object.create({
            state: 'OK'
          }),
          Em.Object.create({
            state: 'CRITICAL'
          })
        ],
        r: [
          Em.Object.create({
            state: 'CRITICAL'
          }),
          Em.Object.create({
            state: 'WARNING'
          }),
          Em.Object.create({
            state: 'OTHER'
          }),
          Em.Object.create({
            state: 'OK'
          })
        ]
      }
    ];

    cases.forEach(function(test){
      it('should ' + test.m, function () {
        view.set('controller.content', test.c);
        expect(view.get('content')).eql(test.r);
      });
    });

  });

});
