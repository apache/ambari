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
    var self = this;

    App.clusterStatus.set('wizardControllerName', App.router.get('installerController.name'));

    router.getAuthenticated().done(function (loggedIn) {
      if (loggedIn) {
        var applicationController = router.get('applicationController');
        App.router.get('experimentalController').loadSupports().complete(function () {
          applicationController.startKeepAlivePoller();
          // check server/web client versions match
          App.router.get('installerController').checkServerClientVersion().done(function () {

            $('title').text(Em.I18n.t('app.name.subtitle').format(Em.I18n.t('app.name.subtitle.installer')));
            $('#main').addClass('install-wizard-content');

            App.router.get('mainViewsController').loadAmbariViews();
            if (App.isAuthorized('AMBARI.ADD_DELETE_CLUSTERS')) {
              router.get('mainController').stopPolling();
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
        Ember.run.next(function () {
          router.transitionTo('login');
        });
      }
    });
  },

  routePath: function (router, event) {
    const stepIndex = installerController.getStepIndex(event)
    router.setNavigationFlow(stepIndex);

    if (!router.isFwdNavigation) {
      this._super(router, event);
    } else {
      router.set('backBtnForHigherStep', true);

      var installerController = router.get('installerController');
      router.transitionTo(installerController.get('currentStepName'));
    }
  },

  connectOutlets: function (router, context) {
    router.get('applicationController').connectOutlet('installer');
  },

  step0: Em.Route.extend({
    route: '/step0',

    connectOutlets: function (router) {
      console.time('step0 connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      var wizardStep0Controller = router.get('wizardStep0Controller');
      wizardStep0Controller.set('wizardController', controller);
      controller.setCurrentStep('step0');
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardStep0', controller.get('content'));
        self.scrollTop();
        console.timeEnd('step0 connectOutlets');
      });
    },

    next: function (router) {
      console.time('step0 next');
      var controller = router.get('installerController');
      controller.save('cluster');
      App.db.setStacks(undefined);
      App.db.setRepos(undefined);
      App.db.setLocalRepoVDFData(undefined);
      App.Stack.find().clear();

      controller.set('content.stacks',undefined);
      router.transitionTo('step2');
      console.timeEnd('step0 next');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    
    connectOutlets: function (router, context) {
      console.time('step2 connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      var wizardStep2Controller = router.get('wizardStep2Controller');
      wizardStep2Controller.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('step2');
      router.setNavigationFlow(newStepIndex);
      //controller.clearInstallOptions();
      controller.setCurrentStep('step2');
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        self.scrollTop();
        controller.connectOutlet('wizardStep2', controller.get('content'));
        console.timeEnd('step2 connectOutlets');
      });
    },
    
    back: Em.Router.transitionTo('step0'),
    
    next: function (router) {
      console.time('step2 next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        var controller = router.get('installerController');
        controller.save('installOptions');
        //hosts was saved to content.hosts inside wizardStep2Controller
        controller.save('hosts');
        controller.setStepSaved('step2');
        router.transitionTo('step3');
      }
      console.timeEnd('step2 next');
    }
  }),

  step3: App.StepRoute.extend({
    route: '/step3',
    
    connectOutlets: function (router) {
      console.time('step3 connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      var wizardStep3Controller = router.get('wizardStep3Controller');
      wizardStep3Controller.set('wizardController', controller);
      controller.setCurrentStep('step3');
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardStep3', controller.get('content'));
        self.scrollTop();
        console.timeEnd('step3 connectOutlets');
      });
    },

    backTransition: function (router) {
      router.transitionTo('step2');
    },

    next: function (router, context) {
      console.time('step3 next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        var controller = router.get('installerController');
        var wizardStep3Controller = router.get('wizardStep3Controller');
        controller.saveConfirmedHosts(wizardStep3Controller);
        if (!wizardStep3Controller.get('isSaved')) {
          var wizardSelectMpacksController = App.router.get('wizardSelectMpacksController');
          wizardSelectMpacksController.set('wizardController', controller);
          wizardSelectMpacksController.clearSelection();
          controller.set('content.selectedServices', undefined);
          controller.set('content.selectedServiceNames', undefined);
          controller.set('content.selectedMpacks', undefined);
          controller.setDBProperties({
            bootStatus: true,
            selectedServices: undefined,
            selectedServiceNames: undefined,
            installedServiceNames: undefined,
            selectedMpack: undefined
          });
        }
        controller.setStepSaved('step3');
        router.transitionTo('configureDownload');
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
      var controller = router.get('installerController');
      controller.removeHosts(context);
    }
  }),

  configureDownload: Em.Route.extend({
    route: '/configureDownload',
    
    connectOutlets: function (router) {
      console.time('configureDownload connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      var configureDownloadController = router.get('wizardConfigureDownloadController');
      configureDownloadController.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('configureDownload');
      router.setNavigationFlow(newStepIndex);
      controller.setCurrentStep('configureDownload');
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardConfigureDownload', controller.get('content'));
        self.scrollTop();
        console.timeEnd('configureDownload connectOutlets');
      });
    },
    
    back: Em.Router.transitionTo('step3'),
    
    next: function (router) {
      console.time('configureDownload next');
      if(router.get('btnClickInProgress')) {
        return;
      }
      App.set('router.nextBtnClickInProgress', true);
      var controller = router.get('installerController');
      controller.save('downloadConfig');
      controller.setDBProperty('service', undefined);
      router.transitionTo('selectMpacks');
      console.timeEnd('configureDownload next');
    }
  }),

  selectMpacks: App.StepRoute.extend({
    route: '/selectMpacks',
    breadcrumbs: { label: Em.I18n.translations['installer.selectMpacks.header'] },
    connectOutlets: function (router) {
      console.time('selectMpacks connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      controller.setCurrentStep('selectMpacks');
      var wizardSelectMpacksController = router.get('wizardSelectMpacksController');
      wizardSelectMpacksController.set('wizardController', controller);
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardSelectMpacks', controller.get('content'));
        self.scrollTop();
        console.timeEnd('selectMpacks connectOutlets');
      });
    },

    backTransition: function (router) {
      router.transitionTo('configureDownload');
    },

    next: function (router, context) {
      console.time('selectMpacks next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        var controller = router.get('installerController');
        controller.save('selectedServiceNames');
        controller.save('selectedServices');
        controller.save('selectedMpacks');
        controller.save('advancedMode');
        var wizardStep6Controller = router.get('wizardStep6Controller');
        // Clear subsequent settings if user changed service selections
        if (!wizardStep6Controller.get('isSaved')) {
          router.get('wizardStep5Controller').clearRecommendations();
          controller.setDBProperty('recommendations', undefined);
          controller.set('content.masterComponentHosts', undefined);
          controller.setDBProperty('masterComponentHosts', undefined);
          controller.clearEnhancedConfigs();
          controller.setDBProperty('slaveComponentHosts', undefined);
          wizardStep6Controller.set('isClientsSet', false);
        }
        controller.setStepSaved('selectMpacks');
        const downloadConfig = controller.get('content.downloadConfig');
        if (downloadConfig && downloadConfig.useCustomRepo) {
          router.transitionTo('customMpackRepos');
        } else {
          router.transitionTo('downloadMpacks');
        }  
        console.timeEnd('selectMpacks next');
      }
    },
  }),

  customMpackRepos: App.StepRoute.extend({
    route: '/customMpackRepos',
    
    connectOutlets: function (router) {
      console.time('customMpackRepos connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      const downloadConfig = controller.get('content.downloadConfig');
      
      //do not allow navigation to this step unless we are using custom repos
      if (downloadConfig && !downloadConfig.useCustomRepo) {
        Em.run.next(function () {
          router.transitionTo('downloadMpacks');
        });
      }  

      var customMpackReposController = router.get('wizardCustomMpackReposController');
      customMpackReposController.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('customMpackRepos');
      router.setNavigationFlow(newStepIndex);
      controller.setCurrentStep('customMpackRepos');
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardCustomMpackRepos', controller.get('content'));
        self.scrollTop();
        console.timeEnd('customMpackRepos connectOutlets');
      });
    },

    backTransition: function (router) {
      router.transitionTo('selectMpacks');
    },

    next: function (router) {
      console.time('customMpackRepos next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        const controller = router.get('installerController');
        controller.save('selectedMpacks');
        controller.setStepSaved('customMpackRepos');
        router.transitionTo('downloadMpacks');
        console.timeEnd('customMpackRepos next');
      }  
    }
  }),
  
  downloadMpacks: App.StepRoute.extend({
    route: '/downloadMpacks',
    connectOutlets: function (router) {
      console.time('downloadMpacks connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      var downloadMpacksController = router.get('wizardDownloadMpacksController');
      downloadMpacksController.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('downloadMpacks');
      router.setNavigationFlow(newStepIndex);
      controller.setCurrentStep('downloadMpacks');
      
      //disable customMpackRepos step (even though it is an earlier step) if not using custom repos
      // Em.run.next(function () {
      //   controller.get('isStepDisabled')
      //     .findProperty('step', controller.getStepIndex('customMpackRepos'))
      //     .set('value', !controller.get('content.downloadConfig.useCustomRepo'));
      // });

      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardDownloadMpacks', controller.get('content'));
        self.scrollTop();
        console.timeEnd('downloadMpacks connectOutlets');
      });
    },

    backTransition: function (router) {
      const controller = router.get('installerController');
      const downloadConfig = controller.get('content.downloadConfig');
      if (downloadConfig && downloadConfig.useCustomRepo) {
        router.transitionTo('customMpackRepos');
      } else {
        router.transitionTo('selectMpacks');
      }
    },

    next: function (router) {
      console.time('downloadMpacks next');
      if(router.get('btnClickInProgress')) {
        return;
      }
      App.set('router.nextBtnClickInProgress', true);
      const controller = router.get('installerController');
      const downloadConfig = controller.get('content.downloadConfig');
      if (downloadConfig && downloadConfig.useCustomRepo) {
        router.transitionTo('customProductRepos');
      } else {
        router.transitionTo('verifyProducts');
      }  
      console.timeEnd('downloadMpacks next');
    }
  }),

  customProductRepos: App.StepRoute.extend({
    route: '/customProductRepos',

    connectOutlets: function (router) {
      console.time('customProductRepos connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      const downloadConfig = controller.get('content.downloadConfig');
      
      //disable navigation to this step unless we are using custom repos
      if (downloadConfig && !downloadConfig.useCustomRepo) {
        Em.run.next(function () {
          router.transitionTo('verifyProducts');
        });
      }  
      
      var customMpackReposController = router.get('wizardCustomMpackReposController');
      customMpackReposController.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('customProductRepos');
      router.setNavigationFlow(newStepIndex);
      controller.setCurrentStep('customProductRepos');
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardCustomProductRepos', controller.get('content'));
        self.scrollTop();
        console.timeEnd('customProductRepos connectOutlets');
      });
    },

    backTransition: function (router) {
      router.transitionTo('downloadMpacks');
    },

    next: function (router) {
      console.time('customProductRepos next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        const controller = router.get('installerController');
        controller.save('selectedMpacks');
        controller.setStepSaved('customProductRepos');
        router.transitionTo('verifyProducts');
        console.timeEnd('customProductRepos next');
      }
    }
  }),

  verifyProducts: App.StepRoute.extend({
    route: '/verifyProducts',
    connectOutlets: function (router) {
      console.time('verifyProducts connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      var verifyProductsController = router.get('wizardVerifyProductsController');
      verifyProductsController.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('verifyProducts');
      router.setNavigationFlow(newStepIndex);
      controller.setCurrentStep('verifyProducts');

      //disable customProductRepos step (even though it is an earlier step) if not using custom repos
      // Em.run.next(function () {
      //   controller.get('isStepDisabled')
      //     .findProperty('step', controller.getStepIndex('customProductRepos'))
      //     .set('value', !controller.get('content.downloadConfig.useCustomRepo'));
      // });

      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardVerifyProducts', controller.get('content'));
        self.scrollTop();
        console.timeEnd('verifyProducts connectOutlets');
      });
    },

    backTransition: function (router) {
      const controller = router.get('installerController');
      const downloadConfig = controller.get('content.downloadConfig');
      if (downloadConfig && downloadConfig.useCustomRepo) {
        router.transitionTo('customProductRepos');
      } else {
        router.transitionTo('downloadMpacks');
      }
    },

    next: function (router) {
      console.time('verifyProducts next');
      if (router.get('btnClickInProgress')) {
        return;
      }
      App.set('router.nextBtnClickInProgress', true);
      const controller = router.get('installerController');
      router.transitionTo(controller.getNextStepName());
      console.timeEnd('verifyProducts next');
    }
  }),

  step4: App.StepRoute.extend({
    route: '/step4',
    connectOutlets: function (router, context) {
      console.time('step4 connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      var wizardStep4Controller = router.get('wizardStep4Controller');
      wizardStep4Controller.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('step4');
      router.setNavigationFlow(newStepIndex);
      controller.setCurrentStep('step4');
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardStep4', App.StackService.find().filterProperty('isInstallable', true));
        self.scrollTop();
        console.timeEnd('step4 connectOutlets');
      });
    },

    backTransition: function(router) {
      router.transitionTo('step1');
    },

    next: function (router) {
      console.time('step4 next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        var controller = router.get('installerController');
        var wizardStep4Controller = router.get('wizardStep4Controller');
        controller.saveServices(wizardStep4Controller);
        controller.saveClients(wizardStep4Controller);
        router.get('wizardStep5Controller').clearRecommendations(); // Force reload recommendation between steps 4 and 5
        controller.setDBProperties({
          recommendations: undefined,
          masterComponentHosts: undefined
        });
        controller.clearEnhancedConfigs();
        router.transitionTo('step5');
      }
      console.timeEnd('step4 next');
    }
  }),

  step5: App.StepRoute.extend({
    route: '/step5',
    connectOutlets: function (router, context) {
      console.time('step5 connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      var wizardStep5Controller = router.get('wizardStep5Controller');
      wizardStep5Controller.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('step5');
      router.setNavigationFlow(newStepIndex);
      wizardStep5Controller.setProperties({
        servicesMasters: [],
        isInitialLayout: true
      });
      controller.setCurrentStep('step5');
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardStep5', controller.get('content'));
        self.scrollTop();
        console.timeEnd('step5 connectOutlets');
      });
    },
    backTransition: function(router) {
      router.transitionTo('verifyProducts');
    },
    next: function (router) {
      console.time('step5 next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        var controller = router.get('installerController');
        var wizardStep5Controller = router.get('wizardStep5Controller');
        controller.saveMasterComponentHosts(wizardStep5Controller);
        controller.setDBProperty('recommendations', wizardStep5Controller.get('content.recommendations'));
        // Clear subsequent steps if user made changes
        if (!wizardStep5Controller.get('isSaved')) {
          controller.setDBProperty('slaveComponentHosts', undefined);
          var wizardStep6Controller = router.get('wizardStep6Controller');
          wizardStep6Controller.set('isClientsSet', false);
        }
        controller.setStepSaved('step5');
        router.transitionTo('step6');
      }
      console.timeEnd('step5 next');
    }
  }),

  step6: App.StepRoute.extend({
    route: '/step6',
    connectOutlets: function (router, context) {
      console.time('step6 connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      var wizardStep6Controller = router.get('wizardStep6Controller');
      wizardStep6Controller.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('step6');
      router.setNavigationFlow(newStepIndex);
      controller.setCurrentStep('step6');
      wizardStep6Controller.set('hosts', []);
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardStep6', controller.get('content'));
        self.scrollTop();
        console.timeEnd('step6 connectOutlets');
      });
    },
    backTransition: function(router) {
      router.transitionTo('step5');
    },

    next: function (router) {
      console.time('step6 next');
      var controller = router.get('installerController');
      var wizardStep6Controller = router.get('wizardStep6Controller');
      if (!wizardStep6Controller.get('submitDisabled')) {
        wizardStep6Controller.showValidationIssuesAcceptBox(function () {
          if (!router.get('btnClickInProgress')) {
            App.set('router.nextBtnClickInProgress', true);
            controller.saveSlaveComponentHosts(wizardStep6Controller);
            controller.get('content').set('serviceConfigProperties', null);
            controller.get('content').set('componentsFromConfigs', []);
            // Clear subsequent steps if user made changes
            if (!wizardStep6Controller.get('isSaved')) {
              controller.setDBProperties({
                serviceConfigGroups: null,
                recommendationsHostGroups: wizardStep6Controller.get('content.recommendationsHostGroups'),
                recommendationsConfigs: null,
                componentsFromConfigs: []
              });
              controller.clearServiceConfigProperties();
            }
            controller.setStepSaved('step6');
            router.transitionTo('step7');
            console.timeEnd('step6 next');
          }
        });
      }
    }
  }),

  step7: App.StepRoute.extend({
    route: '/step7',

    connectOutlets: function (router, context) {
      console.time('step7 connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      var wizardStep7Controller = router.get('wizardStep7Controller');
      wizardStep7Controller.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('step7');
      router.setNavigationFlow(newStepIndex);
      controller.setCurrentStep('step7');     
      router.get('preInstallChecksController').loadStep();
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardStep7', controller.get('content'));
        self.scrollTop();
        console.timeEnd('step7 connectOutlets');
      });
    },

    backTransition: function (router) {
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
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        var controller = router.get('installerController');
        var wizardStep7Controller = router.get('wizardStep7Controller');
        controller.saveServiceConfigProperties(wizardStep7Controller);
        controller.saveServiceConfigGroups(wizardStep7Controller);
        controller.setDBProperty('recommendationsConfigs', wizardStep7Controller.get('recommendationsConfigs'));
        controller.saveComponentsFromConfigs(controller.get('content.componentsFromConfigs'));
        controller.setDBProperty('recommendationsHostGroup', wizardStep7Controller.get('content.recommendationsHostGroup'));
        controller.setDBProperty('masterComponentHosts', wizardStep7Controller.get('content.masterComponentHosts'));
        App.clusterStatus.setClusterStatus({
          localdb: App.db.data
        });
        router.transitionTo('step8');
        console.timeEnd('step7 next');
      }
    }
  }),

  step8: App.StepRoute.extend({
    route: '/step8',
    connectOutlets: function (router, context) {
      console.time('step8 connectOutlets');
      var self = this;
      var controller = router.get('installerController');
      var wizardStep8Controller = router.get('wizardStep8Controller');
      wizardStep8Controller.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('step8');
      router.setNavigationFlow(newStepIndex);
      controller.setCurrentStep('step8');
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardStep8', controller.get('content'));
        self.scrollTop();
        console.timeEnd('step8 connectOutlets');
      });
    },
    backTransition: function (router) {
      if(router.get('wizardStep8Controller.isBackBtnDisabled') == false) {
        router.transitionTo('step7');
      }
    },
    next: function (router) {
      console.time('step8 next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        var controller = router.get('installerController');
        var wizardStep8Controller = router.get('wizardStep8Controller');
        // invoke API call to install selected services
        controller.installServices(false, function () {
          controller.setInfoForStep9();
          // We need to do recovery based on whether we are in Add Host or Installer wizard
          controller.saveClusterState('CLUSTER_INSTALLING_3');
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
      var self = this;
      var controller = router.get('installerController');
      var wizardStep9Controller = router.get('wizardStep9Controller');
      wizardStep9Controller.set('wizardController', controller);
      controller.loadAllPriorSteps().done(function () {
        wizardStep9Controller.loadDoServiceChecksFlag().done(function () {
          var newStepIndex = controller.getStepIndex('step7');
          router.setNavigationFlow(newStepIndex);
          controller.setCurrentStep('step9');
          controller.setStepsEnable();
          if (!App.get('testMode')) {
            controller.setLowerStepsDisable(9);
          }
          controller.connectOutlet('wizardStep9', controller.get('content'));
          self.scrollTop();
          console.timeEnd('step9 connectOutlets');
        });
      });
    },

    back: Em.Router.transitionTo('step8'),

    retry: function (router) {
      console.time('step9 retry');
      var controller = router.get('installerController');
      var wizardStep9Controller = router.get('wizardStep9Controller');
      if (wizardStep9Controller.get('showRetry')) {
        if (wizardStep9Controller.get('content.cluster.status') === 'INSTALL FAILED') {
          var isRetry = true;
          controller.installServices(isRetry, function () {
            controller.setInfoForStep9();
            wizardStep9Controller.resetHostsForRetry();
            // We need to do recovery based on whether we are in Add Host or Installer wizard
            controller.saveClusterState('CLUSTER_INSTALLING_3');
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
          context === '/main/views.index' || context === '/main/view.index') {
        this._super(router, context);
      } else {
        return false;
      }
    },

    next: function (router) {
      console.time('step9 next');
      if(!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        var controller = router.get('installerController');
        var wizardStep9Controller = router.get('wizardStep9Controller');
        controller.saveInstalledHosts(wizardStep9Controller);
        controller.saveClusterState('CLUSTER_INSTALLED_4');
        router.transitionTo('step10');
        console.timeEnd('step9 next');
      }
    }
  }),

  step10: Em.Route.extend({
    route: '/step10',

    connectOutlets: function (router, context) {
      var self = this;
      var controller = router.get('installerController');
      var wizardStep10Controller = router.get('wizardStep10Controller');
      wizardStep10Controller.set('wizardController', controller);
      controller.loadAllPriorSteps().done(function () {
        if (!App.get('testMode')) {
          var newStepIndex = controller.getStepIndex('step10');
          router.setNavigationFlow(newStepIndex);
          controller.setCurrentStep('step10');
          controller.setStepsEnable();
          controller.setLowerStepsDisable(10);
        }
        controller.connectOutlet('wizardStep10', controller.get('content'));
        self.scrollTop();
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

  gotoStep10: Em.Router.transitionTo('step10'),

  gotoConfigureDownload: Em.Router.transitionTo('configureDownload'),

  gotoSelectMpacks: Em.Router.transitionTo('selectMpacks'),

  gotoCustomMpackRepos: Em.Router.transitionTo('customMpackRepos'),

  gotoDownloadMpacks: Em.Router.transitionTo('downloadMpacks'),

  gotoCustomProductRepos: Em.Router.transitionTo('customProductRepos'),

  gotoVerifyProducts: Em.Router.transitionTo('verifyProducts')

});
