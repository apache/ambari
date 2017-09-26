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

import org.apache.ambari.logsearch.conf.global.SolrAuditLogsState;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;
import org.apache.ambari.logsearch.conf.global.SolrServiceLogsState;
import org.apache.ambari.logsearch.conf.global.SolrEventHistoryState;
import org.apache.ambari.logsearch.dao.SolrSchemaFieldDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableSolrRepositories
@EnableScheduling
public class SolrConfig {

  @Bean(name = "serviceSolrTemplate")
  public SolrTemplate serviceSolrTemplate() {
    return null;
  }

  @Bean(name = "auditSolrTemplate")
  @DependsOn("serviceSolrTemplate")
  public SolrTemplate auditSolrTemplate() {
    return null;
  }

  @Bean(name = "eventHistorySolrTemplate")
  @DependsOn("serviceSolrTemplate")
  public SolrTemplate eventHistorySolrTemplate() {
    return null;
  }

  @Bean
  @DependsOn({"serviceSolrTemplate", "auditSolrTemplate"})
  public SolrSchemaFieldDao solrSchemaFieldDao() {
    return new SolrSchemaFieldDao();
  }

  @Bean(name = "solrServiceLogsState")
  public SolrCollectionState solrServiceLogsState() {
    return new SolrServiceLogsState();
  }

  @Bean(name = "solrAuditLogsState")
  public SolrCollectionState solrAuditLogsState() {
    return new SolrAuditLogsState();
  }

  @Bean(name = "solrEventHistoryState")
  public SolrCollectionState solrEventHistoryState() {
    return new SolrEventHistoryState();
  }
}

