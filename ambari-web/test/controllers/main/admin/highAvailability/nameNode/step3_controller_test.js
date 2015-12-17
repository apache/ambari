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

var controller;

describe('App.HighAvailabilityWizardStep3Controller', function() {

  var serverConfigData = {
    items: [
      {
        type: 'hdfs-site',
        properties: {
          'dfs.namenode.http-address': 'h1:1234',
          'dfs.namenode.https-address': 'h1:4321',
          'dfs.namenode.rpc-address': 'h1:1111',
          'dfs.journalnode.edits.dir': '/hadoop/hdfs/journalnode123'
        }
      },
      {
        type: 'zoo.cfg',
        properties: {
          clientPort: '4444'
        }
      },
      {
        type: 'hbase-site',
        properties: {
          'hbase.rootdir': 'hdfs://h34:8020/apps/hbase/data'
        }
      },
      {
        type: 'ams-hbase-site',
        properties: {
          'hbase.rootdir': 'file:///var/lib/ambari-metrics-collector/hbase'
        }
      },
      {
        type: 'accumulo-site',
        properties: {
          'instance.volumes': 'hdfs://localhost:8020/apps/accumulo/data'
        }
      },
      {
        type: 'hawq-site',
        properties: {
          'hawq_dfs_url': 'localhost:8020/hawq_default'
        }
      }
    ]
  };

  beforeEach(function () {
    controller = App.HighAvailabilityWizardStep3Controller.create();
    controller.set('serverConfigData', serverConfigData);
  });

  afterEach(function () {
    controller.destroy();
  });

  describe('#removeConfigs', function() {

    var tests = [
      {
        m: 'should not delete properties if configsToRemove is empty',
        configs: {
          items: [
            {
              type: 'site1',
              properties: {
                property1: 'value1',
                property2: 'value2',
                property3: 'value3',
                property4: 'value4'
              }
            }
          ]
        },
        toRemove: {},
        expected: {
          items: [
            {
              type: 'site1',
              properties: {
                property1: 'value1',
                property2: 'value2',
                property3: 'value3',
                property4: 'value4'
              }
            }
          ]
        }
      },
      {
        m: 'should delete properties from configsToRemove',
        configs: {
          items: [
            {
              type: 'site1',
              properties: {
                property1: 'value1',
                property2: 'value2',
                property3: 'value3',
                property4: 'value4'
              }
            }
          ]
        },
        toRemove: {
          'site1': ['property1', 'property3']
        },
        expected: {
          items: [
            {
              type: 'site1',
              properties: {
                property2: 'value2',
                property4: 'value4'
              }
            }
          ]
        }
      },
      {
        m: 'should delete properties from configsToRemove from different sites',
        configs: {
          items: [
            {
              type: 'site1',
              properties: {
                property1: 'value1',
                property2: 'value2',
                property3: 'value3',
                property4: 'value4'
              }
            },
            {
              type: 'site2',
              properties: {
                property1: 'value1',
                property2: 'value2',
                property3: 'value3',
                property4: 'value4'
              }
            }
          ]
        },
        toRemove: {
          'site1': ['property1', 'property3'],
          'site2': ['property2', 'property4']
        },
        expected: {
          items: [
            {
              type: 'site1',
              properties: {
                property2: 'value2',
                property4: 'value4'
              }
            },
            {
              type: 'site2',
              properties: {
                property1: 'value1',
                property3: 'value3'
              }
            }
          ]
        }
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function() {
        var controller = App.HighAvailabilityWizardStep3Controller.create({
          configsToRemove: test.toRemove,
          serverConfigData: test.configs
        });
        var result = controller.removeConfigs(test.toRemove, controller.get('serverConfigData'));
        expect(JSON.stringify(controller.get('serverConfigData'))).to.equal(JSON.stringify(test.expected));
        expect(JSON.stringify(result)).to.equal(JSON.stringify(test.expected));
      });
    });
  });

  describe('#tweakServiceConfigs', function () {

    var nameServiceId = 'tdk';

    var masterComponentHosts = [
      {component: 'NAMENODE', isInstalled: true, hostName: 'h1'},
      {component: 'NAMENODE', isInstalled: false, hostName: 'h2'},
      {component: 'JOURNALNODE', hostName: 'h1'},
      {component: 'JOURNALNODE', hostName: 'h2'},
      {component: 'JOURNALNODE', hostName: 'h3'},
      {component: 'ZOOKEEPER_SERVER', hostName: 'h1'},
      {component: 'ZOOKEEPER_SERVER', hostName: 'h2'},
      {component: 'ZOOKEEPER_SERVER', hostName: 'h3'}
    ];

    beforeEach(function () {
      controller.set('content', Em.Object.create({
        masterComponentHosts: masterComponentHosts,
        slaveComponentHosts: [],
        hosts: {},
        nameServiceId: nameServiceId
      }));
      var get = sinon.stub(App, 'get');
      get.withArgs('isHadoopWindowsStack').returns(true);
      sinon.stub(App.Service, 'find', function () {
        return [{serviceName: 'HDFS'}, {serviceName: 'HBASE'}, {serviceName: 'AMBARI_METRICS'}, {serviceName: 'ACCUMULO'}, {serviceName: 'HAWQ'}]
      });
    });

    afterEach(function () {
      App.Service.find.restore();
      App.get.restore();
    });

    Em.A([
      {
        config: {
          name: 'dfs.namenode.rpc-address.${dfs.nameservices}.nn1'
        },
        value: 'h1:1111',
        name: 'dfs.namenode.rpc-address.' + nameServiceId + '.nn1'
      },
      {
        config: {
          name: 'dfs.namenode.rpc-address.${dfs.nameservices}.nn2'
        },
        value: 'h2:8020',
        name: 'dfs.namenode.rpc-address.' + nameServiceId + '.nn2'
      },
      {
        config: {
          name: 'dfs.namenode.http-address.${dfs.nameservices}.nn1'
        },
        value: 'h1:1234',
        name: 'dfs.namenode.http-address.' + nameServiceId + '.nn1'
      },
      {
        config: {
          name: 'dfs.namenode.http-address.${dfs.nameservices}.nn2'
        },
        value: 'h2:50070',
        name: 'dfs.namenode.http-address.' + nameServiceId + '.nn2'
      },
      {
        config: {
          name: 'dfs.namenode.https-address.${dfs.nameservices}.nn1'
        },
        value: 'h1:4321',
        name: 'dfs.namenode.https-address.' + nameServiceId + '.nn1'
      },
      {
        config: {
          name: 'dfs.namenode.https-address.${dfs.nameservices}.nn2'
        },
        value: 'h2:50470',
        name: 'dfs.namenode.https-address.' + nameServiceId + '.nn2'
      },
      {
        config: {
          name: 'dfs.namenode.shared.edits.dir'
        },
        value: 'qjournal://h1:8485;h2:8485;h3:8485/' + nameServiceId
      },
      {
        config: {
          name: 'ha.zookeeper.quorum'
        },
        value: 'h1:4444,h2:4444,h3:4444'
      },
      {
        config: {
          name: 'hbase.rootdir',
          filename: 'hbase-site'
        },
        value: 'hdfs://' + nameServiceId + '/apps/hbase/data'
      },
      {
        config: {
          name: 'hbase.rootdir',
          filename: 'ams-hbase-site'
        },
        value: 'file:///var/lib/ambari-metrics-collector/hbase'
      },
      {
        config: {
          name: 'instance.volumes'
        },
        value: 'hdfs://' + nameServiceId + '/apps/accumulo/data'
      },
      {
        config: {
          name: 'instance.volumes.replacements'
        },
        value: 'hdfs://localhost:8020/apps/accumulo/data hdfs://' + nameServiceId + '/apps/accumulo/data'
      },
      {
        config: {
          name: 'dfs.journalnode.edits.dir'
        },
        value: '/hadoop/hdfs/journalnode123'
      },
      {
        config: {
          name: 'hawq_dfs_url',
          filename: 'hawq-site'
        },
        value: nameServiceId + '/hawq_default'
      }
    ]).forEach(function (test) {
      it(test.config.name, function () {
        test.config.displayName = test.config.name;
        var configs = [test.config];
        configs = controller.tweakServiceConfigs(configs);
        expect(configs[0].value).to.equal(test.value);
        expect(configs[0].recommendedValue).to.equal(test.value);
        if(test.name) {
          expect(configs[0].name).to.equal(test.name);
          expect(configs[0].displayName).to.equal(test.name);
        }
      });
    });

  });

});

