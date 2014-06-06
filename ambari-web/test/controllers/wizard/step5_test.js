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
require('controllers/wizard/step5_controller');
require('utils/ajax/ajax');
var c;
describe('App.WizardStep5Controller', function () {
  beforeEach(function() {
    c = App.WizardStep5Controller.create();
  });
  var controller = App.WizardStep5Controller.create();
  controller.set('content', {});
  var cpu = 2, memory = 4;

  var schemes = Em.A([
    {'description': 'empty condition'},
    {
      'description': 'second host if amount more than 1',
      "else": 1
    },
    {
      'description': 'first host if amount less than 3, third host if amount less than 6, fourth host if amount more than 5',
      "3": 0,
      "6": 2,
      "else": 3
    },
    {
      'description': 'second host if amount less than 3, second host if amount less than 6, third host if amount less than 31, sixth host if amount more than 30',
      "3": 1,
      "6": 1,
      "31": 2,
      "else": 5
    }
  ]);

  var test_config = [
    {
      title: '1 host',
      hosts: ['host0'],
      equals: [0, 0, 0, 0]
    },
    {
      title: '2 hosts',
      hosts: ['host0', 'host1'],
      equals: [0, 1, 0, 1]
    },
    {
      title: '3 hosts',
      hosts: ['host0', 'host1', 'host2'],
      equals: [0, 1, 2, 1]
    },
    {
      title: '5 hosts',
      hosts: ['host0', 'host1', 'host2', 'host3', 'host4'],
      equals: [0, 1, 2, 1]
    },
    {
      title: '6 hosts',
      hosts: ['host0', 'host1', 'host2', 'host3', 'host4', 'host6'],
      equals: [0, 1, 3, 2]
    },
    {
      title: '10 hosts',
      hosts: ['host0', 'host1', 'host2', 'host3', 'host4', 'host5', 'host6', 'host7', 'host8', 'host9'],
      equals: [0, 1, 3, 2]
    },
    {
      title: '31 hosts',
      hosts: ['host0', 'host1', 'host2', 'host3', 'host4', 'host5', 'host6', 'host7', 'host8', 'host9', 'host10', 'host11', 'host12', 'host13', 'host14', 'host15', 'host16', 'host17', 'host18', 'host19', 'host20', 'host21', 'host22', 'host23', 'host24', 'host25', 'host26', 'host27', 'host28', 'host29', 'host30'],
      equals: [0, 1, 3, 5]
    }
  ];

  schemes.forEach(function(scheme, index) {
    describe('#getHostForComponent() condition: ' + scheme.description, function() {

      delete scheme['description'];

      test_config.forEach(function(test) {
        it(test.title, function () {
          controller.get('hosts').clear();
          test.hosts.forEach(function(_host) {
            controller.get('hosts').pushObject(Em.Object.create({
              host_name: _host,
              cpu: cpu,
              memory: memory
            }));
          });
          expect(controller.getHostForComponent(test.hosts.length, scheme).host_name).to.equal(test.hosts[test.equals[index]]);
        });
      });
    });
  });

  describe('#getZooKeeperServer', function() {
    it('should be array with three host names if hosts number more than three', function() {
      var hosts = [
        {host_name: 'host1'},
        {host_name: 'host2'},
        {host_name: 'host3'}
      ];

      controller.set('hosts', hosts);
      expect(controller.getZooKeeperServer(hosts.length)).to.eql(['host1', 'host2', 'host3']);
    });

    it('should be array with one host names if hosts number less than three', function() {
      var hosts = [
        {host_name: 'host1'},
        {host_name: 'host2'}
      ];

      controller.set('hosts', hosts);
      expect(controller.getZooKeeperServer(hosts.length)).to.eql(['host1']);
    });
  });

  describe('#getServerHost', function() {
    it('should be host name if one host ', function() {
      var hosts = [
        {host_name: 'host1'}
      ];

      controller.set('hosts', hosts);
      expect(controller.getServerHost(hosts.length)).to.eql('host1');
    });

    it('should be host name if hosts number more than one', function() {
      var hosts = [
        {host_name: 'host1'},
        {host_name: 'host2'}
      ];

      controller.set('hosts', hosts);
      expect(controller.getServerHost(hosts.length)).to.eql('host1');
    });

    it('should be host name different from localhost if hosts number more than one', function() {
      var hosts = [
        {host_name: location.hostname},
        {host_name: 'host2'}
      ];
      //first host_name is empty string, because of location.hostname = "" in console,
      //to implement current test case

      controller.set('hosts', hosts);
      expect(controller.getServerHost(hosts.length)).to.eql('host2');
    });
  });


  controller.set('content', {});

  describe('#isReassignWizard', function() {
    it('true if content.controllerName is reassignMasterController', function() {
      controller.set('content.controllerName', 'reassignMasterController');
      expect(controller.get('isReassignWizard')).to.equal(true);
    });
    it('false if content.controllerName is not reassignMasterController', function() {
      controller.set('content.controllerName', 'mainController');
      expect(controller.get('isReassignWizard')).to.equal(false);
    });
  });

  describe('#isAddServiceWizard', function() {
    it('true if content.controllerName is addServiceController', function() {
      controller.set('content.controllerName', 'addServiceController');
      expect(controller.get('isAddServiceWizard')).to.equal(true);
    });
    it('false if content.controllerName is not addServiceController', function() {
      controller.set('content.controllerName', 'mainController');
      expect(controller.get('isAddServiceWizard')).to.equal(false);
    });
  });

  describe('#isReassignHive', function() {

    beforeEach(function() {
      sinon.stub(controller, 'getIsSubmitDisabled', Em.K);
    });

    afterEach(function() {
      controller.getIsSubmitDisabled.restore();
    });

    var tests = Em.A([
      {
        servicesMasters: Em.A([{component_name: 'HIVE_SERVER'}]),
        controllerName: 'reassignMasterController',
        e: true
      },
      {
        servicesMasters: Em.A([{component_name: 'HIVE_SERVER'}]),
        controllerName: 'addServiceController',
        e: false
      },
      {
        servicesMasters: Em.A([{component_name: 'ZOOKEEPER_SERVER'}]),
        controllerName: 'reassignMasterController',
        e: false
      },
      {
        servicesMasters: Em.A([{component_name: 'ZOOKEEPER_SERVER'}]),
        controllerName: 'addServiceController',
        e: false
      }
    ]);

    tests.forEach(function(test) {
      it(test.controllerName + ' ' + test.servicesMasters.mapProperty('component_name').join(','), function() {
        controller.set('content.controllerName', test.controllerName);
        controller.set('servicesMasters', test.servicesMasters);
        expect(controller.get('isReassignHive')).to.equal(test.e);
      });
    });

  });

  describe('#sortHosts', function() {

    var tests = Em.A([
      {
        hosts: [
          Em.Object.create({memory: 4, cpu: 1, host_name: 'host1', id: 1}),
          Em.Object.create({memory: 3, cpu: 1, host_name: 'host2', id: 2}),
          Em.Object.create({memory: 2, cpu: 1, host_name: 'host3', id: 3}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host4', id: 4})
        ],
        m: 'memory',
        e: [1,2,3,4]
      },
      {
        hosts: [
          Em.Object.create({memory: 1, cpu: 4, host_name: 'host1', id: 1}),
          Em.Object.create({memory: 1, cpu: 3, host_name: 'host2', id: 2}),
          Em.Object.create({memory: 1, cpu: 2, host_name: 'host3', id: 3}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host4', id: 4})
        ],
        m: 'cpu',
        e: [1,2,3,4]
      },
      {
        hosts: [
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host4', id: 1}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host2', id: 2}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host3', id: 3}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host1', id: 4})
        ],
        m: 'host_name',
        e: [4,2,3,1]
      },
      {
        hosts: [
          Em.Object.create({memory: 2, cpu: 1, host_name: 'host1', id: 1}),
          Em.Object.create({memory: 1, cpu: 2, host_name: 'host3', id: 2}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host4', id: 3}),
          Em.Object.create({memory: 1, cpu: 1, host_name: 'host2', id: 4})
        ],
        m: 'mix',
        e: [1,2,4,3]
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        var hosts = Em.copy(test.hosts);
        controller.sortHosts(hosts);
        expect(Em.A(hosts).mapProperty('id')).to.eql(test.e);
      });
    });

  });

  describe('#renderHostInfo', function() {

    var tests = Em.A([
      {
        hosts: {
          h1: {memory: 4, cpu: 1, name: 'host1', bootStatus: 'INIT'},
          h2: {memory: 3, cpu: 1, name: 'host2', bootStatus: 'INIT'},
          h3: {memory: 2, cpu: 1, name: 'host3', bootStatus: 'INIT'},
          h4: {memory: 1, cpu: 1, name: 'host4', bootStatus: 'INIT'}
        },
        m: 'no one host is REGISTERED',
        e: []
      },
      {
        hosts: {
          h1: {memory: 4, cpu: 1, name: 'host1', bootStatus: 'REGISTERED'},
          h2: {memory: 3, cpu: 1, name: 'host2', bootStatus: 'REGISTERED'},
          h3: {memory: 2, cpu: 1, name: 'host3', bootStatus: 'REGISTERED'},
          h4: {memory: 1, cpu: 1, name: 'host4', bootStatus: 'REGISTERED'}
        },
        m: 'all hosts are REGISTERED, memory',
        e: ['host1', 'host2', 'host3', 'host4']
      },
      {
        hosts: {
          h1: {memory: 1, cpu: 4, name: 'host1', bootStatus: 'REGISTERED'},
          h2: {memory: 1, cpu: 3, name: 'host2', bootStatus: 'REGISTERED'},
          h3: {memory: 1, cpu: 2, name: 'host3', bootStatus: 'REGISTERED'},
          h4: {memory: 1, cpu: 1, name: 'host4', bootStatus: 'REGISTERED'}
        },
        m: 'all hosts are REGISTERED, cpu',
        e: ['host1', 'host2', 'host3', 'host4']
      },
      {
        hosts: {
          h1: {memory: 1, cpu: 1, name: 'host4', bootStatus: 'REGISTERED'},
          h2: {memory: 1, cpu: 1, name: 'host2', bootStatus: 'REGISTERED'},
          h3: {memory: 1, cpu: 1, name: 'host3', bootStatus: 'REGISTERED'},
          h4: {memory: 1, cpu: 1, name: 'host1', bootStatus: 'REGISTERED'}
        },
        m: 'all hosts are REGISTERED, host_name',
        e: ['host1', 'host2', 'host3', 'host4']
      },
      {
        hosts: {
          h1: {memory: 2, cpu: 1, name: 'host1', bootStatus: 'REGISTERED'},
          h2: {memory: 1, cpu: 2, name: 'host3', bootStatus: 'INIT'},
          h3: {memory: 1, cpu: 1, name: 'host4', bootStatus: 'REGISTERED'},
          h4: {memory: 1, cpu: 1, name: 'host2', bootStatus: 'INIT'}
        },
        m: 'mix',
        e: ['host1', 'host4']
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        controller.set('content', {hosts: test.hosts});
        controller.renderHostInfo();
        var r = controller.get('hosts');
        expect(Em.A(r).mapProperty('host_name')).to.eql(test.e);
      });
    });

  });

  describe('#hasHiveServer', function() {

    var tests = Em.A([
      {
        selectedServicesMasters: Em.A([{component_name: 'HIVE_SERVER'}]),
        controllerName: 'reassignMasterController',
        e: false
      },
      {
        selectedServicesMasters: Em.A([{component_name: 'HIVE_SERVER'}]),
        controllerName: 'addServiceController',
        e: true
      },
      {
        selectedServicesMasters: Em.A([{component_name: 'ANOTHER'}]),
        controllerName: 'addServiceController',
        e: false
      },
      {
        selectedServicesMasters: Em.A([{component_name: 'ANOTHER'}]),
        controllerName: 'reassignMasterController',
        e: false
      }
    ]);

    tests.forEach(function(test) {
      it(test.controllerName + ' ' + test.selectedServicesMasters.mapProperty('component_name').join(','), function() {
        controller.set('content.controllerName', test.controllerName);
        controller.set('selectedServicesMasters', test.selectedServicesMasters);
        expect(controller.get('hasHiveServer')).to.equal(test.e);
      });
    });

  });

  describe('#selectHost', function() {

    var tests = Em.A([
      {componentName: 'KERBEROS_SERVER', hostsCount: 1, e: 'host1'},
      {componentName: 'KERBEROS_SERVER', hostsCount: 3, e: 'host2'},
      {componentName: 'KERBEROS_SERVER', hostsCount: 6, e: 'host4'},
      {componentName: 'KERBEROS_SERVER', hostsCount: 31, e: 'host6'},
      {componentName: 'KERBEROS_SERVER', hostsCount: 32, e: 'host6'},
      {componentName: 'NAMENODE', hostsCount: 1, e: 'host1'},
      {componentName: 'NAMENODE', hostsCount: 2, e: 'host1'},
      {componentName: 'SECONDARY_NAMENODE', hostsCount: 1, e: 'host1'},
      {componentName: 'SECONDARY_NAMENODE', hostsCount: 2, e: 'host2'},
      {componentName: 'JOBTRACKER', hostsCount: 1, e: 'host1'},
      {componentName: 'JOBTRACKER', hostsCount: 3, e: 'host2'},
      {componentName: 'JOBTRACKER', hostsCount: 6, e: 'host2'},
      {componentName: 'JOBTRACKER', hostsCount: 31, e: 'host3'},
      {componentName: 'JOBTRACKER', hostsCount: 32, e: 'host3'},
      {componentName: 'HISTORYSERVER', hostsCount: 1, e: 'host1'},
      {componentName: 'HISTORYSERVER', hostsCount: 3, e: 'host2'},
      {componentName: 'HISTORYSERVER', hostsCount: 6, e: 'host2'},
      {componentName: 'HISTORYSERVER', hostsCount: 31, e: 'host3'},
      {componentName: 'HISTORYSERVER', hostsCount: 32, e: 'host3'},
      {componentName: 'RESOURCEMANAGER', hostsCount: 1, e: 'host1'},
      {componentName: 'RESOURCEMANAGER', hostsCount: 3, e: 'host2'},
      {componentName: 'RESOURCEMANAGER', hostsCount: 6, e: 'host2'},
      {componentName: 'RESOURCEMANAGER', hostsCount: 31, e: 'host3'},
      {componentName: 'RESOURCEMANAGER', hostsCount: 32, e: 'host3'},
      {componentName: 'HBASE_MASTER', hostsCount: 1, e: ['host1']},
      {componentName: 'HBASE_MASTER', hostsCount: 3, e: ['host1']},
      {componentName: 'HBASE_MASTER', hostsCount: 6, e: ['host3']},
      {componentName: 'HBASE_MASTER', hostsCount: 31, e: ['host4']},
      {componentName: 'HBASE_MASTER', hostsCount: 32, e: ['host4']},
      {componentName: 'OOZIE_SERVER', hostsCount: 1, e: 'host1'},
      {componentName: 'OOZIE_SERVER', hostsCount: 3, e: 'host2'},
      {componentName: 'OOZIE_SERVER', hostsCount: 6, e: 'host3'},
      {componentName: 'OOZIE_SERVER', hostsCount: 31, e: 'host4'},
      {componentName: 'OOZIE_SERVER', hostsCount: 32, e: 'host4'},
      {componentName: 'HIVE_SERVER', hostsCount: 1, e: 'host1'},
      {componentName: 'HIVE_SERVER', hostsCount: 3, e: 'host2'},
      {componentName: 'HIVE_SERVER', hostsCount: 6, e: 'host3'},
      {componentName: 'HIVE_SERVER', hostsCount: 31, e: 'host5'},
      {componentName: 'HIVE_SERVER', hostsCount: 32, e: 'host5'},
      {componentName: 'HIVE_METASTORE', hostsCount: 1, e: 'host1'},
      {componentName: 'HIVE_METASTORE', hostsCount: 3, e: 'host2'},
      {componentName: 'HIVE_METASTORE', hostsCount: 6, e: 'host3'},
      {componentName: 'HIVE_METASTORE', hostsCount: 31, e: 'host5'},
      {componentName: 'HIVE_METASTORE', hostsCount: 32, e: 'host5'},
      {componentName: 'WEBHCAT_SERVER', hostsCount: 1, e: 'host1'},
      {componentName: 'WEBHCAT_SERVER', hostsCount: 3, e: 'host2'},
      {componentName: 'WEBHCAT_SERVER', hostsCount: 6, e: 'host3'},
      {componentName: 'WEBHCAT_SERVER', hostsCount: 31, e: 'host5'},
      {componentName: 'WEBHCAT_SERVER', hostsCount: 32, e: 'host5'},
      {componentName: 'APP_TIMELINE_SERVER', hostsCount: 1, e: 'host1'},
      {componentName: 'APP_TIMELINE_SERVER', hostsCount: 3, e: 'host2'},
      {componentName: 'APP_TIMELINE_SERVER', hostsCount: 6, e: 'host2'},
      {componentName: 'APP_TIMELINE_SERVER', hostsCount: 31, e: 'host3'},
      {componentName: 'APP_TIMELINE_SERVER', hostsCount: 32, e: 'host3'},
      {componentName: 'FALCON_SERVER', hostsCount: 1, e: 'host1'},
      {componentName: 'FALCON_SERVER', hostsCount: 3, e: 'host2'},
      {componentName: 'FALCON_SERVER', hostsCount: 6, e: 'host3'},
      {componentName: 'FALCON_SERVER', hostsCount: 31, e: 'host4'},
      {componentName: 'FALCON_SERVER', hostsCount: 32, e: 'host4'}
    ]);

    tests.forEach(function(test) {
      it(test.componentName + ' ' + test.hostsCount, function() {
        controller.set('hosts', d3.range(1, test.hostsCount + 1).map(function(i) { return {host_name: 'host' + i.toString()};}));
        expect(controller.selectHost(test.componentName)).to.eql(test.e);
      });
    });

    describe('getServerHost should be called for', function() {
      Em.A(['STORM_UI_SERVER','DRPC_SERVER','STORM_REST_API','NIMBUS','GANGLIA_SERVER','NAGIOS_SERVER','HUE_SERVER']).forEach(function(componentName) {
        it(componentName, function() {
          sinon.spy(controller, 'getServerHost');
          controller.selectHost(componentName);
          expect(controller.getServerHost.calledOnce).to.equal(true);
          controller.getServerHost.restore();
        });
      });
    });

  });

  describe('#last', function() {

    var tests = Em.A([
      {
        selectedServicesMasters: Em.A([
          {component_name: 'c1', indx: 1},
          {component_name: 'c2', indx: 2},
          {component_name: 'c1', indx: 2}
        ]),
        m: 'Components exists',
        c: 'c1',
        e: 2
      },
      {
        selectedServicesMasters: Em.A([
          {component_name: 'c1', indx: 1},
          {component_name: 'c2', indx: 2},
          {component_name: 'c1', indx: 2}
        ]),
        m: 'Components don\'t exists',
        c: 'c3',
        e: null
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        controller.set('selectedServicesMasters', test.selectedServicesMasters);
        if (!Em.isNone(test.e)) {
          expect(controller.last(test.c).indx).to.equal(test.e);
        }
        else {
          expect(Em.isNone(controller.last(test.c))).to.equal(true);
        }
      })
    });

  });

  describe('#getIsSubmitDisabled', function() {
    it('should base on selected host to masters if it\'s not a isReassignWizard', function() {
      c.set('controllerName', 'addServiceController');
      c.reopen({servicesMasters: [{isHostNameValid: true}, {isHostNameValid: false}]});
      c.getIsSubmitDisabled();
      expect(c.get('submitDisabled')).to.equal(true);
      c.reopen({servicesMasters: [{isHostNameValid: true}, {isHostNameValid: true}]});
      c.getIsSubmitDisabled();
      expect(c.get('submitDisabled')).to.equal(false);
    });
  });

  describe('#remainingHosts', function() {
    it('should show count of hosts without masters', function() {
      c.reopen({masterHostMapping: [{}]});
      c.set('hosts', [{},{},{}]);
      expect(c.get('remainingHosts')).to.equal(2);
    });
  });

  describe('#clearStep', function() {
    var tests = Em.A([
      {p: 'hosts'},
      {p: 'selectedServicesMasters'},
      {p: 'servicesMasters'}
    ]);
    tests.forEach(function(test) {
      it('should cleanup ' + test.p, function() {
        c.set(test.p, [Em.Object.create({}),Em.Object.create({})]);
        c.clearStep();
        expect(c.get(test.p).length).to.equal(0);
      });
    });
  });

  describe('#updateComponent', function() {
    var tests = Em.A([
      {
        componentName: 'HBASE_SERVER',
        services: Em.A([
          Em.Object.create({isInstalled: true, serviceName: 'HBASE'})
        ]),
        selectedServicesMasters: Em.A([
          Em.Object.create({showAddControl: false, showRemoveControl: true, component_name: 'HBASE_SERVER'}),
          Em.Object.create({showAddControl: true, showRemoveControl: false, component_name: 'HBASE_SERVER'})
        ]),
        hosts: Em.A([
          Em.Object.create({})
        ]),
        controllerName: 'addServiceController',
        m: 'service is installed',
        e: {
          showAddControl: true,
          showRemoveControl: false
        }
      },
      {
        componentName: 'HBASE_SERVER',
        services: Em.A([
          Em.Object.create({isInstalled: false, serviceName: 'HBASE'})
        ]),
        selectedServicesMasters: Em.A([
          Em.Object.create({showAddControl: true, showRemoveControl: false, component_name: 'HBASE_SERVER'})
        ]),
        hosts: Em.A([
          Em.Object.create({})
        ]),
        controllerName: 'addServiceController',
        m: 'service not installed, but all host already have provided component',
        e: {
          showAddControl: true,
          showRemoveControl: false
        }
      },
      {
        componentName: 'HBASE_SERVER',
        services: Em.A([
          Em.Object.create({isInstalled: false, serviceName: 'HBASE'})
        ]),
        selectedServicesMasters: Em.A([
          Em.Object.create({showAddControl: false, showRemoveControl: true, component_name: 'HBASE_SERVER'})
        ]),
        hosts: Em.A([
          Em.Object.create({}),
          Em.Object.create({})
        ]),
        controllerName: 'addServiceController',
        m: 'service not installed, not all host already have provided component',
        e: {
          showAddControl: true,
          showRemoveControl: true
        }
      },
      {
        componentName: 'HBASE_SERVER',
        services: Em.A([
          Em.Object.create({isInstalled: false, serviceName: 'HBASE'})
        ]),
        selectedServicesMasters: Em.A([
          Em.Object.create({showAddControl: false, showRemoveControl: true, component_name: 'HBASE_SERVER'})
        ]),
        hosts: Em.A([
          Em.Object.create({}),
          Em.Object.create({})
        ]),
        controllerName: 'reassignMasterController',
        m: 'service not installed, not all host already have provided component, but is reassignMasterController',
        e: {
          showAddControl: false,
          showRemoveControl: false
        }
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        c.reopen({
          content: Em.Object.create({
            services: test.services,
            controllerName: test.controllerName
          }),
          selectedServicesMasters: test.selectedServicesMasters
        });
        c.updateComponent(test.componentName);
        Em.keys(test.e).forEach(function(k) {
          expect(c.last(test.componentName).get(k)).to.equal(test.e[k]);
        });
      });
    });
  });

  describe('#renderComponents', function() {

    var tests = Em.A([
      {
        masterComponents: Em.A([
          {component_name: 'ZOOKEEPER_SERVER'}
        ]),
        services: Em.A([]),
        controllerName: 'reassignMasterController',
        m: 'One component',
        isHaEnabled: false,
        component_name: 'ZOOKEEPER_SERVER',
        e: {
          selectedServicesMasters: ['ZOOKEEPER_SERVER'],
          servicesMasters: ['ZOOKEEPER_SERVER'],
          showRemoveControl: [false],
          isInstalled: [false],
          zId: [1]
        }
      },
      {
        masterComponents: Em.A([
          {component_name: 'ZOOKEEPER_SERVER'},
          {component_name: 'SECONDARY_NAMENODE'}
        ]),
        services: Em.A([]),
        controllerName: 'addServiceController',
        m: 'One component',
        isHaEnabled: true,
        component_name: 'ZOOKEEPER_SERVER',
        e: {
          selectedServicesMasters: ['ZOOKEEPER_SERVER'],
          servicesMasters: ['ZOOKEEPER_SERVER'],
          showRemoveControl: [false],
          zId: [1]
        }
      },
      {
        masterComponents: Em.A([
          {component_name: 'ZOOKEEPER_SERVER'},
          {component_name: 'ZOOKEEPER_SERVER'}
        ]),
        services: Em.A([
          Em.Object.create({serviceName:'ZOOKEEPER', isInstalled: true})
        ]),
        controllerName: 'addServiceController',
        m: 'Two components, but service is installed',
        isHaEnabled: false,
        component_name: 'ZOOKEEPER_SERVER',
        e: {
          selectedServicesMasters: ['ZOOKEEPER_SERVER', 'ZOOKEEPER_SERVER'],
          servicesMasters: ['ZOOKEEPER_SERVER', 'ZOOKEEPER_SERVER'],
          showRemoveControl: [false, false],
          zId: [1, 2]
        }
      },
      {
        masterComponents: Em.A([
          {component_name: 'ZOOKEEPER_SERVER'},
          {component_name: 'ZOOKEEPER_SERVER'},
          {component_name: 'NAMENODE'}
        ]),
        services: Em.A([
        ]),
        controllerName: 'addServiceController',
        m: 'Two components, but service is installed',
        isHaEnabled: false,
        component_name: 'ZOOKEEPER_SERVER',
        e: {
          selectedServicesMasters: ['ZOOKEEPER_SERVER', 'ZOOKEEPER_SERVER', 'NAMENODE'],
          servicesMasters: ['ZOOKEEPER_SERVER', 'ZOOKEEPER_SERVER', 'NAMENODE'],
          showRemoveControl: [true, true, undefined],
          zId: [1, 2, 1]
        }
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        sinon.stub(App, 'get', function(k) {
          if ('isHaEnabled' === k) return test.isHaEnabled;
          return Em.get(App, k);
        });
        c.reopen({
          content: Em.Object.create({
            getIsSubmitDisabled: Em.K,
            services: test.services,
            controllerName: test.controllerName,
            reassign: {component_name: test.component_name}
          })
        });
        c.renderComponents(test.masterComponents);
        App.get.restore();
        expect(c.get('selectedServicesMasters').mapProperty('component_name')).to.eql(test.e.selectedServicesMasters);
        expect(c.get('servicesMasters').mapProperty('component_name')).to.eql(test.e.servicesMasters);
        expect(c.get('selectedServicesMasters').mapProperty('showRemoveControl')).to.eql(test.e.showRemoveControl);
        expect(c.get('selectedServicesMasters').mapProperty('zId')).to.eql(test.e.zId);
        if (c.get('isReasignController')) {
          expect(c.get('servicesMasters').mapProperty('isInstalled')).to.eql(test.e.isInstalled);
        }
      });
    });
  });

  describe('#updateHiveCoHosts', function() {
    var tests = Em.A([
      {
        selectedServicesMasters: Em.A([
          Em.Object.create({component_name: 'HIVE_SERVER', selectedHost: 'h1'}),
          Em.Object.create({component_name: 'HIVE_METASTORE', selectedHost: 'h2'}),
          Em.Object.create({component_name: 'WEBHCAT_SERVER', selectedHost: 'h3'})
        ]),
        servicesMasters: Em.A([
          Em.Object.create({component_name: 'HIVE_SERVER', selectedHost: 'h1'})
        ]),
        isReassignHive: false,
        m: 'should set new host for both',
        e: ['h1','h1','h1']
      },
      {
        selectedServicesMasters: Em.A([
          Em.Object.create({component_name: 'HIVE_SERVER', selectedHost: 'h1'}),
          Em.Object.create({component_name: 'HIVE_METASTORE', selectedHost: 'h2'}),
          Em.Object.create({component_name: 'WEBHCAT_SERVER', selectedHost: 'h3'})
        ]),
        servicesMasters: Em.A([
          Em.Object.create({component_name: 'HIVE_METASTORE', selectedHost: 'h1'})
        ]),
        isReassignHive: false,
        m: 'should set new host for WEBHCAT_SERVER',
        e: ['h1','h2','h1']
      },
      {
        selectedServicesMasters: Em.A([
          Em.Object.create({component_name: 'HIVE_METASTORE', selectedHost: 'h2'}),
          Em.Object.create({component_name: 'WEBHCAT_SERVER', selectedHost: 'h3'})
        ]),
        servicesMasters: Em.A([
          Em.Object.create({component_name: 'HIVE_METASTORE', selectedHost: 'h1'})
        ]),
        isReassignHive: false,
        m: 'missing HIVE_SERVER',
        e: ['h2','h3']
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        c.set('selectedServicesMasters', test.selectedServicesMasters);
        c.set('servicesMasters', test.servicesMasters);
        c.reopen({isReassignHive: test.isReassignHive});
        c.updateHiveCoHosts();
        expect(c.get('selectedServicesMasters').mapProperty('selectedHost')).to.eql(test.e);
      });
    });

  });

  describe('#assignHostToMaster', function() {
    var tests = Em.A([
      {
        componentName: 'c1',
        selectedHost: 'h2',
        zId: '1',
        e: {
          indx: 0
        }
      },
      {
        componentName: 'c2',
        selectedHost: 'h3',
        zId: '2',
        e: {
          indx: 3
        }
      },
      {
        componentName: 'c3',
        selectedHost: 'h1',
        e: {
          indx: 2
        }
      },
      {
        componentName: 'c2',
        selectedHost: 'h4',
        e: {
          indx: 1
        }
      }
    ]),
    selectedServicesMasters = Em.A([
      Em.Object.create({component_name: 'c1', zId: '1', selectedHost: 'h1'}),
      Em.Object.create({component_name: 'c2', zId: '1', selectedHost: 'h1'}),
      Em.Object.create({component_name: 'c3', zId: '1', selectedHost: 'h3'}),
      Em.Object.create({component_name: 'c2', zId: '2', selectedHost: 'h2'})
    ]);

    tests.forEach(function(test) {
      it(test.componentName + ' ' + test.selectedHost + ' ' + test.zId, function() {
        c.set('selectedServicesMasters', selectedServicesMasters);
        c.assignHostToMaster(test.componentName, test.selectedHost, test.zId);
        expect(c.get('selectedServicesMasters').objectAt(test.e.indx).get('selectedHost')).to.equal(test.selectedHost);
      })
    });
  });

  describe('#submit', function() {
    beforeEach(function() {
      if(!App.router) {
        App.router = Em.Object.create({send: Em.K});
      }
      sinon.stub(App.router, 'send', Em.K);
    });
    afterEach(function() {
      App.router.send.restore();
    });
    it('should go next if not isSubmitDisabled', function() {
      c.reopen({servicesMasters: [{isHostNameValid: true}]});
      c.submit();
      expect(App.router.send.calledWith('next')).to.equal(true);
    });
    it('shouldn\'t go next if isSubmitDisabled', function() {
      c.reopen({servicesMasters: [{isHostNameValid: false}]});
      c.submit();
      expect(App.router.send.called).to.equal(false);
    });
  });

  describe('#removeComponent', function() {
    var tests = Em.A([
      {
        componentName: 'c1',
        zId: 1,
        selectedServicesMasters: Em.A([]),
        hosts: [],
        m: 'empty selectedServicesMasters',
        e: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        zId: 1,
        selectedServicesMasters: Em.A([
          Em.Object.create({zId: 1, component_name: 'HBASE_SERVER'})
        ]),
        hosts: [],
        m: 'no such components',
        e: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        zId: 1,
        selectedServicesMasters: Em.A([
          Em.Object.create({zId: 1, component_name: 'ZOOKEPEER_SERVER'})
        ]),
        hosts: [],
        m: 'component is only 1',
        e: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        zId: 2,
        selectedServicesMasters: Em.A([
          Em.Object.create({zId: 1, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false}),
          Em.Object.create({zId: 2, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false})
        ]),
        hosts: [{},{}],
        m: 'two components, add allowed, remove not allowed',
        e: true,
        showAddControl: true,
        showRemoveControl: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        zId: 2,
        selectedServicesMasters: Em.A([
          Em.Object.create({zId: 1, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false}),
          Em.Object.create({zId: 2, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false})
        ]),
        hosts: [{}],
        m: 'two components, add not allowed, remove not allowed',
        e: true,
        showAddControl: false,
        showRemoveControl: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        zId: 2,
        selectedServicesMasters: Em.A([
          Em.Object.create({zId: 1, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false}),
          Em.Object.create({zId: 2, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false}),
          Em.Object.create({zId: 3, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: true})
        ]),
        hosts: [{},{}],
        m: 'three components, add not allowed, remove allowed',
        e: true,
        showAddControl: false,
        showRemoveControl: true
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        zId: 2,
        selectedServicesMasters: Em.A([
          Em.Object.create({zId: 1, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false}),
          Em.Object.create({zId: 2, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false}),
          Em.Object.create({zId: 3, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: true})
        ]),
        hosts: [{},{}, {}],
        m: 'three components, add allowed, remove allowed',
        e: true,
        showAddControl: true,
        showRemoveControl: true
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        c.set('selectedServicesMasters', test.selectedServicesMasters);
        c.set('hosts', test.hosts);
        expect(c.removeComponent(test.componentName, test.zId)).to.equal(test.e);
        if(test.e) {
          expect(c.get('selectedServicesMasters.lastObject.showRemoveControl')).to.equal(test.showRemoveControl);
          expect(c.get('selectedServicesMasters.lastObject.showAddControl')).to.equal(test.showAddControl);
        }
      })
    });
  });

  describe('#addComponent', function() {
    var tests = Em.A([
      {
        componentName: 'c1',
        selectedServicesMasters: Em.A([]),
        hosts: [],
        m: 'empty selectedServicesMasters',
        e: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        selectedServicesMasters: Em.A([
          Em.Object.create({zId: 1, component_name: 'HBASE_SERVER'})
        ]),
        hosts: [],
        m: 'no such components',
        e: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        selectedServicesMasters: Em.A([
          Em.Object.create({zId: 1, component_name: 'ZOOKEPEER_SERVER'})
        ]),
        hosts: [],
        m: 'one component, 0 hosts',
        e: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        selectedServicesMasters: Em.A([
          Em.Object.create({zId: 1, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false}),
          Em.Object.create({zId: 2, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false})
        ]),
        hosts: [Em.Object.create({}), Em.Object.create({})],
        m: 'two components, two hosts',
        e: false
      },
      {
        componentName: 'ZOOKEPEER_SERVER',
        selectedServicesMasters: Em.A([
          Em.Object.create({zId: 1, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false}),
          Em.Object.create({zId: 2, component_name: 'ZOOKEPEER_SERVER', showAddControl: false, showRemoveControl: false})
        ]),
        hosts: [Em.Object.create({}), Em.Object.create({}), Em.Object.create({})],
        m: 'two components, 3 hosts',
        e: true
      }
    ]);
    tests.forEach(function(test) {
      it(test.m, function() {
        c.set('selectedServicesMasters', test.selectedServicesMasters);
        c.set('hosts', test.hosts);
        expect(c.addComponent(test.componentName)).to.equal(test.e);
      });
    });
  });

  describe('#loadStep', function() {
    var methods = Em.A(['clearStep', 'renderHostInfo', 'renderComponents', 'loadComponents']);
    describe('should call several methods', function() {
      beforeEach(function() {
        methods.forEach(function(m) {
          sinon.spy(c, m);
        });
        c.reopen({content: {services: Em.A([])}});
      });
      afterEach(function() {
        methods.forEach(function(m) {
          c[m].restore();
        });
      });
      methods.forEach(function(m) {
        it(m, function() {
          c.loadStep();
          expect(c[m].calledOnce).to.equal(true);
        });
      });
    });
    it('should update HBASE if App.supports.multipleHBaseMasters is true', function() {
      App.set('supports.multipleHBaseMasters', true);
      sinon.spy(c, 'updateComponent');
      c.reopen({content: {services: Em.A([])}});
      c.loadStep();
      expect(c.updateComponent.calledTwice).to.equal(true);
      c.updateComponent.restore();
    });
  });

  describe('#title', function() {
    it('should be custom title for reassignMasterController', function() {
      c.set('content', {controllerName: 'reassignMasterController'});
      expect(c.get('title')).to.equal(Em.I18n.t('installer.step5.reassign.header'));
    });
    it('should be default for other', function() {
      c.set('content', {controllerName: 'notReassignMasterController'});
      expect(c.get('title')).to.equal(Em.I18n.t('installer.step5.header'));
    });
  });

  describe('#masterHostMapping', function() {
    Em.A([
        {
          selectedServicesMasters: [
            Em.Object.create({selectedHost: 'h1'}),
            Em.Object.create({selectedHost: 'h2'}),
            Em.Object.create({selectedHost: 'h1'})
          ],
          hosts: [
            Em.Object.create({host_name: 'h1', host_info: {}}),
            Em.Object.create({host_name: 'h2', host_info: {}})
          ],
          m: 'Two hosts',
          e: [
            {host_name: 'h1', hostInfo: {}, masterServices: [{}, {}]},
            {host_name: 'h2', hostInfo: {}, masterServices: [{}]}
          ]
        },
        {
          selectedServicesMasters: [],
          hosts: [],
          m: 'No hosts',
          e: []
        },
        {
          selectedServicesMasters: [
            Em.Object.create({selectedHost: 'h1'}),
            Em.Object.create({selectedHost: 'h1'})
          ],
          hosts: [
            Em.Object.create({host_name: 'h1', host_info: {}})
          ],
          m: 'One host',
          e: [
            {host_name: 'h1', hostInfo: {}, masterServices: [{}, {}]}
          ]
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          c.reopen({
            selectedServicesMasters: test.selectedServicesMasters,
            hosts: test.hosts
          });
          var result = c.get('masterHostMapping');
          expect(result.length).to.equal(test.e.length);
          result.forEach(function(r, i) {
            expect(r.get('host_name')).to.equal(test.e[i].host_name);
            expect(r.get('masterServices.length')).to.equal(test.e[i].masterServices.length);
            expect(r.get('hostInfo')).to.be.an.object;
          });
        });
      });
  });

  describe('#loadComponents', function() {
    Em.A([
        {
          services: [
            Em.Object.create({isSelected: true, serviceName: 's1'})
          ],
          masterComponents: Em.A([
            Em.Object.create({displayName: 'c1d', serviceName: 's1', componentName: 'c1', isShownOnInstallerAssignMasterPage: true})
          ]),
          masterComponentHosts: Em.A([
            {component: 'c1', hostName: 'h2', isInstalled: true}
          ]),
          selectHost: 'h3',
          m: 'savedComponent exists',
          e: {
            component_name: 'c1',
            display_name: 'c1d',
            selectedHost: 'h2',
            isInstalled: true,
            serviceId: 's1'
          }
        },
        {
          services: [
            Em.Object.create({isSelected: true, serviceName: 's1'})
          ],
          masterComponents: Em.A([
            Em.Object.create({displayName: 'c1d', serviceName: 's1', componentName: 'c1', isShownOnInstallerAssignMasterPage: true})
          ]),
          masterComponentHosts: Em.A([
            {component: 'c2', hostName: 'h2', isInstalled: true}
          ]),
          selectHost: 'h3',
          m: 'savedComponent doesn\'t exist',
          e: {
            component_name: 'c1',
            display_name: 'c1d',
            selectedHost: 'h3',
            isInstalled: false,
            serviceId: 's1'
          }
        },
        {
          services: [
            Em.Object.create({isSelected: true, serviceName: 's1'})
          ],
          masterComponents: Em.A([
            Em.Object.create({displayName: 'c1d', serviceName: 's1', componentName: 'ZOOKEEPER_SERVER', isShownOnInstallerAssignMasterPage: true})
          ]),
          masterComponentHosts: Em.A([
            {component: 'c1', hostName: 'h2', isInstalled: true}
          ]),
          selectHost: ['h3'],
          m: 'component ZOOKEEPER_SERVER',
          e: {
            component_name: 'ZOOKEEPER_SERVER',
            display_name: 'c1d',
            selectedHost: 'h3',
            isInstalled: false,
            serviceId: 's1',
            isHiveCoHost: false
          }
        },
        {
          services: [
            Em.Object.create({isSelected: true, serviceName: 's1'})
          ],
          masterComponents: Em.A([
            Em.Object.create({displayName: 'c1d', serviceName: 's1', componentName: 'HBASE_MASTER', isShownOnInstallerAssignMasterPage: true})
          ]),
          masterComponentHosts: Em.A([
            {component: 'c1', hostName: 'h2', isInstalled: true}
          ]),
          selectHost: ['h3'],
          m: 'component HBASE_MASTER',
          e: {
            component_name: 'HBASE_MASTER',
            display_name: 'c1d',
            selectedHost: 'h3',
            isInstalled: false,
            serviceId: 's1',
            isHiveCoHost: false
          }
        },
        {
          services: [
            Em.Object.create({isSelected: true, serviceName: 's1'})
          ],
          masterComponents: Em.A([
            Em.Object.create({displayName: 'c1d', serviceName: 's1', componentName: 'ZOOKEEPER_SERVER', isShownOnInstallerAssignMasterPage: true})
          ]),
          masterComponentHosts: Em.A([
            {component: 'ZOOKEEPER_SERVER', hostName: 'h2', isInstalled: true}
          ]),
          selectHost: ['h3'],
          m: 'component ZOOKEEPER_SERVER(2)',
          e: {
            component_name: 'ZOOKEEPER_SERVER',
            display_name: 'c1d',
            selectedHost: 'h2',
            isInstalled: true,
            serviceId: 's1',
            isHiveCoHost: false
          }
        },
        {
          services: [
            Em.Object.create({isSelected: true, serviceName: 's1'})
          ],
          masterComponents: Em.A([
            Em.Object.create({displayName: 'c1d', serviceName: 's1', componentName: 'HBASE_MASTER', isShownOnInstallerAssignMasterPage: true})
          ]),
          masterComponentHosts: Em.A([
            {component: 'HBASE_MASTER', hostName: 'h2', isInstalled: true}
          ]),
          selectHost: ['h3'],
          m: 'component HBASE_MASTER (2)',
          e: {
            component_name: 'HBASE_MASTER',
            display_name: 'c1d',
            selectedHost: 'h2',
            isInstalled: true,
            serviceId: 's1',
            isHiveCoHost: false
          }
        }
      ]).forEach(function (test) {
        it(test.m, function() {
          c.reopen({
            content: {
              services: test.services,
              masterComponentHosts: test.masterComponentHosts
            }
          });
          sinon.stub(App.StackServiceComponent, 'find', function() {
            return test.masterComponents;
          });
          sinon.stub(c, 'selectHost', function() {
            return test.selectHost;
          });
          var r = c.loadComponents();
          App.StackServiceComponent.find.restore();
          c.selectHost.restore();
          expect(r.length).to.equal(1);
          Em.keys(test.e).forEach(function(k) {
            expect(r[0][k]).to.equal(test.e[k]);
          });
        });
      });
  });

  describe('#_isHiveCoHost', function() {
    Em.A([
        {
          componentName: 'HIVE_METASTORE',
          isReassignWizard: false,
          e: true
        },
        {
          componentName: 'WEBHCAT_SERVER',
          isReassignWizard: false,
          e: true
        },
        {
          componentName: 'HIVE_METASTORE',
          isReassignWizard: true,
          e: false
        },
        {
          componentName: 'WEBHCAT_SERVER',
          isReassignWizard: true,
          e: false
        },
        {
          componentName: 'C1',
          isReassignWizard: false,
          e: false
        },
        {
          componentName: 'C1',
          isReassignWizard: true,
          e: false
        }
      ]).forEach(function (test) {
        it(test.componentName.toString() + ' ' + test.isReassignWizard.toString(), function () {
          c.reopen({
            isReassignWizard: test.isReassignWizard
          });
          var r = c._isHiveCoHost(test.componentName);
          expect(r).to.equal(test.e);
        });
      });
  });

  describe('#isHostNameValid', function() {

    beforeEach(function() {
      controller.set('hosts', [{host_name: 'h1'}]);
      controller.set('selectedServicesMasters', [{component_name: 'c1', selectedHost: 'h2'}]);
    });

    it('hostname is empty', function() {
      expect(controller.isHostNameValid('c1', '')).to.be.false;
    });

    it('hostname not exists', function() {
      expect(controller.isHostNameValid('c1', 'h2')).to.be.false;
    });

    it('hostname is assigned to such component', function() {
      controller.get('selectedServicesMasters').pushObject({component_name: 'c1', selectedHost: 'h2'});
      expect(controller.isHostNameValid('c1', 'h2')).to.be.false;
    });

    it('hostname is valid', function() {
      expect(controller.isHostNameValid('c1', 'h1')).to.be.true;
    });

  });

  describe('#updateIsHostNameValidFlag', function() {

    beforeEach(function() {
      controller.set('selectedServicesMasters', [
        Em.Object.create({component_name: 'ZOOKEEPER_SERVER', zId: 1, isHostNameValid: true}),
        Em.Object.create({component_name: 'ZOOKEEPER_SERVER', zId: 2, isHostNameValid: true}),
        Em.Object.create({component_name: 'c1', zId: null, isHostNameValid: true})
      ]);
    });

    it('shouldn\'t do nothing componentName not provided', function() {
      controller.updateIsHostNameValidFlag(null, null, false);
      expect(controller.get('selectedServicesMasters').everyProperty('isHostNameValid', true)).to.be.true;
    });

    it('should update one multiple component', function() {
      controller.updateIsHostNameValidFlag('ZOOKEEPER_SERVER', 2, false);
      expect(controller.get('selectedServicesMasters').mapProperty('isHostNameValid')).to.eql([true, false, true]);
    });

    it('should update single component', function() {
      controller.updateIsHostNameValidFlag('c1', null, false);
      expect(controller.get('selectedServicesMasters').mapProperty('isHostNameValid')).to.eql([true, true, false]);
    });

  });

});