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

App.ReassignMasterController = App.WizardController.extend({

  name: 'reassignMasterController',

  totalSteps: 6,

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,

  /**
   * All wizards data will be stored in this variable
   *
   * cluster - cluster name
   * installOptions - ssh key, repo info, etc.
   * services - services list
   * hosts - list of selected hosts
   * slaveComponentHosts, - info about slave hosts
   * masterComponentHosts - info about master hosts
   * config??? - to be described later
   */
  content: Em.Object.create({
    cluster: null,
    hosts: null,
    installOptions: null,
    services: null,
    slaveComponentHosts: null,
    masterComponentHosts: null,
    serviceConfigProperties: null,
    advancedServiceConfig: null,
    controllerName: 'reassignMasterController',
    serviceName: 'MISC',
    hdfsUser: "hdfs",
    group: "hadoop",
    reassign: null,
    componentsWithManualCommands: ['NAMENODE', 'SECONDARY_NAMENODE'],
    hasManualSteps: false,
    securityEnabled: false
  }),

  /**
   * Wizard properties in local storage, which should be cleaned right after wizard has been finished
   */
  dbPropertiesToClean: [
    'cluster',
    'hosts',
    'installOptions',
    'masterComponentHosts',
    'serviceComponents',
    'masterComponent',
    'securityEnabled',
    'currentStep',
    'reassignHosts',
    'tasksStatuses',
    'tasksRequestIds',
    'requestIds'
  ],

  addManualSteps: function () {
    this.set('content.hasManualSteps', this.get('content.componentsWithManualCommands').contains(this.get('content.reassign.component_name')) || this.get('content.securityEnabled'));
  }.observes('content.reassign.component_name', 'content.securityEnabled'),

  getSecurityStatus: function () {
    if (App.get('testMode')) {
      this.set('securityEnabled', !App.get('testEnableSecurity'));
    } else {
      //get Security Status From Server
      App.ajax.send({
        name: 'config.tags',
        sender: this,
        success: 'getSecurityStatusSuccessCallback',
        error: 'errorCallback'
      });
    }
  },

  errorCallback: function () {
    console.error('Cannot get security status from server');
  },

  getSecurityStatusSuccessCallback: function (data) {
    var configs = data.Clusters.desired_configs;
    if ('cluster-env' in configs) {
      this.getServiceConfigsFromServer(configs['cluster-env'].tag);
    }
    else {
      console.error('Cannot get security status from server');
    }
  },

  getServiceConfigsFromServer: function (tag) {
    var self = this;
    var tags = [
      {
        siteName: "cluster-env",
        tagName: tag
      }
    ];
    App.router.get('configurationController').getConfigsByTags(tags).done(function (data) {
      var configs = data.findProperty('tag', tag).properties;
      var result = configs && (configs['security_enabled'] === 'true' || configs['security_enabled'] === true);
      self.saveSecurityEnabled(result);
      App.clusterStatus.setClusterStatus({
        clusterName: self.get('content.cluster.name'),
        clusterState: 'DEFAULT',
        wizardControllerName: 'reassignMasterController',
        localdb: App.db.data
      });
    });
  },

  /**
   * Load tasks statuses for step5 of Reassign Master Wizard to restore installation
   */
  loadTasksStatuses: function () {
    var statuses = App.db.getReassignTasksStatuses();
    this.set('content.tasksStatuses', statuses);
    console.log('ReassignMasterController.loadTasksStatuses: loaded statuses', statuses);
  },

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

  /**
   * Save Master Component Hosts data to Main Controller
   * @param stepController App.WizardStep5Controller
   */
  saveMasterComponentHosts: function (stepController) {
    var obj = stepController.get('selectedServicesMasters');

    var masterComponentHosts = [];
    obj.forEach(function (_component) {
      masterComponentHosts.push({
        display_name: _component.get('display_name'),
        component: _component.get('component_name'),
        hostName: _component.get('selectedHost'),
        serviceId: _component.get('serviceId'),
        isInstalled: true
      });
    });
    App.db.setMasterComponentHosts(masterComponentHosts);
    this.set('content.masterComponentHosts', masterComponentHosts);
  },

  loadComponentToReassign: function () {
    var masterComponent = App.db.getMasterToReassign();
    if (masterComponent) {
      this.set('content.reassign', masterComponent);
    }
  },

  saveComponentToReassign: function (masterComponent) {
    var component = {
      component_name: masterComponent.get('componentName'),
      display_name: masterComponent.get('displayName'),
      service_id: masterComponent.get('service.serviceName'),
      host_id: masterComponent.get('hostName')
    };
    App.db.setMasterToReassign(component);
  },

  saveTasksStatuses: function (statuses) {
    App.db.setReassignTasksStatuses(statuses);
    this.set('content.tasksStatuses', statuses);
    console.log('ReassignMasterController.saveTasksStatuses: saved statuses', statuses);
  },

  loadTasksRequestIds: function () {
    var requestIds = App.db.getReassignTasksRequestIds();
    this.set('content.tasksRequestIds', requestIds);
  },

  saveTasksRequestIds: function (requestIds) {
    App.db.setReassignTasksRequestIds(requestIds);
    this.set('content.tasksRequestIds', requestIds);
  },

  loadRequestIds: function () {
    var requestIds = App.db.getReassignMasterWizardRequestIds();
    this.set('content.requestIds', requestIds);
  },

  saveRequestIds: function (requestIds) {
    App.db.setReassignMasterWizardRequestIds(requestIds);
    this.set('content.requestIds', requestIds);
  },

  saveComponentDir: function (componentDir) {
    App.db.setReassignMasterWizardComponentDir(componentDir);
    this.set('content.componentDir', componentDir);
  },

  loadComponentDir: function () {
    var componentDir = App.db.getReassignMasterWizardComponentDir();
    this.set('content.componentDir', componentDir);
  },

  saveReassignHosts: function (reassignHosts) {
    App.db.setReassignMasterWizardReassignHosts(reassignHosts);
    this.set('content.reassignHosts', reassignHosts);
  },

  loadReassignHosts: function () {
    var reassignHosts = App.db.getReassignMasterWizardReassignHosts();
    this.set('content.reassignHosts', reassignHosts);
  },


  saveSecurityEnabled: function (securityEnabled) {
    this.setDBProperty('securityEnabled', securityEnabled);
    this.set('content.securityEnabled', securityEnabled);
  },

  loadSecurityEnabled: function () {
    var securityEnabled = this.getDBProperty('securityEnabled');
    this.set('content.securityEnabled', securityEnabled);
  },

  saveSecureConfigs: function (secureConfigs) {
    this.setDBProperty('secureConfigs', secureConfigs);
    this.set('content.secureConfigs', secureConfigs);
  },

  loadSecureConfigs: function () {
    var secureConfigs = this.getDBProperty('secureConfigs');
    this.set('content.secureConfigs', secureConfigs);
  },

  /**
   * Load data for all steps until <code>current step</code>
   */
  loadAllPriorSteps: function () {
    var step = this.get('currentStep');
    switch (step) {
      case '6':
      case '5':
        this.loadSecureConfigs();
        this.loadComponentDir();
      case '4':
        this.loadTasksStatuses();
        this.loadTasksRequestIds();
        this.loadRequestIds();
      case '3':
        this.loadReassignHosts();
      case '2':
        this.loadServicesFromServer();
        this.loadMasterComponentHosts();
        this.loadConfirmedHosts();
      case '1':
        this.loadComponentToReassign();
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
    this.setCurrentStep('1');
    this.clearAllSteps();
    this.clearStorageData();
    this.resetDbNamespace();
    App.router.get('updateController').updateAll();
  }

});
