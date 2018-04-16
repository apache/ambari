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
  "federationConfig": {
    serviceName: 'MISC',
    displayName: 'MISC',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'HDFS', displayName: 'HDFS'}),
      App.ServiceConfigCategory.create({ name: 'RANGER', displayName: 'Ranger'})
    ],
    sites: ['core-site'],
    configs: [
      {
        "name": "dfs.journalnode.edits.dir.{{nameservice2}}",
        "displayName": "dfs.journalnode.edits.dir.{{nameservice2}}",
        "description": "The Directory where the JournalNode will store its local state.",
        "isReconfigurable": true,
        "recommendedValue": "",
        "value": "",
        "displayType": "directory",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.journalnode.edits.dir.{{nameservice1}}",
        "displayName": "dfs.journalnode.edits.dir.{{nameservice1}}",
        "description": "The Directory where the JournalNode will store its local state.",
        "isReconfigurable": false,
        "recommendedValue": "{{journalnode_edits_dir}}",
        "value": "{{journalnode_edits_dir}}",
        "displayType": "directory",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.nameservices",
        "displayName": "dfs.nameservices",
        "description": "Comma-separated list of nameservices.",
        "isReconfigurable": false,
        "recommendedValue": "{{nameservice1}},{{nameservice2}}",
        "value": "{{nameservice1}},{{nameservice2}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.internal.nameservices",
        "displayName": "dfs.internal.nameservices",
        "description": "Comma-separated list of nameservices.",
        "isReconfigurable": false,
        "recommendedValue": "{{nameservice1}},{{nameservice2}}",
        "value": "{{nameservice1}},{{nameservice2}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.ha.namenodes.{{nameservice2}}",
        "displayName": "dfs.ha.namenodes.{{nameservice2}}",
        "description": "The prefix for a given nameservice, contains a comma-separated list of namenodes for a given nameservice.",
        "isReconfigurable": false,
        "recommendedValue": "nn3,nn4",
        "value": "nn3,nn4",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.rpc-address.{{nameservice2}}.nn3",
        "displayName": "dfs.namenode.rpc-address.{{nameservice2}}.nn3",
        "description": "RPC address that handles all clients requests for nn3.",
        "isReconfigurable": false,
        "recommendedValue": "{{namenode3}}:{{nnRpcPort}}",
        "value": "{{namenode3}}:{{nnRpcPort}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.rpc-address.{{nameservice2}}.nn4",
        "displayName": "dfs.namenode.rpc-address.{{nameservice2}}.nn4",
        "description": "RPC address that handles all clients requests for nn4.",
        "isReconfigurable": false,
        "recommendedValue": "{{namenode4}}:{{nnRpcPort}}",
        "value": "{{namenode4}}:{{nnRpcPort}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.http-address.{{nameservice2}}.nn3",
        "displayName": "dfs.namenode.http-address.{{nameservice2}}.nn3",
        "description": "The fully-qualified HTTP address for nn3 NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "{{namenode3}}:{{nnHttpPort}}",
        "value": "{{namenode3}}:{{nnHttpPort}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.http-address.{{nameservice2}}.nn4",
        "displayName": "dfs.namenode.http-address.{{nameservice2}}.nn4",
        "description": "The fully-qualified HTTP address for nn4 NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "{{namenode4}}:{{nnHttpPort}}",
        "value": "{{namenode4}}:{{nnHttpPort}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.https-address.{{nameservice2}}.nn3",
        "displayName": "dfs.namenode.https-address.{{nameservice2}}.nn3",
        "description": "The fully-qualified HTTPS address for nn3 NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "{{namenode3}}:{{nnHttpsPort}}",
        "value": "{{namenode3}}:{{nnHttpsPort}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.https-address.{{nameservice2}}.nn4",
        "displayName": "dfs.namenode.https-address.{{nameservice2}}.nn4",
        "description": "The fully-qualified HTTPS address for nn4 NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "{{namenode4}}:{{nnHttpsPort}}",
        "value": "{{namenode4}}:{{nnHttpsPort}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.client.failover.proxy.provider.{{nameservice2}}",
        "displayName": "dfs.client.failover.proxy.provider.{{nameservice2}}",
        "description": "The Java class that HDFS clients use to contact the Active NameNode.",
        "recommendedValue": "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider",
        "isReconfigurable": false,
        "value": "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.shared.edits.dir.{{nameservice1}}",
        "displayName": "dfs.namenode.shared.edits.dir.{{nameservice1}}",
        "description": "The URI which identifies the group of JNs where the NameNodes will write/read edits.",
        "isReconfigurable": false,
        "recommendedValue": "qjournal://{{journalnodes}}/{{nameservice1}}",
        "value": "qjournal://{{journalnodes}}/{{nameservice1}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.shared.edits.dir.{{nameservice2}}",
        "displayName": "dfs.namenode.shared.edits.dir.{{nameservice2}}",
        "description": "The URI which identifies the group of JNs where the NameNodes will write/read edits.",
        "isReconfigurable": false,
        "recommendedValue": "qjournal://{{journalnodes}}/{{nameservice2}}",
        "value": "qjournal://{{journalnodes}}/{{nameservice2}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.servicerpc-address.{{nameservice1}}.nn1",
        "displayName": "dfs.namenode.servicerpc-address.{{nameservice1}}.nn1",
        "description": "RPC address for HDFS Services communication.",
        "isReconfigurable": false,
        "recommendedValue": "{{namenode1}}:8021",
        "value": "{{namenode1}}:8021",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.servicerpc-address.{{nameservice1}}.nn2",
        "displayName": "dfs.namenode.servicerpc-address.{{nameservice1}}.nn2",
        "description": "RPC address for HDFS Services communication.",
        "isReconfigurable": false,
        "recommendedValue": "{{namenode2}}:8021",
        "value": "{{namenode2}}:8021",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.servicerpc-address.{{nameservice2}}.nn3",
        "displayName": "dfs.namenode.servicerpc-address.{{nameservice2}}.nn3",
        "description": "RPC address for HDFS Services communication.",
        "isReconfigurable": false,
        "recommendedValue": "{{namenode3}}:8021",
        "value": "{{namenode3}}:8021",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.servicerpc-address.{{nameservice2}}.nn4",
        "displayName": "dfs.namenode.servicerpc-address.{{nameservice2}}.nn4",
        "description": "RPC address for HDFS Services communication.",
        "isReconfigurable": false,
        "recommendedValue": "{{namenode4}}:8021",
        "value": "{{namenode4}}:8021",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "ranger.tagsync.atlas.hdfs.instance.{{clustername}}.nameservice.{{nameservice1}}.ranger.service",
        "displayName": "ranger.tagsync.atlas.hdfs.instance.{{clustername}}.nameservice.{{nameservice1}}.ranger.service",
        "isReconfigurable": false,
        "recommendedValue": "{{ranger_service_name_ns1}}",
        "value": "{{ranger_service_name_ns1}}",
        "category": "RANGER",
        "filename": "ranger-tagsync-site",
        "serviceName": 'MISC'
      },
      {
        "name": "ranger.tagsync.atlas.hdfs.instance.{{clustername}}.nameservice.{{nameservice2}}.ranger.service",
        "displayName": "ranger.tagsync.atlas.hdfs.instance.{{clustername}}.nameservice.{{nameservice2}}.ranger.service",
        "isReconfigurable": false,
        "recommendedValue": "{{ranger_service_name_ns2}}",
        "value": "{{ranger_service_name_ns2}}",
        "category": "RANGER",
        "filename": "ranger-tagsync-site",
        "serviceName": 'MISC'
      }
    ]
  }
};
