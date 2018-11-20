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

package org.apache.ambari.server.api.services.stackadvisor.recommendations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorResponse;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

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

    @JsonProperty("config-groups")
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    private Set<ConfigGroup> configGroups;

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

    public Set<ConfigGroup> getConfigGroups() {
      return configGroups;
    }

    public void setConfigGroups(Set<ConfigGroup> configGroups) {
      this.configGroups = configGroups;
    }
  }

  public static class Blueprint {
    @JsonProperty
    private Map<String, BlueprintConfigurations> configurations;

    @JsonProperty("host_groups")
    private Set<HostGroup> hostGroups;

    public Map<String, BlueprintConfigurations> getConfigurations() {
      return configurations;
    }

    public void setConfigurations(Map<String, BlueprintConfigurations> configurations) {
      this.configurations = configurations;
    }

    public Set<HostGroup> getHostGroups() {
      return hostGroups;
    }

    public void setHostGroups(Set<HostGroup> hostGroups) {
      this.hostGroups = hostGroups;
    }
  }

  public static class BlueprintConfigurations {
    @JsonProperty
    private final Map<String, String> properties = new HashMap<>();

    @JsonProperty("property_attributes")
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    private Map<String, ValueAttributesInfo> propertyAttributes = null;

    public BlueprintConfigurations() {

    }

    public Map<String, String> getProperties() {
      return properties;
    }

    /**
     * Returns a map of properties for this configuration.
     * <p/>
     * It is expected that a non-null value is always returned.
     *
     * @param properties a map of properties, always non-null
     */
    public void setProperties(Map<String, String> properties) {
      this.properties.clear();
      if(properties != null) {
        this.properties.putAll(properties);
      }
    }

    public Map<String, ValueAttributesInfo> getPropertyAttributes() {
      return propertyAttributes;
    }

    public void setPropertyAttributes(Map<String, ValueAttributesInfo> propertyAttributes) {
      this.propertyAttributes = propertyAttributes;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      BlueprintConfigurations that = (BlueprintConfigurations) o;

      if (properties != null ? !properties.equals(that.properties) : that.properties != null) return false;
      return propertyAttributes != null ? propertyAttributes.equals(that.propertyAttributes) : that.propertyAttributes == null;
    }

    @Override
    public int hashCode() {
      int result = properties != null ? properties.hashCode() : 0;
      result = 31 * result + (propertyAttributes != null ? propertyAttributes.hashCode() : 0);
      return result;
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

  public static class ConfigGroup {

    @JsonProperty
    private List<String> hosts;

    @JsonProperty
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    private Map<String, BlueprintConfigurations> configurations =
      new HashMap<>();

    @JsonProperty("dependent_configurations")
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    private Map<String, BlueprintConfigurations> dependentConfigurations =
      new HashMap<>();

    public ConfigGroup() {

    }

    public List<String> getHosts() {
      return hosts;
    }

    public void setHosts(List<String> hosts) {
      this.hosts = hosts;
    }

    public Map<String, BlueprintConfigurations> getConfigurations() {
      return configurations;
    }

    public void setConfigurations(Map<String, BlueprintConfigurations> configurations) {
      this.configurations = configurations;
    }

    public Map<String, BlueprintConfigurations> getDependentConfigurations() {
      return dependentConfigurations;
    }

    public void setDependentConfigurations(Map<String, BlueprintConfigurations> dependentConfigurations) {
      this.dependentConfigurations = dependentConfigurations;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ConfigGroup that = (ConfigGroup) o;

      if (hosts != null ? !hosts.equals(that.hosts) : that.hosts != null) return false;
      if (configurations != null ? !configurations.equals(that.configurations) : that.configurations != null)
        return false;
      return dependentConfigurations != null ? dependentConfigurations.equals(that.dependentConfigurations) : that.dependentConfigurations == null;
    }

    @Override
    public int hashCode() {
      int result = hosts != null ? hosts.hashCode() : 0;
      result = 31 * result + (configurations != null ? configurations.hashCode() : 0);
      result = 31 * result + (dependentConfigurations != null ? dependentConfigurations.hashCode() : 0);
      return result;
    }
  }

}
