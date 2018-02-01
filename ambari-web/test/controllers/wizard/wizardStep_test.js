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
var controller = App.WizardStepController.create({
  wizardController: App.WizardController.create({
    steps: [
      'FirstStep',
      'SecondStep',
      'ThirdStep'
    ],
    currentStep: 1
  })
});

describe('App.WizardStepController', function () {
  describe('#isStepDisabled', function () {
    it('Returns false when this step is before current step', function () {
      controller.set('wizardController.stepName', 'FirstStep');
      var actual = controller.isStepDisabled();
      expect(actual).to.be.false;
    });

    it('Returns false when this step is the current step', function () {
      controller.set('wizardController.stepName', 'SecondStep');
      var actual = controller.isStepDisabled();
      expect(actual).to.be.false;
    });

    it('Returns true when this step is after current step', function () {
      controller.set('wizardController.stepName', 'ThirdStep');
      var actual = controller.isStepDisabled();
      expect(actual).to.be.false;
    });
  });
});