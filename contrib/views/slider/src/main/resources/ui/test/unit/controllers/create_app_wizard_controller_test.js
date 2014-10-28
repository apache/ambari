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

moduleFor('controller:createAppWizard', 'App.CreateAppWizardController');

var stepCases = [
    {
      currentStep: 0,
      step: 5,
      fromNextButton: true,
      expectedCurrentStep: 0
    },
    {
      currentStep: 1,
      step: 0,
      fromNextButton: true,
      expectedCurrentStep: 1
    },
    {
      currentStep: 2,
      step: 3,
      fromNextButton: false,
      expectedCurrentStep: 2
    },
    {
      currentStep: 0,
      step: 1,
      fromNextButton: true,
      expectedCurrentStep: 1
    }
  ],
  currentStepTitle = 'currentStep should be {0}',
  newApp = {
    configs: {
      n0: 'v0'
    },
    predefinedConfigNames: []
  };

test('loadStep', function () {

  var controller = this.subject({
    transitionToRoute: Em.K
  });

  Em.run(function () {
    controller.loadStep();
  });

  equal(controller.get('currentStep'), 1, 'currentStep should be 1');

});

test('gotoStep', function () {

  var controller = this.subject({
    transitionToRoute: Em.K
  });

  stepCases.forEach(function (item) {

    Em.run(function () {
      controller.set('currentStep', item.currentStep);
      controller.gotoStep(item.step, item.fromNextButton);
    });

    equal(controller.get('currentStep'), item.expectedCurrentStep, currentStepTitle.format(item.expectedCurrentStep));

  });

});

test('actions.gotoStep', function () {

  var controller = this.subject({
    transitionToRoute: Em.K
  });

  stepCases.rejectBy('fromNextButton').forEach(function (item) {

    Em.run(function () {
      controller.set('currentStep', item.currentStep);
      controller.send('gotoStep', item.step);
    });

    equal(controller.get('currentStep'), item.expectedCurrentStep, currentStepTitle.format(item.expectedCurrentStep));

  });

});

test('gotoStep', function () {

  var createAppWizardController = this.subject({
    transitionToRoute: Em.K,
    newApp: newApp
  });

  Em.run(function () {
    createAppWizardController.gotoStep(1);
  });

  propEqual(createAppWizardController.get('newApp.configs', {}, 'custom configs should be dropped'));

});

test('actions.gotoStep', function () {

  var createAppWizardController = this.subject({
    transitionToRoute: Em.K,
    newApp: newApp
  });

  Em.run(function () {
    createAppWizardController.send('gotoStep', 1);
  });

  propEqual(createAppWizardController.get('newApp.configs', {}, 'custom configs should be dropped'));

});

test('dropCustomConfigs', function () {

  var controller = this.subject({
    newApp: {
      configs: {
        n0: 'v0',
        n1: 'v1'
      },
      predefinedConfigNames: ['n0']
    }
  });

  Em.run(function () {
    controller.dropCustomConfigs();
  });

  propEqual(controller.get('newApp.configs'), {n0: 'v0'}, 'custom configs should be dropped');

});

test('nextStep', function () {

  var controller = this.subject({
    transitionToRoute: Em.K,
    currentStep: 1
  });

  Em.run(function () {
    controller.nextStep();
  });

  equal(controller.get('currentStep'), '2', 'should go to step2');

});

test('prevStep', function () {

  var controller = this.subject({
    transitionToRoute: Em.K,
    currentStep: 2
  });

  Em.run(function () {
    controller.prevStep();
  });

  equal(controller.get('currentStep'), '1', 'should go to step1');

});

test('hidePopup', function () {

  var controller = this.subject({
    viewEnabled: true,
    transitionToRoute: Em.K,
    newApp: {}
  });

  Em.run(function () {
    controller.hidePopup();
  });

  equal(controller.get('newApp'), null, 'should erase app data');

});
