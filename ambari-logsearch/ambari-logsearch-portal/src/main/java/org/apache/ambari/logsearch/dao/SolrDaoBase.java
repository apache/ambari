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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logsearch.common.LogSearchContext;
import org.apache.ambari.logsearch.common.MessageEnums;
import org.apache.ambari.logsearch.manager.ManagerBase.LogType;
import org.apache.ambari.logsearch.model.response.BarGraphData;
import org.apache.ambari.logsearch.model.response.NameValueData;
import org.apache.ambari.logsearch.util.DateUtil;
import org.apache.ambari.logsearch.util.RESTErrorUtil;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

public abstract class SolrDaoBase {

  private static final Logger LOG_PERFORMANCE = Logger.getLogger("org.apache.ambari.logsearch.performance");

  public Map<String, String> schemaFieldNameMap = new HashMap<>();
  public Map<String, String> schemaFieldTypeMap = new HashMap<>();

  private LogType logType;
  
  protected SolrDaoBase(LogType logType) {
    this.logType = logType;
  }

  public QueryResponse process(SolrQuery solrQuery) throws SolrServerException, IOException {
    if (getSolrClient() != null) {
      String event = solrQuery.get("event");
      solrQuery.remove("event");
      QueryResponse queryResponse = getSolrClient().query(solrQuery, METHOD.POST);
      if (event != null && !"/audit/logs/live/count".equalsIgnoreCase(event)) {
        LOG_PERFORMANCE.info("\n Username :- " + LogSearchContext.getCurrentUsername() + " Event :- " + event + " SolrQuery :- " +
            solrQuery + "\nQuery Time Execution :- " + queryResponse.getQTime() + " Total Time Elapsed is :- " +
            queryResponse.getElapsedTime());
      }
      return queryResponse;
    } else {
      throw RESTErrorUtil.createRESTException("Solr configuration improper for " + logType.getLabel() +" logs",
          MessageEnums.ERROR_SYSTEM);
    }
  }

  @SuppressWarnings("unchecked")
  public void extractValuesFromBuckets(SimpleOrderedMap<Object> jsonFacetResponse, String outerField, String innerField,
                                        List<BarGraphData> histogramData) {
    NamedList<Object> stack = (NamedList<Object>) jsonFacetResponse.get(outerField);
    ArrayList<Object> stackBuckets = (ArrayList<Object>) stack.get("buckets");
    for (Object temp : stackBuckets) {
      BarGraphData vBarGraphData = new BarGraphData();

      SimpleOrderedMap<Object> level = (SimpleOrderedMap<Object>) temp;
      String name = ((String) level.getVal(0)).toUpperCase();
      vBarGraphData.setName(name);

      Collection<NameValueData> vNameValues = new ArrayList<>();
      vBarGraphData.setDataCount(vNameValues);
      ArrayList<Object> levelBuckets = (ArrayList<Object>) ((NamedList<Object>) level.get(innerField)).get("buckets");
      for (Object temp1 : levelBuckets) {
        SimpleOrderedMap<Object> countValue = (SimpleOrderedMap<Object>) temp1;
        String value = DateUtil.convertDateWithMillisecondsToSolrDate((Date) countValue.getVal(0));

        String count = "" + countValue.getVal(1);
        NameValueData vNameValue = new NameValueData();
        vNameValue.setName(value);
        vNameValue.setValue(count);
        vNameValues.add(vNameValue);
      }
      histogramData.add(vBarGraphData);
    }
  }

  public abstract CloudSolrClient getSolrClient();
}
