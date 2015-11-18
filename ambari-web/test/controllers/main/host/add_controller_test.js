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
require('controllers/wizard');
require('controllers/main/host/add_controller');
require('models/host_component');
require('models/service');
require('mappers/server_data_mapper');

describe('App.AddHostController', function () {

  var controller = App.AddHostController.create({
    testDBHosts: null,
    getDBProperty: function () {
      return this.get('testDBHosts');
    },
    setDBProperty: function () {
    },
    loadClients: function () {
    }
  });

  describe('#removeHosts()', function () {
    var testCases = [
      {
        title: 'No hosts, db is empty',
        content: {
          hosts: [],
          dbHosts: {}
        },
        result: {}
      },
      {
        title: 'Host is passed, db is empty',
        content: {
          hosts: [
            {name: 'host1'}
          ],
          dbHosts: {}
        },
        result: {}
      },
      {
        title: 'Passed host different from hosts in db',
        content: {
          hosts: [
            {name: 'host1'}
          ],
          dbHosts: {
            'host2': {}
          }
        },
        result: {
          'host2': {}
        }
      },
      {
        title: 'Passed host match host in db',
        content: {
          hosts: [
            {name: 'host1'}
          ],
          dbHosts: {
            'host1': {}
          }
        },
        result: {}
      }
    ];
    beforeEach(function () {
      sinon.spy(controller, "setDBProperty");
    });
    afterEach(function () {
      controller.setDBProperty.restore();
    });
    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('testDBHosts', test.content.dbHosts);
        controller.removeHosts(test.content.hosts);
        expect(controller.setDBProperty.calledWith('hosts', test.result)).to.be.true;
      });
    });
  });

  describe('#sortServiceConfigGroups()', function () {
    var testCases = [
      {
        title: 'No selected services',
        selectedServices: [
          {configGroups: []}
        ],
        result: [
          {configGroups: []}
        ]
      },
      {
        title: 'Only one group is present',
        selectedServices: [
          {configGroups: [
            {configGroups: {group_name: 'b'}}
          ]}
        ],
        result: [
          {configGroups: [
            {configGroups: {group_name: 'b'}}
          ]}
        ]
      },
      {
        title: 'Reverse order of groups',
        selectedServices: [
          {configGroups: [
            {ConfigGroup: {group_name: 'b2'}},
            {ConfigGroup: {group_name: 'a1'}}
          ]}
        ],
        result: [
          {configGroups: [
            {ConfigGroup: {group_name: 'a1'}},
            {ConfigGroup: {group_name: 'b2'}}
          ]}
        ]
      },
      {
        title: 'Correct order of groups',
        selectedServices: [
          {configGroups: [
            {ConfigGroup: {group_name: 'a1'}},
            {ConfigGroup: {group_name: 'b2'}}
          ]}
        ],
        result: [
          {configGroups: [
            {ConfigGroup: {group_name: 'a1'}},
            {ConfigGroup: {group_name: 'b2'}}
          ]}
        ]
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.sortServiceConfigGroups(test.selectedServices);
        expect(test.selectedServices).to.eql(test.result);
      });
    });
  });

  describe('#loadServiceConfigGroupsBySlaves()', function () {
    var testCases = [
      {
        title: 'slaveComponentHosts is null',
        slaveComponentHosts: null,
        result: {
          output: false,
          selectedServices: []
        }
      },
      {
        title: 'slaveComponentHosts is empty',
        slaveComponentHosts: [],
        result: {
          output: false,
          selectedServices: []
        }
      },
      {
        title: 'Component does not have hosts',
        slaveComponentHosts: [
          {hosts: []}
        ],
        result: {
          output: true,
          selectedServices: []
        }
      },
      {
        title: 'Only client component is present',
        slaveComponentHosts: [
          {
            hosts: [
              {hostName: 'host1'}
            ],
            componentName: 'CLIENT'
          }
        ],
        result: {
          output: true,
          selectedServices: []
        }
      }
    ];

    controller.set('content.configGroups', [
      {
        ConfigGroup: {
          tag: 'HDFS',
          group_name: 'HDFS test'
        }
      }
    ]);
    testCases.forEach(function (test) {
      it(test.title, function () {
        var selectedServices = [];
        controller.set('content.slaveComponentHosts', test.slaveComponentHosts);
        expect(controller.loadServiceConfigGroupsBySlaves(selectedServices)).to.equal(test.result.output);
        expect(selectedServices).to.eql(test.result.selectedServices);
      });
    });
  });

  describe('#loadServiceConfigGroupsByClients()', function () {
    var testCases = [
      {
        title: 'slaveComponentHosts is null',
        content: {
          slaveComponentHosts: null,
          clients: [],
          selectedServices: []
        },
        result: {
          output: false,
          selectedServices: []
        }
      },
      {
        title: 'slaveComponentHosts is empty',
        content: {
          slaveComponentHosts: [],
          clients: [],
          selectedServices: []
        },
        result: {
          output: false,
          selectedServices: []
        }
      },
      {
        title: 'Client does not have hosts',
        content: {
          slaveComponentHosts: [
            {
              componentName: 'CLIENT',
              hosts: []
            }
          ],
          clients: [],
          selectedServices: []
        },
        result: {
          output: false,
          selectedServices: []
        }
      },
      {
        title: 'Client has hosts, but clients is empty',
        content: {
          slaveComponentHosts: [
            {
              componentName: 'CLIENT',
              hosts: [
                {hostName: 'host1'}
              ]
            }
          ],
          clients: [],
          selectedServices: []
        },
        result: {
          output: false,
          selectedServices: []
        }
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('content.slaveComponentHosts', test.content.slaveComponentHosts);
        controller.set('content.clients', test.content.clients);
        expect(controller.loadServiceConfigGroupsByClients(test.content.selectedServices)).to.equal(test.result.output);
        expect(test.content.selectedServices).to.eql(test.result.selectedServices);
      });
    });
  });

  describe('#installServices()', function () {

    beforeEach(function () {
      sinon.spy(App.ajax, "send");
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('No hosts', function () {
      controller.set('content.cluster', {name: 'cl'});
      controller.set('testDBHosts', {});
      expect(controller.installServices()).to.be.false;
      expect(App.ajax.send.called).to.be.false;
    });
    it('Cluster name is empty', function () {
      controller.set('content.cluster', {name: ''});
      controller.set('testDBHosts', {'host1': {}});
      expect(controller.installServices()).to.be.false;
      expect(App.ajax.send.called).to.be.false;
    });
    it('Cluster name is correct and hosts are present', function () {
      controller.set('content.cluster', {name: 'cl'});
      controller.set('testDBHosts', {'host1': {isInstalled: false}});
      expect(controller.installServices()).to.be.true;
      expect(App.ajax.send.called).to.be.true;
    });
  });

  describe('#getClientsToInstall', function () {
    var services = [
      Em.Object.create({
        serviceName: 'service1'
      }),
      Em.Object.create({
        serviceName: 'service2'
      })
    ];
    var components = [
      Em.Object.create({
        componentName: 'comp1',
        displayName: 'comp1',
        serviceName: 'service1',
        isClient: true
      }),
      Em.Object.create({
        componentName: 'comp2',
        displayName: 'comp2',
        serviceName: 'service1',
        isClient: true
      }),
      Em.Object.create({
        componentName: 'comp3',
        displayName: 'comp3',
        serviceName: 'service2',
        isClient: false
      }),
      Em.Object.create({
        componentName: 'comp4',
        displayName: 'comp4',
        serviceName: 'service3',
        isClient: true
      })
    ];
    var clients = [
      {
        component_name: 'comp1',
        display_name: 'comp1',
        isInstalled: false
      },
      {
        component_name: 'comp2',
        display_name: 'comp2',
        isInstalled: false
      }
    ];
    it("generatel list of clients to install", function () {
      expect(controller.getClientsToInstall(services, components)).to.eql(clients);
    })
  });

  describe("#setCurrentStep()", function () {
    before(function () {
      sinon.stub(App.clusterStatus, 'setClusterStatus', Em.K);
      sinon.stub(App.db, 'setWizardCurrentStep', Em.K);
    });
    after(function () {
      App.clusterStatus.setClusterStatus.restore();
      App.db.setWizardCurrentStep.restore();
    });
    it("call App.clusterStatus.setClusterStatus()", function () {
      controller.setCurrentStep();
      expect(App.clusterStatus.setClusterStatus.getCall(0).args[0].wizardControllerName).to.be.equal('addHostController');
    });
  });

  describe("#getCluster()", function () {
    before(function () {
      sinon.stub(App.router, 'getClusterName').returns('c1');
    });
    after(function () {
      App.router.getClusterName.restore();
    });
    it("", function () {
      controller.set('clusterStatusTemplate', {'prop': 'clusterStatusTemplate'});
      expect(controller.getCluster()).to.be.eql({
        prop: 'clusterStatusTemplate',
        name: 'c1'
      });
    });
  });

  describe("#loadServices", function () {
    var services = {
      db: null,
      stack: [],
      model: []
    };
    beforeEach(function () {
      sinon.stub(controller, 'getDBProperty', function () {
        return services.db;
      });
      sinon.stub(App.StackService, 'find', function () {
        return services.stack;
      });
      sinon.stub(App.Service, 'find', function () {
        return services.model;
      });
      sinon.stub(controller, 'setDBProperty', Em.K);
    });
    afterEach(function () {
      controller.getDBProperty.restore();
      App.StackService.find.restore();
      App.Service.find.restore();
      controller.setDBProperty.restore();
    });
    it("No services in db, no installed services", function () {
      services.stack = [Em.Object.create({
        serviceName: 'S1'
      })];
      controller.loadServices();
      expect(controller.setDBProperty.getCall(0).args).to.eql(['services',
        {
          selectedServices: [],
          installedServices: []
        }
      ]);
      expect(controller.get('content.services')).to.eql([
        Em.Object.create({
          serviceName: 'S1',
          isInstalled: false,
          isSelected: false
        })
      ])
    });
    it("No services in db, installed service present", function () {
      services.stack = [
        Em.Object.create({
          serviceName: 'S1'
        }),
        Em.Object.create({
          serviceName: 'S2'
        })
      ];
      services.model = [
        Em.Object.create({
          serviceName: 'S1'
        })
      ];
      controller.loadServices();
      expect(controller.setDBProperty.getCall(0).args).to.eql(['services',
        {
          selectedServices: ['S1'],
          installedServices: ['S1']
        }
      ]);
      expect(controller.get('content.services')).to.eql([
        Em.Object.create({
          serviceName: 'S1',
          isInstalled: true,
          isSelected: true
        }),
        Em.Object.create({
          serviceName: 'S2',
          isInstalled: false,
          isSelected: false
        })
      ]);
    });
    it("DB is empty", function () {
      services.stack = [Em.Object.create({
        serviceName: 'S1'
      })];
      services.db = {
        selectedServices: [],
        installedServices: []
      };
      controller.loadServices();
      expect(controller.setDBProperty.called).to.be.false;
      expect(controller.get('content.services')).to.eql([
        Em.Object.create({
          serviceName: 'S1',
          isSelected: false,
          isInstalled: false
        })
      ]);
    });
    it("DB has selected and installed services", function () {
      services.stack = [
        Em.Object.create({
          serviceName: 'S1'
        }),
        Em.Object.create({
          serviceName: 'S2'
        })
      ];
      services.db = {
        selectedServices: ['S1'],
        installedServices: ['S2']
      };
      controller.loadServices();
      expect(controller.setDBProperty.called).to.be.false;
      expect(controller.get('content.services')).to.eql([
        Em.Object.create({
          serviceName: 'S1',
          isInstalled: false,
          isSelected: true
        }),
        Em.Object.create({
          serviceName: 'S2',
          isInstalled: true,
          isSelected: false
        })
      ]);
    });
  });

  describe("#loadSlaveComponentHosts()", function () {

    var mock = {
      hosts: null,
      slaveComponentHosts: null
    };

    beforeEach(function () {
      sinon.stub(controller, 'getDBProperties', function (propsList) {
        var ret = {};
        propsList.forEach(function(k) {
          ret[k] = mock[k];
        });
        return ret;
      });
    });

    afterEach(function () {
      controller.getDBProperties.restore();
    });

    it("No slaveComponentHosts in db, null", function () {
      controller.loadSlaveComponentHosts();
      expect(controller.get('content.slaveComponentHosts')).to.be.empty;
    });

    it("No slaveComponentHosts in db", function () {
      mock.slaveComponentHosts = [];
      controller.loadSlaveComponentHosts();
      expect(controller.get('content.slaveComponentHosts')).to.be.empty;
    });

    it("One slaveComponent without hosts", function () {
      mock.slaveComponentHosts = [
        {hosts: []}
      ];
      mock.hosts = {};
      controller.loadSlaveComponentHosts();
      expect(controller.get('content.slaveComponentHosts')).to.be.eql([
        {hosts: []}
      ]);
    });

    it("One slaveComponent with host", function () {
      mock.slaveComponentHosts = [
        {hosts: [
          {host_id: 1}
        ]}
      ];
      mock.hosts = {'host1': {id: 1}};
      controller.loadSlaveComponentHosts();
      expect(controller.get('content.slaveComponentHosts')).to.be.eql([
        {hosts: [
          {
            host_id: 1,
            hostName: 'host1'
          }
        ]}
      ]);
    });

  });

  describe("#saveClients()", function () {
    before(function () {
      sinon.stub(App.StackServiceComponent, 'find').returns('StackServiceComponent');
      sinon.stub(controller, 'getClientsToInstall').returns(['client']);
      sinon.stub(controller, 'setDBProperty', Em.K);
    });
    after(function () {
      controller.setDBProperty.restore();
      App.StackServiceComponent.find.restore();
      controller.getClientsToInstall.restore();
    });
    it("", function () {
      controller.set('content.services', [Em.Object.create({'isSelected': true, 'isInstallable': true})]);
      controller.saveClients();
      expect(controller.getClientsToInstall.calledWith(
        [Em.Object.create({'isSelected': true, 'isInstallable': true})],
        'StackServiceComponent'
      )).to.be.true;
      expect(controller.setDBProperty.calledWith('clientInfo', ['client'])).to.be.true;
      expect(controller.get('content.clients')).to.be.eql(['client']);
    });
  });

  describe("#getClientsToInstall()", function () {
    var testCases = [
      {
        title: 'No services',
        data: {
          services: [],
          components: []
        },
        result: []
      },
      {
        title: 'No components',
        data: {
          services: [
            {}
          ],
          components: []
        },
        result: []
      },
      {
        title: 'Component is not client',
        data: {
          services: [Em.Object.create({serviceName: 'S1'})],
          components: [Em.Object.create({serviceName: 'S1'})]
        },
        result: []
      },
      {
        title: 'Component is not client',
        data: {
          services: [Em.Object.create({serviceName: 'S1'})],
          components: [Em.Object.create({serviceName: 'S1', isClient: false})]
        },
        result: []
      },
      {
        title: 'Component is client',
        data: {
          services: [Em.Object.create({serviceName: 'S1'})],
          components: [Em.Object.create({
            serviceName: 'S1',
            isClient: true,
            componentName: 'C1',
            displayName: 'C1'
          })]
        },
        result: [
          {
            component_name: 'C1',
            display_name: 'C1',
            isInstalled: false
          }
        ]
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        expect(controller.getClientsToInstall(test.data.services, test.data.components)).to.eql(test.result);
      });
    });
  });

  describe("#applyConfigGroup()", function () {
    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });
    it("No config groups", function () {
      controller.set('content.configGroups', []);
      controller.applyConfigGroup();
      expect(App.ajax.send.called).to.be.false;
    });
    it("selectedConfigGroup absent", function () {
      controller.set('content.configGroups', [
        {
          configGroups: [],
          selectedConfigGroup: ''
        }
      ]);
      controller.applyConfigGroup();
      expect(App.ajax.send.called).to.be.false;
    });
    it("selectedConfigGroup present", function () {
      controller.set('content.configGroups', [
        {
          configGroups: [
            {
              ConfigGroup: {
                id: 1,
                group_name: 'G1',
                hosts: []
              }
            }
          ],
          selectedConfigGroup: 'G1',
          hosts: ['host1']
        }
      ]);
      controller.applyConfigGroup();

      expect(App.ajax.send.getCall(0).args[0].name).to.equal('config_groups.update_config_group');
      expect(App.ajax.send.getCall(0).args[0].data).to.eql({
        "id": 1,
        "configGroup": {
          "ConfigGroup": {
            "id": 1,
            "group_name": "G1",
            "hosts": [
              {
                "host_name": "host1"
              }
            ]
          }
        }
      });
    });
  });

  describe("#getServiceConfigGroups()", function () {
    before(function () {
      sinon.stub(controller, 'getDBProperty').withArgs('serviceConfigGroups').returns(['serviceConfigGroup']);
    });
    after(function () {
      controller.getDBProperty.restore();
    });
    it("", function () {
      controller.getServiceConfigGroups();
      expect(controller.get('content.configGroups')).to.eql(['serviceConfigGroup']);
    });
  });

  describe("#saveServiceConfigGroups()", function () {
    before(function () {
      sinon.stub(controller, 'setDBProperty', Em.K);
    });
    after(function () {
      controller.setDBProperty.restore();
    });
    it("call setDBProperty()", function () {
      controller.set('content.configGroups', [
        {}
      ]);
      controller.saveServiceConfigGroups();
      expect(controller.setDBProperty.calledWith('serviceConfigGroups', [
        {}
      ])).to.be.true;
    });
  });

  describe("#loadServiceConfigGroups()", function () {
    before(function () {
      sinon.stub(controller, 'loadServiceConfigGroupsBySlaves', Em.K);
      sinon.stub(controller, 'loadServiceConfigGroupsByClients', Em.K);
      sinon.stub(controller, 'sortServiceConfigGroups', Em.K);
    });
    after(function () {
      controller.loadServiceConfigGroupsBySlaves.restore();
      controller.loadServiceConfigGroupsByClients.restore();
      controller.sortServiceConfigGroups.restore();
    });
    it("", function () {
      controller.loadServiceConfigGroups();
      expect(controller.loadServiceConfigGroupsByClients.calledWith([])).to.be.true;
      expect(controller.loadServiceConfigGroupsBySlaves.calledWith([])).to.be.true;
      expect(controller.sortServiceConfigGroups.calledWith([])).to.be.true;
      expect(controller.get('content.configGroups')).to.eql([]);
    });
  });

  describe("#sortServiceConfigGroups", function () {
    var testCases = [
      {
        title: 'sorted',
        selectedServices: [
          {
            configGroups: [
              {
                ConfigGroup: {
                  group_name: 'a'
                }
              },
              {
                ConfigGroup: {
                  group_name: 'b'
                }
              }
            ]
          }
        ],
        result: ['a', 'b']
      },
      {
        title: 'not sorted',
        selectedServices: [
          {
            configGroups: [
              {
                ConfigGroup: {
                  group_name: 'b'
                }
              },
              {
                ConfigGroup: {
                  group_name: 'a'
                }
              }
            ]
          }
        ],
        result: ['a', 'b']
      },
      {
        title: 'sort equal',
        selectedServices: [
          {
            configGroups: [
              {
                ConfigGroup: {
                  group_name: 'a'
                }
              },
              {
                ConfigGroup: {
                  group_name: 'a'
                }
              }
            ]
          }
        ],
        result: ['a', 'a']
      }
    ];
    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.sortServiceConfigGroups(test.selectedServices);
        expect(test.selectedServices[0].configGroups.mapProperty('ConfigGroup.group_name')).to.eql(test.result);
      });
    });
  });

  describe("#loadServiceConfigGroupsBySlaves()", function () {
    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({
        stackService: Em.Object.create({
          serviceName: 'S1',
          displayName: 's1'
        })
      }));
      controller.set('content.configGroups', [
        {
          ConfigGroup: {
            tag: 'S1',
            group_name: 'G1'
          }
        }
      ]);
    });
    afterEach(function () {
      App.StackServiceComponent.find.restore();
    });
    it("slaveComponentHosts is empty", function () {
      var selectedServices = [];
      controller.set('content.slaveComponentHosts', []);
      expect(controller.loadServiceConfigGroupsBySlaves(selectedServices)).to.be.false;
      expect(selectedServices).to.be.empty;
    });
    it("slaveComponentHosts has ho hosts", function () {
      var selectedServices = [];
      controller.set('content.slaveComponentHosts', [
        {hosts: []}
      ]);
      expect(controller.loadServiceConfigGroupsBySlaves(selectedServices)).to.be.true;
      expect(selectedServices).to.be.empty;
    });
    it("slaveComponentHosts is CLIENT", function () {
      var selectedServices = [];
      controller.set('content.slaveComponentHosts', [
        {
          hosts: [
            {hostName: 'host1'}
          ],
          componentName: 'CLIENT'
        }
      ]);
      expect(controller.loadServiceConfigGroupsBySlaves(selectedServices)).to.be.true;
      expect(selectedServices).to.be.empty;
    });
    it("slaveComponentHosts is slave", function () {
      var selectedServices = [];
      controller.set('content.slaveComponentHosts', [
        {
          hosts: [
            {hostName: 'host1'}
          ],
          componentName: 'C1'
        },
        {
          hosts: [
            {hostName: 'host2'}
          ],
          componentName: 'C2'
        }
      ]);
      expect(controller.loadServiceConfigGroupsBySlaves(selectedServices)).to.be.true;
      expect(selectedServices.toArray()).to.eql([
        {
          "serviceId": "S1",
          "displayName": "s1",
          "hosts": [
            "host1",
            "host2"
          ],
          "configGroupsNames": [
            "Default",
            "G1"
          ],
          "configGroups": [
            {
              "ConfigGroup": {
                "tag": "S1",
                "group_name": "G1"
              }
            }
          ],
          "selectedConfigGroup": "Default"
        }
      ]);
    });
  });

  describe("#loadServiceConfigGroupsByClients()", function () {
    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find').returns(Em.Object.create({
        stackService: Em.Object.create({
          serviceName: 'S1',
          displayName: 's1'
        })
      }));
      sinon.stub(controller, 'loadClients', Em.K);
      controller.set('content.configGroups', [
        {
          ConfigGroup: {
            tag: 'S1',
            group_name: 'G1'
          }
        }
      ]);
    });
    afterEach(function () {
      controller.loadClients.restore();
      App.StackServiceComponent.find.restore();
    });
    it("Clients is null", function () {
      var selectedServices = [];
      controller.set('content.slaveComponentHosts', null);
      controller.set('content.clients', null);

      expect(controller.loadServiceConfigGroupsByClients(selectedServices)).to.be.false;
      expect(selectedServices).to.be.empty;
    });
    it("No CLIENT component", function () {
      var selectedServices = [];
      controller.set('content.slaveComponentHosts', []);
      controller.set('content.clients', []);

      expect(controller.loadServiceConfigGroupsByClients(selectedServices)).to.be.false;
      expect(selectedServices).to.be.empty;
    });
    it("Clients is empty", function () {
      var selectedServices = [];
      controller.set('content.slaveComponentHosts', [
        {
          componentName: 'CLIENT',
          hosts: []
        }
      ]);
      controller.set('content.clients', []);

      expect(controller.loadServiceConfigGroupsByClients(selectedServices)).to.be.false;
      expect(selectedServices).to.be.empty;
    });
    it("Client component does not have hosts", function () {
      var selectedServices = [];
      controller.set('content.slaveComponentHosts', [
        {
          componentName: 'CLIENT',
          hosts: []
        }
      ]);
      controller.set('content.clients', [
        {}
      ]);

      expect(controller.loadServiceConfigGroupsByClients(selectedServices)).to.be.false;
      expect(selectedServices).to.be.empty;
    });
    it("Client present, selectedServices is empty", function () {
      var selectedServices = [];
      controller.set('content.slaveComponentHosts', [
        {
          componentName: 'CLIENT',
          hosts: [
            {hostName: 'host1'}
          ]
        }
      ]);
      controller.set('content.clients', [
        {
          component_name: 'C1'
        }
      ]);

      expect(controller.loadServiceConfigGroupsByClients(selectedServices)).to.be.true;
      expect(selectedServices).to.be.eql([
        {
          "serviceId": "S1",
          "displayName": "s1",
          "hosts": [
            "host1"
          ],
          "configGroupsNames": [
            "Default",
            "G1"
          ],
          "configGroups": [
            {
              "ConfigGroup": {
                "tag": "S1",
                "group_name": "G1"
              }
            }
          ],
          "selectedConfigGroup": "Default"
        }
      ]);
    });
    it("Client present, selectedServices has service", function () {
      var selectedServices = [
        {
          serviceId: 'S1',
          hosts: ['host1', 'host2']
        }
      ];
      controller.set('content.slaveComponentHosts', [
        {
          componentName: 'CLIENT',
          hosts: [
            {hostName: 'host1'}
          ]
        }
      ]);
      controller.set('content.clients', [
        {
          component_name: 'C1'
        }
      ]);

      expect(controller.loadServiceConfigGroupsByClients(selectedServices)).to.be.true;
      expect(selectedServices[0].hosts).to.be.eql(["host1", "host2"]);
    });
  });

  describe("#loadServiceConfigProperties()", function () {
    beforeEach(function () {
      this.mock = sinon.stub(App.db, 'get');
      this.mock.withArgs('Installer', 'serviceConfigProperties').returns([1]);
    });
    afterEach(function () {
      this.mock.restore();
    });
    it("serviceConfigProperties is null", function () {
      this.mock.withArgs('AddService', 'serviceConfigProperties').returns(null);
      controller.loadServiceConfigProperties();
      expect(controller.get('content.serviceConfigProperties')).to.eql([1]);
    });
    it("serviceConfigProperties is empty", function () {
      this.mock.withArgs('AddService', 'serviceConfigProperties').returns([]);
      controller.loadServiceConfigProperties();
      expect(controller.get('content.serviceConfigProperties')).to.eql([1]);
    });
    it("serviceConfigProperties has data", function () {
      this.mock.withArgs('AddService', 'serviceConfigProperties').returns([1]);
      controller.loadServiceConfigProperties();
      expect(controller.get('content.serviceConfigProperties')).to.eql([1]);
    });
  });

  describe("#loadAllPriorSteps()", function () {
    var stepsSet = {
      '1': [
        {
          name: 'load',
          args: ['hosts']
        },
        {
          name: 'load',
          args: ['installOptions']
        },
        {
          name: 'load',
          args: ['cluster']
        }
      ],
      '2': [
        {
          name: 'loadServices',
          args: []
        }
      ],
      '3': [
        {
          name: 'loadClients',
          args: []
        },
        {
          name: 'loadServices',
          args: []
        },
        {
          name: 'loadMasterComponentHosts',
          args: []
        },
        {
          name: 'loadSlaveComponentHosts',
          args: []
        },
        {
          name: 'load',
          args: ['hosts']
        }
      ],
      '5': [
        {
          name: 'loadServiceConfigProperties',
          args: []
        },
        {
          name: 'getServiceConfigGroups',
          args: []
        }
      ]
    };
    var testCases = [
      {
        currentStep: '0',
        calledFunctions: []
      },
      {
        currentStep: '1',
        calledFunctions: stepsSet['1']
      },
      {
        currentStep: '2',
        calledFunctions: stepsSet['1'].concat(stepsSet['2'])
      },
      {
        currentStep: '3',
        calledFunctions: stepsSet['3'].concat(stepsSet['2'], stepsSet['1'])
      },
      {
        currentStep: '4',
        calledFunctions: stepsSet['3'].concat(stepsSet['2'], stepsSet['1'])
      },
      {
        currentStep: '5',
        calledFunctions: stepsSet['5'].concat(stepsSet['3'], stepsSet['2'], stepsSet[1])
      },
      {
        currentStep: '6',
        calledFunctions: stepsSet['5'].concat(stepsSet['3'], stepsSet['2'], stepsSet[1])
      },
      {
        currentStep: '7',
        calledFunctions: stepsSet['5'].concat(stepsSet['3'], stepsSet['2'], stepsSet[1])
      },
      {
        currentStep: '8',
        calledFunctions: []
      }
    ];
    var functionsToCall = [
      'loadServiceConfigProperties',
      'getServiceConfigGroups',
      'loadClients',
      'loadServices',
      'loadMasterComponentHosts',
      'loadSlaveComponentHosts',
      'load'
    ];
    beforeEach(function () {
      this.mock = sinon.stub(controller, 'get');
      sinon.stub(controller, 'loadServiceConfigProperties', Em.K);
      sinon.stub(controller, 'getServiceConfigGroups', Em.K);
      sinon.stub(controller, 'loadClients', Em.K);
      sinon.stub(controller, 'loadServices', Em.K);
      sinon.stub(controller, 'loadMasterComponentHosts', Em.K);
      sinon.stub(controller, 'loadSlaveComponentHosts', Em.K);
      sinon.stub(controller, 'load', Em.K);
      sinon.stub(controller, 'saveClusterStatus', Em.K);
    });
    afterEach(function () {
      this.mock.restore();
      controller.loadServiceConfigProperties.restore();
      controller.getServiceConfigGroups.restore();
      controller.loadClients.restore();
      controller.loadServices.restore();
      controller.loadMasterComponentHosts.restore();
      controller.loadSlaveComponentHosts.restore();
      controller.load.restore();
      controller.saveClusterStatus.restore();
    });
    testCases.forEach(function (test) {
      it("current step - " + test.currentStep, function () {
        this.mock.returns(test.currentStep);
        controller.loadAllPriorSteps();
        functionsToCall.forEach(function (fName) {
          var callStack = test.calledFunctions.filterProperty('name', fName);
          if (callStack.length > 0) {
            callStack.forEach(function (f, index) {
              expect(controller[f.name].getCall(index).args).to.eql(f.args);
            }, this);
          } else {
            expect(controller[fName].called).to.be.false;
          }
        }, this);
      });
    }, this);
  });

  describe("#clearAllSteps()", function () {
    beforeEach(function () {
      sinon.stub(controller, 'clearInstallOptions', Em.K);
      sinon.stub(controller, 'getCluster').returns({});
    });
    afterEach(function () {
      controller.clearInstallOptions.restore();
      controller.getCluster.restore();
    });
    it("", function () {
      controller.clearAllSteps();
      expect(controller.getCluster.calledOnce).to.be.true;
      expect(controller.clearInstallOptions.calledOnce).to.be.true;
      expect(controller.get('content.cluster')).to.eql({});
    });
  });

  describe("#clearStorageData()", function () {
    beforeEach(function () {
      sinon.stub(controller, 'resetDbNamespace', Em.K);
    });
    afterEach(function () {
      controller.resetDbNamespace.restore();
    });
    it("launch resetDbNamespace", function () {
      controller.clearStorageData();
      expect(controller.resetDbNamespace.calledOnce).to.be.true;
    });
  });

  describe("#finish()", function () {
    var mock = {
      updateAll: Em.K,
      getAllHostNames: Em.K
    };
    beforeEach(function () {
      sinon.stub(controller, 'clearAllSteps', Em.K);
      sinon.stub(controller, 'clearStorageData', Em.K);
      sinon.stub(App.updater, 'immediateRun', Em.K);
      sinon.stub(App.router, 'get').returns(mock);
      sinon.spy(mock, 'updateAll');
      sinon.spy(mock, 'getAllHostNames');
    });
    afterEach(function () {
      controller.clearAllSteps.restore();
      controller.clearStorageData.restore();
      App.updater.immediateRun.restore();
      App.router.get.restore();
      mock.updateAll.restore();
      mock.getAllHostNames.restore();
    });
    it("", function () {
      controller.finish();
      expect(controller.clearAllSteps.calledOnce).to.be.true;
      expect(controller.clearStorageData.calledOnce).to.be.true;
      expect(mock.updateAll.calledOnce).to.be.true;
      expect(App.updater.immediateRun.calledWith('updateHost')).to.be.true;
      expect(mock.getAllHostNames.calledOnce).to.be.true;
    });
  });
});
