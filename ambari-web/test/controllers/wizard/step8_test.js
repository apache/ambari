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
var modelSetup = require('test/init_model_test');
require('utils/ajax/ajax_queue');
require('controllers/main/admin/security');
require('controllers/main/service/info/configs');
require('controllers/wizard/step8_controller');
var installerStep8Controller, configurationController;

describe('App.WizardStep8Controller', function () {

  var configs = Em.A([
    Em.Object.create({filename: 'hdfs-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'hdfs-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'hue-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'hue-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'mapred-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'mapred-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'yarn-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'yarn-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'capacity-scheduler.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'capacity-scheduler.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'mapred-queue-acls.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'mapred-queue-acls.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'hbase-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'hbase-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'oozie-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'oozie-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'hive-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'hive-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'pig-properties.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'webhcat-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'webhcat-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'tez-site.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'tez-site.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'falcon-startup.properties.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'falcon-startup.properties.xml', name: 'p2', value: 'v2'}),
    Em.Object.create({filename: 'falcon-runtime.properties.xml', name: 'p1', value: 'v1'}),
    Em.Object.create({filename: 'falcon-runtime.properties.xml', name: 'p2', value: 'v2'})
  ]);

  beforeEach(function () {
    installerStep8Controller = App.WizardStep8Controller.create({
      configs: configs
    });
    configurationController = App.MainServiceInfoConfigsController.create({});
  });

  var siteObjTests = Em.A([
    {name: 'createHdfsSiteObj', e: {type: 'hdfs-site', tag: 'version1', l: 2}},
    {name: 'createHueSiteObj', e: {type: 'hue-site', tag: 'version1', l: 2}},
    {name: 'createMrSiteObj', e: {type: 'mapred-site', tag: 'version1', l: 2}},
    {name: 'createYarnSiteObj', e: {type: 'yarn-site', tag: 'version1', l: 2}},
    {name: 'createCapacityScheduler', e: {type: 'capacity-scheduler', tag: 'version1', l: 2}},
    {name: 'createMapredQueueAcls', e: {type: 'mapred-queue-acls', tag: 'version1', l: 2}},
    {name: 'createHbaseSiteObj', e: {type: 'hbase-site', tag: 'version1', l: 2}},
    {name: 'createOozieSiteObj', e: {type: 'oozie-site', tag: 'version1', l: 2}},
    {name: 'createHiveSiteObj', e: {type: 'hive-site', tag: 'version1', l: 2}},
    {name: 'createWebHCatSiteObj', e: {type: 'webhcat-site', tag: 'version1', l: 2}},
    {name: 'createTezSiteObj', e: {type: 'tez-site', tag: 'version1', l: 2}},
    {name: 'createPigPropertiesSiteObj', e: {type: 'pig-properties', tag: 'version1', l: 1}},
    {name: 'createFalconStartupSiteObj', e: {type: 'falcon-startup.properties', tag: 'version1', l: 2}},
    {name: 'createFalconRuntimeSiteObj', e: {type: 'falcon-runtime.properties', tag: 'version1', l: 2}}
  ]);

  siteObjTests.forEach(function (test) {
    describe('#' + test.name, function () {

      it(test.name, function () {

        var siteObj = installerStep8Controller.createSiteObj(test.e.type, test.e.tag);
        expect(siteObj.tag).to.equal(test.e.tag);
        expect(Em.keys(siteObj.properties).length).to.equal(test.e.l);
      });

    });
  });

  describe('#createSelectedServicesData', function () {

    var tests = Em.A([
      {selectedServices: Em.A(['MAPREDUCE2']), e: 2},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN']), e: 5},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE']), e: 7},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE']), e: 9},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE']), e: 12},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE']), e: 13},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE']), e: 14},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG']), e: 15},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG', 'FALCON']), e: 17},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG', 'FALCON', 'STORM']), e: 18},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG', 'FALCON', 'STORM', 'TEZ']), e: 19},
      {selectedServices: Em.A(['MAPREDUCE2', 'YARN', 'HBASE', 'OOZIE', 'HIVE', 'HUE', 'PIG', 'FALCON', 'STORM', 'TEZ', 'ZOOKEEPER']), e: 21}
    ]);

    tests.forEach(function (test) {
      it(test.selectedServices.join(','), function () {
        var services = test.selectedServices.map(function (serviceName) {
          return Em.Object.create({isSelected: true, isInstalled: false, serviceName: serviceName});
        });
        installerStep8Controller = App.WizardStep8Controller.create({
          content: {controllerName: 'addServiceController', services: services},
          configs: configs
        });
        var serviceData = installerStep8Controller.createSelectedServicesData();
        expect(serviceData.mapProperty('ServiceInfo.service_name')).to.eql(test.selectedServices.toArray());
        installerStep8Controller.clearStep();
      });
    });

  });

  describe('#getRegisteredHosts', function () {

    var tests = Em.A([
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1'}),
          h2: Em.Object.create({bootStatus: 'OTHER', name: 'h2'})
        },
        e: ['h1'],
        m: 'Two hosts, one registered'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'OTHER', name: 'h1'}),
          h2: Em.Object.create({bootStatus: 'OTHER', name: 'h2'})
        },
        e: [],
        m: 'Two hosts, zero registered'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1'}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2'})
        },
        e: ['h1', 'h2'],
        m: 'Two hosts, two registered'
      }
    ]);

    tests.forEach(function (test) {
      it(test.m, function () {
        installerStep8Controller.set('content', Em.Object.create({hosts: test.hosts}));
        var registeredHosts = installerStep8Controller.getRegisteredHosts();
        expect(registeredHosts.mapProperty('hostName').toArray()).to.eql(test.e);
      });
    });

  });

  describe('#createRegisterHostData', function () {

    var tests = Em.A([
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1', isInstalled: false}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: false})
        },
        e: ['h1', 'h2'],
        m: 'two registered, two isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'OTHER', name: 'h1', isInstalled: false}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: false})
        },
        e: ['h2'],
        m: 'one registered, two isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'OTHER', name: 'h1', isInstalled: true}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: false})
        },
        e: ['h2'],
        m: 'one registered, one isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1', isInstalled: true}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: false})
        },
        e: ['h2'],
        m: 'two registered, one isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'OTHER', name: 'h1', isInstalled: false}),
          h2: Em.Object.create({bootStatus: 'OTHER', name: 'h2', isInstalled: false})
        },
        e: [],
        m: 'zero registered, two isInstalled false'
      },
      {
        hosts: {
          h1: Em.Object.create({bootStatus: 'REGISTERED', name: 'h1', isInstalled: true}),
          h2: Em.Object.create({bootStatus: 'REGISTERED', name: 'h2', isInstalled: true})
        },
        e: [],
        m: 'two registered, zeto insInstalled false'
      }
    ]);

    tests.forEach(function (test) {
      it(test.m, function () {
        installerStep8Controller.set('content', Em.Object.create({hosts: test.hosts}));
        var registeredHostData = installerStep8Controller.createRegisterHostData();
        expect(registeredHostData.mapProperty('Hosts.host_name').toArray()).to.eql(test.e);
      });
    });

  });

  describe('#clusterName', function () {
    it('should be equal to content.cluster.name', function () {
      installerStep8Controller.set('content', {cluster: {name: 'new_name'}});
      expect(installerStep8Controller.get('clusterName')).to.equal('new_name');
    });
  });

  describe('#createCoreSiteObj', function () {
    it('should return config', function () {
      var content = Em.Object.create({
        services: Em.A([
          Em.Object.create({
            serviceName: 's1',
            isSelected: true,
            isInstalled: false
          }),
          Em.Object.create({
            serviceName: 's2',
            isSelected: true,
            isInstalled: false
          }),
          Em.Object.create({
            serviceName: 's3',
            isSelected: true,
            isInstalled: false
          }),
          Em.Object.create({
            serviceName: 'GLUSTERFS',
            isSelected: false,
            isInstalled: true,
            configTypesRendered: {hdfs:'tag1'}
          })
        ])
      });
      var installedServices = content.services.filterProperty('isInstalled', true);
      var selectedServices = content.services.filterProperty('isSelected', true);
      installerStep8Controller.set('content', content);
      installerStep8Controller.set('installedServices', installedServices);
      installerStep8Controller.set('selectedServices', selectedServices);
      installerStep8Controller.set('configs', Em.A([
        Em.Object.create({
          name: 'fs_glusterfs_default_name',
          filename: 'core-site.xml',
          value: 'value',
          overrides: Em.A([
            Em.Object.create({
              value: '4',
              hosts: Em.A(['h1','h2'])
            })
          ])
        }),
        Em.Object.create({
          name: 'fs.defaultFS',
          filename: 'core-site.xml',
          value: 'value',
          overrides: Em.A([
            Em.Object.create({
              value: '4',
              hosts: Em.A(['h1','h2'])
            })
          ])
        }),
        Em.Object.create({
          name: 'glusterfs_defaultFS_name',
          filename: 'core-site.xml',
          value: 'value',
          overrides: Em.A([
            Em.Object.create({
              value: '4',
              hosts: Em.A(['h1','h2'])
            })
          ])
        })
      ]));
      var expected = {
        "type": "core-site",
        "tag": "version1",
        "properties": {
          "fs_glusterfs_default_name": "value",
          "fs.defaultFS": "value",
          "glusterfs_defaultFS_name": "value"
        }
      };

      expect(installerStep8Controller.createCoreSiteObj()).to.eql(expected);
    });
  });

  describe('#createConfigurationGroups', function () {
    beforeEach(function() {
      sinon.stub(App.router,'get').returns(Em.Object.create({
        getDBProperty: function() {
          return Em.A([
            Em.Object.create({
              value: 1
            })
          ]);
        },
        getConfigAttributes: function() {
          return Em.A(['atr']);
        },
        createSiteObj: App.MainServiceInfoConfigsController.create({}).createSiteObj.bind(App.MainServiceInfoConfigsController.create({}))
      }));
    });
    afterEach(function() {
      App.router.get.restore();
    });
    it('should push group in properties', function () {
      var content = Em.Object.create({
        configGroups: Em.A([
          Em.Object.create({
            isDefault: true,
            service: Em.Object.create({
              id: 1
            }),
            name: 'n1',
            description: 'describe',
            hosts: ['h1', 'h2'],
            properties: Em.A([
              Em.Object.create({
                value: 'p1',
                filename: 'file.xml'
              }),
              Em.Object.create({
                value: 'p2',
                filename: 'file1.xml'
              })
            ])
          }),
          Em.Object.create({
            isDefault: false,
            service: Em.Object.create({
              id: 2
            }),
            name: 'n2',
            hosts: ['h3', 'h4'],
            description: 'describe1',
            properties: Em.A([
              Em.Object.create({
                value: 'p3',
                filename: 'file2.xml'
              }),
              Em.Object.create({
                value: 'p4',
                filename: 'file3.xml'
              })
            ])
          })
        ])
      });
      var defaultGroups = Em.A([
        Em.Object.create({
          group: 'n2',
          filename: 'file5.xml'
        }),
        Em.Object.create({
          group: 'n1',
          filename: 'file4.xml'
        })
      ]);
      installerStep8Controller.set('content', content);
      installerStep8Controller.set('clusterName', 'name');
      installerStep8Controller.set('customNonDefaultGroupConfigs', defaultGroups);
      installerStep8Controller.set('ajaxRequestsQueue', App.ajaxQueue.create());
      installerStep8Controller.get('ajaxRequestsQueue').clear();
      installerStep8Controller.createConfigurationGroups();
      var expected = [
        {
          "value": "p3",
          "filename": "file2.xml"
        },
        {
          "value": "p4",
          "filename": "file3.xml"
        },
        {
          "group": "n2",
          "filename": "file5.xml"
        }
      ];
      var result = JSON.parse(JSON.stringify(content.configGroups[1].properties));
      expect(result).to.eql(expected);
    });
  });

  describe('#isConfigsChanged', function () {
    it('should return true if config changed', function () {
      var properties = Em.Object.create({
        property:true,
        property1: Em.Object.create({
          hasInitialValue: false,
          isNotDefaultValue: false
        })
      });
      var configs = Em.A([Em.Object.create({
        name: 'property'
      })]);
      expect(installerStep8Controller.isConfigsChanged(properties,configs)).to.be.true;
    });
  });

  describe('#loadServices', function () {
    it('should load services', function () {
      var services = Em.A([
        Em.Object.create({
          serviceName: 's1',
          isSelected: true,
          displayNameOnSelectServicePage: 's01',
          isClientOnlyService: false,
          serviceComponents: Em.A([
            Em.Object.create({
              isClient: true
            })
          ]),
          isHiddenOnSelectServicePage: false
        }),
        Em.Object.create({
          serviceName: 's2',
          isSelected: true,
          displayNameOnSelectServicePage: 's02',
          serviceComponents: Em.A([
            Em.Object.create({
              isMaster: true
            })
          ]),
          isHiddenOnSelectServicePage: false
        }),
        Em.Object.create({
          serviceName: 's3',
          isSelected: true,
          displayNameOnSelectServicePage: 's03',
          serviceComponents: Em.A([
            Em.Object.create({
              isHAComponentOnly: true
            })
          ]),
          isHiddenOnSelectServicePage: false
        }),
        Em.Object.create({
          serviceName: 's4',
          isSelected: true,
          displayNameOnSelectServicePage: 's03',
          isClientOnlyService: true,
          serviceComponents: Em.A([
            Em.Object.create({
              isClient: true
            })
          ]),
          isHiddenOnSelectServicePage: false
        })
      ]);
      var selectedServices = services.filterProperty('isSelected');
      var slaveComponentHosts = Em.A([
        Em.Object.create({
          componentName: 'CLIENT',
          hostName: 'h1',
          hosts: Em.A([
            Em.Object.create({hostName: 'h1', isInstalled: true}),
            Em.Object.create({hostName: 'h2', isInstalled: false})
          ])
        })
      ]);
      var content = Em.Object.create({
        services: services,
        selectedServices: selectedServices,
        slaveComponentHosts: slaveComponentHosts,
        hosts: Em.A([
          Em.Object.create({hostName: 'h1', isInstalled: true}),
          Em.Object.create({hostName: 'h2', isInstalled: false})
        ])
      });
      installerStep8Controller.set('content', content);
      installerStep8Controller.set('services', Em.A([]));
      installerStep8Controller.reopen({selectedServices: selectedServices});
      installerStep8Controller.loadServices();
      var expected = [
        {
          "service_name": "s1",
          "display_name": "s01",
          "service_components": []
        },
        {
          "service_name": "s2",
          "display_name": "s02",
          "service_components": []
        },
        {
          "service_name": "s3",
          "display_name": "s03",
          "service_components": []
        },
        {
          "service_name": "s4",
          "display_name": "s03",
          "service_components": [
            {
              "component_name": "CLIENT",
              "display_name": "Clients",
              "component_value": "2 hosts"
            }
          ]
        }
      ];
      var result = JSON.parse(JSON.stringify(installerStep8Controller.get('services')));
      expect(result).to.be.eql(expected);
    });
  });

  describe('#removeClientsFromList', function () {
    it('should remove h1', function () {
      installerStep8Controller.set('content', Em.Object.create({
        hosts: Em.Object.create({
          h1: Em.Object.create({
            hostName: 'h1',
            isInstalled: true,
            hostComponents: Em.A([Em.Object.create({HostRoles: Em.Object.create({component_name: "h1"})})])
          }),
          h2: Em.Object.create({
            hostName: 'h2',
            isInstalled: true,
            hostComponents: Em.A([Em.Object.create({HostRoles: Em.Object.create({component_name: "h2"})})])
          })
        })
      }));
      var hostList = Em.A(['h1','h2']);
      installerStep8Controller.removeClientsFromList('h1', hostList);
      expect(JSON.parse(JSON.stringify(hostList))).to.eql(["h2"]);
    });
  });

  describe('#createSlaveAndClientsHostComponents', function () {
    it('should return non install object', function () {
      installerStep8Controller.set('content', Em.Object.create({
        masterComponentHosts: Em.A([
          Em.Object.create({
            componentName: 'CLIENT',
            component: 'HBASE_MASTER',
            hostName: 'h1'
          })
        ]),
        slaveComponentHosts: Em.A([
          Em.Object.create({
            componentName: 'CLIENT',
            hostName: 'h1',
            hosts: Em.A([
              Em.Object.create({hostName: 'h1', isInstalled: true}),
              Em.Object.create({hostName: 'h2', isInstalled: false})
            ])
          }),
          Em.Object.create({
            componentName: 'CLIENT1',
            hostName: 'h1',
            hosts: Em.A([
              Em.Object.create({hostName: 'h1', isInstalled: true}),
              Em.Object.create({hostName: 'h2', isInstalled: false})
            ])

          })
        ]),
        clients: Em.A([
          Em.Object.create({
            isInstalled: false
          })
        ]),
        services: Em.A([
          Em.Object.create({
            isInstalled: true,
            serviceName: "name",
            isClient: true
          })
        ]),
        hosts: Em.Object.create({
          h1: Em.Object.create({
            hostName: 'h1',
            isInstalled: true,
            hostComponents: Em.A([Em.Object.create({})])
          }),
          h2: Em.Object.create({
            hostName: 'h2',
            isInstalled: false,
            hostComponents: Em.A([Em.Object.create({})])
          })
        }),
        additionalClients: Em.A([{hostNames: "name", componentName: "client"}])
      }));
      installerStep8Controller.set('ajaxRequestsQueue', App.ajaxQueue.create());
      installerStep8Controller.get('ajaxRequestsQueue').clear();
      installerStep8Controller.createSlaveAndClientsHostComponents();
      expect(installerStep8Controller.get('content.clients')[0].isInstalled).to.be.false;
    });
  });

  describe('#createAdditionalClientComponents', function () {
    it('should bes equal to content.cluster.name', function () {
      installerStep8Controller.set('content', Em.Object.create({
        masterComponentHosts: Em.A([
          Em.Object.create({
            componentName: 'CLIENT',
            component: 'HBASE_MASTER',
            hostName: 'h1'
          })
        ]),
        slaveComponentHosts: Em.A([
          Em.Object.create({
            componentName: 'CLIENT',
            hostName: 'h1',
            hosts: Em.A([
              Em.Object.create({hostName: 'h1', isInstalled: true}),
              Em.Object.create({hostName: 'h2', isInstalled: false})
            ])
          })
        ]),
        clients: Em.A([
          Em.Object.create({
            isInstalled: false
          })
        ]),
        services: Em.A([
          Em.Object.create({
            isInstalled: true,
            serviceName: "name",
            isClient: true
          })
        ]),
        hosts: Em.Object.create({
          h1: Em.Object.create({
            hostName: 'h1',
            isInstalled: true,
            hostComponents: Em.A([Em.Object.create({})])
          }),
          h2: Em.Object.create({
            hostName: 'h2',
            isInstalled: false,
            hostComponents: Em.A([Em.Object.create({})])
          })
        }),
        additionalClients: Em.A([{hostNames: "name", componentName: "client"}])
      }));
      installerStep8Controller.set('ajaxRequestsQueue', App.ajaxQueue.create());
      installerStep8Controller.get('ajaxRequestsQueue').clear();
      installerStep8Controller.createAdditionalClientComponents();
      var result = [
        {
          "hostNames": "name",
          "componentName": "client"
        }
      ];
      var expected = installerStep8Controller.get('content.additionalClients');
      expect(JSON.parse(JSON.stringify(expected))).to.eql(result);
    });
  });

  describe('#assignComponentHosts', function () {
    it('should return host name', function () {
      var component = Em.Object.create({
        isMaster: true,
        componentName: 'HBASE_MASTER',
        hostName: 'h1'
      });
      installerStep8Controller.set('content', Em.Object.create({
        masterComponentHosts:Em.A([
          Em.Object.create({component: 'HBASE_MASTER', hostName: 'h1'})
      ])}));
      var res = installerStep8Controller.assignComponentHosts(component);
      expect(res).to.equal("h1");
    });
    it('should return number of hosts', function () {
      var component = Em.Object.create({
        componentName: 'HBASE_MASTER',
        isClient: false,
        hostName: 'h1'
      });
      installerStep8Controller.set('content', Em.Object.create({
        slaveComponentHosts:Em.A([
          Em.Object.create({
            componentName: 'HBASE_MASTER',
            hostName: 'h1',
            hosts: [
              {hostName: 'h1'},
              {hostName: 'h2'}
            ]
          })
      ])}));
      var res = installerStep8Controller.assignComponentHosts(component);
      expect(res).to.equal("2 hosts");
    });
  });

  describe('#loadClusterInfo', function () {
    beforeEach(function () {
      sinon.stub(App.Stack, 'find', function(){
        return Em.A([
          Em.Object.create({isSelected: false, hostName: 'h1'}),
          Em.Object.create({
            isSelected: true,
            hostName: 'h2',
            operatingSystems: Em.A([Em.Object.create({
              name:'windows',
              isSelected: true,
              repositories: Em.A([Em.Object.create({
                baseUrl: "url",
                osType: "2",
                repoId: "3"
              })])
            })])
          }),
          Em.Object.create({isSelected: false, hostName: 'h3'})
        ]);
      });
    });
    afterEach(function () {
      App.Stack.find.restore();
    });
    it('should return config with display_name', function () {
      installerStep8Controller.set('clusterInfo', Em.A([]));
      installerStep8Controller.loadClusterInfo();
      var res = [{
        "config_name":"cluster",
        "display_name":"Cluster Name"
      },{
        "config_name":"hosts",
        "display_name":"Total Hosts",
        "config_value":"0 (0 new)"
      }];
      var calcRes = JSON.parse(JSON.stringify(installerStep8Controller.get('clusterInfo')));
      expect(calcRes).to.eql(res);
    });
  });

  describe('#loadStep', function () {
    beforeEach(function () {
      sinon.stub(installerStep8Controller, 'clearStep', Em.K);
      sinon.stub(installerStep8Controller, 'formatProperties', Em.K);
      sinon.stub(installerStep8Controller, 'loadConfigs', Em.K);
      sinon.stub(installerStep8Controller, 'loadClusterInfo', Em.K);
      sinon.stub(installerStep8Controller, 'loadServices', Em.K);
      installerStep8Controller.set('content', {controllerName: 'installerController'});
    });
    afterEach(function () {
      installerStep8Controller.clearStep.restore();
      installerStep8Controller.formatProperties.restore();
      installerStep8Controller.loadConfigs.restore();
      installerStep8Controller.loadClusterInfo.restore();
      installerStep8Controller.loadServices.restore();
    });
    it('should call clearStep', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.clearStep.calledOnce).to.equal(true);
    });
    it('should call loadClusterInfo', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.loadClusterInfo.calledOnce).to.equal(true);
    });
    it('should call loadServices', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.loadServices.calledOnce).to.equal(true);
    });
    it('should call formatProperties if content.serviceConfigProperties is true', function () {
      installerStep8Controller.set('content.serviceConfigProperties', true);
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.loadServices.calledOnce).to.equal(true);
    });
    it('should call loadConfigs if content.serviceConfigProperties is true', function () {
      installerStep8Controller.set('content.serviceConfigProperties', true);
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.loadConfigs.calledOnce).to.equal(true);
    });
    it('should set isSubmitDisabled to false', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.get('isSubmitDisabled')).to.equal(false);
    });
    it('should set isBackBtnDisabled to false', function () {
      installerStep8Controller.loadStep();
      expect(installerStep8Controller.get('isBackBtnDisabled')).to.equal(false);
    });
  });

  describe('#getRegisteredHosts', function() {
    Em.A([
        {
          hosts: {},
          m: 'no content.hosts',
          e: []
        },
        {
          hosts: {
            h1:{bootStatus: ''},
            h2:{bootStatus: ''}
          },
          m: 'no registered hosts',
          e: []
        },
        {
          hosts: {
            h1:{bootStatus: 'REGISTERED', hostName: '', name: 'n1'},
            h2:{bootStatus: 'REGISTERED', hostName: '', name: 'n2'}
          },
          m: 'registered hosts available',
          e: ['n1', 'n2']
        }
      ]).forEach(function(test) {
        it(test.m, function() {
          installerStep8Controller.set('content', {hosts: test.hosts});
          var hosts = installerStep8Controller.getRegisteredHosts();
          expect(hosts.mapProperty('hostName')).to.eql(test.e);
        });
      });
  });

  describe('#loadRepoInfo', function() {

    beforeEach(function () {
      sinon.stub(App, 'get').withArgs('currentStackName').returns('HDP');
      sinon.stub(App.ajax, 'send', Em.K);
      sinon.stub(App.StackVersion, 'find', function() {
        return [
          Em.Object.create({state: 'CURRENT', repositoryVersion: {repositoryVersion: '2.3.0.0-2208'}})
        ];
      });
    });

    afterEach(function () {
      App.ajax.send.restore();
      App.get.restore();
      App.StackVersion.find.restore();
    });
    it('should use current StackVersion', function() {
      installerStep8Controller.loadRepoInfo();
      var data = App.ajax.send.args[0][0].data;
      expect(data).to.eql({stackName: 'HDP', repositoryVersion: '2.3.0.0-2208'});
    });
  });

  describe('#loadRepoInfoSuccessCallback', function () {
    beforeEach(function () {
      installerStep8Controller.set('clusterInfo', Em.Object.create({}));
    });

    it('should assert error if no data returned from server', function () {
      expect(function () {
        installerStep8Controller.loadRepoInfoSuccessCallback({items: []});
      }).to.throw(Error);
    });

    Em.A([
      {
        m: 'Normal JSON',
        e: {
          base_url: ['baseurl1', 'baseurl2'],
          os_type: ['redhat6', 'suse11'],
          repo_id: ['HDP-2.3', 'HDP-UTILS-1.1.0.20']
        },
        items: [
          {
            repository_versions: [
              {
                operating_systems: [
                  {
                    repositories: [
                      {
                        Repositories: {
                          base_url: 'baseurl1',
                          os_type: 'redhat6',
                          repo_id: 'HDP-2.3'
                        }
                      }
                    ]
                  },
                  {
                    repositories: [
                      {
                        Repositories: {
                          base_url: 'baseurl2',
                          os_type: 'suse11',
                          repo_id: 'HDP-UTILS-1.1.0.20'
                        }
                      }
                    ]
                  }
                ]
              }
            ]
          }
        ]
      }
    ]).forEach(function (test) {

      it(test.m, function () {
        installerStep8Controller.loadRepoInfoSuccessCallback({items: test.items});
        expect(installerStep8Controller.get('clusterInfo.repoInfo').mapProperty('base_url')).to.eql(test.e.base_url);
        expect(installerStep8Controller.get('clusterInfo.repoInfo').mapProperty('os_type')).to.eql(test.e.os_type);
        expect(installerStep8Controller.get('clusterInfo.repoInfo').mapProperty('repo_id')).to.eql(test.e.repo_id);
      });

    });

    /*Em.A([
        {
          items: [
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat5',
                    base_url: 'url1'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            }
          ],
          m: 'only redhat5',
          e: {
            base_url: ['url1'],
            os_type: ['redhat5']
          }
        },
        {
          items: [
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat5',
                    base_url: 'url1'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            },
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat6',
                    base_url: 'url2'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            }
          ],
          m: 'redhat5, redhat6',
          e: {
            base_url: ['url1', 'url2'],
            os_type: ['redhat5', 'redhat6']
          }
        },
        {
          items: [
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat5',
                    base_url: 'url1'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            },
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'redhat6',
                    base_url: 'url2'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            },
            {
              repositories: [
                {
                  Repositories: {
                    os_type: 'sles11',
                    base_url: 'url3'
                  }
                }
              ],
              OperatingSystems: {
                is_type: ''
              }
            }
          ],
          m: 'redhat5, redhat6, sles11',
          e: {
            base_url: ['url1', 'url2', 'url3'],
            os_type: ['redhat5', 'redhat6', 'sles11']
          }
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          installerStep8Controller.loadRepoInfoSuccessCallback({items: test.items});
          expect(installerStep8Controller.get('clusterInfo.repoInfo').mapProperty('base_url')).to.eql(test.e.base_url);
          expect(installerStep8Controller.get('clusterInfo.repoInfo').mapProperty('os_type')).to.eql(test.e.os_type);
        });
      });*/
  });

  describe('#loadRepoInfoErrorCallback', function() {
    it('should set [] to repoInfo', function() {
      installerStep8Controller.set('clusterInfo', Em.Object.create({repoInfo: [{}, {}]}));
      installerStep8Controller.loadRepoInfoErrorCallback({});
      expect(installerStep8Controller.get('clusterInfo.repoInfo.length')).to.eql(0);
    });
  });

  describe('#loadHbaseMasterValue', function () {
    Em.A([
        {
          masterComponentHosts: [{component: 'HBASE_MASTER', hostName: 'h1'}],
          component: Em.Object.create({component_name: 'HBASE_MASTER'}),
          m: 'one host',
          e: 'h1'
        },
        {
          masterComponentHosts: [{component: 'HBASE_MASTER', hostName: 'h1'}, {component: 'HBASE_MASTER', hostName: 'h2'}, {component: 'HBASE_MASTER', hostName: 'h3'}],
          component: Em.Object.create({component_name: 'HBASE_MASTER'}),
          m: 'many hosts',
          e: 'h1 ' + Em.I18n.t('installer.step8.other').format(2)
        }
      ]).forEach(function (test) {
        it(test.m, function() {
          installerStep8Controller.set('content', {masterComponentHosts: test.masterComponentHosts});
          installerStep8Controller.loadHbaseMasterValue(test.component);
          expect(test.component.component_value).to.equal(test.e);
        });
      });
  });

  describe('#loadZkServerValue', function() {
    Em.A([
        {
          masterComponentHosts: [{component: 'ZOOKEEPER_SERVER'}],
          component: Em.Object.create({component_name: 'ZOOKEEPER_SERVER'}),
          m: '1 host',
          e: '1 host'
        },
        {
          masterComponentHosts: [{component: 'ZOOKEEPER_SERVER'},{component: 'ZOOKEEPER_SERVER'},{component: 'ZOOKEEPER_SERVER'}],
          component: Em.Object.create({component_name: 'ZOOKEEPER_SERVER'}),
          m: 'many hosts',
          e: '3 hosts'
        }
      ]).forEach(function (test) {
        it(test.m, function () {
          installerStep8Controller.set('content', {masterComponentHosts: test.masterComponentHosts});
          installerStep8Controller.loadZkServerValue(test.component);
          expect(test.component.component_value).to.equal(test.e);
        });
      });
  });

  describe('#submit', function() {
    beforeEach(function() {
      sinon.stub(installerStep8Controller, 'submitProceed', Em.K);
      sinon.stub(installerStep8Controller, 'showRestartWarnings').returns($.Deferred().resolve().promise());
      sinon.stub(App.get('router.mainAdminKerberosController'), 'getKDCSessionState', Em.K);
    });
    afterEach(function() {
      installerStep8Controller.submitProceed.restore();
      installerStep8Controller.showRestartWarnings.restore();
      App.set('isKerberosEnabled', false);
      App.get('router.mainAdminKerberosController').getKDCSessionState.restore();
    });
    it('AddServiceController Kerberos enabled', function () {
      installerStep8Controller.reopen({
        isSubmitDisabled: false,
        content: {controllerName: 'addServiceController'}
      });
      installerStep8Controller.submit();
      expect(App.get('router.mainAdminKerberosController').getKDCSessionState.called).to.equal(true);
    });
    it('shouldn\'t do nothing if isSubmitDisabled is true', function() {
      installerStep8Controller.reopen({isSubmitDisabled: true});
      installerStep8Controller.submit();
      expect(App.get('router.mainAdminKerberosController').getKDCSessionState.called).to.equal(false);
      expect(installerStep8Controller.submitProceed.called).to.equal(false);
    });
  });

  describe('#getExistingClusterNamesSuccessCallBack', function() {
    it('should set clusterNames received from server', function() {
      var data = {
          items:[
            {Clusters: {cluster_name: 'c1'}},
            {Clusters: {cluster_name: 'c2'}},
            {Clusters: {cluster_name: 'c3'}}
          ]
        },
        clasterNames = ['c1','c2','c3'];
      installerStep8Controller.getExistingClusterNamesSuccessCallBack(data);
      expect(installerStep8Controller.get('clusterNames')).to.eql(clasterNames);
    });
  });

  describe('#getExistingClusterNamesErrorCallback', function() {
    it('should set [] to clusterNames', function() {
      installerStep8Controller.set('clusterNames', ['c1', 'c2']);
      installerStep8Controller.getExistingClusterNamesErrorCallback();
      expect(installerStep8Controller.get('clusterNames')).to.eql([]);
    });
  });

  describe('#deleteClusters', function() {

    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });

    afterEach(function () {
      App.ajax.send.restore();
    });

    it('should call App.ajax.send for each provided clusterName', function() {
      var clusterNames = ['h1', 'h2', 'h3'];
      installerStep8Controller.deleteClusters(clusterNames);
      expect(App.ajax.send.callCount).to.equal(clusterNames.length);
      clusterNames.forEach(function(n, i) {
        expect(App.ajax.send.getCall(i).args[0].data).to.eql({name: n, isLast: i == clusterNames.length - 1});
      });
    });

    it('should clear cluster delete error popup body views', function () {
      installerStep8Controller.deleteClusters([]);
      expect(installerStep8Controller.get('clusterDeleteErrorViews')).to.eql([]);
    });

  });

  describe('#createSelectedServicesData', function() {
    it('should reformat provided data', function() {
      var selectedServices = [
        Em.Object.create({serviceName: 's1'}),
        Em.Object.create({serviceName: 's2'}),
        Em.Object.create({serviceName: 's3'})
      ];
      var expected = [
        {"ServiceInfo": { "service_name": 's1' }},
        {"ServiceInfo": { "service_name": 's2' }},
        {"ServiceInfo": { "service_name": 's3' }}
      ];
      installerStep8Controller.reopen({selectedServices: selectedServices});
      var createdData = installerStep8Controller.createSelectedServicesData();
      expect(createdData).to.eql(expected);
    });
  });

  describe('#createRegisterHostData', function() {
    it('should return empty data if no hosts', function() {
      sinon.stub(installerStep8Controller, 'getRegisteredHosts', function() {return [];});
      expect(installerStep8Controller.createRegisterHostData()).to.eql([]);
      installerStep8Controller.getRegisteredHosts.restore();
    });
    it('should return computed data', function() {
      var data = [
        {isInstalled: false, hostName: 'h1'},
        {isInstalled: true, hostName: 'h2'},
        {isInstalled: false, hostName: 'h3'}
      ];
      var expected = [
        {"Hosts": { "host_name": 'h1'}},
        {"Hosts": { "host_name": 'h3'}}
      ];
      sinon.stub(installerStep8Controller, 'getRegisteredHosts', function() {return data;});
      expect(installerStep8Controller.createRegisterHostData()).to.eql(expected);
      installerStep8Controller.getRegisteredHosts.restore();
    });
  });

  describe('#createStormSiteObj', function() {
    it('should replace quote \'"\' to "\'" for some properties', function() {
      var configs = [
          {filename: 'storm-site.xml', value: ["a", "b"], name: 'storm.zookeeper.servers'}
        ],
        expected = {
          type: 'storm-site',
          tag: 'version1',
          properties: {
            'storm.zookeeper.servers': '[\'a\',\'b\']'
          }
        };
      installerStep8Controller.reopen({configs: configs});
      expect(installerStep8Controller.createStormSiteObj('version1')).to.eql(expected);
    });

    it('should not escape special characters', function() {
      var configs = [
          {filename: 'storm-site.xml', value: "abc\n\t", name: 'nimbus.childopts'},
          {filename: 'storm-site.xml', value: "a\nb", name: 'supervisor.childopts'},
          {filename: 'storm-site.xml', value: "a\t\tb", name: 'worker.childopts'}
        ],
        expected = {
          type: 'storm-site',
          tag: 'version1',
          properties: {
            'nimbus.childopts': 'abc\n\t',
            'supervisor.childopts': 'a\nb',
            'worker.childopts': 'a\t\tb'
          }
        };
      installerStep8Controller.reopen({configs: configs});
      expect(installerStep8Controller.createStormSiteObj('version1')).to.eql(expected);
    });
  });

  describe('#ajaxQueueFinished', function() {
    it('should call App.router.next', function() {
      sinon.stub(App.router, 'send', Em.K);
      installerStep8Controller.ajaxQueueFinished();
      expect(App.router.send.calledWith('next')).to.equal(true);
      App.router.send.restore();
    });
  });

  describe('#addRequestToAjaxQueue', function() {
    describe('testMode = true', function() {
      before(function() {
        App.set('testMode', true);
      });
      after(function() {
        App.set('testMode', false);
      });
      it('shouldn\'t do nothing', function() {
        installerStep8Controller.set('ajaxRequestsQueue', App.ajaxQueue.create());
        installerStep8Controller.get('ajaxRequestsQueue').clear();
        installerStep8Controller.addRequestToAjaxQueue({});
        expect(installerStep8Controller.get('ajaxRequestsQueue.queue.length')).to.equal(0);
      });
    });
    describe('testMode = true', function() {
      before(function() {
        App.set('testMode', false);
      });
      it('should add request', function() {
        var clusterName = 'c1';
        installerStep8Controller.reopen({clusterName: clusterName});
        installerStep8Controller.set('ajaxRequestsQueue', App.ajaxQueue.create());
        installerStep8Controller.get('ajaxRequestsQueue').clear();
        installerStep8Controller.addRequestToAjaxQueue({name:'name', data:{}});
        var request = installerStep8Controller.get('ajaxRequestsQueue.queue.firstObject');
        expect(request.error).to.equal('ajaxQueueRequestErrorCallback');
        expect(request.data.cluster).to.equal(clusterName);
      });
    });
  });

  describe('#ajaxQueueRequestErrorCallback', function() {
    var obj = Em.Object.create({
      registerErrPopup: Em.K,
      setStepsEnable: Em.K
    });
    beforeEach(function() {
      sinon.stub(App.router, 'get', function() {
        return obj;
      });
      sinon.spy(obj, 'registerErrPopup');
      sinon.spy(obj, 'setStepsEnable');
    });
    afterEach(function() {
      App.router.get.restore();
      obj.registerErrPopup.restore();
      obj.setStepsEnable.restore();
    });
    it('should set hasErrorOccurred true', function () {
      installerStep8Controller.set('hasErrorOccurred', false);
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(installerStep8Controller.get('hasErrorOccurred')).to.equal(true);
    });
    it('should set isSubmitDisabled false', function () {
      installerStep8Controller.set('isSubmitDisabled', true);
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(installerStep8Controller.get('isSubmitDisabled')).to.equal(false);
    });
    it('should set isBackBtnDisabled false', function () {
      installerStep8Controller.set('isBackBtnDisabled', true);
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(installerStep8Controller.get('isBackBtnDisabled')).to.equal(false);
    });
    it('should call setStepsEnable', function () {
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(obj.setStepsEnable.calledOnce).to.equal(true);
    });
    it('should call registerErrPopup', function () {
      installerStep8Controller.ajaxQueueRequestErrorCallback({responseText: '{"message": ""}'});
      expect(obj.registerErrPopup.calledOnce).to.equal(true);
    });
  });

  describe('#removeInstalledServicesConfigurationGroups', function() {
    beforeEach(function() {
      sinon.stub(installerStep8Controller, 'deleteConfigurationGroup', Em.K);
    });
    afterEach(function() {
      installerStep8Controller.deleteConfigurationGroup.restore();
    });
    it('should call App.config.deleteConfigGroup for each received group', function() {
      var groups = [{}, {}, {}];
      installerStep8Controller.removeInstalledServicesConfigurationGroups(groups);
      expect(installerStep8Controller.deleteConfigurationGroup.callCount).to.equal(groups.length);
    });
  });

  describe('#applyInstalledServicesConfigurationGroup', function() {
    beforeEach(function() {
      sinon.stub($, 'ajax', function () {
        return {
          retry: function () {
            return {then: Em.K}
          }
        }
      });
      sinon.stub(App.router, 'get', function() {
        return configurationController;
      });
    });
    afterEach(function() {
      $.ajax.restore();
      App.router.get.restore();
    });
    it('should do ajax request for each config group', function() {
      var configGroups = [{ConfigGroup: {id:''}}, {ConfigGroup: {id:''}}];
      installerStep8Controller.applyInstalledServicesConfigurationGroup(configGroups);
      expect($.ajax.callCount).to.equal(configGroups.length);
    });
  });

  describe('#getExistingClusterNames', function() {
    beforeEach(function() {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    afterEach(function() {
      App.ajax.send.restore();
    });
    it('should do ajax request', function() {
      installerStep8Controller.getExistingClusterNames();
      expect(App.ajax.send.calledOnce).to.be.true;
    });
  });

  describe('Queued requests', function() {

    beforeEach(function() {
      installerStep8Controller.clearStep();
      sinon.spy(installerStep8Controller, 'addRequestToAjaxQueue');
    });

    afterEach(function() {
      installerStep8Controller.addRequestToAjaxQueue.restore();
    });

    describe('#createCluster', function() {

      it('shouldn\'t add request to queue if not installerController used', function() {
        installerStep8Controller.reopen({content: {controllerName: 'addServiceController'}});
        installerStep8Controller.createCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.called).to.equal(false);
      });

      it('App.currentStackVersion should be changed if localRepo selected', function() {
        App.set('currentStackVersion', 'HDP-1.1.1');
        installerStep8Controller.reopen({content: {controllerName: 'installerController', installOptions: {localRepo: true}}});
        var data = {
          data: JSON.stringify({ "Clusters": {"version": 'HDPLocal-1.1.1' }})
        };
        installerStep8Controller.createCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data).to.equal(data.data);
      });

      it('App.currentStackVersion shouldn\'t be changed if localRepo ins\'t selected', function() {
        App.set('currentStackVersion', 'HDP-1.1.1');
        installerStep8Controller.reopen({content: {controllerName: 'installerController', installOptions: {localRepo: false}}});
        var data = {
          data: JSON.stringify({ "Clusters": {"version": 'HDP-1.1.1' }})
        };
        installerStep8Controller.createCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data).to.eql(data.data);
      });

    });

    describe('#createSelectedServices', function() {

      it('shouldn\'t do nothing if no data', function() {
        sinon.stub(installerStep8Controller, 'createSelectedServicesData', function() {return [];});
        installerStep8Controller.createSelectedServices();
        expect(installerStep8Controller.addRequestToAjaxQueue.called).to.equal(false);
        installerStep8Controller.createSelectedServicesData.restore();
      });

      it('should call addRequestToAjaxQueue with computed data', function() {
        var data = [
          {"ServiceInfo": { "service_name": 's1' }},
          {"ServiceInfo": { "service_name": 's2' }},
          {"ServiceInfo": { "service_name": 's3' }}
        ];
        sinon.stub(installerStep8Controller, 'createSelectedServicesData', function() {return data;});
        installerStep8Controller.createSelectedServices();
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data).to.equal(JSON.stringify(data));
        installerStep8Controller.createSelectedServicesData.restore();
      });

    });

    describe('#registerHostsToCluster', function() {
      it('shouldn\'t do nothing if no data', function() {
        sinon.stub(installerStep8Controller, 'createRegisterHostData', function() {return [];});
        installerStep8Controller.registerHostsToCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.called).to.equal(false);
        installerStep8Controller.createRegisterHostData.restore();
      });
      it('should call addRequestToAjaxQueue with computed data', function() {
        var data = [
          {"Hosts": { "host_name": 'h1'}},
          {"Hosts": { "host_name": 'h3'}}
        ];
        sinon.stub(installerStep8Controller, 'createRegisterHostData', function() {return data;});
        installerStep8Controller.registerHostsToCluster();
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data).to.equal(JSON.stringify(data));
        installerStep8Controller.createRegisterHostData.restore();
      });
    });

    describe('#registerHostsToComponent', function() {

      it('shouldn\'t do request if no hosts provided', function() {
        installerStep8Controller.registerHostsToComponent([]);
        expect(installerStep8Controller.addRequestToAjaxQueue.called).to.equal(false);
      });

      it('should do request if hostNames are provided', function() {
        var hostNames = ['h1', 'h2'],
          componentName = 'c1';
        installerStep8Controller.registerHostsToComponent(hostNames, componentName);
        var data = JSON.parse(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data);
        expect(data.RequestInfo.query).to.equal('Hosts/host_name=h1|Hosts/host_name=h2');
        expect(data.Body.host_components[0].HostRoles.component_name).to.equal('c1');
      });

    });

    describe('#applyConfigurationsToCluster', function() {
      it('should call addRequestToAjaxQueue', function() {
        var serviceConfigTags = [
            {
              type: 'hdfs',
              tag: 'tag1',
              properties: [
                {},
                {}
              ]
            }
          ],
          data = '['+JSON.stringify({
            Clusters: {
              desired_config: [serviceConfigTags[0]]
            }
          })+']';
        installerStep8Controller.reopen({
          installedServices: [
              Em.Object.create({
                isSelected: true,
                isInstalled: false,
                configTypesRendered: {hdfs:'tag1'}
              })
            ], selectedServices: []
        });
        installerStep8Controller.applyConfigurationsToCluster(serviceConfigTags);
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data).to.equal(data);
      });
    });

    describe('#applyConfigurationGroups', function() {
      it('should call addRequestToAjaxQueue', function() {
        var data = [{}, {}];
        installerStep8Controller.applyConfigurationGroups(data);
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data).to.equal(JSON.stringify(data));
      });
    });

    describe('#newServiceComponentErrorCallback', function() {

      it('should add request for new component', function() {
        var serviceName = 's1',
          componentName = 'c1';
        installerStep8Controller.newServiceComponentErrorCallback({}, {}, '', {}, {serviceName: serviceName, componentName: componentName});
        var data = JSON.parse(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.data);
        expect(installerStep8Controller.addRequestToAjaxQueue.args[0][0].data.serviceName).to.equal(serviceName);
        expect(data.components[0].ServiceComponentInfo.component_name).to.equal(componentName);
      });

    });

    describe('#createAdditionalHostComponents', function() {

      beforeEach(function() {
        sinon.stub(installerStep8Controller, 'registerHostsToComponent', Em.K);
      });

      afterEach(function() {
        installerStep8Controller.registerHostsToComponent.restore();
      });

      it('should add components with isRequiredOnAllHosts == true (1)', function() {
        installerStep8Controller.reopen({
          getRegisteredHosts: function() {
            return [{hostName: 'h1'}, {hostName: 'h2'}];
          },
          content: {
            services: [
              Em.Object.create({
                serviceName: 'GANGLIA', isSelected: true, isInstalled: false, serviceComponents: [
                  Em.Object.create({
                    componentName: 'GANGLIA_MONITOR',
                    isRequiredOnAllHosts: true
                  }),
                  Em.Object.create({
                    componentName: 'GANGLIA_SERVER',
                    isRequiredOnAllHosts: false
                  })
                ]
              })
            ]
          }
        });
        installerStep8Controller.createAdditionalHostComponents();
        expect(installerStep8Controller.registerHostsToComponent.calledOnce).to.equal(true);
        expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(['h1', 'h2']);
        expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal('GANGLIA_MONITOR');
      });

      it('should add components with isRequiredOnAllHosts == true (2)', function() {
        installerStep8Controller.reopen({
          getRegisteredHosts: function() {
            return [{hostName: 'h1', isInstalled: true}, {hostName: 'h2', isInstalled: false}];
          },
          content: {
            services: [
              Em.Object.create({
                serviceName: 'GANGLIA', isSelected: true, isInstalled: true, serviceComponents: [
                  Em.Object.create({
                    componentName: 'GANGLIA_MONITOR',
                    isRequiredOnAllHosts: true
                  }),
                  Em.Object.create({
                    componentName: 'GANGLIA_SERVER',
                    isRequiredOnAllHosts: false
                  })
                ]
              })
            ]
          }
        });
        installerStep8Controller.createAdditionalHostComponents();
        expect(installerStep8Controller.registerHostsToComponent.calledOnce).to.equal(true);
        expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(['h2']);
        expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal('GANGLIA_MONITOR');
      });

      var newDatabases = [
        {name: 'New MySQL Database',
         component: 'MYSQL_SERVER'
        },
        {name: 'New PostgreSQL Database',
          component: 'POSTGRESQL_SERVER'
        }
      ];

      newDatabases.forEach(function (db) {
        it('should add {0}'.format(db.component), function() {
          installerStep8Controller.reopen({
            getRegisteredHosts: function() {
              return [{hostName: 'h1'}, {hostName: 'h2'}];
            },
            content: {
              masterComponentHosts: [
                {component: 'HIVE_SERVER', hostName: 'h1'},
                {component: 'HIVE_SERVER', hostName: 'h2'}
              ],
              services: [
                Em.Object.create({serviceName: 'HIVE', isSelected: true, isInstalled: false, serviceComponents: []})
              ],
              serviceConfigProperties: [
                {name: 'hive_database', value: db.name}
              ]
            }
          });
          installerStep8Controller.createAdditionalHostComponents();
          expect(installerStep8Controller.registerHostsToComponent.calledOnce).to.equal(true);
          expect(installerStep8Controller.registerHostsToComponent.args[0][0]).to.eql(['h1', 'h2']);
          expect(installerStep8Controller.registerHostsToComponent.args[0][1]).to.equal(db.component);
        });

      });

    });

    describe('#createNotification', function () {

      beforeEach(function () {
        var stub = sinon.stub(App, 'get');
        stub.withArgs('testMode').returns(false);
        installerStep8Controller.clearStep();
        installerStep8Controller.set('content', {controllerName: 'installerController'});
        installerStep8Controller.set('configs', [
          {name: 'create_notification', value: 'yes', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'ambari.dispatch.recipients', value: 'to@f.c', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'mail.smtp.host', value: 'h', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'mail.smtp.port', value: '25', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'mail.smtp.from', value: 'from@f.c', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'mail.smtp.starttls.enable', value: true, serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'mail.smtp.startssl.enable', value: false, serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'smtp_use_auth', value: 'true', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'ambari.dispatch.credential.username', value: 'usr', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'ambari.dispatch.credential.password', value: 'pwd', serviceName: 'MISC', filename: 'alert_notification'},
          {name: 'some_p', value: 'some_v', serviceName: 'MISC', filename: 'alert_notification'}
        ]);
        installerStep8Controller.get('ajaxRequestsQueue').clear();
        sinon.stub($, 'ajax', function () {return {complete: Em.K}});
      });

      afterEach(function () {
        App.get.restore();
        $.ajax.restore();
      });

      it('should add request to queue', function () {
        installerStep8Controller.createNotification();
        expect(installerStep8Controller.get('ajaxRequestsQueue.queue.length')).to.equal(1);
        installerStep8Controller.get('ajaxRequestsQueue').runNextRequest();
        expect($.ajax.calledOnce).to.be.true;
        expect($.ajax.args[0][0].url.contains('overwrite_existing=true')).to.be.true;
      });

      it('sent data should be valid', function () {

        installerStep8Controller.createNotification();
        var data = installerStep8Controller.get('ajaxRequestsQueue.queue')[0].data.data.AlertTarget;
        expect(data.global).to.be.true;
        expect(data.notification_type).to.equal('EMAIL');
        expect(data.alert_states).to.eql(['OK', 'WARNING', 'CRITICAL', 'UNKNOWN']);
        expect(data.properties['ambari.dispatch.recipients']).to.eql(['to@f.c']);
        expect(data.properties['mail.smtp.host']).to.equal('h');
        expect(data.properties['mail.smtp.port']).to.equal('25');
        expect(data.properties['mail.smtp.from']).to.equal('from@f.c');
        expect(data.properties['mail.smtp.starttls.enable']).to.equal(true);
        expect(data.properties['mail.smtp.startssl.enable']).to.equal(false);
        expect(data.properties['ambari.dispatch.credential.username']).to.equal('usr');
        expect(data.properties['ambari.dispatch.credential.password']).to.equal('pwd');
        expect(data.properties['some_p']).to.equal('some_v');

      });

    });

  });

  describe('#isAllClusterDeleteRequestsCompleted', function () {
    it('should depend on completed cluster delete requests number', function () {
      installerStep8Controller.setProperties({
        clusterDeleteRequestsCompleted: 0,
        clusterNames: ['c0']
      });
      expect(installerStep8Controller.get('isAllClusterDeleteRequestsCompleted')).to.be.false;
      installerStep8Controller.incrementProperty('clusterDeleteRequestsCompleted');
      expect(installerStep8Controller.get('isAllClusterDeleteRequestsCompleted')).to.be.true;
    });
  });

  describe('#deleteClusterSuccessCallback', function () {

    beforeEach(function () {
      sinon.stub(installerStep8Controller, 'showDeleteClustersErrorPopup', Em.K);
      sinon.stub(installerStep8Controller, 'startDeploy', Em.K);
      installerStep8Controller.setProperties({
        clusterDeleteRequestsCompleted: 0,
        clusterNames: ['c0', 'c1'],
        clusterDeleteErrorViews: []
      });
      installerStep8Controller.deleteClusterSuccessCallback();
    });

    afterEach(function () {
      installerStep8Controller.showDeleteClustersErrorPopup.restore();
      installerStep8Controller.startDeploy.restore();
    });

    it('no failed requests', function () {
      expect(installerStep8Controller.get('clusterDeleteRequestsCompleted')).to.equal(1);
      expect(installerStep8Controller.showDeleteClustersErrorPopup.called).to.be.false;
      expect(installerStep8Controller.startDeploy.called).to.be.false;
      installerStep8Controller.deleteClusterSuccessCallback();
      expect(installerStep8Controller.get('clusterDeleteRequestsCompleted')).to.equal(2);
      expect(installerStep8Controller.showDeleteClustersErrorPopup.called).to.be.false;
      expect(installerStep8Controller.startDeploy.calledOnce).to.be.true;
    });

    it('one request failed', function () {
      installerStep8Controller.deleteClusterErrorCallback({}, null, null, {});
      expect(installerStep8Controller.get('clusterDeleteRequestsCompleted')).to.equal(2);
      expect(installerStep8Controller.showDeleteClustersErrorPopup.calledOnce).to.be.true;
      expect(installerStep8Controller.startDeploy.called).to.be.false;
    });

  });

  describe('#deleteClusterErrorCallback', function () {

    var request = {
        status: 500,
        responseText: '{"message":"Internal Server Error"}'
      },
      ajaxOptions = 'error',
      error = 'Internal Server Error',
      opt = {
        url: 'api/v1/clusters/c0',
        type: 'DELETE'
      };

    beforeEach(function () {
      installerStep8Controller.setProperties({
        clusterDeleteRequestsCompleted: 0,
        clusterNames: ['c0', 'c1'],
        clusterDeleteErrorViews: []
      });
      sinon.stub(installerStep8Controller, 'showDeleteClustersErrorPopup', Em.K);
      installerStep8Controller.deleteClusterErrorCallback(request, ajaxOptions, error, opt);
    });

    afterEach(function () {
      installerStep8Controller.showDeleteClustersErrorPopup.restore();
    });

    it('should show error popup only if all requests are completed', function () {
      expect(installerStep8Controller.get('clusterDeleteRequestsCompleted')).to.equal(1);
      expect(installerStep8Controller.showDeleteClustersErrorPopup.called).to.be.false;
      installerStep8Controller.deleteClusterErrorCallback(request, ajaxOptions, error, opt);
      expect(installerStep8Controller.get('clusterDeleteRequestsCompleted')).to.equal(2);
      expect(installerStep8Controller.showDeleteClustersErrorPopup.calledOnce).to.be.true;
    });

    it('should create error popup body view', function () {
      expect(installerStep8Controller.get('clusterDeleteErrorViews')).to.have.length(1);
      expect(installerStep8Controller.get('clusterDeleteErrorViews.firstObject.url')).to.equal('api/v1/clusters/c0');
      expect(installerStep8Controller.get('clusterDeleteErrorViews.firstObject.type')).to.equal('DELETE');
      expect(installerStep8Controller.get('clusterDeleteErrorViews.firstObject.status')).to.equal(500);
      expect(installerStep8Controller.get('clusterDeleteErrorViews.firstObject.message')).to.equal('Internal Server Error');
    });

  });

  describe('#showDeleteClustersErrorPopup', function () {

    beforeEach(function () {
      installerStep8Controller.setProperties({
        isSubmitDisabled: true,
        isBackBtnDisabled: true
      });
      sinon.stub(App.ModalPopup, 'show', Em.K);
      installerStep8Controller.showDeleteClustersErrorPopup();
    });

    afterEach(function () {
      App.ModalPopup.show.restore();
    });

    it('should show error popup and unlock navigation', function () {
      expect(installerStep8Controller.get('isSubmitDisabled')).to.be.false;
      expect(installerStep8Controller.get('isBackBtnDisabled')).to.be.false;
      expect(App.ModalPopup.show.calledOnce).to.be.true;
    });

  });

  describe('#startDeploy', function () {

    var stubbedNames = ['createCluster', 'createSelectedServices', 'updateConfigurations', 'createConfigurations',
        'applyConfigurationsToCluster', 'createComponents', 'registerHostsToCluster', 'createConfigurationGroups',
        'createMasterHostComponents', 'createSlaveAndClientsHostComponents', 'createAdditionalClientComponents',
        'createAdditionalHostComponents'],
      cases = [
        {
          controllerName: 'installerController',
          notExecuted: ['createAdditionalClientComponents', 'updateConfigurations'],
          fileNamesToUpdate: [],
          title: 'Installer, no configs to update'
        },
        {
          controllerName: 'addHostController',
          notExecuted: ['updateConfigurations', 'createConfigurations', 'applyConfigurationsToCluster', 'createAdditionalClientComponents'],
          title: 'Add Host Wizard'
        },
        {
          controllerName: 'addServiceController',
          notExecuted: ['updateConfigurations'],
          fileNamesToUpdate: [],
          title: 'Add Service Wizard, no configs to update'
        },
        {
          controllerName: 'addServiceController',
          notExecuted: [],
          fileNamesToUpdate: [''],
          title: 'Add Service Wizard, some configs to be updated'
        }
      ];

    beforeEach(function () {
      sinon.stub(App, 'get').withArgs('isKerberosEnabled').returns(false);
      stubbedNames.forEach(function (name) {
        sinon.stub(installerStep8Controller, name, Em.K);
      });
      installerStep8Controller.setProperties({
        serviceConfigTags: [],
        content: {
          controllerName: null
        }
      });
    });

    afterEach(function () {
      App.get.restore();
      stubbedNames.forEach(function (name) {
        installerStep8Controller[name].restore();
      });
      installerStep8Controller.get.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        sinon.stub(installerStep8Controller, 'get')
          .withArgs('ajaxRequestsQueue').returns({
            start: Em.K
          })
          .withArgs('ajaxRequestsQueue.queue.length').returns(1)
          .withArgs('wizardController').returns({
            getDBProperty: function () {
              return item.fileNamesToUpdate;
            }
          })
          .withArgs('content.controllerName').returns(item.controllerName);
        installerStep8Controller.startDeploy();
        stubbedNames.forEach(function (name) {
          expect(installerStep8Controller[name].called).to.equal(!item.notExecuted.contains(name));
        });
      });
    });

  });

  describe('#getClientsMap', function () {

    var cases = [
      {
        flag: 'isMaster',
        result: {
          c8: ['c1', 'c2'],
          c9: ['c1', 'c2']
        },
        title: 'dependencies for masters'
      },
      {
        flag: 'isSlave',
        result: {
          c8: ['c5', 'c6'],
          c9: ['c5', 'c6']
        },
        title: 'dependencies for slaves'
      },
      {
        flag: 'isClient',
        result: {
          c8: ['c9', 'c10'],
          c9: ['c9', 'c10']
        },
        title: 'dependencies for clients'
      },
      {
        flag: null,
        result: {
          c8: ['c1', 'c2', 'c5', 'c6', 'c9', 'c10'],
          c9: ['c1', 'c2', 'c5', 'c6', 'c9', 'c10']
        },
        title: 'dependencies for all components'
      }
    ];

    before(function () {
      sinon.stub(App.StackServiceComponent, 'find').returns([
        Em.Object.create({
          componentName: 'c0',
          isMaster: true,
          dependencies: [
            {
              componentName: 'c1'
            },
            {
              componentName: 'c2'
            },
            {
              componentName: 'c4'
            },
            {
              componentName: 'c5'
            }
          ]
        }),
        Em.Object.create({
          componentName: 'c1',
          isMaster: true,
          dependencies: [
            {
              componentName: 'c4'
            },
            {
              componentName: 'c5'
            },
            {
              componentName: 'c8'
            },
            {
              componentName: 'c9'
            }
          ]
        }),
        Em.Object.create({
          componentName: 'c2',
          isMaster: true,
          dependencies: [
            {
              componentName: 'c1'
            },
            {
              componentName: 'c2'
            },
            {
              componentName: 'c8'
            },
            {
              componentName: 'c9'
            }
          ]
        }),
        Em.Object.create({
          componentName: 'c3',
          isMaster: true,
          dependencies: []
        }),
        Em.Object.create({
          componentName: 'c4',
          isSlave: true,
          dependencies: [
            {
              componentName: 'c1'
            },
            {
              componentName: 'c2'
            },
            {
              componentName: 'c4'
            },
            {
              componentName: 'c5'
            }
          ]
        }),
        Em.Object.create({
          componentName: 'c5',
          isSlave: true,
          dependencies: [
            {
              componentName: 'c4'
            },
            {
              componentName: 'c5'
            },
            {
              componentName: 'c8'
            },
            {
              componentName: 'c9'
            }
          ]
        }),
        Em.Object.create({
          componentName: 'c6',
          isSlave: true,
          dependencies: [
            {
              componentName: 'c1'
            },
            {
              componentName: 'c2'
            },
            {
              componentName: 'c8'
            },
            {
              componentName: 'c9'
            }
          ]
        }),
        Em.Object.create({
          componentName: 'c7',
          isSlave: true,
          dependencies: []
        }),
        Em.Object.create({
          componentName: 'c8',
          isClient: true,
          dependencies: [
            {
              componentName: 'c1'
            },
            {
              componentName: 'c2'
            },
            {
              componentName: 'c4'
            },
            {
              componentName: 'c5'
            }
          ]
        }),
        Em.Object.create({
          componentName: 'c9',
          isClient: true,
          dependencies: [
            {
              componentName: 'c4'
            },
            {
              componentName: 'c5'
            },
            {
              componentName: 'c8'
            },
            {
              componentName: 'c9'
            }
          ]
        }),
        Em.Object.create({
          componentName: 'c10',
          isClient: true,
          dependencies: [
            {
              componentName: 'c1'
            },
            {
              componentName: 'c2'
            },
            {
              componentName: 'c8'
            },
            {
              componentName: 'c9'
            }
          ]
        }),
        Em.Object.create({
          componentName: 'c11',
          isClient: true,
          dependencies: []
        })
      ]);
    });

    after(function () {
      App.StackServiceComponent.find.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(installerStep8Controller.getClientsMap(item.flag)).to.eql(item.result);
      });
    });

  });

  describe('#showLoadingIndicator', function() {
    it('if popup doesn\'t exist should create another', function() {
      installerStep8Controller.set('isSubmitDisabled', true);
      sinon.spy(App.ModalPopup, 'show');
      installerStep8Controller.showLoadingIndicator();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
      App.ModalPopup.show.restore();
    });

  });

});
