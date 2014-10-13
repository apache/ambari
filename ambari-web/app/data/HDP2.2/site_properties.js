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
var hdp2properties = require('data/HDP2/site_properties').configProperties;
var hdp22properties = hdp2properties.filter(function (item){
  //In HDP2.2 storm.thrift.transport property is computed on server
  return item.name !== 'storm.thrift.transport' && item.name !== 'storm_rest_api_host';
});

hdp22properties.push(
{
  "id": "site property",
  "name": "hive.zookeeper.quorum",
  "displayName": "hive.zookeeper.quorum",
  "defaultValue": "localhost:2181",
  "displayType": "multiLine",
  "isVisible": true,
  "serviceName": "HIVE",
  "category": "Advanced hive-site"
});

module.exports =
{
  "configProperties": hdp22properties
};
