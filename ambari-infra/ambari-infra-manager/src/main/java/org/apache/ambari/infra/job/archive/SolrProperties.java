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

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.JobParameters;

public class SolrProperties {
  private String zooKeeperConnectionString;
  private String collection;
  private String queryText;
  private String filterQueryText;
  private String[] sortColumn;
  private String deleteQueryText;

  public String getZooKeeperConnectionString() {
    return zooKeeperConnectionString;
  }

  public void setZooKeeperConnectionString(String zooKeeperConnectionString) {
    this.zooKeeperConnectionString = zooKeeperConnectionString;
  }

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

  public String getDeleteQueryText() {
    return deleteQueryText;
  }

  public void setDeleteQueryText(String deleteQueryText) {
    this.deleteQueryText = deleteQueryText;
  }

  public SolrQueryBuilder toQueryBuilder() {
    return new SolrQueryBuilder().
            setQueryText(queryText)
            .setFilterQueryText(filterQueryText)
            .addSort(sortColumn);
  }

  public void validate() {
    if (isBlank(zooKeeperConnectionString))
      throw new IllegalArgumentException("The property zooKeeperConnectionString can not be null or empty string!");

    if (isBlank(collection))
      throw new IllegalArgumentException("The property collection can not be null or empty string!");
  }

  public SolrProperties merge(JobParameters jobParameters) {
    SolrProperties solrProperties = new SolrProperties();
    solrProperties.setZooKeeperConnectionString(jobParameters.getString("zooKeeperConnectionString", zooKeeperConnectionString));
    solrProperties.setCollection(jobParameters.getString("collection", collection));
    solrProperties.setQueryText(jobParameters.getString("queryText", queryText));
    solrProperties.setFilterQueryText(jobParameters.getString("filterQueryText", filterQueryText));
    solrProperties.setDeleteQueryText(jobParameters.getString("deleteQueryText", deleteQueryText));

    String sortValue;
    List<String> sortColumns = new ArrayList<>();
    int i = 0;
    while ((sortValue = jobParameters.getString(String.format("sortColumn[%d]", i))) != null) {
      sortColumns.add(sortValue);
      ++i;
    }
    if (!sortColumns.isEmpty()) {
      solrProperties.setSortColumn(sortColumns.toArray(new String[0]));
    }
    else {
      solrProperties.setSortColumn(sortColumn);
    }

    return solrProperties;
  }
}
