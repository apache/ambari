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
package org.apache.ambari.infra.job.archive;

import org.apache.solr.client.solrj.SolrQuery;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.solr.client.solrj.SolrQuery.ORDER.asc;

public class SolrQueryBuilder {

  private static final String INTERVAL_START = "start";
  private static final String INTERVAL_END = "end";
  private String queryText;
  private final Map<String, String> interval;
  private String filterQueryText;
  private Document document;
  private String[] sortFields;

  public SolrQueryBuilder() {
    this.queryText = "*:*";
    interval = new HashMap<>();
    interval.put(INTERVAL_START, "*");
    interval.put(INTERVAL_END, "*");
  }

  public SolrQueryBuilder setQueryText(String queryText) {
    this.queryText = queryText;
    return this;
  }

  public SolrQueryBuilder setInterval(String startValue, String endValue) {
    if (isBlank(startValue))
      startValue = "*";
    if (isBlank(endValue))
      endValue = "*";
    this.interval.put(INTERVAL_START, startValue);
    this.interval.put(INTERVAL_END, endValue);
    return this;
  }

  public SolrQueryBuilder setFilterQueryText(String filterQueryText) {
    this.filterQueryText = filterQueryText;
    return this;
  }


  public SolrQueryBuilder setDocument(Document document) {
    this.document = document;
    return this;
  }

  public SolrQueryBuilder addSort(String... sortBy) {
    this.sortFields = sortBy;
    return this;
  }

  public SolrQuery build() {
    SolrQuery solrQuery = new SolrQuery();

    SolrParametrizedString queryText = new SolrParametrizedString(this.queryText).set(interval);
    solrQuery.setQuery(queryText.toString());

    if (filterQueryText != null) {
      SolrParametrizedString filterQuery = new SolrParametrizedString(filterQueryText)
              .set(interval);

      if (document != null) {
        filterQuery = filterQuery.set(document.getFieldMap());
        solrQuery.setFilterQueries(filterQuery.toString());
      }
    }

    if (sortFields != null) {
      for (String field : sortFields)
        solrQuery.addSort(field, asc);
    }

    return solrQuery;
  }
}
