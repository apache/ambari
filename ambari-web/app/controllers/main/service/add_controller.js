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
App.AddServiceController = App.WizardController.extend({

  name: 'addServiceController',

  serviceConfigs:require('data/service_configs'),

  totalSteps: 7,

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
    controllerName: 'addServiceController'
  }),

  setCurrentStep: function (currentStep, completed) {
    this._super(currentStep, completed);
    App.clusterStatus.setClusterStatus({
      wizardControllerName: this.get('name'),
      localdb: App.db.data
    });
  },

  /**
   * return new object extended from clusterStatusTemplate
   * @return Object
   */
  getCluster: function(){
    return jQuery.extend({}, this.get('clusterStatusTemplate'), {name: App.router.getClusterName()});
  },

  /**
   * Load confirmed hosts.
   * Will be used at <code>Assign Masters(step5)</code> step
   */
  loadConfirmedHosts: function(){
    var hosts = this.getDBProperty('hosts');
    if(!hosts){
      var hosts = {};

      App.Host.find().forEach(function(item){
        hosts[item.get('id')] = {
          name: item.get('id'),
          cpu: item.get('cpu'),
          memory: item.get('memory'),
          disk_info: item.get('diskInfo'),
          bootStatus: "REGISTERED",
          isInstalled: true
        };
      });
      this.setDBProperty('hosts', hosts);
    }

    this.set('content.hosts', hosts);
    console.log('AddServiceController.loadConfirmedHosts: loaded hosts', hosts);
  },

  /**
   * Load services data from server.
   */
  loadServicesFromServer: function() {
    if(this.getDBProperty('service')){
      return;
    }
    var displayOrderConfig = require('data/services');
    var apiUrl = App.get('stack2VersionURL');
    var apiService = this.loadServiceComponents(displayOrderConfig, apiUrl);
    //
    apiService.forEach(function(item, index){
      apiService[index].isSelected = App.Service.find().someProperty('id', item.serviceName);
      apiService[index].isDisabled = apiService[index].isSelected;
      apiService[index].isInstalled = apiService[index].isSelected;
    });
    this.set('content.services', apiService);
    this.setDBProperty('service', apiService);
  },

  /**
   * Load services data. Will be used at <code>Select services(step4)</code> step
   */
  loadServices: function () {
    var servicesInfo = this.getDBProperty('service');
    servicesInfo.forEach(function (item, index) {
      servicesInfo[index] = Em.Object.create(item);
    });
    this.set('content.services', servicesInfo);
    console.log('AddServiceController.loadServices: loaded data ', servicesInfo);

    var serviceNames = servicesInfo.filterProperty('isSelected', true).filterProperty('isDisabled', false).mapProperty('serviceName');
    console.log('selected services ', serviceNames);

    this.set('content.skipSlavesStep', !serviceNames.contains('MAPREDUCE') && !serviceNames.contains('HBASE'));
    if (this.get('content.skipSlavesStep')) {
      this.get('isStepDisabled').findProperty('step', 3).set('value', this.get('content.skipSlavesStep'));
    }
  },

  /**
   * Save data to model
   * @param stepController App.WizardStep4Controller
   */
  saveServices: function (stepController) {var serviceNames = [];
    this.setDBProperty('service', stepController.get('content'));
    console.log('AddServiceController.saveServices: saved data', stepController.get('content'));
    stepController.filterProperty('isSelected', true).filterProperty('isInstalled', false).forEach(function (item) {
      serviceNames.push(item.serviceName);
    });
    this.set('content.selectedServiceNames', serviceNames);
    this.setDBProperty('selectedServiceNames',serviceNames);
    console.log('AddServiceController.selectedServiceNames:', serviceNames);

    this.set('content.skipSlavesStep', !serviceNames.contains('MAPREDUCE') && !serviceNames.contains('HBASE'));
    if (this.get('content.skipSlavesStep')) {
      this.get('isStepDisabled').findProperty('step', 3).set('value', this.get('content.skipSlavesStep'));
    }
  },

  /**
   * Save Master Component Hosts data to Main Controller
   * @param stepController App.WizardStep5Controller
   */
  saveMasterComponentHosts: function (stepController) {
    var obj = stepController.get('selectedServicesMasters');
    var masterComponentHosts = [];
    var installedComponents = App.HostComponent.find();

    obj.forEach(function (_component) {
        masterComponentHosts.push({
          display_name: _component.display_name,
          component: _component.component_name,
          hostName: _component.selectedHost,
          serviceId: _component.serviceId,
          isInstalled: installedComponents.someProperty('componentName', _component.component_name)
        });
    });

    console.log("AddServiceController.saveMasterComponentHosts: saved hosts ", masterComponentHosts);
    this.setDBProperty('masterComponentHosts', masterComponentHosts);
    this.set('content.masterComponentHosts', masterComponentHosts);

    this.set('content.skipMasterStep', this.get('content.masterComponentHosts').everyProperty('isInstalled', true));
    this.get('isStepDisabled').findProperty('step', 2).set('value', this.get('content.skipMasterStep'));
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadMasterComponentHosts: function () {
    var masterComponentHosts = this.getDBProperty('masterComponentHosts');
    if(!masterComponentHosts){
      masterComponentHosts = [];
      App.HostComponent.find().filterProperty('isMaster', true).forEach(function(item){
        masterComponentHosts.push({
          component: item.get('componentName'),
          hostName: item.get('host.hostName'),
          isInstalled: true
        })
      });

    }
    this.set("content.masterComponentHosts", masterComponentHosts);
    console.log("AddServiceController.loadMasterComponentHosts: loaded hosts ", masterComponentHosts);

    this.set('content.skipMasterStep', this.get('content.masterComponentHosts').everyProperty('isInstalled', true));
    this.get('isStepDisabled').findProperty('step', 2).set('value', this.get('content.skipMasterStep'));
  },

  /**
   * Does service have any configs
   * @param {string} serviceName
   * @returns {boolean}
   */
  isServiceConfigurable: function(serviceName) {
    return this.get('serviceConfigs').mapProperty('serviceName').contains(serviceName);
  },

  /**
   * Should Config Step be skipped (based on selected services list)
   * @returns {boolean}
   */
  skipConfigStep: function() {
    var skipConfigStep = true;
    var selectedServices = this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false).mapProperty('serviceName');
    selectedServices.map(function(serviceName) {
      skipConfigStep = skipConfigStep && !this.isServiceConfigurable(serviceName);
    }, this);
    return skipConfigStep;
  },

  loadServiceConfigProperties: function() {
    this._super();
    if (!this.get('content.services')) {
      this.loadServices();
    }
    if (this.get('currentStep') > 1 && this.get('currentStep') < 6) {
      this.set('content.skipConfigStep', this.skipConfigStep());
      this.get('isStepDisabled').findProperty('step', 4).set('value', this.get('content.skipConfigStep'));
    }
  },

  saveServiceConfigProperties: function(stepController) {
    this._super(stepController);
    if (this.get('currentStep') > 1 && this.get('currentStep') < 6) {
      this.set('content.skipConfigStep', this.skipConfigStep());
      this.get('isStepDisabled').findProperty('step', 4).set('value', this.get('content.skipConfigStep'));
    }
  },

  /**
   * return slaveComponents bound to hosts
   * @return {Array}
   */
  getSlaveComponentHosts: function () {
    var components = [{
      name : 'DATANODE',
      service : 'HDFS'
    },
    {
      name: 'TASKTRACKER',
      service: 'MAPREDUCE'
    },
    {
      name: 'HBASE_REGIONSERVER',
      service: 'HBASE'
    }];

    if (App.get('isHadoop2Stack')) {
      components.push({
        name: 'NODEMANAGER',
        service: 'YARN'
      });
    }

    var result = [];
    var services = App.Service.find();
    var selectedServices = this.get('content.services').filterProperty('isSelected', true).mapProperty('serviceName');
    for(var index=0; index < components.length; index++){
      var comp = components[index];
      if(!selectedServices.contains(comp.service)){
        continue;
      }


      var service = services.findProperty('id', comp.service);
      var hosts = [];

      if(!service){
        service = services.findProperty('id', 'HDFS');
        service.get('hostComponents').filterProperty('componentName', 'DATANODE').forEach(function (host_component) {
          hosts.push({
            group: "Default",
            hostName: host_component.get('host.id'),
            isInstalled: false
          });
        }, this);
      } else {
        service.get('hostComponents').filterProperty('componentName', comp.name).forEach(function (host_component) {
          hosts.push({
            group: "Default",
            hostName: host_component.get('host.id'),
            isInstalled: true
          });
        }, this);
      }

      result.push({
        componentName: comp.name,
        displayName: App.format.role(comp.name),
        hosts: hosts
      })
    }

    var clientsHosts = App.HostComponent.find().filterProperty('componentName', 'HDFS_CLIENT');
    var hosts = [];

    clientsHosts.forEach(function (host_component) {
        hosts.push({
          group: "Default",
          hostName: host_component.get('host.id'),
          isInstalled: true
        });
    }, this);

    result.push({
      componentName: 'CLIENT',
      displayName: 'client',
      hosts: hosts
    });

    return result;
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadSlaveComponentHosts: function () {
    var slaveComponentHosts = this.getDBProperty('slaveComponentHosts');
    if(!slaveComponentHosts){
      slaveComponentHosts = this.getSlaveComponentHosts();
    }
    this.set("content.slaveComponentHosts", slaveComponentHosts);
    console.log("AddServiceController.loadSlaveComponentHosts: loaded hosts ", slaveComponentHosts);
  },

  /**
   * Load information about hosts with clients components
   */
  loadClients: function(){
    var clients = this.getDBProperty('clientInfo');
    this.set('content.clients', clients);
    console.log("AddServiceController.loadClients: loaded list ", clients);
  },

  /**
   * Generate clients list for selected services and save it to model
   * @param stepController step4WizardController
   */
  saveClients: function(stepController){
    var clients = [];
    var serviceComponents = require('data/service_components');
    var hostComponents = App.HostComponent.find();

    stepController.get('content').filterProperty('isSelected',true).forEach(function (_service) {
      var client = serviceComponents.filterProperty('service_name', _service.serviceName).findProperty('isClient', true);
      if (client) {
        clients.pushObject({
          component_name: client.component_name,
          display_name: client.display_name,
          isInstalled: hostComponents.filterProperty('componentName', client.component_name).length > 0
        });
      }
    }, this);

    this.setDBProperty('clientInfo', clients);
    this.set('content.clients', clients);
    console.log("AddServiceController.saveClients: saved list ", clients);
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
        this.load('cluster');
      case '4':
        this.loadServiceConfigProperties();
      case '3':
        this.loadServices();
        this.loadClients();
        this.loadSlaveComponentHosts();//depends on loadServices
      case '2':
        this.loadMasterComponentHosts();
        this.loadConfirmedHosts();
      case '1':
        this.loadServices();
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
    App.router.get('updateController').updateAll();
  },

  installServices: function (isRetry) {
    this.set('content.cluster.oldRequestsId', []);
    var clusterName = this.get('content.cluster.name');
    var data;
    var name;
    if (isRetry) {
      this.getFailedHostComponents();
      console.log('failedHostComponents', this.get('failedHostComponents'));
      name = 'wizard.install_services.installer_controller.is_retry';
      data = {
        "RequestInfo": {
          "context" : Em.I18n.t('requestInfo.installComponents'),
          "query": "HostRoles/component_name.in(" + this.get('failedHostComponents').join(',') + ")"
        },
        "Body": {
          "HostRoles": {
            "state": "INSTALLED"
          }
        }
      };
      data = JSON.stringify(data);
    }
    else {
      name = 'wizard.install_services.installer_controller.not_is_retry';
      data = '{"RequestInfo": {"context" :"' + Em.I18n.t('requestInfo.installServices') + '"}, "Body": {"ServiceInfo": {"state": "INSTALLED"}}}';
    }
    App.ajax.send({
      name: name,
      sender: this,
      data: {
        data: data,
        cluster: clusterName
      },
      success: 'installServicesSuccessCallback',
      error: 'installServicesErrorCallback'
    });
  },

  /**
   * List of failed to install HostComponents while adding Service
   */
  failedHostComponents: [],

  getFailedHostComponents: function() {
    App.ajax.send({
      name: 'wizard.install_services.add_service_controller.get_failed_host_components',
      sender: this,
      success: 'getFailedHostComponentsSuccessCallback',
      error: 'getFailedHostComponentsErrorCallback'
    });
  },

  /**
   * Parse all failed components and filter out installed earlier components (based on selected to install services)
   * @param {Object} json
   */
  getFailedHostComponentsSuccessCallback: function(json) {
    var allFailed = json.items.filterProperty('HostRoles.state', 'INSTALL_FAILED');
    var currentFailed = [];
    var selectedServices = this.getDBProperty('service').filterProperty('isInstalled', false).filterProperty('isSelected', true).mapProperty('serviceName');
    allFailed.forEach(function(failed) {
      if (selectedServices.contains(failed.component[0].ServiceComponentInfo.service_name)) {
        currentFailed.push(failed.HostRoles.component_name);
      }
    });
    this.set('failedHostComponents', currentFailed);
  },

  getFailedHostComponentsErrorCallback: function(request, ajaxOptions, error) {
    console.warn(error);
  }

});
