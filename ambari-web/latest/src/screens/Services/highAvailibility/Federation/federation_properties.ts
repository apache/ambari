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

interface ConfigProperty {
  name: string;
  displayName: string;
  description?: string;
  isReconfigurable: boolean;
  recommendedValue: string;
  value: string;
  displayType?: string;
  category: string;
  filename: string;
  serviceName: string;
  firstRun?: boolean;
}

class ServiceConfigCategory {
  name: string;
  displayName: string;

  constructor(options: { name: string; displayName: string }) {
    this.name = options.name;
    this.displayName = options.displayName;
  }

  static create(options: { name: string; displayName: string }): ServiceConfigCategory {
    return new ServiceConfigCategory(options);
  }
}

export const federationProperties = () => {
    // Define interfaces for our configuration types


// Create a ServiceConfigCategory class similar to the App.ServiceConfigCategory in the original code


// Define the federation configuration
const federationConfig = {
  serviceName: 'MISC',
  displayName: 'MISC',
  configCategories: [
    ServiceConfigCategory.create({ name: 'HDFS', displayName: 'HDFS' }),
    ServiceConfigCategory.create({ name: 'RANGER', displayName: 'Ranger' }),
    ServiceConfigCategory.create({ name: 'ACCUMULO', displayName: 'Accumulo' })
  ],
  sites: ['core-site'],
  configs: [
      {
        "name": "dfs.journalnode.edits.dir.{{newNameservice}}",
        "displayName": "dfs.journalnode.edits.dir.{{newNameservice}}",
        "description": "The Directory where the JournalNode will store its local state.",
        "isReconfigurable": true,
        "recommendedValue": "",
        "value": "",
        "displayType": "directory",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC',
        "isRequired": true
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
        "serviceName": 'MISC',
        "firstRun": true
      },
      {
        "name": "dfs.nameservices",
        "displayName": "dfs.nameservices",
        "description": "Comma-separated list of nameservices.",
        "isReconfigurable": false,
        "recommendedValue": "{{nameServicesList}},{{newNameservice}}",
        "value": "{{nameServicesList}},{{newNameservice}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.internal.nameservices",
        "displayName": "dfs.internal.nameservices",
        "description": "Comma-separated list of nameservices.",
        "isReconfigurable": false,
        "recommendedValue": "{{nameServicesList}},{{newNameservice}}",
        "value": "{{nameServicesList}},{{newNameservice}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.ha.namenodes.{{newNameservice}}",
        "displayName": "dfs.ha.namenodes.{{newNameservice}}",
        "description": "The prefix for a given nameservice, contains a comma-separated list of namenodes for a given nameservice.",
        "isReconfigurable": false,
        "recommendedValue": "{{newNameNode1Index}},{{newNameNode2Index}}",
        "value": "{{newNameNode1Index}},{{newNameNode2Index}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.rpc-address.{{newNameservice}}.{{newNameNode1Index}}",
        "displayName": "dfs.namenode.rpc-address.{{newNameservice}}.{{newNameNode1Index}}",
        "description": "RPC address that handles all clients requests for {{newNameNode1Index}}.",
        "isReconfigurable": false,
        "recommendedValue": "{{newNameNode1}}:{{nnRpcPort}}",
        "value": "{{newNameNode1}}:{{nnRpcPort}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.rpc-address.{{newNameservice}}.{{newNameNode2Index}}",
        "displayName": "dfs.namenode.rpc-address.{{newNameservice}}.{{newNameNode2Index}}",
        "description": "RPC address that handles all clients requests for {{newNameNode2Index}}.",
        "isReconfigurable": false,
        "recommendedValue": "{{newNameNode2}}:{{nnRpcPort}}",
        "value": "{{newNameNode2}}:{{nnRpcPort}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.http-address.{{newNameservice}}.{{newNameNode1Index}}",
        "displayName": "dfs.namenode.http-address.{{newNameservice}}.{{newNameNode1Index}}",
        "description": "The fully-qualified HTTP address for {{newNameNode1Index}} NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "{{newNameNode1}}:{{nnHttpPort}}",
        "value": "{{newNameNode1}}:{{nnHttpPort}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.http-address.{{newNameservice}}.{{newNameNode2Index}}",
        "displayName": "dfs.namenode.http-address.{{newNameservice}}.{{newNameNode2Index}}",
        "description": "The fully-qualified HTTP address for {{newNameNode2Index}} NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "{{newNameNode2}}:{{nnHttpPort}}",
        "value": "{{newNameNode2}}:{{nnHttpPort}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.https-address.{{newNameservice}}.{{newNameNode1Index}}",
        "displayName": "dfs.namenode.https-address.{{newNameservice}}.{{newNameNode1Index}}",
        "description": "The fully-qualified HTTPS address for {{newNameNode1Index}} NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "{{newNameNode1}}:{{nnHttpsPort}}",
        "value": "{{newNameNode1}}:{{nnHttpsPort}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.https-address.{{newNameservice}}.{{newNameNode2Index}}",
        "displayName": "dfs.namenode.https-address.{{newNameservice}}.{{newNameNode2Index}}",
        "description": "The fully-qualified HTTPS address for {{newNameNode2Index}} NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "{{newNameNode2}}:{{nnHttpsPort}}",
        "value": "{{newNameNode2}}:{{nnHttpsPort}}",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.client.failover.proxy.provider.{{newNameservice}}",
        "displayName": "dfs.client.failover.proxy.provider.{{newNameservice}}",
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
        "serviceName": 'MISC',
        "firstRun": true
      },
      {
        "name": "dfs.namenode.shared.edits.dir.{{newNameservice}}",
        "displayName": "dfs.namenode.shared.edits.dir.{{newNameservice}}",
        "description": "The URI which identifies the group of JNs where the NameNodes will write/read edits.",
        "isReconfigurable": false,
        "recommendedValue": "qjournal://{{journalnodes}}/{{newNameservice}}",
        "value": "qjournal://{{journalnodes}}/{{newNameservice}}",
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
        "serviceName": 'MISC',
        "firstRun": true
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
        "serviceName": 'MISC',
        "firstRun": true
      },
      {
        "name": "dfs.namenode.servicerpc-address.{{newNameservice}}.{{newNameNode1Index}}",
        "displayName": "dfs.namenode.servicerpc-address.{{newNameservice}}.{{newNameNode1Index}}",
        "description": "RPC address for HDFS Services communication.",
        "isReconfigurable": false,
        "recommendedValue": "{{newNameNode1}}:8021",
        "value": "{{newNameNode1}}:8021",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.servicerpc-address.{{newNameservice}}.{{newNameNode2Index}}",
        "displayName": "dfs.namenode.servicerpc-address.{{newNameservice}}.{{newNameNode2Index}}",
        "description": "RPC address for HDFS Services communication.",
        "isReconfigurable": false,
        "recommendedValue": "{{newNameNode2}}:8021",
        "value": "{{newNameNode2}}:8021",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      }
    ] as ConfigProperty[]
};
return {
    federationConfig: federationConfig,
    ServiceConfigCategory: ServiceConfigCategory
}
}
