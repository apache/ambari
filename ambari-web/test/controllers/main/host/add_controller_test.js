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

  describe('#saveClients()', function () {

    var modelSetup = require('test/init_model_test');
    var testCases = [
      {
        title: 'No services',
        services: [],
        result: []
      },
      {
        title: 'No selected services',
        services: [
          {isSelected: false}
        ],
        result: []
      },
      {
        title: 'Service is not in stack',
        services: [
          {
            serviceName: 'TEST',
            isSelected: true
          }
        ],
        result: []
      },
      {
        title: 'Service does not have any clients',
        services: [
          {
            serviceName: 'GANGLIA',
            isSelected: true
          }
        ],
        result: []
      },
      {
        title: 'StackServiceComponent is empty',
        services: [
          {
            serviceName: 'HDFS',
            isSelected: true
          }
        ],
        result: []
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        controller.set('content.services', test.services);
        controller.saveClients();
        expect(controller.setDBProperty.calledWith('clientInfo', test.result)).to.be.true;
        expect(controller.get('content.clients')).to.be.empty;
      });
    });

    it('HDFS has uninstalled client', function () {
      modelSetup.setupStackServiceComponent();
      var services = [
        {
          serviceName: 'HDFS',
          isSelected: true
        }
      ];
      controller.set('content.services', services);
      controller.saveClients();
      expect(controller.get('content.clients')).to.eql([
        {
          component_name: 'HDFS_CLIENT',
          display_name: 'HDFS Client',
          isInstalled: false
        }
      ]);
      expect(controller.setDBProperty.calledWith('clientInfo', [
        {
          component_name: 'HDFS_CLIENT',
          display_name: 'HDFS Client',
          isInstalled: false
        }
      ])).to.be.true;
      modelSetup.cleanStackServiceComponent();
    });
    it('HDFS has installed client', function () {
      modelSetup.setupStackServiceComponent();
      var services = [
        {
          serviceName: 'HDFS',
          isSelected: true
        }
      ];
      App.store.load(App.HostComponent, {
        id: 'HDFS_CLIENT_host1',
        component_name: "HDFS_CLIENT"
      });
      controller.set('content.services', services);
      controller.saveClients();
      expect(controller.get('content.clients')).to.eql([
        {
          component_name: 'HDFS_CLIENT',
          display_name: 'HDFS Client',
          isInstalled: true
        }
      ]);
      expect(controller.setDBProperty.calledWith('clientInfo', [
        {
          component_name: 'HDFS_CLIENT',
          display_name: 'HDFS Client',
          isInstalled: true
        }
      ])).to.be.true;
      modelSetup.cleanStackServiceComponent();
    });
  });

  describe('#applyConfigGroup()', function () {

    beforeEach(function () {
      sinon.spy(App.ajax, "send");
    });
    afterEach(function () {
      App.ajax.send.restore();
    });

    it('No config groups', function () {
      controller.set('content.serviceConfigGroups', []);
      controller.applyConfigGroup();
      expect(App.ajax.send.called).to.be.false;
    });
    it('Selected group has no groups', function () {
      var serviceConfigGroups = [
        {
          configGroups: [],
          selectedConfigGroup: ''
        }
      ];
      controller.set('content.serviceConfigGroups', serviceConfigGroups);
      controller.applyConfigGroup();
      expect(App.ajax.send.called).to.be.false;
    });
    it('Selected group does not match groups', function () {
      var serviceConfigGroups = [
        {
          configGroups: [
            {
              ConfigGroup: {
                group_name: 'group1'
              }
            }
          ],
          selectedConfigGroup: 'group2'
        }
      ];
      controller.set('content.serviceConfigGroups', serviceConfigGroups);
      controller.applyConfigGroup();
      expect(App.ajax.send.called).to.be.false;
    });
    it('Selected group has zero hosts', function () {
      var serviceConfigGroups = [
        {
          configGroups: [
            {
              ConfigGroup: {
                group_name: 'group1',
                hosts: []
              },
              href: 'href'
            }
          ],
          hosts: [],
          selectedConfigGroup: 'group1'
        }
      ];
      controller.set('content.serviceConfigGroups', serviceConfigGroups);
      controller.applyConfigGroup();
      expect(serviceConfigGroups[0].configGroups[0].ConfigGroup.hosts).to.be.empty;
      expect(serviceConfigGroups[0].configGroups[0].href).to.be.undefined;
      expect(App.ajax.send.calledOnce).to.be.true;
    });
    it('Selected group has host', function () {
      var serviceConfigGroups = [
        {
          configGroups: [
            {
              ConfigGroup: {
                group_name: 'group1',
                hosts: []
              },
              href: 'href'
            }
          ],
          hosts: ['host1'],
          selectedConfigGroup: 'group1'
        }
      ];
      controller.set('content.serviceConfigGroups', serviceConfigGroups);
      controller.applyConfigGroup();
      expect(serviceConfigGroups[0].configGroups[0].ConfigGroup.hosts).to.eql([
        {host_name: 'host1'}
      ]);
      expect(serviceConfigGroups[0].configGroups[0].href).to.be.undefined;
      expect(App.ajax.send.calledOnce).to.be.true;
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
      },
      {
        title: 'Slave component is present',
        slaveComponentHosts: [
          {
            hosts: [
              {hostName: 'host1'}
            ],
            componentName: 'DATANODE'
          }
        ],
        result: {
          output: true,
          selectedServices: [
            {
              serviceId: 'HDFS',
              displayName: 'HDFS',
              hosts: ['host1'],
              configGroupsNames: ['HDFS Default', 'HDFS test'],
              configGroups: [
                {
                  ConfigGroup: {
                    tag: 'HDFS',
                    group_name: 'HDFS test'
                  }
                }
              ],
              selectedConfigGroup: 'HDFS Default'
            }
          ]
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
      },
      {
        title: 'Client is present',
        content: {
          slaveComponentHosts: [
            {
              componentName: 'CLIENT',
              hosts: [
                {hostName: 'host1'}
              ]
            }
          ],
          clients: [
            {
              component_name: 'HDFS_CLIENT'
            }
          ],
          selectedServices: []
        },
        result: {
          output: true,
          selectedServices: [
            {
              serviceId: 'HDFS',
              displayName: 'HDFS',
              hosts: ['host1'],
              configGroupsNames: ['HDFS Default', 'HDFS test'],
              configGroups: [
                {
                  ConfigGroup: {
                    tag: 'HDFS',
                    group_name: 'HDFS test'
                  }
                }
              ],
              selectedConfigGroup: 'HDFS Default'
            }
          ]
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
      controller.set('testDBHosts', {'host1': {}});
      expect(controller.installServices()).to.be.true;
      expect(App.ajax.send.called).to.be.true;
    });
    it('Cluster name is correct and hosts are present, isRetry = true', function () {
      controller.set('content.cluster', {name: 'cl'});
      controller.set('testDBHosts', {'host1': {}});
      expect(controller.installServices(true)).to.be.true;
      expect(App.ajax.send.called).to.be.true;
    });
  });
});
