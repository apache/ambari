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
  hostNameEmptyError: false,
  hostNameErr: false,
  manualInstall: false,
  hostNameNotRequiredErr: false,
  hostNameErrMsg: '',
  sshKey: '',
  passphrase: '',
  confirmPassphrase: '',
  sshKeyNullErr: false,
  passphraseMatchErr: false,
  localRepo: false,
  localRepoPath: '',
  softRepoLocalPathNullErr: false,
  isSubmitDisabled: false,

  installType: function () {
    if (this.get('manualInstall') === true) {
      return 'manualDriven';
    } else {
      return 'ambariDriven';
    }
  }.observes('manualInstall'),

  hideRepoErrMsg: function () {
    if (this.get('localRepo') === false) {
      this.set('softRepoLocalPathNullErr', false);
    }
  }.observes('localRepo'),

  validateHostNames: function () {
    this.hostNameArr = this.get('hostNames').trim().split(new RegExp("\\s+","g"));
    for (var index in this.hostNameArr) {
      console.log("host name is: " + this.hostNameArr[index]);
      //TODO: other validation for hostnames will be covered over here
      // For now hostnames that start or end with '-' are not allowed
      if (/^\-/.test(this.hostNameArr[index]) || /\-$/.test(this.hostNameArr[index])) {
        console.log('Invalid host name: ' + this.hostNameArr[index]);
        this.set('hostNameErr', true);
        this.set('hostNameErrMsg', Em.I18n.t('installer.step2.hostName.error.invalid'));
        this.set('hostNameEmptyError', false);
        this.set('hostNameNotRequiredErr', false);
        return false;
      }
    }
    return true;
  },

  validateHosts: function () {
    if (this.get('hostNames') === '' && this.get('manualInstall') === false) {
      this.set('hostNameEmptyError', true);
      this.set('hostNameNotRequiredErr', false);
      this.set('hostNameErr', false);
      this.set('hostNameErrMsg', Em.I18n.t('installer.step2.hostName.error.required'));
    } else if (this.get('hostNames') !== '' && this.get('manualInstall') === true) {
      this.set('hostNameNotRequiredErr', true);
      this.set('hostNameEmptyError', false);
      this.set('hostNameErr', false);
      this.set('hostNameErrMsg', Em.I18n.t('installer.step2.hostName.error.notRequired'));
    } else {
      this.set('hostNameErr', false);
      this.set('hostNameEmptyError', false);
      this.set('hostNameNotRequiredErr', false);
      this.set('hostNameErrMsg', '');
    }
  }.observes('hostNames', 'manualInstall'),

  validateSSHKey: function () {
    if (this.get('manualInstall') === false) {
      if (this.get('sshKey') === '') {
        this.set('sshKeyNullErr', true);
      }
      else {
        this.set('sshKeyNullErr', false);
      }
    }
  }.observes('manualInstall', 'sshKey'),

  validatePassphrase: function () {
    if (this.get('manualInstall') === false) {
      if (this.get('passphrase') !== this.get('confirmPassphrase')) {
        this.set('passphraseMatchErr', true);
      } else {
        this.set('passphraseMatchErr', false);
      }
    }
  }.observes('manualInstall', 'passphrase', 'confirmPassphrase'),

  validateLocalRepo: function () {
    if (this.get('localRepo') === true) {
      if (this.get('localRepoPath') === '') {
        this.set('softRepoLocalPathNullErr', true);
      } else {
        this.set('softRepoLocalPathNullErr', false);
      }
    } else {
      this.set('softRepoLocalPathNullErr', false);
    }
  }.observes('localRepoPath'),

  validateStep2: function () {
    this.validateHosts();
    this.validateSSHKey();
    this.validatePassphrase();
    this.validateLocalRepo();
    return this.validateHostNames();
  },

  hostManageErr: function () {
    return (this.get('hostNameEmptyError') || this.get('hostNameNotRequiredErr') ||
      this.get('hostNameErr') || this.get('sshKeyNullErr') || this.get('passphraseMatchErr'));
  }.property('hostNameErrMsg', 'sshKeyNullErr', 'passphraseMatchErr'),

  sshLessInstall: function () {
    if (this.get('manualInstall') === true) {
      this.set('hostManageErr', false);
      this.set('hostNameEmptyError', false);
      this.set('sshKeyNullErr', false);
      this.set('passphraseMatchErr', false);
    }
  }.observes('manualInstall'),

  advOptErr: function () {
    return this.get('softRepoLocalPathNullErr');
  }.property('softRepoLocalPathNullErr'),

  step2Err: function () {
    if (this.get('hostManageErr') === true || this.get('advOptErr') === true) {
      this.set('isSubmitDisabled', true);
    } else {
      this.set('isSubmitDisabled', false);
    }
  }.observes('hostManageErr', 'advOptErr'),

  softRepo: function () {
    if (this.get('localRepo') === false) {
      this.set('localRepoPath', '');
    }
  }.observes('localRepo'),


  evaluateStep2: function () {

    console.log('TRACE: Entering controller:InstallerStep2:evaluateStep2 function');
    console.log('value of manual install is: ' + this.get('manualInstall'));

    var validateResult = !this.validateStep2();

    if (this.get('isSubmitDisabled') === true ) {
      console.log("ERROR: error in validation");
      return false;
    } else {
      if (this.get('manualInstall') === true) {
        this.manualInstallPopup();
        return true;
      }
    }

    var hostInfo = {};
    for (var i = 0; i < this.hostNameArr.length; i++) {
      hostInfo[this.hostNameArr[i]] = {
        name: this.hostNameArr[i],
        installType: this.get('installType')
      };
    }
    App.db.setHosts(hostInfo);

    if (this.get('localRepo') === false) {
      App.db.setSoftRepo({ 'repoType': 'remote', 'repoPath': null});
    } else {
      App.db.setSoftRepo({ 'repoType': 'local', 'repoPath': this.get('localRepoPath') });
    }

    if (this.get('manualInstall') === false) {
      // For now using mock jquery call
      //TODO: hook up with bootstrap call
      var bootStrapData = {'sshKey': this.get('sshKey'), 'sshKeyPassphrase': this.get('passphrase'), hosts: this.get('hostNameArr')}.stringify;
      $.ajax({
        type: 'POST',
        url: '/ambari_server/api/bootstrap',
        data: bootStrapData,
        async: false,
        timeout: 2000,
        success: function () {
          console.log("TRACE: In success function for the post bootstrap function");
          App.transitionTo('step3');
        },
        error: function () {
          console.log("ERROR: bootstrap post call failed");
          return false;
        },
        statusCode: {
          404: function () {
            console.log("URI not found.");
            alert("URI not found,. This needs to be hooked up with a @POST bootstrap call");
            //After the bootstrap call hook up change the below return statement to "return false"
            console.log("TRACE: In faliure function for the post bootstrap function");
            //Remove below line, once bootstrap has been implemented
            App.router.transitionTo('step3');
            return true;
          }
        },
        dataType: 'application/json'
      });
    } else {
      console.log("ERROR: ASSERTION FAILED -> program should have never reached over here");
    }

  },

  manualInstallPopup: function (event) {
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step2.manualInstall.popup.header'),
      onPrimary: function () {
        this.hide();
        App.router.transitionTo('step3');
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/installer/step2ManualInstallPopup')
      })
    });
  }

});