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

App.ServiceInfo = Ember.Object.extend({
  elementId: 'service',
  serviceName: '',
  displayName: '',
  isDisabled: '',
  isHidden: '',
  isSelected: 'true',
  description: ''
});


/*App.User = Em.Object.extend({
 username: null
 });*/

App.ServiceModel = Em.Object.extend({
  name:null,
  components:[]
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

App.Component = DS.Model.extend({
  componentName:DS.attr('string'),
  service:DS.belongsTo('App.Service'),
  host:DS.belongsTo('App.Host')
});

App.Component.FIXTURES = [
  {
    id:1,
    component_name:'NameNode',
    service_id:1,
    host_id:1
  },
  {
    id:2,
    component_name:'SNameNode',
    service_id:1,
    host_id:2
  },
  {
    id:3,
    component_name:'DataNode',
    service_id:1,
    host_id:2
  },
  {
    id:4,
    component_name:'100Tracker',
    service_id:2,
    host_id:4
  },
  {
    id:5,
    component_name:'jobTaskTracker',
    service_id:2,
    host_id:4
  }
];
// COMPONENTS:
//- HbaseMaster, 100 Region Servers
//Zookeeper - 3 Zookeeper Servers
//Oozie - Oozie Master
//Hive - Hive Metastore

App.Service = DS.Model.extend({
  serviceName:DS.attr('string'),
  label:DS.attr('string'),
  components:DS.hasMany('App.Component')
});

App.Service.FIXTURES = [
  {
    id:1,
    service_name:'hdfs',
    label:'HDFS',
    components:[1, 2, 3]
  },
  {
    id:2,
    service_name:'mapreduce',
    label:'MapReduce',
    components:[4, 5]
  },
  {
    id:3,
    service_name:'hbase',
    label:'HBase'
  },
  {
    id:4,
    service_name:'zookeeper',
    label:'Zookeeper'
  },
  {
    id:5,
    service_name:'oozie',
    label:'Oozie'
  },
  {
    id:6,
    service_name:'hive',
    label:'Hive'
  }
];

