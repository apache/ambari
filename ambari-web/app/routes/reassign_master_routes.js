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
  route: '/services/reassign',

  enter: function (router) {
    console.log('in /service/reassign:enter');
      Em.run.next(function () {
        var reassignMasterController = router.get('reassignMasterController');
        App.router.get('updateController').set('isWorking', false);
        App.ModalPopup.show({
          classNames: ['full-width-modal'],
          header:Em.I18n.t('services.reassign.header'),
          bodyClass:  App.ReassignMasterView.extend({
            controller: reassignMasterController
          }),
          primary:Em.I18n.t('form.cancel'),
          showFooter: false,
          secondary: null,

          onPrimary:function () {
            this.hide();
            App.router.get('updateController').set('isWorking', true);
            App.router.transitionTo('main.services.index');
          },
          onClose: function() {
            this.hide();
            App.router.get('updateController').set('isWorking', true);
            App.router.transitionTo('main.services.index')
          },
          didInsertElement: function(){
            this.fitHeight();
          }
        });

        App.clusterStatus.updateFromServer();
        var currentClusterStatus = App.clusterStatus.get('value');
        if (currentClusterStatus && currentClusterStatus.clusterState == 'REASSIGN_MASTER_INSTALLING') {
          reassignMasterController.setCurrentStep('5');
          App.db.data = currentClusterStatus.localdb;
        } else {
          reassignMasterController.setCurrentStep('1');
        }
        router.transitionTo('step' + reassignMasterController.get('currentStep'));
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
        controller.connectOutlet('wizardStep11');
      })
    },
    next: function (router) {
      App.db.setMasterComponentHosts(undefined);
      router.transitionTo('step2');
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
        controller.connectOutlet('wizardStep5', controller.get('content'));
      })

    },
    back: Em.Router.transitionTo('step1'),
    next: function (router) {
      var controller = router.get('reassignMasterController');
      var wizardStep5Controller = router.get('wizardStep5Controller');
      controller.saveMasterComponentHosts(wizardStep5Controller);
      router.transitionTo('step3');
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router) {
      console.log('in reassignMaster.step3:connectOutlets');
      var controller = router.get('reassignMasterController');
      if (controller.get('skipStep3')) {
        router.transitionTo('step4');
      } else {
        controller.setCurrentStep('3');
        controller.dataLoading().done(function () {
          controller.loadAllPriorSteps();
          controller.set('content.serviceName', controller.get('content.reassign.service_id'));
          controller.connectOutlet('wizardStep12', controller.get('content'));
        })
      }
    },
    back: Em.Router.transitionTo('step2'),
    next: function (router) {
      var controller = router.get('reassignMasterController');
      var wizardStep12Controller = router.get('wizardStep12Controller');
      controller.set('content.modifiedConfigs', wizardStep12Controller.get('modifiedConfigs'));
      controller.saveServiceConfigProperties(wizardStep12Controller);
      router.transitionTo('step4');
    }
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router) {
      console.log('in reassignMaster.step4:connectOutlets');
      var controller = router.get('reassignMasterController');
      controller.setCurrentStep('4');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.connectOutlet('wizardStep13', controller.get('content'));
      })
    },
    back: Em.Router.transitionTo('step3'),
    next: function (router) {
      App.db.setReassignTasksStatuses(['INITIALIZE', 'INITIALIZE', 'INITIALIZE', 'INITIALIZE', 'INITIALIZE', 'INITIALIZE', 'INITIALIZE', 'INITIALIZE']);
      App.clusterStatus.setClusterStatus({
        clusterName: router.get('reassignMasterController.content.cluster.name'),
        clusterState: 'REASSIGN_MASTER_INSTALLING',
        wizardControllerName: 'reassignMasterController',
        localdb: App.db.data
      });
      router.transitionTo('step5');
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
        controller.connectOutlet('wizardStep14', controller.get('content'));
      })
    },
    back: Em.Router.transitionTo('step3'),
    complete: function (router, context) {
      var controller = router.get('reassignMasterController');
      var wizardStep14Controller = router.get('wizardStep14Controller');
      if (!wizardStep14Controller.get('isSubmitDisabled')) {
        controller.finish();
        $(context.currentTarget).parents("#modal").find(".close").trigger('click');
        App.clusterStatus.setClusterStatus({
          clusterName: router.get('reassignMasterController.content.cluster.name'),
          clusterState: 'REASSIGN_MASTER_COMPLETED',
          wizardControllerName: 'reassignMasterController',
          localdb: App.db.data
        });
        router.transitionTo('main.index');
      }
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
