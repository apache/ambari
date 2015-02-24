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
App.AddServiceController = App.WizardController.extend(App.AddSecurityConfigs, {

  name: 'addServiceController',
  // @TODO: remove after Kerberos Automation supports
  totalSteps: App.supports.automatedKerberos ? 8 : 7,

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,

  /**
   * @type {object}
   * @default null
   */
  serviceToInstall: null,

  /**
   *
   */
  installClientQueueLength: 0,

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
    controllerName: 'addServiceController',
    configGroups: [],
    clients: [],
    additionalClients: [],
    smokeuser: "ambari-qa",
    group: "hadoop"
  }),

  loadMap: {
    '1': [
      {
        type: 'sync',
        callback: function () {
          this.loadServices();
        }
      }
    ],
    '2': [
      {
        type: 'async',
        callback: function () {
          var dfd = $.Deferred();
          var self = this;
          this.loadHosts().done(function () {
            self.loadMasterComponentHosts();
            self.load('hosts');
            dfd.resolve();
          });
          return dfd.promise();
        }
      }
    ],
    '3': [
      {
        type: 'async',
        callback: function () {
          var dfd = $.Deferred();
          var self = this;
          this.loadHosts().done(function () {
            self.loadServices();
            self.loadClients();
            self.loadSlaveComponentHosts();//depends on loadServices
            dfd.resolve();
          });
          return dfd.promise();
        }
      }
    ],
    '4': [
      {
        type: 'async',
        callback: function () {
          var self = this;
          var dfd = $.Deferred();
          this.loadKerberosDescriptorConfigs().done(function() {
            self.loadServiceConfigGroups();
            self.loadServiceConfigProperties();
            dfd.resolve();
          });
          return dfd.promise();
        }
      }
    ],
    '5': [
      {
        type: 'sync',
        callback: function () {
          this.checkSecurityStatus();
          this.load('cluster');
          this.set('content.additionalClients', []);
          this.set('installClientQueueLength', 0);
          this.set('installClietsQueue', App.ajaxQueue.create({abortOnError: false}));
        }
      }
    ]
  },

  setCurrentStep: function (currentStep, completed) {
    this._super(currentStep, completed);
    App.clusterStatus.setClusterStatus({
      wizardControllerName: this.get('name'),
      localdb: App.db.data
    });
  },

  loadHosts: function () {
    var dfd;
    if (this.getDBProperty('hosts')) {
      dfd = $.Deferred();
      dfd.resolve();
    } else {
      dfd = App.ajax.send({
        name: 'hosts.confirmed',
        sender: this,
        data: {},
        success: 'loadHostsSuccessCallback',
        error: 'loadHostsErrorCallback'
      });
    }
    return dfd.promise();
  },

  loadHostsSuccessCallback: function (response) {
    var installedHosts = {};

    response.items.forEach(function (item, indx) {
      installedHosts[item.Hosts.host_name] = {
        name: item.Hosts.host_name,
        cpu: item.Hosts.cpu_count,
        memory: item.Hosts.total_mem,
        disk_info: item.Hosts.disk_info,
        osType: item.Hosts.os_type,
        osArch: item.Hosts.os_arch,
        ip: item.Hosts.ip,
        bootStatus: "REGISTERED",
        isInstalled: true,
        hostComponents: item.host_components,
        id: indx++
      };
    });
    this.setDBProperty('hosts', installedHosts);
    this.set('content.hosts', installedHosts);
  },

  loadHostsErrorCallback: function (jqXHR, ajaxOptions, error, opt) {
    App.ajax.defaultErrorHandler(jqXHR, opt.url, opt.method, jqXHR.status);
    console.log('Loading hosts failed');
  },

  /**
   * Load services data. Will be used at <code>Select services(step4)</code> step
   */
  loadServices: function () {
    var services = this.getDBProperty('services');
    if (!services) {
      services = {
        selectedServices: [],
        installedServices: []
      };
      App.StackService.find().forEach(function (item) {
        var isInstalled = App.Service.find().someProperty('id', item.get('serviceName'));
        var isSelected = item.get('serviceName') == this.get('serviceToInstall');
        item.set('isSelected', isInstalled || isSelected);
        item.set('isInstalled', isInstalled);
        if (isInstalled) {
          services.selectedServices.push(item.get('serviceName'));
          services.installedServices.push(item.get('serviceName'));
        } else if(isSelected) {
          services.selectedServices.push(item.get('serviceName'));
        }
      }, this);
      this.setDBProperty('services', services);
    } else {
      App.StackService.find().forEach(function (item) {
        var isSelected = services.selectedServices.contains(item.get('serviceName')) || item.get('serviceName') == this.get('serviceToInstall');
        var isInstalled = services.installedServices.contains(item.get('serviceName'));
        item.set('isSelected', isSelected || (this.get("currentStep") == "1" ? isInstalled : isSelected));
        item.set('isInstalled', isInstalled);
      }, this);
      var isServiceWithSlave = App.StackService.find().filterProperty('isSelected').filterProperty('hasSlave').filterProperty('isInstalled', false).length;
      var isServiceWithClient = App.StackService.find().filterProperty('isSelected').filterProperty('hasClient').filterProperty('isInstalled', false).length;
      var isServiceWithCustomAssignedNonMasters = App.StackService.find().filterProperty('isSelected').filterProperty('hasNonMastersWithCustomAssignment').filterProperty('isInstalled', false).length;
      this.set('content.skipSlavesStep', !isServiceWithSlave && !isServiceWithClient || !isServiceWithCustomAssignedNonMasters);
      if (this.get('content.skipSlavesStep')) {
        this.get('isStepDisabled').findProperty('step', 3).set('value', this.get('content.skipSlavesStep'));
      }
    }
    this.set('content.services', App.StackService.find());
  },

  /**
   * Save data to model
   * @param stepController App.WizardStep4Controller
   */
  saveServices: function (stepController) {
    var services = {
      selectedServices: [],
      installedServices: []
    };
    var selectedServices = stepController.get('content').filterProperty('isSelected', true).filterProperty('isInstalled', false).mapProperty('serviceName');
    services.selectedServices.pushObjects(selectedServices);
    services.installedServices.pushObjects(stepController.get('content').filterProperty('isInstalled', true).mapProperty('serviceName'));
    // save services that already installed but ignored on choose services page
    // these services marked by `isInstallable` flag with value `false`, for example `Kerberos` service
    services.installedServices.pushObjects(App.Service.find().mapProperty('serviceName').filter(function(serviceName) {
      return !services.installedServices.contains(serviceName);
    }));
    this.setDBProperty('services', services);
    console.log('AddServiceController.saveServices: saved data', stepController.get('content'));

    this.set('content.selectedServiceNames', selectedServices);
    this.setDBProperty('selectedServiceNames', selectedServices);
    var isServiceWithSlave = stepController.get('content').filterProperty('isSelected').filterProperty('hasSlave').filterProperty('isInstalled', false).mapProperty('serviceName').length;
    var isServiceWithClient = App.StackService.find().filterProperty('isSelected').filterProperty('hasClient').filterProperty('isInstalled', false).mapProperty('serviceName').length;
    this.set('content.skipSlavesStep', !isServiceWithSlave && !isServiceWithClient);
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
      var installedComponent = installedComponents.findProperty('componentName', _component.component_name);
      masterComponentHosts.push({
        display_name: _component.display_name,
        component: _component.component_name,
        hostName: _component.selectedHost,
        serviceId: _component.serviceId,
        isInstalled: !!installedComponent,
        workStatus: installedComponent && installedComponent.get('workStatus')
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
    this._super();
    this.set('content.skipMasterStep', App.StackService.find().filterProperty('isSelected').filterProperty('hasMaster').everyProperty('isInstalled', true));
    this.get('isStepDisabled').findProperty('step', 2).set('value', this.get('content.skipMasterStep'));
  },

  /**
   * Does service have any configs
   * @param {string} serviceName
   * @returns {boolean}
   */
  isServiceNotConfigurable: function (serviceName) {
    return App.get('services.noConfigTypes').contains(serviceName);
  },

  /**
   * Should Config Step be skipped (based on selected services list)
   * @returns {boolean}
   */
  skipConfigStep: function () {
    var skipConfigStep = true;
    var selectedServices = this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false).mapProperty('serviceName');
    selectedServices.map(function (serviceName) {
      skipConfigStep = skipConfigStep && this.isServiceNotConfigurable(serviceName);
    }, this);
    return skipConfigStep;
  },

  loadServiceConfigProperties: function () {
    this._super();
    if (!this.get('content.services')) {
      this.loadServices();
    }
    if (this.get('currentStep') > 1 && this.get('currentStep') < 6) {
      this.set('content.skipConfigStep', this.skipConfigStep());
      this.get('isStepDisabled').findProperty('step', 4).set('value', this.get('content.skipConfigStep'));
    }
  },

  /**
   * Load kerberos descriptor configuration
   * @returns {$.Deferred}
   */
  loadKerberosDescriptorConfigs: function() {
    var self = this,
        dfd = $.Deferred();
    if (App.router.get('mainAdminKerberosController.securityEnabled')) {
      this.getDescriptorConfigs().then(function(properties) {
        self.set('kerberosDescriptorConfigs', properties);
        dfd.resolve();
      });
    } else {
      dfd.resolve();
    }
    return dfd.promise();
  },

  saveServiceConfigProperties: function (stepController) {
    this._super(stepController);
    if (this.get('currentStep') > 1 && this.get('currentStep') < 6) {
      this.set('content.skipConfigStep', this.skipConfigStep());
      this.get('isStepDisabled').findProperty('step', 4).set('value', this.get('content.skipConfigStep'));
    }
  },

  /**
   * Load master component hosts data for using in required step controllers
   */
  loadSlaveComponentHosts: function () {
    var slaveComponentHosts = this.getDBProperty('slaveComponentHosts'),
      hosts = this.getDBProperty('hosts'),
      host_names = Em.keys(hosts);
    if (!Em.isNone(slaveComponentHosts)) {
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
    if (!slaveComponentHosts) {
      slaveComponentHosts = this.getSlaveComponentHosts();
    }
    this.set("content.slaveComponentHosts", slaveComponentHosts);
    console.log("AddServiceController.loadSlaveComponentHosts: loaded hosts ", slaveComponentHosts);
  },

  /**
   * return slaveComponents bound to hosts
   * @return {Array}
   */
  getSlaveComponentHosts: function () {
    var components = this.get('slaveComponents');
    var result = [];
    var installedServices = App.Service.find().mapProperty('serviceName');
    var selectedServices = this.get('content.services').filterProperty('isSelected', true).mapProperty('serviceName');
    var installedComponentsMap = {};
    var uninstalledComponents = [];
    var hosts = this.getDBProperty('hosts') || this.get('content.hosts');
    var masterComponents = App.get('components.masters');
    var nonMasterComponentHosts = [];

    components.forEach(function (component) {
      if (installedServices.contains(component.get('serviceName'))) {
        installedComponentsMap[component.get('componentName')] = [];
      } else if (selectedServices.contains(component.get('serviceName'))) {
        uninstalledComponents.push(component);
      }
    }, this);

    for (var hostName in hosts) {
      if (hosts[hostName].isInstalled) {
        var isMasterComponentHosted = false;
        hosts[hostName].hostComponents.forEach(function (component) {
          if (installedComponentsMap[component.HostRoles.component_name]) {
            installedComponentsMap[component.HostRoles.component_name].push(hostName);
          }
          if (masterComponents.contains(component.HostRoles.component_name)) {
            isMasterComponentHosted = true;
          }
        }, this);
        if (!isMasterComponentHosted) {
          nonMasterComponentHosts.push(hostName);
        }
      }
    }

    for (var componentName in installedComponentsMap) {
      var component = {
        componentName: componentName,
        displayName: App.format.role(componentName),
        hosts: [],
        isInstalled: true
      };
      installedComponentsMap[componentName].forEach(function (hostName) {
        component.hosts.push({
          group: "Default",
          hostName: hostName,
          isInstalled: true
        });
      }, this);
      result.push(component);
    }

    if (!nonMasterComponentHosts.length) {
      nonMasterComponentHosts.push(Object.keys(hosts)[0]);
    }
    var uninstalledComponentHosts =  nonMasterComponentHosts.map(function(_hostName){
      return {
        group: "Default",
        hostName: _hostName,
        isInstalled: false
      }
    });
    uninstalledComponents.forEach(function (component) {
      result.push({
        componentName: component.get('componentName'),
        displayName: App.format.role(component.get('componentName')),
        hosts: uninstalledComponentHosts,
        isInstalled: false
      })
    });

    return result;
  },

  /**
   * Generate clients list for selected services and save it to model
   * @param stepController step4WizardController
   */
  saveClients: function (stepController) {
    var clients = [];
    var serviceComponents = App.StackServiceComponent.find();
    this.get('content.services').filterProperty('isSelected').filterProperty('isInstalled',false).forEach(function (_service) {
      var serviceClients = serviceComponents.filterProperty('serviceName', _service.get('serviceName')).filterProperty('isClient');
      serviceClients.forEach(function (client) {
        clients.push({
          component_name: client.get('componentName'),
          display_name: client.get('displayName'),
          isInstalled: false
        });
      }, this);
    }, this);

    this.setDBProperty('clientInfo', clients);
    this.set('content.clients', clients);
    console.log("AddServiceController.saveClients: saved list ", clients);
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
    this.clearAllSteps();
    this.clearStorageData();
    this.resetDbNamespace();
    App.router.get('updateController').updateAll();
  },

  /**
   * genarates data for ajax request to launch install services
   * @method generateDataForInstallServices
   * @param {Array} selectedServices
   * @returns {{context: *, ServiceInfo: {state: string}, urlParams: string}}
   */
  generateDataForInstallServices: function(selectedServices) {
    if (selectedServices.contains('OOZIE')) {
      selectedServices = selectedServices.concat(['HDFS', 'YARN', 'MAPREDUCE2']);
    }
    return {
      "context": Em.I18n.t('requestInfo.installServices'),
      "ServiceInfo": {"state": "INSTALLED"},
      "urlParams": "ServiceInfo/service_name.in(" + selectedServices.join(',')  + ")"
    };
  },

  /**
   * run this method after success/error callbacks
   * for <code>installServicesRequest<code>
   */
  installServicesComplete: function () {
    App.get('router.wizardStep8Controller').set('servicesInstalled', true);
    this.setInfoForStep9();
    this.saveClusterState('ADD_SERVICES_INSTALLING_3');
    App.router.transitionTo('step7');
  },

  /**
   * main method for installinf clients
   * @method installServices
   */
  installServices: function () {
    var self = this;
    this.set('content.cluster.oldRequestsId', []);
    this.installAdditionalClients().done(function () {
      self.installSelectedServices();
    });
  },

  installSelectedServices: function () {
    var name = 'common.services.update';
    var selectedServices = this.get('content.services').filterProperty('isInstalled', false).filterProperty('isSelected', true).mapProperty('serviceName');
    var data = this.generateDataForInstallServices(selectedServices);
    this.installServicesRequest(name, data, this.installServicesComplete.bind(this));
  },

  installServicesRequest: function (name, data, callback) {
    callback = callback || Em.K;
    App.ajax.send({
      name: name,
      sender: this,
      data: data,
      success: 'installServicesSuccessCallback',
      error: 'installServicesErrorCallback'
    }).then(callback, callback);
  },

  /**
   * installs clients before install new services
   * on host where some components require this
   * @method installAdditionalClients
   */
  installAdditionalClients: function () {
    var dfd = $.Deferred();
    if (this.get('content.additionalClients.length') > 0) {
      this.get('content.additionalClients').forEach(function (c, k) {
        if (c.hostNames.length > 0) {
          var queryStr = 'HostRoles/component_name='+ c.componentName + '&HostRoles/host_name.in(' + c.hostNames.join() + ')';
          this.get('installClietsQueue').addRequest({
            name: 'common.host_component.update',
            sender: this,
            data: {
              query: queryStr,
              context: 'Install ' + App.format.role(c.componentName),
              HostRoles: {
                state: 'INSTALLED'
              },
              counter: k,
              deferred: dfd
            },
            success: 'installClientSuccess',
            error: 'installClientError'
          });
        }
      }, this);
      if (this.get('installClietsQueue.queue.length') == 0) {
        return dfd.resolve();
      } else {
        this.set('installClientQueueLength', this.get('installClietsQueue.queue.length'));
        App.get('router.wizardStep8Controller').set('servicesInstalled', true);
        this.get('installClietsQueue').start();
      }
    } else {
      dfd.resolve();
    }
    return dfd.promise();
  },

  /**
   * callback for when install clients success
   * @param data
   * @param opt
   * @param params
   * @method installClientComplete
   */
  installClientSuccess: function(data, opt, params) {
    if (this.get('installClientQueueLength') - 1 == params.counter) {
      params.deferred.resolve();
    }
  },

  /**
   * callback for when install clients fail
   * @param request
   * @param ajaxOptions
   * @param error
   * @param opt
   * @param params
   */
  installClientError: function(request, ajaxOptions, error, opt, params) {
    if (this.get('installClientQueueLength') - 1 == params.counter) {
      params.deferred.resolve();
    }
  },

  checkSecurityStatus: function() {
    if (App.supports.automatedKerberos) {
      if (!App.router.get('mainAdminKerberosController.securityEnabled')) {
        this.set('skipConfigureIdentitiesStep', true);
        this.get('isStepDisabled').findProperty('step', 5).set('value', true);
      }
    }
  }

});
