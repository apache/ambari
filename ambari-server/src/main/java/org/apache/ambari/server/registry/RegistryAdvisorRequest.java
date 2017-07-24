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

import java.util.Arrays;
import java.util.List;

import org.apache.ambari.server.exceptions.RegistryAdvisorException;

/**
 * Represents a registry advisor request
 */
public class RegistryAdvisorRequest {
  private Long registryId;
  private RegistryAdvisorRequestType requestType;
  private List<String> selectedScenarios;
  private List<MpackEntry> selectedMpacks;

  /**
   * Constructor
   * @param registryId          ID of software registry
   */
  public RegistryAdvisorRequest(Long registryId) {
    this.registryId = registryId;
  }

  public Long getRegistryId() {
    return registryId;
  }

  public RegistryAdvisorRequestType getRequestType() {
    return requestType;
  }

  public List<String> getSelectedScenarios() {
    return selectedScenarios;
  }

  public List<MpackEntry> getSelectedMpacks() {
    return selectedMpacks;
  }

  /**
   * Registry advisor request builder
   */
  public static class RegistryAdvisorRequestBuilder {
    RegistryAdvisorRequest instance;

    private RegistryAdvisorRequestBuilder(Long registryId) {
      this.instance = new RegistryAdvisorRequest(registryId);
    }

    public static RegistryAdvisorRequestBuilder forRegistry(Long registryId) {
      return new RegistryAdvisorRequestBuilder(registryId);
    }

    public RegistryAdvisorRequestBuilder ofType(RegistryAdvisorRequestType requestType) {
      this.instance.requestType = requestType;
      return this;
    }

    public RegistryAdvisorRequestBuilder forScenarios(List<String> selectedScenarios) {
      this.instance.selectedScenarios = selectedScenarios;
      return this;
    }

    public RegistryAdvisorRequestBuilder forMpacks(List<MpackEntry> selectedMpacks) {
      this.instance.selectedMpacks = selectedMpacks;
      return this;
    }

    public RegistryAdvisorRequest build() {
      return this.instance;
    }
  }

  /**
   *
   */
  public enum RegistryAdvisorRequestType {
    SCENARIO_MPACKS("scenario-mpacks");

    private String type;

    RegistryAdvisorRequestType(String type) {
      this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return type;
    }

    /**
     * Get registry advisor request type from string
     * @param text  string input
     * @return  {@link RegistryAdvisorRequestType}
     * @throws RegistryAdvisorException
     */
    public static RegistryAdvisorRequestType fromString(String text) throws RegistryAdvisorException {
      if (text != null) {
        for (RegistryAdvisorRequestType next : RegistryAdvisorRequestType.values()) {
          if (text.equalsIgnoreCase(next.type)) {
            return next;
          }
        }
      }
      throw new RegistryAdvisorException(String.format(
        "Unknown request type: %s, possible values: %s", text,
        Arrays.toString(RegistryAdvisorRequestType.values())));
    }
  }
}
