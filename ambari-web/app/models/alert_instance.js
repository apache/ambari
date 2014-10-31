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

App.AlertInstance = DS.Model.extend({
  id: DS.attr('number'),
  label: DS.attr('string'),
  alertDefinition: DS.belongsTo('App.AlertDefinition'),
  serviceName: DS.attr('string'),
  componentName: DS.attr('string'),
  hostName: DS.attr('string'),
  scope: DS.attr('string'),
  originalTimestamp: DS.attr('number'),
  latestTimestamp: DS.attr('number'),
  maintenanceState: DS.attr('string'),
  instance: DS.attr('string'),
  state: DS.attr('string'),
  text: DS.attr('string')
});

App.AlertInstance.FIXTURES = [
  {
    "id": 1,
    "cluster_name": "tdk",
    "component_name": "SECONDARY_NAMENODE",
    "host_name": "tr-2.c.pramod-thangali.internal",
    "instance": null,
    "label": "Secondary NameNode Process",
    "latest_timestamp": 1414664775337,
    "maintenance_state": "OFF",
    "name": "secondary_namenode_process",
    "original_timestamp": 1414585335334,
    "scope": "ANY",
    "service_name": "HDFS",
    "state": "CRITICAL",
    "text": "Connection failed: [Errno 111] Connection refused on host tr-2.c.pramod-thangali.internal:50090",
    "alert_definition": 1
  },
  {
    "cluster_name" : "tdk",
    "component_name" : "DATANODE",
    "host_name" : "tr-3.c.pramod-thangali.internal",
    "id" : 2,
    "instance" : null,
    "label" : "DataNode Web UI",
    "latest_timestamp" : 1414666905645,
    "maintenance_state" : "OFF",
    "name" : "datanode_webui",
    "original_timestamp" : 1414585365674,
    "scope" : "HOST",
    "service_name" : "HDFS",
    "state" : "CRITICAL",
    "text" : "Connection failed to 0.0.0.0:50075",
    "alert_definition": 2
  },
  {
    "cluster_name": "tdk",
    "component_name": "ZOOKEEPER_SERVER",
    "host_name": "tr-1.c.pramod-thangali.internal",
    "id": 3,
    "instance": null,
    "label": "ZooKeeper Server Process",
    "latest_timestamp": 1414665174611,
    "maintenance_state": "OFF",
    "name": "zookeeper_server_process",
    "original_timestamp": 1414585014606,
    "scope": "ANY",
    "service_name": "ZOOKEEPER",
    "state": "CRITICAL",
    "text": "TCP OK - 0.0000 response on port 2181",
    "alert_definition": 3
  },
  {
    "cluster_name": "tdk",
    "component_name": "ZOOKEEPER_SERVER",
    "host_name": "tr-2.c.pramod-thangali.internal",
    "id": 4,
    "instance": null,
    "label": "ZooKeeper Server Process",
    "latest_timestamp": 1414665135341,
    "maintenance_state": "OFF",
    "name": "zookeeper_server_process",
    "original_timestamp": 1414585035316,
    "scope": "ANY",
    "service_name": "ZOOKEEPER",
    "state": "OK",
    "text": "TCP OK - 0.0000 response on port 2181",
    "alert_definition": 3
  },
  {
    "cluster_name": "tdk",
    "component_name": "ZOOKEEPER_SERVER",
    "host_name": "tr-3.c.pramod-thangali.internal",
    "id": 5,
    "instance": null,
    "label": "ZooKeeper Server Process",
    "latest_timestamp": 1414665165640,
    "maintenance_state": "OFF",
    "name": "zookeeper_server_process",
    "original_timestamp": 1414585065616,
    "scope": "ANY",
    "service_name": "ZOOKEEPER",
    "state": "OK",
    "text": "TCP OK - 0.0000 response on port 2181",
    "alert_definition": 3
  }
];
