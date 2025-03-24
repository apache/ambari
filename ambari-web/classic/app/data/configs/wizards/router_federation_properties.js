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
        "routerFederationConfig": {
            serviceName: 'MISC',
            displayName: 'MISC',
            configCategories: [
                App.ServiceConfigCategory.create({ name: 'HDFS', displayName: 'HDFS'})
            ],
            sites: ['core-site', 'hdfs-rbf-site'],
            configs: [
                {
                    "name": "dfs.federation.router.monitor.namenode",
                    "displayName": "dfs.federation.router.monitor.namenode",
                    "description": "RPC address for HDFS Services communication.",
                    "isReconfigurable": false,
                    "recommendedValue": "{{nameservice1}}.nn1, {{nameservice1}}.nn2, {{newNameservice}}.{{newNameNode1Index}},{{newNameservice}}.{{newNameNode2Index}}",
                    "value": "{{modifiedNameServices}}",
                    "category": "HDFS",
                    "filename": "hdfs-rbf-site",
                    "serviceName": 'MISC',
                    "isRouterConfigs" : true
                },
                {
                    "name": "dfs.federation.router.default.nameserviceId",
                    "displayName": "dfs.federation.router.default.nameserviceId",
                    "description": "Nameservice identifier of the default subcluster to monitor.",
                    "isReconfigurable": false,
                    "recommendedValue": "{{nameservice1}}",
                    "value": "{{nameservice1}}",
                    "category": "HDFS",
                    "filename": "hdfs-rbf-site",
                    "serviceName": 'MISC',
                    "isRouterConfigs" : true
                },
                {
                    "name": "zk-dt-secret-manager.zkAuthType",
                    "displayName": "zk-dt-secret-manager.zkAuthType",
                    "description": "Secret Manager Zookeeper Authentication Type",
                    "isReconfigurable": false,
                    "recommendedValue": "none",
                    "value": "none",
                    "category": "HDFS",
                    "filename": "hdfs-rbf-site",
                    "serviceName": 'MISC',
                    "isRouterConfigs" : true
                },
                {
                    "name": "zk-dt-secret-manager.zkConnectionString",
                    "displayName": "zk-dt-secret-manager.zkConnectionString",
                    "description": "Secret Manager Zookeeper Connection String",
                    "isReconfigurable": false,
                    "recommendedValue": "zk1.example.com:2181,zk2.example.com:2181,zk3.example.com:2181",
                    "value": "{{zkAddress}}",
                    "category": "HDFS",
                    "filename": "hdfs-rbf-site",
                    "serviceName": 'MISC',
                    "isRouterConfigs" : true
                }
            ]
        }

    };