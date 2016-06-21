/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logsearch.solr;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer;
import org.apache.solr.common.cloud.SolrZkClient;

public class AmbariSolrCloudClientBuilder {
  String zookeeperHosts;
  String collection;
  String configSet;
  String configDir;
  int shards = 1;
  int replication = 1;
  int retryTimes = 10;
  int interval = 5;
  int maxShardsPerNode = replication * shards;
  String routerName = "implicit";
  String routerField = "_router_field_";
  CloudSolrClient solrCloudClient;
  SolrZkClient solrZkClient;
  boolean splitting;
  String jaasFile;

  public AmbariSolrCloudClient build() {
    return new AmbariSolrCloudClient(this);
  }

  public AmbariSolrCloudClientBuilder withZookeeperHosts(String zookeeperHosts) {
    this.zookeeperHosts = zookeeperHosts;
    return this;
  }

  public AmbariSolrCloudClientBuilder withCollection(String collection) {
    this.collection = collection;
    return this;
  }

  public AmbariSolrCloudClientBuilder withConfigSet(String configSet) {
    this.configSet = configSet;
    return this;
  }

  public AmbariSolrCloudClientBuilder withConfigDir(String configDir) {
    this.configDir = configDir;
    return this;
  }

  public AmbariSolrCloudClientBuilder withShards(int shards) {
    this.shards = shards;
    return this;
  }

  public AmbariSolrCloudClientBuilder withReplication(int replication) {
    this.replication = replication;
    return this;
  }

  public AmbariSolrCloudClientBuilder withRetry(int retryTimes) {
    this.retryTimes = retryTimes;
    return this;
  }

  public AmbariSolrCloudClientBuilder withInterval(int interval) {
    this.interval = interval;
    return this;
  }

  public AmbariSolrCloudClientBuilder withMaxShardsPerNode(int maxShardsPerNode) {
    this.maxShardsPerNode = maxShardsPerNode;
    return this;
  }

  public AmbariSolrCloudClientBuilder withRouterName(String routerName) {
    this.routerName = routerName;
    return this;
  }

  public AmbariSolrCloudClientBuilder withRouterField(String routerField) {
    this.routerField = routerField;
    return this;
  }

  public AmbariSolrCloudClientBuilder withSplitting(boolean splitting) {
    this.splitting = splitting;
    return this;
  }

  public AmbariSolrCloudClientBuilder withJaasFile(String jaasFile) {
    this.jaasFile = jaasFile;
    setupSecurity(jaasFile);
    return this;
  }

  public AmbariSolrCloudClientBuilder withSolrCloudClient() {
    this.solrCloudClient = new CloudSolrClient(this.zookeeperHosts);
    return this;
  }

  public AmbariSolrCloudClientBuilder withSolrZkClient(int zkClientTimeout, int zkClientConnectTimeout) {
    this.solrZkClient = new SolrZkClient(this.zookeeperHosts, zkClientTimeout, zkClientConnectTimeout);
    return this;
  }

  private void setupSecurity(String jaasFile) {
    if (jaasFile != null) {
      System.setProperty("java.security.auth.login.config", jaasFile);
      HttpClientUtil.setConfigurer(new Krb5HttpClientConfigurer());
    }
  }
}
