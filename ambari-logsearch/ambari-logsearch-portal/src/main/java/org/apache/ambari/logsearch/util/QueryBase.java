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

import org.apache.solr.client.solrj.SolrQuery;

public class QueryBase {

  //Solr Facet Methods
  public void setFacetField(SolrQuery solrQuery, String facetField) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set("facet.field", facetField);
    setFacetLimit(solrQuery, -1);
  }

  public void setJSONFacet(SolrQuery solrQuery, String jsonQuery) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set("json.facet", jsonQuery);
    setFacetLimit(solrQuery, -1);
  }

  public void setFacetSort(SolrQuery solrQuery, String sortType) {
    solrQuery.setFacet(true);
    solrQuery.setFacetSort(sortType);
  }

  public void setFacetPivot(SolrQuery solrQuery, int mincount,
                            String... hirarchy) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set("facet.pivot", hirarchy);
    solrQuery.set("facet.pivot.mincount", mincount);
    setFacetLimit(solrQuery, -1);
  }

  public void setFacetDate(SolrQuery solrQuery, String facetField,
                           String from, String to, String unit) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set("facet.date", facetField);
    solrQuery.set("facet.date.start", from);
    solrQuery.set("facet.date.end", to);
    solrQuery.set("facet.date.gap", unit);
    solrQuery.set("facet.mincount", 0);
    setFacetLimit(solrQuery, -1);
  }

  public void setFacetRange(SolrQuery solrQuery, String facetField,
                            String from, String to, String unit) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set("facet.range", facetField);
    solrQuery.set("facet.range.start", from);
    solrQuery.set("facet.range.end", to);
    solrQuery.set("facet.range.gap", unit);
    solrQuery.set("facet.mincount", 0);
    setFacetLimit(solrQuery, -1);
  }

  public void setFacetLimit(SolrQuery solrQuery, int limit) {
    solrQuery.set("facet.limit", limit);
  }

  //Solr Group Mehtods
  public void setGroupField(SolrQuery solrQuery, String groupField, int rows) {
    solrQuery.set("group", true);
    solrQuery.set("group.field", groupField);
    solrQuery.set("group.main", true);
    setRowCount(solrQuery, rows);
  }

  //Main Query
  public void setMainQuery(SolrQuery solrQuery, String query) {
    String defalultQuery = "*:*";
    if (query == null || query.isEmpty())
      solrQuery.setQuery(defalultQuery);
    else
      solrQuery.setQuery(query);
  }

  public void setStart(SolrQuery solrQuery, int start) {
    if (start > 0) {
      solrQuery.setStart(start);
    } else {
      solrQuery.setStart(0);
    }
  }

  //Set Number of Rows
  public void setRowCount(SolrQuery solrQuery, int rows) {
    if (rows > 0) {
      solrQuery.setRows(rows);
    } else {
      solrQuery.setRows(0);
      solrQuery.remove("sort");
    }
  }

  //Solr Facet Methods
  public void setFacetField(SolrQuery solrQuery, String facetField, int minCount) {
    solrQuery.setFacet(true);
    setRowCount(solrQuery, 0);
    solrQuery.set("facet.field", facetField);
    solrQuery.set("facet.mincount", minCount);
    setFacetLimit(solrQuery, -1);
  }

}
