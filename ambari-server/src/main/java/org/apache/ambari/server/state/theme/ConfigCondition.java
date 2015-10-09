/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
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

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigCondition {
  @JsonProperty("configs")
  private List<String> configs;
  @JsonProperty("resource")
  private String resource;
  @JsonProperty("if")
  private String ifLabel;
  @JsonProperty("then")
  private ConfigConditionResult then;
  @JsonProperty("else")
  private ConfigConditionResult elseLabel;

  public List<String> getConfigs() {
    return configs;
  }

  public void setConfigs(List<String> configs) {
    this.configs = configs;
  }

  public String getIfLabel() {
    return ifLabel;
  }

  public void setIfLabel(String ifLabel) {
    this.ifLabel = ifLabel;
  }

  public ConfigConditionResult getThen() {
    return then;
  }

  public void setThen(ConfigConditionResult then) {
    this.then = then;
  }

  public ConfigConditionResult getElseLabel() {
    return elseLabel;
  }

  public void setElseLabel(ConfigConditionResult elseLabel) {
    this.elseLabel = elseLabel;
  }


  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public class ConfigConditionResult {
    @JsonProperty("property_value_attributes")
    private ValueAttributesInfo propertyValueAttributes;

    public ValueAttributesInfo getPropertyValueAttributes() {
      return propertyValueAttributes;
    }

    public void setPropertyValueAttributes(ValueAttributesInfo propertyValueAttributes) {
      this.propertyValueAttributes = propertyValueAttributes;
    }
  }

}
