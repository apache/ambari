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
require('models/service_config')

var configProperties = App.ConfigProperties.create();

module.exports = [
  {
    serviceName: 'HDFS',
    displayName: 'HDFS',
    filename: 'hdfs-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'NameNode'}),
      App.ServiceConfigCategory.create({ name: 'SNameNode'}),
      App.ServiceConfigCategory.create({ name: 'DataNode'}),
      App.ServiceConfigCategory.create({ name: 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced'})
    ],
    configs: configProperties.filterProperty('serviceName', 'HDFS')
  },

  {
    serviceName: 'MAPREDUCE',
    displayName: 'MapReduce',
    filename: 'mapred-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'JobTracker'}),
      App.ServiceConfigCategory.create({ name: 'TaskTracker'}),
      App.ServiceConfigCategory.create({ name: 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced'})
    ],
    configs: configProperties.filterProperty('serviceName', 'MAPREDUCE')
  },

  {
    serviceName: 'HIVE',
    displayName: 'Hive/HCat',
    filename: 'hive-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Hive Metastore'}),
      App.ServiceConfigCategory.create({ name: 'Advanced'})
    ],
    configs: configProperties.filterProperty('serviceName', 'HIVE')
  },

  {
    serviceName: 'WEBHCAT',
    displayName: 'WebHCat',
    filename: 'webhcat-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Advanced'})
    ],
    configs: configProperties.filterProperty('serviceName', 'WEBCHAT')
  },

  {
    serviceName: 'HBASE',
    displayName: 'HBase',
    filename: 'hbase-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'HBase Master'}),
      App.ServiceConfigCategory.create({ name: 'RegionServer'}),
      App.ServiceConfigCategory.create({ name: 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced'})
    ],
    configs: configProperties.filterProperty('serviceName', 'HBASE')
  },

  {
    serviceName: 'ZOOKEEPER',
    displayName: 'ZooKeeper',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'ZooKeeper Server'}),
      App.ServiceConfigCategory.create({ name: 'Advanced'})
    ],
    configs: configProperties.filterProperty('serviceName', 'ZOOKEEPER')
  },

  {
    serviceName: 'OOZIE',
    displayName: 'Oozie',
    filename: 'oozie-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Oozie Server'}),
      App.ServiceConfigCategory.create({ name: 'Advanced'})
    ],
    configs: configProperties.filterProperty('serviceName', 'OOZIE')
  },

  {
    serviceName: 'NAGIOS',
    displayName: 'Nagios',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'General'})
    ],
    configs: configProperties.filterProperty('serviceName', 'NAGIOS')
  },

  {
    serviceName: 'MISC',
    displayName: 'Misc',
    configCategories: [
      /*App.ServiceConfigCategory.create({ name: 'General'}),*/
      App.ServiceConfigCategory.create({ name: 'Users and Groups'})
    ],
    configs: configProperties.filterProperty('serviceName', 'MISC')
  }

]