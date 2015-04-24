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

describe('App.TimeIntervalSpinnerView', function () {

  beforeEach(function () {
    view = App.TimeIntervalSpinnerView.create({
      controller: Em.Object.create({
        removeCurrentFromDependentList: Em.K
      }),
      initPopover: Em.K,
      setProperties: Em.K
    });
  });

  afterEach(function () {
    view.destroy();
  });

  describe('#generateWidgetValue', function () {

    var createProperty = function (widgetUnits, configPropertyUnits, incrementStep) {
      return Em.Object.create({
        stackConfigProperty: Em.Object.create({
          widget: {
            units: [
              { unit: widgetUnits }
            ]
          },
          valueAttributes: {
            unit: configPropertyUnits,
            increment_step: incrementStep
          }
        })
      });
    };

    var tests = [
      {
        input: 60000,
        config: createProperty("days,hours,minutes", "milliseconds", 1000),
        e: [
          { label: 'Days', value: 0, incrementStep: 1, enabled: true},
          { label: 'Hours', value: 0, incrementStep: 1, enabled: true},
          { label: 'Minutes', value: 1, incrementStep: 1, enabled: true}
        ]
      },
      {
        input: "2592000000",
        config: createProperty("days,hours,minutes", "milliseconds", 60000),
        e: [
          { label: 'Days', value: 30, incrementStep: 1, enabled: true},
          { label: 'Hours', value: 0, incrementStep: 1, enabled: true},
          { label: 'Minutes', value: 0, incrementStep: 1, enabled: true}
        ]
      },
      {
        input: "604800000",
        config: createProperty("days,hours,minutes", "milliseconds", 60000),
        e: [
          { label: 'Days', value: 7, incrementStep: 1, enabled: true},
          { label: 'Hours', value: 0, incrementStep: 1, enabled: true},
          { label: 'Minutes', value: 0, incrementStep: 1, enabled: true}
        ]
      },
      {
        input: "804820200",
        config: createProperty("days,hours,minutes", "milliseconds", 60000),
        e: [
          { label: 'Days', value: 9, incrementStep: 1, enabled: true},
          { label: 'Hours', value: 7, incrementStep: 1, enabled: true},
          { label: 'Minutes', value: 33, incrementStep: 1, enabled: true}
        ]
      },
      {
        input: "70000",
        config: createProperty("minutes", "milliseconds", 1000),
        e: [
          { label: 'Minutes', value: 1, incrementStep: 1, enabled: true}
        ]
      },
      {
        input: "140",
        config: createProperty("hours,minutes", "minutes", 1),
        e: [
          { label: 'Hours', value: 2, incrementStep: 1, enabled: true},
          { label: 'Minutes', value: 20, incrementStep: 1, enabled: true}
        ]
      },
      {
        input: "2",
        config: createProperty("hours", "hours", 1),
        e: [
          { label: 'Hours', value: 2, incrementStep: 1, enabled: true}
        ]
      }
    ];

    tests.forEach(function (test) {
      it('should convert {0} {1} to {2}'.format(test.input, test.config.get('stackConfigProperty.valueAttributes.unit'), JSON.stringify(test.e)), function () {
        view.set('config', test.config);
        var result = view.generateWidgetValue(test.input, test.inputType, test.desiredUnits).map(function (item) {
          // remove unnecessary keys
          return App.permit(item, ['label', 'value', 'enabled', 'incrementStep']);
        });
        expect(result).to.eql(test.e);
      });
    });

  });

  describe('#parseIncrement', function () {

    var createProperty = function (widgetUnits, configPropertyUnits, incrementStep, value) {
      return Em.Object.create({
        value: value,
        stackConfigProperty: Em.Object.create({
          widget: {
            units: [
              { unit: widgetUnits }
            ]
          },
          valueAttributes: {
            unit: configPropertyUnits,
            minimum: 1,
            maximum: 2,
            increment_step: incrementStep
          }
        })
      });
    };

    Em.A([
        {
          input: "120000",
          config: createProperty("minutes,seconds", "milliseconds", 10000, "120000"),
          e: [
            { label: 'Minutes', value: 2, incrementStep: 1, enabled: true},
            { label: 'Seconds', value: 0, incrementStep: 10, enabled: true}
          ]
        },
        {
          input: "120000",
          config: createProperty("minutes,seconds", "milliseconds", 60000, "120000"),
          e: [
            { label: 'Minutes', value: 2, incrementStep: 1, enabled: true},
            { label: 'Seconds', value: 0, incrementStep: 60, enabled: false}
          ]
        }
      ]).forEach(function (test) {
        it('should convert {0} {1} to {2}'.format(test.input, test.config.get('stackConfigProperty.valueAttributes.unit'), JSON.stringify(test.e)), function () {
          view.set('config', test.config);
          view.prepareContent();
          var result = view.get('content').map(function (c) {
            return App.permit(c, ['label', 'value', 'incrementStep', 'enabled']);
          });
          expect(result).to.eql(test.e);
        });
      });

  });

  describe('#checkErrors', function () {

    Em.A([
        {
          config: Em.Object.create({
            value: "540",
            stackConfigProperty: Em.Object.create({
              widget: {
                units: [
                  { unit: "hours,minutes" }
                ]
              },
              valueAttributes: {type: "int", maximum: "86400", minimum: "600", unit: "seconds"}
            })
          }),
          e: {
            warnMessage: Em.I18n.t('config.warnMessage.outOfBoundaries.less').format("10 Minutes"),
            warn: true
          }
        },
        {
          config: Em.Object.create({
            value: "86460",
            stackConfigProperty: Em.Object.create({
              widget: {
                units: [
                  { unit: "hours,minutes" }
                ]
              },
              valueAttributes: {type: "int", maximum: "86400", minimum: "600", unit: "seconds"}
            })
          }),
          e: {
            warnMessage: Em.I18n.t('config.warnMessage.outOfBoundaries.greater').format("24 Hours"),
            warn: true
          }
        },
        {
          config: Em.Object.create({
            value: "12000",
            stackConfigProperty: Em.Object.create({
              widget: {
                units: [
                  { unit: "hours,minutes" }
                ]
              },
              valueAttributes: {type: "int", maximum: "86400", minimum: "600", unit: "seconds"}
            })
          }),
          e: {
            warnMessage:'',
            warn: false
          }
        }
      ]).forEach(function (test) {
        it('', function () {
          view.set('config', test.config);
          view.prepareContent();
          view.checkErrors();
          expect(view.get('config.warnMessage')).to.equal(test.e.warnMessage);
          expect(view.get('config.warn')).to.equal(test.e.warn);
        });
      });

  });

});