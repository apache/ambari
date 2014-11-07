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
var numberUtils = require('utils/number_utils');
require('mixins/common/localStorage');
require('models/config_group');
require('controllers/wizard/step7_controller');

var installerStep7Controller;

describe('App.InstallerStep7Controller', function () {

  beforeEach(function () {
    sinon.stub(App.config, 'setPreDefinedServiceConfigs', Em.K);
    installerStep7Controller = App.WizardStep7Controller.create({
      content: {
        advancedServiceConfig: [],
        serviceConfigProperties: []
      }
    });
  });

  afterEach(function() {
    App.config.setPreDefinedServiceConfigs.restore();
  });

  describe('#installedServiceNames', function () {

    var tests = Em.A([
      {
        content: Em.Object.create({
          controllerName: 'installerController',
          services: Em.A([
            Em.Object.create({
              isInstalled: true,
              serviceName: 'SQOOP'
            }),
            Em.Object.create({
              isInstalled: true,
              serviceName: 'HDFS'
            })
          ])
        }),
        e: ['SQOOP', 'HDFS'],
        m: 'installerController with SQOOP'
      },
      {
        content: Em.Object.create({
          controllerName: 'installerController',
          services: Em.A([
            Em.Object.create({
              isInstalled: true,
              serviceName: 'HIVE'
            }),
            Em.Object.create({
              isInstalled: true,
              serviceName: 'HDFS'
            })
          ])
        }),
        e: ['HIVE', 'HDFS'],
        m: 'installerController without SQOOP'
      },
      {
        content: Em.Object.create({
          controllerName: 'addServiceController',
          services: Em.A([
            Em.Object.create({
              isInstalled: true,
              serviceName: 'HIVE'
            }),
            Em.Object.create({
              isInstalled: true,
              serviceName: 'HDFS'
            })
          ])
        }),
        e: ['HIVE', 'HDFS'],
        m: 'addServiceController without SQOOP'
      }
    ]);

    tests.forEach(function (test) {
      it(test.m, function () {
        installerStep7Controller.set('content', test.content);
        expect(installerStep7Controller.get('installedServiceNames')).to.include.members(test.e);
        expect(test.e).to.include.members(installerStep7Controller.get('installedServiceNames'));
      });
    });

  });

  describe('#isSubmitDisabled', function () {
    it('should be true if miscModalVisible', function () {
      installerStep7Controller.reopen({miscModalVisible: true});
      expect(installerStep7Controller.get('isSubmitDisabled')).to.equal(true);
    });
    it('should be true if some of stepConfigs has errors', function () {
      installerStep7Controller.reopen({
        miscModalVisible: false,
        stepConfigs: [
          {
            showConfig: true,
            errorCount: 1
          }
        ]
      });
      expect(installerStep7Controller.get('isSubmitDisabled')).to.equal(true);
    });
    it('should be false if all of stepConfigs don\'t have errors and miscModalVisible is false', function () {
      installerStep7Controller.reopen({
        miscModalVisible: false,
        stepConfigs: [
          {
            showConfig: true,
            errorCount: 0
          }
        ]
      });
      expect(installerStep7Controller.get('isSubmitDisabled')).to.equal(false);
    });
  });

  describe('#selectedServiceNames', function () {
    it('should use content.services as source of data', function () {
      installerStep7Controller.set('content', {
        services: [
          {isSelected: true, isInstalled: false, serviceName: 's1'},
          {isSelected: false, isInstalled: false, serviceName: 's2'},
          {isSelected: true, isInstalled: true, serviceName: 's3'},
          {isSelected: false, isInstalled: false, serviceName: 's4'},
          {isSelected: true, isInstalled: false, serviceName: 's5'},
          {isSelected: false, isInstalled: false, serviceName: 's6'},
          {isSelected: true, isInstalled: true, serviceName: 's7'},
          {isSelected: false, isInstalled: false, serviceName: 's8'}
        ]
      });
      var expected = ['s1', 's5'];
      expect(installerStep7Controller.get('selectedServiceNames')).to.eql(expected);
    });
  });

  describe('#allSelectedServiceNames', function () {
    it('should use content.services as source of data', function () {
      installerStep7Controller.set('content', {
        services: [
          Em.Object.create({isSelected: true, isInstalled: false, serviceName: 's1'}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's2'}),
          Em.Object.create({isSelected: true, isInstalled: true, serviceName: 's3'}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's4'}),
          Em.Object.create({isSelected: true, isInstalled: false, serviceName: 's5'}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's6'}),
          Em.Object.create({isSelected: true, isInstalled: true, serviceName: 's7'}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's8'})
        ]
      });
      var expected = ['s1', 's3', 's5', 's7'];
      expect(installerStep7Controller.get('allSelectedServiceNames')).to.eql(expected);
    });
  });

  describe('#masterComponentHosts', function () {
    it('should be equal to content.masterComponentHosts', function () {
      var masterComponentHosts = [
        {},
        {},
        {}
      ];
      installerStep7Controller.reopen({content: {masterComponentHosts: masterComponentHosts}});
      expect(installerStep7Controller.get('masterComponentHosts')).to.eql(masterComponentHosts);
    });
  });

  describe('#slaveComponentHosts', function () {
    it('should be equal to content.slaveGroupProperties', function () {
      var slaveGroupProperties = [
        {},
        {},
        {}
      ];
      installerStep7Controller.reopen({content: {slaveGroupProperties: slaveGroupProperties}});
      expect(installerStep7Controller.get('slaveComponentHosts')).to.eql(slaveGroupProperties);
    });
  });

  describe('#clearStep', function () {
    it('should clear stepConfigs', function () {
      installerStep7Controller.set('stepConfigs', [
        {},
        {}
      ]);
      installerStep7Controller.clearStep();
      expect(installerStep7Controller.get('stepConfigs.length')).to.equal(0);
    });
    it('should clear filter', function () {
      installerStep7Controller.set('filter', 'filter');
      installerStep7Controller.clearStep();
      expect(installerStep7Controller.get('filter')).to.equal('');
    });
    it('should set for each filterColumns "selected" false', function () {
      installerStep7Controller.set('filterColumns', [
        {selected: true},
        {selected: false},
        {selected: true}
      ]);
      installerStep7Controller.clearStep();
      expect(installerStep7Controller.get('filterColumns').everyProperty('selected', false)).to.equal(true);
    });
  });

  describe('#loadInstalledServicesConfigGroups', function () {
    before(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    after(function () {
      App.ajax.send.restore();
    });
    it('should do ajax request for each received service name', function () {
      var serviceNames = ['s1', 's2', 's3'];
      installerStep7Controller.loadInstalledServicesConfigGroups(serviceNames);
      expect(App.ajax.send.callCount).to.equal(serviceNames.length);
    });
  });

  describe('#getConfigTags', function () {
    before(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    after(function () {
      App.ajax.send.restore();
    });
    it('should do ajax-request', function () {
      installerStep7Controller.getConfigTags();
      expect(App.ajax.send.calledOnce).to.equal(true);
    });
  });

  describe('#setGroupsToDelete', function () {
    beforeEach(function () {
      installerStep7Controller.set('wizardController', Em.Object.create(App.LocalStorage, {name: 'tdk'}));
    });
    it('should add new groups to groupsToDelete', function () {
      var groupsToDelete = [
          {id: '1'},
          {id: '2'}
        ],
        groups = [
          Em.Object.create({id: '3'}),
          Em.Object.create(),
          Em.Object.create({id: '5'})
        ],
        expected = [
          {id: "1"},
          {id: "2"},
          {id: "3"},
          {id: "5"}
        ];
      installerStep7Controller.set('groupsToDelete', groupsToDelete);
      installerStep7Controller.setGroupsToDelete(groups);
      expect(installerStep7Controller.get('groupsToDelete')).to.eql(expected);
      expect(installerStep7Controller.get('wizardController').getDBProperty('groupsToDelete')).to.eql(expected);
    });
  });

  describe('#selectConfigGroup', function () {
    beforeEach(function () {
      installerStep7Controller.reopen({content: {services: []}});
      sinon.stub(installerStep7Controller, 'switchConfigGroupConfigs', Em.K);
    });
    afterEach(function () {
      installerStep7Controller.switchConfigGroupConfigs.restore();
    });
    it('should set selectedConfigGroup', function () {
      var group = {':': []};
      installerStep7Controller.selectConfigGroup({context: group});
      expect(installerStep7Controller.get('selectedConfigGroup')).to.eql(group);
    });
  });

  describe('#addOverrideProperty', function () {
    it('should add override property', function () {
      var groupName = 'groupName',
        selectedService = {configGroups: [Em.Object.create({name: groupName, properties: []})]},
        selectedConfigGroup = {name: groupName},
        serviceConfigProperty = Em.Object.create({overrides: []}),
        expected = Em.Object.create({
          value: '',
          isOriginalSCP: false,
          isEditable: true
        });
      installerStep7Controller.reopen({selectedService: selectedService, selectedConfigGroup: selectedConfigGroup});
      var newSCP = installerStep7Controller.addOverrideProperty(serviceConfigProperty);
      Em.keys(expected).forEach(function (k) {
        expect(newSCP.get(k)).to.equal(expected.get(k));
      });
      var group = installerStep7Controller.get('selectedService.configGroups').findProperty('name', groupName);
      expect(newSCP.get('group')).to.eql(group);
      expect(newSCP.get('parentSCP')).to.eql(serviceConfigProperty);
      expect(group.get('properties.length')).to.equal(1);
    });
  });

  describe('#resolveYarnConfigs', function () {
    it('should set property to true', function () {
      var allSelectedServiceNames = ['SLIDER', 'YARN'],
        configs = [
          {name: 'hadoop.registry.rm.enabled', value: false, defaultValue: false}
        ],
        expected = [
          {name: 'hadoop.registry.rm.enabled', value: true, defaultValue: true, forceUpdate: true}
        ];
      installerStep7Controller.reopen({allSelectedServiceNames: allSelectedServiceNames});
      installerStep7Controller.resolveYarnConfigs(configs);
      expect(configs[0]).to.eql(expected[0]);
    });

    it('should set property to false', function () {
      var allSelectedServiceNames = ['YARN'],
        configs = [
          {name: 'hadoop.registry.rm.enabled', value: true, defaultValue: true}
        ],
        expected = [
          {name: 'hadoop.registry.rm.enabled', value: false, defaultValue: false, forceUpdate: true}
        ];
      installerStep7Controller.reopen({allSelectedServiceNames: allSelectedServiceNames});
      installerStep7Controller.resolveYarnConfigs(configs);
      expect(configs[0]).to.eql(expected[0]);
    });

    it('should skip setting property', function () {
      var allSelectedServiceNames = ['YARN', 'SLIDER'],
        configs = [
          {name: 'hadoop.registry.rm.enabled', value: true, defaultValue: true}
        ],
        expected = [
          {name: 'hadoop.registry.rm.enabled', value: true, defaultValue: true}
        ];
      installerStep7Controller.reopen({allSelectedServiceNames: allSelectedServiceNames});
      installerStep7Controller.resolveYarnConfigs(configs);
      expect(configs[0]).to.eql(expected[0]);
    });
  });

  describe('#resolveStormConfigs', function () {

    beforeEach(function () {
      installerStep7Controller.reopen({
        content: {services: []},
        wizardController: Em.Object.create({

          hosts: {'h1': {name: 'host1', id: 'h1'}},
          masterComponentHosts: [{component: 'GANGLIA_SERVER', host_id: 'h1'}],

          getDBProperty: function (k) {
            return this.get(k);
          }
        })
      });
    });

    it('shouldn\'t do nothing if Ganglia and Storm are installed', function () {
      var installedServiceNames = ['GANGLIA', 'STORM'],
        configs = [
          {name: 'nimbus.childopts', value: '.jar=host=host2', defaultValue: ''},
          {name: 'supervisor.childopts', value: '.jar=host=host2', defaultValue: ''},
          {name: 'worker.childopts', value: '.jar=host=host2', defaultValue: ''}
        ],
        expected = [
          {name: 'nimbus.childopts', value: '.jar=host=host2', defaultValue: ''},
          {name: 'supervisor.childopts', value: '.jar=host=host2', defaultValue: ''},
          {name: 'worker.childopts', value: '.jar=host=host2', defaultValue: ''}
        ];
      installerStep7Controller.reopen({installedServiceNames: installedServiceNames});
      installerStep7Controller.resolveStormConfigs(configs);
      expect(configs).to.eql(expected);
    });

    it('shouldn\'t do nothing if Ganglia is in allSelectedServiceNames', function () {
      var allSelectedServiceNames = ['GANGLIA'],
        configs = [
          {name: 'nimbus.childopts', value: '.jar=host=host2', defaultValue: ''},
          {name: 'supervisor.childopts', value: '.jar=host=host2', defaultValue: ''},
          {name: 'worker.childopts', value: '.jar=host=host2', defaultValue: ''}
        ],
        expected = [
          {name: 'nimbus.childopts', value: '.jar=host=host1', defaultValue: '.jar=host=host1', forceUpdate: true},
          {name: 'supervisor.childopts', value: '.jar=host=host1', defaultValue: '.jar=host=host1', forceUpdate: true},
          {name: 'worker.childopts', value: '.jar=host=host1', defaultValue: '.jar=host=host1', forceUpdate: true}
        ];
      installerStep7Controller.reopen({allSelectedServiceNames: allSelectedServiceNames});
      installerStep7Controller.resolveStormConfigs(configs);
      Em.keys(expected[0]).forEach(function (k) {
        expect(configs.mapProperty(k)).to.eql(expected.mapProperty(k));
      });
    });

    it('shouldn\'t do nothing if Ganglia is in installedServiceNames (2)', function () {
      var installedServiceNames = ['GANGLIA'],
        configs = [
          {name: 'nimbus.childopts', value: '.jar=host=host2', defaultValue: ''},
          {name: 'supervisor.childopts', value: '.jar=host=host2', defaultValue: ''},
          {name: 'worker.childopts', value: '.jar=host=host2', defaultValue: ''}
        ],
        expected = [
          {name: 'nimbus.childopts', value: '.jar=host=host1', defaultValue: '.jar=host=host1', forceUpdate: true},
          {name: 'supervisor.childopts', value: '.jar=host=host1', defaultValue: '.jar=host=host1', forceUpdate: true},
          {name: 'worker.childopts', value: '.jar=host=host1', defaultValue: '.jar=host=host1', forceUpdate: true}
        ];
      installerStep7Controller.reopen({installedServiceNames: installedServiceNames});
      installerStep7Controller.resolveStormConfigs(configs);
      Em.keys(expected[0]).forEach(function (k) {
        expect(configs.mapProperty(k)).to.eql(expected.mapProperty(k));
      });
    });

    it('should replace host name for *.childopts properties if Ganglia is in installedServiceNames for Add Service Wizard', function () {
      var installedServiceNames = ['GANGLIA'],
        configs = [
          {name: 'nimbus.childopts', value: '.jar=host=host2', defaultValue: ''},
          {name: 'supervisor.childopts', value: '.jar=host=host2', defaultValue: ''},
          {name: 'worker.childopts', value: '.jar=host=host2', defaultValue: ''}
        ],
        expected = [
          {name: 'nimbus.childopts', value: '.jar=host=realhost1', defaultValue: '.jar=host=realhost1', forceUpdate: true},
          {name: 'supervisor.childopts', value: '.jar=host=realhost1', defaultValue: '.jar=host=realhost1', forceUpdate: true},
          {name: 'worker.childopts', value: '.jar=host=realhost1', defaultValue: '.jar=host=realhost1', forceUpdate: true}
        ];
      installerStep7Controller.reopen({
        installedServiceNames: installedServiceNames,
        wizardController: Em.Object.create({
          name: 'addServiceController',
          masterComponentHosts: [{component: 'GANGLIA_SERVER', hostName: 'realhost1'}],
          getDBProperty: function (k) {
            return this.get(k);
          }
        })
      });
      installerStep7Controller.resolveStormConfigs(configs);
      Em.keys(expected[0]).forEach(function (k) {
        expect(configs.mapProperty(k)).to.eql(expected.mapProperty(k));
      });
    });

  });

  describe('#resolveServiceDependencyConfigs', function () {
    beforeEach(function () {
      sinon.stub(installerStep7Controller, 'resolveStormConfigs', Em.K);
      sinon.stub(installerStep7Controller, 'resolveYarnConfigs', Em.K);
    });
    afterEach(function () {
      installerStep7Controller.resolveStormConfigs.restore();
      installerStep7Controller.resolveYarnConfigs.restore();
    });
    var serviceNames = [
      {serviceName: 'STORM', method: "resolveStormConfigs"},
      {serviceName: 'YARN', method: "resolveYarnConfigs"}].forEach(function(t) {
      it("should call " + t.method + " if serviceName is " + t.serviceName, function () {
        var configs = [
          {},
          {}
        ];
        installerStep7Controller.resolveServiceDependencyConfigs(t.serviceName, configs);
        expect(installerStep7Controller[t.method].calledWith(configs)).to.equal(true);
      });
    });
  });

  describe('#selectedServiceObserver', function () {
    beforeEach(function () {
      installerStep7Controller.reopen({content: {services: []}});
      sinon.stub(installerStep7Controller, 'switchConfigGroupConfigs', Em.K);
    });
    afterEach(function () {
      installerStep7Controller.switchConfigGroupConfigs.restore();
    });
    it('shouldn\'t do nothing if App.supports.hostOverridesInstaller is false', function () {
      App.set('supports.hostOverridesInstaller', false);
      var configGroups = [
          {},
          {}
        ],
        selectedConfigGroup = {};
      installerStep7Controller.reopen({configGroups: configGroups, selectedConfigGroup: selectedConfigGroup});
      installerStep7Controller.selectedServiceObserver();
      expect(installerStep7Controller.get('configGroups')).to.eql(configGroups);
      expect(installerStep7Controller.get('selectedConfigGroup')).to.eql(selectedConfigGroup);
    });
    it('shouldn\'t do nothing if selectedService is null', function () {
      App.set('supports.hostOverridesInstaller', true);
      var configGroups = [
          {},
          {}
        ],
        selectedConfigGroup = {};
      installerStep7Controller.reopen({selectedService: null, configGroups: configGroups, selectedConfigGroup: selectedConfigGroup});
      installerStep7Controller.selectedServiceObserver();
      expect(installerStep7Controller.get('configGroups')).to.eql(configGroups);
      expect(installerStep7Controller.get('selectedConfigGroup')).to.eql(selectedConfigGroup);
    });
    it('shouldn\'t do nothing if selectedService.serviceName is MISC', function () {
      App.set('supports.hostOverridesInstaller', true);
      var configGroups = [
          {},
          {}
        ],
        selectedConfigGroup = {};
      installerStep7Controller.reopen({selectedService: {serviceName: 'MISC'}, configGroups: configGroups, selectedConfigGroup: selectedConfigGroup});
      installerStep7Controller.selectedServiceObserver();
      expect(installerStep7Controller.get('configGroups')).to.eql(configGroups);
      expect(installerStep7Controller.get('selectedConfigGroup')).to.eql(selectedConfigGroup);
    });
    it('should update configGroups and selectedConfigGroup', function () {
      App.set('supports.hostOverridesInstaller', true);
      var defaultGroup = {isDefault: true, n: 'n2'},
        configGroups = [
          {isDefault: false, n: 'n1'},
          defaultGroup,
          {n: 'n3'}
        ],
        selectedConfigGroup = {};
      installerStep7Controller.reopen({selectedService: {serviceName: 's1', configGroups: configGroups}});
      installerStep7Controller.selectedServiceObserver();
      expect(installerStep7Controller.get('configGroups').mapProperty('n')).to.eql(['n2', 'n1', 'n3']);
      expect(installerStep7Controller.get('selectedConfigGroup')).to.eql(defaultGroup);
    });
  });

  describe('#loadConfigGroups', function () {
    beforeEach(function () {
      installerStep7Controller.reopen({
        wizardController: Em.Object.create({
          allHosts: [
            {hostName: 'h1'},
            {hostName: 'h2'},
            {hostName: 'h3'}
          ]
        })
      });
    });
    it('shouldn\'t do nothing if only MISC available', function () {
      var configGroups = [
        {}
      ];
      installerStep7Controller.reopen({
        stepConfigs: [Em.Object.create({serviceName: 'MISC', configGroups: configGroups})]
      });
      installerStep7Controller.loadConfigGroups([]);
      expect(installerStep7Controller.get('stepConfigs.firstObject.configGroups')).to.eql(configGroups);
    });
    it('should set configGroups for service if they don\'t exist', function () {
      var configGroups = [],
        serviceName = 'HDFS',
        serviceConfigGroups = [
          {service: {id: 's1'}}
        ];
      installerStep7Controller.reopen({
        stepConfigs: [Em.Object.create({serviceName: serviceName, displayName: serviceName, configGroups: configGroups})]
      });
      var manageCGController = App.router.get('manageConfigGroupsController');
      sinon.stub(manageCGController, 'hostsToPublic', function(data){return ['c6401','c6402','c6403']});
      installerStep7Controller.loadConfigGroups(serviceConfigGroups);
      expect(installerStep7Controller.get('stepConfigs.firstObject.configGroups.length')).to.equal(1);
      var group = installerStep7Controller.get('stepConfigs.firstObject.configGroups.firstObject');
      expect(group.get('name')).to.equal(serviceName + ' Default');
      expect(group.get('description').contains(serviceName)).to.equal(true);
      expect(group.get('isDefault')).to.equal(true);
      expect(group.get('hosts')).to.eql(['h1', 'h2', 'h3']);
      expect(group.get('service.id')).to.equal(serviceName);
      expect(group.get('serviceName')).to.equal(serviceName);
      manageCGController.hostsToPublic.restore();
    });
    it('should update configGroups for service (only default group)', function () {
      var configGroups = [],
        serviceName = 'HDFS',
        serviceConfigGroups = [
          {service: {id: 'HDFS'}, isDefault: true, n: 'n1'}
        ];
      installerStep7Controller.reopen({
        stepConfigs: [Em.Object.create({serviceName: serviceName, displayName: serviceName, configGroups: configGroups})]
      });
      installerStep7Controller.loadConfigGroups(serviceConfigGroups);
      expect(installerStep7Controller.get('stepConfigs.firstObject.configGroups').findProperty('isDefault').get('n')).to.equal('n1');
    });
    it('should update configGroups for service', function () {
      var configGroups = [],
        serviceName = 'HDFS',
        properties = [
          { name: "p1", filename: "file.xml" },
          { name: "p2", filename: "file.xml" }
        ],
        serviceConfigGroups = [
          {service: {id: 'HDFS'}, properties: properties.slice(), isDefault: true, n: 'n1'},
          {service: {id: 'HDFS'}, properties: properties.slice(), isDefault: false, n: 'n2'}
        ];
      installerStep7Controller.reopen({
        stepConfigs: [Em.Object.create({serviceName: serviceName, configGroups: configGroups, configs: properties})]
      });
      installerStep7Controller.loadConfigGroups(serviceConfigGroups);
      expect(installerStep7Controller.get('stepConfigs.firstObject.configGroups.length')).to.equal(2);
      expect(installerStep7Controller.get('stepConfigs.firstObject.configGroups').findProperty('isDefault').get('n')).to.equal('n1');
      expect(installerStep7Controller.get('stepConfigs.firstObject.configGroups').findProperty('isDefault', false).get('properties').everyProperty('group.n', 'n2')).to.equal(true);
      expect(installerStep7Controller.get('stepConfigs.firstObject.configGroups').findProperty('isDefault', false).get('parentConfigGroup.n')).to.equal('n1');
    });
  });

  describe('#_getDisplayedConfigGroups', function () {
    it('should return [] if no selected group', function () {
      installerStep7Controller.reopen({
        content: {services: []},
        selectedConfigGroup: null
      });
      expect(installerStep7Controller._getDisplayedConfigGroups()).to.eql([]);
    });
    it('should return default config group if another selected', function () {
      var defaultGroup = Em.Object.create({isDefault: false});
      installerStep7Controller.reopen({
        content: {services: []},
        selectedConfigGroup: defaultGroup
      });
      expect(installerStep7Controller._getDisplayedConfigGroups()).to.eql([defaultGroup]);
    });
    it('should return other groups if default selected', function () {
      var defaultGroup = Em.Object.create({isDefault: true}),
        cfgG = Em.Object.create({isDefault: true}),
        configGroups = Em.A([
          Em.Object.create({isDefault: false}),
          Em.Object.create({isDefault: false}),
          cfgG,
          Em.Object.create({isDefault: false})
        ]);
      installerStep7Controller.reopen({
        content: {services: []},
        selectedConfigGroup: defaultGroup,
        selectedService: {configGroups: configGroups}
      });
      expect(installerStep7Controller._getDisplayedConfigGroups()).to.eql(configGroups.without(cfgG));
    });
  });

  describe('#_setEditableValue', function () {
    it('shouldn\'t update config if no selectedConfigGroup', function () {
      installerStep7Controller.reopen({
        selectedConfigGroup: null
      });
      var config = Em.Object.create({isEditable: null});
      var updatedConfig = installerStep7Controller._setEditableValue(config);
      expect(updatedConfig.get('isEditable')).to.be.null;
    });
    it('should set isEditable equal to selectedGroup.isDefault if service not installed', function () {
      var isDefault = true;
      installerStep7Controller.reopen({
        installedServiceNames: [],
        selectedService: {serviceName: 'abc'},
        selectedConfigGroup: Em.Object.create({isDefault: isDefault})
      });
      var config = Em.Object.create({isEditable: null});
      var updatedConfig = installerStep7Controller._setEditableValue(config);
      expect(updatedConfig.get('isEditable')).to.equal(isDefault);
      installerStep7Controller.toggleProperty('selectedConfigGroup.isDefault');
      updatedConfig = installerStep7Controller._setEditableValue(config);
      expect(updatedConfig.get('isEditable')).to.equal(!isDefault);
    });
    Em.A([
        {
          isEditable: false,
          isReconfigurable: false,
          isDefault: true,
          e: false
        },
        {
          isEditable: true,
          isReconfigurable: true,
          isDefault: true,
          e: true
        },
        {
          isEditable: false,
          isReconfigurable: true,
          isDefault: false,
          e: false
        },
        {
          isEditable: true,
          isReconfigurable: false,
          isDefault: false,
          e: false
        }
      ]).forEach(function (test) {
        it('service installed, isEditable = ' + test.isEditable.toString() + ', isReconfigurable = ' + test.isReconfigurable.toString(), function () {
          var config = Em.Object.create({
            isReconfigurable: test.isReconfigurable,
            isEditable: test.isEditable
          });
          installerStep7Controller.reopen({
            installedServiceNames: Em.A(['a']),
            selectedService: Em.Object.create({serviceName: 'a'}),
            selectedConfigGroup: Em.Object.create({isDefault: test.isDefault})
          });
          var updateConfig = installerStep7Controller._setEditableValue(config);
          expect(updateConfig.get('isEditable')).to.equal(test.e);
        });
      });
  });

  describe('#_setOverrides', function () {
    it('shouldn\'t update config if no selectedConfigGroup', function () {
      installerStep7Controller.reopen({
        selectedConfigGroup: null
      });
      var config = Em.Object.create({overrides: null});
      var updatedConfig = installerStep7Controller._setOverrides(config, []);
      expect(updatedConfig.get('overrides')).to.be.null;
    });
    it('no overrideToAdd', function () {
      var isDefault = true,
        name = 'n1',
        config = Em.Object.create({overrides: null, name: name, flag: 'flag'}),
        overrides = Em.A([
          Em.Object.create({name: name, value: 'v1'}),
          Em.Object.create({name: name, value: 'v2'}),
          Em.Object.create({name: 'n2', value: 'v3'})
        ]);
      installerStep7Controller.reopen({
        overrideToAdd: null,
        selectedConfigGroup: Em.Object.create({
          isDefault: isDefault
        })
      });
      var updatedConfig = installerStep7Controller._setOverrides(config, overrides);
      expect(updatedConfig.get('overrides.length')).to.equal(2);
      expect(updatedConfig.get('overrides').everyProperty('isEditable', !isDefault)).to.equal(true);
      expect(updatedConfig.get('overrides').everyProperty('parentSCP.flag', 'flag')).to.equal(true);
    });
    it('overrideToAdd exists', function () {
      var isDefault = true,
        name = 'n1',
        config = Em.Object.create({overrides: null, name: name, flag: 'flag'}),
        overrides = Em.A([
          Em.Object.create({name: name, value: 'v1'}),
          Em.Object.create({name: name, value: 'v2'}),
          Em.Object.create({name: 'n2', value: 'v3'})
        ]);
      installerStep7Controller.reopen({
        overrideToAdd: Em.Object.create({name: name}),
        selectedService: {configGroups: [Em.Object.create({name: 'n', properties: []})]},
        selectedConfigGroup: Em.Object.create({
          isDefault: isDefault,
          name: 'n'
        })
      });
      var updatedConfig = installerStep7Controller._setOverrides(config, overrides);
      expect(updatedConfig.get('overrides.length')).to.equal(3);
      expect(updatedConfig.get('overrides').everyProperty('isEditable', !isDefault)).to.equal(true);
      expect(updatedConfig.get('overrides').everyProperty('parentSCP.flag', 'flag')).to.equal(true);
    });
  });

  describe('#switchConfigGroupConfigs', function () {
    it('if selectedConfigGroup is null, serviceConfigs shouldn\'t be changed', function () {
      installerStep7Controller.reopen({
        selectedConfigGroup: null,
        content: {services: []},
        serviceConfigs: {configs: [
          {overrides: []},
          {overrides: []}
        ]}
      });
      installerStep7Controller.switchConfigGroupConfigs();
      expect(installerStep7Controller.get('serviceConfigs.configs').everyProperty('overrides.length', 0)).to.equal(true);
    });
    it('should set configs for serviceConfigs', function () {
      var configGroups = [
        Em.Object.create({
          properties: [
            {name: 'g1', value: 'v1'},
            {name: 'g2', value: 'v2'}
          ]
        })
      ];
      sinon.stub(installerStep7Controller, '_getDisplayedConfigGroups', function () {
        return configGroups;
      });
      sinon.stub(installerStep7Controller, '_setEditableValue', function (config) {
        config.set('isEditable', true);
        return config;
      });
      installerStep7Controller.reopen({
        selectedConfigGroup: Em.Object.create({isDefault: true, name: 'g1'}),
        content: {services: []},
        selectedService: {configs: Em.A([Em.Object.create({name: 'g1', overrides: [], properties: []}), Em.Object.create({name: 'g2', overrides: []})])},
        serviceConfigs: {configs: [Em.Object.create({name: 'g1'})]}
      });
      installerStep7Controller.switchConfigGroupConfigs();
      var configs = installerStep7Controller.get('selectedService.configs');
      expect(configs.findProperty('name', 'g1').get('overrides').length).to.equal(1);
      expect(configs.findProperty('name', 'g2').get('overrides').length).to.equal(1);
      expect(configs.everyProperty('isEditable', true)).to.equal(true);
      installerStep7Controller._getDisplayedConfigGroups.restore();
      installerStep7Controller._setEditableValue.restore();
    });
  });

  describe('#selectProperService', function () {
    Em.A([
        {
          name: 'addServiceController',
          stepConfigs: [
            {selected: false, name: 'n1'},
            {selected: true, name: 'n2'},
            {selected: true, name: 'n3'}
          ],
          e: 'n2'
        },
        {
          name: 'installerController',
          stepConfigs: [
            {showConfig: false, name: 'n1'},
            {showConfig: false, name: 'n2'},
            {showConfig: true, name: 'n3'}
          ],
          e: 'n3'
        }
      ]).forEach(function (test) {
        it(test.name, function () {
          sinon.stub(installerStep7Controller, 'selectedServiceObserver', Em.K);
          installerStep7Controller.reopen({
            wizardController: Em.Object.create({
              name: test.name
            }),
            stepConfigs: test.stepConfigs
          });
          installerStep7Controller.selectProperService();
          expect(installerStep7Controller.get('selectedService.name')).to.equal(test.e);
          installerStep7Controller.selectedServiceObserver.restore();
        });
      });
  });

  describe('#setStepConfigs', function () {
    beforeEach(function () {
      installerStep7Controller.reopen({
        content: {services: []},
        wizardController: Em.Object.create({
          getDBProperty: function (key) {
            return this.get(key);
          }
        })
      });
    });
    afterEach(function () {
      App.config.renderConfigs.restore();
    });
    it('if wizard isn\'t addService, should set output of App.config.renderConfigs', function () {
      var serviceConfigs = Em.A([
        {},
        {}
      ]);
      sinon.stub(App.config, 'renderConfigs', function () {
        return serviceConfigs;
      });
      installerStep7Controller.set('wizardController.name', 'installerController');
      installerStep7Controller.setStepConfigs([], []);
      expect(installerStep7Controller.get('stepConfigs')).to.eql(serviceConfigs);
    });
    it('addServiceWizard used', function () {
      var serviceConfigs = Em.A([Em.Object.create({serviceName: 's1'}), Em.Object.create({serviceName: 's2'})]);
      installerStep7Controller.set('wizardController.name', 'addServiceController');
      installerStep7Controller.reopen({selectedServiceNames: ['s2']});
      sinon.stub(App.config, 'renderConfigs', function () {
        return serviceConfigs;
      });
      installerStep7Controller.setStepConfigs([], []);
      expect(installerStep7Controller.get('stepConfigs').everyProperty('showConfig', true)).to.equal(true);
      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 's2').get('selected')).to.equal(true);
    });
    it('addServiceWizard used, HA enabled', function () {
      sinon.stub(App, 'get', function (k) {
        if (k === 'isHaEnabled') {
          return true;
        }
        return Em.get(App, k);
      });
      var serviceConfigs = Em.A([
        Em.Object.create({
          serviceName: 'HDFS',
          configs: [
            {category: 'SECONDARY_NAMENODE'},
            {category: 'SECONDARY_NAMENODE'},
            {category: 'NameNode'},
            {category: 'NameNode'},
            {category: 'SECONDARY_NAMENODE'}
          ]
        }),
        Em.Object.create({serviceName: 's2'})]
      );
      installerStep7Controller.set('wizardController.name', 'addServiceController');
      installerStep7Controller.reopen({selectedServiceNames: ['HDFS', 's2']});
      sinon.stub(App.config, 'renderConfigs', function () {
        return serviceConfigs;
      });
      installerStep7Controller.setStepConfigs([], []);
      expect(installerStep7Controller.get('stepConfigs').everyProperty('showConfig', true)).to.equal(true);
      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 'HDFS').get('selected')).to.equal(true);
      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 'HDFS').get('configs').length).to.equal(2);
      App.get.restore();
    });
  });

  describe('#checkHostOverrideInstaller', function () {
    beforeEach(function () {
      sinon.stub(installerStep7Controller, 'loadConfigGroups', Em.K);
      sinon.stub(installerStep7Controller, 'loadInstalledServicesConfigGroups', Em.K);
    });
    afterEach(function () {
      installerStep7Controller.loadConfigGroups.restore();
      installerStep7Controller.loadInstalledServicesConfigGroups.restore();
      App.get.restore();
    });
    Em.A([
        {
          hostOverridesInstaller: false,
          installedServiceNames: [],
          m: 'hostOverridesInstaller is false, installedServiceNames is empty',
          e: {
            loadConfigGroups: false,
            loadInstalledServicesConfigGroups: false
          }
        },
        {
          hostOverridesInstaller: false,
          installedServiceNames: ['s1', 's2'],
          m: 'hostOverridesInstaller is false, installedServiceNames is n\'t empty',
          e: {
            loadConfigGroups: false,
            loadInstalledServicesConfigGroups: false
          }
        },
        {
          hostOverridesInstaller: true,
          installedServiceNames: [],
          m: 'hostOverridesInstaller is true, installedServiceNames is empty',
          e: {
            loadConfigGroups: true,
            loadInstalledServicesConfigGroups: false
          }
        },
        {
          hostOverridesInstaller: true,
          installedServiceNames: ['s1', 's2', 's3'],
          m: 'hostOverridesInstaller is true, installedServiceNames isn\'t empty',
          e: {
            loadConfigGroups: true,
            loadInstalledServicesConfigGroups: true
          }
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          sinon.stub(App, 'get', function (k) {
            if (k === 'supports.hostOverridesInstaller') return test.hostOverridesInstaller;
            return Em.get(App, k);
          });
          installerStep7Controller.reopen({installedServiceNames: test.installedServiceNames});
          installerStep7Controller.checkHostOverrideInstaller();
          if (test.e.loadConfigGroups) {
            expect(installerStep7Controller.loadConfigGroups.calledOnce).to.equal(true);
          }
          else {
            expect(installerStep7Controller.loadConfigGroups.called).to.equal(false);
          }
          if (test.e.loadInstalledServicesConfigGroups) {
            expect(installerStep7Controller.loadInstalledServicesConfigGroups.calledOnce).to.equal(true);
          }
          else {
            expect(installerStep7Controller.loadInstalledServicesConfigGroups.called).to.equal(false);
          }
        });
      });
  });

  describe('#loadStep', function () {
    beforeEach(function () {
      installerStep7Controller.reopen({
        content: {services: []},
        wizardController: Em.Object.create({
          getDBProperty: function (k) {
            return this.get(k);
          }
        })
      });
      sinon.stub(App.config, 'mergePreDefinedWithStored', Em.K);
      sinon.stub(App.config, 'addAdvancedConfigs', Em.K);
      sinon.stub(App.config, 'addCustomConfigs', Em.K);
      sinon.stub(App.config, 'fileConfigsIntoTextarea', Em.K);
      sinon.stub(installerStep7Controller, 'clearStep', Em.K);
      sinon.stub(installerStep7Controller, 'getConfigTags', Em.K);
      sinon.stub(installerStep7Controller, 'setInstalledServiceConfigs', Em.K);
      sinon.stub(installerStep7Controller, 'resolveServiceDependencyConfigs', Em.K);
      sinon.stub(installerStep7Controller, 'setStepConfigs', Em.K);
      sinon.stub(installerStep7Controller, 'checkHostOverrideInstaller', Em.K);
      sinon.stub(installerStep7Controller, 'activateSpecialConfigs', Em.K);
      sinon.stub(installerStep7Controller, 'selectProperService', Em.K);
      sinon.stub(installerStep7Controller, 'applyServicesConfigs', Em.K);
      sinon.stub(App.router, 'send', Em.K);
    });
    afterEach(function () {
      App.config.mergePreDefinedWithStored.restore();
      App.config.addAdvancedConfigs.restore();
      App.config.addCustomConfigs.restore();
      App.config.fileConfigsIntoTextarea.restore();
      installerStep7Controller.clearStep.restore();
      installerStep7Controller.getConfigTags.restore();
      installerStep7Controller.setInstalledServiceConfigs.restore();
      installerStep7Controller.resolveServiceDependencyConfigs.restore();
      installerStep7Controller.setStepConfigs.restore();
      installerStep7Controller.checkHostOverrideInstaller.restore();
      installerStep7Controller.activateSpecialConfigs.restore();
      installerStep7Controller.selectProperService.restore();
      installerStep7Controller.applyServicesConfigs.restore();
      App.router.send.restore();
    });
    it('should call clearStep', function () {
      installerStep7Controller.loadStep();
      expect(installerStep7Controller.clearStep.calledOnce).to.equal(true);
    });
    it('shouldn\'t do nothing if isAdvancedConfigLoaded is false', function () {
      installerStep7Controller.set('isAdvancedConfigLoaded', false);
      installerStep7Controller.loadStep();
      expect(installerStep7Controller.clearStep.called).to.equal(false);
    });
    it('should use App.config to map configs', function () {
      installerStep7Controller.loadStep();
      expect(App.config.mergePreDefinedWithStored.calledOnce).to.equal(true);
      expect(App.config.addAdvancedConfigs.calledOnce).to.equal(true);
      expect(App.config.addCustomConfigs.calledOnce).to.equal(true);
    });
    it('should call setInstalledServiceConfigs for addServiceController', function () {
      installerStep7Controller.set('wizardController.name', 'addServiceController');
      installerStep7Controller.loadStep();
      expect(installerStep7Controller.setInstalledServiceConfigs.calledOnce).to.equal(true);
    });
  });

  describe('#applyServicesConfigs', function() {
    beforeEach(function() {
      installerStep7Controller.reopen({
        allSelectedServiceNames: []
      });
      sinon.stub(App.config, 'fileConfigsIntoTextarea', function(configs) {
        return configs;
      });
      sinon.stub(installerStep7Controller, 'resolveServiceDependencyConfigs', Em.K);
      sinon.stub(installerStep7Controller, 'loadServerSideConfigsRecommendations', function() {
        return $.Deferred().resolve();
      });
      sinon.stub(installerStep7Controller, 'checkHostOverrideInstaller', Em.K);
      sinon.stub(installerStep7Controller, 'activateSpecialConfigs', Em.K);
      sinon.stub(installerStep7Controller, 'selectProperService', Em.K);
      sinon.stub(installerStep7Controller, 'setStepConfigs', Em.K);
      sinon.stub(App.router, 'send', Em.K);
    });
    afterEach(function () {
      App.config.fileConfigsIntoTextarea.restore();
      installerStep7Controller.resolveServiceDependencyConfigs.restore();
      installerStep7Controller.loadServerSideConfigsRecommendations.restore();
      installerStep7Controller.checkHostOverrideInstaller.restore();
      installerStep7Controller.activateSpecialConfigs.restore();
      installerStep7Controller.selectProperService.restore();
      installerStep7Controller.setStepConfigs.restore();
      App.router.send.restore();
    });

    it('should run some methods' , function () {
     installerStep7Controller.applyServicesConfigs({name: 'configs'}, {name: 'storedConfigs'});
     expect(installerStep7Controller.loadServerSideConfigsRecommendations.calledOnce).to.equal(true);
     expect(installerStep7Controller.get('isRecommendedLoaded')).to.equal(true);
     expect(installerStep7Controller.setStepConfigs.calledWith({name: 'configs'}, {name: 'storedConfigs'})).to.equal(true);
     expect(installerStep7Controller.checkHostOverrideInstaller.calledOnce).to.equal(true);
     expect(installerStep7Controller.activateSpecialConfigs.calledOnce).to.equal(true);
     expect(installerStep7Controller.selectProperService.calledOnce).to.equal(true);
    });

    Em.A([
      {
        allSelectedServiceNames: ['YARN'],
        fileConfigsIntoTextarea: true,
        m: 'should run fileConfigsIntoTextarea and resolveServiceDependencyConfigs',
        resolveServiceDependencyConfigs: true,
        capacitySchedulerUi: false
      },
      {
        allSelectedServiceNames: ['YARN'],
        m: 'shouldn\'t run fileConfigsIntoTextarea but  run resolveServiceDependencyConfigs',
        resolveServiceDependencyConfigs: true,
        capacitySchedulerUi: true
      },
      {
        allSelectedServiceNames: ['STORM'],
        resolveServiceDependencyConfigs: true,
        m: 'should run resolveServiceDependencyConfigs'
      }
    ]).forEach(function(t) {
      it(t.m, function () {
        sinon.stub(App, 'get', function (k) {
          if (k === 'supports.capacitySchedulerUi') return t.capacitySchedulerUi;
          return Em.get(App, k);
        });
        installerStep7Controller.reopen({
          allSelectedServiceNames: t.allSelectedServiceNames
        });
        installerStep7Controller.applyServicesConfigs({name: 'configs'}, {name: 'storedConfigs'});
        if (t.fileConfigsIntoTextarea) {
          expect(App.config.fileConfigsIntoTextarea.calledWith({name: 'configs'}, 'capacity-scheduler.xml')).to.equal(true);
        } else {
          expect(App.config.fileConfigsIntoTextarea.calledOnce).to.equal(false);
        }
        if (t.resolveServiceDependencyConfigs) {
          expect(installerStep7Controller.resolveServiceDependencyConfigs.calledWith(t.allSelectedServiceNames[0], {name: 'configs'})).to.equal(true);
        } else {
          expect(installerStep7Controller.resolveServiceDependencyConfigs.calledOnce).to.equal(false);
        }
        App.get.restore();
      });
    });
  });
  describe('#_updateValueForCheckBoxConfig', function () {
    Em.A([
        {
          v: 'true',
          e: true
        },
        {
          v: 'false',
          e: false
        }
      ]).forEach(function (test) {
        it(test.v, function () {
          var serviceConfigProperty = Em.Object.create({value: test.v});
          installerStep7Controller._updateValueForCheckBoxConfig(serviceConfigProperty);
          expect(serviceConfigProperty.get('value')).to.equal(test.e);
          expect(serviceConfigProperty.get('defaultValue')).to.equal(test.e);
        });
      });
  });

  describe('#_updateIsEditableFlagForConfig', function () {
    Em.A([
        {
          isAdmin: false,
          isReconfigurable: false,
          isHostsConfigsPage: true,
          defaultGroupSelected: false,
          m: 'false for non-admin users',
          e: false
        },
        {
          isAdmin: true,
          isReconfigurable: false,
          isHostsConfigsPage: true,
          defaultGroupSelected: false,
          m: 'false if defaultGroupSelected is false and isHostsConfigsPage is true',
          e: false
        },
        {
          isAdmin: true,
          isReconfigurable: false,
          isHostsConfigsPage: true,
          defaultGroupSelected: true,
          m: 'false if defaultGroupSelected is true and isHostsConfigsPage is true',
          e: false
        },
        {
          isAdmin: true,
          isReconfigurable: false,
          isHostsConfigsPage: false,
          defaultGroupSelected: false,
          m: 'false if defaultGroupSelected is false and isHostsConfigsPage is false',
          e: false
        },
        {
          isAdmin: true,
          isReconfigurable: true,
          isHostsConfigsPage: false,
          defaultGroupSelected: true,
          m: 'equal to isReconfigurable if defaultGroupSelected is true and isHostsConfigsPage is false',
          e: true
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          sinon.stub(App, 'get', function (k) {
            if (k === 'isAdmin') return test.isAdmin;
            return Em.get(App, k);
          });
          installerStep7Controller.reopen({isHostsConfigsPage: test.isHostsConfigsPage});
          var serviceConfigProperty = Em.Object.create({
            isReconfigurable: test.isReconfigurable
          });
          installerStep7Controller._updateIsEditableFlagForConfig(serviceConfigProperty, test.defaultGroupSelected);
          App.get.restore();
          expect(serviceConfigProperty.get('isEditable')).to.equal(test.e);
        });
      });
  });

  describe('#_updateOverridesForConfig', function () {

    it('should set empty array', function () {
      var serviceConfigProperty = Em.Object.create({
        overrides: null
      }), component = Em.Object.create();
      installerStep7Controller._updateOverridesForConfig(serviceConfigProperty, component);
      expect(serviceConfigProperty.get('overrides')).to.eql(Em.A([]));
    });

    it('host overrides not supported', function () {
      var serviceConfigProperty = Em.Object.create({
        overrides: [
          {value: 'new value'}
        ]
      }), component = Em.Object.create({selectedConfigGroup: {isDefault: false}});
      installerStep7Controller._updateOverridesForConfig(serviceConfigProperty, component);
      expect(serviceConfigProperty.get('overrides').length).to.equal(1);
      expect(serviceConfigProperty.get('overrides.firstObject.value')).to.equal('new value');
      expect(serviceConfigProperty.get('overrides.firstObject.isOriginalSCP')).to.equal(false);
      expect(serviceConfigProperty.get('overrides.firstObject.parentSCP')).to.eql(serviceConfigProperty);
    });

    it('host overrides supported', function () {
      sinon.stub(App, 'get', function (k) {
        if (k === 'supports.hostOverrides') return true;
        return Em.get(App, k);
      });
      var serviceConfigProperty = Em.Object.create({
          overrides: [
            {value: 'new value', group: Em.Object.create({name: 'n1'})}
          ]
        }),
        component = Em.Object.create({
          selectedConfigGroup: {isDefault: true},
          configGroups: Em.A([
            Em.Object.create({name: 'n1', properties: []})
          ])
        });
      installerStep7Controller._updateOverridesForConfig(serviceConfigProperty, component);
      App.get.restore();
      expect(serviceConfigProperty.get('overrides').length).to.equal(1);
      expect(serviceConfigProperty.get('overrides.firstObject.value')).to.equal('new value');
      expect(serviceConfigProperty.get('overrides.firstObject.isOriginalSCP')).to.equal(false);
      expect(serviceConfigProperty.get('overrides.firstObject.parentSCP')).to.eql(serviceConfigProperty);
      expect(component.get('configGroups.firstObject.properties').length).to.equal(1);
      expect(component.get('configGroups.firstObject.properties.firstObject.isEditable')).to.equal(false);
      expect(component.get('configGroups.firstObject.properties.firstObject.group')).to.be.object;
    });

  });

  describe('#setSecureConfigs', function() {
    var serviceConfigObj = Em.Object.create({
      serviceName: 'HDFS',
      configs: [
        Em.Object.create({ name: 'hadoop.http.authentication.signature.secret.file' }),
        Em.Object.create({ name: 'hadoop.security.authentication' })
      ]
    });
    var tests = [
      { name: 'hadoop.http.authentication.signature.secret.file', e: false },
      { name: 'hadoop.security.authentication', e: true }
    ];

    sinon.stub(App, 'get', function(key) {
      if (['isHadoop22Stack', 'isHadoop2Stack'].contains(key)) return true;
      else App.get(key);
    });
    var controller = App.WizardStep7Controller.create({});
    controller.get('secureConfigs').pushObjects([
      {
        name: 'hadoop.http.authentication.signature.secret.file',
        serviceName: 'HDFS',
        value: ''
      }
    ]);
    controller.setSecureConfigs(serviceConfigObj, 'HDFS');
    App.get.restore();
    tests.forEach(function(test) {
      it('{0} is {1}required'.format(test.name, !!test.e ? '' : 'non ' ), function() {
        expect(serviceConfigObj.get('configs').findProperty('name', test.name).get('isRequired')).to.eql(test.e);
      });
    });
  });

});
