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

module.exports = Em.Route.extend({
  route: '/highAvailability/enable',

  enter: function (router) {
    Em.run.next(function () {
      var highAvailabilityWizardController = router.get('highAvailabilityWizardController');
      App.router.get('updateController').set('isWorking', false);
      var popup = App.ModalPopup.show({
        classNames: ['full-width-modal'],
        header: Em.I18n.t('admin.highAvailability.wizard.header'),
        bodyClass: App.HighAvailabilityWizardView.extend({
          controller: highAvailabilityWizardController
        }),
        primary: Em.I18n.t('form.cancel'),
        showFooter: false,
        secondary: null,
        hideCloseButton: function () {
          this.set('showCloseButton', App.router.get('highAvailabilityWizardController.currentStep') < 5);
        }.observes('App.router.highAvailabilityWizardController.currentStep'),

        onClose: function () {
          this.hide();
          App.router.get('updateController').set('isWorking', true);
          App.router.transitionTo('main.admin.adminHighAvailability')
        },
        didInsertElement: function () {
          this.fitHeight();
        }
      });
      highAvailabilityWizardController.set('popup', popup);
      App.clusterStatus.updateFromServer();
      var currentClusterStatus = App.clusterStatus.get('value');
      if (currentClusterStatus) {
        switch (currentClusterStatus.clusterState) {
          case 'HIGH_AVAILABILITY_DEPLOY' :
            App.db.data = currentClusterStatus.localdb;
            highAvailabilityWizardController.setCurrentStep(currentClusterStatus.localdb.HighAvailabilityWizard.currentStep);
            break;
          default:
            highAvailabilityWizardController.setCurrentStep('1');
            break;
        }
      }
      router.transitionTo('step' + highAvailabilityWizardController.get('currentStep'));
    });
  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.setCurrentStep('1');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('highAvailabilityWizardStep1', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.saveNameServiceId(router.get('highAvailabilityWizardStep1Controller.content.nameServiceId'));
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.setCurrentStep('2');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('highAvailabilityWizardStep2', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      var highAvailabilityWizardStep2Controller = router.get('highAvailabilityWizardStep2Controller');
      controller.saveMasterComponentHosts(highAvailabilityWizardStep2Controller);
      router.transitionTo('step3');
    },
    back: function (router) {
      router.transitionTo('step1');
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.setCurrentStep('3');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('highAvailabilityWizardStep3',  controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      router.transitionTo('step4');
    },
    back: function (router) {
      router.transitionTo('step2');
    }
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.setCurrentStep('4');
      controller.setLowerStepsDisable(4);
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('highAvailabilityWizardStep4',  controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      router.transitionTo('step5');
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.setCurrentStep('5');
      controller.setLowerStepsDisable(5);
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('highAvailabilityWizardStep5',  controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      router.transitionTo('step6');
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.setCurrentStep('6');
      controller.setLowerStepsDisable(6);
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('highAvailabilityWizardStep6',  controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      router.transitionTo('step7');
    }
  }),

  step7: Em.Route.extend({
    route: '/step7',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.setCurrentStep('7');
      controller.setLowerStepsDisable(7);
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('highAvailabilityWizardStep7',  controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      router.transitionTo('step8');
    }
  }),

  step8: Em.Route.extend({
    route: '/step8',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.setCurrentStep('8');
      controller.setLowerStepsDisable(8);
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('highAvailabilityWizardStep8',  controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      router.transitionTo('step9');
    }
  }),

  step9: Em.Route.extend({
    route: '/step9',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.setCurrentStep('9');
      controller.setLowerStepsDisable(9);
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('highAvailabilityWizardStep9',  controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.finish();
      controller.get('popup').hide();
      App.clusterStatus.setClusterStatus({
        clusterName: controller.get('content.cluster.name'),
        clusterState: 'HIGH_AVAILABILITY_COMPLETED',
        wizardControllerName: 'highAvailabilityWizardController',
        localdb: App.db.data
      });
      router.transitionTo('main.index');
    }
  }),

  gotoStep1: Em.Router.transitionTo('step1'),

  gotoStep2: Em.Router.transitionTo('step2'),

  gotoStep3: Em.Router.transitionTo('step3'),

  gotoStep4: Em.Router.transitionTo('step4'),

  gotoStep5: Em.Router.transitionTo('step5'),

  gotoStep6: Em.Router.transitionTo('step6'),

  gotoStep7: Em.Router.transitionTo('step7'),

  gotoStep8: Em.Router.transitionTo('step8'),

  gotoStep9: Em.Router.transitionTo('step9')
});
