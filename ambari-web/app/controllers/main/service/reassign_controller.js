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
    componentsWithManualCommands: ['NAMENODE', 'SECONDARY_NAMENODE', 'OOZIE_SERVER', 'MYSQL_SERVER', 'APP_TIMELINE_SERVER'],
    hasManualSteps: false,
    hasCheckDBStep: false,
    componentsWithCheckDBStep: ['HIVE_METASTORE', 'HIVE_SERVER', 'OOZIE_SERVER'],
    componentsWithoutSecurityConfigs: ['MYSQL_SERVER'],
    reassignComponentsInMM: []
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
    'currentStep',
    'reassignHosts',
    'tasksStatuses',
    'tasksRequestIds',
    'requestIds'
  ],

  addManualSteps: function () {
    var hasManualSteps = this.get('content.componentsWithManualCommands').contains(this.get('content.reassign.component_name'));
    this.set('content.hasManualSteps', hasManualSteps);
    this.set('totalSteps', hasManualSteps ? 6 : 4);
  }.observes('content.reassign.component_name'),

  addCheckDBStep: function () {
    this.set('content.hasCheckDBStep', this.get('content.componentsWithCheckDBStep').contains(this.get('content.reassign.component_name')));
  }.observes('content.reassign.component_name'),

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

  saveSecureConfigs: function (secureConfigs) {
    this.setDBProperty('secureConfigs', secureConfigs);
    this.set('content.secureConfigs', secureConfigs);
  },

  loadSecureConfigs: function () {
    var secureConfigs = this.getDBProperty('secureConfigs');
    this.set('content.secureConfigs', secureConfigs);
  },

  saveServiceProperties: function (properties) {
    this.setDBProperty('serviceProperties', properties);
    this.set('content.serviceProperties', properties);
  },

  loadServiceProperties: function () {
    var serviceProperties = this.getDBProperty('serviceProperties');
    this.set('content.serviceProperties', serviceProperties);
  },

  saveDatabaseType: function (type) {
    this.setDBProperty('databaseType', type);
    this.set('content.databaseType', type);
  },

  loadDatabaseType: function () {
    var databaseType = this.getDBProperty('databaseType');
    this.set('content.databaseType', databaseType);
    var component = this.get('content.reassign.component_name');

    if (component === 'OOZIE_SERVER') {
      if (this.get('content.hasCheckDBStep') && databaseType && databaseType !== 'derby') {
        // components with manual commands
        var manual = App.router.reassignMasterController.get('content.componentsWithManualCommands').without('OOZIE_SERVER');
        App.router.reassignMasterController.set('content.hasManualSteps', false);
        App.router.reassignMasterController.set('content.componentsWithManualCommands', manual);
        this.set('totalSteps', 4);
      }
    }
  },

  loadReassignComponentsInMM: function () {
    var reassignComponentsInMM = this.getDBProperty('reassignComponentsInMM');
    this.set('content.reassignComponentsInMM', reassignComponentsInMM);
  },

  saveReassignComponentsInMM: function (reassignComponentsInMM) {
    this.setDBProperty('reassignComponentsInMM', reassignComponentsInMM);
    this.set('content.reassignComponentsInMM', reassignComponentsInMM);
  },

  /**
   * Load data for all steps until <code>current step</code>
   */
  loadAllPriorSteps: function () {
    var step = this.get('currentStep');
    switch (step) {
      case '7':
      case '6':
      case '5':
        this.loadSecureConfigs();
        this.loadComponentDir();
      case '4':
        this.loadTasksStatuses();
        this.loadTasksRequestIds();
        this.loadRequestIds();
        this.loadReassignComponentsInMM();
      case '3':
        this.loadReassignHosts();
      case '2':
        this.loadServicesFromServer();
        this.loadMasterComponentHosts();
        this.loadConfirmedHosts();
      case '1':
        this.loadComponentToReassign();
        this.loadDatabaseType();
        this.loadServiceProperties();
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

  setCurrentStep: function (currentStep, completed) {
    this._super(currentStep, completed);
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      wizardControllerName: 'reassignMasterController',
      localdb: App.db.data
    });
  },

  getReassignComponentsInMM: function () {
    var hostComponentsInMM = [];
    var sourceHostComponents = App.HostComponent.find().filterProperty('hostName', this.get('content.reassignHosts.source'));
    var reassignComponents = this.get('content.reassign.component_name') === 'NAMENODE' && App.get('isHaEnabled') ? ['NAMENODE', 'ZKFC'] : [this.get('content.reassign.component_name')];
    reassignComponents.forEach(function(hostComponent){
      var componentToReassign = sourceHostComponents.findProperty('componentName', hostComponent);
      if (componentToReassign && !componentToReassign.get('isActive') && componentToReassign.get('workStatus') === 'STARTED') {
        hostComponentsInMM.push(hostComponent);
      }
    });
    return hostComponentsInMM;
  },

  /**
   * Clear all temporary data
   */
  finish: function () {
    this.clearAllSteps();
    this.clearStorageData();
    this.resetDbNamespace();
    App.router.get('updateController').updateAll();
  }

});
