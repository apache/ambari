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
import org.apache.ambari.logsearch.conf.SolrServiceLogPropsConfig;
import org.apache.log4j.Logger;
import org.springframework.data.solr.core.SolrTemplate;

@Named
public class ServiceLogsSolrDao extends SolrDaoBase {

  private static final Logger LOG = Logger.getLogger(ServiceLogsSolrDao.class);

  @Inject
  private SolrCollectionDao solrCollectionDao;

  @Inject
  private SolrServiceLogPropsConfig solrServiceLogPropsConfig;

  @Inject
  @Named("serviceSolrTemplate")
  private SolrTemplate serviceSolrTemplate;

  @Inject
  private SolrSchemaFieldDao solrSchemaFieldDao;

  public ServiceLogsSolrDao() {
    super(LogType.SERVICE);
  }

  @Override
  public SolrTemplate getSolrTemplate() {
    return serviceSolrTemplate;
  }

  @PostConstruct
  public void postConstructor() {
    LOG.info("postConstructor() called.");
    try {
      solrCollectionDao.checkSolrStatus(getSolrClient());
      solrCollectionDao.setupCollections(getSolrClient(), solrServiceLogPropsConfig);
      solrSchemaFieldDao.serviceCollectionSetUp();
    } catch (Exception e) {
      LOG.error("error while connecting to Solr for service logs : solrUrl=" + solrServiceLogPropsConfig.getSolrUrl()
        + ", zkConnectString=" + solrServiceLogPropsConfig.getZkConnectString()
        + ", collection=" + solrServiceLogPropsConfig.getCollection(), e);
    }
  }

  public SolrSchemaFieldDao getSolrSchemaFieldDao() {
    return solrSchemaFieldDao;
  }
}
