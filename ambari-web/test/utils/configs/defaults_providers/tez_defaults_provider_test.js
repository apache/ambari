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

describe('TezDefaultsProvider', function() {

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
          'mapreduce.map.java.opts': '-Xmx2048m',
          'mapreduce.map.memory.mb': 2560,
          'mapreduce.reduce.java.opts': '-Xmx2048m',
          'mapreduce.reduce.memory.mb': 2560,
          'yarn.app.mapreduce.am.command-opts': '-Xmx2048m',
          'yarn.app.mapreduce.am.resource.mb': 2560,
          'yarn.nodemanager.resource.memory-mb': 20480,
          'yarn.scheduler.maximum-allocation-mb': 20480,
          'yarn.scheduler.minimum-allocation-mb': 2560,
          'mapreduce.task.io.sort.mb': 1024,
          'tez.am.resource.memory.mb': 2560,
          'tez.am.java.opts': '-server -Xmx2048m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC'
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
          'mapreduce.map.java.opts': '-Xmx819m',
          'mapreduce.map.memory.mb': 1024,
          'mapreduce.reduce.java.opts': '-Xmx819m',
          'mapreduce.reduce.memory.mb': 1024,
          'yarn.app.mapreduce.am.command-opts': '-Xmx819m',
          'yarn.app.mapreduce.am.resource.mb': 1024,
          'yarn.nodemanager.resource.memory-mb': 8192,
          'yarn.scheduler.maximum-allocation-mb': 8192,
          'yarn.scheduler.minimum-allocation-mb': 1024,
          'mapreduce.task.io.sort.mb': 410,
          'tez.am.resource.memory.mb': 1024,
          'tez.am.java.opts': '-server -Xmx819m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC'
        }
      }
    ];
    tests.forEach(function(test) {
      describe(test.m, function() {
        var defaultsProvider = App.TezDefaultsProvider.create();
        defaultsProvider.set('clusterData', null);
        var configs = defaultsProvider.getDefaults(test.localDB);
        Em.keys(configs).forEach(function(config) {
          it(config, function() {
            if (test.e) {
              expect(configs[config]).to.equal(test.e[config]);
            } else {
              expect(configs[config] == 0 || configs[config] == null).to.equal(true);
            }
          });
        });
      });
    });
  });

});
