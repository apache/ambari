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

import org.hibernate.validator.constraints.NotBlank;

public class SolrQueryProperties {
  @NotBlank
  private String collection;
  @NotBlank
  private String queryText;
  private String filterQueryText;
  private String[] sort;

  public String getCollection() {
    return collection;
  }

  public void setCollection(String collection) {
    this.collection = collection;
  }

  public String getQueryText() {
    return queryText;
  }

  public void setQueryText(String queryText) {
    this.queryText = queryText;
  }

  public String getFilterQueryText() {
    return filterQueryText;
  }

  public void setFilterQueryText(String filterQueryText) {
    this.filterQueryText = filterQueryText;
  }

  public String[] getSort() {
    return sort;
  }

  public void setSort(String[] sort) {
    this.sort = sort;
  }

  public SolrQueryBuilder toQueryBuilder() {
    return new SolrQueryBuilder().
            setQueryText(queryText)
            .setFilterQueryText(filterQueryText)
            .addSort(sort);
  }
}
