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

function isStepDisabled(index) {
  return Em.computed('controller.isStepDisabled.@each.{step,value}', function () {
    return this.isStepDisabled(index);
  }).cacheable();
}

function isStepCompleted(index) {
  return Em.computed('controller.{currentStep,isStepDisabled.@each.value}', function () {
    return this.isStepCompleted(index);
  }).cacheable();
}

App.WizardMenuMixin = Em.Mixin.create({

  isStepDisabled: function (stepName) {
    let index = this.get('controller').getStepIndex(stepName);
    let step = this.get('controller.isStepDisabled').findProperty('step', index);

    if (step) {
      return step.get('value');
    }

    return false;
  },

  isStepCompleted(stepName) {
    let index = this.get('controller').getStepIndex(stepName);
    return this.get('controller.currentStep') > index;
  },

  isStep0Disabled: isStepDisabled("step0"),
  isStep1Disabled: isStepDisabled("step1"),
  isStep2Disabled: isStepDisabled("step2"),
  isStep3Disabled: isStepDisabled("step3"),
  isConfigureDownloadDisabled: isStepDisabled("configureDownload"),
  isSelectMpacksDisabled: isStepDisabled("selectMpacks"),
  isCustomMpackReposDisabled: isStepDisabled("customMpackRepos"),
  isDownloadMpacksDisabled: isStepDisabled("downloadMpacks"),
  isCustomProductReposDisabled: isStepDisabled("customProductRepos"),
  isVerifyProductsDisabled: isStepDisabled("verifyProducts"),
  isStep4Disabled: isStepDisabled("step4"),
  isStep5Disabled: isStepDisabled("step5"),
  isStep6Disabled: isStepDisabled("step6"),
  isStep7Disabled: isStepDisabled("step7"),
  isStep8Disabled: isStepDisabled("step8"),
  isStep9Disabled: isStepDisabled("step9"),
  isStep10Disabled: isStepDisabled("step10"),
  isDownloadOptionsDisabled: isStepDisabled("downloadOptions"),
  isSelectUpgradeOptionsDisabled: isStepDisabled('selectUpgradeOptions'),
  isReviewConfigsDisabled: isStepDisabled('reviewConfigs'),
  isSelectUpgradeTypeDisabled: isStepDisabled('selectUpgradeType'),
  isUpgradeSummaryDisabled: isStepDisabled('upgradeSummary'),

  isStep0Completed: isStepCompleted("step0"),
  isStep1Completed: isStepCompleted("step1"),
  isStep2Completed: isStepCompleted("step2"),
  isStep3Completed: isStepCompleted("step3"),
  isConfigureDownloadCompleted: isStepCompleted("configureDownload"),
  isSelectMpacksCompleted: isStepCompleted("selectMpacks"),
  isCustomMpackReposCompleted: isStepCompleted("customMpackRepos"),
  isDownloadMpacksCompleted: isStepCompleted("downloadMpacks"),
  isCustomProductReposCompleted: isStepCompleted("customProductRepos"),
  isVerifyProductsCompleted: isStepCompleted("verifyProducts"),
  isStep4Completed: isStepCompleted("step4"),
  isStep5Completed: isStepCompleted("step5"),
  isStep6Completed: isStepCompleted("step6"),
  isStep7Completed: isStepCompleted("step7"),
  isStep8Completed: isStepCompleted("step8"),
  isStep9Completed: isStepCompleted("step9"),
  isStep10Completed: isStepCompleted("step10"),
  isDownloadOptionsCompleted: isStepCompleted("downloadOptions"),
  isSelectUpgradeOptionsCompleted: isStepCompleted("selectUpgradeOptions"),
  isReviewConfigsCompleted: isStepCompleted("reviewConfigs"),
  isSelectUpgradeTypeCompleted: isStepCompleted("selectUpgradeType"),
  isUpgradeSummaryCompleted: isStepCompleted("upgradeSummary")

});
