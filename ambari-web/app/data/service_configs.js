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
require('models/service_config');

var configProperties = App.ConfigProperties.create();

module.exports = [
  {
    serviceName: 'HDFS',
    displayName: 'HDFS',
    filename: 'hdfs-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'NameNode', displayName : 'NameNode', hostComponentNames : ['NAMENODE']}),
      App.ServiceConfigCategory.create({ name: 'SNameNode', displayName : 'Secondary Name Node', hostComponentNames : ['SECONDARY_NAMENODE']}),
      App.ServiceConfigCategory.create({ name: 'DataNode', displayName : 'DataNode', hostComponentNames : ['DATANODE']}),
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedCoreSite', displayName : 'Custom core-site.xml', siteFileName: 'core-site.xml', canAddProperty: true}),
      App.ServiceConfigCategory.create({ name: 'AdvancedHDFSSite', displayName : 'Custom hdfs-site.xml', siteFileName: 'hdfs-site.xml', canAddProperty: true})
    ],
    sites: ['global', 'core-site', 'hdfs-site'],
    configs: configProperties.filterProperty('serviceName', 'HDFS')
  },

  {
    serviceName: 'MAPREDUCE',
    displayName: 'MapReduce',
    filename: 'mapred-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'JobTracker', displayName : 'JobTracker', hostComponentNames : ['JOBTRACKER']}),
      App.ServiceConfigCategory.create({ name: 'TaskTracker', displayName : 'TaskTracker', hostComponentNames : ['TASKTRACKER']}),
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'CapacityScheduler', displayName : 'Capacity Scheduler', isCapacityScheduler : true, isCustomView: true, siteFileName: 'capacity-scheduler.xml', siteFileNames: ['capacity-scheduler.xml', 'mapred-queue-acls.xml'], canAddProperty: true}),
      App.ServiceConfigCategory.create({ name: 'AdvancedMapredSite', displayName : 'Custom mapred-site.xml', siteFileName: 'mapred-site.xml', canAddProperty: true})
    ],
    sites: ['global', 'core-site', 'mapred-site', 'capacity-scheduler', 'mapred-queue-acls'],
    configs: configProperties.filterProperty('serviceName', 'MAPREDUCE')
  },

  {
    serviceName: 'MAPREDUCE2',
    displayName: 'MapReduce 2',
    filename: 'mapred-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'HistoryServer', displayName : 'History Server', hostComponentNames : ['HISTORYSERVER']}),
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedMapredSite', displayName : 'Custom mapred-site.xml', siteFileName: 'mapred-site.xml', canAddProperty: true})
    ],
    sites: ['core-site', 'mapred-site', 'mapred-queue-acls'],
    configs: configProperties.filterProperty('serviceName', 'MAPREDUCE2')
  },

  {
    serviceName: 'YARN',
    displayName: 'YARN',
    filename: 'yarn-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'ResourceManager', displayName : 'Resource Manager', hostComponentNames : ['RESOURCEMANAGER']}),
      App.ServiceConfigCategory.create({ name: 'NodeManager', displayName : 'Node Manager', hostComponentNames : ['NODEMANAGER']}),
      App.ServiceConfigCategory.create({ name: 'CapacityScheduler', displayName : 'Capacity Scheduler', isCapacityScheduler : true, isCustomView: true, siteFileName: 'capacity-scheduler.xml', siteFileNames: ['capacity-scheduler.xml', 'mapred-queue-acls.xml'], canAddProperty: true}),
      App.ServiceConfigCategory.create({ name: 'AdvancedYARNSite', displayName : 'Custom yarn-site.xml', siteFileName: 'yarn-site.xml', canAddProperty: true})
    ],
    sites: ['core-site', 'yarn-site', 'capacity-scheduler'],
    configs: configProperties.filterProperty('serviceName', 'MAPREDUCE2')
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
    sites: ['global', 'hive-site'],
    configs: configProperties.filterProperty('serviceName', 'HIVE')
  },

  {
    serviceName: 'WEBHCAT',
    displayName: 'WebHCat',
    filename: 'webhcat-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'WebHCat Server', displayName : 'WebHCat Server'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedWebHCatSite', displayName : 'Custom webhcat-site.xml', siteFileName: 'webhcat-site.xml', canAddProperty: true})
    ],
    sites: ['global', 'webhcat-site'],
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
    sites: ['global', 'hbase-site'],
    configs: configProperties.filterProperty('serviceName', 'HBASE')
  },

  {
    serviceName: 'ZOOKEEPER',
    displayName: 'ZooKeeper',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'ZooKeeper Server', displayName : 'ZooKeeper Server'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'})
    ],
    sites: ['global'],
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
    sites: ['global', 'oozie-site'],
    configs: configProperties.filterProperty('serviceName', 'OOZIE')
  },

  {
    serviceName: 'NAGIOS',
    displayName: 'Nagios',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'})
    ],
    sites: ['global'],
    configs: configProperties.filterProperty('serviceName', 'NAGIOS')
  },

  {
    serviceName: 'HUE',
    displayName: 'Hue',
    filename: 'hue-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Hue Server', displayName : 'Hue Server'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'})
    ],
    sites: ['hue-site'],
    configs: configProperties.filterProperty('serviceName', 'HUE')
  },

  {
    serviceName: 'MISC',
    displayName: 'Misc',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Users and Groups', displayName : 'Users and Groups'})
    ],
    sites: ['global'],
    configs: configProperties.filterProperty('serviceName', 'MISC')
  }

];
