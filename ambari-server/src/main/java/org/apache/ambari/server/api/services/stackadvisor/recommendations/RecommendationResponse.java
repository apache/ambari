/**
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

package org.apache.ambari.server.api.services.stackadvisor.recommendations;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorResponse;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Recommendation response POJO.
 */
public class RecommendationResponse extends StackAdvisorResponse {

  @JsonProperty
  private Set<String> hosts;

  @JsonProperty
  private Set<String> services;

  @JsonProperty
  private Recommendation recommendations;

  public Set<String> getHosts() {
    return hosts;
  }

  public void setHosts(Set<String> hosts) {
    this.hosts = hosts;
  }

  public Set<String> getServices() {
    return services;
  }

  public void setServices(Set<String> services) {
    this.services = services;
  }

  public Recommendation getRecommendations() {
    return recommendations;
  }

  public void setRecommendations(Recommendation recommendations) {
    this.recommendations = recommendations;
  }

  public static class Recommendation {
    @JsonProperty
    private Blueprint blueprint;

    @JsonProperty("blueprint_cluster_binding")
    private BlueprintClusterBinding blueprintClusterBinding;

    public Blueprint getBlueprint() {
      return blueprint;
    }

    public void setBlueprint(Blueprint blueprint) {
      this.blueprint = blueprint;
    }

    public BlueprintClusterBinding getBlueprintClusterBinding() {
      return blueprintClusterBinding;
    }

    public void setBlueprintClusterBinding(BlueprintClusterBinding blueprintClusterBinding) {
      this.blueprintClusterBinding = blueprintClusterBinding;
    }
  }

  public static class Blueprint {
    @JsonProperty
    private Map<String, Map<String, Map<String, String>>> configurations;

    @JsonProperty("host_groups")
    private Set<HostGroup> hostGroups;

    public Map<String, Map<String, Map<String, String>>> getConfigurations() {
      return configurations;
    }

    public void setConfigurations(Map<String, Map<String, Map<String, String>>> configurations) {
      this.configurations = configurations;
    }

    public Set<HostGroup> getHostGroups() {
      return hostGroups;
    }

    public void setHostGroups(Set<HostGroup> hostGroups) {
      this.hostGroups = hostGroups;
    }
  }

  public static class HostGroup {
    @JsonProperty
    private String name;

    @JsonProperty
    private Set<Map<String, String>> components;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Set<Map<String, String>> getComponents() {
      return components;
    }

    public void setComponents(Set<Map<String, String>> components) {
      this.components = components;
    }
  }

  public static class BlueprintClusterBinding {
    @JsonProperty("host_groups")
    private Set<BindingHostGroup> hostGroups;

    public Set<BindingHostGroup> getHostGroups() {
      return hostGroups;
    }

    public void setHostGroups(Set<BindingHostGroup> hostGroups) {
      this.hostGroups = hostGroups;
    }
  }

  public static class BindingHostGroup {
    @JsonProperty
    private String name;

    @JsonProperty
    private Set<Map<String, String>> hosts;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Set<Map<String, String>> getHosts() {
      return hosts;
    }

    public void setHosts(Set<Map<String, String>> hosts) {
      this.hosts = hosts;
    }
  }

}
