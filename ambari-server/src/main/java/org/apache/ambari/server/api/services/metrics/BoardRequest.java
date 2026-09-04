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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BoardRequest {
  private Long groupId;
  private String name;
  private String ident;
  private String tags;
  private Integer publicFlag;
  private Integer builtIn;
  private Integer hidden;
  private Long publicCategory;
  private String displayLocations;
  private String configs;

  @JsonProperty("group_id") public Long getGroupId() { return groupId; }
  @JsonProperty("group_id") public void setGroupId(Long groupId) { this.groupId = groupId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getIdent() { return ident; }
  public void setIdent(String ident) { this.ident = ident; }
  public String getTags() { return tags; }
  public void setTags(String tags) { this.tags = tags; }
  @JsonProperty("public") public Integer getPublicFlag() { return publicFlag; }
  @JsonProperty("public") public void setPublicFlag(Integer publicFlag) { this.publicFlag = publicFlag; }
  @JsonProperty("built_in") public Integer getBuiltIn() { return builtIn; }
  @JsonProperty("built_in") public void setBuiltIn(Integer builtIn) { this.builtIn = builtIn; }
  @JsonProperty("hide") public Integer getHidden() { return hidden; }
  @JsonProperty("hide") public void setHidden(Integer hidden) { this.hidden = hidden; }
  @JsonProperty("public_cate") public Long getPublicCategory() { return publicCategory; }
  @JsonProperty("public_cate") public void setPublicCategory(Long publicCategory) { this.publicCategory = publicCategory; }
  @JsonProperty("display_locations") public String getDisplayLocations() { return displayLocations; }
  @JsonProperty("display_locations") public void setDisplayLocations(String displayLocations) { this.displayLocations = displayLocations; }
  public String getConfigs() { return configs; }
  public void setConfigs(String configs) { this.configs = configs; }
}
