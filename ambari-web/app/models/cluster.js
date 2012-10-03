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

App.User = Em.Object.extend({
  username: DS.attr("string")
});

App.User.FIXTURES = [
  {},
  {}
];

App.ClusterModel = Em.Object.extend({
    clusterName: null,
    hosts: [],
    services: []

});

// uncomment if column names are camelized in JSON (or fixture), rather than _ separated
/*
DS.Model.reopen({
    namingConvention: {
        keyToJSONKey: function(key) {
            return key;
        },

        foreignKey: function(key) {
            return key;
        }
    }
});
*/

App.Host = DS.Model.extend({
  hostName: DS.attr('string'),
  cluster: DS.belongsTo('App.Cluster'),
  components: DS.hasMany('App.Component'),
  cpu: DS.attr('string'),
  memory: DS.attr('string'),
  diskUsage: DS.attr('string'),
  loadAvg: DS.attr('string'),
  os: DS.attr('string'),
  ip: DS.attr('string'),
  isChecked: false,
  healthStatus: DS.attr('string'),
  workStatus: DS.attr('boolean')
});

App.Host.FIXTURES = [
  {
    id: 1,
    host_name: 'z_host1',
    cluster_id: 1,
    components:[1, 2, 3, 4],
    cpu: '2x2.5GHz',
    memory: '8GB',
    disk_usage: '40',
    load_avg: '0.2, 1.2, 2.4',
    ip: '123.123.123.123',
    health_status: 'LIVE',
    work_status: true
  },
  {
    id: 2,
    host_name: 'host2',
    cluster_id: 1,
    components:[4, 5],
    cpu: '2x2.5GHz',
    memory: '8GB',
    disk_usage: '20',
    load_avg: '0.2, 1.2, 2.4',
    ip: '255.255.255.255',
    health_status: 'DEAD',
    work_status: true
  },
  {
    id: 3,
    host_name: 'n_host3',
    cluster_id: 2,
    components:[4, 5, 7],
    health_status: 'LIVE',
    work_status: false
  },
  {
    id: 4,
    host_name: 'b_host4',
    cluster_id: 2,
    health_status: 'DEAD',
    work_status: false
  },
  {
    id: 5,
    host_name: 'host5',
    cluster_id: 1,
    components:[4, 5],
    cpu: '2x2.5GHz',
    memory: '8GB',
    disk_usage: '20',
    load_avg: '0.2, 1.2, 2.4',
    ip: '255.255.255.255',
    health_status: 'DEAD',
    work_status: true
  },
  {
    id: 6,
    host_name: 'a_host6',
    cluster_id: 1,
    components:[4, 5],
    cpu: '2x2.5GHz',
    memory: '8GB',
    disk_usage: '20',
    load_avg: '0.2, 1.2, 2.4',
    ip: '255.255.255.255',
    health_status: 'LIVE',
    work_status: false

  },
  {
    id: 7,
    host_name: 'host7',
    cluster_id: 1,
    components:[4, 5],
    cpu: '2x2.5GHz',
    memory: '8GB',
    disk_usage: '20',
    load_avg: '0.2, 1.2, 2.4',
    ip: '255.255.255.255',
    health_status: 'LIVE',
    work_status: true
  }
];

App.Cluster = DS.Model.extend({
    clusterName: DS.attr('string'),
    stackName: DS.attr('string'),
    hosts: DS.hasMany('App.Host')
});

App.Cluster.FIXTURES = [
  {
    id: 1,
    cluster_name: 'cluster1',
    stack_name: 'HDP',
    hosts: [1, 2]
  },

{
    id: 2,
    cluster_name: 'cluster2',
    stack_name: 'BigTop',
    hosts: [3]
  }
];

