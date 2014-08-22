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
  route: '/highAvailability/NameNode/enable',

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
          var currStep = App.router.get('highAvailabilityWizardController.currentStep');
          switch (currStep) {
            case "5" :
            case "7" :
            case "9" :
              if(App.supports.autoRollbackHA){
                this.set('showCloseButton', false);
              }else{
                this.set('showCloseButton', true);
              }
              break;
            default :
              this.set('showCloseButton', true);
          }
        }.observes('App.router.highAvailabilityWizardController.currentStep'),

        onClose: function () {
          var self = this;
          var currStep = App.router.get('highAvailabilityWizardController.currentStep');
          var highAvailabilityProgressPageController = App.router.get('highAvailabilityProgressPageController');
          if(parseInt(currStep) > 4){
            if(!App.supports.autoRollbackHA){
              highAvailabilityProgressPageController.manualRollback();
            } else{
              this.hide();
              App.router.get('highAvailabilityWizardController').setCurrentStep('1');
              App.router.transitionTo('rollbackHighAvailability');
            }
          } else {
            var controller = App.router.get('highAvailabilityWizardController');
            controller.clearTasksData();
            controller.finish();
            App.router.get('updateController').set('isWorking', true);
            App.clusterStatus.setClusterStatus({
              clusterName: controller.get('content.cluster.name'),
              clusterState: 'DEFAULT',
              localdb: App.db.data
            },{alwaysCallback: function() {self.hide();App.router.transitionTo('main.services.index');location.reload();}});
          }
        },
        didInsertElement: function () {
          this.fitHeight();
        }
      });
      highAvailabilityWizardController.set('popup', popup);
      var currentClusterStatus = App.clusterStatus.get('value');
      if (currentClusterStatus) {
        switch (currentClusterStatus.clusterState) {
          case 'HIGH_AVAILABILITY_DEPLOY' :
            highAvailabilityWizardController.setCurrentStep(currentClusterStatus.localdb.HighAvailabilityWizard.currentStep);
            break;
          default:
            var currStep = App.router.get('highAvailabilityWizardController.currentStep');
            highAvailabilityWizardController.setCurrentStep(currStep);
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
      controller.usersLoading().done(function () {
        controller.saveHdfsUser();
        controller.setCurrentStep('1');
        controller.dataLoading().done(function () {
          controller.loadAllPriorSteps();
          controller.connectOutlet('highAvailabilityWizardStep1', controller.get('content'));
        })
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
      var addNN = highAvailabilityWizardStep2Controller.get('selectedServicesMasters').findProperty('isAddNameNode', true).get('selectedHost');
      var sNN = highAvailabilityWizardStep2Controller.get('selectedServicesMasters').findProperty('component_name','SECONDARY_NAMENODE').get('selectedHost');
      if(addNN){
        App.db.setRollBackHighAvailabilityWizardAddNNHost(addNN);
      }
      if(sNN){
        App.db.setRollBackHighAvailabilityWizardSNNHost(sNN);
      }

      controller.saveMasterComponentHosts(highAvailabilityWizardStep2Controller);
      controller.get('content').set('serviceConfigProperties', null);
      controller.setDBProperty('serviceConfigProperties', null);
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
      var controller = router.get('highAvailabilityWizardController');
      var stepController = router.get('highAvailabilityWizardStep3Controller');
      controller.saveServiceConfigProperties(stepController);
      controller.saveConfigTag(stepController.get("hdfsSiteTag"));
      controller.saveConfigTag(stepController.get("coreSiteTag"));
      if (App.Service.find().someProperty('serviceName', 'HBASE')) {
        controller.saveConfigTag(stepController.get("hbaseSiteTag"));
      }
      router.transitionTo('step4');
    },
    back: Em.Router.transitionTo('step2')
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
      var controller = router.get('highAvailabilityWizardController');
      controller.clearTasksData();
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
      var controller = router.get('highAvailabilityWizardController');
      controller.clearTasksData();
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
      var controller = router.get('highAvailabilityWizardController');
      controller.clearTasksData();
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
      App.showConfirmationPopup(function() {
        router.transitionTo('step9');
      }, Em.I18n.t('admin.highAvailability.wizard.step8.confirmPopup.body'));
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
      controller.clearTasksData();
      controller.finish();
      App.clusterStatus.setClusterStatus({
        clusterName: controller.get('content.cluster.name'),
        clusterState: 'DEFAULT',
        localdb: App.db.data
      },{alwaysCallback: function() {controller.get('popup').hide();router.transitionTo('main.services.index');location.reload();}});
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
