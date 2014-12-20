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
require('views/main/dashboard/widget');

describe('App.DashboardWidgetView', function() {
  var dashboardWidgetView = App.DashboardWidgetView.create();

  describe('#viewID', function() {
    it('viewID is computed with id', function() {
      dashboardWidgetView.set('id', 5);
      expect(dashboardWidgetView.get('viewID')).to.equal('widget-5');
    });
  });

  describe('#hoverContentTopClass', function() {
    var tests = [
      {
        h: ['', ''],
        e: 'content-hidden-two-line',
        m: '2 lines'
      },
      {
        h: ['', '', ''],
        e: 'content-hidden-three-line',
        m: '3 lines'
      },
      {
        h: [''],
        e: '',
        m: '1 line'
      },
      {
        h: [],
        e: '',
        m: '0 lines'
      },
      {
        h: ['', '', '', '', ''],
        e: 'content-hidden-five-line',
        m: '5 lines'
      },
      {
        h: ['', '', '', ''],
        e: 'content-hidden-four-line',
        m: '4 lines'
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        dashboardWidgetView.set('hiddenInfo', test.h);
        expect(dashboardWidgetView.get('hoverContentTopClass')).to.equal(test.e);
      });
    });
  });

});
