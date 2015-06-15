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
require('views/common/widget/gauge_widget_view');

describe('App.GaugeWidgetView', function () {
  var view = App.GaugeWidgetView.create({
    value: 0,
    content: {
      properties: {
        warning_threshold: 0,
        critical_threshold: 0
      }
    }
  });

  describe("#chartView.contentColor()", function() {
    var testCases = [
      {
        title: 'both thresholds NOT existed',
        data: {
          value: 0.2,
          warningThreshold: null,
          criticalThreshold: null
        },
        result: App.healthStatusGreen
      },
      {
        title: 'both thresholds existed 1',
        data: {
          value: 0.2,
          warningThreshold: 0.1,
          criticalThreshold: 0.3
        },
        result: App.healthStatusOrange
      },
      {
        title: 'both thresholds existed 2',
        data: {
          value: 0.2,
          warningThreshold: 0.3,
          criticalThreshold: 0.1
        },
        result: App.healthStatusOrange
      },
      {
        title: 'both thresholds existed 3',
        data: {
          value: 0.05,
          warningThreshold: 0.1,
          criticalThreshold: 0.3
        },
        result: App.healthStatusGreen
      },
      {
        title: 'both thresholds existed 4',
        data: {
          value: 0.35,
          warningThreshold: 0.3,
          criticalThreshold: 0.1
        },
        result: App.healthStatusGreen
      },
      {
        title: 'both thresholds existed 5',
        data: {
          value: 0.35,
          warningThreshold: 0.1,
          criticalThreshold: 0.3
        },
        result: App.healthStatusRed
      },
      {
        title: 'both thresholds existed 6',
        data: {
          value: 0.05,
          warningThreshold: 0.3,
          criticalThreshold: 0.1
        },
        result: App.healthStatusRed
      },
      {
        title: 'only warning threshold existed 1',
        data: {
          value: 0,
          warningThreshold: 1,
          criticalThreshold: null
        },
        result: App.healthStatusGreen
      },
      {
        title: 'only warning threshold existed 2',
        data: {
          value: 2,
          warningThreshold: 1,
          criticalThreshold: null
        },
        result: App.healthStatusOrange
      },
      {
        title: 'only critical threshold existed 1',
        data: {
          value: 0.5,
          warningThreshold: null,
          criticalThreshold: 1
        },
        result: App.healthStatusGreen
      },
      {
        title: 'only critical threshold existed 2',
        data: {
          value: 1.5,
          warningThreshold: null,
          criticalThreshold: 1
        },
        result: App.healthStatusRed
      },
      {
        title: 'invalid thresholds 1',
        data: {
          value: 1.5,
          warningThreshold: '&*&%',
          criticalThreshold: 1
        },
        result: App.healthStatusRed
      },
      {
        title: 'invalid thresholds 2',
        data: {
          value: 1.5,
          warningThreshold: '&*&%',
          criticalThreshold: '@#^^'
        },
        result: App.healthStatusGreen
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        view.set('value', test.data.value);
        view.set('content.properties.warning_threshold', test.data.warningThreshold);
        view.set('content.properties.error_threshold', test.data.criticalThreshold);
        var chartView = view.chartView.create({
          parentView: view
        });
        expect(chartView.get('contentColor')).to.eql(test.result);
      });
    });
  });
});