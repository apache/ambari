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

App.CreateUpgradePlanWizardController = App.WizardController.extend({

  name: 'createUpgradePlanWizardController',

  totalSteps: 6,

  currentStep: '0',

  steps: [
    "downloadOptions",
    "selectUpgradeOptions",
    "downloadMpacks",
    "reviewConfigs",
	  "selectUpgradeType",
	  "upgradeSummary"
  ],

  displayName: Em.I18n.t('admin.createUpgradePlan.wizard.header'),

  hideBackButton: false,

  //Add shared properties for the steps
  content: Em.Object.create({
    controllerName: 'createUpgradePlanWizardController',
  }),

  setCurrentStep: function (currentStep, completed) {
    this._super(currentStep, completed);
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'CREATE_UPGRADE_PLAN',
      wizardControllerName: 'createUpgradePlanWizardController',
      localdb: App.db.data
    });
  },

  //Define type and callback as per each step
  loadMap: {
    'downloadOptions': [
      {
        type: 'sync',
        callback: function () {

        }
      }
    ],
    'selectUpgradeOptions': [
      {
        type: 'sync',
        callback: function () {

        }
      }
    ],
    'downloadMpacks': [
      {
        type: 'sync',
        callback: function () {

        }
      }
    ],
    'reviewConfigs': [
      {
        type: 'sync',
        callback: function () {

        }
      }
    ],
    'selectUpgradeType': [
      {
        type: 'sync',
        callback: function () {

        }
      }
    ],
    'upgradeSummary': [
      {
        type: 'sync',
        callback: function () {

        }
      }
    ]
  },


  gotoDownloadOptions: function () {
    this.gotoStep('downloadOptions');
  },

  gotoSelectUpgradeOptions: function () {
    this.gotoStep('selectUpgradeOptions');
  },

  gotoDownloadMpacks: function () {
    this.gotoStep('downloadMpacks');
  },

  gotoReviewConfigs: function () {
    this.gotoStep('reviewConfigs');
  },

  gotoSelectUpgradeType: function () {
    this.gotoStep('selectUpgradeType');
  },

  gotoUpgradeSummary: function () {
    this.gotoStep('upgradeSummary');
  },

  isDownloadOptions: function () {
    return this.get('currentStep') == this.getStepIndex('downloadOptions');
  }.property('currentStep'),

  isSelectUpgradeOptions: function () {
    return this.get('currentStep') == this.getStepIndex('selectUpgradeOptions');
  }.property('currentStep'),

  isDownloadMpacks: function () {
    return this.get('currentStep') == this.getStepIndex('downloadMpacks');
  }.property('currentStep'),

  isReviewConfigs: function () {
    return this.get('currentStep') == this.getStepIndex('reviewConfigs');
  }.property('currentStep'),

  isSelectUpgradeType: function () {
    return this.get('currentStep') == this.getStepIndex('selectUpgradeType');
  }.property('currentStep'),

  isSUpgradeSummary: function () {
    return this.get('currentStep') == this.getStepIndex('upgradeSummary');
  }.property('currentStep'),


  setStepsEnable: function () {

    for (var i = 0; i < this.get('steps').length; i++) {
      var currentStep = this.get('currentStep');
      var step = this.get('isStepDisabled').findProperty('step', i);
      var stepValue = i <= currentStep && App.get('router.clusterController.isLoaded') ? false : true;
      step.set('value', stepValue);
    }
  }.observes('currentStep', 'App.router.clusterController.isLoaded'),

  finish: function () {
    App.db.data.Installer = {};
    this.resetDbNamespace();
    App.router.get('updateController').updateAll();
  }

});