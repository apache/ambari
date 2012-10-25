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

    if (router.getAuthenticated()) {
      router.get('mainController').stopLoadOperationsPeriodically();
      console.log('In installer with successful authenticated');
      // router.loadAllPriorSteps(router.getInstallerCurrentStep());
      Ember.run.next(function () {
        router.transitionTo('step' + router.getInstallerCurrentStep());
      });
    } else {
      console.log('In installer but its not authenticated');
      console.log('value of authenticated is: ' + router.getAuthenticated());
      Ember.run.next(function () {
        router.transitionTo('login');
      });
    }
  },

  routePath: function (router, event) {
    console.log("INFO: value of router is: " + router);
    console.log("INFO: value of event is: " + event);
    router.setNavigationFlow(event);
    if (!router.isFwdNavigation) {
      this._super(router, event);
    } else {
      router.set('backBtnForHigherStep', true);
      router.transitionTo('step' + router.getInstallerCurrentStep());
    }
  },

  connectOutlets: function (router, context) {
    console.log('in /installer:connectOutlets');
    router.get('applicationController').connectOutlet('installer');
  },

  step1: Em.Route.extend({
    route: '/step1',
    enter: function (router) {

    },
    connectOutlets: function (router, context) {
      console.log('in installer.step1:connectOutlets');
      router.setNavigationFlow('step1');
      router.setInstallerCurrentStep('1', false);
      router.get('installerController').connectOutlet('installerStep1');
    },
    next: function (router, context) {
      // App.db.setAllHostNames(undefined);
      // App.db.setInstallType(undefined);
      //App.db.setSoftRepo(undefined);
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router, context) {
      router.setNavigationFlow('step2');
      router.setInstallerCurrentStep('2', false);
      router.get('installerController').connectOutlet('installerStep2');
    },
    back: Em.Router.transitionTo('step1'),
    next: function (router, context) {
      App.db.setBootStatus(false);
      var hosts = App.db.getHosts();
      var hostInfo = {};
      for (var index in hosts) {
        hostInfo[index] = {
          name: hosts[index].name,
          bootStatus: 'pending'
        };
      }
      App.db.setHosts(hostInfo);
      router.transitionTo('step3');
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router, context) {
      router.setNavigationFlow('step3');
      router.setInstallerCurrentStep('3', false);
      router.get('installerController').connectOutlet('installerStep3');
    },
    back: Em.Router.transitionTo('step2'),
    next: function (router, context) {
      App.db.setBootStatus(true);
      App.db.setService(require('data/mock/services'));
      router.transitionTo('step4');
    }
  }),

  step4: Em.Route.extend({
    route: '/step4',
    connectOutlets: function (router, context) {
      router.setNavigationFlow('step4');
      router.setInstallerCurrentStep('4', false);
      router.get('installerController').connectOutlet('installerStep4');
    },
    back: Em.Router.transitionTo('step3'),
    next: function (router, context) {
      App.db.setMasterComponentHosts(undefined);
      App.db.setHostToMasterComponent(undefined);
      router.transitionTo('step5');
    }
  }),

  step5: Em.Route.extend({
    route: '/step5',
    connectOutlets: function (router, context) {
      router.setNavigationFlow('step5');
      router.setInstallerCurrentStep('5', false);
      router.get('installerController').connectOutlet('installerStep5');
    },
    back: Em.Router.transitionTo('step4'),
    next: function (router, context) {
      App.db.setSlaveComponentHosts(undefined);
      router.transitionTo('step6');
    }
  }),

  step6: Em.Route.extend({
    route: '/step6',
    connectOutlets: function (router, context) {
      router.setNavigationFlow('step6');
      router.setInstallerCurrentStep('6', false);
      router.get('installerController').connectOutlet('installerStep6');
    },
    back: Em.Router.transitionTo('step5'),
    next: function (router, context) {
      App.db.setServiceConfigProperties(undefined);
      router.transitionTo('step7');
    }
  }),

  step7: Em.Route.extend({
    route: '/step7',
    connectOutlets: function (router, context) {
      var controller = router.get('installerController');
      router.setInstallerCurrentStep('7', false);
      controller.loadAllPriorSteps();
      controller.connectOutlet('wizardStep7', controller.get('content'));
    },
    back: Em.Router.transitionTo('step6'),
    next: function(router){
      var installerController = router.get('installerController');
      var wizardStep7Controller = router.get('wizardStep7Controller');
      installerController.saveServiceConfigProperties( wizardStep7Controller );
      router.transitionTo('step8');
    }
  }),

  step8: Em.Route.extend({
    route: '/step8',
    connectOutlets: function (router, context) {
      console.log('in installer.step8:connectOutlets');
      var controller = router.get('installerController');
      router.setInstallerCurrentStep('8', false);
      controller.loadAllPriorSteps();
      controller.connectOutlet('wizardStep8', controller.get('content'));
    },
    back: Em.Router.transitionTo('step7'),
    next: Em.Router.transitionTo('step9')
  }),

  step9: Em.Route.extend({
    route: '/step9',
    connectOutlets: function (router, context) {
      console.log('in installer.step9:connectOutlets');
      var controller = router.get('installerController');
      controller.setInfoForStep9();
      router.setInstallerCurrentStep('9', false);
      controller.loadAllPriorSteps();
      controller.connectOutlet('wizardStep9', controller.get('content'));
    },
    back: Em.Router.transitionTo('step8'),
    next: function (router) {
      var addHostController = router.get('installerController');
      var wizardStep9Controller = router.get('wizardStep9Controller');
      addHostController.saveClusterInfo(wizardStep9Controller);
      addHostController.saveInstalledHosts(wizardStep9Controller);
      router.transitionTo('step10');
    }
  }),

  step10: Em.Route.extend({
    route: '/step10',
    connectOutlets: function (router, context) {
      router.setNavigationFlow('step10');
      router.setInstallerCurrentStep('10', false);
      router.get('installerController').connectOutlet('installerStep10');
    },
    back: Em.Router.transitionTo('step9'),

    complete: function (router, context) {
      if (true) {   // this function will be moved to installerController where it will validate
        router.setInstallerCurrentStep('1', true);
        router.setSection('main');
        router.transitionTo('main');
      } else {
        console.log('cluster installation failure');
      }
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

  gotoStep9: Em.Router.transitionTo('step9'),

  gotoStep10: Em.Router.transitionTo('step10')

});