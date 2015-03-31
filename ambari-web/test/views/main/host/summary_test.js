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
var extendedMainHostSummaryView = App.MainHostSummaryView.extend({content: {}, addToolTip: function(){}, installedServices: []});
var modelSetup = require('test/init_model_test');

describe('App.MainHostSummaryView', function() {

  beforeEach(function() {
    modelSetup.setupStackServiceComponent();
    mainHostSummaryView = extendedMainHostSummaryView.create({});
  });

  afterEach(function(){
    modelSetup.cleanStackServiceComponent();
  });

  describe('#sortedComponents', function() {

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

    var tests = Em.A([
      {
        installableClientComponents: [{}, {}],
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({
              componentName: 'HDFS_CLIENT'
            }),
            Em.Object.create({
              componentName: 'DATANODE'
            })
          ])
        }),
        services: ['HDFS', 'YARN', 'MAPREDUCE2'],
        e: ['MAPREDUCE2_CLIENT', 'NODEMANAGER', 'YARN_CLIENT'],
        m: 'some components are already installed'
      },
      {
        installableClientComponents: [],
        content: Em.Object.create({
          hostComponents: Em.A([
            Em.Object.create({
              componentName: 'HDFS_CLIENT'
            }),
            Em.Object.create({
              componentName: 'YARN_CLIENT'
            }),
            Em.Object.create({
              componentName: 'MAPREDUCE2_CLIENT'
            }),
            Em.Object.create({
              componentName: 'NODEMANAGER'
            })
          ])
        }),
        services: ['HDFS', 'YARN', 'MAPREDUCE2'],
        e: ['DATANODE'],
        m: 'all clients and some other components are already installed'
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        mainHostSummaryView.reopen({installableClientComponents: test.installableClientComponents});
        mainHostSummaryView.set('content', test.content);
        mainHostSummaryView.set('installedServices', test.services);
        expect(mainHostSummaryView.get('addableComponents').mapProperty('componentName')).to.eql(test.e);
      });
    });

  });

  describe("#clientsWithCustomCommands", function() {
    before(function() {
      sinon.stub(App.StackServiceComponent, 'find', function(component) {
        var customCommands = [];

        if (component == 'WITH_CUSTOM_COMMANDS') {
          customCommands = ['CUSTOMCOMMAND'];
        }

        var obj = Em.Object.create({
          customCommands: customCommands,
          filterProperty: function () {
            return {
              mapProperty: Em.K
            };
          }
        });
        return obj;
      });
    });

    after(function() {
      App.StackServiceComponent.find.restore();
    });
    var content = Em.Object.create({
      hostComponents: Em.A([
        Em.Object.create({
          componentName: 'WITH_CUSTOM_COMMANDS',
          displayName: 'WITH_CUSTOM_COMMANDS',
          hostName: 'c6401',
          service: Em.Object.create({
            serviceName: 'TESTSRV'
          })
        }),
        Em.Object.create({
          componentName: 'WITHOUT_CUSTOM_COMMANDS',
          displayName: 'WITHOUT_CUSTOM_COMMANDS',
          hostName: 'c6401',
          service: Em.Object.create({
            serviceName: 'TESTSRV'
          })
        })
      ])
    });

    it("Clients with custom commands only", function() {
      mainHostSummaryView.set('content', content);
      expect(mainHostSummaryView.get('clientsWithCustomCommands').length).to.eql(1);
      expect(mainHostSummaryView.get('clientsWithCustomCommands')).to.have.deep.property('[0].commands[0].command', 'CUSTOMCOMMAND');
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
});
