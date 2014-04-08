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
require('models/host');
require('models/host_component');
require('messages');

describe('App.WizardStep2Controller', function () {

  describe('#isInstaller', function() {
    it('true if controllerName is installerController', function() {
      var controller = App.WizardStep2Controller.create({content: {controllerName: 'installerController'}});
      expect(controller.get('isInstaller')).to.equal(true);
    });
    it('false if controllerName isn\'t installerController', function() {
      var controller = App.WizardStep2Controller.create({content: {controllerName: 'addServiceController'}});
      expect(controller.get('isInstaller')).to.equal(false);
    });
  });

  describe('#manualInstall', function() {
    it('should be equal to content.installOptions.manualInstall', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {manualInstall: true}}});
      expect(controller.get('manualInstall')).to.equal(true);
      controller.toggleProperty('content.installOptions.manualInstall');
      expect(controller.get('manualInstall')).to.equal(false);
    });
  });

  describe('#hostNames', function() {
    it('should be equal to content.installOptions.hostNames', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {hostNames: ['1','2','3']}}});
      expect(controller.get('hostNames')).to.eql(['1','2','3']);
      controller.set('content.installOptions.hostNames', ['1', '2']);
      expect(controller.get('hostNames')).to.eql(['1', '2']);
    });
  });

  describe('#sshKey', function() {
    it('should be equal to content.installOptions.sshKey', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {sshKey: '123'}}});
      expect(controller.get('sshKey')).to.equal('123');
      controller.set('content.installOptions.sshKey', '321');
      expect(controller.get('sshKey')).to.equal('321');
    });
  });

  describe('#sshUser', function() {
    it('should be equal to content.installOptions.sshUser', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {sshUser: '123'}}});
      expect(controller.get('sshUser')).to.equal('123');
      controller.set('content.installOptions.sshUser', '321');
      expect(controller.get('sshUser')).to.equal('321');
    });
  });

  describe('#installType', function() {
    it('should be manualDriven if manualInstall is selected', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {manualInstall: true}}});
      expect(controller.get('installType')).to.equal('manualDriven');
    });
    it('should be ambariDriven if manualInstall isn\'t selected', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {manualInstall: false}}});
      expect(controller.get('installType')).to.equal('ambariDriven');
    });
  });

  describe('#updateHostNameArr()', function () {

      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari'
      });
      App.store.load(App.Host, {'host_name': 'apache.ambari', id: '1'});
      controller.updateHostNameArr();

      it('should push to hostNameArr only new host names', function(){
        expect(controller.get('hostNameArr').length).to.equal(0);
      });

      it('should push to inputtedAgainHostNames already installed host names', function(){
        expect(controller.get('inputtedAgainHostNames').length).to.equal(1);
      })
  });

  describe('#isAllHostNamesValid()', function () {

    var controller = App.WizardStep2Controller.create({
      hostNames: ''
    });

    it('should return true if all host names are valid', function(){
      controller.set('hostNames', 'amache.org ambari.com');
      expect(controller.isAllHostNamesValid()).to.equal(true);
    });

    var tests = Em.A([
      'hostname',
      '-hostname.com',
      'hostname-.com',
      'host_name.com',
      '123.123.123.123',
      'hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname.hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname.hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname.hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname',
      'hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname.hostname'
    ]);
    tests.forEach(function (test) {
      it('should return false for invalid host names ' + test + ' ', function () {
        controller.set('hostNames', test);
        expect(controller.isAllHostNamesValid()).to.equal(false);
      });
    });
  });

  describe('#checkHostError()', function () {

    var controller = App.WizardStep2Controller.create();

    it('should set hostsError if hostNames is ""', function () {
      controller.set('content', {'installOptions': {'hostNames': ''}});
      controller.checkHostError();
      expect(controller.get('hostsError').length).to.be.above(2);
    });

    it('should set hostsError to null if hostNames is valid', function () {
      controller.set('content', {'installOptions': {'hostNames': 'ambari'}});
      controller.checkHostError();
      expect(controller.get('hostsError')).to.equal(null);
    })
  });

  describe('#checkHostAfterSubmitHandler()', function () {

    it('should be called after changing hasSubmitted', function (done) {
      var controller = App.WizardStep2Controller.create({
        checkHostError: function () {
          done();
        }
      });
      controller.set('hasSubmitted', true);
    });

    it('should be called after changing hostNames', function (done) {
      var controller = App.WizardStep2Controller.create({
        hasSubmitted: true,
        checkHostError: function () {
          done();
        }
      });
      controller.set('content', {'installOptions': {'hostNames': 'ambari'}});
    })
  });

  describe('#sshKeyError', function () {

    var tests = Em.A([
      {
        manualInstall: false,
        sshKey: '',
        hasSubmitted: false,
        e: null
      },
      {
        manualInstall: true,
        sshKey: '',
        hasSubmitted: false,
        e: null
      },
      {
        manualInstall: true,
        sshKey: 'nobody',
        hasSubmitted: false,
        e: null
      },
      {
        manualInstall: false,
        sshKey: 'nobody',
        hasSubmitted: false,
        e: null
      },
      {
        manualInstall: false,
        sshKey: '',
        hasSubmitted: true,
        e: null
      },
      {
        manualInstall: true,
        sshKey: '',
        hasSubmitted: true,
        e: null
      },
      {
        manualInstall: true,
        sshKey: 'nobody',
        hasSubmitted: true,
        e: null
      },
      {
        manualInstall: false,
        sshKey: 'nobody',
        hasSubmitted: true,
        e: null
      }
    ]);

    tests.forEach(function(test) {
      it(test.sshKey + ' ' + test.manualInstall.toString() + ' ' + test.hasSubmitted.toString(), function() {
        var controller = App.WizardStep2Controller.create({content: {installOptions: {manualInstall: test.manualInstall, sshKey: test.sshKey}}});
        if(Em.isNone(test.e)) {
          expect(controller.get('sshKeyError')).to.equal(null);
        }
        else {
          expect(controller.get('sshKeyError').length).to.be.above(2);
        }
      });
    });
  });

  describe('#sshUserError', function () {

    var tests = Em.A([
      {
        manualInstall: false,
        sshUser: '',
        e: ''
      },
      {
        manualInstall: true,
        sshUser: '',
        e: null
      },
      {
        manualInstall: true,
        sshUser: 'nobody',
        e: null
      },
      {
        manualInstall: false,
        sshUser: 'nobody',
        e: null
      }
    ]);

    tests.forEach(function(test) {
      it('', function() {
        var controller = App.WizardStep2Controller.create({content: {installOptions: {manualInstall: test.manualInstall, sshUser: test.sshUser}}});
        if(Em.isNone(test.e)) {
          expect(controller.get('sshUserError')).to.equal(null);
        }
        else {
          expect(controller.get('sshUserError').length).to.be.above(2);
        }
      });
    });

  });

  describe('#getHostInfo()', function () {

    it('should return object with bootStatus, installType and name for every element in hostNameArr', function () {
      var controller = App.WizardStep2Controller.create({
        hostNameArr: ['apache', 'ambari'],
        installType: 'manualDriven'
      });

      var test = controller.getHostInfo();
      expect(test).to.eql({
        'apache':{'name':'apache', 'installType': 'manualDriven', 'bootStatus': 'PENDING'},
        'ambari':{'name':'ambari', 'installType': 'manualDriven', 'bootStatus': 'PENDING'}
      });
    })
  });

  describe('#setSshKey()', function () {

    it('should set content.installOptions.sshKey', function () {
      var controller = App.WizardStep2Controller.create({
       content: {'installOptions': {'sshKey': '111'}}
      });
      controller.setSshKey('222');
      expect(controller.get('content.installOptions.sshKey')).to.equal('222');
    })
  });

  describe('#evaluateStep()', function () {

    it('should return false if isSubmitDisabled is true', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari'
      });
      controller.set('isSubmitDisabled', true);
      expect(controller.evaluateStep()).to.equal(false);
    });

    it('should return false if hostsError is not empty', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari'
      });
      controller.set('hostsError', 'error');
      expect(controller.evaluateStep()).to.equal(false);
    });

    it('should return false if sshKeyError is not empty', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari'
      });
      controller.set('sshKeyError', 'error');
      expect(controller.evaluateStep()).to.equal(false);
    });

    it('should return false if hostNameArr is empty', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: ''
      });
      expect(controller.evaluateStep()).to.equal(false);
    });

    it('should return false if isPattern is false', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari',
        isPattern: false
      });
      expect(controller.evaluateStep()).to.equal(false);
    })
  });

  describe('#parseHostNamesAsPatternExpression()', function () {

    it('should parse hosts from pattern expression to hostNameArr', function () {
      var controller = App.WizardStep2Controller.create({
        hostNameArr: ['host[001-011]']
      });
      controller.parseHostNamesAsPatternExpression();
      var result = true;
      var hosts = controller.get('hostNameArr');
      for (var i = 1; i<12; i++) {
        var extra = (i.toString().length == 1) ? 0 : '';
        if (hosts[i-1] !== 'host0' + extra + i) {
          result = false;
        }
      }
      expect(result).to.equal(true);
    })
  });

  describe('#proceedNext()', function () {

    it('should call manualInstallPopup if manualInstall is true', function (done) {
      var controller = App.WizardStep2Controller.create({
        hostNames: '',
        manualInstall: true,
        manualInstallPopup: function () {
          done();
        }
      });
      controller.proceedNext(true);
    })
  });

  describe('#isSubmitDisabled', function () {

    var controller = App.WizardStep2Controller.create({
      hostsError: '',
      sshKeyError: ''
    });

    it('should return value if hostsError is not empty', function () {
      controller.set('hostsError', 'error');
      expect(controller.get('isSubmitDisabled').length).to.above(0);
    });

    it('should return value if sshKeyError is not empty', function () {
      controller.set('sshKeyError', 'error');
      controller.set('hostsError', '');
      expect(controller.get('isSubmitDisabled').length).to.above(0);
    })
  });

  describe('#installedHostsPopup', function() {
    before(function() {
      sinon.spy(App.ModalPopup, 'show');
    });
    after(function() {
      App.ModalPopup.show.restore();
    });
    it('should call App.ModalPopup.show', function() {
      var controller = App.WizardStep2Controller.create();
      controller.installedHostsPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });
  });

  describe('#warningPopup', function() {
    before(function() {
      sinon.spy(App.ModalPopup, 'show');
    });
    after(function() {
      App.ModalPopup.show.restore();
    });
    it('should call App.ModalPopup.show', function() {
      var controller = App.WizardStep2Controller.create();
      controller.warningPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });
  });

  describe('#hostNamePatternPopup', function() {
    before(function() {
      sinon.spy(App.ModalPopup, 'show');
    });
    after(function() {
      App.ModalPopup.show.restore();
    });
    it('should call App.ModalPopup.show', function() {
      var controller = App.WizardStep2Controller.create();
      controller.hostNamePatternPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });
  });

  describe('#manualInstallPopup', function() {
    before(function() {
      sinon.spy(App.ModalPopup, 'show');
    });
    after(function() {
      App.ModalPopup.show.restore();
    });
    it('should call App.ModalPopup.show', function() {
      var controller = App.WizardStep2Controller.create();
      controller.manualInstallPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });
  });

  describe('#manualInstallWarningPopup', function() {
    beforeEach(function() {
      sinon.spy(App.ModalPopup, 'show');
    });
    afterEach(function() {
      App.ModalPopup.show.restore();
    });
    it('should call App.ModalPopup.show if content.installOptions.useSsh is false', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {useSsh: false}}});
      controller.manualInstallWarningPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
    });
    it('shouldn\'t call App.ModalPopup.show if content.installOptions.useSsh is true', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {useSsh: true}}});
      controller.manualInstallWarningPopup();
      expect(App.ModalPopup.show.called).to.equal(false);
    });
  });

  describe('#setAmbariJavaHome', function() {
    beforeEach(function() {
      sinon.spy($, 'ajax');
    });
    afterEach(function() {
      $.ajax.restore();
    });
    it('should do ajax-request', function() {
      var controller = App.WizardStep2Controller.create({onGetAmbariJavaHomeSuccess: Em.K, onGetAmbariJavaHomeError: Em.K});
      controller.setAmbariJavaHome();
      expect($.ajax.calledOnce).to.equal(true);
    });
  });

  describe('#onGetAmbariJavaHomeSuccess', function() {
    it('should set java.home value receiced from server', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {}}});
      var test = {RootServiceComponents: {properties: {'java.home': '/root'}}};
      controller.onGetAmbariJavaHomeSuccess(test);
      expect(controller.content.installOptions.javaHome).to.equal('/root');
    });
  });

  describe('#onGetAmbariJavaHomeError', function() {
    it('should set default java.home value', function() {
      var controller = App.WizardStep2Controller.create({content: {installOptions: {}}});
      controller.onGetAmbariJavaHomeError();
      expect(controller.content.installOptions.javaHome).to.equal(App.get('defaultJavaHome'));
    });
  });

});
