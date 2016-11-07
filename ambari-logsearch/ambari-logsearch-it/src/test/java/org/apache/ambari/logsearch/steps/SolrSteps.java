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
package org.apache.ambari.logsearch.steps;

import org.apache.ambari.logsearch.domain.StoryDataRegistry;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.junit.Assert;

import java.io.IOException;

public class SolrSteps {

  @Then("the number of <component> docs is: <docSize>")
  public void numberOfDocsForComponent(@Named("component") String component, @Named("docSize") int docSize)
    throws IOException, SolrServerException, InterruptedException {
    SolrClient solrClient = StoryDataRegistry.INSTANCE.getCloudSolrClient();
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery(String.format("type:%s", component));
    solrQuery.setStart(0);
    solrQuery.setRows(20);
    QueryResponse queryResponse = solrClient.query(StoryDataRegistry.INSTANCE.getServiceLogsCollection(), solrQuery);
    SolrDocumentList list = queryResponse.getResults();
    Assert.assertEquals(docSize, list.size());
  }
}
