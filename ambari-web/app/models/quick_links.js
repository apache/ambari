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
var portRegex = '\\w*:(\\d+)';

App.QuickLinks = DS.Model.extend({
  label: DS.attr('string'),
  url: DS.attr('string'),
  service_id: DS.attr('string'),
  template: DS.attr('string'),
  http_config: DS.attr('string'),
  https_config: DS.attr('string'),
  site: DS.attr('string'),
  regex: DS.attr('string'),
  default_http_port: DS.attr('number'),
  default_https_port: DS.attr('number')
});


App.QuickLinks.FIXTURES = [
  {
    id:1,
    label:'NameNode UI',
    url:'%@://%@:%@',
    service_id: 'HDFS',
    template:'%@://%@:%@',
    http_config: 'dfs.namenode.http-address',
    https_config: 'dfs.namenode.https-address',
    site: 'hdfs-site',
    regex: portRegex,
    default_http_port: 50070,
    default_https_port: 50470
  },
  {
    id:2,
    label:'NameNode logs',
    url:'%@://%@:%@/logs',
    service_id: 'HDFS',
    template:'%@://%@:%@/logs',
    http_config: 'dfs.namenode.http-address',
    https_config: 'dfs.namenode.https-address',
    site: 'hdfs-site',
    regex: portRegex,
    default_http_port: 50070,
    default_https_port: 50470
  },
  {
    id:3,
    label:'NameNode JMX',
    url:'%@://%@:%@/jmx',
    service_id: 'HDFS',
    template:'%@://%@:%@/jmx',
    http_config: 'dfs.namenode.http-address',
    https_config: 'dfs.namenode.https-address',
    site: 'hdfs-site',
    regex: portRegex,
    default_http_port: 50070,
    default_https_port: 50470
  },
  {
    id:4,
    label:'Thread Stacks',
    url:'%@://%@:%@/stacks',
    service_id: 'HDFS',
    template:'%@://%@:%@/stacks',
    http_config: 'dfs.namenode.http-address',
    https_config: 'dfs.namenode.https-address',
    site: 'hdfs-site',
    regex: portRegex,
    default_http_port: 50070,
    default_https_port: 50470
  },
  {
    id:5,
    label:'JobTracker UI',
    url:'%@://%@:%@/jobtracker.jsp',
    service_id: 'MAPREDUCE',
    template:'%@://%@:%@/jobtracker.jsp',
    http_config: 'mapred.job.tracker.http.address',
    https_config: 'mapred.job.tracker.https.address',
    site: 'mapred-site',
    regex: portRegex,
    default_http_port: 50030,
    default_https_port: 50035
  },
  {
    id:6,
    label:'Scheduling Info',
    url:'%@://%@:%@/scheduler',
    service_id: 'MAPREDUCE',
    template:'%@://%@:%@/scheduler',
    http_config: 'mapred.job.tracker.http.address',
    https_config: 'mapred.job.tracker.https.address',
    site: 'mapred-site',
    regex: portRegex,
    default_http_port: 50030,
    default_https_port: 50035
  },
  {
    id:7,
    label:'Running Jobs',
    url:'%@://%@:%@/jobtracker.jsp#running_jobs',
    service_id: 'MAPREDUCE',
    template:'%@://%@:%@/jobtracker.jsp#running_jobs',
    http_config: 'mapred.job.tracker.http.address',
    https_config: 'mapred.job.tracker.https.address',
    site: 'mapred-site',
    regex: portRegex,
    default_http_port: 50030,
    default_https_port: 50035
  },
  {
    id:8,
    label:'Retired Jobs',
    url:'%@://%@:%@/jobtracker.jsp#retired_jobs',
    service_id: 'MAPREDUCE',
    template:'%@://%@:%@/jobtracker.jsp#retired_jobs',
    http_config: 'mapred.job.tracker.http.address',
    https_config: 'mapred.job.tracker.https.address',
    site: 'mapred-site',
    regex: portRegex,
    default_http_port: 50030,
    default_https_port: 50035
  },
  {
    id:9,
    label:'JobHistory Server',
    url:'%@://%@:%@/jobhistoryhome.jsp',
    service_id: 'MAPREDUCE',
    template:'%@://%@:%@/jobhistoryhome.jsp',
    http_config: 'mapreduce.history.server.http.address',
    site: 'mapred-site',
    regex: portRegex,
    default_http_port: 51111
  },

  {
    id:10,
    label:'JobTracker Logs',
    url:'%@://%@:%@/logs',
    service_id: 'MAPREDUCE',
    template:'%@://%@:%@/logs',
    http_config: 'mapred.job.tracker.http.address',
    https_config: 'mapred.job.tracker.https.address',
    site: 'mapred-site',
    regex: portRegex,
    default_http_port: 50030,
    default_https_port: 50035
  },
  {
    id:11,
    label:'JobTracker JMX',
    url:'%@://%@:%@/jmx',
    service_id: 'MAPREDUCE',
    template:'%@://%@:%@/jmx',
    http_config: 'mapred.job.tracker.http.address',
    https_config: 'mapred.job.tracker.https.address',
    site: 'mapred-site',
    regex: portRegex,
    default_http_port: 50030,
    default_https_port: 50035
  },
  {
    id:12,
    label:'Thread Stacks',
    url:'%@://%@:%@/stacks',
    service_id: 'MAPREDUCE',
    template:'%@://%@:%@/stacks',
    http_config: 'mapred.job.tracker.http.address',
    https_config: 'mapred.job.tracker.https.address',
    site: 'mapred-site',
    regex: portRegex,
    default_http_port: 50030,
    default_https_port: 50035
  },
  {
    id:13,
    label:'HBase Master UI',
    url:'%@://%@:%@/master-status',
    service_id: 'HBASE',
    template:'%@://%@:%@/master-status',
    http_config: 'hbase.master.info.port',
    site: 'hbase-site',
    regex: '^(\\d+)$',
    default_http_port: 60010
  },
  {
    id:14,
    label:'HBase Logs',
    url:'%@://%@:60010/logs',
    service_id: 'HBASE',
    template:'%@://%@:%@/logs',
    http_config: 'hbase.master.info.port',
    site: 'hbase-site',
    regex: '^(\\d+)$',
    default_http_port: 60010
  },
  {
    id:15,
    label:'Zookeeper Info',
    url:'%@://%@:60010/zk.jsp',
    service_id: 'HBASE',
    template:'%@://%@:%@/zk.jsp',
    http_config: 'hbase.master.info.port',
    site: 'hbase-site',
    regex: '^(\\d+)$',
    default_http_port: 60010
  },
  {
    id:16,
    label:'HBase Master JMX',
    url:'%@://%@:60010/jmx',
    service_id: 'HBASE',
    template:'%@://%@:%@/jmx',
    http_config: 'hbase.master.info.port',
    site: 'hbase-site',
    regex: '^(\\d+)$',
    default_http_port: 60010
  },
  {
    id:17,
    label:'Debug Dump',
    url:'%@://%@:%@/dump',
    service_id: 'HBASE',
    template:'%@://%@:%@/dump',
    http_config: 'hbase.master.info.port',
    site: 'hbase-site',
    regex: '^(\\d+)$',
    default_http_port: 60010
  },
  {
    id:18,
    label:'Thread Stacks',
    url:'%@://%@:%@/stacks',
    service_id: 'HBASE',
    template:'%@://%@:%@/stacks',
    http_config: 'hbase.master.info.port',
    site: 'hbase-site',
    regex: '^(\\d+)$',
    default_http_port: 60010
  },
  {
    id:19,
    label:'Oozie Web UI',
    url:'%@://%@:%@/oozie',
    service_id: 'OOZIE',
    template:'%@://%@:%@/oozie',
    http_config: 'oozie.base.url',
    site: 'oozie-site',
    regex: portRegex,
    default_http_port: 11000
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
    template:'%@://%@:%@',
    http_config: 'yarn.resourcemanager.webapp.address',
    https_config: 'yarn.resourcemanager.webapp.https.address',
    site: 'yarn-site',
    regex: portRegex,
    default_http_port: 8088,
    default_https_port: 8090

  },
  {
    id:24,
    label:'ResourceManager logs',
    url:'%@://%@:%@/logs',
    service_id: 'YARN',
    template:'%@://%@:%@/logs',
    http_config: 'yarn.resourcemanager.webapp.address',
    https_config: 'yarn.resourcemanager.webapp.https.address',
    site: 'yarn-site',
    regex: portRegex,
    default_http_port: 8088,
    default_https_port: 8090
  },
  {
    id:25,
    label:'ResourceManager JMX',
    url:'%@://%@:%@/jmx',
    service_id: 'YARN',
    template:'%@://%@:%@/jmx',
    http_config: 'yarn.resourcemanager.webapp.address',
    https_config: 'yarn.resourcemanager.webapp.https.address',
    site: 'yarn-site',
    regex: portRegex,
    default_http_port: 8088,
    default_https_port: 8090
  },
  {
    id:26,
    label:'Thread Stacks',
    url:'%@://%@:%@/stacks',
    service_id: 'YARN',
    template:'%@://%@:%@/stacks',
    http_config: 'yarn.resourcemanager.webapp.address',
    https_config: 'yarn.resourcemanager.webapp.https.address',
    site: 'yarn-site',
    regex: portRegex,
    default_http_port: 8088,
    default_https_port: 8090
  },
  {
    id:27,
    label:'JobHistory UI',
    url:'%@://%@:%@',
    service_id: 'MAPREDUCE2',
    template:'%@://%@:%@',
    http_config: 'mapreduce.jobhistory.webapp.address',
    https_config: 'mapreduce.jobhistory.webapp.https.address',
    site: 'mapred-site',
    regex: portRegex,
    default_http_port: 19888
  },
  {
    id:28,
    label:'JobHistory logs',
    url:'%@://%@:%@/logs',
    service_id: 'MAPREDUCE2',
    template:'%@://%@:%@/logs',
    http_config: 'mapreduce.jobhistory.webapp.address',
    https_config: 'mapreduce.jobhistory.webapp.https.address',
    site: 'mapred-site',
    regex: portRegex,
    default_http_port: 19888
  },
  {
    id:29,
    label:'JobHistory JMX',
    url:'%@://%@:%@/jmx',
    service_id: 'MAPREDUCE2',
    template:'%@://%@:%@/jmx',
    http_config: 'mapreduce.jobhistory.webapp.address',
    https_config: 'mapreduce.jobhistory.webapp.https.address',
    site: 'mapred-site',
    regex: portRegex,
    default_http_port: 19888
  },
  {
    id:30,
    label:'Thread Stacks',
    url:'%@://%@:%@/stacks',
    service_id: 'MAPREDUCE2',
    template:'%@://%@:%@/stacks',
    http_config: 'mapreduce.jobhistory.webapp.address',
    https_config: 'mapreduce.jobhistory.webapp.https.address',
    site: 'mapred-site',
    regex: portRegex,
    default_http_port: 19888
  },
  {
    id:31,
    label:'Storm UI',
    url:'%@://%@:%@/',
    service_id: 'STORM',
    template:'%@://%@:%@/',
    http_config: 'stormuiserver_host',
    https_config: 'stormuiserver_host',
    site: 'storm-site',
    regex: portRegex,
    default_http_port: 8744
  },
  {
    id:32,
    label:'Falcon Web UI',
    url:'%@://%@:%@/',
    service_id: 'FALCON',
    template:'%@://%@:%@/',
    http_config: 'falcon_port',
    site: 'falcon-env',
    regex: '^(\\d+)$',
    default_http_port: 15000
  }
];
