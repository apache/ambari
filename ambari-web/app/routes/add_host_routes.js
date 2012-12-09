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
  route: '/hosts/add',

  enter: function (router) {
    console.log('in /hosts/add:enter');

    Ember.run.next(function () {
      var addHostController = router.get('addHostController');
      router.transitionTo('step' + addHostController.get('currentStep'));
    });

  },

  connectOutlets: function (router, context) {
    console.log('in /hosts/add:connectOutlets');
    router.get('mainController').connectOutlet('addHost');
  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      console.log('in addHost.step1:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('1', false);
      controller.set('hideBackButton', true);
      controller.loadAllPriorSteps();
      controller.connectOutlet('wizardStep2', controller.get('content.hosts'));
    },

    next: function (router) {
      router.transitionTo('step2');
      App.db.setBootStatus(false);
    },
    evaluateStep: function (router) {
      console.log('in addHost.step1:evaluateStep');
      var addHostController = router.get('addHostController');
      var wizardStep2Controller = router.get('wizardStep2Controller');

      wizardStep2Controller.set('hasSubmitted', true);

      if (!wizardStep2Controller.get('isSubmitDisabled')) {
        addHostController.saveHosts(wizardStep2Controller);
        wizardStep2Controller.evaluateStep();
      }
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      console.log('in addHost.step2:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('2', false);
      controller.loadAllPriorSteps();
      controller.connectOutlet('wizardStep3', controller.get('content'));
    },
    back: Em.Router.transitionTo('step1'),
    next: function (router, context) {
      var addHostController = router.get('addHostController');
      var wizardStep3Controller = router.get('wizardStep3Controller');
      addHostController.saveConfirmedHosts(wizardStep3Controller);

      App.db.setBootStatus(true);
      App.db.setService(require('data/mock/services'));
      router.transitionTo('step3');
    },
    /**
     * Wrapper for remove host action.
     * Since saving data stored in addHostController, we should call this from router
     * @param router
     * @param context Array of hosts to delete
     */
    removeHosts: function (router, context) {
      console.log('in addHost.step2.removeHosts:hosts to delete ', context);
      var controller = router.get('addHostController');
      controller.removeHosts(context);
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router, context) {
      console.log('in addHost.step3:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('3', false);
      controller.set('hideBackButton', false);
      controller.loadAllPriorSteps();
      controller.connectOutlet('wizardStep4', controller.get('content.services'));
    },
    back: Em.Router.transitionTo('step2'),
    next: function (router, context) {
      var addHostController = router.get('addHostController');
      var wizardStep4Controller = router.get('wizardStep4Controller');
      addHostController.saveServices(wizardStep4Controller);
      addHostController.saveClients(wizardStep4Controller);
      App.db.setMasterComponentHosts(undefined);
      router.transitionTo('step4');
    }
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router, context) {
      console.log('in addHost.step4:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('4', false);
      controller.loadAllPriorSteps();
      controller.connectOutlet('wizardStep5', controller.get('content'));

    },
    back: Em.Router.transitionTo('step3'),
    next: function (router, context) {
      var addHostController = router.get('addHostController');
      var wizardStep5Controller = router.get('wizardStep5Controller');
      addHostController.saveMasterComponentHosts(wizardStep5Controller);
      App.db.setSlaveComponentHosts(undefined);
      router.transitionTo('step5');
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',
    connectOutlets: function (router, context) {
      console.log('in addHost.step5:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('5', false);
      controller.loadAllPriorSteps();
      controller.connectOutlet('wizardStep6', controller.get('content'));
    },
    back: Em.Router.transitionTo('step4'),
    next: function (router) {
      var addHostController = router.get('addHostController');
      var wizardStep6Controller = router.get('wizardStep6Controller');

      if (wizardStep6Controller.validate()) {
        addHostController.saveSlaveComponentHosts(wizardStep6Controller);
        App.db.setServiceConfigProperties(null);
        router.transitionTo('step6');
      }
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',
    connectOutlets: function (router) {
      console.log('in addhost.step6:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('6', false);
      controller.loadAllPriorSteps();
      controller.connectOutlet('wizardStep7', controller.get('content'));
    },
    back: Em.Router.transitionTo('step5'),
    next: function (router) {
      var addHostController = router.get('addHostController');
      var wizardStep7Controller = router.get('wizardStep7Controller');
      addHostController.saveServiceConfigProperties(wizardStep7Controller);
      router.transitionTo('step7');
    }
  }),

  step7: Em.Route.extend({
    route: '/step7',
    connectOutlets: function (router, context) {
      console.log('in addHost.step7:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('7', false);
      controller.loadAllPriorSteps();
      controller.connectOutlet('wizardStep8', controller.get('content'));
    },
    back: Em.Router.transitionTo('step6'),
    next: function (router) {
      var addHostController = router.get('addHostController');
      var wizardStep8Controller = router.get('wizardStep8Controller');
      addHostController.installServices();
      addHostController.setInfoForStep9();
      router.transitionTo('step8');
    }
  }),

  step8: Em.Route.extend({
    route: '/step8',
    connectOutlets: function (router, context) {
      console.log('in addHost.step8:connectOutlets');
      var controller = router.get('addHostController');
      controller.setInfoForStep9();
      controller.setCurrentStep('8', false);
      controller.loadAllPriorSteps();
      controller.connectOutlet('wizardStep9', controller.get('content'));
    },
    back: Em.Router.transitionTo('step7'),
    retry: function(router,context) {
      var addHostController = router.get('addHostController');
      var wizardStep9Controller = router.get('wizardStep9Controller');
      addHostController.installServices();
      wizardStep9Controller.navigateStep();
    },
    next: function (router) {
      var addHostController = router.get('addHostController');
      var wizardStep9Controller = router.get('wizardStep9Controller');
      addHostController.saveInstalledHosts(wizardStep9Controller);
      router.transitionTo('step9');
    }
  }),

  step9: Em.Route.extend({
    route: '/step9',
    connectOutlets: function (router, context) {
      console.log('in addHost.step9:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('9', false);
      controller.loadAllPriorSteps();
      controller.connectOutlet('wizardStep10');
    },
    back: Em.Router.transitionTo('step8'),
    complete: function (router, context) {
      if (true) {   // this function will be moved to installerController where it will validate
        var addHostController = router.get('addHostController');
        addHostController.finish();
        router.transitionTo('hosts');
      } else {
        console.log('cluster installation failure');
      }
    }
  }),

  backToHostsList: function (router, event) {
    router.transitionTo('hosts');
  },

  gotoStep1: Em.Router.transitionTo('step1'),

  gotoStep2: Em.Router.transitionTo('step2'),

  gotoStep3: Em.Router.transitionTo('step3'),

  gotoStep4: Em.Router.transitionTo('step4'),

  gotoStep5: Em.Router.transitionTo('step5'),

  gotoStep6: Em.Router.transitionTo('step6'),

  gotoStep7: Em.Router.transitionTo('step7'),

  gotoStep8: Em.Router.transitionTo('step8'),

  gotoStep9: Em.Router.transitionTo('step9'),

  gotoStep10: Em.Router.transitionTo('step10')

});
