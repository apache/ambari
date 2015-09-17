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
  route: '/addSecurity',
  enter: function (router) {
    console.log('in /security/add:enter');
    var controller = router.get('addSecurityController');
    controller.dataLoading().done(Ember.run.next(function () {
      //after refresh check if the wizard is open then restore it
      if (router.get('mainAdminSecurityController').getAddSecurityWizardStatus() === 'RUNNING') {
        var mainAdminSecurityController = router.get('mainAdminSecurityController');
        var addSecurityController = router.get('addSecurityController');
        var currentStep = router.get('addSecurityController').get('currentStep');
        App.router.get('updateController').set('isWorking', false);
        App.ModalPopup.show({
            classNames: ['full-width-modal'],
            header: Em.I18n.t('admin.addSecurity.header'),
            bodyClass: App.MainAdminSecurityAddMenuView.extend({
              controllerBinding: 'App.router.addSecurityController'
            }),
            primary: Em.I18n.t('form.cancel'),
            secondary: null,
            showFooter: false,

            onClose: function () {
              var self = this;
              if (router.get('addSecurityController.currentStep') == 4) {
                var controller = router.get('mainAdminSecurityAddStep4Controller');
                if (!controller.get('isSubmitDisabled')) {
                  router.get('mainAdminSecurityAddStep4Controller').clearStep();
                  self.proceedOnClose();
                  return;
                }
                var applyingConfigCommand = router.get('mainAdminSecurityAddStep4Controller.commands').findProperty('name', 'APPLY_CONFIGURATIONS');
                if (applyingConfigCommand) {
                  if (!applyingConfigCommand.get('isCompleted')) {
                    if (applyingConfigCommand.get('isStarted')) {
                      App.showAlertPopup(Em.I18n.t('admin.security.applying.config.header'), Em.I18n.t('admin.security.applying.config.body'));
                    } else {
                      App.showConfirmationPopup(function () {
                        self.proceedOnClose();
                      }, Em.I18n.t('admin.addSecurity.enable.onClose'));
                    }
                  } else {
                    App.showConfirmationPopup(function () {
                        self.proceedOnClose();
                      },
                      Em.I18n.t('admin.addSecurity.enable.after.stage2.onClose')
                    );
                  }
                  return;
                }
              }
              router.get('mainAdminSecurityAddStep4Controller').clearStep();
              App.db.setSecurityDeployCommands(undefined);
              self.proceedOnClose();
            },
            proceedOnClose: function () {
              var self = this;
              router.get('mainAdminSecurityAddStep4Controller').clearStep();
              router.get('addSecurityController.content.services').clear();
              router.set('addSecurityController.content.serviceConfigProperties', null);
              App.router.get('updateController').set('isWorking', true);
              mainAdminSecurityController.setAddSecurityWizardStatus(null);
              App.db.setSecurityDeployCommands(undefined);
              addSecurityController.finish();
              App.clusterStatus.setClusterStatus({
                clusterName: router.get('content.cluster.name'),
                clusterState: 'DEFAULT',
                localdb: App.db.data
              }, {alwaysCallback: function() {
                self.hide();
                router.transitionTo('adminSecurity.index');
                Em.run.next(function() {
                  location.reload();   // this is needed because the ATS Component may be deleted in older HDP stacks.
                });
              }});
            },
            didInsertElement: function () {
              this.fitHeight();
            }
          }
        );

        App.router.transitionTo('step' + currentStep);
      } else {
        router.transitionTo('adminSecurity.index');
      }
    }));
  },

  step1: Em.Route.extend({
    route: '/start',
    enter: function (router) {
      router.get('addSecurityController').setCurrentStep('1');
      if(!App.get('testMode')){
        App.clusterStatus.setClusterStatus({
          clusterName: this.get('clusterName'),
          clusterState: 'ADD_SECURITY_STEP_1',
          wizardControllerName: router.get('addSecurityController.name'),
          localdb: App.db.data
        });
      }
    },

    connectOutlets: function (router) {
      console.log('in addSecurity.step1:connectOutlets');
      var controller = router.get('addSecurityController');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('mainAdminSecurityAddStep1', controller.get('content'));
      })
    },

    unroutePath: function () {
      return false;
    },

    next: function (router) {
      var addSecurityController = router.get('addSecurityController');
      addSecurityController.get('content').set('serviceConfigProperties', null);
      App.db.setSecureConfigProperties(null);
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/configure',

    enter: function (router) {
      router.get('addSecurityController').setCurrentStep('2');
      if(!App.get('testMode')){
        App.clusterStatus.setClusterStatus({
          clusterName: this.get('clusterName'),
          clusterState: 'ADD_SECURITY_STEP_2',
          wizardControllerName: router.get('addSecurityController.name'),
          localdb:  App.db.data
        });
      }
    },
    connectOutlets: function (router) {
      console.log('in addSecurity.step2:connectOutlets');
      var controller = router.get('addSecurityController');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('mainAdminSecurityAddStep2', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    back: Em.Router.transitionTo('step1'),
    next: function (router) {
      var addSecurityController = router.get('addSecurityController');
      var addSecurityStep2Controller = router.get('mainAdminSecurityAddStep2Controller');
      addSecurityController.saveServiceConfigProperties(addSecurityStep2Controller);
      router.transitionTo('step3');
    }
  }),

  step3: Em.Route.extend({
    route: '/principal_keytab',

    enter: function (router) {
      router.get('addSecurityController').setCurrentStep('3');
      if(!App.get('testMode')){
        App.clusterStatus.setClusterStatus({
          clusterName: this.get('clusterName'),
          clusterState: 'ADD_SECURITY_STEP_3',
          wizardControllerName: router.get('addSecurityController.name'),
          localdb:  App.db.data
        });
      }
    },
    connectOutlets: function (router) {
      console.log('in addSecurity.step3:connectOutlets');
      var controller = router.get('addSecurityController');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('mainAdminSecurityAddStep3', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    back: Em.Router.transitionTo('step2'),
    next: function (router) {
      App.db.setSecurityDeployCommands(undefined);
      router.transitionTo('step4');
    }
  }),

  step4: Em.Route.extend({
    route: '/apply',

    enter: function (router) {
      router.get('addSecurityController').setCurrentStep('4');
    },

    connectOutlets: function (router) {
      console.log('in addSecurity.step4:connectOutlets');
      var controller = router.get('addSecurityController');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.setLowerStepsDisable(4);
        controller.connectOutlet('mainAdminSecurityAddStep4', controller.get('content'));
      })
    },
    unroutePath: function () {
      return false;
    },
    back: function (router, context) {
      var controller = router.get('mainAdminSecurityAddStep4Controller');
      if (!controller.get('isBackBtnDisabled')) {
        router.transitionTo('step3');
      }
    },
    done: function (router, context) {
      var controller = router.get('mainAdminSecurityAddStep4Controller');
      if (!controller.get('isSubmitDisabled')) {
        $(context.currentTarget).parents("#modal").find(".close").trigger('click');
      }
    }
  })

});

