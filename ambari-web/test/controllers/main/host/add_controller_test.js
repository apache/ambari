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

  beforeEach(function () {
    sinon.spy(controller, "setDBProperty");
  });
  afterEach(function () {
    controller.setDBProperty.restore();
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
            {hostName: 'host1'}
          ],
          dbHosts: {}
        },
        result: {}
      },
      {
        title: 'Passed host different from hosts in db',
        content: {
          hosts: [
            {hostName: 'host1'}
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
            {hostName: 'host1'}
          ],
          dbHosts: {
            'host1': {}
          }
        },
        result: {}
      }
    ];
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
  })
});
