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
  route: '/service/add',
  App: require('app'),

  enter: function (router) {
    if (App.isAuthorized('SERVICE.ADD_DELETE_SERVICES') && App.supports.enableAddDeleteServices) {
      // `getSecurityStatus` call is required to retrieve information related to kerberos type: Manual or automated kerberos
      router.get('mainController').isLoading.call(router.get('clusterController'),'isClusterNameLoaded').done(function () {
        App.router.get('mainAdminKerberosController').getSecurityStatus().always(function () {
          Em.run.next(function () {
            var addServiceController = router.get('addServiceController');
            App.router.get('updateController').set('isWorking', false);
            var popup = App.ModalPopup.show({
              classNames: ['wizard-modal-wrapper', 'add-service-wizard-modal'],
              header: Em.I18n.t('services.add.header'),
              modalDialogClasses: ['modal-xlg'],
              bodyClass: App.AddServiceView.extend({
                controllerBinding: 'App.router.addServiceController'
              }),
              primary: Em.I18n.t('form.cancel'),
              showFooter: false,
              secondary: null,

              onPrimary: function () {
                this.hide();
                App.router.transitionTo('main.services.index');
              },
              onClose: function () {
                this.showWarningPopup();
              },
              afterWarning: function () {
                this.set('showCloseButton', false); // prevent user to click "Close" many times
                App.router.get('updateController').set('isWorking', true);
                App.router.get('updateController').updateServices(function () {
                  App.router.get('updateController').updateServiceMetric();
                });
                var exitPath = addServiceController.getDBProperty('onClosePath') || 'main.services.index';
                addServiceController.resetOnClose(addServiceController, exitPath);
              },
              showWarningPopup: function() {
                var mainPopupContext = this;
                var currentStep = addServiceController.get('currentStepName');
                const lastStep = addServiceController.get('lastStepName');
                const DEPLOY_STEP = 'step6';
                if (currentStep === lastStep) {
                  mainPopupContext.afterWarning();
                } else {
                  App.ModalPopup.show({
                    encodeBody: false,
                    header: currentStep === DEPLOY_STEP ? Em.I18n.t('common.warning') : Em.I18n.t('popup.confirmation.commonHeader'),
                    primaryClass: currentStep === DEPLOY_STEP ? 'btn-warning' : 'btn-success',
                    secondary: Em.I18n.t('form.cancel'),
                    body: currentStep === DEPLOY_STEP ? Em.I18n.t('services.add.warningStep6') : Em.I18n.t('services.add.warning'),
                    onPrimary: function () {
                      this.hide();
                      mainPopupContext.afterWarning();
                    }
                  });
                }
              },
              didInsertElement: function () {
                this._super();
                this.fitHeight();
              }
            });
            addServiceController.set('popup', popup);
            App.router.get('wizardWatcherController').setUser(addServiceController.get('name'));
            router.transitionTo(addServiceController.get('currentStepName'));
          });
        });
      });
    } else {
      Em.run.next(function () {
        App.router.transitionTo('main.services');
      });
    }

  },

  configureDownload: Em.Route.extend({
    route: '/configureDownload',
    breadcrumbs: { label: Em.I18n.translations['installer.configureDownload.header'] },
    connectOutlets: function (router) {
      console.time('configureDownload connectOutlets');
      var self = this;
      var controller = router.get('addServiceController');
      var configureDownloadController = router.get('wizardConfigureDownloadController');
      configureDownloadController.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('configureDownload');
      router.setNavigationFlow(newStepIndex);
      controller.setCurrentStep('configureDownload');
      controller.set('hideBackButton', true);
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardConfigureDownload', controller.get('content'));
        self.scrollTop();
        console.timeEnd('configureDownload connectOutlets');
      });
    },
    
    next: function (router) {
      console.time('configureDownload next');
      if(router.get('btnClickInProgress')) {
        return;
      }
      App.set('router.nextBtnClickInProgress', true);
      var controller = router.get('addServiceController');
      controller.save('downloadConfig');
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
      var controller = router.get('addServiceController');
      controller.setCurrentStep('selectMpacks');
      controller.set('hideBackButton', false);
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
      var controller = router.get('addServiceController');
      controller.clearErrors();
      router.transitionTo('configureDownload');
    },

    next: function (router, context) {
      console.time('selectMpacks next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        var controller = router.get('addServiceController');
        controller.save('selectedServiceNames');
        controller.save('selectedServices');
        controller.save('selectedMpacks');
        controller.save('mpacksToRegister');
        controller.save('serviceGroups');
        controller.save('addedServiceGroups');
        controller.save('serviceInstances');
        controller.save('addedServiceInstances');
        controller.save('advancedMode');
        var wizardSelectMpacksController = router.get('wizardSelectMpacksController');
        // Clear subsequent settings if user changed service selections
        if (!wizardSelectMpacksController.get('isSaved')) {
          router.get('wizardStep5Controller').clearRecommendations();
          controller.setDBProperty('recommendations', undefined);
          controller.set('content.masterComponentHosts', undefined);
          controller.setDBProperty('masterComponentHosts', undefined);
          controller.clearEnhancedConfigs();
          controller.setDBProperty('slaveComponentHosts', undefined);
          router.get('wizardStep6Controller').set('isClientsSet', false);
        }
        controller.setStepSaved('selectMpacks');
        if (controller.get('content.mpacksToRegister').length > 0) {
          const downloadConfig = controller.get('content.downloadConfig');
          if (downloadConfig && downloadConfig.useCustomRepo) {
            router.transitionTo('customMpackRepos');
          } else {
            router.transitionTo('downloadMpacks');
          }
        } else {
          router.transitionTo('step5');
        }
        console.timeEnd('selectMpacks next');
      }
    },
  }),

  customMpackRepos: App.StepRoute.extend({
    route: '/customMpackRepos',
    breadcrumbs: { label: Em.I18n.translations['installer.customMpackRepos.header'] },
    connectOutlets: function (router) {
      console.time('customMpackRepos connectOutlets');
      var self = this;
      var controller = router.get('addServiceController');
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
      var controller = router.get('addServiceController');
      controller.clearErrors();
      router.transitionTo('selectMpacks');
    },

    next: function (router) {
      console.time('customMpackRepos next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        const controller = router.get('addServiceController');
        controller.save('mpacksToRegister');
        controller.setStepSaved('customMpackRepos');
        router.transitionTo('downloadMpacks');
        console.timeEnd('customMpackRepos next');
      }  
    }
  }),
  
  downloadMpacks: App.StepRoute.extend({
    route: '/downloadMpacks',
    breadcrumbs: { label: Em.I18n.translations['installer.downloadMpacks.header'] },
    connectOutlets: function (router) {
      console.time('downloadMpacks connectOutlets');
      var self = this;
      var controller = router.get('addServiceController');
      var downloadMpacksController = router.get('wizardDownloadMpacksController');
      downloadMpacksController.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('downloadMpacks');
      router.setNavigationFlow(newStepIndex);
      controller.setCurrentStep('downloadMpacks');
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardDownloadMpacks', controller.get('content'));
        self.scrollTop();
        console.timeEnd('downloadMpacks connectOutlets');
      });
    },

    backTransition: function (router) {
      const controller = router.get('addServiceController');
      controller.clearErrors();
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
      const controller = router.get('addServiceController');
      controller.save('registeredMpacks');
      controller.save('selectedStack');
      const downloadConfig = controller.get('content.downloadConfig');
      if (downloadConfig && downloadConfig.useCustomRepo) {
        router.transitionTo('customProductRepos');
      } else {
        router.transitionTo('step5');
      }  
      console.timeEnd('downloadMpacks next');
    }
  }),

  customProductRepos: App.StepRoute.extend({
    route: '/customProductRepos',
    breadcrumbs: { label: Em.I18n.translations['installer.customProductRepos.header'] },
    connectOutlets: function (router) {
      console.time('customProductRepos connectOutlets');
      var self = this;
      var controller = router.get('addServiceController');
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
      var controller = router.get('addServiceController');
      controller.clearErrors();
      router.transitionTo('downloadMpacks');
    },

    next: function (router) {
      console.time('customProductRepos next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        const controller = router.get('addServiceController');
        controller.clearErrors();
        controller.save('selectedMpacks');
        controller.save('registeredMpacks');
        controller.setStepSaved('customProductRepos');
        router.transitionTo('verifyProducts');
        console.timeEnd('customProductRepos next');
      }
    }
  }),

  verifyProducts: App.StepRoute.extend({
    route: '/verifyProducts',
    breadcrumbs: { label: Em.I18n.translations['installer.verifyProducts.header'] },
    connectOutlets: function (router) {
      console.time('verifyProducts connectOutlets');
      var self = this;
      var controller = router.get('addServiceController');
      var verifyProductsController = router.get('wizardVerifyProductsController');
      verifyProductsController.set('wizardController', controller);
      var newStepIndex = controller.getStepIndex('verifyProducts');
      router.setNavigationFlow(newStepIndex);
      controller.setCurrentStep('verifyProducts');
      controller.loadAllPriorSteps().done(function () {
        controller.setStepsEnable();
        controller.connectOutlet('wizardVerifyProducts', controller.get('content'));
        self.scrollTop();
        console.timeEnd('verifyProducts connectOutlets');
      });
    },

    backTransition: function (router) {
      const controller = router.get('addServiceController');
      controller.clearErrors();
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
      const controller = router.get('addServiceController');
      router.transitionTo(controller.getNextStepName());
      console.timeEnd('verifyProducts next');
    }
  }),

  step5: App.StepRoute.extend({
    route: '/step5',
    breadcrumbs: { label: Em.I18n.translations['installer.step5.header'] },
    connectOutlets: function (router, context) {
      console.time('step5 connectOutlets');
      var self = this;
      var controller = router.get('addServiceController');
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
    
    backTransition: function (router) {
      var controller = router.get('addServiceController');
      controller.clearErrors();
      const downloadConfig = controller.get('content.downloadConfig');
      if (controller.get('content.mpacksToRegister').length > 0) {
        if (downloadConfig && downloadConfig.useCustomRepo) {
          router.transitionTo('verifyProducts');
        } else {
          router.transitionTo('downloadMpacks');
        }
      } else {
        router.transitionTo('selectMpacks');
      }
    },
    
    next: function (router) {
      console.time('step5 next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        var controller = router.get('addServiceController');
        var wizardStep5Controller = router.get('wizardStep5Controller');
        controller.saveMasterComponentHosts(wizardStep5Controller);
        controller.setDBProperty('recommendations', wizardStep5Controller.get('content.recommendations') || wizardStep5Controller.get('recommendations'));
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
    breadcrumbs: { label: Em.I18n.translations['installer.step6.header'] },
    connectOutlets: function (router, context) {
      console.time('step6 connectOutlets');
      var self = this;
      var controller = router.get('addServiceController');
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
      var controller = router.get('addServiceController');
      controller.clearErrors();
      router.transitionTo('step5');
    },

    next: function (router) {
      console.time('step6 next');
      var controller = router.get('addServiceController');
      var wizardStep6Controller = router.get('wizardStep6Controller');
      if (!wizardStep6Controller.get('submitDisabled')) {
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
            if (App.get('isKerberosEnabled')) {
              controller.setDBProperty('kerberosDescriptorConfigs', null);
            }      
          }
          controller.setStepSaved('step6');
          router.transitionTo('step7');
          console.timeEnd('step6 next');
        }
      }
    }
  }),

  step7: App.StepRoute.extend({
    route: '/step7',
    breadcrumbs: { label: Em.I18n.translations['installer.step7.header'] },
    connectOutlets: function (router, context) {
      console.time('step7 connectOutlets');
      var self = this;
      var controller = router.get('addServiceController');
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
      var controller = router.get('addServiceController');
      controller.clearErrors();

      var step = router.get('addServiceController.content.skipSlavesStep') ? 'step5' : 'step6';
      var wizardStep7Controller = router.get('wizardStep7Controller');

      var goToPreviousStep = function() {
        router.transitionTo(step);
      };

      if (wizardStep7Controller.hasChanges()) {
        wizardStep7Controller.showChangesWarningPopup(goToPreviousStep);
      } else {
        goToPreviousStep();
      }
      console.timeEnd('step7 back');
    },

    next: function (router) {
      console.time('step7 next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        var controller = router.get('addServiceController');
        var wizardStep7Controller = router.get('wizardStep7Controller');
        var kerberosDescriptor = controller.get('kerberosDescriptor');
        wizardStep7Controller.checkDescriptor().always(function (data, status) {
          wizardStep7Controller.storeClusterDescriptorStatus(status === 'success');
          if (App.get('isKerberosEnabled')) {
            wizardStep7Controller.updateKerberosDescriptor(kerberosDescriptor, wizardStep7Controller.getDescriptorConfigs());
            addServiceController.saveKerberosDescriptorConfigs(kerberosDescriptor);
            if (router.get('mainAdminKerberosController.isManualKerberos')) {
              router.get('wizardStep8Controller').set('wizardController', router.get('addServiceController'));
              router.get('wizardStep8Controller').updateKerberosDescriptor(true);
            }
          }
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
        });
        console.timeEnd('step7 next');
      }
    }
  }),

  step8: App.StepRoute.extend({
    route: '/step8',
    breadcrumbs: { label: Em.I18n.translations['installer.step8.header'] },
    connectOutlets: function (router, context) {
      console.time('step8 connectOutlets');
      var self = this;
      var controller = router.get('addServiceController');
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
      if(!!App.get('router.mainAdminKerberosController.kdc_type')){
        router.get('kerberosWizardStep5Controller').getCSVData(true);
      }
    },
    
    backTransition: function (router) {
      if (router.get('wizardStep8Controller.isBackBtnDisabled') == false) {
        var controller = router.get('addServiceController');
        controller.clearErrors();

        router.transitionTo('step7');
      }
    },
    
    next: function (router) {
      console.time('step8 next');
      if (!router.get('btnClickInProgress')) {
        App.set('router.nextBtnClickInProgress', true);
        var controller = router.get('addServiceController');
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
    breadcrumbs: { label: Em.I18n.translations['installer.step9.header'] },
    connectOutlets: function (router, context) {
      console.time('step9 connectOutlets');
      var self = this;
      var controller = router.get('addServiceController');
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

    backTransition: function (router) {
      var controller = router.get('addServiceController');
      controller.clearErrors();
      router.transitionTo('step8');
    },

    retry: function (router) {
      console.time('step9 retry');
      var controller = router.get('addServiceController');
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
        var controller = router.get('addServiceController');
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
    breadcrumbs: { label: Em.I18n.translations['installer.step10.header'] },
    connectOutlets: function (router, context) {
      var self = this;
      var controller = router.get('addServiceController');
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
    
    backTransition: function (router) {
      var controller = router.get('addServiceController');
      controller.clearErrors();
      router.transitionTo('step9');
    },
    
    complete: function (router, context) {
      var controller = router.get('addServiceController');
      controller.get('popup').onClose();
      controller.finish();
    }
  }),

  gotoStep0: Em.Router.transitionTo('step0'),

  gotoStep2: Em.Router.transitionTo('step2'),

  gotoStep3: Em.Router.transitionTo('step3'),

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
