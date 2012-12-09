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

App.Service1 = DS.Model.extend({
  //primaryKey : 'serviceName',
  serviceName: DS.attr('string'),
  description: DS.attr('string'),
  components: DS.hasMany('App.Service1Component', { embedded: true }),

  displayName: function() {
    switch (this.get('serviceName').toLowerCase()) {
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

App.Service1.Health = {
  live:"LIVE",
  dead:"DEAD",
  start:"STARTING",
  stop:"STOPPING"
};

App.Service1Component = DS.Model.extend({
  componentName: DS.attr('string'),
  hostComponents: DS.hasMany('App.HostComponent1'),
  service: DS.belongsTo('App.Service1'),
  state : DS.attr('string'),
  host_name : DS.attr('string'),

  displayName: function() {
    switch (this.get('componentName')) {
      case 'NAMENODE':
        return 'NameNode';
      case 'SNAMENODE':
        return 'SNameNode';
      case 'DATANODE':
        return 'DataNode';
      case 'HDFS_CLIENT':
        return 'HDFS Client';
      case 'JOBTRACKER':
        return 'JobTracker';
      case 'TASKTRACKER':
        return 'TaskTracker';
      case 'MAPREDUCE_CLIENT':
        return 'MapReduce Client';
      case 'ZOOKEEPER_SERVER':
        return 'ZooKeeper Server';
      case 'ZOOKEEPER_CLIENT':
        return 'ZooKeeper Client';
      case 'HIVE_SERVER':
        return 'Hive Server';
      case 'HIVE_CLIENT':
        return 'Hive Client';
      case 'HIVE_MYSQL':
        return 'Hive MySQL';
      case 'HBASE_MASTER':
        return 'HBase Master';
      case 'HBASE_REGIONSERVER':
        return 'RegionServer';
      case 'HBASE_CLIENT':
        return 'HBase Client';
      case 'PIG_CLIENT':
        return 'Pig Client';
      case 'SQOOP_CLIENT':
        return 'Sqoop Client';
      case 'TEMPLETON_SERVER':
        return 'Templeton Server';
      case 'TEMPLETON_CLIENT':
        return 'Templeton Client';
      case 'OOZIE_SERVER':
        return 'Oozie Server';
      case 'OOZIE_CLIENT':
        return 'Oozie Client';
      case 'NAGIOS_SERVER':
        return 'Nagios Server';
      case 'GANGLIA_SERVER':
        return 'Ganglia Collector';
      case 'GANGLIA_MONITOR':
        return 'Ganglia Monitor';
    }
    return this.get('componentName');
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

App.HostComponent1 = DS.Model.extend({
  primaryKey: 'hostComponentId',
  hostComponentId: DS.attr('string'), // component_name + host_name
  state: DS.attr('string'),
  host: DS.belongsTo('App.Host'),
  hostName: DS.attr('string')
});

// A hack to allow App.<model>.find() with the DS.FixtureAdapter
App.Service1.FIXTURES = [];
App.Service1Component.FIXTURES = [];
App.HostComponent1.FIXTURES = [];
