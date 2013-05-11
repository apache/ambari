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

App.QuickLinks = DS.Model.extend({
  label: DS.attr('string'),
  url: DS.attr('string'),
  service_id: DS.attr('string')
});

App.QuickLinks.FIXTURES = [
  {
    id:1,
    label:'NameNode UI',
    url:'http://%@:50070/dfshealth.jsp',
    service_id: 'HDFS'
  },
  {
    id:2,
    label:'NameNode logs',
    url:'http://%@:50070/logs',
    service_id: 'HDFS'
  },
  {
    id:3,
    label:'NameNode JMX',
    url:'http://%@:50070/jmx',
    service_id: 'HDFS'
  },
  {
    id:4,
    label:'Thread Stacks',
    url:'http://%@:50070/stacks',
    service_id: 'HDFS'
  },
  {
    id:5,
    label:'JobTracker UI',
    url:'http://%@:50030/jobtracker.jsp',
    service_id: 'MAPREDUCE'
  },
  {
    id:6,
    label:'Scheduling Info',
    url:'http://%@:50030/scheduler',
    service_id: 'MAPREDUCE'
  },
  {
    id:7,
    label:'Running Jobs',
    url:'http://%@:50030/jobtracker.jsp#running_jobs',
    service_id: 'MAPREDUCE'
  },
  {
    id:8,
    label:'Retired Jobs',
    url:'http://%@:50030/jobtracker.jsp#retired_jobs',
    service_id: 'MAPREDUCE'
  },
  {
    id:9,
    label:'JobHistory Server',
    url:'http://%@:51111/jobhistoryhome.jsp',
    service_id: 'MAPREDUCE'
  },
  {
    id:10,
    label:'JobTracker Logs',
    url:'http://%@:50030/logs',
    service_id: 'MAPREDUCE'
  },
  {
    id:11,
    label:'JobTracker JMX',
    url:'http://%@:50030/jmx',
    service_id: 'MAPREDUCE'
  },
  {
    id:12,
    label:'Thread Stacks',
    url:'http://%@:50030/stacks',
    service_id: 'MAPREDUCE'
  },
  {
    id:13,
    label:'HBase Master UI',
    url:'http://%@:60010/master-status',
    service_id: 'HBASE'
  },
  {
    id:14,
    label:'HBase Logs',
    url:'http://%@:60010/logs',
    service_id: 'HBASE'
  },
  {
    id:15,
    label:'Zookeeper Info',
    url:'http://%@:60010/zk.jsp',
    service_id: 'HBASE'
  },
  {
    id:16,
    label:'HBase Master JMX',
    url:'http://%@:60010/jmx',
    service_id: 'HBASE'
  },
  {
    id:17,
    label:'Debug Dump',
    url:'http://%@:60010/dump',
    service_id: 'HBASE'
  },
  {
    id:18,
    label:'Thread Stacks',
    url:'http://%@:60010/stacks',
    service_id: 'HBASE'
  }
];
