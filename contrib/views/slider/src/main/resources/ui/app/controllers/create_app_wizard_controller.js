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

App.CreateAppWizardController = Ember.ObjectController.extend({

  newApp: null,

  currentStep: 1,

  TOTAL_STEPS_NUMBER: 4,

  loadStep: function () {
    this.set('currentStep', 1);
    this.gotoStep(this.get('currentStep'));
  },

  gotoStep: function (step, fromNextButon) {
    if (step > this.get('TOTAL_STEPS_NUMBER') || step < 1 || (!fromNextButon && step > this.get('currentStep'))) {
      return false;
    }
    this.set('currentStep', step);
    this.transitionToRoute('createAppWizard.step' + step);
  },

  nextStep: function () {
    this.gotoStep(this.get('currentStep') + 1, true);
  },

  prevStep: function () {
    this.gotoStep(this.get('currentStep') - 1);
  },

  hidePopup: function () {
    $('#createAppWizard').hide();
    this.transitionToRoute('slider_apps');
  },

  actions: {
    gotoStep: function (step) {
      this.gotoStep(step);
    }
  }
});
