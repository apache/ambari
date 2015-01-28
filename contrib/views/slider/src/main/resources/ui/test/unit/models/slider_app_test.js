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

moduleForModel('sliderApp', 'App.SliderApp', {

  needs: [
    'model:sliderAppType',
    'model:sliderAppComponent',
    'model:quickLink',
    'model:sliderAppAlert',
    'model:typedProperty'
  ],

  setup: function () {
    App.set('metricsHost', null);
  },

  teardown: function () {
    App.set('metricsHost', null);
  }

});

test('doNotShowComponentsAndAlerts', function () {

  var sliderApp = this.subject({name: 'p1', status: 'FROZEN'});

  equal(sliderApp.get('doNotShowComponentsAndAlerts'), true, 'Should be true if status is FROZEN');

  Em.run(function () {
    sliderApp.set('status', 'FAILED');
  });
  equal(sliderApp.get('doNotShowComponentsAndAlerts'), true, 'Should be true if status is FAILED');

});


test('showMetrics', function () {

  var sliderApp = this.subject({name: 'p1', configs: {}, supportedMetricNames: ''});
  equal(sliderApp.get('showMetrics'), false, 'should be false if supportedMetricNames is not provided');

  Em.run(function () {
    App.set('metricsHost', 'some_host');
    sliderApp.set('supportedMetricNames', 'some');
  });
  equal(sliderApp.get('showMetrics'), true, 'should be true if App.metricsHost is provided');

  Em.run(function () {
    App.set('metricsHost', null);
    sliderApp.set('status', App.SliderApp.Status.running);
  });
  equal(sliderApp.get('showMetrics'), true, 'should be true if status is RUNNING');

});

test('mapObject', function () {
  var sliderApp = this.subject(),
    longString = new Array(102).join('1'),
    configs = {
      n1: 'v1',
      n2: 'v2',
      n3: 'v3\nv3',
      n4: longString
    },
    expected = [
      {key: 'n1', value: 'v1', isMultiline: false},
      {key: 'n2', value: 'v2', isMultiline: false},
      {key: 'n3', value: 'v3\nv3', isMultiline: true},
      {key: 'n4', value: longString, isMultiline: true}
    ],
    result;


  Em.run(function() {
    result = sliderApp.mapObject(configs);
  });
  deepEqual(result, expected, 'should map configs to array');

});
