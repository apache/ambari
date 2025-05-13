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
    route: '/NameNode/federation/routerBasedFederation',

    breadcrumbs: {
        label: Em.I18n.t('admin.routerFederation.wizard.header')
    },

    enter: function (router, transition) {
        var routerFederationWizardController = router.get('routerFederationWizardController');
        routerFederationWizardController.dataLoading().done(function () {
            //Set HDFS as current service
            App.router.set('mainServiceItemController.content', App.Service.find().findProperty('serviceName', 'HDFS'));
            App.router.get('updateController').set('isWorking', false);
            var popup = App.ModalPopup.show({
                classNames: ['wizard-modal-wrapper'],
                modalDialogClasses: ['modal-xlg'],
                header: Em.I18n.t('admin.routerFederation.wizard.header'),
                bodyClass: App.RouterFederationWizardView.extend({
                    controller: routerFederationWizardController
                }),
                primary: Em.I18n.t('form.cancel'),
                showFooter: false,
                secondary: null,

                onClose: function () {
                    var routerFederationWizardController = router.get('routerFederationWizardController'),
                        currStep = routerFederationWizardController.get('currentStep');
                    App.showConfirmationPopup(function () {
                        routerFederationWizardController.resetOnClose(routerFederationWizardController, 'main.services.index');
                    }, Em.I18n.t(parseInt(currStep) === 4 ? 'admin.routerFederation.closePopup2' : 'admin.routerFederation.closePopup'));
                },
                didInsertElement: function () {
                    this._super();
                    this.fitHeight();
                }
            });
            routerFederationWizardController.set('popup', popup);
            var currentClusterStatus = App.clusterStatus.get('value');
            if (currentClusterStatus) {
                switch (currentClusterStatus.clusterState) {
                    case 'RBF_FEDERATION_DEPLOY' :
                        routerFederationWizardController.setCurrentStep(currentClusterStatus.localdb.RouterFederationWizard.currentStep);
                        break;
                    default:
                        var currStep = App.router.get('routerFederationWizardController.currentStep');
                        routerFederationWizardController.setCurrentStep(currStep);
                        break;
                }
            }
            Em.run.next(function () {
                App.router.get('wizardWatcherController').setUser(routerFederationWizardController.get('name'));
                router.transitionTo('step' + routerFederationWizardController.get('currentStep'));
            });
        });
    },

    step1: Em.Route.extend({
        route: '/step1',
        connectOutlets: function (router) {
            var controller = router.get('routerFederationWizardController');
            controller.dataLoading().done(function () {
                controller.setCurrentStep('1');
                controller.connectOutlet('routerFederationWizardStep1', controller.get('content'));
            })
        },
        unroutePath: function () {
            return false;
        },
        next: function (router) {
            var controller = router.get('routerFederationWizardController');
            router.transitionTo('step2');
        }
    }),

    step2: Em.Route.extend({
        route: '/step2',
        connectOutlets: function (router) {
            var controller = router.get('routerFederationWizardController');
            controller.dataLoading().done(function () {
                controller.setCurrentStep('2');
                controller.loadAllPriorSteps();
                controller.connectOutlet('routerFederationWizardStep2', controller.get('content'));
            })
        },
        unroutePath: function () {
            return false;
        },
        next: function (router) {
            var wizardController = router.get('routerFederationWizardController');
            var stepController = router.get('routerFederationWizardStep2Controller');
            wizardController.saveMasterComponentHosts(stepController);
            router.transitionTo('step3');
        },
        back: function (router) {
            router.transitionTo('step1');
        }
    }),

    step3: Em.Route.extend({
        route: '/step3',
        connectOutlets: function (router) {
            var controller = router.get('routerFederationWizardController');
            controller.dataLoading().done(function () {
                controller.setCurrentStep('3');
                controller.loadAllPriorSteps();
                controller.connectOutlet('routerFederationWizardStep3', controller.get('content'));
            })
        },
        unroutePath: function () {
            return false;
        },
        next: function (router) {
            var controller = router.get('routerFederationWizardController');
            var stepController = router.get('routerFederationWizardStep3Controller');
            controller.saveServiceConfigProperties(stepController);
            router.transitionTo('step4');
        },
        back: Em.Router.transitionTo('step2')
    }),

    step4: Em.Route.extend({
        route: '/step4',
        connectOutlets: function (router) {
            var controller = router.get('routerFederationWizardController');
            controller.dataLoading().done(function () {
                controller.setCurrentStep('4');
                controller.setLowerStepsDisable(4);
                controller.loadAllPriorSteps();
                controller.connectOutlet('routerFederationWizardStep4', controller.get('content'));
            })
        },
        unroutePath: function (router, path) {
            // allow user to leave route if wizard has finished
            if (router.get('routerFederationWizardController').get('isFinished')) {
                this._super(router, path);
            } else {
                return false;
            }
        },
        next: function (router) {
            var controller = router.get('routerFederationWizardController');
            controller.resetOnClose(controller, 'main.services.index');
        }
    })

});