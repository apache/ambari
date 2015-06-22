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

moduleFor('controller:createAppWizardStep4', 'App.CreateAppWizardStep4Controller', {

  needs: [
    'controller:createAppWizard'
  ],

  setup: function () {
    sinon.stub(App.ajax, 'send', Em.K);
  },

  teardown: function () {
    App.ajax.send.restore();
    App.reset();
  }

});

test('appWizardController', function () {

  expect(1);

  var controller = this.subject({
    controllers: {
      createAppWizard: {
        key: 'value0'
      }
    }
  });

  Em.run(function () {
    controller.set('controllers.createAppWizard.key', 'value1');
  });

  equal(controller.get('appWizardController.key'), 'value1', 'should link to App.CreateAppWizardController');

});

test('isSubmitDisabled', function () {

  expect(3);

  var controller = this.subject({
    newApp: Em.Object.create({
      appType: {
        index: 'test',
        version: 'test'
      },
      name: test,
      configs: [],
      components: [],
      queue: 'test',
      queueName: 'test'
    })
  });

  equal(controller.get('isSubmitDisabled'), false, 'should be false by default');

  controller.sendAppDataToServer();
  equal(controller.get('isSubmitDisabled'), true, 'should be true after sendAppDataToServer call');

  controller.sendAppDataToServerCompleteCallback();
  equal(controller.get('isSubmitDisabled'), false, 'should be false after sendAppDataToServerCompleteCallback call');

});

test('configsFormatted', function () {

  expect(1);

  var controller = this.subject();

  Em.run(function () {
    controller.set('newApp', {
      configs: {
        p0: 'v0',
        p1: 'v1',
        p2: 'v2'
      }
    });
  });

  equal(controller.get('configsFormatted'), '"p0":"v0",\n"p1":"v1",\n"p2":"v2"', 'configs formatted correctly');

});

test('resourcesFormatted', function () {

  expect(18);

  var propertiesCases = [
      {
        propertyName: 'numInstances',
        expectedPropertyName: 'instanceCount',
        value: '1'
      },
      {
        propertyName: 'yarnMemory',
        expectedPropertyName: 'yarnMemory',
        value: '256'
      },
      {
        propertyName: 'yarnCPU',
        expectedPropertyName: 'yarnCpuCores',
        value: '2'
      },
      {
        propertyName: 'priority',
        expectedPropertyName: 'priority',
        value: 2
      }
    ],
    globalCases = [
      {
        includeFilePatterns: null,
        includeFilePatternsExpected: null,
        excludeFilePatterns: null,
        excludeFilePatternsExpected: null,
        frequency: '1000',
        frequencyExpected: '1000',
        title: 'all parameters except one are null'
      },
      {
        includeFilePatterns: '*.log',
        includeFilePatternsExpected: '*.log',
        excludeFilePatterns: '*.zip',
        excludeFilePatternsExpected: '*.zip',
        frequency: '1000',
        frequencyExpected: '1000',
        title: 'all parameters are valid'
      }
    ],
    globalCasesUndefined = [
      {
        includeFilePatterns: null,
        excludeFilePatterns: null,
        frequency: null,
        title: 'no patterns and frequency specified'
      },
      {
        includeFilePatterns: ' \r\n',
        excludeFilePatterns: null,
        frequency: null,
        title: 'one parameter is empty string after trimming'
      }
    ],
    selectedYarnLabelCases = [
      {
        selectedYarnLabel: 0,
        components: []
      },
      {
        selectedYarnLabel: 1,
        yarnLabelExpression: ''
      },
      {
        selectedYarnLabel: 2,
        specialLabel: 'specialLabel',
        yarnLabelExpression: 'specialLabel'
      }
    ],
    title = '{0} should be {1}',
    selectedYarnLabelTitle = 'selected YARN label is {0}',
    label = 'label',
    controller = this.subject({
      newApp: Em.Object.create({
        components: [
          Em.Object.create({
            name: 'c',
            numInstances: '0',
            yarnMemory: '512',
            yarnCPU: '1',
            priority: 1
          })
        ]
      })
    });

  propertiesCases.forEach(function (item) {

    Em.run(function () {
      controller.get('newApp.components')[0].set(item.propertyName, item.value);
    });

    equal(controller.get('resourcesFormatted.components')[0][item.expectedPropertyName], item.value, title.format(item.expectedPropertyName, item.value));

  });

  Em.run(function () {
    controller.get('newApp.components')[0].setProperties({
      yarnLabelChecked: false,
      yarnLabel: label
    });
  });

  ok(!controller.get('resourcesFormatted.components')[0].yarnLabel, 'yarnLabel shouldn\'t be set');

  Em.run(function () {
    controller.get('newApp.components')[0].set('yarnLabelChecked', true);
  });

  equal(controller.get('resourcesFormatted.components')[0].yarnLabel, label, title.format('yarnLabel', '\'' + label + '\''));

  Em.run(function () {
    controller.get('newApp.components')[0].set('yarnLabel', ' ' + label + '\n');
  });

  equal(controller.get('resourcesFormatted.components')[0].yarnLabel, label, 'yarnLabel should be trimmed');

  globalCases.forEach(function (item) {

    Em.run(function () {
      controller.get('newApp').setProperties({
        includeFilePatterns: item.includeFilePatterns,
        excludeFilePatterns: item.excludeFilePatterns,
        frequency: item.frequency
      });
      controller.notifyPropertyChange('newApp.components.@each.numInstances');
    });

    var global = controller.get('resourcesFormatted.global');

    deepEqual(global['yarn.log.include.patterns'], item.includeFilePatternsExpected, item.title);
    deepEqual(global['yarn.log.exclude.patterns'], item.excludeFilePatternsExpected, item.title);
    deepEqual(global['yarn.log.interval'], item.frequencyExpected, item.title);

  });

  globalCasesUndefined.forEach(function (item) {

    Em.run(function () {
      controller.get('newApp').setProperties({
        includeFilePatterns: item.includeFilePatterns,
        excludeFilePatterns: item.excludeFilePatterns,
        frequency: item.frequency
      });
      controller.notifyPropertyChange('newApp.components.@each.numInstances');
    });

    equal(typeof controller.get('resourcesFormatted.global'), 'undefined', item.title);

  });

  selectedYarnLabelCases.forEach(function (item) {

    Em.run(function () {
      controller.get('newApp').setProperties({
        selectedYarnLabel: item.selectedYarnLabel,
        specialLabel: item.specialLabel
      });
      controller.notifyPropertyChange('newApp.components.@each.numInstances');
    });

    var message = selectedYarnLabelTitle.format(item.selectedYarnLabel);

    if (Em.isNone(item.yarnLabelExpression)) {
      ok(!controller.get('resourcesFormatted.components').isAny('id', 'slider-appmaster'), message);
    } else {
      equal(controller.get('resourcesFormatted.components').findBy('id', 'slider-appmaster')['yarn.label.expression'],
        item.yarnLabelExpression, message);
    }

  });

});

test('loadStep', function () {

  expect(1);

  var controller = this.subject();

  Em.run(function () {
    sinon.stub(controller, 'initializeNewApp', Em.K);
    controller.loadStep();
  });

  ok(controller.initializeNewApp.calledOnce, 'initializeNewApp should be executed');

  controller.initializeNewApp.restore();

});

test('initializeNewApp', function () {

  expect(1);

  var newApp = {
      key: 'value'
    },
    controller = this.subject({
      controllers: {
        createAppWizard: {
          newApp: newApp
        }
      }
    });

  Em.run(function () {
    controller.loadStep();
  });

  propEqual(controller.get('newApp'), newApp, 'should initialize new app');

});

test('sendAppDataToServerSuccessCallback', function () {

  expect(1);

  var controller = this.subject();

  Em.run(function () {
    sinon.stub(controller.get('appWizardController'), 'hidePopup', Em.K);
    controller.sendAppDataToServerSuccessCallback();
  });

  ok(controller.get('appWizardController').hidePopup.calledOnce, 'popup should be closed');

  controller.get('appWizardController').hidePopup.restore();

});

test('sendAppDataToServerCompleteCallback', function () {

  expect(1);

  var controller = this.subject({
    isSubmitDisabled: true
  });

  Em.run(function () {
    controller.sendAppDataToServerCompleteCallback();
  });

  ok(!controller.get('isSubmitDisabled'), 'Finish button should be enabled');

});

test('sendAppDataToServer', function () {

  var controller = this.subject({
      newApp: Em.Object.create({
        appType: {
          index: 'ACCUMULO',
          version: '1'
        },
        name: 'name',
        twoWaySSLEnabled: false,
        configs: {
          key: 'value'
        }
      }),
      resourcesFormatted: {
        components: []
      }
    }),
    cases = [
      {
        queueName: null,
        title: 'queueName not set'
      },
      {
        queueName: ' \n',
        title: 'empty queueName value'
      },
      {
        queueName: ' queue\n',
        queue: 'queue',
        title: 'queueName set correctly'
      }
    ];

  Em.run(function () {
    controller.sendAppDataToServer();
  });

  ok(controller.get('isSubmitDisabled'), 'Finish button should be disabled');
  ok(App.ajax.send.calledOnce, 'request to server should be sent');

  cases.forEach(function (item) {

    Em.run(function () {
      controller.set('newApp.queueName', item.queueName);
      controller.sendAppDataToServer();
    });

    var data = {
      typeName: 'ACCUMULO',
      typeVersion: '1',
      name: 'name',
      twoWaySSLEnabled: 'false',
      resources: {
        components: []
      },
      typeConfigs: {
        key: 'value'
      }
    };
    if (item.queue) {
      data.queue = item.queue;
    }

    propEqual(App.ajax.send.lastCall.args[0].data.data, data, item.title);

  });

});

test('actions.finish', function () {

  var controller = this.subject();

  Em.run(function () {
    sinon.stub(controller, 'sendAppDataToServer', Em.K);
    controller.send('finish');
  });

  ok(controller.sendAppDataToServer.calledOnce, 'data should be sent to server');

  controller.sendAppDataToServer.restore();

});