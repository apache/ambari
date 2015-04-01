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

App.WidgetWizardController = App.WizardController.extend({

  name: 'widgetWizardController',

  totalSteps: 3,

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,


  content: Em.Object.create({
    controllerName: 'widgetWizardController',
    widgetService: null,
    widgetType: '',
    widgetProperties: [],
    widgetMetrics: [],
    widgetValues: [],
    widgetName: null,
    widgetDescription: null,
    widgetScope: null
  }),

  /**
   * set current step
   * @param {string} currentStep
   * @param {boolean} completed
   * @param {boolean} skipStateSave
   */
  setCurrentStep: function (currentStep, completed, skipStateSave) {
    this._super(currentStep, completed);
    if (App.get('testMode') || skipStateSave) {
      return;
    }
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'WIDGET_DEPLOY',
      wizardControllerName: 'widgetWizardController',
      localdb: App.db.data
    });
  },

  setStepsEnable: function () {
    for (var i = 1; i <= this.get('totalSteps'); i++) {
      var step = this.get('isStepDisabled').findProperty('step', i);
      if (i <= this.get('currentStep') && App.get('router.clusterController.isLoaded')) {
        step.set('value', false);
      } else {
        step.set('value', i != this.get('currentStep'));
      }
    }
  }.observes('currentStep', 'App.router.clusterController.isLoaded'),



  /**
   * save status of the cluster.
   * @param clusterStatus object with status,requestId fields.
   */
  saveClusterStatus: function (clusterStatus) {
    var oldStatus = this.toObject(this.get('content.cluster'));
    clusterStatus = jQuery.extend(oldStatus, clusterStatus);
    if (clusterStatus.requestId) {
      clusterStatus.requestId.forEach(function (requestId) {
        if (clusterStatus.oldRequestsId.indexOf(requestId) === -1) {
          clusterStatus.oldRequestsId.push(requestId)
        }
      }, this);
    }
    this.set('content.cluster', clusterStatus);
    this.save('cluster');
  },

  loadWidgetService: function() {
    this.set('content.widgetService', this.getDBProperty('widgetService'));
  },

  loadWidgetType: function() {
    this.set('content.widgetType', this.getDBProperty('widgetType'));
  },

  loadWidgetProperties: function() {
    this.set('content.widgetProperties', this.getDBProperty('widgetProperties'));
  },

  loadWidgetMetrics: function() {
    this.set('content.widgetMetrics', this.getDBProperty('widgetMetrics'));
  },

  loadWidgetValues: function() {
    this.set('content.widgetValues', this.getDBProperty('widgetValues'));
  },


  saveWidgetService: function(widgetService) {
    this.setDBProperty('widgetService',widgetService);
    this.set('content.widgetService', widgetService);
  },

  saveWidgetType: function(widgetType) {
    this.setDBProperty('widgetType',widgetType);
    this.set('content.widgetType', widgetType);
  },

  saveWidgetProperties: function(widgetProperties) {
    this.setDBProperty('widgetProperties',widgetProperties);
    this.set('content.widgetProperties', widgetProperties);
  },

  saveWidgetMetrics: function(widgetMetrics) {
    this.setDBProperty('widgetMetrics',widgetMetrics);
    this.set('content.widgetMetrics', widgetMetrics);
  },

  saveWidgetValues: function(widgetValues) {
    this.setDBProperty('widgetValues',widgetValues);
    this.set('content.widgetValues', widgetValues);
  },

  loadMap: {
    '1': [
      {
        type: 'sync',
        callback: function () {
          this.loadWidgetService();
          this.loadWidgetType();
        }
      }
    ],
    '2': [
      {
        type: 'sync',
        callback: function () {
          this.loadWidgetProperties();
          this.loadWidgetMetrics();
          this.loadWidgetValues();
        }
      }
    ]
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

  clearTasksData: function () {
    this.saveTasksStatuses(undefined);
    this.saveRequestIds(undefined);
    this.saveTasksRequestIds(undefined);
  },

  /**
   * Clear all temporary data
   */
  finish: function () {
    this.setCurrentStep('1', false, true);
    this.saveWidgetType('');
    this.resetDbNamespace();
    App.get('router.updateController').updateAll();
  }
});
