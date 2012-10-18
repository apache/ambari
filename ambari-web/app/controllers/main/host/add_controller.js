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

App.AddHostController = Em.Controller.extend({

  name: 'addHostController',

  /**
   * All wizards data will be stored in this variable
   */
  content: Em.Object.create(),

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,

  isStepDisabled: [],

  totalSteps: 9,

  init: function () {
    this.isStepDisabled.pushObject(Ember.Object.create({
      step: 1,
      value: false
    }));
    for (var i = 2; i <= this.totalSteps; i++) {
      this.isStepDisabled.pushObject(Ember.Object.create({
        step: i,
        value: true
      }));
    }
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

  /**
   * Return current step of Add Host Wizard
   */
  currentStep: function () {
    return App.get('router').getWizardCurrentStep('addHost');
  }.property(),

  /**
   * Set current step to new value.
   * Method moved from App.router.setInstallerCurrentStep
   * @param currentStep
   * @param completed
   */
  setCurrentStep: function (currentStep, completed) {
    App.db.setWizardCurrentStep('addHost', currentStep, completed);
    this.set('currentStep', currentStep);
  },

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
    if (this.get('isStepDisabled').findProperty('step', step).get('value') === false) {
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
   * Load all data for <code>Specify Host(install step2)</code> step
   * Data Example:
   * {
   *   hostNames: '',
   *   manualInstall: false,
   *   sshKey: '',
   *   passphrase: '',
   *   confirmPassphrase: '',
   *   localRepo: false,
   *   localRepoPath: ''
   * }
   */
  loadHosts: function () {

    if (!this.content.hosts) {
      this.content.hosts = Em.Object.create();
    }

    //TODO : rewire it as model. or not :)
    var hostsInfo = Em.Object.create();

    hostsInfo.hostNames = App.db.getAllHostNames() || ''; //empty string if undefined

    //TODO : should we check installType for add host wizard????
    var installType = App.db.getInstallType();
    //false if installType not equals 'manual'
    hostsInfo.manualInstall = installType && installType.installType === 'manual' || false;

    var softRepo = App.db.getSoftRepo();
    if (softRepo && softRepo.repoType === 'local') {
      hostsInfo.localRepo = true;
      hostsInfo.localRepopath = softRepo.repoPath;
    } else {
      hostsInfo.localRepo = false;
      hostsInfo.localRepoPath = '';
    }

    hostsInfo.sshKey = 'random';
    hostsInfo.passphrase = '';
    hostsInfo.confirmPassphrase = '';

    this.set('content.hosts', hostsInfo);
    console.log("AddHostController:loadHosts: loaded data ", hostsInfo);
  },

  /**
   * Save data, which user filled, to main controller
   * @param stepController App.WizardStep2Controller
   */
  saveHosts: function (stepController) {
    //TODO: put data to content.hosts and only then save it)

    //App.db.setBootStatus(false);
    App.db.setAllHostNames(stepController.get('hostNames'));
    App.db.setHosts(stepController.getHostInfo());
    if (stepController.get('manualInstall') === false) {
      App.db.setInstallType({installType: 'ambari' });
    } else {
      App.db.setInstallType({installType: 'manual' });
    }
    if (stepController.get('localRepo') === false) {
      App.db.setSoftRepo({ 'repoType': 'remote', 'repoPath': null});
    } else {
      App.db.setSoftRepo({ 'repoType': 'local', 'repoPath': stepController.get('localRepoPath') });
    }
  },

  /**
   * Return hosts, which were add at <code>Specify Host(step2)</code> step
   * @paramm isNew whether return all hosts or only new ones
   */
  getHostList: function (isNew) {
    var hosts = [];
    var hostArray = App.db.getHosts()
    console.log('in addHostController.getHostList: host names is ', hostArray);

    for (var i in hostArray) {
      var hostInfo = App.HostInfo.create({
        name: hostArray[i].name,
        bootStatus: hostArray[i].bootStatus
      });

      hosts.pushObject(hostInfo);
    }
    ;

    console.log('TRACE: pushing ' + hosts);
    return hosts;
  },

  /**
   * Remove host from model. Used at <code>Confirm hosts(step2)</code> step
   * @param hosts Array of hosts, which we want to delete
   */
  removeHosts: function (hosts) {
    //todo Replace this code with real logic
    App.db.removeHosts(hosts);
  },

  /**
   * Save data, which user filled, to main controller
   * @param stepController App.WizardStep3Controller
   */
  saveConfirmedHosts: function (stepController) {
    var hostInfo = {};
    stepController.get('content').forEach(function (_host) {
      hostInfo[_host.name] = {
        name: _host.name,
        cpu: _host.cpu,
        memory: _host.memory,
        bootStatus: _host.bootStatus
      };
    });
    console.log('addHostController:saveConfirmedHosts: save hosts ', hostInfo);
    App.db.setHosts(hostInfo);
  },

  /**
   * Remove all data for hosts
   */
  clearHosts: function () {
    var hosts = this.get('content').get('hosts');
    if (hosts) {
      hosts.hostNames = '';
      hosts.manualInstall = false;
      hosts.localRepo = '';
      hosts.localRepopath = '';
      hosts.sshKey = '';
      hosts.passphrase = '';
      hosts.confirmPassphrase = '';
    }
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
    console.log('addHostController.loadServices: loaded data ', servicesInfo);
  },

  /**
   * Save data to model
   * @param stepController App.WizardStep4Controller
   */
  saveServices: function (stepController) {
    var serviceNames = [];
    App.db.setService(stepController.get('content'));
    stepController.filterProperty('isSelected', true).forEach(function (item) {
      serviceNames.push(item.serviceName);
    });
    App.db.setSelectedServiceNames(serviceNames);
    console.log('addHostController.saveServices: saved data ', serviceNames);
  },

  /**
   * Load data for all steps until <code>current step</code>
   */
  loadAllPriorSteps: function () {
    var step = this.get('currentStep');
    switch (step) {
      /*case '10':
       this.get('installerStep9Controller').loadStep();
       case '9':
       this.get('installerStep8Controller').loadStep();
       case '8':
       this.get('installerStep7Controller').loadStep();
       case '7':
       this.get('installerStep6Controller').loadStep();
       case '6':
       this.get('installerStep5Controller').loadStep();
       case '5':
       this.get('installerStep4Controller').loadStep();*/
      case '4':
      //this.get('installerStep3Controller').loadStep();
      case '3':
        this.loadServices();
      case '2':
      case '1':
        this.loadHosts();
    }
  },

  /**
   * Remove all loaded data.
   * Created as copy for App.router.clearAllSteps
   */
  clearAllSteps: function () {
    this.clearHosts();
  }

});
