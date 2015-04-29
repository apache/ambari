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
require('models/configs/objects/service_config');
require('models/configs/objects/service_config_category');
require('models/configs/objects/service_config_property');
require('models/service');

App.SecureConfigProperties = Ember.ArrayProxy.extend({
  content: require('data/HDP2/secure_properties').configProperties
});

var configProperties = App.SecureConfigProperties.create();

var configs = [
  {
    serviceName: 'GENERAL',
    displayName: 'General',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'KERBEROS', displayName: 'Kerberos'}),
      App.ServiceConfigCategory.create({ name: 'AMBARI', displayName: 'Ambari'})
    ],
    sites: ['cluster-env'],
    configs: configProperties.filterProperty('serviceName', 'GENERAL')
  },
  {
    serviceName: 'HDFS',
    displayName: 'HDFS',
    filename: 'hdfs-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'General', displayName: 'General', exclude:'HA'}),
      App.ServiceConfigCategory.create({ name: 'NameNode', displayName: 'NameNode'}),
      App.ServiceConfigCategory.create({ name: 'SNameNode', displayName: 'Secondary NameNode',exclude:'HA'}),
      App.ServiceConfigCategory.create({ name: 'JournalNode', displayName: 'JournalNode',exclude:'non-HA'}),
      App.ServiceConfigCategory.create({ name: 'DataNode', displayName: 'DataNode'})
    ],
    sites: ['hadoop-env','core-site', 'hdfs-site'],
    configs: configProperties.filterProperty('serviceName', 'HDFS')
  },
  {
    serviceName: 'MAPREDUCE2',
    displayName: 'MapReduce2',
    filename: 'mapred-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'JobHistoryServer', displayName: 'History Server'})
    ],
    sites: ['mapred-site'],
    configs: configProperties.filterProperty('serviceName', 'MAPREDUCE2')
  },
  {
    serviceName: 'YARN',
    displayName: 'YARN',
    filename: 'yarn-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'ResourceManager', displayName : 'ResourceManager'}),
      App.ServiceConfigCategory.create({ name: 'NodeManager', displayName : 'NodeManager'})
    ], // these properties can be dynamic
    sites: ['yarn-site', 'core-site'],
    configs: configProperties.filterProperty('serviceName', 'YARN')
  },
  {
    serviceName: 'HIVE',
    displayName: 'Hive',
    filename: 'hive-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Hive Metastore', displayName: 'Hive Metastore and Hive Server 2'}),
      App.ServiceConfigCategory.create({ name: 'WebHCat Server', displayName : 'WebHCat Server'})
    ],
    sites: ['hive-site','webhcat-site'],
    configs: configProperties.filterProperty('serviceName', 'HIVE')
  },
  {
    serviceName: 'HBASE',
    displayName: 'HBase',
    filename: 'hbase-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'HBase Master', displayName: 'HBase Master'}),
      App.ServiceConfigCategory.create({ name: 'RegionServer', displayName: 'RegionServer'})
    ],
    sites: ['hbase-env','hbase-site'],
    configs: configProperties.filterProperty('serviceName', 'HBASE')
  },
  {
    serviceName: 'ZOOKEEPER',
    displayName: 'ZooKeeper',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'ZooKeeper Server', displayName: 'ZooKeeper Server'})
    ],
    sites: ['zookeeper-env'],
    configs: configProperties.filterProperty('serviceName', 'ZOOKEEPER')

  },
  {
    serviceName: 'OOZIE',
    displayName: 'Oozie',
    filename: 'oozie-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Oozie Server', displayName:  'Oozie Server'})
    ],
    sites: ['oozie-env','oozie-site'],
    configs: configProperties.filterProperty('serviceName', 'OOZIE')
  },
  {
    serviceName: 'STORM',
    displayName: 'Storm',
    filename: 'storm-site',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Storm Topology', displayName:  'Storm Client'})
    ],
    sites: ['storm-env','storm-site'],
    configs: configProperties.filterProperty('serviceName', 'STORM')
  },
  {
    serviceName: 'FALCON',
    displayName: 'Falcon',
    filename: 'falcon-startup.properties',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Falcon Server', displayName:  'Falcon Server startup properties'})
    ],
    sites: ['falcon-startup.properties'],
    configs: configProperties.filterProperty('serviceName', 'FALCON')
  },
  {
    serviceName: 'KNOX',
    displayName: 'Knox',
    filename: 'gateway-site.xml',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'Knox Gateway', displayName:  'Knox Gateway'})
    ],
    sites: ['gateway-site','knox-env'],
    configs: configProperties.filterProperty('serviceName', 'KNOX')
  }
];

if(App.get('isHadoop22Stack')){
  for(var i = 0; i < configs.length; i++){
    var config = configs[i];
    if( config.serviceName === 'STORM' ){
      config.configCategories.unshift(App.ServiceConfigCategory.create({ name: 'Nimbus', displayName:  'Nimbus'}));
    }
  }
}


module.exports = configs;


