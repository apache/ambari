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
require('models/host');
require('models/service');
require('models/host_component');
require('mappers/server_data_mapper');
require('views/main/host/summary');

var mainHostSummaryView;
var modelSetup = require('test/init_model_test');

describe('App.MainHostSummaryView', function() {

  beforeEach(function() {
    modelSetup.setupStackServiceComponent();
    mainHostSummaryView = App.MainHostSummaryView.create({content: Em.Object.create()});
  });

  afterEach(function(){
    modelSetup.cleanStackServiceComponent();
  });

  describe("#installedServices", function() {

    beforeEach(function() {
      sinon.stub(App.Service, 'find').returns([Em.Object.create({serviceName: 'S1'})]);
    });
    afterEach(function() {
      App.Service.find.restore();
    });

    it("should return installed services", function() {
      expect(mainHostSummaryView.get('installedServices')).to.eql(['S1']);
    });
  });

  describe('#sortedComponentsFormatter()', function() {

    var tests = Em.A([
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({isMaster: false, isSlave: true, componentName: 'B'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'A'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'C'}),
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'D'})
          ])
        }),
        m: 'List of masters, slaves and clients',
        e: ['A', 'C', 'B']
      },
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({isMaster: false, isSlave: true, componentName: 'B'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'A'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'C'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'D'})
          ])
        }),
        m: 'List of masters and slaves',
        e: ['A', 'C', 'D', 'B']
      },
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'B'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'A'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'C'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'D'})
          ])
        }),
        m: 'List of masters',
        e: ['B', 'A', 'C', 'D']
      },
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({isMaster: false, isSlave: true, componentName: 'B'}),
            Em.Object.create({isMaster: false, isSlave: true, componentName: 'A'}),
            Em.Object.create({isMaster: false, isSlave: true, componentName: 'C'}),
            Em.Object.create({isMaster: false, isSlave: true, componentName: 'D'})
          ])
        }),
        m: 'List of slaves',
        e: ['B', 'A', 'C', 'D']
      },
      {
        content: Em.Object.create({
          hostComponents: Em.A([])
        }),
        m: 'Empty list',
        e: []
      },
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'B'}),
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'A'}),
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'C'}),
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'D'})
          ])
        }),
        m: 'List of clients',
        e: []
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        test.content.get('hostComponents').forEach(function(component) {
          component.set('id', component.get('componentName'));
        });
        mainHostSummaryView.set('sortedComponents', []);
        mainHostSummaryView.set('content', test.content);
        mainHostSummaryView.sortedComponentsFormatter();
        expect(mainHostSummaryView.get('sortedComponents').mapProperty('componentName')).to.eql(test.e);
      });
    });

  });

  describe('#clients', function() {

    var tests = Em.A([
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({isMaster: false, isSlave: true, componentName: 'B'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'A'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'C'}),
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'D'})
          ])
        }),
        m: 'List of masters, slaves and clients',
        e: ['D']
      },
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({isMaster: false, isSlave: true, componentName: 'B'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'A'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'C'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'D'})
          ])
        }),
        m: 'List of masters and slaves',
        e: []
      },
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'B'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'A'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'C'}),
            Em.Object.create({isMaster: true, isSlave: false, componentName: 'D'})
          ])
        }),
        m: 'List of masters',
        e: []
      },
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({isMaster: false, isSlave: true, componentName: 'B'}),
            Em.Object.create({isMaster: false, isSlave: true, componentName: 'A'}),
            Em.Object.create({isMaster: false, isSlave: true, componentName: 'C'}),
            Em.Object.create({isMaster: false, isSlave: true, componentName: 'D'})
          ])
        }),
        m: 'List of slaves',
        e: []
      },
      {
        content: Em.Object.create({
          hostComponents: Em.A([])
        }),
        m: 'Empty list',
        e: []
      },
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'B'}),
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'A'}),
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'C'}),
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'D'})
          ])
        }),
        m: 'List of clients',
        e: ['B', 'A', 'C', 'D']
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        mainHostSummaryView.set('content', test.content);
        expect(mainHostSummaryView.get('clients').mapProperty('componentName')).to.eql(test.e);
      });
    });

    it('should set isInstallFailed for clients with INIT and INSTALL_FAILED workStatus', function() {
      mainHostSummaryView.set('content', Em.Object.create({
        hostComponents: [
          Em.Object.create({isMaster: false, isSlave: false, componentName: 'B', workStatus: 'INIT'}),
          Em.Object.create({isMaster: false, isSlave: false, componentName: 'A', workStatus: 'INSTALLED'}),
          Em.Object.create({isMaster: false, isSlave: false, componentName: 'C', workStatus: 'INSTALL_FAILED'}),
          Em.Object.create({isMaster: false, isSlave: false, componentName: 'D', workStatus: 'INSTALLING'})
        ]
      }));
      expect(mainHostSummaryView.get('clients').filterProperty('isInstallFailed', true).mapProperty('componentName')).to.eql(['B', 'C']);
      expect(mainHostSummaryView.get('clients').filterProperty('isInstallFailed', false).mapProperty('componentName')).to.eql(['A', 'D']);
    });

  });

  describe('#areClientWithStaleConfigs', function() {

    var tests = Em.A([
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'D', staleConfigs: true}),
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'C', staleConfigs: false})
          ])
        }),
        m: 'Some clients with stale configs',
        e: true
      },
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'D', staleConfigs: false}),
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'C', staleConfigs: false})
          ])
        }),
        m: 'No clients with stale configs',
        e: false
      },
      {
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'D', staleConfigs: true}),
            Em.Object.create({isMaster: false, isSlave: false, componentName: 'C', staleConfigs: true})
          ])
        }),
        m: 'All clients with stale configs',
        e: true
      },
      {
        content: Em.Object.create({
          hostComponents: Em.A([])
        }),
        m: 'Empty list',
        e: false
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        mainHostSummaryView.set('content', test.content);
        expect(mainHostSummaryView.get('areClientWithStaleConfigs')).to.equal(test.e);
      });
    });

  });

  describe('#isAddComponent', function() {

    var tests = Em.A([
      {content: {healthClass: 'health-status-DEAD-YELLOW', hostComponents: Em.A([])}, e: false},
      {content: {healthClass: 'OTHER_VALUE', hostComponents: Em.A([])}, e: true}
    ]);

    tests.forEach(function(test) {
      it(test.content.healthClass, function() {
        mainHostSummaryView.set('content', test.content);
        expect(mainHostSummaryView.get('isAddComponent')).to.equal(test.e);
      });
    });

  });

  describe('#addableComponents', function() {

    beforeEach(function() {
      this.mock = sinon.stub(App.StackServiceComponent, 'find');
      sinon.stub(mainHostSummaryView, 'hasCardinalityConflict').returns(false);
    });
    afterEach(function() {
      App.StackServiceComponent.find.restore();
      mainHostSummaryView.hasCardinalityConflict.restore();
    });

    var tests = Em.A([
      {
        addableToHostComponents: [
          Em.Object.create({
            serviceName: 'HDFS',
            componentName: 'DATANODE',
            isAddableToHost: true
          }),
          Em.Object.create({
            serviceName: 'HDFS',
            componentName: 'HDFS_CLIENT',
            isAddableToHost: true
          })
        ],
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({
              componentName: 'HDFS_CLIENT'
            })
          ])
        }),
        services: ['HDFS'],
        e: ['DATANODE'],
        m: 'some components are already installed'
      },
      {
        addableToHostComponents: [
          Em.Object.create({
            serviceName: 'HDFS',
            componentName: 'HDFS_CLIENT',
            isAddableToHost: true
          })
        ],
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({
              componentName: 'HDFS_CLIENT'
            })
          ])
        }),
        services: ['HDFS'],
        e: [],
        m: 'all components are already installed'
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        this.mock.returns(test.addableToHostComponents);
        mainHostSummaryView.set('content', test.content);
        mainHostSummaryView.reopen({
          installedServices: test.services
        });
        mainHostSummaryView.propertyDidChange('addableComponents');
        expect(mainHostSummaryView.get('addableComponents').mapProperty('componentName')).to.eql(test.e);
      });
    });
  });

  describe('#areClientsNotInstalled', function () {

    var cases = [
      {
        clients: [
          {
            isInstallFailed: true
          }
        ],
        installableClientComponents: [],
        areClientsNotInstalled: true,
        title: 'some clients failed to install, no clients to add'
      },
      {
        clients: [
          {
            isInstallFailed: false
          }
        ],
        installableClientComponents: [{}],
        areClientsNotInstalled: true,
        title: 'no clients failed to install, some clients to add'
      },
      {
        clients: [
          {
            isInstallFailed: true
          }
        ],
        installableClientComponents: [{}],
        areClientsNotInstalled: true,
        title: 'some clients failed to install, some clients to add'
      },
      {
        clients: [
          {
            isInstallFailed: false
          }
        ],
        installableClientComponents: [],
        areClientsNotInstalled: false,
        title: 'no clients failed to install, no clients to add'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        mainHostSummaryView.reopen({
          clients: item.clients,
          installableClientComponents: item.installableClientComponents
        });
        expect(mainHostSummaryView.get('areClientsNotInstalled')).to.equal(item.areClientsNotInstalled);
      });
    });

  });

  describe('#notInstalledClientComponents', function () {

    it('should concat not added clients and the ones that failed to install', function () {
      mainHostSummaryView.reopen({
        clients: [
          Em.Object.create({
            componentName: 'c0',
            workStatus: 'INIT'
          }),
          Em.Object.create({
            componentName: 'c1',
            workStatus: 'INSTALL_FAILED'
          }),
          Em.Object.create({
            componentName: 'c2',
            workStatus: 'INSTALLED'
          })
        ],
        installableClientComponents: [
          Em.Object.create({
            componentName: 'c3'
          })
        ]
      });
      expect(mainHostSummaryView.get('notInstalledClientComponents')).to.eql([
        Em.Object.create({
          componentName: 'c0',
          workStatus: 'INIT'
        }),
        Em.Object.create({
          componentName: 'c1',
          workStatus: 'INSTALL_FAILED'
        }),
        Em.Object.create({
          componentName: 'c3'
        })
      ]);
    });

  });

  describe("#needToRestartMessage", function() {

    it("one component", function() {
      var expected = Em.I18n.t('hosts.host.details.needToRestart').format(1, Em.I18n.t('common.component').toLowerCase());
      mainHostSummaryView.set('content', Em.Object.create({
        componentsWithStaleConfigsCount: 1
      }));
      expect(mainHostSummaryView.get('needToRestartMessage')).to.equal(expected);
    });

    it("multiple components", function() {
      var expected = Em.I18n.t('hosts.host.details.needToRestart').format(2, Em.I18n.t('common.components').toLowerCase());
      mainHostSummaryView.set('content', Em.Object.create({
        componentsWithStaleConfigsCount: 2
      }));
      expect(mainHostSummaryView.get('needToRestartMessage')).to.equal(expected);
    });

  });

  describe("#redrawComponents()", function() {

    beforeEach(function() {
      this.mock = sinon.stub(App.router, 'get');
      sinon.stub(mainHostSummaryView, 'sortedComponentsFormatter');
      sinon.stub(App.router, 'set');
    });
    afterEach(function() {
      this.mock.restore();
      mainHostSummaryView.sortedComponentsFormatter.restore();
      App.router.set.restore();
    });

    it("redrawComponents is false", function() {
      this.mock.returns(false);
      mainHostSummaryView.redrawComponents();
      expect(mainHostSummaryView.sortedComponentsFormatter.called).to.be.false;
    });

    it("redrawComponents is true", function() {
      this.mock.returns(true);
      mainHostSummaryView.redrawComponents();
      expect(mainHostSummaryView.sortedComponentsFormatter.calledOnce).to.be.true;
      expect(mainHostSummaryView.get('sorteComponents')).to.be.empty;
      expect(App.router.set.calledWith('mainHostDetailsController.redrawComponents', false)).to.be.true;
    });

  });

  describe("#willInsertElement()", function() {

    beforeEach(function() {
      sinon.stub(mainHostSummaryView, 'sortedComponentsFormatter');
      sinon.stub(mainHostSummaryView, 'addObserver');
    });
    afterEach(function() {
      mainHostSummaryView.sortedComponentsFormatter.restore();
      mainHostSummaryView.addObserver.restore();
    });

    it("sortedComponentsFormatter should be called ", function() {
      mainHostSummaryView.willInsertElement();
      expect(mainHostSummaryView.sortedComponentsFormatter.calledOnce).to.be.true;
      expect(mainHostSummaryView.addObserver.calledWith('content.hostComponents.length', mainHostSummaryView, 'sortedComponentsFormatter')).to.be.true;
      expect(mainHostSummaryView.get('sortedComponents')).to.be.empty;
    });
  });

  describe("#didInsertElement()", function() {

    beforeEach(function() {
      sinon.stub(mainHostSummaryView, 'addToolTip');
    });
    afterEach(function() {
      mainHostSummaryView.addToolTip.restore();
    });

    it("addToolTip should be called", function() {
      mainHostSummaryView.didInsertElement();
      expect(mainHostSummaryView.addToolTip.calledOnce).to.be.true;
    });
  });

  describe("#addToolTip()", function() {

    beforeEach(function() {
      sinon.stub(App, 'tooltip');
      mainHostSummaryView.removeObserver('addComponentDisabled', mainHostSummaryView, 'addToolTip');
    });
    afterEach(function() {
      App.tooltip.restore();
    });

    it("addComponentDisabled is false ", function() {
      mainHostSummaryView.reopen({
        addComponentDisabled: false
      });
      mainHostSummaryView.addToolTip();
      expect(App.tooltip.called).to.be.false;
    });

    it("addComponentDisabled is true ", function() {
      mainHostSummaryView.reopen({
        addComponentDisabled: true
      });
      mainHostSummaryView.addToolTip();
      expect(App.tooltip.called).to.be.true;
    });

  });

  describe("#installableClientComponents", function() {

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          isClient: true,
          serviceName: 'S1',
          componentName: 'C1'
        }),
        Em.Object.create({
          isClient: true,
          serviceName: 'S1',
          componentName: 'C2'
        }),
        Em.Object.create({
          isClient: true,
          serviceName: 'S2',
          componentName: 'C1'
        })
      ]);
    });
    afterEach(function() {
      App.StackServiceComponent.find.restore();
    });

    it("should return installable client components", function() {
      mainHostSummaryView.reopen({
        installedServices: ['S1'],
        clients: [
          Em.Object.create({componentName: 'C2'})
        ]
      });
      mainHostSummaryView.propertyDidChange('installableClientComponents');
      expect(mainHostSummaryView.get('installableClientComponents').mapProperty('componentName')).to.eql(['C1']);
    });
  });

  describe("#hasCardinalityConflict()", function () {

    beforeEach(function() {
      this.mockSlave = sinon.stub(App.SlaveComponent, 'find');
      this.mockStack = sinon.stub(App.StackServiceComponent, 'find');
    });

    afterEach(function() {
      this.mockSlave.restore();
      this.mockStack.restore();
    });

    it("totalCount equal to maxToInstall", function() {
      this.mockSlave.returns(Em.Object.create({
        totalCount: 1
      }));
      this.mockStack.returns(Em.Object.create({
        maxToInstall: 1
      }));
      expect(mainHostSummaryView.hasCardinalityConflict('C1')).to.be.true;
    });

    it("totalCount more than maxToInstall", function() {
      this.mockSlave.returns(Em.Object.create({
        totalCount: 2
      }));
      this.mockStack.returns(Em.Object.create({
        maxToInstall: 1
      }));
      expect(mainHostSummaryView.hasCardinalityConflict('C1')).to.be.true;
    });

    it("totalCount less than maxToInstall", function() {
      this.mockSlave.returns(Em.Object.create({
        totalCount: 0
      }));
      this.mockStack.returns(Em.Object.create({
        maxToInstall: 1
      }));
      expect(mainHostSummaryView.hasCardinalityConflict('C1')).to.be.false;
    });
  });

  describe("#installClients()", function () {

    beforeEach(function () {
      var controller = {installClients: Em.K};
      sinon.spy(controller, 'installClients');
      mainHostSummaryView.set('controller', controller);
      mainHostSummaryView.reopen({'notInstalledClientComponents': [1,2,3]});
    });

    afterEach(function () {
      mainHostSummaryView.get('controller.installClients').restore();
    });

    it("should call installClients method from controller", function () {
      mainHostSummaryView.installClients();
      expect(mainHostSummaryView.get('controller.installClients').calledWith([1,2,3])).to.be.true;
    });
  });

  describe("#reinstallClients()", function () {

    beforeEach(function () {
      var controller = {installClients: Em.K};
      sinon.spy(controller, 'installClients');
      mainHostSummaryView.set('controller', controller);
      mainHostSummaryView.reopen({'installFailedClients': [1,2,3]});
    });

    afterEach(function () {
      mainHostSummaryView.get('controller.installClients').restore();
    });

    it("should call installClients method from controller", function () {
      mainHostSummaryView.reinstallClients();
      expect(mainHostSummaryView.get('controller.installClients').calledWith([1,2,3])).to.be.true;
    });
  });

  describe("#timeSinceHeartBeat", function () {

    beforeEach(function() {
      sinon.stub($, 'timeago').returns('1');
    });

    afterEach(function() {
      $.timeago.restore();
    });

    it("rawLastHeartBeatTime = null", function() {
      mainHostSummaryView.set('content.rawLastHeartBeatTime', null);
      mainHostSummaryView.propertyDidChange('timeSinceHeartBeat');
      expect(mainHostSummaryView.get('timeSinceHeartBeat')).to.be.empty;
    });

    it("rawLastHeartBeatTime = 1", function() {
      mainHostSummaryView.set('content.rawLastHeartBeatTime', '1');
      mainHostSummaryView.propertyDidChange('timeSinceHeartBeat');
      expect(mainHostSummaryView.get('timeSinceHeartBeat')).to.be.equal('1');
    });
  });

  describe("#clientsWithCustomCommands", function () {

    beforeEach(function() {
      this.mockComponents = sinon.stub(App.StackServiceComponent, 'find');
    });

    afterEach(function() {
      this.mockComponents.restore();
    });

    var testCases = [
      {
        component: Em.Object.create(),
        clients: [],
        expected: []
      },
      {
        component: Em.Object.create(),
        clients: [
          Em.Object.create({componentName: 'KERBEROS_CLIENT'})
        ],
        expected: []
      },
      {
        component: Em.Object.create({customCommands: []}),
        clients: [
          Em.Object.create({componentName: 'C1'})
        ],
        expected: []
      },
      {
        component: Em.Object.create({customCommands: ['cmd1']}),
        clients: [
          Em.Object.create({
            hostName: 'host1',
            displayName: 'dn1',
            componentName: 'C1',
            service: Em.Object.create({serviceName: 'S1'})
          })
        ],
        expected: [{
          label: 'dn1',
          commands: [
            {
              label: Em.I18n.t('services.service.actions.run.executeCustomCommand.menu').format('cmd1'),
              service: "S1",
              hosts: 'host1',
              component: 'C1',
              command: 'cmd1'
            }
          ]
        }]
      }
    ];

    testCases.forEach(function(test) {
      it("component = " + JSON.stringify(test.component) +
         " clients = " + JSON.stringify(test.clients), function() {
        this.mockComponents.returns(test.component);
        mainHostSummaryView.reopen({
          clients: test.clients
        });
        mainHostSummaryView.propertyDidChange('clientsWithCustomCommands');
        expect(mainHostSummaryView.get('clientsWithCustomCommands')).to.be.eql(test.expected);
      });
    });
  });
});
