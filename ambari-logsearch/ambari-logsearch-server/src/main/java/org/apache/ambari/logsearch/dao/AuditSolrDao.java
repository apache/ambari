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

package org.apache.ambari.logsearch.dao;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.ambari.logsearch.common.LogType;
import org.apache.ambari.logsearch.conf.SolrAuditLogPropsConfig;
import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;
import org.apache.ambari.logsearch.configurer.SolrAuditAliasConfigurer;
import org.apache.ambari.logsearch.configurer.SolrCollectionConfigurer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.data.solr.core.SolrTemplate;

@Named
public class AuditSolrDao extends SolrDaoBase {

  private static final Logger LOG = Logger.getLogger(AuditSolrDao.class);

  @Inject
  private SolrAuditLogPropsConfig solrAuditLogPropsConfig;

  @Inject
  @Named("auditSolrTemplate")
  private SolrTemplate auditSolrTemplate;

  @Inject
  @Named("solrAuditLogsState")
  private SolrCollectionState solrAuditLogsState;

  public AuditSolrDao() {
    super(LogType.AUDIT);
  }

  @Override
  public SolrTemplate getSolrTemplate() {
    return auditSolrTemplate;
  }

  @Override
  public void setSolrTemplate(SolrTemplate solrTemplate) {
    this.auditSolrTemplate = solrTemplate;
  }

  @PostConstruct
  public void postConstructor() {
    String aliasNameIn = solrAuditLogPropsConfig.getAliasNameIn();
    String rangerAuditCollection = solrAuditLogPropsConfig.getRangerCollection();

    try {
      new SolrCollectionConfigurer(this, true).start();
      boolean createAlias = (aliasNameIn != null && StringUtils.isNotBlank(rangerAuditCollection));
      if (createAlias) {
        new SolrAuditAliasConfigurer(this).start();
      }
    } catch (Exception e) {
      LOG.error("Error while connecting to Solr for audit logs : solrUrl=" + solrAuditLogPropsConfig.getSolrUrl() + ", zkConnectString=" +
        solrAuditLogPropsConfig.getZkConnectString() + ", collection=" + solrAuditLogPropsConfig.getCollection(), e);
    }
  }

  @Override
  public SolrCollectionState getSolrCollectionState() {
    return this.solrAuditLogsState;
  }

  @Override
  public SolrPropsConfig getSolrPropsConfig() {
    return this.solrAuditLogPropsConfig;
  }


}
