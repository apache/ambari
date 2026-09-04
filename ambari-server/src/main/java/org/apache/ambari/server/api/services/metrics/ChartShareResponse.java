/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.api.services.metrics;

import org.apache.ambari.server.orm.entities.ChartShareEntity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChartShareResponse {
  private final ChartShareEntity entity;

  public ChartShareResponse(ChartShareEntity entity) {
    this.entity = entity;
  }

  public Long getId() { return entity.getId(); }
  public String getCluster() { return entity.getCluster(); }
  @JsonProperty("datasource_id") public Long getDatasourceId() { return entity.getDatasourceId(); }
  public String getConfigs() { return entity.getConfigs(); }
  @JsonProperty("create_at") public Long getCreateAt() { return entity.getCreateAt(); }
  @JsonProperty("create_by") public String getCreateBy() { return entity.getCreateBy(); }
}
