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

package org.apache.ambari.server.state.theme;

import org.apache.ambari.server.state.ValueAttributesInfo;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.List;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigPlacement {
	@JsonProperty("config")
	private String config;
	@JsonProperty("subsection-name")
	private String subsectionName;
  @JsonProperty("subsection-tab-name")
  private String subsectionTabName;

  @JsonProperty("property_value_attributes")
  private ValueAttributesInfo propertyValueAttributes;

  @JsonProperty("depends-on")
  private List<ConfigCondition> dependsOn;


  public String getConfig() {
    return config;
  }

  public void setConfig(String config) {
    this.config = config;
  }

  public String getSubsectionName() {
    return subsectionName;
  }

  public void setSubsectionName(String subsectionName) {
    this.subsectionName = subsectionName;
  }

  public String getSubsectionTabName() {
    return subsectionTabName;
  }

  public void setSubsectionTabName(String subsectionTabName) {
    this.subsectionTabName = subsectionTabName;
  }

  public ValueAttributesInfo getPropertyValueAttributes() {
    return propertyValueAttributes;
  }

  public void setPropertyValueAttributes(ValueAttributesInfo propertyValueAttributes) {
    this.propertyValueAttributes = propertyValueAttributes;
  }

  public List<ConfigCondition> getDependsOn() {
    return dependsOn;
  }

  public void setDependsOn(List<ConfigCondition> dependsOn) {
    this.dependsOn = dependsOn;
  }

  public boolean isRemoved() {
    return subsectionName == null;
  }

  public void mergeWithParent(ConfigPlacement parent) {
    if (subsectionName == null) {
      subsectionName = parent.subsectionName;
    }
  }
}
