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
var configPropertyHelper = require('utils/configs/config_property_helper');

require('models/configs/objects/service_config_property');

var serviceConfig,
  group,
  serviceConfigProperty,

  components = [
    {
      name: 'NameNode',
      master: true
    },
    {
      name: 'SNameNode',
      master: true
    },
    {
      name: 'JobTracker',
      master: true
    },
    {
      name: 'HBase Master',
      master: true
    },
    {
      name: 'Oozie Master',
      master: true
    },
    {
      name: 'Hive Metastore',
      master: true
    },
    {
      name: 'WebHCat Server',
      master: true
    },
    {
      name: 'ZooKeeper Server',
      master: true
    },
    {
      name: 'Ganglia',
      master: true
    },
    {
      name: 'DataNode',
      slave: true
    },
    {
      name: 'TaskTracker',
      slave: true
    },
    {
      name: 'RegionServer',
      slave: true
    }
  ],
  masters = components.filterProperty('master'),
  slaves = components.filterProperty('slave');


describe('configPropertyHelper', function () {

  beforeEach(function () {
    serviceConfigProperty = App.ServiceConfigProperty.create();
  });

  describe('#setRecommendedValue', function () {
    it('should change the recommended value', function () {
      serviceConfigProperty.set('recommendedValue', 'value0');
      configPropertyHelper.setRecommendedValue(serviceConfigProperty, /\d/, '1');
      expect(serviceConfigProperty.get('recommendedValue')).to.equal('value1');
    });
  });


  describe('#initialValue', function () {

    var cases = {
      'kafka.ganglia.metrics.host': [
        {
          message: 'kafka.ganglia.metrics.host property should have the value of ganglia hostname when ganglia is selected',
          localDB: {
            masterComponentHosts: [
              {
                component: 'GANGLIA_SERVER',
                hostName: 'c6401'
              }
            ]
          },
          expected: 'c6401'
        },
        {
          message: 'kafka.ganglia.metrics.host property should have the value "localhost" when ganglia is not selected',
          localDB: {
            masterComponentHosts: [
              {
                component: 'NAMENODE',
                hostName: 'c6401'
              }
            ]
          },
          expected: 'localhost'
        }
      ],
      'hive_database': [
        {
          alwaysEnableManagedMySQLForHive: true,
          currentStateName: '',
          isManagedMySQLForHiveEnabled: false,
          receivedValue: 'New MySQL Database',
          value: 'New MySQL Database',
          options: [
            {
              displayName: 'New MySQL Database'
            }
          ],
          hidden: false
        },
        {
          alwaysEnableManagedMySQLForHive: false,
          currentStateName: 'configs',
          isManagedMySQLForHiveEnabled: false,
          receivedValue: 'New MySQL Database',
          value: 'New MySQL Database',
          options: [
            {
              displayName: 'New MySQL Database'
            }
          ],
          hidden: false
        },
        {
          alwaysEnableManagedMySQLForHive: false,
          currentStateName: '',
          isManagedMySQLForHiveEnabled: true,
          receivedValue: 'New MySQL Database',
          value: 'New MySQL Database',
          options: [
            {
              displayName: 'New MySQL Database'
            }
          ],
          hidden: false
        },
        {
          alwaysEnableManagedMySQLForHive: false,
          currentStateName: '',
          isManagedMySQLForHiveEnabled: false,
          receivedValue: 'New MySQL Database',
          value: 'Existing MySQL Database',
          options: [
            {
              displayName: 'New MySQL Database'
            }
          ],
          hidden: true
        },
        {
          alwaysEnableManagedMySQLForHive: false,
          currentStateName: '',
          isManagedMySQLForHiveEnabled: false,
          receivedValue: 'New PostgreSQL Database',
          value: 'New PostgreSQL Database',
          options: [
            {
              displayName: 'New MySQL Database'
            }
          ],
          hidden: true
        }
      ],
      'hbase.zookeeper.quorum': [
        {
          filename: 'hbase-site.xml',
          value: 'host0,host1',
          recommendedValue: 'host0,host1',
          title: 'should set ZooKeeper Server hostnames'
        },
        {
          filename: 'ams-hbase-site.xml',
          value: 'localhost',
          recommendedValue: null,
          title: 'should ignore ZooKeeper Server hostnames'
        }
      ],
      'hivemetastore_host': {
        localDB: {
          masterComponentHosts: [
            {
              component: 'HIVE_METASTORE',
              hostName: 'h0'
            },
            {
              component: 'HIVE_METASTORE',
              hostName: 'h1'
            }
          ]
        },
        value: ['h0', 'h1'],
        title: 'array that contains names of hosts with Hive Metastore'
      },
      'hive_master_hosts': {
        localDB: {
          masterComponentHosts: [
            {
              component: 'HIVE_SERVER',
              hostName: 'h0'
            },
            {
              component: 'HIVE_METASTORE',
              hostName: 'h0'
            },
            {
              component: 'HIVE_METASTORE',
              hostName: 'h1'
            },
            {
              component: 'WEBHCAT_SERVER',
              hostName: 'h2'
            }
          ]
        },
        value: 'h0,h1',
        title: 'comma separated list of hosts with Hive Server and Metastore'
      },
      'hive.metastore.uris': {
        localDB: {
          masterComponentHosts: [
            {
              component: 'HIVE_METASTORE',
              hostName: 'h0'
            },
            {
              component: 'HIVE_METASTORE',
              hostName: 'h1'
            }
          ]
        },
        dependencies: {
          'hive.metastore.uris': 'thrift://localhost:9083'
        },
        recommendedValue: 'thrift://localhost:9083',
        value: 'thrift://h0:9083,thrift://h1:9083',
        title: 'comma separated list of Metastore hosts with thrift prefix and port'
      },
      'templeton.hive.properties': {
        localDB: {
          masterComponentHosts: [
            {
              component: 'HIVE_METASTORE',
              hostName: 'h0'
            },
            {
              component: 'HIVE_METASTORE',
              hostName: 'h1'
            }
          ]
        },
        dependencies: {
          'hive.metastore.uris': 'thrift://localhost:9083'
        },
        recommendedValue: 'hive.metastore.local=false,hive.metastore.uris=thrift://localhost:9083,hive.metastore.sasl.enabled=false',
        value: 'hive.metastore.local=false,hive.metastore.uris=thrift://h0:9083\\,thrift://h1:9083,hive.metastore.sasl.enabled=false,hive.metastore.execute.setugi=true',
        title: 'should add relevant hive.metastore.uris value'
      },
      'yarn.resourcemanager.zk-address': {
        localDB: {
          masterComponentHosts: [
            {
              component: 'ZOOKEEPER_SERVER',
              hostName: 'h0'
            },
            {
              component: 'ZOOKEEPER_SERVER',
              hostName: 'h1'
            }
          ]
        },
        dependencies: {
          clientPort: '2182'
        },
        recommendedValue: 'localhost:2181',
        value: 'h0:2182,h1:2182',
        title: 'should add ZK host and port dynamically'
      },
      'oozie_hostname': {
        localDB: {
          masterComponentHosts: [
            {
              component: 'OOZIE_SERVER',
              hostName: 'h0'
            },
            {
              component: 'OOZIE_SERVER',
              hostName: 'h1'
            }
          ]
        },
        value: ['h0', 'h1'],
        title: 'array that contains names of hosts with Oozie Server'
      },
      'knox_gateway_host': {
        localDB: {
          masterComponentHosts: [
            {
              component: 'KNOX_GATEWAY',
              hostName: 'h0'
            },
            {
              component: 'KNOX_GATEWAY',
              hostName: 'h1'
            }
          ]
        },
        value: ['h0', 'h1'],
        title: 'array that contains names of hosts with Knox Gateway'
      }
    };

    cases['kafka.ganglia.metrics.host'].forEach(function (item) {
      it(item.message, function () {
        serviceConfigProperty.setProperties({
          name: 'kafka.ganglia.metrics.host',
          value: 'localhost'
        });
        configPropertyHelper.initialValue(serviceConfigProperty, item.localDB, []);
        expect(serviceConfigProperty.get('value')).to.equal(item.expected);
      });
    });

    cases['hive_database'].forEach(function (item) {
      var title = 'hive_database value should be set to {0}';
      it(title.format(item.value), function () {
        sinon.stub(App, 'get')
          .withArgs('supports.alwaysEnableManagedMySQLForHive').returns(item.alwaysEnableManagedMySQLForHive)
          .withArgs('router.currentState.name').returns(item.currentStateName)
          .withArgs('isManagedMySQLForHiveEnabled').returns(item.isManagedMySQLForHiveEnabled);
        serviceConfigProperty.setProperties({
          name: 'hive_database',
          value: item.receivedValue,
          options: item.options
        });
        configPropertyHelper.initialValue(serviceConfigProperty, {}, []);
        expect(serviceConfigProperty.get('value')).to.equal(item.value);
        expect(serviceConfigProperty.get('options').findProperty('displayName', 'New MySQL Database').hidden).to.equal(item.hidden);
        App.get.restore();
      });
    });

    cases['hbase.zookeeper.quorum'].forEach(function (item) {
      it(item.title, function () {
        serviceConfigProperty.setProperties({
          name: 'hbase.zookeeper.quorum',
          value: 'localhost',
          'filename': item.filename
        });
        configPropertyHelper.initialValue(serviceConfigProperty, {
          masterComponentHosts: {
            filterProperty: function () {
              return {
                mapProperty: function () {
                  return ['host0', 'host1'];
                }
              };
            }
          }
        }, []);
        expect(serviceConfigProperty.get('value')).to.equal(item.value);
        expect(serviceConfigProperty.get('recommendedValue')).to.equal(item.recommendedValue);
      });
    });

    it(cases['hive_master_hosts'].title, function () {
      serviceConfigProperty.set('name', 'hive_master_hosts');
      configPropertyHelper.initialValue(serviceConfigProperty, cases['hive_master_hosts'].localDB, []);
      expect(serviceConfigProperty.get('value')).to.equal(cases['hive_master_hosts'].value);
    });

    it(cases['hive.metastore.uris'].title, function () {
      serviceConfigProperty.setProperties({
        name: 'hive.metastore.uris',
        recommendedValue: cases['hive.metastore.uris'].recommendedValue
      });
      configPropertyHelper.initialValue(serviceConfigProperty, cases['hive.metastore.uris'].localDB, {'hive.metastore.uris': cases['hive.metastore.uris'].recommendedValue});
      expect(serviceConfigProperty.get('value')).to.equal(cases['hive.metastore.uris'].value);
      expect(serviceConfigProperty.get('recommendedValue')).to.equal(cases['hive.metastore.uris'].value);
    });

    it(cases['templeton.hive.properties'].title, function () {
      serviceConfigProperty.setProperties({
        name: 'templeton.hive.properties',
        recommendedValue: cases['templeton.hive.properties'].recommendedValue,
        value: cases['templeton.hive.properties'].recommendedValue
      });
      configPropertyHelper.initialValue(serviceConfigProperty, cases['templeton.hive.properties'].localDB,  {'hive.metastore.uris': cases['templeton.hive.properties'].recommendedValue});
      expect(serviceConfigProperty.get('value')).to.equal(cases['templeton.hive.properties'].value);
      expect(serviceConfigProperty.get('recommendedValue')).to.equal(cases['templeton.hive.properties'].value);
    });

    it(cases['yarn.resourcemanager.zk-address'].title, function () {
      serviceConfigProperty.setProperties({
        name: 'yarn.resourcemanager.zk-address',
        recommendedValue: cases['yarn.resourcemanager.zk-address'].recommendedValue
      });
      configPropertyHelper.initialValue(serviceConfigProperty, cases['yarn.resourcemanager.zk-address'].localDB,  cases['yarn.resourcemanager.zk-address'].dependencies);
      expect(serviceConfigProperty.get('value')).to.equal(cases['yarn.resourcemanager.zk-address'].value);
      expect(serviceConfigProperty.get('recommendedValue')).to.equal(cases['yarn.resourcemanager.zk-address'].value);
    });

  });

  describe('#getHiveMetastoreUris', function () {

    var cases = [
      {
        hosts: [
          {
            hostName: 'h0',
            component: 'HIVE_SERVER'
          },
          {
            hostName: 'h1',
            component: 'HIVE_METASTORE'
          },
          {
            hostName: 'h2',
            component: 'HIVE_METASTORE'
          }
        ],
        recommendedValue: 'thrift://localhost:9083',
        expected: 'thrift://h1:9083,thrift://h2:9083',
        title: 'typical case'
      },
      {
        hosts: [
          {
            hostName: 'h0',
            component: 'HIVE_SERVER'
          }
        ],
        recommendedValue: 'thrift://localhost:9083',
        expected: '',
        title: 'no Metastore hosts in DB'
      },
      {
        hosts: [
          {
            hostName: 'h0',
            component: 'HIVE_SERVER'
          },
          {
            hostName: 'h1',
            component: 'HIVE_METASTORE'
          },
          {
            hostName: 'h2',
            component: 'HIVE_METASTORE'
          }
        ],
        recommendedValue: '',
        expected: '',
        title: 'default value without port'
      },
      {
        hosts: [
          {
            hostName: 'h0',
            component: 'HIVE_SERVER'
          },
          {
            hostName: 'h1',
            component: 'HIVE_METASTORE'
          },
          {
            hostName: 'h2',
            component: 'HIVE_METASTORE'
          }
        ],
        expected: '',
        title: 'no default value specified'
      }
    ];

    cases.forEach(function (item) {
      it(item.title, function () {
        expect(configPropertyHelper.getHiveMetastoreUris(item.hosts, item.recommendedValue)).to.equal(item.expected);
      });
    });

  });

  describe('#unionAllMountPoints', function () {

    var localDB = {
        masterComponentHosts: [
          {
            component: 'NAMENODE',
            hostName: 'h0'
          },
          {
            component: 'SECONDARY_NAMENODE',
            hostName: 'h4'
          },
          {
            component: 'APP_TIMELINE_SERVER',
            hostName: 'h0'
          },
          {
            component: 'ZOOKEEPER_SERVER',
            hostName: 'h0'
          },
          {
            component: 'ZOOKEEPER_SERVER',
            hostName: 'h1'
          },
          {
            component: 'OOZIE_SERVER',
            hostName: 'h0'
          },
          {
            component: 'OOZIE_SERVER',
            hostName: 'h1'
          },
          {
            component: 'NIMBUS',
            hostName: 'h2'
          },
          {
            component: 'FALCON_SERVER',
            hostName: 'h3'
          },
          {
            component: 'KAFKA_BROKER',
            hostName: 'h0'
          },
          {
            component: 'KAFKA_BROKER',
            hostName: 'h1'
          }
        ],
        slaveComponentHosts: [
          {
            componentName: 'DATANODE',
            hosts: [
              {
                hostName: 'h0'
              },
              {
                hostName: 'h1'
              }
            ]
          },
          {
            componentName: 'TASKTRACKER',
            hosts: [
              {
                hostName: 'h0'
              },
              {
                hostName: 'h1'
              }
            ]
          },
          {
            componentName: 'NODEMANAGER',
            hosts: [
              {
                hostName: 'h0'
              },
              {
                hostName: 'h1'
              },
              {
                hostName: 'h4'
              }
            ]
          },
          {
            componentName: 'HBASE_REGIONSERVER',
            hosts: [
              {
                hostName: 'h0'
              },
              {
                hostName: 'h1'
              }
            ]
          },
          {
            componentName: 'SUPERVISOR',
            hosts: [
              {
                hostName: 'h0'
              },
              {
                hostName: 'h1'
              }
            ]
          }
        ],
        hosts: {
          h0: {
            disk_info: [
              {
                mountpoint: '/'
              },
              {
                mountpoint: '/home'
              },
              {
                mountpoint: '/boot'
              },
              {
                mountpoint: '/boot/efi'
              },
              {
                mountpoint: '/mnt'
              },
              {
                mountpoint: '/mnt/efi'
              },
              {
                mountpoint: '/media/disk0',
                available: '100000000'
              },
              {
                mountpoint: '/mount0',
                available: '100000000'
              }
            ]
          },
          h4: {
            disk_info: [
              {
                mountpoint: 'c:',
                available: '100000000'
              }
            ]
          }
        }
      },
      cases = [
        {
          name: 'dfs.namenode.name.dir',
          isOnlyFirstOneNeeded: false,
          value: '/media/disk0/default\n/mount0/default\n'
        },
        {
          name: 'dfs.name.dir',
          isOnlyFirstOneNeeded: false,
          value: '/media/disk0/default\n/mount0/default\n'
        },
        {
          name: 'fs.checkpoint.dir',
          isOnlyFirstOneNeeded: true,
          value: 'file:///c:/default\n'
        },
        {
          name: 'dfs.namenode.checkpoint.dir',
          isOnlyFirstOneNeeded: true,
          value: 'file:///c:/default\n'
        },
        {
          name: 'dfs.data.dir',
          isOnlyFirstOneNeeded: false,
          value: '/media/disk0/default\n/mount0/default\n/media/disk1/default\n/mount1/default\n'
        },
        {
          name: 'dfs.datanode.data.dir',
          isOnlyFirstOneNeeded: false,
          value: '/media/disk0/default\n/mount0/default\n/media/disk1/default\n/mount1/default\n'
        },
        {
          name: 'mapred.local.dir',
          isOnlyFirstOneNeeded: false,
          value: '/media/disk0/default\n/mount0/default\n/media/disk1/default\n/mount1/default\n'
        },
        {
          name: 'yarn.nodemanager.log-dirs',
          isOnlyFirstOneNeeded: false,
          value: '/media/disk0/default\n/mount0/default\n/media/disk1/default\n/mount1/default\nc:\\default\n'
        },
        {
          name: 'yarn.nodemanager.local-dirs',
          isOnlyFirstOneNeeded: false,
          value: '/media/disk0/default\n/mount0/default\n/media/disk1/default\n/mount1/default\nc:\\default\n'
        },
        {
          name: 'yarn.timeline-service.leveldb-timeline-store.path',
          isOnlyFirstOneNeeded: true,
          value: '/media/disk0/default'
        },
        {
          name: 'yarn.timeline-service.leveldb-state-store.path',
          isOnlyFirstOneNeeded: true,
          value: '/media/disk0/default'
        },
        {
          name: 'dataDir',
          isOnlyFirstOneNeeded: true,
          value: '/media/disk0/default'
        },
        {
          name: 'oozie_data_dir',
          isOnlyFirstOneNeeded: true,
          value: '/media/disk0/default'
        },
        {
          name: 'storm.local.dir',
          isOnlyFirstOneNeeded: true,
          value: '/media/disk0/default'
        },
        {
          name: '*.falcon.graph.storage.directory',
          isOnlyFirstOneNeeded: true,
          value: '/default'
        },
        {
          name: '*.falcon.graph.serialize.path',
          isOnlyFirstOneNeeded: true,
          value: '/default'
        },
        {
          name: 'log.dirs',
          isOnlyFirstOneNeeded: false,
          value: '/media/disk0/default\n/mount0/default\n/media/disk1/default\n/mount1/default\n'
        }
      ];

    beforeEach(function () {
      sinon.stub(App.Host, 'find').returns([
        Em.Object.create({
          id: 'h1',
          diskInfo: [
            {
              mountpoint: '/media/disk1',
              type: 'devtmpfs'
            },
            {
              mountpoint: '/media/disk1',
              type: 'tmpfs'
            },
            {
              mountpoint: '/media/disk1',
              type: 'vboxsf'
            },
            {
              mountpoint: '/media/disk1',
              type: 'CDFS'
            },
            {
              mountpoint: '/media/disk1',
              available: '0'
            },
            {
              mountpoint: '/media/disk1',
              available: '100000000'
            },
            {
              mountpoint: '/mount1',
              available: '100000000'
            }
          ]
        }),
        Em.Object.create({
          id: 'h2',
          diskInfo: [
            {
              mountpoint: '/'
            }
          ]
        }),
        Em.Object.create({
          id: 'h3',
          diskInfo: []
        })
      ]);
    });

    afterEach(function () {
      App.Host.find.restore();
    });

    cases.forEach(function (item) {
      it(item.name, function () {
        serviceConfigProperty.setProperties({
          name: item.name,
          recommendedValue: '/default'
        });
        configPropertyHelper.unionAllMountPoints(serviceConfigProperty, item.isOnlyFirstOneNeeded, localDB);
        expect(serviceConfigProperty.get('value')).to.equal(item.value);
        expect(serviceConfigProperty.get('recommendedValue')).to.equal(item.value);
      });
    });

  });
});