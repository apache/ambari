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
  route: '/services/add',

  enter: function (router) {
    console.log('in /service/add:enter');
    if (App.db.getUser().admin) {
      Em.run.next(function () {
        var addServiceController = router.get('addServiceController');
        App.router.get('updateController').set('isWorking', false);
        App.ModalPopup.show({
          classNames: ['full-width-modal'],
          header:Em.I18n.t('services.add.header'),
          bodyClass:  App.AddServiceView.extend({
            controllerBinding: 'App.router.addServiceController'
          }),
          primary:Em.I18n.t('form.cancel'),
          showFooter: false,
          secondary: null,

          onPrimary:function () {
            this.hide();
            App.router.get('updateController').set('isWorking', true);
            App.router.transitionTo('main.services');
          },
          onClose: function() {
            this.hide();
            App.router.get('updateController').set('isWorking', true);
            App.router.transitionTo('main.services')
          }
        });
        router.transitionTo('step' + addServiceController.get('currentStep'));
      });
    } else {
      Em.run.next(function () {
        App.router.transitionTo('main.services');
      });
    }

  },

  /*connectOutlets: function (router) {
    console.log('in /service/add:connectOutlets');
    router.get('mainController').connectOutlet('addService');
  },*/

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      console.log('in addService.step1:connectOutlets');
      var controller = router.get('addServiceController');
      controller.setCurrentStep('1');
      controller.set('hideBackButton', true);
      controller.dataLoading().done(function () {
        controller.loadServicesFromServer();
        controller.loadAllPriorSteps();
        controller.connectOutlet('wizardStep4', controller.get('content.services'));
      })
    },
    next: function (router) {
      var addServiceController = router.get('addServiceController');
      var wizardStep4Controller = router.get('wizardStep4Controller');
      addServiceController.saveServices(wizardStep4Controller);
      addServiceController.saveClients(wizardStep4Controller);
      App.db.setMasterComponentHosts(undefined);
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      console.log('in addService.step2:connectOutlets');
      var controller = router.get('addServiceController');
      controller.setCurrentStep('2');
      controller.set('hideBackButton', false);
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('wizardStep5', controller.get('content'));
      })

    },
    back: Em.Router.transitionTo('step1'),
    next: function (router) {
      var addServiceController = router.get('addServiceController');
      var wizardStep5Controller = router.get('wizardStep5Controller');
      addServiceController.saveMasterComponentHosts(wizardStep5Controller);
      App.db.setSlaveComponentHosts(undefined);
      router.transitionTo('step3');
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router) {
      console.log('in addService.step3:connectOutlets');
      var controller = router.get('addServiceController');
      controller.setCurrentStep('3');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('wizardStep6', controller.get('content'));
      })
    },
    back: function(router){
      var controller = router.get('addServiceController');
      if(!controller.get('content.missMasterStep')){
        router.transitionTo('step2');
      } else {
        router.transitionTo('step1');
      }
    },
    next: function (router) {
      var addServiceController = router.get('addServiceController');
      var wizardStep6Controller = router.get('wizardStep6Controller');

      if (wizardStep6Controller.validate()) {
        addServiceController.saveSlaveComponentHosts(wizardStep6Controller);
        addServiceController.get('content').set('serviceConfigProperties', null);
        App.db.setServiceConfigProperties(null);
        App.db.setSlaveProperties(null);
        //addServiceController.loadSlaveGroupProperties();
        addServiceController.loadAdvancedConfigs();
        router.transitionTo('step4');
      }
    }
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router) {
      console.log('in addService.step4:connectOutlets');
      var controller = router.get('addServiceController');
      controller.setCurrentStep('4');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('wizardStep7', controller.get('content'));
      })
    },
    back: function(router){
      var controller = router.get('addServiceController');
      if(!controller.get('content.missSlavesStep')){
        router.transitionTo('step3');
      } else if(!controller.get('content.missMasterStep')) {
        router.transitionTo('step2');
      } else {
        router.transitionTo('step1');
      }
    },
    next: function (router) {
      var addServiceController = router.get('addServiceController');
      var wizardStep7Controller = router.get('wizardStep7Controller');
      addServiceController.saveServiceConfigProperties(wizardStep7Controller);
      router.transitionTo('step5');
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',
    connectOutlets: function (router, context) {
      console.log('in addService.step5:connectOutlets');
      var controller = router.get('addServiceController');
      controller.setCurrentStep('5');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('wizardStep8', controller.get('content'));
      })
    },
    back: Em.Router.transitionTo('step4'),
    next: function (router) {
      var addServiceController = router.get('addServiceController');
      var wizardStep8Controller = router.get('wizardStep8Controller');
      addServiceController.installServices();
      addServiceController.setInfoForStep9();
      router.transitionTo('step6');
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',
    connectOutlets: function (router, context) {
      console.log('in addService.step6:connectOutlets');
      var controller = router.get('addServiceController');
      controller.setCurrentStep('6');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        if (!App.testMode) {              //if test mode is ON don't disable prior steps link.
          controller.setLowerStepsDisable(6);
        }
        controller.connectOutlet('wizardStep9', controller.get('content'));
      })
    },
    back: Em.Router.transitionTo('step5'),
    retry: function(router,context) {
      var addServiceController = router.get('addServiceController');
      var wizardStep9Controller = router.get('wizardStep9Controller');
      if (!wizardStep9Controller.get('isSubmitDisabled')) {
        if (wizardStep9Controller.get('content.cluster.status') !== 'START FAILED') {
        addServiceController.installServices(true);
        addServiceController.setInfoForStep9();
        } else {
          wizardStep9Controller.set('content.cluster.isCompleted', false);
        }
        wizardStep9Controller.navigateStep();
      }
    },
    unroutePath: function() {
      return false;
    },
    next: function (router) {
      var addServiceController = router.get('addServiceController');
      var wizardStep9Controller = router.get('wizardStep9Controller');
      addServiceController.saveInstalledHosts(wizardStep9Controller);
      router.transitionTo('step7');
    }
  }),

  step7: Em.Route.extend({
    route: '/step7',
    connectOutlets: function (router, context) {
      console.log('in addService.step7:connectOutlets');
      var controller = router.get('addServiceController');
      controller.setCurrentStep('7');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('wizardStep10', controller.get('content'));
      })
    },
    back: Em.Router.transitionTo('step6'),
    complete: function (router, context) {
      if (true) {   // this function will be moved to installerController where it will validate
        router.get('addServiceController').finish();
        $(context.currentTarget).parents("#modal").find(".close").trigger('click');
      }
    }
  }),

  gotoStep1: Em.Router.transitionTo('step1'),

  gotoStep2: Em.Router.transitionTo('step2'),

  gotoStep3: Em.Router.transitionTo('step3'),

  gotoStep4: Em.Router.transitionTo('step4'),

  gotoStep5: Em.Router.transitionTo('step5'),

  gotoStep6: Em.Router.transitionTo('step6'),

  gotoStep7: Em.Router.transitionTo('step7'),

  backToServices: function (router) {
    App.router.get('updateController').set('isWorking', true);
    router.transitionTo('services');
  }

});
