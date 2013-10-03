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
  service_id: DS.attr('string'),
  template: DS.attr('string')
});

App.QuickLinks.FIXTURES = [
  {
    id:1,
    label:'NameNode UI',
    url:'%@://%@:50070/dfshealth.jsp',
    service_id: 'HDFS',
    template:'%@://%@:50070/dfshealth.jsp'
  },
  {
    id:2,
    label:'NameNode logs',
    url:'%@://%@:50070/logs',
    service_id: 'HDFS',
    template:'%@://%@:50070/logs'
  },
  {
    id:3,
    label:'NameNode JMX',
    url:'%@://%@:50070/jmx',
    service_id: 'HDFS',
    template:'%@://%@:50070/jmx'
  },
  {
    id:4,
    label:'Thread Stacks',
    url:'%@://%@:50070/stacks',
    service_id: 'HDFS',
    template:'%@://%@:50070/stacks'
  },
  {
    id:5,
    label:'JobTracker UI',
    url:'%@://%@:50030/jobtracker.jsp',
    service_id: 'MAPREDUCE',
    template:'%@://%@:50030/jobtracker.jsp'
  },
  {
    id:6,
    label:'Scheduling Info',
    url:'%@://%@:50030/scheduler',
    service_id: 'MAPREDUCE',
    template:'%@://%@:50030/scheduler'
  },
  {
    id:7,
    label:'Running Jobs',
    url:'%@://%@:50030/jobtracker.jsp#running_jobs',
    service_id: 'MAPREDUCE',
    template:'%@://%@:50030/jobtracker.jsp#running_jobs'
  },
  {
    id:8,
    label:'Retired Jobs',
    url:'%@://%@:50030/jobtracker.jsp#retired_jobs',
    service_id: 'MAPREDUCE',
    template:'%@://%@:50030/jobtracker.jsp#retired_jobs'
  },
  {
    id:9,
    label:'JobHistory Server',
    url:'%@://%@:51111/jobhistoryhome.jsp',
    service_id: 'MAPREDUCE',
    template:'%@://%@:51111/jobhistoryhome.jsp'
  },
  {
    id:10,
    label:'JobTracker Logs',
    url:'%@://%@:50030/logs',
    service_id: 'MAPREDUCE',
    template:'%@://%@:50030/logs'
  },
  {
    id:11,
    label:'JobTracker JMX',
    url:'%@://%@:50030/jmx',
    service_id: 'MAPREDUCE',
    template:'%@://%@:50030/jmx'
  },
  {
    id:12,
    label:'Thread Stacks',
    url:'%@://%@:50030/stacks',
    service_id: 'MAPREDUCE',
    template:'%@://%@:50030/stacks'
  },
  {
    id:13,
    label:'HBase Master UI',
    url:'%@://%@:60010/master-status',
    service_id: 'HBASE',
    template:'%@://%@:60010/master-status'
  },
  {
    id:14,
    label:'HBase Logs',
    url:'%@://%@:60010/logs',
    service_id: 'HBASE',
    template:'%@://%@:60010/logs'
  },
  {
    id:15,
    label:'Zookeeper Info',
    url:'%@://%@:60010/zk.jsp',
    service_id: 'HBASE',
    template:'%@://%@:60010/zk.jsp'
  },
  {
    id:16,
    label:'HBase Master JMX',
    url:'%@://%@:60010/jmx',
    service_id: 'HBASE',
    template:'%@://%@:60010/jmx'
  },
  {
    id:17,
    label:'Debug Dump',
    url:'%@://%@:60010/dump',
    service_id: 'HBASE',
    template:'%@://%@:60010/dump'
  },
  {
    id:18,
    label:'Thread Stacks',
    url:'%@://%@:60010/stacks',
    service_id: 'HBASE',
    template:'%@://%@:60010/stacks'
  },
  {
    id:19,
    label:'Oozie Web UI',
    url:'%@://%@:11000/oozie',
    service_id: 'OOZIE',
    template:'%@://%@:11000/oozie'
  },
  {
    id:20,
    label:'Ganglia Web UI',
    url:'%@://%@/ganglia',
    service_id: 'GANGLIA',
    template:'%@://%@/ganglia'

  },
  {
    id:21,
    label:'Nagios Web UI',
    url:'%@://%@/nagios',
    service_id: 'NAGIOS',
    template:'%@://%@/nagios'
  },
  {
    id:22,
    label:'Hue Web UI',
    url:'%@://%@/hue',
    service_id: 'HUE',
    template:'%@://%@/hue'
  },
  {
    id:23,
    label:'ResourceManager UI',
    url:'%@://%@:%@',
    service_id: 'YARN',
    template:'%@://%@:%@'
  },
  {
    id:24,
    label:'ResourceManager logs',
    url:'%@://%@:%@/logs',
    service_id: 'YARN',
    template:'%@://%@:%@/logs'
  },
  {
    id:25,
    label:'ResourceManager JMX',
    url:'%@://%@:%@/jmx',
    service_id: 'YARN',
    template:'%@://%@:%@/jmx'
  },
  {
    id:26,
    label:'Thread Stacks',
    url:'%@://%@:%@/stacks',
    service_id: 'YARN',
    template:'%@://%@:%@/stacks'
  },
  {
    id:27,
    label:'JobHistory UI',
    url:'%@://%@:19888',
    service_id: 'MAPREDUCE2',
    template:'%@://%@:19888'
  },
  {
    id:28,
    label:'JobHistory logs',
    url:'%@://%@:19888/logs',
    service_id: 'MAPREDUCE2',
    template:'%@://%@:19888/logs'
  },
  {
    id:29,
    label:'JobHistory JMX',
    url:'%@://%@:19888/jmx',
    service_id: 'MAPREDUCE2',
    template:'%@://%@:19888/jmx'
  },
  {
    id:30,
    label:'Thread Stacks',
    url:'%@://%@:19888/stacks',
    service_id: 'MAPREDUCE2',
    template:'%@://%@:19888/stacks'
  }
];
