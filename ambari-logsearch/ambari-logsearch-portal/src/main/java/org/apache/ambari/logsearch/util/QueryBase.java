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

package org.apache.ambari.logsearch.util;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;

public class QueryBase {

  //Solr Facet Methods
  public void setFacetField(SolrQuery solrQuery, String facetField) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set(LogSearchConstants.FACET_FIELD, facetField);
    setFacetLimit(solrQuery, -1);
  }

  public void setJSONFacet(SolrQuery solrQuery, String jsonQuery) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set(LogSearchConstants.FACET_JSON_FIELD, jsonQuery);
    setFacetLimit(solrQuery, -1);
  }

  public void setFacetSort(SolrQuery solrQuery, String sortType) {
    solrQuery.setFacet(true);
    solrQuery.setFacetSort(sortType);
  }

  public void setFacetPivot(SolrQuery solrQuery, int mincount, String... hirarchy) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set(LogSearchConstants.FACET_PIVOT, hirarchy);
    solrQuery.set(LogSearchConstants.FACET_PIVOT_MINCOUNT, mincount);
    setFacetLimit(solrQuery, -1);
  }

  public void setFacetDate(SolrQuery solrQuery, String facetField, String from, String to, String unit) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set(LogSearchConstants.FACET_DATE, facetField);
    solrQuery.set(LogSearchConstants.FACET_DATE_START, from);
    solrQuery.set(LogSearchConstants.FACET_DATE_END, to);
    solrQuery.set(LogSearchConstants.FACET_DATE_GAP, unit);
    solrQuery.set(LogSearchConstants.FACET_MINCOUNT, 0);
    setFacetLimit(solrQuery, -1);
  }

  public void setFacetRange(SolrQuery solrQuery, String facetField, String from, String to, String unit) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set(LogSearchConstants.FACET_RANGE, facetField);
    solrQuery.set(LogSearchConstants.FACET_RANGE_START, from);
    solrQuery.set(LogSearchConstants.FACET_RANGE_END, to);
    solrQuery.set(LogSearchConstants.FACET_RANGE_GAP, unit);
    solrQuery.set(LogSearchConstants.FACET_MINCOUNT, 0);
    setFacetLimit(solrQuery, -1);
  }

  public void setFacetLimit(SolrQuery solrQuery, int limit) {
    solrQuery.set("facet.limit", limit);
  }

  //Solr Group Mehtods
  public void setGroupField(SolrQuery solrQuery, String groupField, int rows) {
    solrQuery.set(LogSearchConstants.FACET_GROUP, true);
    solrQuery.set(LogSearchConstants.FACET_GROUP_FIELD, groupField);
    solrQuery.set(LogSearchConstants.FACET_GROUP_MAIN, true);
    setRowCount(solrQuery, rows);
  }

  //Main Query
  public void setMainQuery(SolrQuery solrQuery, String query) {
    String defalultQuery = "*:*";
    if (StringUtils.isBlank(query)){
      solrQuery.setQuery(defalultQuery);
    }else{
      solrQuery.setQuery(query);
    }
  }

  public void setStart(SolrQuery solrQuery, int start) {
    int defaultStart = 0;
    if (start > defaultStart) {
      solrQuery.setStart(start);
    } else {
      solrQuery.setStart(defaultStart);
    }
  }

  //Set Number of Rows
  public void setRowCount(SolrQuery solrQuery, int rows) {
    if (rows > 0) {
      solrQuery.setRows(rows);
    } else {
      solrQuery.setRows(0);
      solrQuery.remove(LogSearchConstants.SORT);
    }
  }

  //Solr Facet Methods
  public void setFacetFieldWithMincount(SolrQuery solrQuery, String facetField, int minCount) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set(LogSearchConstants.FACET_FIELD, facetField);
    solrQuery.set(LogSearchConstants.FACET_MINCOUNT, minCount);
    setFacetLimit(solrQuery, -1);
  }
  
  public void setFl(SolrQuery solrQuery,String field){
    solrQuery.set(LogSearchConstants.FL, field);
  }

}
