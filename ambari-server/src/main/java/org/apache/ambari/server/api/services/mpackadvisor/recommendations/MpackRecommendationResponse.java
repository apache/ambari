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

package org.apache.ambari.server.api.services.mpackadvisor.recommendations;

import static org.apache.ambari.server.controller.internal.MpackAdvisorResourceProvider.BLUEPRINT_HOST_GROUPS_COMPONENTS_MPACK_INSTANCE_PROPERTY;
import static org.apache.ambari.server.controller.internal.MpackAdvisorResourceProvider.BLUEPRINT_HOST_GROUPS_COMPONENTS_NAME_PROPERTY;
import static org.apache.ambari.server.controller.internal.MpackAdvisorResourceProvider.BLUEPRINT_HOST_GROUPS_COMPONENTS_SERVICE_INSTANCE_PROPERTY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorResponse;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.collect.ImmutableMap;

/**
 * Recommendation response POJO.
 */
public class MpackRecommendationResponse extends MpackAdvisorResponse {

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

  /* TODO : Even though MpackInstance class exists at :
   * ambari-server/src/main/java/org/apache/ambari/server/topology/MpackInstance.java,
   * we need the MpackInstance -> ServiceInstance to have BlueprintConfigurations
   * class to handle the read Configurations with ease.
   * Need to look into using the existing classes.
   */

  public static class MpackInstance {
    @JsonProperty("name")
    private String name;
    @JsonProperty("version")
    private String version;

    private String mpackType;

    @JsonProperty("service_instances")
    private Collection<ServiceInstance> serviceInstances = new ArrayList<>();

    public String getName() {
      return name;
    }
    public void setName(String mpackName) {
      this.name = mpackName;
    }
    public String getVersion() {
      return version;
    }
    public void setVersion(String mpackVersion) {
      this.version = mpackVersion;
    }
    public Collection<ServiceInstance> getServiceInstances() {
      return serviceInstances;
    }

    public void setServiceInstances(Collection<ServiceInstance> serviceInstances) {
      this.serviceInstances = serviceInstances;
      serviceInstances.forEach(si -> si.setMpackInstance(this));
    }
    public void addServiceInstance(ServiceInstance serviceInstance) {
      serviceInstances.add(serviceInstance);
      serviceInstance.setMpackInstance(this);
    }
  }

  public static class ServiceInstance {
    @JsonProperty("name")
    private String name;
    @JsonProperty("type")
    private String type;
    @JsonProperty("version")
    private String version;
    @JsonProperty
    private Map<String, BlueprintConfigurations> configurations;

    @JsonProperty("mpackInstance")
    private MpackInstance mpackInstance;

    public ServiceInstance() { }

    public Map<String, BlueprintConfigurations> getConfigurations() {
      return configurations;
    }

    public void setConfigurations(Map<String, BlueprintConfigurations> configurations) {
      this.configurations = configurations;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public MpackInstance getMpackInstance() {
      return mpackInstance;
    }

    void setMpackInstance(MpackInstance mpackInstance) {
      this.mpackInstance = mpackInstance;
    }
  }

  public static class Blueprint {
    @JsonProperty
    private Map<String, BlueprintConfigurations> configurations;

    @JsonProperty("host_groups")
    private Set<HostGroup> hostGroups;

    @JsonProperty("mpack_instances")
    private Set<MpackInstance> mpackInstances;

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

    public void setMpackInstances(Set<MpackInstance> mpacks) {
      this.mpackInstances = mpacks;
    }

    public Set<MpackInstance> getMpackInstances() {
      return this.mpackInstances;
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
  }

  public static class HostGroup {
    @JsonProperty //("name")
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

    public static Map<String, String> createComponent(String componentName, String mpackInstance, String serviceInstance) {
      return ImmutableMap.of(
        BLUEPRINT_HOST_GROUPS_COMPONENTS_NAME_PROPERTY, componentName,
        BLUEPRINT_HOST_GROUPS_COMPONENTS_MPACK_INSTANCE_PROPERTY, mpackInstance,
        BLUEPRINT_HOST_GROUPS_COMPONENTS_SERVICE_INSTANCE_PROPERTY, serviceInstance);
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
  }

}
