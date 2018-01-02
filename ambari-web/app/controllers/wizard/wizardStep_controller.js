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

/*
  A base class for wizard step controllers.
*/
App.WizardStepController = Em.Controller.extend({  
  /**
   * Determines whether the step should be disabled.
   * This is a base implementation that should be extended
   * in derived classes to provide special case logic.
   * The base implementation returns true if the step being checked
   * is after the current step.
   * 
   * @returns true if the step should be disabled
   */
  isStepDisabled: function () {
    const wizardController = this.get('wizardController');
    const currentIndex = wizardController.get('currentStep');
    const stepName = this.get('stepName');
    const stepIndex = wizardController.getStepIndex(stepName);

    return stepIndex > currentIndex;
  }
});