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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 *  Scenario recommenation entry
 */
public class ScenarioEntry {
  private String scenarioName;
  private RegistryScenario registryScenario;

  public ScenarioEntry(String scenarioName) {
    this.scenarioName = scenarioName;
  }

  /**
   * Get scenario name
   * @return
   */
  @JsonProperty("scenario_name")
  public String getScenarioName() {
    return scenarioName;
  }


  /**
   * Set registry scenario
   * @param registryScenario
   */
  public void setRegistryScenario(RegistryScenario registryScenario) {
    this.registryScenario = registryScenario;
  }

  /**
   * Get registry scenario
   * @return
   */
  @JsonIgnore
  public RegistryScenario getRegistryScenario() {
    return registryScenario;
  }
}
