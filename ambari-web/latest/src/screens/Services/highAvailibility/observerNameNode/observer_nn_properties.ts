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
  isRequired?: boolean;
}

class ServiceConfigCategory {
  name: string;
  displayName: string;

  constructor(options: { name: string; displayName: string }) {
    this.name = options.name;
    this.displayName = options.displayName;
  }

  static create(options: {
    name: string;
    displayName: string;
  }): ServiceConfigCategory {
    return new ServiceConfigCategory(options);
  }
}

export const observerNnProperties = () => {
  const observerNnConfig: {
    serviceName: string;
    displayName: string;
    configCategories: ServiceConfigCategory[];
    sites: string[];
    configs: ConfigProperty[];
  } = {
    serviceName: "MISC",
    displayName: "MISC",
    configCategories: [
      ServiceConfigCategory.create({ name: "HDFS", displayName: "HDFS" }),
    ],
    sites: ["hdfs-site", "hdfs-client"],
    configs: [
      /********************************************** HDFS ***************************************/
      {
        name: "dfs.ha.namenodes.{{namespaceId}}",
        displayName: "dfs.ha.namenodes.{{namespaceId}}",
        description:
          "The prefix for a given nameservice, contains a comma-separated list of namenodes for a given nameservice.",
        isReconfigurable: false,
        recommendedValue: "nn1,nn2,nn3",
        value: "{{listNameNodes}}",
        category: "HDFS",
        filename: "hdfs-site",
        serviceName: "MISC",
      },
      {
        name: "dfs.namenode.rpc-address.{{namespaceId}}.{{newNamenodeIndex}}",
        displayName:
          "dfs.namenode.rpc-address.{{namespaceId}}.{{newNamenodeIndex}}",
        description: "RPC address that handles all clients requests for the new NameNode.",
        isReconfigurable: false,
        recommendedValue: "0.0.0.0:8020",
        value: "{{newNameNode}}:{{nnRpcPort}}",
        category: "HDFS",
        filename: "hdfs-site",
        serviceName: "MISC",
      },
      {
        name: "dfs.namenode.http-address.{{namespaceId}}.{{newNamenodeIndex}}",
        displayName:
          "dfs.namenode.http-address.{{namespaceId}}.{{newNamenodeIndex}}",
        description: "The fully-qualified HTTP address for the new NameNode.",
        isReconfigurable: false,
        recommendedValue: "0.0.0.0:50070",
        value: "{{newNameNode}}:{{nnHttpPort}}",
        category: "HDFS",
        filename: "hdfs-site",
        serviceName: "MISC",
      },
      {
        name: "dfs.namenode.https-address.{{namespaceId}}.{{newNamenodeIndex}}",
        displayName:
          "dfs.namenode.https-address.{{namespaceId}}.{{newNamenodeIndex}}",
        description: "The fully-qualified HTTPS address for the new NameNode.",
        isReconfigurable: false,
        recommendedValue: "0.0.0.0:50470",
        value: "{{newNameNode}}:{{nnHttpsPort}}",
        category: "HDFS",
        filename: "hdfs-site",
        serviceName: "MISC",
      },
      {
        name: "dfs.client.failover.proxy.provider.{{namespaceId}}",
        displayName: "dfs.client.failover.proxy.provider.{{namespaceId}}",
        description:
          "The prefix for a given nameservice, contains a list of the RPC addresses for the namenodes.",
        isReconfigurable: false,
        recommendedValue:
          "org.apache.hadoop.hdfs.server.namenode.ha.ObserverReadProxyProvider",
        value:
          "org.apache.hadoop.hdfs.server.namenode.ha.ObserverReadProxyProvider",
        category: "HDFS",
        filename: "hdfs-site",
        serviceName: "MISC",
      },
      {
        name: "dfs.client.failover.observer.auto-msync-period.{{namespaceId}}",
        displayName:
          "dfs.client.failover.observer.auto-msync-period.{{namespaceId}}",
        description:
          "The auto msync period for observer reads, controlling how often the client syncs with the active NameNode.",
        isReconfigurable: false,
        recommendedValue: "500ms",
        value: "500ms",
        category: "HDFS",
        filename: "hdfs-site",
        serviceName: "MISC",
      },
      {
        name: "dfs.namenode.state.context.enabled",
        displayName: "dfs.namenode.state.context.enabled",
        description:
          "Enables the NameNode to include state context in RPC responses, required for observer reads.",
        isReconfigurable: false,
        recommendedValue: "true",
        value: "true",
        category: "HDFS",
        filename: "hdfs-site",
        serviceName: "MISC",
      },
      {
        name: "dfs.ha.tail-edits.in-progress",
        displayName: "dfs.ha.tail-edits.in-progress",
        description:
          "Enables tailing of in-progress edit log segments, required for low-latency observer reads.",
        isReconfigurable: false,
        recommendedValue: "true",
        value: "true",
        category: "HDFS",
        filename: "hdfs-site",
        serviceName: "MISC",
      },
      {
        name: "dfs.ha.tail-edits.period",
        displayName: "dfs.ha.tail-edits.period",
        description:
          "How often the standby/observer NameNode should tail edits from the JournalNodes.",
        isReconfigurable: false,
        recommendedValue: "0ms",
        value: "0ms",
        category: "HDFS",
        filename: "hdfs-site",
        serviceName: "MISC",
      },
      {
        name: "dfs.ha.tail-edits.period.backoff-max",
        displayName: "dfs.ha.tail-edits.period.backoff-max",
        description:
          "The maximum backoff period between edit tailing attempts when no new edits are available.",
        isReconfigurable: false,
        recommendedValue: "10s",
        value: "10s",
        category: "HDFS",
        filename: "hdfs-site",
        serviceName: "MISC",
      },
      {
        name: "dfs.journalnode.edit-cache-size.bytes",
        displayName: "dfs.journalnode.edit-cache-size.bytes",
        description:
          "The size of the in-memory cache of edits on the JournalNode, used to serve edits to observer/standby NameNodes.",
        isReconfigurable: false,
        recommendedValue: "1048576",
        value: "1048576",
        category: "HDFS",
        filename: "hdfs-site",
        serviceName: "MISC",
      },
    ],
  };

  return { observerNnConfig };
};
