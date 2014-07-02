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

App.AddHostController = App.WizardController.extend({

  name: 'addHostController',

  totalSteps: 7,

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,

  /**
   * All wizards data will be stored in this variable
   *
   * cluster - cluster name
   * hosts - hosts, ssh key, repo info, etc.
   * services - services list
   * hostsInfo - list of selected hosts
   * slaveComponentHosts, hostSlaveComponents - info about slave hosts
   * masterComponentHosts - info about master hosts
   * serviceConfigGroups - info about selected config group for service
   * configGroups - all config groups
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
    controllerName: 'addHostController',
    serviceConfigGroups: null,
    configGroups: null
  }),

  /**
   * save info about wizard progress, particularly current step of wizard
   * @param currentStep
   * @param completed
   */
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
  getCluster: function () {
    return jQuery.extend({}, this.get('clusterStatusTemplate'), {name: App.router.getClusterName()});
  },

  /**
   * return new object extended from installOptionsTemplate
   * @return Object
   */
  getInstallOptions: function () {
    return jQuery.extend({}, this.get('installOptionsTemplate'));
  },

  /**
   * Remove host from model. Used at <code>Confirm hosts</code> step
   * @param hosts Array of hosts, which we want to delete
   */
  removeHosts: function (hosts) {
    var dbHosts = this.getDBProperty('hosts');
    hosts.forEach(function (_hostInfo) {
      var host = _hostInfo.hostName;
      delete dbHosts[host];
    });
    this.setDBProperty('hosts', dbHosts);
  },

  /**
   * Load services data from server.
   * TODO move to mixin
   */
  loadServicesFromServer: function () {
    var apiService = this.loadServiceComponents();
    apiService.forEach(function (item, index) {
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
    console.log('AddHostController.loadServices: loaded data ', servicesInfo);
    servicesInfo.forEach(function (item, index) {
      servicesInfo[index] = Em.Object.create(item);
    });
    this.set('content.services', servicesInfo);
    var serviceNames = servicesInfo.filterProperty('isSelected', true).mapProperty('serviceName');
    console.log('selected services ', serviceNames);
  },

  /**
   * Load slave component hosts data for using in required step controllers
   * TODO move to mixin
   */
  loadSlaveComponentHosts: function () {
    var slaveComponentHosts = this.getDBProperty('slaveComponentHosts') || [];
    if (slaveComponentHosts.length) {
      var hosts = this.getDBProperty('hosts'),
          host_names = Em.keys(hosts);
      slaveComponentHosts.forEach(function (component) {
        component.hosts.forEach(function (host) {
          //Em.set(host, 'hostName', hosts[host.host_id].name);
          for (var i = 0; i < host_names.length; i++) {
            if (hosts[host_names[i]].id === host.host_id) {
              host.hostName = host_names[i];
              break;
            }
          }
        });
      });
    }
    this.set("content.slaveComponentHosts", slaveComponentHosts);
    console.log("AddHostController.loadSlaveComponentHosts: loaded hosts ", slaveComponentHosts);
  },

  /**
   * Generate clients list for selected services and save it to model
   */
  saveClients: function () {
    var clients = [];
    var serviceComponents = App.StackServiceComponent.find();
    var clientComponents = [];
    var hosts = this.get('content.hosts');

    for (var hostName in hosts) {
      if(hosts[hostName].isInstalled) {
        hosts[hostName].hostComponents.forEach(function (component) {
          clientComponents[component.HostRoles.component_name] = true;
        }, this);
      }
    }

    this.get('content.services').filterProperty('isSelected').forEach(function (_service) {
      var client = serviceComponents.filterProperty('serviceName', _service.serviceName).findProperty('isClient');
      if (client) {
        clients.push({
          component_name: client.get('componentName'),
          display_name: client.get('displayName'),
          isInstalled: !!clientComponents[client.get('componentName')]
        });
      }
    }, this);

    this.setDBProperty('clientInfo', clients);
    this.set('content.clients', clients);
    console.log("AddHostController.saveClients: saved list ", clients);
  },

  /**
   *  Apply config groups from step4 Configurations
   */
  applyConfigGroup: function () {
    var serviceConfigGroups = this.get('content.serviceConfigGroups');
    serviceConfigGroups.forEach(function (group) {
      if (group.configGroups.someProperty('ConfigGroup.group_name', group.selectedConfigGroup)) {
        var configGroup = group.configGroups.findProperty('ConfigGroup.group_name', group.selectedConfigGroup);
        group.hosts.forEach(function (host) {
          configGroup.ConfigGroup.hosts.push({
            host_name: host
          });
        }, this);
        delete configGroup.href;
        App.ajax.send({
          name: 'config_groups.update_config_group',
          sender: this,
          data: {
            id: configGroup.ConfigGroup.id,
            configGroup: configGroup
          }
        });
      }
    }, this);
  },

  /**
   * Load information about selected config groups
   */
  getServiceConfigGroups: function () {
    var serviceConfigGroups = this.getDBProperty('serviceConfigGroups');
    this.set('content.serviceConfigGroups', serviceConfigGroups);
  },

  /**
   * Save information about selected config groups
   */
  saveServiceConfigGroups: function () {
    this.setDBProperty('serviceConfigGroups', this.get('content.serviceConfigGroups'));
    this.set('content.serviceConfigGroups', this.get('content.serviceConfigGroups'));
  },

  /**
   * Set content.serviceConfigGroups for step4
   */
  loadServiceConfigGroups: function () {
    var selectedServices = [];
    this.loadServiceConfigGroupsBySlaves(selectedServices);
    this.loadServiceConfigGroupsByClients(selectedServices);
    this.sortServiceConfigGroups(selectedServices);
    this.set('content.serviceConfigGroups', selectedServices);
  },
  /**
   * sort config groups by name
   * @param selectedServices
   */
  sortServiceConfigGroups: function (selectedServices) {
    selectedServices.forEach(function (selectedService) {
      selectedService.configGroups.sort(function (cfgA, cfgB) {
        if (cfgA.ConfigGroup.group_name < cfgB.ConfigGroup.group_name) return -1;
        if (cfgA.ConfigGroup.group_name > cfgB.ConfigGroup.group_name) return 1;
        return 0;
      });
    });
  },
  /**
   * load service config groups by slave components,
   * push them into selectedServices
   * @param selectedServices
   */
  loadServiceConfigGroupsBySlaves: function (selectedServices) {
    var componentServiceMap = App.QuickDataMapper.componentServiceMap();
    var slaveComponentHosts = this.get('content.slaveComponentHosts');
    if (slaveComponentHosts && slaveComponentHosts.length > 0) {
      slaveComponentHosts.forEach(function (slave) {
        if (slave.hosts.length > 0) {
          if (slave.componentName !== "CLIENT") {
            var service = componentServiceMap[slave.componentName];
            var configGroups = this.get('content.configGroups').filterProperty('ConfigGroup.tag', service);
            var configGroupsNames = configGroups.mapProperty('ConfigGroup.group_name');
            var defaultGroupName = App.Service.DisplayNames[service] + ' Default';
            configGroupsNames.unshift(defaultGroupName);
            selectedServices.push({
              serviceId: service,
              displayName: App.Service.DisplayNames[service],
              hosts: slave.hosts.mapProperty('hostName'),
              configGroupsNames: configGroupsNames,
              configGroups: configGroups,
              selectedConfigGroup: defaultGroupName
            });
          }
        }
      }, this);
      return true;
    }
    return false;
  },
  /**
   * load service config groups by clients,
   * push them into selectedServices
   * @param selectedServices
   */
  loadServiceConfigGroupsByClients: function (selectedServices) {
    var componentServiceMap = App.QuickDataMapper.componentServiceMap();
    var slaveComponentHosts = this.get('content.slaveComponentHosts');
    var clients = this.get('content.clients');
    var client = slaveComponentHosts && slaveComponentHosts.findProperty('componentName', 'CLIENT');
    var selectedClientHosts = client && client.hosts.mapProperty('hostName');
    if (clients && selectedClientHosts && clients.length > 0 && selectedClientHosts.length > 0) {
      this.loadClients();
      clients.forEach(function (client) {
        var service = componentServiceMap[client.component_name];
        var serviceMatch = selectedServices.findProperty('serviceId', service);
        if (serviceMatch) {
          serviceMatch.hosts = serviceMatch.hosts.concat(selectedClientHosts).uniq();
        } else {
          var configGroups = this.get('content.configGroups').filterProperty('ConfigGroup.tag', service);
          var configGroupsNames = configGroups.mapProperty('ConfigGroup.group_name').sort();
          var defaultGroupName = App.Service.DisplayNames[service] + ' Default';
          configGroupsNames.unshift(defaultGroupName);
          selectedServices.push({
            serviceId: service,
            displayName: App.Service.DisplayNames[service],
            hosts: selectedClientHosts,
            configGroupsNames: configGroupsNames,
            configGroups: configGroups,
            selectedConfigGroup: defaultGroupName
          });
        }
      }, this);
      return true;
    }
    return false;
  },

  loadServiceConfigProperties: function () {
    var serviceConfigProperties = App.db.get('AddService', 'serviceConfigProperties');
    if (!serviceConfigProperties || !serviceConfigProperties.length) {
      serviceConfigProperties = App.db.get('Installer', 'serviceConfigProperties');
    }
    this.set('content.serviceConfigProperties', serviceConfigProperties);
    console.log("AddHostController.loadServiceConfigProperties: loaded config ", serviceConfigProperties);
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
        this.loadServiceConfigProperties();
        this.getServiceConfigGroups();
      case '4':
      case '3':
        this.loadClients();
        this.loadServices();
        this.loadMasterComponentHosts();
        this.loadSlaveComponentHosts();
        this.load('hosts',true);
      case '2':
        this.loadServices();
      case '1':
        this.load('hosts',true);
        this.load('installOptions');
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
    App.router.get('updateController').updateAll();
    App.updater.immediateRun('updateHost');
  },

  /**
   * send request to server in order to install services
   * @param isRetry
   */
  installServices: function (isRetry) {
    this.set('content.cluster.oldRequestsId', []);
    var clusterName = this.get('content.cluster.name');
    var hostNames = [];
    for (var hostname in this.getDBProperty('hosts')) {
      if(this.getDBProperty('hosts')[hostname].isInstalled == false){
        hostNames.push(hostname);
      }
    }
    if(!clusterName || hostNames.length === 0) return false;

    var name = 'wizard.install_services.add_host_controller.';
    name += (isRetry) ? 'is_retry' : 'not_is_retry';

    var data = JSON.stringify({
      "RequestInfo": {
        "context": Em.I18n.t('requestInfo.installComponents'),
        "query": "HostRoles/host_name.in(" + hostNames.join(',') + ")"
      },
      "Body": {
        "HostRoles": {"state": "INSTALLED"}
      }
    });
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
    return true;
  }
});
