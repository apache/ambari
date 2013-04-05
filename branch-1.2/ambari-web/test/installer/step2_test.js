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
var Ember = require('ember');
require('controllers/wizard/step2_controller');

describe.skip('App.WizardStep2Controller', function () {

  /*describe('#hostsError()', function () {

    it('should return t(installer.step2.hostName.error.required) if manualInstall is false, hostNames is empty, and hasSubmitted is true', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('manualInstall', false);
      controller.set('hostNames', '');
      controller.set('hasSubmitted', true);
      expect(controller.get('hostsError')).to.equal(Ember.I18n.t('installer.step2.hostName.error.required'));
    })

    it('should return null if manualInstall is false, hostNames is not empty, and hasSubmitted is true', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('manualInstall', false);
      controller.set('hostNames', 'ambari');
      controller.set('hasSubmitted', true);
      expect(controller.get('hostsError')).to.equal(null);
    })

    it('should return t(installer.step2.hostName.error.invalid) if manualInstall is false and hostNames has an element ' +
      'that starts with a hyphen', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('manualInstall', false);
      controller.set('hostNames', "-apache");
      expect(controller.get('hostsError')).to.equal(Ember.I18n.t('installer.step2.hostName.error.invalid'));
    })

    it('should return t(installer.step2.hostName.error.invalid) if manualInstall is false and hostNames has an element ' +
      'that ends with a hyphen', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('manualInstall', false);
      controller.set('hostNames', 'apache-');
      expect(controller.get('hostsError')).to.equal(Ember.I18n.t('installer.step2.hostName.error.invalid'));
    })

    it('should return t(installer.step2.hostName.error.required) if manualInstall is true, hostNames is empty, and ' +
      'hasSubmitted is true', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('manualInstall', true);
      controller.set('hostNames', '');
      controller.set('hasSubmitted', true);
      expect(controller.get('hostsError')).to.equal(Ember.I18n.t('installer.step2.hostName.error.required'));
    })

  })

  describe('#sshKeyError()', function () {
    it('should return t(installer.step2.sshKey.error.required) to true if manualInstall is false, sshKey is empty, ' +
      'and hasSubmitted is true', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('manualInstall', false);
      controller.set('sshKey', '');
      controller.set('hasSubmitted', true);
      expect(controller.get('sshKeyError')).to.equal(Ember.I18n.t('installer.step2.sshKey.error.required'));
    })

    it('should return null if manualInstall is true, sshKey is empty, and hasSubmitted is true', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('sshKey', '');
      controller.set('manualInstall', true);
      controller.set('hasSubmitted', true);
      expect(controller.get('sshKeyError')).to.equal(null);
    })

    it('should return null if sshKey is not null and hasSubmitted is true', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('sshKey', 'ambari');
      controller.set('hasSubmitted', true);
      expect(controller.get('sshKeyError')).to.equal(null);
    })

  })*/
    /* Passphrase has been disabled, so commenting out tests
    it('should set passphraseMatchErr to true if ' +
      'passphrase and confirmPassphrase doesn\'t match ', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('manualInstall', false);
      controller.set('passphrase', 'apache ambari');
      controller.set('confirmPassphrase', 'ambari');
      controller.validateStep2();
      expect(controller.get('passphraseMatchErr')).to.equal(true);
    })

    it('should set passphraseMatchErr to false if passphrase and ' +
      'confirmPassphrase doesn\'t match but manualInstall is true ', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('passphrase', 'apache ambari');
      controller.set('confirmPassphrase', 'ambari');
      controller.set('manualInstall', true);
      controller.validateStep2();
      expect(controller.get('passphraseMatchErr')).to.equal(false);
    })

    it('should set passphraseMatchErr to true if passphrase and ' +
      'confirmPassphrase matches', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('passphrase', 'apache ambari');
      controller.set('confirmPassphrase', 'apache ambari');
      controller.validateStep2();
      expect(controller.get('passphraseMatchErr')).to.equal(false);
    })
    */

  /*describe('#localRepoError()', function() {

    it('should return t(installer.step2.localRepo.error.required) localRepo is true, localRepoPath is empty, and hasSubmitted is true', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('localRepo', true);
      controller.set('localRepoPath', '');
      controller.set('hasSubmitted', true);
      expect(controller.get('localRepoError')).to.equal(Ember.I18n.t('installer.step2.localRepo.error.required'));
    })

    it('should return null if localRepo is true, localRepoPath is not empty, and hasSubmitted is true', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('localRepo', true);
      controller.set('localRepoPath', '/etc/');
      controller.set('hasSubmitted', true);
      expect(controller.get('localRepoError')).to.equal(null);
    })

    it('should return null if localRepo is false, localRepoPath is empty, and hasSubmitted is true', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('localRepo', false);
      controller.set('localRepoPath', '');
      controller.set('hasSubmitted', true);
      expect(controller.get('localRepoError')).to.equal(null);
    })
  })

  describe('#evaluateStep2(): On hitting step2 \"next\" button', function () {
    it('should return false if isSubmitDisabled is true ', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('isSubmitDisabled', true);
      expect(controller.evaluateStep2()).to.equal(false);
    })
  })*/

})
