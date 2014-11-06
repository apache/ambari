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

var hdp22SepcificProperties = [
  require('data/HDP2.2/yarn_properties'),
  require('data/HDP2.2/tez_properties'),
  require('data/HDP2.2/hive_properties')
].reduce(function(p, c) { return c.concat(p); });

var hdp2properties = require('data/HDP2/site_properties').configProperties;
var excludedConfigs = [
  'storm.thrift.transport', //In HDP2.2 storm.thrift.transport property is computed on server
  'storm_rest_api_host',
  'tez.am.container.session.delay-allocation-millis',
  'tez.am.grouping.max-size',
  'tez.am.grouping.min-size',
  'tez.am.grouping.split-waves',
  'tez.am.java.opts',
  'tez.runtime.intermediate-input.compress.codec',
  'tez.runtime.intermediate-input.is-compressed',
  'tez.runtime.intermediate-output.compress.codec',
  'tez.runtime.intermediate-output.should-compress',
  'dfs.datanode.data.dir'
];
var hdp22properties = hdp2properties.filter(function (item) {
  return !excludedConfigs.contains(item.name);
});

hdp22properties.push(
  {
    "id": "site property",
    "name": "hive.zookeeper.quorum",
    "displayName": "hive.zookeeper.quorum",
    "defaultValue": "localhost:2181",
    "displayType": "multiLine",
    "isRequired": false,
    "isVisible": true,
    "serviceName": "HIVE",
    "category": "Advanced hive-site"
  },
  {
    "id": "site property",
    "name": "hadoop.registry.rm.enabled",
    "displayName": "hadoop.registry.rm.enabled",
    "defaultValue": "false",
    "displayType": "checkbox",
    "isVisible": true,
    "serviceName": "YARN",
    "category": "Advanced yarn-site"
  },
  {
    "id": "site property",
    "name": "hadoop.registry.zk.quorum",
    "displayName": "hadoop.registry.zk.quorum",
    "defaultValue": "localhost:2181",
    "isVisible": true,
    "serviceName": "YARN",
    "category": "Advanced yarn-site"
  },
  {
    "id": "site property",
    "name": "dfs.datanode.data.dir",
    "displayName": "DataNode directories",
    "defaultDirectory": "/hadoop/hdfs/data",
    "displayType": "datanodedirs",
    "category": "DATANODE",
    "serviceName": "HDFS",
    "index": 1
  });

var additionalProperties = [];

hdp22SepcificProperties.forEach(function(config) {
  if (!hdp22properties.findProperty('name', config.name)) additionalProperties.push(config);
  else {
    hdp22properties.findProperty('name', config.name).category = config.category;
  }
});

module.exports =
{
  "configProperties": hdp22properties.concat(additionalProperties)
};
