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
      App.ServiceConfigCategory.create({ name: 'NameNode', displayName : 'NameNode'}),
      App.ServiceConfigCategory.create({ name: 'SNameNode', displayName : 'Secondary Name Node'}),
      App.ServiceConfigCategory.create({ name: 'DataNode', displayName : 'DataNode'}),
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedCoreSite', displayName : 'Custom core-site.xml', siteFileName: 'core-site.xml', canAddProperty: true}),
      App.ServiceConfigCategory.create({ name: 'AdvancedHDFSSite', displayName : 'Custom hdfs-site.xml', siteFileName: 'hdfs-site.xml', canAddProperty: true})
    ],
    configs: configProperties.filterProperty('serviceName', 'HDFS')
  },

  {
    serviceName: 'MAPREDUCE',
    displayName: 'MapReduce',
    filename: 'mapred-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'JobTracker', displayName : 'JobTracker'}),
      App.ServiceConfigCategory.create({ name: 'TaskTracker', displayName : 'TaskTracker'}),
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'Capacity Scheduler', displayName : 'Capacity Scheduler'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedMapredSite', displayName : 'Custom mapred-site.xml', siteFileName: 'mapred-site.xml', canAddProperty: true})
    ],
    configs: configProperties.filterProperty('serviceName', 'MAPREDUCE')
  },

  {
    serviceName: 'HIVE',
    displayName: 'Hive/HCat',
    filename: 'hive-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Hive Metastore', displayName : 'Hive Metastore'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedHiveSite', displayName : 'Custom hive-site.xml', siteFileName: 'hive-site.xml', canAddProperty: true})
    ],
    configs: configProperties.filterProperty('serviceName', 'HIVE')
  },

  {
    serviceName: 'WEBHCAT',
    displayName: 'WebHCat',
    filename: 'webhcat-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedWebHCatSite', displayName : 'Custom webhcat-site.xml', siteFileName: 'webhcat-site.xml', canAddProperty: true})
    ],
    configs: configProperties.filterProperty('serviceName', 'WEBHCAT')
  },

  {
    serviceName: 'HBASE',
    displayName: 'HBase',
    filename: 'hbase-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'HBase Master', displayName : 'HBase Master'}),
      App.ServiceConfigCategory.create({ name: 'RegionServer', displayName : 'RegionServer'}),
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedHbaseSite', displayName : 'Custom hbase-site.xml', siteFileName: 'hbase-site.xml', canAddProperty: true})
    ],
    configs: configProperties.filterProperty('serviceName', 'HBASE')
  },

  {
    serviceName: 'ZOOKEEPER',
    displayName: 'ZooKeeper',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'ZooKeeper Server', displayName : 'ZooKeeper Server'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'})
    ],
    configs: configProperties.filterProperty('serviceName', 'ZOOKEEPER')
  },

  {
    serviceName: 'OOZIE',
    displayName: 'Oozie',
    filename: 'oozie-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Oozie Server', displayName : 'Oozie Server'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedOozieSite', displayName : 'Custom oozie-site.xml', siteFileName: 'oozie-site.xml', canAddProperty: true})
    ],
    configs: configProperties.filterProperty('serviceName', 'OOZIE')
  },

  {
    serviceName: 'NAGIOS',
    displayName: 'Nagios',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'})
    ],
    configs: configProperties.filterProperty('serviceName', 'NAGIOS')
  },

  {
    serviceName: 'MISC',
    displayName: 'Misc',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Users and Groups', displayName : 'Users and Groups'})
    ],
    configs: configProperties.filterProperty('serviceName', 'MISC')
  }

];
