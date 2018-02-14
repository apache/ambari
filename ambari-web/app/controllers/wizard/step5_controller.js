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
require('./wizardStep_controller');

App.WizardStep5Controller = App.WizardStepController.extend(App.BlueprintMixin, App.AssignMasterComponents, {

  name: "wizardStep5Controller",

  stepName: 'step5',

  isSaved: function () {
    const wizardController = this.get('wizardController');
    if (wizardController) {
      return wizardController.getStepSavedState(this.get('stepName'));
    }
    return false;
  }.property('wizardController.content.stepsSavedState'),

  _goNextStepIfValid: function () {
    App.set('router.nextBtnClickInProgress', false);
    if (!this.get('submitDisabled')) {
      App.router.send('next');
    }
  },

  _additionalClearSteps: function() {
    var parentController = App.router.get(this.get('content.controllerName'));
    if (parentController && parentController.get('content.componentsFromConfigs')) {
      parentController.clearConfigActionComponents();
    }
  }

});
