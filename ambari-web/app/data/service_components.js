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

module.exports = new Ember.Set([

  {
    service_name: 'HDFS',
    component_name: 'NAMENODE',
    display_name: 'NameNode',
    isMaster: true,
    isClient: false,
    description: 'Master server that manages the file system namespace and regulates access to files by clients'
  },
  {
    service_name: 'HDFS',
    component_name: 'SECONDARY_NAMENODE',
    display_name: 'SNameNode',
    isMaster: true,
    isClient: false,
    description: 'Helper to the primary NameNode that is responsible for supporting periodic checkpoints of the HDFS metadata'
  },
  {
    service_name: 'HDFS',
    component_name: 'DATANODE',
    display_name: 'DataNode',
    isMaster: false,
    isClient: false,
    description: 'The slave for HDFS'
  },
  {
    service_name: 'HDFS',
    component_name: 'HDFS_CLIENT',
    display_name: 'HDFS Client',
    isMaster: false,
    isClient: true,
    description: 'Client component for HDFS'
  },
  {
    service_name: 'MAPREDUCE',
    component_name: 'JOBTRACKER',
    display_name: 'JobTracker',
    isMaster: true,
    isClient: false,
    description: 'Central Master service that pushes work (MR tasks) out to available TaskTracker nodes in the cluster'
  },
  {
    service_name: 'MAPREDUCE',
    component_name: 'HISTORYSERVER',
    display_name: 'History Server',
    isMaster: true,
    isClient: false,
    description: ''
  },
  {
    service_name: 'MAPREDUCE',
    component_name: 'TASKTRACKER',
    display_name: 'TaskTracker',
    isMaster: false,
    isClient: false,
    description: 'The slave for MapReduce'
  },
  {
    service_name: 'MAPREDUCE',
    component_name: 'MAPREDUCE_CLIENT',
    display_name: 'MapReduce Client',
    isMaster: false,
    isClient: true,
    description: 'Client component for MapReduce'
  },
  {
    service_name: 'MAPREDUCE2',
    component_name: 'MAPREDUCE2_CLIENT',
    display_name: 'MapReduce 2 Client',
    isMaster: false,
    isClient: true,
    description: ''
  },
  {
    service_name: 'MAPREDUCE2',
    component_name: 'HISTORYSERVER',
    display_name: 'History Server',
    isMaster: true,
    isClient: false,
    description: ''
  },
  {
    service_name: 'TEZ',
    component_name: 'TEZ_CLIENT',
    display_name: 'Tez Client',
    isMaster: false,
    isClient: true,
    description: ''
  },
  {
    service_name: 'YARN',
    component_name: 'RESOURCEMANAGER',
    display_name: 'ResourceManager',
    isMaster: true,
    isClient: false,
    description: ''
  },
  {
    service_name: 'YARN',
    component_name: 'YARN_CLIENT',
    display_name: 'YARN Client',
    isMaster: false,
    isClient: true,
    description: ''
  },
  // @todo uncomment after Application Timeline Server API implementation
//  {
//    service_name: 'YARN',
//    component_name: 'APP_TIMELINE_SERVER',
//    display_name: 'App Timeline Server',
//    isMaster: true,
//    isClient: false,
//    stackVersions: ['2.1.1'],
//    description: ''
//  },
  {
    service_name: 'YARN',
    component_name: 'NODEMANAGER',
    display_name: 'NodeManager',
    isMaster: false,
    isClient: false,
    description: ''
  },
  {
    service_name: 'ZOOKEEPER',
    component_name: 'ZOOKEEPER_SERVER',
    display_name: 'ZooKeeper',
    isMaster: true,
    isClient: false,
    description: ''
  },
  {
    service_name: 'ZOOKEEPER',
    component_name: 'ZOOKEEPER_CLIENT',
    display_name: 'ZooKeeper Client',
    isMaster: false,
    isClient: true,
    description: ''
  },
  {
    service_name: 'HBASE',
    component_name: 'HBASE_MASTER',
    display_name: 'HBase Master',
    isMaster: true,
    isClient: false,
    description: ''
  },
  {
    service_name: 'HBASE',
    component_name: 'HBASE_REGIONSERVER',
    display_name: 'RegionServer',
    isMaster: false,
    isClient: false,
    description: 'The slave for HBase'
  },
  {
    service_name: 'HBASE',
    component_name: 'HBASE_CLIENT',
    display_name: 'HBase Client',
    isMaster: false,
    isClient: true,
    description: 'The slave for HBase'
  },
  {
    service_name: 'PIG',
    component_name: 'PIG',
    display_name: 'Pig',
    isMaster: false,
    isClient: true,
    description: ''
  },
  {
    service_name: 'SQOOP',
    component_name: 'SQOOP',
    display_name: 'Sqoop',
    isMaster: false,
    isClient: true,
    description: ''
  },
  {
    service_name: 'OOZIE',
    component_name: 'OOZIE_SERVER',
    display_name: 'Oozie Server',
    isMaster: true,
    isClient: false,
    description: ''
  },
  {
    service_name: 'OOZIE',
    component_name: 'OOZIE_CLIENT',
    display_name: 'Oozie Client',
    isMaster: false,
    isClient: true,
    description: ''
  },
  {
    service_name: 'HIVE',
    component_name: 'HIVE_SERVER',
    display_name: 'HiveServer2',
    isMaster: true,
    isClient: false,
    description: ''
  },
  {
    service_name: 'HIVE',
    component_name: 'HIVE_METASTORE',
    display_name: 'Hive Metastore',
    isMaster: true,
    isClient: false,
    description: ''
  },
  {
    service_name: 'HIVE',
    component_name: 'HIVE_CLIENT',
    display_name: 'Hive Client',
    isMaster: false,
    isClient: true,
    description: ''
  },
  {
    service_name: 'HIVE',
    component_name: 'MYSQL_SERVER',
    display_name: 'MySQL Server for Hive',
    isMaster: false,
    isClient: false,
    description: ''
  },
  {
    service_name: 'HCATALOG',
    component_name: 'HCAT',
    display_name: 'HCat Client',
    isMaster: false,
    isClient: true,
    description: ''
  },
  {
    service_name: 'WEBHCAT',
    component_name: 'WEBHCAT_SERVER',
    display_name: 'WebHCat Server',
    isMaster: true,
    isClient: false,
    description: ''
  },
  {
    service_name: 'DASHBOARD',
    component_name: 'DASHBOARD',
    display_name: 'Monitoring Dashboard',
    isMaster: false,
    isClient: false,
    description: ''
  },
  {
    service_name: 'NAGIOS',
    component_name: 'NAGIOS_SERVER',
    display_name: 'Nagios Server',
    isMaster: true,
    isClient: false,
    description: ''
  },
  {
    service_name: 'GANGLIA',
    component_name: 'GANGLIA_SERVER',
    display_name: 'Ganglia Server',
    isMaster: true,
    isClient: false,
    description: ''
  },
  {
    service_name: 'GANGLIA',
    component_name: 'GANGLIA_MONITOR',
    display_name: 'Ganglia Slave',
    isMaster: false,
    isClient: false,
    description: ''
  },
  {
    service_name: 'KERBEROS',
    component_name: 'KERBEROS_SERVER',
    display_name: 'Kerberos Server',
    isMaster: true,
    isClient: false,
    description: ''
  },
  {
    service_name: 'KERBEROS',
    component_name: 'KERBEROS_ADMIN_CLIENT',
    display_name: 'Kerberos Admin Client',
    isMaster: false,
    isClient: true,
    description: ''
  },
  {
    service_name: 'KERBEROS',
    component_name: 'KERBEROS_CLIENT',
    display_name: 'Kerberos Client',
    isMaster: false,
    isClient: true,
    description: ''
  },
  {
    service_name: 'CLIENT',
    component_name: 'CLIENT',
    display_name: 'Client',
    isMaster: false,
    isClient: true,
    description: ''
  },
  {
    service_name: 'HUE',
    component_name: 'HUE_SERVER',
    display_name: 'Hue Server',
    isMaster: true,
    isClient: false,
    description: ''
  },
  { 
    service_name: 'GLUSTERFS',
    component_name: 'GLUSTERFS_CLIENT',
    display_name: 'GLUSTERFS Client', 
    isMaster: false, 
    isClient: true, 
    description: 'Client component for GLUSTERFS'
  },
  {
    service_name: 'FALCON',
    component_name: 'FALCON_SERVER',
    display_name: 'Falcon Server',
    isMaster: true,
    isClient: false,
    description: 'Falcon Server for mirroring'
  },
  {
    service_name: 'FALCON',
    component_name: 'FALCON_CLIENT',
    display_name: 'Falcon Client',
    isMaster: false,
    isClient: true,
    description: 'Falcon Client for mirroring'
  },
  {
    service_name: 'STORM',
    component_name: 'NIMBUS',
    display_name: 'Nimbus',
    isMaster: true,
    isClient: false,
    description: 'Master component for STORM'
  },
  {
    service_name: 'STORM',
    component_name: 'SUPERVISOR',
    display_name: 'Supervisor',
    isMaster: false,
    isClient: false,
    description: 'Slave component for STORM'
  },
  {
    service_name: 'STORM',
    component_name: 'STORM_UI_SERVER',
    display_name: 'Storm UI Server',
    isMaster: true,
    isClient: false,
    description: 'Master component for STORM'
  },
  {
    service_name: 'STORM',
    component_name: 'DRPC_SERVER',
    display_name: 'DRPC Server',
    isMaster: true,
    isClient: false,
    description: 'Master component for STORM'
  },
  {
    service_name: 'STORM',
    component_name: 'STORM_REST_API',
    display_name: 'Storm REST API Server',
    isMaster: true,
    isClient: false,
    description: 'Master component for STORM'
  }
]);

// @todo remove after Application Timeline Server API implementation
if (App.supports.appTimelineServer) {
  var appTimelineServerObj = {
    service_name: 'YARN',
    component_name: 'APP_TIMELINE_SERVER',
    display_name: 'App Timeline Server',
    isMaster: true,
    isClient: false,
    stackVersions: ['2.1'],
    description: ''
  };
  module.exports.push(appTimelineServerObj);
}
