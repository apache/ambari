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

App.Cluster = DS.Model.extend({
  clusterName: DS.attr('string'),
  stackName: DS.attr('string'),
  hosts: DS.hasMany('App.Host'),
  racks: DS.hasMany('App.Rack'),
  maxHostsPerRack: DS.attr('number')
});

App.Cluster.FIXTURES = [
  {
    id: 1,
    cluster_name: 'cluster1',
    stack_name: 'HDP',
    hosts: [1, 2, 3, 4],
    racks: [1, 2, 3, 4, 5, 6],
    max_hosts_per_rack: 10
  },
  {
    id: 2,
    cluster_name: 'cluster2',
    stack_name: 'BigTop',
    hosts: [5, 6, 7]
  }
];