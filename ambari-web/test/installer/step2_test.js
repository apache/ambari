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
require('controllers/installer/step2_controller');

describe('App.InstallerStep2Controller', function () {

  describe('#validateStep2()', function () {

    it('should set hostNameEmptyError  ' +
      'hostNameNotRequiredErr and hostNameErr to false, ' +
      ' if manualInstall is false and hostNames is empty', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('manualInstall', false);
      controller.set('hostNames', '');
      expect(controller.validateStep2()).to.equal(true);
      expect(controller.get('hostNameEmptyError')).to.equal(true);
      expect(controller.get('hostNameNotRequiredErr')).to.equal(false);
    })

    it('should set hostNameNotRequiredErr ' +
      'hostNameEmptyError and hostNameErr to false, ' +
      ' if manualInstall is true and hostNames is not empty', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('manualInstall', true);
      controller.set('hostNames', 'apache ambari');
      expect(controller.validateStep2()).to.equal(true);
      expect(controller.get('hostNameNotRequiredErr')).to.equal(true);
      expect(controller.get('hostNameEmptyError')).to.equal(false);
    })

    it('should set hostNameErr to true,  ' +
      'hostNameEmptyError and hostNameNotRequiredErr to false, ' +
      ' if manualInstall is false and hostNames has an element that starts' +
      ' with hyphen', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('manualInstall', false);
      controller.set('hostNames', "-apache");
      expect(controller.validateStep2()).to.equal(false);
      expect(controller.get('hostNameEmptyError')).to.equal(false);
      expect(controller.get('hostNameNotRequiredErr')).to.equal(false);
    })

    it('should set hostNameErr to true and,  ' +
      'hostNameEmptyError and hostNameNotRequiredErr to false, ' +
      ' if manualInstall is false and hostNames has an element that ends' +
      ' with hyphen', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('manualInstall', false);
      controller.set('hostNames', 'apache-');
      expect(controller.validateStep2()).to.equal(false);
      expect(controller.get('hostNameEmptyError')).to.equal(false);
      expect(controller.get('hostNameNotRequiredErr')).to.equal(false);
    })

    it('hostNameEmptyError, hostNameNotRequiredErr and hostNameErr to false, ' +
      'hostNameErrMsg to null if manualInstall is true and hostNames is null',
      function () {
        var controller = App.InstallerStep2Controller.create();
        controller.set('manualInstall', true);
        controller.set('hostNames', '');
        controller.validateStep2();
        expect(controller.validateStep2()).to.equal(true);
        expect(controller.get('hostNameEmptyError')).to.equal(false);
        expect(controller.get('hostNameNotRequiredErr')).to.equal(false);
        expect(controller.get('hostNameErrMsg')).to.equal('');
      })


    it('should set sshKeyNullErr to true if ' +
      'manualInstall is false and sshKey is empty', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('manualInstall', false);
      controller.set('sshKey', '');
      controller.validateStep2();
      expect(controller.get('sshKeyNullErr')).to.equal(true);
    })

    it('should set sshKeyNullErr to false if sshKey is null but manualInstall is true', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('sshKey', '');
      controller.set('manualInstall', true);
      controller.validateStep2();
      expect(controller.get('sshKeyNullErr')).to.equal(false);
    })

    it('should set sshKeyNullErr to false if sshKey is not null', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('sshKey', 'ambari');
      controller.validateStep2();
      expect(controller.get('sshKeyNullErr')).to.equal(false);
    })

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

    it('should set softRepoLocalPathNullErr to true if ' +
      'localRepo is true and localRepoPath is null', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('localRepo', true);
      controller.set('localRepoPath', '');
      controller.validateStep2();
      expect(controller.get('softRepoLocalPathNullErr')).to.equal(true);


    })


    it('should set softRepoLocalPathNullErr to false if localRepo is true and ' +
      'localRepoPath is not null', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('localRepo', true);
      controller.set('localRepoPath', '/etc/');
      controller.validateStep2();
      expect(controller.get('softRepoLocalPathNullErr')).to.equal(false);
    })

    it('should set softRepoLocalPathNullErr to false if localRepoPath is null ' +
      'but localRepo is false', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('localRepo', false);
      controller.set('localRepoPath', '');
      controller.validateStep2();
      expect(controller.get('softRepoLocalPathNullErr')).to.equal(false);
    })
  })

})

describe('App.InstallerStep2Controller', function () {

  describe('#evaluateStep2(): On hitting step2 \"next\" button', function () {
    it('should return false, if isSubmitDisabled is true ', function () {
      var controller = App.InstallerStep2Controller.create();
      controller.set('isSubmitDisabled', true);
      expect(controller.evaluateStep2()).to.equal(false);
    })
  })

})
