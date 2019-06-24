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

import static org.apache.ambari.infra.json.StringToDurationConverter.toDuration;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.time.Duration;

import org.apache.ambari.infra.job.JobProperties;
import org.apache.ambari.infra.job.Validatable;
import org.apache.ambari.infra.json.DurationToStringConverter;
import org.apache.ambari.infra.json.StringToDurationConverter;
import org.springframework.batch.core.JobParameters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class DeletingProperties extends JobProperties<DeletingProperties> implements Validatable {
  private String zooKeeperConnectionString;
  private String collection;
  private String filterField;
  private String start;
  private String end;
  @JsonSerialize(converter = DurationToStringConverter.class)
  @JsonDeserialize(converter = StringToDurationConverter.class)
  private Duration ttl;

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

  public String getStart() {
    return start;
  }

  public void setStart(String start) {
    this.start = start;
  }

  public String getEnd() {
    return end;
  }

  public void setEnd(String end) {
    this.end = end;
  }

  public Duration getTtl() {
    return ttl;
  }

  public void setTtl(Duration ttl) {
    this.ttl = ttl;
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

  @Override
  public DeletingProperties merge(JobParameters jobParameters) {
    DeletingProperties deletingProperties = new DeletingProperties();
    deletingProperties.setZooKeeperConnectionString(jobParameters.getString("zooKeeperConnectionString", zooKeeperConnectionString));
    deletingProperties.setCollection(jobParameters.getString("collection", collection));
    deletingProperties.setFilterField(jobParameters.getString("filterField", filterField));
    deletingProperties.setStart(jobParameters.getString("start", "*"));
    deletingProperties.setEnd(jobParameters.getString("end", "*"));
    deletingProperties.setTtl(toDuration(jobParameters.getString("ttl", DurationToStringConverter.toString(ttl))));
    return deletingProperties;
  }
}
