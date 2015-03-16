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

moduleFor('controller:createAppWizardStep3', 'App.CreateAppWizardStep3Controller', {

  needs: [
    'controller:createAppWizard'
  ],

  teardown: function () {
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

test('newAppConfigs', function () {

  expect(1);

  var configs = {
      java_home: '/usr/jdk64/jdk1.7.0_40'
    },
    controller = this.subject();

  Em.run(function () {
    controller.set('controllers.createAppWizard.newApp', {
      configs: configs
    });
  });

  propEqual(controller.get('newAppConfigs'), configs, 'configs should be taken from wizard controller');

});

test('sectionKeys', function () {

  expect(2);

  var cases = [
      {
        sectionKeys: ['general', 'custom'],
        title: 'no newAppConfigs set'
      },
      {
        newAppConfigs: {
          'p0': 'v0',
          'site.p1': 'v1',
          'site.p2.0': 'v2',
          'site.p2.1': 'v3'
        },
        sectionKeys: ['general', 'p1', 'p2', 'custom'],
        title: 'newAppConfigs are set'
      }
    ],
    controller = this.subject();

  cases.forEach(function (item) {

    Em.run(function () {
      controller.set('controllers.createAppWizard.newApp', {
        configs: item.newAppConfigs
      });
    });

    propEqual(controller.get('sectionKeys'), item.sectionKeys, item.title);

  });

});

test('loadStep', function () {

  expect(3);

  var controller = this.subject();

  Em.run(function () {
    sinon.stub(controller, 'clearStep', Em.K);
    sinon.stub(controller, 'initConfigs', Em.K);
    controller.loadStep();
  });

  ok(controller.clearStep.calledOnce, 'clearStep should be executed');
  ok(controller.initConfigs.calledOnce, 'initConfigs should be executed');
  ok(controller.initConfigs.firstCall.args[0], 'true should be passed as argument to initConfigs');

  controller.clearStep.restore();
  controller.initConfigs.restore();

});

test('initConfigs', function () {

  expect(15);

  var controller = this.subject(),
    titleDefault = 'should set default {0} property value',
    titleCustom = 'should set custom {0} property value',
    titleGlobal = 'should set global {0} property value',
    titleLabel = 'label shouldn\'t contain \'site.\'';

  Em.run(function () {
    App.setProperties({
      javaHome: '/usr/jdk64/jdk1.7.0_45',
      metricsHost: 'host0',
      metricsPort: '3333',
      metricsLibPath: '/metrics/lib'
    });
    controller.get('controllers.createAppWizard').setProperties({
      'content': {},
      'newApp': {
        'appType': {
          'configs': {}
        },
        'configs': {
          'java_home': '/usr/jdk64/jdk1.7.0_40',
          'site.global.metric_collector_host': 'host1',
          'site.global.metric_collector_port': '8080',
          'site.global.metric_collector_lib': '/ams/lib'
        }
      }
    });
    controller.initConfigs(true);
  });

  equal(controller.get('configs').findBy('name', 'java_home').get('value'), '/usr/jdk64/jdk1.7.0_45', titleGlobal.format('java_home'));
  equal(controller.get('configs').findBy('name', 'java_home').get('label'), 'java_home', 'label should be equal to name');
  equal(controller.get('configs').findBy('name', 'ams_metrics').configs.findBy('name', 'site.global.metric_collector_host')
    .get('value'), 'host0', titleGlobal.format('site.global.metric_collector_host'));
  equal(controller.get('configs').findBy('name', 'ams_metrics').configs.findBy('name', 'site.global.metric_collector_port')
    .get('value'), '3333', titleGlobal.format('site.global.metric_collector_port'));
  equal(controller.get('configs').findBy('name', 'ams_metrics').configs.findBy('name', 'site.global.metric_collector_lib')
    .get('value'), '/metrics/lib', titleGlobal.format('site.global.metric_collector_lib'));
  equal(controller.get('configs').findBy('name', 'ams_metrics').configs.findBy('name', 'site.global.metric_collector_host')
    .get('label'), 'global.metric_collector_host', titleLabel);
  equal(controller.get('configs').findBy('name', 'ams_metrics').configs.findBy('name', 'site.global.metric_collector_port')
    .get('label'), 'global.metric_collector_port', titleLabel);
  equal(controller.get('configs').findBy('name', 'ams_metrics').configs.findBy('name', 'site.global.metric_collector_lib')
    .get('label'), 'global.metric_collector_lib', titleLabel);

  Em.run(function () {
    App.setProperties({
      javaHome: null,
      metricsHost: null,
      metricsPort: null,
      metricsLibPath: null
    });
    controller.initConfigs();
  });

  equal(controller.get('configs').findBy('name', 'java_home').get('value'), '/usr/jdk64/jdk1.7.0_40', titleCustom.format('java_home'));
  equal(controller.get('configs').findBy('name', 'ams_metrics').configs.findBy('name', 'site.global.metric_collector_host')
    .get('value'), 'host1', titleGlobal.format('site.global.metric_collector_host'));
  equal(controller.get('configs').findBy('name', 'ams_metrics').configs.findBy('name', 'site.global.metric_collector_port')
    .get('value'), '8080', titleGlobal.format('site.global.metric_collector_port'));
  equal(controller.get('configs').findBy('name', 'ams_metrics').configs.findBy('name', 'site.global.metric_collector_lib')
    .get('value'), '/ams/lib', titleGlobal.format('site.global.metric_collector_lib'));

  Em.run(function () {
    controller.get('controllers.createAppWizard').setProperties({
      'newApp': {
        'appType': {
          'configs':  {
            'site.global.metric_collector_host': 'host3',
            'site.global.metric_collector_port': '8888',
            'site.global.metric_collector_lib': '/var/ams/lib'
          }
        },
        'configs': {}
      }
    });
    controller.initConfigs();
  });

  equal(controller.get('configs').findBy('name', 'ams_metrics').configs.findBy('name', 'site.global.metric_collector_host')
    .get('value'), 'host3', titleDefault.format('site.global.metric_collector_host'));
  equal(controller.get('configs').findBy('name', 'ams_metrics').configs.findBy('name', 'site.global.metric_collector_port')
    .get('value'), '8888', titleDefault.format('site.global.metric_collector_port'));
  equal(controller.get('configs').findBy('name', 'ams_metrics').configs.findBy('name', 'site.global.metric_collector_lib')
    .get('value'), '/var/ams/lib', titleDefault.format('site.global.metric_collector_lib'));

});

test('initConfigSetDependencies', function () {

  expect(1);

  var configSet = {
      dependencies: [
        {
          name: 'App.javaHome'
        }
      ]
    },
    javaHome = '/usr/jdk64/jdk1.7.0_40',
    controller = this.subject();

  Em.run(function () {
    App.set('javaHome', javaHome);
    controller.initConfigSetDependencies(configSet);
  });

  equal(configSet.dependencies[0].map, javaHome, 'should set map property');

});

test('clearStep', function () {

  expect(1);

  var controller = this.subject();

  Em.run(function () {
    controller.clearStep();
  });

  ok(!controller.get('isError'), 'isError should be false');

});

test('validateConfigs', function () {

  expect(4);

  var controller = this.subject({
    configs: [
      {
        isSet: false,
        name: 'p0',
        value: 'v0'
      }
    ]
  });

  ok(controller.validateConfigs(), 'configs are valid');
  propEqual(controller.get('configsObject'), {
    p0: 'v0'
  }, 'configsObject is set');

  Em.run(function () {
    controller.set('addConfigSetProperties', function () {
      return null;
    });
  });

  ok(!controller.validateConfigs(), 'configs are invalid');
  ok(controller.get('isError'), 'isError is set to true');

});

test('addConfigSetProperties', function () {

  expect(1);

  var configs = [
      {
        isSet: false,
        configs: [
          {
            name: 'p0',
            value: 'v0'
          }
        ]
      },
      {
        isSet: true,
        trigger: {},
        configs: [
          {
            name: 'p1',
            value: 'v1'
          }
        ]
      },
      {
        isSet: true,
        trigger: {
          name: 'p2',
          value: 'v2'
        },
        configs: [
          {
            name: 'p3',
            value: 'v3'
          }
        ]
      }
    ],
    controller = this.subject();

    deepEqual(controller.addConfigSetProperties(configs), [configs[0], configs[2].configs[0]], 'should add config from config sets to general configs array');

});

test('saveConfigs', function () {

  expect(5);

  var configsObject = {
      p0: 'v0'
    },
    controller = this.subject({
      configsObject: configsObject
    }),
    metricsCases = [
      {
        configsObject: {
          'site.global.metrics_enabled': null
        }
      },
      {
        configsObject: {
          'site.global.metrics_enabled': 'true',
          'site.global.metric_collector_host': 'h0',
          'site.global.metric_collector_port': '3333'
        },
        metricsEnabledExpected: 'true'
      },
      {
        configsObject: {
          'site.global.metrics_enabled': 'true',
          'site.global.metric_collector_host': null,
          'site.global.metric_collector_port': '8080'
        },
        metricsEnabledExpected: 'false'
      },
      {
        configsObject: {
          'site.global.metrics_enabled': 'true',
          'site.global.metric_collector_host': 'h1',
          'site.global.metric_collector_port': null
        },
        metricsEnabledExpected: 'false'
      }
    ],
    metricsTitle = 'site.global.metrics_enabled should be {0}';

  Em.run(function () {
    controller.set('controllers.createAppWizard.newApp', {});
    controller.saveConfigs();
  });

  propEqual(controller.get('controllers.createAppWizard.newApp.configs'), configsObject, 'configs are saved to wizard controller');

  metricsCases.forEach(function (item) {
    controller.reopen({
      configsObject: item.configsObject
    });
    controller.set('controllers.createAppWizard.newApp', {});
    controller.saveConfigs();
    equal(controller.get('controllers.createAppWizard.newApp.configs')['site.global.metrics_enabled'],
      item.metricsEnabledExpected, metricsTitle.format(item.metricsEnabledExpected || 'undefined'));
  }, this);

});

test('actions.submit', function () {

  expect(2);

  var configsObject = {
      p0: 'v0'
    },
    controller = this.subject({
      configs: [
        {
          isSet: false,
          name: 'p0',
          value: 'v0'
        }
      ]
    });

  Em.run(function () {
    controller.get('controllers.createAppWizard').setProperties({
      newApp: {},
      currentStep: 3,
      transitionToRoute: Em.K
    });
    controller.send('submit');
  });

  propEqual(controller.get('controllers.createAppWizard.newApp.configs'), configsObject, 'configs are passed to wizard controller');
  equal(controller.get('controllers.createAppWizard.currentStep'), 4, 'should go to step4');

});
