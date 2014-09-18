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
  route: '/service/reassign',

  leaveWizard: function (router, context) {
    var reassignMasterController = router.get('reassignMasterController');
    App.router.get('updateController').set('isWorking', true);
    reassignMasterController.finish();
    App.clusterStatus.setClusterStatus({
      clusterName: App.router.get('content.cluster.name'),
      clusterState: 'DEFAULT',
      localdb: App.db.data
    }, {alwaysCallback: function () {
      context.hide();
      router.transitionTo('main.index');
      location.reload();
    }});
  },

  enter: function (router) {
    console.log('in /service/reassign:enter');
    var context = this;
    var reassignMasterController = router.get('reassignMasterController');
    reassignMasterController.dataLoading().done(function () {
      if (App.router.get('mainHostController.hostsCountMap.TOTAL') > 1) {
        Em.run.next(function () {
          App.router.get('updateController').set('isWorking', false);
          var popup = App.ModalPopup.show({
            classNames: ['full-width-modal'],
            header: Em.I18n.t('services.reassign.header'),
            bodyClass: App.ReassignMasterView.extend({
              controller: reassignMasterController
            }),
            primary: Em.I18n.t('form.cancel'),
            showFooter: false,
            secondary: null,

            onPrimary: function () {
              this.hide();
              App.router.get('updateController').set('isWorking', true);
              App.router.transitionTo('main.services.index');
            },
            onClose: function () {
              var reassignMasterController = router.get('reassignMasterController');
              var currStep = reassignMasterController.get('currentStep');

              if (parseInt(currStep) > 3) {
                var self = this;
                App.showConfirmationPopup(function () {
                  router.get('reassignMasterWizardStep' + currStep + 'Controller').removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
                  context.leaveWizard(router, self);
                }, Em.I18n.t('services.reassign.closePopup').format(reassignMasterController.get('content.reassign.display_name')));
              } else {
                context.leaveWizard(router, this);
              }
            },
            didInsertElement: function () {
              this.fitHeight();
            }
          });
          reassignMasterController.set('popup', popup);
          reassignMasterController.loadSecurityEnabled();
          reassignMasterController.loadComponentToReassign();
          var currentClusterStatus = App.clusterStatus.get('value');
          if (currentClusterStatus) {
            switch (currentClusterStatus.clusterState) {
              case 'REASSIGN_MASTER_INSTALLING' :
                reassignMasterController.setCurrentStep(currentClusterStatus.localdb.ReassignMaster.currentStep);
                break;
            }
          }
          router.transitionTo('step' + reassignMasterController.get('currentStep'));
        });
      } else {
        App.showAlertPopup(Em.I18n.t('common.error'), Em.I18n.t('services.reassign.error.fewHosts'), function () {
          router.transitionTo('main.services.index');
        })
      }
    });
  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      console.log('in reassignMaster.step1:connectOutlets');
      var controller = router.get('reassignMasterController');
      controller.setCurrentStep('1');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('reassignMasterWizardStep1', controller.get('content'));
      })
    },
    next: function (router) {
      App.db.setMasterComponentHosts(undefined);
      router.transitionTo('step2');
    },

    unroutePath: function () {
      return false;
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      console.log('in reassignMaster.step2:connectOutlets');
      var controller = router.get('reassignMasterController');
      controller.setCurrentStep('2');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('reassignMasterWizardStep2', controller.get('content'));
      })

    },
    back: Em.Router.transitionTo('step1'),
    next: function (router) {
      var controller = router.get('reassignMasterController');
      var reassignMasterWizardStep2 = router.get('reassignMasterWizardStep2Controller');
      controller.saveMasterComponentHosts(reassignMasterWizardStep2);
      var reassignHosts = {};
      var componentName = reassignMasterWizardStep2.get('content.reassign.component_name');
      var masterAssignmentsHosts = reassignMasterWizardStep2.get('selectedServicesMasters').filterProperty('component_name', componentName).mapProperty('selectedHost');
      var currentMasterHosts = App.HostComponent.find().filterProperty('componentName', componentName).mapProperty('hostName');
      masterAssignmentsHosts.forEach(function (host) {
        if (!currentMasterHosts.contains(host)) {
          reassignHosts.target = host;
        }
      }, this);
      currentMasterHosts.forEach(function (host) {
        if (!masterAssignmentsHosts.contains(host)) {
          reassignHosts.source = host;
        }
      }, this);
      controller.saveReassignHosts(reassignHosts);
      router.transitionTo('step3');
    },

    unroutePath: function () {
      return false;
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router) {
      console.log('in reassignMaster.step3:connectOutlets');
      var controller = router.get('reassignMasterController');
      controller.setCurrentStep('3');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('reassignMasterWizardStep3', controller.get('content'));
      })
    },
    back: Em.Router.transitionTo('step2'),
    next: function (router) {
      App.db.setReassignTasksStatuses(undefined);
      App.db.setReassignTasksRequestIds(undefined);
      App.clusterStatus.setClusterStatus({
        clusterName: router.get('reassignMasterController.content.cluster.name'),
        clusterState: 'REASSIGN_MASTER_INSTALLING',
        wizardControllerName: 'reassignMasterController',
        localdb: App.db.data
      });
      router.transitionTo('step4');
    },

    unroutePath: function () {
      return false;
    }
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router) {
      console.log('in reassignMaster.step4:connectOutlets');
      var controller = router.get('reassignMasterController');
      controller.setCurrentStep('4');
      controller.setLowerStepsDisable(4);
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('reassignMasterWizardStep4', controller.get('content'));
      })
    },
    next: function (router) {
      router.get('reassignMasterController').setCurrentStep('5');
      App.clusterStatus.setClusterStatus({
        clusterName: router.get('reassignMasterController.content.cluster.name'),
        clusterState: 'REASSIGN_MASTER_INSTALLING',
        wizardControllerName: 'reassignMasterController',
        localdb: App.db.data
      });
      router.transitionTo('step5');
    },

    complete: function (router) {
      var controller = router.get('reassignMasterController');
      var reassignMasterWizardStep4 = router.get('reassignMasterWizardStep4Controller');
      if (!reassignMasterWizardStep4.get('isSubmitDisabled')) {
        controller.finish();
        controller.get('popup').hide();
        App.clusterStatus.setClusterStatus({
          clusterName: router.get('reassignMasterController.content.cluster.name'),
          clusterState: 'DEFAULT',
          localdb: App.db.data
        }, {alwaysCallback: function () {
          controller.get('popup').hide();
          router.transitionTo('main.index');
          location.reload();
        }});
      }
    },

    unroutePath: function () {
      return false;
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',
    connectOutlets: function (router) {
      console.log('in reassignMaster.step5:connectOutlets');
      var controller = router.get('reassignMasterController');
      controller.setCurrentStep('5');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.setLowerStepsDisable(5);
        if ((controller.get('content.reassign.component_name') === 'NAMENODE') || controller.get('content.reassign.component_name') === 'SECONDARY_NAMENODE') {
          controller.usersLoading().done(function () {
            controller.connectOutlet('reassignMasterWizardStep5', controller.get('content'));
          })
        } else {
          controller.connectOutlet('reassignMasterWizardStep5', controller.get('content'));
        }
      })
    },
    next: function (router) {
      App.showConfirmationPopup(function () {
        router.transitionTo('step6');
      }, Em.I18n.t('services.reassign.step5.confirmPopup.body'));
    },

    unroutePath: function () {
      return false;
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',
    connectOutlets: function (router) {
      console.log('in reassignMaster.step6:connectOutlets');
      var controller = router.get('reassignMasterController');
      controller.setCurrentStep('6');
      controller.setLowerStepsDisable(6);
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('reassignMasterWizardStep6', controller.get('content'));
      })
    },

    next: function (router) {
      var controller = router.get('reassignMasterController');
      var reassignMasterWizardStep6 = router.get('reassignMasterWizardStep6Controller');
      if (!reassignMasterWizardStep6.get('isSubmitDisabled')) {
        controller.finish();
        controller.get('popup').hide();
        App.clusterStatus.setClusterStatus({
          clusterName: router.get('reassignMasterController.content.cluster.name'),
          clusterState: 'DEFAULT',
          localdb: App.db.data
        }, {alwaysCallback: function () {
          controller.get('popup').hide();
          router.transitionTo('main.index');
          location.reload();
        }});
      }
    },

    unroutePath: function () {
      return false;
    }
  }),


  gotoStep1: Em.Router.transitionTo('step1'),

  gotoStep2: Em.Router.transitionTo('step2'),

  gotoStep3: Em.Router.transitionTo('step3'),

  gotoStep4: Em.Router.transitionTo('step4'),

  gotoStep5: Em.Router.transitionTo('step5'),

  gotoStep6: Em.Router.transitionTo('step6'),

  backToServices: function (router) {
    App.router.get('updateController').set('isWorking', true);
    router.transitionTo('services');
  }

});
