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

module.exports = App.WizardRoute.extend(App.RouterRedirections,{
  route: '/createUpgradePlan',

  breadcrumbs: {
    label: Em.I18n.t('admin.createUpgradePlan.wizard.header')
  },

  enter: function (router) {
    var createUpgradePlanWizardController = router.get('createUpgradePlanWizardController');
    createUpgradePlanWizardController.dataLoading().done(function () {
        App.router.get('updateController').set('isWorking', false);
        var popup = App.ModalPopup.show({
          classNames: ['wizard-modal-wrapper'],
          modalDialogClasses: ['modal-xlg'],
          header: Em.I18n.t('admin.createUpgradePlan.wizard.header'),
          bodyClass: App.CreateUpgradePlanWizardView.extend({
            controller: createUpgradePlanWizardController
          }),
          primary: Em.I18n.t('form.cancel'),
          showFooter: false,
          secondary: null,
          //construct cases where close button needs to be hidden. Make it observable on wizard steps
          //default  --- this.set('showCloseButton', true);
          hideCloseButton: function () {
            this.set('showCloseButton', true);
         },
          //call any cleanup functions here before resetOnClose
          onClose: function () {
            var controller = App.router.get('createUpgradePlanWizardController');
            controller.resetOnClose(controller, 'main.admin.serviceGroups');
          },
          didInsertElement: function () {
            this._super();
            this.fitHeight();
          }
        });
        createUpgradePlanWizardController.set('popup', popup);
        var currentClusterStatus = App.clusterStatus.get('value');
        if (currentClusterStatus) {
          switch (currentClusterStatus.clusterState) {
            case 'CREATE_UPGRADE_PLAN' :
              createUpgradePlanWizardController.setCurrentStep(currentClusterStatus.localdb.CreateUpgradePlanWizard.currentStep);
              break;
            default:
              var currStep = App.router.get('createUpgradePlanWizardController.currentStep');
              createUpgradePlanWizardController.setCurrentStep(currStep);
              break;
          }
        }
      Em.run.next(function () {
        App.router.get('wizardWatcherController').setUser(createUpgradePlanWizardController.get('name'));
        router.transitionTo(createUpgradePlanWizardController.get('currentStepName'));
      });
    });
  },

  downloadOptions: App.StepRoute.extend({
    route: '/downloadOptions',
    connectOutlets: function (router) {
      var controller = router.get('createUpgradePlanWizardController'),
        createUpgradePlanWizardDownloadOptionsController = router.get('createUpgradePlanWizardDownloadOptionsController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('downloadOptions');
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('createUpgradePlanWizardDownloadOptions', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    //call any functions that load data required for the next step
    next: function (router) {
      var controller = router.get('createUpgradePlanWizardController');
      router.transitionTo('selectUpgradeOptions');
    }
  }),

  selectUpgradeOptions: App.StepRoute.extend({
    route: '/selectUpgradeOptions',
    connectOutlets: function (router) {
      var controller = router.get('createUpgradePlanWizardController'),
        createUpgradePlanWizardselectUpgradeOptionsController = router.get('createUpgradePlanWizardselectUpgradeOptionsController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('selectUpgradeOptions');
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('createUpgradePlanWizardSelectUpgradeOptions', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    //call any functions that load data required for the next step
    back: function (router) {
      var controller = router.get('createUpgradePlanWizardController');
      router.transitionTo('downloadOptions');
    },
    next: function (router) {
      var controller = router.get('createUpgradePlanWizardController');
      router.transitionTo('downloadMpacks');
    }
  }),

  downloadMpacks: App.StepRoute.extend({
    route: '/downloadMpacks',
    connectOutlets: function (router) {
      var controller = router.get('createUpgradePlanWizardController'),
        createUpgradePlanWizardDownloadMpacksController = router.get('createUpgradePlanWizardDownloadMpacksController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('downloadMpacks');
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('createUpgradePlanWizardDownloadMpacks', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    back: function (router) {
      var controller = router.get('createUpgradePlanWizardController');
      router.transitionTo('selectUpgradeOptions');
    },
    //call any functions that load data required for the next step
    next: function (router) {
      var controller = router.get('createUpgradePlanWizardController');
      router.transitionTo('reviewConfigs');
    }
  }),

  reviewConfigs: App.StepRoute.extend({
    route: '/reviewConfigs',
    connectOutlets: function (router) {
      var controller = router.get('createUpgradePlanWizardController'),
        createUpgradePlanWizardReviewConfigsController = router.get('createUpgradePlanWizardReviewConfigsController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('reviewConfigs');
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('createUpgradePlanWizardReviewConfigs', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    back: function (router) {
      var controller = router.get('createUpgradePlanWizardController');
      router.transitionTo('downloadMpacks');
    },
    //call any functions that load data required for the next step
    next: function (router) {
      var controller = router.get('createUpgradePlanWizardController');
      router.transitionTo('selectUpgradeType');
    }
  }),
  
  selectUpgradeType: App.StepRoute.extend({
    route: '/selectUpgradeType',
    connectOutlets: function (router) {
      var controller = router.get('createUpgradePlanWizardController'),
        createUpgradePlanWizardselectUpgradeTypeController = router.get('createUpgradePlanWizardSelectUpgradeTypeController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('selectUpgradeType');
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('createUpgradePlanWizardSelectUpgradeType', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    back: function (router) {
      var controller = router.get('createUpgradePlanWizardController');
      router.transitionTo('reviewConfigs');
    },
    //call any functions that load data required for the next step
    next: function (router) {
      var controller = router.get('createUpgradePlanWizardController');
      router.transitionTo('upgradeSummary');
    }
  }),

  upgradeSummary: App.StepRoute.extend({
    route: '/upgradeSummary',
    connectOutlets: function (router) {
      var controller = router.get('createUpgradePlanWizardController'),
        createUpgradePlanWizardUpgradeSummaryController = router.get('createUpgradePlanWizardUpgradeSummaryController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('upgradeSummary');
        controller.loadAllPriorSteps().done(function () {
          controller.connectOutlet('createUpgradePlanWizardUpgradeSummary', controller.get('content'));
        });
      })
    },
    unroutePath: function () {
      return false;
    },
    //call any functions that load data required for the next step
    complete: function (router, context) {
      var controller = router.get('createUpgradePlanWizardController');
      controller.resetOnClose(controller, 'main.admin.serviceGroups');
    }
  }),

});
