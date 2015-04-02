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

describe('App.TimeIntervalSpinnerView', function() {
  describe('#generateWidgetValue', function(){
    beforeEach(function() {
      this.view = App.TimeIntervalSpinnerView.create({
        initPopover: Em.K
      });
    });

    afterEach(function() {
      this.view.destroy();
      this.view = null;
    });

    var createProperty = function(widgetUnits, configPropertyUnits) {
      return Em.Object.create({
        stackConfigProperty: Em.Object.create({
          widget: {
            units: [
              { unit: widgetUnits }
            ]
          },
          valueAttributes: { unit: configPropertyUnits }
        })
      });
    };
    var tests = [
      {
        input: 60000,
        config: createProperty("days,hours,minutes", "milliseconds"),
        e: [
          { label: 'Days', value: 0},
          { label: 'Hours', value: 0},
          { label: 'Minutes', value: 1}
        ]
      },
      {
        input: "2592000000",
        config: createProperty("days,hours,minutes", "milliseconds"),
        e: [
          { label: 'Days', value: 30},
          { label: 'Hours', value: 0},
          { label: 'Minutes', value: 0}
        ]
      },
      {
        input: "604800000",
        config: createProperty("days,hours,minutes", "milliseconds"),
        e: [
          { label: 'Days', value: 7},
          { label: 'Hours', value: 0},
          { label: 'Minutes', value: 0}
        ]
      },
      {
        input: "804820200",
        config: createProperty("days,hours,minutes", "milliseconds"),
        e: [
          { label: 'Days', value: 9},
          { label: 'Hours', value: 7},
          { label: 'Minutes', value: 33}
        ]
      },
      {
        input: "70000",
        config: createProperty("minutes", "milliseconds"),
        e: [
          { label: 'Minutes', value: 1}
        ]
      },
      {
        input: "140",
        config: createProperty("hours,minutes", "minutes"),
        e: [
          { label: 'Hours', value: 2},
          { label: 'Minutes', value: 20}
        ]
      },
      {
        input: "2",
        config: createProperty("hours", "hours"),
        e: [
          { label: 'Hours', value: 2}
        ]
      }
    ];

    tests.forEach(function(test) {
      it('should convert {0} {1} to {2}'.format(test.input, test.config.get('stackConfigProperty.valueAttributes.unit'), JSON.stringify(test.e)), function() {
        this.view.set('config', test.config);
        var result = this.view.generateWidgetValue(test.input, test.inputType, test.desiredUnits).map(function(item) {
          // remove unneccessary keys
          return App.permit(item, ['label', 'value']);
        });
        expect(result).to.eql(test.e);
      });
    });
  });
});
