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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.solr.client.solrj.SolrQuery.ORDER.asc;

public class SolrQueryBuilder {

  public static final Pattern PARAMETER_PATTERN = Pattern.compile("\\$\\{[a-z]+\\}");

  private String queryText;
  private String endValue;
  private String filterQueryText;
  private Document document;
  private String[] sortFields;

  public SolrQueryBuilder() {
    this.queryText = "*:*";
  }

  public SolrQueryBuilder setQueryText(String queryText) {
    this.queryText = queryText;
    return this;
  }

  public SolrQueryBuilder setEndValue(String endValue) {
    this.endValue = endValue;
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

    String query = queryText;
    query = setEndValueOn(query);

    solrQuery.setQuery(query);

    if (filterQueryText != null) {
      String filterQuery = filterQueryText;
      filterQuery = setEndValueOn(filterQuery);

      Set<String> paramNames = collectParamNames(filterQuery);
      if (document != null) {
        for (String parameter : paramNames) {
          if (document.get(parameter) != null)
            filterQuery = filterQuery.replace(String.format("${%s}", parameter), document.get(parameter));
        }
      }

      if (document == null && paramNames.isEmpty() || document != null && !paramNames.isEmpty())
        solrQuery.setFilterQueries(filterQuery);
    }

    if (sortFields != null) {
      for (String field : sortFields)
        solrQuery.addSort(field, asc);
    }

    return solrQuery;
  }

  private String setEndValueOn(String query) {
    if (endValue != null)
      query = query.replace("${end}", endValue);
    return query;
  }

  private Set<String> collectParamNames(String filterQuery) {
    Matcher matcher = PARAMETER_PATTERN.matcher(filterQuery);
    Set<String> parameters = new HashSet<>();
    while (matcher.find())
      parameters.add(matcher.group().replace("${", "").replace("}", ""));
    return parameters;
  }
}
