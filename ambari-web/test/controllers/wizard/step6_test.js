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
require('utils/helper');
require('controllers/wizard/step6_controller');
var controller,
  services = [
    Em.Object.create({
      serviceName: 'MAPREDUCE',
      isSelected: true
    }),
    Em.Object.create({
      serviceName: 'YARN',
      isSelected: true
    }),
    Em.Object.create({
      serviceName: 'HBASE',
      isSelected: true
    }),
    Em.Object.create({
      serviceName: 'HDFS',
      isSelected: true
    }),
    Em.Object.create({
      serviceName: 'STORM',
      isSelected: true
    }),
    Em.Object.create({
      serviceName: 'FLUME',
      isSelected: true
    })
  ];
describe('App.WizardStep6Controller', function () {

  beforeEach(function () {
    controller = App.WizardStep6Controller.create();
    controller.set('content', {
      hosts: {},
      masterComponentHosts: {},
      services: services
    });

    var h = {}, m = [];
    Em.A(['host0', 'host1', 'host2', 'host3']).forEach(function (hostName) {
      var obj = Em.Object.create({
        name: hostName,
        hostName: hostName,
        bootStatus: 'REGISTERED'
      });
      h[hostName] = obj;
      m.push(obj);
    });

    controller.set('content.hosts', h);
    controller.set('content.masterComponentHosts', m);
    controller.set('isMasters', false);

  });

  describe('#isAddHostWizard', function () {
    it('true if content.controllerName is addHostController', function () {
      controller.set('content.controllerName', 'addHostController');
      expect(controller.get('isAddHostWizard')).to.equal(true);
    });
    it('false if content.controllerName is not addHostController', function () {
      controller.set('content.controllerName', 'mainController');
      expect(controller.get('isAddHostWizard')).to.equal(false);
    });
  });

  describe('#isInstallerWizard', function () {
    it('true if content.controllerName is addHostController', function () {
      controller.set('content.controllerName', 'installerController');
      expect(controller.get('isInstallerWizard')).to.equal(true);
    });
    it('false if content.controllerName is not addHostController', function () {
      controller.set('content.controllerName', 'mainController');
      expect(controller.get('isInstallerWizard')).to.equal(false);
    });
  });

  describe('#isAddServiceWizard', function () {
    it('true if content.controllerName is addServiceController', function () {
      controller.set('content.controllerName', 'addServiceController');
      expect(controller.get('isAddServiceWizard')).to.equal(true);
    });
    it('false if content.controllerName is not addServiceController', function () {
      controller.set('content.controllerName', 'mainController');
      expect(controller.get('isAddServiceWizard')).to.equal(false);
    });
  });

  describe('#isServiceSelected', function () {
    describe('selected', function () {
      services.forEach(function (service) {
        it(service.serviceName + ' is selected', function () {
          expect(controller.isServiceSelected(service.serviceName)).to.equal(true);
        });
      });
    });
    var unselectedService = 'FAKESERVICE';
    it(unselectedService + ' is not selected', function () {
      expect(controller.isServiceSelected(unselectedService)).to.equal(false);
    });
  });

  describe('#clearStep', function () {
    beforeEach(function () {
      sinon.stub(controller, 'clearError', Em.K);
    });
    afterEach(function () {
      controller.clearError.restore();
    });
    it('should call clearError', function () {
      controller.clearStep();
      expect(controller.clearError.calledOnce).to.equal(true);
    });
    it('should clear hosts', function () {
      controller.set('hosts', [
        {},
        {}
      ]);
      controller.clearStep();
      expect(controller.get('hosts')).to.eql([]);
    });
    it('should clear headers', function () {
      controller.set('headers', [
        {},
        {}
      ]);
      controller.clearStep();
      expect(controller.get('headers')).to.eql([]);
    });
    it('should set isLoaded to false', function () {
      controller.set('isLoaded', true);
      controller.clearStep();
      expect(controller.get('isLoaded')).to.equal(false);
    });
  });

  describe('#selectAllNodes', function () {
    beforeEach(function () {
      sinon.stub(controller, 'setAllNodes', Em.K);
    });
    afterEach(function () {
      controller.setAllNodes.restore();
    });
    it('should call setAllNodes', function () {
      controller.selectAllNodes({context: {name: 'name'}});
      expect(controller.setAllNodes.calledWith('name', true)).to.equal(true);
    });
    it('shouldn\'t call setAllNodes', function () {
      controller.selectAllNodes();
      expect(controller.setAllNodes.called).to.equal(false);
    });
  });

  describe('#deselectAllNodes', function () {
    beforeEach(function () {
      sinon.stub(controller, 'setAllNodes', Em.K);
    });
    afterEach(function () {
      controller.setAllNodes.restore();
    });
    it('should call setAllNodes', function () {
      controller.deselectAllNodes({context: {name: 'name'}});
      expect(controller.setAllNodes.calledWith('name', false)).to.equal(true);
    });
    it('shouldn\'t call setAllNodes', function () {
      controller.deselectAllNodes();
      expect(controller.setAllNodes.called).to.equal(false);
    });
  });

  describe('#checkCallback', function () {
    beforeEach(function () {
      sinon.stub(controller, 'clearError', Em.K);
    });
    afterEach(function () {
      controller.clearError.restore();
    });
    it('should call clearError', function () {
      controller.checkCallback('');
      expect(controller.clearError.calledOnce).to.equal(true);
    });
    Em.A([
        {
          m: 'all checked, isInstalled false',
          headers: Em.A([
            Em.Object.create({name: 'c1'})
          ]),
          hosts: Em.A([
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({
                  component: 'c1',
                  isInstalled: false,
                  checked: true
                })
              ])
            })
          ]),
          component: 'c1',
          e: {
            allChecked: true,
            noChecked: false
          }
        },
        {
          m: 'all checked, isInstalled true',
          headers: Em.A([
            Em.Object.create({name: 'c1'})
          ]),
          hosts: Em.A([
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({
                  component: 'c1',
                  isInstalled: true,
                  checked: true
                })
              ])
            })
          ]),
          component: 'c1',
          e: {
            allChecked: true,
            noChecked: true
          }
        },
        {
          m: 'no one checked',
          headers: Em.A([
            Em.Object.create({name: 'c1'})
          ]),
          hosts: Em.A([
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({
                  component: 'c1',
                  isInstalled: false,
                  checked: false
                })
              ])
            })
          ]),
          component: 'c1',
          e: {
            allChecked: false,
            noChecked: true
          }
        },
        {
          m: 'some checked',
          headers: Em.A([
            Em.Object.create({name: 'c1'})
          ]),
          hosts: Em.A([
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({
                  component: 'c1',
                  isInstalled: false,
                  checked: true
                }),
                Em.Object.create({
                  component: 'c1',
                  isInstalled: false,
                  checked: false
                })
              ])
            })
          ]),
          component: 'c1',
          e: {
            allChecked: false,
            noChecked: false
          }
        },
        {
          m: 'some checked, some isInstalled true',
          headers: Em.A([
            Em.Object.create({name: 'c1'})
          ]),
          hosts: Em.A([
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({
                  component: 'c1',
                  isInstalled: true,
                  checked: true
                }),
                Em.Object.create({
                  component: 'c1',
                  isInstalled: true,
                  checked: true
                })
              ])
            })
          ]),
          component: 'c1',
          e: {
            allChecked: true,
            noChecked: true
          }
        },
        {
          m: 'some checked, some isInstalled true (2)',
          headers: Em.A([
            Em.Object.create({name: 'c1'})
          ]),
          hosts: Em.A([
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({
                  component: 'c1',
                  isInstalled: false,
                  checked: false
                }),
                Em.Object.create({
                  component: 'c1',
                  isInstalled: true,
                  checked: true
                })
              ])
            })
          ]),
          component: 'c1',
          e: {
            allChecked: false,
            noChecked: true
          }
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          controller.clearStep();
          controller.set('headers', test.headers);
          controller.set('hosts', test.hosts);
          controller.checkCallback(test.component);
          var header = controller.get('headers').findProperty('name', test.component);
          expect(header.get('allChecked')).to.equal(test.e.allChecked);
          expect(header.get('noChecked')).to.equal(test.e.noChecked);
        });
      });
  });

  describe('#getHostNames', function () {
    var tests = Em.A([
      {
        hosts: {
          h1: {bootStatus: 'REGISTERED', name: 'h1'},
          h2: {bootStatus: 'REGISTERED', name: 'h2'},
          h3: {bootStatus: 'REGISTERED', name: 'h3'}
        },
        m: 'All REGISTERED',
        e: ['h1', 'h2', 'h3']
      },
      {
        hosts: {
          h1: {bootStatus: 'REGISTERED', name: 'h1'},
          h2: {bootStatus: 'FAILED', name: 'h2'},
          h3: {bootStatus: 'REGISTERED', name: 'h3'}
        },
        m: 'Some REGISTERED',
        e: ['h1', 'h3']
      },
      {
        hosts: {
          h1: {bootStatus: 'FAILED', name: 'h1'},
          h2: {bootStatus: 'FAILED', name: 'h2'},
          h3: {bootStatus: 'FAILED', name: 'h3'}
        },
        m: 'No one REGISTERED',
        e: []
      },
      {
        hosts: {},
        m: 'Empty hosts',
        e: []
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        controller.set('content.hosts', test.hosts);
        var r = controller.getHostNames();
        expect(r).to.eql(test.e);
      });
    });
  });

  describe('#validate', function () {
    var tests = Em.A([
      {
        controllerName: 'addHostController',
        method: 'validateEachHost',
        r: true,
        e: true
      },
      {
        controllerName: 'addHostController',
        method: 'validateEachHost',
        r: false,
        e: false
      },
      {
        controllerName: 'addServiceController',
        method: 'validateEachComponent',
        r: true,
        e: true
      },
      {
        controllerName: 'addServiceController',
        method: 'validateEachComponent',
        r: false,
        e: false
      },
      {
        controllerName: 'installerController',
        method: 'validateEachComponent',
        r: true,
        e: true
      },
      {
        controllerName: 'installerController',
        method: 'validateEachComponent',
        r: false,
        e: false
      }
    ]);
    tests.forEach(function (test) {
      it(test.controllerName + ' ' + test.method + ' returns ' + test.r.toString(), function () {
        sinon.stub(controller, test.method, function () {
          return test.r
        });
        controller.set('content.controllerName', test.controllerName);
        expect(controller.validate()).to.equal(test.e);
        controller[test.method].restore();
      });
    });
  });

  describe('#getMasterComponentsForHost', function () {
    var tests = Em.A([
      {
        masterComponentHosts: Em.A([
          {hostName: 'h1', component: 'c1'}
        ]),
        hostName: 'h1',
        m: 'host exists',
        e: ['c1']
      },
      {
        masterComponentHosts: Em.A([
          {hostName: 'h1', component: 'c1'}
        ]),
        hostName: 'h2',
        m: 'host donesn\'t exists',
        e: []
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        controller.set('content.masterComponentHosts', test.masterComponentHosts);
        var r = controller.getMasterComponentsForHost(test.hostName);
        expect(r).to.eql(test.e);
      });
    });
  });

  describe('#selectMasterComponents', function () {
    var tests = Em.A([
      {
        masterComponentHosts: Em.A([
          {
            hostName: 'h1',
            component: 'c1'
          }
        ]),
        hostsObj: [
          Em.Object.create({
            hostName: 'h1',
            checkboxes: [
              Em.Object.create({
                component: 'c1',
                checked: false
              })
            ]
          })
        ],
        e: true,
        m: 'host and component exist'
      },
      {
        masterComponentHosts: Em.A([
          {
            hostName: 'h1',
            component: 'c2'
          }
        ]),
        hostsObj: [
          Em.Object.create({
            hostName: 'h1',
            checkboxes: [
              Em.Object.create({
                component: 'c1',
                checked: false
              })
            ]
          })
        ],
        e: false,
        m: 'host exists'
      },
      {
        masterComponentHosts: Em.A([
          {
            hostName: 'h2',
            component: 'c2'
          }
        ]),
        hostsObj: [
          Em.Object.create({
            hostName: 'h1',
            checkboxes: [
              Em.Object.create({
                component: 'c1',
                checked: false
              })
            ]
          })
        ],
        e: false,
        m: 'host and component don\'t exist'
      }
    ]);
    tests.forEach(function (test) {
      it(test.m, function () {
        controller.set('content.masterComponentHosts', test.masterComponentHosts);
        var r = controller.selectMasterComponents(test.hostsObj);
        expect(r.findProperty('hostName', 'h1').get('checkboxes').findProperty('component', 'c1').get('checked')).to.equal(test.e);
      });
    });
  });

  describe.skip('#renderSlaves', function () {
    Em.A([
        {
          controllerName: 'addServiceController',
          slaveComponents: [],
          hostsObj: [
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({isInstalled: false, title: 'Client'}),
                Em.Object.create({isInstalled: false, title: ''}),
                Em.Object.create({isInstalled: false, title: ''})
              ]),
              hasMaster: true
            })
          ],
          m: 'host with masters, empty slaveComponents, controllerName - addServiceController',
          e: [
            [false, false, false]
          ]
        },
        {
          controllerName: 'addServiceController',
          slaveComponents: [],
          hostsObj: [
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({isInstalled: false, title: 'Client'}),
                Em.Object.create({isInstalled: false, title: ''}),
                Em.Object.create({isInstalled: false, title: ''})
              ]),
              hasMaster: false
            })
          ],
          m: 'host without masters, empty slaveComponents, controllerName - addServiceController',
          e: [
            [false, true, true]
          ]
        },
        {
          controllerName: 'addServiceController',
          slaveComponents: [],
          services: [
            Em.Object.create({serviceName: 'HDFS', isSelected: true})
          ],
          hostsObj: [
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({isInstalled: false, title: 'Client'}),
                Em.Object.create({isInstalled: false, title: 'DataNode', checked: true}),
                Em.Object.create({isInstalled: false, title: ''})
              ]),
              hasMaster: false
            })
          ],
          m: 'host without masters, empty slaveComponents, controllerName - addServiceController, one datanode checked',
          e: [
            [true, true, true]
          ]
        },
        {
          controllerName: 'addServiceController',
          slaveComponents: [],
          services: [
            Em.Object.create({serviceName: 'HDFS', isSelected: true})
          ],
          hostsObj: [
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({isInstalled: false, title: 'Client'}),
                Em.Object.create({isInstalled: false, title: 'DataNode', checked: false}),
                Em.Object.create({isInstalled: false, title: ''})
              ]),
              hasMaster: true
            })
          ],
          m: 'host with masters, empty slaveComponents, controllerName - addServiceController, one datanode not checked',
          e: [
            [false, false, false]
          ]
        },
        {
          controllerName: 'installerController',
          slaveComponents: [],
          services: [
            Em.Object.create({serviceName: 'HDFS', isSelected: true})
          ],
          hostsObj: [
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({isInstalled: false, title: 'Client'}),
                Em.Object.create({isInstalled: false, title: 'DataNode', checked: true}),
                Em.Object.create({isInstalled: false, title: ''})
              ]),
              hasMaster: true
            })
          ],
          m: 'host with masters, empty slaveComponents, controllerName - installerController, one datanode checked',
          e: [
            [true, true, true]
          ]
        },
        {
          controllerName: 'installerController',
          slaveComponents: [],
          services: [
            Em.Object.create({serviceName: 'HDFS', isSelected: true})
          ],
          hostsObj: [
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({isInstalled: false, title: 'Client'}),
                Em.Object.create({isInstalled: false, title: 'DataNode', checked: false}),
                Em.Object.create({isInstalled: false, title: ''})
              ]),
              hasMaster: true
            }),
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({isInstalled: false, title: 'Client'}),
                Em.Object.create({isInstalled: false, title: 'DataNode', checked: true}),
                Em.Object.create({isInstalled: false, title: ''})
              ]),
              hasMaster: true
            })
          ],
          m: 'hosts with masters, empty slaveComponents, controllerName - installerController, one datanode checked',
          e: [
            [false, false, false],
            [true, true, true]
          ]
        },
        {
          controllerName: 'installerController',
          slaveComponents: [],
          services: [
            Em.Object.create({serviceName: 'HDFS', isSelected: true})
          ],
          hostsObj: [
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({isInstalled: false, title: 'Client'}),
                Em.Object.create({isInstalled: false, title: 'DataNode', checked: false}),
                Em.Object.create({isInstalled: false, title: ''})
              ]),
              hasMaster: true
            }),
            Em.Object.create({
              checkboxes: Em.A([
                Em.Object.create({isInstalled: false, title: 'Client'}),
                Em.Object.create({isInstalled: false, title: 'DataNode', checked: true}),
                Em.Object.create({isInstalled: false, title: ''})
              ]),
              hasMaster: false
            })
          ],
          m: 'some hosts with masters, empty slaveComponents, controllerName - installerController, one datanode checked',
          e: [
            [false, false, false],
            [true, true, true]
          ]
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          controller.set('content.slaveComponents', test.slaveComponents);
          controller.set('content.controllerName', test.controllerName);
          if (test.services) {
            controller.set('content.services', test.services);
          }
          controller.set('isMasters', false);
          controller.loadStep();
          var r = controller.renderSlaves(test.hostsObj);
          expect(r.map(function (i) {
            return i.get('checkboxes').map(function (j) {
              return j.get('checked');
            });
          })).to.eql(test.e);
        });
      });

    Em.A([
        {
          slaveComponents: [
            {componentName: 'c1', hosts: [
              {hostName: 'h1', isInstalled: false},
              {hostName: 'h2', isInstalled: false}
            ]},
            {componentName: 'c2', hosts: [
              {hostName: 'h1', isInstalled: false},
              {hostName: 'h2', isInstalled: false}
            ]}
          ],
          headers: [
            Em.Object.create({name: 'c1', label: 'C1'}),
            Em.Object.create({name: 'c2', label: 'C2'})
          ],
          hostsObj: [
            Em.Object.create({
              hostName: 'h1',
              checkboxes: [
                Em.Object.create({
                  title: 'C1',
                  checked: false,
                  isInstalled: false
                }),
                Em.Object.create({
                  title: 'C2',
                  checked: false,
                  isInstalled: false
                })
              ]
            }),
            Em.Object.create({
              hostName: 'h2',
              checkboxes: [
                Em.Object.create({
                  title: 'C1',
                  checked: false,
                  isInstalled: false
                }),
                Em.Object.create({
                  title: 'C2',
                  checked: false,
                  isInstalled: false
                })
              ]
            })
          ],
          m: 'all Checked, nothing installed before',
          e: {
            checked: [
              [true, true],
              [true, true]
            ],
            isInstalled: [
              [false, false],
              [false, false]
            ]
          }
        },
        {
          slaveComponents: [
            {componentName: 'c1', hosts: [
              {hostName: 'h1', isInstalled: true},
              {hostName: 'h2', isInstalled: true}
            ]},
            {componentName: 'c2', hosts: [
              {hostName: 'h1', isInstalled: true},
              {hostName: 'h2', isInstalled: true}
            ]}
          ],
          headers: [
            Em.Object.create({name: 'c1', label: 'C1'}),
            Em.Object.create({name: 'c2', label: 'C2'})
          ],
          hostsObj: [
            Em.Object.create({
              hostName: 'h1',
              checkboxes: [
                Em.Object.create({
                  title: 'C1',
                  checked: false,
                  isInstalled: false
                }),
                Em.Object.create({
                  title: 'C2',
                  checked: false,
                  isInstalled: false
                })
              ]
            }),
            Em.Object.create({
              hostName: 'h2',
              checkboxes: [
                Em.Object.create({
                  title: 'C1',
                  checked: false,
                  isInstalled: false
                }),
                Em.Object.create({
                  title: 'C2',
                  checked: false,
                  isInstalled: false
                })
              ]
            })
          ],
          m: 'all Checked, all installed before',
          e: {
            checked: [
              [true, true],
              [true, true]
            ],
            isInstalled: [
              [true, true],
              [true, true]
            ]
          }
        },
        {
          slaveComponents: [
            {componentName: 'c1', hosts: [
              {hostName: 'h1', isInstalled: true}
            ]},
            {componentName: 'c2', hosts: [
              {hostName: 'h2', isInstalled: true}
            ]}
          ],
          headers: [
            Em.Object.create({name: 'c1', label: 'C1'}),
            Em.Object.create({name: 'c2', label: 'C2'})
          ],
          hostsObj: [
            Em.Object.create({
              hostName: 'h1',
              checkboxes: [
                Em.Object.create({
                  title: 'C1',
                  checked: false,
                  isInstalled: false
                }),
                Em.Object.create({
                  title: 'C2',
                  checked: false,
                  isInstalled: false
                })
              ]
            }),
            Em.Object.create({
              hostName: 'h2',
              checkboxes: [
                Em.Object.create({
                  title: 'C1',
                  checked: false,
                  isInstalled: false
                }),
                Em.Object.create({
                  title: 'C2',
                  checked: false,
                  isInstalled: false
                })
              ]
            })
          ],
          m: 'some Checked, some installed before',
          e: {
            checked: [
              [true, false],
              [false, true]
            ],
            isInstalled: [
              [true, false],
              [false, true]
            ]
          }
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          controller.set('content.slaveComponentHosts', test.slaveComponents);
          controller.set('headers', test.headers);
          var r = controller.renderSlaves(test.hostsObj);
          var checked = r.map(function (i) {
            return i.get('checkboxes').map(function (j) {
              return j.get('checked')
            })
          });
          var isInstalled = r.map(function (i) {
            return i.get('checkboxes').map(function (j) {
              return j.get('isInstalled')
            })
          });
          expect(checked).to.eql(test.e.checked);
          expect(isInstalled).to.eql(test.e.isInstalled);
        });
      });
  });

});