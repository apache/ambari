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
require('views/main/dashboard/widgets/text_widget');

describe('App.TextDashboardWidgetView', function() {

  var tests = [
    {
      data: 100,
      e: {
        isRed: false,
        isOrange: false,
        isGreen: true,
        isNA: false
      }
    },
    {
      data: 1,
      e: {
        isRed: true,
        isOrange: false,
        isGreen: false,
        isNA: false
      }
    },
    {
      data: 50,
      e: {
        isRed: false,
        isOrange: true,
        isGreen: false,
        isNA: false
      }
    },
    {
      data: null,
      e: {
        isRed: true,
        isOrange: false,
        isGreen: false,
        isNA: true
      }
    }
  ];

  tests.forEach(function(test) {
    describe('data - ' + test.data + ' | thresh1 - 40 | thresh2 - 70', function() {
      var textDashboardWidgetView = App.TextDashboardWidgetView.create({thresh1:40, thresh2:70});
      textDashboardWidgetView.set('data', test.data);
      it('isRed', function() {
        expect(textDashboardWidgetView.get('isRed')).to.equal(test.e.isRed);
      });
      it('isOrange', function() {
        expect(textDashboardWidgetView.get('isOrange')).to.equal(test.e.isOrange);
      });
      it('isGreen', function() {
        expect(textDashboardWidgetView.get('isGreen')).to.equal(test.e.isGreen);
      });
      it('isNA', function() {
        expect(textDashboardWidgetView.get('isNA')).to.equal(test.e.isNA);
      });
    });
  });

});
