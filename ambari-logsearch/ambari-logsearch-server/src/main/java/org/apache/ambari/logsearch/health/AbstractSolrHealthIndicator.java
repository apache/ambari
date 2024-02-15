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
package org.apache.ambari.logsearch.health;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.solr.core.SolrTemplate;

public abstract class AbstractSolrHealthIndicator extends AbstractHealthIndicator {

  @Override
  protected void doHealthCheck(Health.Builder builder) throws Exception {
    Status status = Status.DOWN;
    String errorDetails = null;
    if (getSolrTemplate() != null && getSolrTemplate().getSolrClient() != null) {
      try {
        SolrClient solrClient = getSolrTemplate().getSolrClient();
        SolrQuery q = new SolrQuery("*:*");
        q.setRows(0);
        QueryResponse response = solrClient.query(q);
        if (response.getStatus() == 0) {
          status = Status.UP;
          if (response.getResults() != null) {
            builder.withDetail("numDocs", response.getResults().getNumFound());
          }
        }
      } catch (Exception e) {
        errorDetails = e.getMessage();
      }
    }
    builder.status(status);
    if (errorDetails != null) {
      builder.withDetail("error", errorDetails);
    }
  }

  public abstract SolrTemplate getSolrTemplate();

}
