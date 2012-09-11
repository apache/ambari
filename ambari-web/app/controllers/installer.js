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

App.InstallerController = Em.Controller.extend({

  name: 'installerController',

  clusterName: '',
  validClusterName: true,
  hostNames: '',
  hostNameArr: [],
  errorMsg_clusterName: '',
  hostNameEmptyError: false,

  hostNameErr: false,
  manualInstall: false,
  hostNameNotRequireErr: false,
  InstallType: 'ambariDriven',
  sshKey: '',
  passphrase: '',
  confirmPassphrase: '',
  sshKeyNullErr: false,
  passphraseNullErr: false,
  passphraseMatchErr: false,
  localRepo: false,
  softRepo: 'remote',
  localRepoPath: '',
  softRepoLocalPathNullErr: false,

  hideRepoErrMsg: function () {
    if (this.get('localRepo') === false) {
      this.set('softRepoLocalPathNullErr', false);
    }
  }.observes('localRepo'),

  hostManageErr: function () {
    if (this.get('hostNameEmptyError') || this.get('hostNameNotRequireErr') || this.get('hostNameErr') || this.get('sshKeyNullErr') || this.get('passphraseMatchErr')) {
      return true;
    } else {
      return false
    }
  }.property('hostNameEmptyError', 'hostNameNotRequireErr', 'hostNameErr', 'sshKeyNullErr', 'passphraseMatchErr'),

  advOptErr: function () {
    if (this.get('softRepoLocalPathNullErr')) {
      return true;
    } else {
      return false
    }
  }.property('softRepoLocalPathNullErr'),

  evaluateStep1: function () {
    //TODO: Done
    //task1 =  checks on valid cluster name
    //task2 (prereq(task1 says it's a valid cluster name)) =  storing cluster name in localstorage
    var result;
    console.log('TRACE: Entering controller:Installer:evaluateStep1 function');
    if (this.get('clusterName') == '') {
      this.set('errorMsg_clusterName', App.messages.step1_clusterName_error_null);
      this.set('validClusterName', false);
      result = false;
    } else if (/\s/.test(this.get('clusterName'))) {
      console.log('White spaces not allowed for cluster name');
      this.set('errorMsg_clusterName', App.messages.step1_clusterName_error_Whitespaces);
      this.set('validClusterName', false);
      result = false;
    } else if (/[^\w\s]/gi.test(this.get('clusterName'))) {
      console.log('Special characters are not allowed for the cluster name');
      this.set('errorMsg_clusterName', App.messages.step1_clusterName_error_specialChar);
      this.set('validClusterName', false);
      result = false;
    } else {
      console.log('value of clusterNmae is: ' + this.get('clusterName'));
      this.set('validClusterName', true);
      result = true;
    }
    if (result === true) {
      App.db.setClusterName(this.get('clusterName'));
    }
    console.log('Exiting the evaluatestep1 function');
    return result;
  },

  evaluateStep2: function () {
    // TODO: evaluation/manipulation at the end of step2
    //task1 = do primary validations on whole step before executing any further steps
    //task2 = parsing hostnames string to hostnames json array
    //task3 = check validation for every hostname and store it in localstorage
    //task4 = Storing ambari agent Install type in localStorage (InstallType maps at host level and so every host will have this as an property)
    //task5 = Storing path of software repository(remote/local repo) to localStorage
    //task6 = call to rest API: @Post http://ambari_server/api/bootstrap
    //task7 =  On faliure of the previous call, show 'error injecting host information in server db'
    //task8 =  On success of the previous call, go to step 3(awesome....)

    console.log('TRACE: Entering controller:Installer:evaluateStep2 function');
    /**                 task1                **/
    console.log('value of manual install is: ' + this.get('manualInstall'));
    if (this.get('hostNames') === '' && this.get('manualInstall') === false) {
      this.set('hostNameEmptyError', true);
      this.set('hostNameNotRequireErr', false);
      return false;
    } else if (this.get('hostNames') !== '' && this.get('manualInstall') === true) {
      this.set('hostNameNotRequireErr', true);
      this.set('hostNameEmptyError', false);
      return false;
    } else {
      this.set('hostNameEmptyError', false);
      this.set('hostNameNotRequireErr', false);
    }

    if (this.get('manualInstall') === false) {
      if (this.get('sshKey') === '') {
        this.set('sshKeyNullErr', true);
        return false;
      }
      else {
        this.set('sshKeyNullErr', false);
      }
      if (this.get('passphrase') !== this.get('confirmPassphrase')) {
        this.set('passphraseMatchErr', true);
        return false;
      } else {
        this.set('passphraseMatchErr', false);
      }
    }

    if (this.get('localRepo') === true) {
      if (this.get('localRepoPath') === '') {
        this.set('softRepoLocalPathNullErr', true);
        return false;
      } else {
        this.set('softRepoLocalPathNullErr', false);
      }
    } else {
      this.set('softRepoLocalPathNullErr', false);
    }


    /**                 task2  task3 task4                      **/
    this.hostNameArr = this.get('hostNames').split('\s');
    for (var i = 0; i < this.hostNameArr.length; i++) {
      //TODO: other validation for hostnames will be covered over here
      // For now hostname that are starting or ending with '-' are not allowed
      if (/^\-/.test(this.hostNameArr[i]) || /\-$/.test(this.hostNameArr[i])) {
        console.log('Invalide host name' + this.hostNameArr[i]);
        alert('Invalide host name: ' + this.hostNameArr[i]);
        this.set('hostNameErr', true);
        return false;
      } else {
        this.set('hostNameErr', false);
      }
    }
    var hostInfo = {};
    for (var i = 0; i < this.hostNameArr.length; i++) {
      hostInfo[this.hostNameArr[i]] = {'name': this.hostNameArr[i]};
      // hostInfo[this.hostNameArr[i]].name =  this.hostNameArr[i];
      hostInfo[this.hostNameArr[i]].installType = this.get('InstallType');
    }
    App.db.setHosts(hostInfo);


    /**                     task4                    **/
    var softRepoInfo = {'type': this.get('softRepo'), 'path': ''};

    if ('success' == 'success') {
      return true;
    } else {
      return false;
    }

  },
  evaluateStep3: function () {
    // TODO: evaluation at the end of step3
    /* Not sure if below tasks are to be covered over here
     * as these functions are meant to be called at the end of a step
     * and the following tasks are interactive to the page and not on clicking next button.
     *
     * task1 will be a function called on entering step3 from step3 connectoutlet or init function in InstallerStep3 View.
     * task2 will be a parsing function that on reaching a particular condition(all hosts are in success or faliue status)  will stop task1
     * task3 will be a function binded to remove button
     * task4 will be a function binded to retry button
     *
     *
     * keeping it over here for now
     */


    //task1 = start polling with rest API @Get http://ambari_server/api/bootstrap.
    //task2 = stop polling when all the hosts have either success or failure status.
    //task3(prerequisite = remove) = Remove set of selected hosts from the localStorage
    //task4(prerequisite = retry) = temporarily store list of checked host and call to rest API: @Post http://ambari_server/api/bootstrap


  },
  evaluateStep4: function () {
    // TODO: evaluation at the end of step4

  },
  evaluateStep5: function () {
    // TODO: evaluation at the end of step5

  },
  evaluateStep6: function () {
    // TODO: evaluation at the end of step6

  },
  evaluateStep7: function () {
    // TODO: evaluation at the end of step7

  },
  evaluateStep8: function () {
    // TODO: evaluation at the end of step8

  },

  prevInstallStatus: function () {
    console.log('Inside the prevInstallStep function: The name is ' + App.router.get('loginController.loginName'));
    var result = App.db.isCompleted()
    if (result == '1') {
      return true;
    }
  }.property('App.router.loginController.loginName'),

  currentStep: function () {
    return App.get('router').getInstallerCurrentStep();
  }.property(),

  clusters: null,

  init: function () {
    this.clusters = App.Cluster.find();
  },

  isStep1: function () {
    return this.get('currentStep') == '1';
  }.property('currentStep'),

  isStep2: function () {
    return this.get('currentStep') == '2';
  }.property('currentStep'),

  isStep3: function () {
    return this.get('currentStep') == '3';
  }.property('currentStep'),

  isStep4: function () {
    return this.get('currentStep') == '4';
  }.property('currentStep'),

  isStep5: function () {
    return this.get('currentStep') == '5';
  }.property('currentStep'),

  isStep6: function () {
    return this.get('currentStep') == '6';
  }.property('currentStep'),

  isStep7: function () {
    return this.get('currentStep') == '7';
  }.property('currentStep'),

  isStep8: function () {
    return this.get('currentStep') == '8';
  }.property('currentStep'),


  /**
   *
   * @param cluster ClusterModel
   */
  createCluster: function (cluster) {
    alert('created cluster ' + cluster.name);
  }

});