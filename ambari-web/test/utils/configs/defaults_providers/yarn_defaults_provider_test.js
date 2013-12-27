/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');
require('utils/configs/defaults_providers/defaultsProvider');
require('utils/configs/defaults_providers/yarn_defaults_provider');

describe('YARNDefaultsProvider', function() {

  afterEach(function() {
    App.YARNDefaultsProvider.set('clusterData', null);
    App.YARNDefaultsProvider.set('reservedRam', null);
    App.YARNDefaultsProvider.set('hBaseRam', null);
    App.YARNDefaultsProvider.set('containers', null);
    App.YARNDefaultsProvider.set('recommendedMinimumContainerSize', null);
    App.YARNDefaultsProvider.set('ramPerContainer', null);
    App.YARNDefaultsProvider.set('mapMemory', null);
    App.YARNDefaultsProvider.set('reduceMemory', null);
    App.YARNDefaultsProvider.set('amMemory', null);
  });

  describe('#clusterDataIsValid', function() {
    var tests = [
      {clusterData: {disk: 12,ram: 48,cpu: 12,hBaseInstalled: false},e: true},
      {clusterData: {disk: null,ram: 48,cpu: 12,hBaseInstalled: false},e: false},
      {clusterData: {disk: 12,ram: null,cpu: 12,hBaseInstalled: false},e: false},
      {clusterData: {disk: 12,ram: 48,cpu: null,hBaseInstalled: false},e: false},
      {clusterData: {disk: 12,ram: 48,cpu: 12,hBaseInstalled: null},e: false},
      {clusterData: {disk: 12,ram: 48,cpu: 12},e: false},
      {clusterData: {disk: 12,ram: 48,hBaseInstalled: true},e: false},
      {clusterData: {disk: 12,cpu: 12,hBaseInstalled: true},e: false},
      {clusterData: {ram: 48,cpu: 12,hBaseInstalled: false},e: false}
    ];
    tests.forEach(function(test) {
      it((test.e?'valid':'invalid') + ' clusterData', function() {
        App.YARNDefaultsProvider.set('clusterData', test.clusterData);
        expect(App.YARNDefaultsProvider.clusterDataIsValid()).to.equal(test.e);
      });
    });
  });

  describe('#reservedMemoryRecommendations', function() {
    var tests = [
      {ram: null, e: {os: 1, hbase: 1}},
      {ram: 2, e: {os: 1, hbase: 1}},
      {ram: 4, e: {os: 1, hbase: 1}},
      {ram: 6, e: {os: 2, hbase: 1}},
      {ram: 8, e: {os: 2, hbase: 1}},
      {ram: 12, e: {os: 2, hbase: 2}},
      {ram: 16, e: {os: 2, hbase: 2}},
      {ram: 20, e: {os: 4, hbase: 4}},
      {ram: 24, e: {os: 4, hbase: 4}},
      {ram: 36, e: {os: 6, hbase: 8}},
      {ram: 48, e: {os: 6, hbase: 8}},
      {ram: 56, e: {os: 8, hbase: 8}},
      {ram: 64, e: {os: 8, hbase: 8}},
      {ram: 68, e: {os: 8, hbase: 8}},
      {ram: 72, e: {os: 8, hbase: 8}},
      {ram: 84, e: {os: 12, hbase: 16}},
      {ram: 96, e: {os: 12, hbase: 16}},
      {ram: 112, e: {os: 24, hbase: 24}},
      {ram: 128, e: {os: 24, hbase: 24}},
      {ram: 196, e: {os: 32, hbase: 32}},
      {ram: 256, e: {os: 32, hbase: 32}},
      {ram: 384, e: {os: 64, hbase: 64}},
      {ram: 512, e: {os: 64, hbase: 64}},
      {ram: 756, e: {os: 64, hbase: 64}}
    ];
    App.YARNDefaultsProvider.set('clusterData');
    tests.forEach(function(test) {
      it('ram: ' + test.ram + ' GB', function() {
        sinon.spy(App.YARNDefaultsProvider, 'reservedMemoryRecommendations');
        App.YARNDefaultsProvider.set('clusterData', {
          disk: 12,
          ram: test.ram,
          cpu: 12,
          hBaseInstalled: false
        });
        expect(App.YARNDefaultsProvider.get('reservedRam')).to.equal(test.e.os);
        expect(App.YARNDefaultsProvider.get('hBaseRam')).to.equal(test.e.hbase);
        expect(App.YARNDefaultsProvider.reservedMemoryRecommendations.calledOnce).to.be.true;
        App.YARNDefaultsProvider.reservedMemoryRecommendations.restore();
      });
    });
  });

  describe('#recommendedMinimumContainerSize', function() {
    it('No clusterData', function() {
      App.YARNDefaultsProvider.set('clusterData', null);
      expect(App.YARNDefaultsProvider.get('recommendedMinimumContainerSize')).to.equal(null);
    });
    it('No clusterData.ram', function() {
      App.YARNDefaultsProvider.set('clusterData', {});
      expect(App.YARNDefaultsProvider.get('recommendedMinimumContainerSize')).to.equal(null);
    });

    var tests = [
      {ram: 3, e: 256},
      {ram: 4, e: 256},
      {ram: 6, e: 512},
      {ram: 8, e: 512},
      {ram: 12, e: 1024},
      {ram: 24, e: 1024}
    ];

    tests.forEach(function(test) {
      it('ram: ' + test.ram + ' GB', function() {
        App.YARNDefaultsProvider.set('clusterData', {
          disk: 12,
          ram: test.ram,
          cpu: 12,
          hBaseInstalled: false
        });
        expect(App.YARNDefaultsProvider.get('recommendedMinimumContainerSize')).to.equal(test.e);
      });
    });

  });

  describe('#containers', function() {
    it('No clusterData', function() {
      App.YARNDefaultsProvider.set('clusterData', null);
      expect(App.YARNDefaultsProvider.get('containers')).to.equal(null);
    });
    it('Some clusterData metric is null', function() {
      App.YARNDefaultsProvider.set('clusterData', {disk: null, cpu: 1, ram: 1});
      expect(App.YARNDefaultsProvider.get('containers')).to.equal(null);
      App.YARNDefaultsProvider.set('clusterData', {disk: 1, cpu: null, ram: 1});
      expect(App.YARNDefaultsProvider.get('containers')).to.equal(null);
      App.YARNDefaultsProvider.set('clusterData', {disk:1, cpu: 1, ram: null});
      expect(App.YARNDefaultsProvider.get('containers')).to.equal(null);
    });

    var tests = [
      {
        clusterData: {
          disk: 12,
          ram: 48,
          cpu: 12,
          hBaseInstalled: false
        },
        e: 21
      },
      {
        clusterData: {
          disk: 6,
          ram: 48,
          cpu: 6,
          hBaseInstalled: true
        },
        e: 11
      }
    ];

    tests.forEach(function(test) {
      it((test.hBaseInstalled?'With':'Without') + ' hBase', function() {
        App.YARNDefaultsProvider.set('clusterData', test.clusterData);
        expect(App.YARNDefaultsProvider.get('containers')).to.equal(test.e);
      });
    });

  });

  describe('#ramPerContainer', function() {
    it('No clusterData', function() {
      App.YARNDefaultsProvider.set('clusterData', null);
      expect(App.YARNDefaultsProvider.get('ramPerContainer')).to.equal(null);
    });
    var tests = [
      {
        clusterData: {
          disk: 12,
          ram: 48,
          cpu: 12,
          hBaseInstalled: false
        },
        e: 2048
      },
      {
        clusterData: {
          disk: 12,
          ram: 16,
          cpu: 12,
          hBaseInstalled: true
        },
        e: 1024
      }
    ];

    tests.forEach(function(test) {
      it((test.hBaseInstalled?'With':'Without') + ' hBase', function() {
        App.YARNDefaultsProvider.set('clusterData', test.clusterData);
        expect(App.YARNDefaultsProvider.get('ramPerContainer')).to.equal(test.e);
      });
    });
  });

  describe('#getDefaults', function() {
    var tests = [
      {
        localDB: {},
        m: 'Empty localDB',
        e: null
      },
      {
        localDB: {
          "masterComponentHosts": []
        },
        m: 'localDB without hosts',
        e: null
      },
      {
        localDB: {
          "hosts": {}
        },
        m: 'localDB without masterComponentHosts amd slaveComponentHosts',
        e: null
      },
      {
        localDB: {
          "hosts": {
            "host1": {"name": "host1","cpu": 8,"memory": "25165824.00","disk_info": [{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'}]},
            "host2": {"name": "host2","cpu": 4,"memory": "25165824.00","disk_info": [{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'}]}
          },
          "masterComponentHosts": [],
          "slaveComponentHosts": [
            {
              "componentName": "NODEMANAGER",
              "hosts": [{"hostName": "host2"}]
            }
          ]
        },
        m: 'Without HBase',
        e: {
          'mapreduce.map.java.opts': '-Xmx1024m',
          'mapreduce.map.memory.mb': 1280,
          'mapreduce.reduce.java.opts': '-Xmx2048m',
          'mapreduce.reduce.memory.mb': 2560,
          'yarn.app.mapreduce.am.command-opts': '-Xmx2048m',
          'yarn.app.mapreduce.am.resource.mb': 2560,
          'yarn.nodemanager.resource.memory-mb': 20480,
          'yarn.scheduler.maximum-allocation-mb': 20480,
          'yarn.scheduler.minimum-allocation-mb': 2560,
          'mapreduce.task.io.sort.mb': 512
        }
      },
      {
        localDB: {
          "hosts": {
            "host1": {"name": "host1","cpu": 8,"memory": "25165824.00","disk_info": [{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'}]},
            "host2": {"name": "host2","cpu": 4,"memory": "12582912.00","disk_info": [{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'},{mountpoint:'/'}]}
          },
          "masterComponentHosts": [
            {"component": "HBASE_MASTER","hostName": "host1","serviceId": "HDFS"}
          ],
          "slaveComponentHosts": [
            {
              "componentName": "NODEMANAGER",
              "hosts": [{"hostName": "host2"}]
            }
          ]
        },
        m: 'With HBase',
        e: {
          'mapreduce.map.java.opts': '-Xmx410m',
          'mapreduce.map.memory.mb': 512,
          'mapreduce.reduce.java.opts': '-Xmx819m',
          'mapreduce.reduce.memory.mb': 1024,
          'yarn.app.mapreduce.am.command-opts': '-Xmx819m',
          'yarn.app.mapreduce.am.resource.mb': 1024,
          'yarn.nodemanager.resource.memory-mb': 8192,
          'yarn.scheduler.maximum-allocation-mb': 8192,
          'yarn.scheduler.minimum-allocation-mb': 1024,
          'mapreduce.task.io.sort.mb': 205
        }
      }
    ];
    tests.forEach(function(test) {
      it(test.m, function() {
        App.YARNDefaultsProvider.set('clusterData', null);
        var configs = App.YARNDefaultsProvider.getDefaults(test.localDB);

        for(var config in configs) {
          if (test.e) {
            expect(configs[config]).to.equal(test.e[config]);
          }
          else {
            expect(configs[config] == 0 || configs[config] == null).to.equal(true);
          }
        }
      });
    });
  });

});
