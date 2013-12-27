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
  route: '/hosts/add',

  enter: function (router) {
    console.log('in /hosts/add:enter');

    Ember.run.next(function () {
      var addHostController = router.get('addHostController');
      App.router.get('updateController').set('isWorking', false);
      App.ModalPopup.show({
        classNames: ['full-width-modal'],
        header:Em.I18n.t('hosts.add.header'),
        bodyClass:  App.AddHostView.extend({
          controllerBinding: 'App.router.addHostController'
        }),
        primary:Em.I18n.t('form.cancel'),
        secondary: null,
        showFooter: false,

        onPrimary:function () {
          this.hide();
          App.router.get('updateController').set('isWorking', true);
          router.transitionTo('hosts.index');
        },
        onClose: function() {
          this.hide();
          App.router.get('updateController').set('isWorking', true);
          App.clusterStatus.setClusterStatus({
            clusterName: App.router.get('content.cluster.name'),
            clusterState: 'DEFAULT',
            wizardControllerName: App.router.get('addHostController.name'),
            localdb: App.db.data
          });
          router.transitionTo('hosts.index');
        },
        didInsertElement: function(){
          this.fitHeight();
        }
      });
      var currentClusterStatus = App.clusterStatus.get('value');
      if (currentClusterStatus) {
        switch (currentClusterStatus.clusterState) {
          case 'ADD_HOSTS_DEPLOY_PREP_2' :
            addHostController.setCurrentStep('4');
            break;
          case 'ADD_HOSTS_INSTALLING_3' :
          case 'SERVICE_STARTING_3' :
            addHostController.setCurrentStep('5');
            break;
          case 'ADD_HOSTS_INSTALLED_4' :
            addHostController.setCurrentStep('6');
            break;
          default:
            break;
        }
      }

      router.transitionTo('step' + addHostController.get('currentStep'));
    });

  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      console.log('in addHost.step1:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('1');
      controller.set('hideBackButton', true);
      controller.dataLoading().done(function () {
        controller.loadServicesFromServer();
        controller.loadAllPriorSteps();
        var wizardStep2Controller = router.get('wizardStep2Controller');
        wizardStep2Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep2', controller.get('content'));
      })
    },

    next: function (router) {
      var controller = router.get('addHostController');
      controller.save('installOptions');
      //hosts was saved to content.hosts inside wizardStep2Controller
      controller.save('hosts');
      router.transitionTo('step2');
      controller.setDBProperty('bootStatus', false);
    },
    evaluateStep: function (router) {
      console.log('in addHost.step1:evaluateStep');
      var addHostController = router.get('addHostController');
      var wizardStep2Controller = router.get('wizardStep2Controller');

      wizardStep2Controller.set('hasSubmitted', true);

      if (!wizardStep2Controller.get('isSubmitDisabled')) {
        wizardStep2Controller.evaluateStep();
      }
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      console.log('in addHost.step2:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('2');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        var wizardStep3Controller = router.get('wizardStep3Controller');
        wizardStep3Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep3', controller.get('content'));
      })
    },
    back: function(router){
      router.transitionTo('step1');
    },
    exit: function (router) {
      router.get('wizardStep3Controller').set('stopBootstrap', true);
    },
    next: function (router, context) {
      var addHostController = router.get('addHostController');
      var wizardStep3Controller = router.get('wizardStep3Controller');
      addHostController.saveConfirmedHosts(wizardStep3Controller);
      addHostController.saveClients();

      addHostController.setDBProperty('bootStatus', true);
      router.transitionTo('step3');
    },
    /**
     * Wrapper for remove host action.
     * Since saving data stored in addHostController, we should call this from router
     * @param router
     * @param context Array of hosts to delete
     */
    removeHosts: function (router, context) {
      console.log('in addHost.step2.removeHosts:hosts to delete ', context);
      var controller = router.get('addHostController');
      controller.removeHosts(context);
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router, context) {
      console.log('in addHost.step3:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('3');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        var wizardStep6Controller = router.get('wizardStep6Controller');
        wizardStep6Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep6', controller.get('content'));
        wizardStep6Controller.set('isMasters', false);
      });
    },
    back: Em.Router.transitionTo('step2'),
    next: function (router) {
      var addHostController = router.get('addHostController');
      var wizardStep6Controller = router.get('wizardStep6Controller');

      if (wizardStep6Controller.validate()) {
        addHostController.saveSlaveComponentHosts(wizardStep6Controller);
        if(App.supports.hostOverrides){
          router.transitionTo('step4');
        }else{
          router.transitionTo('step5');
        }
      }
    }
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router, context) {
      console.log('in addHost.step4:connectOutlets');
      var controller = router.get('addHostController');
      var addHostStep4Controller = router.get('addHostStep4Controller');
      controller.setCurrentStep('4');
      addHostStep4Controller.loadConfigGroups();
      addHostStep4Controller.configGroupsLoading().done(function () {
        controller.dataLoading().done(function () {
          controller.loadAllPriorSteps();
          controller.loadServiceConfigGroups();
          addHostStep4Controller.set('wizardController', controller);
          controller.connectOutlet('addHostStep4', controller.get('content'));
        })
      })
    },
    back: function(router){
        router.transitionTo('step3');
    },
    next: function (router) {
      var addHostController = router.get('addHostController');
      addHostController.saveServiceConfigGroups();
      router.transitionTo('step5');
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',
    connectOutlets: function (router, context) {
      console.log('in addHost.step5:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('5');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        controller.getServiceConfigGroups();
        var wizardStep8Controller = router.get('wizardStep8Controller');
        wizardStep8Controller.set('wizardController', controller);
        controller.connectOutlet('wizardStep8', controller.get('content'));
      })
    },
    back: function(router){
      if(!router.get('wizardStep8Controller.isBackBtnDisabled')) {
        if(App.supports.hostOverrides){
          router.transitionTo('step4');
        }else{
          router.transitionTo('step3');
        }
      }
    },
    next: function (router) {
      var addHostController = router.get('addHostController');
      var wizardStep8Controller = router.get('wizardStep8Controller');
      if(App.supports.hostOverrides){
        addHostController.applyConfigGroup();
      }
      addHostController.installServices();
      addHostController.setInfoForStep9();
      // We need to do recovery based on whether we are in Add Host or Installer wizard
      addHostController.saveClusterState('ADD_HOSTS_INSTALLING_3');
      wizardStep8Controller.set('servicesInstalled', true);
      router.transitionTo('step6');
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',
    connectOutlets: function (router, context) {
      console.log('in addHost.step6:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('6');
      controller.dataLoading().done(function () {
        var wizardStep9Controller = router.get('wizardStep9Controller');
        wizardStep9Controller.set('wizardController', controller);
        controller.loadAllPriorSteps();
        if (!App.testMode) {              //if test mode is ON don't disable prior steps link.
          controller.setLowerStepsDisable(6);
        }
        controller.connectOutlet('wizardStep9', controller.get('content'));
      })
    },
    back: Em.Router.transitionTo('step5'),
    retry: function(router,context) {
      var addHostController = router.get('addHostController');
      var wizardStep9Controller = router.get('wizardStep9Controller');
      wizardStep9Controller.set('wizardController', addHostController);
      if (wizardStep9Controller.get('showRetry')) {
        if (wizardStep9Controller.get('content.cluster.status') === 'INSTALL FAILED') {
          var isRetry = true;
          addHostController.installServices(isRetry);
          addHostController.setInfoForStep9();
          wizardStep9Controller.resetHostsForRetry();
          // We need to do recovery based on whether we are in Add Host or Installer wizard
          addHostController.saveClusterState('ADD_HOSTS_INSTALLING_3');
        }
        wizardStep9Controller.navigateStep();
      }
    },
    unroutePath: function() {
      return false;
    },
    next: function (router) {
      var addHostController = router.get('addHostController');
      var wizardStep9Controller = router.get('wizardStep9Controller');
      wizardStep9Controller.set('wizardController', addHostController);
      addHostController.saveInstalledHosts(wizardStep9Controller);

      // We need to do recovery based on whether we are in Add Host or Installer wizard
      addHostController.saveClusterState('ADD_HOSTS_INSTALLED_4');
      router.transitionTo('step7');
    }
  }),

  step7: Em.Route.extend({
    route: '/step7',
    connectOutlets: function (router, context) {
      console.log('in addHost.step7:connectOutlets');
      var controller = router.get('addHostController');
      controller.setCurrentStep('7');
      controller.dataLoading().done(function () {
        controller.loadAllPriorSteps();
        var wizardStep10Controller = router.get('wizardStep10Controller');
        wizardStep10Controller.set('wizardController', controller);
        if (!App.testMode) {              //if test mode is ON don't disable prior steps link.
          controller.setLowerStepsDisable(7);
        }
        controller.connectOutlet('wizardStep10', controller.get('content'));
        router.get('updateController').set('isWorking', true);
      })
    },
    back: Em.Router.transitionTo('step6'),
    complete: function (router, context) {
      var addHostController = router.get('addHostController');
      var hostsUrl = '/hosts?fields=Hosts/host_name,Hosts/public_host_name,Hosts/cpu_count,Hosts/total_mem,' +
        'Hosts/host_status,Hosts/last_heartbeat_time,Hosts/os_arch,Hosts/os_type,Hosts/ip,host_components,' +
        'metrics/disk,metrics/load/load_one,metrics/cpu/cpu_system,metrics/cpu/cpu_user,metrics/memory/mem_total,metrics/memory/mem_free';
      router.get('clusterController').requestHosts(hostsUrl, function () {
        console.log('Request for hosts, with immutable parameters')
      });
      router.get('updateController').updateAll();
      addHostController.finish();
      $(context.currentTarget).parents("#modal").find(".close").trigger('click');

      // We need to do recovery based on whether we are in Add Host or Installer wizard
      addHostController.saveClusterState('DEFAULT');

      location.reload();
    }
  }),

  backToHostsList: function (router, event) {
    App.router.get('updateController').set('isWorking', true);
    router.transitionTo('hosts.index');
  },

  gotoStep1: Em.Router.transitionTo('step1'),

  gotoStep2: Em.Router.transitionTo('step2'),

  gotoStep3: Em.Router.transitionTo('step3'),

  gotoStep4: Em.Router.transitionTo('step4'),

  gotoStep5: Em.Router.transitionTo('step5'),

  gotoStep6: Em.Router.transitionTo('step6'),

  gotoStep7: Em.Router.transitionTo('step7')


});
