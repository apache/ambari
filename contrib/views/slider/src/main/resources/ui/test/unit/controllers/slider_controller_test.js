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

moduleFor('controller:slider', 'App.SliderController', {

  setup: function () {
    App.setProperties({
      metricsHost: null,
      metricsPort: null,
      metricsLibPath: null
    });
    Ember.run(App, App.advanceReadiness);
  },

  teardown: function () {
    App.reset();
  }

});

var properties = [
  Em.Object.create({
    viewConfigName: 'site.global.metric_collector_lib',
    value: 'file:///usr/lib/ambari-metrics-hadoop-sink/ambari-metrics-hadoop-sink.jar'
  }),
  Em.Object.create({
    viewConfigName: 'site.global.metric_collector_host',
    value: 'h2'
  }),
  Em.Object.create({
    viewConfigName: 'site.global.metric_collector_port',
    value: '6188'
  })
];

test('getViewDisplayParametersSuccessCallback', function () {

  var sliderController = this.subject({});
  Em.run(function () {
    sliderController.getViewDisplayParametersSuccessCallback({
      "ViewInstanceInfo" : {
        "description" : "description s1",
        "label" : "display s1",
        "instance_data": {
          "java.home": "/usr/jdk64/jdk1.7.0_45",
          "slider.user": "admin"
        }
      }
    })
  });
  equal(App.get('label'), 'display s1', 'valid label is set');
  equal(App.get('description'), 'description s1', 'valid description is set');
  equal(App.get('javaHome'), '/usr/jdk64/jdk1.7.0_45', 'valid default java_home property is set');
  equal(App.get('sliderUser'), 'admin', 'valid sliderUser is set');

});

test('getParametersFromViewPropertiesSuccessCallback', function () {

  var controller = this.subject();

  Em.run(function () {
    sinon.stub(App.SliderApp.store, 'all').returns(properties);
    controller.getParametersFromViewPropertiesSuccessCallback({
      parameters: {
        'site.global.metric_collector_lib': 'file:///usr/lib/ambari-metrics-hadoop-sink/ambari-metrics-hadoop-sink.jar',
        'site.global.metric_collector_host': 'h2',
        'site.global.metric_collector_port': '6188'
      },
      validations: [{}, {}]
    });
    App.SliderApp.store.all.restore();
  });

  equal(App.get('metricsHost'), 'h2', 'should set metrics server host');
  equal(App.get('metricsPort'), '6188', 'should set metrics server port');
  equal(App.get('metricsLibPath'), 'file:///usr/lib/ambari-metrics-hadoop-sink/ambari-metrics-hadoop-sink.jar', 'should set metrics lib path');

});

 test('initMetricsServerProperties', function () {

   var controller = this.subject();

   Em.run(function () {
     sinon.stub(App.SliderApp.store, 'all').returns(properties);
     controller.initMetricsServerProperties();
     App.SliderApp.store.all.restore();
   });

   equal(App.get('metricsHost'), 'h2', 'should set metrics server host');
   equal(App.get('metricsPort'), '6188', 'should set metrics server port');
   equal(App.get('metricsLibPath'), 'file:///usr/lib/ambari-metrics-hadoop-sink/ambari-metrics-hadoop-sink.jar', 'should set metrics lib path');

 });

test('finishSliderConfiguration', function () {

  var cases = [
      {
        validations: [],
        viewEnabled: true,
        title: 'view enabled'
      },
      {
        validations: [{}, {}],
        viewEnabled: false,
        title: 'view disabled'
      }
    ],
    controller = this.subject();

  cases.forEach(function (item) {

    Em.run(function () {
      controller.finishSliderConfiguration({
        validations: item.validations
      });
    });

    equal(App.get('viewEnabled'), item.viewEnabled, item.title);

  });

});