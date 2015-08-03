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

/**
 * Array of Objects
 * {
   *  hostProperty - hostName property name for current component
   *  componentName - master componentName
   *  serviceName - serviceName of component
   *  serviceUseThis - services that use hostname property of component(componentName)
   *  m(multiple) - true if can be more than one components installed on cluster
   * }
 */

module.exports = [
  {
    hostProperty: 'snamenode_host',
    componentName: 'SECONDARY_NAMENODE',
    serviceName: 'HDFS',
    serviceUseThis: []
  },
  {
    hostProperty: 'jobtracker_host',
    componentName: 'JOBTRACKER',
    serviceName: 'MAPREDUCE2',
    serviceUseThis: []
  },
  {
    hostProperty: 'hs_host',
    componentName: 'HISTORYSERVER',
    serviceName: 'MAPREDUCE2',
    serviceUseThis: ['YARN']
  },
  {
    hostProperty: 'ats_host',
    componentName: 'APP_TIMELINE_SERVER',
    serviceName: 'YARN',
    serviceUseThis: []
  },
  {
    hostProperty: 'rm_host',
    componentName: 'RESOURCEMANAGER',
    serviceName: 'YARN',
    serviceUseThis: []
  },
  {
    hostProperty: 'hivemetastore_host',
    componentName: 'HIVE_METASTORE',
    serviceName: 'HIVE',
    serviceUseThis: ['HIVE'],
    m: true
  },
  {
    hostProperty: 'hive_ambari_host',
    componentName: 'HIVE_SERVER',
    serviceName: 'HIVE',
    serviceUseThis: []
  },
  {
    hostProperty: 'oozieserver_host',
    componentName: 'OOZIE_SERVER',
    serviceName: 'OOZIE',
    serviceUseThis: [],
    m: true
  },
  {
    hostProperty: 'oozie_ambari_host',
    componentName: 'OOZIE_SERVER',
    serviceName: 'OOZIE',
    serviceUseThis: []
  },
  {
    hostProperty: 'hbasemaster_host',
    componentName: 'HBASE_MASTER',
    serviceName: 'HBASE',
    serviceUseThis: [],
    m: true
  },
  {
    hostProperty: 'webhcatserver_host',
    componentName: 'WEBHCAT_SERVER',
    serviceName: 'HIVE',
    serviceUseThis: [],
    m: true
  },
  {
    hostProperty: 'zookeeperserver_hosts',
    componentName: 'ZOOKEEPER_SERVER',
    serviceName: 'ZOOKEEPER',
    serviceUseThis: ['HBASE', 'HIVE'],
    m: true
  },
  {
    hostProperty: 'stormuiserver_host',
    componentName: 'STORM_UI_SERVER',
    serviceName: 'STORM',
    serviceUseThis: []
  },
  {
    hostProperty: 'drpcserver_host',
    componentName: 'DRPC_SERVER',
    serviceName: 'STORM',
    serviceUseThis: []
  },
  {
    hostProperty: 'storm_rest_api_host',
    componentName: 'STORM_REST_API',
    serviceName: 'STORM',
    serviceUseThis: []
  },
  {
    hostProperty: 'supervisor_hosts',
    componentName: 'SUPERVISOR',
    serviceName: 'STORM',
    serviceUseThis: [],
    m: true
  },
  {
    hostProperty: 'rangerserver_host',
    componentName: 'RANGER_ADMIN',
    serviceName: 'RANGER',
    serviceUseThis: [],
    m: true
  }
];
