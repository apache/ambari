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
require('controllers/main/admin/security/disable');


describe('App.MainAdminSecurityDisableController', function () {

  var controller = App.MainAdminSecurityDisableController.create({
    serviceConfigTags: null,
    secureProperties: null,
    secureMapping: null
  });


  describe('#resumeCommands()', function () {
    var context = {
      getSecurityDeployCommands: function () {
        return this.testData;
      }
    };

    var mock = {
      setStepsEnable: Em.K,
      setLowerStepsDisable: Em.K
    };

    beforeEach(function () {
      sinon.stub(App.db, "getSecurityDeployCommands", context.getSecurityDeployCommands);
      sinon.stub(App.router, 'get', function () {
        return mock;
      });
    });
    afterEach(function () {
      App.db.getSecurityDeployCommands.restore();
      App.router.get.restore();
    });

    it('commands are absent in local storage', function () {
      App.db.testData = null;
      expect(controller.resumeCommands()).to.be.false;
    });
    it('zero commands in local storage', function () {
      App.db.testData = [];
      expect(controller.resumeCommands()).to.be.false;
    });
    it('one command is present', function () {
      App.db.testData = [
        {
          name: 'command1'
        }
      ];
      controller.get('commands').clear();
      expect(controller.resumeCommands()).to.be.true;
      expect(controller.get('commands').mapProperty('name')).to.eql(['command1']);
    });
    it('command is started and completed', function () {
      App.db.testData = [
        {
          name: 'command1',
          isStarted: true,
          isCompleted: true
        }
      ];
      controller.get('commands').clear();
      expect(controller.resumeCommands()).to.be.true;
      expect(controller.get('commands').mapProperty('name')).to.eql(['command1']);
      expect(controller.get('commands').findProperty('name', 'command1').get('isStarted')).to.be.true;
    });
    it('command is started but not completed', function () {
      App.db.testData = [
        {
          name: 'command1',
          isStarted: true,
          isCompleted: false
        }
      ];
      controller.get('commands').clear();
      expect(controller.resumeCommands()).to.be.true;
      expect(controller.get('commands').mapProperty('name')).to.eql(['command1']);
      expect(controller.get('commands').findProperty('name', 'command1').get('isStarted')).to.be.false;
    });
  });

  describe('#isSubmitDisabled', function () {
    var testCases = [
      {
        title: 'commands is empty',
        commands: [],
        result: false
      },
      {
        title: 'one started command',
        commands: [Em.Object.create({
          isStarted: true
        })],
        result: true
      },
      {
        title: 'one failed command',
        commands: [Em.Object.create({
          isError: true
        })],
        result: false
      },
      {
        title: 'one success command',
        commands: [Em.Object.create({
          isSuccess: true
        })],
        result: false
      },
      {
        title: 'not all commands are success',
        commands: [
          Em.Object.create({
            isSuccess: true
          }),
          Em.Object.create({
            isSuccess: false
          })
        ],
        result: true
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('commands', test.commands);
        expect(controller.get('isSubmitDisabled')).to.equal(test.result);
      });
    });
  });

  describe('#syncStopServicesCommand()', function () {

    it('No background operations', function () {
      controller.set('commands', [Em.Object.create({
        name: 'STOP_SERVICES',
        requestId: 1
      })]);
      controller.syncStopServicesCommand.apply(controller);
      expect(controller.get('commands').findProperty('name', 'STOP_SERVICES').get('requestId')).to.equal(1);
    });
    it('background operation is not running', function () {
      App.router.set('backgroundOperationsController.services', [
        Em.Object.create({
          isRunning: false
        })
      ]);
      controller.syncStopServicesCommand.apply(controller);
      expect(controller.get('commands').findProperty('name', 'STOP_SERVICES').get('requestId')).to.equal(1);
    });
    it('background operation is running but not "Stop All Services"', function () {
      App.router.set('backgroundOperationsController.services', [
        Em.Object.create({
          isRunning: true
        })
      ]);
      controller.syncStopServicesCommand.apply(controller);
      expect(controller.get('commands').findProperty('name', 'STOP_SERVICES').get('requestId')).to.equal(1);
    });
    it('"Stop All Services" operation is running', function () {
      App.router.set('backgroundOperationsController.services', [
        Em.Object.create({
          name: 'Stop All Services',
          isRunning: true,
          id: 2
        })
      ]);
      controller.syncStopServicesCommand.apply(controller);
      expect(controller.get('commands').findProperty('name', 'STOP_SERVICES').get('requestId')).to.equal(2);
    });
  });

  describe('#manageSecureConfigs()', function () {

    beforeEach(function () {
      sinon.stub(controller, "modifySiteConfigs", Em.K);
    });
    afterEach(function () {
      controller.modifySiteConfigs.restore();
    });

    var testCases = [
      {
        title: 'serviceConfigTags, secureProperties, secureMapping are null',
        content: {
          serviceConfigTags: null,
          secureProperties: null,
          secureMapping: null
        }
      },
      {
        title: 'serviceConfigTags is null',
        content: {
          serviceConfigTags: null,
          secureProperties: [],
          secureMapping: []
        }
      },
      {
        title: 'secureProperties is null',
        content: {
          serviceConfigTags: [],
          secureProperties: null,
          secureMapping: []
        }
      },
      {
        title: 'secureMapping is null',
        content: {
          serviceConfigTags: [],
          secureProperties: [],
          secureMapping: null
        }
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('commands', [Em.Object.create({
          name: 'APPLY_CONFIGURATIONS'
        })]);
        controller.set('serviceConfigTags', test.content.serviceConfigTags);
        controller.set('secureProperties', test.content.secureProperties);
        controller.set('secureMapping', test.content.secureMapping);

        expect(controller.manageSecureConfigs()).to.be.false;
        expect(controller.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS').get('isSuccess')).to.be.false;
        expect(controller.get('commands').findProperty('name', 'APPLY_CONFIGURATIONS').get('isError')).to.be.true;
      });
    });
    it('serviceConfigTags is empty', function () {
      controller.set('serviceConfigTags', []);
      controller.set('secureProperties', []);
      controller.set('secureMapping', []);

      expect(controller.manageSecureConfigs()).to.be.true;
    });
    it('serviceConfigTags has cluster-env site', function () {
      controller.set('serviceConfigTags', [
        {
          siteName: 'cluster-env',
          configs: {}
        }
      ]);

      expect(controller.manageSecureConfigs()).to.be.true;
      expect(controller.get('serviceConfigTags').findProperty('siteName', 'cluster-env').configs.security_enabled).to.equal('false');
    });
    it('serviceConfigTags has site.xml', function () {
      controller.set('serviceConfigTags', [
        {
          siteName: 'site'
        }
      ]);
      expect(controller.manageSecureConfigs()).to.be.true;
      expect(controller.modifySiteConfigs.calledOnce).to.be.true;
    });
  });

  describe('#modifySiteConfigs()', function () {
    var testCases = [
      {
        title: '_serviceConfigTags and secureMapping are null',
        content: {
          secureMapping: null,
          _serviceConfigTags: null
        },
        result: false
      },
      {
        title: '_serviceConfigTags is null',
        content: {
          secureMapping: [],
          _serviceConfigTags: null
        },
        result: false
      },
      {
        title: 'secureMapping is null',
        content: {
          secureMapping: null,
          _serviceConfigTags: {}
        },
        result: false
      },
      {
        title: 'secureMapping and _serviceConfigTags are empty',
        content: {
          secureMapping: [],
          _serviceConfigTags: {
            configs: {}
          }
        },
        result: true
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        expect(controller.modifySiteConfigs(test.content.secureMapping, test.content._serviceConfigTags)).to.equal(test.result);
      });
    });
    it('secureMapping doesn\'t contain passed siteName', function () {
      var secureMapping = [];
      var _serviceConfigTags = {
        configs: {
          'config2': true
        },
        siteName: 'site1'
      };
      expect(controller.modifySiteConfigs(secureMapping, _serviceConfigTags)).to.be.true;
      expect(_serviceConfigTags.configs.config2).to.be.true;
    });
    it('secureMapping contain passed siteName but doesn\'t match config name', function () {
      var secureMapping = [
        {
          filename: 'site1.xml'
        }
      ];
      var _serviceConfigTags = {
        configs: {
          'config2': true
        },
        siteName: 'site1'
      };
      expect(controller.modifySiteConfigs(secureMapping, _serviceConfigTags)).to.be.true;
      expect(_serviceConfigTags.configs.config2).to.be.true;
    });
    it('secureMapping contain passed siteName and match config name', function () {
      var secureMapping = [
        {
          filename: 'site1.xml',
          name: 'config2'
        }
      ];
      var _serviceConfigTags = {
        configs: {
          'config2': true
        },
        siteName: 'site1'
      };
      expect(controller.modifySiteConfigs(secureMapping, _serviceConfigTags)).to.be.true;
      expect(_serviceConfigTags.configs.config2).to.be.undefined;
    });
    it('secureMapping contain passed siteName and included in secureConfigValuesMap', function () {
      var secureMapping = [
        {
          filename: 'site1.xml',
          name: 'config2',
          nonSecureValue: 'nonSecureValue'
        }
      ];
      var _serviceConfigTags = {
        configs: {
          'config2': true
        },
        siteName: 'site1'
      };
      controller.set('secureConfigValuesMap', {
        'config2': 'value'
      });
      expect(controller.modifySiteConfigs(secureMapping, _serviceConfigTags)).to.be.true;
      expect(_serviceConfigTags.configs.config2).to.equal('nonSecureValue');
    });
  });
});
