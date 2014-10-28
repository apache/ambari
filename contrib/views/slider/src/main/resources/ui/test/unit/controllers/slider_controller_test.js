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
      gangliaHost: null,
      gangliaClusters: null
    });
    Ember.run(App, App.advanceReadiness);
  },

  teardown: function () {
    App.reset();
  }

});

var properties = [
  Em.Object.create({
    viewConfigName: 'ganglia.additional.clusters',
    value: 'h0:8080,h1:3333'
  }),
  Em.Object.create({
    viewConfigName: 'ganglia.server.hostname',
    value: 'h2'
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

  var storeAll = App.SliderApp.store.all,
    controller = this.subject();

  Em.run(function () {
    App.SliderApp.store.all = function () {
      return properties;
    };
    controller.getParametersFromViewPropertiesSuccessCallback({
      parameters: {
        'ganglia.additional.clusters': 'h0:8080,h1:3333',
        'ganglia.server.hostname': 'h2'
      },
      validations: [{}, {}]
    });
  });

  deepEqual(App.get('gangliaClusters'), [
    {
      name: 'h0',
      port: '8080'
    },
    {
      name: 'h1',
      port: '3333'
    }
  ], 'should set gangliaClusters');
  equal(App.get('gangliaHost'), 'h2', 'should set gangliaHost');

  App.SliderApp.store.set('all', storeAll);

});

 test('initGangliaProperties', function () {

   var storeAll = App.SliderApp.store.all,
     controller = this.subject();

   Em.run(function () {
     App.SliderApp.store.all = function () {
       return properties;
     };
     controller.initGangliaProperties();
   });

   deepEqual(App.get('gangliaClusters'), [
     {
       name: 'h0',
       port: '8080'
     },
     {
       name: 'h1',
       port: '3333'
     }
   ], 'should set gangliaClusters');
   equal(App.get('gangliaHost'), 'h2', 'should set gangliaHost');

   App.SliderApp.store.set('all', storeAll);

 });

test('formatGangliaClusters', function () {

  var cases = [
      {
        prop: null,
        gangliaCustomClusters: [],
        title: 'empty value'
      },
      {
        prop: 'h0',
        gangliaCustomClusters: [],
        title: 'no port specified'
      },
      {
        prop: 'h1:8080,h2:3333',
        gangliaCustomClusters: [
          {
            name: 'h1',
            port: '8080'
          },
          {
            name: 'h2',
            port: '3333'
          }
        ],
        title: 'two items with ports'
      },
      {
        prop: '\'h3\':8080',
        gangliaCustomClusters: [
          {
            name: 'h3',
            port: '8080'
          }
        ],
        title: 'remove apostrophes'
      }
    ],
    controller = this.subject();

  cases.forEach(function (item) {

    deepEqual(controller.formatGangliaClusters(item.prop), item.gangliaCustomClusters, item.title);

  });

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