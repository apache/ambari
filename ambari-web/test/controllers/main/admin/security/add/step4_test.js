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
require('controllers/main/admin/security/security_progress_controller');
require('controllers/main/admin/security/add/step4');
require('mixins/wizard/addSecurityConfigs');
require('utils/polling');
require('models/cluster_states');
require('models/service');

var controller;

describe('App.MainAdminSecurityAddStep4Controller', function () {

  beforeEach(function () {
    controller = App.MainAdminSecurityAddStep4Controller.create({
      content: {},
      commands: [],
      enableSubmit: function () {
        this._super()
      },
      secureMapping: [],
      secureProperties: [],
      secureServices: []
    });
  });

  describe('#isBackBtnDisabled', function () {
    it('commands have error', function () {
      controller.set('commands', [Em.Object.create({
        isError: true
      })]);
      expect(controller.get('isBackBtnDisabled')).to.be.false;
    });
    it('commands do not have error', function () {
      controller.set('commands', [Em.Object.create({
        isError: false
      })]);
      expect(controller.get('isBackBtnDisabled')).to.be.true;
    });
  });

  describe('#isSecurityApplied', function () {
    var testCases = [
      {
        title: 'No START_SERVICES command',
        commands: [],
        result: false
      },
      {
        title: 'START_SERVICES is not success',
        commands: [Em.Object.create({
          name: 'START_SERVICES',
          isSuccess: false
        })],
        result: false
      },
      {
        title: 'START_SERVICES is success',
        commands: [Em.Object.create({
          name: 'START_SERVICES',
          isSuccess: true
        })],
        result: true
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('commands', test.commands);
        expect(controller.get('isSecurityApplied')).to.equal(test.result);
      });
    });
  });

  describe('#enableSubmit()', function () {
    var mock = {
      setStepsEnable: Em.K,
      setLowerStepsDisable: Em.K
    };

    beforeEach(function () {
      sinon.stub(App.router, 'get', function () {
        return mock;
      });
      sinon.spy(mock, 'setStepsEnable');
      sinon.spy(mock, 'setLowerStepsDisable');
    });
    afterEach(function () {
      App.router.get.restore();
      mock.setStepsEnable.restore();
      mock.setLowerStepsDisable.restore();
    });

    it('Command has error', function () {
      controller.set('commands', [Em.Object.create({
        isError: true
      })]);
      controller.enableSubmit();
      expect(controller.get('isSubmitDisabled')).to.be.false;
      expect(mock.setStepsEnable.calledOnce).to.be.true;
    });
    it('Command is successful', function () {
      controller.set('commands', [Em.Object.create({
        isSuccess: true
      })]);
      controller.enableSubmit();
      expect(controller.get('isSubmitDisabled')).to.be.false;
    });
    it('Command is in progress', function () {
      controller.set('commands', [Em.Object.create()]);
      controller.enableSubmit();
      expect(controller.get('isSubmitDisabled')).to.be.true;
      expect(mock.setLowerStepsDisable.calledWith(4)).to.be.true;
    });
  });

  describe('#clearStep()', function () {
    it('Clear step info', function () {
      controller.set('commands', [Em.Object.create()]);
      controller.set('isSubmitDisabled', false);
      controller.set('serviceConfigTags', [
        {}
      ]);
      controller.clearStep();
      expect(controller.get('isSubmitDisabled')).to.be.true;
      expect(controller.get('commands')).to.be.empty;
      expect(controller.get('serviceConfigTags')).to.be.empty;
    });
  });

  describe('#loadCommands()', function () {

    before(function () {
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
    });

    after(function () {
      App.clusterStatus.setClusterStatus.restore();
    });

    var tests = Em.A([
      {
        doesATSSupportKerberos: true,
        isATSInstalled: true,
        e: {
          l: 3,
          d: false
        }
      },
      {
        doesATSSupportKerberos: true,
        isATSInstalled: false,
        e: {
          l: 3,
          d: false
        }
      },
      {
        doesATSSupportKerberos: false,
        isATSInstalled: true,
        e: {
          l: 4,
          d: true
        }
      },
      {
        doesATSSupportKerberos: false,
        isATSInstalled: false,
        e: {
          l: 3,
          d: false
        }
      }
    ]);

    tests.forEach(function (test) {
      it('doesATSSupportKerberos ' + test.doesATSSupportKerberos.toString() + ', isATSInstalled ' + test.isATSInstalled.toString(), function () {
        sinon.stub(App, 'get', function (k) {
          if ('doesATSSupportKerberos' === k) return test.doesATSSupportKerberos;
          return Em.get(App, k);
        });
        controller.set('content.isATSInstalled', test.isATSInstalled);
        controller.loadCommands();
        App.get.restore();
        expect(controller.get('commands.length')).to.equal(test.e.l);
        expect(controller.get('commands').someProperty('name', 'DELETE_ATS')).to.equal(test.e.d);
      });
    });

  });

  describe('#loadStep()', function () {

    beforeEach(function () {
      sinon.stub(controller, 'clearStep', Em.K);
      sinon.stub(controller, 'prepareSecureConfigs', Em.K);
    });
    afterEach(function () {
      controller.clearStep.restore();
      controller.prepareSecureConfigs.restore();
      controller.resumeSavedCommands.restore();
    });

    it('Resume saved commands', function () {
      sinon.stub(controller, 'resumeSavedCommands', function () {
        return true;
      });

      controller.loadStep();
      expect(controller.clearStep.calledOnce).to.be.true;
      expect(controller.prepareSecureConfigs.calledOnce).to.be.true;
      expect(controller.resumeSavedCommands.calledOnce).to.be.true;
    });
    it('No saved commands', function () {
      sinon.stub(controller, 'resumeSavedCommands', function () {
        return false;
      });
      sinon.stub(controller, 'loadCommands', Em.K);
      sinon.stub(controller, 'addInfoToCommands', Em.K);
      sinon.stub(controller, 'syncStopServicesOperation', Em.K);
      sinon.stub(controller, 'addObserverToCommands', Em.K);
      sinon.stub(controller, 'moveToNextCommand', Em.K);

      controller.loadStep();
      expect(controller.clearStep.calledOnce).to.be.true;
      expect(controller.prepareSecureConfigs.calledOnce).to.be.true;
      expect(controller.resumeSavedCommands.calledOnce).to.be.true;

      controller.loadCommands.restore();
      controller.addInfoToCommands.restore();
      controller.syncStopServicesOperation.restore();
      controller.addObserverToCommands.restore();
      controller.moveToNextCommand.restore();
    });
  });

  describe('#syncStopServicesOperation()', function () {

    afterEach(function () {
      App.router.get.restore();
    });

    it('No running operations', function () {
      sinon.stub(App.router, 'get', function () {
        return [];
      });

      expect(controller.syncStopServicesOperation()).to.be.false;
    });
    it('Running operation is not Stop All Services', function () {
      sinon.stub(App.router, 'get', function () {
        return [Em.Object.create({isRunning: true})];
      });

      expect(controller.syncStopServicesOperation()).to.be.false;
    });
    it('No STOP_SERVICES in commands', function () {
      sinon.stub(App.router, 'get', function () {
        return [Em.Object.create({
          isRunning: true,
          name: 'Stop All Services'
        })];
      });
      controller.set('commands', []);

      expect(controller.syncStopServicesOperation()).to.be.false;
    });
    it('Sync stop services commands', function () {
      sinon.stub(App.router, 'get', function () {
        return [Em.Object.create({
          isRunning: true,
          name: 'Stop All Services',
          id: 1
        })];
      });
      controller.set('commands', [Em.Object.create({
        name: 'STOP_SERVICES'
      })]);

      expect(controller.syncStopServicesOperation()).to.be.true;
      expect(controller.get('commands').findProperty('name', 'STOP_SERVICES').get('requestId')).to.equal(1);
    });
  });

  describe('#resumeSavedCommands()', function () {

    beforeEach(function () {
      sinon.stub(controller, 'addObserverToCommands', Em.K);
      sinon.stub(controller, 'moveToNextCommand', Em.K);
      controller.set('commands', []);
    });
    afterEach(function () {
      controller.moveToNextCommand.restore();
      controller.addObserverToCommands.restore();
      App.db.getSecurityDeployCommands.restore();
    });


    it('Commands is null', function () {
      sinon.stub(App.db, 'getSecurityDeployCommands', function () {
        return null;
      });
      expect(controller.resumeSavedCommands()).to.be.false;
    });
    it('Commands is empty', function () {
      sinon.stub(App.db, 'getSecurityDeployCommands', function () {
        return [];
      });
      expect(controller.resumeSavedCommands()).to.be.false;
    });
    it('Command has error', function () {
      sinon.stub(App.db, 'getSecurityDeployCommands', function () {
        return [
          {
            isError: true,
            name: 'command1'
          }
        ];
      });
      expect(controller.resumeSavedCommands()).to.be.true;
      expect(controller.get('commands').mapProperty('name')).to.eql(['command1']);
      expect(controller.addObserverToCommands.calledOnce).to.be.true;
    });
    it('Command in progress', function () {
      sinon.stub(App.db, 'getSecurityDeployCommands', function () {
        return [
          {
            isStarted: true,
            isCompleted: false,
            name: 'command1'
          }
        ];
      });
      expect(controller.resumeSavedCommands()).to.be.true;
      expect(controller.get('commands').mapProperty('name')).to.eql(['command1']);
      expect(controller.get('commands').findProperty('name', 'command1').get('isStarted')).to.be.false;
      expect(controller.addObserverToCommands.calledOnce).to.be.true;
      expect(controller.moveToNextCommand.calledOnce).to.be.true;
    });
    it('Command completed', function () {
      sinon.stub(App.db, 'getSecurityDeployCommands', function () {
        return [
          {
            isCompleted: true,
            name: 'command1'
          }
        ];
      });
      expect(controller.resumeSavedCommands()).to.be.true;
      expect(controller.get('commands').mapProperty('name')).to.eql(['command1']);
      expect(controller.addObserverToCommands.calledOnce).to.be.true;
      expect(controller.moveToNextCommand.calledOnce).to.be.true;
    });
  });

  describe('#manageSecureConfigs()', function () {

    beforeEach(function () {
      sinon.stub(controller, 'setPrincipalValue', Em.K);
    });
    afterEach(function () {
      controller.setPrincipalValue.restore();
    });

    it('serviceConfigTags is null', function () {
      sinon.stub(controller, 'onJsError', Em.K);
      controller.set('serviceConfigTags', null);
      controller.set('configs', [
        {}
      ]);
      controller.set('commands', [Em.Object.create({
        name: 'APPLY_CONFIGURATIONS'
      })]);

      expect(controller.manageSecureConfigs()).to.be.false;
      expect(controller.onJsError.calledOnce).to.be.true;
      expect(controller.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS').get('isSuccess')).to.be.false;
      expect(controller.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS').get('isError')).to.be.true;

      controller.onJsError.restore();
    });
    it('Add configs from site-*.xml', function () {
      controller.set('serviceConfigTags', [
        {
          siteName: 'site1',
          configs: {}
        }
      ]);
      controller.set('configs', [
        {
          name: 'config1',
          value: "value1",
          filename: 'site1.xml'
        }
      ]);

      expect(controller.manageSecureConfigs()).to.be.true;
      expect(controller.get('serviceConfigTags')[0].configs).to.eql({'config1': 'value1'});
    });
    it('Add configs from global.xml, config matches "_hosts"', function () {
      controller.reopen({
        secureConfigs: [
          {
            serviceName: 'service1',
            name: 'config1'
          }
        ]
      });

      controller.set('serviceConfigTags', [
        {
          siteName: 'global',
          configs: {}
        }
      ]);
      controller.set('globalProperties', [
        {
          name: 'config1_hosts',
          value: "value1",
          filename: 'site1.xml'
        }
      ]);

      expect(controller.manageSecureConfigs()).to.be.true;
      expect(controller.get('serviceConfigTags')[0].configs).to.eql({});
      expect(controller.setPrincipalValue.calledWith('service1', 'config1')).to.be.true;
    });
  });

  describe('#deleteComponents()', function () {
    it('Send ajax', function () {
      sinon.stub(App.ajax, 'send', Em.K);

      controller.deleteComponents('comp1', 'host1');
      expect(App.ajax.send.calledOnce).to.be.true;

      App.ajax.send.restore();
    });
  });

  describe('#onDeleteComplete()', function () {
    it('', function () {
      controller.set('commands', [Em.Object.create({
        name: 'DELETE_ATS'
      })]);

      controller.onDeleteComplete();
      expect(controller.get('commands').findProperty('name', 'DELETE_ATS').get('isError')).to.be.false;
      expect(controller.get('commands').findProperty('name', 'DELETE_ATS').get('isSuccess')).to.be.true;
    });
  });

  describe('#onJsError()', function () {
    it('Show popup', function () {
      sinon.stub(App.ModalPopup, 'show', Em.K);

      controller.onJsError();
      expect(App.ModalPopup.show.calledOnce).to.be.true;

      App.ModalPopup.show.restore();
    });
  });

});
