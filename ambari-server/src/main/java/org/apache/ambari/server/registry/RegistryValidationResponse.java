/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.registry;

import java.util.List;

import org.apache.ambari.server.registry.RegistryAdvisorRequest.RegistryAdvisorRequestType;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Registry validation response
 */
public class RegistryValidationResponse extends  RegistryAdvisorResponse {

  @JsonProperty
  private List<RegistryValidationItem> items;

  public List<RegistryValidationItem> getItems() {
    return items;
  }

  public void setItems(List<RegistryValidationItem> items) {
    this.items = items;
  }
  
  /**
   * Constructor
   *
   * @param registryId ID of software registry
   */
  public RegistryValidationResponse(final Long registryId) {
    super(registryId);
  }

  /**
   * Registry advisor request builder
   */
  public static class RegistryValidationResponseBuilder {
    RegistryValidationResponse instance;

    private RegistryValidationResponseBuilder(Long registryId) {
      this.instance = new RegistryValidationResponse(registryId);
    }

    public static RegistryValidationResponseBuilder forRegistry(Long registryId) {
      return new RegistryValidationResponseBuilder(registryId);
    }

    public RegistryValidationResponseBuilder ofType(RegistryAdvisorRequestType requestType) {
      this.instance.setRequestType(requestType);
      return this;
    }

    public RegistryValidationResponseBuilder forScenarios(List<String> selectedScenarios) {
      this.instance.setSelectedScenarios(selectedScenarios);
      return this;
    }

    public RegistryValidationResponseBuilder forMpacks(List<MpackEntry> selectedMpacks) {
      this.instance.setSelectedMpacks(selectedMpacks);
      return this;
    }

    public RegistryValidationResponseBuilder withId(Long id) {
      this.instance.setId(id);
      return this;
    }

    public RegistryValidationResponseBuilder withValidations(List<RegistryValidationItem> items) {
      this.instance.items = items;
      return this;
    }

    public RegistryValidationResponse build() {
      return this.instance;
    }
  }

  /**
   * Registry validation item
   */
  public static class RegistryValidationItem {
    @JsonProperty
    private String type;

    @JsonProperty
    private String level;

    @JsonProperty
    private String message;

    public RegistryValidationItem(String type, String level, String message) {
      this.type = type;
      this.level = level;
      this.message = message;
    }
    public String getType() {
      return type;
    }
    public String getLevel() {
      return level;
    }
    public String getMessage() {
      return message;
    }
  }
  
}
