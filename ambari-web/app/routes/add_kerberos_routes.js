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

module.exports = App.WizardRoute.extend({
  route: '/enable',
  enter: function (router) {
    Em.run.next(function () {
      var kerberosWizardController = router.get('kerberosWizardController');
      App.router.get('updateController').set('isWorking', false);
      var popup = App.ModalPopup.show({
        classNames: ['full-width-modal'],
        header: Em.I18n.t('admin.kerberos.wizard.header'),
        bodyClass: App.KerberosWizardView.extend({
          controller: kerberosWizardController
        }),
        primary: Em.I18n.t('form.cancel'),
        showFooter: false,
        secondary: null,

        onClose: function () {
          var self = this;
          var kerberosProgressPageController = App.router.get('kerberosProgressPageController');
          var controller = App.router.get('kerberosWizardController');
          controller.clearTasksData();
          controller.finish();
          App.router.get('updateController').set('isWorking', true);
          if(App.get('testMode')){
            App.router.transitionTo('adminKerberos.index');
            location.reload();
          }
          App.clusterStatus.setClusterStatus({
            clusterName: App.router.getClusterName(),
            clusterState: 'DEFAULT',
            localdb: App.db.data
          }, {alwaysCallback: function () {
            self.hide();
            App.router.transitionTo('adminKerberos.index');
          }});

        },
        didInsertElement: function () {
          this.fitHeight();
        }
      });
      kerberosWizardController.set('popup', popup);
      var currentClusterStatus = App.clusterStatus.get('value');
      if (currentClusterStatus) {
        if (App.testMode) {
          kerberosWizardController.setCurrentStep(App.db.data.KerberosWizard.currentStep);
        } else {
          switch (currentClusterStatus.clusterState) {
            case 'KERBEROS_DEPLOY' :
              kerberosWizardController.setCurrentStep(currentClusterStatus.localdb.KerberosWizard.currentStep);
              break;
            default:
              var currStep = App.router.get('kerberosWizardController.currentStep');
              kerberosWizardController.setCurrentStep(currStep);
              break;
          }
        }

      }
      router.transitionTo('step' + kerberosWizardController.get('currentStep'));
    });
  },

  step1: Em.Route.extend({
    route: '/step1',
    enter: function (router) {
      router.get('kerberosWizardController').setCurrentStep('1');
    },

    connectOutlets: function (router) {
      console.log('in addSecurity.step1:connectOutlets');
      var controller = router.get('kerberosWizardController');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('kerberosWizardStep1', controller.get('content'));
      })
    },

    unroutePath: function () {
      return false;
    },

    next: function (router) {
      var kerberosWizardController = router.get('kerberosWizardController');
      var kerberosStep1controller = router.get('kerberosWizardStep1Controller');
      kerberosWizardController.saveKerberosOption(kerberosStep1controller);
      kerberosWizardController.setDBProperty('serviceConfigProperties', null);
      kerberosWizardController.setDBProperty('advancedServiceConfig', null);
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',

    enter: function (router) {
      router.get('kerberosWizardController').setCurrentStep('2');
    },
    connectOutlets: function (router) {
      console.log('in kerberosWizardController.step2:connectOutlets');
      var controller = router.get('kerberosWizardController');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        var kerberosWizardStep2Controller = router.get('kerberosWizardStep2Controller');
        kerberosWizardStep2Controller.set('wizardController', controller);
        controller.connectOutlet('kerberosWizardStep2', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    back: Em.Router.transitionTo('step1'),
    next: function (router) {
      var kerberosWizardController = router.get('kerberosWizardController');
      var kerberosWizardStep2Controller = router.get('kerberosWizardStep2Controller');
      kerberosWizardController.saveServiceConfigProperties(kerberosWizardStep2Controller);
      kerberosWizardController.clearTasksData();
      router.transitionTo('step3');
    }
  }),
  step3: Em.Route.extend({
    route: '/step3',

    enter: function (router) {
      router.get('kerberosWizardController').setCurrentStep('3');
    },
    connectOutlets: function (router) {
      console.log('in kerberosWizardController.step3:connectOutlets');
      var controller = router.get('kerberosWizardController');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('kerberosWizardStep3', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    back: Em.Router.transitionTo('step2'),
    next: function (router) {
      // load kerberos descriptor for all services
      router.transitionTo('step4');
    }
  }),

  step4: Em.Route.extend({
    route: '/step4',

    enter: function (router) {
      router.get('kerberosWizardController').setCurrentStep('4');
    },
    connectOutlets: function (router) {
      console.log('in kerberosWizardController.step4:connectOutlets');
      var controller = router.get('kerberosWizardController');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('kerberosWizardStep4', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    back: Em.Router.transitionTo('step3'),
    next: function (router) {
      router.transitionTo('step5');
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',

    enter: function (router) {
      router.get('kerberosWizardController').setCurrentStep('5');
    },
    connectOutlets: function (router) {
      console.log('in kerberosWizardController.step5:connectOutlets');
      var controller = router.get('kerberosWizardController');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('kerberosWizardStep5', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    back: Em.Router.transitionTo('step4'),
    next: function (router) {
      router.transitionTo('step6');
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',

    enter: function (router) {
      router.get('kerberosWizardController').setCurrentStep('6');
    },
    connectOutlets: function (router) {
      console.log('in kerberosWizardController.step6:connectOutlets');
      var controller = router.get('kerberosWizardController');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('kerberosWizardStep5', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    next: function (router) {
      var controller = router.get('kerberosWizardController');
      controller.finish();
      App.clusterStatus.setClusterStatus({
        clusterName: App.router.getClusterName(),
        clusterState: 'DEFAULT',
        localdb: App.db.data
      }, {alwaysCallback: function () {
        self.hide();
        App.router.transitionTo('adminKerberos.index');
      }});

    }
  }),


  gotoStep1: Em.Router.transitionTo('step1'),

  gotoStep2: Em.Router.transitionTo('step2'),

  gotoStep3: Em.Router.transitionTo('step3'),

  gotoStep4: Em.Router.transitionTo('step4'),

  gotoStep5: Em.Router.transitionTo('step5'),

  gotoStep6: Em.Router.transitionTo('step6')

});