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
require('utils/configs/defaults_providers/yarn_defaults_provider');
require('utils/configs/defaults_providers/tez_defaults_provider');
require('utils/configs/defaults_providers/hive_defaults_provider');
require('utils/configs/defaults_providers/storm_defaults_provider');
require('utils/configs/defaults_providers/oozie_defaults_provider');
require('utils/configs/defaults_providers/user_defaults_provider');
require('utils/configs/validators/yarn_configs_validator');
require('utils/configs/validators/hive_configs_validator');
require('utils/configs/validators/tez_configs_validator');
require('utils/configs/validators/mapreduce2_configs_validator');
require('utils/configs/validators/storm_configs_validator');
require('utils/configs/validators/user_configs_validator');

module.exports = [
  {
    serviceName: 'HDFS',
    displayName: 'HDFS',
    filename: 'hdfs-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'NameNode', displayName : 'NameNode', hostComponentNames : ['NAMENODE']}),
      App.ServiceConfigCategory.create({ name: 'SNameNode', displayName : 'Secondary NameNode', hostComponentNames : ['SECONDARY_NAMENODE']}),
      App.ServiceConfigCategory.create({ name: 'DataNode', displayName : 'DataNode', hostComponentNames : ['DATANODE']}),
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedCoreSite', displayName : 'Custom core-site.xml', siteFileName: 'core-site.xml', canAddProperty: true}),
      App.ServiceConfigCategory.create({ name: 'AdvancedHDFSSite', displayName : 'Custom hdfs-site.xml', siteFileName: 'hdfs-site.xml', canAddProperty: true}),
      App.ServiceConfigCategory.create({ name: 'AdvancedHDFSLog4j', displayName : 'Custom log4j.properties', siteFileName: 'hdfs-log4j.xml', canAddProperty: false})
    ],
    sites: ['global', 'core-site', 'hdfs-site', 'hdfs-log4j'],
    configs: []
  },
  {
    serviceName: 'GLUSTERFS',
    displayName: 'GLUSTERFS',
    filename: 'core-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'})
    ],
    sites: ['core-site'],
    configs: []
  },

  {
    serviceName: 'MAPREDUCE',
    displayName: 'MapReduce',
    filename: 'mapred-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'HistoryServer', displayName : 'History Server', hostComponentNames : ['HISTORYSERVER']}),
      App.ServiceConfigCategory.create({ name: 'JobTracker', displayName : 'JobTracker', hostComponentNames : ['JOBTRACKER']}),
      App.ServiceConfigCategory.create({ name: 'TaskTracker', displayName : 'TaskTracker', hostComponentNames : ['TASKTRACKER']}),
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedMapredSite', displayName : 'Custom mapred-site.xml', siteFileName: 'mapred-site.xml', canAddProperty: true}),
      App.ServiceConfigCategory.create({ name: 'AdvancedMapredLog4j', displayName : 'Custom log4j.properties', siteFileName: 'mapreduce-log4j.xml', canAddProperty: false})
    ],
    sites: ['global', 'mapred-site', 'mapred-queue-acls', 'mapreduce-log4j'],
    configs: []
  },

  {
    serviceName: 'YARN',
    displayName: 'YARN',
    configsValidator: App.YARNConfigsValidator,
    defaultsProviders: [App.YARNDefaultsProvider.create()],
    filename: 'yarn-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'ResourceManager', displayName : 'Resource Manager', hostComponentNames : ['RESOURCEMANAGER']}),
      App.ServiceConfigCategory.create({ name: 'NodeManager', displayName : 'Node Manager', hostComponentNames : ['NODEMANAGER']}),
      App.ServiceConfigCategory.create({ name: 'AppTimelineServer', displayName : 'Application Timeline Server', hostComponentNames : ['APP_TIMELINE_SERVER']}),
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'CapacityScheduler', displayName : 'Scheduler', isCapacityScheduler : true, isCustomView: true, siteFileName: 'capacity-scheduler.xml', siteFileNames: ['capacity-scheduler.xml', 'mapred-queue-acls.xml'], canAddProperty: App.supports.capacitySchedulerUi}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedYARNSite', displayName : 'Custom yarn-site.xml', siteFileName: 'yarn-site.xml', canAddProperty: true}),
      App.ServiceConfigCategory.create({ name: 'AdvancedYARNLog4j', displayName : 'Custom log4j.properties', siteFileName: 'yarn-log4j.xml', canAddProperty: false})
    ],
    sites: ['global', 'yarn-site', 'capacity-scheduler', 'yarn-log4j'],
    configs: []
  },

  {
    serviceName: 'MAPREDUCE2',
    displayName: 'MapReduce 2',
    filename: 'mapred-site',
    configsValidator: App.MapReduce2ConfigsValidator,
    defaultsProviders: [App.YARNDefaultsProvider.create()],
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'HistoryServer', displayName : 'History Server', hostComponentNames : ['HISTORYSERVER']}),
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedMapredSite', displayName : 'Custom mapred-site.xml', siteFileName: 'mapred-site.xml', canAddProperty: true}),
      App.ServiceConfigCategory.create({ name: 'AdvancedMapredLog4j', displayName : 'Custom log4j.properties', siteFileName: 'mapreduce2-log4j.xml', canAddProperty: false})
    ],
    sites: ['global', 'mapred-site', 'mapred-queue-acls', 'mapreduce2-log4j'],
    configs: []
  },

  {
    serviceName: 'HIVE',
    displayName: 'Hive',
    filename: 'hive-site',
    configsValidator: App.HiveConfigsValidator,
    defaultsProviders: [App.HiveDefaultsProvider.create()],
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Hive Metastore', displayName : 'Hive Metastore'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedHiveSite', displayName : 'Custom hive-site.xml', siteFileName: 'hive-site.xml', canAddProperty: true}),
      App.ServiceConfigCategory.create({ name: 'AdvancedHiveLog4j', displayName : 'Custom log4j.properties', siteFileName: 'hive-log4j.xml', canAddProperty: false}),
      App.ServiceConfigCategory.create({ name: 'AdvancedHiveExecLog4j', displayName : 'Custom hive-exec-log4j', siteFileName: 'hive-exec-log4j.xml', canAddProperty: false})
    ],
    sites: ['global', 'hive-site', 'hive-log4j', 'hive-exec-log4j'],
    configs: []
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
    configs: []
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
      App.ServiceConfigCategory.create({ name: 'AdvancedHbaseSite', displayName : 'Custom hbase-site.xml', siteFileName: 'hbase-site.xml', canAddProperty: true}),
      App.ServiceConfigCategory.create({ name: 'AdvancedHbaseLog4j', displayName : 'Custom log4j.properties', siteFileName: 'hbase-log4j.xml', canAddProperty: false})
    ],
    sites: ['global', 'hbase-site', 'hbase-log4j'],
    configs: []
  },

  {
    serviceName: 'ZOOKEEPER',
    displayName: 'ZooKeeper',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'ZooKeeper Server', displayName : 'ZooKeeper Server'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedZooLog4j', displayName : 'Custom log4j.properties', siteFileName: 'zookeeper-log4j.xml', canAddProperty: false}),
      App.ServiceConfigCategory.create({ name: 'AdvancedZooCfg', displayName : 'Custom zoo.cfg', siteFileName: 'zoo.cfg', canAddProperty: true})
    ],
    sites: ['global', 'zookeeper-log4j', 'zoo.cfg'],
    configs: []
  },

  {
    serviceName: 'OOZIE',
    displayName: 'Oozie',
    defaultsProviders: [App.OOZIEDefaultsProvider.create()],
    filename: 'oozie-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Oozie Server', displayName : 'Oozie Server'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedOozieSite', displayName : 'Custom oozie-site.xml', siteFileName: 'oozie-site.xml', canAddProperty: true}),
      App.ServiceConfigCategory.create({ name: 'AdvancedOozieLog4j', displayName : 'Custom log4j.properties', siteFileName: 'oozie-log4j.xml', canAddProperty: false})
    ],
    sites: ['global', 'oozie-site', 'oozie-log4j'],
    configs: []
  },

  {
    serviceName: 'NAGIOS',
    displayName: 'Nagios',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'})
    ],
    sites: ['global'],
    configs: []
  },

  {
    serviceName: 'GANGLIA',
    displayName: 'Ganglia',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'})
    ],
    sites: ['global'],
    configs: []
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
    configs: []
  },

  {
    serviceName: 'PIG',
    displayName: 'Pig',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedPigLog4j', displayName : 'Custom log4j.properties', siteFileName: 'pig-log4j.xml', canAddProperty: false})
    ],
    sites: ['pig-log4j'],
    configs: []
  },
  {
    serviceName: 'FALCON',
    displayName: 'Falcon',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Falcon', displayName : 'Falcon Server'}),
      App.ServiceConfigCategory.create({ name: 'Falcon - Oozie integration', displayName : 'Falcon - Oozie integration'}),
      App.ServiceConfigCategory.create({ name: 'FalconStartupSite', displayName : 'Falcon startup.properties'}),
      App.ServiceConfigCategory.create({ name: 'FalconRuntimeSite', displayName : 'Falcon runtime.properties'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedFalconStartupSite', displayName : 'Custom startup.properties', siteFileName: 'falcon-startup.properties.xml', canAddProperty: true}),
      App.ServiceConfigCategory.create({ name: 'AdvancedFalconRuntimeSite', displayName : 'Custom runtime.properties', siteFileName: 'falcon-runtime.properties.xml', canAddProperty: true})
    ],
    sites: ['global', 'oozie-site','falcon-startup.properties', 'falcon-runtime.properties'],
    configs: []
  },

  {
    serviceName: 'STORM',
    displayName: 'Storm',
    configsValidator: App.STORMConfigsValidator,
    defaultsProviders: [App.STORMDefaultsProvider.create()],
    filename: 'storm-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Nimbus', displayName : 'Nimbus'}),
      App.ServiceConfigCategory.create({ name: 'Supervisor', displayName : 'Supervisor'}),
      App.ServiceConfigCategory.create({ name: 'StormUIServer', displayName : 'Storm UI Server'}),
      App.ServiceConfigCategory.create({ name: 'StormRestApi', displayName : 'Storm REST API Server'}),
      App.ServiceConfigCategory.create({ name: 'DRPCServer', displayName : 'DRPC Server'}),
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedStormSite', displayName : 'Custom storm.yaml', siteFileName: 'storm-site.xml', canAddProperty: true})
    ],
    sites: ['global', 'storm-site'],
    configs: []
  },
  {
    serviceName: 'TEZ',
    displayName: 'Tez',
    filename: 'tez-site',
    configsValidator: App.TezConfigsValidator,
    defaultsProviders: [App.TezDefaultsProvider.create()],
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Advanced', displayName : 'Advanced'}),
      App.ServiceConfigCategory.create({ name: 'AdvancedTezSite', displayName : 'Custom tez-site.xml', siteFileName: 'tez-site.xml', canAddProperty: true})
    ],
    sites: ['global', 'tez-site'],
    configs: []
  },
  {
    serviceName: 'FLUME',
    displayName: 'Flume',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'AdvancedFlumeConf', displayName : 'flume.conf', siteFileName: 'flume.conf', canAddProperty: false})
    ],
    sites: ['flume.conf'],
    configs: []
  },
  {
    serviceName: 'MISC',
    displayName: 'Misc',
    configsValidator: App.userConfigsValidator,
    defaultsProviders: [App.userDefaultsProvider.create()],
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'General', displayName : 'General'}),
      App.ServiceConfigCategory.create({ name: 'Users and Groups', displayName : 'Users and Groups'})
    ],
    sites: ['global'],
    configs: []
  }

];
