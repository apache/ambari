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
require('controllers/main/service/add_controller');
var addServiceController = null;
var testHelpers = require('test/helpers');

describe('App.AddServiceController', function() {

  beforeEach(function () {
    addServiceController = App.AddServiceController.create({
      content: Em.Object.create({}),
      currentStep: 3
    });
  });

  describe('#generateDataForInstallServices', function() {
    var tests = [{
      selected: ["YARN","HBASE"],
      res: {
        "context": Em.I18n.t('requestInfo.installServices'),
        "ServiceInfo": {"state": "INSTALLED"},
        "urlParams": "ServiceInfo/service_name.in(YARN,HBASE)"
      }
    },
    {
      selected: ['OOZIE'],
      res: {
        "context": Em.I18n.t('requestInfo.installServices'),
        "ServiceInfo": {"state": "INSTALLED"},
        "urlParams": "ServiceInfo/service_name.in(OOZIE,HDFS,YARN,MAPREDUCE2)"
      }
    }];
    tests.forEach(function(t){
      it('should generate data with ' + t.selected.join(","), function () {
        expect(addServiceController.generateDataForInstallServices(t.selected)).to.be.eql(t.res);
      });
    });
  });

  describe('#saveServices', function() {
    beforeEach(function() {
      sinon.stub(addServiceController, 'setDBProperty', Em.K);
    });

    afterEach(function() {
      addServiceController.setDBProperty.restore();
    });

    var tests = [
      {
        appService: [
          Em.Object.create({ serviceName: 'HDFS' }),
          Em.Object.create({ serviceName: 'KERBEROS' })
        ],
        stepCtrlContent: Em.Object.create({
          content: Em.A([
            Em.Object.create({ serviceName: 'HDFS', isInstalled: true, isSelected: true }),
            Em.Object.create({ serviceName: 'YARN', isInstalled: false, isSelected: true })
          ])
        }),
        e: {
          selected: ['YARN'],
          installed: ['HDFS', 'KERBEROS']
        }
      },
      {
        appService: [
          Em.Object.create({ serviceName: 'HDFS' }),
          Em.Object.create({ serviceName: 'STORM' })
        ],
        stepCtrlContent: Em.Object.create({
          content: Em.A([
            Em.Object.create({ serviceName: 'HDFS', isInstalled: true, isSelected: true }),
            Em.Object.create({ serviceName: 'YARN', isInstalled: false, isSelected: true }),
            Em.Object.create({ serviceName: 'MAPREDUCE2', isInstalled: false, isSelected: true })
          ])
        }),
        e: {
          selected: ['YARN', 'MAPREDUCE2'],
          installed: ['HDFS', 'STORM']
        }
      }
    ];

    var message = '{0} installed, {1} selected. Installed list should be {2} and selected - {3}';
    tests.forEach(function(test) {

      var installed = test.appService.mapProperty('serviceName');
      var selected = test.stepCtrlContent.get('content').filterProperty('isSelected', true)
        .filterProperty('isInstalled', false).mapProperty('serviceName');

      describe(message.format(installed, selected, test.e.installed, test.e.selected), function() {

        beforeEach(function () {
          sinon.stub(App.Service, 'find').returns(test.appService);
          addServiceController.saveServices(test.stepCtrlContent);
          this.savedServices = addServiceController.setDBProperty.withArgs('services').args[0][1];
        });

        afterEach(function () {
          App.Service.find.restore();
        });

        it(JSON.stringify(test.e.selected) + ' are in the selectedServices', function () {
          expect(this.savedServices.selectedServices).to.have.members(test.e.selected);
        });

        it(JSON.stringify(test.e.installed) + ' are in the installedServices', function () {
          expect(this.savedServices.installedServices).to.have.members(test.e.installed);
        });

      });
    });
  });

  describe('#loadHosts', function () {

    var cases = [
      {
        hosts: {},
        isAjaxRequestSent: false,
        title: 'hosts are already loaded'
      },
      {
        areHostsLoaded: false,
        isAjaxRequestSent: true,
        title: 'hosts aren\'t yet loaded'
      }
    ];

    afterEach(function () {
      addServiceController.getDBProperty.restore();
    });

    cases.forEach(function (item) {
      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(addServiceController, 'getDBProperty').withArgs('hosts').returns(item.hosts);
          addServiceController.loadHosts();
          this.args = testHelpers.findAjaxRequest('name', 'hosts.confirmed');
        });

        it('request is ' + (item.isAjaxRequestSent ? '' : 'not') + ' sent', function () {
          expect(Em.isNone(this.args)).to.be.equal(!item.isAjaxRequestSent);
        });
      });
    });

  });

  describe('#loadHostsSuccessCallback', function () {

    it('should load hosts to local db and model', function () {
      var hostComponents = [
          [
            {
              HostRoles: {
                component_name: 'c0',
                state: 'STARTED'
              }
            },
            {
              HostRoles: {
                component_name: 'c1',
                state: 'INSTALLED'
              }
            }
          ],
          [
            {
              HostRoles: {
                component_name: 'c2',
                state: 'STARTED'
              }
            },
            {
              HostRoles: {
                component_name: 'c3',
                state: 'INSTALLED'
              }
            }
          ]
        ],
        response = {
          items: [
            {
              Hosts: {
                host_name: 'h0',
              },
              host_components: hostComponents[0]
            },
            {
              Hosts: {
                host_name: 'h1'
              },
              host_components: hostComponents[1]
            }
          ]
        },
        expected = {
          h0: {
            name: 'h0',
            bootStatus: 'REGISTERED',
            isInstalled: true,
            hostComponents: hostComponents[0],
            id: 0
          },
          h1: {
            name: 'h1',
            bootStatus: 'REGISTERED',
            isInstalled: true,
            hostComponents: hostComponents[1],
            id: 1
          }
        };
      addServiceController.loadHostsSuccessCallback(response);
      var hostsInDb = addServiceController.getDBProperty('hosts');
      var hostsInModel = addServiceController.get('content.hosts');
      expect(hostsInDb).to.eql(expected);
      expect(hostsInModel).to.eql(expected);
    });

  });

  describe('#loadHostsErrorCallback', function () {

    beforeEach(function () {
      sinon.stub(App.ajax, 'defaultErrorHandler', Em.K);
    });

    afterEach(function () {
      App.ajax.defaultErrorHandler.restore();
    });

    it('should execute default error handler', function () {
      addServiceController.loadHostsErrorCallback({status: '500'}, 'textStatus', 'errorThrown', {url: 'url', type: 'GET'});
      expect(App.ajax.defaultErrorHandler.calledOnce).to.be.true;
      expect(App.ajax.defaultErrorHandler.calledWith({status: '500'}, 'url', 'GET', '500')).to.be.true;
    });

  });

  describe('#loadServices', function() {
    var mock = {
      db: {}
    };
    beforeEach(function() {
      this.controller = App.AddServiceController.create({});
      this.mockGetDBProperty = sinon.stub(this.controller, 'getDBProperty');
      sinon.stub(this.controller, 'setDBProperty', function(key, value) {
        mock.db = value;
      });
      sinon.stub(this.controller, 'hasDependentSlaveComponent');
      sinon.stub(App.store, 'fastCommit', Em.K);
      this.mockStackService = sinon.stub(App.StackService, 'find');
      this.mockService = sinon.stub(App.Service, 'find');
    });

    afterEach(function() {
      this.mockGetDBProperty.restore();
      this.controller.setDBProperty.restore();
      this.controller.hasDependentSlaveComponent.restore();
      this.mockStackService.restore();
      this.mockService.restore();
      App.store.fastCommit.restore();
    });

    var tests = [
      {
        appStackService: [
          Em.Object.create({ id: 'HDFS', serviceName: 'HDFS', coSelectedServices: []}),
          Em.Object.create({ id: 'YARN', serviceName: 'YARN', coSelectedServices: ['MAPREDUCE2']}),
          Em.Object.create({ id: 'MAPREDUCE2', serviceName: 'MAPREDUCE2', coSelectedServices: []}),
          Em.Object.create({ id: 'FALCON', serviceName: 'FALCON', coSelectedServices: []}),
          Em.Object.create({ id: 'STORM', serviceName: 'STORM', coSelectedServices: []})
        ],
        appService: [
          Em.Object.create({ id: 'HDFS', serviceName: 'HDFS'}),
          Em.Object.create({ id: 'STORM', serviceName: 'STORM'})
        ],
        servicesFromDB: false,
        serviceToInstall: 'MAPREDUCE2',
        e: {
          selectedServices: ['HDFS', 'YARN', 'MAPREDUCE2', 'STORM'],
          installedServices: ['HDFS', 'STORM']
        },
        m: 'MapReduce selected on Admin -> Stack Versions Page, Yarn service should be selected because it coselected'
      },
      {
        appStackService: [
          Em.Object.create({ id: 'HDFS', serviceName: 'HDFS', coSelectedServices: []}),
          Em.Object.create({ id: 'YARN', serviceName: 'YARN', coSelectedServices: ['MAPREDUCE2']}),
          Em.Object.create({ id: 'HBASE', serviceName: 'HBASE', coSelectedServices: []}),
          Em.Object.create({ id: 'STORM', serviceName: 'STORM', coSelectedServices: []})
        ],
        appService: [
          Em.Object.create({ id: 'HDFS', serviceName: 'HDFS'}),
          Em.Object.create({ id: 'STORM', serviceName: 'STORM'})
        ],
        servicesFromDB: {
          selectedServices: ['HBASE'],
          installedServices: ['HDFS', 'STORM']
        },
        serviceToInstall: null,
        e: {
          selectedServices: ['HDFS', 'HBASE', 'STORM'],
          installedServices: ['HDFS', 'STORM']
        },
        m: 'HDFS and STORM are installed. Select HBASE'
      }
    ];

    tests.forEach(function(test) {
      describe(test.m, function() {

        beforeEach(function () {
          this.mockStackService.returns(test.appStackService);
          this.mockService.returns(test.appService);
          this.mockGetDBProperty.withArgs('services').returns(test.servicesFromDB);
          this.controller.set('serviceToInstall', test.serviceToInstall);
          this.controller.loadServices();
        });

        if (test.servicesFromDB) {
          // verify values for App.StackService
          it(JSON.stringify(test.e.selectedServices) + ' are selected', function () {
            expect(test.appStackService.filterProperty('isSelected', true).mapProperty('serviceName')).to.be.eql(test.e.selectedServices);
          });
          it(JSON.stringify(test.e.installedServices) + ' are installed', function () {
            expect(test.appStackService.filterProperty('isInstalled', true).mapProperty('serviceName')).to.be.eql(test.e.installedServices);
          });
        }
        else {
          // verify saving to local db on first enter to the wizard
          it('selectedServices are saced', function () {
            expect(mock.db.selectedServices).to.be.eql(test.e.selectedServices);
          });
          it('installedServices are saved', function () {
            expect(mock.db.installedServices).to.be.eql(test.e.installedServices);
          });

        }

        it('serviceToInstall is null', function () {
          expect(this.controller.get('serviceToInstall')).to.be.null;
        });

      });
    }, this);
  });

  describe('#loadServiceConfigGroups', function () {

    var dbMock,
      dbMock2,
      cases = [
        {
          serviceConfigGroups: null,
          areInstalledConfigGroupsLoaded: false,
          title: 'config groups not yet loaded'
        },
        {
          serviceConfigGroups: [],
          areInstalledConfigGroupsLoaded: true,
          title: 'config groups already loaded'
        }
      ];

    beforeEach(function () {
      dbMock = sinon.stub(addServiceController, 'getDBProperties');
      dbMock2 = sinon.stub(addServiceController, 'getDBProperty');
    });

    afterEach(function () {
      dbMock.restore();
      dbMock2.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        dbMock.withArgs(['serviceConfigGroups', 'hosts']).returns({
          hosts: {},
          serviceConfigGroups: item.serviceConfigGroups
        });
        dbMock2.withArgs('hosts').returns({}).
          withArgs('serviceConfigGroups').returns(item.serviceConfigGroups);
        addServiceController.loadServiceConfigGroups();
        expect(addServiceController.get('areInstalledConfigGroupsLoaded')).to.equal(item.areInstalledConfigGroupsLoaded);
      });
    });

  });

  describe('#clearStorageData', function () {
    it('areInstalledConfigGroupsLoaded should be false', function () {
      addServiceController.set('areInstalledConfigGroupsLoaded', true);
      addServiceController.clearStorageData();
      expect(addServiceController.get('areInstalledConfigGroupsLoaded')).to.be.false;
    });
  });

  describe('#loadClients', function () {

    var cases = [
      {
        clients: null,
        contentClients: [],
        saveClientsCallCount: 1,
        title: 'no clients info in local db'
      },
      {
        clients: [{}],
        contentClients: [{}],
        saveClientsCallCount: 0,
        title: 'clients info saved in local db'
      }
    ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          sinon.stub(addServiceController, 'getDBProperty').withArgs('clientInfo').returns(item.clients);
          sinon.stub(addServiceController, 'saveClients', Em.K);
          addServiceController.set('content.clients', []);
          addServiceController.loadClients();
        });

        afterEach(function () {
          addServiceController.getDBProperty.restore();
          addServiceController.saveClients.restore();
        });

        it('content.clients', function () {
          expect(addServiceController.get('content.clients', [])).to.eql(item.contentClients);
        });

        it('saveClients call', function () {
          expect(addServiceController.saveClients.callCount).to.equal(item.saveClientsCallCount);
        });

      });

    });

  });

  describe('#getServicesBySelectedSlaves', function () {

    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          componentName: 'c1',
          serviceName: 's1'
        }),
        Em.Object.create({
          componentName: 'c2',
          serviceName: 's2'
        }),
        Em.Object.create({
          componentName: 'c3',
          serviceName: 's3'
        }),
        Em.Object.create({
          componentName: 'c4',
          serviceName: 's1'
        })
      ]);
    });

    [
      {
        title: 'should return empty array',
        sch: [],
        expect: []
      },
      {
        title: 'should return empty array if component is absent in StackServiceComponent model',
        sch: [
          {
            componentName: 'c5',
            hosts: [
              {
                isInstalled: false
              },
              {
                isInstalled: true
              }
            ]
          },
        ],
        expect: []
      },
      {
        title: 'should return services for not installed slaves',
        sch: [
          {
            componentName: 'c1',
            hosts: [
              {
                isInstalled: false
              },
              {
                isInstalled: true
              }
            ]
          },
          {
            componentName: 'c2',
            hosts: [
              {
                isInstalled: false
              },
              {
                isInstalled: true
              }
            ]
          },
          {
            componentName: 'c4',
            hosts: [
              {
                isInstalled: false
              },
              {
                isInstalled: true
              }
            ]
          }
        ],
        expect: ['s1', 's2']
      }
    ].forEach(function (test) {
          describe(test.title, function () {
            it(function () {
              addServiceController.set('content.slaveComponentHosts', test.sch);
              expect(addServiceController.getServicesBySelectedSlaves()).to.eql(test.expect);
            });
          })
        });

  });

  describe('#loadMap', function() {

    describe('should load service', function() {
      var loadServices = false;
      var checker = {
        loadServices: function() {
          loadServices = true;
        }
      };

      beforeEach(function () {
        addServiceController.loadMap['1'][0].callback.call(checker);
      });

      it('service info is loaded', function () {
        expect(loadServices).to.be.true;
      });
    });

    describe('should load hosts and recommendations', function() {
      var loadHosts = false;
      var loadMasterComponentHosts = false;
      var load = false;
      var loadRecommendations = false;

      var checker = {
        loadHosts: function () {
          loadHosts = true;
          return $.Deferred().resolve().promise();
        },
        loadMasterComponentHosts: function () {
          loadMasterComponentHosts = true;
          return $.Deferred().resolve().promise();
        },
        load: function () {
          load = true;
        },
        loadRecommendations: function () {
          loadRecommendations = true;
        }
      };

      beforeEach(function () {
        addServiceController.loadMap['2'][0].callback.call(checker);
      });

      it('hosts are loaded', function () {
        expect(loadHosts).to.be.true;
      });

      it('master component hosts are loaded', function () {
        expect(loadMasterComponentHosts).to.be.true;
      });

      it('hosts info is loaded', function () {
        expect(load).to.be.true;
      });

      it('recommendations are loaded', function () {
        expect(loadRecommendations).to.be.true;
      });
    });

    describe('should load slave components', function() {
      var loadHosts = false;
      var loadServices = false;
      var loadClients = false;
      var loadSlaveComponentHosts = false;

      var checker = {
        loadHosts: function () {
          loadHosts = true;
          return $.Deferred().resolve().promise();
        },
        loadServices: function () {
          loadServices = true;
        },
        loadClients: function () {
          loadClients = true;
        },
        loadSlaveComponentHosts: function () {
          loadSlaveComponentHosts = true;
        }
      };

      beforeEach(function () {
        addServiceController.loadMap['3'][0].callback.call(checker);
      });

      it('hosts are loaded', function () {
        expect(loadHosts).to.be.true;
      });

      it('services are loaded', function () {
        expect(loadServices).to.be.true;
      });

      it('clients are loaded', function () {
        expect(loadClients).to.be.true;
      });

      it('slave component hosts are loaded', function () {
        expect(loadSlaveComponentHosts).to.be.true;
      });
    });

    describe('should load config groups', function() {
      var load = false;
      var loadKerberosDescriptorConfigs = false;
      var loadServiceConfigGroups = false;
      var loadConfigThemes = false;
      var loadServiceConfigProperties = false;
      var loadCurrentHostGroups = false;

      var checker = {
        set: function() {
          return Em.K;
        },
        load: function () {
          load = true;
        },
        loadKerberosDescriptorConfigs: function () {
          loadKerberosDescriptorConfigs = true;
          return $.Deferred().resolve().promise();
        },
        loadServiceConfigGroups: function () {
          loadServiceConfigGroups = true;
        },
        loadConfigThemes: function () {
          loadConfigThemes = true;
          return $.Deferred().resolve().promise();
        },
        loadServiceConfigProperties: function () {
          loadServiceConfigProperties = true;
        },
        loadCurrentHostGroups: function () {
          loadCurrentHostGroups = true;
        }
      };

      beforeEach(function () {
        addServiceController.loadMap['4'][0].callback.call(checker);
      });

      it('cluster info is loaded', function () {
        expect(load).to.be.true;
      });

      it('kerberos descriptor configs are loaded', function () {
        expect(loadKerberosDescriptorConfigs).to.be.true;
      });

      it('service config groups are loaded', function () {
        expect(loadServiceConfigGroups).to.be.true;
      });

      it('config themes are loaded', function () {
        expect(loadConfigThemes).to.be.true;
      });

      it('service config properties are loaded', function () {
        expect(loadServiceConfigProperties).to.be.true;
      });

      it('current host groups are loaded', function () {
        expect(loadCurrentHostGroups).to.be.true;
      });
    });
  });

  describe("#setCurrentStep()", function () {

    beforeEach(function() {
      sinon.stub(App.clusterStatus, 'setClusterStatus');
    });

    afterEach(function() {
      App.clusterStatus.setClusterStatus.restore();
    });

    it("should set current step", function() {
      addServiceController.setCurrentStep();
      expect(App.clusterStatus.setClusterStatus.calledOnce).to.be.true;
    });
  });

  describe("#loadCurrentHostGroups()", function () {

    beforeEach(function() {
      sinon.stub(addServiceController, 'getDBProperty').returns([]);
    });

    afterEach(function() {
      addServiceController.getDBProperty.restore();
    });

    it("should set current host groups", function() {
      addServiceController.loadCurrentHostGroups();
      expect(addServiceController.get('content.recommendationsHostGroups')).to.eql([]);
    });
  });

  describe('#saveMasterComponentHosts', function () {

    var stepController = Em.Object.create({
        selectedServicesMasters: [
          Em.Object.create({
            display_name: 'd0',
            component_name: 'c0',
            selectedHost: 'h0',
            serviceId: 's0',
          }),
          Em.Object.create({
            display_name: 'd1',
            component_name: 'c1',
            selectedHost: 'h1',
            serviceId: 's1',
          })
        ]
      }),
      masterComponentHosts = [
        {
          display_name: 'd0',
          component: 'c0',
          hostName: 'h0',
          serviceId: 's0',
          isInstalled: true,
          workStatus: 'ACTIVE'
        },
        {
          display_name: 'd1',
          component: 'c1',
          hostName: 'h1',
          serviceId: 's1',
          isInstalled: true,
          workStatus: 'PENDING'
        }
      ];

    beforeEach(function () {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'c0',
          workStatus: 'ACTIVE'
        }),
        Em.Object.create({
          componentName: 'c1',
          workStatus: 'PENDING'
        })
      ]);
      sinon.stub(addServiceController, 'setDBProperty', Em.K);
      addServiceController.saveMasterComponentHosts(stepController);
    });

    afterEach(function () {
      App.HostComponent.find.restore();
      addServiceController.setDBProperty.restore();
    });

    it('should set DB property masterComponentHosts', function () {
      expect(addServiceController.setDBProperty.calledOnce).to.be.true;
      expect(addServiceController.get('content.masterComponentHosts')).to.eql(masterComponentHosts);
    });
  });

  describe('#isServiceNotConfigurable', function () {

    it('should return true', function () {
      App.services.reopen(Em.Object.create({noConfigTypes:['s1']}));
      expect(addServiceController.isServiceNotConfigurable('s1')).to.be.true;
    });

    it('should return false', function () {
      App.services.reopen(Em.Object.create({noConfigTypes:[]}));
      expect(addServiceController.isServiceNotConfigurable('s2')).to.be.false;
    });
  });

  describe('#skipConfigStep', function () {
    var services = [
      {
        serviceName: 's1',
        isSelected: true,
        isInstalled: false
      },
      {
        serviceName: 's2',
        isSelected: true,
        isInstalled: true
      }
    ];

    it('should return true', function () {
      App.services.reopen(Em.Object.create({noConfigTypes:['s1']}));
      addServiceController.set('content.services',services);
      addServiceController.skipConfigStep();
      expect(addServiceController.skipConfigStep()).to.be.true;
    });

    it('should return false', function () {
      App.services.reopen(Em.Object.create({noConfigTypes:[]}));
      addServiceController.set('content.services', services);
      addServiceController.skipConfigStep();
      expect(addServiceController.skipConfigStep()).to.be.false;
    });
  });

  describe("#loadServiceConfigProperties()", function () {

    beforeEach(function() {
      sinon.stub(addServiceController, 'loadServices');
      sinon.stub(addServiceController, 'skipConfigStep').returns(true);
      addServiceController.set('content.skipConfigStep', false);
    });

    afterEach(function() {
      addServiceController.loadServices.restore();
      addServiceController.skipConfigStep.restore();
    });

    it("should set skip config step", function() {
      addServiceController.set('content.services', []);
      addServiceController.set('currentStep', 3);
      addServiceController.set('isStepDisabled', [
        Em.Object.create({
          step: 4,
          value: false
        })
      ]);
      addServiceController.loadServiceConfigProperties();
      expect(addServiceController.loadServices.calledOnce).to.be.false;
      expect(addServiceController.get('content.skipConfigStep')).to.be.true;
      expect(addServiceController.get('isStepDisabled').findProperty('step', 4).value).to.be.true;
    });

    it("should load services", function() {
      addServiceController.set('content.services', null);
      addServiceController.set('currentStep', 1);
      addServiceController.loadServiceConfigProperties();
      expect(addServiceController.loadServices.calledOnce).to.be.true;
      expect(addServiceController.get('content.skipConfigStep')).to.be.false;
    });
  });

  describe("#saveServiceConfigProperties()", function () {

    beforeEach(function() {
      sinon.stub(addServiceController, 'skipConfigStep').returns(true);
    });

    afterEach(function() {
      addServiceController.skipConfigStep.restore();
    });

    it("should save service config properties", function() {
      addServiceController.set('content.services', []);
      addServiceController.set('currentStep', 3);
      addServiceController.set('isStepDisabled', [
        Em.Object.create({
          step: 4,
          value: false
        })
      ]);
      addServiceController.saveServiceConfigProperties(
        Em.Object.create(
          {
            installedServiceNames: ['s1'],
            stepConfigs: []
          }
        )
      );
      expect(addServiceController.get('content.skipConfigStep')).to.be.true;
      expect(addServiceController.get('isStepDisabled').findProperty('step', 4).value).to.be.true;
    });
  });

  describe("#loadSlaveComponentHosts()", function () {

    beforeEach(function() {
      sinon.stub(addServiceController, 'getDBProperties').returns(
        {
          slaveComponentHosts: [{hosts: [{hostName: 'h2', host_id: 2}, {hostName: 'h3', host_id: 3}]}],
          hosts: {'h1':{id: 1}, 'hh2': {id: 2}, 'h3': {id: 3}}
        }
      );
      sinon.stub(addServiceController, 'getDBProperty').returns({});
    });

    afterEach(function() {
      addServiceController.getDBProperties.restore();
      addServiceController.getDBProperty.restore();
    });

    it("should load slave component hosts", function() {
      addServiceController.set('content.hosts', []);
      addServiceController.loadSlaveComponentHosts();
      expect(addServiceController.get('content.installedHosts')).to.eql({});
      expect(addServiceController.get('content.slaveComponentHosts')[0].hosts)
        .to.eql([{hostName: 'hh2', host_id: 2}, {hostName: 'h3', host_id: 3}]);
    });
  });

  describe("#saveClients", function () {
    var services = [
      Em.Object.create({
        serviceName: 's1',
        isSelected: true,
        isInstalled: false
      }),
      Em.Object.create({
        serviceName: 's2',
        isSelected: true,
        isInstalled: true
      })
    ];

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns(
        [
          Em.Object.create({
            componentName: 'c1',
            displayName: 'c1',
            serviceName: 's1',
            isClient: []
          }),
          Em.Object.create({
            componentName: 'c2',
            displayName: 'c2',
            serviceName: 's2'
          })
        ]
      );
      sinon.stub(addServiceController, 'setDBProperty');
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
      addServiceController.setDBProperty.restore();
    });

    it("should save clients", function() {
      addServiceController.set('content.services',services);
      addServiceController.set('content.hosts', []);
      addServiceController.saveClients();
      expect(addServiceController.setDBProperty.calledOnce).to.be.true;
      expect(addServiceController.get('content.clients')[0]).to.eql(
        {
          component_name: 'c1',
          display_name: 'c1',
          isInstalled: false
        }
      );
    });
  });

  describe("#clearAllSteps()", function () {

    beforeEach(function() {
      sinon.stub(addServiceController, 'clearInstallOptions');
      sinon.stub(addServiceController, 'getCluster').returns('c1');
    });

    afterEach(function() {
      addServiceController.clearInstallOptions.restore();
      addServiceController.getCluster.restore();
    });

    it("should clear install options", function() {
      addServiceController.clearAllSteps();
      expect(addServiceController.clearInstallOptions.calledOnce).to.be.true;
      expect(addServiceController.get('content.cluster')).to.equal('c1');
    });
  });

  describe("#finish()", function () {
    var container = Em.Object.create({
      updateAll: Em.K
    });

    beforeEach(function() {
      sinon.stub(addServiceController, 'clearAllSteps');
      sinon.stub(addServiceController, 'clearStorageData');
      sinon.stub(addServiceController, 'clearServiceConfigProperties');
      sinon.stub(addServiceController, 'resetDbNamespace');
      sinon.stub(App.router, 'get').returns(container);
      sinon.stub(container, 'updateAll');
      addServiceController.finish();
    });

    afterEach(function() {
      addServiceController.clearAllSteps.restore();
      addServiceController.clearStorageData.restore();
      addServiceController.clearServiceConfigProperties.restore();
      addServiceController.resetDbNamespace.restore();
      App.router.get.restore();
      container.updateAll.restore();
    });

    it("clearAllSteps should be called", function() {
      expect(addServiceController.clearAllSteps.calledOnce).to.be.true;
    });

    it("clearStorageData should be called", function() {
      expect(addServiceController.clearStorageData.calledOnce).to.be.true;
    });

    it("clearServiceConfigProperties should be called", function() {
      expect(addServiceController.clearServiceConfigProperties.calledOnce).to.be.true;
    });

    it("resetDbNamespace should be called", function() {
      expect(addServiceController.resetDbNamespace.calledOnce).to.be.true;
    });

    it("updateAll should be called", function() {
      expect(container.updateAll.calledOnce).to.be.true;
    });
  });

  describe("#installServices()", function () {
    var set = false;
    var installAdditionalClients = false;
    var installSelectedServices = false;

    var checker = {
      set: function () {
        set = true;
      },
      installAdditionalClients: function () {
        installAdditionalClients = true;
        return $.Deferred().resolve().promise();
      },
      installSelectedServices: function () {
        installSelectedServices = true;
      }
    };

    beforeEach(function () {
      addServiceController.installServices.call(checker);
    });

    it("should set content", function() {
      expect(set).to.be.true;
    });

    it("should install additional clients", function() {
      expect(installAdditionalClients).to.be.true;
    });

    it("should install selected services", function() {
      expect(installSelectedServices).to.be.true;
    });
  });

  describe("#installSelectedServices()", function () {
    var services = [
      Em.Object.create({
        serviceName: 's1',
        isSelected: true,
        isInstalled: false
      }),
      Em.Object.create({
        serviceName: 's2',
        isSelected: true,
        isInstalled: true
      })
    ];

    beforeEach(function() {
      sinon.stub(addServiceController, 'installServicesRequest');
      sinon.stub(addServiceController, 'getServicesBySelectedSlaves').returns(['s3']);
      sinon.stub(addServiceController, 'generateDataForInstallServices').returns(['s1', 's2', 's3']);
    });

    afterEach(function() {
      addServiceController.installServicesRequest.restore();
      addServiceController.getServicesBySelectedSlaves.restore();
      addServiceController.generateDataForInstallServices.restore();
    });

    it("should install selected services", function() {
      addServiceController.set('content.services', services);
      addServiceController.installSelectedServices(Em.clb);
      expect(addServiceController.installServicesRequest.calledOnce).to.be.true;
    });
  });

  describe("#installServicesRequest()", function () {

    it("should call send ajax request", function() {
      var args = testHelpers.findAjaxRequest('name', 'name');
      addServiceController.installServicesRequest('name', {}, Em.clb);
      expect(args).to.exists;
    });
  });

  describe("#getServicesBySelectedSlaves", function () {

    beforeEach(function() {
      sinon.stub(App.StackServiceComponent, 'find').returns(
        [
          Em.Object.create({
            componentName: 'c1',
            displayName: 'c1',
            serviceName: 's1',
          }),
          Em.Object.create({
            componentName: 'c2',
            displayName: 'c2',
            serviceName: 's2'
          })
        ]
      );
    });

    afterEach(function() {
      App.StackServiceComponent.find.restore();
    });

    it("should get services", function() {
      addServiceController.set('content.slaveComponentHosts', [
        {
          hosts: [
            Em.Object.create({
              hostName: 'h1',
              host_id: 1,
              isInstalled: true
            }),
            Em.Object.create({
              hostName: 'h2',
              host_id: 2,
              isInstalled: false
            })
          ],
          componentName: 'c1',
        }
      ]);
      expect(addServiceController.getServicesBySelectedSlaves()).to.eql(['s1']);
    });
  });

  describe("#installClientSuccess()", function () {
    var params = {
      deferred: {
        resolve: Em.K
      },
      counter: 2
    };

    beforeEach(function() {
      sinon.stub(params.deferred, 'resolve');
    });

    afterEach(function() {
      params.deferred.resolve.restore();
    });

    it("should call resolve", function() {
      addServiceController.set('installClientQueueLength', 3);
      addServiceController.installClientSuccess({}, {}, params);
      expect(params.deferred.resolve.calledOnce).to.be.true;
    });
  });

  describe("#installClientError()", function () {
    var params = {
      deferred: {
        resolve: Em.K
      },
      counter: 2
    };

    beforeEach(function() {
      sinon.stub(params.deferred, 'resolve');
    });

    afterEach(function() {
      params.deferred.resolve.restore();
    });

    it("should call resolve", function() {
      addServiceController.set('installClientQueueLength', 3);
      addServiceController.installClientError({}, {}, {}, {}, params);
      expect(params.deferred.resolve.calledOnce).to.be.true;
    });
  });

});
