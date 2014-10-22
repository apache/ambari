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
    App.ajax.send = Em.K;
  },

  teardown: function () {
    App.reset();
  }

});

test('isSubmitDisabled', function () {

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

test('resourcesFormatted', function () {

  var cases = [
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
    title = '{0} should be {1}',
    label = 'label';

  var controller = this.subject({
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

  cases.forEach(function (item) {

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

});