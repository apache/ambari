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
    router.get('mainController').dataLoading().done(function () {
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
          switch (kerberosWizardController.get('currentStep')) {
            case "2":
              var step2Controller = router.get('kerberosWizardStep2Controller');
              if (step2Controller.get('testConnectionInProgress')) {
                step2Controller.showConnectionInProgressPopup(function () {
                  kerberosWizardController.warnBeforeExitPopup(function () {
                    self.exitWizard();
                  }, false);
                });
              } else {
                kerberosWizardController.warnBeforeExitPopup(function () {
                  self.exitWizard();
                }, false);
              }
              break;
            case "6":
            case "7":
              kerberosWizardController.warnBeforeExitPopup(function () {
                self.exitWizard();
              }, true);
              break;
            default:
              kerberosWizardController.warnBeforeExitPopup(function () {
                self.exitWizard();
              }, false);
          }
        },
        didInsertElement: function () {
          this.fitHeight();
        },

        exitWizard: function () {
          var self = this;
          var kerberosProgressPageController = App.router.get('kerberosProgressPageController');
          var controller = App.router.get('kerberosWizardController');
          var exitPath = controller.getDBProperty('onClosePath') || 'adminKerberos.index';
          controller.clearTasksData();
          controller.finish();
          App.get('router.updateController').set('isWorking', true);
          if (App.get('testMode')) {
            App.get('router').transitionTo('adminKerberos.index');
            Em.run.next(function() {
              location.reload();
            });
          }
          App.clusterStatus.setClusterStatus({
            clusterName: App.router.getClusterName(),
            clusterState: 'DEFAULT',
            localdb: App.db.data
          }, {
            alwaysCallback: function () {
              self.hide();
              App.get('router').transitionTo(exitPath);
              Em.run.next(function() {
                location.reload();
              });
            }
          });
        }
      });
      kerberosWizardController.set('popup', popup);
      var currentClusterStatus = App.clusterStatus.get('value');
      if (currentClusterStatus) {
        if (App.get('testMode')) {
          kerberosWizardController.setCurrentStep(App.db.data.KerberosWizard.currentStep);
        } else {
          switch (currentClusterStatus.clusterState) {
            case 'KERBEROS_DEPLOY' :
              kerberosWizardController.setCurrentStep(currentClusterStatus.localdb.KerberosWizard.currentStep);
              break;
            default:
              var currStep = App.get('router.kerberosWizardController.currentStep');
              kerberosWizardController.setCurrentStep(currStep);
              break;
          }
        }

      }
      Em.run.next(function(){
        router.transitionTo('step' + kerberosWizardController.get('currentStep'));
      });
    });
  },

  step1: Em.Route.extend({
    route: '/step1',

    connectOutlets: function (router) {
      console.log('in addSecurity.step1:connectOutlets');
      var controller = router.get('kerberosWizardController');
      controller.dataLoading().done(function () {
        router.get('kerberosWizardController').setCurrentStep('1');
        controller.loadAllPriorSteps();
        controller.connectOutlet('kerberosWizardStep1', controller.get('content'));
      });
    },

    unroutePath: function () {
      return false;
    },

    next: function (router) {
      var kerberosWizardController = router.get('kerberosWizardController');
      var kerberosStep1controller = router.get('kerberosWizardStep1Controller');

      kerberosWizardController.saveKerberosOption(kerberosStep1controller);
      kerberosWizardController.setDBProperty('serviceConfigProperties', null);
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',

    connectOutlets: function (router) {
      console.log('in kerberosWizardController.step2:connectOutlets');
      var controller = router.get('kerberosWizardController');
      controller.dataLoading().done(function () {
        router.get('kerberosWizardController').setCurrentStep('2');
        controller.loadAllPriorSteps();
        var kerberosWizardStep2Controller = router.get('kerberosWizardStep2Controller');
        kerberosWizardStep2Controller.set('wizardController', controller);
        controller.connectOutlet('kerberosWizardStep2', controller.get('content'));
      });
    },
    unroutePath: function () {
      return false;
    },
    back: function(router) {
      var controller = router.get('kerberosWizardStep2Controller');
      var kerberosWizardController = router.get('kerberosWizardController');
      if (!controller.get('isBackBtnDisabled')) {
        kerberosWizardController.overrideVisibility(controller.get('configs'), true, []);
        router.transitionTo('step1');
      }
    },

    next: function (router) {
      var kerberosWizardController = router.get('kerberosWizardController');
      var kerberosWizardStep2Controller = router.get('kerberosWizardStep2Controller');

      if (kerberosWizardController.get('skipClientInstall')) {
        kerberosWizardStep2Controller.get('stepConfigs')[0].get('configs').findProperty('name', 'manage_identities').set('value', 'false');
        kerberosWizardStep2Controller.get('stepConfigs')[0].get('configs').findProperty('name', 'install_packages').set('value', 'false');
        kerberosWizardStep2Controller.get('stepConfigs')[0].get('configs').findProperty('name', 'manage_krb5_conf').set('value', 'false');
      }

      kerberosWizardController.saveServiceConfigProperties(kerberosWizardStep2Controller);
      kerberosWizardController.clearTasksData();
      if (kerberosWizardController.get('skipClientInstall')) {
        kerberosWizardController.setDBProperty('kerberosDescriptorConfigs', null);
        router.transitionTo('step4');
      } else {
        router.transitionTo('step3');
      }
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',

    connectOutlets: function (router) {
      console.log('in kerberosWizardController.step3:connectOutlets');
      var controller = router.get('kerberosWizardController');
      controller.dataLoading().done(function () {
        router.get('kerberosWizardController').setCurrentStep('3');
        controller.loadAllPriorSteps();
        controller.connectOutlet('kerberosWizardStep3', controller.get('content'));
      });
    },
    unroutePath: function () {
      return false;
    },
    back: Em.Router.transitionTo('step2'),
    next: function (router) {
      var kerberosWizardController = router.get('kerberosWizardController');
      kerberosWizardController.setDBProperty('kerberosDescriptorConfigs', null);
      kerberosWizardController.clearCachedStepConfigValues(router.get('kerberosWizardStep4Controller'));
      router.transitionTo('step4');
    }
  }),

  step4: Em.Route.extend({
    route: '/step4',

    connectOutlets: function (router) {
      console.log('in kerberosWizardController.step4:connectOutlets');
      var controller = router.get('kerberosWizardController');
      var step4Controller = router.get('kerberosWizardStep4Controller');
      controller.setCurrentStep(4);
      controller.dataLoading().done(function () {
        router.get('kerberosWizardController').setCurrentStep('4');
        controller.loadAllPriorSteps();
        controller.setLowerStepsDisable(4);
        step4Controller.set('wizardController', controller);
        controller.connectOutlet('kerberosWizardStep4', controller.get('content'));
      });
    },
    unroutePath: function () {
      return false;
    },
    back: function (router) {
      if (router.get('kerberosWizardController.skipClientInstall')) {
        router.transitionTo('step2');
      } else {
        router.transitionTo('step3');
      }
    },
    next: function (router) {
      var kerberosWizardController = router.get('kerberosWizardController');
      var step5Controller = router.get('kerberosWizardStep5Controller');
      var kerberosDescriptor = kerberosWizardController.get('kerberosDescriptorConfigs');
      kerberosWizardController.cacheStepConfigValues(router.get('kerberosWizardStep4Controller'));
      step5Controller.postKerberosDescriptor(kerberosDescriptor).always(function (data, result, request) {
        if (result === 'error' && data.status === 409) {
          step5Controller.putKerberosDescriptor(kerberosDescriptor);
        } else {
          step5Controller.unkerberizeCluster();
        }
      });
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',

    connectOutlets: function (router) {
      console.log('in kerberosWizardController.step5:connectOutlets');
      var controller = router.get('kerberosWizardController');
      controller.dataLoading().done(function () {
        router.get('kerberosWizardController').setCurrentStep('5');
        controller.setLowerStepsDisable(5);
        controller.loadAllPriorSteps();
        controller.connectOutlet('kerberosWizardStep5', controller.get('content'));
      });
    },

    unroutePath: function () {
      return false;
    },

    exitWizard: function (router) {
      var popup = router.get('kerberosWizardController.popup');
      popup.onClose();
    },

    downloadCSV: function (router) {
      var kerberosWizardStep5Controller = router.get('kerberosWizardStep5Controller');
      kerberosWizardStep5Controller.getCSVData();
    },

    back: Em.Router.transitionTo('step4'),

    next: function (router) {
      var kerberosWizardController = router.get('kerberosWizardController');
      kerberosWizardController.setDBProperties({
        tasksStatuses: null,
        tasksRequestIds: null
      });
      router.transitionTo('step6');
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',

    connectOutlets: function (router) {
      console.log('in kerberosWizardController.step6:connectOutlets');
      var controller = router.get('kerberosWizardController');
      controller.dataLoading().done(function () {
        router.get('kerberosWizardController').setCurrentStep('6');
        controller.setLowerStepsDisable(6);
        controller.loadAllPriorSteps();
        controller.connectOutlet('kerberosWizardStep6', controller.get('content'));
      });
    },
    unroutePath: function () {
      return false;
    },
    back: Em.Router.transitionTo('step4'),
    next: function (router) {
      var kerberosWizardController = router.get('kerberosWizardController');
      kerberosWizardController.setDBProperties({
        tasksStatuses: null,
        tasksRequestIds: null
      });
      router.transitionTo('step7');
    }
  }),

  step7: Em.Route.extend({
    route: '/step7',

    connectOutlets: function (router) {
      console.log('in kerberosWizardController.step7:connectOutlets');
      var controller = router.get('kerberosWizardController');
      var step7Controller = router.get('kerberosWizardStep7Controller');
      controller.dataLoading().done(function () {
        router.get('kerberosWizardController').setCurrentStep('7');
        controller.setLowerStepsDisable(7);
        controller.loadAllPriorSteps();
        step7Controller.setRequest();
        controller.connectOutlet('kerberosWizardStep7', controller.get('content'));
      });
    },
    unroutePath: function () {
      return false;
    },
    back: Em.Router.transitionTo('step4'),
    next: function (router) {
      router.transitionTo('step8');
    }
  }),

  step8: Em.Route.extend({
    route: '/step8',

    connectOutlets: function (router) {
      console.log('in kerberosWizardController.step8:connectOutlets');
      var controller = router.get('kerberosWizardController');
      controller.dataLoading().done(function () {
        router.get('kerberosWizardController').setCurrentStep('8');
        controller.setLowerStepsDisable(8);
        controller.loadAllPriorSteps();
        controller.connectOutlet('kerberosWizardStep8', controller.get('content'));
      });
    },
    unroutePath: function () {
      return false;
    },
    back: Em.Router.transitionTo('step7'),
    next: function (router) {
      var controller = router.get('kerberosWizardController');
      controller.finish();
      App.clusterStatus.setClusterStatus({
        clusterName: App.get('router').getClusterName(),
        clusterState: 'DEFAULT',
        localdb: App.db.data
      }, {
        alwaysCallback: function () {
          controller.get('popup').hide();
          App.get('router').transitionTo('adminKerberos.index');
          Em.run.next(function() {
            location.reload();
          });
        }
      });

    }
  })
});
