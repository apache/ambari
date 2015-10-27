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

module.exports = Em.Route.extend(App.RouterRedirections, {
  route: '/installer',
  App: require('app'),

  enter: function (router) {
    console.log('in /installer:enter');
    var self = this;

    App.clusterStatus.set('wizardControllerName', App.router.get('installerController.name'));

    router.getAuthenticated().done(function (loggedIn) {
      if (loggedIn) {
        var applicationController = router.get('applicationController');
        App.router.get('experimentalController').loadSupports().complete(function () {
          applicationController.startKeepAlivePoller();
          // check server/web client versions match
          App.router.get('installerController').checkServerClientVersion().done(function () {

            var name = 'Cluster Install Wizard';
            $('title').text('Ambari - ' + name);
            $('#main').addClass('install-wizard-content');

            App.router.get('mainViewsController').loadAmbariViews();
            if (App.isAccessible('ADMIN')) {
              router.get('mainController').stopPolling();
              console.log('In installer with successful authenticated');
              console.log('current step=' + router.get('installerController.currentStep'));
              Em.run.next(function () {
                App.clusterStatus.updateFromServer().complete(function () {
                  var currentClusterStatus = App.clusterStatus.get('value');
                  //@TODO: Clean up  following states. Navigation should be done solely via currentStep stored in the localDb and API persist endpoint.
                  //       Actual currentStep value for the installer controller should always remain in sync with localdb and at persist store in the server.
                  if (currentClusterStatus) {
                    if (self.get('installerStatuses').contains(currentClusterStatus.clusterState)) {
                      self.redirectToInstaller(router, currentClusterStatus, true);
                    }
                    else {
                      router.transitionTo('main.dashboard.index');
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
      console.time('step0 connectOutlets');
      console.log('in installer.step0:connectOutlets');
      var controller = router.get('installerController');
      controller.setCurrentStep('0');
      controller.loadAllPriorSteps().done(function () {
        controller.connectOutlet('wizardStep0', controller.get('content'));
        console.timeEnd('step0 connectOutlets');
      });
    },

    next: function (router) {
      console.time('step0 next');
      var installerController = router.get('installerController');
      installerController.save('cluster');
      App.db.setStacks(undefined);
      installerController.set('content.stacks',undefined);
      router.transitionTo('step1');
      console.timeEnd('step0 next');
    }
  }),

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      console.time('step1 connectOutlets');
      console.log('in installer.step1:connectOutlets');
      var controller = router.get('installerController');
      controller.setCurrentStep('1');
      controller.loadAllPriorSteps().done(function () {
        controller.connectOutlet('wizardStep1', controller.get('content'));
        console.timeEnd('step1 connectOutlets');
      });
    },
    back: Em.Router.transitionTo('step0'),
    next: function (router) {
      console.time('step1 next');
      var wizardStep1Controller = router.get('wizardStep1Controller');
      var installerController = router.get('installerController');
      installerController.validateJDKVersion(function() {
        installerController.checkRepoURL(wizardStep1Controller).done(function () {
          installerController.setDBProperty('service', undefined);
          installerController.setStacks();
          installerController.clearInstallOptions();
          router.transitionTo('step2');
          console.timeEnd('step1 next');
        });
      }, function() {});
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router, context) {
      console.time('step2 connectOutlets');
      router.setNavigationFlow('step2');

      var controller = router.get('installerController');
      controller.setCurrentStep('2');
      controller.loadAllPriorSteps().done(function () {
        controller.connectOutlet('wizardStep2', controller.get('content'));
        console.timeEnd('step2 connectOutlets');
      });
    },
    back: Em.Router.transitionTo('step1'),
    next: function (router) {
      console.time('step2 next');
      var controller = router.get('installerController');
      controller.save('installOptions');
      //hosts was saved to content.hosts inside wizardStep2Controller
      controller.save('hosts');
      router.transitionTo('step3');
      console.timeEnd('step2 next');
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router) {
      console.time('step3 connectOutlets');
      console.log('in installer.step3:connectOutlets');
      var controller = router.get('installerController');
      controller.setCurrentStep('3');
      controller.loadAllPriorSteps().done(function () {
        var wizardStep3Controller = router.get('wizardStep3Controller');
        wizardStep3Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep3', controller.get('content'));
        console.timeEnd('step3 connectOutlets');
      });
    },
    back: function (router) {
      router.transitionTo('step2');
    },
    next: function (router, context) {
      console.time('step3 next');
      if (!router.transitionInProgress) {
        router.set('transitionInProgress', true);
        var installerController = router.get('installerController');
        var wizardStep3Controller = router.get('wizardStep3Controller');
        installerController.saveConfirmedHosts(wizardStep3Controller);
        installerController.setDBProperties({
          bootStatus: true,
          selectedServiceNames: undefined,
          installedServiceNames: undefined
        });
        router.transitionTo('step4');
        console.timeEnd('step3 next');
      }
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
      console.time('step4 connectOutlets');
      router.setNavigationFlow('step4');
      var controller = router.get('installerController');
      controller.setCurrentStep('4');
      controller.loadAllPriorSteps().done(function () {
        var wizardStep4Controller = router.get('wizardStep4Controller');
        wizardStep4Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep4', App.StackService.find().filterProperty('isInstallable', true));
        console.timeEnd('step4 connectOutlets');
      });
    },
    back: Em.Router.transitionTo('step3'),

    next: function (router) {
      console.time('step4 next');
      var controller = router.get('installerController');
      var wizardStep4Controller = router.get('wizardStep4Controller');
      controller.saveServices(wizardStep4Controller);
      controller.saveClients(wizardStep4Controller);

      router.get('wizardStep5Controller').clearRecommendations(); // Force reload recommendation between steps 4 and 5
      controller.setDBProperties({
        recommendations: undefined,
        masterComponentHosts: undefined
      });
      controller.set('stackConfigsLoaded', false);
      router.transitionTo('step5');
      console.timeEnd('step4 next');
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',
    connectOutlets: function (router, context) {
      console.time('step5 connectOutlets');
      router.setNavigationFlow('step5');

      var controller = router.get('installerController');
      var wizardStep5Controller = router.get('wizardStep5Controller');
      wizardStep5Controller.setProperties({
        servicesMasters: [],
        isInitialLayout: true
      });
      controller.set('stackConfigsLoaded', false);
      controller.setCurrentStep('5');
      controller.loadAllPriorSteps().done(function () {
        controller.connectOutlet('wizardStep5', controller.get('content'));
        console.timeEnd('step5 connectOutlets');
      });
    },
    back: Em.Router.transitionTo('step4'),
    next: function (router) {
      console.time('step5 next');
      var controller = router.get('installerController');
      var wizardStep5Controller = router.get('wizardStep5Controller');
      var wizardStep6Controller = router.get('wizardStep6Controller');
      controller.saveMasterComponentHosts(wizardStep5Controller);
      controller.setDBProperties({
        slaveComponentHosts: undefined,
        recommendations: wizardStep5Controller.get('content.recommendations')
      });
      wizardStep6Controller.set('isClientsSet', false);
      router.transitionTo('step6');
      console.timeEnd('step5 next');
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',
    connectOutlets: function (router, context) {
      console.time('step6 connectOutlets');
      router.setNavigationFlow('step6');

      var controller = router.get('installerController');
      router.get('wizardStep6Controller').set('hosts', []);
      controller.setCurrentStep('6');
      controller.loadAllPriorSteps().done(function () {
        controller.connectOutlet('wizardStep6', controller.get('content'));
        console.timeEnd('step6 connectOutlets');
      });
    },
    back: Em.Router.transitionTo('step5'),

    next: function (router) {
      console.time('step6 next');
      var controller = router.get('installerController');
      var wizardStep6Controller = router.get('wizardStep6Controller');
      var wizardStep7Controller = router.get('wizardStep7Controller');
      if (!wizardStep6Controller.get('submitDisabled')) {
        wizardStep6Controller.showValidationIssuesAcceptBox(function () {
          if (!router.transitionInProgress) {
            router.set('transitionInProgress', true);
            controller.saveSlaveComponentHosts(wizardStep6Controller);
            controller.get('content').set('serviceConfigProperties', null);
            controller.setDBProperties({
              serviceConfigProperties: null,
              serviceConfigGroups: null,
              recommendationsHostGroups: wizardStep6Controller.get('content.recommendationsHostGroups'),
              recommendationsConfigs: null
            });
            router.transitionTo('step7');
            console.timeEnd('step6 next');
          }
        });
      }
    }
  }),

  step7: Em.Route.extend({
    route: '/step7',
    enter: function (router) {
      console.time('step7 enter');
      console.log('in /wizardStep7Controller:enter');
      var controller = router.get('installerController');
      controller.setCurrentStep('7');
      console.timeEnd('step7 enter');
    },
    connectOutlets: function (router, context) {
      console.time('step7 connectOutlets');
      var controller = router.get('installerController');
      router.get('preInstallChecksController').loadStep();
      var wizardStep7Controller = router.get('wizardStep7Controller');
      controller.loadAllPriorSteps().done(function () {
        wizardStep7Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep7', controller.get('content'));
        console.timeEnd('step7 connectOutlets');
      });
    },
    back: function (router) {
      console.time('step7 back');
      var step = router.get('installerController.content.skipSlavesStep') ? 'step5' : 'step6';
      var wizardStep7Controller = router.get('wizardStep7Controller');

      var goToNextStep = function() {
        router.transitionTo(step);
      };

      if (wizardStep7Controller.hasChanges()) {
        wizardStep7Controller.showChangesWarningPopup(goToNextStep);
      } else {
        goToNextStep();
      }
      console.timeEnd('step7 back');
    },
    next: function (router) {
      console.time('step7 next');
      if(!router.transitionInProgress) {
        router.set('transitionInProgress', true);
        var controller = router.get('installerController');
        var wizardStep7Controller = router.get('wizardStep7Controller');
        controller.saveServiceConfigProperties(wizardStep7Controller);
        controller.saveServiceConfigGroups(wizardStep7Controller);
        controller.setDBProperty('recommendationsConfigs', wizardStep7Controller.get('recommendationsConfigs'));
        App.clusterStatus.setClusterStatus({
          localdb: App.db.data
        }, {
          alwaysCallback: function() {
            router.transitionTo('step8');
            console.timeEnd('step7 next');
          }
        });
      }
    }
  }),

  step8: Em.Route.extend({
    route: '/step8',
    connectOutlets: function (router, context) {
      console.time('step8 connectOutlets');
      console.log('in installer.step8:connectOutlets');
      var controller = router.get('installerController');
      controller.setCurrentStep('8');
      controller.loadAllPriorSteps().done(function () {
        var wizardStep8Controller = router.get('wizardStep8Controller');
        wizardStep8Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep8', controller.get('content'));
        console.timeEnd('step8 connectOutlets');
      });
    },
    back: Em.Router.transitionTo('step7'),
    next: function (router) {
      console.time('step8 next');
      if (!router.transitionInProgress) {
        router.set('transitionInProgress', true);
        var installerController = router.get('installerController');
        var wizardStep8Controller = router.get('wizardStep8Controller');
        // invoke API call to install selected services
        installerController.installServices(false, function () {
          installerController.setInfoForStep9();
          // We need to do recovery based on whether we are in Add Host or Installer wizard
          installerController.saveClusterState('CLUSTER_INSTALLING_3');
          wizardStep8Controller.set('servicesInstalled', true);
          router.transitionTo('step9');
          console.timeEnd('step8 next');
        });
      }
    }
  }),

  step9: Em.Route.extend({
    route: '/step9',
    connectOutlets: function (router, context) {
      console.time('step9 connectOutlets');
      console.log('in installer.step9:connectOutlets');
      var controller = router.get('installerController'),
          wizardStep9Controller = router.get('wizardStep9Controller');
      controller.loadAllPriorSteps().done(function () {
        wizardStep9Controller.loadDoServiceChecksFlag().done(function () {
          controller.setCurrentStep('9');
          if (!App.get('testMode')) {
            controller.setLowerStepsDisable(9);
          }
          wizardStep9Controller.set('wizardController', controller);
          controller.connectOutlet('wizardStep9', controller.get('content'));
          console.timeEnd('step9 connectOutlets');
        });
      });
    },
    back: Em.Router.transitionTo('step8'),
    retry: function (router) {
      console.time('step9 retry');
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
        console.timeEnd('step9 retry');
      }
    },
    unroutePath: function (router, context) {
      // exclusion for transition to Admin view or Views view
      if (context === '/adminView' ||
          context === '/main/views.index') {
        this._super(router, context);
      } else {
        return false;
      }
    },
    next: function (router) {
      console.time('step9 next');
      if(!router.transitionInProgress) {
        router.set('transitionInProgress', true);
        var installerController = router.get('installerController');
        var wizardStep9Controller = router.get('wizardStep9Controller');
        installerController.saveInstalledHosts(wizardStep9Controller);

        installerController.saveClusterState('CLUSTER_INSTALLED_4');
        router.transitionTo('step10');
        console.timeEnd('step9 next');
      }
    }
  }),

  step10: Em.Route.extend({
    route: '/step10',
    connectOutlets: function (router, context) {
      console.log('in installer.step10:connectOutlets');
      var controller = router.get('installerController');
      controller.loadAllPriorSteps().done(function () {
        if (!App.get('testMode')) {
          controller.setCurrentStep('10');
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
