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
package org.apache.ambari.logsearch.conf;

import org.apache.ambari.logsearch.solr.AmbariSolrCloudClient;
import org.apache.ambari.logsearch.solr.AmbariSolrCloudClientBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

import javax.inject.Inject;

@Configuration
@EnableSolrRepositories
public class SolrConfig {

  private static final Logger LOG = LoggerFactory.getLogger(SolrConfig.class);

  @Inject
  private SolrServiceLogPropsConfig solrServiceLogPropsConfig;

  @Inject
  private SolrAuditLogPropsConfig solrAuditLogPropsConfig;

  @Inject
  private SolrUserPropsConfig solrUserConfigPropsConfig;

  @Inject
  private SolrKerberosConfig solrKerberosConfig;

  @Bean(name = "serviceSolrTemplate")
  public SolrTemplate serviceSolrTemplate() {
    setupSecurity();
    return new SolrTemplate(createClient(
      solrServiceLogPropsConfig.getSolrUrl(),
      solrServiceLogPropsConfig.getZkConnectString(),
      solrServiceLogPropsConfig.getCollection()));
  }

  @Bean(name = "auditSolrTemplate")
  @DependsOn("serviceSolrTemplate")
  public SolrTemplate auditSolrTemplate() {
    return new SolrTemplate(createClient(
      solrAuditLogPropsConfig.getSolrUrl(),
      solrAuditLogPropsConfig.getZkConnectString(),
      solrAuditLogPropsConfig.getCollection()));
  }

  @Bean(name = "userConfigSolrTemplate")
  @DependsOn("serviceSolrTemplate")
  public SolrTemplate userConfigSolrTemplate() {
    return new SolrTemplate(createClient(
      solrUserConfigPropsConfig.getSolrUrl(),
      solrUserConfigPropsConfig.getZkConnectString(),
      solrUserConfigPropsConfig.getCollection()));
  }

  private CloudSolrClient createClient(String solrUrl, String zookeeperConnectString, String defaultCollection) {
    if (StringUtils.isNotEmpty(zookeeperConnectString)) {
      CloudSolrClient cloudSolrClient = new CloudSolrClient(zookeeperConnectString);
      cloudSolrClient.setDefaultCollection(defaultCollection);
      return cloudSolrClient;
    } else if (StringUtils.isNotEmpty(solrUrl)) {
      throw new UnsupportedOperationException("Currently only cloud mode is supported. Set zookeeper connect string.");
    }
    throw new IllegalStateException(
      "Solr url or zookeeper connection string is missing. collection: " + defaultCollection);
  }

  private void setupSecurity() {
    String jaasFile = solrKerberosConfig.getJaasFile();
    boolean securityEnabled = solrKerberosConfig.isEnabled();
    if (securityEnabled) {
      System.setProperty("java.security.auth.login.config", jaasFile);
      HttpClientUtil.setConfigurer(new Krb5HttpClientConfigurer());
      LOG.info("setupSecurity() called for kerberos configuration, jaas file: " + jaasFile);
    }
  }
}

