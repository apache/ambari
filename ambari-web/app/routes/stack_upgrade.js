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
  route: 'admin/cluster/upgrade',

  enter: function (router) {
    console.log('in /admin/cluster/upgrade:enter');
    Ember.run.next(function () {
      var stackUpgradeController = router.get('stackUpgradeController');
      App.router.get('updateController').set('isWorking', false);
      App.ModalPopup.show({
        classNames: ['full-width-modal'],
        header:Em.I18n.t('installer.stackUpgrade.header'),
        bodyClass:  App.StackUpgradeView.extend({
          controllerBinding: 'App.router.stackUpgradeController'
        }),
        showFooter: false,
        onClose: function() {
          this.hide();
          App.router.get('updateController').set('isWorking', true);
          router.transitionTo('admin.adminCluster');
        },
        didInsertElement: function(){
          this.fitHeight();
        }
      });
      var statuses = ['STOPPING_SERVICES', 'STACK_UPGRADING', 'STACK_UPGRADE_FAILED', 'STACK_UPGRADED'];
      var currentClusterStatus = App.clusterStatus.get('value');
      if (currentClusterStatus.localdb) App.db.data = currentClusterStatus.localdb;
      if (statuses.contains(currentClusterStatus.clusterState)) {
        stackUpgradeController.setCurrentStep(3);
      }
      router.transitionTo('step' + stackUpgradeController.get('currentStep'));
    });
  },

  step1: Em.Route.extend({
    route: '/step1',
    connectOutlets: function (router) {
      console.log('in stackUpgrade.step1:connectOutlets');
      var controller = router.get('stackUpgradeController');
      controller.setCurrentStep('1');
      controller.loadAllPriorSteps();
      controller.connectOutlet('stackUpgradeStep1', controller.get('content'));
    },
    next: Em.Router.transitionTo('step2')
  }),

  step2: Em.Route.extend({
    route: '/step2',
    connectOutlets: function (router) {
      console.log('in stackUpgrade.step2:connectOutlets');
      var controller = router.get('stackUpgradeController');
      controller.setCurrentStep('2');
      controller.loadAllPriorSteps();
      controller.connectOutlet('stackUpgradeStep2', controller.get('content'));
    },
    back: Em.Router.transitionTo('step1'),
    next: function(router){
      var controller = router.get('stackUpgradeController');
      var stepController = router.get('stackUpgradeStep3Controller');
      controller.save('upgradeOptions');
      router.transitionTo('step3');
      App.clusterStatus.setClusterStatus({
        clusterName: controller.get('content.cluster.name'),
        clusterState: 'PENDING',
        wizardControllerName: 'stackUpgradeController',
        localdb: App.db.data
      });
      stepController.stopServices();
    }
  }),

  step3: Em.Route.extend({
    route: '/step3',
    connectOutlets: function (router, context) {
      console.log('in stackUpgrade.step3:connectOutlets');
      var controller = router.get('stackUpgradeController');
      controller.setCurrentStep('3');
      controller.setLowerStepsDisable(3);
      controller.loadAllPriorSteps();
      controller.connectOutlet('stackUpgradeStep3', controller.get('content'));
    },
    finish: function (router, context) {
      var controller = router.get('stackUpgradeController');
      var stepController = router.get('stackUpgradeStep3Controller');
      controller.finish();
      $(context.currentTarget).parents("#modal").find(".close").trigger('click');
      App.clusterStatus.setClusterStatus({
        clusterName: controller.get('content.cluster.name'),
        clusterState: 'DEFAULT',
        wizardControllerName: 'stackUpgradeController',
        localdb: App.db.data
      });
      App.router.get('updateController').updateAll();
      App.set('currentStackVersion', controller.get('content.upgradeVersion'));
    }
  }),
  backToCluster: function(router, context){
    var controller = router.get('stackUpgradeController');
    controller.finish();
    $(context.currentTarget).parents("#modal").find(".close").trigger('click');
  },

  gotoStep1: Em.Router.transitionTo('step1'),

  gotoStep2: Em.Router.transitionTo('step2'),

  gotoStep3: Em.Router.transitionTo('step3')

});
