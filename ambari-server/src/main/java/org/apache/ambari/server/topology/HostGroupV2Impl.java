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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.ambari.server.controller.internal.ProvisionAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class HostGroupV2Impl implements HostGroupV2, Configurable {

  private String name;
  private String blueprintName;
  private List<ComponentV2> components;
  private Set<ServiceId> serviceIds;
  private Configuration configuration;
  private String cardinality;
  private boolean containsMasterComponent;
  @JsonIgnore
  private Map<ServiceId, Service> serviceMap;

  public HostGroupV2Impl() { }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getBlueprintName() {
    return blueprintName;
  }

  @Override
  @JsonIgnore
  public String getFullyQualifiedName() {
    return blueprintName + ":" + name;
  }

  @Override
  public Collection<ComponentV2> getComponents() {
    return components;
  }

  @Override
  @JsonIgnore
  public Collection<String> getComponentNames() {
    return getComponentNames(components);
  }

  private Collection<String> getComponentNames(List<ComponentV2> components) {
    return Lists.transform(components,
      new Function<ComponentV2, String>() {
        @Override public String apply(@Nullable ComponentV2 input) { return input.getName(); }
      });
  }

  @Override
  @JsonIgnore
  public Collection<String> getComponentNames(ProvisionAction provisionAction) {
    List<ComponentV2> filtered =
      ImmutableList.copyOf(Collections2.filter(components, Predicates.equalTo(provisionAction)));
    return getComponentNames(filtered);
  }

  @Override
  public Collection<ComponentV2> getComponents(Service service) {
    return getComponentsByServiceId(service.getId());
  }

  @Override
  public Collection<ComponentV2> getComponentsByServiceId(ServiceId serviceId) {
    return components.stream().filter(c -> c.getServiceId().equals(serviceId)).collect(Collectors.toList());
  }

  @Override
  @JsonIgnore
  public boolean containsMasterComponent() {
    return containsMasterComponent;
  }

  @Override
  @JsonIgnore
  public Collection<ServiceId> getServiceIds() {
    return serviceIds;
  }

  @Override
  @JsonIgnore
  public Collection<Service> getServices() {
    return serviceMap.values();
  }

  @Override
  @JsonIgnore
  public Service getService(ServiceId serviceId) {
    return serviceMap.get(serviceId);
  }

  @Override
  @JsonIgnore
  public Collection<String> getServiceNames() {
    return serviceMap.values().stream().map(Service::getName).collect(Collectors.toList());
  }

  @JsonIgnore
  public void setServiceMap(Map<ServiceId, Service> serviceMap) {
    Preconditions.checkArgument(serviceMap.keySet().equals(this.serviceIds),
      "Maitained list of service ids doesn't match with received service map: %s vs %s", serviceIds, serviceMap.keySet());
    this.serviceMap = serviceMap;
  }

  @Override
  @JsonIgnore
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public String getCardinality() {
    return cardinality;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setBlueprintName(String blueprintName) {
    this.blueprintName = blueprintName;
  }

  public void setComponents(List<ComponentV2> components) {
    this.components = components;
    this.containsMasterComponent = components.stream().anyMatch(ComponentV2::isMasterComponent);
    this.serviceIds = components.stream().map(ComponentV2::getServiceId).collect(Collectors.toSet());
  }

  @JsonIgnore
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public void setCardinality(String cardinality) {
    this.cardinality = cardinality;
  }

}

