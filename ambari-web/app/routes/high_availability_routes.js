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
      App.ModalPopup.show({
        classNames: ['full-width-modal'],
        header: Em.I18n.t('admin.highAvailability.wizard.header'),
        bodyClass: App.HighAvailabilityWizardView.extend({
          controller: highAvailabilityWizardController
        }),
        primary: Em.I18n.t('form.cancel'),
        showFooter: false,
        secondary: null,

        onClose: function () {
          this.hide();
          App.router.get('updateController').set('isWorking', true);
          App.router.transitionTo('main.admin.adminHighAvailability')
        },
        didInsertElement: function () {
          this.fitHeight();
        }
      });
      router.transitionTo('step1');
    });
  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      var controller = router.get('highAvailabilityWizardController');
      controller.setCurrentStep('1');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('highAvailabilityWizardStep1');
      })
    },
    next: function (router) {
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
    next: function (router) {
      router.transitionTo('step5');
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',
    connectOutlets: function (router) {
      $('a.close').hide();
      var controller = router.get('highAvailabilityWizardController');
      controller.setCurrentStep('5');
      controller.setLowerStepsDisable(5);
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('highAvailabilityWizardStep5',  controller.get('content'));
      })
    },
    back: function (router) {
      router.transitionTo('step4');
    },
    next: function (router) {}
  }),

  gotoStep1: Em.Router.transitionTo('step1'),

  gotoStep2: Em.Router.transitionTo('step2'),

  gotoStep3: Em.Router.transitionTo('step3'),

  gotoStep4: Em.Router.transitionTo('step4'),

  gotoStep5: Em.Router.transitionTo('step5')
});
