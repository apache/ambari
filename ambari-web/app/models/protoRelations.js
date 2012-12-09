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

App.Service = DS.Model.extend({
  serviceName: DS.attr('string'),
  serviceAudit:DS.hasMany('App.ServiceAudit'),
  label:DS.attr('string'),//
  healthStatus:DS.attr('string'),
  workStatus:DS.attr('boolean'),//
  alerts:DS.hasMany('App.Alert'),
  quickLinks:DS.hasMany('App.QuickLinks'),
  components: DS.hasMany('App.ServiceComponent', { embedded: true }),
  //components:DS.hasMany('App.Component'),
  displayName: function() {
    switch (this.get('serviceName')) {
      case 'hdfs':
        return 'HDFS';
      case 'mapreduce':
        return 'MapReduce';
      case 'hbase':
        return 'HBase';
      case 'oozie':
        return 'Oozie';
      case 'hive':
        return 'Hive/HCatalog';
      case 'zookeeper':
        return 'ZooKeeper';
      case 'pig':
        return 'Pig';
      case 'sqoop':
        return 'Sqoop';
      case 'templeton':
        return 'Templeton';
      case 'ganglia':
        return 'Ganglia';
      case 'nagios':
        return 'Nagios';
    }
    return this.get('serviceName');
  }.property('serviceName')
});

App.Service.Health = {
  live:"LIVE",
  dead:"DEAD",
  start:"STARTING",
  stop:"STOPPING"
};

App.ServiceComponent = DS.Model.extend({
  componentName: DS.attr('string'),
  hostComponents: DS.hasMany('App.HostComponent1'),
  service: DS.belongsTo('App.Service1'),
  state : DS.attr('string'),
  host_name : DS.attr('string'),

  displayName: function() {
    return App.format.role(this.get('componentName'));
  }.property('componentName'),

  isMaster: function() {
    switch (this.get('componentName')) {
      case 'NAMENODE':
      case 'SNAMENODE':
      case 'JOBTRACKER':
      case 'ZOOKEEPER_SERVER':
      case 'HIVE_SERVER':
      case 'HBASE_MASTER':
      case 'NAGIOS_SERVER':
      case 'GANGLIA_SERVER':
      case 'OOZIE_SERVER':
      case 'TEMPLETON_SERVER':
        return true;
      default:
        return false;
    }
    return this.get('componentName');
  }.property('componentName')
});

App.HostComponent = DS.Model.extend({
  primaryKey: 'hostComponentId',
  hostComponentId: DS.attr('string'), // component_name + host_name
  state: DS.attr('string'),
  host: DS.belongsTo('App.Host'),
  hostName: DS.attr('string')
});

App.QuickLinks = DS.Model.extend({
  label:DS.attr('string'),
  url:DS.attr('string')
});

App.QuickLinks.FIXTURES = [
  {
    id:1,
    label:'NameNode UI',
    url:'http://%@:50070/dfshealth.jsp'
  },
  {
    id:2,
    label:'NameNode logs',
    url:'http://%@:50070/logs'
  },
  {
    id:3,
    label:'NameNode JMX',
    url:'http://%@:50070/jmx'
  },
  {
    id:4,
    label:'Thread Stacks',
    url:'http://%@:50070/stacks'
  },
  {
    id:5,
    label:'JobTracker UI',
    url:'http://%@:50030/jobtracker.jsp'
  },
  {
    id:6,
    label:'Scheduling Info',
    url:'http://%@:50030/scheduler'
  },
  {
    id:7,
    label:'Running Jobs',
    url:'http://%@:50030/jobtracker.jsp#running_jobs'
  },
  {
    id:8,
    label:'Retired Jobs',
    url:'http://%@:50030/jobtracker.jsp#retired_jobs'
  },
  {
    id:9,
    label:'JobHistory Server',
    url:'http://%@:51111/jobhistoryhome.jsp'
  },
  {
    id:10,
    label:'JobTracker Logs',
    url:'http://%@:50030/logs'
  },
  {
    id:11,
    label:'HBase Master UI',
    url:'http://%@:60010/master-status'
  },
  {
    id:12,
    label:'HBase Logs',
    url:'http://%@:60010/logs'
  },
  {
    id:13,
    label:'Zookeeper Info',
    url:'http://%@:60010/zk.jsp'
  },
  {
    id:14,
    label:'HBase Master JMX',
    url:'http://%@:60010/jmx'
  },
  {
    id:15,
    label:'Debug Dump',
    url:'http://%@:60010/dump'
  },
  {
    id:16,
    label:'Thread Stacks',
    url:'http://%@:60010/stacks'
  },
  {
    id:17,
    label:'JobTracker JMX',
    url:'http://%@:50030/jmx'
  },
  {
    id:18,
    label:'Thread Stacks',
    url:'http://%@:50030/stacks'
  }
];


// A hack to allow App.<model>.find() with the DS.FixtureAdapter
App.Service.FIXTURES = [];
App.ServiceComponent.FIXTURES = [];
App.HostComponent.FIXTURES = [];


