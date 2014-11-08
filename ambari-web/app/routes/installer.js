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

module.exports = Em.Route.extend({
  route: '/installer',
  App: require('app'),

  enter: function (router) {
    console.log('in /installer:enter');

    App.clusterStatus.set('wizardControllerName', App.router.get('installerController.name'));

    router.getAuthenticated().done(function (loggedIn) {
      if (loggedIn) {
        var applicationController = router.get('applicationController');
        applicationController.startKeepAlivePoller();
        // check server/web client versions match
        App.router.get('installerController').checkServerClientVersion().done(function () {

          var name = 'Cluster Install Wizard';
          $('title').text('Ambari - ' + name);

          App.router.get('mainViewsController').loadAmbariViews();
          if (App.get('isAdmin')) {
            router.get('mainController').stopPolling();
            console.log('In installer with successful authenticated');
            console.log('current step=' + router.get('installerController.currentStep'));
            Ember.run.next(function () {
              var installerController = router.get('installerController');
              App.clusterStatus.updateFromServer().complete(function () {
                var currentClusterStatus = App.clusterStatus.get('value');
                //@TODO: Clean up  following states. Navigation should be done solely via currentStep stored in the localDb and API persist endpoint.
                //       Actual currentStep value for the installer controller should always remain in sync with localdb and at persist store in the server.
                if (currentClusterStatus) {
                  switch (currentClusterStatus.clusterState) {
                    case 'CLUSTER_NOT_CREATED_1' :
                      var localDb = currentClusterStatus.localdb;
                      if (localDb && localDb.Installer && localDb.Installer.currentStep) {
                        App.db.data = currentClusterStatus.localdb;
                        App.router.setAuthenticated(true);
                        var controllerName = installerController.get('name');
                        var suffixLength = 10;
                        var currentStep = App.get('router').getWizardCurrentStep(controllerName.substr(0, controllerName.length - suffixLength));
                        installerController.setCurrentStep(currentStep);
                      }
                      router.transitionTo('step' + installerController.get('currentStep'));
                      break;
                    case 'CLUSTER_DEPLOY_PREP_2' :
                      installerController.setCurrentStep('8');
                      App.db.data = currentClusterStatus.localdb;
                      App.router.setAuthenticated(true);
                      router.transitionTo('step' + installerController.get('currentStep'));
                      break;
                    case 'CLUSTER_INSTALLING_3' :
                    case 'SERVICE_STARTING_3' :
                      if (!installerController.get('isStep9')) {
                        installerController.setCurrentStep('9');
                      }
                      router.transitionTo('step' + installerController.get('currentStep'));
                      break;
                    case 'CLUSTER_INSTALLED_4' :
                      if (!installerController.get('isStep10')) {
                        installerController.setCurrentStep('10');
                      }
                      App.db.data = currentClusterStatus.localdb;
                      App.router.setAuthenticated(true);
                      router.transitionTo('step' + installerController.get('currentStep'));
                      break;
                    case 'DEFAULT' :
                    default:
                      router.transitionTo('main.dashboard.index');
                      break;
                  }
                }
              });
            });
          } else {
            Em.run.next(function () {
              App.router.transitionTo('main.views.index');
            });
          }
        });
      } else {
        console.log('In installer but its not authenticated');
        console.log('value of authenticated is: ' + router.getAuthenticated());
        Ember.run.next(function () {
          router.transitionTo('login');
        });
      }
    });
  },

  routePath: function (router, event) {
    console.log("INFO: value of router is: " + router);
    console.log("INFO: value of event is: " + event);
    router.setNavigationFlow(event);
    if (!router.isFwdNavigation) {
      this._super(router, event);
    } else {
      router.set('backBtnForHigherStep', true);

      var installerController = router.get('installerController');
      router.transitionTo('step' + installerController.get('currentStep'));
    }
  },

  connectOutlets: function (router, context) {
    console.log('in /installer:connectOutlets');
    router.get('applicationController').connectOutlet('installer');
  },

  step0: Em.Route.extend({
    route: '/step0',
    connectOutlets: function (router) {
      console.log('in installer.step0:connectOutlets');
      var controller = router.get('installerController');
      controller.setCurrentStep('0');
      controller.loadAllPriorSteps().done(function () {
        controller.connectOutlet('wizardStep0', controller.get('content'));
      });
    },

    next: function (router) {
      var installerController = router.get('installerController');
      installerController.save('cluster');
      App.db.setStacks(undefined);
      installerController.set('content.stacks',undefined);
      router.transitionTo('step1');
    }
  }),

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      console.log('in installer.step1:connectOutlets');
      var controller = router.get('installerController');
      controller.setCurrentStep('1');
      controller.loadAllPriorSteps().done(function () {
        controller.connectOutlet('wizardStep1', controller.get('content'));
      });
    },
    back: Em.Router.transitionTo('step0'),
    next: function (router) {
      var wizardStep1Controller = router.get('wizardStep1Controller');
      var installerController = router.get('installerController');
      installerController.checkRepoURL(wizardStep1Controller).done(function () {
        installerController.setDBProperty('service', undefined);
        installerController.setStacks();
        installerController.clearInstallOptions();
        router.transitionTo('step2');
      });
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router, context) {
      router.setNavigationFlow('step2');

      var controller = router.get('installerController');
      controller.setCurrentStep('2');
      controller.loadAllPriorSteps().done(function () {
        controller.connectOutlet('wizardStep2', controller.get('content'));
      });
    },
    back: Em.Router.transitionTo('step1'),
    next: function (router) {
      var controller = router.get('installerController');
      controller.save('installOptions');
      //hosts was saved to content.hosts inside wizardStep2Controller
      controller.save('hosts');
      router.transitionTo('step3');
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router) {
      console.log('in installer.step3:connectOutlets');
      var controller = router.get('installerController');
      controller.setCurrentStep('3');
      controller.loadAllPriorSteps().done(function () {
        var wizardStep3Controller = router.get('wizardStep3Controller');
        wizardStep3Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep3', controller.get('content'));
      });
    },
    back: function (router) {
      router.transitionTo('step2');
    },
    next: function (router, context) {
      var installerController = router.get('installerController');
      var wizardStep3Controller = router.get('wizardStep3Controller');
      installerController.saveConfirmedHosts(wizardStep3Controller);
      installerController.setDBProperty('bootStatus', true);
      installerController.setDBProperty('selectedServiceNames', undefined);
      installerController.setDBProperty('installedServiceNames', undefined);
      router.transitionTo('step4');
    },
    exit: function (router) {
      router.get('wizardStep3Controller').set('stopBootstrap', true);
    },
    /**
     * Wrapper for remove host action.
     * Since saving data stored in installerController, we should call this from router
     * @param router
     * @param context Array of hosts to delete
     */
    removeHosts: function (router, context) {
      console.log('in installer.step2.removeHosts:hosts to delete ', context);
      var controller = router.get('installerController');
      controller.removeHosts(context);
    }
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router, context) {
      router.setNavigationFlow('step4');
      var controller = router.get('installerController');
      controller.setCurrentStep('4');
      controller.loadAllPriorSteps().done(function () {
        controller.connectOutlet('wizardStep4', App.StackService.find());
      });
    },
    back: Em.Router.transitionTo('step3'),

    next: function (router) {
      var controller = router.get('installerController');
      var wizardStep4Controller = router.get('wizardStep4Controller');
      controller.saveServices(wizardStep4Controller);
      controller.saveClients(wizardStep4Controller);

      router.get('wizardStep5Controller').clearRecommendations(); // Force reload recommendation between steps 4 and 5
      controller.setDBProperty('recommendations', undefined);
      controller.setDBProperty('masterComponentHosts', undefined);
      router.transitionTo('step5');
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',
    connectOutlets: function (router, context) {
      router.setNavigationFlow('step5');

      var controller = router.get('installerController');
      router.get('wizardStep5Controller').set('servicesMasters', []);
      controller.setCurrentStep('5');
      controller.loadAllPriorSteps().done(function () {
        controller.connectOutlet('wizardStep5', controller.get('content'));
      });
    },
    back: Em.Router.transitionTo('step4'),
    next: function (router) {
      var controller = router.get('installerController');
      var wizardStep5Controller = router.get('wizardStep5Controller');
      var wizardStep6Controller = router.get('wizardStep6Controller');
      controller.saveMasterComponentHosts(wizardStep5Controller);
      controller.setDBProperty('slaveComponentHosts', undefined);
      controller.setDBProperty('recommendations', wizardStep5Controller.get('content.recommendations'));
      wizardStep6Controller.set('isClientsSet', false);
      router.transitionTo('step6');
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',
    connectOutlets: function (router, context) {
      router.setNavigationFlow('step6');

      var controller = router.get('installerController');
      router.get('wizardStep6Controller').set('hosts', []);
      controller.setCurrentStep('6');
      controller.loadAllPriorSteps().done(function () {
        controller.connectOutlet('wizardStep6', controller.get('content'));
      });
    },
    back: Em.Router.transitionTo('step5'),

    next: function (router) {
      var controller = router.get('installerController');
      var wizardStep6Controller = router.get('wizardStep6Controller');
      var wizardStep7Controller = router.get('wizardStep7Controller');

      if (!wizardStep6Controller.get('submitDisabled')) {
        wizardStep6Controller.showValidationIssuesAcceptBox(function () {
          controller.saveSlaveComponentHosts(wizardStep6Controller);
          controller.get('content').set('serviceConfigProperties', null);
          controller.setDBProperty('serviceConfigProperties', null);
          controller.setDBProperty('advancedServiceConfig', null);
          controller.setDBProperty('serviceConfigGroups', null);
          controller.setDBProperty('recommendationsHostGroups', wizardStep6Controller.get('content.recommendationsHostGroups'));
          controller.setDBProperty('recommendationsConfigs', null);
          controller.loadAdvancedConfigs(wizardStep7Controller);
          wizardStep7Controller.set('isAdvancedConfigLoaded', false);
          router.transitionTo('step7');
        });
      }
    }
  }),

  step7: Em.Route.extend({
    route: '/step7',
    enter: function (router) {
      console.log('in /wizardStep7Controller:enter');
      var controller = router.get('installerController');
      controller.setCurrentStep('7');
    },
    connectOutlets: function (router, context) {
      var controller = router.get('installerController');

      controller.loadAllPriorSteps().done(function () {
        var wizardStep7Controller = router.get('wizardStep7Controller');
        wizardStep7Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep7', controller.get('content'));
      });
    },
    back: Em.Router.transitionTo('step6'),
    next: function (router) {
      var controller = router.get('installerController');
      var wizardStep7Controller = router.get('wizardStep7Controller');
      controller.saveServiceConfigProperties(wizardStep7Controller);
      if (App.supports.hostOverridesInstaller) {
        controller.saveServiceConfigGroups(wizardStep7Controller);
      }
      controller.setDBProperty('recommendationsConfigs', wizardStep7Controller.get('recommendationsConfigs'));
      router.transitionTo('step8');
    }
  }),

  step8: Em.Route.extend({
    route: '/step8',
    connectOutlets: function (router, context) {
      console.log('in installer.step8:connectOutlets');
      var controller = router.get('installerController');
      controller.setCurrentStep('8');
      controller.loadAllPriorSteps().done(function () {
        var wizardStep8Controller = router.get('wizardStep8Controller');
        wizardStep8Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep8', controller.get('content'));
      });
    },
    back: Em.Router.transitionTo('step7'),
    next: function (router) {
      var installerController = router.get('installerController');
      var wizardStep8Controller = router.get('wizardStep8Controller');
      // invoke API call to install selected services
      installerController.installServices(false, function () {
        installerController.setInfoForStep9();
        // We need to do recovery based on whether we are in Add Host or Installer wizard
        installerController.saveClusterState('CLUSTER_INSTALLING_3');
        wizardStep8Controller.set('servicesInstalled', true);
        router.transitionTo('step9');
      });
    }
  }),

  step9: Em.Route.extend({
    route: '/step9',
    connectOutlets: function (router, context) {
      console.log('in installer.step9:connectOutlets');
      var controller = router.get('installerController');
      controller.setCurrentStep('9');
      controller.loadAllPriorSteps().done(function () {
        if (!App.get('testMode')) {
          controller.setLowerStepsDisable(9);
        }
        var wizardStep9Controller = router.get('wizardStep9Controller');
        wizardStep9Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep9', controller.get('content'));
      });
    },
    back: Em.Router.transitionTo('step8'),
    retry: function (router) {
      var installerController = router.get('installerController');
      var wizardStep9Controller = router.get('wizardStep9Controller');
      if (wizardStep9Controller.get('showRetry')) {
        if (wizardStep9Controller.get('content.cluster.status') === 'INSTALL FAILED') {
          var isRetry = true;
          installerController.installServices(isRetry, function () {
            installerController.setInfoForStep9();
            wizardStep9Controller.resetHostsForRetry();
            // We need to do recovery based on whether we are in Add Host or Installer wizard
            installerController.saveClusterState('CLUSTER_INSTALLING_3');
            wizardStep9Controller.navigateStep();
          });
        } else {
          wizardStep9Controller.navigateStep();
        }
      }
    },
    unroutePath: function (router, context) {
      // exclusion for transition to Admin View
      if (context === '/adminView') {
        this._super(router, context);
      } else {
        return false;
      }
    },
    next: function (router) {
      var installerController = router.get('installerController');
      var wizardStep9Controller = router.get('wizardStep9Controller');
      installerController.saveInstalledHosts(wizardStep9Controller);

      installerController.saveClusterState('CLUSTER_INSTALLED_4');
      router.transitionTo('step10');
    }
  }),

  step10: Em.Route.extend({
    route: '/step10',
    connectOutlets: function (router, context) {
      console.log('in installer.step10:connectOutlets');
      var controller = router.get('installerController');
      controller.setCurrentStep('10');
      controller.loadAllPriorSteps().done(function () {
        if (!App.get('testMode')) {
          controller.setLowerStepsDisable(10);
        }
        controller.connectOutlet('wizardStep10', controller.get('content'));
      });
    },
    back: Em.Router.transitionTo('step9'),
    complete: function (router, context) {
      var controller = router.get('installerController');
      controller.finish();
      controller.setClusterProvisioningState('INSTALLED').complete(function () {
        // We need to do recovery based on whether we are in Add Host or Installer wizard
        controller.saveClusterState('DEFAULT');
        App.router.set('clusterController.isLoaded', false);
        router.set('clusterInstallCompleted', true);
        router.transitionTo('main.dashboard.index');
      });
    }
  }),

  gotoStep0: Em.Router.transitionTo('step0'),

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
