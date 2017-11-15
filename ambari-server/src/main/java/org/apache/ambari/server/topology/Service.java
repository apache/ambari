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

package org.apache.ambari.server.topology;


import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.server.controller.StackV2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


public class Service implements Configurable {

  private String type;

  private ServiceId id = new ServiceId();

  private String stackId;

  @JsonIgnore
  private Configuration configuration;

  private Set<ServiceId> dependencies = ImmutableSet.of();

  @JsonIgnore
  private Map<ServiceId, Service> dependencyMap = ImmutableMap.of();

  @JsonIgnore
  private ServiceGroup serviceGroup;

  @JsonIgnore
  private StackV2 stack;

  /**
   * Gets the name of this service
   *
   * @return component name
   */
  public String getName() {
    return this.id.getName();
  }

  @JsonIgnore
  public String getServiceGroupId() {
    return this.id.getServiceGroup();
  }

  @JsonIgnore
  public ServiceGroup getServiceGroup() {
    return serviceGroup;
  }

  public String getType() {
    return type;
  }

  public String getStackId() {
    return stackId;
  }

  @JsonIgnore
  public StackV2 getStack() {
    return stack;
  }

  @JsonIgnore
  public Set<ServiceId> getDependentServiceIds() {
    return dependencies;
  }

  @JsonProperty("dependencies")
  public Set<Map<String, String>> getDependenciesForSerialization() {
    return dependencies.stream().map(
      serviceId -> ImmutableMap.of("service_name", serviceId.getName(), "service_group", serviceId.getServiceGroup())).
      collect(Collectors.toSet());
  }

  public Set<Service> getDependencies() {
    return ImmutableSet.copyOf(dependencyMap.values());
  }

  @JsonIgnore
  public Configuration getConfiguration() {
    return configuration;
  }

  public void setType(String type) {
    this.type = type;
    if (null == this.getName()) {
      setName(type);
    }
  }

  public void setName(String name) {
    this.id.setName(name);
  }

  public void setServiceGroup(ServiceGroup serviceGroup) {
    this.serviceGroup = serviceGroup;
    this.id.setServiceGroup(serviceGroup.getName());
  }

  @JsonProperty("stack_id")
  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  public void setStackFromBlueprint(BlueprintV2 blueprint) {
    this.stack = blueprint.getStackById(this.stackId);
    configuration.setParentConfiguration(stack.getConfiguration());
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public void setDependencies(Set<ServiceId> dependencies) {
    this.dependencies = dependencies;
  }

  /**
   * Called during post-deserialization
   * @param dependencyMap
   */
  void setDependencyMap(Map<ServiceId, Service> dependencyMap) {
    Preconditions.checkArgument(dependencyMap.keySet().equals(dependencies),
      "Received dependency map is not consisted with persisted dependency references: %s vs. %s",
      dependencyMap.keySet(), dependencies);
    this.dependencyMap = dependencyMap;
  }

  @JsonIgnore
  public ServiceId getId() {
    return id;
  }

  @Override
  public String toString() {
    return "Service{" +
      "type='" + type + '\'' +
      ", id=" + id +
      ", stackId='" + stackId + '\'' +
      '}';
  }

  @JsonIgnore
  public String getServiceGroupName() {
    if (serviceGroup != null) {
      return serviceGroup.getName();
    }
    return null;
  }
}
