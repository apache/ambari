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

App.WizardController = Em.Controller.extend({

  isStepDisabled: null,

  init: function () {
    this.set('isStepDisabled', []);
    this.clusters = App.Cluster.find();
    this.get('isStepDisabled').pushObject(Ember.Object.create({
      step: 1,
      value: false
    }));
    for (var i = 2; i <= this.get('totalSteps'); i++) {
      this.get('isStepDisabled').pushObject(Ember.Object.create({
        step: i,
        value: true
      }));
    }
  },

  slaveComponents: function () {
    return require('data/service_components').filter(function (component) {
      return !(component.isClient || component.isMaster ||
        ["GANGLIA_MONITOR", "DASHBOARD", "MYSQL_SERVER"].contains(component.component_name));
    });
  }.property(),

  dbNamespace: function(){
    return this.get('name').capitalize().replace('Controller', "");
  }.property('name'),
  /**
   * get property from local storage
   * @param key
   * @return {*}
   */
  getDBProperty: function(key){
    return App.db.get(this.get('dbNamespace'), key);
  },
  /**
   * set property to local storage
   * @param key
   * @param value
   */
  setDBProperty: function(key, value){
    App.db.set(this.get('dbNamespace'), key, value);
  },

  setStepsEnable: function () {
    for (var i = 1; i <= this.totalSteps; i++) {
      var step = this.get('isStepDisabled').findProperty('step', i);
      if (i <= this.get('currentStep')) {
        step.set('value', false);
      } else {
        step.set('value', true);
      }
    }
  }.observes('currentStep'),

  setLowerStepsDisable: function (stepNo) {
    for (var i = 1; i < stepNo; i++) {
      var step = this.get('isStepDisabled').findProperty('step', i);
      step.set('value', true);
    }
  },

  /**
   * Set current step to new value.
   * Method moved from App.router.setInstallerCurrentStep
   * @param currentStep
   * @param completed
   */
  currentStep: function () {
    return App.get('router').getWizardCurrentStep(this.get('name').substr(0, this.get('name').length - 10));
  }.property(),

  /**
   * Set current step to new value.
   * Method moved from App.router.setInstallerCurrentStep
   * @param currentStep
   * @param completed
   */
  setCurrentStep: function (currentStep, completed) {
    App.db.setWizardCurrentStep(this.get('name').substr(0, this.get('name').length - 10), currentStep, completed);
    this.set('currentStep', currentStep);
  },

  clusters: null,

  isStep0: function () {
    return this.get('currentStep') == 0;
  }.property('currentStep'),

  isStep1: function () {
    return this.get('currentStep') == 1;
  }.property('currentStep'),

  isStep2: function () {
    return this.get('currentStep') == 2;
  }.property('currentStep'),

  isStep3: function () {
    return this.get('currentStep') == 3;
  }.property('currentStep'),

  isStep4: function () {
    return this.get('currentStep') == 4;
  }.property('currentStep'),

  isStep5: function () {
    return this.get('currentStep') == 5;
  }.property('currentStep'),

  isStep6: function () {
    return this.get('currentStep') == 6;
  }.property('currentStep'),

  isStep7: function () {
    return this.get('currentStep') == 7;
  }.property('currentStep'),

  isStep8: function () {
    return this.get('currentStep') == 8;
  }.property('currentStep'),

  isStep9: function () {
    return this.get('currentStep') == 9;
  }.property('currentStep'),

  isStep10: function () {
    return this.get('currentStep') == 10;
  }.property('currentStep'),

  gotoStep: function (step) {
    if (this.get('isStepDisabled').findProperty('step', step).get('value') !== false) {
      return false;
    }
    // if going back from Step 9 in Install Wizard, delete the checkpoint so that the user is not redirected
    // to Step 9
    if (this.get('content.controllerName') == 'installerController' && this.get('currentStep') === '9' && step < 9) {
      App.clusterStatus.setClusterStatus({
        clusterName: this.get('clusterName'),
        clusterState: 'CLUSTER_NOT_CREATED_1',
        wizardControllerName: 'installerController',
        localdb: App.db.data
      });
    }
    if ((this.get('currentStep') - step) > 1) {
      App.ModalPopup.show({
        header: Em.I18n.t('installer.navigation.warning.header'),
        onPrimary: function () {
          App.router.send('gotoStep' + step);
          this.hide();
        },
        body: "If you proceed to go back to Step " + step + ", you will lose any changes you have made beyond this step"
      });
    } else {
      App.router.send('gotoStep' + step);
    }
    return true;
  },

  gotoStep0: function () {
    this.gotoStep(0);
  },

  gotoStep1: function () {
    this.gotoStep(1);
  },

  gotoStep2: function () {
    this.gotoStep(2);
  },

  gotoStep3: function () {
    this.gotoStep(3);
  },

  gotoStep4: function () {
    this.gotoStep(4);
  },

  gotoStep5: function () {
    this.gotoStep(5);
  },

  gotoStep6: function () {
    this.gotoStep(6);
  },

  gotoStep7: function () {
    this.gotoStep(7);
  },

  gotoStep8: function () {
    this.gotoStep(8);
  },

  gotoStep9: function () {
    this.gotoStep(9);
  },

  gotoStep10: function () {
    this.gotoStep(10);
  },

  /**
   * Initialize host status info for step9
   */
  setInfoForStep9: function () {

    var hostInfo = this.getDBProperty('hosts');
    for (var index in hostInfo) {
      hostInfo[index].status = "pending";
      hostInfo[index].message = 'Waiting';
      hostInfo[index].logTasks = [];
      hostInfo[index].tasks = [];
      hostInfo[index].progress = '0';
    }
    this.setDBProperty('hosts', hostInfo);
  },

  /**
   * Remove all data for installOptions step
   */
  clearInstallOptions: function () {
    var installOptions = jQuery.extend({}, this.get('installOptionsTemplate'));
    this.set('content.installOptions', installOptions);
    this.setDBProperty('installOptions', installOptions);
    this.set('content.hosts', []);
    this.setDBProperty('hosts', []);
  },

  toObject: function (object) {
    var result = {};
    for (var i in object) {
      if (object.hasOwnProperty(i)) {
        result[i] = object[i];
      }
    }
    return result;
  },

  /**
   * save status of the cluster. This is called from step8 and step9 to persist install and start requestId
   * @param clusterStatus object with status, isCompleted, requestId, isInstallError and isStartError field.
   */
  saveClusterStatus: function (clusterStatus) {
    var oldStatus = this.toObject(this.get('content.cluster'));
    clusterStatus = jQuery.extend(oldStatus, clusterStatus);
    if (clusterStatus.requestId &&
      clusterStatus.oldRequestsId.indexOf(clusterStatus.requestId) === -1) {
      clusterStatus.oldRequestsId.push(clusterStatus.requestId);
    }
    this.set('content.cluster', clusterStatus);
    this.setDBProperty('cluster', clusterStatus);
  },

  /**
   * Invoke installation of selected services to the server and saves the request id returned by the server.
   * @param isRetry
   */
  installServices: function (isRetry) {

    // clear requests since we are installing services
    // and we don't want to get tasks for previous install attempts
    this.set('content.cluster.oldRequestsId', []);
    var clusterName = this.get('content.cluster.name');
    var data;
    var name;
    if (isRetry) {
      name = 'wizard.install_services.installer_controller.is_retry';
      data = '{"RequestInfo": {"context" :"' + Em.I18n.t('requestInfo.installComponents') + '"}, "Body": {"HostRoles": {"state": "INSTALLED"}}}';
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

  installServicesSuccessCallback: function (jsonData) {
    var installStartTime = App.dateTime();
    console.log("TRACE: In success function for the installService call");
    if (jsonData) {
      var requestId = jsonData.Requests.id;
      console.log('requestId is: ' + requestId);
      var clusterStatus = {
        status: 'PENDING',
        requestId: requestId,
        isInstallError: false,
        isCompleted: false,
        installStartTime: installStartTime
      };
      this.saveClusterStatus(clusterStatus);
    } else {
      console.log('ERROR: Error occurred in parsing JSON data');
    }
  },

  installServicesErrorCallback: function (request, ajaxOptions, error) {
    console.log("TRACE: In error function for the installService call");
    console.log("TRACE: error code status is: " + request.status);
    console.log('Error message is: ' + request.responseText);
    var clusterStatus = {
      status: 'PENDING',
      requestId: this.get('content.cluster.requestId'),
      isInstallError: true,
      isCompleted: false
    };
    this.saveClusterStatus(clusterStatus);
    App.showAlertPopup(Em.I18n.t('common.errorPopup.header'), request.responseText);
  },

  bootstrapRequestId: null,

  /*
   Bootstrap selected hosts.
   */
  launchBootstrap: function (bootStrapData) {
    App.ajax.send({
      name: 'wizard.launch_bootstrap',
      sender: this,
      data: {
        bootStrapData: bootStrapData
      },
      success: 'launchBootstrapSuccessCallback',
      error: 'launchBootstrapErrorCallback'
    });

    return this.get('bootstrapRequestId');
  },

  launchBootstrapSuccessCallback: function (data) {
    console.log("TRACE: POST bootstrap succeeded");
    this.set('bootstrapRequestId', data.requestId);
  },

  launchBootstrapErrorCallback: function () {
    console.log("ERROR: POST bootstrap failed");
    alert('Bootstrap call failed. Please try again.');
  },

  /**
   * Load <code>content.<name></code> variable from localStorage, if wasn't loaded before.
   * If you specify <code>reload</code> to true - it will reload it.
   * @param name
   * @param reload
   * @return {Boolean}
   */
  load: function (name, reload) {
    if (this.get('content.' + name) && !reload) {
      return false;
    }
    var result = this.getDBProperty(name);
    if (!result) {
      if (this['get' + name.capitalize()]) {
        result = this['get' + name.capitalize()]();
        this.setDBProperty(name, result);
        console.log(this.get('name') + ": created " + name, result);
      }
      else {
        console.debug('get' + name.capitalize(), ' not defined in the ' + this.get('name'));
      }
    }
    this.set('content.' + name, result);
    console.log(this.get('name') + ": loaded " + name, result);
  },

  save: function (name) {
    var value = this.toObject(this.get('content.' + name));
    this.setDBProperty(name, value);
    console.log(this.get('name') + ": saved " + name, value);
  },

  clear: function () {
    this.set('content', Ember.Object.create({
      'controllerName': this.get('content.controllerName')
    }));
    this.set('currentStep', 0);
    this.clearStorageData();
  },

  clusterStatusTemplate: {
    name: "",
    status: "PENDING",
    isCompleted: false,
    requestId: null,
    installStartTime: null,
    installTime: null,
    isInstallError: false,
    isStartError: false,
    oldRequestsId: []
  },

  clearStorageData: function () {
    this.setDBProperty('service',undefined); //not to use this data at AddService page
    this.setDBProperty('hosts', undefined);
    this.setDBProperty('masterComponentHosts', undefined);
    this.setDBProperty('slaveComponentHosts', undefined);
    this.setDBProperty('cluster', undefined);
    this.setDBProperty('allHostNames', undefined);
    this.setDBProperty('installOptions', undefined);
    this.setDBProperty('allHostNamesPattern', undefined);
  },

  installOptionsTemplate: {
    hostNames: "", //string
    manualInstall: false, //true, false
    useSsh: true, //bool
    javaHome: App.defaultJavaHome, //string
    localRepo: false, //true, false
    sshKey: "", //string
    bootRequestId: null, //string
    sshUser: "root" //string
  },

  loadedServiceComponents: null,

  /**
   * Generate serviceComponents as pr the stack definition  and save it to localdata
   * called form stepController step4WizardController
   */
  loadServiceComponents: function () {
    App.ajax.send({
      name: 'wizard.service_components',
      sender: this,
      data: {
        stackUrl: App.get('stack2VersionURL'),
        stackVersion: App.get('currentStackVersionNumber')
      },
      success: 'loadServiceComponentsSuccessCallback',
      error: 'loadServiceComponentsErrorCallback'
    });
    return this.get('loadedServiceComponents');
  },

  loadServiceComponentsSuccessCallback: function (jsonData) {
    var displayOrderConfig = require('data/services');
    console.log("TRACE: getService ajax call  -> In success function for the getServiceComponents call");
    console.log("TRACE: jsonData.services : " + jsonData.items);

    // Creating Model
    var Service = Ember.Object.extend({
      serviceName: null,
      displayName: null,
      isDisabled: true,
      isSelected: true,
      isInstalled: false,
      description: null,
      version: null
    });

    var data = [];
    // loop through all the service components
    for (var i = 0; i < displayOrderConfig.length; i++) {
      var entry = jsonData.items.findProperty("StackServices.service_name", displayOrderConfig[i].serviceName);
      if (entry) {
        var myService = Service.create({
          serviceName: entry.StackServices.service_name,
          displayName: displayOrderConfig[i].displayName,
          isDisabled: displayOrderConfig[i].isDisabled,
          isSelected: displayOrderConfig[i].isSelected,
          canBeSelected: displayOrderConfig[i].canBeSelected,
          isInstalled: false,
          isHidden: displayOrderConfig[i].isHidden,
          description: entry.StackServices.comments,
          version: entry.StackServices.service_version
        });

        data.push(myService);
      }
      else {
        console.warn('Service not found - ', displayOrderConfig[i].serviceName);
      }
    }

    this.set('loadedServiceComponents', data);
    console.log('TRACE: service components: ' + JSON.stringify(data));

  },

  loadServiceComponentsErrorCallback: function (request, ajaxOptions, error) {
    console.log("TRACE: STep5 -> In error function for the getServiceComponents call");
    console.log("TRACE: STep5 -> error code status is: " + request.status);
    console.log('Step8: Error message is: ' + request.responseText);
  },

  loadServicesFromServer: function () {
    var services = this.getDBProperty('service');
    var apiService = this.loadServiceComponents();
    this.set('content.services', apiService);
    this.setDBProperty('service',apiService);
  },
  /**
   * Load config groups from local DB
   */
  loadServiceConfigGroups: function () {
    var serviceConfigGroups = this.getDBProperty('serviceConfigGroups') || [];
    this.set('content.configGroups', serviceConfigGroups);
    console.log("InstallerController.configGroups: loaded config ", serviceConfigGroups);
  },

  registerErrPopup: function (header, message) {
    App.ModalPopup.show({
      header: header,
      secondary: false,
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile('<p>{{view.message}}</p>'),
        message: message
      })
    });
  },

  /**
   * Save hosts that the user confirmed to proceed with from step 3
   * @param stepController App.WizardStep3Controller
   */
  saveConfirmedHosts: function (stepController) {
    var hostInfo = {};
    stepController.get('content.hosts').forEach(function (_host) {
      if (_host.bootStatus == 'REGISTERED') {
        hostInfo[_host.name] = {
          name: _host.name,
          cpu: _host.cpu,
          memory: _host.memory,
          disk_info: _host.disk_info,
          os_type: _host.os_type,
          os_arch: _host.os_arch,
          ip: _host.ip,
          bootStatus: _host.bootStatus,
          isInstalled: false
        };
      }
    });
    console.log('wizardController:saveConfirmedHosts: save hosts ', hostInfo);
    this.setDBProperty('hosts', hostInfo);
    this.set('content.hosts', hostInfo);
  },

  /**
   * Save data after installation to main controller
   * @param stepController App.WizardStep9Controller
   */
  saveInstalledHosts: function (stepController) {
    var hosts = stepController.get('hosts');
    var hostInfo = this.getDBProperty('hosts');

    for (var index in hostInfo) {
      hostInfo[index].status = "pending";
      var host = hosts.findProperty('name', hostInfo[index].name);
      if (host) {
        hostInfo[index].status = host.status;
        hostInfo[index].message = host.message;
        hostInfo[index].progress = host.progress;
      }
    }
    this.set('content.hosts', hostInfo);
    this.setDBProperty('hosts', hostInfo);
    console.log('wizardController:saveInstalledHosts: save hosts ', hostInfo);
  },

  /**
   * Save slaveHostComponents to main controller
   * @param stepController
   */
  saveSlaveComponentHosts: function (stepController) {

    var hosts = stepController.get('hosts');
    var headers = stepController.get('headers');

    var formattedHosts = Ember.Object.create();
    headers.forEach(function (header) {
      formattedHosts.set(header.get('name'), []);
    });

    hosts.forEach(function (host) {

      var checkboxes = host.get('checkboxes');
      headers.forEach(function (header) {
        var cb = checkboxes.findProperty('title', header.get('label'));
        if (cb.get('checked')) {
          formattedHosts.get(header.get('name')).push({
            hostName: host.hostName,
            group: 'Default',
            isInstalled: cb.get('isInstalled')
          });
        }
      });
    });

    var slaveComponentHosts = [];

    headers.forEach(function (header) {
      slaveComponentHosts.push({
        componentName: header.get('name'),
        displayName: header.get('label').replace(/\s/g, ''),
        hosts: formattedHosts.get(header.get('name'))
      });
    });

    this.setDBProperty('slaveComponentHosts', slaveComponentHosts);
    console.log('wizardController.slaveComponentHosts: saved hosts', slaveComponentHosts);
    this.set('content.slaveComponentHosts', slaveComponentHosts);
  },

  /**
   * Return true if cluster data is loaded and false otherwise.
   * This is used for all wizard controllers except for installer wizard.
   */
  dataLoading: function () {
    var dfd = $.Deferred();
    this.connectOutlet('loading');
    if (App.router.get('clusterController.isLoaded')) {
      dfd.resolve();
    } else {
      var interval = setInterval(function () {
        if (App.router.get('clusterController.isLoaded')) {
          dfd.resolve();
          clearInterval(interval);
        }
      }, 50);
    }
    return dfd.promise();
  },

  /**
   * Return true if user data is loaded via App.MainServiceInfoConfigsController
   * This function is used in reassign master wizard right now.
   */

  usersLoading: function () {
    var self = this;
    var dfd = $.Deferred();
    var miscController = App.MainAdminMiscController.create({content: self.get('content')});
    miscController.loadUsers();
    var interval = setInterval(function () {
      if (miscController.get('dataIsLoaded')) {
        if (self.get("content.hdfsUser")) {
          self.set('content.hdfsUser', miscController.get('content.hdfsUser'));
        }
        dfd.resolve();
        clearInterval(interval);
      }
    }, 10);
    return dfd.promise();
  },

  /**
   * Save cluster status before going to deploy step
   * @param name cluster state. Unique for every wizard
   */
  saveClusterState: function (name) {
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: name,
      wizardControllerName: this.get('content.controllerName'),
      localdb: App.db.data
    });
  },

  /**
   * load advanced configs from server
   */
  loadAdvancedConfigs: function () {
    var configs = (this.getDBProperty('advancedServiceConfig')) ? this.getDBProperty('advancedServiceConfig') : [];
    this.get('content.services').filterProperty('isSelected', true).mapProperty('serviceName').forEach(function (_serviceName) {
      var serviceComponents = App.config.loadAdvancedConfig(_serviceName);
      if (serviceComponents) {
        configs = configs.concat(serviceComponents);
      }
    }, this);
    this.set('content.advancedServiceConfig', configs);
    this.setDBProperty('advancedServiceConfig', configs);
  },
  /**
   * Load serviceConfigProperties to model
   */
  loadServiceConfigProperties: function () {
    var serviceConfigProperties = this.getDBProperty('serviceConfigProperties');
    this.set('content.serviceConfigProperties', serviceConfigProperties);
    console.log("AddHostController.loadServiceConfigProperties: loaded config ", serviceConfigProperties);
  },
  /**
   * Save config properties
   * @param stepController Step7WizardController
   */
  saveServiceConfigProperties: function (stepController) {
    var serviceConfigProperties = [];
    var updateServiceConfigProperties = [];
    stepController.get('stepConfigs').forEach(function (_content) {

      if (_content.serviceName === 'YARN' && !App.supports.capacitySchedulerUi) {
        _content.set('configs', App.config.textareaIntoFileConfigs(_content.get('configs'), 'capacity-scheduler.xml'));
      }

      _content.get('configs').forEach(function (_configProperties) {
        var configProperty = {
          id: _configProperties.get('id'),
          name: _configProperties.get('name'),
          value: _configProperties.get('value'),
          defaultValue: _configProperties.get('defaultValue'),
          description: _configProperties.get('description'),
          serviceName: _configProperties.get('serviceName'),
          domain: _configProperties.get('domain'),
          filename: _configProperties.get('filename'),
          displayType: _configProperties.get('displayType'),
          isRequiredByAgent: _configProperties.get('isRequiredByAgent')
        };
        serviceConfigProperties.push(configProperty);
      }, this);
      // check for configs that need to update for installed services
      if (stepController.get('installedServiceNames') && stepController.get('installedServiceNames').contains(_content.get('serviceName'))) {
        // get only modified configs
        var configs = _content.get('configs').filterProperty('isNotDefaultValue').filter(function(config) {
          var notAllowed = ['masterHost', 'masterHosts', 'slaveHosts', 'slaveHost'];
          return !notAllowed.contains(config.get('displayType'));
        });
        // if modified configs detected push all service's configs for update
        if (configs.length)
          updateServiceConfigProperties = updateServiceConfigProperties.concat(serviceConfigProperties.filterProperty('serviceName',_content.get('serviceName')));
      }
    }, this);
    this.setDBProperty('serviceConfigProperties', serviceConfigProperties);
    this.set('content.serviceConfigProperties', serviceConfigProperties);
    this.setDBProperty('configsToUpdate', updateServiceConfigProperties);
  },
  /**
   * save Config groups
   * @param stepController
   */
  saveServiceConfigGroups: function (stepController) {
    var serviceConfigGroups = [];
    var isForUpdate = false;
    stepController.get('stepConfigs').forEach(function (service) {
      // mark group of installed service
      if (service.get('selected') === false) isForUpdate = true;
      service.get('configGroups').forEach(function (configGroup) {
        var properties = [];
        configGroup.get('properties').forEach(function (property) {
          properties.push({
            isRequiredByAgent: property.get('isRequiredByAgent'),
            name: property.get('name'),
            value: property.get('value'),
            filename: property.get('filename')
          })
        });
        //configGroup copied into plain JS object to avoid Converting circular structure to JSON
        serviceConfigGroups.push({
          id: configGroup.get('id'),
          name: configGroup.get('name'),
          description: configGroup.get('description'),
          hosts: configGroup.get('hosts'),
          properties: properties,
          isDefault: configGroup.get('isDefault'),
          isForUpdate: isForUpdate,
          service: {id: configGroup.get('service.id')}
        });
      }, this)
    }, this);
    this.setDBProperty('serviceConfigGroups', serviceConfigGroups);
    this.set('content.configGroups', serviceConfigGroups);
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

    components.forEach(function (component) {
      if (installedServices.contains(component.service_name)) {
        installedComponentsMap[component.component_name] = [];
      } else if (selectedServices.contains(component.service_name)) {
        uninstalledComponents.push(component);
      }
    }, this);
    installedComponentsMap['HDFS_CLIENT'] = [];

    App.HostComponent.find().forEach(function (hostComponent) {
      if (installedComponentsMap[hostComponent.get('componentName')]) {
        installedComponentsMap[hostComponent.get('componentName')].push(hostComponent.get('host.id'));
      }
    }, this);

    for (var componentName in installedComponentsMap) {
      var name = (componentName === 'HDFS_CLIENT') ? 'CLIENT' : componentName;
      var component = {
        componentName: name,
        displayName: App.format.role(name),
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

    uninstalledComponents.forEach(function (component) {
      var hosts = jQuery.extend(true, [], result.findProperty('componentName', 'DATANODE').hosts);
      hosts.setEach('isInstalled', false);
      result.push({
        componentName: component.component_name,
        displayName: App.format.role(component.component_name),
        hosts: hosts,
        isInstalled: false
      })
    });

    return result;
  }
});
