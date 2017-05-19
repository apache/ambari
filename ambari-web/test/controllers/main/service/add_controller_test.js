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
    addServiceController = App.AddServiceController.create({});
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

});
