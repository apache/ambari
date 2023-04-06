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
import org.springframework.batch.core.JobParameters;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;

public class SolrQueryProperties {
  @NotBlank
  private String collection;
  @NotBlank
  private String queryText;
  private String filterQueryText;
  private String[] sortColumn;

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

  public String[] getSortColumn() {
    return sortColumn;
  }

  public void setSortColumn(String[] sortColumn) {
    this.sortColumn = sortColumn;
  }

  public SolrQueryBuilder toQueryBuilder() {
    return new SolrQueryBuilder().
            setQueryText(queryText)
            .setFilterQueryText(filterQueryText)
            .addSort(sortColumn);
  }

  public void apply(JobParameters jobParameters) {
    collection = jobParameters.getString("collection", collection);
    queryText = jobParameters.getString("queryText", queryText);
    filterQueryText = jobParameters.getString("filterQueryText", filterQueryText);

    String sortValue;
    List<String> sortColumns = new ArrayList<>();
    int i = 0;
    while ((sortValue = jobParameters.getString(String.format("sortColumn[%d]", i))) != null) {
      sortColumns.add(sortValue);
      ++i;
    }

    if (sortColumns.size() > 0)
      sortColumn = sortColumns.toArray(new String[sortColumns.size()]);
  }

  public void validate() {
    if (isBlank(collection))
      throw new IllegalArgumentException("The property collection can not be null or empty string!");
  }
}
