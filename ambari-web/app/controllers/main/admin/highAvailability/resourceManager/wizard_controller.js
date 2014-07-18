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

App.RMHighAvailabilityWizardController = App.WizardController.extend({

  name: 'rMHighAvailabilityWizardController',

  totalSteps: 4,

  content: Em.Object.create({
    controllerName: 'rMHighAvailabilityWizardController'
  }),

  setCurrentStep: function (currentStep, completed) {
    this._super(currentStep, completed);
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'RM_HIGH_AVAILABILITY_DEPLOY',
      wizardControllerName: 'rMHighAvailabilityWizardController',
      localdb: App.db.data
    });
  },

  /**
   * Load data for all steps until <code>current step</code>
   */
  loadAllPriorSteps: function () {
    var step = this.get('currentStep');
    switch (step) {
      case '4':
      case '3':
      case '2':
        this.loadServicesFromServer();
        this.loadMasterComponentHosts();
        this.loadConfirmedHosts();
      case '1':
        this.load('cluster');
    }
  },

  /**
   * Remove all loaded data.
   * Created as copy for App.router.clearAllSteps
   */
  clearAllSteps: function () {
    this.clearInstallOptions();
    // clear temporary information stored during the install
    this.set('content.cluster', this.getCluster());
  },

  /**
   * Clear all temporary data
   */
  finish: function () {
    this.setCurrentStep(1);
    App.db.data.RMHighAvailabilityWizard = {};
    App.router.get('updateController').updateAll();
  }
});
