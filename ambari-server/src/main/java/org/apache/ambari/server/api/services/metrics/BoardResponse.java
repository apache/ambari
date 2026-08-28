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

import org.apache.ambari.server.orm.entities.BoardEntity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoardResponse {
  private final BoardEntity board;
  private final String configs;

  public BoardResponse(BoardEntity board, String configs) {
    this.board = board;
    this.configs = configs;
  }

  public Long getId() { return board.getId(); }
  @JsonProperty("group_id") public Long getGroupId() { return board.getGroupId(); }
  public String getName() { return board.getName(); }
  public String getIdent() { return board.getIdent(); }
  public String getTags() { return board.getTags(); }
  @JsonProperty("public") public Integer getPublicFlag() { return board.getPublicFlag(); }
  @JsonProperty("built_in") public Integer getBuiltIn() { return board.getBuiltIn(); }
  @JsonProperty("hide") public Integer getHidden() { return board.getHidden(); }
  @JsonProperty("create_at") public Long getCreateAt() { return board.getCreateAt(); }
  @JsonProperty("create_by") public String getCreateBy() { return board.getCreateBy(); }
  @JsonProperty("update_at") public Long getUpdateAt() { return board.getUpdateAt(); }
  @JsonProperty("update_by") public String getUpdateBy() { return board.getUpdateBy(); }
  @JsonProperty("public_cate") public Long getPublicCategory() { return board.getPublicCategory(); }
  @JsonProperty("display_locations") public String getDisplayLocations() { return board.getDisplayLocations(); }
  public String getConfigs() { return configs; }
}
