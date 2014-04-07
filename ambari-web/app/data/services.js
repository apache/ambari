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

module.exports = [
  {
    serviceName: 'HDFS',
    displayName: 'HDFS',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    description: Em.I18n.t('services.hdfs.description')
  },
  {
    serviceName: 'GLUSTERFS',
    displayName: 'GLUSTERFS',
    isDisabled: false,
    isSelected: false,
    canBeSelected: true,
    description: Em.I18n.t('services.glusterfs.description')
  },
  {
    serviceName: 'MAPREDUCE',
    displayName: 'MapReduce',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    description: Em.I18n.t('services.mapreduce.description')
  },
  {
    serviceName: 'MAPREDUCE2',
    displayName: 'MapReduce 2',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    isHidden: true,
    description: Em.I18n.t('services.mapreduce2.description')
  },
  {
    serviceName: 'YARN',
    displayName: 'YARN + MapReduce2',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    description: Em.I18n.t('services.yarn.description')
  },
  {
    serviceName: 'TEZ',
    displayName: 'Tez',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    description: Em.I18n.t('services.tez.description')
  },
  {
    serviceName: 'NAGIOS',
    displayName: 'Nagios',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    description: Em.I18n.t('services.nagios.description')
  },
  {
    serviceName: 'GANGLIA',
    displayName: 'Ganglia',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    description: Em.I18n.t('services.ganglia.description')
  },
  {
    serviceName: 'HIVE',
    displayName: 'Hive + HCat',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    description: Em.I18n.t('services.hive.description')
  },
  {
    serviceName: 'HCATALOG',
    displayName: 'HCatalog',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    isHidden: true
  },
  {
    serviceName: 'WEBHCAT',
    displayName: 'WebHCat',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    isHidden: true
  },
  {
    serviceName: 'HBASE',
    displayName: 'HBase',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    description: Em.I18n.t('services.hbase.description')
  },
  {
    serviceName: 'PIG',
    displayName: 'Pig',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    description: Em.I18n.t('services.pig.description')
  },
  {
    serviceName: 'SQOOP',
    displayName: 'Sqoop',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    description: Em.I18n.t('services.sqoop.description')
  },
  {
    serviceName: 'OOZIE',
    displayName: 'Oozie',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    description: Em.I18n.t('services.oozie.description')
  },
  {
    serviceName: 'ZOOKEEPER',
	  displayName: 'ZooKeeper',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    description: Em.I18n.t('services.zookeeper.description')
  },
  {
    serviceName: 'HUE',
    displayName: 'Hue',
    isDisabled: false,
    isSelected: App.supports.hue,
    canBeSelected: App.supports.hue,
    isHidden: !App.supports.hue
  },
  {
    serviceName: 'FALCON',
    displayName: 'Falcon',
    isDisabled: false,
    isSelected: App.supports.falcon,
    canBeSelected: App.supports.falcon,
    isHidden: !App.supports.falcon,
    description: Em.I18n.t('services.falcon.description')
  },
  {
    serviceName: 'STORM',
    displayName: 'Storm',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    isHidden: false,
    description: Em.I18n.t('services.storm.description')
  }
];

if (App.supports.flume) {
  var flume = {
    serviceName: 'FLUME',
    displayName: 'Flume',
    isDisabled: false,
    isSelected: true,
    canBeSelected: true,
    isHidden: false
  };
  module.exports.push(flume);
}
