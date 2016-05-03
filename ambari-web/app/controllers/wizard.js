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

App.WizardController = Em.Controller.extend(App.LocalStorage, App.ThemesMappingMixin, {

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
    'serviceComponents',
    'fileNamesToUpdate'
  ],

  sensibleConfigs: [
    { name: 'admin_principal', filename: 'krb5-conf.xml'},
    { name: 'admin_password',  filename: 'krb5-conf.xml' }
  ],

  init: function () {
    this.clusters = App.Cluster.find();
    this.setIsStepDisabled();
  },

  /**
   * Set <code>isStepDisabled</code> with list of available steps (basing on <code>totalSteps</code>)
   * @method setIsStepDisabled
   */
  setIsStepDisabled: function () {
      this.set('isStepDisabled', []);
      this.get('isStepDisabled').pushObject(Em.Object.create({
        step: 1,
        value: false
      }));
      for (var i = 2; i <= this.get('totalSteps'); i++) {
        this.get('isStepDisabled').pushObject(Em.Object.create({
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
          displayName: App.format.role(componentName, false)
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

  /**
   * Enable step link in left nav menu
   * @param step - step number
   */
  enableStep: function (step) {
    this.get('isStepDisabled').findProperty('step', step).set('value', false);
  },

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

  /**
   * Move user to the selected step
   *
   * @param {number} step number of the step, where user is moved
   * @param {boolean} disableNaviWarning true - don't show warning about moving more than 1 step back
   * @returns {boolean}
   */
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
        localdb: {}
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
    var installOptions = this.getInstallOptions();
    this.set('content.installOptions', installOptions);
    this.set('content.hosts', {});
    this.setDBProperties({
      installOptions: installOptions,
      hosts: {}
    });
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
   * Convert any object or array to pure JS instance without inherit properties
   * It is used to convert Ember.Object to pure JS Object and Ember.Array to pure JS Array
   * @param originalInstance
   * @returns {*}
   */
  toJSInstance: function (originalInstance) {
    var convertedInstance = originalInstance;
    if (Em.isArray(originalInstance)) {
      convertedInstance = [];
      originalInstance.forEach(function (element) {
        convertedInstance.push(this.toJSInstance(element));
      }, this)
    } else if (originalInstance && typeof originalInstance === 'object') {
      convertedInstance = {};
      for (var property in originalInstance) {
        if (originalInstance.hasOwnProperty(property)) {
          convertedInstance[property] = this.toJSInstance(originalInstance[property]);
        }
      }
    }
    return convertedInstance
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
        urlParams: "HostRoles/desired_state=INSTALLED"
      };
    } else {
      data = {
        context: Em.I18n.t('requestInfo.installServices'),
        ServiceInfo: {"state": "INSTALLED"},
        urlParams: "ServiceInfo/state=INIT"
      };
    }

    var clusterStatus = {
      status: 'PENDING'
    };
    this.saveClusterStatus(clusterStatus);

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
    var convertedValue = this.toJSInstance(this.get('content.' + name));
    this.setDBProperty(name, convertedValue);
    console.log(this.get('name') + ": saved " + name, convertedValue);
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
    var hash = {};
    this.get('dbPropertiesToClean').forEach(function (key) {
      hash[key] = undefined;
    }, this);
    this.setDBProperties(hash);
  },

  getInstallOptions: function() {
    return jQuery.extend({}, App.get('isHadoopWindowsStack') ? this.get('installWindowsOptionsTemplate') : this.get('installOptionsTemplate'));
  },

  installOptionsTemplate: {
    hostNames: "", //string
    manualInstall: false, //true, false
    useSsh: true, //bool
    javaHome: App.defaultJavaHome, //string
    localRepo: false, //true, false
    sshKey: "", //string
    bootRequestId: null, //string
    sshUser: "root", //string
    agentUser: "root" //string
  },

  installWindowsOptionsTemplate: {
    hostNames: "", //string
    manualInstall: false, //true, false
    useSsh: true, //bool
    javaHome: App.defaultJavaHome, //string
    localRepo: false, //true, false
    sshKey: "", //string
    bootRequestId: null, //string
    sshUser: "", //string
    agentUser: "" //string
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
        stackVersion: App.get('currentStackVersionNumber')
      },
      success: 'loadServiceComponentsSuccessCallback',
      error: 'loadServiceComponentsErrorCallback'
    });
  },

  loadServiceComponentsSuccessCallback: function (jsonData) {
    var props = this.getDBProperties(['selectedServiceNames', 'installedServiceNames']);
    var savedSelectedServices = props.selectedServiceNames;
    var savedInstalledServices = props.installedServiceNames;
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
    var props = this.getDBProperties(['serviceConfigGroups', 'hosts']);
    var serviceConfigGroups = props.serviceConfigGroups,
      hosts = props.hosts || {},
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

      var checkboxes = host.checkboxes;
      headers.forEach(function (header) {
        var cb = checkboxes.findProperty('title', header.get('label'));
        if (cb.checked) {
          formattedHosts.get(header.get('name')).push({
            group: 'Default',
            isInstalled: cb.isInstalled,
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
    var fileNamesToUpdate = this.getDBProperty('fileNamesToUpdate') || [];
    var installedServiceNames = stepController.get('installedServiceNames') || [];
    var installedServiceNamesMap = {};
    installedServiceNames.forEach(function(name) {
      installedServiceNamesMap[name] = true;
    });
    stepController.get('stepConfigs').forEach(function (_content) {
      if (_content.serviceName === 'YARN') {
        _content.set('configs', App.config.textareaIntoFileConfigs(_content.get('configs'), 'capacity-scheduler.xml'));
      }
      _content.get('configs').forEach(function (_configProperties) {
        if (!Em.isNone(_configProperties.get('group'))) {
          return false;
        }
        var configProperty = App.config.createDefaultConfig(
          _configProperties.get('name'),
          _configProperties.get('serviceName'),
          _configProperties.get('filename'),
          _configProperties.get('isUserProperty'),
          {value: _configProperties.get('value')}
        );
        configProperty = App.config.mergeStaticProperties(configProperty, _configProperties, ['name', 'filename']);

        if (this.isExcludedConfig(configProperty)) {
          configProperty.value = '';
        }
        serviceConfigProperties.push(configProperty);
      }, this);
      // check for configs that need to update for installed services
      if (installedServiceNamesMap[_content.get('serviceName')]) {
        // get only modified configs
        var configs = _content.get('configs').filter(function (config) {
          if (config.get('isNotDefaultValue') || (config.get('savedValue') === null)) {
            return config.isRequiredByAgent!== false;
          }
          return false;
        });
        // if modified configs detected push all service's configs for update
        if (configs.length) {
          fileNamesToUpdate = fileNamesToUpdate.concat(configs.mapProperty('filename').uniq());
        }
      }
    }, this);
    this.setDBProperties({
      fileNamesToUpdate: fileNamesToUpdate,
      serviceConfigProperties: serviceConfigProperties
    });
    this.set('content.serviceConfigProperties', serviceConfigProperties);
  },

  isExcludedConfig: function (configProperty) {
    return this.get('sensibleConfigs').mapProperty('name').indexOf(configProperty.name) > -1
      && this.get('sensibleConfigs').mapProperty('filename').indexOf(configProperty.filename) > -1;
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
        displayName: App.format.role(name, false),
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
        displayName: App.format.role(component.get('componentName'), false),
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
  },

  loadHosts: function () {
    var dfd;
    var hostsInDb = this.getDBProperty('hosts');
    if (hostsInDb) {
      this.set('content.hosts', hostsInDb);
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
   * Determine if <code>Assign Slaves and Clients</code> step should be skipped
   * @method setSkipSlavesStep
   * @param services
   * @param step
   */
  setSkipSlavesStep: function (services, step) {
    var hasServicesWithSlave = services.someProperty('hasSlave');
    var hasServicesWithClient = services.someProperty('hasClient');
    var hasServicesWithCustomAssignedNonMasters = services.someProperty('hasNonMastersWithCustomAssignment');
    this.set('content.skipSlavesStep', !hasServicesWithSlave && !hasServicesWithClient || !hasServicesWithCustomAssignedNonMasters);
    if (this.get('content.skipSlavesStep')) {
      this.get('isStepDisabled').findProperty('step', step).set('value', this.get('content.skipSlavesStep'));
    }
  },

  /**
   * Load config themes for enhanced config layout.
   *
   * @method loadConfigThemes
   * @return {$.Deferred}
   */
  loadConfigThemes: function () {
    var self = this;
    var dfd = $.Deferred();
    if (!this.get('stackConfigsLoaded')) {
      var serviceNames = App.StackService.find().filter(function (s) {
        return s.get('isSelected') || s.get('isInstalled');
      }).mapProperty('serviceName');
      // Load stack configs before loading themes
      App.config.loadClusterConfigsFromStack().always(function() {
        App.config.loadConfigsFromStack(serviceNames).done(function () {
          if (App.get('isClusterSupportsEnhancedConfigs')) {
            self.loadConfigThemeForServices(serviceNames).always(function () {
              self.set('stackConfigsLoaded', true);
              App.themesMapper.generateAdvancedTabs(serviceNames);
              dfd.resolve();
            });
          } else {
            self.set('stackConfigsLoaded', true);
            dfd.resolve();
          }
        });
      });
    }
    else {
      dfd.resolve();
      this.set('stackConfigsLoaded', true);
    }
    return dfd.promise();
  },

  /**
   * Cache "stepConfigs" to local storage in name value pairs
   * @param stepController
   */
  cacheStepConfigValues: function(stepController) {
    var self = this;
    var stepConfigs = [];
    stepController.get("stepConfigs").forEach(function (category) {
      var configs = category.configs.map(function(config) {
        if (self.isExcludedConfig(config)) {
          config.set('value', '');
        }
        return {
          name: config.name,
          filename: config.filename,
          value: config.value
        };
      });
      stepConfigs = stepConfigs.concat(configs);
    });
    if (stepConfigs.length > 0 ) {
      this.setDBProperty(stepController.name + "-sc", stepConfigs);
    }
  },

  loadCachedStepConfigValues: function(stepController) {
    return this.getDBProperty(stepController.name + "-sc");
  },

  clearCachedStepConfigValues: function(stepController) {
    this.setDBProperty(stepController.name + "-sc", null);
  },

  saveTasksStatuses: function (tasksStatuses) {
    this.set('content.tasksStatuses', tasksStatuses);
    this.setDBProperty('tasksStatuses', tasksStatuses);
  },

  loadTasksStatuses: function() {
    var tasksStatuses = this.getDBProperty('tasksStatuses');
    this.set('content.tasksStatuses', tasksStatuses);
  },

  saveTasksRequestIds: function (tasksRequestIds) {
    this.set('content.tasksRequestIds', tasksRequestIds);
    this.setDBProperty('tasksRequestIds', tasksRequestIds);
  },

  loadTasksRequestIds: function() {
    var tasksRequestIds = this.getDBProperty('tasksRequestIds');
    this.set('content.tasksRequestIds', tasksRequestIds);
  },

  saveRequestIds: function (requestIds) {
    this.set('content.requestIds', requestIds);
    this.setDBProperty('requestIds', requestIds);
  },

  loadRequestIds: function() {
    var requestIds = this.getDBProperty('requestIds');
    this.set('content.requestIds', requestIds);
  },

  loadRecommendations: function () {
    this.set("content.recommendations", this.getDBProperty('recommendations'));
  }
});
