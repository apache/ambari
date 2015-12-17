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
require('views/main/charts/heatmap/heatmap_host');

describe('App.MainChartsHeatmapHostView', function() {

  var view = App.MainChartsHeatmapHostView.create({
    templateName: '',
    controller: Em.Object.create(),
    content: {}
  });

  describe('#hostTemperatureStyle', function () {
    var testCases = [
      {
        title: 'if hostToSlotMap is null then hostTemperatureStyle should be empty',
        hostName: 'host',
        controller: Em.Object.create({
          hostToSlotMap: null,
          selectedMetric: {
            slotDefinitions: []
          }
        }),
        result: ''
      },
      {
        title: 'if hostName is null then hostTemperatureStyle should be empty',
        hostName: '',
        controller: Em.Object.create({
          hostToSlotMap: {},
          selectedMetric: {
            slotDefinitions: []
          }
        }),
        result: ''
      },
      {
        title: 'if slot less than 0 then hostTemperatureStyle should be empty',
        hostName: 'host1',
        controller: Em.Object.create({
          hostToSlotMap: {
            "host1": -1
          },
          selectedMetric: {
            slotDefinitions: []
          }
        }),
        result: ''
      },
      {
        title: 'if slotDefinitions is null then hostTemperatureStyle should be empty',
        hostName: 'host1',
        controller: Em.Object.create({
          hostToSlotMap: {
            "host1": 1
          },
          selectedMetric: {
            slotDefinitions: null
          }
        }),
        result: ''
      },
      {
        title: 'if slotDefinitions length not more than slot number then hostTemperatureStyle should be empty',
        hostName: 'host1',
        controller: Em.Object.create({
          hostToSlotMap: {
            "host1": 1
          },
          selectedMetric: {
            slotDefinitions: [{}]
          }
        }),
        result: ''
      },
      {
        title: 'if slotDefinitions correct then hostTemperatureStyle should be "style1"',
        hostName: 'host1',
        controller: Em.Object.create({
          hostToSlotMap: {
            "host1": 1
          },
          selectedMetric: {
            slotDefinitions: [
              Em.Object.create({cssStyle: 'style0'}),
              Em.Object.create({cssStyle: 'style1'})
            ]
          }
        }),
        result: 'style1'
      }
    ];
    testCases.forEach(function (test) {
      it(test.title, function () {
        view.set('content.hostName', test.hostName);
        view.set('controller', test.controller);
        expect(view.get('hostTemperatureStyle')).to.equal(test.result);
      });
    });
  });

});
