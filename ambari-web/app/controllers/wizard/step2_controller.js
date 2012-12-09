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

App.WizardStep2Controller = Em.Controller.extend({
  name: 'wizardStep2Controller',
  hostNameArr: [],
  hasSubmitted: false,

  hostNames: function () {
    return this.get('content.hostNames');
  }.property('content.hostNames'),

  manualInstall: function () {
    return this.get('content.manualInstall');
  }.property('content.manualInstall'),

  localRepo: function () {
    return this.get('content.localRepo');
  }.property('content.localRepo'),


  localRepoPath: function () {
    return this.get('content.localRepoPath');
  }.property('content.localRepoPath'),

  sshKey: function () {
    return this.get('content.sshKey');
  }.property('content.sshKey'),

  passphrase: function () {
    return this.get('content.passphrase');
  }.property('content.passphrase'),

  confirmPassphrase: function () {
    return this.get('content.confirmPassphrase');
  }.property('content.confirmPassphrase'),

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
    if (
       // (typeof this.get('localRepoPath') === 'undefined') ||
        !(/^([-a-z0-9._\/]|%[0-9a-f]{2})*$/i.test(this.get('localRepoPath')) ) ||
        (this.get('hasSubmitted') && this.get('localRepo') && this.get('localRepoPath').trim() === '' )
       )
    {
      return Em.I18n.t('installer.step2.localRepo.error.required');
    }
    return null;
  }.property('localRepo', 'localRepoPath', 'hasSubmitted'),

  /**
   * Get host info, which will be saved in parent controller
   */
  getHostInfo: function () {

    var hostNameArr = this.get('hostNameArr');
    var hostInfo = {};
    for (var i = 0; i < hostNameArr.length; i++) {
      hostInfo[hostNameArr[i]] = {
        name: hostNameArr[i],
        installType: this.get('installType'),
        bootStatus: 'pending'
      };
    }

    return hostInfo;
  },

  /**
   * Onclick handler for <code>next button</code>. Do all UI work except data saving.
   * This work is doing by router.
   * @return {Boolean}
   */
  evaluateStep: function () {
    console.log('TRACE: Entering controller:WizardStep2:evaluateStep function');

    if (this.get('isSubmitDisabled')) {
      return false;
    }

    if (this.get('manualInstall') === true) {
      this.manualInstallPopup();
      return false;
    }

    var bootStrapData = JSON.stringify({'sshKey': this.get('sshKey'), hosts: this.get('hostNameArr')});

    // TODO: skipping bootstrap for now
    if (true) {
      App.router.send('next');
      return true;
    }

    $.ajax({
      type: 'POST',
      url: '/api/bootstrap',
      data: bootStrapData,
      timeout: 10000,
      contentType: 'application/json',
      success: function () {
        console.log("TRACE: POST /api/bootstrap succeeded");
        App.router.send('next');
      },
      error: function () {
        console.log("ERROR: POST /api/bootstrap failed");
        alert('Bootstrap call failed.  Please try again.');
      }
    });
  },

  manualInstallPopup: function (event) {
    App.ModalPopup.show({
      header: Em.I18n.t('installer.step2.manualInstall.popup.header'),
      onPrimary: function () {
        this.hide();
        App.router.send('next');
      },
      bodyClass: Ember.View.extend({
        templateName: require('templates/wizard/step2ManualInstallPopup')
      })
    });
  },

  isSubmitDisabled: function () {
    return (this.get('hostsError') || this.get('sshKeyError') || this.get('localRepoError'));
  }.property('hostsError', 'sshKeyError', 'localRepoError')

});
