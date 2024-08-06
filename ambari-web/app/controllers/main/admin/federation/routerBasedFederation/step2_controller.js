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

App.RouterFederationWizardStep2Controller = Em.Controller.extend(App.AssignMasterComponents, {

    name: "routerFederationWizardStep2Controller",

    useServerValidation: false,

    mastersToShow: ['NAMENODE', 'ROUTER'],

    mastersToAdd: ['ROUTER'],

    showCurrentPrefix: ['NAMENODE', 'ROUTER'],

    showAdditionalPrefix: ['ROUTER'],

    mastersAddableInHA: ['ROUTER'],

    showInstalledMastersFirst: true,

    renderComponents: function (masterComponents) {
        // check if we are restoring components assignment by checking existence of ROUTER component in array
        var restoringComponents = masterComponents.someProperty('component_name', 'ROUTER');
        masterComponents = restoringComponents ? masterComponents : masterComponents.concat(this.generateRouterComponents());
        this._super(masterComponents);
        // if you have similar functions for router, call them here
    },


    generateRouterComponents: function () {
        var router = [];
        App.HostComponent.find().filterProperty('componentName', 'ROUTER').forEach(function (rbf) {
            var rbfComponent = this.createComponentInstallationObject(Em.Object.create({
                serviceName: rbf.get('service.serviceName'),
                componentName: rbf.get('componentName')
            }), rbf.get('hostName'));
            rbfComponent.isInstalled = true;
            router.push(rbfComponent);
        }, this);
        return router;
    },

    actions: {
        back() {
            this.clearStep()
        }
    }

});
