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

import org.apache.ambari.logsearch.common.LogSearchContext;
import org.apache.ambari.logsearch.common.LogType;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.conf.SolrKerberosConfig;
import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.ambari.logsearch.conf.global.SolrCollectionState;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.ambari.logsearch.util.SolrUtil;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.SolrCallback;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.SolrDataQuery;

import javax.inject.Inject;
import java.io.IOException;

public abstract class SolrDaoBase {

  private static final Logger LOG = Logger.getLogger(SolrDaoBase.class);
  private static final Logger LOG_PERFORMANCE = Logger.getLogger("org.apache.ambari.logsearch.performance");

  private LogType logType;

  @Inject
  private SolrKerberosConfig solrKerberosConfig;
  
  protected SolrDaoBase(LogType logType) {
    this.logType = logType;
  }

  public QueryResponse process(SolrQuery solrQuery, String event) {
    SolrUtil.removeDoubleOrTripleEscapeFromFilters(solrQuery);
    LOG.info("Solr query will be processed: " + solrQuery);
    if (getSolrClient() != null) {
      event = event == null ? solrQuery.get("event") : event;
      solrQuery.remove("event");
      try {
        QueryResponse queryResponse = getSolrClient().query(solrQuery, METHOD.POST);
        if (event != null) {
          LOG_PERFORMANCE.info("\n Username :- " + LogSearchContext.getCurrentUsername() + " Event :- " + event + " SolrQuery :- " +
            solrQuery + "\nQuery Time Execution :- " + queryResponse.getQTime() + " Total Time Elapsed is :- " +
            queryResponse.getElapsedTime());
        }
        return queryResponse;
      } catch (Exception e){
        LOG.error("Error during solrQuery=" + e);
        throw RESTErrorUtil.createRESTException(MessageEnums.SOLR_ERROR.getMessage().getMessage(), MessageEnums.ERROR_SYSTEM);
      }
    } else {
      throw RESTErrorUtil.createRESTException("Solr configuration improper for " + logType.getLabel() +" logs",
          MessageEnums.ERROR_SYSTEM);
    }
  }

  public QueryResponse process(SolrQuery solrQuery) {
    return process(solrQuery, null);
  }

  public QueryResponse process(SolrDataQuery solrDataQuery) {
    return process(new DefaultQueryParser().doConstructSolrQuery(solrDataQuery));
  }

  public long count(final SolrDataQuery solrDataQuery) {
    return getSolrTemplate().execute(new SolrCallback<Long>() {
      @Override
      public Long doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
        SolrQuery solrQuery = new DefaultQueryParser().doConstructSolrQuery(solrDataQuery);
        solrQuery.setStart(0);
        solrQuery.setRows(0);
        QueryResponse queryResponse = solrClient.query(solrQuery);
        long count = solrClient.query(solrQuery).getResults().getNumFound();
        LOG_PERFORMANCE.info("\n Username :- " + LogSearchContext.getCurrentUsername() + " Count SolrQuery :- " +
          solrQuery + "\nQuery Time Execution :- " + queryResponse.getQTime() + " Total Time Elapsed is :- " +
          queryResponse.getElapsedTime() + " Count result :- " + count);
        return count;
      }
    });
  }

  public QueryResponse process(SolrDataQuery solrDataQuery, String event) {
    return process(new DefaultQueryParser().doConstructSolrQuery(solrDataQuery), event);
  }

  public CloudSolrClient getSolrClient() {
    return (CloudSolrClient) getSolrTemplate().getSolrClient();
  }

  public abstract SolrTemplate getSolrTemplate();

  public abstract void setSolrTemplate(SolrTemplate solrTemplate);

  public abstract SolrCollectionState getSolrCollectionState();

  public abstract SolrPropsConfig getSolrPropsConfig();

  public SolrKerberosConfig getSolrKerberosConfig() {
    return this.solrKerberosConfig;
  }

}
