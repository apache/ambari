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

/**
 * Abstract registry advisor response POJO
 */
public abstract class RegistryAdvisorResponse {
  private Long id;
  private Long registryId;
  private RegistryAdvisorRequest.RegistryAdvisorRequestType requestType;
  private List<String> selectedScenarios;
  private List<MpackEntry> selectedMpacks;

  /**
   * Set registry advisor request id
   * @param id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Get registry advisor request id
   * @return
   */
  public Long getId() {
    return id;
  }

  /**
   * Set registry id
   * @param registryId
   */
  public void setRegistryId(Long registryId) {
    this.registryId = registryId;
  }

  /**
   * Get registry id
   * @return
   */
  public Long getRegistryId() {
    return registryId;
  }

  /**
   * Set request type
   * @param requestType
   */
  public void setRequestType(RegistryAdvisorRequestType requestType) {
    this.requestType = requestType;
  }

  /**
   * Get request type
   * @return
   */
  public RegistryAdvisorRequestType getRequestType() {
    return requestType;
  }

  /**
   * Set selected scenarios
   * @param selectedScenarios
   */
  public void setSelectedScenarios(List<String> selectedScenarios) {
    this.selectedScenarios = selectedScenarios;
  }

  /**
   * Get selected scenarios
   * @return
   */
  public List<String> getSelectedScenarios() {
    return selectedScenarios;
  }

  /**
   * Set selected mpacks
   * @param selectedMpacks
   */
  public void setSelectedMpacks(List<MpackEntry> selectedMpacks) {
    this.selectedMpacks = selectedMpacks;
  }

  /**
   * Get selected mpacks
   * @return
   */
  public List<MpackEntry> getSelectedMpacks() {
    return selectedMpacks;
  }

  /**
   * Constructor
   * @param registryId          ID of software registry
   */
  public RegistryAdvisorResponse(Long registryId) {
    this.registryId = registryId;
  }
}
