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

moduleFor('controller:createAppWizardStep2', 'App.CreateAppWizardStep2Controller', {

  needs: [
    'controller:createAppWizard'
  ]

});

var title = 'should be {0}';

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

test('isError', function () {

  expect(18);

  var cases = [
      {
        content: [],
        isError: false
      },
      {
        content: [
          Em.Object.create()
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            numInstances: '0'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            yarnMemory: '0'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            yarnCPU: '0'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            numInstances: ' \r\n',
            yarnMemory: ' \r\n',
            yarnCPU: ' \r\n'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            numInstances: 'n',
            yarnMemory: '0',
            yarnCPU: '0'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            numInstances: '0',
            yarnMemory: 'n',
            yarnCPU: '0'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            numInstances: '0',
            yarnMemory: '0',
            yarnCPU: 'n'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            numInstances: '0.5',
            yarnMemory: '0',
            yarnCPU: '0'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            numInstances: '0',
            yarnMemory: '0.5',
            yarnCPU: '0'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            numInstances: '0',
            yarnMemory: '0',
            yarnCPU: '0.5'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            numInstances: '-1',
            yarnMemory: '0',
            yarnCPU: '0'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            numInstances: '0',
            yarnMemory: '-1',
            yarnCPU: '0'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            numInstances: '0',
            yarnMemory: '0',
            yarnCPU: '-1'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            numInstances: '0',
            yarnMemory: '0',
            yarnCPU: '0'
          }),
          Em.Object.create({
            numInstances: '-1',
            yarnMemory: '0',
            yarnCPU: '0'
          })
        ],
        isError: true
      },
      {
        content: [
          Em.Object.create({
            numInstances: '0',
            yarnMemory: '0',
            yarnCPU: '0'
          })
        ],
        isError: false
      },
      {
        content: [
          Em.Object.create({
            numInstances: '0',
            yarnMemory: '0',
            yarnCPU: '0'
          }),
          Em.Object.create({
            numInstances: '1',
            yarnMemory: '1',
            yarnCPU: '1'
          })
        ],
        isError: false
      }
    ],
    controller = this.subject();

  cases.forEach(function (item) {

    Em.run(function () {
      controller.set('content', item.content);
    });

    equal(controller.get('isError'), item.isError, title.format(item.isError));

  });

});

test('isSubmitDisabled', function () {

  expect(2);

  var cases = [true, false],
  controller = this.subject();

  cases.forEach(function (item) {

    Em.run(function () {
      controller.set('isError', item);
    });

    equal(controller.get('isSubmitDisabled'), item, title.format(item));

  });

});

test('initializeNewApp', function () {

  expect(4);

  var controller = this.subject();

  Em.run(function () {
    controller.set('controllers.createAppWizard.newApp', {
      components: [
        Em.Object.create({
          name: 'n'
        })
      ]
    });
    controller.initializeNewApp();
  });

  equal(controller.get('newApp.components.length'), 1, 'newApp should be taken from appWizardController');
  equal(controller.get('newApp.components')[0].get('name'), 'n', 'newApp has correct names of components');
  equal(controller.get('content.length'), 1, 'content should be taken from appWizardController');
  equal(controller.get('content')[0].get('name'), 'n', 'content has correct names of components');

});

test('loadTypeComponents', function () {

  expect(8);

  var toStringTitle = 'should convert {0} to string',
    controller = this.subject();

  Em.run(function () {
    controller.set('newApp', {
      appType: {
        components: [
          Em.Object.create({
            name: 'n0',
            defaultNumInstances: 0,
            defaultYARNMemory: 128,
            defaultYARNCPU: 1
          })
        ]
      }
    });
    controller.set('controllers.createAppWizard.newApp', {
      components: [
        Em.Object.create({
          name: 'n1'
        })
      ]
    });
    controller.loadTypeComponents();
  });

  equal(controller.get('content.length'), 1, 'content should contain one item');
  equal(controller.get('content')[0].get('name'), 'n1', 'should take components from wizard controller');

  Em.run(function () {
    controller.get('content').clear();
    controller.get('controllers.createAppWizard.newApp.components').clear();
    controller.loadTypeComponents();
  });

  equal(controller.get('content.length'), 1, 'content contains one item');
  equal(controller.get('content')[0].get('name'), 'n0', 'should take components from step controller');
  deepEqual(controller.get('content')[0].get('numInstances'), '0', toStringTitle.format('numInstances'));
  deepEqual(controller.get('content')[0].get('yarnMemory'), '128', toStringTitle.format('yarnMemory'));
  deepEqual(controller.get('content')[0].get('yarnCPU'), '1', toStringTitle.format('yarnCPU'));

  Em.run(function () {
    controller.get('content').clear();
    controller.get('controllers.createAppWizard.newApp.components').clear();
    controller.get('newApp.appType.components').clear();
    controller.loadTypeComponents();
  });

  equal(controller.get('content.length'), 0, 'content should remain empty');

});

test('isNotInteger', function () {

  expect(6);

  var controller = this.subject({});
  equal(controller.isNotInteger('1'), false, 'Valid value');
  equal(controller.isNotInteger('-1'), true, 'Invalid value (1)');
  equal(controller.isNotInteger('bbb'), true, 'Invalid value (2)');
  equal(controller.isNotInteger('1a'), true, 'Invalid value (3)');
  equal(controller.isNotInteger('!@#$%^'), true, 'Invalid value (4)');
  equal(controller.isNotInteger(null), true, 'Invalid value (5)');

});

test('saveComponents', function () {

  expect(2);

  var controller = this.subject({
    content: [
      Em.Object.create({
        name: 'n'
      })
    ]
  });

  Em.run(function () {
    controller.set('controllers.createAppWizard.newApp', {});
    controller.saveComponents();
  });

  equal(controller.get('appWizardController.newApp.components.length'), 1, 'components in wizard controller should be set from content');
  equal(controller.get('appWizardController.newApp.components')[0].get('name'), 'n', 'components in wizard controller have correct names');

});

test('actions.submit', function () {

  expect(3);

  var controller = this.subject({
    content: [
      Em.Object.create({
        name: 'n'
      })
    ]
  });

  Em.run(function () {
    controller.get('controllers.createAppWizard').setProperties({
      newApp: {},
      currentStep: 2,
      transitionToRoute: Em.K
    });
    controller.send('submit');
  });

  equal(controller.get('appWizardController.newApp.components.length'), 1, 'components in wizard controller should be set from content');
  equal(controller.get('appWizardController.newApp.components')[0].get('name'), 'n', 'components in wizard controller have correct names');
  equal(controller.get('appWizardController.currentStep'), '3', 'should go to step3');

});
