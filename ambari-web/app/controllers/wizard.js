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

  isStepDisabled: [],

  init: function () {
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
    for (var i = 2; i <= this.totalSteps; i++) {
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

  prevInstallStatus: function () {
    console.log('Inside the prevInstallStep function: The name is ' + App.router.get('loginController.loginName'));
    var result = App.db.isCompleted()
    if (result == '1') {
      return true;
    }
  }.property('App.router.loginController.loginName'),

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
   * Temporary function for wizardStep9, before back-end integration
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
    this.set('content.cluster.requestId', null);

    var self = this;
    var clusterName = this.get('content.cluster.name');
    var url;
    var method = (App.testMode) ? 'GET' : 'PUT';
    var data;

    switch (this.get('content.controllerName')) {
      case 'addHostController':
        if (isRetry) {
          url = App.apiPrefix + '/clusters/' + clusterName + '/host_components?HostRoles/state!=INSTALLED';
          data = '{"HostRoles": {"state": "INSTALLED"}}';
        } else {
          url = App.apiPrefix + '/clusters/' + clusterName + '/host_components?HostRoles/state=INIT';
          data = '{"HostRoles": {"state": "INSTALLED"}}';
        }
        break;
      case 'installerController':
      default:
        if (isRetry) {
          url = (App.testMode) ? '/data/wizard/deploy/2_hosts/poll_1.json' : App.apiPrefix + '/clusters/' + clusterName + '/host_components?HostRoles/state!=INSTALLED';
          data = '{"HostRoles": {"state": "INSTALLED"}}';
        } else {
          url = (App.testMode) ? '/data/wizard/deploy/2_hosts/poll_1.json' : App.apiPrefix + '/clusters/' + clusterName + '/services?ServiceInfo/state=INIT';
          data = '{"ServiceInfo": {"state": "INSTALLED"}}';
        }
        break;
    }

    $.ajax({
      type: method,
      url: url,
      data: data,
      async: false,
      dataType: 'text',
      timeout: App.timeout,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        var installStartTime = new Date().getTime();
        console.log("TRACE: In success function for the installService call");
        console.log("TRACE: value of the url is: " + url);
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
          self.saveClusterStatus(clusterStatus);
        } else {
          console.log('ERROR: Error occurred in parsing JSON data');
        }
      },

      error: function (request, ajaxOptions, error) {
        console.log("TRACE: In error function for the installService call");
        console.log("TRACE: value of the url is: " + url);
        console.log("TRACE: error code status is: " + request.status);
        console.log('Error message is: ' + request.responseText);
        var clusterStatus = {
          status: 'PENDING',
          isInstallError: false,
          isCompleted: false
        };

        self.saveClusterStatus(clusterStatus);
      },

      statusCode: require('data/statusCodes')
    });
  },

  /*
   Bootstrap selected hosts.
   */
  launchBootstrap: function (bootStrapData) {
    var self = this;
    var requestId = null;
    var method = App.testMode ? 'GET' : 'POST';
    var url = App.testMode ? '/data/wizard/bootstrap/bootstrap.json' : App.apiPrefix + '/bootstrap';
    $.ajax({
      type: method,
      url: url,
      async: false,
      data: bootStrapData,
      timeout: App.timeout,
      contentType: 'application/json',
      success: function (data) {
        console.log("TRACE: POST bootstrap succeeded");
        requestId = data.requestId;
      },
      error: function () {
        console.log("ERROR: POST bootstrap failed");
        alert('Bootstrap call failed.  Please try again.');
      },
      statusCode: require('data/statusCodes')
    });
    return requestId;
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
    this.set('content', Ember.Object.create({'controllerName': this.get('content.controllerName')}));
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
    bootRequestId: null //string
  },
  /**
   * Generate serviceComponents as pr the stack definition  and save it to localdata
   * called form stepController step4WizardController
   */
  loadServiceComponents: function (displayOrderConfig, apiUrl) {
    var result = null;
    var method = 'GET';
    var testUrl = '/data/wizard/stack/hdp/version/1.2.0.json';
    var url = (App.testMode) ? testUrl : App.apiPrefix + apiUrl;
    $.ajax({
      type: method,
      url: url,
      async: false,
      dataType: 'text',
      timeout: App.timeout,
      success: function (data) {
        var jsonData = jQuery.parseJSON(data);
        console.log("TRACE: getService ajax call  -> In success function for the getServiceComponents call");
        console.log("TRACE: jsonData.services : " + jsonData.services);

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
          var entry = jsonData.services.findProperty("name", displayOrderConfig[i].serviceName);

          var myService = Service.create({
            serviceName: entry.name,
            displayName: displayOrderConfig[i].displayName,
            isDisabled: i === 0,
            isSelected: true,
            isInstalled: false,
            isHidden: displayOrderConfig[i].isHidden,
            description: entry.comment,
            version: entry.version
          });

          data.push(myService);
        }

        result = data;
        console.log('TRACE: service components: ' + JSON.stringify(data));

      },

      error: function (request, ajaxOptions, error) {
        console.log("TRACE: STep5 -> In error function for the getServiceComponents call");
        console.log("TRACE: STep5 -> value of the url is: " + url);
        console.log("TRACE: STep5 -> error code status is: " + request.status);
        console.log('Step8: Error message is: ' + request.responseText);
      },

      statusCode: require('data/statusCodes')
    });
    return result;
  },

  loadServicesFromServer: function() {
    var services = App.db.getService();
    if (services) {
      return;
    }
    var displayOrderConfig = require('data/services');
    var apiUrl = '/stacks/HDP/version/1.2.0';
    var apiService = this.loadServiceComponents(displayOrderConfig, apiUrl);
    this.set('content.services', apiService);
    App.db.setService(apiService);
  },

  /**
   * Load properties for group of slaves to model
   */
  loadSlaveGroupProperties: function () {
    var groupConfigProperties = App.db.getSlaveProperties() ? App.db.getSlaveProperties() : this.get('content.slaveComponentHosts');
    if (groupConfigProperties) {
      groupConfigProperties.forEach(function (_slaveComponentObj) {
        if (_slaveComponentObj.groups) {
          var groups = [];
          _slaveComponentObj.groups.forEach(function (_group) {
            var properties = [];
            _group.properties.forEach(function (_property) {
              var property = App.ServiceConfigProperty.create(_property);
              property.set('value', _property.storeValue);
              properties.pushObject(property);
            }, this);
            _group.properties = properties;
            groups.pushObject(App.Group.create(_group));
          }, this);
          _slaveComponentObj.groups = groups;
        }
      }, this);
    }
    this.set('content.slaveGroupProperties', groupConfigProperties);
  }

})
