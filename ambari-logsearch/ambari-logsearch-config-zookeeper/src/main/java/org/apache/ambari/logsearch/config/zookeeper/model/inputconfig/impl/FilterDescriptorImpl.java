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

import org.apache.ambari.logsearch.config.api.ShipperConfigElementDescription;
import org.apache.ambari.logsearch.config.api.ShipperConfigTypeDescription;
import org.apache.ambari.logsearch.config.api.model.inputconfig.FilterDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.PostMapValues;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@ShipperConfigTypeDescription(
  name = "Filter",
  description = "The filter element in the [input configuration](inputConfig.md) contains a list of filter descriptions, each describing one filter applied on an input.\n" +
                "\n" +
                "The general elements in the json are the following:"
)
public abstract class FilterDescriptorImpl implements FilterDescriptor {
  @ShipperConfigElementDescription(
    path = "/filter/[]/filter",
    type = "string",
    description = "The type of the filter.",
    examples = {"grok", "keyvalue", "json"}
  )
  @Expose
  private String filter;

  @ShipperConfigElementDescription(
    path = "/filter/[]/conditions",
    type = "json object",
    description = "The conditions of which input to filter."
  )
  @Expose
  private ConditionsImpl conditions;

  @ShipperConfigElementDescription(
    path = "/filter/[]/sort_order",
    type = "integer",
    description = "Describes the order in which the filters should be applied.",
    examples = {"1", "3"}
  )
  @Expose
  @SerializedName("sort_order")
  private Integer sortOrder;

  @ShipperConfigElementDescription(
    path = "/filter/[]/source_field",
    type = "integer",
    description = "The source of the filter, must be set for keyvalue filters.",
    examples = {"field_further_to_filter"},
    defaultValue = "log_message"
  )
  @Expose
  @SerializedName("source_field")
  private String sourceField;

  @ShipperConfigElementDescription(
    path = "/filter/[]/remove_source_field",
    type = "boolean",
    description = "Remove the source field after the filter is applied.",
    examples = {"true", "false"},
    defaultValue = "false"
  )
  @Expose
  @SerializedName("remove_source_field")
  private Boolean removeSourceField;

  @ShipperConfigElementDescription(
    path = "/filter/[]/post_map_values",
    type = "dictionary string to list of json objects",
    description = "Mappings done after the filtering provided it's result."
  )
  @Expose
  @SerializedName("post_map_values")
  private Map<String, List<PostMapValuesImpl>> postMapValues;

  @ShipperConfigElementDescription(
    path = "/filter/[]/is_enabled",
    type = "boolean",
    description = "A flag to show if the filter should be used.",
    examples = {"true", "false"},
    defaultValue = "true"
  )
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
