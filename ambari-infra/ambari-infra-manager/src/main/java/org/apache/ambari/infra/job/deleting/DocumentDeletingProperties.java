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
package org.apache.ambari.infra.job.deleting;

import org.apache.ambari.infra.job.JobProperties;
import org.springframework.batch.core.JobParameters;

import static org.apache.commons.lang.StringUtils.isBlank;

public class DocumentDeletingProperties extends JobProperties<DocumentDeletingProperties> {
  private String zooKeeperConnectionString;
  private String collection;
  private String filterField;

  public DocumentDeletingProperties() {
    super(DocumentDeletingProperties.class);
  }

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

  public String getFilterField() {
    return filterField;
  }

  public void setFilterField(String filterField) {
    this.filterField = filterField;
  }

  @Override
  public void apply(JobParameters jobParameters) {
    zooKeeperConnectionString = jobParameters.getString("zooKeeperConnectionString", zooKeeperConnectionString);
    collection = jobParameters.getString("collection", collection);
    filterField = jobParameters.getString("filterField", filterField);
  }

  @Override
  public void validate() {
    if (isBlank(zooKeeperConnectionString))
      throw new IllegalArgumentException("The property zooKeeperConnectionString can not be null or empty string!");

    if (isBlank(collection))
      throw new IllegalArgumentException("The property collection can not be null or empty string!");

    if (isBlank(filterField))
      throw new IllegalArgumentException("The property filterField can not be null or empty string!");
  }
}
