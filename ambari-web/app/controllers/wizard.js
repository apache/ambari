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
require('models/host');

App.WizardController = Em.Controller.extend(App.LocalStorage, {

  isStepDisabled: null,

  /**
   * map of actions which load data required by which step
   * used by <code>loadAllPriorSteps</code>
   */
  loadMap: {},

  /**
   * Wizard properties in local storage, which should be cleaned right after wizard has been finished
   */
  dbPropertiesToClean: [
    'service',
    'hosts',
    'masterComponentHosts',
    'slaveComponentHosts',
    'cluster',
    'allHostNames',
    'installOptions',
    'allHostNamesPattern',
    'serviceComponents'
  ],

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
    return App.StackServiceComponent.find().filterProperty('isSlave', true);
  }.property('App.router.clusterController.isLoaded'),

  allHosts: function () {
    var dbHosts = this.get('content.hosts');
    var hosts = [];
    var hostComponents = [];

    for (var hostName in dbHosts) {
      hostComponents = [];
      var disksOverallCapacity = 0;
      var diskFree = 0;
      dbHosts[hostName].hostComponents.forEach(function (componentName) {
        hostComponents.push(Em.Object.create({
          componentName: componentName,
          displayName: App.format.role(componentName)
        }));
      });
      dbHosts[hostName].disk_info.forEach(function (disk) {
        disksOverallCapacity += parseFloat(disk.size);
        diskFree += parseFloat(disk.available);
      });

      hosts.push(Em.Object.create({
        id: hostName,
        hostName: hostName,
        publicHostName: hostName,
        diskInfo: dbHosts[hostName].disk_info,
        diskTotal: disksOverallCapacity / (1024 * 1024),
        diskFree: diskFree / (1024 * 1024),
        disksMounted: dbHosts[hostName].disk_info.length,
        cpu: dbHosts[hostName].cpu,
        memory: dbHosts[hostName].memory,
        osType: dbHosts[hostName].osType ? dbHosts[hostName].osType: 0,
        osArch: dbHosts[hostName].osArch ? dbHosts[hostName].osArch : 0,
        ip: dbHosts[hostName].ip ? dbHosts[hostName].ip: 0,
        hostComponents: hostComponents
      }))
    }
    return hosts;
  }.property('content.hosts'),

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

  gotoStep: function (step, disableNaviWarning) {
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
    if ((this.get('currentStep') - step) > 1 && !disableNaviWarning) {
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
    this.set('content.hosts', {});
    this.setDBProperty('hosts', {});
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
  installServices: function (isRetry, callback) {
    // clear requests since we are installing services
    // and we don't want to get tasks for previous install attempts
    this.set('content.cluster.oldRequestsId', []);
    var data;
    callback = callback || Em.K;
    if (isRetry) {
      data = {
        context: Em.I18n.t('requestInfo.installComponents'),
        HostRoles: {"state": "INSTALLED"},
        urlParams: "HostRoles/state=INSTALLED"
      }
    } else {
      data = {
        context: Em.I18n.t('requestInfo.installServices'),
        ServiceInfo: {"state": "INSTALLED"},
        urlParams: "ServiceInfo/state=INIT"
      }
    }

    App.ajax.send({
      name: isRetry ? 'common.host_components.update' : 'common.services.update',
      sender: this,
      data: data,
      success: 'installServicesSuccessCallback',
      error: 'installServicesErrorCallback'
    }).then(callback, callback);
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
  /**
   * show popup, that display status of bootstrap launching
   * @param callback
   * @return {Object}
   */
  showLaunchBootstrapPopup: function (callback) {
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step2.bootStrap.header'),
      isError: false,
      serverError: null,
      bodyClass: Em.View.extend({
        templateName: require('templates/wizard/bootstrap_call_popup')
      }),
      showFooter: false,
      showCloseButton: false,
      secondary: null,
      /**
       * handle requestId when call is completed,
       * if it's correct call callback and hide popup
       * otherwise notify error and enable buttons to close popup
       * @param requestId
       * @param serverError
       */
      finishLoading: function (requestId, serverError) {
        if (Em.isNone(requestId)) {
          this.set('isError', true);
          this.set('showFooter', true);
          this.set('showCloseButton', true);
          this.set('serverError', serverError);
        } else {
          callback(requestId);
          this.hide();
        }
      }
    });
  },
  /**
   * Bootstrap selected hosts.
   * @param bootStrapData
   * @param callback
   * @return {Object}
   */
  launchBootstrap: function (bootStrapData, callback) {
    var popup = this.showLaunchBootstrapPopup(callback);
    App.ajax.send({
      name: 'wizard.launch_bootstrap',
      sender: this,
      data: {
        bootStrapData: bootStrapData,
        popup: popup
      },
      success: 'launchBootstrapSuccessCallback',
      error: 'launchBootstrapErrorCallback'
    });
    return popup;
  },

  launchBootstrapSuccessCallback: function (data, opt, params) {
    console.log("TRACE: POST bootstrap succeeded");
    params.popup.finishLoading(data.requestId, null);
  },

  launchBootstrapErrorCallback: function (request, ajaxOptions, error, opt, params) {
    console.log("ERROR: POST bootstrap failed");
    params.popup.finishLoading(null, error);
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
    this.get('dbPropertiesToClean').forEach(function (key) {
      this.setDBProperty(key, undefined);
    }, this);
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
    return App.ajax.send({
      name: 'wizard.service_components',
      sender: this,
      data: {
        stackUrl: App.get('stackVersionURL'),
        stackVersion: App.get('currentStackVersionNumber'),
        async: false
      },
      success: 'loadServiceComponentsSuccessCallback',
      error: 'loadServiceComponentsErrorCallback'
    });
  },

  loadServiceComponentsSuccessCallback: function (jsonData) {
    var savedSelectedServices = this.getDBProperty('selectedServiceNames');
    var savedInstalledServices = this.getDBProperty('installedServiceNames');
    this.set('content.selectedServiceNames', savedSelectedServices);
    this.set('content.installedServiceNames', savedInstalledServices);
    if (!savedSelectedServices) {
      jsonData.items.forEach(function (service) {
        service.StackServices.is_selected = true;
      }, this);
    } else {
      jsonData.items.forEach(function (service) {
        if (savedSelectedServices.contains(service.StackServices.service_name))
          service.StackServices.is_selected = true;
        else
          service.StackServices.is_selected = false;
      }, this);
    }

    if (!savedInstalledServices) {
      jsonData.items.forEach(function (service) {
        service.StackServices.is_installed = false;
      }, this);
    } else {
      jsonData.items.forEach(function (service) {
        if (savedInstalledServices.contains(service.StackServices.service_name))
          service.StackServices.is_installed = true;
        else
          service.StackServices.is_installed = false;
      }, this);
    }

    App.stackServiceMapper.mapStackServices(jsonData);
  },

  loadServiceComponentsErrorCallback: function (request, ajaxOptions, error) {
    console.log("TRACE: STep5 -> In error function for the getServiceComponents call");
    console.log("TRACE: STep5 -> error code status is: " + request.status);
    console.log('Step8: Error message is: ' + request.responseText);
  },

  /**
   * Load config groups from local DB
   */
  loadServiceConfigGroups: function () {
    var serviceConfigGroups = this.getDBProperty('serviceConfigGroups'),
      hosts = this.getDBProperty('hosts'),
      host_names = Em.keys(hosts);
    if (Em.isNone(serviceConfigGroups)) {
      serviceConfigGroups = [];
    }
    else {
      serviceConfigGroups.forEach(function(group) {
        var hostNames = group.hosts.map(function(host_id) {
          for (var i = 0; i < host_names.length; i++) {
            if (hosts[host_names[i]].id === host_id) {
              return host_names[i];
            }
          }
          Em.assert('host is missing!!!!', false);
        });
        Em.set(group, 'hosts', hostNames);
      });
    }
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
    var hosts = this.get('content.hosts'),
      indx = 1;

    //add previously installed hosts
    for (var hostName in hosts) {
      if (!hosts[hostName].isInstalled) {
        delete hosts[hostName];
      }
    }

    stepController.get('confirmedHosts').forEach(function (_host) {
      if (_host.bootStatus == 'REGISTERED') {
        hosts[_host.name] = {
          name: _host.name,
          cpu: _host.cpu,
          memory: _host.memory,
          disk_info: _host.disk_info,
          os_type: _host.os_type,
          os_arch: _host.os_arch,
          ip: _host.ip,
          bootStatus: _host.bootStatus,
          isInstalled: false,
          id: indx++
        };
      }
    });
    console.log('wizardController:saveConfirmedHosts: save hosts ', hosts);
    this.setDBProperty('hosts', hosts);
    this.set('content.hosts', hosts);
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
    var hosts = stepController.get('hosts'),
      dbHosts = this.getDBProperty('hosts'),
      headers = stepController.get('headers');

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
            group: 'Default',
            isInstalled: cb.get('isInstalled'),
            host_id: dbHosts[host.hostName].id
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
    var miscController = App.MainAdminServiceAccountsController.create({content: self.get('content')});
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
  loadAdvancedConfigs: function (dependentController) {
    var self = this;
    var loadServiceConfigsFn = function(clusterProperties) {
      var stackServices = self.get('content.services').filter(function (service) {
        return service.get('isInstalled') || service.get('isSelected');
      });
      var counter = stackServices.length;
      var loadAdvancedConfigResult = [];
      dependentController.set('isAdvancedConfigLoaded', false);
      stackServices.forEach(function (service) {
        var serviceName = service.get('serviceName');
        App.config.loadAdvancedConfig(serviceName, function (properties) {
          var supportsFinal = App.config.getConfigTypesInfoFromService(service).supportsFinal;

          function shouldSupportFinal(filename) {
            var matchingConfigType = supportsFinal.find(function (configType) {
              return filename.startsWith(configType);
            });
            return !!matchingConfigType;
          }

          properties.forEach(function (property) {
            property.supportsFinal = shouldSupportFinal(property.filename);
          });
          loadAdvancedConfigResult.pushObjects(properties);
          counter--;
          //pass configs to controller after last call is completed
          if (counter === 0) {
            loadAdvancedConfigResult.pushObjects(clusterProperties);
            self.set('content.advancedServiceConfig', loadAdvancedConfigResult);
            self.setDBProperty('advancedServiceConfig', loadAdvancedConfigResult);
            dependentController.set('isAdvancedConfigLoaded', true);
          }
        });
      }, this);
    };
    App.config.loadClusterConfig(loadServiceConfigsFn);
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
    var fileNamesToUpdate = [];
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
          isVisible: _configProperties.get('isVisible'),
          isFinal: _configProperties.get('isFinal'),
          defaultIsFinal: _configProperties.get('isFinal'),
          supportsFinal: _configProperties.get('supportsFinal'),
          filename: _configProperties.get('filename'),
          displayType: _configProperties.get('displayType'),
          isRequiredByAgent: _configProperties.get('isRequiredByAgent'),
          hasInitialValue: !!_configProperties.get('hasInitialValue'),
          isRequired: _configProperties.get('isRequired'), // flag that allow saving property with empty value
          group: !!_configProperties.get('group') ? _configProperties.get('group.name') : null,
          showLabel: _configProperties.get('showLabel')
        };
        serviceConfigProperties.push(configProperty);
      }, this);
      // check for configs that need to update for installed services
      if (stepController.get('installedServiceNames') && stepController.get('installedServiceNames').contains(_content.get('serviceName'))) {
        // get only modified configs
        var configs = _content.get('configs').filter(function (config) {
          if (config.get('isNotDefaultValue') || (config.get('defaultValue') === null)) {
            var notAllowed = ['masterHost', 'masterHosts', 'slaveHosts', 'slaveHost'];
            return !notAllowed.contains(config.get('displayType')) && !!config.filename;
          }
          return false;
        });
        // if modified configs detected push all service's configs for update
        if (configs.length) {
          fileNamesToUpdate = fileNamesToUpdate.concat(configs.mapProperty('filename').uniq());
        }
        // watch for properties that are not modified but have to be updated
        if (_content.get('configs').someProperty('forceUpdate')) {
          // check for already added modified properties
          var forceUpdatedFileNames = _content.get('configs').filterProperty('forceUpdate', true).mapProperty('filename').uniq();
          fileNamesToUpdate = fileNamesToUpdate.concat(forceUpdatedFileNames).uniq();
        }
      }
    }, this);
    this.setDBProperty('serviceConfigProperties', serviceConfigProperties);
    this.set('content.serviceConfigProperties', serviceConfigProperties);
    this.setDBProperty('fileNamesToUpdate', fileNamesToUpdate);
  },
  /**
   * save Config groups
   * @param stepController
   * @param isAddService
   */
  saveServiceConfigGroups: function (stepController, isAddService) {
    var serviceConfigGroups = [],
      isForInstalledService = false,
      hosts = isAddService ? App.router.get('addServiceController').getDBProperty('hosts') : this.getDBProperty('hosts');
    stepController.get('stepConfigs').forEach(function (service) {
      // mark group of installed service
      if (service.get('selected') === false) isForInstalledService = true;
      service.get('configGroups').forEach(function (configGroup) {
        var properties = [];
        configGroup.get('properties').forEach(function (property) {
          properties.push({
            isRequiredByAgent: property.get('isRequiredByAgent'),
            name: property.get('name'),
            value: property.get('value'),
            isFinal: property.get('isFinal'),
            filename: property.get('filename')
          })
        });
        //configGroup copied into plain JS object to avoid Converting circular structure to JSON
        var hostNames = configGroup.get('hosts').map(function(host_name) {return hosts[host_name].id;});
        serviceConfigGroups.push({
          id: configGroup.get('id'),
          name: configGroup.get('name'),
          description: configGroup.get('description'),
          hosts: hostNames,
          publicHosts: configGroup.get('hosts').map(function(hostName) {return App.router.get('manageConfigGroupsController').hostsToPublic(hostName); }),
          properties: properties,
          isDefault: configGroup.get('isDefault'),
          isForInstalledService: isForInstalledService,
          isForUpdate: configGroup.isForUpdate || configGroup.get('hash') != this.getConfigGroupHash(configGroup, hostNames),
          service: {id: configGroup.get('service.id')}
        });
      }, this)
    }, this);
    this.setDBProperty('serviceConfigGroups', serviceConfigGroups);
    this.set('content.configGroups', serviceConfigGroups);
  },

  /**
   * generate string hash for config group
   * @param {Object} configGroup
   * @param {Array|undefined} hosts
   * @returns {String|null}
   * @method getConfigGroupHash
   */
  getConfigGroupHash: function(configGroup,  hosts) {
    if (!Em.get(configGroup, 'properties.length') && !Em.get(configGroup, 'hosts.length') && !hosts) {
      return null;
    }
    var hash = {};
    Em.get(configGroup, 'properties').forEach(function (config) {
      hash[Em.get(config, 'name')] = {value: Em.get(config, 'value'), isFinal: Em.get(config, 'isFinal')};
    });
    hash['hosts'] = hosts || Em.get(configGroup, 'hosts');
    return JSON.stringify(hash);
  },

  /**
   * return slaveComponents bound to hosts
   * @return {Array}
   */
  getSlaveComponentHosts: function () {
    var components = this.get('slaveComponents');
    var result = [];
    var installedServices = App.Service.find().mapProperty('serviceName');
    var selectedServices = App.StackService.find().filterProperty('isSelected', true).mapProperty('serviceName');
    var installedComponentsMap = {};
    var uninstalledComponents = [];

    components.forEach(function (component) {
      if (installedServices.contains(component.get('serviceName'))) {
        installedComponentsMap[component.get('componentName')] = [];
      } else if (selectedServices.contains(component.get('serviceName'))) {
        uninstalledComponents.push(component);
      }
    }, this);
    installedComponentsMap['HDFS_CLIENT'] = [];

    App.HostComponent.find().forEach(function (hostComponent) {
      if (installedComponentsMap[hostComponent.get('componentName')]) {
        installedComponentsMap[hostComponent.get('componentName')].push(hostComponent.get('hostName'));
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
        componentName: component.get('componentName'),
        displayName: App.format.role(component.get('componentName')),
        hosts: hosts,
        isInstalled: false
      })
    });

    return result;
  },
  /**
   * Load master component hosts data for using in required step controllers
   */
  loadMasterComponentHosts: function () {
    var masterComponentHosts = this.getDBProperty('masterComponentHosts');
    var stackMasterComponents = App.get('components.masters').uniq();
    if (!masterComponentHosts) {
      masterComponentHosts = [];
      App.HostComponent.find().filter(function(component) {
        return stackMasterComponents.contains(component.get('componentName'));
      }).forEach(function (item) {
        masterComponentHosts.push({
          component: item.get('componentName'),
          hostName: item.get('hostName'),
          isInstalled: true,
          serviceId: item.get('service.id'),
          display_name: item.get('displayName')
        })
      });
      this.setDBProperty('masterComponentHosts', masterComponentHosts);
    }
    this.set("content.masterComponentHosts", masterComponentHosts);
  },
  /**
   * Load information about hosts with clients components
   */
  loadClients: function () {
    var clients = this.getDBProperty('clientInfo');
    this.set('content.clients', clients);
    console.log(this.get('content.controllerName') + ".loadClients: loaded list ", clients);
  },

  /**
   * load methods assigned to each step
   * methods executed in exact order as they described in map
   * @return {object}
   */
  loadAllPriorSteps: function () {
    var currentStep = this.get('currentStep');
    var loadMap = this.get('loadMap');
    var operationStack = [];
    var dfd = $.Deferred();

    for (var s in loadMap) {
      if (parseInt(s) <= parseInt(currentStep)) {
        operationStack.pushObjects(loadMap[s]);
      }
    }

    var sequence = App.actionSequence.create({context: this});
    sequence.setSequence(operationStack).onFinish(function () {
      dfd.resolve();
    }).start();

    return dfd.promise();
  },

  /**
   * return new object extended from clusterStatusTemplate
   * @return Object
   */
  getCluster: function () {
    return jQuery.extend({}, this.get('clusterStatusTemplate'), {name: App.router.getClusterName()});
  },

  /**
   * Load services data from server.
   */
  loadServicesFromServer: function () {
    var services = this.getDBProperty('services');
    if (!services) {
      services = {
        selectedServices: [],
        installedServices: []
      };
      App.StackService.find().forEach(function(item){
        var isInstalled = App.Service.find().someProperty('id', item.get('serviceName'));
        item.set('isSelected', isInstalled);
        item.set('isInstalled', isInstalled);
        if (isInstalled) {
          services.selectedServices.push(item.get('serviceName'));
          services.installedServices.push(item.get('serviceName'));
        }
      },this);
      this.setDBProperty('services',services);
    } else {
      App.StackService.find().forEach(function(item) {
        var isSelected =   services.selectedServices.contains(item.get('serviceName'));
        var isInstalled = services.installedServices.contains(item.get('serviceName'));
        item.set('isSelected', isSelected);
        item.set('isInstalled', isInstalled);
      },this);
    }
    this.set('content.services', App.StackService.find());
  },

  /**
   * Load confirmed hosts.
   * Will be used at <code>Assign Masters(step5)</code> step
   */
  loadConfirmedHosts: function () {
    var hosts = App.db.getHosts();

    if (hosts) {
      this.set('content.hosts', hosts);
    }
  }
});
