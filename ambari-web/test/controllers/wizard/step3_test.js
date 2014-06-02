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


var Ember = require('ember');
var App = require('app');
var c;
require('utils/http_client');
require('models/host');
require('controllers/wizard/step3_controller');

describe('App.WizardStep3Controller', function () {

  beforeEach(function() {
    c = App.WizardStep3Controller.create({
      wizardController: App.InstallerController.create(),
      disablePreviousSteps: Em.K
    });
  });

  describe('#getAllRegisteredHostsCallback', function () {
    it('One host is already in the cluster, one host is registered', function() {
      var controller = App.WizardStep3Controller.create({
        hostsInCluster: [{
          hostName: 'wst3_host1'
        }],
        bootHosts: [
          {name:'wst3_host1'},
          {name:'wst3_host2'}
        ]
      });
      var test_data = {
        items: [
          {
            Hosts: {
              host_name: 'wst3_host1'
            }
          },
          {
            Hosts: {
              host_name: 'wst3_host2'
            }
          },
          {
            Hosts: {
              host_name: 'wst3_host3'
            }
          }
        ]
      };
      controller.getAllRegisteredHostsCallback(test_data);
      expect(controller.get('hasMoreRegisteredHosts')).to.equal(true);
      expect(controller.get('registeredHosts').length).to.equal(1);
    });

    it('All hosts are new', function() {
      var controller = App.WizardStep3Controller.create({
        hostsInCluster: [{
          hostName: 'wst3_host1'
        }],
        bootHosts: [
          {name:'wst3_host3'},
          {name:'wst3_host4'}
        ]
      });
      var test_data = {
        items: [
          {
            Hosts: {
              host_name: 'wst3_host3'
            }
          },
          {
            Hosts: {
              host_name: 'wst3_host4'
            }
          }
        ]
      };
      controller.getAllRegisteredHostsCallback(test_data);
      expect(controller.get('hasMoreRegisteredHosts')).to.equal(false);
      expect(controller.get('registeredHosts')).to.equal('');
    });

    it('No new hosts', function() {
      var controller = App.WizardStep3Controller.create({
        hostsInCluster: [{
          hostName: 'wst3_host1'
        }],
        bootHosts: [
          {name:'wst3_host1'}
        ]
      });
      var test_data = {
        items: [
          {
            Hosts: {
              host_name: 'wst3_host1'
            }
          }
        ]
      };
      controller.getAllRegisteredHostsCallback(test_data);
      expect(controller.get('hasMoreRegisteredHosts')).to.equal(false);
      expect(controller.get('registeredHosts')).to.equal('');
    });

  });

  describe('#registrationTimeoutSecs', function() {
    it('Manual install', function() {
      var controller = App.WizardStep3Controller.create({
        content: {
          installOptions: {
            manualInstall: true
          }
        }
      });
      expect(controller.get('registrationTimeoutSecs')).to.equal(15);
    });
    it('Not manual install', function() {
      var controller = App.WizardStep3Controller.create({
        content: {
          installOptions: {
            manualInstall: false
          }
        }
      });
      expect(controller.get('registrationTimeoutSecs')).to.equal(120);
    });
  });

  describe('#isHostHaveWarnings', function() {
    var tests = [
      {
        warnings: [{},{}],
        m: 'Warnings exist',
        e: true
      },
      {
        warnings: [],
        m: 'Warnings don\'t exist',
        e: false
      }
    ];
    tests.forEach(function(test) {
      var controller = App.WizardStep3Controller.create();
      controller.set('warnings', test.warnings);
      it(test.m, function() {
        expect(controller.get('isHostHaveWarnings')).to.equal(test.e);
      });
    });
  });

  describe('#isWarningsBoxVisible', function() {
    it('for testMode should be always true', function() {
      App.testMode = true;
      expect(c.get('isWarningsBoxVisible')).to.equal(true);
      App.testMode = false;
    });
    it('for "real" mode should be based on isRegistrationInProgress', function() {
      c.set('disablePreviousSteps', Em.K);
      App.testMode = false;
      c.set('isRegistrationInProgress', false);
      expect(c.get('isWarningsBoxVisible')).to.equal(true);
      c.set('isRegistrationInProgress', true);
      expect(c.get('isWarningsBoxVisible')).to.equal(false);
      App.testMode = true;
    });
  });

  describe('#clearStep', function() {
    it('should clear hosts', function() {
      c.set('hosts', [{}, {}]);
      c.clearStep();
      expect(c.get('hosts')).to.eql([]);
    });
    it('should clear bootHosts', function() {
      c.set('bootHosts', [{}, {}]);
      c.clearStep();
      expect(c.get('bootHosts').length).to.equal(0);
    });
    it('should set stopBootstrap to false', function() {
      c.set('stopBootstrap', true);
      c.clearStep();
      expect(c.get('stopBootstrap')).to.equal(false);
    });
    it('should set wizardController DBProperty bootStatus to false', function() {
      c.get('wizardController').setDBProperty('bootStatus', true);
      c.clearStep();
      expect(c.get('wizardController').getDBProperty('bootStatus')).to.equal(false);
    });
    it('should set isSubmitDisabled to true', function() {
      c.set('isSubmitDisabled', false);
      c.clearStep();
      expect(c.get('isSubmitDisabled')).to.equal(true);
    });
    it('should set isSubmitDisabled to true', function() {
      c.set('isRetryDisabled', false);
      c.clearStep();
      expect(c.get('isRetryDisabled')).to.equal(true);
    });
  });

  describe('#loadStep', function() {
    it('should set registrationStartedAt to null', function() {
      c.set('disablePreviousSteps', Em.K);
      c.set('registrationStartedAt', {});
      c.loadStep();
      expect(c.get('registrationStartedAt')).to.be.null;
    });
    it('should set isLoaded to false', function() {
      c.set('disablePreviousSteps', Em.K);
      c.set('clearStep', Em.K);
      c.set('loadHosts', Em.K);
      c.set('isLoaded', true);
      c.loadStep();
      expect(c.get('isLoaded')).to.equal(false);
    });
    it('should call clearStep', function() {
      c.set('disablePreviousSteps', Em.K);
      c.set('loadHosts', Em.K);
      sinon.spy(c, 'clearStep');
      c.loadStep();
      expect(c.get('clearStep').calledOnce).to.equal(true);
      c.clearStep.restore();
    });
    it('should call loadHosts', function() {
      c.set('disablePreviousSteps', Em.K);
      c.set('loadHosts', Em.K);
      sinon.spy(c, 'loadHosts');
      c.loadStep();
      expect(c.get('loadHosts').calledOnce).to.equal(true);
      c.loadHosts.restore();
    });
    it('should call disablePreviousSteps', function() {
      c.set('disablePreviousSteps', Em.K);
      c.set('loadHosts', Em.K);
      sinon.spy(c, 'disablePreviousSteps');
      c.loadStep();
      expect(c.get('disablePreviousSteps').calledOnce).to.equal(true);
      c.disablePreviousSteps.restore();
    });
  });

  describe('#loadHosts', function() {
    it('should set isLoaded to true', function() {
      c.set('navigateStep', Em.K);
      c.set('content', {hosts: {}});
      c.loadHosts();
      expect(c.get('isLoaded')).to.equal(true);
    });
    it('should set bootStatus REGISTERED on testMode', function() {
      App.testMode = true;
      c.set('navigateStep', Em.K);
      c.set('content', {hosts: {c: {name: 'name'}}});
      c.loadHosts();
      expect(c.get('hosts').everyProperty('bootStatus', 'REGISTERED')).to.equal(true);
    });
    it('should set bootStatus DONE on "real" mode and when installOptions.manualInstall is selected', function() {
      App.testMode = false;
      c.set('navigateStep', Em.K);
      c.set('content', {installOptions:{manualInstall: true}, hosts: {c: {name: 'name'}}});
      c.loadHosts();
      expect(c.get('hosts').everyProperty('bootStatus', 'DONE')).to.equal(true);
      App.testMode = true;
    });
    it('should set bootStatus PENDING on "real" mode and when installOptions.manualInstall is not selected', function() {
      App.testMode = false;
      c.set('navigateStep', Em.K);
      c.set('content', {installOptions:{manualInstall: false}, hosts: {c: {name: 'name'}}});
      c.loadHosts();
      expect(c.get('hosts').everyProperty('bootStatus', 'PENDING')).to.equal(true);
      App.testMode = true;
    });
    it('should set bootStatus PENDING on "real" mode and when installOptions.manualInstall is not selected', function() {
      c.set('navigateStep', Em.K);
      c.set('content', {hosts: {c: {name: 'name'}, d: {name: 'name1'}}});
      c.loadHosts();
      expect(c.get('hosts').everyProperty('isChecked', false)).to.equal(true);
    });
  });

  describe('#parseHostInfo', function() {

    var tests = Em.A([
      {
        bootHosts: Em.A([
          Em.Object.create({name: 'c1', bootStatus: 'REGISTERED', bootLog: ''}),
          Em.Object.create({name: 'c2', bootStatus: 'REGISTERING', bootLog: ''}),
          Em.Object.create({name: 'c3', bootStatus: 'RUNNING', bootLog: ''})
        ]),
        hostsStatusFromServer: Em.A([
          {hostName: 'c1', status: 'REGISTERED', log: 'c1'},
          {hostName: 'c2', status: 'REGISTERED', log: 'c2'},
          {hostName: 'c3', status: 'RUNNING', log: 'c3'}
        ]),
        m: 'bootHosts not empty, hostsStatusFromServer not empty, one is RUNNING',
        e: {
          c: true,
          r: true
        }
      },
      {
        bootHosts: Em.A([]),
        hostsStatusFromServer: Em.A([
          {hostName: 'c1', status: 'REGISTERED', log: 'c1'},
          {hostName: 'c2', status: 'REGISTERED', log: 'c2'},
          {hostName: 'c3', status: 'RUNNING', log: 'c3'}
        ]),
        m: 'bootHosts is empty',
        e: {
          c: false,
          r: false
        }
      },
      {
        bootHosts: Em.A([
          Em.Object.create({name: 'c1', bootStatus: 'REGISTERED', bootLog: ''}),
          Em.Object.create({name: 'c2', bootStatus: 'REGISTERING', bootLog: ''}),
          Em.Object.create({name: 'c3', bootStatus: 'REGISTERED', bootLog: ''})
        ]),
        hostsStatusFromServer: Em.A([
          {hostName: 'c1', status: 'REGISTERED', log: 'c1'},
          {hostName: 'c2', status: 'REGISTERED', log: 'c2'},
          {hostName: 'c3', status: 'REGISTERED', log: 'c3'}
        ]),
        m: 'bootHosts not empty, hostsStatusFromServer not empty, no one is RUNNING',
        e: {
          c: true,
          r: false
        }
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        c.set('bootHosts', test.bootHosts);
        var r = c.parseHostInfo(test.hostsStatusFromServer);
        expect(r).to.equal(test.e.r);
        if (test.e.c) {
          test.hostsStatusFromServer.forEach(function(h) {
            var r = c.get('bootHosts').findProperty('name', h.hostName);
            if (!['REGISTERED', 'REGISTERING'].contains(r.get('bootStatus'))) {
              expect(r.get('bootStatus')).to.equal(h.status);
              expect(r.get('bootLog')).to.equal(h.log);
            }
          });
        }
      });
    });
  });

  describe('#removeHosts', function() {
    it('should call App.showConfirmationPopup', function() {
      sinon.spy(App, 'showConfirmationPopup');
      c.removeHosts(Em.A([]));
      expect(App.showConfirmationPopup.calledOnce).to.equal(true);
      App.showConfirmationPopup.restore();
    });
    it('primary should disable Submit if no more hosts', function() {
      var hosts = [{}];
      c.set('hosts', hosts);
      var popup = c.removeHosts(hosts);
      popup.onPrimary();
      expect(c.get('isSubmitDisabled')).to.equal(true);
    });
  });

  describe('#removeHost', function() {
    it('should call removeHosts with array as arg', function() {
      var host = {a:''};
      sinon.spy(c, 'removeHosts');
      c.removeHost(host);
      expect(c.removeHosts.calledWith([host]));
      c.removeHosts.restore();
    });
  });

  describe('#removeSelectedHosts', function() {
    it('should remove selected hosts', function() {
      c = App.WizardStep3Controller.create({
        wizardController: App.InstallerController.create(),
        hosts: [
          {isChecked: true, name: 'c1'},
          {isChecked: false, name: 'c2'}
        ]
      });
      c.removeSelectedHosts().onPrimary();
      expect(c.get('hosts').mapProperty('name')).to.eql(['c2']);
    });
  });

  describe('#selectedHostsPopup', function() {
    it('should show App.ModalPopup', function() {
      sinon.spy(App.ModalPopup, 'show');
      c.selectedHostsPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
      App.ModalPopup.show.restore();
    });
  });

  describe('#retryHosts', function () {
    var s;
    var installer = {launchBootstrap: Em.K};

    beforeEach(function () {
      sinon.spy(installer, "launchBootstrap");
      s = sinon.stub(App.router, 'get', function () {
        return installer;
      });
      sinon.stub(c, 'doBootstrap', Em.K);
    });

    afterEach(function () {
      c.doBootstrap.restore();
      s.restore();
      installer.launchBootstrap.restore();
    });

    it('should set numPolls to 0', function () {
      c.set('content', {installOptions: {}});
      c.set('numPolls', 123);
      c.retryHosts(Em.A([]));
      expect(c.get('numPolls')).to.equal(0);
    });
    it('should set registrationStartedAt to null', function () {
      c.set('content', {installOptions: {}});
      c.retryHosts(Em.A([]));
      expect(c.get('registrationStartedAt')).to.be.null;
    });
    it('should startRegistration if installOptions.manualInstall is true', function () {
      sinon.spy(c, 'startRegistration');
      c.set('content', {installOptions: {manualInstall: true}});
      c.retryHosts(Em.A([]));
      expect(c.startRegistration.calledOnce).to.equal(true);
      c.startRegistration.restore();
    });
    it('should launchBootstrap if installOptions.manualInstall is false', function () {
      c.set('content', {installOptions: {manualInstall: false}});
      c.retryHosts(Em.A([]));
      expect(installer.launchBootstrap.calledOnce).to.be.true;
    });
  });

  describe('#retryHost', function() {
    it('should callretryHosts with array as arg', function() {
      var host = {n: 'c'}, s = sinon.stub(App.router, 'get', function() {
        return {launchBootstrap: Em.K}
      });
      sinon.spy(c, 'retryHosts');
      c.set('content', {installOptions: {}});
      c.set('doBootstrap', Em.K);
      c.retryHost(host);
      expect(c.retryHosts.calledWith([host])).to.equal(true);
      c.retryHosts.restore();
      s.restore();
    });
  });

  describe('#retrySelectedHosts', function() {
    it('shouldn\'t do nothing if isRetryDisabled is true', function() {
      c.set('isRetryDisabled', true);
      sinon.spy(c, 'retryHosts');
      c.retrySelectedHosts();
      expect(c.retryHosts.called).to.equal(false);
      c.retryHosts.restore();
    });
    it('should retry hosts with FAILED bootStatus and set isRetryDisabled to true', function() {
      var s = sinon.stub(App.router, 'get', function() {
        return {launchBootstrap: Em.K}
      });
      c = App.WizardStep3Controller.create({
        wizardController: App.InstallerController.create(),
        isRetryDisabled: false,
        bootHosts: Em.A([Em.Object.create({name: 'c1', bootStatus: 'FAILED'}), Em.Object.create({name: 'c2', bootStatus: 'REGISTERED'})]),
        content: {installOptions: {}},
        doBootstrap: Em.K
      });
      sinon.spy(c, 'retryHosts');
      c.retrySelectedHosts();
      expect(c.retryHosts.calledWith([{name: 'c1', bootStatus: 'RUNNING'}]));
      expect(c.get('isRetryDisabled')).to.equal(true);
      c.retryHosts.restore();
      s.restore();
    });
  });

  describe('#startBootstrap', function() {
    it('should drop numPolls and registrationStartedAt', function() {
      c.set('numPolls', 123);
      c.set('registrationStartedAt', 1234);
      c.set('doBootstrap', Em.K);
      c.startBootstrap();
      expect(c.get('numPolls')).to.equal(0);
      expect(c.get('registrationStartedAt')).to.be.null;
    });
    it('should drop numPolls and registrationStartedAt', function() {
      var hosts = Em.A([{name: 'c1'}, {name: 'c2'}]);
      c = App.WizardStep3Controller.create({
        wizardController: App.InstallerController.create(),
        doBootstrap: Em.K,
        setRegistrationInProgressOnce: Em.K,
        hosts: hosts
      });
      c.startBootstrap();
      expect(c.get('bootHosts').mapProperty('name')).to.eql(['c1','c2']);
    });
  });

  describe('#setRegistrationInProgressOnce', function() {
    it('should call Ember.run.once with "setRegistrationInProgress"', function() {
      sinon.spy(Em.run, 'once');
      c.setRegistrationInProgressOnce();
      expect(Em.run.once.firstCall.args[1]).to.equal('setRegistrationInProgress');
      Em.run.once.restore();
    });
  });

  describe('#setRegistrationInProgress', function() {
    var tests = Em.A([
      {
        bootHosts: [],
        isLoaded: false,
        e: true,
        m: 'no bootHosts and isLoaded is false'
      },
      {
        bootHosts: [],
        isLoaded: true,
        e: false,
        m: 'no bootHosts and isLoaded is true'
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'RUNNING'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        isLoaded: true,
        e: false,
        m: 'bootHosts without REGISTERED/FAILED and isLoaded is true'
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'RUNNING'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        isLoaded: false,
        e: true,
        m: 'bootHosts without REGISTERED/FAILED and isLoaded is false'
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'REGISTERED'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        isLoaded: false,
        e: true,
        m: 'bootHosts with one REGISTERED and isLoaded is false'
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'FAILED'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        isLoaded: false,
        e: true,
        m: 'bootHosts with one FAILED and isLoaded is false'
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'REGISTERED'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        isLoaded: true,
        e: false,
        m: 'bootHosts with one REGISTERED and isLoaded is true'
      },
      {
        bootHosts: [
          Em.Object.create({bootStatus: 'FAILED'}),
          Em.Object.create({bootStatus: 'RUNNING'})
        ],
        isLoaded: true,
        e: false,
        m: 'bootHosts with one FAILED and isLoaded is true'
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        sinon.stub(c, 'disablePreviousSteps', Em.K);
        c.set('bootHosts', test.bootHosts);
        c.set('isLoaded', test.isLoaded);
        c.setRegistrationInProgress();
        expect(c.get('isRegistrationInProgress')).to.equal(test.e);
        c.disablePreviousSteps.restore();
      });
    });
  });

  describe('#doBootstrap', function() {
    beforeEach(function() {
      sinon.spy(App.ajax, 'send');
    });
    afterEach(function() {
      App.ajax.send.restore();
    });
    it('shouldn\'t do nothing if stopBootstrap is true', function() {
      c.set('stopBootstrap', true);
      c.doBootstrap();
      expect(App.ajax.send.called).to.equal(false);
    });
    it('should increment numPolls if stopBootstrap is false', function() {
      c.set('stopBootstrap', false);
      c.set('numPolls', 0);
      c.doBootstrap();
      expect(c.get('numPolls')).to.equal(1);
    });
    it('should do ajax call if stopBootstrap is false', function() {
      c.set('stopBootstrap', false);
      c.doBootstrap();
      expect(App.ajax.send.called).to.equal(true);
    });
  });

  describe('#startRegistration', function() {
    it('shouldn\'t do nothing if registrationStartedAt isn\'t null', function() {
      c.set('registrationStartedAt', 1234);
      sinon.spy(c, 'isHostsRegistered');
      c.startRegistration();
      expect(c.isHostsRegistered.called).to.equal(false);
      expect(c.get('registrationStartedAt')).to.equal(1234);
      c.isHostsRegistered.restore();
    });
    it('shouldn\'t do nothing if registrationStartedAt isn\'t null', function() {
      c.set('registrationStartedAt', null);
      sinon.spy(c, 'isHostsRegistered');
      c.startRegistration();
      expect(c.isHostsRegistered.calledOnce).to.equal(true);
      c.isHostsRegistered.restore();
    });
  });

  describe('#isHostsRegistered', function() {
    beforeEach(function() {
      sinon.spy(App.ajax, 'send');
    });
    afterEach(function() {
      App.ajax.send.restore();
    });
    it('shouldn\'t do nothing if stopBootstrap is true', function() {
      c.set('stopBootstrap', true);
      c.isHostsRegistered();
      expect(App.ajax.send.called).to.equal(false);
    });
    it('should do ajax call if stopBootstrap is false', function() {
      c.set('stopBootstrap', false);
      c.isHostsRegistered();
      expect(App.ajax.send.called).to.equal(true);

    });
  });

  describe('#isHostsRegisteredSuccessCallback', function() {
    var tests = Em.A([
      {
        bootHosts: Em.A([
          Em.Object.create({bootStatus: 'DONE'})
        ]),
        data: {items:[]},
        m: 'one host DONE',
        e: {
          bs: 'REGISTERING',
          getHostInfoCalled: false
        }
      },
      {
        bootHosts: Em.A([
          Em.Object.create({bootStatus: 'REGISTERING', name: 'c1'})
        ]),
        data: {items:[{Hosts: {host_name: 'c1'}}]},
        m: ' one host REGISTERING',
        e: {
          bs: 'FAILED',
          getHostInfoCalled: false
        }
      },
      {
        bootHosts: Em.A([
          Em.Object.create({bootStatus: 'REGISTERING', name: 'c1'})
        ]),
        data: {items:[{Hosts: {host_name: 'c2'}}]},
        m: 'one host REGISTERING but data without info about it',
        e: {
          bs: 'FAILED',
          getHostInfoCalled: false
        }
      },
      {
        bootHosts: Em.A([
          Em.Object.create({bootStatus: 'RUNNING', name: 'c1'})
        ]),
        data: {items:[{Hosts: {host_name: 'c1'}}]},
        m: ' one host RUNNING',
        e: {
          bs: 'RUNNING',
          getHostInfoCalled: false
        }
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        sinon.spy(c, 'getHostInfo');
        c.set('bootHosts', test.bootHosts);
        c.isHostsRegisteredSuccessCallback(test.data);
        expect(c.get('bootHosts')[0].get('bootStatus')).to.equal(test.e.bs);
        expect(c.getHostInfo.called).to.equal(test.e.getHostInfoCalled);
        c.getHostInfo.restore();
      });
    });
  });

  describe('#getAllRegisteredHosts', function() {
    it('should call App.ajax.send', function() {
      sinon.spy(App.ajax, 'send');
      c.getAllRegisteredHosts();
      expect(App.ajax.send.calledOnce).to.equal(true);
      App.ajax.send.restore();
    });
  });

  describe('#getAllRegisteredHostsCallback', function() {
    var tests = Em.A([
      {
        hostsInCluster: ['c3'],
        bootHosts: [{name:'c1'},{name:'c2'}],
        hosts: Em.A([
          {Hosts: {host_name:'c1'}},
          {Hosts: {host_name:'c2'}}
        ]),
        m: 'No registered hosts',
        e: {
          hasMoreRegisteredHosts: false,
          registeredHosts: ''
        }
      },
      {
        hostsInCluster: ['c4'],
        bootHosts: [{name:'c3'},{name:'c5'}],
        hosts: Em.A([
          {Hosts: {host_name:'c1'}},
          {Hosts: {host_name:'c2'}}
        ]),
        m: '2 registered hosts',
        e: {
          hasMoreRegisteredHosts: true,
          registeredHosts: ['c1','c2']
        }
      },
      {
        hostsInCluster: ['c4'],
        bootHosts: [{name:'c1'},{name:'c5'}],
        hosts: Em.A([
          {Hosts: {host_name:'c1'}},
          {Hosts: {host_name:'c2'}}
        ]),
        m: '1 registered host',
        e: {
          hasMoreRegisteredHosts: true,
          registeredHosts: ['c2']
        }
      },
      {
        hostsInCluster: ['c1'],
        bootHosts: [{name:'c3'},{name:'c5'}],
        hosts: Em.A([
          {Hosts: {host_name:'c1'}},
          {Hosts: {host_name:'c2'}}
        ]),
        m: '1 registered host (2)',
        e: {
          hasMoreRegisteredHosts: true,
          registeredHosts: ['c2']
        }
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        c.reopen({hostsInCluster: test.hostsInCluster, setRegistrationInProgress: Em.K});
        c.set('bootHosts', test.bootHosts);
        c.getAllRegisteredHostsCallback({items:test.hosts});
        expect(c.get('hasMoreRegisteredHosts')).to.equal(test.e.hasMoreRegisteredHosts);
        expect(c.get('registeredHosts')).to.eql(test.e.registeredHosts);
      });
    });
  });

  describe('#registerErrPopup', function() {
    it('should call App.ModalPopup.show', function() {
      sinon.spy(App.ModalPopup, 'show');
      c.registerErrPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
      App.ModalPopup.show.restore();
    });
  });

  describe('#getHostInfo', function() {
    it('should do ajax request', function() {
      sinon.spy(App.ajax, 'send');
      c.getHostInfo();
      expect(App.ajax.send.calledOnce).to.equal(true);
      App.ajax.send.restore();
    });
  });

  describe('#getHostInfoErrorCallback', function() {
    it('should call registerErrPopup', function() {
      sinon.spy(c, 'registerErrPopup');
      c.getHostInfoErrorCallback();
      expect(c.registerErrPopup.calledOnce).to.equal(true);
      c.registerErrPopup.restore();
    });
  });

  describe('#stopRegistration', function() {
    var tests = Em.A([
      {
        bootHosts: [{bootStatus: 'REGISTERED'}, {bootStatus: 'RUNNING'}],
        e: {isSubmitDisabled: false, isRetryDisabled: true}
      },
      {
        bootHosts: [{bootStatus: 'FAILED'}, {bootStatus: 'RUNNING'}],
        e: {isSubmitDisabled: true, isRetryDisabled: false}
      },
      {
        bootHosts: [{bootStatus: 'FAILED'}, {bootStatus: 'REGISTERED'}],
        e: {isSubmitDisabled: false, isRetryDisabled: false}
      },
      {
        bootHosts: [{bootStatus: 'RUNNING'}, {bootStatus: 'RUNNING'}],
        e: {isSubmitDisabled: true, isRetryDisabled: true}
      }
    ]);
    tests.forEach(function(test) {
      it(test.bootHosts.mapProperty('bootStatus').join(', '), function() {
        c.reopen({bootHosts: test.bootHosts});
        c.stopRegistration();
        expect(c.get('isSubmitDisabled')).to.equal(test.e.isSubmitDisabled);
        expect(c.get('isRetryDisabled')).to.equal(test.e.isRetryDisabled);
      });
    });
  });

  describe('#submit', function() {
    it('if isHostHaveWarnings should show confirmation popup', function() {
      c.reopen({isHostHaveWarnings: true});
      sinon.spy(App.ModalPopup, 'show');
      c.submit();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
      App.ModalPopup.show.restore();
    });
    it('if isHostHaveWarnings should show confirmation popup. on Primary should set bootHosts to content.hosts', function() {
      var bootHosts = [{name: 'c1'}];
      c.reopen({isHostHaveWarnings: true, bootHosts: bootHosts, hosts: []});
      c.submit().onPrimary();
      expect(c.get('content.hosts')).to.eql(bootHosts);
    });
    it('if isHostHaveWarnings is false should set bootHosts to content.hosts', function() {
      var bootHosts = [{name: 'c1'}];
      c.reopen({isHostHaveWarnings: false, bootHosts: bootHosts, hosts: []});
      c.submit();
      expect(c.get('content.hosts')).to.eql(bootHosts);
    });
  });

  describe('#hostLogPopup', function() {
    it('should show App.ModalPopup', function() {
      sinon.spy(App.ModalPopup, 'show');
      c.hostLogPopup({context:Em.Object.create({})});
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
      App.ModalPopup.show.restore();
    });
  });

  describe('#rerunChecksSuccessCallback', function() {
    beforeEach(function() {
      sinon.stub(c, 'parseWarnings', Em.K);
    });
    afterEach(function() {
      c.parseWarnings.restore();
    });
    it('should set checksUpdateProgress to 100', function() {
      c.set('checksUpdateProgress', 0);
      c.rerunChecksSuccessCallback({});
      expect(c.get('checksUpdateProgress')).to.equal(100);
    });
    it('should set checksUpdateStatus to SUCCESS', function() {
      c.set('checksUpdateStatus', '');
      c.rerunChecksSuccessCallback({});
      expect(c.get('checksUpdateStatus')).to.equal('SUCCESS');
    });
    it('should set call parseWarnings', function() {
      c.rerunChecksSuccessCallback({});
      expect(c.parseWarnings.calledOnce).to.equal(true);
    });
  });

  describe('#rerunChecksErrorCallback', function() {
    it('should set checksUpdateProgress to 100', function() {
      c.set('checksUpdateProgress', 0);
      c.rerunChecksErrorCallback({});
      expect(c.get('checksUpdateProgress')).to.equal(100);
    });
    it('should set checksUpdateStatus to FAILED', function() {
      c.set('checksUpdateStatus', '');
      c.rerunChecksErrorCallback({});
      expect(c.get('checksUpdateStatus')).to.equal('FAILED');
    });
  });

  describe('#filterBootHosts', function() {
    var tests = Em.A([
      {
        bootHosts: [
          Em.Object.create({name: 'c1'}),
          Em.Object.create({name: 'c2'})
        ],
        data: {
          items: [
            {Hosts: {host_name: 'c1'}}
          ]
        },
        m: 'one host',
        e: ['c1']
      },
      {
        bootHosts: [
          Em.Object.create({name: 'c1'}),
          Em.Object.create({name: 'c2'})
        ],
        data: {
          items: [
            {Hosts: {host_name: 'c3'}}
          ]
        },
        m: 'no hosts',
        e: []
      },
      {
      bootHosts: [
        Em.Object.create({name: 'c1'}),
        Em.Object.create({name: 'c2'})
      ],
        data: {
        items: [
          {Hosts: {host_name: 'c1'}},
          {Hosts: {host_name: 'c2'}}
        ]
      },
      m: 'many hosts',
        e: ['c1', 'c2']
    }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        c.reopen({bootHosts: test.bootHosts});
        var filteredData = c.filterBootHosts(test.data);
        expect(filteredData.items.mapProperty('Hosts.host_name')).to.eql(test.e);
      });
    });
  });

  describe('#hostWarningsPopup', function() {
    it('should show App.ModalPopup', function() {
      sinon.stub(App.ModalPopup, 'show', Em.K);
      c.hostWarningsPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
      App.ModalPopup.show.restore();
    });
    it('should clear checksUpdateStatus on primary', function() {
      c.set('checksUpdateStatus', 'not null value');
      c.hostWarningsPopup().onPrimary();
      expect(c.get('checksUpdateStatus')).to.be.null;
    });
    it('should clear checksUpdateStatus on close', function() {
      c.set('checksUpdateStatus', 'not null value');
      c.hostWarningsPopup().onClose();
      expect(c.get('checksUpdateStatus')).to.be.null;
    });
    it('should rerunChecks onSecondary', function() {
      sinon.stub(c, 'rerunChecks', Em.K);
      c.hostWarningsPopup().onSecondary();
      expect(c.rerunChecks.calledOnce).to.equal(true);
    });
  });

  describe('#registeredHostsPopup', function() {
    it('should show App.ModalPopup', function() {
      sinon.spy(App.ModalPopup, 'show');
      c.registeredHostsPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
      App.ModalPopup.show.restore();
    });
  });

  describe('#parseWarnings', function() {
    it('no warnings if last_agent_env isn\'t specified', function() {
      c.set('warnings', [{}]);
      c.set('warningsByHost', [{},{}]);
      c.parseWarnings({items:[{Hosts:{host_name:'c1'}}]});
      expect(c.get('warnings')).to.eql([]);
      expect(c.get('warningsByHost.length')).to.equal(1); // default group
      expect(c.get('isWarningsLoaded')).to.equal(true);
    });

    Em.A([
        {
          m: 'parse stackFoldersAndFiles',
          tests : Em.A([
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {stackFoldersAndFiles: []}}}],
              m: 'empty stackFoldersAndFiles',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {stackFoldersAndFiles: [{name: 'n1'}]}}}],
              m: 'not empty stackFoldersAndFiles',
              e: {
                warnings: [{
                  name: 'n1',
                  hosts: ['c1'],
                  onSingleHost: true,
                  category: 'fileFolders'
                }],
                warningsByHost: [1]
              }
            },
            {
              items: [
                {Hosts:{host_name: 'c1', last_agent_env: {stackFoldersAndFiles: [{name: 'n1'}]}}},
                {Hosts:{host_name: 'c2', last_agent_env: {stackFoldersAndFiles: [{name: 'n1'}]}}}
              ],
              m: 'not empty stackFoldersAndFiles on two hosts',
              e: {
                warnings: [{
                  name: 'n1',
                  hosts: ['c1', 'c2'],
                  onSingleHost: false,
                  category: 'fileFolders'
                }],
                warningsByHost: [1]
              }
            }
          ])
        },
        {
          m: 'parse installedPackages',
          tests : Em.A([
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {installedPackages: []}}}],
              m: 'empty installedPackages',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {installedPackages: [{name: 'n1'}]}}}],
              m: 'not empty installedPackages',
              e: {
                warnings: [{
                  name: 'n1',
                  hosts: ['c1'],
                  onSingleHost: true,
                  category: 'packages'
                }],
                warningsByHost: [1]
              }
            },
            {
              items: [
                {Hosts:{host_name: 'c1', last_agent_env: {installedPackages: [{name: 'n1'}]}}},
                {Hosts:{host_name: 'c2', last_agent_env: {installedPackages: [{name: 'n1'}]}}}
              ],
              m: 'not empty installedPackages on two hosts',
              e: {
                warnings: [{
                  name: 'n1',
                  hosts: ['c1', 'c2'],
                  onSingleHost: false,
                  category: 'packages'
                }],
                warningsByHost: [1]
              }
            }
          ])
        },
        {
          m: 'parse hostHealth.liveServices',
          tests : Em.A([
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {hostHealth: []}}}],
              m: 'empty hostHealth',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {hostHealth:{liveServices: []}}}}],
              m: 'empty liveServices',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {hostHealth:{liveServices: [{status: 'Unhealthy', name: 'n1'}]}}}}],
              m: 'not empty hostHealth.liveServices',
              e: {
                warnings: [{
                  name: 'n1',
                  hosts: ['c1'],
                  onSingleHost: true,
                  category: 'services'
                }],
                warningsByHost: [1]
              }
            },
            {
              items: [
                {Hosts:{host_name: 'c1', last_agent_env: {hostHealth:{liveServices: [{status: 'Unhealthy', name: 'n1'}]}}}},
                {Hosts:{host_name: 'c2', last_agent_env: {hostHealth:{liveServices: [{status: 'Unhealthy', name: 'n1'}]}}}}
              ],
              m: 'not empty hostHealth.liveServices on two hosts',
              e: {
                warnings: [{
                  name: 'n1',
                  hosts: ['c1', 'c2'],
                  onSingleHost: false,
                  category: 'services'
                }],
                warningsByHost: [1, 1]
              }
            }
          ])
        },
        {
          m: 'parse existingUsers',
          tests : Em.A([
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {existingUsers: []}}}],
              m: 'empty existingUsers',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {existingUsers: [{userName: 'n1'}]}}}],
              m: 'not empty existingUsers',
              e: {
                warnings: [{
                  name: 'n1',
                  hosts: ['c1'],
                  onSingleHost: true,
                  category: 'users'
                }],
                warningsByHost: [1]
              }
            },
            {
              items: [
                {Hosts:{host_name: 'c1', last_agent_env: {existingUsers: [{userName: 'n1'}]}}},
                {Hosts:{host_name: 'c2', last_agent_env: {existingUsers: [{userName: 'n1'}]}}}
              ],
              m: 'not empty existingUsers on two hosts',
              e: {
                warnings: [{
                  name: 'n1',
                  hosts: ['c1', 'c2'],
                  onSingleHost: false,
                  category: 'users'
                }],
                warningsByHost: [1, 1]
              }
            }
          ])
        },
        {
          m: 'parse alternatives',
          tests : Em.A([
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {alternatives: []}}}],
              m: 'empty alternatives',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {alternatives: [{name: 'n1'}]}}}],
              m: 'not empty alternatives',
              e: {
                warnings: [{
                  name: 'n1',
                  hosts: ['c1'],
                  onSingleHost: true,
                  category: 'alternatives'
                }],
                warningsByHost: [1]
              }
            },
            {
              items: [
                {Hosts:{host_name: 'c1', last_agent_env: {alternatives: [{name: 'n1'}]}}},
                {Hosts:{host_name: 'c2', last_agent_env: {alternatives: [{name: 'n1'}]}}}
              ],
              m: 'not empty alternatives on two hosts',
              e: {
                warnings: [{
                  name: 'n1',
                  hosts: ['c1', 'c2'],
                  onSingleHost: false,
                  category: 'alternatives'
                }],
                warningsByHost: [1, 1]
              }
            }
          ])
        },
        {
          m: 'parse hostHealth.activeJavaProcs',
          tests : Em.A([
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {hostHealth: [], javaProcs: []}}}],
              m: 'empty hostHealth',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {hostHealth:{activeJavaProcs: []}}}}],
              m: 'empty activeJavaProcs',
              e: {
                warnings: [],
                warningsByHost: [0]
              }
            },
            {
              items: [{Hosts:{host_name: 'c1', last_agent_env: {hostHealth:{activeJavaProcs: [{pid: 'n1', command: ''}]}}}}],
              m: 'not empty hostHealth.activeJavaProcs',
              e: {
                warnings: [{
                  pid: 'n1',
                  hosts: ['c1'],
                  onSingleHost: true,
                  category: 'processes'
                }],
                warningsByHost: [1]
              }
            },
            {
              items: [
                {Hosts:{host_name: 'c1', last_agent_env: {hostHealth:{activeJavaProcs: [{pid: 'n1', command: ''}]}}}},
                {Hosts:{host_name: 'c2', last_agent_env: {hostHealth:{activeJavaProcs: [{pid: 'n1', command: ''}]}}}}
              ],
              m: 'not empty hostHealth.activeJavaProcs on two hosts',
              e: {
                warnings: [{
                  pid: 'n1',
                  hosts: ['c1', 'c2'],
                  onSingleHost: false,
                  category: 'processes'
                }],
                warningsByHost: [1, 1]
              }
            }
          ])
        }
    ]).forEach(function(category) {
      describe(category.m, function() {
        category.tests.forEach(function(test) {
          it(test.m, function() {
            c.parseWarnings({items: test.items});
            c.get('warnings').forEach(function(w, i) {
              Em.keys(test.e.warnings[i]).forEach(function(k) {
                expect(w[k]).to.eql(test.e.warnings[i][k]);
              });
            });
            for(var i in test.e.warningsByHost) {
              if(test.e.warningsByHost.hasOwnProperty(i)) {
                expect(c.get('warningsByHost')[i].warnings.length).to.equal(test.e.warningsByHost[i]);
              }
            }
          });
        });
      });
    });

  });

  describe('#hostsInCluster', function() {
    it('should load data from App.Host model', function() {
      var hosts = [
        Em.Object.create({hostName: 'h1'}),
        Em.Object.create({hostName: 'h2'}),
        Em.Object.create({hostName: 'h3'})
      ], expected = ['h1', 'h2', 'h3'];
      sinon.stub(App.Host, 'find', function() {
        return hosts;
      });
      expect(c.get('hostsInCluster')).to.eql(expected);
      App.Host.find.restore();
    });
  });

  describe('#navigateStep', function() {
    Em.A([
        {
          isLoaded: true,
          manualInstall: false,
          bootStatus: false,
          m: 'should call startBootstrap',
          e: true
        },
        {
          isLoaded: true,
          manualInstall: false,
          bootStatus: true,
          m: 'shouldn\'t call startBootstrap (1)',
          e: false
        },
        {
          isLoaded: false,
          manualInstall: false,
          bootStatus: false,
          m: 'shouldn\'t call startBootstrap (2)',
          e: false
        },
        {
          isLoaded: false,
          manualInstall: true,
          bootStatus: false,
          m: 'shouldn\'t call startBootstrap (3)',
          e: false
        }
    ]).forEach(function(test) {
        it(test.m, function() {
          c.reopen({
            isLoaded: test.isLoaded,
            content: {
              installOptions: {
                manualInstall: test.manualInstall
              }
            },
            wizardController: Em.Object.create({
              getDBProperty: function() {
                return test.bootStatus
              }
            })
          });
          sinon.stub(c, 'startBootstrap', Em.K);
          c.navigateStep();
          if(test.e) {
            expect(c.startBootstrap.calledOnce).to.equal(true);
          }
          else {
            expect(c.startBootstrap.called).to.equal(false);
          }
          c.startBootstrap.restore();
        });
      });

    it('should set test data if testMode is true', function() {
      c.reopen({
        isLoaded: true,
        hosts: [{}, {}, {}],
        content: {
          installOptions: {
            manualInstall: true
          }
        },
        setRegistrationInProgress: Em.K
      });
      sinon.stub(App, 'get', function(k) {
        if('testMode' === k) return true;
        return Em.get(App, k);
      });
      c.navigateStep();
      App.get.restore();
      expect(c.get('bootHosts.length')).to.equal(c.get('hosts.length'));
      expect(c.get('bootHosts').everyProperty('cpu', '2')).to.equal(true);
      expect(c.get('bootHosts').everyProperty('memory', '2000000')).to.equal(true);
      expect(c.get('isSubmitDisabled')).to.equal(false);
    });

    it('should start registration', function() {
      c.reopen({
        isLoaded: true,
        hosts: [{}, {}, {}],
        content: {
          installOptions: {
            manualInstall: true
          }
        },
        setRegistrationInProgress: Em.K,
        startRegistration: Em.K
      });
      sinon.spy(c, 'startRegistration');
      sinon.stub(App, 'get', function(k) {
        if('testMode' === k) return false;
        return Em.get(App, k);
      });
      c.navigateStep();
      App.get.restore();
      expect(c.startRegistration.calledOnce).to.equal(true);
      expect(c.get('bootHosts.length')).to.equal(c.get('hosts.length'));
      expect(c.get('registrationStartedAt')).to.be.null;
      c.startRegistration.restore();
    });

  });

  describe('#checkHostDiskSpace', function() {
    Em.A([
        {
          diskInfo: [
            {
              available: App.minDiskSpace * 1024 * 1024 - 1024,
              mountpoint: '/'
            }
          ],
          m: 'available less than App.minDiskSpace',
          e: false
        },
        {
          diskInfo: [
            {
              available: App.minDiskSpaceUsrLib * 1024 * 1024 - 1024,
              mountpoint: '/usr'
            }
          ],
          m: 'available less than App.minDiskSpaceUsrLib (1)',
          e: false
        },
        {
          diskInfo: [
            {
              available: App.minDiskSpaceUsrLib * 1024 * 1024 - 1024,
              mountpoint: '/usr/lib'
            }
          ],
          m: 'available less than App.minDiskSpaceUsrLib (2)',
          e: false
        },
        {
          diskInfo: [
            {
              available: App.minDiskSpace * 1024 * 1024 + 1024,
              mountpoint: '/'
            }
          ],
          m: 'available greater than App.minDiskSpace',
          e: true
        },
        {
          diskInfo: [
            {
              available: App.minDiskSpaceUsrLib * 1024 * 1024 + 1024,
              mountpoint: '/usr'
            }
          ],
          m: 'available greater than App.minDiskSpaceUsrLib (1)',
          e: true
        },
        {
          diskInfo: [
            {
              available: App.minDiskSpaceUsrLib * 1024 * 1024 + 1024,
              mountpoint: '/usr/lib'
            }
          ],
          m: 'available greater than App.minDiskSpaceUsrLib (2)',
          e: true
        },
        {
          diskInfo: [
            {
              available: App.minDiskSpaceUsrLib * 1024 * 1024 + 1024,
              mountpoint: '/home/tdk'
            }
          ],
          m: 'mount point without free space checks',
          e: true
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          var r = c.checkHostDiskSpace('', test.diskInfo);
          expect(Em.isEmpty(r)).to.equal(test.e);
        });
      });
  });

  describe('#checkHostOSType', function() {
    it('should return empty string if no stacks provided', function() {
      c.reopen({content: {stacks: null}});
      expect(c.checkHostOSType()).to.equal('');
    });
    it('os type is valid', function() {
      var osType = 'redhat6';
      c.reopen({
        content: {
          stacks: [
            Em.Object.create({isSelected: true, operatingSystems: [{selected: true, osType: osType}]})
          ]
        }
      });
      expect(c.checkHostOSType(osType, '')).to.equal('');
    });
    it('os type is invalid', function() {
      var osType = 'os2';
      c.reopen({
        content: {
          stacks: [
            Em.Object.create({isSelected: true, operatingSystems: [{selected: true, osType: 'os1'}]})
          ]
        }
      });
      expect(Em.isEmpty(c.checkHostOSType(osType, ''))).to.equal(false);
    });
  });

  describe('#getHostInfoSuccessCallback', function() {

    beforeEach(function() {
      sinon.stub(c, 'parseWarnings', Em.K);
      sinon.stub(c, 'stopRegistration', Em.K);
    });

    afterEach(function() {
      c.parseWarnings.restore();
      c.stopRegistration.restore();
    });

    it('should call _setHostDataWithSkipBootstrap if skipBootstrap is true', function() {
      sinon.spy(c, '_setHostDataWithSkipBootstrap');
      sinon.stub(App, 'get', function(k) {
        if ('skipBootstrap' === k) return true;
        return Em.get(App, k);
      });
      c.reopen({
        bootHosts: [Em.Object.create({name: 'h1'})]
      });
      var jsonData = {items: [{Hosts: {host_name: 'h1'}}]};
      c.getHostInfoSuccessCallback(jsonData);
      expect(c._setHostDataWithSkipBootstrap.calledOnce).to.equal(true);
      App.get.restore();
      c._setHostDataWithSkipBootstrap.restore();
    });

    it('should add repo warnings', function() {

      var jsonData = {items: [{Hosts: {host_name: 'h1'}}]};

      sinon.stub(c, 'checkHostOSType', function() {return 'not_null_value';});
      sinon.stub(c, 'checkHostDiskSpace', Em.K);
      sinon.stub(c, '_setHostDataFromLoadedHostInfo', Em.K);

      sinon.stub(App, 'get', function(k) {
        if ('skipBootstrap' === k) return false;
        return Em.get(App, k);
      });

      c.reopen({
        bootHosts: [Em.Object.create({name: 'h1'})]
      });

      c.getHostInfoSuccessCallback(jsonData);
      expect(c.get('repoCategoryWarnings.length')).to.equal(1);
      expect(c.get('repoCategoryWarnings.firstObject.hostsNames').contains('h1')).to.equal(true);

      c.checkHostOSType.restore();
      c.checkHostDiskSpace.restore();
      c._setHostDataFromLoadedHostInfo.restore();
      App.get.restore();
    });

    it('should add disk warnings', function() {

      var jsonData = {items: [{Hosts: {host_name: 'h1'}}]};

      sinon.stub(c, 'checkHostDiskSpace', function() {return 'not_null_value';});
      sinon.stub(c, 'checkHostOSType', Em.K);
      sinon.stub(c, '_setHostDataFromLoadedHostInfo', Em.K);

      sinon.stub(App, 'get', function(k) {
        if ('skipBootstrap' === k) return false;
        return Em.get(App, k);
      });

      c.reopen({
        bootHosts: [Em.Object.create({name: 'h1'})]
      });

      c.getHostInfoSuccessCallback(jsonData);
      expect(c.get('diskCategoryWarnings.length')).to.equal(1);
      expect(c.get('diskCategoryWarnings.firstObject.hostsNames').contains('h1')).to.equal(true);

      c.checkHostOSType.restore();
      c.checkHostDiskSpace.restore();
      c._setHostDataFromLoadedHostInfo.restore();
      App.get.restore();
    });

  });

  describe('#_setHostDataWithSkipBootstrap', function() {
    it('should set mock-data', function() {
      var host = Em.Object.create({});
      c._setHostDataWithSkipBootstrap(host);
      expect(host.get('cpu')).to.equal(2);
      expect(host.get('memory')).to.equal('2000000.00');
      expect(host.get('disk_info.length')).to.equal(4);
    });
  });

  describe('#_setHostDataFromLoadedHostInfo', function() {
    it('should set data from hostInfo', function() {
      var host = Em.Object.create(),
        hostInfo = {
          Hosts: {
            cpu_count: 2,
            total_mem: 12345,
            os_type: 't1',
            os_arch: 'os1',
            ip: '0.0.0.0',
            disk_info: [
              {mountpoint: '/boot'},
              {mountpoint: '/usr'},
              {mountpoint: '/no-boot'},
              {mountpoint: '/boot'}
            ]
          }
        };
      c._setHostDataFromLoadedHostInfo(host, hostInfo);
      expect(host.get('cpu')).to.equal(2);
      expect(host.get('os_type')).to.equal('t1');
      expect(host.get('os_arch')).to.equal('os1');
      expect(host.get('ip')).to.equal('0.0.0.0');
      expect(host.get('memory')).to.equal('12345.00');
      expect(host.get('disk_info.length')).to.equal(2);
    });
  });

});
