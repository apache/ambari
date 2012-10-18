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

App.InstallerStep2Controller = Em.Controller.extend({
  name: 'installerStep2Controller',
  content: [],
  hostNames: '',
  hostNameArr: [],
  manualInstall: false,
  sshKey: '',
  passphrase: '',
  confirmPassphrase: '',
  localRepo: false,
  localRepoPath: '',
  hasSubmitted: false,

  clearStep: function () {
    this.set('hostNames', '');
    this.set('sshKey', '');
    this.set('passphrase', '');
    this.set('confirmPassphrase', '');
    this.set('localRepoPath', '');
  },

  navigateStep: function () {
    if (App.router.get('isFwdNavigation') === true) {
      this.loadStep();
    }
  },

  loadStep: function () {
    console.log("TRACE: Loading step2: Install Options");
    var hostNames = App.db.getAllHostNames();
    var softRepo = App.db.getSoftRepo();
    var installType = App.db.getInstallType();
    if (hostNames !== undefined) {
      this.set('hostNames', hostNames);
    } else {
      this.set('hostNames', '');
    }

    if (installType !== undefined && installType.installType === 'manual') {
      this.set('manualInstall', true);
    } else {
      this.set('manualInstall', false);
    }

    if (softRepo !== undefined && softRepo.repoType === 'local') {
      this.set('localRepo', true);
      this.set('localRepoPath', softRepo.repoPath);
    } else {
      this.set('localRepo', false);
      this.set('localRepoPath', '');
    }
  },

  installType: function () {
    if (this.get('manualInstall') === true) {
      return 'manualDriven';
    } else {
      return 'ambariDriven';
    }
  }.property('manualInstall'),

  isHostNameValid: function (hostname) {
    // For now hostnames that start or end with '-' are not allowed
    return !(/^\-/.test(hostname) || /\-$/.test(hostname));
  },

  isAllHostNamesValid: function () {
    this.hostNameArr = this.get('hostNames').trim().split(new RegExp("\\s+", "g"));
    for (var index in this.hostNameArr) {
      if (!this.isHostNameValid(this.hostNameArr[index])) {
        return false;
      }
    }
    return true;
  },

  hostsError: function () {
    if (this.get('hasSubmitted') && this.get('hostNames').trim() === '') {
      return Em.I18n.t('installer.step2.hostName.error.required');
    } else if (this.isAllHostNamesValid() === false) {
      return Em.I18n.t('installer.step2.hostName.error.invalid');
    }
    return null;
  }.property('hostNames', 'manualInstall', 'hasSubmitted'),

  sshKeyError: function () {
    if (this.get('hasSubmitted') && this.get('manualInstall') === false && this.get('sshKey').trim() === '') {
      return Em.I18n.t('installer.step2.sshKey.error.required');
    }
    return null;
  }.property('sshKey', 'manualInstall', 'hasSubmitted'),

  localRepoError: function () {
    if (this.get('hasSubmitted') && this.get('localRepo') && this.get('localRepoPath').trim() === '') {
      return Em.I18n.t('installer.step2.localRepo.error.required');
    }
    return null;
  }.property('localRepo', 'localRepoPath', 'hasSubmitted'),

  evaluateStep2: function () {
    console.log('TRACE: Entering controller:InstallerStep2:evaluateStep2 function');

    this.set('hasSubmitted', true);

    if (this.get('hostsError') || this.get('sshKeyError') || this.get('localRepoError')) {
      return false;
    }

    if (this.get('isSubmitDisabled') === true) {
      return false;
    }

    var hostInfo = {};
    this.hostNameArr.forEach(function (hostName) {
      hostInfo[hostName] = {
        name: hostName,
        installType: this.get('installType'),
        bootStatus: 'pending'
      };
    }, this);
    App.db.setAllHostNames(this.get('hostNames'));
    App.db.setHosts(hostInfo);
    if (this.get('manualInstall') === false) {
      App.db.setInstallType({installType: 'ambari' });
    } else {
      App.db.setInstallType({installType: 'manual' });
    }
    if (this.get('localRepo') === false) {
      App.db.setSoftRepo({ 'repoType': 'remote', 'repoPath': null});
    } else {
      App.db.setSoftRepo({ 'repoType': 'local', 'repoPath': this.get('localRepoPath') });
    }

    if (this.get('manualInstall') === true) {
      this.manualInstallPopup();
      return false;
    }

    // For now using mock jquery call
    //TODO: hook up with bootstrap call
    var bootStrapData = {'sshKey': this.get('sshKey'), hosts: this.get('hostNameArr')}.stringify;
    $.ajax({
      type: 'POST',
      url: '/api/bootstrap',
      data: bootStrapData,
      async: false,
      timeout: 2000,
      success: function () {
        console.log("TRACE: In success function for the post bootstrap function");
        App.router.transitionTo('step3');
      },
      error: function () {
        console.log("ERROR: bootstrap post call failed");
        return false;
      },
      complete: function() {
        // TODO: remove this function.  this is just to force navigating to the next step before bootstrap integration
        App.router.send('next');
      },
      statusCode: {
        404: function () {
          console.log("URI not found.");
          //After the bootstrap call hook up change the below return statement to "return false"
          console.log("TRACE: In faliure function for the post bootstrap function");
          return false;
        }
      },
      dataType: 'application/json'
    });
  },

  manualInstallPopup: function (event) {
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step2.manualInstall.popup.header'),
      onPrimary: function () {
        this.hide();
        App.router.send('next');
        // App.router.transitionTo('step3');
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/installer/step2ManualInstallPopup')
      })
    });
  },

  isSubmitDisabled: function() {
    return (this.get('hostNameError') || this.get('sshKeyError') || this.get('localRepoError'));
  }.property('hostNameError', 'sshKeyError', 'localRepoError')

});