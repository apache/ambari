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

module.exports =
    {
        "haConfig": {
            serviceName: 'MISC',
            displayName: 'MISC',
            configCategories: [
                App.ServiceConfigCategory.create({ name: 'HDFS', displayName: 'HDFS'}),
                App.ServiceConfigCategory.create({ name: 'HBASE', displayName: 'HBase'}),
                App.ServiceConfigCategory.create({ name: 'AMBARI_METRICS', displayName: 'Ambari Metrics'}),
                App.ServiceConfigCategory.create({ name: 'RANGER', displayName: 'Ranger'})
            ],
            sites: ['core-site', 'hdfs-site', 'hbase-site', 'accumulo-site', 'ams-hbase-site', 'hawq-site', 'hdfs-client', 'ranger-env', 'ranger-knox-plugin-properties', 'ranger-kms-audit', 'ranger-storm-plugin-properties', 'ranger-hbase-plugin-properties', 'ranger-hdfs-plugin-properties', 'ranger-hive-plugin-properties', 'ranger-kafka-audit', 'ranger-knox-audit', 'ranger-hdfs-audit', 'ranger-hive-audit', 'ranger-atlas-audit', 'ranger-storm-audit', 'ranger-hbase-audit', 'ranger-yarn-audit'],
            configs: [
                /**********************************************HDFS***************************************/

                {
                    "name": "dfs.ha.namenodes.{{namespaceId}}",
                    "displayName": "dfs.ha.namenodes.{{namespaceId}}",
                    "description": "The prefix for a given nameservice, contains a comma-separated list of namenodes for a given nameservice.",
                    "isReconfigurable": false,
                    "recommendedValue": "nn1,nn2,nn3",
                    "value": "{{listNameNodes}}",
                    "category": "HDFS",
                    "filename": "hdfs-site",
                    "serviceName": 'MISC'
                },
                {
                    "name": "dfs.namenode.rpc-address.{{namespaceId}}.{{newNamenodeIndex}}",
                    "displayName": "dfs.namenode.rpc-address.{{namespaceId}}.{{newNamenodeIndex}}",
                    "description": "RPC address that handles all clients requests for new NameNode.",
                    "isReconfigurable": false,
                    "recommendedValue": "0.0.0.0:8020",
                    "value": "{{newNameNode}}:8020",
                    "category": "HDFS",
                    "filename": "hdfs-site",
                    "serviceName": 'MISC'
                },
                {
                    "name": "dfs.namenode.http-address.{{namespaceId}}.{{newNamenodeIndex}}",
                    "displayName": "dfs.namenode.http-address.{{namespaceId}}.{{newNamenodeIndex}}",
                    "description": "The fully-qualified HTTP address for new NameNode.",
                    "isReconfigurable": false,
                    "recommendedValue": "0.0.0.0:50070",
                    "value": "{{newNameNode}}:50070",
                    "category": "HDFS",
                    "filename": "hdfs-site",
                    serviceName: 'MISC'
                },

                {
                    "name": "dfs.namenode.https-address.{{namespaceId}}.{{newNamenodeIndex}}",
                    "displayName": "dfs.namenode.https-address.{{namespaceId}}.{{newNamenodeIndex}}",
                    "description": "The fully-qualified HTTP address for new NameNode.",
                    "isReconfigurable": false,
                    "recommendedValue": "0.0.0.0:50470",
                    "value": "{{newNameNode}}:50470",
                    "category": "HDFS",
                    "filename": "hdfs-site",
                    "serviceName": 'MISC'
                },

                /**********************************************HAWQ***************************************/
                {
                    "name": "dfs.ha.namenodes.{{namespaceId}}",
                    "displayName": "dfs.ha.namenodes.{{namespaceId}}",
                    "description": "The prefix for a given nameservice, contains a comma-separated list of namenodes for a given nameservice.",
                    "isReconfigurable": false,
                    "recommendedValue": "nn1,nn2,nn3",
                    "value": "{{listNameNodes}}",
                    "category": "HAWQ",
                    "filename": "hdfs-client",
                    "serviceName": 'MISC'
                },
                {
                    "name": "dfs.namenode.rpc-address.{{namespaceId}}.{{newNamenodeIndex}}",
                    "displayName": "dfs.namenode.rpc-address.{{namespaceId}}.{{newNamenodeIndex}}",
                    "description": "RPC address that handles all clients requests for new NameNode..",
                    "isReconfigurable": false,
                    "recommendedValue": "0.0.0.0:8020",
                    "value": "{{newNameNode}}:8020",
                    "category": "HAWQ",
                    "filename": "hdfs-client",
                    "serviceName": 'MISC'
                },
                {
                    "name": "dfs.namenode.http-address.{{namespaceId}}.{{newNamenodeIndex}}",
                    "displayName": "dfs.namenode.http-address.{{namespaceId}}.{{newNamenodeIndex}}",
                    "description": "The fully-qualified HTTP address for new NameNode.",
                    "isReconfigurable": false,
                    "recommendedValue": "0.0.0.0:50070",
                    "value": "{{newNameNode}}:50070",
                    "category": "HAWQ",
                    "filename": "hdfs-client",
                    "serviceName": 'MISC'
                },
                {
                    "name": "dfs.namenode.https-address.{{namespaceId}}.{{newNamenodeIndex}}",
                    "displayName": "dfs.namenode.https-address.{{namespaceId}}.{{newNamenodeIndex}}",
                    "description": "The fully-qualified HTTPS address for new NameNode.",
                    "isReconfigurable": false,
                    "recommendedValue": "0.0.0.0:50470",
                    "value": "{{newNameNode}}:50470",
                    "category": "HAWQ",
                    "filename": "hdfs-client",
                    "serviceName": 'MISC'
                }

            ]
        }
    };
