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

  /**
   * New app created via current wizard
   * Populated with data step by step
   * @type {object|null}
   */
  newApp: null,

  /**
   * Current step number
   * @type {number}
   */
  currentStep: 1,

  /**
   * Overall steps count
   * @type {number}
   */
  TOTAL_STEPS_NUMBER: 4,

  /**
   * Init controller's data
   * @method loadStep
   */
  loadStep: function () {
    this.set('currentStep', 1);
    this.gotoStep(this.get('currentStep'));
  },

  /**
   * Proceed user to selected step
   * @param {number} step step's number
   * @param {bool} fromNextButton is user came from "Next"-button click
   * @method gotoStep
   */
  gotoStep: function (step, fromNextButton) {
    if (step > this.get('TOTAL_STEPS_NUMBER') || step < 1 || (!fromNextButton && step > this.get('currentStep'))) {
      return;
    }
    this.set('currentStep', step);

    if (step == 1) {
      this.dropCustomConfigs();
    }
    this.transitionToRoute('createAppWizard.step' + step);
  },

  /**
   * Custom configs added by used should be dropped if user come back to the 1st step
   * @method dropCustomConfigs
   */
  dropCustomConfigs: function() {
    var configs = this.get('newApp.configs'),
      predefined = this.get('newApp.predefinedConfigNames'),
      filtered = {};
    if (!configs) return;
    Em.keys(configs).forEach(function(name) {
      if (predefined.contains(name)) {
        filtered[name] = configs[name];
      }
    });
    this.set('newApp.configs', filtered);
  },

  /**
   * Proceed user no next step
   * @method nextStep
   */
  nextStep: function () {
    this.gotoStep(this.get('currentStep') + 1, true);
  },

  /**
   * Proceed user to prev step
   * @method prevStep
   */
  prevStep: function () {
    this.gotoStep(this.get('currentStep') - 1);
  },

  /**
   * Hide wizard-popup
   * @method hidePopup
   */
  hidePopup: function () {
    $('#createAppWizard').hide();
    this.set('newApp', null);
    this.transitionToRoute('slider_apps');
  },

  actions: {

    /**
     * Proceed user to selected step
     * @param {number} step step's number
     * @method gotoStep
     */
    gotoStep: function (step) {
      this.gotoStep(step);
    }
  }

});
