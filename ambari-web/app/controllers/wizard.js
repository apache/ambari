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
    this.isStepDisabled.pushObject(Ember.Object.create({
      step: 1,
      value: false
    }));
    for (var i = 2; i <= this.get('totalSteps'); i++) {
      this.isStepDisabled.pushObject(Ember.Object.create({
        step: i,
        value: true
      }));
    }
    // window.onbeforeunload = function () {
    // return "You have not saved your document yet.  If you continue, your work will not be saved."
    //}
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
      return;
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

    var hostInfo = App.db.getHosts();
    for (var index in hostInfo) {
      hostInfo[index].status = "pending";
      hostInfo[index].message = 'Waiting';
      hostInfo[index].logTasks = [];
      hostInfo[index].tasks = [];
      hostInfo[index].progress = '0';
    }
    App.db.setHosts(hostInfo);
  },

  /**
   * Remove all data for installOptions step
   */
  clearInstallOptions: function () {
    var installOptions = jQuery.extend({}, this.get('installOptionsTemplate'));
    this.set('content.installOptions', installOptions);
    this.save('installOptions');
    this.set('content.hosts', []);
    this.save('hosts');
  },

  toObject: function(object){
    var result = {};
    for(var i in object){
      if(object.hasOwnProperty(i)){
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
      clusterStatus.oldRequestsId.indexOf(clusterStatus.requestId) === -1){
      clusterStatus.oldRequestsId.push(clusterStatus.requestId);
    }
    this.set('content.cluster', clusterStatus);
    this.save('cluster');
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

    switch (this.get('content.controllerName')) {
      case 'addHostController':

        var hostnames = [];
        for (var hostname in App.db.getHosts()) {
          hostnames.push(hostname);
        }

        if (isRetry) {
          name = 'wizard.install_services.add_host_controller.is_retry';
        }
        else {
          name = 'wizard.install_services.add_host_controller.not_is_retry';
        }
        data = {
          "RequestInfo": {
            "context": Em.I18n.t('requestInfo.installComponents'),
            "query": "HostRoles/host_name.in(" + hostnames.join(',') + ")"
          },
          "Body": {
            "HostRoles": {"state": "INSTALLED"}
          }
        };
        data = JSON.stringify(data);
        break;
      case 'installerController':
      default:
        if (isRetry) {
          name = 'wizard.install_services.installer_controller.is_retry';
          data = '{"RequestInfo": {"context" :"'+ Em.I18n.t('requestInfo.installComponents') +'"}, "Body": {"HostRoles": {"state": "INSTALLED"}}}';
        }
        else {
          name = 'wizard.install_services.installer_controller.not_is_retry';
          data = '{"RequestInfo": {"context" :"'+ Em.I18n.t('requestInfo.installServices') +'"}, "Body": {"ServiceInfo": {"state": "INSTALLED"}}}';
        }
        break;
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
    var installStartTime = new Date().getTime();
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
    var result = App.db['get' + name.capitalize()]();
    if (!result){
      result = this['get' + name.capitalize()]();
      App.db['set' + name.capitalize()](result);
      console.log(this.get('name') + ": created " + name, result);
    }
    this.set('content.' + name, result);
    console.log(this.get('name') + ": loaded " + name, result);
  },

  save: function(name){
    var value = this.toObject(this.get('content.' + name));
    App.db['set' + name.capitalize()](value);
    console.log(this.get('name') + ": saved " + name, value);
  },

  clear: function () {
    this.set('content', Ember.Object.create({
      'controllerName': this.get('content.controllerName')
    }));
    this.set('currentStep', 0);
    this.clearStorageData();
  },

  clusterStatusTemplate : {
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

  clearStorageData: function(){
    App.db.setService(undefined); //not to use this data at AddService page
    App.db.setHosts(undefined);
    App.db.setMasterComponentHosts(undefined);
    App.db.setSlaveComponentHosts(undefined);
    App.db.setCluster(undefined);
    App.db.setAllHostNames(undefined);
    App.db.setSlaveProperties(undefined);
    App.db.setInstallOptions(undefined);
    App.db.setAllHostNamesPattern(undefined);
  },

  installOptionsTemplate: {
    hostNames: "", //string
    manualInstall: false, //true, false
    useSsh: true, //bool
    isJavaHome : false, //bool
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
        stackUrl: App.get('stack2VersionURL')
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
          isDisabled: i === 0,
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

  loadServicesFromServer: function() {
    var services = App.db.getService();
    if (services) {
      return;
    }
    var apiService = this.loadServiceComponents();
    this.set('content.services', apiService);
    App.db.setService(apiService);
  },

  registerErrPopup: function (header, message) {
    App.ModalPopup.show({
      header: header,
      secondary: false,
      onPrimary: function () {
        this.hide();
      },
      bodyClass: Ember.View.extend({
        template: Ember.Handlebars.compile(['<p>{{view.message}}</p>'].join('\n')),
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
      hostInfo[_host.name] = {
        name: _host.name,
        cpu: _host.cpu,
        memory: _host.memory,
        disk_info: _host.disk_info,
        bootStatus: _host.bootStatus,
        isInstalled: false
      };
    });
    console.log('wizardController:saveConfirmedHosts: save hosts ', hostInfo);
    App.db.setHosts(hostInfo);
    this.set('content.hosts', hostInfo);
  },

  /**
   * Save data after installation to main controller
   * @param stepController App.WizardStep9Controller
   */
  saveInstalledHosts: function (stepController) {
    var hosts = stepController.get('hosts');
    var hostInfo = App.db.getHosts();

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
    this.save('hosts');
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
    headers.forEach(function(header) {
      formattedHosts.set(header.get('name'), []);
    });

    hosts.forEach(function (host) {

      var checkboxes = host.get('checkboxes');
      headers.forEach(function(header) {
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

    headers.forEach(function(header) {
      slaveComponentHosts.push({
        componentName: header.get('name'),
        displayName: header.get('label').replace(/\s/g, ''),
        hosts: formattedHosts.get(header.get('name'))
      });
    });

    App.db.setSlaveComponentHosts(slaveComponentHosts);
    console.log('wizardController.slaveComponentHosts: saved hosts', slaveComponentHosts);
    this.set('content.slaveComponentHosts', slaveComponentHosts);
  },

  /**
   * Return true if cluster data is loaded and false otherwise.
   * This is used for all wizard controllers except for installer wizard.
   */
  dataLoading: function(){
    var dfd = $.Deferred();
    this.connectOutlet('loading');
    if (App.router.get('clusterController.isLoaded')){
      dfd.resolve();
    } else{
      var interval = setInterval(function(){
        if (App.router.get('clusterController.isLoaded')){
          dfd.resolve();
          clearInterval(interval);
        }
      },50);
    }
    return dfd.promise();
  },

  /**
   * Save cluster status before going to deploy step
   * @param name cluster state. Unique for every wizard
   */
  saveClusterState: function(name){
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
    var configs = (App.db.getAdvancedServiceConfig()) ? App.db.getAdvancedServiceConfig() : [];
    this.get('content.services').filterProperty('isSelected', true).mapProperty('serviceName').forEach(function (_serviceName) {
      var serviceComponents = App.config.loadAdvancedConfig(_serviceName);
      if(serviceComponents){
        configs = configs.concat(serviceComponents);
      }
    }, this);
    this.set('content.advancedServiceConfig', configs);
    App.db.setAdvancedServiceConfig(configs);
  },
  /**
   * Load serviceConfigProperties to model
   */
  loadServiceConfigProperties: function () {
    var serviceConfigProperties = App.db.getServiceConfigProperties();
    this.set('content.serviceConfigProperties', serviceConfigProperties);
    console.log("AddHostController.loadServiceConfigProperties: loaded config ", serviceConfigProperties);
  },
  /**
   * Save config properties
   * @param stepController Step7WizardController
   */
  saveServiceConfigProperties: function (stepController) {
    var serviceConfigProperties = [];
    stepController.get('stepConfigs').forEach(function (_content) {
      _content.get('configs').forEach(function (_configProperties) {
        var displayType = _configProperties.get('displayType');
        if (displayType === 'directories' || displayType === 'directory') {
          var value = _configProperties.get('value').trim().split(/\s+/g).join(',');
          _configProperties.set('value', value);
        }
        var overrides = _configProperties.get('overrides');
        var overridesArray = [];
        if(overrides!=null){
          overrides.forEach(function(override){
            var overrideEntry = {
              value: override.get('value'),
              hosts: []
            };
            override.get('selectedHostOptions').forEach(function(host){
              overrideEntry.hosts.push(host);
            });
            overridesArray.push(overrideEntry);
          });
        }
        overridesArray = (overridesArray.length) ? overridesArray : null;
        var configProperty = {
          id: _configProperties.get('id'),
          name: _configProperties.get('name'),
          value: _configProperties.get('value'),
          defaultValue: _configProperties.get('defaultValue'),
          serviceName: _configProperties.get('serviceName'),
          domain:  _configProperties.get('domain'),
          filename: _configProperties.get('filename'),
          overrides: overridesArray
        };
        serviceConfigProperties.push(configProperty);
      }, this);
    }, this);
    App.db.setServiceConfigProperties(serviceConfigProperties);
    this.set('content.serviceConfigProperties', serviceConfigProperties);
  }
})
