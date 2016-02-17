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

var installerStep7Controller,
  issuesFilterCases = [
    {
      isSubmitDisabled: true,
      submitButtonClicked: true,
      isIssuesFilterActive: true,
      issuesFilterText: '',
      issuesFilterLinkText: Em.I18n.t('installer.step7.showAllProperties'),
      title: 'issues filter on, submit button clicked'
    },
    {
      isSubmitDisabled: true,
      submitButtonClicked: false,
      isIssuesFilterActive: true,
      issuesFilterText: Em.I18n.t('installer.step7.showingPropertiesWithIssues'),
      issuesFilterLinkText: Em.I18n.t('installer.step7.showAllProperties'),
      title: 'issues filter on, submit button disabled'
    },
    {
      isSubmitDisabled: true,
      submitButtonClicked: true,
      isIssuesFilterActive: false,
      issuesFilterText: '',
      issuesFilterLinkText: '',
      title: 'issues filter off, submit button clicked'
    },
    {
      isSubmitDisabled: true,
      submitButtonClicked: false,
      isIssuesFilterActive: false,
      issuesFilterText: '',
      issuesFilterLinkText: Em.I18n.t('installer.step7.showPropertiesWithIssues'),
      title: 'issues filter off, submit button disabled'
    },
    {
      isSubmitDisabled: false,
      submitButtonClicked: false,
      isIssuesFilterActive: true,
      issuesFilterText: '',
      issuesFilterLinkText: Em.I18n.t('installer.step7.showAllProperties'),
      title: 'issues filter on, submit button enabled'
    },
    {
      isSubmitDisabled: false,
      submitButtonClicked: false,
      isIssuesFilterActive: false,
      issuesFilterText: '',
      issuesFilterLinkText: '',
      title: 'issues filter off, submit button enabled'
    },
    {
      isSubmitDisabled: false,
      submitButtonClicked: false,
      isIssuesFilterActive: true,
      issuesFilterText: '',
      issuesFilterLinkText: Em.I18n.t('installer.step7.showAllProperties'),
      title: 'issues filter on, submit button not clicked but active'
    },
    {
      isSubmitDisabled: false,
      submitButtonClicked: true,
      isIssuesFilterActive: true,
      issuesFilterText: '',
      issuesFilterLinkText: Em.I18n.t('installer.step7.showAllProperties'),
      title: 'issues filter on, submit button clicked and active'
    }
  ],
  issuesFilterTestSetup = function (controller, testCase) {
    controller.set('submitButtonClicked', testCase.submitButtonClicked);
    controller.reopen({
      isSubmitDisabled: testCase.isSubmitDisabled
    });
    controller.get('filterColumns').findProperty('attributeName', 'hasIssues').set('selected', testCase.isIssuesFilterActive);
  };

describe('App.InstallerStep7Controller', function () {

  beforeEach(function () {
    sinon.stub(App.config, 'setPreDefinedServiceConfigs', Em.K);
    installerStep7Controller = App.WizardStep7Controller.create({
      content: Em.Object.create({
        advancedServiceConfig: [],
        serviceConfigProperties: []
      })
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

  describe('#_createSiteToTagMap', function () {
    it('should return filtered map', function () {
      var desired_configs = {
        site1: {
          tag: "tag1"
        },
        site2: {
          tag: "tag2"
        },
        site3: {
          tag: "tag3"
        }
      };
      var sites = {
        site1: true,
        site3: true
      };
      var siteToTagMap = installerStep7Controller._createSiteToTagMap(desired_configs,sites)
      expect(siteToTagMap).to.eql({
        site1: "tag1",
        site3: "tag3"
      });
    });
  });

  describe('#checkDatabaseConnectionTest', function () {
    it('should return promise in process', function () {
      installerStep7Controller.set('content', {
        services: Em.A([
          Em.Object.create({isSelected: true, isInstalled: false, serviceName: 'OOZIE', ignored: []}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 'HIVE', ignored: []}),
          Em.Object.create({isSelected: true, isInstalled: true, serviceName: 's3', ignored: []}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's4', ignored: []}),
          Em.Object.create({isSelected: true, isInstalled: false, serviceName: 's5', ignored: []}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's6', ignored: []}),
          Em.Object.create({isSelected: true, isInstalled: true, serviceName: 's7', ignored: []}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's8', ignored: []})
        ])
      });
      var obj = Em.Object.create({name:'oozie_database',value:"aa"});
      installerStep7Controller.set('stepConfigs',Em.A([Em.Object.create({serviceName: 'OOZIE', configs: Em.A([obj]) })]));
      var deffer = installerStep7Controller.checkDatabaseConnectionTest();
      expect(deffer.isResolved()).to.equal(false);
      deffer.resolve(true);
      deffer.done(function(data) {
        expect(data).to.equal(true);
      });
    });
  });

  describe.skip('#submit', function () {

    beforeEach(function () {
      sinon.stub(App, 'get').withArgs('supports.preInstallChecks').returns(false);
    });

    afterEach(function () {
      App.get.restore();
    });

    it('should return false if submit disabled', function () {
      installerStep7Controller.set('isSubmitDisabled',true);
      expect(installerStep7Controller.submit()).to.be.false;
    });
    it('sumbit button should be unclicked if no configs', function () {
      installerStep7Controller.set('isSubmitDisabled',false);
      installerStep7Controller.submit();
      expect(installerStep7Controller.get('submitButtonClicked')).to.be.false;
    });
  });

  describe('#activateSpecialConfigs', function () {
    var expected = [{
       "smokeuser": {
         "isEditable": true
       },
       "group": {
         "isEditable": true
       },
       "services": [
         {
           "serviceName": "s1",
           "isSelected": true,
           "isInstalled": false
         },
         {
           "serviceName": "s2",
           "isSelected": false,
           "isInstalled": false
         },
         {
           "serviceName": "s3",
           "isSelected": true,
           "isInstalled": true
         },
         {
           "serviceName": "s4",
           "isSelected": false,
           "isInstalled": false
         },
         {
           "serviceName": "s5",
           "isSelected": true,
           "isInstalled": false
         },
         {
           "serviceName": "s6",
           "isSelected": false,
           "isInstalled": false
         },
         {
           "serviceName": "s7",
           "isSelected": true,
           "isInstalled": true
         },
         {
           "serviceName": "s8",
           "isSelected": false,
           "isInstalled": false
         }
       ]
      },[
        {
          "serviceName": "MISC",
          "configs": [
            {
              "name": "smokeuser",
              "value": {
                "isEditable": true
              },
              "isEditable": false
            },
            {
              "name": "user_group",
              "value": {
                "isEditable": true
              },
              "isEditable": false
            },
            {
              "name": "kdc_type"
            }
          ]
        },
        {
          "serviceName": "KERBEROS",
          "configs": [
            {
              "name": "smokeuser",
              "value": {
                "isEditable": true
              },
              "isEditable": false
            },
            {
              "name": "user_group",
              "value": {
                "isEditable": true
              },
              "isEditable": false
            },
            {
              "name": "kdc_type"
            }
          ]
        }
      ]];
      var allSelectedServiceNames = ['SLIDER', 'YARN'];
      var configs = Em.A([Em.Object.create({
        name: 'smokeuser',
        value: ''
      }),Em.Object.create({
        name: 'user_group',
        value: ''
      }),Em.Object.create({
        name: 'kdc_type',
        value: ''
      })]);
      var stepConfigs = Em.A([Em.Object.create({serviceName: 'MISC', configs: configs}),
                              Em.Object.create({serviceName: 'KERBEROS', configs: configs})]);
      var content = Em.Object.create({
        smokeuser: Em.Object.create({isEditable: true}),
        group: Em.Object.create({isEditable: true}),
        services: Em.A([
          Em.Object.create({isSelected: true, isInstalled: false, serviceName: 's1'}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's2'}),
          Em.Object.create({isSelected: true, isInstalled: true, serviceName: 's3'}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's4'}),
          Em.Object.create({isSelected: true, isInstalled: false, serviceName: 's5'}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's6'}),
          Em.Object.create({isSelected: true, isInstalled: true, serviceName: 's7'}),
          Em.Object.create({isSelected: false, isInstalled: false, serviceName: 's8'})
        ])
      });
    it('should return configs with true value', function () {
      installerStep7Controller.set('wizardController', Em.Object.create(App.LocalStorage, {name: 'addServiceController'}));
      installerStep7Controller.set('addMiscTabToPage',true);
      installerStep7Controller.reopen({allSelectedServiceNames: allSelectedServiceNames});
      installerStep7Controller.set('stepConfigs', stepConfigs);
      installerStep7Controller.set('content', content);
      installerStep7Controller.activateSpecialConfigs();
      expect(JSON.parse(JSON.stringify(installerStep7Controller.get('content')))).to.be.eql(expected[0]);
    });
    it('should return stepsConfigs with true value', function () {
      installerStep7Controller.set('wizardController', Em.Object.create(App.LocalStorage, {name: 'kerberosWizardController'}));
      installerStep7Controller.set('addMiscTabToPage',true);
      installerStep7Controller.reopen({allSelectedServiceNames: allSelectedServiceNames});
      installerStep7Controller.set('stepConfigs', stepConfigs);
      installerStep7Controller.set('content', content);
      installerStep7Controller.activateSpecialConfigs();
      expect(JSON.parse(JSON.stringify(installerStep7Controller.get('stepConfigs')))).to.be.eql(expected[1]);
    });
  });

  describe('#getConfigTagsSuccess', function () {
    beforeEach(function(){
      sinon.stub(App.StackService, 'find', function () {
        return [
          Em.Object.create({
            serviceName: 's0',
            isInstalled: true,
            configTypes: {
              site3: true,
              site1: true
            }
          }),
          Em.Object.create({
            serviceName: 's1',
            isInstalled: true,
            configTypes: {
              site1: true,
              site2: true
            }
          })
        ];
      });
    });
    afterEach(function(){
      App.StackService.find.restore();
    });

    it('should return serviceConfigTags', function () {
      var desired_configs = {
        site1: {
          tag: "tag1"
        },
        site2: {
          tag: "tag2"
        },
        site3: {
          tag: "tag3"
        }
      };
      var data = {
        Clusters: {
          desired_configs: desired_configs
        }
      };
      var siteToTagMap = installerStep7Controller.getConfigTagsSuccess(data)
      expect(installerStep7Controller.get('serviceConfigTags')).to.eql([
        {
          "siteName": "site1",
          "tagName": "tag1",
          "newTagName": null
        },
        {
          "siteName": "site2",
          "tagName": "tag2",
          "newTagName": null
        },
        {
          "siteName": "site3",
          "tagName": "tag3",
          "newTagName": null
        }
      ]);
      expect(installerStep7Controller.get('isAppliedConfigLoaded')).to.equal(true);
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

  describe('#checkMySQLHost', function () {
    it('should send query', function () {
      expect(installerStep7Controller.checkMySQLHost().readyState).to.equal(1);
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

  describe('#resolveYarnConfigs', function () {
    it('should set property to true', function () {
      var allSelectedServiceNames = ['SLIDER', 'YARN'],
        configs = [
          {name: 'hadoop.registry.rm.enabled', value: 'false', recommendedValue: 'false'}
        ],
        expected = [
          {name: 'hadoop.registry.rm.enabled', value: 'true', recommendedValue: 'true'}
        ];
      installerStep7Controller.reopen({allSelectedServiceNames: allSelectedServiceNames});
      installerStep7Controller.resolveYarnConfigs(configs);
      expect(configs[0]).to.eql(expected[0]);
    });

    it('should set property to false', function () {
      var allSelectedServiceNames = ['YARN'],
        configs = [
          {name: 'hadoop.registry.rm.enabled', value: 'true', recommendedValue: 'true'}
        ],
        expected = [
          {name: 'hadoop.registry.rm.enabled', value: 'false', recommendedValue: 'false'}
        ];
      installerStep7Controller.reopen({allSelectedServiceNames: allSelectedServiceNames});
      installerStep7Controller.resolveYarnConfigs(configs);
      expect(configs[0]).to.eql(expected[0]);
    });

    it('should skip setting property', function () {
      var allSelectedServiceNames = ['YARN', 'SLIDER'],
        configs = [
          {name: 'hadoop.registry.rm.enabled', value: 'true', recommendedValue: 'true'}
        ],
        expected = [
          {name: 'hadoop.registry.rm.enabled', value: 'true', recommendedValue: 'true'}
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

          getDBProperties: function (keys) {
            return this.getProperties(keys);
          }
        })
      });
    });

    it('shouldn\'t do nothing if Ganglia and Storm are installed', function () {
      var installedServiceNames = ['GANGLIA', 'STORM'],
        configs = [
          {name: 'nimbus.childopts', value: '.jar=host=host2', recommendedValue: ''},
          {name: 'supervisor.childopts', value: '.jar=host=host2', recommendedValue: ''},
          {name: 'worker.childopts', value: '.jar=host=host2', recommendedValue: ''}
        ],
        expected = [
          {name: 'nimbus.childopts', value: '.jar=host=host2', recommendedValue: ''},
          {name: 'supervisor.childopts', value: '.jar=host=host2', recommendedValue: ''},
          {name: 'worker.childopts', value: '.jar=host=host2', recommendedValue: ''}
        ];
      installerStep7Controller.reopen({installedServiceNames: installedServiceNames});
      installerStep7Controller.resolveStormConfigs(configs);
      expect(configs).to.eql(expected);
    });

    it('shouldn\'t do nothing if Ganglia is in allSelectedServiceNames', function () {
      var allSelectedServiceNames = ['GANGLIA'],
        configs = [
          {name: 'nimbus.childopts', value: '.jar=host=host2', recommendedValue: ''},
          {name: 'supervisor.childopts', value: '.jar=host=host2', recommendedValue: ''},
          {name: 'worker.childopts', value: '.jar=host=host2', recommendedValue: ''}
        ],
        expected = [
          {name: 'nimbus.childopts', value: '.jar=host=host1', recommendedValue: '.jar=host=host1'},
          {name: 'supervisor.childopts', value: '.jar=host=host1', recommendedValue: '.jar=host=host1'},
          {name: 'worker.childopts', value: '.jar=host=host1', recommendedValue: '.jar=host=host1'}
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
          {name: 'nimbus.childopts', value: '.jar=host=host2', recommendedValue: ''},
          {name: 'supervisor.childopts', value: '.jar=host=host2', recommendedValue: ''},
          {name: 'worker.childopts', value: '.jar=host=host2', recommendedValue: ''}
        ],
        expected = [
          {name: 'nimbus.childopts', value: '.jar=host=host1', recommendedValue: '.jar=host=host1'},
          {name: 'supervisor.childopts', value: '.jar=host=host1', recommendedValue: '.jar=host=host1'},
          {name: 'worker.childopts', value: '.jar=host=host1', recommendedValue: '.jar=host=host1'}
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
          {name: 'nimbus.childopts', value: '.jar=host=host2', recommendedValue: ''},
          {name: 'supervisor.childopts', value: '.jar=host=host2', recommendedValue: ''},
          {name: 'worker.childopts', value: '.jar=host=host2', recommendedValue: ''}
        ],
        expected = [
          {name: 'nimbus.childopts', value: '.jar=host=realhost1', recommendedValue: '.jar=host=realhost1'},
          {name: 'supervisor.childopts', value: '.jar=host=realhost1', recommendedValue: '.jar=host=realhost1'},
          {name: 'worker.childopts', value: '.jar=host=realhost1', recommendedValue: '.jar=host=realhost1'}
        ];
      installerStep7Controller.reopen({
        installedServiceNames: installedServiceNames,
        wizardController: Em.Object.create({
          name: 'addServiceController',
          masterComponentHosts: [{component: 'GANGLIA_SERVER', hostName: 'realhost1'}],
          getDBProperties: function (keys) {
            return this.getProperties(keys);
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
    [
      {serviceName: 'STORM', method: "resolveStormConfigs"},
      {serviceName: 'YARN', method: "resolveYarnConfigs"}
    ].forEach(function(t) {
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
      installerStep7Controller.loadConfigGroups(serviceConfigGroups);
      expect(installerStep7Controller.get('stepConfigs.firstObject.configGroups.length')).to.equal(1);
      var group = installerStep7Controller.get('stepConfigs.firstObject.configGroups.firstObject');
      expect(group.get('name')).to.equal('Default');
      expect(group.get('description').contains(serviceName)).to.equal(true);
      expect(group.get('isDefault')).to.equal(true);
      expect(group.get('hosts')).to.eql(['h1', 'h2', 'h3']);
      expect(group.get('service.id')).to.equal(serviceName);
      expect(group.get('serviceName')).to.equal(serviceName);
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
      installerStep7Controller.renderConfigs.restore();
    });

    it('if wizard isn\'t addService, should set output of installerStep7Controller.renderConfigs', function () {
      var serviceConfigs = Em.A([
        {serviceName:'HDFS', configs: []},
        {}
      ]);
      sinon.stub(installerStep7Controller, 'renderConfigs', function () {
        return serviceConfigs;
      });
      installerStep7Controller.set('wizardController.name', 'installerController');
      installerStep7Controller.setStepConfigs([], []);
      expect(installerStep7Controller.get('stepConfigs')).to.eql(serviceConfigs);
    });

    it('addServiceWizard used', function () {
      var serviceConfigs = Em.A([Em.Object.create({serviceName: 'HDFS', configs: []}), Em.Object.create({serviceName: 's2'})]);
      installerStep7Controller.set('wizardController.name', 'addServiceController');
      installerStep7Controller.reopen({selectedServiceNames: ['s2']});
      sinon.stub(installerStep7Controller, 'renderConfigs', function () {
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
            Em.Object.create({category: 'SECONDARY_NAMENODE'}),
            Em.Object.create({category: 'SECONDARY_NAMENODE'}),
            Em.Object.create({category: 'NameNode'}),
            Em.Object.create({category: 'NameNode'}),
            Em.Object.create({category: 'SECONDARY_NAMENODE'})
          ]
        }),
        Em.Object.create({serviceName: 's2'})]
      );
      installerStep7Controller.set('wizardController.name', 'addServiceController');
      installerStep7Controller.reopen({selectedServiceNames: ['HDFS', 's2']});
      sinon.stub(installerStep7Controller, 'renderConfigs', function () {
        return serviceConfigs;
      });
      installerStep7Controller.setStepConfigs([], []);
      expect(installerStep7Controller.get('stepConfigs').everyProperty('showConfig', true)).to.equal(true);
      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 'HDFS').get('selected')).to.equal(true);
      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 'HDFS').get('configs').length).to.equal(5);
      App.get.restore();
    });

    it('not windows stack', function () {

      var s = sinon.stub(App, 'get');
      s.withArgs('isHadoopWindowsStack').returns(false);
      s.withArgs('isHaEnabled').returns(false);

      var serviceConfigs = Em.A([
        Em.Object.create({
          serviceName: 'HDFS',
          configs: [
            {category: 'NameNode'},
            {category: 'NameNode'}
          ]
        }),
        Em.Object.create({serviceName: 's2'})]
      );

      installerStep7Controller.reopen({selectedServiceNames: ['HDFS', 's2']});
      sinon.stub(installerStep7Controller, 'renderConfigs', function () {
        return serviceConfigs;
      });
      installerStep7Controller.setStepConfigs([], []);

      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 'HDFS').get('configs').length).to.equal(2);

      s.restore();

    });

    it('windows stack', function () {

      var s = sinon.stub(App, 'get');
      s.withArgs('isHadoopWindowsStack').returns(true);
      s.withArgs('isHaEnabled').returns(false);

      var serviceConfigs = Em.A([
        Em.Object.create({
          serviceName: 'HDFS',
          configs: [
            {category: 'NameNode'},
            {category: 'NameNode'}
          ]
        }),
        Em.Object.create({serviceName: 's2'})]
      );

      installerStep7Controller.reopen({selectedServiceNames: ['HDFS', 's2']});
      installerStep7Controller.set('installedServiceNames',['HDFS', 's2', 's3']);
      sinon.stub(installerStep7Controller, 'renderConfigs', function () {
        return serviceConfigs;
      });
      installerStep7Controller.setStepConfigs([], []);

      expect(installerStep7Controller.get('stepConfigs').findProperty('serviceName', 'HDFS').get('configs').length).to.equal(2);

      s.restore();

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
          installedServiceNames: [],
          m: 'installedServiceNames is empty',
          e: {
            loadConfigGroups: true,
            loadInstalledServicesConfigGroups: false
          }
        },
        {
          installedServiceNames: ['s1', 's2', 's3'],
          areInstalledConfigGroupsLoaded: false,
          m: 'installedServiceNames isn\'t empty, config groups not yet loaded',
          e: {
            loadConfigGroups: true,
            loadInstalledServicesConfigGroups: true
          }
        },
        {
          installedServiceNames: ['s1', 's2', 's3'],
          areInstalledConfigGroupsLoaded: true,
          m: 'installedServiceNames isn\'t empty, config groups already loaded',
          e: {
            loadConfigGroups: true,
            loadInstalledServicesConfigGroups: false
          }
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          sinon.stub(App, 'get', function (k) {
            if (k === 'supports.hostOverridesInstaller') return test.hostOverridesInstaller;
            return Em.get(App, k);
          });
          installerStep7Controller.reopen({
            installedServiceNames: test.installedServiceNames,
            wizardController: {
              areInstalledConfigGroupsLoaded: test.areInstalledConfigGroupsLoaded
            }
          });
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
          },
          stackConfigsLoaded: true
        })
      });
      sinon.stub(App.config, 'mergePreDefinedWithStack', Em.K);
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
      App.config.mergePreDefinedWithStack.restore();
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
      installerStep7Controller.set('wizardController.stackConfigsLoaded', false);
      installerStep7Controller.loadStep();
      expect(installerStep7Controller.clearStep.called).to.equal(false);
    });
    it('should use App.config to map configs', function () {
      installerStep7Controller.loadStep();
      expect(App.config.mergePreDefinedWithStack.calledOnce).to.equal(true);
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
      sinon.stub(installerStep7Controller, 'resolveServiceDependencyConfigs', Em.K);
      sinon.stub(installerStep7Controller, 'loadServerSideConfigsRecommendations', function() {
        return $.Deferred().resolve();
      });
      sinon.stub(installerStep7Controller, 'checkHostOverrideInstaller', Em.K);
      sinon.stub(installerStep7Controller, 'activateSpecialConfigs', Em.K);
      sinon.stub(installerStep7Controller, 'selectProperService', Em.K);
      sinon.stub(installerStep7Controller, 'setStepConfigs', Em.K);
      sinon.stub(App.router, 'send', Em.K);
      sinon.stub(App.StackService, 'find', function () {
        return {
          findProperty: function () {
            return Em.Object.create({
              isInstalled: true,
              isSelected: false
            });
          },
          filterProperty: function () {
            return [];
          }
        }
      });
     installerStep7Controller.set('content.hosts', Em.A([{hostName: 'h1'}, {hostName: 'h2'}]));
    });
    afterEach(function () {
      installerStep7Controller.resolveServiceDependencyConfigs.restore();
      installerStep7Controller.loadServerSideConfigsRecommendations.restore();
      installerStep7Controller.checkHostOverrideInstaller.restore();
      installerStep7Controller.activateSpecialConfigs.restore();
      installerStep7Controller.selectProperService.restore();
      installerStep7Controller.setStepConfigs.restore();
      App.router.send.restore();
      App.StackService.find.restore();
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
  });

  describe('#_updateIsEditableFlagForConfig', function () {
    beforeEach(function(){
      this.mock = sinon.stub(App, 'isAccessible');
    });
    afterEach(function () {
      this.mock.restore();
    });
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
          this.mock.returns(test.isAdmin);
          installerStep7Controller.reopen({isHostsConfigsPage: test.isHostsConfigsPage});
          var serviceConfigProperty = Em.Object.create({
            isReconfigurable: test.isReconfigurable
          });
          installerStep7Controller._updateIsEditableFlagForConfig(serviceConfigProperty, test.defaultGroupSelected);
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

  describe('#setInstalledServiceConfigs', function () {

    var controller = App.WizardStep7Controller.create({
        installedServiceNames: ['HBASE', 'AMBARI_METRICS']
      }),
      configs = [
        {
          name: 'hbase.client.scanner.caching',
          value: '1000',
          serviceName: 'HBASE',
          filename: 'hbase-site.xml'
        },
        {
          name: 'hbase.client.scanner.caching',
          value: '2000',
          serviceName: 'AMBARI_METRICS',
          filename: 'ams-hbase-site.xml'
        }
      ],
      configsByTags = [
        {
          type: 'hbase-site',
          tag: 'version2',
          properties: {
            'hbase.client.scanner.caching': '1500'
          }
        },
        {
          type: 'ams-hbase-site',
          tag: 'version2',
          properties: {
            'hbase.client.scanner.caching': '2500'
          }
        },
        {
          type: 'site-without-properties',
          tag: 'version1'
        }
      ],
      installedServiceNames = ['HBASE', 'AMBARI_METRICS'];

    it('should handle properties with the same name', function () {
      configs = controller.setInstalledServiceConfigs(configs, configsByTags, installedServiceNames);
      var properties = configs.filterProperty('name', 'hbase.client.scanner.caching');
      expect(properties).to.have.length(2);
      expect(properties.findProperty('filename', 'hbase-site.xml').value).to.equal('1500');
      expect(properties.findProperty('filename', 'hbase-site.xml').savedValue).to.equal('1500');
      expect(properties.findProperty('filename', 'ams-hbase-site.xml').value).to.equal('2500');
      expect(properties.findProperty('filename', 'ams-hbase-site.xml').savedValue).to.equal('2500');
    });

  });

  describe('#getAmbariDatabaseSuccess', function () {

    var controller = App.WizardStep7Controller.create({
        stepConfigs: [
          {
            serviceName: 'HIVE',
            configs: [
              {
                name: 'hive_hostname',
                value: 'h0'
              }
            ]
          }
        ]
      }),
      cases = [
        {
          data: {
            hostComponents: []
          },
          mySQLServerConflict: false,
          title: 'no Ambari Server host components'
        },
        {
          data: {
            hostComponents: [
              {
                RootServiceHostComponents: {
                  properties: {
                    'server.jdbc.url': 'jdbc:mysql://h0/db0?createDatabaseIfNotExist=true'
                  }
                }
              }
            ]
          },
          mySQLServerConflict: true,
          title: 'Ambari MySQL Server and Hive Server are on the same host'
        },
        {
          data: {
            hostComponents: [
              {
                RootServiceHostComponents: {
                  properties: {
                    'server.jdbc.url': 'jdbc:mysql://h1/db1?createDatabaseIfNotExist=true'
                  }
                }
              }
            ]
          },
          mySQLServerConflict: false,
          title: 'Ambari MySQL Server and Hive Server are on different hosts'
        }
      ];

    cases.forEach(function (item) {
      it(item.title, function () {
        controller.getAmbariDatabaseSuccess(item.data);
        expect(controller.get('mySQLServerConflict')).to.equal(item.mySQLServerConflict);
      });
    });

  });

  describe('#showDatabaseConnectionWarningPopup', function () {

    var cases = [
        {
          method: 'onSecondary',
          submitButtonClicked: false,
          isRejected: true,
          title: 'Cancel button clicked'
        },
        {
          method: 'onPrimary',
          submitButtonClicked: true,
          isResolved: true,
          title: 'Proceed Anyway button clicked'
        }
      ],
      dfd,
      testObject,
      serviceNames = ['HIVE', 'OOZIE'],
      bodyMessage = 'HIVE, OOZIE';

    beforeEach(function () {
      installerStep7Controller.set('submitButtonClicked', true);
      dfd = $.Deferred(function (d) {
        d.done(function () {
          testObject.isResolved = true;
        });
        d.fail(function () {
          testObject.isRejected = true;
        })
      });
      testObject = {};
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        var popup = installerStep7Controller.showDatabaseConnectionWarningPopup(serviceNames, dfd);
        expect(popup.get('body')).to.equal(Em.I18n.t('installer.step7.popup.database.connection.body').format(bodyMessage));
        popup[item.method]();
        expect(testObject.isResolved).to.equal(item.isResolved);
        expect(testObject.isRejected).to.equal(item.isRejected);
        expect(installerStep7Controller.get('submitButtonClicked')).to.equal(item.submitButtonClicked);
      });
    });

  });

  describe('#issuesFilterText', function () {

    issuesFilterCases.forEach(function (item) {
      it(item.title, function () {
        issuesFilterTestSetup(installerStep7Controller, item);
        expect(installerStep7Controller.get('issuesFilterText')).to.equal(item.issuesFilterText);
      })
    });

  });

  describe('#loadServiceTagsSuccess', function () {
    it('should create ClusterSiteToTagMap', function () {
      var params = Em.Object.create({
        serviceName: "OOZIE",
        serviceConfigsDef: Em.Object.create({
          configTypes: Em.Object.create({
            site3: true,
            site2: true,
            site1: true
          })
        })
      });
      var wizardController = Em.Object.create({
          allHosts: [
            {hostName: 'h1'},
            {hostName: 'h2'},
            {hostName: 'h3'}
          ]
      });
      installerStep7Controller.set('wizardController', wizardController);
      installerStep7Controller.set('stepConfigs', Em.A([Em.Object.create({serviceName: 'OOZIE', configs: Em.A([]) })]));
      var desired_configs = {
        site1: {
          tag: "tag1"
        },
        site2: {
          tag: "tag2"
        },
        site3: {
          tag: "tag3"
        }
      };
      var data = {
        config_groups: Em.A([Em.Object.create({
          ConfigGroup: Em.Object.create({
            tag: 'OOZIE',
            hosts: Em.A([Em.Object.create({host_name: 'h1'})]),
            id: 1,
            group_name: "",
            description: "",
            desired_configs: Em.A([Em.Object.create({
              type: '1',
              tag: 'h1'
            })])
          })
        })]),
        Clusters: {
          desired_configs: desired_configs
        }
      };
      installerStep7Controller.loadServiceTagsSuccess(data, {}, params);
      var result = installerStep7Controller.get("loadedClusterSiteToTagMap");
      expect(JSON.parse(JSON.stringify(result))).to.eql(JSON.parse(JSON.stringify({"site1":"tag1","site2":"tag2","site3":"tag3"})));
    })
  });

  describe('#issuesFilterLinkText', function () {

    issuesFilterCases.forEach(function (item) {
      it(item.title, function () {
        issuesFilterTestSetup(installerStep7Controller, item);
        expect(installerStep7Controller.get('issuesFilterLinkText')).to.equal(item.issuesFilterLinkText);
      })
    });

  });

  describe('#toggleIssuesFilter', function () {
    beforeEach(function () {
      sinon.stub(installerStep7Controller, 'propertyDidChange', Em.K);
    });
    afterEach(function () {
      installerStep7Controller.propertyDidChange.restore();
    });
    it('should toggle issues filter', function () {
      var issuesFilter = installerStep7Controller.get('filterColumns').findProperty('attributeName', 'hasIssues');
      issuesFilter.set('selected', false);
      installerStep7Controller.toggleIssuesFilter();
      expect(issuesFilter.get('selected')).to.be.true;
      installerStep7Controller.toggleIssuesFilter();
      expect(issuesFilter.get('selected')).to.be.false;
    });
    it('selected service should be changed', function () {
      installerStep7Controller.setProperties({
        selectedService: {
          errorCount: 0,
          configGroups: []
        },
        stepConfigs: [
          {
            errorCount: 1,
            configGroups: []
          },
          {
            errorCount: 2,
            configGroups: []
          }
        ]
      });
      installerStep7Controller.toggleIssuesFilter();
      expect(installerStep7Controller.get('selectedService')).to.eql({
        errorCount: 1,
        configGroups: []
      });
      expect(installerStep7Controller.propertyDidChange.calledOnce).to.be.true;
      expect(installerStep7Controller.propertyDidChange.calledWith('selectedServiceNameTrigger')).to.be.true;
    });
  });

  describe('#addKerberosDescriptorConfigs', function() {
    var configs = [
      { name: 'prop1', displayName: 'Prop1' },
      { name: 'prop2', displayName: 'Prop2' },
      { name: 'prop3', displayName: 'Prop3' }
    ];
    var descriptor = [
      Em.Object.create({ name: 'prop4', filename: 'file-1'}),
      Em.Object.create({ name: 'prop1', filename: 'file-1'})
    ];
    var propertiesAttrTests = [
      {
        attr: 'isUserProperty', val: false,
        m: 'descriptor properties should not be marked as custom'
      },
      {
        attr: 'category', val: 'Advanced file-1',
        m: 'descriptor properties should be added to Advanced category'
      },
      {
        attr: 'isOverridable', val: false,
        m: 'descriptor properties should not be overriden'
      }
    ];

    propertiesAttrTests.forEach(function(test) {
      it(test.m, function() {
        installerStep7Controller.addKerberosDescriptorConfigs(configs, descriptor);
        expect(configs.findProperty('name', 'prop1')[test.attr]).to.be.eql(test.val);
      });
    });
  });

  describe('#addHawqConfigsOnNnHa', function () {
    var configs = [
      {
        filename: 'hdfs-site.xml',
        description: 'dfs.nameservices__hdfs-site',
        displayName: 'dfs.nameservices',
        displayType: 'string',
        name: 'dfs.nameservices',
        value: 'haservice',
        recommendedValue: 'haservice'
      },
      {
        filename: 'hdfs-site.xml',
        description: 'dfs.ha.namenodes.haservice__hdfs-site',
        displayName: 'dfs.ha.namenodes.haservice',
        displayType: 'string',
        name: 'dfs.ha.namenodes.haservice',
        value: 'nn1,nn2',
        recommendedValue: 'nn1,nn2'
      },
      {
        filename: 'hdfs-site.xml',
        description: 'dfs.namenode.rpc-address.haservice.nn1__hdfs-site',
        displayName: 'dfs.namenode.rpc-address.haservice.nn1',
        displayType: 'string',
        name: 'dfs.namenode.rpc-address.haservice.nn1',
        value: 'c6401.ambari.apache.org:8020',
        recommendedValue: 'c6401.ambari.apache.org:8020'
      },
      {
        filename: 'hdfs-site.xml',
        description: 'dfs.namenode.rpc-address.haservice.nn2__hdfs-site',
        displayName: 'dfs.namenode.rpc-address.haservice.nn2',
        displayType: 'string',
        name: 'dfs.namenode.rpc-address.haservice.nn2',
        value: 'c6402.ambari.apache.org:8020',
        recommendedValue: 'c6402.ambari.apache.org:8020'
      },
      {
        filename: 'hdfs-site.xml',
        description: 'dfs.namenode.http-address.haservice.nn1__hdfs-site',
        displayName: 'dfs.namenode.http-address.haservice.nn1',
        displayType: 'string',
        name: 'dfs.namenode.http-address.haservice.nn1',
        value: 'c6401.ambari.apache.org:50070',
        recommendedValue: 'c6401.ambari.apache.org:50070'
      },
      {
        filename: 'hdfs-site.xml',
        description: 'dfs.namenode.http-address.haservice.nn2__hdfs-site',
        displayName: 'dfs.namenode.http-address.haservice.nn2',
        displayType: 'string',
        name: 'dfs.namenode.http-address.haservice.nn2',
        value: 'c6402.ambari.apache.org:50070',
        recommendedValue: 'c6402.ambari.apache.org:50070'
      }
    ];

    it('should copy properties from hdfs-site to hdfs-client for HAWQ', function() {
      var oldConfigs = configs.slice();
      installerStep7Controller.addHawqConfigsOnNnHa(configs);
      // ensure 6 new configs were added
      expect(configs.length - oldConfigs.length).to.be.eql(6);
      var hdfsClientConfigs = configs.filterProperty('filename', 'hdfs-client.xml');
      oldConfigs.forEach(function(property){
        // find the same property in hdfs-client for HAWQ and see if attribute value matches with the corresponding property's attribute value in hdfs-site
        expect(hdfsClientConfigs.findProperty('name', property.name).description).to.be.eql(property.description);
        expect(hdfsClientConfigs.findProperty('name', property.name).displayName).to.be.eql(property.displayName);
        expect(hdfsClientConfigs.findProperty('name', property.name).value).to.be.eql(property.value);
        expect(hdfsClientConfigs.findProperty('name', property.name).recommendedValue).to.be.eql(property.recommendedValue);
      });
    });
  });

  describe('#addHawqConfigsOnRMHa', function () {
    var configs = [
      {
        id: 'yarn.resourcemanager.hostname.rm1',
        name: 'yarn.resourcemanager.hostname.rm1',
        value: 'c6401.ambari.apache.org',
        recommendedValue: 'c6401.ambari.apache.org',
        filename: 'yarn-site.xml'
      },
      {
        id: 'yarn.resourcemanager.hostname.rm2',
        name: 'yarn.resourcemanager.hostname.rm2',
        value: 'c6402.ambari.apache.org',
        recommendedValue: 'c6402.ambari.apache.org',
        filename: 'yarn-site.xml'
      }
    ];

    it('should update properties in yarn-client for HAWQ if yarn ha is enabled', function() {
      var inputConfigsCount = configs.length;
      installerStep7Controller.addHawqConfigsOnRMHa(configs);
      var yarnClientConfig = configs.filterProperty('filename', 'yarn-client.xml');
      var yarnRmDetails = yarnClientConfig.findProperty('name', 'yarn.resourcemanager.ha');
      var yarnRmSchedulerDetails = yarnClientConfig.findProperty('name', 'yarn.resourcemanager.scheduler.ha');

      var expectedYarnRmHaValue = 'c6401.ambari.apache.org:8032,c6402.ambari.apache.org:8032';
      expect(yarnRmDetails.value).to.be.eql(expectedYarnRmHaValue);
      expect(yarnRmDetails.recommendedValue).to.be.eql(expectedYarnRmHaValue);
      expect(yarnRmDetails.displayName).to.be.eql('yarn.resourcemanager.ha');
      expect(yarnRmDetails.description).to.be.eql('Comma separated yarn resourcemanager host addresses with port');

      var expectedYarnRmSchedulerValue = 'c6401.ambari.apache.org:8030,c6402.ambari.apache.org:8030';
      expect(yarnRmSchedulerDetails.value).to.be.eql(expectedYarnRmSchedulerValue);
      expect(yarnRmSchedulerDetails.recommendedValue).to.be.eql(expectedYarnRmSchedulerValue);
      expect(yarnRmSchedulerDetails.displayName).to.be.eql('yarn.resourcemanager.scheduler.ha');
      expect(yarnRmSchedulerDetails.description).to.be.eql('Comma separated yarn resourcemanager scheduler addresses with port');

      var noOfConfigsAdded = 2;
      expect(configs.length).to.be.eql(inputConfigsCount + noOfConfigsAdded) ;
    });
  });

  describe('#removeHawqStandbyAddressHostConfig', function() {
    var testHawqSiteConfigs = [
      {
        name: 'hawq_standby_address_host',
        value: 'h2'
      },
      {
        name: 'hawq_master_address_host',
        value: 'h1'
      }
    ];
    var oldHawqSiteLength = testHawqSiteConfigs.length;
    it('hawq_standby_address_host should be removed', function() {
      var hawqSiteConfigs = testHawqSiteConfigs.slice();
      var updatedHawqSiteConfigs = installerStep7Controller.removeHawqStandbyHostAddressConfig(hawqSiteConfigs);
      expect(updatedHawqSiteConfigs.length).to.be.eql(oldHawqSiteLength-1);
      expect(updatedHawqSiteConfigs.findProperty('name', 'hawq_standby_address_host')).to.be.eql(undefined);
      expect(updatedHawqSiteConfigs.findProperty('name', 'hawq_master_address_host').value).to.be.eql('h1');
    });

  });

  describe('#errorsCount', function () {

    it('should ignore configs with widgets (enhanced configs)', function () {

      installerStep7Controller.reopen({selectedService: {
        configs: [
          Em.Object.create({isVisible: true, widgetType: 'type', isValid: false}),
          Em.Object.create({isVisible: true, widgetType: 'type', isValid: true}),
          Em.Object.create({isVisible: true, isValid: true}),
          Em.Object.create({isVisible: true, isValid: false})
        ]
      }});

      expect(installerStep7Controller.get('errorsCount')).to.equal(1);

    });

    it('should ignore configs with widgets (enhanced configs) and hidden configs', function () {

      installerStep7Controller.reopen({selectedService: {
        configs: [
          Em.Object.create({isVisible: true, widgetType: 'type', isValid: false}),
          Em.Object.create({isVisible: true, widgetType: 'type', isValid: true}),
          Em.Object.create({isVisible: false, isValid: false}),
          Em.Object.create({isVisible: true, isValid: true}),
          Em.Object.create({isVisible: true, isValid: false})
        ]
      }});

      expect(installerStep7Controller.get('errorsCount')).to.equal(1);

    });

  });

  describe('#_reconfigureServicesOnNnHa', function () {

    var dfsNameservices = 'some_cluster';

    Em.A([
      {
        serviceName: 'HBASE',
        configToUpdate: 'hbase.rootdir',
        oldValue: 'hdfs://nameserv:8020/apps/hbase/data',
        expectedNewValue: 'hdfs://' + dfsNameservices + '/apps/hbase/data'
      },
      {
        serviceName: 'ACCUMULO',
        configToUpdate: 'instance.volumes',
        oldValue: 'hdfs://localhost:8020/apps/accumulo/data',
        expectedNewValue: 'hdfs://' + dfsNameservices + '/apps/accumulo/data'
      },
      {
        serviceName: 'HAWQ',
        configToUpdate: 'hawq_dfs_url',
        oldValue: 'localhost:8020/hawq_data',
        expectedNewValue: dfsNameservices + '/hawq_data'
      }
    ]).forEach(function (test) {
      it(test.serviceName + ' ' + test.configToUpdate, function () {
        var serviceConfigs = [App.ServiceConfig.create({
          serviceName: test.serviceName,
          configs: [
            Em.Object.create({
              name: test.configToUpdate,
              value: test.oldValue
            })
          ]
        }),
          App.ServiceConfig.create({
            serviceName: 'HDFS',
            configs: [
              Em.Object.create({
                name: 'dfs.nameservices',
                value: dfsNameservices
              })
            ]
          })];
        installerStep7Controller.reopen({
          selectedServiceNames: [test.serviceName, 'HDFS']
        });
        serviceConfigs = installerStep7Controller._reconfigureServicesOnNnHa(serviceConfigs);
        expect(serviceConfigs.findProperty('serviceName', test.serviceName).configs.findProperty('name', test.configToUpdate).get('value')).to.equal(test.expectedNewValue);
      });
    });

  });

  describe('#showOozieDerbyWarning', function() {
    var controller;
    beforeEach(function() {
      controller = App.WizardStep7Controller.create({});
      sinon.stub(App.ModalPopup, 'show', Em.K);
    });
    afterEach(function() {
      App.ModalPopup.show.restore();
    });

    Em.A([
      {
        selectedServiceNames: ['HDFS', 'OOZIE'],
        databaseType: Em.I18n.t('installer.step7.oozie.database.new'),
        e: true,
        m: 'Oozie selected with derby database, warning popup should be shown'
      },
      {
        selectedServiceNames: ['HDFS'],
        databaseType: Em.I18n.t('installer.step7.oozie.database.new'),
        e: false,
        m: 'Oozie not selected warning popup should be skipped'
      },
      {
        selectedServiceNames: ['HDFS', 'OOZIE'],
        databaseType: 'New Mysql Database',
        e: false,
        m: 'Oozie selected, mysql database used, warning popup should be sk'
      }
    ]).forEach(function(test) {
      it(test.m, function() {
        var callback = sinon.spy();
        sinon.stub(controller, 'findConfigProperty').returns(Em.Object.create({ value: test.databaseType}));
        controller.reopen({
          selectedServiceNames: test.selectedServiceNames
        });
        controller.showOozieDerbyWarningPopup(callback);
        controller.findConfigProperty.restore();
        expect(App.ModalPopup.show.calledOnce).to.equal(test.e);
      });
    });
  });

  describe('#addHostNamesToConfigs', function() {

    beforeEach(function () {
      sinon.stub(App.StackServiceComponent, 'find', function () {
        return Em.Object.create({
          id: 'NAMENODE',
          displayName: 'NameNode'
        });
      });
    });

    afterEach(function () {
      App.StackServiceComponent.find.restore();
    });

    it('should not create duplicate configs', function () {
      var serviceConfig = Em.Object.create({
        configs: [],
        serviceName: 'HDFS',
        configCategories: [
          {
            showHost: true,
            name: 'NAMENODE'
          }
        ]
      });
      var masterComponents = [
        {component: 'NAMENODE', hostName: 'h1'}
      ];
      var slaveComponents = [];
      installerStep7Controller.addHostNamesToConfigs(serviceConfig, masterComponents, slaveComponents);
      expect(serviceConfig.get('configs').filterProperty('name', 'namenode_host').length).to.equal(1);
      installerStep7Controller.addHostNamesToConfigs(serviceConfig, masterComponents, slaveComponents);
      expect(serviceConfig.get('configs').filterProperty('name', 'namenode_host').length).to.equal(1);
    });

  });

  describe('#resolveHiveMysqlDatabase', function () {

    beforeEach(function () {
      installerStep7Controller.get('content').setProperties({
        services: Em.A([
          Em.Object.create({serviceName: 'HIVE', isSelected: true, isInstalled: false})
        ])
      });
      installerStep7Controller.setProperties({
        stepConfigs: Em.A([
          Em.Object.create({serviceName: 'HIVE', configs: [{name: 'hive_database', value: 'New MySQL Database'}]})
        ]),
        mySQLServerConflict: true
      });
      sinon.stub(installerStep7Controller, 'moveNext', Em.K);
      sinon.stub(installerStep7Controller, 'checkMySQLHost', function () {
        return $.Deferred().resolve();
      });
      sinon.spy(App.ModalPopup, 'show');
    });

    afterEach(function () {
      installerStep7Controller.moveNext.restore();
      installerStep7Controller.checkMySQLHost.restore();

      App.ModalPopup.show.restore();
    });

    it('no HIVE service', function () {
      installerStep7Controller.set('content.services', Em.A([]));
      installerStep7Controller.resolveHiveMysqlDatabase();
      expect(installerStep7Controller.moveNext.calledOnce).to.be.true;
      expect(App.ModalPopup.show.called).to.be.false;
    });

    it('if mySQLServerConflict, popup is shown', function () {
      installerStep7Controller.resolveHiveMysqlDatabase();
      expect(installerStep7Controller.moveNext.called).to.be.false;
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });

  });

  describe('#mySQLWarningHandler', function () {

    beforeEach(function () {
      installerStep7Controller.set('mySQLServerConflict', true);
      sinon.spy(App.ModalPopup, 'show');
      sinon.stub(App.router, 'get').returns({gotoStep: Em.K});
      sinon.stub(App.router.get(), 'gotoStep', Em.K);
    });

    afterEach(function () {
      App.ModalPopup.show.restore();
      App.router.get().gotoStep.restore();
      App.router.get.restore();
    });

    it('warning popup is shown', function () {
      installerStep7Controller.mySQLWarningHandler();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });

    it('submitButtonClicked is set to false on primary click', function () {
      installerStep7Controller.mySQLWarningHandler().onPrimary();
      expect(installerStep7Controller.get('submitButtonClicked')).to.be.false;
    });

    it('second popup is shown on secondary click', function () {
      installerStep7Controller.mySQLWarningHandler().onSecondary();
      expect(App.ModalPopup.show.calledTwice).to.be.true;
    });

    it('submitButtonClicked is set to false on secondary click on the second popup', function () {
      installerStep7Controller.mySQLWarningHandler().onSecondary().onSecondary();
      expect(installerStep7Controller.get('submitButtonClicked')).to.be.false;
    });

    it('user is moved to step5 on primary click on the second popup (installerController)', function () {
      installerStep7Controller.set('content.controllerName', 'installerController');
      installerStep7Controller.mySQLWarningHandler().onSecondary().onPrimary();
      expect(App.router.get('installerController').gotoStep.calledWith(5, true)).to.be.true;
    });

    it('user is moved to step2 on primary click on the second popup (addSeviceController)', function () {
      installerStep7Controller.set('content.controllerName', 'addServiceController');
      installerStep7Controller.mySQLWarningHandler().onSecondary().onPrimary();
      expect(App.router.get('addSeviceController').gotoStep.calledWith(2, true)).to.be.true;
    });

  });

});
