/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
require('./wizardStep_controller');

App.WizardCustomMpackReposController = App.WizardStepController.extend({

  name: 'wizardCustomMpackReposController',

  stepName: 'customMpackRepos',

  mpacks: Em.computed.alias('content.selectedMpacks'),
  
  isStepDisabled: function (stepIndex, currentIndex) {
    const normallyDisabled = this._super(stepIndex, currentIndex);
    const useCustomRepo = this.get('wizardController.content.downloadConfig.useCustomRepo');

    return normallyDisabled || !useCustomRepo;
  },

  isSubmitDisabled: function () {
    const mpacks = this.get('mpacks');
    return mpacks.filterProperty('downloadUrl', '').length > 0 || App.get('router.btnClickInProgress');
  }.property('mpacks.@each.downloadUrl', 'App.router.btnClickInProgress'),

  submit: function () {
    if (App.get('router.nextBtnClickInProgress')) {
      return;
    }

    App.router.send('next');
  }
});
