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

package org.apache.ambari.infra.solr;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer;
import org.apache.solr.common.cloud.SolrZkClient;

public class AmbariSolrCloudClientBuilder {
  private static final String KEYSTORE_LOCATION_ARG = "javax.net.ssl.keyStore";
  private static final String KEYSTORE_PASSWORD_ARG = "javax.net.ssl.keyStorePassword";
  private static final String KEYSTORE_TYPE_ARG = "javax.net.ssl.keyStoreType";
  private static final String TRUSTSTORE_LOCATION_ARG = "javax.net.ssl.trustStore";
  private static final String TRUSTSTORE_PASSWORD_ARG = "javax.net.ssl.trustStorePassword";
  private static final String TRUSTSTORE_TYPE_ARG = "javax.net.ssl.trustStoreType";
  
  String zkConnectString;
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
  String znode;
  String saslUsers;
  String propName;
  String propValue;
  String securityJsonLocation;
  boolean secure;

  public AmbariSolrCloudClient build() {
    return new AmbariSolrCloudClient(this);
  }

  public AmbariSolrCloudClientBuilder withZkConnectString(String zkConnectString) {
    this.zkConnectString = zkConnectString;
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
    this.solrCloudClient = new CloudSolrClient.Builder().withZkHost(this.zkConnectString).build();
    return this;
  }

  public AmbariSolrCloudClientBuilder withSolrZkClient(int zkClientTimeout, int zkClientConnectTimeout) {
    this.solrZkClient = new SolrZkClient(this.zkConnectString, zkClientTimeout, zkClientConnectTimeout);
    return this;
  }

  public AmbariSolrCloudClientBuilder withKeyStoreLocation(String keyStoreLocation) {
    if (keyStoreLocation != null) {
      System.setProperty(KEYSTORE_LOCATION_ARG, keyStoreLocation);
    }
    return this;
  }

  public AmbariSolrCloudClientBuilder withKeyStorePassword(String keyStorePassword) {
    if (keyStorePassword != null) {
      System.setProperty(KEYSTORE_PASSWORD_ARG, keyStorePassword);
    }
    return this;
  }

  public AmbariSolrCloudClientBuilder withKeyStoreType(String keyStoreType) {
    if (keyStoreType != null) {
      System.setProperty(KEYSTORE_TYPE_ARG, keyStoreType);
    }
    return this;
  }

  public AmbariSolrCloudClientBuilder withTrustStoreLocation(String trustStoreLocation) {
    if (trustStoreLocation != null) {
      System.setProperty(TRUSTSTORE_LOCATION_ARG, trustStoreLocation);
    }
    return this;
  }

  public AmbariSolrCloudClientBuilder withTrustStorePassword(String trustStorePassword) {
    if (trustStorePassword != null) {
      System.setProperty(TRUSTSTORE_PASSWORD_ARG, trustStorePassword);
    }
    return this;
  }

  public AmbariSolrCloudClientBuilder withTrustStoreType(String trustStoreType) {
    if (trustStoreType != null) {
      System.setProperty(TRUSTSTORE_TYPE_ARG, trustStoreType);
    }
    return this;
  }

  public AmbariSolrCloudClientBuilder withSaslUsers(String saslUsers) {
    this.saslUsers = saslUsers;
    return this;
  }

  public AmbariSolrCloudClientBuilder withZnode(String znode) {
    this.znode = znode;
    return this;
  }

  public AmbariSolrCloudClientBuilder withClusterPropName(String clusterPropName) {
    this.propName = clusterPropName;
    return this;
  }

  public AmbariSolrCloudClientBuilder withClusterPropValue(String clusterPropValue) {
    this.propValue = clusterPropValue;
    return this;
  }

  public AmbariSolrCloudClientBuilder withSecurityJsonLocation(String securityJson) {
    this.securityJsonLocation = securityJson;
    return this;
  }

  public AmbariSolrCloudClientBuilder withSecure(boolean isSecure) {
    this.secure = isSecure;
    return this;
  }

  private void setupSecurity(String jaasFile) {
    if (jaasFile != null) {
      System.setProperty("java.security.auth.login.config", jaasFile);
      HttpClientUtil.addConfigurer(new Krb5HttpClientConfigurer());
    }
  }
}
