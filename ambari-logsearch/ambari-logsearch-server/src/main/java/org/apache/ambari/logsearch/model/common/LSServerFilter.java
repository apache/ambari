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

package org.apache.ambari.logsearch.model.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.PostMapValues;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonInclude(Include.NON_NULL)
public abstract class LSServerFilter {
  @NotNull
  private String filter;
  
  @Valid
  @NotNull
  private LSServerConditions conditions;
  
  @JsonProperty("sort_order")
  private Integer sortOrder;
  
  @JsonProperty("source_field")
  private String sourceField;
  
  @JsonProperty("remove_source_field")
  private Boolean removeSourceField;
  
  @Valid
  @JsonProperty("post_map_values")
  private Map<String, LSServerPostMapValuesList> postMapValues;
  
  @JsonProperty("is_enabled")
  private Boolean isEnabled;
  
  public LSServerFilter() {}

  public LSServerFilter(FilterDescriptor filterDescriptor) {
    this.filter = filterDescriptor.getFilter();
    this.conditions = new LSServerConditions(filterDescriptor.getConditions());
    this.sortOrder = filterDescriptor.getSortOrder();
    this.sourceField = filterDescriptor.getSourceField();
    this.removeSourceField = filterDescriptor.isRemoveSourceField();
    
    if (filterDescriptor.getPostMapValues() != null) {
      this.postMapValues = new HashMap<String, LSServerPostMapValuesList>();
      for (Map.Entry<String, ? extends List<? extends PostMapValues>> e : filterDescriptor.getPostMapValues().entrySet()) {
        LSServerPostMapValuesList lsServerPostMapValuesList = new LSServerPostMapValuesList(e.getValue());
        postMapValues.put(e.getKey(), lsServerPostMapValuesList);
      }
    }
    
    this.isEnabled = filterDescriptor.isEnabled();
  }

  public String getFilter() {
    return filter;
  }

  public void setFilter(String filter) {
    this.filter = filter;
  }

  public LSServerConditions getConditions() {
    return conditions;
  }

  public void setConditions(LSServerConditions conditions) {
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

  public Boolean getRemoveSourceField() {
    return removeSourceField;
  }

  public void setRemoveSourceField(Boolean removeSourceField) {
    this.removeSourceField = removeSourceField;
  }

  public Map<String, LSServerPostMapValuesList> getPostMapValues() {
    return postMapValues;
  }

  public void setPostMapValues(Map<String, LSServerPostMapValuesList> postMapValues) {
    this.postMapValues = postMapValues;
  }

  public Boolean getIsEnabled() {
    return isEnabled;
  }

  public void setIsEnabled(Boolean isEnabled) {
    this.isEnabled = isEnabled;
  }
}
