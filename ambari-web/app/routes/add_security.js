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
  route: '/addSecurity',
  App: require('app'),
  enter: function (router) {
    console.log('in /security/add:enter');

    Ember.run.next(function () {
      if (!router.get('mainAdminController.securityEnabled')) {
        router.get('mainAdminSecurityController').setAddSecurityWizardStatus('RUNNING');
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

          onPrimary: function () {
            this.hide();
            App.router.get('updateController').set('isWorking', true);
            router.transitionTo('adminSecurity.index');
          },
          onClose: function () {
            this.hide();
            App.router.get('updateController').set('isWorking', true);
            mainAdminSecurityController.setAddSecurityWizardStatus(null);
            router.get('addSecurityController').setCurrentStep(1);
            router.get('addSecurityController.content').saveCurrentStage(2);
            router.transitionTo('adminSecurity.index');
          },
          didInsertElement: function () {
            this.fitHeight();
          }
        });
        App.router.transitionTo('step' + currentStep);
      } else {
        router.transitionTo('adminSecurity.index');
      }
    });
  },

  step1: Em.Route.extend({
    route: '/start',
    connectOutlets: function (router) {
      console.log('in addSecurity.step1:connectOutlets');
      var controller = router.get('addSecurityController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('1');
        controller.loadAllPriorSteps();
        controller.connectOutlet('mainAdminSecurityAddStep1', controller.get('content'));
      })
    },

    next: function (router) {
      var addSecurityController = router.get('addSecurityController');
      addSecurityController.get('content').set('serviceConfigProperties', null);
      router.transitionTo('step2');
    }
  }),

  step2: Em.Route.extend({
    route: '/configure',
    connectOutlets: function (router) {
      console.log('in addSecurity.step2:connectOutlets');
      var controller = router.get('addSecurityController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('2');
        controller.loadAllPriorSteps();
        controller.connectOutlet('mainAdminSecurityAddStep2', controller.get('content'));
      })
    },
    back: Em.Router.transitionTo('step1'),
    next: function (router) {
      var addSecurityController = router.get('addSecurityController');
      var addSecurityStep2Controller = router.get('mainAdminSecurityAddStep2Controller');
      addSecurityController.saveServiceConfigProperties(addSecurityStep2Controller);
      addSecurityController.get('content').saveCurrentStage('2');
      router.transitionTo('step3');
    }
  }),

  step3: Em.Route.extend({
    route: '/apply',
    connectOutlets: function (router) {
      console.log('in addSecurity.step3:connectOutlets');
      var controller = router.get('addSecurityController');
      controller.dataLoading().done(function () {
        controller.setCurrentStep('3');
        controller.loadAllPriorSteps();
        controller.connectOutlet('mainAdminSecurityAddStep3', controller.get('content'));
      })
    },
    back: function (router, context) {
      var controller = router.get('mainAdminSecurityAddStep3Controller');
      if (!controller.get('isSubmitDisabled')) {
        router.transitionTo('step2');
      }
    },
    done: function (router, context) {
      router.get('mainAdminSecurityController').setAddSecurityWizardStatus(null);
      var controller = router.get('mainAdminSecurityAddStep3Controller');
      if (!controller.get('isSubmitDisabled')) {
        $(context.currentTarget).parents("#modal").find(".close").trigger('click');
      }
    }
  }),

  gotoStep1: Em.Router.transitionTo('step1'),

  gotoStep2: Em.Router.transitionTo('step2'),

  gotoStep3: Em.Router.transitionTo('step3')

});

