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
    route: '/highAvailability/NameNode/MultipleNN/enable',

    breadcrumbs: {
        label: Em.I18n.t('admin.multipleNameNode.wizard.header')
    },

    enter: function (router, transition) {
        var multipleNameNodeWizardController = router.get('multipleNameNodeWizardController');
        multipleNameNodeWizardController.dataLoading().done(function () {
            //Set HDFS as current service
            App.router.set('mainServiceItemController.content', App.Service.find().findProperty('serviceName', 'HDFS'));
            App.router.get('updateController').set('isWorking', false);
            var popup = App.ModalPopup.show({
                classNames: ['wizard-modal-wrapper'],
                modalDialogClasses: ['modal-xlg'],
                header: Em.I18n.t('admin.multipleNameNode.wizard.header'),
                bodyClass: App.MultipleNameNodeWizardView.extend({
                    controller: multipleNameNodeWizardController
                }),
                primary: Em.I18n.t('form.cancel'),
                showFooter: false,
                secondary: null,

                onClose: function () {
                    var multipleNameNodeWizardController = router.get('multipleNameNodeWizardController'),
                        currStep = multipleNameNodeWizardController.get('currentStep');
                    if (parseInt(currStep) === 4) {
                        App.showConfirmationPopup(function () {
                            multipleNameNodeWizardController.resetOnClose(multipleNameNodeWizardController, 'main.services.index');
                        }, Em.I18n.t('admin.multipleNameNode.closePopup2'));
                    } else {
                        multipleNameNodeWizardController.resetOnClose(multipleNameNodeWizardController, 'main.services.index');
                    }
                },
                didInsertElement: function () {
                    this._super();
                    this.fitHeight();
                }
            });
            multipleNameNodeWizardController.set('popup', popup);
            var currentClusterStatus = App.clusterStatus.get('value');
            if (currentClusterStatus) {
                switch (currentClusterStatus.clusterState) {
                    case 'MULTIPLE_NAMENODE_DEPLOY' :
                        multipleNameNodeWizardController.setCurrentStep(currentClusterStatus.localdb.MultipleNameNodeWizard.currentStep);
                        break;
                    default:
                        var currStep = App.router.get('multipleNameNodeWizardController.currentStep');
                        multipleNameNodeWizardController.setCurrentStep(currStep);
                        break;
                }
            }
            Em.run.next(function () {
                App.router.get('wizardWatcherController').setUser(multipleNameNodeWizardController.get('name'));
                router.transitionTo('step' + multipleNameNodeWizardController.get('currentStep'));
            });
        });
    },

    step1: Em.Route.extend({
        route: '/step1',
        connectOutlets: function (router) {
            var controller = router.get('multipleNameNodeWizardController');
            controller.dataLoading().done(function () {
                controller.setCurrentStep('1');
                controller.connectOutlet('multipleNameNodeWizardStep1', controller.get('content'));
            })
        },
        unroutePath: function () {
            return false;
        },
        next: function (router) {
            var controller = router.get('multipleNameNodeWizardController');
            controller.setDBProperty('nnHosts', undefined);
            controller.clearMasterComponentHosts();
            router.transitionTo('step2');
        }
    }),

    step2: Em.Route.extend({
        route: '/step2',
        connectOutlets: function (router) {
            var controller = router.get('multipleNameNodeWizardController');
            controller.dataLoading().done(function () {
                controller.setCurrentStep('2');
                controller.loadAllPriorSteps();
                controller.connectOutlet('multipleNameNodeWizardStep2', controller.get('content'));
            })
        },
        unroutePath: function () {
            return false;
        },
        next: function (router) {
            var wizardController = router.get('multipleNameNodeWizardController');
            var stepController = router.get('multipleNameNodeWizardStep2Controller');
            var currentNN = stepController.get('servicesMasters').filterProperty('component_name', 'NAMENODE').findProperty('isInstalled', true);
            var additionalNN = stepController.get('servicesMasters').filterProperty('component_name', 'NAMENODE').findProperty('isInstalled', false);
            var nnHost = {
                currentNN: currentNN.get('selectedHost'),
                additionalNN: additionalNN.get('selectedHost')
            };
            wizardController.saveNNHosts(nnHost);
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
            var controller = router.get('multipleNameNodeWizardController');
            controller.dataLoading().done(function () {
                controller.setCurrentStep('3');
                controller.loadAllPriorSteps();
                controller.connectOutlet('multipleNameNodeWizardStep3', controller.get('content'));
            })
        },
        unroutePath: function () {
            return false;
        },
        next: function (router) {
            var wizardController = router.get('multipleNameNodeWizardController');
            var stepController = router.get('multipleNameNodeWizardStep3Controller');
            wizardController.saveServiceConfigProperties(stepController);
            var configs = stepController.get('selectedService.configs');
            wizardController.saveConfigs(configs);
            router.transitionTo('step4');
        },
        back: Em.Router.transitionTo('step2')
    }),

    step4: Em.Route.extend({
        route: '/step4',
        connectOutlets: function (router) {
            var controller = router.get('multipleNameNodeWizardController');
            controller.dataLoading().done(function () {
                controller.setCurrentStep('4');
                controller.setLowerStepsDisable(4);
                controller.loadAllPriorSteps();
                controller.connectOutlet('multipleNameNodeWizardStep4', controller.get('content'));
            })
        },
        unroutePath: function (router, path) {
            // allow user to leave route if wizard has finished
            if (router.get('multipleNameNodeWizardController').get('isFinished')) {
                this._super(router, path);
            } else {
                return false;
            }
        },
        next: function (router) {
            var controller = router.get('multipleNameNodeWizardController');
            controller.resetOnClose(controller, 'main.services.index');
        }
    })

});
