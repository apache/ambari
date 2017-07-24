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

import java.util.Collection;
import java.util.List;

import org.apache.ambari.server.registry.RegistryAdvisorRequest.RegistryAdvisorRequestType;

/**
 * Registry recommendation response
 */
public class RegistryRecommendationResponse extends RegistryAdvisorResponse {


  private RegistryRecommendations recommendations;

  /**
   * Constructor
   * @param registryId          ID of software registry
   */
  public RegistryRecommendationResponse(Long registryId) {
    super(registryId);
  }

  /**
   * Set recommendations
   * @param recommendations
   */
  public void setRecommendations(RegistryRecommendations recommendations) {
    this.recommendations = recommendations;
  }

  /**
   * Get recommentations
   * @return
   */
  public RegistryRecommendations getRecommendations() {
    return recommendations;
  }

  /**
   * Registry advisor request builder
   */
  public static class RegistryRecommendationResponseBuilder {
    RegistryRecommendationResponse instance;

    private RegistryRecommendationResponseBuilder(Long registryId) {
      this.instance = new RegistryRecommendationResponse(registryId);
    }

    public static RegistryRecommendationResponseBuilder forRegistry(Long registryId) {
      return new RegistryRecommendationResponseBuilder(registryId);
    }

    public RegistryRecommendationResponseBuilder ofType(RegistryAdvisorRequestType requestType) {
      this.instance.setRequestType(requestType);
      return this;
    }

    public RegistryRecommendationResponseBuilder forScenarios(List<String> selectedScenarios) {
      this.instance.setSelectedScenarios(selectedScenarios);
      return this;
    }

    public RegistryRecommendationResponseBuilder forMpacks(List<MpackEntry> selectedMpacks) {
      this.instance.setSelectedMpacks(selectedMpacks);
      return this;
    }

    public RegistryRecommendationResponseBuilder withId(Long id) {
      this.instance.setId(id);
      return this;
    }

    public RegistryRecommendationResponseBuilder withRecommendations(RegistryRecommendations recommendations) {
      this.instance.recommendations = recommendations;
      return this;
    }

    public RegistryRecommendationResponse build() {
      return this.instance;
    }
  }

  /**
   * Registry recommendations
   */
  public static class RegistryRecommendations {
    public List<Collection<MpackEntry>> mpackBundles;

    public void setMpackBundles(List<Collection<MpackEntry>> mpackBundles) {
      this.mpackBundles = mpackBundles;
    }

    public List<Collection<MpackEntry>> getMpackBundles() {
      return mpackBundles;
    }
  }
}
