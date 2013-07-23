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

  describe('#updateHostNameArr()', function () {

      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari'
      });
      App.store.load(App.Host, {'host_name': 'apache.ambari', id: '1'});
      controller.updateHostNameArr();

      it('should push to hostNameArr only new host names', function(){
        expect(controller.get('hostNameArr').length).to.equal(0);
      })

      it('should push to inputtedAgainHostNames already installed host names', function(){
        expect(controller.get('inputtedAgainHostNames').length).to.equal(1);
      })
  })

  describe('#isAllHostNamesValid()', function () {

    var controller = App.WizardStep2Controller.create({
      hostNames: ''
    });

    it('should return true if all host names are valid', function(){
      controller.set('hostNames', 'amache.org ambari.com');
      expect(controller.isAllHostNamesValid()).to.equal(true);
    })

    var tests = [
      'hostname',
      '-hostname.com',
      'hostname-.com',
      'host_name.com',
      '123.123.123.123',
      'hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname.hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname.hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname.hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname',
      'hostnamehostnamehostnamehostnamehostnamehostnamehostnamehostnamehostname.hostname'
    ];
    tests.forEach(function (test) {
      it('should return false for invalid host names ' + test + ' ', function () {
        controller.set('hostNames', test);
        expect(controller.isAllHostNamesValid()).to.equal(false);
      });
    });
  })

  describe('#checkHostError()', function () {

    var controller = App.WizardStep2Controller.create();

    it('should set hostsError if hostNames is ""', function () {
      controller.set('content', {'installOptions': {'hostNames': ''}});
      controller.checkHostError();
      expect(controller.get('hostsError').length).to.be.above(2);
    })

    /*it('should set hostsError if hostNames is invalid', function () {
      controller.set('content', {'installOptions': {'hostNames': '@#$%'}});
      controller.checkHostError();
      expect(controller.get('hostsError').length).to.be.above(2);
    })*/

    it('should set hostsError to null if hostNames is valid', function () {
      controller.set('content', {'installOptions': {'hostNames': 'ambari'}});
      controller.checkHostError();
      expect(controller.get('hostsError')).to.equal(null);
    })
  })

  describe('#checkHostAfterSubmitHandler()', function () {

    it('should be called after changing hasSubmitted', function (done) {
      var controller = App.WizardStep2Controller.create({
        checkHostError: function () {
          done();
        }
      });
      controller.set('hasSubmitted', true);
    })

    it('should be called after changing hostNames', function (done) {
      var controller = App.WizardStep2Controller.create({
        hasSubmitted: true,
        checkHostError: function () {
          done();
        }
      });
      controller.set('content', {'installOptions': {'hostNames': 'ambari'}});
    })
  })

  describe('#sshKeyError', function () {

    var controller = App.WizardStep2Controller.create({
      manualInstall: false,
      sshKey: '',
      hasSubmitted: true
    });

    it('should return error message if hasSubmitted is true, manualInstall is false and sshKey is ""', function () {
      expect(controller.get('sshKeyError').length).to.be.above(2);
    })

    it('should return null if hasSubmitted is false', function () {
      controller.set('hasSubmitted', false);
      expect(controller.get('sshKeyError')).to.equal(null);
    })
  })

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
  })

  describe('#setSshKey()', function () {

    it('should set content.installOptions.sshKey', function () {
      var controller = App.WizardStep2Controller.create({
       content: {'installOptions': {'sshKey': '111'}}
      });
      controller.setSshKey('222');
      expect(controller.get('content.installOptions.sshKey')).to.equal('222');
    })
  })

  describe('#evaluateStep()', function () {

    it('should return false if isSubmitDisabled is true', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari'
      });
      controller.set('isSubmitDisabled', true);
      expect(controller.evaluateStep()).to.equal(false);
    })

    it('should return false if hostsError is not empty', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari'
      });
      controller.set('hostsError', 'error');
      expect(controller.evaluateStep()).to.equal(false);
    })

    it('should return false if sshKeyError is not empty', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari'
      });
      controller.set('sshKeyError', 'error');
      expect(controller.evaluateStep()).to.equal(false);
    })

    it('should return false if hostNameArr is empty', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: ''
      });
      expect(controller.evaluateStep()).to.equal(false);
    })

    it('should return false if isPattern is false', function () {
      var controller = App.WizardStep2Controller.create({
        hostNames: 'apache.ambari',
        isPattern: false
      });
      expect(controller.evaluateStep()).to.equal(false);
    })
  })

  describe('#patternExpression()', function () {

    it('should parse hosts from pattern expression to hostNameArr', function () {
      var controller = App.WizardStep2Controller.create({
        hostNameArr: ['host[001-011]']
      });
      controller.patternExpression();
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
  })

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
  })

  describe('#isSubmitDisabled', function () {

    var controller = App.WizardStep2Controller.create({
      hostsError: '',
      sshKeyError: ''
    });

    it('should return value if hostsError is not empty', function () {
      controller.set('hostsError', 'error');
      expect(controller.get('isSubmitDisabled').length).to.above(0);
    })

    it('should return value if sshKeyError is not empty', function () {
      controller.set('sshKeyError', 'error');
      controller.set('hostsError', '');
      expect(controller.get('isSubmitDisabled').length).to.above(0);
    })
  })

  /*describe('#saveHosts()', function () {
    var controller = App.WizardStep2Controller.create({
      hostNameArr: ['ambari']
    });
    controller.set('content', Ember.Object.create({'hosts':Ember.Object.create({})}));

    App.router = Ember.Object.create({
      send:function() {}
    });

    it('should set content.hosts', function () {
      controller.saveHosts();
      expect(controller.get('content.hosts')).to.not.be.empty;
    })
  })*/
})
