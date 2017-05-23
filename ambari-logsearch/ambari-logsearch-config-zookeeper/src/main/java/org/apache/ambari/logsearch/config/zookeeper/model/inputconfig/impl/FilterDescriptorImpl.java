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

package org.apache.ambari.logsearch.config.zookeeper.model.inputconfig.impl;

import java.util.List;
import java.util.Map;

import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.PostMapValues;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public abstract class FilterDescriptorImpl implements FilterDescriptor {
  @Expose
  private String filter;

  @Expose
  private ConditionsImpl conditions;

  @Expose
  @SerializedName("sort_order")
  private Integer sortOrder;

  @Expose
  @SerializedName("source_field")
  private String sourceField;

  @Expose
  @SerializedName("remove_source_field")
  private Boolean removeSourceField;

  @Expose
  @SerializedName("post_map_values")
  private Map<String, List<PostMapValuesImpl>> postMapValues;

  @Expose
  @SerializedName("is_enabled")
  private Boolean isEnabled;

  public String getFilter() {
    return filter;
  }

  public void setFilter(String filter) {
    this.filter = filter;
  }

  public ConditionsImpl getConditions() {
    return conditions;
  }

  public void setConditions(ConditionsImpl conditions) {
    this.conditions = conditions;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public String getSourceField() {
    return sourceField;
  }

  public void setSourceField(String sourceField) {
    this.sourceField = sourceField;
  }

  public Boolean isRemoveSourceField() {
    return removeSourceField;
  }

  public void setRemoveSourceField(Boolean removeSourceField) {
    this.removeSourceField = removeSourceField;
  }

  public Map<String, ? extends List<? extends PostMapValues>> getPostMapValues() {
    return postMapValues;
  }

  public void setPostMapValues(Map<String, List<PostMapValuesImpl>> postMapValues) {
    this.postMapValues = postMapValues;
  }

  public Boolean isEnabled() {
    return isEnabled;
  }

  public void setIsEnabled(Boolean isEnabled) {
    this.isEnabled = isEnabled;
  }
}
