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
require('models/host_component');
require('models/host');

describe('App.MainAdminSecurityProgressController', function () {

  var controller = App.MainAdminSecurityProgressController.create({
    loadClusterConfigs: function () {},
    deleteComponents: function () {}
  });

  describe('#retry()', function () {

    beforeEach(function () {
      sinon.spy(controller, "startCommand");
    });
    afterEach(function () {
      controller.startCommand.restore();
    });

    it('commands are empty', function () {
      controller.set('commands', []);
      controller.retry();
      expect(controller.startCommand.called).to.be.false;
    });

    it('command is successful', function () {
      controller.set('commands', [
        Em.Object.create({
          name: 'test',
          isSuccess: true,
          isError: false,
          isStarted: true
        })
      ]);
      controller.retry();
      expect(controller.startCommand.calledOnce).to.be.false;
      expect(controller.get('commands').findProperty('name', 'test').get('isError')).to.be.false;
      expect(controller.get('commands').findProperty('name', 'test').get('isStarted')).to.be.true;
    });

    it('command is failed', function () {
      controller.set('commands', [
        Em.Object.create({
          name: 'test',
          isSuccess: true,
          isError: true,
          isStarted: true
        })
      ]);
      controller.retry();
      expect(controller.startCommand.calledOnce).to.be.true;
      expect(controller.get('commands').findProperty('name', 'test').get('isError')).to.be.false;
      expect(controller.get('commands').findProperty('name', 'test').get('isStarted')).to.be.false;
    });
  });

  describe('#updateServices()', function () {

    it('commands are empty', function () {
      controller.set('services', [
        {}
      ]);
      controller.set('commands', []);
      controller.updateServices();
      expect(controller.get('services')).to.be.empty;
    });

    it('command doesn\'t have polledData', function () {
      controller.set('services', [
        {}
      ]);
      controller.set('commands', [Em.Object.create({
        label: 'label'
      })]);
      controller.updateServices();
      expect(controller.get('services')).to.be.empty;
    });

    it('command has polledData', function () {
      controller.set('services', [
        {}
      ]);
      controller.set('commands', [Em.Object.create({
        label: 'service1',
        polledData: [
          {
            Tasks: {
              host_name: 'host1'
            }
          }
        ]
      })]);
      controller.updateServices();
      expect(controller.get('services').findProperty('name', 'service1').get('hosts')).to.eql([
        {
          name: 'host1',
          publicName: 'host1',
          logTasks: [
            {
              Tasks: {host_name: 'host1'}
            }
          ]
        }
      ]);
    });
  });

  describe('#setIndex()', function () {
    it('commandArray is empty', function () {
      var commandArray = [];
      controller.setIndex(commandArray);
      expect(commandArray).to.be.empty;
      expect(controller.get('totalSteps')).to.equal(0);
    });
    it('one command in commandArray', function () {
      var commandArray = [
        Em.Object.create({name: 'command1'})
      ];
      controller.setIndex(commandArray);
      expect(commandArray[0].get('index')).to.equal(1);
      expect(controller.get('totalSteps')).to.equal(1);
    });
    it('commands with random indexes', function () {
      var commandArray = [];
      commandArray[3] = Em.Object.create({name: 'command3'});
      commandArray[11] = Em.Object.create({name: 'command11'});
      controller.setIndex(commandArray);
      expect(commandArray[3].get('index')).to.equal(4);
      expect(commandArray[11].get('index')).to.equal(12);
      expect(controller.get('totalSteps')).to.equal(12);
    });
  });

  describe('#startCommand()', function () {

    var command = Em.Object.create({
      start: Em.K
    });

    beforeEach(function () {
      sinon.spy(command, "start");
      sinon.spy(controller, "loadClusterConfigs");
      sinon.spy(controller, "deleteComponents");
      sinon.stub(controller, "saveCommands", Em.K);
    });

    afterEach(function () {
      command.start.restore();
      controller.loadClusterConfigs.restore();
      controller.deleteComponents.restore();
      controller.saveCommands.restore();
    });

    it('number of commands doesn\'t match totalSteps', function () {
      controller.set('commands', []);
      controller.set('totalSteps', 1);
      expect(controller.startCommand()).to.be.false;
    });

    it('commands is empty', function () {
      controller.set('commands', []);
      controller.set('totalSteps', 0);
      expect(controller.startCommand()).to.be.false;
    });

    it('command is started and completed', function () {
      controller.set('commands', [Em.Object.create({
        isStarted: true,
        isCompleted: true
      })]);
      controller.set('totalSteps', 1);
      expect(controller.startCommand()).to.be.false;
    });

    it('command is started and incompleted', function () {
      controller.set('commands', [Em.Object.create({
        isStarted: true,
        isCompleted: false
      })]);
      controller.set('totalSteps', 1);
      expect(controller.startCommand()).to.be.true;
    });

    it('command parameter passed, isPolling is true', function () {
      controller.set('commands', []);
      controller.set('totalSteps', 0);
      command.set('isPolling', true);
      expect(controller.startCommand(command)).to.be.true;
      expect(command.get('isStarted')).to.be.true;
      expect(command.start.calledOnce).to.be.true;
      command.set('isPolling', false);
    });

    it('command parameter passed, name is "APPLY_CONFIGURATIONS"', function () {
      command.set('name', 'APPLY_CONFIGURATIONS');
      expect(controller.startCommand(command)).to.be.true;
      expect(command.get('isStarted')).to.be.true;
      expect(controller.loadClusterConfigs.calledOnce).to.be.true;
    });

    it('command parameter passed, name is "DELETE_ATS"', function () {
      command.set('name', 'DELETE_ATS');

      sinon.stub(App.HostComponent, 'find', function() {
        return [Em.Object.create({
          id: 'APP_TIMELINE_SERVER_ats_host',
          componentName: 'APP_TIMELINE_SERVER',
          hostName: 'ats_host'
        })];
      });
      expect(controller.startCommand(command)).to.be.true;
      expect(command.get('isStarted')).to.be.true;
      expect(controller.deleteComponents.calledWith('APP_TIMELINE_SERVER', 'ats_host')).to.be.true;

      App.HostComponent.find.restore();
    });

  });

  describe('#onCompleteCommand()', function () {

    beforeEach(function () {
      sinon.spy(controller, "moveToNextCommand");
      sinon.stub(controller, "saveCommands", Em.K);
    });
    afterEach(function () {
      controller.moveToNextCommand.restore();
      controller.saveCommands.restore();

    });

    it('number of commands doesn\'t match totalSteps', function () {
      controller.set('commands', []);
      controller.set('totalSteps', 1);
      expect(controller.onCompleteCommand()).to.be.false;
    });
    it('No successful commands', function () {
      controller.set('commands', [Em.Object.create({
        isSuccess: false
      })]);
      controller.set('totalSteps', 1);
      expect(controller.onCompleteCommand()).to.be.false;
    });
    it('No successful commands', function () {
      controller.set('commands', [Em.Object.create({
        isSuccess: false
      })]);
      controller.set('totalSteps', 1);
      expect(controller.onCompleteCommand()).to.be.false;
    });
    it('Last command is successful', function () {
      controller.set('commands', [
        Em.Object.create({
          isSuccess: false
        }),
        Em.Object.create({
          isSuccess: true
        })
      ]);
      controller.set('totalSteps', 2);
      expect(controller.onCompleteCommand()).to.be.false;
    });
    it('all commands are successful', function () {
      controller.set('commands', [
        Em.Object.create({
          isSuccess: true,
          name: 'command1'
        }),
        Em.Object.create({
          isSuccess: false,
          name: 'command2'
        })
      ]);
      controller.set('totalSteps', 2);
      expect(controller.onCompleteCommand()).to.be.true;
      expect(controller.moveToNextCommand.calledWith(Em.Object.create({
        isSuccess: false,
        name: 'command2'
      }))).to.be.true;
    });
  });

  describe('#moveToNextCommand()', function () {

    beforeEach(function () {
      sinon.spy(controller, "startCommand");
    });
    afterEach(function () {
      controller.startCommand.restore();
    });

    it('No commands present', function () {
      controller.set('commands', []);
      expect(controller.moveToNextCommand()).to.be.false;
    });
    it('Only started command present', function () {
      controller.set('commands', [
        Em.Object.create({
          isStarted: true
        })
      ]);
      expect(controller.moveToNextCommand()).to.be.false;
    });
    it('Command is not started', function () {
      controller.set('commands', [
        Em.Object.create({
          isStarted: false,
          name: 'command1'
        })
      ]);
      expect(controller.moveToNextCommand()).to.be.true;
      expect(controller.startCommand.calledWith(Em.Object.create({
        isStarted: false,
        name: 'command1'
      }))).to.be.true;
    });
    it('Next command provide as argument', function () {
      var nextCommand = Em.Object.create({
        isStarted: false,
        name: 'command2'
      });
      expect(controller.moveToNextCommand(nextCommand)).to.be.true;
      expect(controller.startCommand.calledWith(Em.Object.create({
        isStarted: false,
        name: 'command2'
      }))).to.be.true;
    });
  });

  describe('#setServiceTagNames()', function () {
    var testCases = [
      {
        title: 'configs is empty object',
        content: {
          secureService: {},
          configs: {}
        },
        result: undefined
      },
      {
        title: 'secureService.sites is null',
        content: {
          secureService: {
            sites: null
          },
          configs: {
            site1: {}
          }
        },
        result: undefined
      },
      {
        title: 'secureService.sites doesn\'t contain required config tag',
        content: {
          secureService: {
            sites: []
          },
          configs: {
            site1: {}
          }
        },
        result: undefined
      },
      {
        title: 'secureService.sites contains required config tag',
        content: {
          secureService: {
            sites: ['site1']
          },
          configs: {
            site1: {
              tag: 'tag1'
            }
          }
        },
        result: {
          siteName: 'site1',
          tagName: 'tag1',
          newTagName: null,
          configs: {}
        }
      }
    ];
    testCases.forEach(function (test) {
      it(test.title, function () {
        expect(controller.setServiceTagNames(test.content.secureService, test.content.configs)).to.eql(test.result);
      });
    });
  });

  describe('#modifyConfigsForSecure', function () {
    var cfg = {
      properties: {
        'ui.childopts': 'value1',
        'supervisor.childopts': 'value2',
        'common_property': 'value4'
      }
    };
    var siteName = 'storm-site';
    var result = {
      'ui.childopts': 'value1 -Djava.security.auth.login.config=/etc/storm/conf/storm_jaas.conf',
      'supervisor.childopts': 'value2 -Djava.security.auth.login.config=/etc/storm/conf/storm_jaas.conf',
      'common_property': 'value4'
    };
    var propertiesToUpdate = [
      {
        siteName: 'storm-site',
        name: 'ui.childopts',
        append: ' -Djava.security.auth.login.config=/etc/storm/conf/storm_jaas.conf'
      },
      {
        siteName: 'storm-site',
        name: 'supervisor.childopts',
        append: ' -Djava.security.auth.login.config=/etc/storm/conf/storm_jaas.conf'
      }
    ];
    it("should change some storm sonfigs", function () {
      controller.set('propertiesToUpdate', propertiesToUpdate);
      expect(controller.modifyConfigsForSecure(siteName, cfg)).to.eql(result);
    });
  });
});
