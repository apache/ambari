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
package org.apache.ambari.server.controller;

import java.util.List;

import org.apache.ambari.server.controller.internal.RegistryScenarioResourceProvider;
import org.apache.ambari.server.registry.RegistryScenarioMpack;

import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a registry scenario response.
 */
public class RegistryScenarioResponse {
  private Long registryId;
  private String scenarioName;
  private String scenarioDescription;
  private List<? extends RegistryScenarioMpack> scenarioMpacks;

  /**
   * Constructor
   * @param registryId          registry id
   * @param scenarioName        scenario name
   * @param scenarioDescription scenario description
   * @param scenarioMpacks      list of scenario mpacks
   */
  public RegistryScenarioResponse(
    Long registryId, String scenarioName, String scenarioDescription, List<? extends RegistryScenarioMpack> scenarioMpacks) {
    this.registryId = registryId;
    this.scenarioName = scenarioName;
    this.scenarioDescription = scenarioDescription;
    this.scenarioMpacks = scenarioMpacks;
  }

  /**
   * Get registry id
   * @return
   */
  public Long getRegistryId() {
    return registryId;
  }

  /**
   * Get scenario name
   * @return
   */
  public String getScenarioName() {
    return scenarioName;
  }

  /**
   * Get scenario description
   * @return
   */
  public String getScenarioDescription() {
    return scenarioDescription;
  }

  /**
   * Get list of scenario
   * @return
   */
  public List<? extends RegistryScenarioMpack> getScenarioMpacks() {
    return scenarioMpacks;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = 1;
    result = 31 + getRegistryId().hashCode();
    result = 31 + getScenarioName().hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RegistryScenarioResponse)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RegistryScenarioResponse registryScenarioResponse = (RegistryScenarioResponse) obj;
    return (getRegistryId().equals(registryScenarioResponse.getRegistryId()) &&
      getScenarioName().equals(registryScenarioResponse.getScenarioName()));
  }

  public interface RegistryScenarioResponseWrapper extends ApiModel {
    @ApiModelProperty(name = RegistryScenarioResourceProvider.RESPONSE_KEY)
    @SuppressWarnings("unused")
    RegistryScenarioResponse getRegistryScenarioResponse();
  }
}
