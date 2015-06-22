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

moduleFor('controller:createAppWizardStep1', 'App.CreateAppWizardStep1Controller', {

  needs: [
    'controller:createAppWizard'
  ],

  setup: function () {
    sinon.stub(App.ajax, 'send', Em.K);
  },

  teardown: function () {
    App.ajax.send.restore();
  }

});

var selectedType = Em.Object.create({
  id: 'HBASE',
  configs: {
    n0: 'v0'
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

test('isAppTypesError', function () {

  expect(2);

  var controller = this.subject({availableTypes: {content: []}});
  equal(controller.get('isAppTypesError'), true, 'should be true if no app types provided');

  Em.run(function () {
    controller.set('availableTypes', {content: [
      {}
    ]});
  });
  equal(controller.get('isAppTypesError'), false, 'should be false if app types provided');

});

test('typeDescription', function () {

  expect(2);

  var controller = this.subject();

  equal(controller.get('typeDescription'), '', 'default typeDescription');

  Em.run(function () {
    controller.set('selectedType', Em.Object.create({
      displayName: 'HBASE'
    }));
  });

  equal(controller.get('typeDescription'), Em.I18n.t('wizard.step1.typeDescription').format('HBASE'), 'typeDescription is set from selectedType.displayName');

});

test('initializeNewApp', function () {

  expect(9);

  var controller = this.subject({
      store: Em.Object.create({
        all: function () {
          return [];
        }
      })
    }),
    app = Em.Object.create({
      name: 'n',
      includeFilePatterns: 'i',
      excludeFilePatterns: 'e',
      frequency: 'f',
      queueName: 'q',
      specialLabel: 's',
      selectedYarnLabel: 'y'
    }),
    title = '{0} should be taken from appWizardController.newApp';

  Em.run(function () {
    controller.initializeNewApp();
  });

  equal(controller.get('newApp.selectedYarnLabel'), 0, 'selectedYarnLabel should be 0 as default');

  var values = Em.keys(controller.get('newApp')).without('appType').without('configs').without('selectedYarnLabel').map(function (item) {
    return controller.get('newApp.' + item);
  });

  propEqual(values.uniq(), [false, ''], 'should set properties values to empty strings as default');

  Em.run(function () {
    controller.set('controllers.createAppWizard.newApp', app);
    controller.initializeNewApp();
  });

  Em.keys(app).forEach(function (key) {
    equal(controller.get('newApp.' + key), app.get(key), title.format(key));
  });

});

test('loadAvailableTypes', function () {

  expect(1);

  var testObject = {
      key: 'value'
    },
    controller = this.subject({
    store: Em.Object.create({
      all: function () {
        return testObject;
      }
    })
  });

  Em.run(function () {
    controller.loadAvailableTypes();
  });

  propEqual(controller.get('availableTypes'), testObject, 'availableTypes should be loaded from store');

});

test('nameValidator', function () {
  expect(7);

  var tests = [
    { name: 'Slider', e: true },
    { name: '_slider', e: true },
    { name: 'slider*2', e: true },
    { name: 'slider', e: false },
    { name: 'slider_1-2_3', e: false }
  ];

  var controller = this.subject({isNameError: false,
    store: Em.Object.create({
      all: function (key) {
        return {
          sliderApp: [
            { name: 'slider2' }
          ]
        }[key];
      }
    })
  });

  tests.forEach(function (test) {
    Em.run(function () {
      controller.set('newApp', { name: test.name});
    });

    equal(controller.get('isNameError'), test.e, 'Name `' + test.name + '` is' + (!!test.e ? ' not ' : ' ') + 'valid');
  });

  Em.run(function () {
    controller.set('newApp', { name: 'slider2'});
  });

  equal(controller.get('isNameError'), true, 'Name `slider2` already exist');
  equal(controller.get('nameErrorMessage'), Em.I18n.t('wizard.step1.nameRepeatError'), 'Error message should be shown');
});

test('validateAppNameSuccessCallback', function () {

  expect(5);

  var title = 'newApp should have {0} set',
    controller = this.subject({
      newApp: Em.Object.create(),
      selectedType: selectedType
    });

  Em.run(function () {
    controller.set('appWizardController.transitionToRoute', Em.K);
    controller.validateAppNameSuccessCallback();
  });

  deepEqual(controller.get('newApp.appType'), selectedType, title.format('appType'));
  deepEqual(controller.get('newApp.configs'), selectedType.configs, title.format('configs'));
  deepEqual(controller.get('newApp.predefinedConfigNames'), Em.keys(selectedType.configs), title.format('predefinedConfigNames'));
  deepEqual(controller.get('appWizardController.newApp'), controller.get('newApp'), 'newApp should be set in CreateAppWizardController');
  equal(controller.get('appWizardController.currentStep'), 2, 'should proceed to the next step');

});

test('validateAppNameErrorCallback', function () {

  expect(7);

  var controller = this.subject();

  Em.run(function () {
    sinon.stub(Bootstrap.ModalManager, 'open', Em.K);
    sinon.stub(controller, 'defaultErrorHandler', Em.K);
    controller.validateAppNameErrorCallback({
      status: 409
    }, null, null, null, {
      name: 'name'
    });
  });

  ok(Bootstrap.ModalManager.open.calledOnce, 'app name conflict popup should be displayed');
  ok(!controller.defaultErrorHandler.called, 'defaultErrorHandler shouldn\'t be executed');

  Em.run(function () {
    Bootstrap.ModalManager.open.restore();
    controller.defaultErrorHandler.restore();
    sinon.stub(Bootstrap.ModalManager, 'open', Em.K);
    sinon.stub(controller, 'defaultErrorHandler', Em.K);
    controller.validateAppNameErrorCallback({
      status: 400
    }, null, null, {
      url: 'url',
      type: 'type'
    }, null);
  });

  ok(!Bootstrap.ModalManager.open.called, 'app name conflict popup shouldn\'t be displayed');
  ok(controller.defaultErrorHandler.calledOnce, 'defaultErrorHandler should be executed');
  propEqual(controller.defaultErrorHandler.firstCall.args[0], {
    status: 400
  }, 'should pass request info to defaultErrorHandler');
  equal(controller.defaultErrorHandler.firstCall.args[1], 'url', 'should pass url to defaultErrorHandler');
  equal(controller.defaultErrorHandler.firstCall.args[2], 'type', 'should pass type to defaultErrorHandler');

  Bootstrap.ModalManager.open.restore();
  controller.defaultErrorHandler.restore();

});

test('validateAppNameCompleteCallback', function () {

  expect(1);

  var controller = this.subject({
    validateAppNameRequestExecuting: true
  });

  Em.run(function () {
    controller.validateAppNameCompleteCallback();
  });

  ok(!controller.get('validateAppNameRequestExecuting'), 'validateAppNameRequestExecuting should be set to false');

});

test('isSubmitDisabled', function () {

  expect(6);

  var controller = this.subject({
      availableTypes: {
        content: [
          {}
        ]
      },
      isNameError: false,
      newApp: {
        name: 'some'
      }
    }),
    cases = [
      {
        key: 'validateAppNameRequestExecuting',
        title: 'request is executing'
      },
      {
        key: 'isNameError',
        title: 'app name is invalid'
      },
      {
        key: 'isAppTypesError',
        title: 'no app types are available'
      },
      {
        key: 'isFrequencyError',
        title: 'frequency value is invalid'
      }
    ],
    keys = cases.mapProperty('key'),
    failTitle = 'submit button is disabled when {0}';

  equal(controller.get('isSubmitDisabled'), false);

  cases.forEach(function (item) {
    Em.run(function () {
      keys.forEach(function (key) {
        controller.set(key, item.key != key);
      });
    });
    equal(controller.get('isSubmitDisabled'), true, failTitle.format(item.title));
  });

  Em.run(function () {
    keys.forEach(function (key) {
      controller.set(key, true);
    });
    controller.set('newApp.name', '');
  });
  equal(controller.get('isSubmitDisabled'), true, failTitle.format('no app name is specified'));

});

test('frequencyValidator', function () {

  expect(8);

  var controller = this.subject(),
    cases = [
      {
        value: '123',
        isFrequencyError: false,
        frequencyErrorMessage: '',
        title: 'numeric value'
      },
      {
        value: '123a',
        isFrequencyError: true,
        frequencyErrorMessage: Em.I18n.t('wizard.step1.frequencyError'),
        title: 'value contains letter'
      },
      {
        value: '123-',
        isFrequencyError: true,
        frequencyErrorMessage: Em.I18n.t('wizard.step1.frequencyError'),
        title: 'value contains special symbol'
      },
      {
        value: '123 ',
        isFrequencyError: true,
        frequencyErrorMessage: Em.I18n.t('wizard.step1.frequencyError'),
        title: 'value contains space'
      }
    ],
    errorTitle = '{0}: isFrequencyError is set correctly',
    messageTitle = '{0}: error message is set correctly';

  cases.forEach(function (item) {
    Em.run(function () {
      controller.set('newApp', {
        frequency: item.value
      });
    });
    equal(controller.get('isFrequencyError'), item.isFrequencyError, errorTitle.format(item.title));
    equal(controller.get('frequencyErrorMessage'), item.frequencyErrorMessage, messageTitle.format(item.title));
  });

});

test('saveApp', function () {

  expect(4);

  var controller = this.subject({
      newApp: Em.Object.create(),
      selectedType: selectedType
    }),
    saveAppTitle = 'newApp should have {0} set';

  Em.run(function () {
    controller.saveApp();
  });

  propEqual(controller.get('newApp.appType'), selectedType, saveAppTitle.format('appType'));
  propEqual(controller.get('newApp.configs'), selectedType.configs, saveAppTitle.format('configs'));
  deepEqual(controller.get('newApp.predefinedConfigNames'), Em.keys(selectedType.configs), saveAppTitle.format('predefinedConfigNames'));
  propEqual(controller.get('appWizardController.newApp'), controller.get('newApp'), 'newApp should be set in CreateAppWizardController');

});

test('actions.submit', function () {

  expect(3);

  var controller = this.subject({
    validateAppNameRequestExecuting: false,
    validateAppNameSuccessCallback: Em.K,
    newApp: {
      name: 'name'
    }
  });

  Em.run(function () {
    controller.send('submit');
  });

  ok(controller.get('validateAppNameRequestExecuting'), 'validateAppNameRequestExecuting should be set to true');
  ok(App.ajax.send.calledOnce, 'request to server should be sent');
  equal(App.ajax.send.firstCall.args[0].data.name, 'name', 'name should be passed');

});