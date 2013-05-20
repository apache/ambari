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
    var hosts = App.db.getHosts();
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
      App.db.setHosts(hosts);
    }

    this.set('content.hosts', hosts);
    console.log('AddServiceController.loadConfirmedHosts: loaded hosts', hosts);
  },

  /**
   * Load services data from server.
   */
  loadServicesFromServer: function() {
    if(App.db.getService()){
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
    App.db.setService(apiService);
  },

  /**
   * Load services data. Will be used at <code>Select services(step4)</code> step
   */
  loadServices: function () {
    var servicesInfo = App.db.getService();
    servicesInfo.forEach(function (item, index) {
      servicesInfo[index] = Em.Object.create(item);
    });
    this.set('content.services', servicesInfo);
    console.log('AddServiceController.loadServices: loaded data ', servicesInfo);

    var serviceNames = servicesInfo.filterProperty('isSelected', true).filterProperty('isDisabled', false).mapProperty('serviceName');
    console.log('selected services ', serviceNames);

    this.set('content.skipSlavesStep', !serviceNames.contains('MAPREDUCE') && !serviceNames.contains('HBASE'));
  },

  /**
   * Save data to model
   * @param stepController App.WizardStep4Controller
   */
  saveServices: function (stepController) {var serviceNames = [];
    App.db.setService(stepController.get('content'));
    console.log('AddServiceController.saveServices: saved data', stepController.get('content'));
    stepController.filterProperty('isSelected', true).filterProperty('isInstalled', false).forEach(function (item) {
      serviceNames.push(item.serviceName);
    });
    this.set('content.selectedServiceNames', serviceNames);
    App.db.setSelectedServiceNames(serviceNames);
    console.log('AddServiceController.selectedServiceNames:', serviceNames);

    this.set('content.skipSlavesStep', !serviceNames.contains('MAPREDUCE') && !serviceNames.contains('HBASE'));
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
    App.db.setMasterComponentHosts(masterComponentHosts);
    this.set('content.masterComponentHosts', masterComponentHosts);

    this.set('content.skipMasterStep', this.get('content.masterComponentHosts').everyProperty('isInstalled', true));
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadMasterComponentHosts: function () {
    var masterComponentHosts = App.db.getMasterComponentHosts();
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
    },{
      name: 'HBASE_REGIONSERVER',
      service: 'HBASE'
    }];

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
    })

    return result;
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadSlaveComponentHosts: function () {
    var slaveComponentHosts = App.db.getSlaveComponentHosts();
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
    var clients = App.db.getClientsForSelectedServices();
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

    App.db.setClientsForSelectedServices(clients);
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
  }

});
